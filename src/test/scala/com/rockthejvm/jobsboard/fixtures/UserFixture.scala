package com.rockthejvm.jobsboard.fixtures

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.rockthejvm.jobsboard.domain.Role
import com.rockthejvm.jobsboard.domain.user.*
import tsec.passwordhashers.jca.BCrypt
import com.rockthejvm.jobsboard.algebra.Users

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

  val newUserAdmin = NewUserInfo(
    adminEmail,
    adminRawPassword,
    Some("admin1"),
    Some("admin2"),
    Some("admincompany")
  )

  val newUserRecruiter = NewUserInfo(
    recruiterEmail,
    recruiterRawPassword,
    Some("recruiter1"),
    Some("recruiter2"),
    Some("recruitercompany")
  )

  val mockedUsers: Users[IO] = new Users[IO] {
    override def find(email: String): IO[Option[User]] =
      if (email == adminEmail) IO.pure(Some(admin))
      else IO.pure(None)
    override def create(user: User): IO[String]       = IO.pure(adminEmail)
    override def update(user: User): IO[Option[User]] = IO.pure(Some(user))
    override def delete(email: String): IO[Boolean]   = IO.pure(true)
  }
}
