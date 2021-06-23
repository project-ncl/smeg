package org.jboss.pnc.smeg.rest

import org.commonjava.maven.ext.core.state.RESTState
import org.commonjava.maven.ext.io.rest.Translator

import java.util.Properties
import scala.collection.JavaConverters._
import scala.util.Try

case class RestConfig(
                       restUrl: Option[String],
                       brewPullActive: Boolean,
                       mode: String,
                       restMaxSize: Int,
                       restMinSize: Int,
                       restHeaders: Map[String,String],
                       restConnectionTimeout: Int,
                       restSocketRetry: Int,
                       restRetryDuration: Int
                     ) {

  def restLookupEnabled: Boolean = restUrl.isDefined
  def getRestHeadersAsJavaMap: java.util.Map[String, String] = mapAsJavaMap(restHeaders)

}

object RestConfig {
  val REST_URL: String = RESTState.REST_URL
  val REST_BREW_PULL_ACTIVE: String = RESTState.REST_BREW_PULL_ACTIVE
  val REST_MODE: String = RESTState.REST_MODE
  val REST_MAX_SIZE: String = RESTState.REST_MAX_SIZE
  val REST_MIN_SIZE: String = RESTState.REST_MIN_SIZE
  val REST_HEADERS: String = RESTState.REST_HEADERS
  val REST_CONNECTION_TIMEOUT_SEC: String = RESTState.REST_CONNECTION_TIMEOUT_SEC
  val REST_SOCKET_TIMEOUT_SEC: String = RESTState.REST_SOCKET_TIMEOUT_SEC
  val REST_RETRY_DURATION_SEC: String = RESTState.REST_RETRY_DURATION_SEC

  def apply(props: Properties): RestConfig = {
    RestConfig(
      restUrl = Option(props.getProperty(REST_URL)),
      brewPullActive = props.getProperty(REST_BREW_PULL_ACTIVE, "false").toBoolean,
      mode = props.getProperty(REST_MODE),
      restMaxSize = props.getProperty(REST_MAX_SIZE, "-1").toInt,
      restMinSize = Try(props.getProperty(REST_MIN_SIZE).toInt).getOrElse(Translator.CHUNK_SPLIT_COUNT),
      restHeaders = RESTState.restHeaderParser(props.getProperty(REST_HEADERS, "")).asScala.toMap,
      restConnectionTimeout = Try(props.getProperty(REST_CONNECTION_TIMEOUT_SEC).toInt).getOrElse(Translator.DEFAULT_CONNECTION_TIMEOUT_SEC),
      restSocketRetry = Try(props.getProperty(REST_SOCKET_TIMEOUT_SEC).toInt).getOrElse(Translator.DEFAULT_SOCKET_TIMEOUT_SEC),
      restRetryDuration = Try(props.getProperty(REST_RETRY_DURATION_SEC).toInt).getOrElse(Translator.RETRY_DURATION_SEC)
    )
  }
}