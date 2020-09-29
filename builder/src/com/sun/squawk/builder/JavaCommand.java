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
 * A <code>JavaCommand</code> denotes a builder command that executes a Java program.
 * The execution occurs in a separate JVM process.
 */
public class JavaCommand extends Command {

    public final String classPath;
    public final boolean bootclasspath;
    public final String extraJVMArgs;
    public final String mainClassName;
    public String description;

    /**
     * Creates a new Java command.
     *
     * @param name   the name of the command
     */
    public JavaCommand(String name, String classPath, boolean bootclasspath, String extraJVMArgs, String mainClassName, Build env) {
        super(env, name);
        this.classPath = classPath;
        this.bootclasspath = bootclasspath;
        this.extraJVMArgs = extraJVMArgs;
        this.mainClassName = mainClassName;
    }

    /**
     * {@inheritDoc}
     */
    public JavaCommand setDescription(String desc) {
        description = desc;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public String getDescription() {
        return description == null ? super.getDescription() : description;
    }

    /**
     * {@inheritDoc}
     */
    public void run(String[] args) {
        env.java(classPath, bootclasspath, extraJVMArgs, mainClassName, args);
    }
}
