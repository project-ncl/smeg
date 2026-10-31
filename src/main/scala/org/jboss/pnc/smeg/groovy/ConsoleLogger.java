package org.jboss.pnc.smeg.groovy;

import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MarkerIgnoringBase;
import org.slf4j.helpers.MessageFormatter;

/**
 * SLF4J logger printing to stdout, handed to Groovy scripts as their {@code logger}. SLF4J has no binding on the
 * sbt plugin classpath, so a regular SLF4J logger would discard everything. Debug is enabled with
 * {@code -Dsmeg.debug}; trace is always off.
 * <p>
 * Package-private so that scaladoc skips it: documenting the inherited SLF4J 2 members crashes the Scala 2.12.13
 * classfile parser on {@code org.slf4j.event.Level}.
 */
class ConsoleLogger extends MarkerIgnoringBase {

    ConsoleLogger() {
        this.name = "smeg";
    }

    private void log(String level, FormattingTuple tuple) {
        System.out.println("[" + level + "] " + tuple.getMessage());
        if (tuple.getThrowable() != null) {
            tuple.getThrowable().printStackTrace(System.out);
        }
    }

    private void log(String level, String msg, Object... args) {
        log(level, MessageFormatter.arrayFormat(msg, args));
    }

    private void log(String level, String msg, Throwable t) {
        log(level, new FormattingTuple(msg, null, t));
    }

    @Override
    public boolean isTraceEnabled() {
        return false;
    }

    @Override
    public void trace(String msg) {
    }

    @Override
    public void trace(String format, Object arg) {
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
    }

    @Override
    public void trace(String format, Object... arguments) {
    }

    @Override
    public void trace(String msg, Throwable t) {
    }

    @Override
    public boolean isDebugEnabled() {
        return Boolean.getBoolean("smeg.debug");
    }

    @Override
    public void debug(String msg) {
        if (isDebugEnabled()) {
            log("debug", msg);
        }
    }

    @Override
    public void debug(String format, Object arg) {
        if (isDebugEnabled()) {
            log("debug", format, arg);
        }
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        if (isDebugEnabled()) {
            log("debug", format, arg1, arg2);
        }
    }

    @Override
    public void debug(String format, Object... arguments) {
        if (isDebugEnabled()) {
            log("debug", format, arguments);
        }
    }

    @Override
    public void debug(String msg, Throwable t) {
        if (isDebugEnabled()) {
            log("debug", msg, t);
        }
    }

    @Override
    public boolean isInfoEnabled() {
        return true;
    }

    @Override
    public void info(String msg) {
        log("info", msg);
    }

    @Override
    public void info(String format, Object arg) {
        log("info", format, arg);
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        log("info", format, arg1, arg2);
    }

    @Override
    public void info(String format, Object... arguments) {
        log("info", format, arguments);
    }

    @Override
    public void info(String msg, Throwable t) {
        log("info", msg, t);
    }

    @Override
    public boolean isWarnEnabled() {
        return true;
    }

    @Override
    public void warn(String msg) {
        log("warn", msg);
    }

    @Override
    public void warn(String format, Object arg) {
        log("warn", format, arg);
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        log("warn", format, arg1, arg2);
    }

    @Override
    public void warn(String format, Object... arguments) {
        log("warn", format, arguments);
    }

    @Override
    public void warn(String msg, Throwable t) {
        log("warn", msg, t);
    }

    @Override
    public boolean isErrorEnabled() {
        return true;
    }

    @Override
    public void error(String msg) {
        log("error", msg);
    }

    @Override
    public void error(String format, Object arg) {
        log("error", format, arg);
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        log("error", format, arg1, arg2);
    }

    @Override
    public void error(String format, Object... arguments) {
        log("error", format, arguments);
    }

    @Override
    public void error(String msg, Throwable t) {
        log("error", msg, t);
    }
}
