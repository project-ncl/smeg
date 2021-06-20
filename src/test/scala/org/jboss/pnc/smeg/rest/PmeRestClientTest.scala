package org.jboss.pnc.smeg.rest

import org.jboss.pnc.smeg.UnitSpec
import org.jboss.pnc.smeg.manipulation.SbtVersionCalculator
import org.jboss.pnc.smeg.model.GAV
import org.jboss.pnc.smeg.rest.RestConfig.{REST_MODE, REST_URL}

import java.util.Properties

class PmeRestClientTest extends UnitSpec {

  private def fixture = new {
    val props = new Properties
    val calc = new SbtVersionCalculator
  }

  behavior of "PmeRestClient"

  ignore should "work" in {
    val f = fixture
    import f._

    val gav = GAV("com.google.guava", "guava", "19.0")

    props.setProperty(REST_URL, "http://da-master-devel.psi.redhat.com/da/rest/v-1/")
    props.setProperty(REST_MODE, "PERSISTENT")
    props.setProperty("versionIncrementalSuffix", "redhat")

    val config = RestConfig(props)
    val restClient = RestClient(config)

    val latestVersion = restClient.lookupProjectVersion(gav)

    val newVersion = calc.calculate(gav, props, Set(latestVersion))

    println(newVersion)
  }
}
