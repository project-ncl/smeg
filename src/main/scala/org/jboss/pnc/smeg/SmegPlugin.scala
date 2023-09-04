package org.jboss.pnc.smeg

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.sdk.trace.`export`.SpanExporter
import sbt._
import Keys._
import com.redhat.resilience.otel.OTelCLIHelper
import org.jboss.pnc.smeg.manipulation.{Manipulator, ProjectVersionManipulations}
import org.jboss.pnc.smeg.state.SmegSystemProperties.MANIPULATION_DISABLE
import org.jboss.pnc.smeg.state._
import org.jboss.pnc.smeg.util.PropFuncs._
import org.jboss.pnc.smeg.util.otel.NoopSpanExporter

import scala.language.postfixOps
import sbt.io.syntax.file

import scala.xml.XML


object SmegPlugin extends AutoPlugin {

  override def trigger = allRequirements
  override lazy val buildSettings = Seq(commands ++= Seq(manipulate, writeReport))

  lazy val manipulate = Command.command("manipulate") { (state: State) =>
    initOpenTelemetry(state)
    val span = GlobalOpenTelemetry
      .getTracer("pnc-smeg")
      .spanBuilder("SmegPlugin.manipulate")
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
    initOpenTelemetry(state)
    val span = GlobalOpenTelemetry
      .getTracer("pnc-smeg")
      .spanBuilder("SmegPlugin.writeReport")
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

  private def initOpenTelemetry(state: State): Unit = {
    if (!OTelCLIHelper.otelEnabled()) {
      state.log.info("Initializing otel ...")
      state.log.debug("Env.TRACEPARENT: " + sys.env.get("TRACEPARENT").getOrElse(None))
      state.log.debug("Env.TRACESTATE: " + sys.env.get("TRACESTATE").getOrElse(None))
      state.log.debug("Env.TRACE_ID: " + sys.env.get("TRACE_ID").getOrElse(None))
      state.log.debug("Env.SPAN_ID: " + sys.env.get("SPAN_ID").getOrElse(None))

      val grpcEndpoint: Option[String] = sys.props.get("OTEL_EXPORTER_OTLP_ENDPOINT")
        .orElse(sys.env.get("OTEL_EXPORTER_OTLP_ENDPOINT"))

      val spanProcessor = OTelCLIHelper.defaultSpanProcessor(getSpanExporter(grpcEndpoint))
      OTelCLIHelper.startOTel("pnc-smeg", spanProcessor)
    }
  }

  private def getSpanExporter(maybeGrpcEndpoint: Option[String]): SpanExporter = {
    maybeGrpcEndpoint match {
      case Some(endpoint) => OTelCLIHelper.defaultSpanExporter(endpoint)
      case None => NoopSpanExporter.getInstance()
    }
  }


}
