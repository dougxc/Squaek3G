/*
 * @(#)Test.java	1.5 03/01/07
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.sun.tck.cldc.lib;

import java.io.PrintStream;

/**
 * This is a basic interface for CLDC TCK tests.
 *
 * @see com.sun.cldctck.lib.Status
 *
 * @version @(#)Test.java	1.5 
 * @author Stanislav Avzan
 */

public interface Test {

    /** 
     * Method to be called by the test execution engine.
     *
     */
    public Status run(String[] args, PrintStream log, PrintStream ref);

}
