package org.jboss.pnc.smeg.model

import org.commonjava.maven.atlas.ident.ref.{ProjectVersionRef, SimpleProjectVersionRef}

case class GAV(groupId: String, artifactId: String, version: String) {

  def toProjectVersionRef: ProjectVersionRef = new SimpleProjectVersionRef(groupId, artifactId, version)

  override def toString: String = s"$groupId:$artifactId:$version"

}

object GAV {
  def apply(gav: String): Option[GAV] = gav.split(':') match {
    case Array(g, a, v) => Some(GAV(g, a, v))
    case _ => None
  }

  def apply(ref: ProjectVersionRef): GAV = GAV(ref.getGroupId, ref.getArtifactId, ref.getVersionString)
}