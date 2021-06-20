package org.jboss.pnc.smeg

import sbt._
import Keys._
import org.jboss.pnc.smeg.manipulation.{Manipulator, ProjectVersionManipulations}
import org.jboss.pnc.smeg.state._
import org.jboss.pnc.smeg.util.PropFuncs._

object SmegPlugin extends AutoPlugin {

  override def trigger = allRequirements
  override lazy val buildSettings = Seq(commands += manipulate)

  type ManipulationTask = State => State


  lazy val manipulate = Command.command("manipulate") { (state: State) =>
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

    state
  }
}
