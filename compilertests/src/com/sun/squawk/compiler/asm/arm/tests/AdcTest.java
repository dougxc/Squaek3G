/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: AdcTest.java,v 1.3 2005/02/03 00:56:40 dl156546 Exp $
 */

package com.sun.squawk.compiler.asm.arm.tests;

import com.sun.squawk.compiler.asm.*;
import com.sun.squawk.compiler.asm.arm.*;
import junit.framework.*;

/**
 * Test fixtures for the adc{cond}{s} assembler instructions.
 *
 * @author David Liu
 * @version 1.0
 */
public class AdcTest extends TestCase {
    public AdcTest() {
    }

    public static void main (String[] args) {
        junit.textui.TestRunner.run (suite());
    }

    public static junit.framework.Test suite() {
        return new TestSuite(AdcTest.class);
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
     * Tests the adc instruction.
     */
    public void testAdc() {
        asm.adc(asm.R1, asm.R2, Operand2.reg(asm.R3));
        asm.adc(asm.R15, asm.R0, Operand2.asr(asm.SP, 17));

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0xe0, 0xa2, 0x10, 0x03,
        0xe0, 0xa0, 0xf8, 0xcd }));

        assertEquals("adc r1, r2, r3", dis.disassembleNext());
        assertEquals("adc pc, r0, sp, asr #17", dis.disassembleNext());
    }

    /**
     * Tests the adcs instruction.
     */
    public void testAdcs() {
        asm.adcs(asm.R1, asm.R2, Operand2.reg(asm.R3));
        asm.adcs(asm.R15, asm.R0, Operand2.reg(asm.SP));

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0xe0, 0xb2, 0x10, 0x03,
        0xe0, 0xb0, 0xf0, 0x0d }));

        assertEquals("adcs r1, r2, r3", dis.disassembleNext());
        assertEquals("adcs pc, r0, sp", dis.disassembleNext());
    }

    /**
     * Tests the adccond instruction.
     */
    public void testAdccond() {
        asm.adccond(asm.COND_EQ, asm.R1, asm.R2, Operand2.reg(asm.R3));
        asm.adccond(asm.COND_HI, asm.R15, asm.R0, Operand2.reg(asm.SP));

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0x00, 0xa2, 0x10, 0x03,
        0x80, 0xa0, 0xf0, 0x0d }));

        assertEquals("adceq r1, r2, r3", dis.disassembleNext());
        assertEquals("adchi pc, r0, sp", dis.disassembleNext());
    }

    /**
     * Tests the adcconds instruction.
     */
    public void testAdcconds() {
        asm.adcconds(asm.COND_EQ, asm.R1, asm.R2, Operand2.reg(asm.R3));
        asm.adcconds(asm.COND_HI, asm.R15, asm.R0, Operand2.lsr(asm.SP, asm.R7));

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0x00, 0xb2, 0x10, 0x03,
        0x80, 0xb0, 0xf7, 0x3d }));

        assertEquals("adceqs r1, r2, r3", dis.disassembleNext());
        assertEquals("adchis pc, r0, sp, lsr r7", dis.disassembleNext());
    }
}
