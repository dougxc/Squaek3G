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
 * Provides support for assertions that can be removed on demand in order for
 * building a release version.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class Assert {
    /**
     * Whether assertions are included in the bytecodes or not.
     */
    public static final boolean ASSERTS_ENABLED = true;

    /**
     * Don't let anyone instantiate this class.
     */
    private Assert() {}

    /**
     * Asserts that the specified condition is true. If the condition is false
     * an exception with the specified message will be thrown.
     *
     * @param   cond  condition to be tested
     * @param   msg   message that explains a potential failure
     * @throws  AssertionFailed  if the specified assertion is broken
     */
    public static void that(boolean cond, String msg) {
        if (!cond) {
            throw new AssertionFailed(msg);
        }
    }

    /**
     * Asserts that the compiler should never reach this point.
     *
     * @throws  AssertionFailed  if the inclusion of assertions is enabled
     */
    public static void shouldNotReachHere() {
        if (ASSERTS_ENABLED) {
            throw new AssertionFailed("should not reach here");
        }
    }
}
