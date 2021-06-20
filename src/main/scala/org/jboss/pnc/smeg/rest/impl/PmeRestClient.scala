package org.jboss.pnc.smeg.rest.impl

import org.commonjava.maven.ext.io.rest.{DefaultTranslator, Translator}
import org.jboss.pnc.smeg.model.GAV
import org.jboss.pnc.smeg.rest.{RestClient, RestConfig}

import scala.collection.JavaConverters._

class PmeRestClient(translator: Translator) extends RestClient {


  //  def apply(config: RestConfig): Translator = {
  //    import config._
  //    new DefaultTranslator(
  //      restUrl.get,
  //      restMaxSize,
  //      restMinSize,
  //      brewPullActive,
  //      mode,
  //      getRestHeadersAsJavaMap,
  //      restConnectionTimeout,
  //      restSocketRetry,
  //      restRetryDuration
  //    )
  //  }

  //  def getClient(): DefaultTranslator = {
  //    val restUrl = "http://da-master-devel.psi.redhat.com/da/rest/v-1/"
  //    val brewPullActive = false
  //    val mode = "PERSISTENT"
  //    val restMaxSize = -1
  //    val restMinSize = Translator.CHUNK_SPLIT_COUNT
  //    val restHeaders = mapAsJavaMap(Map[String,String]()) //Map.empty
  //    val restConnectionTimeout = Translator.DEFAULT_CONNECTION_TIMEOUT_SEC
  //    val restSocketTimeout = Translator.DEFAULT_SOCKET_TIMEOUT_SEC
  //    val restRetryDuration = Translator.RETRY_DURATION_SEC
  //
  //
  //    val restEndpoint = new DefaultTranslator(
  //      restUrl,
  //      restMaxSize,
  //      restMinSize,
  //      brewPullActive,
  //      mode,
  //      restHeaders,
  //      restConnectionTimeout,
  //      restSocketTimeout,
  //      restRetryDuration)
  //
  //
  //    restEndpoint
  //  }
  //    new DefaultTranslator(
  //      configuration.daEndpoint(),
  //      configuration.restMaxSize(),
  //      Translator.CHUNK_SPLIT_COUNT,
  //      configuration.restRepositoryGroup(),
  //      configuration.restBrewPullActive(),
  //      configuration.restMode(),
  //      configuration.versionIncrementalSuffix(),
  //      configuration.restHeaders(),
  //      configuration.restConnectionTimeout(),
  //      configuration.restSocketTimeout(),
  //      configuration.restRetryDuration());
  //  }

  override def lookupProjectVersion(gav: GAV): String = {
    lookupProjectVersions(Set(gav)).head._2
  }

  override def lookupProjectVersions(gavs: Set[GAV]): Map[GAV, String] = {
    translator.lookupProjectVersions(gavs.map(_.toProjectVersionRef).toList.asJava).asScala.map(v => GAV(v._1) -> v._2).toMap
  }
}

object PmeRestClient {
  def apply(restConfig: RestConfig): PmeRestClient = {
    val translator: Translator = new DefaultTranslator(
      restConfig.restUrl.get,
      restConfig.restMaxSize,
      restConfig.restMinSize,
      restConfig.brewPullActive,
      restConfig.mode,
      restConfig.getRestHeadersAsJavaMap,
      restConfig.restConnectionTimeout,
      restConfig.restSocketRetry,
      restConfig.restRetryDuration
    )
    new PmeRestClient(translator)
  }
}
