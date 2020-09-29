/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: AddTest.java,v 1.3 2005/02/03 00:56:40 dl156546 Exp $
 */

package com.sun.squawk.compiler.asm.arm.tests;

import com.sun.squawk.compiler.asm.*;
import com.sun.squawk.compiler.asm.arm.*;
import junit.framework.*;

/**
 * Test fixtures for the add{cond}{s} assembler instructions.
 *
 * @author David Liu
 * @version 1.0
 */
public class AddTest extends TestCase {
    public AddTest() {
    }

    public static void main (String[] args) {
        junit.textui.TestRunner.run (suite());
    }

    public static junit.framework.Test suite() {
        return new TestSuite(AddTest.class);
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
     * Tests the add instruction.
     */
    public void testAdd() {
        asm.add(asm.R1, asm.R2, Operand2.reg(asm.R3));
        asm.add(asm.PC, asm.R0, Operand2.asr(asm.SP, 17));

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0xe0, 0x82, 0x10, 0x03,
        0xe0, 0x80, 0xf8, 0xcd }));

        assertEquals("add r1, r2, r3", dis.disassembleNext());
        assertEquals("add pc, r0, sp, asr #17", dis.disassembleNext());
    }

    /**
     * Tests the adds instruction.
     */
    public void testAdds() {
        asm.adds(asm.R1, asm.R2, Operand2.reg(asm.R3));
        asm.adds(asm.PC, asm.R0, Operand2.reg(asm.SP));

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0xe0, 0x92, 0x10, 0x03,
        0xe0, 0x90, 0xf0, 0x0d }));

        assertEquals("adds r1, r2, r3", dis.disassembleNext());
        assertEquals("adds pc, r0, sp", dis.disassembleNext());
    }

    /**
     * Tests the addcond instruction.
     */
    public void testAddcond() {
        asm.addcond(asm.COND_EQ, asm.R1, asm.R2, Operand2.reg(asm.R3));
        asm.addcond(asm.COND_HI, asm.PC, asm.R0, Operand2.reg(asm.SP));

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0x00, 0x82, 0x10, 0x03,
        0x80, 0x80, 0xf0, 0x0d }));

        assertEquals("addeq r1, r2, r3", dis.disassembleNext());
        assertEquals("addhi pc, r0, sp", dis.disassembleNext());
    }

    /**
     * Tests the addconds instruction.
     */
    public void testAddconds() {
        asm.addconds(asm.COND_EQ, asm.R1, asm.R2, Operand2.reg(asm.R3));
        asm.addconds(asm.COND_HI, asm.PC, asm.R0, Operand2.lsr(asm.SP, asm.R7));

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0x00, 0x92, 0x10, 0x03,
        0x80, 0x90, 0xf7, 0x3d }));

        assertEquals("addeqs r1, r2, r3", dis.disassembleNext());
        assertEquals("addhis pc, r0, sp, lsr r7", dis.disassembleNext());
    }
}
