package com.rockthejvm.jobsboard.fixtures

import com.rockthejvm.jobsboard.domain.Role
import com.rockthejvm.jobsboard.domain.user

import user.*

trait UserFixture {
  val InvalidEmail   = "myInvalidEmail.com"
  val AdminEmail     = "admin@something.com"
  val RecruiterEmail = "recruiter@something.com"

  val Admin =
    User(
      email = AdminEmail,
      hashedPassword = "adminPassword",
      firstName = Some("firstName"),
      lastName = Some("lastName"),
      company = Some("someCompany"),
      role = Role.ADMIN
    )

  val Recruiter =
    User(
      email = RecruiterEmail,
      hashedPassword = "recruiterPassword",
      firstName = Some("firstName2"),
      lastName = Some("lastName2"),
      company = Some("someOtherCompany"),
      role = Role.RECRUITER
    )

  val InvalidUser =
    User(
      email = InvalidEmail,
      hashedPassword = "hashedPassword2",
      firstName = Some("invalidFistName"),
      lastName = Some("invalidLastName"),
      company = Some("invalidCompany"),
      role = Role.ADMIN
    )

  val UpdatedAdmin =
    User(
      email = AdminEmail,
      hashedPassword = "HASHEDPASSWORD",
      firstName = Some("Megan"),
      lastName = Some("Fox"),
      company = Some("Transformers"),
      role = Role.ADMIN
    )
}
