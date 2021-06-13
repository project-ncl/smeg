package org.jboss.pnc.smeg

import sbt._
import Keys._
import Utils._

object SmegPlugin extends AutoPlugin {
  override def trigger = allRequirements
  override lazy val buildSettings = Seq(commands += manipulate)

  lazy val manipulate = Command.command("manipulate") { (state: State) =>
    val manipulations = new ManipulationSpec
    val extracted = Project.extract(state)

    /*
     * SBT Setting Override
     */
    mapOverloadedSysProps("overrideSetting") foreach (x => manipulations.overrideSetting(x._1, x._2))

    /*
     * Version Override
     */
    val versionOverride = sys.props.get("versionOverride")
    val suffix = sys.props.get("versionSuffix")
    var version: String = null
    if (versionOverride.isDefined) {
      if (suffix.isDefined) {
        version = formatVersion(versionOverride.get, suffix.get)
      } else {
        version = versionOverride.get
      }
    } else if (suffix.isDefined) {
      version = formatVersion(extracted.get(Keys.version), suffix.get)
    }
    manipulations.overrideSetting("version", version)

    /*
     * Apply manipulations
     */
    Manipulator.writeManipulationSpec(manipulations)

    state
  }
}
