package org.jboss.pnc.smeg

import collection.Map

object Utils {

  val mapOverloadedSysProps: String => Map[String, String] = (baseKey: String) => {
    sys.props
      .filterKeys(_.startsWith(s"${baseKey}."))
      .map(kv => kv._1.substring(kv._1.indexOf(".") + 1) -> kv._2)
  }

  val formatVersion: (String, String) => String = (version: String, suffix: String) => {
    s"${version}.${suffix}"
  }

}
