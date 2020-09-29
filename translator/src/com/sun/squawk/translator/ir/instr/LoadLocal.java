/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM translator.
 */
package com.sun.squawk.translator.ir.instr;

import com.sun.squawk.translator.ir.*;
import com.sun.squawk.*;

/**
 * An instance of <code>LoadLocal</code> represents an instruction that loads
 * a value from a specified local variable and pushes it onto the operand stack.
 *
 * @author  Doug Simon
 */
public final class LoadLocal extends StackProducer implements LocalVariable {

    /**
     * The local containing the value that is loaded.
     */
    private final Local local;

    /**
     * Creates an instance of <code>LoadLocal</code> representing an
     * instruction that loads a value from a specified local variable and
     * pushes it onto the operand stack.
     *
     * @param type   the type of the value loaded
     * @param local  the local variable from which the value that is loaded
     */
    public LoadLocal(Klass type, Local local) {
        super(type);
        this.local = local;
    }

    /**
     * {@inheritDoc}
     */
    public Local getLocal() {
        return local;
    }

    /**
     * Returns <code>false</code> to indicate that a load does not write a value
     * to the referenced local variable.
     *
     * @return  false
     */
    public boolean writesValue() {
        return false;
    }

    /**
     * Returns <code>true</code> to indicate that a load reads a value from
     * the referenced local variable.
     *
     * @return true
     */
    public boolean readsValue() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void visit(InstructionVisitor visitor) {
        visitor.doLoadLocal(this);
    }

}
