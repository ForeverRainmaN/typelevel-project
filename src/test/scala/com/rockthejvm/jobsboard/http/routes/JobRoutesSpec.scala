package com.rockthejvm.jobsboard.http.routes

import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.implicits.*
import com.rockthejvm.jobsboard.algebra.*
import com.rockthejvm.jobsboard.domain.Job.*
import com.rockthejvm.jobsboard.domain.pagination.*
import com.rockthejvm.jobsboard.fixtures.JobFixture
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.*
import org.http4s.implicits.*
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.util.UUID

class JobRoutesSpec
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with Http4sDsl[IO]
    with JobFixture {

  val jobs: Jobs[IO] = new Jobs[IO] {
    override def create(ownerEmail: String, jobInfo: JobInfo): IO[UUID] =
      IO.pure(newJobUuid)

    override def find(id: UUID): IO[Option[Job]] =
      if (id == awesomeJobUuid) IO.pure(Some(awesomeJob))
      else IO.pure(None)

    override def update(id: UUID, jbInfo: JobInfo): IO[Option[Job]] =
      if (id == awesomeJobUuid) IO.pure(Some(updatedAwesomeJob))
      else IO.pure(None)

    override def delete(id: UUID): IO[Int] =
      if (id == awesomeJobUuid) IO.pure(1)
      else IO.pure(0)

    override def all(): IO[List[Job]] =
      IO.pure(List(awesomeJob))

    override def all(filter: JobFilter, pagination: Pagination): IO[List[Job]] =
      if (filter.remote) IO.pure(List())
      else IO.pure(List(awesomeJob))
  }

  given logger: Logger[IO]      = Slf4jLogger.getLogger[IO]
  val jobRoutes: HttpRoutes[IO] = JobRoutes[IO](jobs).routes

  "JobRoutes" - {
    "should return a job with a given id" in {
      for {
        response <- jobRoutes.orNotFound.run(
          Request(method = Method.GET, uri = uri"/jobs/843df718-ec6e-4d49-9289-f799c0f40064")
        )
        retrieved <- response.as[Job]
      } yield {
        response.status shouldBe Status.Ok
        retrieved shouldBe awesomeJob
      }
    }

    "should return all jobs" in {
      for {
        response <- jobRoutes.orNotFound
          .run(
            Request(method = Method.GET, uri = uri"/jobs")
              .withEntity(JobFilter())
          )
        retrieved <- response.as[List[Job]]
      } yield {
        response.status shouldBe Status.Ok
        retrieved shouldBe List(awesomeJob)
      }
    }

    "should return all jobs that satisfy a filter" in {
      for {
        response <- jobRoutes.orNotFound
          .run(
            Request(method = Method.GET, uri = uri"/jobs")
              .withEntity(JobFilter(remote = false))
          )
        retrieved <- response.as[List[Job]]
      } yield {
        response.status shouldBe Status.Ok
        retrieved shouldBe List(awesomeJob)
      }
    }

    "should return empty list if nothing was found while filtering" in {
      for {
        response <- jobRoutes.orNotFound
          .run(
            Request(method = Method.GET, uri = uri"/jobs")
              .withEntity(JobFilter(remote = true))
          )
        retrieved <- response.as[List[Job]]
      } yield {
        response.status shouldBe Status.Ok
        retrieved shouldBe List()
      }
    }

    "should create a new job" in {
      for {
        response <- jobRoutes.orNotFound.run(
          Request(
            method = Method.POST,
            uri = uri"/jobs/create"
          ).withEntity(awesomeJob.jobInfo)
        )
        retrieved <- response.as[UUID]
      } yield {
        response.status shouldBe Status.Created
        retrieved shouldBe newJobUuid
      }
    }

    "should only update a job that exists" in {
      for {
        responseOk <- jobRoutes.orNotFound.run(
          Request(
            method = Method.PUT,
            uri = uri"/jobs/843df718-ec6e-4d49-9289-f799c0f40064"
          ).withEntity(updatedAwesomeJob.jobInfo)
        )
        responseInvalid <- jobRoutes.orNotFound.run(
          Request(
            method = Method.PUT,
            uri = uri"/jobs/843df718-ec6e-4d49-9289-000000000000"
          ).withEntity(updatedAwesomeJob.jobInfo)
        )
      } yield {
        responseOk.status shouldBe Status.Ok
        responseInvalid.status shouldBe Status.NotFound
      }
    }
  }

  "should only delete a job that exists" in {
    for {
      responseOk <- jobRoutes.orNotFound.run(
        Request(
          method = Method.DELETE,
          uri = uri"/jobs/843df718-ec6e-4d49-9289-f799c0f40064"
        )
      )
      responseInvalid <- jobRoutes.orNotFound.run(
        Request(
          method = Method.DELETE,
          uri = uri"/jobs/843df718-ec6e-4d49-9289-000000000000"
        )
      )
    } yield {
      responseOk.status shouldBe Status.Ok
      responseInvalid.status shouldBe Status.NotFound
    }
  }
}
