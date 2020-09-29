/*
 * Copyright 2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 */
package com.sun.squawk.builder.platform;

import java.io.*;
import com.sun.squawk.builder.*;
import com.sun.squawk.builder.ccompiler.*;

/**
 * This class represents MacOSX on an X86 processor.
 * 
 * @author Cristina Cifuentes
 *
 */
public class MacOSX_X86 extends Unix {

    public MacOSX_X86(Build env) {
        super(env);
    }

    /**
     * {@inheritDoc}
     */
    public File getToolsDir() {
        return new File("tools", "macosx-x86");
    }

    /**
     * {@inheritDoc}
     */
    public CCompiler createDefaultCCompiler() {
        return new GccMacOSXX86Compiler(env, this);
    }

    /**
     * {@inheritDoc}
     */
    public void showJNIEnvironmentMessage(PrintStream out) {
        out.println();
        out.println("There is no need to configure the environment for Squawk on Mac OS X/X86");
        out.println("as the location of the JavaVM framework is built into the executable.");
        out.println();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isBigEndian() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean allowUnalignedLoads() {
        return false;
    }

}
