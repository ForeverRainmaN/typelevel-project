package com.rockthejvm.jobsboard.http.routes

import cats.effect.kernel.Concurrent
import cats.implicits.*
import com.rockthejvm.jobsboard.domain.Job.*
import com.rockthejvm.jobsboard.http.responses.FailureResponse
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

import java.util.UUID
import scala.collection.mutable

class JobRoutes[F[_]: Concurrent] private extends Http4sDsl[F] {
  private val database = mutable.Map[UUID, Job]()

  private val allJobsRoute: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root =>
    Ok(database.values.asJson)
  }

  private val findJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / UUIDVar(id) =>
    database.get(id) match
      case Some(job) => Ok(job)
      case None      => NotFound(FailureResponse(s"Job with $id not found."))
  }

  private def createJob(jobInfo: JobInfo): F[Job] =
    Job(
      id = UUID.randomUUID(),
      date = System.currentTimeMillis(),
      ownerEmail = "TODO@rockthejvm.com",
      jobInfo = jobInfo,
      active = true
    ).pure[F]

  private val createJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "create" =>
      for {
        jobInfo <- req.as[JobInfo]
        job     <- createJob(jobInfo)
        resp    <- Created(job.id)
      } yield resp
  }

  private val updateJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ PUT -> Root / UUIDVar(id) =>
      database.get(id) match {
        case Some(job) =>
          for {
            jobInfo <- req.as[JobInfo]
            _       <- database.put(id, job.copy(jobInfo = jobInfo)).pure[F]
            resp    <- Ok()
          } yield resp

        case None => NotFound(FailureResponse(s"Cannot update job $id: not found"))
      }
  }

  private val deleteJoRoute: HttpRoutes[F] = HttpRoutes.of[F] { 
    case DELETE -> Root / UUIDVar(id) =>
        database.get(id) match {
          case Some(job) =>
            for {
              _       <- database.remove(id).pure[F]
              resp    <- Ok()
            } yield resp

          case None => NotFound(FailureResponse(s"Cannot delete job $id: not found"))
        }
    }

  val routes: HttpRoutes[F] = Router(
    "/jobs" -> (allJobsRoute <+> findJobRoute <+> createJobRoute <+> updateJobRoute <+> deleteJoRoute)
  )
}

object JobRoutes {
  def apply[F[_]: Concurrent] = new JobRoutes[F]
}
