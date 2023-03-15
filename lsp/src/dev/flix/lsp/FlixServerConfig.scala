package dev.flix.lsp

case class FlixServerConfig()
object FlixServerConfig {
  val default: FlixServerConfig = FlixServerConfig()
  def isTesting: Boolean = "true" == System.getProperty("example.testing")
}
