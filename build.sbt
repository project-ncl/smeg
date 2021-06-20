ThisBuild / scalaVersion     := "2.12.13"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "org.jboss.pnc.smeg"
ThisBuild / organizationName := "Project NCL"

val versionPme = "4.4-SNAPSHOT"

libraryDependencies ++= Seq(
  "com.softwaremill.sttp.client3" %% "core" % "3.3.5",
  "org.commonjava.maven.ext" % "pom-manipulation-core" % versionPme,
  "org.commonjava.maven.ext" % "pom-manipulation-io" % versionPme,
  "org.scalactic" %% "scalactic" % "3.2.9",
  "org.scalatest" %% "scalatest" % "3.2.9" % "test"
)

excludeDependencies ++= Seq(
  ExclusionRule("org.apache.logging.log4j", "log4j-slf4j-impl")
)

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
