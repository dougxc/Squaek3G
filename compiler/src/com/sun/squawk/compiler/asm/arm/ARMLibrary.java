/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: ARMLibrary.java,v 1.3 2005/02/03 00:56:08 dl156546 Exp $
 */

package com.sun.squawk.compiler.asm.arm;

import com.sun.squawk.compiler.asm.*;

/**
 * Library of ARM code that can be used with and injected into the ARM assembler. This class implements some
 * useful functions that can be called by branching (with link) to the entry points of the supplied
 * functions. Only functions that are actually called are emitted into the code buffer. In
 * the example below, only the <code>integerDivision</code> procedure is called. No library code is
 * actually emitted into the code buffer until the <code>emit</code> method is called.  At that point,
 * this class will emit the assembler code for the functions whose labels were retrieved (these are
 * presumed to be the functions that are actually called); <code>integerDivision</code> in this example.
 * <p>
 * Example:
 * <pre>
 *     CodeBuffer code = new CodeBuffer ();
 *     Assembler asm = new Assembler (code);
 *     ARMLibrary lib = new ARMLibrary (asm);
 *
 *     // ...
 *
 *     asm.bl (lib.integerDivision ());
 *
 *     // ...
 *
 *     lib.emit ();
 * </pre>
 * <p>
 * These procedures output messages to the console using the <a href="../../../../../../../../doc/ARMDemonRDPProtocol.pdf">Remote Debug Protocol</a>
 * (in particular, see section 4.3, page 11) which is dependent upon the Demon debug monitor running in
 * the environment. For example, the GNU ARM simulator also simulates the Demon deubg monitor,
 * printing the output to the console.
 *
 * @author   David Liu
 * @version  1.00
 */
public class ARMLibrary {
    /**
     * Constructs the ARM library object.
     *
     * @param asm assembler that the library functions will be called from
     */
    public ARMLibrary (ARMAssembler asm) {
        this.asm = asm;
    }

    private ARMAssembler asm;
    private ALabel printHexEntry;
    private ALabel printDecimalEntry;
    private ALabel integerDivideEntry;

    /**
     * Emits the code for the library functions that were used into the assembler at the current point
     * in the code buffer. This method must be called <b>after</b> all library calls otherwise the
     * code for some functions may be omitted (resulting in unbound label errors).
     *
     */
    public void emit () {
        asm.align ();
        emitPrintHex ();
        emitPrintDecimal ();
        emitIntegerDivision ();
    }

    /**
     * Returns the label for the entry point of a function to print a word in hexadecimal. This method
     * does not emit code into the code buffer (see {@link #emit()}).
     * <p>
     * Procedure entry:
     * <li><code>R0</code> - number to print
     * <p>
     * Procedure exit:
     * <li><code>R0</code>, <code>R1</code>, <code>R2</code> - destroyed
     *
     * @return entry point of the function
     */
    public ALabel printHex () {
        if (printHexEntry == null) {
            printHexEntry = asm.newLabel();
        }

        return printHexEntry;
    }

    /**
     * Emits the code for the printHex function only if it was called by the user code.
     *
     */
    private void emitPrintHex () {
        if (printHexEntry == null) {
            return ;
        }

        asm.bind(printHexEntry);
        ALabel bufferEnd = asm.newLabel();
        ALabel bufferEndPtr = asm.newLabel();
        ALabel loop = asm.newLabel();

        // save a copy of the registers
        asm.stm(new Address4 (asm.ADDR_FD, asm.SP, true, new RegRange[] {
                              new RegRange (asm.R4, asm.R12), new RegRange (asm.LR) }));

        asm.mov(asm.R1, Operand2.reg(asm.R0));
        asm.ldr(asm.R2, Address2.pre(bufferEndPtr));

        // loop through each hex digit
        asm.bind(loop);

        // strip hex digit from the least significant 4 bits of r1
        asm.and(asm.R0, asm.R1, Operand2.imm(0xf));
        asm.mov(asm.R1, Operand2.lsr(asm.R1, 4));

        // hex digit in r0

        asm.cmp(asm.R0, Operand2.imm(10));
        asm.addcond(asm.COND_LT, asm.R0, asm.R0, Operand2.imm (48));
        asm.addcond(asm.COND_GE, asm.R0, asm.R0, Operand2.imm (87));
        asm.strb(asm.R0, Address2.preW(asm.R2, -1));

        // go to next digit

        asm.cmp(asm.R1, Operand2.imm(0));
        asm.bcond(asm.COND_NE, loop);

        // print the number

        asm.mov(asm.R0, Operand2.imm(48));
        asm.swi(asm.SWI_WriteC);

        asm.mov(asm.R0, Operand2.imm(120));
        asm.swi(asm.SWI_WriteC);

        asm.mov(asm.R0, Operand2.reg(asm.R2));
        asm.swi(asm.SWI_Write0);

        // restore the registers and return
        asm.ldm(new Address4 (asm.ADDR_FD, asm.SP, true, new RegRange[] {
                              new RegRange (asm.R4, asm.R12), new RegRange (asm.PC) }));

        asm.bind(bufferEndPtr);
        asm.emitLabel(bufferEnd);

        asm.emitInt(0);
        asm.emitInt(0);
        asm.bind(bufferEnd);
        asm.emitByte(0);
        asm.align();
    }

    /**
     * Returns the label for the entry point of a function to print a decimal number to the debug
     * monitor (which may in turn output to the console). This method does not emit code into the
     * code buffer (see {@link #emit()}).
     * <p>
     * Procedure entry:
     * <li><code>R0</code> - number to be printed
     * <p>
     * Procedure exit:
     * <li><code>R0</code>, <code>R1</code>, <code>R2</code>, <code>R3</code> - destroyed
     *
     * @return entry point of the function
     */
    public ALabel printDecimal () {
        if (printDecimalEntry == null) {
            printDecimalEntry = asm.newLabel();
        }

        return printDecimalEntry;
    }

    /**
     * Emits the code for the printDecimal function only if it was called by the user code.
     *
     */
    private void emitPrintDecimal() {
        if (printDecimalEntry == null) {
            return;
        }

        asm.bind(printDecimalEntry);
        ALabel numBufferEnd = asm.newLabel();
        ALabel numBufferEndPtr = asm.newLabel();
        ALabel loop = asm.newLabel();

        // save a copy of the registers
        asm.stm(new Address4 (asm.ADDR_FD, asm.SP, true, new RegRange[] {
                              new RegRange (asm.R4, asm.R12), new RegRange (asm.LR) }));

        asm.mov(asm.R1, Operand2.reg(asm.R0));
        asm.mov(asm.R2, Operand2.imm(10));
        asm.ldr(asm.R5, Address2.pre(numBufferEndPtr));

        asm.bind(loop);

        asm.bl(integerDivision ());
        asm.mov(asm.R4, Operand2.reg(asm.R0));
        asm.mov(asm.R0, Operand2.reg(asm.R1));
        asm.add(asm.R0, asm.R0, Operand2.imm(48));
        asm.strb(asm.R0, Address2.preW(asm.R5, -1));

        asm.mov(asm.R1, Operand2.reg(asm.R4));

        asm.cmp(asm.R1, Operand2.imm(0));
        asm.bcond(asm.COND_NE, loop);

        asm.mov(asm.R0, Operand2.reg(asm.R5));
        asm.swi(asm.SWI_Write0);

        // restore the registers and return
        asm.ldm(new Address4 (asm.ADDR_FD, asm.SP, true, new RegRange[] {
                              new RegRange (asm.R4, asm.R12), new RegRange (asm.PC) }));

        asm.bind(numBufferEndPtr);
        asm.emitLabel(numBufferEnd);

        for (int i = 0; i < 15; i++) {
            asm.emitByte(0);
        }
        asm.bind(numBufferEnd);
        asm.emitByte(0);

        asm.align();
    }

    /**
     * Returns the label for the entry point of a function that implements <b>positive</b>
     * integer division. This method does not emit code into the code buffer (see {@link #emit()}).
     * <p>
     * Procedure entry:
     * <li><code>R1</code> - positive number to be divided
     * <li><code>R2</code> - positive divisor
     * <p>
     * Procedure exit:
     * <li><code>R0</code> - result
     * <li><code>R1</code> - remainder
     * <li><code>R2</code>, <code>R3</code> - destroyed
     *
     * @return entry point of the function
     */
    public ALabel integerDivision () {
        if (integerDivideEntry == null) {
            integerDivideEntry = asm.newLabel();
        }

        return integerDivideEntry;
    }

    /**
     * Emits the code for the integerDivision function only if it was called by the user code.
     *
     */
    private void emitIntegerDivision() {
        if (integerDivideEntry == null) {
            return;
        }

        asm.bind(integerDivideEntry);
        ALabel start = asm.newLabel();

        // save a copy of the registers
        asm.stm(new Address4 (asm.ADDR_FD, asm.SP, true, new RegRange[] {
                              new RegRange (asm.R4, asm.R12), new RegRange (asm.LR) }));

        asm.mov(asm.R0, Operand2.imm(0));
        asm.mov(asm.R3, Operand2.imm(1));

        asm.bind(start);
        asm.cmp(asm.R2, Operand2.reg(asm.R1));
        asm.movcond(asm.COND_LS, asm.R2, Operand2.lsl(asm.R2, 1));
        asm.movcond(asm.COND_LS, asm.R3, Operand2.lsl(asm.R3, 1));
        asm.bcond(asm.COND_LS, start);

        ALabel next = asm.newLabel();
        asm.bind(next);
        asm.cmp(asm.R1, Operand2.reg(asm.R2));
        asm.subcond(asm.COND_CS, asm.R1, asm.R1, Operand2.reg(asm.R2));
        asm.addcond(asm.COND_CS, asm.R0, asm.R0, Operand2.reg(asm.R3));

        asm.movs(asm.R3, Operand2.lsr(asm.R3, 1));
        asm.movcond(asm.COND_CC, asm.R2, Operand2.lsr(asm.R2, 1));
        asm.bcond(asm.COND_CC, next);

        // restore the registers and return
        asm.ldm(new Address4 (asm.ADDR_FD, asm.SP, true, new RegRange[] {
                              new RegRange (asm.R4, asm.R12), new RegRange (asm.PC) }));
    }
}
