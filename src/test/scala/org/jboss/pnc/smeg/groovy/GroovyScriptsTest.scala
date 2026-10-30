package org.jboss.pnc.smeg.groovy

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

  private def fixture = new {
    val baseDir: File = Files.createTempDirectory("smeg-groovy-base").toFile
    val scriptsDir: File = Files.createTempDirectory("smeg-groovy-scripts").toFile

    def script(name: String, body: String): String = {
      val file = new File(scriptsDir, name)
      Files.write(file.toPath, body.getBytes(UTF_8))
      file.toURI.toString
    }

    def out: String = new String(Files.readAllBytes(new File(baseDir, "out.txt").toPath), UTF_8)
  }

  behavior of "GroovyScripts"

  it should "run the scripts in order with baseDir, logger and userProperties available" in {
    val f = fixture
    val first = f.script("first.groovy",
      """new File(baseDir, "out.txt") << "first:${userProperties.getProperty('smeg.test.value')}\n"
        |logger.info("first script ran in {}", baseDir)""".stripMargin)
    val second = f.script("second-script.groovy",
      """new File(getBaseDir(), "out.txt") << "second\n" """)
    sys.props("smeg.test.value") = "42"
    sys.props(GROOVY_SCRIPTS) = s"$first, $second"

    GroovyScripts.preparse(f.baseDir)

    f.out shouldBe "first:42\nsecond\n"
  }

  it should "run the scripts only once per JVM" in {
    val f = fixture
    sys.props(GROOVY_SCRIPTS) = f.script("once.groovy", """new File(baseDir, "out.txt") << "x" """)

    GroovyScripts.preparse(f.baseDir)
    GroovyScripts.preparse(f.baseDir)

    f.out shouldBe "x"
  }

  it should "not run anything when manipulation is disabled" in {
    val f = fixture
    sys.props(GROOVY_SCRIPTS) = f.script("disabled.groovy", """new File(baseDir, "out.txt") << "x" """)
    sys.props(MANIPULATION_DISABLE) = "true"

    GroovyScripts.preparse(f.baseDir)

    new File(f.baseDir, "out.txt").exists shouldBe false
  }

  it should "fail when a script fails" in {
    val f = fixture
    sys.props(GROOVY_SCRIPTS) = f.script("broken.groovy", """throw new IllegalStateException("boom")""")

    an[IllegalStateException] should be thrownBy GroovyScripts.preparse(f.baseDir)
    sys.props.get(GroovyScripts.PREPARSE_DONE) shouldBe None
  }

  "scriptClassName" should "turn a script URL into a valid class name" in {
    GroovyScripts.scriptClassName("https://host/path/sb-artemis-edits.groovy") shouldBe "sb_artemis_edits"
    GroovyScripts.scriptClassName("https://host/raw/fix.groovy?ref=main") shouldBe "fix"
    GroovyScripts.scriptClassName("https://host/1.groovy") shouldBe "_1"
  }

  "ScriptLogger.format" should "fill {} placeholders in order" in {
    ScriptLogger.format("a {} b {}", "x", Int.box(2)) shouldBe "a x b 2"
    ScriptLogger.format("no args {}") shouldBe "no args {}"
    ScriptLogger.format("extra {}", "x", "y") shouldBe "extra x"
  }
}
