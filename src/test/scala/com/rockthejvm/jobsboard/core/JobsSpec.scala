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

class JobsSpec extends AllTestsSpec with JobFixture {

  "Jobs 'algebra'" - {
    "should return no job if the given UUID does not exist" in {
      withTransactor(config) { xa =>
        for {
          _      <- truncateTable(xa)("jobs")
          result <- LiveJobs[IO](xa).flatMap(_.find(notFoundJobUuid))
        } yield result
      }.asserting(_ shouldBe None)
    }

    "should retrieve a job by id" in {
      withTransactor(config) { xa =>
        for {
          _    <- truncateTable(xa)("jobs")
          jobs <- LiveJobs[IO](xa)
          id   <- jobs.create("daniel@rockthejvm.com", awesomeJob.jobInfo)
          job  <- jobs.find(id)
        } yield job
      }.asserting(_.map(_.jobInfo) shouldBe Some(awesomeJob.jobInfo))
    }

    "should retrieve all jobs" in {
      withTransactor(config) { xa =>
        for {
          _    <- truncateTable(xa)("jobs")
          jobs <- LiveJobs[IO](xa)
          id   <- jobs.create("daniel@rockthejvm.com", awesomeJob.jobInfo)
          all  <- jobs.all()
        } yield (all, id)
      }.asserting { case (all, id) =>
        all.map(_.id) should contain(id)
        all.head.jobInfo shouldBe awesomeJob.jobInfo
      }
    }

    "should create a new job" in {
      withTransactor(config) { xa =>
        for {
          _        <- truncateTable(xa)("jobs")
          jobs     <- LiveJobs[IO](xa)
          jobId    <- jobs.create("daniel@rockthejvm.com", rockTheJvmNewJob)
          maybeJob <- jobs.find(jobId)
        } yield maybeJob
      }.asserting(_.map(_.jobInfo) shouldBe Some(rockTheJvmNewJob))
    }

    "should return an updated job if it exists" in {
      withTransactor(config) { xa =>
        for {
          _               <- truncateTable(xa)("jobs")
          jobs            <- LiveJobs[IO](xa)
          id              <- jobs.create("daniel@rockthejvm.com", awesomeJob.jobInfo)
          maybeUpdatedJob <- jobs.update(id, updatedAwesomeJob.jobInfo)
        } yield maybeUpdatedJob
      }.asserting { maybeJob =>
        maybeJob shouldBe defined
        maybeJob.get.jobInfo shouldBe updatedAwesomeJob.jobInfo
      }
    }

    "should return none when trying to update a job that does not exist" in {
      withTransactor(config) { xa =>
        for {
          _   <- truncateTable(xa)("jobs")
          res <- LiveJobs[IO](xa).flatMap(_.update(notFoundJobUuid, updatedAwesomeJob.jobInfo))
        } yield res
      }.asserting(_ shouldBe None)
    }

    "should delete an existing job" in {
      withTransactor(config) { xa =>
        for {
          _            <- truncateTable(xa)("jobs")
          jobs         <- LiveJobs[IO](xa)
          id           <- jobs.create("daniel@rockthejvm.com", awesomeJob.jobInfo)
          deletedCount <- jobs.delete(id)
          countAfter <- sql"SELECT COUNT(*) FROM jobs WHERE id = $id".query[Int].unique.transact(xa)
        } yield (deletedCount, countAfter)
      }.asserting { case (deletedCount, countAfter) =>
        deletedCount shouldBe 1
        countAfter shouldBe 0
      }
    }

    "should return zero updated rows if the job ID to delete is not found" in {
      withTransactor(config) { xa =>
        for {
          _   <- truncateTable(xa)("jobs")
          res <- LiveJobs[IO](xa).flatMap(_.delete(notFoundJobUuid))
        } yield res
      }.asserting(_ shouldBe 0)
    }

    "should filter remote jobs" in {
      withTransactor(config) { xa =>
        for {
          _    <- truncateTable(xa)("jobs")
          jobs <- LiveJobs[IO](xa)
          _    <- jobs.create("remote@test.com", rockTheJvmNewJob.copy(remote = true))
          _    <- jobs.create("office@test.com", awesomeJob.jobInfo.copy(remote = false))
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
      withTransactor(config) { xa =>
        for {
          _        <- truncateTable(xa)("jobs")
          jobs     <- LiveJobs[IO](xa)
          _        <- jobs.create("daniel@rockthejvm.com", awesomeJob.jobInfo)
          filtered <- jobs.all(JobFilter(tags = List("scala", "cats", "zio")), Pagination.default)
        } yield filtered
      }.flatMap { result =>
        IO(assert(result.nonEmpty)) *>
          IO(assert(result.head.jobInfo == awesomeJob.jobInfo))
      }
    }
  }
}
