package com.rockthejvm.jobsboard.config

import cats.MonadThrow
import cats.syntax.flatMap.*
import pureconfig.ConfigReader
import pureconfig.ConfigSource
import pureconfig.error.ConfigReaderException

import scala.reflect.ClassTag

object syntax {
  extension (source: ConfigSource)
    def loadF[F[_], A](using reader: ConfigReader[A], F: MonadThrow[F], ct: ClassTag[A]): F[A] =
      F.pure(source.load[A]).flatMap {
        case Left(errors) => F.raiseError[A](ConfigReaderException(errors)(ct))
        case Right(value) => F.pure(value)
      }
}
