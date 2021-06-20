package org.jboss.pnc.smeg.state

import org.jboss.pnc.smeg.model.GAV
import org.jboss.pnc.smeg.rest.RestConfig
import sbt.{Keys, Project, State}

class ManipulationSession(val sbtState: State) {

  val extractedState = Project.extract(sbtState)

  val restConfig: RestConfig = RestConfig(System.getProperties)

  def getRootProjectGav: GAV = {
    GAV(extractedState.get(Keys.organization), extractedState.get(Keys.name), extractedState.get(Keys.version))
  }

}
