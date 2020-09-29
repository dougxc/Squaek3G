/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: MvnTest.java,v 1.3 2005/02/03 00:56:40 dl156546 Exp $
 */

package com.sun.squawk.compiler.asm.arm.tests;

import com.sun.squawk.compiler.asm.*;
import com.sun.squawk.compiler.asm.arm.*;
import junit.framework.*;

/**
 * Test fixtures for the mvn{cond}{s} assembler instructions.
 *
 * @author David Liu
 * @version 1.0
 */
public class MvnTest extends TestCase {
    public MvnTest() {
    }

    public static void main (String[] args) {
        junit.textui.TestRunner.run (suite());
    }

    public static junit.framework.Test suite() {
        return new TestSuite(MvnTest.class);
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
     * Tests the mvn instruction.
     */
    public void testMvn() {
        asm.mvn(asm.R1, Operand2.reg(asm.R3));
        asm.mvn(asm.PC, Operand2.asr(asm.SP, 17));

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0xe1, 0xe0, 0x10, 0x03,
        0xe1, 0xe0, 0xf8, 0xcd }));

        assertEquals("mvn r1, r3", dis.disassembleNext());
        assertEquals("mvn pc, sp, asr #17", dis.disassembleNext());
    }

    /**
     * Tests the mvns instruction.
     */
    public void testMvns() {
        asm.mvns(asm.R1, Operand2.reg(asm.R3));
        asm.mvns(asm.PC, Operand2.asr(asm.SP, 17));

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0xe1, 0xf0, 0x10, 0x03,
        0xe1, 0xf0, 0xf8, 0xcd }));

        assertEquals("mvns r1, r3", dis.disassembleNext());
        assertEquals("mvns pc, sp, asr #17", dis.disassembleNext());
    }

    /**
     * Tests the mvncond instruction.
     */
    public void testMvncond() {
        asm.mvncond(asm.COND_CC, asm.R1, Operand2.reg(asm.R3));
        asm.mvncond(asm.COND_VS, asm.PC, Operand2.asr(asm.SP, 17));

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0x31, 0xe0, 0x10, 0x03,
        0x61, 0xe0, 0xf8, 0xcd }));

        assertEquals("mvncc r1, r3", dis.disassembleNext());
        assertEquals("mvnvs pc, sp, asr #17", dis.disassembleNext());
    }

    /**
     * Tests the mvnconds instruction.
     */
    public void testMvnconds() {
        asm.mvnconds(asm.COND_CC, asm.R1, Operand2.reg(asm.R3));
        asm.mvnconds(asm.COND_VS, asm.PC, Operand2.asr(asm.SP, 17));

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0x31, 0xf0, 0x10, 0x03,
        0x61, 0xf0, 0xf8, 0xcd }));

        assertEquals("mvnccs r1, r3", dis.disassembleNext());
        assertEquals("mvnvss pc, sp, asr #17", dis.disassembleNext());
    }
}
