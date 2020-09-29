/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: SubTest.java,v 1.3 2005/02/03 00:56:40 dl156546 Exp $
 */

package com.sun.squawk.compiler.asm.arm.tests;

import com.sun.squawk.compiler.asm.*;
import com.sun.squawk.compiler.asm.arm.*;
import junit.framework.*;

/**
 * Test fixtures for the sub{cond}{s} assembler instructions.
 *
 * @author David Liu
 * @version 1.0
 */
public class SubTest extends TestCase {
    public SubTest() {
    }

    public static void main (String[] args) {
        junit.textui.TestRunner.run (suite());
    }

    public static junit.framework.Test suite() {
        return new TestSuite(SubTest.class);
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
     * Tests the sub instruction.
     */
    public void testSub() {
        asm.sub(asm.R1, asm.R2, Operand2.reg(asm.R3));
        asm.sub(asm.PC, asm.R0, Operand2.asr(asm.SP, 17));

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0xe0, 0x42, 0x10, 0x03,
        0xe0, 0x40, 0xf8, 0xcd }));

        assertEquals("sub r1, r2, r3", dis.disassembleNext());
        assertEquals("sub pc, r0, sp, asr #17", dis.disassembleNext());
    }

    /**
     * Tests the subs instruction.
     */
    public void testSubs() {
        asm.subs(asm.R1, asm.R2, Operand2.reg(asm.R3));
        asm.subs(asm.PC, asm.R0, Operand2.reg(asm.SP));

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0xe0, 0x52, 0x10, 0x03,
        0xe0, 0x50, 0xf0, 0x0d }));

        assertEquals("subs r1, r2, r3", dis.disassembleNext());
        assertEquals("subs pc, r0, sp", dis.disassembleNext());
    }

    /**
     * Tests the subcond instruction.
     */
    public void testSubcond() {
        asm.subcond(asm.COND_EQ, asm.R1, asm.R2, Operand2.reg(asm.R3));
        asm.subcond(asm.COND_HI, asm.PC, asm.R0, Operand2.reg(asm.SP));

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0x00, 0x42, 0x10, 0x03,
        0x80, 0x40, 0xf0, 0x0d }));

        assertEquals("subeq r1, r2, r3", dis.disassembleNext());
        assertEquals("subhi pc, r0, sp", dis.disassembleNext());
    }

    /**
     * Tests the subconds instruction.
     */
    public void testSubconds() {
        asm.subconds(asm.COND_EQ, asm.R1, asm.R2, Operand2.reg(asm.R3));
        asm.subconds(asm.COND_HI, asm.PC, asm.R0, Operand2.lsr(asm.SP, asm.R7));

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0x00, 0x52, 0x10, 0x03,
        0x80, 0x50, 0xf7, 0x3d }));

        assertEquals("subeqs r1, r2, r3", dis.disassembleNext());
        assertEquals("subhis pc, r0, sp, lsr r7", dis.disassembleNext());
    }
}
