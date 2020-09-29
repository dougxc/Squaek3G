/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: SmullTest.java,v 1.3 2005/02/03 00:56:40 dl156546 Exp $
 */

package com.sun.squawk.compiler.asm.arm.tests;

import com.sun.squawk.compiler.asm.*;
import com.sun.squawk.compiler.asm.arm.*;
import junit.framework.*;

/**
 * Tests the smull{cond}{s} instruction.
 *
 * @author David Liu
 * @version 1.0
 */
public class SmullTest extends TestCase {
    public SmullTest() {
    }

    public static void main (String[] args) {
        junit.textui.TestRunner.run (suite());
    }

    public static junit.framework.Test suite() {
        return new TestSuite(SmullTest.class);
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
     * Tests the smull instruction.
     */
    public void testSmull() {
        asm.smull(asm.R0, asm.R1, asm.R7, asm.LR);

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0xe0, 0xc1, 0x0e, 0x97 }));

        assertEquals("smull r0, r1, r7, lr", dis.disassembleNext());
    }

    /**
     * Tests the smullcond instruction.
     */
    public void testSmullcond() {
        asm.smullcond(asm.COND_MI, asm.R0, asm.R1, asm.R7, asm.LR);

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0x40, 0xc1, 0x0e, 0x97 }));

        assertEquals("smullmi r0, r1, r7, lr", dis.disassembleNext());
    }

    /**
     * Tests the smullconds instruction.
     */
    public void testSmullconds() {
        asm.smullconds(asm.COND_MI, asm.R0, asm.R1, asm.R7, asm.LR);

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0x40, 0xd1, 0x0e, 0x97 }));

        assertEquals("smullmis r0, r1, r7, lr", dis.disassembleNext());
    }

    /**
     * Tests the smulls instruction.
     */
    public void testSmulls() {
        asm.smulls(asm.R0, asm.R1, asm.R7, asm.LR);

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0xe0, 0xd1, 0x0e, 0x97 }));

        assertEquals("smulls r0, r1, r7, lr", dis.disassembleNext());
    }
}
