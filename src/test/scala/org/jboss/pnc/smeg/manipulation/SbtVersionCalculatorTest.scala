package org.jboss.pnc.smeg.manipulation

import org.commonjava.atlas.maven.ident.ref.{ProjectRef, SimpleProjectVersionRef}
import org.jboss.pnc.mavenmanipulator.core.state.VersioningState
import org.jboss.pnc.smeg.UnitSpec
import org.jboss.pnc.smeg.model.GAV
import org.jboss.pnc.smeg.rest.{RestClient, RestConfig}

import java.util.Properties
import scala.collection.JavaConverters.{mapAsJavaMap, setAsJavaSet}
import java.util

class SbtVersionCalculatorTest extends UnitSpec {

 def fixture = {
   new {
     val vc = new SbtVersionCalculator
     val props = new Properties
   }
 }

  behavior of "SbtVersionCalculator"

  "calculate method" should "override the version when versionOverride property is set" in {
    val f = fixture
    f.props.setProperty("versionOverride", "10.10.10.rebuild-10")

    val result = f.vc.calculate("org.jboss.pnc.smeg", "smeg-plugin", "1.1.1", new VersioningState(f.props))

    assert(result == "10.10.10.rebuild-10")
  }

  it should "override the version and add a suffix when versionOverride and versionSuffix properties are set" in {
    val f = fixture
    f.props.setProperty("versionOverride", "10.10.10")
    f.props.setProperty("versionSuffix", "rebuild-10")

    val result = f.vc.calculate("org.jboss.pnc.smeg", "smeg-plugin", "1.1.1", new VersioningState(f.props))

    assert(result == "10.10.10.rebuild-10")
  }

  it should "increment the current suffix version when no candidates are supplied" in {
    val f = fixture

    f.props.setProperty(VersioningState.INCREMENT_SERIAL_SUFFIX_SYSPROP, "rebuild")
    f.props.setProperty(VersioningState.INCREMENT_SERIAL_SUFFIX_PADDING_SYSPROP, "0")

    val result = f.vc.calculate("org.jboss.pnc.smeg", "smeg-plugin", "1.1.1.rebuild-2", f.props, Set[String]())

    assert(result == "1.1.1.rebuild-3")
  }

  it should "correctly set the specified version padding" in {
    val f = fixture

    f.props.setProperty(VersioningState.INCREMENT_SERIAL_SUFFIX_SYSPROP, "rebuild")
    f.props.setProperty(VersioningState.INCREMENT_SERIAL_SUFFIX_PADDING_SYSPROP, "3")

    val result = f.vc.calculate(GAV("org.jboss.pnc.smeg", "smeg-plugin", "1.1.1.rebuild-1"), f.props)

    assert(result == "1.1.1.rebuild-002")
  }

  /** Properties reqour passes for a persistent LIGHTWELL_NOVEL build (the last -DversionIncrementalSuffix wins). */
  private def novelProps: Properties = {
    val props = new Properties
    props.setProperty(VersioningState.INCREMENT_SERIAL_SUFFIX_SYSPROP, "n")
    props.setProperty(VersioningState.INCREMENT_SERIAL_SUFFIX_PADDING_SYSPROP, "5")
    props.setProperty(VersioningState.ENFORCE_VERSION_PREFIX, "rhlw")
    props
  }

  private val mchange = GAV("com.mchange", "mchange-commons-java", "0.6.1")

  "restLookupGav" should "append the enforceVersionPrefix reference like PME's RESTCollector" in {
    val f = fixture

    assert(f.vc.restLookupGav(mchange, novelProps) == mchange.copy(version = "0.6.1.rhlw-00000"))
  }

  it should "keep a version that already carries the enforceVersionPrefix reference" in {
    val f = fixture
    val gav = mchange.copy(version = "0.6.1.rhlw-00003")

    assert(f.vc.restLookupGav(gav, novelProps) == gav)
  }

  it should "strip SNAPSHOT before appending the reference" in {
    val f = fixture

    assert(f.vc.restLookupGav(mchange.copy(version = "0.6.1-SNAPSHOT"), novelProps).version == "0.6.1.rhlw-00000")
  }

  it should "use the versionOverride when set" in {
    val f = fixture
    val props = novelProps
    props.setProperty("versionOverride", "0.6.2")

    assert(f.vc.restLookupGav(mchange, props) == mchange.copy(version = "0.6.2"))
  }

  it should "leave the version alone without enforceVersionPrefix" in {
    val f = fixture
    val props = new Properties
    props.setProperty(VersioningState.INCREMENT_SERIAL_SUFFIX_SYSPROP, "rebuild")

    assert(f.vc.restLookupGav(mchange, props) == mchange)
  }

  "calculate method for a novel build" should "mint the first novel suffix when DA knows no build" in {
    val f = fixture

    assert(f.vc.calculate(mchange, novelProps, Set[String]()) == "0.6.1.rhlw-00000-n-00001")
  }

  it should "advance the novel counter past the build DA returns" in {
    val f = fixture

    assert(f.vc.calculate(mchange, novelProps, Set("0.6.1.rhlw-00000-n-00001")) == "0.6.1.rhlw-00000-n-00002")
  }

  "lookupCandidates" should "query DA with the normalised GAV and keep its answer for the root project" in {
    val f = fixture
    val props = novelProps
    props.setProperty("restURL", "http://da.example/rest/v-1")
    val requested = Seq.newBuilder[Set[GAV]]
    ProjectVersionManipulations.restClient = new RestClient {
      override def lookupProjectVersion(gav: GAV): String = ???
      override def lookupProjectVersions(gavs: Set[GAV]): Map[GAV, String] = {
        requested += gavs
        Map(mchange.copy(version = "0.6.1.rhlw-00000") -> "0.6.1.rhlw-00000-n-00001")
      }
    }
    try {
      val candidates = ProjectVersionManipulations.lookupCandidates(f.vc, mchange, props, RestConfig(props), _ => ())

      assert(requested.result() == Seq(Set(mchange.copy(version = "0.6.1.rhlw-00000"))))
      assert(candidates == Set("0.6.1.rhlw-00000-n-00001"))
      assert(f.vc.calculate(mchange, props, candidates) == "0.6.1.rhlw-00000-n-00002")
    } finally {
      ProjectVersionManipulations.restClient = null
    }
  }

  it should "not query DA without a restURL" in {
    val f = fixture
    ProjectVersionManipulations.restClient = new RestClient {
      override def lookupProjectVersion(gav: GAV): String = ???
      override def lookupProjectVersions(gavs: Set[GAV]): Map[GAV, String] = fail("DA must not be queried")
    }
    try {
      val props = novelProps
      assert(ProjectVersionManipulations.lookupCandidates(f.vc, mchange, props, RestConfig(props), _ => ()).isEmpty)
    } finally {
      ProjectVersionManipulations.restClient = null
    }
  }

}
