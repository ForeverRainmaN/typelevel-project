package com.rockthejvm.jobsboard.algebra

import cats.*
import cats.implicits.*
import com.rockthejvm.jobsboard.domain.Job.*

import java.util.UUID

trait Jobs[F[_]] {
  def create(ownerEmail: String, jobInfo: JobInfo): F[UUID]
  def all(): F[List[Job]]
  def find(id: UUID): F[Option[Job]]
  def update(id: UUID, jbInfo: JobInfo): F[Option[Job]]
  def delete(id: UUID): F[Int]
}

class LiveJobs[F[_]] private extends Jobs[F] {

  override def create(ownerEmail: String, jobInfo: JobInfo): F[UUID] = ???

  override def all(): F[List[Job]] = ???

  override def find(id: UUID): F[Option[Job]] = ???

  override def update(id: UUID, jbInfo: JobInfo): F[Option[Job]] = ???

  override def delete(id: UUID): F[Int] = ???
}

object LiveJobs {
  def apply[F[_]: Applicative]: F[LiveJobs[F]] = new LiveJobs[F].pure[F]
}
