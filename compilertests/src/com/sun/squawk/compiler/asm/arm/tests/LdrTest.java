/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: LdrTest.java,v 1.4 2005/02/03 00:56:40 dl156546 Exp $
 */

package com.sun.squawk.compiler.asm.arm.tests;

import com.sun.squawk.compiler.asm.*;
import com.sun.squawk.compiler.asm.arm.*;
import junit.framework.*;

/**
 * Test fixtures for the ldr{cond} and ldr{cond}b instructions.
 *
 * @author David Liu
 * @version 1.0
 */
public class LdrTest extends TestCase {
    public LdrTest() {
    }

    public static void main (String[] args) {
        junit.textui.TestRunner.run (suite());
    }

    public static junit.framework.Test suite() {
        return new TestSuite(LdrTest.class);
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
     * Tests the ldr instruction.
     */
    public void testLdr() {
        asm.ldr(asm.R1, Address2.pre(asm.SP));
        asm.ldr(asm.R2, Address2.pre(asm.R7, 0x1BA));
        asm.ldr(asm.R3, Address2.pre(asm.R2, -1, asm.IP, asm.LSL, 1));
        asm.ldr(asm.R3, Address2.pre(asm.R2, -1, asm.IP, asm.LSR, 7));
        asm.ldr(asm.R3, Address2.pre(asm.R2, -1, asm.IP, asm.ASR, 19));
        asm.ldr(asm.R3, Address2.pre(asm.R2, -1, asm.IP, asm.ROR, 25));
        asm.ldr(asm.R3, Address2.pre(asm.R2, -1, asm.IP, asm.ROR, 0));
        asm.ldr(asm.R4, Address2.preW(asm.FP, -0x0F2));
        asm.ldr(asm.R5, Address2.preW(asm.R10, 1, asm.R9));
        asm.ldr(asm.R6, Address2.preW(asm.R1, 1, asm.LR, asm.LSR, 3));
        asm.ldr(asm.R7, Address2.post(asm.IP, -0xFFF));
        asm.ldr(asm.R8, Address2.post(asm.R5, -1, asm.FP));
        asm.ldr(asm.R9, Address2.post(asm.R7, 1, asm.R3, asm.LSL, 7));
        asm.ldr(asm.PC, Address2.post(asm.R3, 0x111));

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0xe5, 0x9d, 0x10, 0x00,
        0xe5, 0x97, 0x21, 0xba,
        0xe7, 0x12, 0x30, 0x8c,
        0xe7, 0x12, 0x33, 0xac,
        0xe7, 0x12, 0x39, 0xcc,
        0xe7, 0x12, 0x3c, 0xec,
        0xe7, 0x12, 0x30, 0x6c,
        0xe5, 0x3b, 0x40, 0xf2,
        0xe7, 0xba, 0x50, 0x09,
        0xe7, 0xb1, 0x61, 0xae,
        0xe4, 0x1c, 0x7f, 0xff,
        0xe6, 0x15, 0x80, 0x0b,
        0xe6, 0x97, 0x93, 0x83,
        0xe4, 0x93, 0xf1, 0x11 }));

        assertEquals("ldr r1, [sp]", dis.disassembleNext());
        assertEquals("ldr r2, [r7, #442]", dis.disassembleNext());
        assertEquals("ldr r3, [r2, -ip, lsl #1]", dis.disassembleNext());
        assertEquals("ldr r3, [r2, -ip, lsr #7]", dis.disassembleNext());
        assertEquals("ldr r3, [r2, -ip, asr #19]", dis.disassembleNext());
        assertEquals("ldr r3, [r2, -ip, ror #25]", dis.disassembleNext());
        assertEquals("ldr r3, [r2, -ip, rrx]", dis.disassembleNext());
        assertEquals("ldr r4, [fp, #-242]!", dis.disassembleNext());
        assertEquals("ldr r5, [r10, r9]!", dis.disassembleNext());
        assertEquals("ldr r6, [r1, lr, lsr #3]!", dis.disassembleNext());
        assertEquals("ldr r7, [ip], #-4095", dis.disassembleNext());
        assertEquals("ldr r8, [r5], -fp", dis.disassembleNext());
        assertEquals("ldr r9, [r7], r3, lsl #7", dis.disassembleNext());
        assertEquals("ldr pc, [r3], #273", dis.disassembleNext());
    }

    /**
     * Tests the ldrb instruction.
     */
    public void testLdrb() {
        asm.ldrb(asm.R1, Address2.pre(asm.SP));
        asm.ldrb(asm.R2, Address2.pre(asm.R7, 0x1BA));
        asm.ldrb(asm.R3, Address2.pre(asm.R2, -1, asm.IP, asm.LSL, 1));
        asm.ldrb(asm.R3, Address2.pre(asm.R2, -1, asm.IP, asm.LSR, 7));
        asm.ldrb(asm.R3, Address2.pre(asm.R2, -1, asm.IP, asm.ASR, 19));
        asm.ldrb(asm.R3, Address2.pre(asm.R2, -1, asm.IP, asm.ROR, 25));
        asm.ldrb(asm.R3, Address2.pre(asm.R2, -1, asm.IP, asm.ROR, 0));
        asm.ldrb(asm.R4, Address2.preW(asm.FP, -0x0F2));
        asm.ldrb(asm.R5, Address2.preW(asm.R10, 1, asm.R9));
        asm.ldrb(asm.R6, Address2.preW(asm.R1, 1, asm.LR, asm.LSR, 3));
        asm.ldrb(asm.R7, Address2.post(asm.IP, -0xFFF));
        asm.ldrb(asm.R8, Address2.post(asm.R5, -1, asm.FP));
        asm.ldrb(asm.R9, Address2.post(asm.R7, 1, asm.R3, asm.LSL, 7));
        asm.ldrb(asm.PC, Address2.post(asm.R3, 0x111));

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0xe5, 0xdd, 0x10, 0x00,
        0xe5, 0xd7, 0x21, 0xba,
        0xe7, 0x52, 0x30, 0x8c,
        0xe7, 0x52, 0x33, 0xac,
        0xe7, 0x52, 0x39, 0xcc,
        0xe7, 0x52, 0x3c, 0xec,
        0xe7, 0x52, 0x30, 0x6c,
        0xe5, 0x7b, 0x40, 0xf2,
        0xe7, 0xfa, 0x50, 0x09,
        0xe7, 0xf1, 0x61, 0xae,
        0xe4, 0x5c, 0x7f, 0xff,
        0xe6, 0x55, 0x80, 0x0b,
        0xe6, 0xd7, 0x93, 0x83,
        0xe4, 0xd3, 0xf1, 0x11 }));

        assertEquals("ldrb r1, [sp]", dis.disassembleNext());
        assertEquals("ldrb r2, [r7, #442]", dis.disassembleNext());
        assertEquals("ldrb r3, [r2, -ip, lsl #1]", dis.disassembleNext());
        assertEquals("ldrb r3, [r2, -ip, lsr #7]", dis.disassembleNext());
        assertEquals("ldrb r3, [r2, -ip, asr #19]", dis.disassembleNext());
        assertEquals("ldrb r3, [r2, -ip, ror #25]", dis.disassembleNext());
        assertEquals("ldrb r3, [r2, -ip, rrx]", dis.disassembleNext());
        assertEquals("ldrb r4, [fp, #-242]!", dis.disassembleNext());
        assertEquals("ldrb r5, [r10, r9]!", dis.disassembleNext());
        assertEquals("ldrb r6, [r1, lr, lsr #3]!", dis.disassembleNext());
        assertEquals("ldrb r7, [ip], #-4095", dis.disassembleNext());
        assertEquals("ldrb r8, [r5], -fp", dis.disassembleNext());
        assertEquals("ldrb r9, [r7], r3, lsl #7", dis.disassembleNext());
        assertEquals("ldrb pc, [r3], #273", dis.disassembleNext());
    }

    /**
     * Tests the ldrcond instruction.
     */
    public void testLdrcond() {
        asm.ldrcond(asm.COND_GT, asm.R1, Address2.pre(asm.SP));
        asm.ldrcond(asm.COND_VC, asm.SP, Address2.post(asm.R7, -1, asm.R6, asm.ASR, 17));
        asm.ldrcond(asm.COND_EQ, asm.PC, Address2.preW(asm.SP, 0xfff));

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0xc5, 0x9d, 0x10, 0x00,
        0x76, 0x17, 0xd8, 0xc6,
        0x05, 0xbd, 0xff, 0xff }));

        assertEquals("ldrgt r1, [sp]", dis.disassembleNext());
        assertEquals("ldrvc sp, [r7], -r6, asr #17", dis.disassembleNext());
        assertEquals("ldreq pc, [sp, #4095]!", dis.disassembleNext());
    }

    /**
     * Tests the ldrcondb instruction.
     */
    public void testLdrcondb() {
        asm.ldrcondb(asm.COND_GT, asm.R1, Address2.pre(asm.SP));
        asm.ldrcondb(asm.COND_VC, asm.SP, Address2.post(asm.R7, -1, asm.R6, asm.ASR, 17));
        asm.ldrcondb(asm.COND_EQ, asm.PC, Address2.preW(asm.SP, 0xfff));

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0xc5, 0xdd, 0x10, 0x00,
        0x76, 0x57, 0xd8, 0xc6,
        0x05, 0xfd, 0xff, 0xff }));

        assertEquals("ldrgtb r1, [sp]", dis.disassembleNext());
        assertEquals("ldrvcb sp, [r7], -r6, asr #17", dis.disassembleNext());
        assertEquals("ldreqb pc, [sp, #4095]!", dis.disassembleNext());
    }

    /**
     * Tests the ldrh instruction.
     */
    public void testLdrh() {
        asm.ldrh(asm.R0, Address3.pre(asm.PC));
        asm.ldrh(asm.SP, Address3.pre(asm.R7, -123));

        asm.ldrh(asm.R8, Address3.pre(asm.R0, 1, asm.R6));
        asm.ldrh(asm.R3, Address3.pre(asm.R6, -1, asm.FP));

        asm.ldrh(asm.R5, Address3.preW(asm.R3, 0));
        asm.ldrh(asm.IP, Address3.preW(asm.R6, 251));

        asm.ldrh(asm.R7, Address3.preW(asm.R9, 1, asm.R4));
        asm.ldrh(asm.PC, Address3.preW(asm.R10, -1, asm.R1));

        asm.ldrh(asm.LR, Address3.post(asm.R4, -250));
        asm.ldrh(asm.FP, Address3.post(asm.FP, 67));

        asm.ldrh(asm.R1, Address3.post(asm.R5, 1, asm.PC));
        asm.ldrh(asm.R2, Address3.post(asm.LR, -1, asm.SP));

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0xe1, 0xdf, 0x00, 0xb0,
        0xe1, 0x57, 0xd7, 0xbb,
        0xe1, 0x90, 0x80, 0xb6,
        0xe1, 0x16, 0x30, 0xbb,
        0xe1, 0xf3, 0x50, 0xb0,
        0xe1, 0xf6, 0xcf, 0xbb,
        0xe1, 0xb9, 0x70, 0xb4,
        0xe1, 0x3a, 0xf0, 0xb1,
        0xe0, 0x54, 0xef, 0xba,
        0xe0, 0xdb, 0xb4, 0xb3,
        0xe0, 0x95, 0x10, 0xbf,
        0xe0, 0x1e, 0x20, 0xbd }));

        assertEquals("ldrh r0, [pc] ; 0x8", dis.disassembleNext());
        assertEquals("ldrh sp, [r7, #-123]", dis.disassembleNext());

        assertEquals("ldrh r8, [r0, r6]", dis.disassembleNext());
        assertEquals("ldrh r3, [r6, -fp]", dis.disassembleNext());

        assertEquals("ldrh r5, [r3]!", dis.disassembleNext());
        assertEquals("ldrh ip, [r6, #251]!", dis.disassembleNext());

        assertEquals("ldrh r7, [r9, r4]!", dis.disassembleNext());
        assertEquals("ldrh pc, [r10, -r1]!", dis.disassembleNext());

        assertEquals("ldrh lr, [r4], #-250", dis.disassembleNext());
        assertEquals("ldrh fp, [fp], #67", dis.disassembleNext());

        assertEquals("ldrh r1, [r5], pc", dis.disassembleNext());
        assertEquals("ldrh r2, [lr], -sp", dis.disassembleNext());
    }

    /**
     * Tests the ldrcondh instruction.
     */
    public void testLdrcondh() {
        asm.ldrcondh(asm.COND_HI, asm.R0, Address3.pre(asm.PC));
        asm.ldrcondh(asm.COND_VC, asm.SP, Address3.post(asm.R7, -123));

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0x81, 0xdf, 0x00, 0xb0,
        0x70, 0x57, 0xd7, 0xbb }));

        assertEquals("ldrhih r0, [pc] ; 0x8", dis.disassembleNext());
        assertEquals("ldrvch sp, [r7], #-123", dis.disassembleNext());
    }

    /**
     * Tests the ldrsb instruction.
     */
    public void testLdrsb() {
        asm.ldrsb(asm.R0, Address3.pre(asm.PC));
        asm.ldrsb(asm.SP, Address3.post(asm.R7, -123));

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0xe1, 0xdf, 0x00, 0xd0,
        0xe0, 0x57, 0xd7, 0xdb }));

        assertEquals("ldrsb r0, [pc] ; 0x8", dis.disassembleNext());
        assertEquals("ldrsb sp, [r7], #-123", dis.disassembleNext());
    }

    /**
     * Tests the ldrcondsb instruction.
     */
    public void testLdrcondsb() {
        asm.ldrcondsb(asm.COND_HI, asm.R0, Address3.pre(asm.PC));
        asm.ldrcondsb(asm.COND_VC, asm.SP, Address3.post(asm.R7, -123));

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0x81, 0xdf, 0x00, 0xd0,
        0x70, 0x57, 0xd7, 0xdb }));

        assertEquals("ldrhisb r0, [pc] ; 0x8", dis.disassembleNext());
        assertEquals("ldrvcsb sp, [r7], #-123", dis.disassembleNext());
    }

    /**
     * Tests the ldrsh instruction.
     */
    public void testLdrsh() {
        asm.ldrsh(asm.R0, Address3.pre(asm.PC));
        asm.ldrsh(asm.SP, Address3.post(asm.R7, -123));

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0xe1, 0xdf, 0x00, 0xf0,
        0xe0, 0x57, 0xd7, 0xfb }));

        assertEquals("ldrsh r0, [pc] ; 0x8", dis.disassembleNext());
        assertEquals("ldrsh sp, [r7], #-123", dis.disassembleNext());
    }

    /**
     * Tests the ldrcondsh instruction.
     */
    public void testLdrcondsh() {
        asm.ldrcondsh(asm.COND_HI, asm.R0, Address3.pre(asm.PC));
        asm.ldrcondsh(asm.COND_VC, asm.SP, Address3.post(asm.R7, -123));

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0x81, 0xdf, 0x00, 0xf0,
        0x70, 0x57, 0xd7, 0xfb }));

        assertEquals("ldrhish r0, [pc] ; 0x8", dis.disassembleNext());
        assertEquals("ldrvcsh sp, [r7], #-123", dis.disassembleNext());
    }
}
