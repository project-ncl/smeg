package org.jboss.pnc.smeg.state

import org.jboss.pnc.smeg.model.GAV
import org.jboss.pnc.smeg.rest.RestConfig
import org.jboss.pnc.smeg.util.ManipulationException
import sbt.internal.util.AttributeKey
import sbt.{Keys, Logger, Project, SettingKey, State, TaskKey}
import SmegSystemProperties._
import org.jboss.pnc.smeg.util.PropFuncs.mapOverloadedSysProps

import scala.collection.Map

class ManipulationSession(val sbtState: State) {

  val extractedState = Project.extract(sbtState)

  val restConfig: RestConfig = RestConfig(System.getProperties)

  val settingTranspositions: Map[String, String] = mapOverloadedSysProps(SETTING_TRANSPOSITIONS)

  def getRootProjectGav: GAV = {
    val gav = getRootProjectGav(Keys.organization, Keys.name, Keys.version)

    if (sys.props.contains(FORCE_ROOT_GA)) {
      val ga = sys.props(FORCE_ROOT_GA).split(':');
      GAV(ga(0),ga(1), gav.version)
    } else {
      gav
    }
  }

  def getLogger: Logger = sbtState.log

  private def getRootProjectGav(groupIdKey: SettingKey[String], artifactIdKey: SettingKey[String], versionKey: SettingKey[String]): GAV = {
    val groupId = getSettingOrThrow(groupIdKey.key.label)
    val artifactId = getSettingOrThrow(artifactIdKey.key.label)
    val version = getSettingOrThrow(versionKey.key.label)

    GAV(groupId, artifactId, version)
  }

  private def getSettingOrThrow(name: String): String = {
    val transposed = transpose(name)

    val key = SettingKey[String](AttributeKey[String](name = transposed))
    getSettingOrThrow(key)
  }

  private def getSettingOrThrow(key: SettingKey[String]): String =
      extractedState.getOpt(key).getOrElse(throw new ManipulationException(s"No SettingKey named '$key' exists in the build'"))


  private def transpose(key: String): String = {
    settingTranspositions.getOrElse(key, key)
  }
}


