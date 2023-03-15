package dev.flix.lsp

import java.util.concurrent.CompletableFuture

import org.eclipse.lsp4j.MessageActionItem
import org.eclipse.lsp4j.MessageParams
import org.eclipse.lsp4j.PublishDiagnosticsParams
import org.eclipse.lsp4j.ShowMessageRequestParams
import org.eclipse.lsp4j.services.LanguageClient
import ca.uwaterloo.flix.api.Flix
import org.eclipse.lsp4j.LogTraceParams
import org.eclipse.lsp4j.ProgressParams

trait FlixClient extends LanguageClient {
  override def telemetryEvent(x: Any): Unit = ()
  override def publishDiagnostics(params: PublishDiagnosticsParams): Unit = () 
  override def showMessage(x: MessageParams): Unit = ()
  override def showMessageRequest(
      x: ShowMessageRequestParams
  ): CompletableFuture[MessageActionItem] =
    new CompletableFuture[MessageActionItem]()
  override def logMessage(x: MessageParams): Unit = ()
  override def logTrace(x: LogTraceParams): Unit = ()
  override def notifyProgress(x: ProgressParams): Unit = ()
  def shutdown(): Unit = {}
}
