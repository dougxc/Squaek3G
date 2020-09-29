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
import com.sun.squawk.util.*;
import com.sun.squawk.*;

/**
 * An instance of <code>Invoke</code> represents an instruction that invokes
 * a method.
 *
 * @author  Doug Simon
 */
public abstract class Invoke extends StackProducer {

    /**
     * The method invoked.
     */
    private final Method method;

    /**
     * The parameters passed to the invocation.
     */
    private final StackProducer[] parameters;

    /**
     * Creates an <code>Invoke</code> representing an instruction that invokes
     * a method.
     *
     * @param  method      the method invoked
     * @param  parameters  the parameters passed to the invocation
     */
    public Invoke(Method method, StackProducer[] parameters) {
        super(method.getReturnType());
        this.method = method;
        this.parameters = parameters;
        Assert.always(!method.isInterpreterInvoked(), "the symbols for this method should have been stripped");
    }

    /**
     * Gets the method invoked by this instruction.
     *
     * @return the method invoked by this instruction
     */
    public Method getMethod() {
        return method;
    }

    /**
     * Gets the parameters passed to the invocation.
     *
     * @return  the parameters passed to the invocation
     */
    public StackProducer[] getParameters() {
        return parameters;
    }

    /**
     * {@inheritDoc}
     */
    public final Object getConstantObject() {
        if (pushesClassOfMethod()) {
            return method.getDefiningClass();
        } else {
            return null;
        }
    }

    /**
     * Determines if this instruction is requires the invoked method's defining class
     * to be pushed to the stack so that the method table is made available.
     *
     * @return boolean
     */
    abstract boolean pushesClassOfMethod();

    /**
     * {@inheritDoc}
     */
    public boolean constrainsStack() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void visit(OperandVisitor visitor) {
        for (int i = 0; i != parameters.length; ++i) {
            StackProducer parameter = parameters[i];
            parameter = visitor.doOperand(this, parameter);
            parameters[i] = parameter;
        }
    }
}
