/*
 * Copyright 2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk.builder.platform;

import java.io.File;

import com.sun.squawk.builder.*;
import com.sun.squawk.builder.ccompiler.*;

/**
 * This class represents Solaris on an X86 processor.
 * 
 * @author Cristina Cifuentes
 *
 */
public class SunOS_X86 extends Unix {

    public SunOS_X86(Build env) {
        super(env);
    }

    /**
     * {@inheritDoc}
     */
    public File getToolsDir() {
        return new File("tools", "sunos-x86");
    }

    /**
     * {@inheritDoc}
     */
    public CCompiler createDefaultCCompiler() {
        return new GccCompiler(env, this);
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
