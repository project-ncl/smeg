package org.jboss.pnc.smeg

import org.scalatest._
import flatspec._
import org.scalatest.matchers.should

abstract class UnitSpec extends AnyFlatSpec with should.Matchers with OptionValues with Inside with Inspectors with BeforeAndAfter
