import sbt.Keys.versionScheme
import sbt._

ThisBuild / scalaVersion     := "2.12.13"
ThisBuild / organization     := "org.jboss.pnc.smeg"
ThisBuild / organizationName := "Project NCL"

ThisBuild / versionScheme := Some("semver-spec")

val versionPme = "5.10"

lazy val openTelemetrySpecific = {
  val version = "1.29.0"
  Seq(
    "io.opentelemetry" % "opentelemetry-bom" % version pomOnly(),
    "io.opentelemetry" % "opentelemetry-api" % version,
    "io.opentelemetry" % "opentelemetry-sdk" % version,
    "io.opentelemetry" % "opentelemetry-exporter-otlp" % version
  )
}

// Define resolvers
resolvers ++= Seq(
  "RedHat GA" at "https://maven.repository.redhat.com/ga/",
  "Sonatype Snapshots" at "https://central.sonatype.com/repository/maven-snapshots",
  "Artima Maven" at "https://repo.artima.com/releases",
  "Central" at "https://repo1.maven.org/maven2"
)

// Define dependencies
libraryDependencies ++= Seq(
  "com.softwaremill.sttp.client3" %% "core" % "3.11.0",
  "org.jboss.pnc.maven-manipulator" % "pom-manipulation-core" % versionPme,
  "org.jboss.pnc.maven-manipulator" % "pom-manipulation-io" % versionPme,
  // used directly by GroovyScripts; same Groovy line as PME (Groovy 4 would clash with it on groovy.lang.*)
  "org.codehaus.groovy" % "groovy" % "3.0.25",
  // commons-logging API for PME's REST client (httpclient), routed to slf4j: it used to come from Galley,
  // which is excluded below, and commons-logging itself is excluded as it breaks on sbt's older log4j
  "org.slf4j" % "jcl-over-slf4j" % "2.0.20",
  // overrides the 2.22.3 from PME for CVE-2026-110879; a direct dependency, unlike dependencyOverrides, is in the POM
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.23.0",
  "org.scalactic" %% "scalactic" % "3.2.20",
  "org.scalatest" %% "scalatest" % "3.2.20" % "test"
)
libraryDependencies ++= openTelemetrySpecific

excludeDependencies ++= Seq(
  ExclusionRule("org.apache.logging.log4j", "log4j-slf4j-impl"),
  ExclusionRule("commons-logging", "commons-logging"),
  // Java 8 incompatible deps coming from PME
  ExclusionRule("ch.qos.logback", "logback-classic"),
  ExclusionRule("ch.qos.logback", "logback-core"),
  ExclusionRule("org.commonjava.maven.galley")
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

// POM needed by Mend SCA
makePom / artifactPath := baseDirectory.value / "pom.xml"
pomIncludeRepository := { _ => false }
pomExtra :=
  <repositories>
    <repository>
      <id>RedHat GA</id>
      <url>https://maven.repository.redhat.com/ga/</url>
      <releases><enabled>true</enabled></releases>
      <snapshots><enabled>false</enabled></snapshots>
    </repository>
    <repository>
      <id>Sonatype Snapshots</id>
      <url>https://central.sonatype.com/repository/maven-snapshots</url>
      <releases><enabled>false</enabled></releases>
      <snapshots><enabled>true</enabled></snapshots>
    </repository>
  </repositories>

ThisBuild / publishTo := {
  if (isSnapshot.value) {
    Some("Sonatype Nexus Repository Manager" at "https://central.sonatype.com/repository/maven-snapshots/")
  } else {
    // staged locally, then uploaded to the Sonatype Central Portal by sonaRelease
    localStaging.value
  }
}

// Metadata required by Maven Central
ThisBuild / description := "SBT manipulation tool in the style of PME"
ThisBuild / homepage := Some(url("https://github.com/project-ncl/smeg"))
ThisBuild / licenses := List("Apache License, Version 2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.html"))
ThisBuild / scmInfo := Some(ScmInfo(url("https://github.com/project-ncl/smeg"), "scm:git:git@github.com:project-ncl/smeg.git"))
ThisBuild / developers := List(
  Developer("project-ncl", "Project NCL", "pnc-bot-release@redhat.com", url("https://github.com/project-ncl"))
)

// tags without the "v" prefix of sbt-release, like the previous releases
releaseTagName := (ThisBuild / version).value

// `sbt "release with-defaults"` (see .github/workflows/sbt-release.yml): releases the version of version.sbt without
// -SNAPSHOT, then sets the next snapshot version (e.g. 0.4.0.0 -> 0.4.0.1-SNAPSHOT)
releaseProcess := {
  import sbtrelease.ReleaseStateTransformations._
  Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion, // its check, run before any step, aborts on uncommitted changes
    tagRelease,
    releaseStepCommand("publishSigned"),
    releaseStepCommand("sonaRelease"),
    setNextVersion,
    commitNextVersion,
    pushChanges
  )
}


