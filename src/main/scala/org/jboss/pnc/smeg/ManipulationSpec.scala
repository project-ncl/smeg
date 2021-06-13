package org.jboss.pnc.smeg

class ManipulationSpec {
  private val manipulations = Seq.newBuilder[String]

  def overrideSetting(key: String, value: String): Unit = {
    manipulations += s"""ThisBuild / ${key} := \"${value}\""""
  }

  def toSeq: Seq[String] = {
      manipulations.result()
  }

//  private var _versionOverride: Option[String] = None
//
//  def versionOverride: Option[String] = _versionOverride
//  def versionOverride_= (version: String): Unit = {
//    _versionOverride = Option(version)
//  }

//  var _versionIncrementalSuffix: Option[String] = None
//
//  def versionIncrementalSuffix: Option[String] = _versionIncrementalSuffix
//  def versionIncrementalSuffix_= (suffix: String): Unit = {
//    versionIncrementalSuffix = suffix
//  }


}
