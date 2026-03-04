package com.rockthejvm.jobsboard.core

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.rockthejvm.jobsboard.algebra.LiveJobs
import com.rockthejvm.jobsboard.config.PostgresTestConfig
import com.rockthejvm.jobsboard.config.syntax.*
import com.rockthejvm.jobsboard.domain.Job.*
import com.rockthejvm.jobsboard.domain.pagination.*
import com.rockthejvm.jobsboard.fixtures.JobFixture
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig.ConfigSource

class JobsSpec
    extends AsyncFreeSpec
    with DoobieSpec
    with AsyncIOSpec
    with Matchers
    with JobFixture {

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  val config: IO[PostgresTestConfig] =
    ConfigSource.resources("test.conf").at("test-database").loadF[IO, PostgresTestConfig]

  def withTransactor[A](test: Transactor[IO] => IO[A]): IO[A] =
    config.flatMap { c =>
      createTransactor(c).use { xa =>
        sql"TRUNCATE jobs".update.run.transact(xa) >> test(xa)
      }
    }

  "Jobs 'algebra'" - {
    "should return no job if the given UUID does not exist" in {
      withTransactor { xa =>
        LiveJobs[IO](xa).flatMap(_.find(NotFoundJobUuid))
      }.asserting(_ shouldBe None)
    }

    "should retrieve a job by id" in {
      withTransactor { xa =>
        for {
          jobs <- LiveJobs[IO](xa)
          id   <- jobs.create("daniel@rockthejvm.com", AwesomeJob.jobInfo)
          job  <- jobs.find(id)
        } yield job
      }.asserting(_.map(_.jobInfo) shouldBe Some(AwesomeJob.jobInfo))
    }

    "should retrieve all jobs" in {
      withTransactor { xa =>
        for {
          jobs <- LiveJobs[IO](xa)
          id   <- jobs.create("daniel@rockthejvm.com", AwesomeJob.jobInfo)
          all  <- jobs.all()
        } yield (all, id)
      }.asserting { case (all, id) =>
        all.map(_.id) should contain(id)
        all.head.jobInfo shouldBe AwesomeJob.jobInfo
      }
    }

    "should create a new job" in {
      withTransactor { xa =>
        for {
          jobs     <- LiveJobs[IO](xa)
          jobId    <- jobs.create("daniel@rockthejvm.com", RockTheJvmNewJob)
          maybeJob <- jobs.find(jobId)
        } yield maybeJob
      }.asserting(_.map(_.jobInfo) shouldBe Some(RockTheJvmNewJob))
    }

    "should return an updated job if it exists" in {
      withTransactor { xa =>
        for {
          jobs            <- LiveJobs[IO](xa)
          id              <- jobs.create("daniel@rockthejvm.com", AwesomeJob.jobInfo)
          maybeUpdatedJob <- jobs.update(id, UpdatedAwesomeJob.jobInfo)
        } yield maybeUpdatedJob
      }.asserting { maybeJob =>
        maybeJob shouldBe defined
        maybeJob.get.jobInfo shouldBe UpdatedAwesomeJob.jobInfo
      }
    }

    "should return none when trying to update a job that does not exist" in {
      withTransactor { xa =>
        LiveJobs[IO](xa).flatMap(_.update(NotFoundJobUuid, UpdatedAwesomeJob.jobInfo))
      }.asserting(_ shouldBe None)
    }

    "should delete an existing job" in {
      withTransactor { xa =>
        for {
          jobs         <- LiveJobs[IO](xa)
          id           <- jobs.create("daniel@rockthejvm.com", AwesomeJob.jobInfo)
          deletedCount <- jobs.delete(id)
          countAfter <- sql"SELECT COUNT(*) FROM jobs WHERE id = $id".query[Int].unique.transact(xa)
        } yield (deletedCount, countAfter)
      }.asserting { case (deletedCount, countAfter) =>
        deletedCount shouldBe 1
        countAfter shouldBe 0
      }
    }

    "should return zero updated rows if the job ID to delete is not found" in {
      withTransactor { xa =>
        LiveJobs[IO](xa).flatMap(_.delete(NotFoundJobUuid))
      }.asserting(_ shouldBe 0)
    }

    "should filter remote jobs" in {
      withTransactor { xa =>
        for {
          jobs <- LiveJobs[IO](xa)
          _    <- jobs.create("remote@test.com", RockTheJvmNewJob.copy(remote = true))
          _ <- jobs.create(
            "office@test.com",
            AwesomeJob.jobInfo.copy(remote = false)
          )
          filteredRemoteTrue  <- jobs.all(JobFilter(remote = true), Pagination.default)
          filteredRemoteFalse <- jobs.all(JobFilter(remote = false), Pagination.default)
        } yield (filteredRemoteTrue, filteredRemoteFalse)
      }.asserting { case (remoteTrue, remoteFalse) =>
        remoteTrue should have size 1
        remoteTrue.head.jobInfo.remote shouldBe true
        remoteFalse should have size 1
        remoteFalse.head.jobInfo.remote shouldBe false
      }
    }

    "should filter jobs by tags" in {
      withTransactor { xa =>
        for {
          jobs     <- LiveJobs[IO](xa)
          _        <- jobs.create("daniel@rockthejvm.com", AwesomeJob.jobInfo)
          filtered <- jobs.all(JobFilter(tags = List("scala", "cats", "zio")), Pagination.default)
        } yield filtered
      }.flatMap { result =>
        IO(assert(result.nonEmpty)) *>
          IO(assert(result.head.jobInfo == AwesomeJob.jobInfo))
      }
    }
  }
}
