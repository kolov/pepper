import sbt.Keys.credentials
import sbt.{url, Credentials, Developer, Path, ScmInfo}

lazy val Versions = new {
  val zio = "1.0.0-RC18-2"
  val zioInteropCats = "2.0.0.0-RC12"
  val http4s = "0.21.0-M6"
  val tapir = "0.14.3"
  val specs2 = "4.7.1"
  val logback = "1.2.3"
}

lazy val scala212 = "2.12.10"
lazy val scala213 = "2.13.1"

lazy val supportedScalaVersions = List(scala212, scala213)

ThisBuild / organization      := "com.akolov"
ThisBuild / name              := "pepper"
ThisBuild / scalaVersion      := scala213
ThisBuild / publishMavenStyle := true
ThisBuild / credentials += Credentials(Path.userHome / ".sbt" / "sonatype_credentials")
ThisBuild / description       := "Authorization for Tapir endpoints"
ThisBuild / licenses          := Seq("MIT License" -> url("https://github.com/kolov/pepper/blob/master/LICENSE"))
ThisBuild / useGpg            := true
ThisBuild / homepage          := Some(url("https://github.com/kolov/pepper"))
ThisBuild / releaseCrossBuild := true
ThisBuild / pomIncludeRepository := { _ =>
  false
}
ThisBuild / publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/kolov/pepper"),
    "scm:git@github.com:kolov/pepper.git"
  )
)

ThisBuild / developers := List(
  Developer(
    id = "kolov",
    name = "Assen Kolov",
    email = "assen.kolov@gmail.com",
    url = url("https://github.com/kolov")
  )
)

lazy val commonSettings = Seq(
  crossScalaVersions := supportedScalaVersions,
  addCompilerPlugin("org.typelevel" % "kind-projector" % "0.11.0" cross CrossVersion.full)
)

lazy val testDependencies = Seq(
  "org.specs2" %% "specs2-core"                    % Versions.specs2 % "test",
  "org.specs2" %% "specs2-mock"                    % Versions.specs2 % "test",
  "com.codecommit" %% "cats-effect-testing-specs2" % "0.3.0"         % "test"
)

lazy val core = (project in file("core")).settings(
  name := "pepper-core",
  commonSettings,
  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-core"                % "2.1.1",
    "com.softwaremill.sttp.tapir" %% "tapir-core" % Versions.tapir
  ) ++ testDependencies
)

lazy val `pepper-http4s` = (project in file("pepper-http4s"))
  .dependsOn(core)
  .settings(
    name := "pepper-http4s",
    commonSettings,
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % Versions.tapir,
      "org.http4s" %% "http4s-blaze-server"                  % Versions.http4s
    ) ++ testDependencies
  )

lazy val demo = (project in file("demo"))
  .dependsOn(`pepper-http4s`)
  .settings(
    commonSettings,
    name           := "pepper-http4s-demo",
    publish / skip := true,
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs"       % Versions.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml" % Versions.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-http4s"  % Versions.tapir,
      "dev.zio" %% "zio"                                          % Versions.zio,
      "dev.zio" %% "zio-interop-cats"                             % Versions.zioInteropCats,
      "ch.qos.logback"                                            % "logback-classic" % Versions.logback
    ) ++ testDependencies
  )

lazy val root = (project in file("."))
  .aggregate(core, `pepper-http4s`, demo)
  .settings(
    publish / skip     := true,
    crossScalaVersions := Nil
  )

lazy val docs = project
  .in(file("project-docs")) // important: it must not be docs/
  .dependsOn(demo)
  .enablePlugins(MdocPlugin)
  .settings(
    crossScalaVersions := Nil,
    publish / skip     := true,
    mdocOut            := new java.io.File(".")
  )
