/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: ClzTest.java,v 1.3 2005/02/03 00:56:40 dl156546 Exp $
 */

package com.sun.squawk.compiler.asm.arm.tests;

import com.sun.squawk.compiler.asm.*;
import com.sun.squawk.compiler.asm.arm.*;
import junit.framework.*;

/**
 * Test fixtures for the clz{cond} assembler instructions.
 *
 * @author David Liu
 * @version 1.0
 */
public class ClzTest extends TestCase {
    public ClzTest() {
    }

    public static void main (String[] args) {
        junit.textui.TestRunner.run (suite());
    }

    public static junit.framework.Test suite() {
        return new TestSuite(ClzTest.class);
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
     * Tests the clz instruction.
     */
    public void testClz() {
        asm.clz(asm.R3, asm.R10);

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0xe1, 0x6f, 0x3f, 0x1a }));

        assertEquals("clz r3, r10", dis.disassembleNext());
    }

    /**
     * Tests the clzcond instruction.
     */
    public void testClzcond() {
        asm.clzcond(asm.COND_EQ, asm.R7, asm.LR);

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0x01, 0x6f, 0x7f, 0x1e }));

        assertEquals("clzeq r7, lr", dis.disassembleNext());
    }
}
