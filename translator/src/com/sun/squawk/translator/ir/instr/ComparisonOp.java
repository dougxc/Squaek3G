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
import com.sun.squawk.util.Assert;

/**
 * An instance of <code>ComparisonOp</code> represents an instruction that
 * pops two values off the operand stack, compares them and pushes the
 * result of the comparison.  This type of instruction is not directly
 * supported in Squawk and must be converted.
 *
 * @author  Doug Simon
 */
public final class ComparisonOp extends StackProducer {
    
    /* Definition of native-method based comparisons: */
    
    /** Dummy opcode for LCMP: */
    public static final int LCMP = -1;

    /**
     * The left operand of the operation.
     */
    private StackProducer left;

    /**
     * The left operand of the operation.
     */
    private StackProducer right;

    /**
     * The JVM opcode corresponding to this operation.
     */
    private final int opcode;

    /**
     * Creates a <code>ComparisonOp</code> instance representing a binary
     * comparison operation.
     *
     * @param   left    the left operand of the operation
     * @param   right   the right operand of the operation
     * @param   opcode  the Squawk opcode corresponding to the operation
     */
    public ComparisonOp(StackProducer left, StackProducer right, int opcode) {
        super(Klass.INT);
        this.left   = left;
        this.right  = right;
        this.opcode = opcode;
    }

    /**
     * Gets the left operand of this comparison operation.
     *
     * @return the left operand of this comparison operation
     */
    public StackProducer getLeft() {
        return left;
    }

    /**
     * Gets the right operand of this comparison operation.
     *
     * @return the right operand of this comparison operation
     */
    public StackProducer getRight() {
        return right;
    }

    /**
     * Gets the Squawk opcode corresponding this comparison operation.
     *
     * @return the Squawk opcode corresponding this comparison operation
     */
    public int getOpcode() {
        Assert.that(opcode >= 0);
        return opcode;
    }

    /**
     * Returns true if doing a long comparison (performed by a native method...).
     */
    public boolean isLCMP() {
        return (opcode == LCMP);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Only the LCMP veriosn may constrain the stack, and only if it isn't later replaced by an "if".
     * This could be optimized a little by implementing
     * some kind of opcode lookahead.
     */
    public boolean constrainsStack() {
        return isLCMP();
    }

    /**
     * {@inheritDoc}
     */
    public void visit(InstructionVisitor visitor) {
        visitor.doComparisonOp(this);
    }

    /**
     * {@inheritDoc}
     */
    public void visit(OperandVisitor visitor) {
        left = visitor.doOperand(this, left);
        right = visitor.doOperand(this, right);
    }
}
