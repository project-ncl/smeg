package org.jboss.pnc.smeg

import io.opentelemetry.api.{GlobalOpenTelemetry, OpenTelemetry}
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.TextMapGetter
import sbt._
import Keys._
import org.jboss.pnc.smeg.manipulation.{Manipulator, ProjectVersionManipulations}
import org.jboss.pnc.smeg.state.SmegSystemProperties.MANIPULATION_DISABLE
import org.jboss.pnc.smeg.state._
import org.jboss.pnc.smeg.util.PropFuncs._
import org.jboss.pnc.smeg.util.Utils

import scala.language.postfixOps
import sbt.io.syntax.file

import java.lang
import scala.xml.XML

import scala.collection.JavaConverters._

object SmegPlugin extends AutoPlugin {

  private lazy val otel: OpenTelemetry = GlobalOpenTelemetry.get()
  private lazy val tracer: Tracer = otel.getTracer("smeg")

  override def trigger = allRequirements
  override lazy val buildSettings = Seq(commands ++= Seq(manipulate, writeReport))

  lazy val manipulate = Command.command("manipulate") { (state: State) =>

    val mdc = Utils.parseStringToMap(System.getProperty("restHeaders"))
    val span = tracer.spanBuilder("manipulate").setParent(extractInvokerContext(mdc)).startSpan()

    try {
      val ss = span.makeCurrent()

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
      span.end()
    }
  }

  lazy val writeReport = Command.command("writeReport") { (state: State) =>

    val mdc = Utils.parseStringToMap(System.getProperty("restHeaders"))
    val span = tracer.spanBuilder("writeReport").setParent(extractInvokerContext(mdc)).startSpan()

    try {
      val ss = span.makeCurrent()

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
      span.end()
    }
  }

  private def extractInvokerContext(mdc: Map[String, String]): Context = {
    val getter = new TextMapGetter[Map[String, String]]() {
      override def get(carrier: Map[String, String], key: String): String = carrier.getOrElse(key, null)
      override def keys(carrier: Map[String, String]): lang.Iterable[String] = asJavaIterable(carrier.keys)
    }
    otel
      .getPropagators()
      .getTextMapPropagator()
      .extract(Context.current(), mdc, getter)
  }
}
