ThisBuild / organization := "com.github.radium226"
ThisBuild / scalaVersion := "2.13.1"
ThisBuild / version      := "0.1-SNAPSHOT"

ThisBuild / scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-unchecked",
  "-language:postfixOps",
  "-language:higherKinds")

addCompilerPlugin("org.typelevel" % "kind-projector_2.13.1" % "0.11.0")

lazy val root = (project in file("."))
  .settings(
    name := "fs2-process",
    libraryDependencies ++= Dependencies.cats,
    libraryDependencies ++= Dependencies.fs2,
    libraryDependencies ++= Dependencies.scalaTest,
    libraryDependencies ++= Dependencies.slf4j
  )
