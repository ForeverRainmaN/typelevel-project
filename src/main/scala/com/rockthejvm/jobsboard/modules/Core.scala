package com.rockthejvm.jobsboard.modules
import cats.*
import cats.effect.*
import doobie.hikari.HikariTransactor
import doobie.util.*
import com.rockthejvm.jobsboard.algebra.*

final class Core[F[_]] private (val jobs: Jobs[F])

// postgress -> jobs -> core -> httpApi -> app
object Core {
  def postgresResource[F[_]: Async]: Resource[F, HikariTransactor[F]] = for {
    ec <- ExecutionContexts.fixedThreadPool[F](32)
    xa <- HikariTransactor.newHikariTransactor[F](
      "org.postgresql.Driver",
      "jdbc:postgresql://localhost:5436/board",
      "docker",
      "docker",
      ec
    )
  } yield xa

  def apply[F[_]: Async]: Resource[F, Core[F]] =
    postgresResource[F]
      .evalMap(transactor => LiveJobs[F](transactor))
      .map(jobs => new Core(jobs))
}
