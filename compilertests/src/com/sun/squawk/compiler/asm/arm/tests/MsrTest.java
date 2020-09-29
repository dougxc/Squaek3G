/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: MsrTest.java,v 1.3 2005/02/03 00:56:40 dl156546 Exp $
 */

package com.sun.squawk.compiler.asm.arm.tests;

import com.sun.squawk.compiler.asm.*;
import com.sun.squawk.compiler.asm.arm.*;
import junit.framework.*;

/**
 * Tests the msr{cond} instruction.
 *
 * @author David Liu
 * @version 1.0
 */
public class MsrTest extends TestCase {
    public MsrTest() {
    }

    public static void main (String[] args) {
        junit.textui.TestRunner.run (suite());
    }

    public static junit.framework.Test suite() {
        return new TestSuite(MsrTest.class);
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
     * Tests the msr instruction.
     */
    public void testMsr() {
        asm.msr(asm.CPSR, asm.PSR_c | asm.PSR_x, 12992);
        asm.msr(asm.SPSR, asm.PSR_f | asm.PSR_s, asm.R10);

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0xe3, 0x23, 0xfd, 0xcb,
        0xe1, 0x6c, 0xf0, 0x0a }));

        assertEquals("msr CPSR_xc, #12992 ; 0x32c0", dis.disassembleNext());
        assertEquals("msr SPSR_fs, r10", dis.disassembleNext());
    }

    /**
     * Tests the msrcond instruction.
     */
    public void testMsrcond() {
        asm.msrcond(asm.COND_LS, asm.CPSR, asm.PSR_c | asm.PSR_x, 12992);
        asm.msrcond(asm.COND_PL, asm.SPSR, asm.PSR_f | asm.PSR_s, asm.R10);

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0x93, 0x23, 0xfd, 0xcb,
        0x51, 0x6c, 0xf0, 0x0a }));

        assertEquals("msrls CPSR_xc, #12992 ; 0x32c0", dis.disassembleNext());
        assertEquals("msrpl SPSR_fs, r10", dis.disassembleNext());
    }
}
