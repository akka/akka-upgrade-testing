import Dependencies._
import com.typesafe.sbt.packager.docker._
import Keys._

import scala.sys.process.Process

version := akkaVersion

ThisBuild / scalaVersion := "2.12.8"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.lightbend"
ThisBuild / organizationName := "Lightbend Inc"

lazy val root = (project in file("."))
  .aggregate(rollingRestart, proto2, proto3)

lazy val rollingRestart = (project in file("rolling-restart"))
  .enablePlugins(JavaServerAppPackaging)
  .settings(name := "akka-upgrade-testing", libraryDependencies ++= serviceDeps)
  .settings(
    dockerCommands :=
      dockerCommands.value.flatMap {
        case ExecCmd("ENTRYPOINT", args @ _*) =>
          Seq(Cmd("ENTRYPOINT", args.mkString(" ")))
        case v => Seq(v)
      },
    dockerExposedPorts := Seq(8558, 2552),
    dockerBaseImage := "openjdk:8-jre-alpine",
    dockerCommands ++= Seq(
      Cmd("USER", "root"),
      Cmd(
        "RUN",
        "/sbin/apk",
        "add",
        "--no-cache",
        "bash",
        "bind-tools",
        "busybox-extras",
        "curl",
        "strace"
      ),
      Cmd("RUN", "chgrp -R 0 . && chmod -R g=u .")
    )
  )

lazy val proto2 = (project in file("proto2"))
  .enablePlugins(ProtobufPlugin)
  .settings(
    (ProtobufConfig / version) := "2.5.0",
    protobufProtoc := "protoc-2.5.0",
    ProtobufConfig / protobufRunProtoc := {
      val s = streams.value
      val protoc = protobufProtoc.value
      println("Running: " + protoc)
      args =>
        Process(protoc, args) ! s.log
    },
    (ProtobufConfig / javaSource) := (Compile / javaSource).value,
    libraryDependencies += Dependencies.scalaTest
  )

lazy val proto3 = (project in file("proto3"))
  .enablePlugins(ProtobufPlugin)
  .settings(
    (ProtobufConfig / version) := "3.9.0",
    protobufProtoc := "protoc",
    (ProtobufConfig / javaSource) := (Compile / javaSource).value,
    libraryDependencies += Dependencies.scalaTest
  )
