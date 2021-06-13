ThisBuild / scalaVersion     := "2.12.13"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "org.jboss.pnc.smeg"
ThisBuild / organizationName := "Project NCL"

libraryDependencies ++= Seq(
  "com.softwaremill.sttp.client3" %% "core" % "3.3.5",
  "org.scalactic" %% "scalactic" % "3.2.9",
  "org.scalatest" %% "scalatest" % "3.2.9" % "test"
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
