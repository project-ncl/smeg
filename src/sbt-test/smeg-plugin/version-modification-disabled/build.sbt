import scala.io.Source

Global / onChangedBuildSource := IgnoreSourceChanges

organization := "com.example"
name := "smeg-version-modification-disabled-test"
version := "0.2.11"
scalaVersion := "2.13.5"

TaskKey[Unit]("runTest") := {
  // PNC LIGHTWELL_UPSTREAM: the suffix is a placeholder, versionModification=false keeps the version
  sys.props += "versionIncrementalSuffix" -> "not-used"
  sys.props += "versionIncrementalSuffixPadding" -> "5"
  sys.props += "versionModification" -> "false"
  Command.process("manipulate", state.value)
}

TaskKey[Unit]("verifyTest") := {
  val actual = Source.fromFile("manipulations.sbt").getLines.toArray
  if (actual.nonEmpty) sys.error(s"Test failure: unexpected manipulations ${actual.mkString(" ")}")
}

TaskKey[Unit]("verifyVersion") := {
  if (version.value != "0.2.11") sys.error(s"Test failure: version is ${version.value}")
}
