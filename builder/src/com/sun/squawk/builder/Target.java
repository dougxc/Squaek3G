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

import java.io.*;
import java.util.*;

/**
 * A <code>Target</code> denotes a builder command that compiles a directory of Java sources.
 */
public final class Target extends Command {

    public final String classPath;
    public final boolean j2me;
    public final boolean preprocess;
    public final File baseDir;
    public final File[] srcDirs;

    public List extraArgs;
    public String version;

    /**
     * Creates a new compilation command.
     *
     * @param classPath      the class path to compile against
     * @param j2me           specifies if the classes being compiled are to be deployed on a J2ME platform
     * @param baseDir        the base directory under which the various intermediate and output directories are created
     * @param srcDirs        the directories that are searched recursively for the source files to be compiled
     * @param preprocess     specifies if the files should be {@link Preprocessor preprocessed} before compilation
     * @param env Build      the builder environment in which this command will run
     */
    public Target(String classPath, boolean j2me, String baseDir, File[] srcDirs, boolean preprocess, Build env) {
        super(env, baseDir);
        this.classPath = classPath;
        this.j2me = j2me;
        this.baseDir = new File(baseDir);
        this.srcDirs = srcDirs;
        this.preprocess = preprocess;
    }

    /**
     * Performs the compilation.
     *
     * {@inheritDoc}
     */
    public void run(String[] args) {
        env.javac(classPath, baseDir, srcDirs, j2me, version, extraArgs, preprocess);
    }

    /**
     * {@inheritDoc}
     */
    public String getDescription() {
        String desc = "compiles the Java sources in " + srcDirs[0];
        for (int i = 1; i <  srcDirs.length; ++i) {
            if (i == srcDirs.length - 1) {
                desc = desc + " and " + srcDirs[i];
            } else {
                desc = desc + ", " + srcDirs[i];
            }
        }
        return desc;
    }

    /**
     * {@inheritDoc}
     */
    public void clean() {
        Build.clear(new File(baseDir, "classes"), true);
        Build.delete(new File(baseDir, "classes.jar"));
        if (preprocess) {
            Build.clear(new File(baseDir, "preprocessed"), true);
        }
        if (j2me) {
            Build.clear(new File(baseDir, "j2meclasses"), true);
        }
        Build.clear(new File(baseDir, "javadoc"), true);
        Build.clear(new File(baseDir, "doccheck"), true);
    }
}
