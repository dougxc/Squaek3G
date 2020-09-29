/*
 * Copyright 2004-2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: X86XLocal.java,v 1.6 2005/01/18 23:21:06 ccifue Exp $
 */

package com.sun.squawk.compiler;

import com.sun.squawk.compiler.asm.x86.*;
import com.sun.squawk.compiler.asm.x86.Address; // Disambiguate from class in java.lang


/**
 * X86-specific support for <code>Local</code> variables.
 *
 * @author Cristina Cifuentes
 */
class X86XLocal extends XLocal implements Constants {

    public X86XLocal(Type t, int offset) {
        super(t, offset);
    }

    public X86XLocal(Type t, int offset, boolean isParam) {
        super(t, offset, isParam);
    }

    public Address addressOf() {
        int offset = this.getOffset();
        return new Address(EBP, offset);
    }

    public Address addressOf(int delta) {
        int offset = this.getOffset() + delta;
        return new Address(EBP, offset);
    }

    /**
     * Computes the offset of the Local (parameter or local) in the
     * activation record it belongs to.
     *
     * This function is X86-dependent; it relies on the stack frame format.
     * Parameters have positive offsets from EBP, starting at location 8.
     * Locals have negative offsets from EBP, starting at location -4 (word-aligned)
     * for 32-bit values, and at location -8 for 64-bit values.
     */
    private int getOffset() {
        int offset = super.getSlotOffset();
        switch (super.getType().getStructureSize()) {
            case 4:
                offset = (super.isParam()) ? 8 /* stack frame delta */ + offset : -offset - 4;
                break;
            case 8:
                offset = (super.isParam()) ? 8 /* stack frame delta */ + offset : -offset - 8;
                break;
            default:
                throw new RuntimeException("Getting offset of non-primary type.  Not supported.");
        }

        // ** NB: the offset for locals on the stack has not been tested **/
        return offset;
    }

}

