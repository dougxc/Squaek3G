/*
 * Copyright 1994-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: Label.java,v 1.5 2005/01/18 23:21:06 ccifue Exp $
 */

package com.sun.squawk.compiler;

/**
 * Label in the <code>Compiler</code> interface.
 *
 * @author   Nik Shaylor
 */
public interface Label {

    /**
     * Test to see if the label has been bound.
     *
     * @return true if it is
     */
    public boolean isBound();

    /**
     * Get the offset to the label in the code buffer.
     *
     * @return the offset in bytes
     */
    public int getOffset();

}
