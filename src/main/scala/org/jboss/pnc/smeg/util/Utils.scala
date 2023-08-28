package org.jboss.pnc.smeg.util

object Utils {

  def parseStringToMap(inputString: String): Map[String, String] = {
    if (inputString == null)
      return null

    val keyValuePairs = inputString.split(",")

    keyValuePairs.foldLeft(Map.empty[String, String]) { (map, pair) =>
      val keyValue = pair.split(":")
      if (keyValue.length == 2) {
        val key = keyValue(0).trim()
        val value = keyValue(1).trim()
        map + (key -> value)
      } else {
        map
      }
    }
  }

}
