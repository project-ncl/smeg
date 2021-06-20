package org.jboss.pnc.smeg.util

import org.commonjava.maven.ext.core.impl.Version
import scala.collection.JavaConverters.setAsJavaSet

object Versions {

  val formatVersion: (String, String) => String = (version: String, suffix: String) => {
    s"${version}.${suffix}"
  }

  def getBuildNumberPadding(version: String, incrementalSerialSuffixPadding: Int = 0): Int = {
    Version.getBuildNumberPadding(incrementalSerialSuffixPadding, setAsJavaSet(Set(version)))
  }

  def padInt(toPad: Int, padding: Int): String = {
    s"%0${padding}d".format(toPad)
  }

  def setBuildNumberPadded(version: String, buildNumber: Int, padding: Int): String = {
    Version.setBuildNumber(version, padInt(buildNumber, padding))
  }

  def incrementBuildNumber(version: String): String = {
    incrementBuildNumber(version, getBuildNumberPadding(version))
  }

  def incrementBuildNumber(version: String, padding: Int): String = {
    setBuildNumberPadded(version, Version.getIntegerBuildNumber(version) +1, padding)
  }

  def appendQualifierSuffix(version: String, suffix: String): String = Version.appendQualifierSuffix(version, suffix)

}
