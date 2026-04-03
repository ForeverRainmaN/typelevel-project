package com.rockthejvm.jobsboard.algebra

import cats.effect.*
import cats.implicits.*
import com.rockthejvm.jobsboard.domain.Role
import com.rockthejvm.jobsboard.domain.auth.*
import com.rockthejvm.jobsboard.domain.security.*
import com.rockthejvm.jobsboard.domain.user.*
import doobie.free.resultset
import org.checkerframework.checker.units.qual.m
import org.typelevel.log4cats.Logger
import tsec.authentication.AugmentedJWT
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.BCrypt

trait Auth[F[_]: Async: Logger] {
  def login(email: String, password: String): F[Option[JWTToken]]
  def signUp(newUserInfo: NewUserInfo): F[Option[User]]
  def changePassword(
      email: String,
      newPasswordInfo: NewPasswordInfo
  ): F[Either[String, Option[User]]]

  def delete(email: String): F[Boolean]

  def authenticator: Authenticator[F]
}

class LiveAuth[F[_]: Async: Logger] private (
    users: Users[F],
    override val authenticator: Authenticator[F]
) extends Auth[F] {
  override def login(email: String, password: String): F[Option[JWTToken]] =
    for {
      maybeUser <- users.find(email)
      maybeValidatedUser <- maybeUser.filterA(user =>
        BCrypt.checkpwBool[F](password, PasswordHash[BCrypt](user.hashedPassword))
      )
      maybeToken <- maybeValidatedUser.traverse(user => authenticator.create(user.email))
    } yield maybeToken
  override def signUp(newUserInfo: NewUserInfo): F[Option[User]] =
    def createNewUser(password: String): F[Option[User]] = for {
      hashedPw <- BCrypt.hashpw[F](password)
      user <- User(
        email = newUserInfo.email,
        hashedPassword = hashedPw,
        company = newUserInfo.company,
        firstName = newUserInfo.firstName,
        lastName = newUserInfo.lastName,
        role = Role.RECRUITER
      ).pure[F]
      _ <- users.create(user)
    } yield Some(user)

    users.find(newUserInfo.email).flatMap { maybeUser =>
      maybeUser.fold(createNewUser(newUserInfo.password))(_ => Option.empty[User].pure[F])
    }
  override def changePassword(
      email: String,
      newPasswordInfo: NewPasswordInfo
  ): F[Either[String, Option[User]]] = {
    def updateUser(user: User, newPassword: String): F[Option[User]] =
      for {
        newHashed   <- BCrypt.hashpw[F](newPasswordInfo.newPassword)
        updatedUser <- users.update(user.copy(hashedPassword = newHashed))
      } yield updatedUser

    def checkAndUpdate(
        user: User,
        oldPassword: String,
        newPassword: String
    ): F[Either[String, Option[User]]] = {
      for {
        passCheck <- BCrypt
          .checkpwBool[F](newPasswordInfo.oldPassword, PasswordHash(user.hashedPassword))
        updateResult <-
          if (passCheck) {
            updateUser(user, newPassword).map(Right(_))
          } else Left("Invalid password").pure[F]
      } yield updateResult
    }

    users.find(email).flatMap {
      case None => Right(None).pure[F]
      case Some(user) =>
        val NewPasswordInfo(oldPassword, newPassword) = newPasswordInfo
        checkAndUpdate(user, oldPassword, newPassword)
    }
  }

  override def delete(email: String): F[Boolean] =
    users.delete(email)
}

object LiveAuth {
  def apply[F[_]: Async: Logger](users: Users[F], authenticator: Authenticator[F]) =
    new LiveAuth[F](users, authenticator).pure[F]
}
