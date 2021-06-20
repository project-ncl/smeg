package org.jboss.pnc.smeg.util

import org.jboss.pnc.smeg.UnitSpec

class VersionsTest extends UnitSpec {

  "Versions.incrementBuildNumber" should "correctly increment the build number" in {
    val version = "1.1.1.rebuild-1"

    val result = Versions.incrementBuildNumber(version)

    assert(result == "1.1.1.rebuild-2")
  }

  "Versions.incrementBuildNumber" should "correctly increment a padded build number" in {
    val version = "1.1.1.rebuild-000001"

    val result = Versions.incrementBuildNumber(version)

    assert(result == "1.1.1.rebuild-000002")
  }

  "Versions.incrementBuildNumber" should "correctly increment a padded build number when a forced padding value is applied" in {
    val version = "1.1.1.rebuild-000001"

    val result = Versions.incrementBuildNumber(version, 3)

    assert(result == "1.1.1.rebuild-002")
  }


}
