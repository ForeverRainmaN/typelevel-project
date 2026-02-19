package com.rockthejvm.jobsboard.http.routes

import cats.Monad
import cats.implicits.*
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

class JobRoutes[F[_]: Monad] private extends Http4sDsl[F] {
  // post / jobs?offset=x&limit=y { filters } // TODO add query params and filters
  private val allJobsRoute: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root =>
    Ok("TODO")
  }

  private val findJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / UUIDVar(id) =>
    Ok(s"TODO find job at $id")
  }

  private val createJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case POST -> Root / "create" =>
    Ok("TODO")
  }

  private val updateJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case PUT -> Root / UUIDVar(id) =>
    Ok(s"TODO update job for $id")
  }

  private val deleteJoRoute: HttpRoutes[F] = HttpRoutes.of[F] { case DELETE -> Root / UUIDVar(id) =>
    Ok(s"TODO delete job for $id")
  }

  val routes: HttpRoutes[F] = Router(
    "/jobs" -> (allJobsRoute <+> findJobRoute <+> createJobRoute <+> updateJobRoute <+> deleteJoRoute)
  )
}

object JobRoutes {
  def apply[F[_]: Monad] = new JobRoutes[F]
}
