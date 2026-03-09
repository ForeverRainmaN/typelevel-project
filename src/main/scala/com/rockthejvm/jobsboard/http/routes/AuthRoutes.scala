package com.rockthejvm.jobsboard.http.routes

import cats.effect.*
import cats.implicits.*
import com.rockthejvm.jobsboard.algebra.*
import com.rockthejvm.jobsboard.http.validation.syntax.*
import org.http4s.HttpRoutes
import org.http4s.server.Router
import org.typelevel.log4cats.Logger

class AuthRoutes[F[_]: Concurrent: Logger] private (auth: Auth[F]) extends HttpValidationDSL[F] {

  // POST /auth/login
  private val loginRoute: HttpRoutes[F] = HttpRoutes.of[F] { case POST -> Root / "login" =>
    Ok("TODO")
  }

  private val createUserRoute: HttpRoutes[F] = HttpRoutes.of[F] { case POST -> Root / "users" =>
    Ok("TODO")
  }

  private val changePasswordRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case PUT -> Root / "users" / "password" =>
      Ok("TODO")
  }

  private val logoutRoute: HttpRoutes[F] = HttpRoutes.of[F] { case POST -> Root / "logout" =>
    Ok("TODO")
  }

  val routes = Router(
    "/auth" -> (loginRoute <+> createUserRoute <+> changePasswordRoute <+> logoutRoute)
  )
}

object AuthRoutes {
  def apply[F[_]: Concurrent: Logger](auth: Auth[F]) =
    new AuthRoutes[F](auth)
}
