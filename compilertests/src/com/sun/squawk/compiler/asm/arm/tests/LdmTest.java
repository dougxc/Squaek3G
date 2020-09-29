/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: LdmTest.java,v 1.4 2005/02/03 00:56:40 dl156546 Exp $
 */

package com.sun.squawk.compiler.asm.arm.tests;

import com.sun.squawk.compiler.asm.*;
import com.sun.squawk.compiler.asm.arm.*;
import junit.framework.*;

/**
 * Test fixtures for the ldm instruction.
 *
 * @author David Liu
 * @version 1.0
 */
public class LdmTest extends TestCase {
    public LdmTest() {
    }

    public static void main (String[] args) {
        junit.textui.TestRunner.run (suite());
    }

    public static junit.framework.Test suite() {
        return new TestSuite(LdmTest.class);
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
     * Tests the ldm instruction.
     */
    public void testLdm() {
        asm.ldm(new Address4(asm.ADDR_DA, asm.R3, new RegRange [] { new RegRange(asm.R0, asm.R2) }));
        asm.ldm(new Address4(asm.ADDR_DA, asm.R3, true, new RegRange [] { new RegRange(asm.R0, asm.R2) }));

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0xe8, 0x13, 0x00, 0x07,
        0xe8, 0x33, 0x00, 0x07 }));

        assertEquals("ldmda r3, {r0, r1, r2}", dis.disassembleNext());
        assertEquals("ldmda r3!, {r0, r1, r2}", dis.disassembleNext());
    }

    /**
     * Tests the ldmcond instruction.
     */
    public void testLdmcond() {
        asm.ldmcond(asm.COND_CC, new Address4(asm.ADDR_DA, asm.R3, new RegRange [] { new RegRange(asm.R0, asm.R2) }));
        asm.ldmcond(asm.COND_EQ, new Address4(asm.ADDR_DA, asm.R3, new RegRange [] { new RegRange(asm.R0, asm.R2) }));
        asm.ldmcond(asm.COND_GT, new Address4(asm.ADDR_DA, asm.R3, new RegRange [] { new RegRange(asm.R0, asm.R2) }));
        asm.ldmcond(asm.COND_LS, new Address4(asm.ADDR_DA, asm.R3, new RegRange [] { new RegRange(asm.R0, asm.R2) }));
        asm.ldmcond(asm.COND_CC, new Address4(asm.ADDR_DA, asm.R3, true, new RegRange [] { new RegRange(asm.R0, asm.R2) }));
        asm.ldmcond(asm.COND_EQ, new Address4(asm.ADDR_DA, asm.R3, true, new RegRange [] { new RegRange(asm.R0, asm.R2) }));
        asm.ldmcond(asm.COND_GT, new Address4(asm.ADDR_DA, asm.R3, true, new RegRange [] { new RegRange(asm.R0, asm.R2) }));
        asm.ldmcond(asm.COND_LS, new Address4(asm.ADDR_DA, asm.R3, true, new RegRange [] { new RegRange(asm.R0, asm.R2) }));

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0x38, 0x13, 0x00, 0x07,
        0x08, 0x13, 0x00, 0x07,
        0xc8, 0x13, 0x00, 0x07,
        0x98, 0x13, 0x00, 0x07,
        0x38, 0x33, 0x00, 0x07,
        0x08, 0x33, 0x00, 0x07,
        0xc8, 0x33, 0x00, 0x07,
        0x98, 0x33, 0x00, 0x07 }));

        assertEquals("ldmccda r3, {r0, r1, r2}", dis.disassembleNext());
        assertEquals("ldmeqda r3, {r0, r1, r2}", dis.disassembleNext());
        assertEquals("ldmgtda r3, {r0, r1, r2}", dis.disassembleNext());
        assertEquals("ldmlsda r3, {r0, r1, r2}", dis.disassembleNext());
        assertEquals("ldmccda r3!, {r0, r1, r2}", dis.disassembleNext());
        assertEquals("ldmeqda r3!, {r0, r1, r2}", dis.disassembleNext());
        assertEquals("ldmgtda r3!, {r0, r1, r2}", dis.disassembleNext());
        assertEquals("ldmlsda r3!, {r0, r1, r2}", dis.disassembleNext());
    }

    /**
     * Tests the register list parameter.
     */
    public void testRegsList() {
        asm.ldm(new Address4(asm.ADDR_DA, asm.R0, new RegRange [] { new RegRange(asm.R0) }));
        asm.ldm(new Address4(asm.ADDR_DA, asm.R0, new RegRange [] {
            new RegRange(asm.R7), new RegRange(asm.FP), new RegRange(asm.PC) }));
        asm.ldm(new Address4(asm.ADDR_DA, asm.R0, new RegRange [] {
            new RegRange(asm.R0, asm.FP), new RegRange(asm.SP, asm.PC) }));
        asm.ldm(new Address4(asm.ADDR_DA, asm.R0, new RegRange [] {
            new RegRange(asm.R0, asm.PC) }));

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0xe8, 0x10, 0x00, 0x01,
        0xe8, 0x10, 0x88, 0x80,
        0xe8, 0x10, 0xef, 0xff,
        0xe8, 0x10, 0xff, 0xff }));

        assertEquals("ldmda r0, {r0}", dis.disassembleNext());
        assertEquals("ldmda r0, {r7, fp, pc}", dis.disassembleNext());
        assertEquals("ldmda r0, {r0, r1, r2, r3, r4, r5, r6, r7, r8, r9, r10, fp, sp, lr, pc}", dis.disassembleNext());
        assertEquals("ldmda r0, {r0, r1, r2, r3, r4, r5, r6, r7, r8, r9, r10, fp, ip, sp, lr, pc}", dis.disassembleNext());
    }

    /**
     * Tests the addressing mode parameter.
     */
    public void testAddressingMode() {
        asm.ldm(new Address4(asm.ADDR_IA, asm.R7, new RegRange [] { new RegRange(asm.R1), new RegRange(asm.SP) }));
        asm.ldm(new Address4(asm.ADDR_IB, asm.R7, new RegRange [] { new RegRange(asm.R1), new RegRange(asm.SP) }));
        asm.ldm(new Address4(asm.ADDR_DA, asm.R7, new RegRange [] { new RegRange(asm.R1), new RegRange(asm.SP) }));
        asm.ldm(new Address4(asm.ADDR_DB, asm.R7, new RegRange [] { new RegRange(asm.R1), new RegRange(asm.SP) }));
        asm.ldm(new Address4(asm.ADDR_FD, asm.R7, new RegRange [] { new RegRange(asm.R1), new RegRange(asm.SP) }));
        asm.ldm(new Address4(asm.ADDR_ED, asm.R7, new RegRange [] { new RegRange(asm.R1), new RegRange(asm.SP) }));
        asm.ldm(new Address4(asm.ADDR_FA, asm.R7, new RegRange [] { new RegRange(asm.R1), new RegRange(asm.SP) }));
        asm.ldm(new Address4(asm.ADDR_EA, asm.R7, new RegRange [] { new RegRange(asm.R1), new RegRange(asm.SP) }));

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0xe8, 0x97, 0x20, 0x02,
        0xe9, 0x97, 0x20, 0x02,
        0xe8, 0x17, 0x20, 0x02,
        0xe9, 0x17, 0x20, 0x02,
        0xe8, 0x97, 0x20, 0x02,
        0xe9, 0x97, 0x20, 0x02,
        0xe8, 0x17, 0x20, 0x02,
        0xe9, 0x17, 0x20, 0x02 }));

        assertEquals("ldmia r7, {r1, sp}", dis.disassembleNext());
        assertEquals("ldmib r7, {r1, sp}", dis.disassembleNext());
        assertEquals("ldmda r7, {r1, sp}", dis.disassembleNext());
        assertEquals("ldmdb r7, {r1, sp}", dis.disassembleNext());
        assertEquals("ldmia r7, {r1, sp}", dis.disassembleNext());
        assertEquals("ldmib r7, {r1, sp}", dis.disassembleNext());
        assertEquals("ldmda r7, {r1, sp}", dis.disassembleNext());
        assertEquals("ldmdb r7, {r1, sp}", dis.disassembleNext());
    }
}
