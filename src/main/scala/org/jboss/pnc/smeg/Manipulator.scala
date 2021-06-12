package org.jboss.pnc.smeg

import sbt.io.IO
import sbt.io.syntax.file

object Manipulator {

  def writeManipulationSpec(spec: ManipulationSpec): Unit = {
    IO.writeLines(file("manipulations.sbt"), spec.toSeq)
//    val version = spec.versionOverride.get
//    IO.writeLines(file("manipulations.sbt"), Seq(s"""ThisBuild / version := \"${version}\""""))
  }

}
