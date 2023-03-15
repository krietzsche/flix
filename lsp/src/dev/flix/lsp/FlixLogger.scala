package dev.flix.lsp

import scribe._
import scribe.file.FileWriter
import scribe.file.PathBuilder
import scribe.format.Formatter
import scribe.format.FormatterInterpolator
import scribe.format.date
import scribe.format.levelPaddedRight
import scribe.format.messages
import scribe.modify.LogModifier

import java.nio.file.Files
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.io.PrintStream
import java.io.PrintWriter
import scala.annotation.tailrec
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.BufferedWriter

object FlixLogger {
  val level = Level.Debug

  def redirectSystemOut(logfile: Path): Unit = {
    Files.createDirectories(logfile.getParent())
    val logStream = Files.newOutputStream(
      logfile,
      StandardOpenOption.APPEND,
      StandardOpenOption.CREATE
    )
    val out = new PrintStream(logStream)
    System.setOut(out)
    System.setErr(out)
    configureRootLogger(logfile)
  }

  def defaultFormat: Formatter = formatter"$date $levelPaddedRight $messages"

  def configureRootLogger(logfile: Path): Unit = {
    Logger.root
      .clearModifiers()
      .clearHandlers()
      .withHandler(
        writer = newFileWriter(logfile),
        formatter = defaultFormat,
        minimumLevel = Some(level)
      )
      .replace()
  }

  def newFilePrintWriter(file: Path): PrintWriter = {
    val fos = Files.newOutputStream(
      file,
      StandardOpenOption.CREATE,
      StandardOpenOption.TRUNCATE_EXISTING // don't append infinitely to existing file
    )
    new PrintWriter(fos, true)
  }

  def newFileWriter(logfile: Path): FileWriter =
    FileWriter(pathBuilder = PathBuilder.static(logfile)).flushAlways

}
