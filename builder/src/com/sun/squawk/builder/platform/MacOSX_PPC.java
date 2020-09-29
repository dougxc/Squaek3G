/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: MacOSX_PPC.java,v 1.1 2005/02/14 08:46:22 dsimon Exp $
 */
package com.sun.squawk.builder.platform;

import java.io.*;
import com.sun.squawk.builder.*;
import com.sun.squawk.builder.ccompiler.*;

/**
 * This class represents Mac OS X on the PowerPC processor.
 */
public class MacOSX_PPC extends Unix {

    public MacOSX_PPC(Build env) {
        super(env);
    }

    /**
     * {@inheritDoc}
     */
    public File getToolsDir() {
        return new File("tools", "macosx-ppc");
    }

    /**
     * {@inheritDoc}
     */
    public void showJNIEnvironmentMessage(PrintStream out) {
        out.println();
        out.println("There is no need to configure the environment for Squawk on Mac OS X/PPC");
        out.println("as the location of the JavaVM framework is built into the executable.");
        out.println();
    }

    /**
     * {@inheritDoc}
     */
    public CCompiler createDefaultCCompiler() {
        return new GccMacOSXCompiler(env, this);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isBigEndian() {
        return true;
    }
}
