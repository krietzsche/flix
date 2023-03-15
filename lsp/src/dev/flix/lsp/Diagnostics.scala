package dev.flix.lsp

import java.nio.file.Path
import java.util.regex.Pattern

import scala.collection.mutable
import scala.jdk.CollectionConverters._

import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.DiagnosticSeverity
import org.eclipse.lsp4j.PublishDiagnosticsParams
import ca.uwaterloo.flix.api.lsp
import LspConverters._

class Diagnostics(
    server: FlixServer,
    client: FlixClient
) {

  def clear(path: Path): Unit = {
    client.publishDiagnostics(lsp.PublishDiagnosticsParams(path.toString, List.empty).asLsp)
  }

  def publish(): Unit = {
    for( e <- server.currentDiagnostics )
      client.publishDiagnostics(e)
  }
}

