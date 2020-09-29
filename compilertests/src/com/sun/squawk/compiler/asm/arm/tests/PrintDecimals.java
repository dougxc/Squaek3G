/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: PrintDecimals.java,v 1.2 2005/02/03 00:56:40 dl156546 Exp $
 */

package com.sun.squawk.compiler.asm.arm.tests;

import com.sun.squawk.compiler.asm.*;
import com.sun.squawk.compiler.asm.arm.*;
import java.io.*;

/**
 * Generates a trivial ARM program that prints hex and decimal numbers using the ARM assembler. The resultant
 * binary, <code>PrintDecimals.bin</code>, can be executed using the GNU arm-elf cross compiler tools:
 * <pre>
 *     arm-elf-objcopy -I binary -O elf32-bigarm -B ARM --adjust-vma 0x8000 --set-section-flags .data=alloc,code,readonly PrintDecimals.bin PrintDecimals.elf
 *     arm-elf-run PrintDecimals.elf
 * </pre>
 *
 * @author David Liu
 * @version 1.0
 */
public class PrintDecimals {
    public PrintDecimals() {
    }

    public static void main (String[] args) throws java.io.FileNotFoundException, java.io.IOException {
        CodeBuffer buffer = new CodeBuffer();
        ARMAssembler asm = new ARMAssembler(buffer);
        ARMLibrary lib = new ARMLibrary(asm);

        ALabel number1 = asm.newLabel();
        ALabel dots = asm.newLabel();
        ALabel dotsStr = asm.newLabel();
        ALabel hex = asm.newLabel();
        ALabel hexStr = asm.newLabel();
        ALabel dec = asm.newLabel();
        ALabel decStr = asm.newLabel();
        ALabel loop = asm.newLabel();
        ALabel done = asm.newLabel();
        ALabel next = asm.newLabel();

        /*
         * Assemble the program
         */
        // print the numbers between 1 and 20
        asm.mov(asm.R7, Operand2.imm(1));
        asm.mov(asm.R8, Operand2.imm(20));

        asm.bind(next);

        asm.mov(asm.R0, Operand2.reg(asm.R7));
        asm.add(asm.R7, asm.R7, Operand2.imm(1));
        asm.bl(lib.printDecimal());

        asm.ldr(asm.R0, Address2.pre(dots));
        asm.swi(asm.SWI_Write0);

        asm.cmp(asm.R7, Operand2.reg(asm.R8));
        asm.bcond(asm.COND_LE, next);

        asm.mov(asm.R0, Operand2.imm(10));
        asm.swi(asm.SWI_WriteC);
        asm.mov(asm.R0, Operand2.imm(13));
        asm.swi(asm.SWI_WriteC);

        // print various numbers in hex and decimal
        asm.ldr(asm.R8, Address2.pre(number1));
        asm.bind(loop);

        // load the next number to print
        asm.add(asm.R8, asm.R8, Operand2.imm(4));
        asm.ldr(asm.R7, Address2.pre(asm.R8));

        // stop printing numbers when we load a negative number
        asm.cmp(asm.R7, Operand2.imm(0));
        asm.bcond(asm.COND_MI, done);

        // print the "Hex: " string
        asm.ldr(asm.R0, Address2.pre(hex));
        asm.swi(asm.SWI_Write0);

        // print the number in hex
        asm.mov(asm.R0, Operand2.reg(asm.R7));
        asm.bl(lib.printHex ());

        // print the ", Decimal: " string
        asm.ldr(asm.R0, Address2.pre(dec));
        asm.swi(asm.SWI_Write0);

        // print the number in decimal
        asm.mov(asm.R0, Operand2.reg(asm.R7));
        asm.bl(lib.printDecimal());

        // newline
        asm.mov(asm.R0, Operand2.imm(13));
        asm.swi(asm.SWI_WriteC);
        asm.mov(asm.R0, Operand2.imm(10));
        asm.swi(asm.SWI_WriteC);

        asm.b(loop);
        asm.bind(done);

        // terminate the program
        asm.swi(asm.SWI_Exit);

        // string constants

        asm.bind(dots);
        asm.emitLabel(dotsStr);

        asm.bind(dotsStr);
        asm.emitByte('.');
        asm.emitByte('.');
        asm.emitByte('.');
        asm.emitByte(' ');
        asm.emitByte(0);
        asm.align();

        asm.bind(hex);
        asm.emitLabel(hexStr);

        asm.bind(hexStr);
        asm.emitByte('H');
        asm.emitByte('e');
        asm.emitByte('x');
        asm.emitByte(':');
        asm.emitByte(' ');
        asm.emitByte(0);
        asm.align();

        asm.bind(dec);
        asm.emitLabel(decStr);

        asm.bind(decStr);
        asm.emitByte(',');
        asm.emitByte(' ');
        asm.emitByte('D');
        asm.emitByte('e');
        asm.emitByte('c');
        asm.emitByte('i');
        asm.emitByte('m');
        asm.emitByte('a');
        asm.emitByte('l');
        asm.emitByte(':');
        asm.emitByte(' ');
        asm.emitByte(0);
        asm.align();

        // numbers to be printed
        asm.bind(number1);
        asm.emitLabel(number1);
        asm.emitInt(0x0);
        asm.emitInt(0x23);
        asm.emitInt(0x1111);
        asm.emitInt(0xcafebab);
        asm.emitInt(0xcafebabe);
        asm.emitInt(-1);

        lib.emit();

        asm.relocate(asm.DEFAULT_ENTRY_POINT);

        /*
         * Disassemble the code
         */
        ARMDisassembler dis = new ARMDisassembler(buffer);
        dis.disassemble(asm.getCodeBegin(), asm.getCodePos());

        /*
         * Save the generated machine code to a file
         */
        FileOutputStream fos = new FileOutputStream("PrintDecimals.bin");
        fos.write(buffer.getBytes(), 0, buffer.getCodeSize());
        fos.close();
    }
}
