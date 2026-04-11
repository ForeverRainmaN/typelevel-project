package com.rockthejvm.jobsboard.http.routes

import cats.effect.kernel.Concurrent
import cats.implicits.*
import com.rockthejvm.jobsboard.algebra.*
import com.rockthejvm.jobsboard.domain.Job.*
import com.rockthejvm.jobsboard.domain.pagination.Pagination
import com.rockthejvm.jobsboard.domain.security.*
import com.rockthejvm.jobsboard.domain.user.*
import com.rockthejvm.jobsboard.http.responses.FailureResponse
import com.rockthejvm.jobsboard.http.validation.Validators
import com.rockthejvm.jobsboard.http.validation.syntax.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.OptionalQueryParamDecoderMatcher
import org.http4s.server.Router
import org.typelevel.log4cats.Logger
import tsec.authentication.SecuredRequestHandler
import tsec.authentication.asAuthed

import java.util.UUID
import scala.language.implicitConversions

class JobRoutes[F[_]: Concurrent: Logger: SecuredHandler] private (jobs: Jobs[F])
    extends HttpValidationDSL[F] {

  object OffsetQueryParam extends OptionalQueryParamDecoderMatcher[Int]("offset")
  object LimitQueryParam  extends OptionalQueryParamDecoderMatcher[Int]("limit")

  private val allJobsRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ GET -> Root :? LimitQueryParam(limit) +& OffsetQueryParam(offset) =>
      for {
        filter   <- req.as[JobFilter]
        jobsList <- jobs.all(filter, Pagination(limit, offset))
        resp     <- Ok(jobsList)
      } yield resp
  }

  private val findJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / UUIDVar(id) =>
    jobs.find(id).flatMap {
      case Some(job) => Ok(job)
      case None      => NotFound(FailureResponse(s"Job $id not found"))
    }
  }

  private val createJobRoute: AuthRoute[F] = { case req @ POST -> Root / "create" asAuthed user =>
    req.request.validate[JobInfo] { jobInfo =>
      for {
        jobId <- jobs.create(user.email, jobInfo)
        resp  <- Created(jobId)
      } yield resp
    }
  }

  private val updateJobRoute: AuthRoute[F] = { case req @ PUT -> Root / UUIDVar(id) asAuthed user =>
    req.request.validate[JobInfo] { jobInfo =>
      jobs.find(id).flatMap {
        case None =>
          NotFound(FailureResponse(s"Cannot update job $id not found"))
        case Some(job) if (user.owns(job) || user.isAdmin) =>
          jobs.update(id, jobInfo) *> Ok()
        case _ => Forbidden(FailureResponse("You can only update your own jobs"))
      }
    }
  }

  private val deleteJobRoute: AuthRoute[F] = { case DELETE -> Root / UUIDVar(id) asAuthed user =>
    jobs.find(id).flatMap {
      case None => NotFound(FailureResponse(s"Cannot delete job $id not found"))
      case Some(job) if (user.owns(job) || user.isAdmin) =>
        for {
          _    <- jobs.delete(id)
          resp <- Ok()
        } yield resp
      case _ => Forbidden(FailureResponse("You can only delete your own jobs"))
    }
  }

  val authedRoutes = SecuredHandler[F].liftService(
    createJobRoute.restrictedTo(allRoles) |+|
      deleteJobRoute.restrictedTo(allRoles) |+|
      updateJobRoute.restrictedTo(allRoles)
  )

  val unauthedRoutes = (allJobsRoute <+> findJobRoute)

  val routes: HttpRoutes[F] = Router(
    "/jobs" -> (unauthedRoutes <+> authedRoutes)
  )
}

object JobRoutes {
  def apply[F[_]: Concurrent: Logger: SecuredHandler](jobs: Jobs[F]) =
    new JobRoutes[F](jobs)
}
