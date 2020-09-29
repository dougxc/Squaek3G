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
 * Relocation record.
 *
 * @author   Nik Shaylor
 * @version  1.00
 */
class Relocator {

    /**
     * The position for this relocator in the code buffer.
     */
    protected int position;

    /**
     * The initial value to be patched.
     */
    protected int value = -1;

    /**
     * The next relocator.
     */
    protected Relocator next;

    /**
     * Create a relocator that will fixup a 4 byte word in the code buffer.
     *
     * @param position  the position in the code buffer
     * @param next      the next relocator in the chain
     */
    public Relocator(int position, Relocator next) {
        this.position = position;
        this.next     = next;
    }

    /**
     * Setup the initial value the word will be set to. This value is added to
     * the code relocation address before it is output.
     *
     * @param value  the initial value the word should be set to
     */
    public void setValue(int value) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(this.value == -1, "relocator bound more than once");
        }
        this.value = value;
    }

    /**
     * Get the position of the relocation in the code buffer.
     *
     * @return the code position
     */
    public int getPosition() {
        return position;
    }

    /**
     * Emit the relocation into the code buffer.
     *
     * @param asm       the Assembler
     * @param address   the reclocation address for the code
     * @return          the relocation information
     */
    public int emit(ARMAssembler asm, int address) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(this.value != -1, "relocator not bound");
        }
        asm.getCode().setCodePos(position);
        asm.emitInt(value + address);
        return (ARMAssembler.RELOC_ABSOLUTE_INT << 24) | position;
    }

    /**
     * Get the next relocator in the chain.
     *
     * @return the next relocator or null if there is none
     */
    public Relocator getNext() {
        return next;
    }

}
