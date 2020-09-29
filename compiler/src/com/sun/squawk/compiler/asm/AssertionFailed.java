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

package com.sun.squawk.compiler.asm;

/**
 * Thrown to indicate that an assertion has failed. This exception usually shows
 * up a bug in the source code of the compiler.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class AssertionFailed extends RuntimeException {
    /**
     * Constructs a new exception with no detail message.
     */
    public AssertionFailed() {
        super();
    }

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param  msg  the detail message
     */
    public AssertionFailed(String msg) {
        super(msg);
    }
}
