/*
 * Copyright 2004 Sun Microsystems, Inc.  All rights reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: ShadowStackConstants.java,v 1.5 2005/01/18 23:21:06 ccifue Exp $
 */

package com.sun.squawk.compiler;

/**
 * Constants used in the shadow stack.
 *
 * @author Cristina Cifuentes
 */
interface ShadowStackConstants {

    public static final int S_REG = 1,
                            S_LIT = 2,
                            S_LOCAL = 3,
                            S_OBJECT = 4,
                            S_FIXUP_SYM = 5,
                            S_LABEL = 6,
                            S_OTHER = 10;

}
