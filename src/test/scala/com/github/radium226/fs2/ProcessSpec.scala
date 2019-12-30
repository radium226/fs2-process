package com.github.radium226.fs2

import cats.effect.{Blocker, ContextShift, ExitCode, IO, Sync}
import fs2._
import com.github.radium226.AbstractSpec
import com.github.radium226.fs2.process.{Keep, Process}
import cats.implicits._
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.duration._

class ProcessSpec extends AbstractSpec {

  def bash(script: String): Stream[IO, Process[IO]] = for {
    blocker   <- Stream.resource[IO, Blocker](Blocker[IO])
    chunkSize  = 1
    process   <- process.stream[IO](List("bash", "-c", script), blocker, chunkSize)
  } yield process

  it should "be able to execute process and retreive stdin and stdout" in {
    val script =
      """for i in $( seq 0 30 ); do
        |  echo "stdout=${i}" >&1
        |  sleep 0.125
        |  echo "stderr=${i}" >&2
        |  sleep 0.125
        |done
        |""".stripMargin

    val stream = for {
      process <- bash(script)
      stdout   = process.stdOut.through(fs2.text.utf8Decode[IO]).through(fs2.text.lines[IO])
      stderr   = process.stdErr.through(fs2.text.utf8Decode[IO]).through(fs2.text.lines[IO])
      line    <- stdout.interleave(stderr).filter(!_.isBlank)
    } yield line

    stream.compile.toList.map({ lines =>
      lines shouldBe (0 to 30).flatMap({ index => List("stdout", "stderr").map(_.concat(s"=${index}")) }).toList
    })
  }

  it should "be able to retrieve exit code" in {
    val script =
      """sleep 5
        |exit 12
        |""".stripMargin

    val stream = for {
      process  <- bash(script)
      exitCode <- process(Keep.exitCode[IO])
    } yield exitCode

    stream.compile.last.map({
      case Some(exitCode) =>
        exitCode shouldBe 12

      case None =>
        fail()
    })
  }

  it should "able to retrieve exit code even if there is a huge output" in {
    val script =
      """set -euo pipefail
        |#timeout 15s bash -c 'while true; do echo "abcdefghijklmnopqrstuvwxyz"; done' || true
        |timeout 15s cat /dev/zero || true
        |exit 42
        |""".stripMargin

    val stream = for {
      process  <- bash(script)
      exitCode <- process(Keep.exitCode[IO])
    } yield exitCode

    stream.compile.last.map({
      case Some(exitCode) =>
        exitCode shouldBe 42

      case None =>
        fail()
    })
  }

  it should "be able to pipe data into process" in {
    val upperCaseLetters = for {
      blocker          <- Stream.resource[IO, Blocker](Blocker[IO])
      chunkSize         = 1
      lowerCaseLetters  = Stream[IO, String](('a' to 'z').mkString("\n")).repeat.take(100)
      process          <- lowerCaseLetters
                            .through(fs2.text.utf8Encode[IO])
                            .through(process.pipe[IO](List("tr", "[a-z]", "[A-Z]"), blocker, chunkSize))
      upperCaseLetters  = process(Keep.stdOut[IO]).through(fs2.text.utf8Decode[IO])
      upperCaseLetter  <- upperCaseLetters
    } yield upperCaseLetter

    upperCaseLetters.showLinesStdOut.compile.drain
  }

  val logger: Logger = LoggerFactory.getLogger("mplayer")

  sealed trait LogLevel
  object LogLevel {

    case object Debug extends LogLevel
    def debug: LogLevel = Debug

    case object Info extends LogLevel
    def info: LogLevel = Info

    case object Warn extends LogLevel
    def warn: LogLevel = Warn

  }

  def logTo[F[_]](logger: Logger, logLevel: LogLevel, blocker: Blocker)(implicit F: Sync[F], contextShift: ContextShift[F]): Pipe[F, Byte, Unit] = { bytes =>
    bytes
      .through(fs2.text.utf8Decode[F])
      .through(fs2.text.lines[F])
      .evalMap({ line =>
        blocker.delay(logLevel match {
          case LogLevel.Debug =>
            logger.debug(line)

          case LogLevel.Info =>
            logger.info(line)

          case LogLevel.Warn =>
            logger.warn(line)
        })
      })
      .drain
  }

  it should "be able to pipe stuff" in {
    (for {
      processBlocker <- Stream.resource[IO, Blocker](Blocker[IO])
      logBlocker     <- Stream.resource[IO, Blocker](Blocker[IO])
      chunkSize       = 1024 * 1024
      catProcess     <- process.stream[IO](List("cat", "src/test/resources/Yes.webm"), processBlocker, chunkSize)
      keepStdOut  = Keep.stdOut(
        logTo[IO](logger, LogLevel.info, logBlocker)
      )
      process        <- catProcess(keepStdOut).through(process.pipe[IO](List("mplayer", "-"), processBlocker, chunkSize))
      keepExitCode    = Keep.exitCode[IO](
        logTo[IO](logger, LogLevel.info, logBlocker),
        logTo[IO](logger, LogLevel.warn, logBlocker)
      )
      exitCode      <- process(keepExitCode)
    } yield exitCode).interruptAfter(1 second).compile.drain
  }

}
