package dev.flix.lsp

import scala.jdk.CollectionConverters._
import LspConverters._

import java.nio.file.Path
import java.util
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

import scala.collection.mutable

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContextExecutorService
import scala.concurrent.Promise
import scala.concurrent.duration.Duration
import scala.util.control.NonFatal

import org.eclipse.lsp4j._
import org.eclipse.lsp4j.jsonrpc.Launcher
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest
import org.eclipse.lsp4j.jsonrpc.messages.{Either => JEither}

import ca.uwaterloo.flix.api.lsp.provider._
import ca.uwaterloo.flix.api.{CrashHandler, Flix, Version}
import ca.uwaterloo.flix.language.CompilationMessage
import ca.uwaterloo.flix.language.ast.SourceLocation
import ca.uwaterloo.flix.language.ast.TypedAst.Root
import ca.uwaterloo.flix.language.phase.extra.CodeHinter
import ca.uwaterloo.flix.util.Formatter.NoFormatter
import ca.uwaterloo.flix.util.Result.{Err, Ok}
import ca.uwaterloo.flix.util.Validation.{Failure, SoftFailure, Success}
import ca.uwaterloo.flix.util._
import ca.uwaterloo.flix.api.lsp.Index

import org.json4s.JsonAST.{JString, JValue}
import org.json4s.JsonDSL._
import org.json4s.ParserUtil.ParseException
import org.json4s._
import org.json4s.native.JsonMethods
import org.json4s.native.JsonMethods.parse
import java.nio.file.Paths
import java.net.URI
import java.nio.file.Files
import org.eclipse.lsp4j._

import ca.uwaterloo.flix.api.lsp

class FlixServer(
    ec: ExecutionContextExecutorService,
    initialConfig: FlixServerConfig,
    sh: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
) {

  lazy val root = new FlixRoot()

  /** LSP */
  val languageClient = new DelegatingLanguageClient(NoopClient)
  private var workspaceFolders: List[String] = Nil
  private var diagnostics: Diagnostics =
    new Diagnostics(this, languageClient)

  lazy val shutdownPromise = new AtomicReference[Promise[Unit]](null)
  private val cancelables = new MutableCancelable()
  private implicit val executionContext: ExecutionContextExecutorService = ec
  
  def connectToLanguageClient(client: FlixClient): Unit = {
    languageClient.underlying = client
    cancelables.add(() => languageClient.shutdown())
  }

  def cancelAll(): Unit = {
    Cancelable.cancelAll(
      List(
        cancelables,
        Cancelable(() => ec.shutdown()),
        Cancelable(() => sh.shutdown())
      )
    )
  }

  @JsonRequest("initialize")
  def initialize(
      params: InitializeParams
  ): CompletableFuture[InitializeResult] = {

    workspaceFolders = params.getWorkspaceFolders().asScala.map( _.getUri).toList
    val caps = new ServerCapabilities()

    caps.setDocumentSymbolProvider(true)
    // caps.setWorkspaceSymbolProvider(true)
    caps.setDefinitionProvider(true)
    caps.setHoverProvider(true)
    caps.setReferencesProvider(true)
    caps.setCompletionProvider(
      new CompletionOptions(
        true,
        List(".", "*").asJava
      )
    )
    caps.setCodeLensProvider(new CodeLensOptions(false))

    val syncOpts = new TextDocumentSyncOptions()
    syncOpts.setChange(TextDocumentSyncKind.Full)
    syncOpts.setSave(new SaveOptions(true))
    syncOpts.setOpenClose(true)
    caps.setTextDocumentSync(syncOpts)

    val filesPattern = new FileOperationPattern("**/*.flix")
    filesPattern.setMatches(FileOperationPatternKind.File)
    val folderFilesPattern = new FileOperationPattern("**/")
    folderFilesPattern.setMatches(FileOperationPatternKind.Folder)
    val fileOperationOptions = new FileOperationOptions(
      List(
        new FileOperationFilter(filesPattern),
        new FileOperationFilter(folderFilesPattern)
      ).asJava
    )
    val fileOpsCaps =
      new FileOperationsServerCapabilities()
    fileOpsCaps.setWillRename(fileOperationOptions)
    val workspaceCapabilities = new WorkspaceServerCapabilities()
    workspaceCapabilities.setFileOperations(
      fileOpsCaps
    )
    caps.setWorkspace(workspaceCapabilities)

    val serverInfo = new ServerInfo("Flix", "0.0.0")
    val result = new InitializeResult(caps, serverInfo)
    
    CompletableFuture.completedFuture(result)
  }

  @JsonNotification("initialized")
  def initialized(params: InitializedParams): CompletableFuture[Unit] = {
    initWorkspace()
    CompletableFuture.completedFuture(())
  }

  @JsonNotification("textDocument/didOpen")
  def didOpen(params: DidOpenTextDocumentParams): CompletableFuture[Unit] = {
    documentChange(
      params.getTextDocument().getUri(),
      params.getTextDocument().getText()
    )
    CompletableFuture.completedFuture(())
  }

  @JsonNotification("textDocument/didChange")
  def didChange(
      params: DidChangeTextDocumentParams
  ): CompletableFuture[Unit] = {
    params.getContentChanges().asScala.headOption match {
      case None =>
        CompletableFuture.completedFuture(())
      case Some(change) =>
        CompletableFuture.completedFuture(())
    }
  }

  def documentChange(uri: String, text: String) = {
    val path = uri.asPath
    diagnostics.clear(path)
    root.addSource(path, text)
    withProgress("Compiling ...", () => root.compile())
    diagnostics.publish()
  }

  def currentDiagnostics = {
    val initError: List[PublishDiagnosticsParams] = root.currentInitError match {
      case Some(msg) => 
        val workPath = root.workspacePath.getOrElse(Paths.get(".").toAbsolutePath()).toString.asFileUri
        val diag = new Diagnostic(new Range(new Position(), new Position()), msg)
        List(new PublishDiagnosticsParams(workPath, List(diag).asJava))
      case None => List()
    }

    root.currentDiagnostics.map(_.asLsp) ++ initError
  }

  @JsonNotification("textDocument/didClose")
  def didClose(params: DidCloseTextDocumentParams): Unit = {
    val path = params.getTextDocument().getUri().asPath
    diagnostics.clear(path)
    root.removeSource(path)
    CompletableFuture.completedFuture(())
  }

  @JsonNotification("textDocument/didSave")
  def didSave(params: DidSaveTextDocumentParams): CompletableFuture[Unit] = {
    documentChange(params.getTextDocument().getUri(), params.getText())
    CompletableFuture.completedFuture(())
  }

  @JsonRequest("textDocument/hover")
  def hover(params: HoverParams): CompletableFuture[Hover] = {
    import org.json4s._
    implicit val formats: Formats = DefaultFormats
    val path = textDocumentPath(params) 
    val hover = root.hover(path, params.getPosition)
    CompletableFuture.completedFuture(hover)
  }

  @JsonRequest("textDocument/completion")
  def completion(params: CompletionParams): CompletableFuture[CompletionList] = {
    val uri = textDocumentPath(params) 
    val rs = root.autocompletions(uri, params.getPosition)
    CompletableFuture.completedFuture(rs)
  }

  @JsonRequest("textDocument/references")
  def references(
      params: ReferenceParams
  ): CompletableFuture[util.List[Location]] = {
    import org.json4s._
    implicit val formats: Formats = DefaultFormats
    val path = textDocumentPath(params) 
    val refs = root.references(path, params.getPosition()) 
    CompletableFuture.completedFuture(refs.asJava)
  }

  @JsonRequest("textDocument/definition")
  def definition(
      params: TextDocumentPositionParams
  ): CompletableFuture[util.List[Location]] = {
    val path = textDocumentPath(params) 
    val defs = root.definitions(path, params.getPosition) 
    CompletableFuture.completedFuture(defs.asJava)
  }

  def textDocumentPath(params: TextDocumentPositionParams) =
    params.getTextDocument.getUri.asPath.toString

  @JsonRequest("textDocument/typeDefinition")
  def typeDefinition(
      position: TextDocumentPositionParams
  ): CompletableFuture[util.List[Location]] =
    CompletableFuture.completedFuture(List[Location]().asJava)

  @JsonRequest("textDocument/implementation")
  def implementation(
      params: TextDocumentPositionParams
  ): CompletableFuture[util.List[Location]] =
    definition(params)
  
  @JsonRequest("workspace/symbol")
  def workspaceSymbol(
      params: WorkspaceSymbolParams
  ): CompletableFuture[util.List[SymbolInformation]] = {
    val sym = root.workspaceSymbols(params.getQuery) 
    CompletableFuture.completedFuture(sym.asJava)
  }

  @JsonRequest("textDocument/documentSymbol")
  def documentSymbol(
      params: DocumentSymbolParams
  ): CompletableFuture[util.List[DocumentSymbol]
  ] = {
    val uri = params.getTextDocument().getUri.asPath.toString
    val sym = root.documentSymbols(uri) 
    CompletableFuture.completedFuture(sym.asJava)
  }

  @JsonRequest("shutdown")
  def shutdown(): CompletableFuture[Unit] = {
    val promise = Promise[Unit]()
    // Ensure we only run `shutdown` at most once and that `exit` waits for the
    // `shutdown` promise to complete.
    if (shutdownPromise.compareAndSet(null, promise)) {
      scribe.info("shutting down")
      try {
        cancelAll()
      } catch {
        case NonFatal(e) =>
          scribe.error("cancellation error", e)
      } finally {
        promise.success(())
      }
      promise.future.asCompletable
    } else {
      shutdownPromise.get().future.asCompletable
    }
  }

  @JsonNotification("exit")
  def exit(): Unit = {
    // `shutdown` is idempotent, we can trigger it as often as we like.
    shutdown()
    // Ensure that `shutdown` has completed before killing the process.
    // Some clients may send `exit` immediately after `shutdown` causing
    // the build server to get killed before it can clean up resources.
    try {
      Await.result(
        shutdownPromise.get().future,
        Duration(3, TimeUnit.SECONDS)
      )
    } catch {
      case NonFatal(e) =>
        scribe.error("shutdown error", e)
    } finally {
      System.exit(0)
    }
  }

  def addSource(path: Path) = {
    if (
      Files.isRegularFile(path)
      && Files.isReadable(path)
      && path.getFileName.toString.endsWith(".flix")
    ) {
      root.addSourceFile(path)
    }
  }


  def initWorkspace(): Unit = {
    try {
      for (e <- workspaceFolders) {
        val uri = URI.create(e)
        if (uri.getScheme == "file") {
          val path = Paths.get(uri.getPath)
          withProgress("Initializing workspace ...", () => root.initWorkspace(path))
        }
      }
      withProgress("Compiling workspace ...", () => root.compile())
      diagnostics.publish()
    } catch {
      case ex: Exception => {
        ex.printStackTrace()
        val msg = new MessageParams(MessageType.Error, ex.getMessage())
        languageClient.showMessage(msg)
        root.setInitError(Some(ex.getMessage()))
      }
    }
  }

  var progressCounter = 0

  def withProgress( msg: String, f: () => Unit) = {
    progressCounter = progressCounter + 1
    val n = progressCounter
    try {
      progressBegin(n, msg)
      f()
    } finally {
      progressEnd(n, msg)
    }
  }

  def progressBegin(token: Int, msg: String): Unit = {
    val work = new WorkDoneProgressBegin()
    work.setMessage(msg)
    work.setTitle("flix")
    languageClient.notifyProgress(
      new ProgressParams (
        JEither.forRight(token), 
        JEither.forLeft(work)))
  }
  
  def progressEnd(token: Int, msg: String): Unit = {
    val work = new WorkDoneProgressEnd()
    work.setMessage(msg)
    languageClient.notifyProgress(
      new ProgressParams (
        JEither.forRight(token), 
        JEither.forLeft(work)))
  }
}

object FlixServer {
  def main(args: Array[String]): Unit = {
    startServer()
  }

  def startServer(): Unit = {
    val tmpDir = System.getProperty("java.io.tmpdir")
    val systemIn = System.in
    val systemOut = System.out
    FlixLogger.redirectSystemOut(Paths.get(tmpDir, "/flix-lsp.log"))
    val exec = Executors.newCachedThreadPool()
    val ec = ExecutionContext.fromExecutorService(exec)
    val sh = Executors.newSingleThreadScheduledExecutor()
    val initialConfig = FlixServerConfig.default
    val server = new FlixServer(
      ec,
      initialConfig = initialConfig
    )
    try {
      scribe.info(s"Starting Flix server with configuration: $initialConfig")
      val launcher = new Launcher.Builder[FlixClient]()
        .traceMessages(
          FlixLogger.newFilePrintWriter(Paths.get(tmpDir, "flix-lsp-trace.log"))
        )
        .setExecutorService(exec)
        .setInput(systemIn)
        .setOutput(systemOut)
        .setRemoteInterface(classOf[FlixClient])
        .setLocalService(server)
        .create()
      val clientProxy = launcher.getRemoteProxy
      // important, plug language client before starting listening!
      server.connectToLanguageClient(clientProxy)
      launcher.startListening().get()
    } catch {
      case NonFatal(e) =>
        e.printStackTrace(System.out)
        sys.exit(1)
    } finally {
      server.cancelAll()
      ec.shutdownNow()
      sh.shutdownNow()
      sys.exit(0)
    }
  }
}
