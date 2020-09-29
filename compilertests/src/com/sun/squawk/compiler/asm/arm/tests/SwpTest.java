/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: SwpTest.java,v 1.3 2005/02/03 00:56:40 dl156546 Exp $
 */

package com.sun.squawk.compiler.asm.arm.tests;

import com.sun.squawk.compiler.asm.*;
import com.sun.squawk.compiler.asm.arm.*;
import junit.framework.*;

/**
 * Tests the swp{cond} instruction.
 *
 * @author David Liu
 * @version 1.0
 */
public class SwpTest extends TestCase {
    public SwpTest() {
    }

    public static void main (String[] args) {
        junit.textui.TestRunner.run (suite());
    }

    public static junit.framework.Test suite() {
        return new TestSuite(SwpTest.class);
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
     * Tests the swp instruction.
     */
    public void testSwp() {
        asm.swp(asm.R0, asm.R1, asm.R2);
        asm.swp(asm.R7, asm.SP, asm.LR);
        asm.swp(asm.R5, asm.R5, asm.SP);

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0xe1, 0x02, 0x00, 0x91,
        0xe1, 0x0e, 0x70, 0x9d,
        0xe1, 0x0d, 0x50, 0x95 }));

        assertEquals("swp r0, r1, [r2]", dis.disassembleNext());
        assertEquals("swp r7, sp, [lr]", dis.disassembleNext());
        assertEquals("swp r5, r5, [sp]", dis.disassembleNext());
    }

    /**
     * Tests the swp{cond} instruction.
     */
    public void testSwpcond() {
        asm.swpcond(asm.COND_PL, asm.R0, asm.R1, asm.R2);
        asm.swpcond(asm.COND_CC, asm.R7, asm.SP, asm.LR);

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0x51, 0x02, 0x00, 0x91,
        0x31, 0x0e, 0x70, 0x9d }));

        assertEquals("swppl r0, r1, [r2]", dis.disassembleNext());
        assertEquals("swpcc r7, sp, [lr]", dis.disassembleNext());
    }

    /**
     * Tests the swpb instruction.
     */
    public void testSwpb() {
        asm.swpb(asm.R0, asm.R1, asm.R2);
        asm.swpb(asm.R7, asm.SP, asm.LR);
        asm.swpb(asm.R5, asm.R5, asm.SP);

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0xe1, 0x42, 0x00, 0x91,
        0xe1, 0x4e, 0x70, 0x9d,
        0xe1, 0x4d, 0x50, 0x95 }));


        assertEquals("swpb r0, r1, [r2]", dis.disassembleNext());
        assertEquals("swpb r7, sp, [lr]", dis.disassembleNext());
        assertEquals("swpb r5, r5, [sp]", dis.disassembleNext());
    }

    /**
     * Tests the swp{cond}b instruction.
     */
    public void testSwpcondb() {
        asm.swpcondb(asm.COND_PL, asm.R0, asm.R1, asm.R2);
        asm.swpcondb(asm.COND_CC, asm.R7, asm.SP, asm.LR);

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0x51, 0x42, 0x00, 0x91,
        0x31, 0x4e, 0x70, 0x9d }));

        assertEquals("swpplb r0, r1, [r2]", dis.disassembleNext());
        assertEquals("swpccb r7, sp, [lr]", dis.disassembleNext());
    }
}
