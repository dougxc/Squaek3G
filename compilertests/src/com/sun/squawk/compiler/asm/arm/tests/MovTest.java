/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: MovTest.java,v 1.3 2005/02/03 00:56:40 dl156546 Exp $
 */

package com.sun.squawk.compiler.asm.arm.tests;

import com.sun.squawk.compiler.asm.*;
import com.sun.squawk.compiler.asm.arm.*;
import junit.framework.*;

/**
 * Test fixtures for the mov{cond}{s} instructions.
 *
 * @author David Liu
 * @version 1.0
 */
public class MovTest extends TestCase {
    public MovTest() {
    }

    public static void main (String[] args) {
        junit.textui.TestRunner.run (suite());
    }

    public static junit.framework.Test suite() {
        return new TestSuite(MovTest.class);
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
     * Tests the mov instruction with an immediate source operand.
     */
    public void testMovImm() {
        asm.mov(asm.R0, Operand2.imm(0));
        asm.mov(asm.R8, Operand2.imm(1));
        asm.mov(asm.R8, Operand2.imm(0xb50));

        assertTrue (ArmTests.compareCode (buffer, new int [] {
                     0xe3, 0xa0, 0x00, 0x00,
                     0xe3, 0xa0, 0x80, 0x01,
                     0xe3, 0xa0, 0x8e, 0xb5 }));

        assertEquals("mov r0, #0x0", dis.disassembleNext());
        assertEquals("mov r8, #0x1", dis.disassembleNext());
        assertEquals("mov r8, #0xb50", dis.disassembleNext());
    }

    /**
     * Tests the mov instruction with a register source operand.
     */
    public void testMovReg() {
        asm.mov(asm.R0, Operand2.reg(asm.PC));
        asm.mov(asm.SP, Operand2.reg(asm.R7));

        assertTrue (ArmTests.compareCode (buffer, new int [] {
                     0xe1, 0xa0, 0x00, 0x0f,
                     0xe1, 0xa0, 0xd0, 0x07 }));

        assertEquals("mov r0, pc", dis.disassembleNext());
        assertEquals("mov sp, r7", dis.disassembleNext());
    }

    /**
     * Tests the mov instruction with a register source operand left shifted by an immediate value.
     */
    public void testMovLslImm() {
        asm.mov(asm.R0, Operand2.lsl(asm.R7, 1));
        asm.mov(asm.PC, Operand2.lsl(asm.LR, 17));

        assertTrue (ArmTests.compareCode (buffer, new int [] {
                     0xe1, 0xa0, 0x00, 0x87,
                     0xe1, 0xa0, 0xf8, 0x8e }));

        assertEquals("mov r0, r7, lsl #1", dis.disassembleNext());
        assertEquals("mov pc, lr, lsl #17", dis.disassembleNext());
    }

    /**
     * Tests the mov instruction with a register source operand left shifted by a register value.
     */
    public void testMovLslReg() {
        asm.mov(asm.R0, Operand2.lsl(asm.R7, asm.R5));
        asm.mov(asm.PC, Operand2.lsl(asm.LR, asm.R1));

        assertTrue (ArmTests.compareCode (buffer, new int [] {
                     0xe1, 0xa0, 0x05, 0x17,
                     0xe1, 0xa0, 0xf1, 0x1e }));

        assertEquals("mov r0, r7, lsl r5", dis.disassembleNext());
        assertEquals("mov pc, lr, lsl r1", dis.disassembleNext());
    }

    /**
     * Tests the mov instruction with a register source operand logical right shifted by an
     * immediate value.
     */
    public void testMovLsrImm() {
        asm.mov(asm.R0, Operand2.lsr(asm.R7, 1));
        asm.mov(asm.PC, Operand2.lsr(asm.LR, 17));

        assertTrue (ArmTests.compareCode (buffer, new int [] {
                     0xe1, 0xa0, 0x00, 0xa7,
                     0xe1, 0xa0, 0xf8, 0xae }));

        assertEquals("mov r0, r7, lsr #1", dis.disassembleNext());
        assertEquals("mov pc, lr, lsr #17", dis.disassembleNext());
    }

    /**
     * Tests the mov instruction with a register source operand logical right shifted by a register
     * value.
     */
    public void testMovLsrReg() {
        asm.mov(asm.R0, Operand2.lsr(asm.R7, asm.R5));
        asm.mov(asm.PC, Operand2.lsr(asm.LR, asm.R1));

        assertTrue (ArmTests.compareCode (buffer, new int [] {
                     0xe1, 0xa0, 0x05, 0x37,
                     0xe1, 0xa0, 0xf1, 0x3e }));

        assertEquals("mov r0, r7, lsr r5", dis.disassembleNext());
        assertEquals("mov pc, lr, lsr r1", dis.disassembleNext());
    }

    /**
     * Tests the mov instruction with a register source operand arithmetic right shifted by an
     * immediate value.
     */
    public void testMovAsrImm() {
        asm.mov(asm.R0, Operand2.asr(asm.R7, 1));
        asm.mov(asm.PC, Operand2.asr(asm.LR, 17));

        assertTrue (ArmTests.compareCode (buffer, new int [] {
                     0xe1, 0xa0, 0x00, 0xc7,
                     0xe1, 0xa0, 0xf8, 0xce }));

        assertEquals("mov r0, r7, asr #1", dis.disassembleNext());
        assertEquals("mov pc, lr, asr #17", dis.disassembleNext());
    }

    /**
     * Tests the mov instruction with a register source operand arithmetic right shifted by a
     * register value.
     */
    public void testMovAsrReg() {
        asm.mov(asm.R0, Operand2.asr(asm.R7, asm.R5));
        asm.mov(asm.PC, Operand2.asr(asm.LR, asm.R1));

        assertTrue (ArmTests.compareCode (buffer, new int [] {
                     0xe1, 0xa0, 0x05, 0x57,
                     0xe1, 0xa0, 0xf1, 0x5e }));

        assertEquals("mov r0, r7, asr r5", dis.disassembleNext());
        assertEquals("mov pc, lr, asr r1", dis.disassembleNext());
    }

    /**
     * Tests the mov instruction with a register source operand rotated right by an immediate value.
     */
    public void testMovRorImm() {
        asm.mov(asm.R0, Operand2.ror(asm.R7, 1));
        asm.mov(asm.PC, Operand2.ror(asm.LR, 17));

        assertTrue (ArmTests.compareCode (buffer, new int [] {
                     0xe1, 0xa0, 0x00, 0xe7,
                     0xe1, 0xa0, 0xf8, 0xee }));

        assertEquals("mov r0, r7, ror #1", dis.disassembleNext());
        assertEquals("mov pc, lr, ror #17", dis.disassembleNext());
    }

    /**
     * Tests the mov instruction with a register source operand rotated right by a register value.
     */
    public void testMovRorReg() {
        asm.mov(asm.R0, Operand2.ror(asm.R7, asm.R5));
        asm.mov(asm.PC, Operand2.ror(asm.LR, asm.R1));

        assertTrue (ArmTests.compareCode (buffer, new int [] {
                     0xe1, 0xa0, 0x05, 0x77,
                     0xe1, 0xa0, 0xf1, 0x7e }));

        assertEquals("mov r0, r7, ror r5", dis.disassembleNext());
        assertEquals("mov pc, lr, ror r1", dis.disassembleNext());
    }

    /**
     * Tests the mov instruction with a register source operand rotated right with extend.
     */
    public void testMovRrx() {
        asm.mov(asm.R0, Operand2.rrx(asm.R7));
        asm.mov(asm.PC, Operand2.rrx(asm.LR));

        assertTrue (ArmTests.compareCode (buffer, new int [] {
                     0xe1, 0xa0, 0x00, 0x67,
                     0xe1, 0xa0, 0xf0, 0x6e }));

        assertEquals("mov r0, r7, rrx", dis.disassembleNext());
        assertEquals("mov pc, lr, rrx", dis.disassembleNext());
    }

    /**
     * Tests the movcond instruction.
     */
    public void testMovcond() {
        asm.movcond(asm.COND_EQ, asm.R0, Operand2.reg(asm.PC));
        asm.movcond(asm.COND_NE, asm.R1, Operand2.reg(asm.LR));
        asm.movcond(asm.COND_CS, asm.R2, Operand2.reg(asm.SP));
        asm.movcond(asm.COND_CC, asm.R3, Operand2.reg(asm.IP));
        asm.movcond(asm.COND_MI, asm.R4, Operand2.reg(asm.FP));
        asm.movcond(asm.COND_PL, asm.R5, Operand2.reg(asm.R10));
        asm.movcond(asm.COND_VS, asm.R6, Operand2.reg(asm.R9));
        asm.movcond(asm.COND_VC, asm.R7, Operand2.reg(asm.R8));
        asm.movcond(asm.COND_HI, asm.R8, Operand2.reg(asm.R7));
        asm.movcond(asm.COND_LS, asm.R9, Operand2.reg(asm.R6));
        asm.movcond(asm.COND_GE, asm.R10, Operand2.reg(asm.R5));
        asm.movcond(asm.COND_LT, asm.FP, Operand2.reg(asm.R4));
        asm.movcond(asm.COND_GT, asm.IP, Operand2.reg(asm.R3));
        asm.movcond(asm.COND_LE, asm.SP, Operand2.reg(asm.R2));
        asm.movcond(asm.COND_AL, asm.LR, Operand2.reg(asm.R1));

        assertTrue (ArmTests.compareCode (buffer, new int [] {
                     0x01, 0xa0, 0x00, 0x0f,
                     0x11, 0xa0, 0x10, 0x0e,
                     0x21, 0xa0, 0x20, 0x0d,
                     0x31, 0xa0, 0x30, 0x0c,
                     0x41, 0xa0, 0x40, 0x0b,
                     0x51, 0xa0, 0x50, 0x0a,
                     0x61, 0xa0, 0x60, 0x09,
                     0x71, 0xa0, 0x70, 0x08,
                     0x81, 0xa0, 0x80, 0x07,
                     0x91, 0xa0, 0x90, 0x06,
                     0xa1, 0xa0, 0xa0, 0x05,
                     0xb1, 0xa0, 0xb0, 0x04,
                     0xc1, 0xa0, 0xc0, 0x03,
                     0xd1, 0xa0, 0xd0, 0x02,
                     0xe1, 0xa0, 0xe0, 0x01 }));

        assertEquals("moveq r0, pc", dis.disassembleNext());
        assertEquals("movne r1, lr", dis.disassembleNext());
        assertEquals("movcs r2, sp", dis.disassembleNext());
        assertEquals("movcc r3, ip", dis.disassembleNext());
        assertEquals("movmi r4, fp", dis.disassembleNext());
        assertEquals("movpl r5, r10", dis.disassembleNext());
        assertEquals("movvs r6, r9", dis.disassembleNext());
        assertEquals("movvc r7, r8", dis.disassembleNext());
        assertEquals("movhi r8, r7", dis.disassembleNext());
        assertEquals("movls r9, r6", dis.disassembleNext());
        assertEquals("movge r10, r5", dis.disassembleNext());
        assertEquals("movlt fp, r4", dis.disassembleNext());
        assertEquals("movgt ip, r3", dis.disassembleNext());
        assertEquals("movle sp, r2", dis.disassembleNext());
        assertEquals("mov lr, r1", dis.disassembleNext());
    }

    /**
     * Tests the movs instruction.
     */
    public void testMovs() {
        asm.movs(asm.R1, Operand2.reg(asm.SP));
        asm.movs(asm.R10, Operand2.reg(asm.R3));

        assertTrue(ArmTests.compareCode (buffer, new int [] {
                     0xe1, 0xb0, 0x10, 0x0d,
                     0xe1, 0xb0, 0xa0, 0x03 }));

        assertEquals("movs r1, sp", dis.disassembleNext());
        assertEquals("movs r10, r3", dis.disassembleNext());
    }

    /**
     * Tests the movconds instruction.
     */
    public void testMovconds() {
        asm.movconds(asm.COND_GT, asm.R5, Operand2.imm(0xBA0));
        asm.movconds(asm.COND_CS, asm.PC, Operand2.ror(asm.SP, asm.R3));

        assertTrue(ArmTests.compareCode (buffer, new int [] {
                     0xc3, 0xb0, 0x5e, 0xba,
                     0x21, 0xb0, 0xf3, 0x7d }));

        assertEquals("movgts r5, #0xba0", dis.disassembleNext());
        assertEquals("movcss pc, sp, ror r3", dis.disassembleNext());
    }
}
