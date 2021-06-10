package org.jboss.pnc.smeg

import sbt._
import Keys._

object SmegPlugin extends AutoPlugin {
  override def trigger = allRequirements
}
