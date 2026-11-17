package org.jboss.pnc.smeg.manipulation

import org.jboss.pnc.mavenmanipulator.core.state.VersioningState
import org.jboss.pnc.smeg.model.GAV
import org.jboss.pnc.smeg.rest.{RestClient, RestConfig}
import org.jboss.pnc.smeg.state.SmegSystemProperties.{FORCE_ROOT_GA, VERSION_SUFFIX_SETTING}
import org.jboss.pnc.smeg.state.{ManipulationSession, ManipulationSpec}
import org.jboss.pnc.smeg.util.Versions

import java.util.Properties

object ProjectVersionManipulations {

  var restClient: RestClient = _

  def calculateAndSet(session: ManipulationSession, spec: ManipulationSpec): Unit = {

    val props = System.getProperties

    // Like PME: no version change with versionModification=false, or without versionSuffix, versionIncrementalSuffix,
    // versionOverride or enforceVersionPrefix
    if (!new VersioningState(props).isEnabled) {
      session.getLogger.info("Version manipulation is disabled, keeping the project version")
      return
    }

    val calc = new SbtVersionCalculator

    val rootGav = session.getRootProjectGav
    val candidates = lookupCandidates(calc, rootGav, props, session.restConfig, msg => session.getLogger.info(msg))

    val nextVersion = calc.calculate(rootGav, props, candidates)

    sys.props.get(VERSION_SUFFIX_SETTING) match {
      case Some(x) =>
        val vsplit = Versions.splitQualifier(nextVersion)
        spec.overrideSetting(session.settingTranspositions.getOrElse("version", "version"), vsplit._1)
        spec.overrideSetting(sys.props(VERSION_SUFFIX_SETTING), vsplit._2)
      case None =>
        spec.overrideSetting(session.settingTranspositions.getOrElse("version", "version"), nextVersion)
    }
  }

  /**
   * Existing versions DA reports for the root project, used as candidates by the version calculator.
   *
   * The lookup GAV is normalised like PME does (see [[SbtVersionCalculator.restLookupGav]]), and DA's answer is
   * keyed by that normalised GAV, so results are matched on groupId:artifactId rather than on the raw root GAV.
   */
  def lookupCandidates(calc: SbtVersionCalculator, rootGav: GAV, props: Properties, restConfig: RestConfig,
                       log: String => Unit): Set[String] =
    if (!restConfig.restLookupEnabled) {
      Set.empty
    } else {
      val lookupGav = calc.restLookupGav(rootGav, props)
      log(s"Passing project GAV into the REST client api: $lookupGav")
      val result = RESTLookup(Set(lookupGav), restConfig)
      log(s"REST Client returned for project versions: $result")
      // The lookup holds a single GAV, so groupId:artifactId identifies its answer, whose key carries the
      // normalised version rather than rootGav's.
      result.collect {
        case (gav, version) if gav.groupId == rootGav.groupId && gav.artifactId == rootGav.artifactId
          && version != null && version.nonEmpty => version
      }.toSet
    }

  protected def RESTLookup(gavs: Set[GAV], restConfig: RestConfig): Map[GAV, String] = {
    if (Option(restClient).isEmpty) {
      restClient = RestClient(restConfig)
    }

   restClient.lookupProjectVersions(gavs)
  }
}
