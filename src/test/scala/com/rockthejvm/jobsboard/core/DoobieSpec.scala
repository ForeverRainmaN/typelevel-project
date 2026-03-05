package com.rockthejvm.jobsboard.core

import cats.effect.IO
import cats.effect.kernel.Resource
import cats.implicits.*
import cats.syntax.*
import com.rockthejvm.jobsboard.config.PostgresTestConfig
import doobie.hikari.HikariTransactor
import doobie.implicits.*
import doobie.util.ExecutionContexts
import doobie.util.fragment.Fragment
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

  def truncateTable(xa: Transactor[IO])(table: String): IO[Unit] =
    Fragment.const(s"TRUNCATE $table").update.run.transact(xa).void

  def withTransactor[A](config: IO[PostgresTestConfig])(test: Transactor[IO] => IO[A]): IO[A] =
    config.flatMap { c =>
      createTransactor(c).use { xa => test(xa) }
    }
}
