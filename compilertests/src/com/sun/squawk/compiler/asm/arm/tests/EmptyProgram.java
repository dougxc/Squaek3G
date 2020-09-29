/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: EmptyProgram.java,v 1.3 2005/02/03 00:56:40 dl156546 Exp $
 */

package com.sun.squawk.compiler.asm.arm.tests;

import com.sun.squawk.compiler.asm.*;
import com.sun.squawk.compiler.asm.arm.*;
import java.io.*;

/**
 * Generates a trivial ARM program that terminates immediately using the ARM assembler. The resultant
 * binary, <code>EmptyProgram.bin</code>, can be executed using the GNU arm-elf cross compiler tools:
 * <pre>
 *     arm-elf-objcopy -I binary -O elf32-bigarm -B ARM --adjust-vma 0x8000 --set-section-flags .data=alloc,code,readonly EmptyProgram.bin EmptyProgram.elf
 *     arm-elf-run EmptyProgram.elf
 * </pre>
 *
 * @author David Liu
 * @version 1.0
 */
public class EmptyProgram {
    public EmptyProgram() {
    }

    public static void main (String[] args) throws java.io.FileNotFoundException, java.io.IOException {
        CodeBuffer buffer = new CodeBuffer();
        ARMAssembler asm = new ARMAssembler(buffer);

        System.out.println("EmptyProgram:");

        /*
         * Assemble the program
         */
        asm.swi(asm.SWI_Exit);

        asm.relocate(asm.DEFAULT_ENTRY_POINT);

        /*
         * Disassemble the code
         */
        ARMDisassembler dis = new ARMDisassembler(buffer);
        dis.disassemble(asm.getCodeBegin(), asm.getCodePos());

        /*
         * Save the generated machine code to a file
         */
        FileOutputStream fos = new FileOutputStream("EmptyProgram.bin");
        fos.write(buffer.getBytes(), 0, buffer.getCodeSize());
        fos.close();

        System.out.println();
    }
}
