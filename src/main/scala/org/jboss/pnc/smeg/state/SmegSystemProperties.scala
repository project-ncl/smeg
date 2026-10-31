package org.jboss.pnc.smeg.state

object SmegSystemProperties {
  val MANIPULATION_DISABLE = "manipulation.disable"
  val SETTING_TRANSPOSITIONS = "settingTransposition"
  val VERSION_SUFFIX_SETTING = "versionSuffixSetting"
  val FORCE_ROOT_GA = "forceRootGA"
  val GROOVY_SCRIPTS = "groovyScripts"
  /** Set once the PREPARSE scripts have run: sbt evaluates the global plugins build several times in the same JVM. */
  val GROOVY_SCRIPTS_DONE = "smeg.groovyScripts.done"
}
