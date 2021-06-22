import scala.io.Source
import scala.language.postfixOps

val baseVersion = settingKey[String]("custom version setting")
val baseVersionSuffix = settingKey[String]("custom version suffix setting")


ThisBuild / scalaVersion     := "2.13.5"
ThisBuild / version          := "1.0.0"
ThisBuild / organization     := "org.smegtest"
ThisBuild / organizationName := "smegtest"
  
lazy val root = (project in file("."))
  .settings(
    name := "smegtest",
    baseVersion := "5.0.0",
    baseVersionSuffix := "rebuild-2",
    TaskKey[Unit]("runTest") := {
      sys.props += "versionIncrementalSuffix" -> "rebuild"
      sys.props += "settingTransposition.version" -> "baseVersion"
      sys.props += "versionSuffixSetting" -> "baseVersionSuffix"
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
