package org.jboss.pnc.smeg.groovy

import groovy.lang.{Binding, GroovyShell, Script}
import org.codehaus.groovy.control.{CompilationFailedException, CompilerConfiguration}
import org.jboss.pnc.mavenmanipulator.common.exception.ManipulationException
import org.jboss.pnc.mavenmanipulator.core.groovy.{InvocationPoint, InvocationStage}
import org.jboss.pnc.mavenmanipulator.io.FileIO
import org.jboss.pnc.mavenmanipulator.io.rest.Translator
import org.jboss.pnc.smeg.rest.RestConfig
import org.jboss.pnc.smeg.rest.impl.PmeRestClient
import org.jboss.pnc.smeg.state.SmegSystemProperties.{GROOVY_SCRIPTS, MANIPULATION_DISABLE}
import org.slf4j.Logger

import java.io.{File, IOException}
import java.util.Properties
import scala.util.control.NonFatal

/**
 * Runs the Groovy scripts listed (comma separated) in the `groovyScripts` system property, the same way as PME and
 * GME: scripts declare `@InvocationPoint` and `@SMEGBaseScript BaseScript smeg`.
 *
 * [[preparse]] runs the `PREPARSE` scripts and is meant to be called from the global plugins build (see README),
 * which sbt loads before the project's own build definition. Scripts can therefore fix builds that would not load
 * at all, e.g. replace a `project/Build.scala` written for sbt 0.13 when sbt 1.x is forced with `-Dsbt.version`.
 */
object GroovyScripts {

  /** Set once the scripts have run: sbt evaluates the global plugins build several times in the same JVM. */
  val PREPARSE_DONE = "smeg.groovyScripts.done"

  private val logger: Logger = new ConsoleLogger

  def preparse(): Unit = preparse(new File(sys.props("user.dir")))

  def preparse(baseDir: File): Unit = {
    val disabled = sys.props.getOrElse(MANIPULATION_DISABLE, "false").toBoolean
    if (disabled || sys.props.get(PREPARSE_DONE).contains("true")) {
      return
    }
    runCustomGroovyScript(InvocationStage.PREPARSE, baseDir, System.getProperties)
    sys.props(PREPARSE_DONE) = "true"
  }

  /**
   * Port of GME's `GroovyUtils.runCustomGroovyScript`, except that a script that cannot be read fails the run (as
   * in PME) rather than being skipped.
   *
   * @param targetStage the stage we are running
   * @param baseDir the root directory of the project
   * @param properties the user properties
   */
  def runCustomGroovyScript(targetStage: InvocationStage, baseDir: File, properties: Properties): Unit = {
    val scripts = Option(properties.getProperty(GROOVY_SCRIPTS)).toSeq.flatMap(_.split(",")).map(_.trim).filter(_.nonEmpty)
    if (scripts.isEmpty) {
      return
    }
    val fileIO = new FileIO()
    val groovyFiles = scripts.map { script =>
      logger.info("Attempting to read URL {}", script)
      try fileIO.resolveURL(script)
      catch {
        case e: IOException => throw new ManipulationException(s"Unable to read groovy script $script", e)
      }
    }

    for (scriptFile <- groovyFiles) {
      // We use the current class' classloader so the script has access to this plugin's API and the groovy API.
      val groovyShell = new GroovyShell(getClass.getClassLoader, new Binding, new CompilerConfiguration)
      val script =
        try groovyShell.parse(scriptFile)
        catch {
          case e @ (_: IOException | _: CompilationFailedException) =>
            throw new ManipulationException(s"Unable to parse script $scriptFile", e)
        }
      val stage = invocationStage(script)

      logger.info(s"For target stage $targetStage attempting to invoke groovy script $scriptFile")
      if (targetStage == stage || stage == InvocationStage.ALL) {
        // Inject the values via a new BaseScript so user's can have completion.
        script match {
          case baseScript: BaseScript =>
            baseScript.setValues(logger, targetStage, baseDir, properties, fileIO, restAPI(properties))
          case _ =>
            throw new ManipulationException("Cannot cast {} to a BaseScript to set values.", script)
        }
        try {
          logger.info(s"Executing $script at invocation point $stage")
          script.run()
        } catch {
          case e: ManipulationException => throw e
          // NonFatal also covers Errors such as the PowerAssertionError thrown by a failed Groovy assert
          case NonFatal(e) => throw new ManipulationException("Problem running script", e)
        }
      } else {
        logger.debug(s"Ignoring script $script as invocation point $stage does not match.")
      }
    }
  }

  private def invocationStage(script: Script): InvocationStage = {
    val invocationPoint = script.getClass.getAnnotation(classOf[InvocationPoint])
    if (invocationPoint != null) {
      logger.debug("InvocationPoint is {}", invocationPoint.invocationPoint())
      invocationPoint.invocationPoint()
    } else {
      val legacyInvocationPoint =
        script.getClass.getAnnotation(classOf[org.commonjava.maven.ext.core.groovy.InvocationPoint])
      if (legacyInvocationPoint != null) {
        logger.warn("Found legacy InvocationPoint {}", legacyInvocationPoint.invocationPoint())
        // While they are the 'same' values they are different classes.
        InvocationStage.valueOf(legacyInvocationPoint.invocationPoint().getStageValue)
      } else {
        throw new ManipulationException("Mandatory annotation '@InvocationPoint(invocationPoint = ' not declared")
      }
    }
  }

  private def restAPI(properties: Properties): Translator = {
    val restConfig = RestConfig(properties)
    if (restConfig.restLookupEnabled) PmeRestClient.translator(restConfig) else null
  }
}
