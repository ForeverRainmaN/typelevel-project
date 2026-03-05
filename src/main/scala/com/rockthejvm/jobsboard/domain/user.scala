package com.rockthejvm.jobsboard.domain

import doobie.postgres.*
import doobie.postgres.implicits.*
import doobie.util.meta.Meta

enum Role:
  case ADMIN, RECRUITER

object Role:
  given Meta[Role] = pgEnumStringOpt("role_type", s => Some(Role.valueOf(s)), _.toString)

object user {
  final case class User(
      email: String,
      hashedPassword: String,
      firstName: Option[String],
      lastName: Option[String],
      company: Option[String],
      role: Role
  )

  final case class NewUserInfo(
      email: String,
      password: String,
      firstName: Option[String],
      lastName: Option[String],
      company: Option[String]
  )
}
