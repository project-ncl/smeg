package org.jboss.pnc.smeg.rest

import org.commonjava.maven.ext.io.rest.Translator
import org.jboss.pnc.smeg.UnitSpec

import java.util.Properties

class RestConfigTest extends UnitSpec {

    private def fixture = new {
      val props = new Properties
    }

  behavior of "RestConfig"

  it should "set correct defaults when creating from empty properties " in {
    val f = fixture
    import f._

    val defaultRestConfig = RestConfig(
      restUrl = None,
      brewPullActive = false,
      mode = null,
      restMaxSize = -1,
      restMinSize = Translator.CHUNK_SPLIT_COUNT,
      restHeaders = Map[String, String](),
      restConnectionTimeout = Translator.DEFAULT_CONNECTION_TIMEOUT_SEC,
      restSocketRetry = Translator.DEFAULT_SOCKET_TIMEOUT_SEC,
      restRetryDuration = Translator.RETRY_DURATION_SEC
    )

    val restConfig = RestConfig(props)

    assert(restConfig == defaultRestConfig)
  }

  it should "set correct values when creating from properties" in {
    val f = fixture
    import RestConfig._
    import f._

    val expected = RestConfig(
      restUrl = Some("https://test-url.com"),
      brewPullActive = true,
      mode = "PERSISTENT",
      restMaxSize = 10,
      restMinSize = 5,
      restHeaders = Map[String, String](),
      restConnectionTimeout = 100,
      restSocketRetry = 20,
      restRetryDuration = 20
    )

    props.setProperty(REST_URL, "https://test-url.com")
    props.setProperty(REST_BREW_PULL_ACTIVE, "true")
    props.setProperty(REST_MODE, "PERSISTENT")
    props.setProperty(REST_MAX_SIZE, "10")
    props.setProperty(REST_MIN_SIZE, "5")
    props.setProperty(REST_CONNECTION_TIMEOUT_SEC, "100")
    props.setProperty(REST_SOCKET_TIMEOUT_SEC, "20")
    props.setProperty(REST_RETRY_DURATION_SEC, "20")

    val actual = RestConfig(props)

    assert(actual == expected)
  }

}
