package com.rockthejvm.jobsboard.http.routes

import cats.data.OptionT
import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec
import com.rockthejvm.jobsboard.algebra.Auth
import com.rockthejvm.jobsboard.domain.auth.*
import com.rockthejvm.jobsboard.domain.security.*
import com.rockthejvm.jobsboard.domain.security.Authenticator
import com.rockthejvm.jobsboard.domain.user.*
import com.rockthejvm.jobsboard.fixtures.UserFixture
import com.rockthejvm.jobsboard.fixtures.SecuredRouteFixture
import com.rockthejvm.jobsboard.http.validation.syntax.HttpValidationDSL
import io.circe.generic.auto.*
import org.http4s.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Authorization
import org.http4s.implicits.*
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.ci.CIStringSyntax
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import tsec.authentication.IdentityStore
import tsec.authentication.JWTAuthenticator
import tsec.jws.mac.JWTMac
import tsec.mac.jca.HMACSHA256

import scala.concurrent.duration.*

class AuthRoutesSpec
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with Http4sDsl[IO]
    with SecuredRouteFixture
    with UserFixture {

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  val mockedAuth: Auth[IO] = new Auth[IO] {
    override def login(email: String, password: String): IO[Option[User]] =
      if (email == adminEmail && password == adminRawPassword)
        IO(Some(admin))
      else IO.pure(None)
    override def signUp(newUserInfo: NewUserInfo): IO[Option[User]] =
      if (newUserInfo.email == recruiterEmail)
        IO.pure(Some(recruiter))
      else
        IO.pure(None)
    override def changePassword(
        email: String,
        newPasswordInfo: NewPasswordInfo
    ): IO[Either[String, Option[User]]] =
      if (email == adminEmail)
        if (newPasswordInfo.oldPassword == adminRawPassword)
          IO.pure(Right(Some(admin)))
        else
          IO.pure(Left("Invalid Password"))
      else
        IO.pure(Right(None))

    override def delete(email: String): IO[Boolean] = IO.pure(true)
  }

  val authRoutes: HttpRoutes[IO] = AuthRoutes[IO](mockedAuth)(mockedAuthenticator).routes

  "AuthRoutes" - {
    "should return a 401 - unauthorized if login fails" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/login")
            .withEntity(LoginInfo(adminEmail, "wrongpassword"))
        )
      } yield {
        response.status shouldBe Status.Unauthorized
      }
    }

    "should return a 200-OK + a JWT if login is successful" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/login")
            .withEntity(LoginInfo(adminEmail, adminRawPassword))
        )
      } yield {
        response.status shouldBe Status.Ok
        response.headers.get(ci"Authorization") shouldBe defined
      }
    }

    "should return a 400-Bad Request if the user to create already exists" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/users")
            .withEntity(newUserAdmin)
        )
      } yield {
        response.status shouldBe Status.BadRequest
      }
    }

    "should return a 201-Created if the user creation succeeds" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/users")
            .withEntity(newUserRecruiter)
        )
      } yield {
        response.status shouldBe Status.Created
      }
    }

    "should return a 200-OK if logging out with a valid JWT token" in {
      for {
        jwtToken <- mockedAuthenticator.create(adminEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/logout")
            .withBearerToken(jwtToken)
        )
      } yield {
        response.status shouldBe Status.Ok
      }
    }

    "should return a 401-Unauthorized if logging out without a valid JWT token" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/logout")
        )
      } yield {
        response.status shouldBe Status.Unauthorized
      }
    }

    "should return a 404-Not found if changing password for a user that doesn't exist" in {
      for {
        jwtToken <- mockedAuthenticator.create(recruiterEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/auth/users/password")
            .withBearerToken(jwtToken)
            .withEntity(NewPasswordInfo("somepassword", "newpassword"))
        )
      } yield {
        response.status shouldBe Status.NotFound
      }
    }

    "should return a 403-Forbidden if old password is incorrect" in {
      for {
        jwtToken <- mockedAuthenticator.create(adminEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/auth/users/password")
            .withBearerToken(jwtToken)
            .withEntity(NewPasswordInfo("wrongpassword", "newpassword"))
        )
      } yield {
        response.status shouldBe Status.Forbidden
      }
    }

    "should return a 401-Unauthorized if changing password without a JWT" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/auth/users/password")
            .withEntity(NewPasswordInfo(adminRawPassword, "newpassword"))
        )
      } yield {
        response.status shouldBe Status.Unauthorized
      }
    }

    "should return a 200-OK if changing password for a user with valid JWT and password" in {
      for {
        jwtToken <- mockedAuthenticator.create(adminEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/auth/users/password")
            .withBearerToken(jwtToken)
            .withEntity(NewPasswordInfo(adminRawPassword, "newpassword"))
        )
      } yield {
        response.status shouldBe Status.Ok
      }
    }
  }

  "should return a 401-Unauthorized if a non-admin tries to delete a user" in {
    for {
      jwtToken <- mockedAuthenticator.create(recruiterEmail)
      response <- authRoutes.orNotFound.run(
        Request(method = Method.DELETE, uri = uri"/auth/users/admin@something.com")
          .withBearerToken(jwtToken)
      )
    } yield {
      response.status shouldBe Status.Unauthorized
    }
  }

  "should return a 200-Ok if a admin tries to delete a user" in {
    for {
      jwtToken <- mockedAuthenticator.create(adminEmail)
      response <- authRoutes.orNotFound.run(
        Request(method = Method.DELETE, uri = uri"/auth/users/admin@something.com")
          .withBearerToken(jwtToken)
      )
    } yield {
      response.status shouldBe Status.Ok
    }
  }
}
