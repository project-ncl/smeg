package org.jboss.pnc.smeg

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.context.Context
import sbt._
import Keys._
import org.jboss.pnc.smeg.manipulation.{Manipulator, ProjectVersionManipulations}
import org.jboss.pnc.smeg.state.SmegSystemProperties.MANIPULATION_DISABLE
import org.jboss.pnc.smeg.state._
import org.jboss.pnc.smeg.util.PropFuncs._
import org.jboss.pnc.smeg.util.{W3cParentContextExtractor, Utils}

import scala.language.postfixOps
import sbt.io.syntax.file

import scala.xml.XML
import scala.collection.JavaConverters._


object SmegPlugin extends AutoPlugin {

  private val propagatedContext: Context = getParentContext()

  override def trigger = allRequirements
  override lazy val buildSettings = Seq(commands ++= Seq(manipulate, writeReport))

  lazy val manipulate = Command.command("manipulate") { (state: State) =>
    val span = GlobalOpenTelemetry
      .getTracer("pnc-smeg")
      .spanBuilder("SmegPlugin.manipulate")
      .setParent(propagatedContext)
      .startSpan()
    try {
      span.makeCurrent
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
    } finally {
      span.end
    }
  }

  lazy val writeReport = Command.command("writeReport") { (state: State) =>
    val span = GlobalOpenTelemetry
      .getTracer("pnc-smeg")
      .spanBuilder("SmegPlugin.writeReport")
      .setParent(propagatedContext)
      .startSpan()
    try {
      span.makeCurrent
      state.log.info("Smeg writeReport")

      val result = Project.runTask(Keys.makePom, state)

      val pomPath = result.get._2.toEither.getOrElse(throw new Exception("Path to POM could not be extracted"))

      val xmlPom = XML.loadFile(pomPath)

      //    val groupId = (xmlPom \ "groupId").text
      //    val artifactId = (xmlPom \ "artifactId").text
      val version = (xmlPom \ "version").text

      val session = new ManipulationSession(state)

      val rootGav = session.getRootProjectGav

      val json =
        s"""
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
    } finally {
      span.end
    }
  }

  private def getParentContext(): Context = {
    val mdc = Utils.parseStringToMap(sys.props("restHeaders"))
    new W3cParentContextExtractor().startOTel(mdc.asJava);
  }

}
