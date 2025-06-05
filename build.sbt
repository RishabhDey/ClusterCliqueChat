name := """play-scala-seed"""
organization := "clique"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.16"

ThisBuild / evictionErrorLevel := Level.Warn

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test

// jwt-scala core library
libraryDependencies += "com.github.jwt-scala" %% "jwt-core" % "10.0.1"
libraryDependencies += "org.mongodb.scala" %% "mongo-scala-driver" % "5.5.0"

