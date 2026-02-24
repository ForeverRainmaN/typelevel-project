ThisBuild / version      := "1.0.1"
ThisBuild / scalaVersion := "3.4.0"
ThisBuild / organization := "com.rockthejvm"

val catsEffectVersion          = "3.5.4"
val http4sVersion              = "0.23.27"
val doobieVersion              = "1.0.0-RC5"
val circeVersion               = "0.14.8" // понижена версия
val pureConfigVersion          = "0.17.7"
val log4catsVersion            = "2.7.0"
val tsecVersion                = "0.5.0"
val scalaTestVersion           = "3.2.19"
val scalaTestCatsEffectVersion = "1.5.0"
val testContainerVersion       = "1.20.4"
val logbackVersion             = "1.5.6"
val slf4jVersion               = "2.0.16"
val javaMailVersion            = "1.6.2"
val stripeVersion              = "24.3.0"

lazy val root = (project in file("."))
  .settings(
    name := "typelevel-project",
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

      // JSON (без circe-fs2, т.к. версия 0.14.8 уже есть, а fs2 может быть не нужен)
      "io.circe" %% "circe-core"    % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser"  % circeVersion,

      // Database
      "org.tpolecat" %% "doobie-core"     % doobieVersion,
      "org.tpolecat" %% "doobie-hikari"   % doobieVersion,
      "org.tpolecat" %% "doobie-postgres" % doobieVersion,

      // Configuration
      "com.github.pureconfig" %% "pureconfig-core" % pureConfigVersion,

      // Logging
      "org.typelevel"      %% "log4cats-slf4j" % log4catsVersion,
      "org.slf4j"           % "slf4j-simple"   % slf4jVersion,
      "io.github.jmcardon" %% "tsec-http4s"    % tsecVersion,

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
      "ch.qos.logback"     % "logback-classic"               % logbackVersion             % Test,
      "org.tpolecat"      %% "doobie-scalatest"              % doobieVersion              % Test
    ),
    Compile / mainClass := Some("com.rockthejvm.jobsboard.Application")
  )
