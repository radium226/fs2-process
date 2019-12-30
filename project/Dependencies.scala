import sbt._

object Dependencies {

  lazy val cats = Seq(
    "org.typelevel" %% "cats-core" % "2.1.0",
    "org.typelevel" %% "cats-effect" % "2.0.0"
  )

  lazy val fs2 = Seq(
    "co.fs2" %% "fs2-core" % "2.1.0",
    "co.fs2" %% "fs2-io" % "2.1.0"
  )

  lazy val scalaTest = Seq(
    "org.scalactic" %% "scalactic" % "3.1.0",
    "org.scalatest" %% "scalatest" % "3.1.0" % "test"
  )

  lazy val slf4j = Seq(
    "org.slf4j" % "slf4j-api" % "1.7.30",
    "org.slf4j" % "slf4j-simple" % "1.7.30"
  )

}
