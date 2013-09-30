package org.tritsch.scala.dpp.build

import sbt._
import sbt.Keys._

object Build extends sbt.Build {
  override lazy val settings = super.settings ++ Seq(
    name := "Simulation of Dining Philosophers Problem",
    version := "0.1",
    scalaVersion := "2.10.2",
    resolvers ++= Seq(
//      Opts.resolver.sonatypeSnapshots,
      "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases"
    ),
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.0.13",
      "com.typesafe" %% "scalalogging-slf4j" % "1.0.1",
      "com.typesafe.akka" %% "akka-slf4j" % "2.2.1",
      "org.slf4j" % "slf4j-api" % "1.7.5",

      "com.typesafe.akka" %% "akka-actor" % "2.1.0",
      "org.scalatest" %% "scalatest" % "1.9.1" % "test"
    )
  )

  lazy val root = Project(id = "dpp", base = file("."))
}
