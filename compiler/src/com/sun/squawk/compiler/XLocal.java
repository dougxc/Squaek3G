/*
 * Copyright 2004-2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: XLocal.java,v 1.8 2005/01/18 23:21:06 ccifue Exp $
 */

package com.sun.squawk.compiler;


/**
 * <code>Local</code> representation for the <code>Compiler</code> implementation.
 *
 * @author Cristina Cifuentes
 */
class XLocal implements Local {

    /**
     * Type for this local
     */
    private Type type;

    /**
     * Slot in the activation record (offset)
     */
    private int slotOffset;

    /**
     * Whether this object is a parameter variable or a local variable:
     * true for parameter and false for local variable.
     */
    private boolean isParam;

    /**
     * Constructor
     *
     * @todo  *** this one should not be needed at the end ***
     */
    public XLocal(Type t, int offset) {
        type = t;
        slotOffset = offset;
    }

    /**
     * Constructor
     *
     * @param t the type of the Local variable
     * @param offset the offset into the activation record
     * @param isParam whether this Local object represents a parameter (PARAM)
     * or a local (LOCAL) variable.
     */
    public XLocal(Type t, int offset, boolean isParam) {
        type = t;
        slotOffset = offset;
        this.isParam = isParam;
    }

    /**
     * Get activation record slot (offset)
     */
    public int getSlotOffset() {
        return slotOffset;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type newType) {
        type = newType;
    }

    /**
     * Determine if Local object is a parameter or a local variable
     */
    public boolean isParam() {
        return isParam;
    }

    public void print() {
        System.err.print("Offset = " + slotOffset + ", is parameter = " + isParam + ", type = ");
        type.print();
    }

}

