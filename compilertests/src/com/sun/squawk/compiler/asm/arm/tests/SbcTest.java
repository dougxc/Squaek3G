/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: SbcTest.java,v 1.3 2005/02/03 00:56:40 dl156546 Exp $
 */

package com.sun.squawk.compiler.asm.arm.tests;

import com.sun.squawk.compiler.asm.*;
import com.sun.squawk.compiler.asm.arm.*;
import junit.framework.*;

/**
 * Test fixtures for the sbc{cond}{s} assembler instructions.
 *
 * @author David Liu
 * @version 1.0
 */
public class SbcTest extends TestCase {
    public SbcTest() {
    }

    public static void main (String[] args) {
        junit.textui.TestRunner.run (suite());
    }

    public static junit.framework.Test suite() {
        return new TestSuite(SbcTest.class);
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
     * Tests the sbc instruction.
     */
    public void testSbc() {
        asm.sbc(asm.R1, asm.R2, Operand2.reg(asm.R3));
        asm.sbc(asm.PC, asm.R0, Operand2.asr(asm.SP, 17));

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0xe0, 0xc2, 0x10, 0x03,
        0xe0, 0xc0, 0xf8, 0xcd }));

        assertEquals ("sbc r1, r2, r3", dis.disassembleNext());
        assertEquals ("sbc pc, r0, sp, asr #17", dis.disassembleNext());
    }

    /**
     * Tests the sbcs instruction.
     */
    public void testSbcs() {
        asm.sbcs(asm.R1, asm.R2, Operand2.reg(asm.R3));
        asm.sbcs(asm.PC, asm.R0, Operand2.reg(asm.SP));

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0xe0, 0xd2, 0x10, 0x03,
        0xe0, 0xd0, 0xf0, 0x0d }));

        assertEquals ("sbcs r1, r2, r3", dis.disassembleNext());
        assertEquals ("sbcs pc, r0, sp", dis.disassembleNext());
    }

    /**
     * Tests the sbccond instruction.
     */
    public void testSbccond() {
        asm.sbccond(asm.COND_EQ, asm.R1, asm.R2, Operand2.reg(asm.R3));
        asm.sbccond(asm.COND_HI, asm.PC, asm.R0, Operand2.reg(asm.SP));

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0x00, 0xc2, 0x10, 0x03,
        0x80, 0xc0, 0xf0, 0x0d }));

        assertEquals ("sbceq r1, r2, r3", dis.disassembleNext());
        assertEquals ("sbchi pc, r0, sp", dis.disassembleNext());
    }

    /**
     * Tests the sbcconds instruction.
     */
    public void testSbcconds() {
        asm.sbcconds(asm.COND_EQ, asm.R1, asm.R2, Operand2.reg(asm.R3));
        asm.sbcconds(asm.COND_HI, asm.PC, asm.R0, Operand2.lsr(asm.SP, asm.R7));

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0x00, 0xd2, 0x10, 0x03,
        0x80, 0xd0, 0xf7, 0x3d }));

        assertEquals ("sbceqs r1, r2, r3", dis.disassembleNext());
        assertEquals ("sbchis pc, r0, sp, lsr r7", dis.disassembleNext());
    }
}
