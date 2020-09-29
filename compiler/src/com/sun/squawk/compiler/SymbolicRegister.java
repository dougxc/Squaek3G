/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: SymbolicRegister.java,v 1.8 2005/01/18 23:21:06 ccifue Exp $
 */

package com.sun.squawk.compiler;

import com.sun.squawk.compiler.Compiler;
import com.sun.squawk.compiler.asm.x86.*;

/**
 * Symbolic registers to be placed on the shadow stack
 * We use 0 as a "no register" value; all other integer numbers are
 * valid virtual/symbolic registers.
 *
 * @author  Cristina Cifuentes
 */

abstract class SymbolicRegister extends SymbolicValueDescriptor implements ShadowStackConstants{

    Register register;
    Type type;
    boolean spilled = false;

    /**
     * Constructor
     */
    public SymbolicRegister() {}

    public int getSymbolicValueDescriptor() {
        return S_REG;
    }

    public int getRegisterSize() {
        return 0;
    }

    public boolean isTypeEquivalent(Type type) {
        return false;
    }

    public Register getRegister() {
        return register;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type newType) {
        type = newType;
    }

    public boolean isSpilled() {
        return spilled;
    }

    public void resetSpilled() {
        spilled = false;
    }

    public void setSpilled() {
        spilled = true;
    }

    public void print() {
        System.err.println("SymbolicRegister - abstract");
    }

}

