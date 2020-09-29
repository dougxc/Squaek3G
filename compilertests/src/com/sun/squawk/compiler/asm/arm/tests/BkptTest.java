/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: BkptTest.java,v 1.3 2005/02/03 00:56:40 dl156546 Exp $
 */

package com.sun.squawk.compiler.asm.arm.tests;

import com.sun.squawk.compiler.asm.*;
import com.sun.squawk.compiler.asm.arm.*;
import junit.framework.*;

/**
 * Test fixtures for the bkpt instruction.
 *
 * @author David Liu
 * @version 1.0
 */
public class BkptTest extends TestCase {
    public BkptTest() {
    }

    public static void main (String[] args) {
        junit.textui.TestRunner.run (suite());
    }

    public static junit.framework.Test suite() {
        return new TestSuite(BkptTest.class);
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
     * Tests the bkpt instruction.
     */
    public void testBkpt() {
        asm.bkpt(0x0);
        asm.bkpt(0xc8);
        asm.bkpt(0xa3f2);

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0xe1, 0x20, 0x00, 0x70,
        0xe1, 0x20, 0x0c, 0x78,
        0xe1, 0x2a, 0x3f, 0x72 }));

        assertEquals("bkpt 0 ; 0x0", dis.disassembleNext());
        assertEquals("bkpt 200 ; 0xc8", dis.disassembleNext());
        assertEquals("bkpt 41970 ; 0xa3f2", dis.disassembleNext());
    }
}
