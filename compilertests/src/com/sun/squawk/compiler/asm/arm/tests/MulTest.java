/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: MulTest.java,v 1.3 2005/02/03 00:56:40 dl156546 Exp $
 */

package com.sun.squawk.compiler.asm.arm.tests;

import com.sun.squawk.compiler.asm.*;
import com.sun.squawk.compiler.asm.arm.*;
import junit.framework.*;

/**
 * Test fixtures for the mul{cond}{s} assembler instructions.
 *
 * @author David Liu
 * @version 1.0
 */
public class MulTest extends TestCase {
    public MulTest() {
    }

    public static void main (String[] args) {
        junit.textui.TestRunner.run (suite());
    }

    public static junit.framework.Test suite() {
        return new TestSuite(MulTest.class);
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
     * Tests the mul instruction.
     */
    public void testMul() {
        asm.mul(asm.R1, asm.R2, asm.R3);
        asm.mul(asm.LR, asm.R0, asm.R7);

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0xe0, 0x01, 0x03, 0x92,
        0xe0, 0x0e, 0x07, 0x90 }));

        assertEquals("mul r1, r2, r3", dis.disassembleNext());
        assertEquals("mul lr, r0, r7", dis.disassembleNext());
    }

    /**
     * Tests the muls instruction.
     */
    public void testMuls() {
        asm.muls(asm.R1, asm.R2, asm.R3);
        asm.muls(asm.LR, asm.R0, asm.R7);

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0xe0, 0x11, 0x03, 0x92,
        0xe0, 0x1e, 0x07, 0x90 }));

        assertEquals("muls r1, r2, r3", dis.disassembleNext());
        assertEquals("muls lr, r0, r7", dis.disassembleNext());
    }

    /**
     * Tests the mulcond instruction.
     */
    public void testMulcond() {
        asm.mulcond(asm.COND_EQ, asm.R1, asm.R2, asm.R3);
        asm.mulcond(asm.COND_CS, asm.LR, asm.R0, asm.R7);

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0x00, 0x01, 0x03, 0x92,
        0x20, 0x0e, 0x07, 0x90 }));

        assertEquals("muleq r1, r2, r3", dis.disassembleNext());
        assertEquals("mulcs lr, r0, r7", dis.disassembleNext());
    }

    /**
     * Tests the mulconds instruction.
     */
    public void testMulconds() {
        asm.mulconds(asm.COND_EQ, asm.R1, asm.R2, asm.R3);
        asm.mulconds(asm.COND_CS, asm.LR, asm.R0, asm.R7);

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0x00, 0x11, 0x03, 0x92,
        0x20, 0x1e, 0x07, 0x90 }));

        assertEquals("muleqs r1, r2, r3", dis.disassembleNext());
        assertEquals("mulcss lr, r0, r7", dis.disassembleNext());
    }
}
