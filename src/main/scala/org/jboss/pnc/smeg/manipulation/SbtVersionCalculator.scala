package org.jboss.pnc.smeg.manipulation

import org.commonjava.atlas.maven.ident.ref.{ProjectRef, SimpleProjectVersionRef}
import org.jboss.pnc.mavenmanipulator.common.Version
import org.jboss.pnc.mavenmanipulator.core.impl.VersionCalculator
import org.jboss.pnc.mavenmanipulator.core.state.VersioningState
import org.jboss.pnc.smeg.model.GAV

import java.util
import java.util.Properties
import scala.collection.JavaConverters.{mapAsJavaMap, setAsJavaSet}

class SbtVersionCalculator extends VersionCalculator(null) {

  def calculate(groupId: String, artifactId: String, version: String, props: Properties, candidates: Set[String]): String = {
    val state = new VersioningState(props)

    val ref = new SimpleProjectVersionRef(groupId, artifactId, version)

    val fullCandidates = candidates ++ Set(version)

    val metadata = Map[ProjectRef, util.Set[String]](
      ref.asProjectRef() -> setAsJavaSet(fullCandidates)
    )

    state.setRESTMetadata(mapAsJavaMap(metadata))

    calculate(groupId, artifactId, version, state)
  }

  def calculate(gav: GAV, props: Properties, candidates: Set[String]): String = calculate(gav.groupId, gav.artifactId, gav.version, props, candidates)

  def calculate(gav: GAV, props: Properties): String = calculate(gav, props, Set[String]())

  /**
   * The GAV to send to DA for the project version lookup, built like PME's RESTCollector does for the execution
   * root: the versionOverride when set, otherwise the version with alternate suffixes handled, converted to OSGi,
   * SNAPSHOT stripped (unless preserved) and the enforceVersionPrefix reference appended.
   *
   * DA matches existing builds against this version, so it must be the one PME would send. With
   * `-DenforceVersionPrefix=rhlw` PME asks for `0.6.1.rhlw-00000`; asking for the plain `0.6.1` finds none of the
   * `0.6.1.rhlw-00000-n-NNNNN` builds, so every build would get the first suffix again.
   */
  def restLookupGav(gav: GAV, props: Properties): GAV = {
    val state = new VersioningState(props)
    Option(state.getOverride).filter(_.nonEmpty) match {
      case Some(versionOverride) => gav.copy(version = versionOverride)
      case None =>
        val ref = new SimpleProjectVersionRef(gav.groupId, gav.artifactId, gav.version)
        val version = VersionCalculator.enforceVersionPrefix(
          state,
          VersionCalculator.handlePotentialSnapshotVersion(
            state.isPreserveSnapshot,
            ref.asProjectRef(),
            Version.getOsgiVersion(VersionCalculator.handleAlternate(state, gav.version))))
        gav.copy(version = version)
    }
  }

}
