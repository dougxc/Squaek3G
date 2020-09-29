/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: Address4.java,v 1.3 2005/01/26 03:00:01 dl156546 Exp $
 */

package com.sun.squawk.compiler.asm.arm;

import com.sun.squawk.compiler.asm.*;

/**
 * Represents an ARM Addressing Mode 4 address used by the ARM assembler for loading and storing
 * subsets of the general purpose registers to memory. Mode 4 addresses are of the following form:
 * <pre>
 *     &lt;instr>{&lt;cond>}&lt;addressing_mode> &lt;Rn>{!}, &lt;registers>{^}
 * </pre>
 * <p>
 * This class encapulsates the following address information:
 * <ul>
 *     <li><code>addressing_mode</code> - see the Mode 4 section in the {@link Constants} class
 *         (for example <code>ADDR_IA</code>).
 *     <li><code>Rn</code> - base register containing the base address where the first register
 *         will be loaded from / stored to.
 *     <li><code>!</code> - whether the base register will be updated after the transfer by
 *         the number of bytes transferred.
 *     <li><code>registers</code> - subset of some or all of the registers to be transferred.
 * </ul>
 * <p>
 * The <code>^</code> flag (used to specify loading of user mode registers) is not currently supported.
 * <p>
 * Address objects are created using either of the two constructors. The only difference between
 * them is that one allows the base register update flag to be explicitly specified.
 *
 * @author   David Liu
 * @version  1.00
 */
public class Address4 implements Constants {
    /**
     * Constructs an address of the form <code>&lt;addressing_mode> &lt;Rn>, &lt;registers></code>. The
     * base register will not be updated after the transfer.
     * <p>
     * Assembler example:   <pre>    da sp, { r0-r3, r4 }</pre>
     * Address4 equivalent: <pre>    new Address4(asm.ADDR_DA, asm.SP, new RegRange [] { new RegRange (asm.R0, asm.R3), new RegRange (asm.R4)})</pre>
     *
     * @param mode addressing mode (see the ADDR_xx constants for Addressing Mode 4 in {@link Constants})
     * @param base base register used in the address
     * @param regs registers to be loaded/stored
     */
    public Address4(int mode, Register base, RegRange [] regs) {
        this(mode, base, false, regs);
    }

    /**
     * Constructs an address of the form <code>&lt;addressing_mode> &lt;Rn>{!}, &lt;registers></code>.
     * <p>
     * Assembler example:   <pre>    da sp!, { r0-r3, r4 }</pre>
     * Address4 equivalent: <pre>    new Address4(asm.ADDR_DA, asm.SP, true, new RegRange [] { new RegRange (asm.R0, asm.R3), new RegRange (asm.R4)})</pre>
     *
     * @param mode addressing mode (see the ADDR_xx constants for Addressing Mode 4 in {@link Constants})
     * @param base base register used in the address
     * @param updateBase determines if the base register should be updated after executing the instruction
     * @param regs registers to be loaded/stored
     */
    public Address4(int mode, Register base, boolean updateBase, RegRange [] regs) {
        Assert.that(regs.length > 0, "at least one register must be specified for loading/storing");

        this.mode = mode;
        this.base = base;
        this.updateBase = updateBase;
        this.regsBits = 0;

        for (int i = 0; i < regs.length; i++) {
            for (int j = 0; j < regs [i].regs.length; j++) {
                regsBits |= (1 << regs [i].regs [j].getNumber());
            }
        }
    }

    /**
     * Addressing mode used.
     */
    private int mode;

    /**
     * Calculates and returns the value of the P bit for this address, which has two meanings:
     *
     *     p == 1    indicates that the word addressed by the base register is included in the
     *               range of memory locations accessed, lying at the top (u == 0) or bottom
     *               (u == 1) of that range.
     *
     *     p == 0    indicates that the word addressed by the base register is excluded from the
     *               range of memory locations accessed, and lies on word beyond the top of the
     *               range (u == 0) or one word below the bottom of the range (u == 1).
     *
     * @param l specifies if a load (l == 1) or store (l == 0) operation is to be performed
     * @return value of the P bit (0 or 1)
     */
    public int getPBit(int l) {
        return ((mode == ADDR_DB) || (mode == ADDR_IB) ||
                 ((l == 1) && (mode == ADDR_EA || mode == ADDR_ED)) ||
                 ((l == 0) && (mode == ADDR_FD || mode == ADDR_FA))) ? 1 : 0;
    }

    /**
     * Calculates and returns the value of the U bit for this address, which indicates that the
     * transfer is made upwards (u == 1) or downwards (u == 0) from the base register.
     *
     * @param l specifies if a load (l == 1) or store (l == 0) operation is to be performed
     * @return value of the U bit (0 or 1)
     */
    public int getUBit(int l) {
        return ((mode == ADDR_IA) || (mode == ADDR_IB) ||
                 ((l == 1) && (mode == ADDR_FD || mode == ADDR_ED)) ||
                 ((l == 0) && (mode == ADDR_EA || mode == ADDR_FA))) ? 1 : 0;
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
     * Whether the base register should be updated after the transfer.
     */
    private boolean updateBase;

    /**
     * Returns whether the base register should be updated after the transfer.
     *
     * @return 1 if the base register is to be updated, 0 otherwise
     */
    public int getUpdateBaseBit() {
        return updateBase ? 1 : 0;
    }

    /**
     * Bitfield specifying which registers are to be loaded/stored.
     */
    private int regsBits;

    /**
     * Returns the registers to be loaded or stored, encoded in a bitfield with R0 at bit 0 to
     * R15 at bit 15.
     *
     * @return bitfield specifying which registers are to be loaded/stored
     */
    public int getRegsBits() {
        return regsBits;
    }
}
