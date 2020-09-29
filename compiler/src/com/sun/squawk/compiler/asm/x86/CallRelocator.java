/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: CallRelocator.java,v 1.3 2005/01/21 23:10:19 dl156546 Exp $
 */

package com.sun.squawk.compiler.asm.x86;

import com.sun.squawk.compiler.asm.*;

/**
 * Class to record call/jmp site locations when fixup is required.
 */
public class CallRelocator extends Relocator {

    /**
     * Create a relocator for a call, jmp, or jcc instruction.
     *
     * @param size      the number of bytes to the address
     * @param dst       the absolute destination address
     * @param position  the offset into the code buffer
     * @param next      pointer to the next Relocator
     */
    public CallRelocator(int size, int dst, int position, Relocator next) {
        super(position+size, next);
        value = dst - 4 - position - size;
    }

    /**
     * Emit the relocation into the code buffer.
     *
     * @param asm       the Assembler
     * @param address   the reclocation address for the code
     * @return          the relocation information
     */
    public int emit(Assembler asm, int address) {
        asm.getCode().setCodePos(position);
        asm.emitInt(value - address);
        return (Assembler.RELOC_RELATIVE_INT << 24) | position;
    }

}
