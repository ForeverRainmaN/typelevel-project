package com.rockthejvm.jobsboard.playground

import cats.effect.*
import cats.effect.kernel.Async
import cats.effect.unsafe.implicits.global
import com.rockthejvm.jobsboard.algebra.*
import com.rockthejvm.jobsboard.domain.Job.*
import com.rockthejvm.jobsboard.logging.syntax.logError
import doobie.*
import doobie.ExecutionContexts
import doobie.hikari.HikariTransactor
import doobie.util.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.io.StdIn

object JobsPlayground extends IOApp.Simple {
  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  val postgresResource: Resource[IO, HikariTransactor[IO]] = for {
    ec <- ExecutionContexts.fixedThreadPool[IO](32)
    xa <- HikariTransactor.newHikariTransactor[IO](
      "org.postgresql.Driver",
      "jdbc:postgresql://localhost:5436/board",
      "docker",
      "docker",
      ec
    )
  } yield xa

  val jobInfo: JobInfo = JobInfo.minimal(
    company = "Rock the JVM",
    title = "Software Englineer",
    description = "Best job ever",
    externalUrl = "rockthejvm.com",
    remote = true,
    location = "Anywhere"
  )

  override def run: IO[Unit] = postgresResource.use { xa =>
    for {
      jobs      <- LiveJobs[IO](xa)
      _         <- IO(println("Ready. Next...")) *> IO(StdIn.readLine)
      id        <- jobs.create("daniel@rockthejvm.com", jobInfo)
      _         <- IO(println("Next...")) *> IO(StdIn.readLine)
      list      <- jobs.all()
      _         <- IO(println(s"All jobs: $list. Next...")) *> IO(StdIn.readLine)
      _         <- jobs.update(id, jobInfo.copy(title = "Software Rockstar"))
      newJob    <- jobs.find(id)
      _         <- IO(println(s"New job: $newJob. Next..."))
      _         <- jobs.delete(id)
      listAfter <- jobs.all()
      _         <- IO(println(s"Deleted job. List now: $listAfter. Next...")) *> IO(StdIn.readLine)
    } yield ()
  }
}
