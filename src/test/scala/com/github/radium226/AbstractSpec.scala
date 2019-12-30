package com.github.radium226

import org.scalatest.{Assertion, Succeeded}
import org.scalatest.flatspec.{AnyFlatSpec, AsyncFlatSpec}
import org.scalatest.matchers.should.Matchers

import scala.concurrent.{ExecutionContext, Future}

import cats.effect.{ContextShift, IO, SyncIO, Timer}
import cats.implicits._


abstract class AbstractSpec extends AsyncFlatSpec with Matchers {

  implicit def syncIoToFutureAssertion(syncIO: SyncIO[Assertion]): Future[Assertion] = {
    syncIO.toIO.unsafeToFuture
  }

  implicit def ioToFutureAssertion(io: IO[Assertion]): Future[Assertion] = {
    io.unsafeToFuture
  }

  implicit def syncIoUnitToFutureAssertion(syncIO: SyncIO[Unit]): Future[Assertion] = {
    syncIO.toIO.as(Succeeded).unsafeToFuture
  }

  implicit def ioUnitToFutureAssertion(io: IO[Unit]): Future[Assertion] = {
    io.as(Succeeded).unsafeToFuture
  }

  implicit val ioContextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  implicit val ioTimer: Timer[IO] = IO.timer(ExecutionContext.global)

}