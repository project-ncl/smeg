package org.jboss.pnc.smeg.rest.impl

import org.jboss.pnc.smeg.model.GAV
import org.jboss.pnc.smeg.rest.RestClient

class NoOpRestClient extends RestClient {
  override def lookupProjectVersion(gav: GAV): String = ???

  override def lookupProjectVersions(gavs: Set[GAV]): Map[GAV, String] = Map[GAV, String]()
}
