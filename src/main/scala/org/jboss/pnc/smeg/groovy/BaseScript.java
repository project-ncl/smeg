package org.jboss.pnc.smeg.groovy;

import groovy.lang.Script;

import java.io.File;
import java.util.Properties;

/**
 * Base class of every Groovy script run by SMEG. Scripts don't need to declare it: SMEG compiles them with this
 * class as the script base class, so {@code getBaseDir()}, {@code getLogger()} and {@code getUserProperties()} (or
 * the {@code baseDir}, {@code logger}, {@code userProperties} properties) are directly available.
 */
public abstract class BaseScript extends Script {

    private File baseDir;
    private ScriptLogger logger;
    private Properties userProperties;

    void init(File baseDir, ScriptLogger logger, Properties userProperties) {
        this.baseDir = baseDir;
        this.logger = logger;
        this.userProperties = userProperties;
    }

    /** Root directory of the sbt project being manipulated. */
    public File getBaseDir() {
        return baseDir;
    }

    public ScriptLogger getLogger() {
        return logger;
    }

    /** System properties passed to the sbt invocation (e.g. via {@code -Dkey=value}). */
    public Properties getUserProperties() {
        return userProperties;
    }
}
