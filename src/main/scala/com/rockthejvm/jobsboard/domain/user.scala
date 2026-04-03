package com.rockthejvm.jobsboard.domain

import doobie.postgres.*
import doobie.postgres.implicits.*
import doobie.util.meta.Meta
import tsec.authorization.AuthGroup
import tsec.authorization.SimpleAuthEnum

enum Role {
  case ADMIN, RECRUITER
}

object Role {
  given Meta[Role] = pgEnumStringOpt("role_type", s => Some(Role.valueOf(s)), _.toString)

  given roleAuthEnum: SimpleAuthEnum[Role, String] with {
    override val values: AuthGroup[Role]     = AuthGroup(Role.ADMIN, Role.RECRUITER)
    override def getRepr(role: Role): String = role.toString
  }
}

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
