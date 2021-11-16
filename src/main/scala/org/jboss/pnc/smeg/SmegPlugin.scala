package org.jboss.pnc.smeg

import sbt._
import Keys._
import org.jboss.pnc.smeg.manipulation.{Manipulator, ProjectVersionManipulations}
import org.jboss.pnc.smeg.state.SmegSystemProperties.MANIPULATION_DISABLE
import org.jboss.pnc.smeg.state._
import org.jboss.pnc.smeg.util.PropFuncs._

import scala.language.postfixOps
import sbt.io.syntax.file

import scala.xml.XML


object SmegPlugin extends AutoPlugin {

  override def trigger = allRequirements
  override lazy val buildSettings = Seq(commands ++= Seq(manipulate, writeReport))

  lazy val manipulate = Command.command("manipulate") { (state: State) =>
    state.log.info("Smeg manipulations")
    if (!sys.props.getOrElse(MANIPULATION_DISABLE, "false").toBoolean) {

      val session = new ManipulationSession(state)
      val manipulations = new ManipulationSpec

      /*
       * SBT Setting Override
       */
      mapOverloadedSysProps("overrideSetting") foreach (x => manipulations.overrideSetting(x._1, x._2))

      /*
       * Project Version override
       */
      ProjectVersionManipulations.calculateAndSet(session, manipulations)

      /*
       * Apply manipulations
       */
      Manipulator.writeManipulationSpec(manipulations)
    }
    Command.process("reload", state)
  }

  lazy val writeReport = Command.command("writeReport") { (state: State) =>
    val result = Project.runTask(Keys.makePom, state)

    val pomPath = result.get._2.toEither.getOrElse(throw new Exception("Path to POM could not be extracted"))

    val xmlPom = XML.loadFile(pomPath)

//    val groupId = (xmlPom \ "groupId").text
//    val artifactId = (xmlPom \ "artifactId").text
    val version = (xmlPom \ "version").text

    val session = new ManipulationSession(state)

    val rootGav = session.getRootProjectGav

    val json = s"""
         |{
         |  "VersioningState": {
         |    "executionRootModified": {
         |      "groupId": "${rootGav.groupId}",
         |      "artifactId": "${rootGav.artifactId}",
         |      "version": "${version}"
         |    }
         |  },
         |  "RemovedRepositories": []
         |}
         |""".stripMargin.trim

    IO.write(file("manipulations.json"), json)
    state
  }
}
