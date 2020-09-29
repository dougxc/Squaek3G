/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: AddrPatchingTest.java,v 1.3 2005/02/03 00:56:40 dl156546 Exp $
 */

package com.sun.squawk.compiler.asm.arm.tests;

import com.sun.squawk.compiler.asm.*;
import com.sun.squawk.compiler.asm.arm.*;
import junit.framework.*;

/**
 * Test fixtures for address patching in the ARM assembler.
 *
 * @author David Liu
 * @version 1.0
 */
public class AddrPatchingTest extends TestCase {
    public AddrPatchingTest() {
    }

    public static void main (String[] args) {
        junit.textui.TestRunner.run (suite());
    }

    public static junit.framework.Test suite() {
        return new TestSuite(AddrPatchingTest.class);
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
     * Tests patching of Addressing Mode 2 based instruction with bound labels.
     */
    public void testAddr2PatchingBound() {
        ALabel label1 = asm.newLabel();
        ALabel label2 = asm.newLabel();
        ALabel label3 = asm.newLabel();

        asm.bind(label1);
        asm.nop();
        asm.bind(label2);
        asm.nop();
        asm.nop();
        asm.nop();
        asm.bind(label3);

        asm.ldr(asm.R7, Address2.pre(label1));
        asm.ldr(asm.R8, Address2.pre(label2));
        asm.ldr(asm.R9, Address2.pre(label3));

        asm.relocate(0x8004);

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0xe1, 0xa0, 0x00, 0x00,
        0xe1, 0xa0, 0x00, 0x00,
        0xe1, 0xa0, 0x00, 0x00,
        0xe1, 0xa0, 0x00, 0x00,
        0xe5, 0x1f, 0x70, 0x18,
        0xe5, 0x1f, 0x80, 0x18,
        0xe5, 0x1f, 0x90, 0x10 }));

        assertEquals("nop ; mov r0, r0", dis.disassembleNext());
        assertEquals("nop ; mov r0, r0", dis.disassembleNext());
        assertEquals("nop ; mov r0, r0", dis.disassembleNext());
        assertEquals("nop ; mov r0, r0", dis.disassembleNext());
        assertEquals("ldr r7, [pc, #-24] ; 0x8004", dis.disassembleNext());
        assertEquals("ldr r8, [pc, #-24] ; 0x8008", dis.disassembleNext());
        assertEquals("ldr r9, [pc, #-16] ; 0x8014", dis.disassembleNext());
    }

    /**
     * Tests patching of Addressing Mode 2 based instruction with unbound labels.
     */
    public void testAddr2PatchingUnbound() {
        ALabel label1 = asm.newLabel();
        ALabel label2 = asm.newLabel();
        ALabel label3 = asm.newLabel();

        asm.ldr(asm.R7, Address2.pre(label1));
        asm.bind(label1);

        asm.ldr(asm.R8, Address2.pre(label2));
        asm.nop();
        asm.bind(label2);

        asm.ldr(asm.R9, Address2.pre(label3));
        asm.nop();
        asm.nop();
        asm.nop();
        asm.bind(label3);

        asm.relocate(0x8004);

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0xe5, 0x1f, 0x70, 0x04,
        0xe5, 0x9f, 0x80, 0x00,
        0xe1, 0xa0, 0x00, 0x00,
        0xe5, 0x9f, 0x90, 0x08,
        0xe1, 0xa0, 0x00, 0x00,
        0xe1, 0xa0, 0x00, 0x00,
        0xe1, 0xa0, 0x00, 0x00 }));

        assertEquals("ldr r7, [pc, #-4] ; 0x8008", dis.disassembleNext());
        assertEquals("ldr r8, [pc] ; 0x8010", dis.disassembleNext());
        assertEquals("nop ; mov r0, r0", dis.disassembleNext());
        assertEquals("ldr r9, [pc, #8] ; 0x8020", dis.disassembleNext());
        assertEquals("nop ; mov r0, r0", dis.disassembleNext());
        assertEquals("nop ; mov r0, r0", dis.disassembleNext());
        assertEquals("nop ; mov r0, r0", dis.disassembleNext());
    }

    /**
     * Tests patching of Addressing Mode 2 based instruction with different sized offsets.
     */
    public void testAddr2Patching() {
        ALabel label1 = asm.newLabel();
        asm.ldr(asm.R7, Address2.pre(label1));
        label1.bindTo(0x18);

        ALabel label2 = asm.newLabel();
        asm.ldr(asm.R7, Address2.pre(label2));
        label2.bindTo(0x10b);

        ALabel label3 = asm.newLabel();
        asm.ldr(asm.R7, Address2.pre(label3));
        label3.bindTo(0x100f);

        asm.relocate(0x8004);

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0xe5, 0x9f, 0x70, 0x10,
        0xe5, 0x9f, 0x70, 0xff,
        0xe5, 0x9f, 0x7f, 0xff, }));

        assertEquals("ldr r7, [pc, #16] ; 0x801c", dis.disassembleNext());
        assertEquals("ldr r7, [pc, #255] ; 0x810f", dis.disassembleNext());
        assertEquals("ldr r7, [pc, #4095] ; 0x9013", dis.disassembleNext());
    }

    /**
     * Tests patching of Addressing Mode 3 based instruction with bound labels.
     */
    public void testAddr3PatchingBound() {
        ALabel label1 = asm.newLabel();
        ALabel label2 = asm.newLabel();
        ALabel label3 = asm.newLabel();

        asm.bind(label1);
        asm.nop();
        asm.bind(label2);
        asm.nop();
        asm.nop();
        asm.nop();
        asm.bind(label3);

        asm.ldrh(asm.R7, Address3.pre(label1));
        asm.ldrh(asm.R8, Address3.pre(label2));
        asm.ldrh(asm.R9, Address3.pre(label3));

        asm.relocate(0x8004);

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0xe1, 0xa0, 0x00, 0x00,
        0xe1, 0xa0, 0x00, 0x00,
        0xe1, 0xa0, 0x00, 0x00,
        0xe1, 0xa0, 0x00, 0x00,
        0xe1, 0x5f, 0x71, 0xb8,
        0xe1, 0x5f, 0x81, 0xb8,
        0xe1, 0x5f, 0x91, 0xb0 }));

        assertEquals("nop ; mov r0, r0", dis.disassembleNext());
        assertEquals("nop ; mov r0, r0", dis.disassembleNext());
        assertEquals("nop ; mov r0, r0", dis.disassembleNext());
        assertEquals("nop ; mov r0, r0", dis.disassembleNext());
        assertEquals("ldrh r7, [pc, #-24] ; 0x8004", dis.disassembleNext());
        assertEquals("ldrh r8, [pc, #-24] ; 0x8008", dis.disassembleNext());
        assertEquals("ldrh r9, [pc, #-16] ; 0x8014", dis.disassembleNext());
    }

    /**
     * Tests patching of Addressing Mode 3 based instruction with unbound labels.
     */
    public void testAddr3PatchingUnbound() {
        ALabel label1 = asm.newLabel();
        ALabel label2 = asm.newLabel();
        ALabel label3 = asm.newLabel();

        asm.ldrh(asm.R7, Address3.pre(label1));
        asm.bind(label1);

        asm.ldrh(asm.R8, Address3.pre(label2));
        asm.nop();
        asm.bind(label2);

        asm.ldrh(asm.R9, Address3.pre(label3));
        asm.nop();
        asm.nop();
        asm.nop();
        asm.bind(label3);

        asm.relocate(0x8004);

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0xe1, 0x5f, 0x70, 0xb4,
        0xe1, 0xdf, 0x80, 0xb0,
        0xe1, 0xa0, 0x00, 0x00,
        0xe1, 0xdf, 0x90, 0xb8,
        0xe1, 0xa0, 0x00, 0x00,
        0xe1, 0xa0, 0x00, 0x00,
        0xe1, 0xa0, 0x00, 0x00 }));

        assertEquals("ldrh r7, [pc, #-4] ; 0x8008", dis.disassembleNext());
        assertEquals("ldrh r8, [pc] ; 0x8010", dis.disassembleNext());
        assertEquals("nop ; mov r0, r0", dis.disassembleNext());
        assertEquals("ldrh r9, [pc, #8] ; 0x8020", dis.disassembleNext());
        assertEquals("nop ; mov r0, r0", dis.disassembleNext());
        assertEquals("nop ; mov r0, r0", dis.disassembleNext());
        assertEquals("nop ; mov r0, r0", dis.disassembleNext());
    }

    /**
     * Tests patching of Addressing Mode 3 based instruction with different sized offsets.
     */
    public void testAddr3Patching() {
        ALabel label1 = asm.newLabel();
        asm.ldrh(asm.R7, Address3.pre(label1));
        label1.bindTo(0x18);

        ALabel label2 = asm.newLabel();
        asm.ldrh(asm.R7, Address3.pre(label2));
        label2.bindTo(0x10b);

        asm.relocate(0x8004);

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0xe1, 0xdf, 0x71, 0xb0,
        0xe1, 0xdf, 0x7f, 0xbf }));

        assertEquals("ldrh r7, [pc, #16] ; 0x801c", dis.disassembleNext());
        assertEquals("ldrh r7, [pc, #255] ; 0x810f", dis.disassembleNext());
    }
}
