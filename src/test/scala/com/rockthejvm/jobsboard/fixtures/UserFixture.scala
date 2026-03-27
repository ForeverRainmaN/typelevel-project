package com.rockthejvm.jobsboard.fixtures

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.rockthejvm.jobsboard.domain.Role
import com.rockthejvm.jobsboard.domain.user
import tsec.passwordhashers.jca.BCrypt

import user.*

trait UserFixture {
  val adminEmail     = "admin@something.com"
  val recruiterEmail = "recruiter@something.com"
  val invalidEmail   = "myInvalidEmail.com"

  val adminRawPassword     = "password"
  val recruiterRawPassword = "recruiterPass"
  val invalidRawPassword   = "invalidPass"
  val newAdminPassword     = "newAdminPassword"

  val adminHashedPassword: String =
    BCrypt.hashpw[IO](adminRawPassword).unsafeRunSync()

  val recruiterHashedPassword: String =
    BCrypt.hashpw[IO](recruiterRawPassword).unsafeRunSync()

  val invalidHashedPassword: String =
    BCrypt.hashpw[IO](invalidRawPassword).unsafeRunSync()

  val updatedHashedPassword: String =
    BCrypt.hashpw[IO](invalidRawPassword).unsafeRunSync()

  val admin = User(
    email = adminEmail,
    hashedPassword = adminHashedPassword,
    firstName = Some("firstName"),
    lastName = Some("lastName"),
    company = Some("someCompany"),
    role = Role.ADMIN
  )

  val recruiter = User(
    email = recruiterEmail,
    hashedPassword = recruiterHashedPassword,
    firstName = Some("firstName2"),
    lastName = Some("lastName2"),
    company = Some("someOtherCompany"),
    role = Role.RECRUITER
  )

  val updatedAdmin = User(
    email = adminEmail,
    hashedPassword = updatedHashedPassword,
    firstName = Some("Megan"),
    lastName = Some("Fox"),
    company = Some("Transformers"),
    role = Role.ADMIN
  )

  val newUserAdmin = User(
    adminEmail,
    adminHashedPassword,
    Some("admin1"),
    Some("admin2"),
    Some("admincompany"),
    Role.ADMIN
  )

  val newUserRecruiter = User(
    recruiterEmail,
    recruiterHashedPassword,
    Some("recruiter1"),
    Some("recruiter2"),
    Some("recruitercompany"),
    Role.RECRUITER
  )
}
