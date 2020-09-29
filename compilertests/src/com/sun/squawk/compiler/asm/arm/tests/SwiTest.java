/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: SwiTest.java,v 1.3 2005/02/03 00:56:40 dl156546 Exp $
 */

package com.sun.squawk.compiler.asm.arm.tests;

import com.sun.squawk.compiler.asm.*;
import com.sun.squawk.compiler.asm.arm.*;
import junit.framework.*;

/**
 * Tests the swi{cond} instruction.
 *
 * @author David Liu
 * @version 1.0
 */
public class SwiTest extends TestCase {
    public SwiTest() {
    }

    public static void main (String[] args) {
        junit.textui.TestRunner.run (suite());
    }

    public static junit.framework.Test suite() {
        return new TestSuite(SwiTest.class);
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
     * Tests the swi instruction.
     */
    public void testSwi() {
        asm.swi(0x0);
        asm.swi(0x7f);
        asm.swi(0xd3c);

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0xef, 0x00, 0x00, 0x00,
        0xef, 0x00, 0x00, 0x7f,
        0xef, 0x00, 0x0d, 0x3c }));

        assertEquals("swi 0x0", dis.disassembleNext());
        assertEquals("swi 0x7f", dis.disassembleNext());
        assertEquals("swi 0xd3c", dis.disassembleNext());
    }

    /**
     * Tests the swi{cond} instruction.
     */
    public void testSwicond() {
        asm.swicond(asm.COND_GT, 0x7f);
        asm.swicond(asm.COND_CS, 0xd3c);

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0xcf, 0x00, 0x00, 0x7f,
        0x2f, 0x00, 0x0d, 0x3c }));

        assertEquals("swigt 0x7f", dis.disassembleNext());
        assertEquals("swics 0xd3c", dis.disassembleNext());
    }
}
