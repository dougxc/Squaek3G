/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk.debugger;

import java.io.*;
import javax.microedition.io.*;

import com.sun.squawk.*;

/**
 * This class provides a (very basic) line-based logging facility.
 */
public class Log {

    /**
     * Disables logging.
     */
    public static final int NONE = 0;

    /**
     * Informational log level.
     */
    public static final int INFO = 1;

    /**
     * More verbose log level.
     */
    public static final int VERBOSE = 2;

    /**
     * Log level used to debug problems (very verbose).
     */
    public static final int DEBUG = 3;

    /**
     * The current logging level.
     */
    public static final int level = configLevel();

    /**
     * Where messages are logged.
     */
    public static final PrintStream out = configOut(level);

    /**
     * Configures the logging level based on the value of the <code>"squawk.debugger.log.level"</code>
     * system property.
     *
     * @return the logging level
     */
    private static int configLevel() {
        int level = NONE;
        String prop = System.getProperty("squawk.debugger.log.level");
        if (prop != null) {
            if (prop.equals("none")) {
                level = NONE;
            } else if (prop.equals("info")) {
                level = INFO;
            } else if (prop.equals("verbose")) {
                level = VERBOSE;
            } else if (prop.equals("debug")) {
                level = DEBUG;
            } else {
                System.err.println("logging disabled - invalid log level in squawk.debugger.log.level system property: " + prop);
            }
        }
        return level;
    }

    /**
     * Configures the logging stream based on the value of the <code>"squawk.debugger.log.url"</code>
     * system property.
     *
     * @return the logging stream
     */
    private static PrintStream configOut(int level) {
        PrintStream out = System.out;
        if (level != NONE) {
            System.err.println("logging level: " + Log.level);
            String prop = System.getProperty("squawk.debugger.log.url");
            if (prop != null) {
                try {
                    out = new PrintStream(Connector.openOutputStream(prop));
                    System.err.println("logging to " + prop);
                } catch (IOException e) {
                    System.err.println("logging to System.out - exception while opening log stream: " + prop);
                    e.printStackTrace();
                }
            } else {
                System.err.println("logging to System.out");
            }
        }
        return out;
    }

    public static boolean info() {
        return level >= INFO;
    }

    public static boolean verbose() {
        return level >= VERBOSE;
    }

    public static boolean debug() {
        return level >= DEBUG;
    }

    /**
     * Logs a message as a line sent to the current logging stream.
     *
     * @param msg    the message to log
     */
    public static void log(String msg) {
        if (out != null) {
            msg = "[" + Thread.currentThread() + "] " + msg;
            out.println(msg);
            out.flush();
        }
    }
}
