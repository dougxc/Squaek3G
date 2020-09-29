/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: RsbTest.java,v 1.3 2005/02/03 00:56:40 dl156546 Exp $
 */

package com.sun.squawk.compiler.asm.arm.tests;

import com.sun.squawk.compiler.asm.*;
import com.sun.squawk.compiler.asm.arm.*;
import junit.framework.*;

/**
 * Test fixtures for the rsb{cond}{s} assembler instructions.
 *
 * @author David Liu
 * @version 1.0
 */
public class RsbTest extends TestCase {
    public RsbTest() {
    }

    public static void main (String[] args) {
        junit.textui.TestRunner.run (suite());
    }

    public static junit.framework.Test suite() {
        return new TestSuite(RsbTest.class);
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
     * Tests the rsb instruction.
     */
    public void testRsb() {
        asm.rsb(asm.R1, asm.R2, Operand2.reg(asm.R3));
        asm.rsb(asm.PC, asm.R0, Operand2.asr(asm.SP, 17));

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0xe0, 0x62, 0x10, 0x03,
        0xe0, 0x60, 0xf8, 0xcd }));

        assertEquals ("rsb r1, r2, r3", dis.disassembleNext());
        assertEquals ("rsb pc, r0, sp, asr #17", dis.disassembleNext());
    }

    /**
     * Tests the rsbs instruction.
     */
    public void testRsbs() {
        asm.rsbs(asm.R1, asm.R2, Operand2.reg(asm.R3));
        asm.rsbs(asm.PC, asm.R0, Operand2.reg(asm.SP));

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0xe0, 0x72, 0x10, 0x03,
        0xe0, 0x70, 0xf0, 0x0d }));

        assertEquals ("rsbs r1, r2, r3", dis.disassembleNext());
        assertEquals ("rsbs pc, r0, sp", dis.disassembleNext());
    }

    /**
     * Tests the rsbcond instruction.
     */
    public void testRsbcond() {
        asm.rsbcond(asm.COND_EQ, asm.R1, asm.R2, Operand2.reg(asm.R3));
        asm.rsbcond(asm.COND_HI, asm.PC, asm.R0, Operand2.reg(asm.SP));

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0x00, 0x62, 0x10, 0x03,
        0x80, 0x60, 0xf0, 0x0d }));

        assertEquals ("rsbeq r1, r2, r3", dis.disassembleNext());
        assertEquals ("rsbhi pc, r0, sp", dis.disassembleNext());
    }

    /**
     * Tests the rsbconds instruction.
     */
    public void testRsbconds() {
        asm.rsbconds(asm.COND_EQ, asm.R1, asm.R2, Operand2.reg(asm.R3));
        asm.rsbconds(asm.COND_HI, asm.PC, asm.R0, Operand2.lsr(asm.SP, asm.R7));

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0x00, 0x72, 0x10, 0x03,
        0x80, 0x70, 0xf7, 0x3d }));

        assertEquals ("rsbeqs r1, r2, r3", dis.disassembleNext());
        assertEquals ("rsbhis pc, r0, sp, lsr r7", dis.disassembleNext());
    }
}
