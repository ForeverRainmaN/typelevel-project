package com.rockthejvm.jobsboard.core

import cats.effect.*
import com.rockthejvm.jobsboard.config.PostgresTestConfig
import doobie.*
import doobie.hikari.HikariTransactor

trait DoobieSpec {
  def createTransactor(config: PostgresTestConfig): Resource[IO, Transactor[IO]] =
    for {
      ce <- ExecutionContexts.fixedThreadPool[IO](1)
      xa <- HikariTransactor.newHikariTransactor[IO](
        "org.postgresql.Driver",
        config.dbUrl,
        config.dbUser,
        config.dbPassword,
        ce
      )
    } yield xa
}
