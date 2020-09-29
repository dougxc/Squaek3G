/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: GccMacOSXCompiler.java,v 1.1 2005/02/14 08:46:22 dsimon Exp $
 */
package com.sun.squawk.builder.ccompiler;

import com.sun.squawk.builder.platform.*;

import com.sun.squawk.builder.*;

/**
 * The interface to the GCC compiler on Mac OS X.
 */
public class GccMacOSXCompiler extends GccCompiler {

    public GccMacOSXCompiler(Build env, Platform platform) {
        super("gcc-macosx", env, platform);
    }

    /**
     * {@inheritDoc}
     */
    public String getLinkSuffix() {
        return " -framework CoreFoundation -framework JavaVM";
    }

    /**
     * {@inheritDoc}
     */
    public String getSharedLibrarySwitch() {
        return "-dynamiclib -single_module";
    }

    /**
     * {@inheritDoc}
     */
    public String getArchitecture() {
        return "PPC";
    }

}
