/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: StmTest.java,v 1.4 2005/02/03 00:56:40 dl156546 Exp $
 */

package com.sun.squawk.compiler.asm.arm.tests;

import com.sun.squawk.compiler.asm.*;
import com.sun.squawk.compiler.asm.arm.*;
import junit.framework.*;

/**
 * Test fixtures for the stm instruction.
 *
 * @author David Liu
 * @version 1.0
 */
public class StmTest extends TestCase {
    public StmTest() {
    }

    public static void main (String[] args) {
        junit.textui.TestRunner.run (suite());
    }

    public static junit.framework.Test suite() {
        return new TestSuite(StmTest.class);
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
     * Tests the stm instruction.
     */
    public void testStm() {
        asm.stm(new Address4(asm.ADDR_DA, asm.R3, new RegRange [] { new RegRange(asm.R0, asm.R2) }));
        asm.stm(new Address4(asm.ADDR_DA, asm.R3, true, new RegRange [] { new RegRange(asm.R0, asm.R2) }));

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0xe8, 0x03, 0x00, 0x07,
        0xe8, 0x23, 0x00, 0x07 }));

        assertEquals("stmda r3, {r0, r1, r2}", dis.disassembleNext());
        assertEquals("stmda r3!, {r0, r1, r2}", dis.disassembleNext());
    }

    /**
     * Tests the stmcond instruction.
     */
    public void testStmcond() {
        asm.stmcond(asm.COND_CC, new Address4(asm.ADDR_DA, asm.R3, new RegRange [] { new RegRange(asm.R0, asm.R2) }));
        asm.stmcond(asm.COND_EQ, new Address4(asm.ADDR_DA, asm.R3, new RegRange [] { new RegRange(asm.R0, asm.R2) }));
        asm.stmcond(asm.COND_GT, new Address4(asm.ADDR_DA, asm.R3, new RegRange [] { new RegRange(asm.R0, asm.R2) }));
        asm.stmcond(asm.COND_LS, new Address4(asm.ADDR_DA, asm.R3, new RegRange [] { new RegRange(asm.R0, asm.R2) }));
        asm.stmcond(asm.COND_CC, new Address4(asm.ADDR_DA, asm.R3, true, new RegRange [] { new RegRange(asm.R0, asm.R2) }));
        asm.stmcond(asm.COND_EQ, new Address4(asm.ADDR_DA, asm.R3, true, new RegRange [] { new RegRange(asm.R0, asm.R2) }));
        asm.stmcond(asm.COND_GT, new Address4(asm.ADDR_DA, asm.R3, true, new RegRange [] { new RegRange(asm.R0, asm.R2) }));
        asm.stmcond(asm.COND_LS, new Address4(asm.ADDR_DA, asm.R3, true, new RegRange [] { new RegRange(asm.R0, asm.R2) }));

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0x38, 0x03, 0x00, 0x07,
        0x08, 0x03, 0x00, 0x07,
        0xc8, 0x03, 0x00, 0x07,
        0x98, 0x03, 0x00, 0x07,
        0x38, 0x23, 0x00, 0x07,
        0x08, 0x23, 0x00, 0x07,
        0xc8, 0x23, 0x00, 0x07,
        0x98, 0x23, 0x00, 0x07 }));

        assertEquals("stmccda r3, {r0, r1, r2}", dis.disassembleNext());
        assertEquals("stmeqda r3, {r0, r1, r2}", dis.disassembleNext());
        assertEquals("stmgtda r3, {r0, r1, r2}", dis.disassembleNext());
        assertEquals("stmlsda r3, {r0, r1, r2}", dis.disassembleNext());
        assertEquals("stmccda r3!, {r0, r1, r2}", dis.disassembleNext());
        assertEquals("stmeqda r3!, {r0, r1, r2}", dis.disassembleNext());
        assertEquals("stmgtda r3!, {r0, r1, r2}", dis.disassembleNext());
        assertEquals("stmlsda r3!, {r0, r1, r2}", dis.disassembleNext());
    }

    /**
     * Tests the register list paramter.
     */
    public void testRegsList() {
       asm.stm(new Address4(asm.ADDR_DA, asm.R0, new RegRange [] { new RegRange(asm.R0) }));
       asm.stm(new Address4(asm.ADDR_DA, asm.R0, new RegRange [] {
           new RegRange(asm.R7), new RegRange(asm.R11), new RegRange(asm.R15) }));
       asm.stm(new Address4(asm.ADDR_DA, asm.R0, new RegRange [] {
           new RegRange(asm.R0, asm.R11), new RegRange(asm.R13, asm.R15) }));
       asm.stm(new Address4(asm.ADDR_DA, asm.R0, new RegRange [] {
           new RegRange(asm.R0, asm.R15) }));

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0xe8, 0x00, 0x00, 0x01,
        0xe8, 0x00, 0x88, 0x80,
        0xe8, 0x00, 0xef, 0xff,
        0xe8, 0x00, 0xff, 0xff }));

        assertEquals("stmda r0, {r0}", dis.disassembleNext());
        assertEquals("stmda r0, {r7, fp, pc}", dis.disassembleNext());
        assertEquals("stmda r0, {r0, r1, r2, r3, r4, r5, r6, r7, r8, r9, r10, fp, sp, lr, pc}", dis.disassembleNext());
        assertEquals("stmda r0, {r0, r1, r2, r3, r4, r5, r6, r7, r8, r9, r10, fp, ip, sp, lr, pc}", dis.disassembleNext());
    }

    /**
     * Tests the addressing mode parameter.
     */
    public void testAddressingMode() {
        asm.stm(new Address4(asm.ADDR_IA, asm.R7, new RegRange [] { new RegRange(asm.R1), new RegRange(asm.R13) }));
        asm.stm(new Address4(asm.ADDR_IB, asm.R7, new RegRange [] { new RegRange(asm.R1), new RegRange(asm.R13) }));
        asm.stm(new Address4(asm.ADDR_DA, asm.R7, new RegRange [] { new RegRange(asm.R1), new RegRange(asm.R13) }));
        asm.stm(new Address4(asm.ADDR_DB, asm.R7, new RegRange [] { new RegRange(asm.R1), new RegRange(asm.R13) }));
        asm.stm(new Address4(asm.ADDR_FD, asm.R7, new RegRange [] { new RegRange(asm.R1), new RegRange(asm.R13) }));
        asm.stm(new Address4(asm.ADDR_ED, asm.R7, new RegRange [] { new RegRange(asm.R1), new RegRange(asm.R13) }));
        asm.stm(new Address4(asm.ADDR_FA, asm.R7, new RegRange [] { new RegRange(asm.R1), new RegRange(asm.R13) }));
        asm.stm(new Address4(asm.ADDR_EA, asm.R7, new RegRange [] { new RegRange(asm.R1), new RegRange(asm.R13) }));

        assertTrue (ArmTests.compareCode (buffer, new int [] {
        0xe8, 0x87, 0x20, 0x02,
        0xe9, 0x87, 0x20, 0x02,
        0xe8, 0x07, 0x20, 0x02,
        0xe9, 0x07, 0x20, 0x02,
        0xe9, 0x07, 0x20, 0x02,
        0xe8, 0x07, 0x20, 0x02,
        0xe9, 0x87, 0x20, 0x02,
        0xe8, 0x87, 0x20, 0x02 }));

        assertEquals("stmia r7, {r1, sp}", dis.disassembleNext());
        assertEquals("stmib r7, {r1, sp}", dis.disassembleNext());
        assertEquals("stmda r7, {r1, sp}", dis.disassembleNext());
        assertEquals("stmdb r7, {r1, sp}", dis.disassembleNext());
        assertEquals("stmdb r7, {r1, sp}", dis.disassembleNext());
        assertEquals("stmda r7, {r1, sp}", dis.disassembleNext());
        assertEquals("stmib r7, {r1, sp}", dis.disassembleNext());
        assertEquals("stmia r7, {r1, sp}", dis.disassembleNext());
    }
}
