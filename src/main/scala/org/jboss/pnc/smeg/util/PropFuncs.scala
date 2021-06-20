package org.jboss.pnc.smeg.util

import java.util.Properties
import scala.collection.Map
import scala.collection.convert.ImplicitConversions.`properties AsScalaMap`

object PropFuncs {

  val mapOverloadedProps: (String, Properties) => Map[String, String] = (keyPrefix: String, props: Properties) => {
    props
        .filterKeys(_.startsWith(s"${keyPrefix}."))
        .map(kv => kv._1.substring(kv._1.indexOf(".") + 1) -> kv._2)
  }

  val mapOverloadedSysProps: String => Map[String, String] = (keyPrefix: String) => mapOverloadedProps(keyPrefix, System.getProperties)
}
