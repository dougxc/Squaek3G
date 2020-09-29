/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: MrsTest.java,v 1.3 2005/02/03 00:56:40 dl156546 Exp $
 */

package com.sun.squawk.compiler.asm.arm.tests;

import com.sun.squawk.compiler.asm.*;
import com.sun.squawk.compiler.asm.arm.*;
import junit.framework.*;

/**
 * Tests the mrs{cond} instruction.
 *
 * @author David Liu
 * @version 1.0
 */
public class MrsTest extends TestCase {
    public MrsTest() {
    }

    public static void main (String[] args) {
        junit.textui.TestRunner.run (suite());
    }

    public static junit.framework.Test suite() {
        return new TestSuite(MrsTest.class);
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
     * Tests the mrs instruction.
     */
    public void testMrs() {
        asm.mrs(asm.R7, asm.CPSR);
        asm.mrs(asm.LR, asm.SPSR);

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0xe1, 0x0f, 0x70, 0x00,
        0xe1, 0x4f, 0xe0, 0x00 }));

        assertEquals("mrs r7, CPSR", dis.disassembleNext());
        assertEquals("mrs lr, SPSR", dis.disassembleNext());
    }

    /**
     * Tests the mrs{cond} instruction.
     */
    public void testMrscond() {
        asm.mrscond(asm.COND_LS, asm.R7, asm.CPSR);
        asm.mrscond(asm.COND_VC, asm.LR, asm.SPSR);

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0x91, 0x0f, 0x70, 0x00,
        0x71, 0x4f, 0xe0, 0x00 }));

        assertEquals("mrsls r7, CPSR", dis.disassembleNext());
        assertEquals("mrsvc lr, SPSR", dis.disassembleNext());
    }
}
