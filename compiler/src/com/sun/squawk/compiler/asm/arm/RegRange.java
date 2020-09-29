/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: RegRange.java,v 1.1 2005/01/21 23:09:38 dl156546 Exp $
 */

package com.sun.squawk.compiler.asm.arm;

/**
 * Represents a range of one or more general purpose registers. This class is used to represent
 * registers specified in ARM Addressing Mode 4.
 *
 * @see Register
 * @see Address4
 * @author David Liu
 * @version 1.0
 */
public class RegRange {
    /**
     * Creates a range of registers specifying only one register.
     *
     * @param reg Register
     */
    public RegRange(Register reg) {
        regs = new Register [] { reg };
    }

    /**
     * Creates a range of registers including all of the registers between the first and last
     * inclusive.
     *
     * @param first register representing the lower bound of the range
     * @param last register representing the upper bound of the range
     */
    public RegRange(Register first, Register last) {
        if (first.getNumber() >= 0) {
            if (last.getNumber() >= first.getNumber()) {
                regs = new Register [last.getNumber() - first.getNumber() + 1];
                for (int i = 0; i < regs.length; i++) {
                    regs [i] = Constants.REGISTERS [first.getNumber() + i];
                }
            } else {
                regs = new Register [] { first };
            }
        } else {
            regs = new Register [] { };
        }
    }

    /**
     * List of general purpose registers that are in the range in ascending order. This list can
     * be empty.
     */
    public final Register [] regs;
}
