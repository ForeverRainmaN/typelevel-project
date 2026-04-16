package com.rockthejvm.jobsboard.core

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.rockthejvm.jobsboard.config.PostgresTestConfig
import com.rockthejvm.jobsboard.config.syntax.loadF
import com.rockthejvm.jobsboard.fixtures.UserFixture
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig.ConfigSource

trait AllTestsSpec extends AsyncFreeSpec with DoobieSpec with AsyncIOSpec with Matchers {
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  val config: IO[PostgresTestConfig] =
    ConfigSource.resources("test.conf").at("test-database").loadF[IO, PostgresTestConfig]
}
