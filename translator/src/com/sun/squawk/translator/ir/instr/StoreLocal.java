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
 * An instance of <code>StoreLocal</code> represents an instruction that pops
 * a value off the operand stack and stores it to a local variable.
 *
 * @author  Doug Simon
 */
public final class StoreLocal extends Instruction implements LocalVariable, Mutator {

    /**
     * The value being stored.
     */
    private StackProducer value;

    /**
     * The local containing the value that is stored to.
     */
    private final Local local;

    /**
     * Creates an instance of <code>StoreLocal</code> that pops a value from
     * the operand stack and stores it to a given local.
     *
     * @param local  the local to which the value is stored
     * @param value  the value stored to the local variable
     */
    public StoreLocal(Local local, StackProducer value) {
        this.local = local;
        this.value = value;
    }

    /**
     * {@inheritDoc}
     */
    public Local getLocal() {
        return local;
    }

    /**
     * Gets the value stored to the local variable.
     *
     * @return the value stored to the local variable
     */
    public StackProducer getValue() {
        return value;
    }

    /**
     * Returns <code>true</code> to indicate that a store writes a value
     * to the referenced local variable.
     *
     * @return  true
     */
    public boolean writesValue() {
        return true;
    }

    /**
     * Returns <code>false</code> to indicate that a store does not read a value
     * from the referenced local variable.
     *
     * @return  false
     */
    public boolean readsValue() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public Klass getMutationType() {
        return local.getType();
    }

    /**
     * {@inheritDoc}
     */
    public void visit(InstructionVisitor visitor) {
        visitor.doStoreLocal(this);
    }

    /**
     * {@inheritDoc}
     */
    public void visit(OperandVisitor visitor) {
        value = visitor.doOperand(this, value);
    }
}
