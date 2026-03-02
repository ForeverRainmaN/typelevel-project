package com.rockthejvm.jobsboard.http.routes

import cats.effect.kernel.Concurrent
import cats.implicits.*
import com.rockthejvm.jobsboard.algebra.*
import com.rockthejvm.jobsboard.domain.Job.*
import com.rockthejvm.jobsboard.http.responses.FailureResponse
import com.rockthejvm.jobsboard.http.validation.Validators.*
import com.rockthejvm.jobsboard.http.validation.syntax.*
import com.rockthejvm.jobsboard.logging.syntax.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.typelevel.log4cats.Logger

import java.util.UUID

class JobRoutes[F[_]: Concurrent: Logger] private (jobs: Jobs[F]) extends HttpValidationDSL[F] {

  private val allJobsRoute: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root =>
    for {
      jobsList <- jobs.all()
      resp     <- Ok(jobsList)
    } yield resp
  }

  private val findJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / UUIDVar(id) =>
    jobs.find(id).flatMap {
      case Some(job) => Ok(job)
      case None      => NotFound(FailureResponse(s"Job $id not found"))
    }
  }

  private val createJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "create" =>
      req.validate[JobInfo] { jobInfo =>
        for {
          jobId <- jobs.create("TODO@rockthejvm.com", jobInfo)
          resp  <- Created(jobId)
        } yield resp
      }
  }

  private val updateJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ PUT -> Root / UUIDVar(id) =>
      req.validate[JobInfo] { jobInfo =>
        for {
          maybeNewJob <- jobs.update(id, jobInfo)
          resp <- maybeNewJob match {
            case Some(job) => Ok()
            case None      => NotFound(FailureResponse(s"Cannot update job $id: not found"))
          }
        } yield resp
      }
  }

  private val deleteJoRoute: HttpRoutes[F] = HttpRoutes.of[F] { case DELETE -> Root / UUIDVar(id) =>
    jobs.find(id).flatMap {
      case Some(_) =>
        for {
          _    <- jobs.delete(id)
          resp <- Ok()
        } yield resp
      case None => NotFound(FailureResponse(s"Cannot delete job $id not found"))
    }
  }

  val routes: HttpRoutes[F] = Router(
    "/jobs" -> (allJobsRoute <+> findJobRoute <+> createJobRoute <+> updateJobRoute <+> deleteJoRoute)
  )
}

object JobRoutes {
  def apply[F[_]: Concurrent: Logger](jobs: Jobs[F]) = new JobRoutes[F](jobs)
}
