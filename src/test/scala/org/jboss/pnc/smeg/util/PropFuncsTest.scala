package org.jboss.pnc.smeg.util

import org.jboss.pnc.smeg.UnitSpec
import org.jboss.pnc.smeg.util.PropFuncs.mapOverloadedProps

import java.util.Properties

class PropFuncsTest extends UnitSpec {

  private def fixture = new {
    val props = new Properties
  }

  behavior of "Properties utilities"

  "mapOverloadedSysProps method" should "find only the prefixed properties, strip the prefix and return a Map of stripped key -> value" in {
    val f = fixture

    val prefix = "testSetting"

    val expected = Map(
      "foo1" -> "bar1",
      "foo2" -> "bar2"
    )

    f.props.setProperty(s"$prefix.foo1", "bar1")
    f.props.setProperty(s"$prefix.foo2", "bar2")
    f.props.setProperty("baz.foo3", "bar3")

    val result = mapOverloadedProps(prefix, f.props)

    assert(result == expected)
  }

}
