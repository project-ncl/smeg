package org.jboss.pnc.smeg.manipulation

import org.commonjava.maven.atlas.ident.ref.{ProjectRef, SimpleProjectVersionRef}
import org.commonjava.maven.ext.core.state.VersioningState
import org.jboss.pnc.smeg.UnitSpec
import org.jboss.pnc.smeg.model.GAV

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

}
