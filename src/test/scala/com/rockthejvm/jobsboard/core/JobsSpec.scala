package com.rockthejvm.jobsboard.core

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.rockthejvm.jobsboard.algebra.LiveJobs
import com.rockthejvm.jobsboard.config.PostgresTestConfig
import com.rockthejvm.jobsboard.config.syntax.*
import com.rockthejvm.jobsboard.fixtures.JobFixture
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import pureconfig.ConfigSource

class JobsSpec
    extends AsyncFreeSpec
    with DoobieSpec
    with AsyncIOSpec
    with Matchers
    with JobFixture {

  "Jobs 'algebra'" - {
    "should return no job if the given UUID does not exist" in {
      val config =
        ConfigSource.resources("test.conf").at("test-database").loadF[IO, PostgresTestConfig]

      config
        .flatMap { config =>
          createTransactor(config).use { xa =>
            LiveJobs[IO](xa).flatMap(_.find(NotFoundJobUuid))
          }
        }
        .asserting(_ shouldBe None)
    }
  }
}
