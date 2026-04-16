package com.rockthejvm.foundations
import cats.effect.{ExitCode, IO, IOApp}

import scala.io.StdIn

object CatsEffect extends IOApp.Simple {
  /*
    describing computations as values
   */
  // IO
  // IO = data structure describing arbitrary computations (including side effects)

  val firstIO: IO[Int] = IO.pure(42)
  val delayedIO: IO[Int] = IO {
    println("I'm just about to produce the meaning of life")
    42
  }

  def evaluateIO[A](ioA: IO[A]): Unit = {
    import cats.effect.unsafe.implicits.global
    val meaningOfLife = delayedIO.unsafeRunSync()
    println(s"the result of the effect is: $meaningOfLife")
  }

  val improvedMeaningOfLife = firstIO.map(_ * 2)
  val printedMeaningOfLife  = firstIO.flatMap(mol => IO(println(mol)))

  def smallProgram(): IO[Unit] = for {
    line1 <- IO(StdIn.readLine())
    line2 <- IO(StdIn.readLine())
    _     <- IO(println(line1 + line2))
  } yield ()

  // old style
  //  def main(args: Array[String]): Unit = {
  //    evaluateIO(delayedIO)
  //  }
  // new version
  override def run = smallProgram()

}
