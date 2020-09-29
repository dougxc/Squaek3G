/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: CmpTest.java,v 1.3 2005/02/03 00:56:40 dl156546 Exp $
 */

package com.sun.squawk.compiler.asm.arm.tests;

import com.sun.squawk.compiler.asm.*;
import com.sun.squawk.compiler.asm.arm.*;
import junit.framework.*;

/**
 * Test fixtures for the cmp{cond} assembler instructions.
 *
 * @author David Liu
 * @version 1.0
 */
public class CmpTest extends TestCase {
    public CmpTest() {
    }

    public static void main (String[] args) {
        junit.textui.TestRunner.run (suite());
    }

    public static junit.framework.Test suite() {
        return new TestSuite(CmpTest.class);
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
     * Tests the cmp instruction.
     */
    public void testCmp() {
        asm.cmp(asm.R13, Operand2.imm(0x254));
        asm.cmp(asm.R1, Operand2.reg(asm.R3));
        asm.cmp(asm.R15, Operand2.asr(asm.R13, 17));

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0xe3, 0x5d, 0x0f, 0x95,
        0xe1, 0x51, 0x00, 0x03,
        0xe1, 0x5f, 0x08, 0xcd }));

        assertEquals("cmp sp, #0x254", dis.disassembleNext());
        assertEquals("cmp r1, r3", dis.disassembleNext());
        assertEquals("cmp pc, sp, asr #17", dis.disassembleNext());
    }

    /**
     * Tests the cmpcond instruction.
     */
    public void testCmpcond() {
        asm.cmpcond(asm.COND_CC, asm.R13, Operand2.imm(0x254));
        asm.cmpcond(asm.COND_GT, asm.R1, Operand2.reg(asm.R3));
        asm.cmpcond(asm.COND_PL, asm.R15, Operand2.asr(asm.R13, 17));

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0x33, 0x5d, 0x0f, 0x95,
        0xc1, 0x51, 0x00, 0x03,
        0x51, 0x5f, 0x08, 0xcd }));

        assertEquals("cmpcc sp, #0x254", dis.disassembleNext());
        assertEquals("cmpgt r1, r3", dis.disassembleNext());
        assertEquals("cmppl pc, sp, asr #17", dis.disassembleNext());
    }
}
