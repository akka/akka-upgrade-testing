import Dependencies._
import com.typesafe.sbt.packager.docker._
import Keys._

import scala.sys.process.Process

ThisBuild / version := akkaVersion
ThisBuild / scalaVersion := "2.12.8"
ThisBuild / organization := "com.lightbend"
ThisBuild / organizationName := "Lightbend Inc"

ThisBuild / testOptions in Test += Tests.Argument("-oDF")

lazy val root = (project in file("."))
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
  .configs(IntegrationTest)
  .settings(Defaults.itSettings)
