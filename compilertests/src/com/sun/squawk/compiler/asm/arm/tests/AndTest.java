/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: AndTest.java,v 1.3 2005/02/03 00:56:40 dl156546 Exp $
 */

package com.sun.squawk.compiler.asm.arm.tests;

import com.sun.squawk.compiler.asm.*;
import com.sun.squawk.compiler.asm.arm.*;
import junit.framework.*;

/**
 * Test fixtures for the and{cond}{s} assembler instructions.
 *
 * @author David Liu
 * @version 1.0
 */
public class AndTest extends TestCase {
    public AndTest() {
    }

    public static void main (String[] args) {
        junit.textui.TestRunner.run (suite());
    }

    public static junit.framework.Test suite() {
        return new TestSuite(AndTest.class);
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
     * Tests the and instruction.
     */
    public void testAnd() {
        asm.and(asm.R1, asm.R2, Operand2.reg(asm.R3));
        asm.and(asm.PC, asm.R0, Operand2.asr(asm.SP, 17));

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0xe0, 0x02, 0x10, 0x03,
        0xe0, 0x00, 0xf8, 0xcd }));

        assertEquals("and r1, r2, r3", dis.disassembleNext());
        assertEquals("and pc, r0, sp, asr #17", dis.disassembleNext());
    }

    /**
     * Tests the ands instruction.
     */
    public void testAnds() {
        asm.ands(asm.R1, asm.R2, Operand2.reg(asm.R3));
        asm.ands(asm.PC, asm.R0, Operand2.asr(asm.SP, 17));

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0xe0, 0x12, 0x10, 0x03,
        0xe0, 0x10, 0xf8, 0xcd }));

        assertEquals("ands r1, r2, r3", dis.disassembleNext());
        assertEquals("ands pc, r0, sp, asr #17", dis.disassembleNext());
    }

    /**
     * Tests the andcond instruction.
     */
    public void testAndcond() {
        asm.andcond(asm.COND_EQ, asm.R1, asm.R2, Operand2.reg(asm.R3));
        asm.andcond(asm.COND_HI, asm.PC, asm.R0, Operand2.asr(asm.SP, 17));

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0x00, 0x02, 0x10, 0x03,
        0x80, 0x00, 0xf8, 0xcd }));

        assertEquals("andeq r1, r2, r3", dis.disassembleNext());
        assertEquals("andhi pc, r0, sp, asr #17", dis.disassembleNext());
    }

    /**
     * Tests the andconds instruction.
     */
    public void testAndconds() {
        asm.andconds(asm.COND_EQ, asm.R1, asm.R2, Operand2.reg(asm.R3));
        asm.andconds(asm.COND_HI, asm.PC, asm.R0, Operand2.asr(asm.SP, 17));

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0x00, 0x12, 0x10, 0x03,
        0x80, 0x10, 0xf8, 0xcd }));

        assertEquals("andeqs r1, r2, r3", dis.disassembleNext());
        assertEquals("andhis pc, r0, sp, asr #17", dis.disassembleNext());
    }
}
