package com.rockthejvm.jobsboard

import cats.*
import cats.effect.{IO, IOApp}
import cats.implicits.*
import com.rockthejvm.jobsboard.config.*
import com.rockthejvm.jobsboard.config.syntax.*
import com.rockthejvm.jobsboard.http.HttpApi
import org.http4s.ember.server.EmberServerBuilder
import pureconfig.ConfigSource

object Application extends IOApp.Simple {

  override def run: IO[Unit] =
    ConfigSource.default.loadF[IO, EmberConfig].flatMap { config =>
      EmberServerBuilder
        .default[IO]
        .withHost(config.host)
        .withPort(config.port)
        .withHttpApp(HttpApi[IO].endpoints.orNotFound)
        .build
        .use(_ => IO.println("Rock the JVM!") >> IO.never)
    }
}
