/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: SymbolicLiteral64.java,v 1.6 2005/01/18 23:21:06 ccifue Exp $
 */

package com.sun.squawk.compiler;

import com.sun.squawk.compiler.Compiler;


/**
 * Symbolic literal values of physical size 64-bits to be placed on the shadow stack
 *
 * @author  Cristina Cifuentes
 */

class SymbolicLiteral64 extends SymbolicLiteral implements ShadowStackConstants{

    /**
     * Long literal values are of size 64-bits.
     */
    private long literal;

    /**
     * Constructor
     *
     * @param lit  the literal value
     */
    public SymbolicLiteral64(long lit, Type type) {
        literal = lit;
        this.type = type;
    }

    public boolean isTypeEquivalent(Type type) {
        /**** check that REF is 64 bits missing ***/
        if ((type == Type.LONG) || (type == Type.REF) || (type == Type.OOP)) {
            return true;
        }
        return false;
    }

    /**
     * Return the literal value
     *
     * @return  the literal value
     */
    public long getLiteral() {
        return literal;
    }

    public int getLiteralSize() {
        return 64;
    }

    public void print() {
        System.err.print("SymbolicLiteral64.  Value = " + literal + ", type = ");
        type.print();
    }

}

