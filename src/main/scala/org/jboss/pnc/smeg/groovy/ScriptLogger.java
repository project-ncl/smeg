package org.jboss.pnc.smeg.groovy;

/**
 * Logger handed to Groovy scripts. Output goes to stdout because scripts can run while the global plugins build
 * is loading, before sbt's own logging is available (and slf4j has no binding on the plugin classpath). Messages
 * use slf4j-style "{}" placeholders, filled in order.
 */
public class ScriptLogger {

    public void debug(String message, Object... args) {
        if (Boolean.getBoolean("smeg.debug")) {
            log("debug", message, args);
        }
    }

    public void info(String message, Object... args) {
        log("info", message, args);
    }

    public void warn(String message, Object... args) {
        log("warn", message, args);
    }

    public void error(String message, Object... args) {
        log("error", message, args);
    }

    private static void log(String level, String message, Object... args) {
        System.out.println("[" + level + "] " + format(message, args));
    }

    static String format(String message, Object... args) {
        if (message == null || args == null || args.length == 0) {
            return String.valueOf(message);
        }
        StringBuilder sb = new StringBuilder();
        int argIndex = 0;
        int from = 0;
        int at;
        while (argIndex < args.length && (at = message.indexOf("{}", from)) >= 0) {
            sb.append(message, from, at).append(args[argIndex++]);
            from = at + 2;
        }
        return sb.append(message.substring(from)).toString();
    }
}
