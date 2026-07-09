import sbt.Keys.versionScheme
import sbt._

ThisBuild / scalaVersion     := "2.12.13"
ThisBuild / version          := "0.3.1.0-SNAPSHOT"
ThisBuild / organization     := "org.jboss.pnc.smeg"
ThisBuild / organizationName := "Project NCL"

ThisBuild / versionScheme := Some("semver-spec")

val versionPme = "4.16"

lazy val openTelemetrySpecific = {
  val version = "1.29.0"
  Seq(
    "io.opentelemetry" % "opentelemetry-bom" % version pomOnly(),
    "io.opentelemetry" % "opentelemetry-api" % version,
    "io.opentelemetry" % "opentelemetry-sdk" % version,
    "io.opentelemetry" % "opentelemetry-exporter-otlp" % version,
    "io.opentelemetry.javaagent" % "opentelemetry-javaagent" % version % "runtime"
  )
}

// Define local resolver to fix SBT 1.9.9 issue
val localRepoDir = file("project/hack/repository")
resolvers += "Local Repository" at s"file://${localRepoDir.getAbsolutePath}"

libraryDependencies += "org.jboss.da" % "reports-model" % "2.1.0"

// Define resolvers
resolvers ++= Seq(
  "RedHat GA" at "https://maven.repository.redhat.com/ga/",
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
  "Artima Maven" at "https://repo.artima.com/releases",
  "Central" at "https://repo1.maven.org/maven2"
)

// Define dependencies
libraryDependencies ++= Seq(
  "org.jboss.bom" % "eap-runtime-artifacts" % "7.3.1.GA",
  "com.softwaremill.sttp.client3" %% "core" % "3.3.5",
  "org.commonjava.maven.ext" % "pom-manipulation-core" % versionPme,
  "org.commonjava.maven.ext" % "pom-manipulation-io" % versionPme,
  "org.scalactic" %% "scalactic" % "3.2.9",
  "org.scalatest" %% "scalatest" % "3.2.9" % "test"
)
libraryDependencies ++= openTelemetrySpecific
//javaAgents += "io.opentelemetry.javaagent" % "opentelemetry-javaagent" % "1.29.0"
//javaOptions += "-Dotel.javaagent.debug=true" //Debug OpenTelemetry Java agent

excludeDependencies ++= Seq(
  ExclusionRule("org.apache.logging.log4j", "log4j-slf4j-impl"),
  ExclusionRule("commons-logging", "commons-logging")
)

// Exclude specific transitive dependencies globally (to solve SBT 1.9.9 issues)
excludeDependencies ++= Seq(
  ExclusionRule("org.jboss.da", "reports-model")
)

assembly / assemblyMergeStrategy := {
  case x if (x.endsWith("javax.inject.Named")) => MergeStrategy.first
  case x if (x.endsWith("beans.xml")) => MergeStrategy.first
  case x if (x.endsWith("module-info.class")) => MergeStrategy.discard
  case x =>
    val oldStrategy = (assembly / assemblyMergeStrategy).value
    oldStrategy(x)
}


lazy val root = (project in file("."))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "smeg-plugin",
    pluginCrossBuild / sbtVersion := {
      scalaBinaryVersion.value match {
        case "2.12" => "1.2.8"
      }
    }
  )

ThisBuild / publishMavenStyle := true
ThisBuild / credentials += Credentials(Path.userHome / ".sbt" / ".credentials")

ThisBuild / publishTo := {
  if (isSnapshot.value) {
    Some("Sonatype Nexus Repository Manager" at "https://repository.jboss.org/nexus/content/repositories/snapshots/")
  } else {
    None
  }
}


