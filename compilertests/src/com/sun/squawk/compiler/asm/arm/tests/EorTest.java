/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: EorTest.java,v 1.3 2005/02/03 00:56:40 dl156546 Exp $
 */

package com.sun.squawk.compiler.asm.arm.tests;

import com.sun.squawk.compiler.asm.*;
import com.sun.squawk.compiler.asm.arm.*;
import junit.framework.*;

/**
 * Test fixtures for the eor{cond}{s} assembler instructions.
 *
 * @author David Liu
 * @version 1.0
 */
public class EorTest extends TestCase {
    public EorTest() {
    }

    public static void main (String[] args) {
        junit.textui.TestRunner.run (suite());
    }

    public static junit.framework.Test suite() {
        return new TestSuite(EorTest.class);
    }

    private CodeBuffer buffer;
    private ARMAssembler asm;
    private ARMDisassembler dis;

    protected void setUp() {
        buffer = new CodeBuffer ();
        asm = new ARMAssembler (buffer);
        dis = new ARMDisassembler (buffer);
    }

    /**
     * Tests the eor instruction.
     */
    public void testEor() {
        asm.eor(asm.R1, asm.R2, Operand2.reg(asm.R3));
        asm.eor(asm.PC, asm.R0, Operand2.asr(asm.SP, 17));

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0xe0, 0x22, 0x10, 0x03,
        0xe0, 0x20, 0xf8, 0xcd }));

        assertEquals("eor r1, r2, r3", dis.disassembleNext());
        assertEquals("eor pc, r0, sp, asr #17", dis.disassembleNext());
    }

    /**
     * Tests the eors instruction.
     */
    public void testEors() {
        asm.eors(asm.R1, asm.R2, Operand2.reg(asm.R3));
        asm.eors(asm.PC, asm.R0, Operand2.asr(asm.SP, 17));

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0xe0, 0x32, 0x10, 0x03,
        0xe0, 0x30, 0xf8, 0xcd }));

        assertEquals("eors r1, r2, r3", dis.disassembleNext());
        assertEquals("eors pc, r0, sp, asr #17", dis.disassembleNext());
    }

    /**
     * Tests the eorcond instruction.
     */
    public void testEorcond() {
        asm.eorcond(asm.COND_EQ, asm.R1, asm.R2, Operand2.reg(asm.R3));
        asm.eorcond(asm.COND_HI, asm.PC, asm.R0, Operand2.asr(asm.SP, 17));

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0x00, 0x22, 0x10, 0x03,
        0x80, 0x20, 0xf8, 0xcd }));

        assertEquals("eoreq r1, r2, r3", dis.disassembleNext());
        assertEquals("eorhi pc, r0, sp, asr #17", dis.disassembleNext());
    }

    /**
     * Tests the eorconds instruction.
     */
    public void testEorconds() {
        asm.eorconds(asm.COND_EQ, asm.R1, asm.R2, Operand2.reg(asm.R3));
        asm.eorconds(asm.COND_HI, asm.PC, asm.R0, Operand2.asr(asm.SP, 17));

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0x00, 0x32, 0x10, 0x03,
        0x80, 0x30, 0xf8, 0xcd }));

        assertEquals("eoreqs r1, r2, r3", dis.disassembleNext());
        assertEquals("eorhis pc, r0, sp, asr #17", dis.disassembleNext());
    }
}
