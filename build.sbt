ThisBuild / version := "1.0.1"

lazy val rockthejvm    = "com.rockthejvm"
lazy val scala3Version = "3.8.1"

///////////////////////////////////////////////////////////////////////////////////////////////////////////
// Common - contains domain model
///////////////////////////////////////////////////////////////////////////////////////////////////////////

lazy val core = (crossProject(JSPlatform, JVMPlatform) in file("common"))
  .settings(
    name         := "common",
    scalaVersion := scala3Version,
    organization := rockthejvm,
    scalacOptions ++= Seq(
      "-Xmax-inlines",
      "64",
      "-Ykind-projector:underscores"
    )
  )
  .jvmSettings(
    // add here if necessary
  )
  .jsSettings(
    // Add JS-specific settings here
  )

///////////////////////////////////////////////////////////////////////////////////////////////////////////
// Frontend
///////////////////////////////////////////////////////////////////////////////////////////////////////////

lazy val tyrianVersion = "0.14.0"
lazy val fs2DomVersion = "0.1.0"
lazy val laikaVersion  = "0.19.0"
lazy val circeVersion  = "0.14.8"

lazy val app = (project in file("app"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name         := "app",
    scalaVersion := scala3Version,
    organization := rockthejvm,
    scalacOptions ++= Seq(
      "-Xmax-inlines",
      "64",
      "-Ykind-projector:underscores"
    ),
    libraryDependencies ++= Seq(
      "io.indigoengine" %%% "tyrian-io"     % tyrianVersion,
      "com.armanbilge"  %%% "fs2-dom"       % fs2DomVersion,
      "org.planet42"    %%% "laika-core"    % laikaVersion,
      "io.circe"        %%% "circe-core"    % circeVersion,
      "io.circe"        %%% "circe-parser"  % circeVersion,
      "io.circe"        %%% "circe-generic" % circeVersion
    ),
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) },
    semanticdbEnabled := true,
    autoAPIMappings   := true
  )
  .dependsOn(core.js)

///////////////////////////////////////////////////////////////////////////////////////////////////////////
// Backend
///////////////////////////////////////////////////////////////////////////////////////////////////////////

lazy val catsEffectVersion          = "3.5.4"
lazy val http4sVersion              = "0.23.27"
lazy val doobieVersion              = "1.0.0-RC5"
lazy val pureConfigVersion          = "0.17.7"
lazy val log4catsVersion            = "2.7.0"
lazy val tsecVersion                = "0.5.0"
lazy val scalaTestVersion           = "3.2.19"
lazy val scalaTestCatsEffectVersion = "1.5.0"
lazy val testContainerVersion       = "1.20.4"
lazy val logbackVersion             = "1.5.6"
lazy val slf4jVersion               = "2.0.16"
lazy val javaMailVersion            = "1.6.2"
lazy val stripeVersion              = "24.3.0"

lazy val server = (project in file("server"))
  .settings(
    name         := "server",
    scalaVersion := scala3Version,
    organization := rockthejvm,
    scalacOptions ++= Seq(
      "-Xmax-inlines",
      "64",
      "-Ykind-projector:underscores"
    ),
    libraryDependencies ++= Seq(
      // Core
      "org.typelevel" %% "cats-effect" % catsEffectVersion,

      // HTTP
      "org.http4s" %% "http4s-dsl"          % http4sVersion,
      "org.http4s" %% "http4s-ember-server" % http4sVersion,
      "org.http4s" %% "http4s-circe"        % http4sVersion,

      // JSON
      "io.circe" %% "circe-core"    % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser"  % circeVersion,

      // Database
      "org.tpolecat" %% "doobie-core"      % doobieVersion,
      "org.tpolecat" %% "doobie-hikari"    % doobieVersion,
      "org.tpolecat" %% "doobie-postgres"  % doobieVersion,
      "org.tpolecat" %% "doobie-scalatest" % doobieVersion % Test,

      // Configuration
      "com.github.pureconfig" %% "pureconfig-core" % pureConfigVersion,

      // Logging
      "org.typelevel" %% "log4cats-slf4j" % log4catsVersion,
      "org.slf4j"      % "slf4j-simple"   % slf4jVersion,

      // Security
      "io.github.jmcardon" %% "tsec-http4s" % tsecVersion,

      // Email
      "com.sun.mail" % "javax.mail" % javaMailVersion,

      // Payment
      "com.stripe" % "stripe-java" % stripeVersion,

      // Testing
      "org.typelevel"     %% "log4cats-noop"                 % log4catsVersion            % Test,
      "org.scalatest"     %% "scalatest"                     % scalaTestVersion           % Test,
      "org.typelevel"     %% "cats-effect-testing-scalatest" % scalaTestCatsEffectVersion % Test,
      "org.testcontainers" % "testcontainers"                % testContainerVersion       % Test,
      "org.testcontainers" % "postgresql"                    % testContainerVersion       % Test,
      "ch.qos.logback"     % "logback-classic"               % logbackVersion             % Test
    ),
    Compile / mainClass := Some("com.rockthejvm.jobsboard.Application")
  )
  .dependsOn(core.jvm)

///////////////////////////////////////////////////////////////////////////////////////////////////////////
// Testing scripts
///////////////////////////////////////////////////////////////////////////////////////////////////////////

lazy val startTestDb = taskKey[Unit]("Start test database")
startTestDb := {
  import scala.sys.process._
  "docker-compose -f docker-compose.test.yml down -v".!
  "docker-compose -f docker-compose.test.yml up -d".!
}

lazy val stopTestDb = taskKey[Unit]("Stop test database")
stopTestDb := {
  import scala.sys.process._
  "docker-compose -f docker-compose.test.yml down".!
}

Test / fork := true
Test / test := (Test / test).dependsOn(startTestDb).value

///////////////////////////////////////////////////////////////////////////////////////////////////////////
// Docker builds
///////////////////////////////////////////////////////////////////////////////////////////////////////////

lazy val stagingBuild = (project in file("build/staging"))
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .settings(
    name            := "rockthejvm-jobsboard-staging",
    scalaVersion    := scala3Version,
    organization    := rockthejvm,
    dockerBaseImage := "openjdk:11-jre-slim-buster",
    dockerExposedPorts ++= Seq(4041),
    Compile / mainClass         := Some("com.rockthejvm.jobsboard.Application"),
    Compile / resourceDirectory := ((server / Compile / resourceDirectory).value / "staging")
  )
  .dependsOn(server)

lazy val prodBuild = (project in file("build/prod"))
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .settings(
    name            := "rockthejvm-jobsboard-prod",
    scalaVersion    := scala3Version,
    organization    := rockthejvm,
    dockerBaseImage := "openjdk:11-jre-slim-buster",
    dockerExposedPorts ++= Seq(4041),
    Compile / mainClass         := Some("com.rockthejvm.jobsboard.Application"),
    Compile / resourceDirectory := ((server / Compile / resourceDirectory).value / "prod"),
    assembly / mainClass        := Some("com.rockthejvm.jobsboard.Application"),
    assembly / assemblyJarName  := "server.jar",
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", "MANIFEST.MF") =>
        MergeStrategy.discard
      case PathList("META-INF", xs @ _*) =>
        MergeStrategy.discard
      case x =>
        MergeStrategy.first
    }
  )
  .dependsOn(server)
