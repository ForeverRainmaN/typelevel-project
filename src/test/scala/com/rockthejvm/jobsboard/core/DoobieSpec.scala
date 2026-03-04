package com.rockthejvm.jobsboard.core

import cats.effect.IO
import cats.effect.kernel.Resource
import com.rockthejvm.jobsboard.config.PostgresTestConfig
import doobie.hikari.HikariTransactor
import doobie.implicits.*
import doobie.util.ExecutionContexts
import doobie.util.transactor.Transactor

trait DoobieSpec {

  def createTransactor(config: PostgresTestConfig): Resource[IO, HikariTransactor[IO]] =
    for {
      ec <- ExecutionContexts.fixedThreadPool[IO](1)
      xa <- HikariTransactor.newHikariTransactor[IO](
        "org.postgresql.Driver",
        config.dbUrl,
        config.dbUser,
        config.dbPassword,
        ec
      )
    } yield xa

  def cleanJobsTable(xa: Transactor[IO]): IO[Unit] =
    sql"TRUNCATE jobs".update.run.transact(xa).void

  def withTransactor[A](config: IO[PostgresTestConfig])(test: Transactor[IO] => IO[A]): IO[A] =
    config.flatMap { c =>
      createTransactor(c).use { xa =>
        cleanJobsTable(xa) >> test(xa)
      }
    }
}
