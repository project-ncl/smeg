package org.jboss.pnc.smeg.manipulation

import org.jboss.pnc.smeg.model.GAV
import org.jboss.pnc.smeg.rest.{RestClient, RestConfig}
import org.jboss.pnc.smeg.state.{ManipulationSession, ManipulationSpec}

object ProjectVersionManipulations {

  var restClient: RestClient = _

  def calculateAndSet(session: ManipulationSession, spec: ManipulationSpec): Unit = {

    val props = System.getProperties

    val calc = new SbtVersionCalculator

    val candidates = RESTLookup(Set(session.getRootProjectGav), session.restConfig).filterKeys(_ == session.getRootProjectGav).values

    val newVersion = calc.calculate(session.getRootProjectGav, props, candidates.toSet)

    spec.overrideSetting("version", newVersion)
  }

  protected def RESTLookup(gavs: Set[GAV], restConfig: RestConfig): Map[GAV, String] = {
    if (Option(restClient).isEmpty) {
      restClient = RestClient(restConfig)
    }

   restClient.lookupProjectVersions(gavs)
  }
}
