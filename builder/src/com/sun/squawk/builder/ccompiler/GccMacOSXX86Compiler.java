/*
 * Copyright 2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */
package com.sun.squawk.builder.ccompiler;

import com.sun.squawk.builder.platform.*;
import com.sun.squawk.builder.*;

/**
 * The interface to the GCC compiler on Mac OS X on the X86 platform.
 * 
 * @author Cristina Cifuentes
 */
public class GccMacOSXX86Compiler extends GccMacOSXCompiler {

    public GccMacOSXX86Compiler(Build env, Platform platform) {
        super(env, platform);
    }

    /**
     * {@inheritDoc}
     */
    public String getArchitecture() {
        return "X86";
    }

}
