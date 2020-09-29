/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: OrrTest.java,v 1.3 2005/02/03 00:56:40 dl156546 Exp $
 */

package com.sun.squawk.compiler.asm.arm.tests;

import com.sun.squawk.compiler.asm.*;
import com.sun.squawk.compiler.asm.arm.*;
import junit.framework.*;

/**
 * Test fixtures for the orr{cond}{s} assembler instructions.
 *
 * @author David Liu
 * @version 1.0
 */
public class OrrTest extends TestCase {
    public OrrTest() {
    }

    public static void main (String[] args) {
        junit.textui.TestRunner.run (suite());
    }

    public static junit.framework.Test suite() {
        return new TestSuite(OrrTest.class);
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
     * Tests the orr instruction.
     */
    public void testOrr() {
        asm.orr(asm.R1, asm.R2, Operand2.reg(asm.R3));
        asm.orr(asm.PC, asm.R0, Operand2.asr(asm.SP, 17));

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0xe1, 0x82, 0x10, 0x03,
        0xe1, 0x80, 0xf8, 0xcd }));

        assertEquals ("orr r1, r2, r3", dis.disassembleNext());
        assertEquals ("orr pc, r0, sp, asr #17", dis.disassembleNext());
    }

    /**
     * Tests the orrs instruction.
     */
    public void testOrrs() {
        asm.orrs(asm.R1, asm.R2, Operand2.reg(asm.R3));
        asm.orrs(asm.PC, asm.R0, Operand2.asr(asm.SP, 17));

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0xe1, 0x92, 0x10, 0x03,
        0xe1, 0x90, 0xf8, 0xcd }));

        assertEquals ("orrs r1, r2, r3", dis.disassembleNext());
        assertEquals ("orrs pc, r0, sp, asr #17", dis.disassembleNext());
    }

    /**
     * Tests the orrcond instruction.
     */
    public void testOrrcond() {
        asm.orrcond(asm.COND_EQ, asm.R1, asm.R2, Operand2.reg(asm.R3));
        asm.orrcond(asm.COND_HI, asm.PC, asm.R0, Operand2.asr(asm.SP, 17));

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0x01, 0x82, 0x10, 0x03,
        0x81, 0x80, 0xf8, 0xcd }));

        assertEquals ("orreq r1, r2, r3", dis.disassembleNext());
        assertEquals ("orrhi pc, r0, sp, asr #17", dis.disassembleNext());
    }

    /**
     * Tests the orrconds instruction.
     */
    public void testOrrconds() {
        asm.orrconds(asm.COND_EQ, asm.R1, asm.R2, Operand2.reg(asm.R3));
        asm.orrconds(asm.COND_HI, asm.PC, asm.R0, Operand2.asr(asm.SP, 17));

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0x01, 0x92, 0x10, 0x03,
        0x81, 0x90, 0xf8, 0xcd }));

        assertEquals ("orreqs r1, r2, r3", dis.disassembleNext());
        assertEquals ("orrhis pc, r0, sp, asr #17", dis.disassembleNext());
    }
}
