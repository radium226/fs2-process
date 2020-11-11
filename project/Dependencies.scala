import sbt._

object Dependencies {

  lazy val scalaTest = Seq(
    "org.scalatest" %% "scalatest" % "3.1.3"
  )

  lazy val cats = Seq(
    "org.typelevel" %% "cats-core" % "2.1.1",
    "org.typelevel" %% "cats-effect" % "2.1.4",
    "org.typelevel" %% "kittens" % "2.1.0",
    "org.typelevel" %% "mouse" % "0.25"
  )

  lazy val fs2 = Seq(
    "co.fs2" %% "fs2-core" % "2.4.0",
    "co.fs2" %% "fs2-io" % "2.4.0"
  )

  lazy val slf4j = Seq(
    "org.slf4j" % "slf4j-api" % "1.7.30",
    "org.slf4j" % "slf4j-simple" % "1.7.30"
  )

}
