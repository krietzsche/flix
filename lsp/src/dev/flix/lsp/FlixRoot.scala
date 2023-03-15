package dev.flix.lsp

import org.eclipse.lsp4j
import LspConverters._

import ca.uwaterloo.flix.api.lsp.Indexer
import ca.uwaterloo.flix.api.lsp.provider._
import ca.uwaterloo.flix.api.lsp.provider.completion.{DeltaContext, Differ}
import ca.uwaterloo.flix.api.{CrashHandler, Flix, Version}
import ca.uwaterloo.flix.language.CompilationMessage
import ca.uwaterloo.flix.language.ast.SourceLocation
import ca.uwaterloo.flix.language.ast.TypedAst.Root
import ca.uwaterloo.flix.language.phase.extra.CodeHinter
import ca.uwaterloo.flix.util.Formatter.NoFormatter
import ca.uwaterloo.flix.util.Result.{Err, Ok}
import ca.uwaterloo.flix.util.Validation.{Failure, SoftFailure, Success}
import ca.uwaterloo.flix.util.Options
import ca.uwaterloo.flix.api.lsp.Index
import ca.uwaterloo.flix.api.lsp.Position
import ca.uwaterloo.flix.api.lsp.PublishDiagnosticsParams

import scala.collection.mutable
import java.nio.file.Path
import ca.uwaterloo.flix.api.Bootstrap

class FlixRoot {

  /** The Flix instance (the same instance is used for incremental compilation).
    */
  private val flix: Flix =
    new Flix().setFormatter(NoFormatter).setOptions(Options.Default.copy(debug = true, installDeps = true ))

  /** A map from source URIs to source code.
    */
  private val sources: mutable.Map[String, String] = mutable.Map.empty

  /** The current AST root. The root is null until the source code is compiled.
    */
  private var root: Option[Root] = None

  /** The current reverse index. The index is empty until the source code is
    * compiled.
    */
  private var index: Index = Index.empty

  /** The current delta context. Initially has no changes.
    */
  private var delta: DeltaContext = DeltaContext(Nil)

  /** A Boolean that records if the root AST is current (i.e. up-to-date).
    */
  private var current: Boolean = false

  private var currentErrors: List[CompilationMessage] = Nil

  var currentDiagnostics: List[PublishDiagnosticsParams] = Nil 

  var currentInitError: Option[String] = None
  
  def setInitError(msg: Option[String]) = {
    currentInitError = msg 
  }

  var workspacePath: Option[Path] = None

  def initWorkspace(path: Path) = {
    workspacePath = Some(path)
    val boot = Bootstrap.bootstrap(path)(System.out)
    boot.get.reconfigureFlix(flix)
  }

  def addSourceFile(path: Path) = {
    scribe.debug(s"addFlix $path")
    flix.addFlix(path)
  }

  def addSource(path: Path, text: String) = {
    val pathStr = path.toString
    flix.addSourceCode(pathStr, text)
    sources.put(pathStr, text)
  }

  def removeSource(path: Path) = {
    flix.remSourceCode(path.toString)
    sources.remove(path.toString)
  }

  /** Processes a validate request.
    */
  def compile(): Unit = {

    // Measure elapsed time.
    val t = System.nanoTime()
    try {
      // Run the compiler up to the type checking phase.
      flix.check() match {
        case Success(root) =>
          scribe.debug("Compilation was successful. Reverse index built.")
          compileSuccess(
            root,
            LazyList.empty,
            flix.options.explain,
            t
          )

        case SoftFailure(root, errors) =>
          scribe.debug("Compilation had non-critical errors. Reverse index built.")
          compileSuccess(
            root,
            errors,
            flix.options.explain,
            t
          )

        case Failure(errors) =>
          scribe.debug("Compilation failed.")

          // Mark the AST as outdated and update the current errors.
          this.current = false
          this.currentErrors = errors.toList
          this.currentDiagnostics = PublishDiagnosticsParams.fromMessages(
            errors,
            flix.options.explain
          )
      }
    } catch {
      case ex: Throwable =>
        // Mark the AST as outdated.
        this.current = false
        CrashHandler.handleCrash(ex)(flix)
    }
  }

  /** Helper function for [[compile]] which handles successful and soft failure
    * compilations.
    */
  private def compileSuccess(
      root: Root,
      errors: LazyList[CompilationMessage],
      explain: Boolean,
      t0: Long
  ): Any = {
    val oldRoot = this.root
    this.root = Some(root)
    this.index = Indexer.visitRoot(root)
    this.delta = Differ.difference(oldRoot, root)
    this.current = true
    this.currentErrors = errors.toList

    // Compute elapsed time.
    val e = System.nanoTime() - t0

    // Print query time.
    scribe.debug(s"lsp/check: ${e / 1_000_000}ms")

    // Compute Code Quality hints.
    val codeHints = CodeHinter.run(root, sources.keySet.toSet)(flix, index)

    // Determine the status based on whether there are errors.
    val status = if (errors.isEmpty) "success" else "failure"

    this.currentDiagnostics = (
      PublishDiagnosticsParams.fromMessages(errors,explain) 
      ::: 
      PublishDiagnosticsParams.fromCodeHints(codeHints)
    )

  }

  def lspPosition(pos: lsp4j.Position) = Position(pos.getLine + 1, pos.getCharacter + 1)

  def hover(path: String, pos: lsp4j.Position) =
    HoverProvider.processHover(
      path,
      lspPosition(pos),
      true
    )(index, root, flix).asHover

  def autocompletions(path: String, pos: lsp4j.Position) =
    CompletionProvider.autoComplete(path, lspPosition(pos), sources.get(path), currentErrors)(flix, index, root, delta)
      .asCompletionList

  def references(path: String, pos: lsp4j.Position) = 
    FindReferencesProvider.findRefs(path, lspPosition(pos))(index, root.orNull).asLocations

  def definitions(path: String, pos: lsp4j.Position) = 
    GotoProvider.processGoto(path, lspPosition(pos))(index, root).asLinkLocations

  def documentSymbols(path: String) =
      SymbolProvider.processDocumentSymbols(path)(root.orNull).map(_.asLsp)

  def workspaceSymbols(query: String) = 
      SymbolProvider.processWorkspaceSymbols(query)(root.orNull).map(_.asLsp)
}

