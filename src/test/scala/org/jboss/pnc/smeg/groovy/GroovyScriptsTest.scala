package org.jboss.pnc.smeg.groovy

import org.jboss.pnc.mavenmanipulator.common.exception.ManipulationException
import org.jboss.pnc.smeg.UnitSpec
import org.jboss.pnc.smeg.state.SmegSystemProperties.{GROOVY_SCRIPTS, MANIPULATION_DISABLE}

import java.io.File
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files

class GroovyScriptsTest extends UnitSpec {

  private val touchedProps = Seq(GROOVY_SCRIPTS, MANIPULATION_DISABLE, GroovyScripts.PREPARSE_DONE, "smeg.test.value")

  before {
    touchedProps.foreach(sys.props.remove)
  }

  after {
    touchedProps.foreach(sys.props.remove)
  }

  private def header(stage: String = "PREPARSE") =
    s"""import org.jboss.pnc.mavenmanipulator.core.groovy.InvocationPoint
       |import org.jboss.pnc.mavenmanipulator.core.groovy.InvocationStage
       |import org.jboss.pnc.mavenmanipulator.core.groovy.SMEGBaseScript
       |import org.jboss.pnc.smeg.groovy.BaseScript
       |
       |@InvocationPoint(invocationPoint = InvocationStage.$stage)
       |@SMEGBaseScript BaseScript smeg
       |""".stripMargin

  private def fixture = new {
    val baseDir: File = Files.createTempDirectory("smeg-groovy-base").toFile
    val scriptsDir: File = Files.createTempDirectory("smeg-groovy-scripts").toFile

    def script(name: String, body: String): String = {
      val file = new File(scriptsDir, name)
      Files.write(file.toPath, body.getBytes(UTF_8))
      file.toURI.toString
    }

    def out: String = new String(Files.readAllBytes(new File(baseDir, "out.txt").toPath), UTF_8)

    def hasOut: Boolean = new File(baseDir, "out.txt").exists
  }

  behavior of "GroovyScripts"

  it should "run the PREPARSE scripts in order with the base script values set" in {
    val f = fixture
    val first = f.script("first.groovy", header() +
      """new File(smeg.baseDir, "out.txt") << "first:${smeg.userProperties.getProperty('smeg.test.value')}:${smeg.invocationStage}\n"
        |assert smeg.fileIO != null
        |smeg.logger.info("first script ran in {}", smeg.baseDir)""".stripMargin)
    val second = f.script("second-script.groovy", header("ALL") +
      """new File(smeg.getBaseDir(), "out.txt") << "second\n" """)
    sys.props("smeg.test.value") = "42"
    sys.props(GROOVY_SCRIPTS) = s"$first, $second"

    GroovyScripts.preparse(f.baseDir)

    f.out shouldBe "first:42:PREPARSE\nsecond\n"
  }

  it should "accept the legacy annotations" in {
    val f = fixture
    sys.props(GROOVY_SCRIPTS) = f.script("legacy.groovy",
      """import org.commonjava.maven.ext.core.groovy.InvocationPoint
        |import org.commonjava.maven.ext.core.groovy.InvocationStage
        |import org.commonjava.maven.ext.core.groovy.SMEGBaseScript
        |import org.jboss.pnc.smeg.groovy.BaseScript
        |
        |@InvocationPoint(invocationPoint = InvocationStage.PREPARSE)
        |@SMEGBaseScript BaseScript smeg
        |new File(smeg.baseDir, "out.txt") << "legacy"
        |""".stripMargin)

    GroovyScripts.preparse(f.baseDir)

    f.out shouldBe "legacy"
  }

  it should "skip scripts for other invocation points" in {
    val f = fixture
    sys.props(GROOVY_SCRIPTS) = f.script("first.groovy", header("FIRST") + """new File(smeg.baseDir, "out.txt") << "x" """)

    GroovyScripts.preparse(f.baseDir)

    f.hasOut shouldBe false
  }

  it should "require the InvocationPoint annotation" in {
    val f = fixture
    sys.props(GROOVY_SCRIPTS) = f.script("no-stage.groovy", """println "x" """)

    the[ManipulationException] thrownBy GroovyScripts.preparse(f.baseDir) should have message
      "Mandatory annotation '@InvocationPoint(invocationPoint = ' not declared"
  }

  it should "run the scripts only once per JVM" in {
    val f = fixture
    sys.props(GROOVY_SCRIPTS) = f.script("once.groovy", header() + """new File(smeg.baseDir, "out.txt") << "x" """)

    GroovyScripts.preparse(f.baseDir)
    GroovyScripts.preparse(f.baseDir)

    f.out shouldBe "x"
  }

  it should "not run anything when manipulation is disabled" in {
    val f = fixture
    sys.props(GROOVY_SCRIPTS) = f.script("disabled.groovy", header() + """new File(smeg.baseDir, "out.txt") << "x" """)
    sys.props(MANIPULATION_DISABLE) = "true"

    GroovyScripts.preparse(f.baseDir)

    f.hasOut shouldBe false
  }

  it should "fail when a script fails" in {
    val f = fixture
    sys.props(GROOVY_SCRIPTS) = f.script("broken.groovy", header() + """throw new IllegalStateException("boom")""")

    val e = the[ManipulationException] thrownBy GroovyScripts.preparse(f.baseDir)
    e.getCause shouldBe an[IllegalStateException]
    sys.props.get(GroovyScripts.PREPARSE_DONE) shouldBe None
  }

  it should "fail when a script assert fails" in {
    val f = fixture
    sys.props(GROOVY_SCRIPTS) = f.script("assert.groovy", header() + "assert 1 == 2")

    val e = the[ManipulationException] thrownBy GroovyScripts.preparse(f.baseDir)
    e.getMessage shouldBe "Problem running script"
    e.getCause shouldBe an[AssertionError]
  }

  it should "fail when a script does not compile" in {
    val f = fixture
    sys.props(GROOVY_SCRIPTS) = f.script("syntax.groovy", header() + "def x = ")

    val e = the[ManipulationException] thrownBy GroovyScripts.preparse(f.baseDir)
    e.getMessage should startWith("Unable to parse script")
    e.getCause shouldBe a[org.codehaus.groovy.control.CompilationFailedException]
  }

  it should "fail when a script cannot be read" in {
    val f = fixture
    sys.props(GROOVY_SCRIPTS) = new File(f.scriptsDir, "missing.groovy").toURI.toString

    a[ManipulationException] should be thrownBy GroovyScripts.preparse(f.baseDir)
  }

  it should "fail on getRESTAPI when restURL is not set" in {
    val f = fixture
    sys.props(GROOVY_SCRIPTS) = f.script("rest.groovy", header() + "smeg.getRESTAPI()")

    the[ManipulationException] thrownBy GroovyScripts.preparse(f.baseDir) should have message
      "No REST API available: restURL is not set"
  }
}
