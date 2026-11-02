import scala.io.Source

Global / onChangedBuildSource := IgnoreSourceChanges

// version set on the project itself, which wins over ThisBuild / version
organization := "com.example"
name := "smeg-project-version-test"
version := "0.6.1"
scalaVersion := "2.13.5"

TaskKey[Unit]("runTest") := {
  sys.props += "versionIncrementalSuffix" -> "n"
  sys.props += "versionIncrementalSuffixPadding" -> "5"
  sys.props += "enforceVersionPrefix" -> "rhlw"
  Command.process("manipulate", state.value)
}

TaskKey[Unit]("verifyTest") := {
  val expected = Source.fromFile("manipulations.sbt.expected").getLines.toArray
  val actual = Source.fromFile("manipulations.sbt").getLines.toArray
  if (!actual.sameElements(expected)) sys.error(s"Test failure: manipulations ${actual.mkString(" ")} do not match")
}

TaskKey[Unit]("verifyVersion") := {
  if (version.value != "0.6.1.rhlw-00000-n-00001") sys.error(s"Test failure: version is ${version.value}")
}
