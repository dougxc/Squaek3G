/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: BTest.java,v 1.3 2005/02/03 00:56:40 dl156546 Exp $
 */

package com.sun.squawk.compiler.asm.arm.tests;

import com.sun.squawk.compiler.asm.*;
import com.sun.squawk.compiler.asm.arm.*;
import junit.framework.*;

/**
 * Test fixtures for the b{cond} and bl{cond} instructions.
 *
 * @author David Liu
 * @version 1.0
 */
public class BTest extends TestCase {
    public BTest() {
    }

    public static void main (String[] args) {
        junit.textui.TestRunner.run (suite());
    }

    public static junit.framework.Test suite() {
        return new TestSuite(BTest.class);
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
     * Tests the b instruction.
     */
    public void testB() {
        ALabel label1 = asm.newLabel();
        ALabel label2 = asm.newLabel();
        ALabel label3 = asm.newLabel();
        ALabel label4 = asm.newLabel();

        asm.b(label1);
        asm.bind(label1);

        asm.b(label2);
        asm.bind(label2);

        asm.b(label3);
        asm.nop();
        asm.bind(label3);

        asm.b(label4);
        asm.nop();
        asm.nop();
        asm.bind(label4);

        asm.b(label2);

        asm.relocate(0x8000);

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0xea, 0xff, 0xff, 0xff,
        0xea, 0xff, 0xff, 0xff,
        0xea, 0x00, 0x00, 0x00,
        0xe1, 0xa0, 0x00, 0x00,
        0xea, 0x00, 0x00, 0x01,
        0xe1, 0xa0, 0x00, 0x00,
        0xe1, 0xa0, 0x00, 0x00,
        0xea, 0xff, 0xff, 0xf9 }));

        assertEquals("b 0x8004", dis.disassembleNext());
        assertEquals("b 0x8008", dis.disassembleNext());
        assertEquals("b 0x8010", dis.disassembleNext());
        assertEquals("nop ; mov r0, r0", dis.disassembleNext());
        assertEquals("b 0x801c", dis.disassembleNext());
        assertEquals("nop ; mov r0, r0", dis.disassembleNext());
        assertEquals("nop ; mov r0, r0", dis.disassembleNext());
        assertEquals("b 0x8008", dis.disassembleNext());
    }

    /**
     * Tests the bl instruction.
     */
    public void testBl() {
        ALabel label1 = asm.newLabel();
        ALabel label2 = asm.newLabel();
        ALabel label3 = asm.newLabel();
        ALabel label4 = asm.newLabel();

        asm.bl(label1);
        asm.bind(label1);

        asm.bl(label2);
        asm.bind(label2);

        asm.bl(label3);
        asm.nop();
        asm.bind(label3);

        asm.bl(label4);
        asm.nop();
        asm.nop();
        asm.bind(label4);

        asm.bl(label2);

        asm.relocate(0x8000);

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0xeb, 0xff, 0xff, 0xff,
        0xeb, 0xff, 0xff, 0xff,
        0xeb, 0x00, 0x00, 0x00,
        0xe1, 0xa0, 0x00, 0x00,
        0xeb, 0x00, 0x00, 0x01,
        0xe1, 0xa0, 0x00, 0x00,
        0xe1, 0xa0, 0x00, 0x00,
        0xeb, 0xff, 0xff, 0xf9 }));

        assertEquals("bl 0x8004", dis.disassembleNext());
        assertEquals("bl 0x8008", dis.disassembleNext());
        assertEquals("bl 0x8010", dis.disassembleNext());
        assertEquals("nop ; mov r0, r0", dis.disassembleNext());
        assertEquals("bl 0x801c", dis.disassembleNext());
        assertEquals("nop ; mov r0, r0", dis.disassembleNext());
        assertEquals("nop ; mov r0, r0", dis.disassembleNext());
        assertEquals("bl 0x8008", dis.disassembleNext());
    }

    /**
     * Tests the bcond instruction.
     */
    public void testBcond() {
        ALabel label1 = asm.newLabel();
        ALabel label2 = asm.newLabel();
        ALabel label3 = asm.newLabel();

        asm.bcond(asm.COND_CS, label1);
        asm.bind(label1);
        asm.bind(label2);
        asm.bcond(asm.COND_VC, label2);
        asm.bcond(asm.COND_EQ, label1);
        asm.bcond(asm.COND_HI, label3);
        asm.add(asm.SP, asm.PC, Operand2.asr(asm.R7, 13));
        asm.bind(label3);

        asm.relocate(0x8000);

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0x2a, 0xff, 0xff, 0xff,
        0x7a, 0xff, 0xff, 0xfe,
        0x0a, 0xff, 0xff, 0xfd,
        0x8a, 0x00, 0x00, 0x00,
        0xe0, 0x8f, 0xd6, 0xc7 }));

        assertEquals("bcs 0x8004", dis.disassembleNext());
        assertEquals("bvc 0x8004", dis.disassembleNext());
        assertEquals("beq 0x8004", dis.disassembleNext());
        assertEquals("bhi 0x8014", dis.disassembleNext());
        assertEquals("add sp, pc, r7, asr #13", dis.disassembleNext());
    }

    /**
     * Tests the blcond instruction.
     */
    public void testBlcond() {
        ALabel label1 = asm.newLabel();
        ALabel label2 = asm.newLabel();
        ALabel label3 = asm.newLabel();

        asm.blcond(asm.COND_CS, label1);
        asm.bind(label1);
        asm.bind(label2);
        asm.blcond(asm.COND_VC, label2);
        asm.blcond(asm.COND_EQ, label1);
        asm.blcond(asm.COND_HI, label3);
        asm.add(asm.SP, asm.PC, Operand2.asr(asm.R7, 13));
        asm.bind(label3);

        asm.relocate(0x8000);

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0x2b, 0xff, 0xff, 0xff,
        0x7b, 0xff, 0xff, 0xfe,
        0x0b, 0xff, 0xff, 0xfd,
        0x8b, 0x00, 0x00, 0x00,
        0xe0, 0x8f, 0xd6, 0xc7 }));

        assertEquals("blcs 0x8004", dis.disassembleNext());
        assertEquals("blvc 0x8004", dis.disassembleNext());
        assertEquals("bleq 0x8004", dis.disassembleNext());
        assertEquals("blhi 0x8014", dis.disassembleNext());
        assertEquals("add sp, pc, r7, asr #13", dis.disassembleNext());
    }
}
