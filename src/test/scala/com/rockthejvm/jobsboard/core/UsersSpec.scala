package com.rockthejvm.jobsboard.core

import cats.effect.IO
import com.rockthejvm.jobsboard.algebra.LiveUsers
import com.rockthejvm.jobsboard.config.PostgresTestConfig
import com.rockthejvm.jobsboard.config.syntax.*
import com.rockthejvm.jobsboard.domain.user.User
import com.rockthejvm.jobsboard.fixtures.UserFixture
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import org.postgresql.util.PSQLException
import org.scalatest.Inside

class UsersSpec extends AllTestsSpec with Inside with UserFixture {

  "User's 'algebra'" - {
    "should create a user" in {
      withTransactor(config) { xa =>
        for {
          _                <- truncateTable(xa)("users")
          users            <- LiveUsers[IO](xa)
          createdUserEmail <- users.create(admin)
          maybeUser <- sql"SELECT * FROM users WHERE email = $createdUserEmail"
            .query[User]
            .option
            .transact(xa)
        } yield (createdUserEmail, maybeUser)
      }.asserting { case (email, maybeUser) =>
        email shouldBe adminEmail
        maybeUser shouldBe Some(admin)
      }
    }

    "should retrieve a user by email" in {
      withTransactor(config) { xa =>
        for {
          _                <- truncateTable(xa)("users")
          users            <- LiveUsers[IO](xa)
          createdUserEmail <- users.create(admin)
          maybeUser        <- users.find(createdUserEmail)
        } yield maybeUser
      }.asserting { _ shouldBe Some(admin) }
    }

    "should return None if the email doesn't exist" in {
      withTransactor(config) { xa =>
        for {
          _         <- truncateTable(xa)("users")
          users     <- LiveUsers[IO](xa)
          maybeUser <- users.find("someRandomEmail@random.com")
        } yield maybeUser
      }.asserting { _ shouldBe None }
    }

    "should fail creating a new user if the email already exists" in {
      withTransactor(config) { xa =>
        for {
          _       <- truncateTable(xa)("users")
          users   <- LiveUsers[IO](xa)
          _       <- users.create(recruiter)
          outcome <- users.create(recruiter).attempt
        } yield outcome
      }.asserting { result =>
        inside(result) {
          case Left(e) =>
            e shouldBe a[PSQLException]
            e.getMessage should include("duplicate key value")
          case _ => fail()
        }
      }
    }

    "should return None when updating a user that does not exist" in {
      withTransactor(config) { xa =>
        for {
          _         <- truncateTable(xa)("users")
          users     <- LiveUsers[IO](xa)
          maybeUser <- users.update(admin)
        } yield maybeUser
      }.asserting(_ shouldBe None)
    }

    "should update an existing user" in {
      withTransactor(config) { xa =>
        for {
          _         <- truncateTable(xa)("users")
          users     <- LiveUsers[IO](xa)
          userEmail <- users.create(admin)
          maybeUser <- users.update(updatedAdmin)
        } yield maybeUser
      }.asserting(_ shouldBe Some(updatedAdmin))
    }

    "should delete a user" in {
      withTransactor(config) { xa =>
        for {
          _         <- truncateTable(xa)("users")
          users     <- LiveUsers[IO](xa)
          _         <- users.create(admin)
          isDeleted <- users.delete(admin.email)
          maybeUser <- sql"SELECT * FROM users WHERE email = ${admin.email}"
            .query[User]
            .option
            .transact(xa)
        } yield (isDeleted, maybeUser)
      }.asserting { case (isDeleted, maybeUser) =>
        isDeleted shouldBe true
        maybeUser shouldBe None
      }
    }

    "should NOT delete a user that does not exist" in {
      withTransactor(config) { xa =>
        for {
          _         <- truncateTable(xa)("users")
          users     <- LiveUsers[IO](xa)
          isDeleted <- users.delete(adminEmail)
        } yield isDeleted
      }.asserting(_ shouldBe false)
    }
  }
}
