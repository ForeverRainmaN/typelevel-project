package com.rockthejvm.jobsboard.http

import cats.Monad
import cats.implicits.*
import com.rockthejvm.jobsboard.http.routes.{HealthRoutes, JobRoutes}
import org.http4s.HttpRoutes
import org.http4s.server.Router

class HttpApi[F[_]: Monad] private {
  private val healthRoutes = HealthRoutes[F].routes
  private val jobRoutes    = JobRoutes[F].routes

  val endpoints: HttpRoutes[F] = Router(
    "/api" -> (healthRoutes <+> jobRoutes)
  )
}

object HttpApi {
  def apply[F[_]: Monad] = new HttpApi[F]
}
