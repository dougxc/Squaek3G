/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: $
 */

package com.sun.squawk.compiler.asm.arm;

import com.sun.squawk.compiler.asm.*;

/**
 * Represents the flexible second operand expression of an ARM data processing and logic instruction
 * such as <code>mov</code> or <code>cmp</code>. It is an abstraction used to represent any of the
 * 11 possible variants of &lt;shifter_operand>:
 * <pre>
 *     #&lt;immediate>
 *     &lt;Rm>
 *     &lt;Rm>, lsl #&lt;shift_imm>
 *     &lt;Rm>, lsl &lt;Rs>
 *     &lt;Rm>, lsr #&lt;shift_imm>
 *     &lt;Rm>, lsr &lt;Rs>
 *     &lt;Rm>, asr #&lt;shift_imm>
 *     &lt;Rm>, asr &lt;Rs>
 *     &lt;Rm>, ror #&lt;shift_imm>
 *     &lt;Rm>, ror &lt;Rs>
 *     &lt;Rm>, rrx
 * </pre>
 * <p>
 * Flexible operands objects are created using one of the following factory methods based upon the
 * shift operation performed on <code>Rm</code>:
 * <ul>
 *     <li><code>imm()</code> - immediate value, no shift
 *     <li><code>reg()</code> - register value, no shift
 *     <li><code>lsl()</code> - logical shift left
 *     <li><code>lsr()</code> - logical shift right
 *     <li><code>asr()</code> - arithmetic shift right
 *     <li><code>ror()</code> - rotate right
 *     <li><code>rrx()</code> - rotate with extend
 * </ul>
 *
 * @author   David Liu
 * @version  1.00
 */
public class Operand2 implements Constants {
    private Operand2(int type, int imm, Register value, Register shift) {
        this.type = type;
        this.imm = imm;
        this.val = value;
        this.sft = shift;
    }

    /**
     * Creates and returns an "immediate" operand.
     * <p>
     * Assembler example:   <pre>    #123</pre>
     * Operand2 equivalent: <pre>    Operand2.imm(123)</pre>
     *
     * @param imm8r immediate value
     */
    public static Operand2 imm(int imm8r) {
        return new Operand2(OPER2_IMM, imm8r, null, null);
    }

    /**
     * Creates and returns a "register" operand.
     * <p>
     * Assembler example:   <pre>    r1</pre>
     * Operand2 equivalent: <pre>    Operand2.reg(asm.R1)</pre>
     *
     * @param reg register value
     * @return the new operand
     */
    public static Operand2 reg(Register reg) {
        return new Operand2(OPER2_REG, 0, reg, null);
    }

    /**
     * Creates and returns a "logical shift left by immediate" operand.
     * <p>
     * Assembler example:   <pre>    r1, lsl #15</pre>
     * Operand2 equivalent: <pre>    Operand2.lsl(asm.R1, 15)</pre>
     *
     * @param reg register whose value is to be shifted
     * @param sft value of the shift (0 - 31)
     * @return the new operand
     */
    public static Operand2 lsl(Register reg, int sft) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(sft >= 0 && sft <= 31, "invalid shift value");
        }

        return new Operand2(OPER2_LSL_IMM, sft, reg, null);
    }

    /**
     * Creates and returns a "logical shift left by register" operand.
     * <p>
     * Assembler example:   <pre>    r1, lsl r2</pre>
     * Operand2 equivalent: <pre>    Operand2.lsl(asm.R1, asm.R2)</pre>
     *
     * @param reg register whose value is to be shifted
     * @param sft value of the shift
     * @return the new operand
     */
    public static Operand2 lsl(Register reg, Register sft) {
        return new Operand2(OPER2_LSL_REG, 0, reg, sft);
    }

    /**
     * Creates and returns a "logical shift right by immediate" operand.
     * <p>
     * Assembler example:   <pre>    r1, lsr #13</pre>
     * Operand2 equivalent: <pre>    Operand2.lsr(asm.R1, 13)</pre>
     *
     * @param reg register whose value is to be shifted
     * @param sft value of the shift (1 - 32)
     * @return the new operand
     */
    public static Operand2 lsr(Register reg, int sft) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(sft >= 1 && sft <= 32, "invalid shift value");
        }

        return new Operand2(OPER2_LSR_IMM, sft, reg, null);
    }

    /**
     * Creates and returns a "logical shift right by register" operand.
     * <p>
     * Assembler example:   <pre>    r1, lsr r2</pre>
     * Operand2 equivalent: <pre>    Operand2.lsr(asm.R1, asm.R2)</pre>
     *
     * @param reg register whose value is to be shifted
     * @param sft register containing the value of the shift
     * @return the new operand
     */
    public static Operand2 lsr(Register reg, Register sft) {
        return new Operand2(OPER2_LSR_REG, 0, reg, sft);
    }

    /**
     * Creates and returns an "arithmetic shift right by immediate" operand.
     * <p>
     * Assembler example:   <pre>    r1, asr #15</pre>
     * Operand2 equivalent: <pre>    Operand2.asr(asm.R1, 15)</pre>
     *
     * @param reg register whose value is to be shifted
     * @param sft value of the shift (1 - 32)
     * @return the new operand
     */
    public static Operand2 asr(Register reg, int sft) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(sft >= 1 && sft <= 32, "invalid shift value");
        }

        return new Operand2(OPER2_ASR_IMM, sft, reg, null);
    }

    /**
     * Creates and returns an "arithmetic shift right by register" operand.
     * <p>
     * Assembler example:   <pre>    r1, asr r2</pre>
     * Operand2 equivalent: <pre>    Operand2.asr(asm.R1, asm.R2)</pre>
     *
     * @param reg register whose value is to be shifted
     * @param sft register containing the value of the shift
     * @return the new operand
     */
    public static Operand2 asr(Register reg, Register sft) {
        return new Operand2(OPER2_ASR_REG, 0, reg, sft);
    }

    /**
     * Creates and returns a "rotate right by immediate" operand.
     * <p>
     * Assembler example:   <pre>    r1, ror #13</pre>
     * Operand2 equivalent: <pre>    Operand2.ror(asm.R1, 13)</pre>
     *
     * @param reg register whose value is to be rotated
     * @param rot value of the rotation
     * @return the new operand
     */
    public static Operand2 ror(Register reg, int rot) {
        return new Operand2(OPER2_ROR_IMM, rot, reg, null);
    }

    /**
     * Creates and returns a "rotate right by register" operand.
     * <p>
     * Assembler example:   <pre>    r1, ror r2</pre>
     * Operand2 equivalent: <pre>    Operand2.ror(asm.R1, asm.R2)</pre>
     *
     * @param reg register whose value is to be rotated
     * @param sft register containing the value of the rotation
     * @return Operand2
     */
    public static Operand2 ror(Register reg, Register sft) {
        return new Operand2(OPER2_ROR_REG, 0, reg, sft);
    }

    /**
     * Creates and returns a "rotate right with extend" operand.
     * <p>
     * Assembler example:   <pre>    r1, rrx</pre>
     * Operand2 equivalent: <pre>    Operand2.rrx(asm.R1)</pre>
     *
     * @param reg register whose value is shifted right by one bit
     * @return the new operand
     */
    public static Operand2 rrx(Register reg) {
        return new Operand2(OPER2_RRX, 0, reg, null);
    }

    private int type;

    /**
     * Returns the type code of this operand expression. The type codes are defined in the ]
     * {@link Constants} interface.
     *
     * @return type code for this operand
     */
    public int getType() {
        return type;
    }

    private int imm;

    /**
     * Returns the immediate value used in this operand expression. Only values that can be
     * represented as a combination of an 8-bit immediate and a rotation of an even number of bits
     * are valid.
     *
     * @return the immediate value
     */
    public int getImm() {
        return imm;
    }

    private Register val;

    /**
     * Returns the register whose value is to be used in this operand expression.
     *
     * @return register that will be used in this operand expression
     */
    public Register getReg() {
        return val;
    }

    private Register sft;

    /**
     * Returns the register whose value contains the value of the shift in this operand expression.
     *
     * @return register containing the value of the shift
     */
    public Register getShift() {
        return sft;
    }
}
