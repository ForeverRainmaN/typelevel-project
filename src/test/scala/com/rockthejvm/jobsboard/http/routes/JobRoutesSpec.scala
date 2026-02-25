package com.rockthejvm.jobsboard.http.routes

import cats.effect.*
import cats.implicits.*
import cats.effect.testing.scalatest.AsyncIOSpec
import io.circe.generic.auto.*
import io.circe.syntax.*

import com.rockthejvm.jobsboard.algebra.*
import com.rockthejvm.jobsboard.domain.Job.*
import com.rockthejvm.jobsboard.fixtures.JobFixture
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.implicits.*
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

import java.util.UUID
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.Logger

class JobRoutesSpec
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with Http4sDsl[IO]
    with JobFixture {

  val jobs: Jobs[IO] = new Jobs[IO] {

    override def create(ownerEmail: String, jobInfo: JobInfo): IO[UUID] =
      IO.pure(NewJobUuid)

    override def find(id: UUID): IO[Option[Job]] =
      if (id == AwesomeJobUuid) IO.pure(Some(AwesomeJob))
      else IO.pure(None)

    override def update(id: UUID, jbInfo: JobInfo): IO[Option[Job]] =
      if (id == AwesomeJobUuid) IO.pure(Some(UpdatedAwesomeJob))
      else IO.pure(None)

    override def delete(id: UUID): IO[Int] =
      if (id == AwesomeJobUuid) IO.pure(1)
      else IO.pure(0)

    override def all(): IO[List[Job]] =
      IO.pure(List(AwesomeJob))
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
        retrieved shouldBe AwesomeJob
      }
    }
  }
}
