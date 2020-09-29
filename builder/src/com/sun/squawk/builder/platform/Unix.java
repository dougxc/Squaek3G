/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: Unix.java,v 1.1 2005/02/14 08:46:22 dsimon Exp $
 */
package com.sun.squawk.builder.platform;

import java.io.File;
import java.io.PrintStream;

import com.sun.squawk.builder.*;

/**
 * An abstraction of Unix OS's.
 */
public abstract class Unix extends Platform {

    public Unix(Build env) {
        super(env);
    }

    /**
     * {@inheritDoc}
     */
    public String getExecutableExtension() {
        return "";
    }

    /**
     * Finds the directory within the JDK containing a given shared library.
     *
     * @param name   the name of the shared library to search for
     * @return  the path to the directory containing <code>name</code>
     */
    private String findJavaSharedLibraryDir(String name) {
        File so = Build.find(env.getJDK().getHome(), name, false);
        if (so== null) {
            throw new BuildException("could not find '" + name + "'");
        }
        return so.getParentFile().getPath();
    }

    /**
     * {@inheritDoc}
     */
    public String getJVMLibraryPath() {
        return findJavaSharedLibraryDir("libjvm.so") + ":" + findJavaSharedLibraryDir("libverify.so");
    }

    /**
     * {@inheritDoc}
     */
    public void showJNIEnvironmentMessage(PrintStream out) {
        try {
            String path = getJVMLibraryPath();
            out.println();
            out.println("To configure the environment for Squawk, try the following command under bash:");
            out.println();
            out.println("    export LD_LIBRARY_PATH=" + path + ":$LD_LIBRARY_PATH");
            out.println();
            out.println("or in csh/tcsh");
            out.println();
            out.println("    setenv LD_LIBRARY_PATH " + path + ":$LD_LIBRARY_PATH");
            out.println();
        } catch (BuildException e) {
            out.println();
            out.println("The LD_LIBRARY_PATH environment variable must be set to include the directories");
            out.println("containing 'libjvm.so' and 'libverify.so'.");
            out.println();
        }
    }
}
