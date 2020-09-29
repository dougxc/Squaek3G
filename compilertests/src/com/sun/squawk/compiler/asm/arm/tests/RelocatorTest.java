/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: RelocatorTest.java,v 1.2 2005/02/03 00:56:40 dl156546 Exp $
 */

package com.sun.squawk.compiler.asm.arm.tests;

import com.sun.squawk.compiler.asm.*;
import com.sun.squawk.compiler.asm.arm.*;
import junit.framework.*;

/**
 * Test fixtures for code relocation.
 *
 * @author David Liu
 * @version 1.0
 */
public class RelocatorTest extends TestCase {
    public RelocatorTest() {
    }

    public static void main (String[] args) {
        junit.textui.TestRunner.run (suite());
    }

    public static junit.framework.Test suite() {
        return new TestSuite(RelocatorTest.class);
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
     * Tests relocation of label addresses.
     */
    public void testRelocator() {
        ALabel label1 = asm.newLabel();
        ALabel label2 = asm.newLabel();

        asm.mov(asm.LR, Operand2.reg(asm.PC));
        asm.bind(label1);
        asm.nop();
        asm.nop();
        asm.emitLabel(label1);
        asm.emitLabel(label2);
        asm.bind(label2);
        asm.mov(asm.R1, Operand2.reg(asm.R2));
        asm.relocate(0x8018);

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0xe1, 0xa0, 0xe0, 0x0f,
        0xe1, 0xa0, 0x00, 0x00,
        0xe1, 0xa0, 0x00, 0x00,
        0x00, 0x00, 0x80, 0x1c,
        0x00, 0x00, 0x80, 0x2c,
        0xe1, 0xa0, 0x10, 0x02 }));

        assertEquals ("mov lr, pc", dis.disassembleNext());
        assertEquals ("nop ; mov r0, r0", dis.disassembleNext());
        assertEquals ("nop ; mov r0, r0", dis.disassembleNext());
        assertEquals ("andeq r8, r0, ip, lsl r0", dis.disassembleNext());
        assertEquals ("andeq r8, r0, ip, lsr #0", dis.disassembleNext());
        assertEquals ("mov r1, r2", dis.disassembleNext());
    }
}
