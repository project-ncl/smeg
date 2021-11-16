import scala.io.Source
import scala.language.postfixOps

ThisBuild / scalaVersion     := "2.13.5"
ThisBuild / version          := "1.0.0"
ThisBuild / organization     := "org.smegtest"
ThisBuild / organizationName := "smegtest"
  
lazy val root = (project in file("."))
  .settings(
    name := "smegtest",
    TaskKey[Unit]("runTest") := {
      sys.props += "forceRootGA" -> "real-group-id:real-artifact-id"
      Command.process("manipulate", state.value)
      Command.process("writeReport", state.value)
    },

    TaskKey[Unit]("verifyTest") := {
      val expected = Source.fromFile("manipulations.json.expected").getLines.toArray
      println("Expected report file structure:\n" + expected.mkString)
      val actual = Source.fromFile("manipulations.json").getLines.toArray
      println("Actual report file structure:\n" + actual.mkString)

      if (!actual.sameElements(expected)) sys.error("Test failure: Output report file is incorrect")
    }
  )
