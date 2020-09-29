/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: $
 */
package com.sun.squawk.builder;

/**
 * A <code>BuildException</code> is thrown to indicate that an execution of a builder is to terminate.
 */
public class BuildException extends RuntimeException {

    /**
     * The exit value that of the command that will be passes to {@link System#exit}
     * if this termination halts the builder.
     */
    public final int exitValue;

    /**
     * Creates a new BuildException with a specified detail message, exit value and cause
     *
     * @param msg        the detail message
     * @param cause      the cause of the exception
     */
    public BuildException(String msg, Throwable cause) {
        super(msg, cause);
        this.exitValue = -1;
    }

    /**
     * Creates a new BuildException with a specified detail message and exit value.
     *
     * @param msg        the detail message
     * @param exitValue  the exit value to be passed to {@link System#exit} if necessary
     */
    public BuildException(String msg, int exitValue) {
        super(msg);
        this.exitValue = exitValue;
    }

    /**
     * Creates a new BuildException with a specified detail message.
     *
     * @param msg        the detail message
     */
    public BuildException(String msg) {
        this(msg, -1);
    }
}

