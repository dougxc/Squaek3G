/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: Info.java,v 1.4 2005/01/21 23:10:19 dl156546 Exp $
 */

package com.sun.squawk.compiler.asm.x86;

import com.sun.squawk.compiler.asm.*;

/**
 * Features we are not decided upon.
 */
public  class Info {

    /**
     * Flag to indicate a Pentium 6 processor.
     */
    public final static boolean CodeForP6 = false;

    /**
     * Number of FPU registers.
     */
    public final static int NUM_FPU_REGS = 8;

}
