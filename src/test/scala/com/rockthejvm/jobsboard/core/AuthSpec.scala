package com.rockthejvm.jobsboard.core

import cats.data.OptionT
import cats.effect.*
import com.rockthejvm.jobsboard.algebra.LiveAuth
import com.rockthejvm.jobsboard.algebra.Users
import com.rockthejvm.jobsboard.domain.Role
import com.rockthejvm.jobsboard.domain.auth.*
import com.rockthejvm.jobsboard.domain.security.*
import com.rockthejvm.jobsboard.domain.user.*
import com.rockthejvm.jobsboard.fixtures.UserFixture
import tsec.authentication.IdentityStore
import tsec.authentication.JWTAuthenticator
import tsec.mac.jca.HMACSHA256
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.BCrypt

import scala.concurrent.duration.*

class AuthSpec extends AllTestsSpec with UserFixture {

  private val mockedUsers: Users[IO] = new Users[IO] {
    override def find(email: String): IO[Option[User]] =
      if (email === adminEmail) IO.pure(Some(admin))
      else IO.pure(None)
    override def create(user: User): IO[String]       = IO.pure(adminEmail)
    override def update(user: User): IO[Option[User]] = IO.pure(Some(user))
    override def delete(email: String): IO[Boolean]   = IO.pure(true)
  }

  val mockedAuthenticator: Authenticator[IO] = {
    val key = HMACSHA256.unsafeGenerateKey
    val idStore: IdentityStore[IO, String, User] = (email: String) =>
      if (email === adminEmail) OptionT.pure(admin)
      else if (email === recruiterEmail) OptionT.pure(recruiter)
      else OptionT.none[IO, User]

    JWTAuthenticator.unbacked.inBearerToken(
      1.day,   // expiration of tokens
      None,    // max idle time (optional)
      idStore, // identity store
      key      // hash key
    )
  }

  "Auth 'algebra'" - {
    "login should return NONE if the user does not exist" in {
      val program = for {
        auth       <- LiveAuth[IO](mockedUsers, mockedAuthenticator)
        maybeToken <- auth.login("user@somewhere.com", "password")
      } yield maybeToken

      program.asserting(_ shouldBe None)
    }

    "login should return NONE if the user exists but the password is wrong" in {
      val program = for {
        auth       <- LiveAuth[IO](mockedUsers, mockedAuthenticator)
        maybeToken <- auth.login(adminEmail, "wrongpassword")
      } yield maybeToken

      program.asserting(_ shouldBe None)
    }

    "login should return a token if the user exists and the password is correct" in {
      val program = for {
        auth       <- LiveAuth[IO](mockedUsers, mockedAuthenticator)
        maybeToken <- auth.login(adminEmail, adminRawPassword)
      } yield maybeToken

      program.asserting(_ shouldBe defined)
    }

    "signing up should not create a user with an existing email" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedAuthenticator)
        maybeUser <- auth.signUp(
          NewUserInfo(
            adminEmail,
            "somePassword",
            Some("someFirstName"),
            Some("someLastName"),
            Some("otherCompany")
          )
        )
      } yield maybeUser

      program.asserting(_ shouldBe None)
    }

    "signing up should create a new user" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedAuthenticator)
        maybeUser <- auth.signUp(
          NewUserInfo(
            "newEmail@somewhere.com",
            "somePassword",
            Some("Bob"),
            Some("Jones"),
            Some("Company")
          )
        )
      } yield maybeUser

      program.asserting {
        case Some(user) =>
          user.email shouldBe "newEmail@somewhere.com"
          user.firstName shouldBe Some("Bob")
          user.lastName shouldBe Some("Jones")
          user.company shouldBe Some("Company")
          user.role shouldBe Role.RECRUITER
        case _ => fail()
      }
    }

    "change password should return Right(None) if the user doesn't exist" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedAuthenticator)
        result <- auth.changePassword(
          "alice@somewhere.com",
          NewPasswordInfo("oldPassword", "newPassword")
        )
      } yield result

      program.asserting(_ shouldBe Right(None))
    }

    "change password should return Left with an error if the password is incorrect" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedAuthenticator)
        result <- auth.changePassword(
          adminEmail,
          NewPasswordInfo("oldPw", "newPw")
        )
      } yield result

      program.asserting(_ shouldBe Left("Invalid password"))
    }

    "change password should change the password if all details are correct" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedAuthenticator)
        result <- auth.changePassword(
          adminEmail,
          NewPasswordInfo(adminRawPassword, "newAdminPassword")
        )
        isCorrectPassword <- result match {
          case Right(Some(user)) =>
            BCrypt.checkpwBool[IO]("newAdminPassword", PasswordHash(user.hashedPassword))
          case _ => IO.pure(false)
        }
      } yield isCorrectPassword

      program.asserting(_ shouldBe true)
    }
  }
}
