package org.jboss.pnc.smeg

import org.scalatest._

class UtilsTest extends UnitSpec {
  "Utils.formatVersion" should "correctly format a version with suffix" in {
    val base = "1.1.1"
    val suffix = "redhat-1"

    val version = Utils.formatVersion(base, suffix)

    assert(version == "1.1.1.redhat-1")
  }
}
