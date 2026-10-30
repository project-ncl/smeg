package org.jboss.pnc.smeg.groovy

import groovy.lang.{Binding, GroovyShell}
import org.codehaus.groovy.control.CompilerConfiguration
import org.jboss.pnc.smeg.state.SmegSystemProperties.{GROOVY_SCRIPTS, MANIPULATION_DISABLE}

import java.io.File
import java.net.URL
import scala.io.Source

/**
 * Runs the Groovy scripts listed (comma separated) in the `groovyScripts` system property.
 *
 * [[preparse]] is meant to be called from the global plugins build (see README), which sbt loads before the
 * project's own build definition. Scripts can therefore fix builds that would not load at all, e.g. replace a
 * `project/Build.scala` written for sbt 0.13 when sbt 1.x is forced with `-Dsbt.version`.
 */
object GroovyScripts {

  /** Set once the scripts have run: sbt evaluates the global plugins build several times in the same JVM. */
  val PREPARSE_DONE = "smeg.groovyScripts.done"

  private val logger = new ScriptLogger

  def preparse(): Unit = preparse(new File(sys.props("user.dir")))

  def preparse(baseDir: File): Unit = {
    val disabled = sys.props.getOrElse(MANIPULATION_DISABLE, "false").toBoolean
    if (disabled || sys.props.get(PREPARSE_DONE).contains("true")) {
      return
    }
    scriptUrls.foreach(run(_, baseDir))
    sys.props(PREPARSE_DONE) = "true"
  }

  private def scriptUrls: Seq[String] =
    sys.props.get(GROOVY_SCRIPTS).toSeq.flatMap(_.split(",")).map(_.trim).filter(_.nonEmpty)

  private def run(url: String, baseDir: File): Unit = {
    logger.info("Smeg running groovy script {} in {}", url, baseDir)
    val config = new CompilerConfiguration
    config.setScriptBaseClass(classOf[BaseScript].getName)
    val shell = new GroovyShell(getClass.getClassLoader, new Binding, config)
    val script = shell.parse(fetch(url), scriptClassName(url)).asInstanceOf[BaseScript]
    script.init(baseDir, logger, System.getProperties)
    script.run()
  }

  private def fetch(url: String): String = {
    val connection = new URL(url).openConnection()
    connection.setConnectTimeout(30000)
    connection.setReadTimeout(60000)
    val source = Source.fromInputStream(connection.getInputStream, "UTF-8")
    try source.mkString finally source.close()
  }

  /** Groovy derives the script class from its name, so it must be a valid Java identifier. */
  private[groovy] def scriptClassName(url: String): String = {
    val fileName = url.split('/').last.split('?').head.stripSuffix(".groovy")
    val name = fileName.replaceAll("[^A-Za-z0-9_$]", "_")
    if (name.isEmpty || !Character.isJavaIdentifierStart(name.head)) s"_$name" else name
  }
}
