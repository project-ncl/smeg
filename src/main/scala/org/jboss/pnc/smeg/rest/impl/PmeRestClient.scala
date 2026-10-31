package org.jboss.pnc.smeg.rest.impl

import org.jboss.pnc.mavenmanipulator.io.rest.{DefaultTranslator, Translator}
import org.jboss.pnc.smeg.model.GAV
import org.jboss.pnc.smeg.rest.{RestClient, RestConfig}

import scala.collection.JavaConverters._

class PmeRestClient(translator: Translator) extends RestClient {

  override def lookupProjectVersion(gav: GAV): String = {
    lookupProjectVersions(Set(gav)).head._2
  }

  override def lookupProjectVersions(gavs: Set[GAV]): Map[GAV, String] = {
    translator.lookupProjectVersions(gavs.map(_.toProjectVersionRef).toList.asJava).asScala.map(v => GAV(v._1) -> v._2).toMap
  }
}

object PmeRestClient {
  def apply(restConfig: RestConfig): PmeRestClient = new PmeRestClient(translator(restConfig))

  def translator(restConfig: RestConfig): Translator = {
    new DefaultTranslator(
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
  }
}
