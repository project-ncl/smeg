import org.slf4j.LoggerFactory

import scala.io.Source
import scala.language.postfixOps

Global / onChangedBuildSource := IgnoreSourceChanges

ThisBuild / scalaVersion     := "2.13.5"
ThisBuild / version          := "1.0.0.rebuild-1"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"
  
lazy val root = (project in file("."))
  .settings(
    name := "Smeg Manual Version Override Test Project",

    TaskKey[Unit]("runTest") := {
      sys.props += "versionIncrementalSuffix" -> "rebuild"
      Command.process("manipulate", state.value)
    },

    TaskKey[Unit]("verifyTest") := {
      val expected = Source.fromFile("manipulations.sbt.expected").getLines.toArray
      println("Expected manipulation file structure:\n" + expected.mkString)
      val actual = Source.fromFile("manipulations.sbt").getLines.toArray
      println("Actual manipulation file structure:\n" + actual.mkString)

      if (!actual.sameElements(expected)) sys.error("Test failure: Output manipulations do not match")
    }
  )
