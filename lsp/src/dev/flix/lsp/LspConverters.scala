package dev.flix.lsp

import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture

import scala.concurrent.Future
import scala.jdk.CollectionConverters._
import scala.jdk.FutureConverters._

import ca.uwaterloo.flix.api.lsp
import org.eclipse.lsp4j._

object LspConverters {
  import org.json4s._
  implicit val formats: Formats = DefaultFormats

  implicit class XUri(uri: String) {
    def asPath: Path = Paths.get(URI.create(uri))
    def asFileUri = 
      if (uri.startsWith("file://"))
        uri 
      else 
        s"file://${uri}" 
  }
  implicit class XScalaFuture[A](future: Future[A]) {
    def asCompletable: CompletableFuture[A] =
      future.asJava.toCompletableFuture
  }
  implicit class JXValue(x: JValue) {
    def asPosition: Position = {
      val line = (x \ "line").extract[Int]
      val char = (x \ "character").extract[Int]
      new Position(line, char)
    }
    def asRange: Range = {
      val start = (x \ "start").asPosition
      val end = (x \ "end").asPosition
      new Range(start, end)
    }
    def asLocations: List[Location] = {
      if((x\"status").extract[String] == "success") {
        (x\"result").children.map( e => {
          val uri = (e \ "uri").extract[String].asFileUri
          val range = (e \ "range").asRange
          new Location(uri, range)      
        }).filter(! _.getUri.contains("<unknown>"))
      } else {
        List.empty[Location]
      }
    }
    def asLinkLocations: List[Location] = {
      if((x\"status").extract[String] == "success") {
        val e = x\"result"
        val uri = (e \ "targetUri").extract[String].asFileUri
        val range = (e \ "targetRange").asRange
        List(new Location(uri, range)) 
      } else {
        List.empty[Location]
      }
    }
    def asCompletionList: CompletionList = {
      if((x\"status").extract[String] == "success") {
        val result = (x\"result")
        val incomplete = (result\"isIncomplete").extract[Boolean]
        val items = (result\"items").children.map( e => {
          val label = (e\"label").extractOrElse[String]("")
          val sortText = (e\"sortText").extractOrElse[String]("")
          val documentation = (e\"documentation").extractOrElse[String]("")
          val r = new CompletionItem(label)
          r.setSortText(sortText)
          r.setDocumentation(documentation)
          r
        })
        new CompletionList(incomplete, items.asJava)
      } else {
        new CompletionList()
      }
    }
    def asHover: Hover = {
      (x \ "result" \ "contents" \ "value").extractOpt[String] match {
        case Some(md) => new Hover(new MarkupContent(MarkupKind.MARKDOWN, md))
        case None     => null
      }
    }
  }
  implicit class XPosition(x: lsp.Position) {
    def asLsp: Position =
      new Position(x.line, x.character)
  }
  implicit class XRange(x: lsp.Range) {
    def asLsp: Range =
      new Range(x.start.asLsp, x.end.asLsp)
  }
  implicit class XLocation(x: lsp.Location) {
    def asLsp: Location =
      new Location(x.uri.asFileUri, x.range.asLsp)
  }
  implicit class XSymbolKind(x: lsp.SymbolKind) {
    def asLsp: SymbolKind = SymbolKind.forValue(x.toInt)
  }
  implicit class XSymbolTag(x: lsp.SymbolTag) {
    def asLsp: SymbolTag = SymbolTag.forValue(1)
  }
  implicit class XDocumentSymbol(x: lsp.DocumentSymbol) {
    def asLsp: DocumentSymbol = {
      val tags = x.tags.map(_.asLsp)
      val children = x.children.map(_.asLsp)
      val y = new DocumentSymbol(
        x.name,
        x.kind.asLsp,
        x.range.asLsp,
        x.selectionRange.asLsp
      )
      y.setChildren(children.asJava)
      y.setTags(tags.asJava)
      y
    }
  }
  implicit class XSymbolInformation(x: lsp.SymbolInformation) {
    def asLsp: SymbolInformation = {
      val tags = x.tags.map(_.asLsp)
      val y = new SymbolInformation(
        x.name,
        x.kind.asLsp,
        x.location.asLsp,
        x.containerName.getOrElse(null)
      )
      y
    }
  }
  implicit class XDiagnosticSeverity(x: lsp.DiagnosticSeverity) {
    def asLsp: DiagnosticSeverity =
      DiagnosticSeverity.forValue(x.toInt)
  }
  implicit class XDiagnostic(x: lsp.Diagnostic) {
    def asLsp: Diagnostic = x.severity match {
      case Some(s) =>
        new Diagnostic(
          x.range.asLsp,
          x.message,
          s.asLsp,
          x.source.getOrElse("")
        )
      case None => new Diagnostic(x.range.asLsp, x.message)
    }
  }
  implicit class XPublishDiagnosticParams(diag: lsp.PublishDiagnosticsParams) {
    def asLsp: PublishDiagnosticsParams = {
      new PublishDiagnosticsParams(
        diag.uri.asFileUri,
        diag.diagnostics.map(_.asLsp).asJava
      )
    }
  }

}
