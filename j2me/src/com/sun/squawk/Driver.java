//if[EXCLUDE]
/*
 * Copyright 1994-2001 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */

package com.sun.squawk;

/**
 * Simple interface for device drivers.
 *
 * @author  Alex Garthwaite
 */
public interface Driver {

    /**
     * Initializes this driver.
     *
     * @see     com.sun.squawk.JavaDriverManager
     */
    public abstract void initialize();
}


