package com.rockthejvm.jobsboard.core

import cats.data.OptionT
import cats.effect.*
import com.rockthejvm.jobsboard.algebra.Emails
import com.rockthejvm.jobsboard.algebra.LiveAuth
import com.rockthejvm.jobsboard.algebra.Tokens
import com.rockthejvm.jobsboard.algebra.Users
import com.rockthejvm.jobsboard.config.SecurityConfig
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
import com.rockthejvm.jobsboard.algebra.LiveUsers

class AuthSpec extends AllTestsSpec with UserFixture {

  private val mockedConfig = SecurityConfig("secret", 1.day)

  val mockedTokens: Tokens[IO] = new Tokens[IO] {
    override def checkToken(email: String, token: String): IO[Boolean] = IO.pure(token == "abc123")
    override def getToken(email: String): IO[Option[String]] =
      if (email == adminEmail) IO.pure(Some("abc123"))
      else IO.pure(None)
  }

  val mockedEmails: Emails[IO] = new Emails[IO] {
    override def sendEmail(to: String, subject: String, content: String): IO[Unit] = IO.unit
    override def sendPasswordRecoveryEmail(to: String, token: String): IO[Unit]    = IO.unit
  }

  def probedEmails(users: Ref[IO, Set[String]]): Emails[IO] = new Emails[IO] {
    override def sendEmail(to: String, subject: String, content: String): IO[Unit] =
      users.modify(set => (set + to, ()))
    override def sendPasswordRecoveryEmail(to: String, token: String): IO[Unit] =
      sendEmail(to, "your token", "token")
  }

  "Auth 'algebra'" - {
    "login should return NONE if the user does not exist" in {
      val program = for {
        auth       <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        maybeToken <- auth.login("user@somewhere.com", "password")
      } yield maybeToken

      program.asserting(_ shouldBe None)
    }

    "login should return NONE if the user exists but the password is wrong" in {
      val program = for {
        auth       <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        maybeToken <- auth.login(adminEmail, "wrongpassword")
      } yield maybeToken

      program.asserting(_ shouldBe None)
    }

    "login should return a token if the user exists and the password is correct" in {
      val program = for {
        auth       <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        maybeToken <- auth.login(adminEmail, adminRawPassword)
      } yield maybeToken

      program.asserting(_ shouldBe defined)
    }

    "signing up should not create a user with an existing email" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
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
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
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
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        result <- auth.changePassword(
          "alice@somewhere.com",
          NewPasswordInfo("oldPassword", "newPassword")
        )
      } yield result

      program.asserting(_ shouldBe Right(None))
    }

    "change password should return Left with an error if the password is incorrect" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        result <- auth.changePassword(
          adminEmail,
          NewPasswordInfo("oldPw", "newPw")
        )
      } yield result

      program.asserting(_ shouldBe Left("Invalid password"))
    }

    "change password should change the password if all details are correct" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
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

    "recoverPassword should fail for a user that does not exist, even if the token is correct" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        result1 <- auth.recoverPasswordFromToken(
          "someone@gmail.com",
          "abc123",
          "igotya"
        )
        result2 <- auth.recoverPasswordFromToken(
          "someone@gmail.com",
          "wrongtoken",
          "igotya"
        )
      } yield (result1, result2)

      program.asserting(_ shouldBe (false, false))
    }

    "recoverPassword should fail for a user that does exist, but the token is wrong" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        result <- auth.recoverPasswordFromToken(
          adminEmail,
          "wrongtoken",
          "hacked"
        )
      } yield result

      program.asserting(_ shouldBe false)
    }

    "recoverPassword should succeed for a correct combination of user/token" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        result <- auth.recoverPasswordFromToken(
          adminEmail,
          "abc123",
          "rockstar"
        )
      } yield result

      program.asserting(_ shouldBe true)
    }

    "sending recovery passwords should fail for a user that does not exist" in {
      val program = for {
        set                  <- Ref.of[IO, Set[String]](Set())
        emails               <- IO(probedEmails(set))
        auth                 <- LiveAuth[IO](mockedUsers, mockedTokens, emails)
        result               <- auth.sendPasswordRecoveryToken("someone@whatever.com")
        usersBeingSentEmails <- set.get
      } yield usersBeingSentEmails

      program.asserting(_ shouldBe empty)
    }

    "sending recovery passwords succeed for a user that exists" in {
      val program = for {
        set                  <- Ref.of[IO, Set[String]](Set())
        emails               <- IO(probedEmails(set))
        auth                 <- LiveAuth[IO](mockedUsers, mockedTokens, emails)
        result               <- auth.sendPasswordRecoveryToken(adminEmail)
        usersBeingSentEmails <- set.get
      } yield usersBeingSentEmails

      program.asserting(_ should contain(adminEmail))
    }
  }
}
