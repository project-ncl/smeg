package org.jboss.pnc.smeg.manipulation

import org.commonjava.maven.atlas.ident.ref.{ProjectRef, SimpleProjectVersionRef}
import org.commonjava.maven.ext.core.impl.VersionCalculator
import org.commonjava.maven.ext.core.state.VersioningState
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

}
