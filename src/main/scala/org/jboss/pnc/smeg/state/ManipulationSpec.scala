package org.jboss.pnc.smeg.state

class ManipulationSpec {
  private val manipulations = Seq.newBuilder[String]

  /**
   * Sets the key in ThisBuild, for the projects inheriting it, and unscoped, for the root project: a project setting
   * (e.g. a plain `version := "1.0"` in build.sbt) wins over ThisBuild, while the unscoped one in manipulations.sbt,
   * loaded after the build definition, wins over it.
   */
  def overrideSetting(key: String, value: String): Unit = {
    manipulations += s"""ThisBuild / ${key} := \"${value}\""""
    manipulations += s"""${key} := \"${value}\""""
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
