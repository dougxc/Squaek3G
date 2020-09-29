/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: Windows_X86.java,v 1.1 2005/02/14 08:46:22 dsimon Exp $
 */
package com.sun.squawk.builder.platform;

import java.io.*;
import com.sun.squawk.builder.*;
import com.sun.squawk.builder.ccompiler.*;

/**
 * This class represents Windows on an x86 or amd64 processor.
 */
public final class Windows_X86 extends Platform {

    public Windows_X86(Build env) {
        super(env);
    }

    /**
     * {@inheritDoc}
     */
    public String getExecutableExtension() {
        return ".exe";
    }

    /**
     * {@inheritDoc}
     */
    public File getToolsDir() {
        return new File("tools/windows-x86");
    }

    /**
     * {@inheritDoc}
     */
    public String getJVMLibraryPath() {
        File dll = Build.find(env.getJDK().getHome(), "jvm.dll", false);
        if (dll == null) {
            throw new BuildException("could not find 'jvm.dll'");
        }
        return dll.getPath();
    }

    /**
     * {@inheritDoc}
     */
    public void showJNIEnvironmentMessage(PrintStream out) {
        String env = getJVMLibraryPath();
        if (env != null) {
            out.println();
            out.println("To configure the environment for Squawk, try the following command:");
            out.println();
            out.println("    set JVMDLL="+env);
            out.println();
        } else {
            out.println();
            out.println("The JVMDLL environment variable must be set to the full path of 'jvm.dll'.");
            out.println();
        }
    }

    /**
     * {@inheritDoc}
     */
    public CCompiler createDefaultCCompiler() {
        return new MscCompiler(env, this);
    }
}
