/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: BicTest.java,v 1.3 2005/02/03 00:56:40 dl156546 Exp $
 */

package com.sun.squawk.compiler.asm.arm.tests;

import com.sun.squawk.compiler.asm.*;
import com.sun.squawk.compiler.asm.arm.*;
import junit.framework.*;

/**
 * Test fixtures for the bic{cond}{s} assembler instructions.
 *
 * @author David Liu
 * @version 1.0
 */
public class BicTest extends TestCase {
    public BicTest() {
    }

    public static void main (String[] args) {
        junit.textui.TestRunner.run (suite());
    }

    public static junit.framework.Test suite() {
        return new TestSuite(BicTest.class);
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
     * Tests the bic instruction.
     */
    public void testBic() {
        asm.bic(asm.R1, asm.R2, Operand2.reg(asm.R3));
        asm.bic(asm.PC, asm.R0, Operand2.asr(asm.SP, 17));

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0xe1, 0xc2, 0x10, 0x03,
        0xe1, 0xc0, 0xf8, 0xcd }));

        assertEquals("bic r1, r2, r3", dis.disassembleNext());
        assertEquals("bic pc, r0, sp, asr #17", dis.disassembleNext());
    }

    /**
     * Tests the bics instruction.
     */
    public void testBics() {
        asm.bics(asm.R1, asm.R2, Operand2.reg(asm.R3));
        asm.bics(asm.PC, asm.R0, Operand2.asr(asm.SP, 17));

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0xe1, 0xd2, 0x10, 0x03,
        0xe1, 0xd0, 0xf8, 0xcd }));

        assertEquals("bics r1, r2, r3", dis.disassembleNext());
        assertEquals("bics pc, r0, sp, asr #17", dis.disassembleNext());
    }

    /**
     * Tests the biccond instruction.
     */
    public void testBiccond() {
        asm.biccond(asm.COND_EQ, asm.R1, asm.R2, Operand2.reg(asm.R3));
        asm.biccond(asm.COND_HI, asm.PC, asm.R0, Operand2.asr(asm.SP, 17));

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0x01, 0xc2, 0x10, 0x03,
        0x81, 0xc0, 0xf8, 0xcd }));

        assertEquals("biceq r1, r2, r3", dis.disassembleNext());
        assertEquals("bichi pc, r0, sp, asr #17", dis.disassembleNext());
    }

    /**
     * Tests the bicconds instruction.
     */
    public void testBicconds() {
        asm.bicconds(asm.COND_EQ, asm.R1, asm.R2, Operand2.reg(asm.R3));
        asm.bicconds(asm.COND_HI, asm.PC, asm.R0, Operand2.asr(asm.SP, 17));

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0x01, 0xd2, 0x10, 0x03,
        0x81, 0xd0, 0xf8, 0xcd }));

        assertEquals("biceqs r1, r2, r3", dis.disassembleNext());
        assertEquals("bichis pc, r0, sp, asr #17", dis.disassembleNext());
    }
}
