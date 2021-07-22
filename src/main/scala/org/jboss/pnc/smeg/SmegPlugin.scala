package org.jboss.pnc.smeg

import sbt._
import Keys._
import org.jboss.pnc.smeg.manipulation.{Manipulator, ProjectVersionManipulations}
import org.jboss.pnc.smeg.state.SmegSystemProperties.MANIPULATION_DISABLE
import org.jboss.pnc.smeg.state._
import org.jboss.pnc.smeg.util.PropFuncs._

import scala.language.postfixOps

object SmegPlugin extends AutoPlugin {

  override def trigger = allRequirements
  override lazy val buildSettings = Seq(commands += manipulate)
  //val manipulationDisabled: TaskKey[Boolean] = TaskKey[Boolean](SmegKeys.manipulationDisabledKey)


  lazy val manipulate = Command.command("manipulate") { (state: State) =>
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

    state
  }
}
