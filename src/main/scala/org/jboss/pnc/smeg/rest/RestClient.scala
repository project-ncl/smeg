package org.jboss.pnc.smeg.rest

import org.jboss.pnc.smeg.model.GAV
import org.jboss.pnc.smeg.rest.impl.{NoOpRestClient, PmeRestClient}

trait RestClient {

  def lookupProjectVersion(gav: GAV): String

  def lookupProjectVersions(gavs: Set[GAV]): Map[GAV, String]
}

object RestClient {
  def apply(restConfig: RestConfig): RestClient = restConfig.restUrl match {
     case Some(_) => PmeRestClient(restConfig)
     case None => new NoOpRestClient
    }
}
