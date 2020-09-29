/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: UmullTest.java,v 1.3 2005/02/03 00:56:40 dl156546 Exp $
 */

package com.sun.squawk.compiler.asm.arm.tests;

import com.sun.squawk.compiler.asm.*;
import com.sun.squawk.compiler.asm.arm.*;
import junit.framework.*;

/**
 * Tests the umull{cond}{s} instruction.
 *
 * @author David Liu
 * @version 1.0
 */
public class UmullTest extends TestCase {
    public UmullTest() {
    }

    public static void main (String[] args) {
        junit.textui.TestRunner.run (suite());
    }

    public static junit.framework.Test suite() {
        return new TestSuite(UmullTest.class);
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
     * Tests the umull instruction.
     */
    public void testUmull() {
        asm.umull(asm.R0, asm.R1, asm.R7, asm.LR);

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0xe0, 0x81, 0x0e, 0x97 }));

        assertEquals("umull r0, r1, r7, lr", dis.disassembleNext());
    }

    /**
     * Tests the umullcond instruction.
     */
    public void testUmullcond() {
        asm.umullcond(asm.COND_MI, asm.R0, asm.R1, asm.R7, asm.LR);

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0x40, 0x81, 0x0e, 0x97 }));

        assertEquals("umullmi r0, r1, r7, lr", dis.disassembleNext());
    }

    /**
     * Tests the umullconds instruction.
     */
    public void testUmullconds() {
        asm.umullconds(asm.COND_MI, asm.R0, asm.R1, asm.R7, asm.LR);

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0x40, 0x91, 0x0e, 0x97 }));

        assertEquals("umullmis r0, r1, r7, lr", dis.disassembleNext());
    }

    /**
     * Tests the umulls instruction.
     */
    public void testUmulls() {
        asm.umulls(asm.R0, asm.R1, asm.R7, asm.LR);

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0xe0, 0x91, 0x0e, 0x97 }));

        assertEquals("umulls r0, r1, r7, lr", dis.disassembleNext());
    }
}
