/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: UmlalTest.java,v 1.3 2005/02/03 00:56:40 dl156546 Exp $
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
public class UmlalTest extends TestCase {
    public UmlalTest() {
    }

    public static void main (String[] args) {
        junit.textui.TestRunner.run (suite());
    }

    public static junit.framework.Test suite() {
        return new TestSuite(UmlalTest.class);
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
     * Tests the umlal instruction.
     */
    public void testUmlal() {
        asm.umlal(asm.R0, asm.R1, asm.R7, asm.R14);

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0xe0, 0xa1, 0x0e, 0x97 }));

        assertEquals("umlal r0, r1, r7, lr", dis.disassembleNext());
    }

    /**
     * Tests the umlalcond instruction.
     */
    public void testUmlalcond() {
        asm.umlalcond(asm.COND_MI, asm.R0, asm.R1, asm.R7, asm.R14);

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0x40, 0xa1, 0x0e, 0x97 }));

        assertEquals("umlalmi r0, r1, r7, lr", dis.disassembleNext());
    }

    /**
     * Tests the umlalconds instruction.
     */
    public void testUmlalconds() {
        asm.umlalconds(asm.COND_MI, asm.R0, asm.R1, asm.R7, asm.R14);

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0x40, 0xb1, 0x0e, 0x97 }));

        assertEquals("umlalmis r0, r1, r7, lr", dis.disassembleNext());
    }

    /**
     * Tests the umlals instruction.
     */
    public void testUmlals() {
        asm.umlals(asm.R0, asm.R1, asm.R7, asm.R14);

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0xe0, 0xb1, 0x0e, 0x97 }));

        assertEquals("umlals r0, r1, r7, lr", dis.disassembleNext());
    }
}
