/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: Address3.java,v 1.3 2005/01/26 03:00:01 dl156546 Exp $
 */

package com.sun.squawk.compiler.asm.arm;

import com.sun.squawk.compiler.asm.*;

/**
 * Represents an ARM Addressing Mode 3 address used by the ARM assembler for loading and storing
 * halfwords, signed bytes and doublewords. This class supports all six variants of Mode 3 addresses:
 * <pre>
 *     [&lt;Rn>, #+/-&lt;offset_8>]
 *     [&lt;Rn>, +/-&lt;Rm>]
 *     [&lt;Rn>, #+/-&lt;offset_8>]!
 *     [&lt;Rn>, +/-&lt;Rm>]!
 *     [&lt;Rn>], #+/-&lt;offset_8>
 *     [&lt;Rn>], +/-&lt;Rm>
 * </pre>
 * <p>
 * Addresses are constructed using one of the overloaded <code>pre</code>, <code>preW</code> or
 * <code>post</code> factory methods, which represent pre-indexed, pre-indexed with base register
 * update and post-indexed addressing, respectively.
 *
 * @author   David Liu
 * @version  1.00
 */
public class Address3 implements Constants {
    /**
     * Constructs a Mode 3 address.
     *
     * @param preIndexed true for pre-indexed mode, false for post-indexed
     * @param type type code for the addressing mode used
     * @param base base register
     * @param offset positive/negative 8-bit immediate offset
     * @param index index register
     * @param label memory location that this address points to
     * @param updateBase specifies whether the base register should be updated
     */
    private Address3(boolean preIndexed, int type, Register base, int offset, Register index, ALabel label, boolean updateBase) {
        Assert.that(offset >= -255 && offset <= 255, "invalid immediate offset");
        Assert.that(preIndexed || !updateBase, "only pre-indexed addresses can have the base register updated");

        this.preIndexed = preIndexed;
        this.type = type;
        this.base = base;
        this.offset = offset;
        this.index = index;
        this.label = label;
        this.updateBase = updateBase;
    }

    /**
     * Constructs an address of the form <code>[label]</code>.
     * <p>
     * Assembler example:   <pre>    [label]</pre>
     * Address2 equivalent: <pre>    Address3.pre(label)</pre>
     *
     * @param label memory location where data is to be loaded from or stored to
     * @return the new address
     */
    public static Address3 pre(ALabel label) {
        return new Address3 (true, ADDR_IMM, PC, 0, NO_REG, label, false);
    }

    /**
     * Constructs and returns an Addressing Mode 3 address of the form <code>[Rn]</code>.
     * <p>
     * Assembler example:   <pre>    [r1]</pre>
     * Address3 equivalent: <pre>    Address3.pre(asm.R1)</pre>
     *
     * @param base base register
     * @return the new address
     */
    public static Address3 pre(Register base) {
        return new Address3(true, ADDR_IMM, base, 0, NO_REG, null, false);
    }

    /**
     * Constructs and returns an Addressing Mode 3 address of the form <code>[Rn, #+/-&lt;immed_8>]</code>.
     * <p>
     * Assembler example:   <pre>    [r1, #-24]</pre>
     * Address3 equivalent: <pre>    Address3.pre(asm.R1, -24)</pre>
     *
     * @param base base register
     * @param imm8 immediate offset
     * @return the new address
     */
    public static Address3 pre(Register base, int imm8) {
        return new Address3(true, ADDR_IMM, base, imm8, NO_REG, null, false);
    }

    /**
     * Constructs and returns an Addressing Mode 3 address of the form <code>[Rn, +/-Rm]</code>.
     * <p>
     * Assembler example:   <pre>    [r1, -r2]</pre>
     * Address3 equivalent: <pre>    Address3.pre(asm.R1, -1, asm.R2)</pre>
     *
     * @param base base register
     * @param sign whether the offset should be added to (1) or subtracted from (-1) the base
     * @param index index register
     * @return the new address
     */
    public static Address3 pre(Register base, int sign, Register index) {
        Assert.that(sign == 1 || sign == -1, "sign must be 1 or -1");
        return new Address3(true, ADDR_REG, base, sign, index, null, false);
    }

    /**
     * Constructs and returns an Addressing Mode 3 address of the form <code>[Rn, #+/-&lt;immed_8>]!</code>.
     * <p>
     * Assembler example:   <pre>    [r1, #-8]!</pre>
     * Address3 equivalent: <pre>    Address3.preW(asm.R1, -8)</pre>
     *
     * @param base base register
     * @param imm8 immediate offset
     * @return the new address
     */
    public static Address3 preW(Register base, int imm8) {
        return new Address3(true, ADDR_IMM, base, imm8, NO_REG, null, true);
    }

    /**
     * Constructs and returns an Addressing Mode 3 address of the form <code>[Rn, +/-Rm]!</code>.
     * <p>
     * Assembler example:   <pre>    [r1, r2]!</pre>
     * Address3 equivalent: <pre>    Address3.preW(asm.R1, 1, asm.R2)</pre>
     *
     * @param base base register
     * @param sign whether the offset should be added to (1) or subtracted from (-1) the base
     * @param index index register
     * @return the new address
     */
    public static Address3 preW(Register base, int sign, Register index) {
        Assert.that(sign == 1 || sign == -1, "sign must be 1 or -1");
        return new Address3(true, ADDR_REG, base, sign, index, null, true);
    }

    /**
     * Constructs and returns an Addressing Mode 3 address of the form <code>[Rn], #+/-&lt;immed_8></code>.
     * <p>
     * Assembler example:   <pre>    [r1], #16</pre>
     * Address3 equivalent: <pre>    Address3.post(asm.R1, 16)</pre>
     *
     * @param base base register
     * @param imm8 immediate value between -255 and 255
     * @return the new address
     */
    public static Address3 post(Register base, int imm8) {
        return new Address3(false, ADDR_IMM, base, imm8, NO_REG, null, false);
    }

    /**
     * Constructs and returns an Addressing Mode 3 address of the form <code>[Rn], +/-Rm</code>.
     * <p>
     * Assembler example:   <pre>    [r1], -r2</pre>
     * Address3 equivalent: <pre>    Address3.post(asm.R1, -1, asm.R2)</pre>
     *
     * @param base base register
     * @param sign whether the offset should be added to (1) or subtracted from (-1) the base
     * @param index index register
     * @return the new address
     */
    public static Address3 post(Register base, int sign, Register index) {
        Assert.that(sign == 1 || sign == -1, "sign must be 1 or -1");
        return new Address3(false, ADDR_REG, base, sign, index, null, false);
    }

    /**
     * Pre or post indexing.
     */
    private boolean preIndexed;

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
    private int type;

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
    private Register base;

    /**
     * Returns the base register for the address.
     *
     * @return base register
     */
    public Register getBaseReg() {
        return base;
    }

    /**
     * 8 bit immediate offset. This value can be positive or negative.
     */
    private int offset;

    /**
     * Returns the 12-bit immediate offset.
     *
     * @return immediate offset
     */
    public int getOffset() {
        return Math.abs (offset);
    }

    /**
     * Returns the sign of the index register or offset.
     *
     * @return true when the offset is added to the base, false when the offset is subtracted
     */
    public boolean getSign() {
        return offset >= 0;
    }

    /**
     * Index register.
     */
    private Register index;

    /**
     * Returns the index register for the address.
     *
     * @return index register
     */
    public Register getIndexReg () {
        return index;
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
        offset = label.getPos() - base;
        label = null;
    }

    /**
     * Determines if the base register is to be updated after data transfer.
     */
    private boolean updateBase;

    /**
     * Returns whether the base register is to be updated after data transfer.
     *
     * @return true if the base register is to be updated
     */
    public boolean getUpdateBase () {
        return updateBase;
    }
}
