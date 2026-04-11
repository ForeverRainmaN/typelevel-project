package com.rockthejvm.jobsboard.core

import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec
import com.rockthejvm.jobsboard.algebra.LiveTokens
import com.rockthejvm.jobsboard.config.*
import com.rockthejvm.jobsboard.config.PostgresTestConfig
import com.rockthejvm.jobsboard.config.syntax.*
import com.rockthejvm.jobsboard.domain.job.*
import com.rockthejvm.jobsboard.domain.pagination.*
import com.rockthejvm.jobsboard.fixtures.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.*

class TokensSpec extends AllTestsSpec with UserFixture {
  "Tokens 'algebra'" - {
    "should not create a new token for a non-existing user" in {
      withTransactor(config) { xa =>
        for {
          _      <- truncateTable(xa)("recoverytokens")
          tokens <- LiveTokens[IO](mockedUsers)(xa, TokenConfig(10000000L))
          token  <- tokens.getToken("somebody@someemail.com")
        } yield token
      }.asserting(_ shouldBe None)
    }

    "should create a token for an existing user" in {
      withTransactor(config) { xa =>
        for {
          _      <- truncateTable(xa)("recoverytokens")
          tokens <- LiveTokens[IO](mockedUsers)(xa, TokenConfig(10000000L))
          token  <- tokens.getToken(adminEmail)
        } yield token
      }.asserting(_ shouldBe defined)
    }

    "should not validate expired tokens" in {
      withTransactor(config) { xa =>
        for {
          _          <- truncateTable(xa)("recoverytokens")
          tokens     <- LiveTokens[IO](mockedUsers)(xa, TokenConfig(100L))
          maybeToken <- tokens.getToken(adminEmail)
          _          <- IO.sleep(500.millis)
          isTokenValid <- maybeToken match {
            case Some(token) => tokens.checkToken(adminEmail, token)
            case None        => IO.pure(false)
          }
        } yield isTokenValid
      }.asserting(_ shouldBe false)
    }

    "should validate tokens that have not expired yet" in {
      withTransactor(config) { xa =>
        for {
          _          <- truncateTable(xa)("recoverytokens")
          tokens     <- LiveTokens[IO](mockedUsers)(xa, TokenConfig(10000000L))
          maybeToken <- tokens.getToken(adminEmail)
          isTokenValid <- maybeToken match {
            case Some(token) => tokens.checkToken(adminEmail, token)
            case None        => IO.pure(false)
          }
        } yield isTokenValid
      }.asserting(_ shouldBe true)
    }

    "should only validate tokens for the user that generated them" in {
      withTransactor(config) { xa =>
        for {
          _          <- truncateTable(xa)("recoverytokens")
          tokens     <- LiveTokens[IO](mockedUsers)(xa, TokenConfig(10000000L))
          maybeToken <- tokens.getToken(adminEmail)
          isAdminTokenValid <- maybeToken match {
            case Some(token) => tokens.checkToken(adminEmail, token)
            case None        => IO.pure(false)
          }
          isOtherTokenValid <- maybeToken match {
            case Some(token) => tokens.checkToken("someoneelse@gmail.com", token)
            case None        => IO.pure(false)
          }
        } yield (isAdminTokenValid, isOtherTokenValid)
      }.asserting { case (isAdminTokenValid, isOtherTokenValid) =>
        isAdminTokenValid shouldBe true
        isOtherTokenValid shouldBe false
      }
    }
  }
}
