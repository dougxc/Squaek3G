/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: HelloWorld.java,v 1.3 2005/02/03 00:56:40 dl156546 Exp $
 */

package com.sun.squawk.compiler.asm.arm.tests;

import com.sun.squawk.compiler.asm.*;
import com.sun.squawk.compiler.asm.arm.*;
import java.io.*;

/**
 * Generates a simple ARM program that prints a "Hello World!" message to the console using the ARM
 * assembler. The resultant binary, <code>HelloWorld.bin</code>, can be executed using the GNU
 * arm-elf cross compiler tools:
 * <pre>
 *     arm-elf-objcopy -I binary -O elf32-bigarm -B ARM --adjust-vma 0x8000 --set-section-flags .data=alloc,code,readonly HelloWorld.bin HelloWorld.elf
 *     arm-elf-run HelloWorld.elf
 * </pre>
 *
 * @author David Liu
 * @version 1.0
 */

public class HelloWorld {
    public HelloWorld() {
    }

    public static void main (String[] args) throws java.io.FileNotFoundException, java.io.IOException {
        CodeBuffer buffer = new CodeBuffer();
        ARMAssembler asm = new ARMAssembler(buffer);

        System.out.println("HelloWorld:");

        /*
         * Assemble the program
         */
        ALabel hello = asm.newLabel();
        ALabel helloAddr = asm.newLabel();

        asm.ldr(asm.R0, Address2.pre(helloAddr));
        asm.swi(asm.SWI_Write0);

        asm.swi(asm.SWI_Exit);

        asm.bind(hello);
        asm.emitByte('H');
        asm.emitByte('e');
        asm.emitByte('l');
        asm.emitByte('l');
        asm.emitByte('o');
        asm.emitByte(' ');
        asm.emitByte('W');
        asm.emitByte('o');
        asm.emitByte('r');
        asm.emitByte('l');
        asm.emitByte('d');
        asm.emitByte('!');
        asm.emitByte('\0');

        asm.align();
        asm.bind(helloAddr);
        asm.emitLabel(hello);

        asm.relocate(asm.DEFAULT_ENTRY_POINT);

        /*
         * Disassemble the code
         */
        ARMDisassembler dis = new ARMDisassembler(buffer);
        dis.disassemble(asm.getCodeBegin(), asm.getCodePos());

        /*
         * Save the generated machine code to a file
         */
        FileOutputStream fos = new FileOutputStream("HelloWorld.bin");
        fos.write(buffer.getBytes(), 0, buffer.getCodeSize());
        fos.close();

        System.out.println();
    }
}
