package com.rockthejvm.jobsboard.domain

import doobie.postgres.*
import doobie.postgres.implicits.*
import doobie.util.meta.Meta
import tsec.authorization.AuthGroup
import tsec.authorization.SimpleAuthEnum
import com.rockthejvm.jobsboard.domain.Job.*

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
  ) {
    def owns(job: Job): Boolean = email == job.ownerEmail
    def isAdmin: Boolean        = role == Role.ADMIN
    def isRecruiter: Boolean    = role == Role.RECRUITER
  }

  final case class NewUserInfo(
      email: String,
      password: String,
      firstName: Option[String],
      lastName: Option[String],
      company: Option[String]
  )
}
