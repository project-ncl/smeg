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
      sys.props += "manipulation.disable" -> "true"
      Command.process("manipulate", state.value)
    }
  )
