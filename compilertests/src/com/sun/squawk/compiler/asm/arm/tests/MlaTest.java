/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: MlaTest.java,v 1.3 2005/02/03 00:56:40 dl156546 Exp $
 */

package com.sun.squawk.compiler.asm.arm.tests;

import com.sun.squawk.compiler.asm.*;
import com.sun.squawk.compiler.asm.arm.*;
import junit.framework.*;

/**
 * Test fixtures for the mla{cond}{s} assembler instructions.
 *
 * @author David Liu
 * @version 1.0
 */
public class MlaTest extends TestCase {
    public MlaTest() {
    }

    public static void main (String[] args) {
        junit.textui.TestRunner.run (suite());
    }

    public static junit.framework.Test suite() {
        return new TestSuite(MlaTest.class);
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
     * Tests the mla instruction.
     */
    public void testMla() {
        asm.mla(asm.R1, asm.R2, asm.R3, asm.R4);
        asm.mla(asm.LR, asm.R0, asm.R7, asm.SP);

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0xe0, 0x21, 0x43, 0x92,
        0xe0, 0x2e, 0xd7, 0x90 }));

        assertEquals("mla r1, r2, r3, r4", dis.disassembleNext());
        assertEquals("mla lr, r0, r7, sp", dis.disassembleNext());
    }

    /**
     * Tests the mlas instruction.
     */
    public void testMlas() {
        asm.mlas(asm.R1, asm.R2, asm.R3, asm.R4);
        asm.mlas(asm.LR, asm.R0, asm.R7, asm.SP);

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0xe0, 0x31, 0x43, 0x92,
        0xe0, 0x3e, 0xd7, 0x90 }));

        assertEquals("mlas r1, r2, r3, r4", dis.disassembleNext());
        assertEquals("mlas lr, r0, r7, sp", dis.disassembleNext());
    }

    /**
     * Tests the mlacond instruction.
     */
    public void testMlacond() {
        asm.mlacond(asm.COND_EQ, asm.R1, asm.R2, asm.R3, asm.R4);
        asm.mlacond(asm.COND_CS, asm.LR, asm.R0, asm.R7, asm.SP);

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0x00, 0x21, 0x43, 0x92,
        0x20, 0x2e, 0xd7, 0x90 }));

        assertEquals("mlaeq r1, r2, r3, r4", dis.disassembleNext());
        assertEquals("mlacs lr, r0, r7, sp", dis.disassembleNext());
    }

    /**
     * Tests the mlaconds instruction.
     */
    public void testMlaconds() {
        asm.mlaconds(asm.COND_EQ, asm.R1, asm.R2, asm.R3, asm.R4);
        asm.mlaconds(asm.COND_CS, asm.LR, asm.R0, asm.R7, asm.SP);

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0x00, 0x31, 0x43, 0x92,
        0x20, 0x3e, 0xd7, 0x90 }));

        assertEquals("mlaeqs r1, r2, r3, r4", dis.disassembleNext());
        assertEquals("mlacss lr, r0, r7, sp", dis.disassembleNext());
    }
}
