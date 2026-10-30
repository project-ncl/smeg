import sbt.Keys.versionScheme
import sbt._

ThisBuild / scalaVersion     := "2.12.13"
ThisBuild / version          := "0.3.1.0-SNAPSHOT"
ThisBuild / organization     := "org.jboss.pnc.smeg"
ThisBuild / organizationName := "Project NCL"

ThisBuild / versionScheme := Some("semver-spec")

val versionPme = "5.4"

lazy val openTelemetrySpecific = {
  val version = "1.29.0"
  Seq(
    "io.opentelemetry" % "opentelemetry-bom" % version pomOnly(),
    "io.opentelemetry" % "opentelemetry-api" % version,
    "io.opentelemetry" % "opentelemetry-sdk" % version,
    "io.opentelemetry" % "opentelemetry-exporter-otlp" % version
  )
}

// Define local resolver to fix SBT 1.9.9 issue
val localRepoDir = file("project/hack/repository")
resolvers += "Local Repository" at s"file://${localRepoDir.getAbsolutePath}"

libraryDependencies += "org.jboss.da" % "reports-model" % "3.0.8"

// Define resolvers
resolvers ++= Seq(
  "RedHat GA" at "https://maven.repository.redhat.com/ga/",
  "Sonatype Snapshots" at "https://central.sonatype.com/repository/maven-snapshots",
  "Artima Maven" at "https://repo.artima.com/releases",
  "Central" at "https://repo1.maven.org/maven2"
)

// Define dependencies
libraryDependencies ++= Seq(
  "org.jboss.bom" % "eap-runtime-artifacts" % "7.3.1.GA" pomOnly(),
  "com.softwaremill.sttp.client3" %% "core" % "3.11.0",
  "org.jboss.pnc.maven-manipulator" % "pom-manipulation-core" % versionPme,
  "org.jboss.pnc.maven-manipulator" % "pom-manipulation-io" % versionPme,
  // used directly by GroovyScripts; same Groovy line as PME (Groovy 4 would clash with it on groovy.lang.*)
  "org.codehaus.groovy" % "groovy" % "3.0.25",
  // commons-logging API for PME's REST client (httpclient), routed to slf4j: it used to come from Galley,
  // which is excluded below, and commons-logging itself is excluded as it breaks on sbt's older log4j
  "org.slf4j" % "jcl-over-slf4j" % "2.0.18",
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
    None
  }
}


