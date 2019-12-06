import mill._, scalalib._, publish._, os._

trait ScalaBase extends ScalaModule {
  def scalaVersion = "2.13.10"
}


object flix extends ScalaBase {
  def rootPath = os.pwd
  def millSourcePath = rootPath / 'main
  override def resources = T.sources {
    millSourcePath
  }

  def moduleDeps = Seq(runtime)
  def ivyDeps = Agg (
    ivy"com.chuusai::shapeless:2.3.3",
    ivy"com.github.scopt::scopt:4.0.1",
    ivy"org.java-websocket:Java-WebSocket:1.3.9",
    ivy"org.jline:jline:3.5.1",
    ivy"org.json4s::json4s-ast:3.5.5",
    ivy"org.json4s::json4s-core:3.5.5",
    ivy"org.json4s::json4s-native:3.5.5",
    ivy"org.ow2.asm:asm:9.2",
    ivy"org.parboiled::parboiled:2.2.1",
    ivy"org.scala-lang.modules::scala-parallel-collections:0.2.0",
    ivy"org.scala-lang.modules::scala-xml:2.0.0-M1",
    ivy"org.scala-lang:scala-reflect:2.13.2",
    ivy"org.scalactic::scalactic:3.0.8",
    ivy"org.scalatest::scalatest:3.0.8",
    ivy"io.get-coursier::coursier:2.1.0-RC4",
    ivy"org.tomlj:tomlj:1.1.0",
    // for bdd
    ivy"com.google.guava:guava:25.1-jre"
  )

  def publishVersion = "0.12.0"
  def pomSettings = PomSettings(
    description = "Flix",
    organization = "flix",
    url = "https://flix.dev",
    licenses = Seq(License.`Apache-2.0`),
    versionControl = VersionControl.github("flix","flix"),
    developers = Seq()
  )

  override def scalacOptions = Seq(
    "-language:postfixOps",
    // "-Xfatal-warnings",
    // "-Ypatmat-exhaust-depth", "400"
  )
  def unmanagedClasspath = T {
    if (!os.exists(rootPath / "lib")) Agg()
    else Agg.from(os.list(rootPath / "lib").map(PathRef(_)))
  }

  def mainClass = Some("ca.uwaterloo.flix.Main")
  def assembly = T {
    val res = super.assembly()
    val appJar = os.Path(sys.env.get("HOME").get.toString) / "app" / "flix.jar"
    os.remove.all(appJar)
    os.copy(
      T.dest / os.RelPath("../assembly.overridden/mill/scalalib/JavaModule/assembly.dest/out.jar"),
      appJar)
    res
  }
}

object runtime extends ScalaBase {
  def millSourcePath = os.pwd / 'runtime
  def publishVersion = "0.12.0"
  def pomSettings = PomSettings(
    description = "Flix",
    organization = "flix",
    url = "https://flix.dev",
    licenses = Seq(License.`Apache-2.0`),
    versionControl = VersionControl.github("flix","runtime"),
    developers = Seq()
  )
  def ivyDeps = Agg {
    ivy"org.junit.jupiter:junit-jupiter-api:5.3.1"
  }
}

object lsp extends ScalaBase {
  def millSourcePath = os.pwd / 'lsp
  def moduleDeps = Seq(flix)
  def ivyDeps = Agg (
    ivy"org.eclipse.lsp4j:org.eclipse.lsp4j:0.20.0",
    ivy"com.outr::scribe:3.11.1",
    ivy"com.outr::scribe-file:3.11.1",
    ivy"com.lihaoyi::pprint:0.7.0"
  )
  def assembly = T {
    val res = super.assembly()
    val appJar = os.Path(sys.env.get("HOME").get.toString) / "app" / "flix-lsp.jar"
    os.remove.all(appJar)
    os.copy(
      T.dest / os.RelPath("../assembly.overridden/mill/scalalib/JavaModule/assembly.dest/out.jar"),
      appJar)
    res
  }
}
