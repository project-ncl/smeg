package org.jboss.pnc.smeg.groovy;

import java.io.File;
import java.util.Properties;

import org.jboss.pnc.mavenmanipulator.common.exception.ManipulationException;
import org.jboss.pnc.mavenmanipulator.core.groovy.InvocationStage;
import org.jboss.pnc.mavenmanipulator.core.groovy.SBTBaseScript;
import org.jboss.pnc.mavenmanipulator.io.FileIO;
import org.jboss.pnc.mavenmanipulator.io.rest.Translator;
import org.slf4j.Logger;

/**
 * Abstract class that should be used by developers wishing to implement groovy scripts for SMEG, declared with
 * {@code @SMEGBaseScript BaseScript smeg}.
 */
public abstract class BaseScript extends SBTBaseScript {

    private Logger logger;

    private File basedir;

    private Properties properties;

    private InvocationStage stage;

    private FileIO fileIO;

    private Translator restAPI;

    public Logger getLogger() {
        return logger;
    }

    /**
     * Get the working directory (the execution root).
     *
     * @return a {@link java.io.File} reference.
     */
    @Override
    public File getBaseDir() {
        return basedir;
    }

    /**
     * Get the user properties
     *
     * @return a {@link java.util.Properties} reference.
     */
    @Override
    public Properties getUserProperties() {
        return properties;
    }

    /**
     * Get the current stage
     *
     * @return a {@link InvocationStage} reference.
     */
    @Override
    public InvocationStage getInvocationStage() {
        return stage;
    }

    @Override
    public FileIO getFileIO() {
        return fileIO;
    }

    @Override
    public Translator getRESTAPI() throws ManipulationException {
        if (restAPI == null) {
            throw new ManipulationException("No REST API available: restURL is not set");
        }
        return restAPI;
    }

    /**
     * Internal use only - {@link GroovyScripts} uses this to initialise the values
     *
     * @param logger the logger
     * @param stage the current invocation stage
     * @param rootDir the root directory of the project.
     * @param properties the current configuration properties
     * @param restAPI the REST API Translator interface ; may be null.
     */
    public void setValues(
            Logger logger,
            InvocationStage stage,
            File rootDir,
            Properties properties,
            Translator restAPI) {
        this.logger = logger;
        this.stage = stage;
        this.basedir = rootDir;
        this.properties = properties;
        this.restAPI = restAPI;
        this.fileIO = new FileIO();

        logger.info("Injecting values for stage {} with basedir {}", stage, rootDir);
    }
}
