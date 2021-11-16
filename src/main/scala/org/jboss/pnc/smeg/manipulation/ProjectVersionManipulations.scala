package org.jboss.pnc.smeg.manipulation

import org.jboss.pnc.smeg.model.GAV
import org.jboss.pnc.smeg.rest.{RestClient, RestConfig}
import org.jboss.pnc.smeg.state.SmegSystemProperties.{FORCE_ROOT_GA, VERSION_SUFFIX_SETTING}
import org.jboss.pnc.smeg.state.{ManipulationSession, ManipulationSpec}
import org.jboss.pnc.smeg.util.Versions

object ProjectVersionManipulations {

  var restClient: RestClient = _

  def calculateAndSet(session: ManipulationSession, spec: ManipulationSpec): Unit = {

    val props = System.getProperties

    val calc = new SbtVersionCalculator

    val candidates = RESTLookup(Set(session.getRootProjectGav), session.restConfig).filterKeys(_ == session.getRootProjectGav).values

    val nextVersion = calc.calculate(session.getRootProjectGav, props, candidates.toSet)

    sys.props.get(VERSION_SUFFIX_SETTING) match {
      case Some(x) =>
        val vsplit = Versions.splitQualifier(nextVersion)
        spec.overrideSetting(session.settingTranspositions.getOrElse("version", "version"), vsplit._1)
        spec.overrideSetting(sys.props(VERSION_SUFFIX_SETTING), vsplit._2)
      case None =>
        spec.overrideSetting(session.settingTranspositions.getOrElse("version", "version"), nextVersion)
    }
  }

  protected def RESTLookup(gavs: Set[GAV], restConfig: RestConfig): Map[GAV, String] = {
    if (Option(restClient).isEmpty) {
      restClient = RestClient(restConfig)
    }

   restClient.lookupProjectVersions(gavs)
  }
}
