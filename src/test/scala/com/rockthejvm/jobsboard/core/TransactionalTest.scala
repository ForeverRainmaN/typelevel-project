package com.rockthejvm.jobsboard.core

import cats.effect.IO
import com.rockthejvm.jobsboard.config.PostgresTestConfig
import doobie.implicits.*
import doobie.util.transactor.Transactor

trait TransactionalTest { self: DoobieSpec =>
  def withRollback[A](config: IO[PostgresTestConfig])(test: Transactor[IO] => IO[A]): IO[A] = {
    config.flatMap { c =>
      createTransactor(c).use { xa =>
        sql"BEGIN".update.run.transact(xa) >>
          test(xa).attempt.flatMap {
            case Right(result) =>
              sql"ROLLBACK".update.run.transact(xa).as(result)
            case Left(e) =>
              sql"ROLLBACK".update.run.transact(xa) >> IO.raiseError(e)
          }
      }
    }
  }
}
