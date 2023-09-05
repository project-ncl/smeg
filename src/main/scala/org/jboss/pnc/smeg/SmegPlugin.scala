package org.jboss.pnc.smeg

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.sdk.trace.`export`.SpanExporter
import io.opentelemetry.sdk.trace.SpanProcessor
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

import java.util.concurrent.TimeUnit
import scala.xml.XML


object SmegPlugin extends AutoPlugin {

  val OTEL_TRACKER_NAME = "pnc-smeg"
  override def trigger = allRequirements
  override lazy val buildSettings = Seq(commands ++= Seq(manipulate, writeReport))
  var spanProcessor: SpanProcessor = null

  lazy val manipulate = Command.command("manipulate") { (state: State) =>
    val span = startSpan(state, "SmegPlugin.manipulate")
    try {
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
      // make sure data is flushed before command exits
      spanProcessor.forceFlush().join(10, TimeUnit.SECONDS)
    }
  }

  lazy val writeReport = Command.command("writeReport") { (state: State) =>
    val span = startSpan(state, "SmegPlugin.writeReport")
    try {
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
      // make sure data is flushed before command exits
      spanProcessor.forceFlush().join(10, TimeUnit.SECONDS)
    }
  }

  private def startSpan(state: State, commandName: String): Span = {
    if (!OTelCLIHelper.otelEnabled()) {
      state.log.info("Initializing otel ...")
      state.log.info("Env.TRACEPARENT: " + sys.env.get("TRACEPARENT").getOrElse(None))
      state.log.info("Env.TRACESTATE: " + sys.env.get("TRACESTATE").getOrElse(None))
      state.log.info("Env.TRACE_ID: " + sys.env.get("TRACE_ID").getOrElse(None))
      state.log.info("Env.SPAN_ID: " + sys.env.get("SPAN_ID").getOrElse(None))

      val grpcEndpoint: Option[String] = sys.props.get("OTEL_EXPORTER_OTLP_ENDPOINT")
        .orElse(sys.env.get("OTEL_EXPORTER_OTLP_ENDPOINT"))

      val spanProcessor = OTelCLIHelper.defaultSpanProcessor(getSpanExporter(grpcEndpoint))
      this.spanProcessor = spanProcessor;
      OTelCLIHelper.startOTel(OTEL_TRACKER_NAME, commandName, spanProcessor)
    }

    if (!Span.current().isRecording) {
      val span = GlobalOpenTelemetry
        .getTracer(OTEL_TRACKER_NAME)
        .spanBuilder(commandName)
        .startSpan()
      span.makeCurrent()
    }
    Span.current()
  }

  private def getSpanExporter(maybeGrpcEndpoint: Option[String]): SpanExporter = {
    maybeGrpcEndpoint match {
      case Some(endpoint) => OTelCLIHelper.defaultSpanExporter(endpoint)
      case None => NoopSpanExporter.getInstance()
    }
  }


}
