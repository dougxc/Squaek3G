/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: StrTest.java,v 1.4 2005/02/03 00:56:40 dl156546 Exp $
 */

package com.sun.squawk.compiler.asm.arm.tests;

import com.sun.squawk.compiler.asm.*;
import com.sun.squawk.compiler.asm.arm.*;
import junit.framework.*;

/**
 * Tests the str{cond} and str{cond}b instructions.
 *
 * @author David Liu
 * @version 1.0
 */
public class StrTest extends TestCase {
    public StrTest() {
    }

    public static void main (String[] args) {
        junit.textui.TestRunner.run (suite());
    }

    public static junit.framework.Test suite() {
        return new TestSuite(StrTest.class);
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
     * Tests the str instruction.
     */
    public void testStr() {
        asm.str(asm.R1, Address2.pre(asm.SP));
        asm.str(asm.R2, Address2.pre(asm.R7, 0x1BA));
        asm.str(asm.R3, Address2.pre(asm.R2, -1, asm.IP, asm.LSL, 1));
        asm.str(asm.R3, Address2.pre(asm.R2, -1, asm.IP, asm.LSR, 7));
        asm.str(asm.R3, Address2.pre(asm.R2, -1, asm.IP, asm.ASR, 19));
        asm.str(asm.R3, Address2.pre(asm.R2, -1, asm.IP, asm.ROR, 25));
        asm.str(asm.R3, Address2.pre(asm.R2, -1, asm.IP, asm.ROR, 0));
        asm.str(asm.R4, Address2.preW(asm.FP, -0x0F2));
        asm.str(asm.R5, Address2.preW(asm.R10, 1, asm.R9));
        asm.str(asm.R6, Address2.preW(asm.R1, 1, asm.LR, asm.LSR, 3));
        asm.str(asm.R7, Address2.post(asm.IP, -0xFFF));
        asm.str(asm.R8, Address2.post(asm.R5, -1, asm.FP));
        asm.str(asm.R9, Address2.post(asm.R7, 1, asm.R3, asm.LSL, 7));
        asm.str(asm.PC, Address2.post(asm.R3, 0x111));

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0xe5, 0x8d, 0x10, 0x00,
        0xe5, 0x87, 0x21, 0xba,
        0xe7, 0x02, 0x30, 0x8c,
        0xe7, 0x02, 0x33, 0xac,
        0xe7, 0x02, 0x39, 0xcc,
        0xe7, 0x02, 0x3c, 0xec,
        0xe7, 0x02, 0x30, 0x6c,
        0xe5, 0x2b, 0x40, 0xf2,
        0xe7, 0xaa, 0x50, 0x09,
        0xe7, 0xa1, 0x61, 0xae,
        0xe4, 0x0c, 0x7f, 0xff,
        0xe6, 0x05, 0x80, 0x0b,
        0xe6, 0x87, 0x93, 0x83,
        0xe4, 0x83, 0xf1, 0x11 }));

        assertEquals("str r1, [sp]", dis.disassembleNext());
        assertEquals("str r2, [r7, #442]", dis.disassembleNext());
        assertEquals("str r3, [r2, -ip, lsl #1]", dis.disassembleNext());
        assertEquals("str r3, [r2, -ip, lsr #7]", dis.disassembleNext());
        assertEquals("str r3, [r2, -ip, asr #19]", dis.disassembleNext());
        assertEquals("str r3, [r2, -ip, ror #25]", dis.disassembleNext());
        assertEquals("str r3, [r2, -ip, rrx]", dis.disassembleNext());
        assertEquals("str r4, [fp, #-242]!", dis.disassembleNext());
        assertEquals("str r5, [r10, r9]!", dis.disassembleNext());
        assertEquals("str r6, [r1, lr, lsr #3]!", dis.disassembleNext());
        assertEquals("str r7, [ip], #-4095", dis.disassembleNext());
        assertEquals("str r8, [r5], -fp", dis.disassembleNext());
        assertEquals("str r9, [r7], r3, lsl #7", dis.disassembleNext());
        assertEquals("str pc, [r3], #273", dis.disassembleNext());
    }

    /**
     * Tests the strb instruction.
     */
    public void testStrb() {
        asm.strb(asm.R1, Address2.pre(asm.SP));
        asm.strb(asm.R2, Address2.pre(asm.R7, 0x1BA));
        asm.strb(asm.R3, Address2.pre(asm.R2, -1, asm.IP, asm.LSL, 1));
        asm.strb(asm.R3, Address2.pre(asm.R2, -1, asm.IP, asm.LSR, 7));
        asm.strb(asm.R3, Address2.pre(asm.R2, -1, asm.IP, asm.ASR, 19));
        asm.strb(asm.R3, Address2.pre(asm.R2, -1, asm.IP, asm.ROR, 25));
        asm.strb(asm.R3, Address2.pre(asm.R2, -1, asm.IP, asm.ROR, 0));
        asm.strb(asm.R4, Address2.preW(asm.FP, -0x0F2));
        asm.strb(asm.R5, Address2.preW(asm.R10, 1, asm.R9));
        asm.strb(asm.R6, Address2.preW(asm.R1, 1, asm.LR, asm.LSR, 3));
        asm.strb(asm.R7, Address2.post(asm.IP, -0xFFF));
        asm.strb(asm.R8, Address2.post(asm.R5, -1, asm.FP));
        asm.strb(asm.R9, Address2.post(asm.R7, 1, asm.R3, asm.LSL, 7));
        asm.strb(asm.PC, Address2.post(asm.R3, 0x111));

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0xe5, 0xcd, 0x10, 0x00,
        0xe5, 0xc7, 0x21, 0xba,
        0xe7, 0x42, 0x30, 0x8c,
        0xe7, 0x42, 0x33, 0xac,
        0xe7, 0x42, 0x39, 0xcc,
        0xe7, 0x42, 0x3c, 0xec,
        0xe7, 0x42, 0x30, 0x6c,
        0xe5, 0x6b, 0x40, 0xf2,
        0xe7, 0xea, 0x50, 0x09,
        0xe7, 0xe1, 0x61, 0xae,
        0xe4, 0x4c, 0x7f, 0xff,
        0xe6, 0x45, 0x80, 0x0b,
        0xe6, 0xc7, 0x93, 0x83,
        0xe4, 0xc3, 0xf1, 0x11 }));

        assertEquals("strb r1, [sp]", dis.disassembleNext());
        assertEquals("strb r2, [r7, #442]", dis.disassembleNext());
        assertEquals("strb r3, [r2, -ip, lsl #1]", dis.disassembleNext());
        assertEquals("strb r3, [r2, -ip, lsr #7]", dis.disassembleNext());
        assertEquals("strb r3, [r2, -ip, asr #19]", dis.disassembleNext());
        assertEquals("strb r3, [r2, -ip, ror #25]", dis.disassembleNext());
        assertEquals("strb r3, [r2, -ip, rrx]", dis.disassembleNext());
        assertEquals("strb r4, [fp, #-242]!", dis.disassembleNext());
        assertEquals("strb r5, [r10, r9]!", dis.disassembleNext());
        assertEquals("strb r6, [r1, lr, lsr #3]!", dis.disassembleNext());
        assertEquals("strb r7, [ip], #-4095", dis.disassembleNext());
        assertEquals("strb r8, [r5], -fp", dis.disassembleNext());
        assertEquals("strb r9, [r7], r3, lsl #7", dis.disassembleNext());
        assertEquals("strb pc, [r3], #273", dis.disassembleNext());
    }

    /**
     * Tests the strcond instruction.
     */
    public void testStrcond() {
        asm.strcond(asm.COND_GT, asm.R1, Address2.pre(asm.SP));
        asm.strcond(asm.COND_VC, asm.SP, Address2.post(asm.R7, -1, asm.R6, asm.ASR, 17));
        asm.strcond(asm.COND_EQ, asm.PC, Address2.preW(asm.SP, 0xfff));

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0xc5, 0x8d, 0x10, 0x00,
        0x76, 0x07, 0xd8, 0xc6,
        0x05, 0xad, 0xff, 0xff }));

        assertEquals("strgt r1, [sp]", dis.disassembleNext());
        assertEquals("strvc sp, [r7], -r6, asr #17", dis.disassembleNext());
        assertEquals("streq pc, [sp, #4095]!", dis.disassembleNext());
    }

    /**
     * Tests the strcondb instruction.
     */
    public void testStrcondb() {
        asm.strcondb(asm.COND_GT, asm.R1, Address2.pre(asm.SP));
        asm.strcondb(asm.COND_VC, asm.SP, Address2.post(asm.R7, -1, asm.R6, asm.ASR, 17));
        asm.strcondb(asm.COND_EQ, asm.PC, Address2.preW(asm.SP, 0xfff));

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0xc5, 0xcd, 0x10, 0x00,
        0x76, 0x47, 0xd8, 0xc6,
        0x05, 0xed, 0xff, 0xff }));

        assertEquals("strgtb r1, [sp]", dis.disassembleNext());
        assertEquals("strvcb sp, [r7], -r6, asr #17", dis.disassembleNext());
        assertEquals("streqb pc, [sp, #4095]!", dis.disassembleNext());
    }

    /**
     * Tests the strh instruction.
     */
    public void testStrh() {
        asm.strh(asm.R0, Address3.pre(asm.PC));
        asm.strh(asm.SP, Address3.pre(asm.R7, -123));

        asm.strh(asm.R8, Address3.pre(asm.R0, 1, asm.R6));
        asm.strh(asm.R3, Address3.pre(asm.R6, -1, asm.FP));

        asm.strh(asm.R5, Address3.preW(asm.R3, 0));
        asm.strh(asm.IP, Address3.preW(asm.R6, 251));

        asm.strh(asm.R7, Address3.preW(asm.R9, 1, asm.R4));
        asm.strh(asm.PC, Address3.preW(asm.R10, -1, asm.R1));

        asm.strh(asm.LR, Address3.post(asm.R4, -250));
        asm.strh(asm.FP, Address3.post(asm.FP, 67));

        asm.strh(asm.R1, Address3.post(asm.R5, 1, asm.PC));
        asm.strh(asm.R2, Address3.post(asm.LR, -1, asm.SP));

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0xe1, 0xcf, 0x00, 0xb0,
        0xe1, 0x47, 0xd7, 0xbb,
        0xe1, 0x80, 0x80, 0xb6,
        0xe1, 0x06, 0x30, 0xbb,
        0xe1, 0xe3, 0x50, 0xb0,
        0xe1, 0xe6, 0xcf, 0xbb,
        0xe1, 0xa9, 0x70, 0xb4,
        0xe1, 0x2a, 0xf0, 0xb1,
        0xe0, 0x44, 0xef, 0xba,
        0xe0, 0xcb, 0xb4, 0xb3,
        0xe0, 0x85, 0x10, 0xbf,
        0xe0, 0x0e, 0x20, 0xbd }));

        assertEquals("strh r0, [pc] ; 0x8", dis.disassembleNext());
        assertEquals("strh sp, [r7, #-123]", dis.disassembleNext());

        assertEquals("strh r8, [r0, r6]", dis.disassembleNext());
        assertEquals("strh r3, [r6, -fp]", dis.disassembleNext());

        assertEquals("strh r5, [r3]!", dis.disassembleNext());
        assertEquals("strh ip, [r6, #251]!", dis.disassembleNext());

        assertEquals("strh r7, [r9, r4]!", dis.disassembleNext());
        assertEquals("strh pc, [r10, -r1]!", dis.disassembleNext());

        assertEquals("strh lr, [r4], #-250", dis.disassembleNext());
        assertEquals("strh fp, [fp], #67", dis.disassembleNext());

        assertEquals("strh r1, [r5], pc", dis.disassembleNext());
        assertEquals("strh r2, [lr], -sp", dis.disassembleNext());
    }

    /**
     * Tests the strcondh instruction.
     */
    public void testStrcondh() {
        asm.strcondh(asm.COND_HI, asm.R0, Address3.pre(asm.PC));
        asm.strcondh(asm.COND_VC, asm.SP, Address3.post(asm.R7, -123));

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0x81, 0xcf, 0x00, 0xb0,
        0x70, 0x47, 0xd7, 0xbb }));

        assertEquals("strhih r0, [pc] ; 0x8", dis.disassembleNext());
        assertEquals("strvch sp, [r7], #-123", dis.disassembleNext());
    }
}
