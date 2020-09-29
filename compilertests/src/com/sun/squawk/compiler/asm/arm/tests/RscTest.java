/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: RscTest.java,v 1.3 2005/02/03 00:56:40 dl156546 Exp $
 */

package com.sun.squawk.compiler.asm.arm.tests;

import com.sun.squawk.compiler.asm.*;
import com.sun.squawk.compiler.asm.arm.*;
import junit.framework.*;

/**
 * Test fixtures for the rsc{cond}{s} assembler instructions.
 *
 * @author David Liu
 * @version 1.0
 */
public class RscTest extends TestCase {
    public RscTest() {
    }

    public static void main (String[] args) {
        junit.textui.TestRunner.run (suite());
    }

    public static junit.framework.Test suite() {
        return new TestSuite(RscTest.class);
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
     * Tests the rsc instruction.
     */
    public void testRsc() {
        asm.rsc(asm.R1, asm.R2, Operand2.reg(asm.R3));
        asm.rsc(asm.R15, asm.R0, Operand2.asr(asm.R13, 17));

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0xe0, 0xe2, 0x10, 0x03,
        0xe0, 0xe0, 0xf8, 0xcd }));

        assertEquals ("rsc r1, r2, r3", dis.disassembleNext());
        assertEquals ("rsc pc, r0, sp, asr #17", dis.disassembleNext());
    }

    /**
     * Tests the rscs instruction.
     */
    public void testRscs() {
        asm.rscs(asm.R1, asm.R2, Operand2.reg(asm.R3));
        asm.rscs(asm.R15, asm.R0, Operand2.reg(asm.R13));

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0xe0, 0xf2, 0x10, 0x03,
        0xe0, 0xf0, 0xf0, 0x0d }));

        assertEquals ("rscs r1, r2, r3", dis.disassembleNext());
        assertEquals ("rscs pc, r0, sp", dis.disassembleNext());
    }

    /**
     * Tests the rsccond instruction.
     */
    public void testRsccond() {
        asm.rsccond(asm.COND_EQ, asm.R1, asm.R2, Operand2.reg(asm.R3));
        asm.rsccond(asm.COND_HI, asm.R15, asm.R0, Operand2.reg(asm.R13));

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0x00, 0xe2, 0x10, 0x03,
        0x80, 0xe0, 0xf0, 0x0d }));

        assertEquals ("rsceq r1, r2, r3", dis.disassembleNext());
        assertEquals ("rschi pc, r0, sp", dis.disassembleNext());
    }

    /**
     * Tests the rscconds instruction.
     */
    public void testRscconds() {
        asm.rscconds(asm.COND_EQ, asm.R1, asm.R2, Operand2.reg(asm.R3));
        asm.rscconds(asm.COND_HI, asm.R15, asm.R0, Operand2.lsr(asm.R13, asm.R7));

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0x00, 0xf2, 0x10, 0x03,
        0x80, 0xf0, 0xf7, 0x3d }));

        assertEquals ("rsceqs r1, r2, r3", dis.disassembleNext());
        assertEquals ("rschis pc, r0, sp, lsr r7", dis.disassembleNext());
    }
}
