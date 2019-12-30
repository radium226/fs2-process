package com.github.radium226.fs2

import cats.effect.{Blocker, Concurrent, ContextShift, Sync}
import fs2._
import java.lang.{Process => JavaProcess}

import cats.Functor
import cats.implicits._

object process {

  trait Keep[F[_], T] {

    def stream(process: Process[F])(implicit F: Concurrent[F]): Stream[F, T]

  }

  object Keep {

    case class StdOut[F[_]](stdErrPipe: Pipe[F, Byte, Unit]) extends Keep[F, Byte] {

      override def stream(process: Process[F])(implicit F: Concurrent[F]): Stream[F, Byte] = {
        process.stdOut concurrently process.stdErr.through(stdErrPipe).drain concurrently process.exitCode.drain
      }

    }

    case class ExitCode[F[_]](stdOutPipe: Pipe[F, Byte, Unit], stdErrPipe: Pipe[F, Byte, Unit]) extends Keep[F, Int] {

      override def stream(process: Process[F])(implicit F: Concurrent[F]): Stream[F, Int] = {
        process.exitCode concurrently process.stdErr.through(stdErrPipe).drain concurrently process.stdOut.through(stdOutPipe).drain
      }

    }

    def stdOut[F[_]](implicit F: Functor[F]): Keep[F, Byte] = stdOut[F]({ stream: Stream[F, Byte] => stream.drain })

    def stdOut[F[_]](stdErrPipe: Pipe[F, Byte, Unit])(implicit F: Functor[F]): Keep[F, Byte] = StdOut[F](stdErrPipe)

    def exitCode[F[_]]: Keep[F, Int] = exitCode()

    def exitCode[F[_]](stdOutPipe: Pipe[F, Byte, Unit] = { stream: Stream[F, Byte] => stream.drain }, stdErrPipe: Pipe[F, Byte, Unit] = { stream: Stream[F, Byte] => stream.drain }): Keep[F, Int] = ExitCode[F](stdOutPipe, stdErrPipe)

  }

  class Process[F[_]](process: JavaProcess, val stdOut: Stream[F, Byte], val stdErr: Stream[F, Byte], val exitCode: Stream[F, Int]) {
    self =>

    def apply[T](keep: Keep[F, T])(implicit F: Concurrent[F]): Stream[F, T] = {
      keep.stream(self)
    }

  }

  def stream[F[_]](command: List[String], blocker: Blocker, chunkSize: Int)(implicit F: Concurrent[F], contextShift: ContextShift[F]): Stream[F, Process[F]] = {
    Stream.never[F].through(pipe(command, blocker, chunkSize))
  }

  def pipe[F[_]](command: List[String], blocker: Blocker, chunkSize: Int)(implicit F: Concurrent[F], contextShift: ContextShift[F]): Pipe[F, Byte, Process[F]] = { stream =>
    Stream.eval[F, JavaProcess](blocker.delay({
      new ProcessBuilder()
        .command(command: _*)
        .redirectInput(ProcessBuilder.Redirect.PIPE)
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)
        .start()
    })).flatMap({ process =>
      val stdin = stream.through(fs2.io.writeOutputStream[F](F.delay(process.getOutputStream), blocker))
      val stdout = fs2.io.readInputStream[F](F.delay(process.getInputStream), chunkSize, blocker)
      val stderr = fs2.io.readInputStream[F](F.delay(process.getErrorStream), chunkSize, blocker)

      Stream[F, Process[F]](new Process[F](process, stdout, stderr, Stream.eval(blocker.delay(process.waitFor())))).concurrently(stdin.drain)
    })

  }

}
