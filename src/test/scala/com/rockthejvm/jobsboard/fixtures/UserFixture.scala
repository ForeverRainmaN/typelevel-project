package com.rockthejvm.jobsboard.fixtures

import com.rockthejvm.jobsboard.domain.Role
import com.rockthejvm.jobsboard.domain.user
import cats.effect.unsafe.implicits.global

import user.*
import tsec.passwordhashers.jca.BCrypt
import cats.effect.IO

trait UserFixture {
  val AdminEmail     = "admin@something.com"
  val RecruiterEmail = "recruiter@something.com"
  val InvalidEmail   = "myInvalidEmail.com"

  val AdminRawPassword     = "password"
  val RecruiterRawPassword = "recruiterPass"
  val InvalidRawPassword   = "invalidPass"
  val NewAdminPassword     = "newAdminPassword"

  val AdminHashedPassword: String =
    BCrypt.hashpw[IO](AdminRawPassword).unsafeRunSync()

  val RecruiterHashedPassword: String =
    BCrypt.hashpw[IO](RecruiterRawPassword).unsafeRunSync()

  val InvalidHashedPassword: String =
    BCrypt.hashpw[IO](InvalidRawPassword).unsafeRunSync()

  val UpdatedHashedPassword: String =
    BCrypt.hashpw[IO](InvalidRawPassword).unsafeRunSync()

  val Admin = User(
    email = AdminEmail,
    hashedPassword = AdminHashedPassword,
    firstName = Some("firstName"),
    lastName = Some("lastName"),
    company = Some("someCompany"),
    role = Role.ADMIN
  )

  val Recruiter = User(
    email = RecruiterEmail,
    hashedPassword = RecruiterHashedPassword,
    firstName = Some("firstName2"),
    lastName = Some("lastName2"),
    company = Some("someOtherCompany"),
    role = Role.RECRUITER
  )

  val UpdatedAdmin = User(
    email = AdminEmail,
    hashedPassword = UpdatedHashedPassword,
    firstName = Some("Megan"),
    lastName = Some("Fox"),
    company = Some("Transformers"),
    role = Role.ADMIN
  )
}
