/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: Address2.java,v 1.3 2005/01/26 03:00:01 dl156546 Exp $
 */

package com.sun.squawk.compiler.asm.arm;

import com.sun.squawk.compiler.asm.*;

/**
 * Represents an ARM Addressing Mode 2 address used by the ARM assembler for loading and storing
 * words and unsigned bytes. This class supports all nine address variants of Mode 2:
 * <pre>
 *     [&lt;Rn>, +/-&lt;offset_12>]
 *     [&lt;Rn>, +/-&lt;Rm>]
 *     [&lt;Rn>, +/-&lt;Rm>, &lt;shift> #&lt;shift_imm>]
 *     [&lt;Rn>, #+/-&lt;offset_12>]!
 *     [&lt;Rn>, +/-&lt;Rm>]!
 *     [&lt;Rn>, +/-&lt;Rm>, &lt;shift> #&lt;shift_imm>]!
 *     [&lt;Rn>], #+/-&lt;offset_12>
 *     [&lt;Rn>], +/-&lt;Rm>]
 *     [&lt;Rn>], +/-&lt;Rm>, &lt;shift> #&lt;shift_imm>
 * </pre>
 * <p>
 * Addresses are constructed using one of the overloaded <code>pre</code>, <code>preW</code> or
 * <code>post</code> factory methods, which represent pre-indexed, pre-indexed with base register
 * update and post-indexed addressing, respectively.
 *
 * @author   David Liu
 * @version  1.00
 */
public class Address2 implements Constants {
    /**
     * Constructs an ARM Addresing Mode 2 address.
     *
     * @param preIndexed true for pre-indexed mode, false for post-indexed
     * @param type type code for the addressing mode used
     * @param base base register
     * @param index index register
     * @param immOffset immediate offset (+/- 12-bit immediate) or sign for register index (1 or -1)
     * @param label memory location that this address points to
     * @param updateBase specifies whether the base register should be updated
     * @param scaleMode shift operation to use (LSL, LSR, ASR, ROR)
     * @param shift number of bits to shift
     */
    private Address2(boolean preIndexed, int type, Register base, Register index, int immOffset, ALabel label, boolean updateBase, int scaleMode, int shift) {
        Assert.that(preIndexed || !updateBase, "base register can only be updated with pre-indexed addressing");
        Assert.that(type == ADDR_IMM || (immOffset == 1 || immOffset == -1), "immOffset must be 1 or -1 when used with register index");
        Assert.that(type != ADDR_SCALE || scaleMode != LSL || (shift >= 0 && shift <= 31), "invalid shift value");
        Assert.that(type != ADDR_SCALE || scaleMode != LSR || (shift >= 1 && shift <= 32), "invalid shift value");
        Assert.that(type != ADDR_SCALE || scaleMode != ASR || (shift >= 1 && shift <= 32), "invalid shift value");
        Assert.that(type != ADDR_SCALE || scaleMode != ROR || (shift >= 0 && shift <= 31), "invalid shift value");
        Assert.that((Math.abs(immOffset) & 0xFFF) == Math.abs(immOffset), "invalid immediate offset");

        this.preIndexed = preIndexed;
        this.type = type;
        this.base = base;
        this.index = index;
        this.immOffset = immOffset;
        this.label = label;
        this.updateBase = updateBase;
        this.scaleMode = scaleMode;
        this.shift = shift;
    }

    /**
     * Constructs an address of the form <code>[label]</code>.
     * <p>
     * Assembler example:   <pre>    [label]</pre>
     * Address2 equivalent: <pre>    Address2.pre(label)</pre>
     *
     * @param label memory location where data is to be loaded from or stored to
     * @return the new address
     */
    public static Address2 pre(ALabel label) {
        return new Address2 (true, ADDR_IMM, PC, NO_REG, 0, label, false, 0, 0);
    }

    /**
     * Constructs an address of the form <code>[&lt;Rn>]</code>.
     * <p>
     * Assembler example:   <pre>    [r4]</pre>
     * Address2 equivalent: <pre>    Address2.pre(asm.R4)</pre>
     *
     * @param base base register
     * @return the new address
     */
    public static Address2 pre(Register base) {
        return new Address2 (true, ADDR_IMM, base, NO_REG, 0, null, false, 0, 0);
    }

    /**
     * Constructs an address of the form <code>[&lt;Rn>, #+/-&lt;offset_12>]</code>.
     * <p>
     * Assembler example:   <pre>    [r4, #-0xff3]</pre>
     * Address2 equivalent: <pre>    Address2.pre(asm.R4, -0xff3)</pre>
     *
     * @param base base register
     * @param imm12 positive/negative immediate 12-bit offset
     * @return the new address
     */
    public static Address2 pre(Register base, int imm12) {
        return new Address2 (true, ADDR_IMM, base, NO_REG, imm12, null, false, 0, 0);
    }

    /**
     * Constructs an address of the form <code>[&lt;Rn>, +/-&lt;Rm>]</code>.
     * <p>
     * Assembler example:   <pre>    [r4, -r5]</pre>
     * Address2 equivalent: <pre>    Address2.pre(asm.R4, -1, asm.R5)</pre>
     *
     * @param base base register
     * @param sign whether the offset should be added to (1) or subtracted from (-1) the base
     * @param index index register
     * @return the new address
     */
    public static Address2 pre(Register base, int sign, Register index) {
        return new Address2 (true, ADDR_REG, base, index, sign, null, false, 0, 0);
    }

    /**
     * Constructs an address of the form <code>[&lt;Rn>, +/-&lt;Rm>, &lt;shift> #&lt;shift_imm>]</code>.
     * <p>
     * Assembler example:   <pre>    [r4, -r5, asr #11]</pre>
     * Address2 equivalent: <pre>    Address2.pre(asm.R4, -1, asm.R5, asm.ASR, 11)</pre>
     *
     * @param base base register
     * @param sign whether the offset should be added to (1) or subtracted from (-1) the base
     * @param index index register
     * @param shift shift operation to perform (LSL, LSR, ASR, ROR)
     * @param imm number of bits to shift (0 - 32 depending upon shift operation)
     * @return the new address
     */
    public static Address2 pre(Register base, int sign, Register index, int shift, int imm) {
        Assert.that (shift == LSL || shift == LSR || shift == ASR || shift == ROR,
                     "invalid shift operation specified");
        return new Address2 (true, ADDR_SCALE, base, index, sign, null, false, shift, imm);
    }

    /**
     * Constructs an address of the form <code>[&lt;Rn>, #+/-&lt;offset_12>]!</code>.
     * <p>
     * Assembler example:   <pre>    [r4, #123]!</pre>
     * Address2 equivalent: <pre>    Address2.preW(asm.R4, 123)</pre>
     *
     * @param base base register
     * @param imm12 postive/negative immediate 12-bit offset
     * @return the new address
     */
    public static Address2 preW(Register base, int imm12) {
        return new Address2 (true, ADDR_IMM, base, NO_REG, imm12, null, true, 0, 0);
    }

    /**
     * Constructs an address of the form <code>[&lt;Rn>, +/-&lt;Rm>]!</code>.
     * <p>
     * Assembler example:   <pre>    [r4, r5]!</pre>
     * Address2 equivalent: <pre>    Address2.preW(asm.R4, 1, asm.R5)</pre>
     *
     * @param base base register
     * @param sign whether the offset should be added to (1) or subtracted from (-1) the base
     * @param index index register
     * @return the new address
     */
    public static Address2 preW(Register base, int sign, Register index) {
        return new Address2 (true, ADDR_REG, base, index, sign, null, true, 0, 0);
    }

    /**
     * Constructs an address of the form <code>[&lt;Rn>, +/-&lt;Rm>, &lt;shift> #&lt;shift_imm>]!</code>.
     * Rotate with extend (RRX) is specified by shifting 0 bits using ROR.
     * <p>
     * Assembler example:   <pre>    [r4, r5, lsl #17]!</pre>
     * Address2 equivalent: <pre>    Address2.preW(asm.R4, 1, asm.R5, asm.LSL, 17)</pre>
     *
     * @param base base register
     * @param sign whether the offset should be added to (1) or subtracted from (-1) the base
     * @param index index register
     * @param shift shift operation to perform (LSL, LSR, ASR, ROR)
     * @param imm number of bits to shift (0 - 32 depending upon shift operation)
     * @return the new address
     */
    public static Address2 preW(Register base, int sign, Register index, int shift, int imm) {
        return new Address2 (true, ADDR_SCALE, base, index, sign, null, true, shift, imm);
    }

    /**
     * Constructs an address of the form <code>[&lt;Rn>], #+/-&lt;offset_12></code>.
     * <p>
     * Assembler example:   <pre>    [r4], #123</pre>
     * Address2 equivalent: <pre>    Address2.post(asm.R4, 123)</pre>
     *
     * @param base base register
     * @param imm12 positive/negative immediate 12-bit offset
     * @return the new address
     */
    public static Address2 post(Register base, int imm12) {
        return new Address2 (false, ADDR_IMM, base, NO_REG, imm12, null, false, 0, 0);
    }

    /**
     * Constructs an address of the form <code>[&lt;Rn>], +/-&lt;Rm></code>.
     * <p>
     * Assembler example:   <pre>    [r4], r5</pre>
     * Address2 equivalent: <pre>    Address2.post(asm.R4, 1, asm.R5)</pre>
     *
     * @param base base register
     * @param sign whether the offset should be added to (1) or subtracted from (-1) the base
     * @param index index register
     * @return the new address
     */
    public static Address2 post(Register base, int sign, Register index) {
        return new Address2 (false, ADDR_REG, base, index, sign, null, false, 0, 0);
    }

    /**
     * Constructs an address of the form <code>[&lt;Rn>], +/-&lt;Rm>, &lt;shift> #&lt;shift_imm></code>.
     * <p>
     * Assembler example:   <pre>    [r4], -r5, ror #17</pre>
     * Address2 equivalent: <pre>    Address2.post(asm.R4, -1, asm.R5, asm.ROR, 17)</pre>
     *
     * @param base base register
     * @param sign whether the offset should be added to (1) or subtracted from (-1) the base
     * @param index index register
     * @param shift shift operation to perform (LSL, LSR, ASR, ROR)
     * @param imm number of bits to shift (0 - 32 depending upon shift operation)
     * @return the new address
     */
    public static Address2 post(Register base, int sign, Register index, int shift, int imm) {
        return new Address2 (false, ADDR_SCALE, base, index, sign, null, false, shift, imm);
    }

    /**
     * Pre or post indexing.
     */
    private boolean preIndexed = true;

    /**
     * Determines if the address is pre-indexed (true) or post-indexed (false).
     *
     * @return the indexing mode
     */
    public boolean getPreIndexed() {
        return preIndexed;
    }

    /**
     * Type code of the addressing mode.
     */
    private int type = ADDR_IMM;

    /**
     * Returns the type code of the addressing mode that this object represents.  Refer to the
     * ADDR_xxx constants in the {@link Constants} class.
     * @return the type code
     */
    public int getType() {
        return type;
    }

    /**
     * Base register.
     */
    private Register base = NO_REG;

    /**
     * Returns the base register for the address.
     *
     * @return base register
     */
    public Register getBaseReg() {
        return base;
    }

    /**
     * Index register.
     */
    private Register index = NO_REG;

    /**
     * Returns the index register for the address.
     *
     * @return index register
     */
    public Register getIndexReg () {
        return index;
    }

    /**
     * 12 bit immediate offset. This value can be positive or negative.
     */
    private int immOffset = 0;

    /**
     * Returns the 12-bit immediate offset.
     *
     * @return immediate offset
     */
    public int getOffset() {
        return Math.abs (immOffset);
    }

    /**
     * Memory location label.
     */
    private ALabel label;

    /**
     * Returns the label representing the memory location that this address points to.
     *
     * @return label
     */
    public ALabel getLabel() {
        return label;
    }

    /**
     * Resolves the relative address pointed to by a label into an immediate offset from the PC
     * register.
     *
     * @param base base address of the location where the address is being used
     */
    public void resolveLabel(int base) {
        immOffset = label.getPos() - base;
        label = null;
    }

    /**
     * Returns the sign of the index register or offset.
     *
     * @return true when the offset is added to the base, false when the offset is subtracted
     */
    public boolean getSign() {
        return immOffset >= 0;
    }

    /**
     * Determines if the base register is to be updated after data transfer.
     */
    private boolean updateBase = false;

    /**
     * Returns whether the base register is to be updated after data transfer.
     *
     * @return true if the base register is to be updated
     */
    public boolean getUpdateBase () {
        return updateBase;
    }

    /**
     * Scaling mode.
     */
    private int scaleMode = 0;

    /**
     * Returns the scaling mode to be used.  See the <code>LSL</code>, <code>LSR</code>,
     * <code>ASR</code> and <code>ROR</code> constants in the {@link Constants} class.
     *
     * @return scaling mode used
     */
    public int getScaleMode () {
        return scaleMode;
    }

    /**
     * Number of bits to shift for the scaled modes.
     */
    private int shift = -1;

    /**
     * Returns the number of bits to shift for the scaled modes.
     *
     * @return number of bits to shift
     */
    public int getShift () {
        Assert.that(type == ADDR_SCALE, "shift only applies for scaled modes");

        return shift;
    }
}
