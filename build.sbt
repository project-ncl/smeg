import sbt.Keys.versionScheme

ThisBuild / scalaVersion     := "2.12.13"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "org.jboss.pnc.smeg"
ThisBuild / organizationName := "Project NCL"

ThisBuild / versionScheme := Some("semver-spec")

val versionPme = "4.4"

libraryDependencies ++= Seq(
  "com.softwaremill.sttp.client3" %% "core" % "3.3.5",
  "org.commonjava.maven.ext" % "pom-manipulation-core" % versionPme,
  "org.commonjava.maven.ext" % "pom-manipulation-io" % versionPme,
  "org.scalactic" %% "scalactic" % "3.2.9",
  "org.scalatest" %% "scalatest" % "3.2.9" % "test"
)

excludeDependencies ++= Seq(
  ExclusionRule("org.apache.logging.log4j", "log4j-slf4j-impl"),
  ExclusionRule("commons-logging", "commons-logging")
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


