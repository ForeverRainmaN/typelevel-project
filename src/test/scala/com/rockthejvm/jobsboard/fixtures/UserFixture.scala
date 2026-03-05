package com.rockthejvm.jobsboard.fixtures

import com.rockthejvm.jobsboard.domain.Role
import com.rockthejvm.jobsboard.domain.user

import user.*

trait UserFixture {
  val InvalidEmail = "myInvalidEmail.com"
  val ValidEmail   = "daniel@rockthejvm.com"

  val ValidUserAdmin =
    User(
      email = "daniel@rockthejvm.com",
      hashedPassword = "hashedPassword",
      firstName = Some("Daniel"),
      lastName = Some("Kurchin"),
      company = Some("Rock The JVM"),
      role = Role.ADMIN
    )

  val ValidUserRecruiter =
    ValidUserAdmin.copy(role = Role.RECRUITER)

  val InvalidUser =
    User(
      email = InvalidEmail,
      hashedPassword = "hashedPassword2",
      firstName = Some("Daniel"),
      lastName = Some("Kurchin"),
      company = Some("Rock The JVM"),
      role = Role.ADMIN
    )

  val UpdatedUserAdmin =
    User(
      email = "daniel@rockthejvm.com",
      hashedPassword = "HASHEDPASSWORD",
      firstName = Some("Megan"),
      lastName = Some("Fox"),
      company = Some("Transformers"),
      role = Role.ADMIN
    )
}
