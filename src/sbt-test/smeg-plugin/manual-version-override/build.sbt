import scala.io.Source
import scala.language.postfixOps

ThisBuild / scalaVersion     := "2.13.5"
ThisBuild / version          := "0.1.0"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"
  
lazy val root = (project in file("."))
  .settings(
    name := "Smeg Manual Version Override Test Project",

    TaskKey[Unit]("runTest") := {
      sys.props += "versionOverride" -> "10.10.10.redhat-10"
      Command.process("manipulate", state.value)
    },

    TaskKey[Unit]("verifyTest") := {
      val expected = Source.fromFile("manipulations.sbt.expected").getLines.toArray
      val actual = Source.fromFile("manipulations.sbt").getLines.toArray

      if (!actual.sameElements(expected)) sys.error("Test failure: Output manipulations do not match")
    }
  )
