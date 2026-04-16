package com.rockthejvm.jobsboard.http.validation

import cats.*
import cats.data.Validated.Invalid
import cats.data.Validated.Valid
import cats.implicits.*
import com.rockthejvm.jobsboard.http.responses.FailureResponse
import com.rockthejvm.jobsboard.logging.syntax.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.EntityDecoder
import org.http4s.Request
import org.http4s.Response
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.*
import org.http4s.implicits.*
import org.http4s.implicits.*
import org.typelevel.log4cats.Logger

import Validators.*

object syntax {
  def validateEntity[A](entity: A)(using validator: Validator[A]): ValidationResult[A] =
    validator.validate(entity)

  trait HttpValidationDSL[F[_]: MonadThrow: Logger] extends Http4sDsl[F] {
    extension (req: Request[F]) {
      def validate[A: Validator](serverLogicIfValid: A => F[Response[F]])(using
          EntityDecoder[F, A]
      ): F[Response[F]] =
        req
          .as[A]
          .logError(e => s"Parsing payload failed: $e")
          .map(validateEntity)
          .flatMap {
            case Valid(entity) => serverLogicIfValid(entity)
            case Invalid(errors) =>
              BadRequest(FailureResponse(errors.toList.map(_.errorMessage).mkString(", ")))
          }
    }
  }
}
