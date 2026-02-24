package com.rockthejvm.jobsboard.algebra

import cats.effect.kernel.MonadCancelThrow
import cats.implicits.*
import com.rockthejvm.jobsboard.domain.Job.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*

import java.util.UUID

trait Jobs[F[_]] {
  def create(ownerEmail: String, jobInfo: JobInfo): F[UUID]
  def all(): F[List[Job]]
  def find(id: UUID): F[Option[Job]]
  def update(id: UUID, jbInfo: JobInfo): F[Option[Job]]
  def delete(id: UUID): F[Int]
}

class LiveJobs[F[_]: MonadCancelThrow] private (xa: Transactor[F]) extends Jobs[F] {
  private val columns: Fragment =
    fr"id, date, ownerEmail, active, company, title, description, externalUrl, remote, location, salaryLo, salaryHi, currency, country, tags, image, seniority, other"

  override def create(ownerEmail: String, jobInfo: JobInfo): F[UUID] =
    sql"""
       INSERT INTO jobs(
        date,
        ownerEmail,
        company,
        title,
        description,
        externalUrl,
        remote,
        location,
        salaryLo,
        salaryHi,
        currency,
        country,
        tags,
        image,
        seniority,
        other,
        active
       ) VALUES (
          ${System.currentTimeMillis()},
          $ownerEmail,
          ${jobInfo.company},
          ${jobInfo.title},
          ${jobInfo.description},
          ${jobInfo.externalUrl},
          ${jobInfo.remote},
          ${jobInfo.location},
          ${jobInfo.salaryLo},
          ${jobInfo.salaryHi},
          ${jobInfo.currency},
          ${jobInfo.country},
          ${jobInfo.tags},
          ${jobInfo.image},
          ${jobInfo.seniority},
          ${jobInfo.other},
          false
         )
       """.update
      .withUniqueGeneratedKeys[UUID]("id")
      .transact(xa)

  override def all(): F[List[Job]] =
    (fr"SELECT" ++ columns ++ fr"FROM jobs")
      .query[Job]
      .to[List]
      .transact(xa)

  override def find(id: UUID): F[Option[Job]] =
    (fr"SELECT" ++ columns ++ fr"FROM jobs WHERE id = $id")
      .query[Job]
      .option
      .transact(xa)

  override def update(id: UUID, jobInfo: JobInfo): F[Option[Job]] =
    sql"""
         UPDATE jobs
         SET
           company = ${jobInfo.company},
           title = ${jobInfo.title},
           description = ${jobInfo.description},
           externalUrl = ${jobInfo.externalUrl},
           remote = ${jobInfo.remote},
           location = ${jobInfo.location},
           salaryLo = ${jobInfo.salaryLo},
           salaryHi = ${jobInfo.salaryHi},
           currency = ${jobInfo.currency},
           country = ${jobInfo.country},
           tags = ${jobInfo.tags},
           image = ${jobInfo.image},
           seniority = ${jobInfo.seniority},
           other = ${jobInfo.other}
          WHERE id = ${id}
       """.update.run.transact(xa).flatMap(_ => find(id))

  override def delete(id: UUID): F[Int] =
    sql"""
         DELETE FROM jobs
         WHERE id = $id
       """.update.run.transact(xa)
}

object LiveJobs {
  given jobRead: Read[Job] = Read[
    (
        UUID,                 // id
        Long,                 // date
        String,               // ownerEmail
        Boolean,              // active
        String,               // company
        String,               // title
        String,               // description
        String,               // externalUrl
        Boolean,              // remote
        String,               // location
        Option[Int],          // salaryLo
        Option[Int],          // salaryHi
        Option[String],       // currency
        Option[String],       // country
        Option[List[String]], // tags
        Option[String],       // image
        Option[String],       // seniority
        Option[String]        // other
    )
  ].map {
    case (
          id: UUID,
          date: Long,
          ownerEmail: String,
          active: Boolean,
          company: String,
          title: String,
          description: String,
          externalUrl: String,
          remote: Boolean,
          location: String,
          salaryLo: Option[Int] @unchecked,
          salaryHi: Option[Int] @unchecked,
          currency: Option[String] @unchecked,
          country: Option[String] @unchecked,
          tags: Option[List[String]] @unchecked,
          image: Option[String] @unchecked,
          seniority: Option[String] @unchecked,
          other: Option[String] @unchecked
        ) =>
      Job(
        id = id,
        date = date,
        ownerEmail = ownerEmail,
        active = active,
        jobInfo = JobInfo(
          company = company,
          title = title,
          description = description,
          externalUrl = externalUrl,
          remote = remote,
          location = location,
          salaryLo = salaryLo,
          salaryHi = salaryHi,
          currency = currency,
          country = country,
          tags = tags,
          image = image,
          seniority = seniority,
          other = other
        )
      )
  }

  def apply[F[_]: MonadCancelThrow](xa: Transactor[F]): F[LiveJobs[F]] =
    new LiveJobs[F](xa).pure[F]
}
