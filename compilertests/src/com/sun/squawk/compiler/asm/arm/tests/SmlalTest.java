/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: SmlalTest.java,v 1.3 2005/02/03 00:56:40 dl156546 Exp $
 */

package com.sun.squawk.compiler.asm.arm.tests;

import com.sun.squawk.compiler.asm.*;
import com.sun.squawk.compiler.asm.arm.*;
import junit.framework.*;

/**
 * Tests the umlal{cond}{s} instruction.
 *
 * @author David Liu
 * @version 1.0
 */
public class SmlalTest extends TestCase {
    public SmlalTest() {
    }

    public static void main (String[] args) {
        junit.textui.TestRunner.run (suite());
    }

    public static junit.framework.Test suite() {
        return new TestSuite(SmlalTest.class);
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
     * Tests the smlal instruction.
     */
    public void testSmlal() {
        asm.smlal(asm.R0, asm.R1, asm.R7, asm.LR);

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0xe0, 0xe1, 0x0e, 0x97 }));

        assertEquals("smlal r0, r1, r7, lr", dis.disassembleNext());
    }

    /**
     * Tests the smlalcond instruction.
     */
    public void testSmlalcond() {
        asm.smlalcond(asm.COND_MI, asm.R0, asm.R1, asm.R7, asm.LR);

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0x40, 0xe1, 0x0e, 0x97 }));

        assertEquals("smlalmi r0, r1, r7, lr", dis.disassembleNext());
    }

    /**
     * Tests the smlalconds instruction.
     */
    public void testSmlalconds() {
        asm.smlalconds(asm.COND_MI, asm.R0, asm.R1, asm.R7, asm.LR);

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0x40, 0xf1, 0x0e, 0x97 }));

        assertEquals("smlalmis r0, r1, r7, lr", dis.disassembleNext());
    }

    /**
     * Tests the smlals instruction.
     */
    public void testSmlals() {
        asm.smlals(asm.R0, asm.R1, asm.R7, asm.LR);

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0xe0, 0xf1, 0x0e, 0x97 }));

        assertEquals("smlals r0, r1, r7, lr", dis.disassembleNext());
    }
}
