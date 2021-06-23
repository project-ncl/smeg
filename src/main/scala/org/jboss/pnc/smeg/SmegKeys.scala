package org.jboss.pnc.smeg

import sbt.internal.util.AttributeKey

object SmegKeys {

  val manipulationDisabledKey = AttributeKey[Boolean](name = "manipulationDisabled")//SettingKey[Boolean]("manipulationDisabled", "Disables SMEg if true")

}
