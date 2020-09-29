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
package com.sun.squawk.builder.platform;


import java.io.File;
import com.sun.squawk.builder.*;
import com.sun.squawk.builder.ccompiler.*;


/**
 * This class represents Linux on a PowerPC processor.
 *
 * @author Andrew Crouch
 * @version 1.0
 */
public class Linux_PPC extends Unix {

    public Linux_PPC(Build env) {
        super(env);
    }

    /**
     * {@inheritDoc}
     */
    public File getToolsDir() {
        return new File("tools", "linux-ppc");
    }

    /**
     * {@inheritDoc}
     */
    public CCompiler createDefaultCCompiler() {
        return new GccLinuxPPCCompiler(env, this);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isBigEndian() {
        return true;
    }
}
