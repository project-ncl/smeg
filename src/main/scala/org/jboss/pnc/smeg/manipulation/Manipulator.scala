package org.jboss.pnc.smeg.manipulation

import org.jboss.pnc.smeg.state.ManipulationSpec
import sbt.io.IO
import sbt.io.syntax.file

object Manipulator {

  def writeManipulationSpec(spec: ManipulationSpec): Unit = {
    IO.writeLines(file("manipulations.sbt"), spec.toSeq)
  }

}
