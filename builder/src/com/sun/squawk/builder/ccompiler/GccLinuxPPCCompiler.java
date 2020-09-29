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
package com.sun.squawk.builder.ccompiler;

import com.sun.squawk.builder.platform.*;

import com.sun.squawk.builder.*;

/**
 * The interface to the GCC compiler on Linux/PPC
 */
public class GccLinuxPPCCompiler extends GccCompiler {

    public GccLinuxPPCCompiler(Build env, Platform platform) {
        super(env, platform);
    }

    /**
     * {@inheritDoc}
     */
    public String options(boolean disableOpts) {
        String s = super.options(disableOpts);
        s += " -DPLATFORM_BIG_ENDIAN=true ";
        return s;
    }

    /**
     * {@inheritDoc}
     */
    public String getArchitecture() {
        return "PPC";
    }

}
