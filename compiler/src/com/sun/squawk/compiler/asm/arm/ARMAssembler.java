/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: ARMAssembler.java,v 1.1 2005/02/03 00:56:08 dl156546 Exp $
 */

package com.sun.squawk.compiler.asm.arm;

import com.sun.squawk.compiler.asm.*;

/**
 * ARMv4 (ARM7) assembler for the Squawk JVM. This class assembles mnemonics into machine code and
 * supports labels and basic code relocation. DSP, co-processor and privileged mode instructions
 * are not currently supported. Each assembler mnemonic has a corresponding set of methods that
 * emit the machine code bytes to the end of the code buffer. For more information about how the
 * support classes for register and memory addressing are used, refer to the package level
 * documentation.
 * <p>
 * The supported ARM mnemonics have corresponding methods, for example <code>nop</code> has a
 * corresponding <code>nop()</code> method in this class. Predicated and other mnemonics with
 * other variants have multiple methods, for example <code>mov{&lt;cond>}{s}</code> is supported by the
 * <code>mov()</code>, <code>movcond()</code>, <code>movconds()</code> and <code>movs()</code>
 * methods. In general, instruction variants are supported in the following manner:
 * <ul>
 *     <li>Predicated conditions, represented as <code>{&lt;cond>}</code> in the mnemonic, have two
 *         overloaded variants of the instruction - one with and one without the condition. For
 *         example, <code>b{&lt;cond>}</code> is supported by the <code>b()</code> and
 *         <code>bcond()</code> methods. Refer to the {@link Constants} class for the list of
 *         supported condition codes.
 *     <li>CPSR update flags, represented as <code>{S}</code> in the mnemonic, also have two
 *         overloaded variants of the instruction. For example, the <code>mov()</code> method emits
 *         an instruction that does not update the CPSR, whereas the <code>movs()</code> method does.
 *         These variants can be in addition to the predicated conditions, giving a total of four
 *         methods in the class for the <code>mov</code> mnemonic.
 *     <li>Multiple register load and store addressing modes (for example increment/decrement after/before)
 *         are specified in the <code>Address4</code> class instead of method variants.
 * </ul>
 * <p>
 * The ARM registers, flexible source operands, and memory/register addressing modes are represented
 * by the {@link Register} / {@link RegRange}, {@link Operand2}, {@link Address2}, {@link Address3}
 * and {@link Address4} classes.
 * <p>
 * For more information on the ARM assembler, refer to the package level documentation.
 *
 * @author   David Liu
 * @version  1.00
 */
public class ARMAssembler extends AbstractAssembler implements Constants {

    /**
     * Relocation type for absolute int addresses.
     */
    public static final int RELOC_ABSOLUTE_INT = 0;

    /**
     * Relocation type for relative int addresses.
     */
    public static final int RELOC_RELATIVE_INT = 1;

    /**
     * List of relocation records.
     */
    private Relocator relocs;

    /**
     * Count of unbound labels.
     */
    private int unboundLabelCount;

    /**
     * Constructs a new assembler generating code into the specified buffer.
     *
     * @param  code  code buffer that stores the instructions
     */
    public ARMAssembler(CodeBuffer code) {
        super(code);
    }

    /**
     * Aligns the next instruction on a word boundary.
     */
    public void align() {
        align(4);
    }

    /**
     * Aligns the next instruction to the specified boundary.
     *
     * @param  modulus  modulus of alignment
     */
    public void align(int modulus) {
        while (getOffset() % modulus != 0) {
            emitByte(0);
        }
    }

    /**
     * Returns the current base address used for instructions relative to the PC register.
     *
     * @return  current base address
     */
    public int getBase() {
        return getOffset() + 8;
    }

    /**
     * Allocate a new label.
     *
     * @return  a new label
     */
    public ALabel newLabel() {
        return new ALabel(this);
    }

    /**
     * Adjust the number of unbound labels.
     *
     * @param  x  the amount to adjust by
     */
    public void unboundLabelCount(int x) {
        unboundLabelCount += x;
    }

    /**
     * Binds the specified label to the current code position.
     *
     * @param  label  label to be bound
     */
    public void bind(ALabel label) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(!label.isBound(), "label can be bound once only");
        }
        label.bindTo(getOffset());
    }

    /**
     * Emits the specified unsigned byte value into the code buffer.
     *
     * @param  x  byte to be emitted
     */
    public void emitByte(int x) {
        code.emit(x);
    }

    /**
     * Emits an array of unsigned byte values into the code buffer.
     *
     * @param x bytes to be emitted
     */
    public void emitBytes(int [] x) {
        for (int i = 0; i < x.length; i++) {
            code.emit(x [i]);
        }
    }

    /**
     * Emits the specified 16-bit integer value into the code buffer.
     *
     * @param  x  16-bit value to be emitted
     */
    public void emitShort(int x) {
        emitByte(x >>> 8);
        emitByte(x & 0xff);
    }

    /**
     * Emits the specified 32-bit integer value into the code buffer.
     *
     * @param  x  32-bit value to be emitted
     */
    public void emitInt(int x) {
        emitShort(x >>> 16);
        emitShort(x & 0xffff);
    }

    /**
     * Emits the specified 24-bit integer value into the code buffer.
     *
     * @param x 24-bit value to be emitted
     */
    public void emitInt24(int x) {
        emitByte((x >>> 16) & 0xff);
        emitByte((x >>> 8) & 0xff);
        emitByte(x & 0xff);
    }

    /**
     * Emits the specified data into the code buffer and generates relocation
     * information.
     *
     * @param  label   the label containing the displacement
     */
    public void emitLabel(ALabel label) {
        relocs = new Relocator(code.getCodePos(), relocs);
        label.addRelocator(relocs);
        emitInt(0xDEADBEEF);
    }

    /**
     * Emits the specified data into the code buffer and generates relocation
     * information.
     *
     * @param  label   the label containing the displacement
     * @param  disp    the displacement when the label is null
     */
    protected void emitData(ALabel label, int disp) {
        if (label == null) {
            emitInt(disp);
        } else {
            emitLabel(label);
        }
    }

    /**
     * Produces and returns a hex dump (with addresses) of the code.
     *
     * @return hex dump of the code
     */
    public String getHexDump() {
        StringBuffer s = new StringBuffer("Code buffer has " + code.getCodeSize() + " bytes:\n\n");

        for (int i = 0; i < code.getCodeSize(); i += 4) {
            String addr = Integer.toHexString (i + code.getRelocation());
            s.append("0000".substring(addr.length()) + addr + ":    ");

            for (int j = i; (j < i + 4) &&  (j < code.getCodeSize ()); j++) {
                String hex = Integer.toHexString((int) code.getBytes() [j] & 0xff);
                s.append((hex.length() < 2 ? "0" : "") + hex + " ");
            }

            s.append("\n");
        }

        return s.toString();
    }

    /**
     * Patches an instruction that uses a Mode 2 address to refer to a label such as ldr or str.
     *
     * @param position    position in the code buffer of the instruction
     * @param label       label whose address is to be patched
     */
    public void patchAddress2Label(int position, ALabel label) {
        int instr = (code.getBytes() [position] << 24) |
            (((int) code.getBytes() [position + 1] & 0xff) << 16) |
            (((int) code.getBytes() [position + 2] & 0xff) << 8) |
            ((int) code.getBytes() [position + 3] & 0xff);

        int sOffset = label.getPos() - position - 8;
        int sign = (sOffset >= 0) ? 1 : 0;
        int offset = Math.abs(sOffset);
        Assert.that(offset <= 0xfff, "relative address offset too large");
        instr = (instr & 0xff7ff000) | (sign << 23) | (offset & 0xfff);

        code.setByteAt(position, instr >>> 24);
        code.setByteAt(position + 1, instr >>> 16);
        code.setByteAt(position + 2, instr >>> 8);
        code.setByteAt(position + 3, instr);
    }

    /**
     * Patches an instruction that uses a Mode 3 address to refer to a label such as ldrh or strh.
     *
     * @param position    position in the code buffer of the instruction
     * @param label       label whose address is to be patched
     */
    public void patchAddress3Label(int position, ALabel label) {
        int instr = (code.getBytes() [position] << 24) |
            (((int) code.getBytes() [position + 1] & 0xff) << 16) |
            (((int) code.getBytes() [position + 2] & 0xff) << 8) |
            ((int) code.getBytes() [position + 3] & 0xff);

        int sOffset = label.getPos() - position - 8;
        int sign = (sOffset >= 0) ? 1 : 0;
        int offset = Math.abs(sOffset);
        Assert.that(offset <= 0xff, "relative address offset too large");
        instr = (instr & 0xff7ff0f0) | (sign << 23) | ((offset & 0xf0) << 4) | (offset & 0xf);

        code.setByteAt(position, instr >>> 24);
        code.setByteAt(position + 1, instr >>> 16);
        code.setByteAt(position + 2, instr >>> 8);
        code.setByteAt(position + 3, instr);
    }

    /**
     * Relocate the code buffer to a specific address.
     *
     * @param  address   the code buffer address.
     * @return           an array if ints that contain the relocation information
     */
    public int[] relocate(int address) {
        Assert.that(unboundLabelCount == 0, "Unbound label count = "+unboundLabelCount);
        int save = code.getCodePos();

        /*
         * Count the number of relocators and allocate an array large
         * enough to hold the rellocation info
         */
        int count = 0;
        Relocator r = relocs;
        while (r != null) {
            count++;
            r = r.getNext();
        }
        int[] relinfo = new int[count];

        /*
         * Iterate throught the relocators resolveing their addresses and
         * recording the relocation information.
         */
        while (relocs != null) {
            relinfo[--count] = relocs.emit(this, address);
            relocs = relocs.getNext();
        }

        code.setCodePos(save);
        code.setRelocation(address);
        return relinfo;
    }

    /**
     * Emits a branch instruction into the code buffer.
     *
     * @param cond condition flag under which the branch will occur
     * @param l specifies whether the link address should be saved (1) or not (0)
     * @param label destination operand label
     */
    private void emitBranch(int cond, int l, ALabel label) {
        Assert.that(l == 0 || l == 1, "invalid l value");

        final int base = getBase ();

        emitByte((cond << 4) | 0xa | l);
        int offset24 = 0xcafebb;

        if (label.isBound()) {
            final int offset = label.getPos() - base;
            Assert.that(offset >= -33554432 && offset <= 33554428, "branch offset too large");
            offset24 = (offset >>> 2) & 0xffffff;
        } else {
            label.addBranch(cond, l, base - 8);
        }

        emitByte(offset24 >>> 16);
        emitByte(offset24 >>> 8);
        emitByte(offset24);
    }

    /**
     * Emits an arithmetic or logic instruction into the code buffer.
     *
     * @param cond condition flag under which this instruction will be executed
     * @param opcode opcode of the instruction
     * @param updateFlags determines if the status flags are to be updated by this instruction
     * @param dst destination operand register
     * @param src first source operand register
     * @param op2 flexible second operand
     */
    private void emitDataProc(int cond, int opcode, boolean updateFlags,
                              Register dst, Register src, Operand2 op2) {
        switch (op2.getType())
        {
            case OPER2_IMM: emitDataProcImm(cond, opcode, updateFlags, dst, src, op2); break;
            case OPER2_REG: emitDataProcReg(cond, opcode, updateFlags, dst, src, op2); break;
            case OPER2_LSL_IMM: emitDataProcSftImm(cond, opcode, 0x0, updateFlags, dst, src, op2); break;
            case OPER2_LSL_REG: emitDataProcSftReg(cond, opcode, 0x1, updateFlags, dst, src, op2); break;
            case OPER2_LSR_IMM: emitDataProcSftImm(cond, opcode, 0x2, updateFlags, dst, src, op2); break;
            case OPER2_LSR_REG: emitDataProcSftReg(cond, opcode, 0x3, updateFlags, dst, src, op2); break;
            case OPER2_ASR_IMM: emitDataProcSftImm(cond, opcode, 0x4, updateFlags, dst, src, op2); break;
            case OPER2_ASR_REG: emitDataProcSftReg(cond, opcode, 0x5, updateFlags, dst, src, op2); break;
            case OPER2_ROR_IMM: emitDataProcSftImm(cond, opcode, 0x6, updateFlags, dst, src, op2); break;
            case OPER2_ROR_REG: emitDataProcSftReg(cond, opcode, 0x7, updateFlags, dst, src, op2); break;
            case OPER2_RRX: emitDataProcRRX(cond, opcode, updateFlags, dst, src, op2); break;
            default: Assert.that(false, "should not reach here");
        }
    }

    /**
     * Emits a data processing instruction with a constant operand.
     *
     * @param cond condition under which the instruction is executed
     * @param opcode operation of the instruction
     * @param updateFlags whether the condition codes are to be updated by the instruction
     * @param dst destination register
     * @param src first operand register
     * @param op2 second operand
     */
    private void emitDataProcImm(int cond, int opcode, boolean updateFlags, Register dst,
                                 Register src, Operand2 op2) {
        emitByte((cond << 4) | 0x2 | ((opcode >>> 3) & 1));
        emitByte((opcode << 5) | (updateFlags ? 0x10 : 0) | (src.getNumber () & 0xf));
        emitByte(((dst.getNumber() & 0xf) << 4) | (AsmHelpers.getPackedImmRot(op2.getImm()) & 0xf));
        emitByte(AsmHelpers.getPackedImm8(op2.getImm()) & 0xff);
    }

    /**
     * Emits a data processing instruction with a register operand.
     *
     * @param cond condition under which the instruction is executed
     * @param opcode operation of the instruction
     * @param updateFlags whether the condition codes are to be updated by the instruction
     * @param dst destination register
     * @param src first operand register
     * @param op2 the second operand
     */
    private void emitDataProcReg(int cond, int opcode, boolean updateFlags, Register dst,
                                 Register src, Operand2 op2) {
        emitByte((cond << 4) | ((opcode >>> 3) & 1));
        emitByte((opcode << 5) | (updateFlags ? 0x10 : 0) | (src.getNumber () & 0xf));
        emitByte((dst.getNumber() & 0xf) << 4);
        emitByte(op2.getReg().getNumber() & 0xf);
    }

    /**
     * Emits a data processing instruction with a register operand that is shifted by an immediate
     * value. The shift operation performed is specified by the oper parameter.
     *
     * @param cond condition under which the instruction is executed
     * @param opcode operation of the instruction
     * @param oper shift operation to perform
     * @param updateFlags whether the condition codes are to be updated by the instruction
     * @param dst destination register
     * @param src source operand register
     * @param op2 the second operand
     */
    private void emitDataProcSftImm(int cond, int opcode, int oper, boolean updateFlags,
                                    Register dst, Register src, Operand2 op2) {
        Assert.that(oper == 0x0 || oper == 0x2 || oper == 0x4 || oper == 0x6, "invalid shift operation");
        emitByte((cond << 4) | ((opcode >>> 3) & 1));
        emitByte((opcode << 5) | (updateFlags ? 0x10 : 0) | (src.getNumber () & 0xf));
        emitByte(((dst.getNumber() & 0xf) << 4) | (op2.getImm() >>> 1));
        emitByte(((op2.getImm() << 7) & 0x80) | ((oper & 0x7) << 4) | (op2.getReg().getNumber() & 0xf));
    }

    /**
     * Emits a data processing instruction with a register operand that is shifted by a register
     * value. The shift operation performed is specified by the oper parameter.
     *
     * @param cond condition under which the instruction is executed
     * @param opcode operation of the instruction
     * @param oper shift operation to perform
     * @param updateFlags whether the condition codes are to be updated by the instruction
     * @param dst destination register
     * @param src first operand register
     * @param op2 the second operand
     */
    private void emitDataProcSftReg(int cond, int opcode, int oper, boolean updateFlags,
                                    Register dst, Register src, Operand2 op2) {
        Assert.that(oper == 0x1 || oper == 0x3 || oper == 0x5 || oper == 0x7, "invalid shift operation");
        emitByte((cond << 4) | ((opcode >>> 3) & 1));
        emitByte((opcode << 5) | (updateFlags ? 0x10 : 0) | (src.getNumber () & 0xf));
        emitByte(((dst.getNumber() & 0xf) << 4) | (op2.getShift().getNumber() & 0xf));
        emitByte(((oper & 0xf) << 4) | (op2.getReg().getNumber() & 0xf));
    }

    /**
     * Emits a data processing instruction with a register operand that is rotated 33-bits right
     * using the Carry Flag as the 33rd bit.
     *
     * @param cond condition under which the instruction is executed
     * @param opcode operation of the instruction
     * @param updateFlags whether the condition codes are to be updated by the instruction
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    private void emitDataProcRRX(int cond, int opcode, boolean updateFlags, Register dst,
                                    Register src, Operand2 op2) {
        emitByte((cond << 4) | ((opcode >>> 3) & 1));
        emitByte((opcode << 5) | (updateFlags ? 0x10 : 0) | (src.getNumber () & 0xf));
        emitByte((dst.getNumber() & 0xf) << 4);
        emitByte(0x60 | (op2.getReg().getNumber() & 0xf));
    }

    /**
     * Emits a load or store instruction.
     *
     * @param cond condition under which the instruction is executed
     * @param access specifies unsigned byte (1) and word (0) access
     * @param action distinguishes between a load (1) and store (0)
     * @param dst destination register
     * @param addr memory address
     */
    private void emitLoadStore(int cond, int access, int action, Register dst, Address2 addr) {
        Assert.that(access == 0 || access == 1, "access can only be 0 or 1");
        Assert.that(action == 0 || action == 1, "action can only be 0 or 1");

        if (addr.getLabel() != null) {
            ALabel label = addr.getLabel();

            if (label.isBound()) {
                addr.resolveLabel(getBase());
            } else {
                label.addAddress2Instr(getOffset());
            }
        }

        final int bit25 = addr.getType() == ADDR_IMM ? 0 : 0x2;
        final int p = addr.getPreIndexed () ? 1 : 0;
        final int s = addr.getSign () ? 0x80 : 0;
        final int b = access << 6;
        final int w = addr.getUpdateBase () ? 0x20 : 0;
        final int l = action << 4;

        emitByte((cond << 4) | 0x4 | bit25 | p);
        emitByte(s | b | w | l | (addr.getBaseReg ().getNumber() & 0xf));

        switch (addr.getType())
        {
            case ADDR_IMM:
            {
                emitByte((dst.getNumber() << 4) | (addr.getOffset() >>> 8));
                emitByte(addr.getOffset() & 0xff);
                break;
            }

            case ADDR_REG:
            {
                emitByte(dst.getNumber () << 4);
                emitByte(addr.getIndexReg ().getNumber () & 0xf);
                break;
            }

            case ADDR_SCALE:
            {
                final int scaleMode = (addr.getScaleMode () & 0x3) << 5;

                emitByte((dst.getNumber() << 4) | ((addr.getShift() >>> 1) & 0xf));
                emitByte(((addr.getShift() & 0x1) << 7) | scaleMode | (addr.getIndexReg().getNumber() & 0xf));
                break;
            }

            default: Assert.that(false, "should not reach here");
        }
    }

    /**
     * Emits a miscellaneous load/store instruction.
     *
     * @param cond condition code
     * @param l distinguishes between load (l == 1) and store (l == 0) instructions
     * @param s distinguishes between a signed (s == 1) and unsigned (s == 0) halfword access
     * @param h distinguishes between halfword (h == 1) and a signed byte (h == 0) access
     * @param reg destination register
     * @param addr addresing mode (see ADDR_xxx constants in {@link Constants} under Addressing Mode 3)
     */
    private void emitLoadStoreMisc(int cond, int l, int s, int h, Register reg, Address3 addr) {
        if (addr.getLabel() != null) {
            ALabel label = addr.getLabel();

            if (label.isBound()) {
                addr.resolveLabel(getBase());
            } else {
                label.addAddress3Instr(getOffset());
            }
        }

        final int bitU = (addr.getSign() ? 1 : 0) << 7;                // add or subtract index
        final int bit22 = (addr.getType() == ADDR_REG ? 0 : 1) << 6;   // immediate or register indexing
        final int bit21 = (addr.getUpdateBase() ? 1 : 0) << 5;         // pre or post indexing
        final int bitL = l << 4;
        final int bits8to11 = addr.getType() == ADDR_REG ? (SBZ.getNumber()) : ((addr.getOffset() >> 4) & 0xf);
        final int bitS = s << 6;
        final int bitH = h << 5;
        final int bits1to4 = addr.getType() == ADDR_REG ? (addr.getIndexReg().getNumber()) : (addr.getOffset() & 0xf);

        emitByte((cond << 4) | (addr.getPreIndexed() ? 1 : 0));
        emitByte(bitU | bit22 | bit21 | bitL | addr.getBaseReg().getNumber());
        emitByte((reg.getNumber() << 4) | bits8to11);
        emitByte(0x90 | bitS | bitH | bits1to4);
    }

    /**
     * Emits a move to status register from ARM register instruction.
     *
     * @param cond condition code
     * @param addrMode specifies if immediate (addrMode == 1) or register (addrMode == 0) addressing is used
     * @param psr specifies the destination PSR register (CPSR or SPSR)
     * @param fields field mask for the destination PSR register
     * @param rotate number of bits to rotate for immediate addressing or SBZ for register addressing
     * @param imm 8-bit immediate value for immediate addressing or source register number for register addressing
     */
    private void emitMsr(int cond, int addrMode, int psr, int fields, int rotate, int imm) {
        emitByte((cond << 4) | (addrMode << 1) | 1);
        emitByte((psr << 6) | 0x20 | fields);
        emitByte((SBO.getNumber() << 4) | (rotate & 0xf));
        emitByte(imm);
    }

    /**
     * Emits a multiply instruction.
     *
     * @param cond condition flag
     * @param s determines whether the CPSR should be updated by this instruction
     * @param dst destination register
     * @param op1 first source operand register
     * @param op2 second source operand register
     */
    private void emitMul(int cond, boolean s, Register dst, Register op1, Register op2) {
        Assert.that(dst != PC && op1 != PC && op2 != PC, "register " + PC + " not allowed here");

        emitByte(cond << 4);
        emitByte((s ? 0x10 : 0x0) | dst.getNumber());
        emitByte((SBZ.getNumber() << 4) | op2.getNumber());
        emitByte(0x90 | op1.getNumber());
    }

    /**
     * Emits a multiply long instruction.
     *
     * @param cond condition code
     * @param sign distinguishes between signed (sign == 1) and unsigned (s == 0) instructions
     * @param acc specifies if the result of the multiplication should be accumulated (acc == 1)
     * @param updateFlags specifies if the status register should be updated
     * @param dstLo destination register for the low word
     * @param dstHi destination register for the high word
     * @param op1 first source operand register
     * @param op2 second source operand register
     */
    private void emitMulLong(int cond, int sign, int acc, boolean updateFlags, Register dstLo, Register dstHi, Register op1, Register op2) {
        emitByte(cond << 4);
        emitByte(0x80 | (sign << 6) | (acc << 5) | ((updateFlags ? 1 : 0) << 4) | dstHi.getNumber());
        emitByte((dstLo.getNumber() << 4) | op2.getNumber());
        emitByte(0x90 | op1.getNumber());
    }

    /**
     * Emits a load/store multiple instruction.
     *
     * @param cond condition code
     * @param l distinguishes between load (l == 1) and store (l == 0) instructions
     * @param addr addresing mode (see ADDR_xxx constants in {@link Constants} under Addressing Mode 4)
     * @param s specifies if the CPSR is loaded from the SPSR (for LDMs that load the PC) or
     * that user mode banked registers are transferred instead of the register of the current
     * mode (for LDMs that do not load the PC and all STMs)
     * @param base base register used by the addressing mode
     * @param regs list of registers to be loaded or stored
     */
    private void emitLoadStoreMultiple(int cond, int l, int s, Address4 addr) {
        emitByte((cond << 4) | 0x8 | addr.getPBit(l));
        emitByte((addr.getUBit(l) << 7) | (s << 6) | (addr.getUpdateBaseBit() << 5) | (l << 4) | addr.getBaseReg().getNumber());
        emitByte(addr.getRegsBits() >>> 8);
        emitByte(addr.getRegsBits() & 0xff);
    }

    /**
     * Adds the value of the src register and carry flag to the op2 operand and stores the result
     * in the dst register.
     * <p>
     * Assembly example: <pre>    adc r0, r1, r2</pre>
     * Java equivalent:  <pre>    asm.adc(asm.R0, asm.R1, Operand2.reg(asm.R2));</pre>
     *
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    public void adc(Register dst, Register src, Operand2 op2) {
        emitDataProc(COND_AL, OPCODE_ADC, false, dst, src, op2);
    }

    /**
     * Similar to {@link #adc(Register, Register, Operand2)} except this instruction also updates
     * the CPSR.
     * <p>
     * Assembly example:        <pre>    adcs r0, r1, r2</pre>
     * ARMAssembler equivalent: <pre>    asm.adcs(asm.R0, asm.R1, Operand2.reg(asm.R2));</pre>
     *
     */
    public void adcs(Register dst, Register src, Operand2 op2) {
        emitDataProc(COND_AL, OPCODE_ADC, true, dst, src, op2);
    }

    /**
     * Similar to {@link #adc(Register, Register, Operand2)} except this instruction is only
     * executed when the condition is satisfied.
     * <p>
     * Assembly example:        <pre>    adceq r0, r1, r2</pre>
     * ARMAssembler equivalent: <pre>    asm.adccond(asm.COND_EQ, asm.R0, asm.R1, Operand2.reg(asm.R2));</pre>
     *
     * @param cond condition under which the instruction is executed
     */
    public void adccond(int cond, Register dst, Register src, Operand2 op2) {
        emitDataProc(cond, OPCODE_ADC, false, dst, src, op2);
    }

    /**
     * Similar to {@link #adc(Register, Register, Operand2)} except this instruction also updates
     * the CPSR and is only executed when the condition is satisfied.
     * <p>
     * Assembly example:        <pre>    adceqs r0, r1, r2</pre>
     * ARMAssembler equivalent: <pre>    asm.adcconds(asm.COND_EQ, asm.R0, asm.R1, Operand2.reg(asm.R2));</pre>
     *
     * @param cond condition under which the instruction is executed
     */
    public void adcconds(int cond, Register dst, Register src, Operand2 op2) {
        emitDataProc(cond, OPCODE_ADC, true, dst, src, op2);
    }

    /**
     * Adds the value of the src register and the op2 operand and stores the result in the dst
     * register.
     * <p>
     * Assembly example:        <pre>    add r0, r1, r2, lsl r3</pre>
     * ARMAssembler equivalent: <pre>    asm.add(asm.R0, asm.R1, asm.R2, Operand2.lsl(asm.R3));</pre>
     *
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    public void add(Register dst, Register src, Operand2 op2) {
        emitDataProc(COND_AL, OPCODE_ADD, false, dst, src, op2);
    }

    /**
     * Similar to {@link #add(Register, Register, Operand2)} except this instruction is executed
     * only when the condition is satisfied.
     * <p>
     * Assembly example:        <pre>    addeq r0, r1, r2, lsl r3</pre>
     * ARMAssembler equivalent: <pre>    asm.addcond(asm.COND_EQ, asm.R0, asm.R1, asm.R2, Operand2.lsl(asm.R3));</pre>
     *
     * @param cond condition under which this instruction is executed
     */
    public void addcond(int cond, Register dst, Register src, Operand2 op2) {
        emitDataProc(cond, OPCODE_ADD, false, dst, src, op2);
    }

    /**
     * Similar to {@link #add(Register, Register, Operand2)} but this instruction updates the CPSR
     * and is executed only when the condition is satisfied.
     * <p>
     * Assembly example:        <pre>    addlts r0, r1, r2, lsl r3</pre>
     * ARMAssembler equivalent: <pre>    asm.addconds(asm.COND_LT, asm.R0, asm.R1, asm.R2, Operand2.lsl(asm.R3));</pre>
     *
     * @param cond condition under which this instruction is executed
     */
    public void addconds(int cond, Register dst, Register src, Operand2 op2) {
        emitDataProc(cond, OPCODE_ADD, true, dst, src, op2);
    }

    /**
     * Similar to {@link #add(Register, Register, Operand2)} except this instruction also updates
     * the CPSR.
     * <p>
     * Assembly example:        <pre>    adds r0, r1, r2, lsl r3</pre>
     * ARMAssembler equivalent: <pre>    asm.adds(asm.R0, asm.R1, asm.R2, Operand2.lsl(asm.R3));</pre>
     *
     */
    public void adds(Register dst, Register src, Operand2 op2) {
        emitDataProc(COND_AL, OPCODE_ADD, true, dst, src, op2);
    }

    /**
     * Performs a bitwise AND of the value in the src register with the value of op2 and stores
     * the result in the dst register.
     * <p>
     * Assembly example:        <pre>    and r15, r0, r13, asr #17</pre>
     * ARMAssembler equivalent: <pre>    asm.and(asm.R15, asm.R0, Operand2.asr(asm.R13, 17));</pre>
     *
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    public void and(Register dst, Register src, Operand2 op2) {
        emitDataProc(COND_AL, OPCODE_AND, false, dst, src, op2);
    }

    /**
     * Similar to {@link #and(Register, Register, Operand2)} except this instruction is executed
     * only when the condition is satisfied.
     * <p>
     * Assembly example:        <pre>    andhi r15, r0, r13, asr #17</pre>
     * ARMAssembler equivalent: <pre>    asm.andcond(asm.COND_HI, asm.R15, asm.R0, Operand2.asr(asm.R13, 17));</pre>
     *
     * @param cond condition code
     */
    public void andcond(int cond, Register dst, Register src, Operand2 op2) {
        emitDataProc(cond, OPCODE_AND, false, dst, src, op2);
    }

    /**
     * Similar to {@link #and(Register, Register, Operand2)} except this instruction also updates
     * the CPSR and is executed only when the condition is satisfied.
     * <p>
     * Assembly example:        <pre>    andhis r15, r0, r13, asr #17</pre>
     * ARMAssembler equivalent: <pre>    asm.andconds(asm.COND_HI, asm.R15, asm.R0, Operand2.asr(asm.R13, 17));</pre>
     *
     * @param cond condition code
     */
    public void andconds(int cond, Register dst, Register src, Operand2 op2) {
        emitDataProc(cond, OPCODE_AND, true, dst, src, op2);
    }

    /**
     * Similar to {@link #and(Register, Register, Operand2)} except this instruction also updates
     * the CPSR.
     * <p>
     * Assembly example:        <pre>    ands r15, r0, r13, asr #17</pre>
     * ARMAssembler equivalent: <pre>    asm.ands(asm.R15, asm.R0, Operand2.asr(asm.R13, 17));</pre>
     *
     */
    public void ands(Register dst, Register src, Operand2 op2) {
        emitDataProc(COND_AL, OPCODE_AND, true, dst, src, op2);
    }

    /**
     * Jumps to the target label unconditionally.
     * <p>
     * Assembly example:        <pre>    b label</pre>
     * ARMAssembler equivalent: <pre>    asm.b(label);</pre>
     *
     * @param label destination operand label
     */
    public void b(ALabel label) {
        emitBranch(COND_AL, 0, label);
    }

    /**
     * Similar to {@link #b(ALabel)} except this instruction is executed only when the condition
     * is satisfied.
     * <p>
     * Assembly example:        <pre>    b label</pre>
     * ARMAssembler equivalent: <pre>    asm.b(label);</pre>
     *
     * @param cond condition code
     */
    public void bcond(int cond, ALabel label) {
        emitBranch(cond, 0, label);
    }

    /**
     * Jumps to the target label and optionally saves the return address in LR when the condition
     * is satisfied.
     * <p>
     * Assembly example:        <pre>    bllt label</pre>
     * ARMAssembler equivalent: <pre>    asm.bcond(asm.COND_LT, 1, label);</pre>
     *
     * @param cond condition code
     * @param l specifies whether the return address should be saved (1) or not (0)
     * @param label destination operand label
     */
    public void bcond(int cond, int l, ALabel label) {
        Assert.that(l == 0 || l == 1, "invalid l value");
        emitBranch(cond, l, label);
    }

    /**
     * Performs a bitwise AND of the value in the src register with the complement of the value of
     * op2 and stores the result in the dst register.
     * <p>
     * Assembly example:        <pre>    bic r15, r0, r13, asr #17</pre>
     * ARMAssembler equivalent: <pre>    asm.bic(asm.R15, asm.R0, Operand2.asr(asm.R13, 17));</pre>
     *
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    public void bic(Register dst, Register src, Operand2 op2) {
        emitDataProc(COND_AL, OPCODE_BIC, false, dst, src, op2);
    }

    /**
     * Similar to {@link #bic(Register, Register, Operand2)} except this instruction is executed
     * only when the condition is satisfied.
     * <p>
     * Assembly example:        <pre>    biceq r1, r2, r3</pre>
     * ARMAssembler equivalent: <pre>    asm.biccond(asm.COND_EQ, asm.R1, asm.R2, Operand2.reg(asm.R3));</pre>
     *
     * @param cond condition code
     */
    public void biccond(int cond, Register dst, Register src, Operand2 op2) {
        emitDataProc(cond, OPCODE_BIC, false, dst, src, op2);
    }

    /**
     * Similar to {@link #bic(Register, Register, Operand2)} except this instruction also updates
     * the CPSR and is executed only when the condition is satisfied.
     * <p>
     * Assembly example:        <pre>    biceqs r1, r2, r3</pre>
     * ARMAssembler equivalent: <pre>    asm.bicconds(asm.COND_EQ, asm.R1, asm.R2, Operand2.reg(asm.R3));</pre>
     *
     * @param cond condition code
     */
    public void bicconds(int cond, Register dst, Register src, Operand2 op2) {
        emitDataProc(cond, OPCODE_BIC, true, dst, src, op2);
    }

    /**
     * Similar to {@link #bic(Register, Register, Operand2)} except this instruction also updates
     * the CPSR.
     * <p>
     * Assembly example:        <pre>    bics r1, r2, r3</pre>
     * ARMAssembler equivalent: <pre>    asm.bics(asm.R1, asm.R2, Operand2.reg(asm.R3));</pre>
     *
     */
    public void bics(Register dst, Register src, Operand2 op2) {
        emitDataProc(COND_AL, OPCODE_BIC, true, dst, src, op2);
    }

    /**
     * Causes a software breakpoint to occur.
     * <p>
     * Assembly example:        <pre>    bkpt 12345</pre>
     * ARMAssembler equivalent: <pre>    asm.bkpt(12345);</pre>
     *
     * @param imm16 16-bit immediate containing information about the breakpoint for use by the debugger
     */
    public void bkpt(int imm16) {
        emitByte(0xe1);
        emitByte(0x20 | (imm16 >>> 12));
        emitByte(imm16 >>> 4);
        emitByte(0x70 | imm16 & 0xf);
    }

    /**
     * Similar to {@link #b(ALabel)} except this instruction also saves the return address to LR.
     * <p>
     * Assembly example:        <pre>    bl label</pre>
     * ARMAssembler equivalent: <pre>    asm.bl(label);</pre>
     *
     */
    public void bl(ALabel label) {
        emitBranch(COND_AL, 1, label);
    }

    /**
     * Similar to {@link #b(ALabel)} except this instruction also saves the return address to LR
     * and is executed only when the condition is satisfied.
     * <p>
     * Assembly example:        <pre>    bleq label</pre>
     * ARMAssembler equivalent: <pre>    asm.blcond(asm.COND_EQ, label);</pre>
     *
     * @param cond condition code
     */
    public void blcond(int cond, ALabel label) {
        emitBranch(cond, 1, label);
    }

    /**
     * Counts the number of binary zero bits before the first binary one bit in a register value.
     * The source register is scanned from the most significant bit (bit 31) to the least
     * significant bit (bit 0). The result value is 32 if no bits are set and 0 if bit 31 is set.
     * <p>
     * Assembly example:        <pre>    clz r3, r10</pre>
     * ARMAssembler equivalent: <pre>    asm.clz(asm.R3, asm.R10);</pre>
     *
     * @param dst destination register where the result value will be stored
     * @param src source register containing the value to be scanned
     */
    public void clz(Register dst, Register src) {
        clzcond(COND_AL, dst, src);
    }

    /**
     * Similar to {@link #clz(Register, Register)} except this instruction is only executed when
     * the condition is satisfied.
     * <p>
     * Assembly example:        <pre>    clzgt r3, r10</pre>
     * ARMAssembler equivalent: <pre>    asm.clzcond(asm.COND_GT, asm.R3, asm.R10);</pre>
     *
     * @param cond condition code
     */
    public void clzcond(int cond, Register dst, Register src) {
        Assert.that(dst != R15 && src != R15, R15 + " cannot be specified as one of the registers");

        emitByte((cond << 4) | 0x1);
        emitByte(0x60 | SBO.getNumber());
        emitByte((dst.getNumber() << 4) | SBO.getNumber());
        emitByte(0x10 | src.getNumber());
    }

    /**
     * Compares a register value with another arithmetic value by addition and updates the CPSR.
     * <p>
     * Assembly example:        <pre>    cmn r13, #0x254</pre>
     * ARMAssembler equivalent: <pre>    asm.cmn(asm.R13, Operand2.imm(0x254));</pre>
     *
     * @param op1 first operand register
     * @param op2 flexible second operand
     */
    public void cmn(Register op1, Operand2 op2) {
        cmncond(COND_AL, op1, op2);
    }

    /**
     * Similar to {@link #cmn(Register, Operand2)} except this instruction is executed only when
     * the condition is satisfied.
     * <p>
     * Assembly example:        <pre>    cmncc r13, #0x254</pre>
     * ARMAssembler equivalent: <pre>    asm.cmncond(asm.COND_CC, asm.R13, Operand2.imm(0x254));</pre>
     *
     * @param cond condition code
     */
    public void cmncond(int cond, Register op1, Operand2 op2) {
        emitDataProc(cond, OPCODE_CMN, true, Register.SBZ, op1, op2);
    }

    /**
     * Compares a register value with another arithmetic value by subtraction and updates the CPSR.
     * <p>
     * Assembly example:        <pre>    cmp r15, r13, asr #17</pre>
     * ARMAssembler equivalent: <pre>    asm.cmp(asm.R15, Operand2.asr(asm.R13, 17));</pre>
     *
     * @param op1 first operand register
     * @param op2 flexible second operand
     */
    public void cmp(Register op1, Operand2 op2) {
        emitDataProc(COND_AL, OPCODE_CMP, true, Register.SBZ, op1, op2);
    }

    /**
     * Similar to {@link #cmp(Register, Operand2)} except this instruction is executed only when
     * the condition is satisfied.
     * <p>
     * Assembly example:        <pre>    cmp r15, r13, asr #17</pre>
     * ARMAssembler equivalent: <pre>    asm.cmp(asm.R15, Operand2.asr(asm.R13, 17));</pre>
     *
     * @param cond condition code
     */
    public void cmpcond(int cond, Register op1, Operand2 op2) {
        emitDataProc(cond, OPCODE_CMP, true, Register.SBZ, op1, op2);
    }

    /**
     * Performs a bitwise XOR of the value in the src register with the value of op2 and stores
     * the result in the dst register.
     * <p>
     * Assembly example:        <pre>    eor r1, r2, r3</pre>
     * ARMAssembler equivalent: <pre>    asm.eor(asm.R1, asm.R2, Operand2.reg(asm.R3));</pre>
     *
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    public void eor(Register dst, Register src, Operand2 op2) {
        emitDataProc(COND_AL, OPCODE_EOR, false, dst, src, op2);
    }

    /**
     * Similar to {@link #eor(Register, Register, Operand2)} except this instruction is executed
     * only when the condition is satisfied.
     * <p>
     * Assembly example:        <pre>    eoreq r1, r2, r3</pre>
     * ARMAssembler equivalent: <pre>    asm.eorcond(asm.COND_EQ, asm.R1, asm.R2, Operand2.reg(asm.R3));</pre>
     *
     * @param cond condition code
     */
    public void eorcond(int cond, Register dst, Register src, Operand2 op2) {
        emitDataProc(cond, OPCODE_EOR, false, dst, src, op2);
    }

    /**
     * Similar to {@link #eor(Register, Register, Operand2)} except this instruction also updates
     * the CPSR and is executed only when the condition is satisfied.
     * <p>
     * Assembly example:        <pre>    eoreqs r1, r2, r3</pre>
     * ARMAssembler equivalent: <pre>    asm.eorconds(asm.COND_EQ, asm.R1, asm.R2, Operand2.reg(asm.R3));</pre>
     *
     * @param cond condition code
     */
    public void eorconds(int cond, Register dst, Register src, Operand2 op2) {
        emitDataProc(cond, OPCODE_EOR, true, dst, src, op2);
    }

    /**
     * Similar to {@link #eor(Register, Register, Operand2)} except this instruction also updates
     * the CPSR.
     * <p>
     * Assembly example:        <pre>    eors r1, r2, r3</pre>
     * ARMAssembler equivalent: <pre>    asm.eors(asm.R1, asm.R2, Operand2.reg(asm.R3));</pre>
     *
     */
    public void eors(Register dst, Register src, Operand2 op2) {
        emitDataProc(COND_AL, OPCODE_EOR, true, dst, src, op2);
    }

    /**
     * Loads a non-empty subset (or all) of the general-purpose registers from sequential memory
     * locations.
     * <p>
     * Assembly example:        <pre>    ldmda r3, {r0-r2}</pre>
     * ARMAssembler equivalent: <pre>    asm.ldm(new Address4(asm.ADDR_DA, asm.R3, new RegRange [] { new RegRange(asm.R0, asm.R2) }));</pre>
     *
     * @param addr address specifying where and how the registers are to be loaded
     */
    public void ldm(Address4 addr) {
        emitLoadStoreMultiple(COND_AL, 1, 0, addr);
    }

    /**
     * Similar to {@link #ldm(Address4)} except this instruction is executed only when the condition
     * is satisfied.
     * <p>
     * Assembly example:        <pre>    ldmdaeq r3, {r0-r2}</pre>
     * ARMAssembler equivalent: <pre>    asm.ldmcond(asm.COND_EQ, new Address4(asm.ADDR_DA, asm.R3, new RegRange [] { new RegRange(asm.R0, asm.R2) }));</pre>
     *
     * @param cond condition code
     */
    public void ldmcond(int cond, Address4 addr) {
        emitLoadStoreMultiple(cond, 1, 0, addr);
    }

    /**
     * Loads a word from the specified memory address and writes it to the destination register.
     * <p>
     * Assembly example:        <pre>    ldr r3, [r2, -r12, ror #25]</pre>
     * ARMAssembler equivalent: <pre>    asm.ldr(asm.R3, Address2.pre(asm.R2, -1, asm.R12, asm.ROR, 25));</pre>
     *
     * @param dst destination register
     * @param addr memory address
     */
    public void ldr(Register dst, Address2 addr) {
        ldrcond(COND_AL, dst, addr);
    }

    /**
     * Loads a byte from the specified memory address and writes the value zero-extended to the
     * destination register.
     * <p>
     * Assembly example:        <pre>    ldrb r3, [r2, -r12, ror #25]</pre>
     * ARMAssembler equivalent: <pre>    asm.ldrb(asm.R3, Address2.pre(asm.R2, -1, asm.R12, asm.ROR, 25));</pre>
     *
     * @param dst destination register
     * @param addr memory address
     */
    public void ldrb(Register dst, Address2 addr) {
        ldrcondb(COND_AL, dst, addr);
    }

    /**
     * Similar to {@link #ldr(Register, Address2)} except this instruction is executed only when
     * the condition is met.
     * <p>
     * Assembly example:        <pre>    ldreq r3, [r2, -r12, ror #25]</pre>
     * ARMAssembler equivalent: <pre>    asm.ldrcond(asm.COND_EQ, asm.R3, Address2.pre(asm.R2, -1, asm.R12, asm.ROR, 25));</pre>
     *
     * @param cond condition under which this instruction is executed
     */
    public void ldrcond(int cond, Register dst, Address2 addr) {
        emitLoadStore(cond, 0, 1, dst, addr);
    }

    /**
     * Similar to {@link #ldrb(Register, Address2)} except this instruction is executed only when
     * the condition is met.
     * <p>
     * Assembly example:        <pre>    ldreqb r3, [r2, -r12, ror #25]</pre>
     * ARMAssembler equivalent: <pre>    asm.ldrcondb(asm.COND_EQ, asm.R3, Address2.pre(asm.R2, -1, asm.R12, asm.ROR, 25));</pre>
     *
     * @param cond condition under which this instruction is executed
     */
    public void ldrcondb(int cond, Register dst, Address2 addr) {
        emitLoadStore(cond, 1, 1, dst, addr);
    }

    /**
     * Loads a halfword from the specified memory address and writes the value zero-extended to
     * the destination register.
     * <p>
     * Assembly example:        <pre>    ldrh r13, [r7, #-123]</pre>
     * ARMAssembler equivalent: <pre>    asm.ldrh(asm.R13, Address3.pre(asm.R7, -123));</pre>
     *
     * @param dst destination register
     * @param addr memory address
     */
    public void ldrh(Register dst, Address3 addr) {
        emitLoadStoreMisc(COND_AL, 1, 0, 1, dst, addr);
    }

    /**
     * Similar to {@link #ldrh(Register, Address3)} except this instruction is executed only when
     * the condition is satisfied.
     * <p>
     * Assembly example:        <pre>    ldreqh r13, [r7, #-123]</pre>
     * ARMAssembler equivalent: <pre>    asm.ldrcondh(asm.COND_EQ, asm.R13, Address3.pre(asm.R7, -123));</pre>
     *
     * @param cond condition code
     */
    public void ldrcondh(int cond, Register dst, Address3 addr) {
        emitLoadStoreMisc(cond, 1, 0, 1, dst, addr);
    }

    /**
     * Similar to {@link #ldrb(Register, Address2)} except this instruction sign-extends the byte
     * value.
     * <p>
     * Assembly example:        <pre>    ldrsb r3, [r2, -r12, ror #25]</pre>
     * ARMAssembler equivalent: <pre>    asm.ldrsb(asm.R3, Address2.pre(asm.R2, -1, asm.R12, asm.ROR, 25));</pre>
     *
     */
    public void ldrsb(Register dst, Address3 addr) {
        emitLoadStoreMisc(COND_AL, 1, 1, 0, dst, addr);
    }

    /**
     * Similar to {@link #ldrcondb(int, Register, Address2)} except this instruction sign-extends
     * the byte value.
     * <p>
     * Assembly example:        <pre>    ldreqsb r3, [r2, -r12, ror #25]</pre>
     * ARMAssembler equivalent: <pre>    asm.ldrcondsb(asm.COND_EQ, asm.R3, Address2.pre(asm.R2, -1, asm.R12, asm.ROR, 25));</pre>
     *
     */
    public void ldrcondsb(int cond, Register dst, Address3 addr) {
        emitLoadStoreMisc(cond, 1, 1, 0, dst, addr);
    }

    /**
     * Similar to {@link #ldrh(Register, Address3)} except this instruction sign-extends the
     * halfword value.
     * <p>
     * Assembly example:        <pre>    ldrsh r13, [r7, #-123]</pre>
     * ARMAssembler equivalent: <pre>    asm.ldrsh(asm.R13, Address3.pre(asm.R7, -123));</pre>
     *
     */
    public void ldrsh(Register dst, Address3 addr) {
        emitLoadStoreMisc(COND_AL, 1, 1, 1, dst, addr);
    }

    /**
     * Similar to {@link #ldrcondh(int, Register, Address3)} except this instruction sign-extends
     * the halfword value.
     * <p>
     * Assembly example:        <pre>    ldreqsh r13, [r7, #-123]</pre>
     * ARMAssembler equivalent: <pre>    asm.ldrcondsh(asm.COND_EQ, asm.R13, Address3.pre(asm.R7, -123));</pre>
     *
     */
    public void ldrcondsh(int cond, Register dst, Address3 addr) {
        emitLoadStoreMisc(cond, 1, 1, 1, dst, addr);
    }

    /**
     * Multiplies the values in op1 and op2, adds op3 to the result and stores the final result
     * into dst.
     * <p>
     * Assembly example:        <pre>    mla r14, r0, r7, r13</pre>
     * ARMAssembler equivalent: <pre>    asm.mla(asm.R14, asm.R0, asm.R7, asm.R13);</pre>
     *
     * @param dst destination register
     * @param op1 source register multiplied with op2
     * @param op2 source register multiplied with op1
     * @param op3 source register added to op1 * op2
     */
    public void mla(Register dst, Register op1, Register op2, Register op3) {
        mla(COND_AL, false, dst, op1, op2, op3);
    }

    /**
     * Similar to {@link #mla(Register, Register, Register, Register)} except this instruction is
     * executed only when the condition is satisfied.
     * <p>
     * Assembly example:        <pre>    mlaeq r14, r0, r7, r13</pre>
     * ARMAssembler equivalent: <pre>    asm.mlacond(asm.COND_EQ, asm.R14, asm.R0, asm.R7, asm.R13);</pre>
     *
     * @param cond condition code
     */
    public void mlacond(int cond, Register dst, Register op1, Register op2, Register op3) {
        mla(cond, false, dst, op1, op2, op3);
    }

    /**
     * Similar to {@link #mla(Register, Register, Register, Register)} except this instruction
     * also updates the CPSR and is executed only when the condition is satisfied.
     * <p>
     * Assembly example:        <pre>    mlaeqs r14, r0, r7, r13</pre>
     * ARMAssembler equivalent: <pre>    asm.mlaconds(asm.COND_EQ, asm.R14, asm.R0, asm.R7, asm.R13);</pre>
     *
     * @param cond condition code
     */
    public void mlaconds(int cond, Register dst, Register op1, Register op2, Register op3) {
        mla(cond, true, dst, op1, op2, op3);
    }

    /**
     * Similar to {@link #mla(Register, Register, Register, Register)} except this instruction
     * also updates the CPSR.
     * <p>
     * Assembly example:        <pre>    mlas r14, r0, r7, r13</pre>
     * ARMAssembler equivalent: <pre>    asm.mlas(asm.R14, asm.R0, asm.R7, asm.R13);</pre>
     *
     */
    public void mlas(Register dst, Register op1, Register op2, Register op3) {
        mla(COND_AL, true, dst, op1, op2, op3);
    }

    /**
     * Emits an mla instruction.
     *
     * @param cond condition code
     * @param s determines whether the CPSR should be updated by this instruction
     * @param dst destination register
     * @param op1 first source operand (multiplied with op2)
     * @param op2 second source operand (multiplied with op1)
     * @param op3 third source operand (added to op1 * op2)
     */
    private void mla(int cond, boolean s, Register dst, Register op1, Register op2, Register op3) {
        Assert.that(dst != PC && op1 != PC && op2 != PC && op3 != PC, "register " + PC + " not allowed here");

        emitByte(cond << 4);
        emitByte(0x20 | (s ? 0x10 : 0x0) | dst.getNumber());
        emitByte((op3.getNumber() << 4) | op2.getNumber());
        emitByte(0x90 | op1.getNumber());
    }

    /**
     * Copies the value of the source operand to the destination operand.
     * <p>
     * Assembly example:        <pre>    mov r8, #0xb50</pre>
     * ARMAssembler equivalent: <pre>    asm.mov(asm.R8, Operand2.imm(0xb50));</pre>
     *
     * @param dst destination register
     * @param op2 flexible source operand
     */
    public void mov(Register dst, Operand2 op2) {
        emitDataProc(COND_AL, OPCODE_MOV, false, dst, Register.SBZ, op2);
    }

    /**
     * Similar to {@link #mov(Register, Operand2)} except this instruction also updates the CPSR.
     * <p>
     * Assembly example:        <pre>    movs r8, #0xb50</pre>
     * ARMAssembler equivalent: <pre>    asm.movs(asm.R8, Operand2.imm(0xb50));</pre>
     *
     */
    public void movs(Register dst, Operand2 op2) {
        emitDataProc(COND_AL, OPCODE_MOV, true, dst, Register.SBZ, op2);
    }

    /**
     * Similar to {@link #mov(Register, Operand2)} except this instruction is executed only when
     * the condition is satisfied.
     * <p>
     * Assembly example:        <pre>    moveq r8, #0xb50</pre>
     * ARMAssembler equivalent: <pre>    asm.movcond(asm.COND_EQ, asm.R8, Operand2.imm(0xb50));</pre>
     *
     * @param cond condition flag
     */
    public void movcond(int cond, Register dst, Operand2 op2) {
        emitDataProc(cond, OPCODE_MOV, false, dst, Register.SBZ, op2);
    }

    /**
     * Similar to {@link #mov(Register, Operand2)} except this instruction also updates the CPSR
     * and is executed only when the condition is satisfied.
     * <p>
     * Assembly example:        <pre>    moveqs r8, #0xb50</pre>
     * ARMAssembler equivalent: <pre>    asm.movconds(asm.COND_EQ, asm.R8, Operand2.imm(0xb50));</pre>
     *

     * @param cond condition flag
     */
    public void movconds(int cond, Register dst, Operand2 op2) {
        emitDataProc(cond, OPCODE_MOV, true, dst, Register.SBZ, op2);
    }

    /**
     * Moves the value of the CPSR or the SPSR into a general purpose register.
     * <p>
     * Assembly example:        <pre>    mrs r14, spsr</pre>
     * ARMAssembler equivalent: <pre>    asm.mrs(asm.R14, asm.SPSR);</pre>
     *
     * @param dst general purpose destination register
     * @param psr either CPSR or SPSR (refer to {@link Constants})
     */
    public void mrs(Register dst, int psr) {
        mrscond(COND_AL, dst, psr);
    }

    /**
     * Similar to {@link #mrs(Register, int)} except this instruction is executed only when the
     * condition is satisfied.
     * <p>
     * Assembly example:        <pre>    mrseq r14, spsr</pre>
     * ARMAssembler equivalent: <pre>    asm.mrscond(asm.COND_EQ, asm.R14, asm.SPSR);</pre>
     *
     * @param cond condition code
     */
    public void mrscond(int cond, Register dst, int psr) {
        Assert.that(psr == CPSR || psr == SPSR, "psr must be CPSR or SPSR");

        emitByte((cond << 4) | 0x1);
        emitByte((psr << 6) | SBO.getNumber());
        emitByte((dst.getNumber() << 4) | SBZ.getNumber());
        emitByte((SBZ.getNumber() << 4) | SBZ.getNumber());
    }

    /**
     * Transfers an immediate constant to the CPSR or SPSR.
     * <p>
     * Assembly example:        <pre>    msr cpsr_cx, #12992</pre>
     * ARMAssembler equivalent: <pre>    asm.msr(asm.CPSR, asm.PSR_c | asm.PSR_x, 12992);</pre>
     *
     * @param psr either CPSR or SPSR (refer to {@link Constants})
     * @param fields combination of CPSR/SPSR field mask bits
     * @param imm immediate value that can be formed by rotating right an 8-bit number an even number of bits
     */
    public void msr(int psr, int fields, int imm) {
        msrcond(COND_AL, psr, fields, imm);
    }

    /**
     * Transfers the value of a general purpose register to the CPSR or SPSR.
     * <p>
     * Assembly example:        <pre>    msr cpsr_cx, r5</pre>
     * ARMAssembler equivalent: <pre>    asm.msr(asm.CPSR, asm.PSR_c | asm.PSR_x, asm.R5);</pre>
     *
     * @param psr either CPSR or SPSR (refer to {@link Constants})
     * @param fields combination of CPSR/SPSR field mask bits
     * @param src general purpose register whose value is to be transferred
     */
    public void msr(int psr, int fields, Register src) {
        msrcond(COND_AL, psr, fields, src);
    }

    /**
     * Similar to {@link #msr(int, int, int)} except this instruction is executed only when the
     * condition is satisfied.
     * <p>
     * Assembly example:        <pre>    msreq cpsr_cx, #12992</pre>
     * ARMAssembler equivalent: <pre>    asm.msrcond(asm.COND_EQ, asm.CPSR, asm.PSR_c | asm.PSR_x, 12992);</pre>
     *
     * @param cond condition code
     */
    public void msrcond(int cond, int psr, int fields, int imm) {
        emitMsr(cond, 1, psr, fields, AsmHelpers.getPackedImmRot(imm), AsmHelpers.getPackedImm8(imm));
    }

    /**
     * Similar to {@link #msr(int, int, Register)} except this instruction is executed only when
     * the condition is satisfied.
     * <p>
     * Assembly example:        <pre>    msreq cpsr_cx, r5</pre>
     * ARMAssembler equivalent: <pre>    asm.msrcond(asm.COND_EQ, asm.CPSR, asm.PSR_c | asm.PSR_x, asm.R5);</pre>
     *
     * @param cond condition code
     */
    public void msrcond(int cond, int psr, int fields, Register src) {
        emitMsr(cond, 0, psr, fields, SBZ.getNumber(), src.getNumber());
    }

    /**
     * Multiplies the values in two registers together and stores the result in another register.
     * This instruction produces only the lower 32 bits of the 64 bit product so the same answer
     * is given for multiplication of both signed and unsigned values.
     * <p>
     * Assembly example:        <pre>    mul r14, r0, r7</pre>
     * ARMAssembler equivalent: <pre>    asm.mul(asm.R14, asm.R0, asm.R7);</pre>
     *
     * @param dst destination register
     * @param op1 first source register
     * @param op2 second source register
     */
    public void mul(Register dst, Register op1, Register op2) {
        emitMul(COND_AL, false, dst, op1, op2);
    }

    /**
     * Similar to {@link #mul(Register, Register, Register)} except this instruction is executed
     * only when the condition is satisfied.
     * <p>
     * Assembly example:        <pre>    muleq r14, r0, r7</pre>
     * ARMAssembler equivalent: <pre>    asm.mulcond(asm.COND_EQ, asm.R14, asm.R0, asm.R7);</pre>
     *
     * @param cond condition code
     */
    public void mulcond(int cond, Register dst, Register op1, Register op2) {
        emitMul(cond, false, dst, op1, op2);
    }

    /**
     * Similar to {@link #mul(Register, Register, Register)} except this instruction also updates
     * the CPSR and is executed only when the condition is satisfied.
     * <p>
     * Assembly example:        <pre>    muleqs r14, r0, r7</pre>
     * ARMAssembler equivalent: <pre>    asm.mulconds(asm.COND_EQ, asm.R14, asm.R0, asm.R7);</pre>
     *
     * @param cond condition code
     */
    public void mulconds(int cond, Register dst, Register op1, Register op2) {
        emitMul(cond, true, dst, op1, op2);
    }

    /**
     * Similar to {@link #mul(Register, Register, Register)} except this instruction also updates
     * the CPSR.
     * <p>
     * Assembly example:        <pre>    muls r14, r0, r7</pre>
     * ARMAssembler equivalent: <pre>    asm.muls(asm.R14, asm.R0, asm.R7);</pre>
     *
     */
    public void muls(Register dst, Register op1, Register op2) {
        emitMul(COND_AL, true, dst, op1, op2);
    }

    /**
     * Copies the logical one's complement of the source operand to the destination register.
     * <p>
     * Assembly example:        <pre>    mvn r15, r13, asr #17</pre>
     * ARMAssembler equivalent: <pre>    asm.mvn(asm.R15, Operand2.asr(asm.R13, 17));</pre>
     *
     * @param dst destination register
     * @param op2 flexible source operand
     */
    public void mvn(Register dst, Operand2 op2) {
        emitDataProc(COND_AL, OPCODE_MVN, false, dst, Register.SBZ, op2);
    }

    /**
     * Similar to {@link #mvn(Register, Operand2)} except this instruction is executed only when
     * the condition is satisfied.
     * <p>
     * Assembly example:        <pre>    mvneq r15, r13, asr #17</pre>
     * ARMAssembler equivalent: <pre>    asm.mvncond(asm.COND_EQ, asm.R15, Operand2.asr(asm.R13, 17));</pre>
     *
     * @param cond condition flag
     */
    public void mvncond(int cond, Register dst, Operand2 op2) {
        emitDataProc(cond, OPCODE_MVN, false, dst, Register.SBZ, op2);
    }

    /**
     * Similar to {@link #mvn(Register, Operand2)} except this instruction also updates the CPSR
     * and is executed only when the condition is satisfied.
     * <p>
     * Assembly example:        <pre>    mvneq r15, r13, asr #17</pre>
     * ARMAssembler equivalent: <pre>    asm.mvncond(asm.COND_EQ, asm.R15, Operand2.asr(asm.R13, 17));</pre>
     *
     * @param cond condition flag
     */
    public void mvnconds(int cond, Register dst, Operand2 op2) {
        emitDataProc(cond, OPCODE_MVN, true, dst, Register.SBZ, op2);
    }

    /**
     * Similar to {@link #mvn(Register, Operand2)} except this instruction also updates the CPSR.
     * <p>
     * Assembly example:        <pre>    mvns r15, r13, asr #17</pre>
     * ARMAssembler equivalent: <pre>    asm.mvns(asm.R15, Operand2.asr(asm.R13, 17));</pre>
     *
     */
    public void mvns(Register dst, Operand2 op2) {
        emitDataProc(COND_AL, OPCODE_MVN, true, dst, Register.SBZ, op2);
    }

    /**
     * Performs no operation. It is implemented as <code>mov r0, r0</code>
     * <p>
     * Assembly example:        <pre>    nop</pre>
     * ARMAssembler equivalent: <pre>    asm.nop();</pre>
     *
     */
    public void nop() {
        mov(Register.R0, Operand2.reg(Register.R0));
    }

    /**
     * Performs a bitwise OR of the value in the src register with the value of op2 and stores the
     * result in the dst register.
     * <p>
     * Assembly example:        <pre>    orr r0, r1, r13, lsl #3</pre>
     * ARMAssembler equivalent: <pre>    asm.orr(asm.R0, asm.R1, Operand2.lsl(asm.R13, 3));</pre>
     *
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    public void orr(Register dst, Register src, Operand2 op2) {
        emitDataProc(COND_AL, OPCODE_ORR, false, dst, src, op2);
    }

    /**
     * Similar to {@link #orr(Register, Register, Operand2)} except this instruction is executed
     * only when the condition is satisfied.
     * <p>
     * Assembly example:        <pre>    orreq r0, r1, r13, lsl #3</pre>
     * ARMAssembler equivalent: <pre>    asm.orrcond(asm.COND_EQ, asm.R0, asm.R1, Operand2.lsl(asm.R13, 3));</pre>
     *
     * @param cond condition code
     */
    public void orrcond(int cond, Register dst, Register src, Operand2 op2) {
        emitDataProc(cond, OPCODE_ORR, false, dst, src, op2);
    }

    /**
     * Similar to {@link #orr(Register, Register, Operand2)} except this instruction also updates
     * the CPSR and is executed only when the condition is satisfied.
     * <p>
     * Assembly example:        <pre>    orreqs r0, r1, r13, lsl #3</pre>
     * ARMAssembler equivalent: <pre>    asm.orrconds(asm.COND_EQ, asm.R0, asm.R1, Operand2.lsl(asm.R13, 3));</pre>
     *
     * @param cond condition code
     */
    public void orrconds(int cond, Register dst, Register src, Operand2 op2) {
        emitDataProc(cond, OPCODE_ORR, true, dst, src, op2);
    }

    /**
     * Similar to {@link #orr(Register, Register, Operand2)} except this instruction also updates
     * the CPSR.
     * <p>
     * Assembly example:        <pre>    orrs r0, r1, r13, lsl #3</pre>
     * ARMAssembler equivalent: <pre>    asm.orrs(asm.R0, asm.R1, Operand2.lsl(asm.R13, 3));</pre>
     *
     */
    public void orrs(Register dst, Register src, Operand2 op2) {
        emitDataProc(COND_AL, OPCODE_ORR, true, dst, src, op2);
    }

    /**
     * Subtracts the value of the src register from the op2 operand and stores the result in the
     * dst register.
     * <p>
     * Assembly example:        <pre>    rsb r0, r1, r13, lsl #3</pre>
     * ARMAssembler equivalent: <pre>    asm.rsb(asm.R0, asm.R1, Operand2.lsl(asm.R13, 3));</pre>
     *
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    public void rsb(Register dst, Register src, Operand2 op2) {
        emitDataProc(COND_AL, OPCODE_RSB, false, dst, src, op2);
    }

    /**
     * Similar to {@link #rsb(Register, Register, Operand2)} except this instruction is executed
     * only when the condition is statisfied.
     * <p>
     * Assembly example:        <pre>    rsbeq r0, r1, r13, lsl #3</pre>
     * ARMAssembler equivalent: <pre>    asm.rsbcond(asm.COND_EQ, asm.R0, asm.R1, Operand2.lsl(asm.R13, 3));</pre>
     *
     * @param cond condition under which the instruction is executed
     */
    public void rsbcond(int cond, Register dst, Register src, Operand2 op2) {
        emitDataProc(cond, OPCODE_RSB, false, dst, src, op2);
    }

    /**
     * Similar to {@link #rsb(Register, Register, Operand2)} except this instruction also updates
     * the CPSR and is executed only when the condition is satisfied.
     * <p>
     * Assembly example:        <pre>    rsbeqs r0, r1, r13, lsl #3</pre>
     * ARMAssembler equivalent: <pre>    asm.rsbconds(asm.COND_EQ, asm.R0, asm.R1, Operand2.lsl(asm.R13, 3));</pre>
     *
     * @param cond condition under which the instruction is executed
     */
    public void rsbconds(int cond, Register dst, Register src, Operand2 op2) {
        emitDataProc(cond, OPCODE_RSB, true, dst, src, op2);
    }

    /**
     * Similar to {@link #rsb(Register, Register, Operand2)} except this instruction also updates
     * the CPSR.
     * <p>
     * Assembly example:        <pre>    rsbs r0, r1, r13, lsl #3</pre>
     * ARMAssembler equivalent: <pre>    asm.rsbs(asm.R0, asm.R1, Operand2.lsl(asm.R13, 3));</pre>
     *
     */
    public void rsbs(Register dst, Register src, Operand2 op2) {
        emitDataProc(COND_AL, OPCODE_RSB, true, dst, src, op2);
    }

    /**
     * Subtracts the value of the src register from the op2 operand with carry and stores the
     * result in the dst register.
     * <p>
     * Assembly example:        <pre>    rsc r0, r1, r13, lsl #3</pre>
     * ARMAssembler equivalent: <pre>    asm.rsc(asm.R0, asm.R1, Operand2.lsl(asm.R13, 3));</pre>
     *
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    public void rsc(Register dst, Register src, Operand2 op2) {
        emitDataProc(COND_AL, OPCODE_RSC, false, dst, src, op2);
    }

    /**
     * Similar to {@link #rsc(Register, Register, Operand2)} except this instruction is executed
     * only when the condition is satisfied.
     * <p>
     * Assembly example:        <pre>    rsceq r0, r1, r13, lsl #3</pre>
     * ARMAssembler equivalent: <pre>    asm.rsccond(asm.COND_EQ, asm.R0, asm.R1, Operand2.lsl(asm.R13, 3));</pre>
     *
     * @param cond condition under which the instruction is executed
     */
    public void rsccond(int cond, Register dst, Register src, Operand2 op2) {
        emitDataProc(cond, OPCODE_RSC, false, dst, src, op2);
    }

    /**
     * Similar to {@link #rsc(Register, Register, Operand2)} except this instruction also updates the
     * CPSR and is executed only when the condition is satisfied.
     *
     * <p>
     * Assembly example:        <pre>    rsceqs r0, r1, r13, lsl #3</pre>
     * ARMAssembler equivalent: <pre>    asm.rscconds(asm.COND_EQ, asm.R0, asm.R1, Operand2.lsl(asm.R13, 3));</pre>
     *
     * @param cond condition under which the instruction is executed
     */
    public void rscconds(int cond, Register dst, Register src, Operand2 op2) {
        emitDataProc(cond, OPCODE_RSC, true, dst, src, op2);
    }

    /**
     * Similar to {@link #rsc(Register, Register, Operand2)} except this instruction also updates
     * the CPSR.
     * <p>
     * Assembly example:        <pre>    rscs r0, r1, r13, lsl #3</pre>
     * ARMAssembler equivalent: <pre>    asm.rscs(asm.R0, asm.R1, Operand2.lsl(asm.R13, 3));</pre>
     *
     */
    public void rscs(Register dst, Register src, Operand2 op2) {
        emitDataProc(COND_AL, OPCODE_RSC, true, dst, src, op2);
    }

    /**
     * Subtracts the value of the op2 operand from the src register with carry and stores the
     * result in the dst register.
     * <p>
     * Assembly example:        <pre>    sbc r0, r1, r13, lsl #3</pre>
     * ARMAssembler equivalent: <pre>    asm.sbc(asm.R0, asm.R1, Operand2.lsl(asm.R13, 3));</pre>
     *
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    public void sbc(Register dst, Register src, Operand2 op2) {
        emitDataProc(COND_AL, OPCODE_SBC, false, dst, src, op2);
    }

    /**
     * Similar to {@link #sbc(Register, Register, Operand2)} except the instruction is executed
     * only when the condition is satisfied.
     * <p>
     * Assembly example:        <pre>    sbceq r0, r1, r13, lsl #3</pre>
     * ARMAssembler equivalent: <pre>    asm.sbccond(asm.COND_EQ, asm.R0, asm.R1, Operand2.lsl(asm.R13, 3));</pre>
     *
     * @param cond condition under which the instruction is executed
     */
    public void sbccond(int cond, Register dst, Register src, Operand2 op2) {
        emitDataProc(cond, OPCODE_SBC, false, dst, src, op2);
    }

    /**
     * Similar to {@link #sbc(Register, Register, Operand2)} except the instruction also updates
     * the CPSR and is executed only when the condition is satisfied.
     * <p>
     * Assembly example:        <pre>    sbceqs r0, r1, r13, lsl #3</pre>
     * ARMAssembler equivalent: <pre>    asm.sbcconds(asm.COND_EQ, asm.R0, asm.R1, Operand2.lsl(asm.R13, 3));</pre>
     *
     * @param cond condition under which the instruction is executed
     */
    public void sbcconds(int cond, Register dst, Register src, Operand2 op2) {
        emitDataProc(cond, OPCODE_SBC, true, dst, src, op2);
    }

    /**
     * Similar to {@link #sbc(Register, Register, Operand2)} except the instruction also updates
     * the CPSR.
     * <p>
     * Assembly example:        <pre>    sbcs r0, r1, r13, lsl #3</pre>
     * ARMAssembler equivalent: <pre>    asm.sbcs(asm.R0, asm.R1, Operand2.lsl(asm.R13, 3));</pre>
     *
     */
    public void sbcs(Register dst, Register src, Operand2 op2) {
        emitDataProc(COND_AL, OPCODE_SBC, true, dst, src, op2);
    }

    /**
     * Multiplies the signed values in the two source registers to produce a 64-bit product
     * which is then added to the 64-bit value held in the two destination registers. The result
     * is written back to the two destination registers.
     * <p>
     * Assembly example:        <pre>    smlal r0, r1, r2, r3</pre>
     * ARMAssembler equivalent: <pre>    asm.smlal(asm.R0, asm.R1, asm.R2, asm.R13);</pre>
     *
     * @param dstLo destination register containing the low word of the 64-bit value
     * @param dstHi destination register containing the high word of the 64-bit value
     * @param op1 first source operand register
     * @param op2 second source operand register
     */
    public void smlal(Register dstLo, Register dstHi, Register op1, Register op2) {
        emitMulLong(COND_AL, 1, 1, false, dstLo, dstHi, op1, op2);
    }

    /**
     * Similar to {@link #smlal(Register, Register, Register, Register)} except the instruction
     * is executed only when the condition is satisfied.
     * <p>
     * Assembly example:        <pre>    smlaleq r0, r1, r2, r3</pre>
     * ARMAssembler equivalent: <pre>    asm.smlalcond(asm.COND_EQ, asm.R0, asm.R1, asm.R2, asm.R13);</pre>
     *
     * @param cond condition code
     */
    public void smlalcond(int cond, Register dstLo, Register dstHi, Register op1, Register op2) {
        emitMulLong(cond, 1, 1, false, dstLo, dstHi, op1, op2);
    }

    /**
     * Similar to {@link #smlal(Register, Register, Register, Register)} except the instruction
     * also updates the CPSR and is executed only when the condition is satisfied.
     * <p>
     * Assembly example:        <pre>    smlaleqs r0, r1, r2, r3</pre>
     * ARMAssembler equivalent: <pre>    asm.smlalconds(asm.COND_EQ, asm.R0, asm.R1, asm.R2, asm.R13);</pre>
     *
     * @param cond condition code
     */
    public void smlalconds(int cond, Register dstLo, Register dstHi, Register op1, Register op2) {
        emitMulLong(cond, 1, 1, true, dstLo, dstHi, op1, op2);
    }

    /**
     * Similar to {@link #smlal(Register, Register, Register, Register)} except the instruction
     * also updates the CPSR.
     * <p>
     * Assembly example:        <pre>    smlals r0, r1, r2, r3</pre>
     * ARMAssembler equivalent: <pre>    asm.smlals(asm.R0, asm.R1, asm.R2, asm.R13);</pre>
     *
     */
    public void smlals(Register dstLo, Register dstHi, Register op1, Register op2) {
        emitMulLong(COND_AL, 1, 1, true, dstLo, dstHi, op1, op2);
    }

    /**
     * Multiplies two 32-bit signed register values and stores the 64-bit product in two
     * destination registers.
     * <p>
     * Assembly example:        <pre>    smull r0, r1, r2, r3</pre>
     * ARMAssembler equivalent: <pre>    asm.smull(asm.R0, asm.R1, asm.R2, asm.R3);</pre>
     *
     * @param dstLo destination register containing the low word of the 64-bit product
     * @param dstHi destination register containing the high word of the 64-bit product
     * @param op1 first source operand register
     * @param op2 second source operand register
     */
    public void smull(Register dstLo, Register dstHi, Register op1, Register op2) {
        Assert.that (dstLo != dstHi && dstHi != op1 && dstLo != op1, "dstLo, dstHi and op1 must all be different");
        emitMulLong(COND_AL, 1, 0, false, dstLo, dstHi, op1, op2);
    }

    /**
     * Similar to {@link #smull(Register, Register, Register, Register)} except the instruction
     * is executed only when the condition is satisfied.
     * <p>
     * Assembly example:        <pre>    smulleq r0, r1, r2, r3</pre>
     * ARMAssembler equivalent: <pre>    asm.smullcond(asm.COND_EQ, asm.R0, asm.R1, asm.R2, asm.R3);</pre>
     *
     * @param cond condition code
     */
    public void smullcond(int cond, Register dstLo, Register dstHi, Register op1, Register op2) {
        Assert.that (dstLo != dstHi && dstHi != op1 && dstLo != op1, "dstLo, dstHi and op1 must all be different");
        emitMulLong(cond, 1, 0, false, dstLo, dstHi, op1, op2);
    }

    /**
     * Similar to {@link #smull(Register, Register, Register, Register)} except the instruction
     * also updates the CPSR and is executed only when the condition is satisfied.
     * <p>
     * Assembly example:        <pre>    smulleqs r0, r1, r2, r3</pre>
     * ARMAssembler equivalent: <pre>    asm.smullconds(asm.COND_EQ, asm.R0, asm.R1, asm.R2, asm.R3);</pre>
     *
     * @param cond condition code
     */
    public void smullconds(int cond, Register dstLo, Register dstHi, Register op1, Register op2) {
        Assert.that (dstLo != dstHi && dstHi != op1 && dstLo != op1, "dstLo, dstHi and op1 must all be different");
        emitMulLong(cond, 1, 0, true, dstLo, dstHi, op1, op2);
    }

    /**
     * Similar to {@link #smull(Register, Register, Register, Register)} except the instruction
     * also updates the CPSR.
     * <p>
     * Assembly example:        <pre>    smulls r0, r1, r2, r3</pre>
     * ARMAssembler equivalent: <pre>    asm.smulls(asm.R0, asm.R1, asm.R2, asm.R3);</pre>
     *
     */
    public void smulls(Register dstLo, Register dstHi, Register op1, Register op2) {
        Assert.that (dstLo != dstHi && dstHi != op1 && dstLo != op1, "dstLo, dstHi and op1 must all be different");
        emitMulLong(COND_AL, 1, 0, true, dstLo, dstHi, op1, op2);
    }

    /**
     * Stores a non-empty subset (or all) of the general-purpose registers to sequential memory
     * locations.
     * <p>
     * Assembly example:        <pre>    stmda r3, {r0-r2}</pre>
     * ARMAssembler equivalent: <pre>    asm.stm(new Address4(asm.ADDR_DA, asm.R3, new RegRange [] { new RegRange(asm.R0, asm.R2) }));</pre>
     *
     * @param addr address specifying where and how the registers are to be loaded
     */
    public void stm(Address4 addr) {
        emitLoadStoreMultiple(COND_AL, 0, 0, addr);
    }

    /**
     * Similar to {@link #stm(Address4)} except this instruction is executed only when the condition
     * is satisfied.
     * <p>
     * Assembly example:        <pre>    stmeqda r3, {r0-r2}</pre>
     * ARMAssembler equivalent: <pre>    asm.stmcond(asm.COND_EQ, new Address4(asm.ADDR_DA, asm.R3, new RegRange [] { new RegRange(asm.R0, asm.R2) }));</pre>
     *
     * @param cond condition code
     */
    public void stmcond(int cond, Address4 addr) {
        emitLoadStoreMultiple(cond, 0, 0, addr);
    }

    /**
     * Stores a word from a register to the specified memory address.
     * <p>
     * Assembly example:        <pre>    str r6, [r1, r14, lsr #3]!</pre>
     * ARMAssembler equivalent: <pre>    asm.str(asm.R6, Address2.preW(asm.R1, 1, asm.R14, asm.LSR, 3));</pre>
     *
     * @param src source register
     * @param addr memory address
     */
    public void str(Register src, Address2 addr) {
        strcond(COND_AL, src, addr);
    }

    /**
     * Stores a byte from the least significant byte of a register to the specified memory address.
     * <p>
     * Assembly example:        <pre>    strb r6, [r1, r14, lsr #3]!</pre>
     * ARMAssembler equivalent: <pre>    asm.strb(asm.R6, Address2.preW(asm.R1, 1, asm.R14, asm.LSR, 3));</pre>
     *
     * @param src source register
     * @param addr memory address
     */
    public void strb(Register src, Address2 addr) {
        strcondb(COND_AL, src, addr);
    }

    /**
     * Similar to {@link #str(Register, Address2)} except that this instruction is executed only
     * when the condition is satisfied.
     * <p>
     * Assembly example:        <pre>    streq r6, [r1, r14, lsr #3]!</pre>
     * ARMAssembler equivalent: <pre>    asm.strcond(asm.COND_EQ, asm.R6, Address2.preW(asm.R1, 1, asm.R14, asm.LSR, 3));</pre>
     *
     * @param cond condition under which this instruction is executed
     */
    public void strcond(int cond, Register src, Address2 addr) {
        emitLoadStore(cond, 0, 0, src, addr);
    }

    /**
     * Similar to {@link #strb(Register, Address2)} except that this instruction is executed only
     * when the condition is satisfied.
     * <p>
     * Assembly example:        <pre>    streqb r6, [r1, r14, lsr #3]!</pre>
     * ARMAssembler equivalent: <pre>    asm.strcondb(asm.COND_EQ, asm.R6, Address2.preW(asm.R1, 1, asm.R14, asm.LSR, 3));</pre>
     *
     * @param cond condition under which this instruction is executed
     */
    public void strcondb(int cond, Register src, Address2 addr) {
        emitLoadStore(cond, 1, 0, src, addr);
    }

    /**
     * Stores a halfword from the least significant halfword of a register to the specified
     * memory address.
     * <p>
     * Assembly example:        <pre>    strh r13, [r7, #-123]</pre>
     * ARMAssembler equivalent: <pre>    asm.strh(asm.R13, Address3.pre(asm.R7, -123));</pre>
     *
     * @param src source register
     * @param addr memory address
     */
    public void strh(Register src, Address3 addr) {
        emitLoadStoreMisc(COND_AL, 0, 0, 1, src, addr);
    }

    /**
     * Similar to {@link #strh(Register, Address3)} except this instruction is executed only when
     * the condition is satisfied.
     * <p>
     * Assembly example:        <pre>    strh r13, [r7, #-123]</pre>
     * ARMAssembler equivalent: <pre>    asm.strh(asm.R13, Address3.pre(asm.R7, -123));</pre>
     *
     * @param cond condition code
     */
    public void strcondh(int cond, Register src, Address3 addr) {
        emitLoadStoreMisc(cond, 0, 0, 1, src, addr);
    }

    /**
     * Subtracts the value of the op2 operand from the src register and stores the result in the
     * dst register.
     * <p>
     * Assembly example:        <pre>    sub r4, r0, r13, asr #17</pre>
     * ARMAssembler equivalent: <pre>    asm.sub(asm.R4, asm.R0, Operand2.asr(asm.R13, 17));</pre>
     *
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    public void sub(Register dst, Register src, Operand2 op2) {
        emitDataProc(COND_AL, OPCODE_SUB, false, dst, src, op2);
    }

    /**
     * Similar to {@link #sub(Register, Register, Operand2)} except this instruction is executed
     * only when the condition is satisfied.
     * <p>
     * Assembly example:        <pre>    subeq r4, r0, r13, asr #17</pre>
     * ARMAssembler equivalent: <pre>    asm.subcond(asm.COND_EQ, asm.R4, asm.R0, Operand2.asr(asm.R13, 17));</pre>
     *
     * @param cond condition under which the instruction is executed
     */
    public void subcond(int cond, Register dst, Register src, Operand2 op2) {
        emitDataProc(cond, OPCODE_SUB, false, dst, src, op2);
    }

    /**
     * Similar to {@link #sub(Register, Register, Operand2)} except this instruction also updates
     * the CPSR and is executed only when the condition is satisfied.
     * <p>
     * Assembly example:        <pre>    subeqs r4, r0, r13, asr #17</pre>
     * ARMAssembler equivalent: <pre>    asm.subconds(asm.COND_EQ, asm.R4, asm.R0, Operand2.asr(asm.R13, 17));</pre>
     *
     * @param cond condition under which the instruction is executed
     */
    public void subconds(int cond, Register dst, Register src, Operand2 op2) {
        emitDataProc(cond, OPCODE_SUB, true, dst, src, op2);
    }

    /**
     * Similar to {@link #sub(Register, Register, Operand2)} except this instruction also updates
     * the CPSR.
     * <p>
     * Assembly example:        <pre>    subs r4, r0, r13, asr #17</pre>
     * ARMAssembler equivalent: <pre>    asm.subs(asm.R4, asm.R0, Operand2.asr(asm.R13, 17));</pre>
     *
     */
    public void subs(Register dst, Register src, Operand2 op2) {
        emitDataProc(COND_AL, OPCODE_SUB, true, dst, src, op2);
    }

    /**
     * Triggers a Software Interrupt (SWI) exception.
     * <p>
     * Assembly example:        <pre>    swi 0xd3c</pre>
     * ARMAssembler equivalent: <pre>    asm.swi(0xd3c);</pre>
     *
     * @param imm24 condition under which the instruction is executed
     */
    public void swi(int imm24) {
        swicond(COND_AL, imm24);
    }

    /**
     * Similar to {@link #swi(int)} except this instruction is executed only when the condition
     * is satisfied.
     * <p>
     * Assembly example:        <pre>    swieq 0xd3c</pre>
     * ARMAssembler equivalent: <pre>    asm.swicond(asm.COND_EQ, 0xd3c);</pre>
     *
     * @param cond condition under which the instruction is executed
     */
    public void swicond(int cond, int imm24) {
        Assert.that((imm24 & 0xfff) == imm24, "not a 24-bit integer");

        emitByte((cond << 4) | 0xf);
        emitInt24(imm24);
    }

    /**
     * Swaps a word between registers and memory. A word is loaded from the memory location
     * specified by the address register and its value stored in the destination register. The
     * value in the source register is then stored at the memory location.
     * <p>
     * Assembly example:        <pre>    swp r5, r5, [sp]</pre>
     * ARMAssembler equivalent: <pre>    asm.swp(asm.R5, asm.R5, asm.SP);</pre>
     *
     * @param dst register where the value at the memory address will be written to
     * @param src value that will be written to the memory address
     * @param addr address of the memory location
     */
    public void swp(Register dst, Register src, Register addr) {
        swpcond(COND_AL, dst, src, addr);
    }

    /**
     * Similar to {@link #swp(Register, Register, Register)} except this instruction is executed
     * only when the condition is satisfied.
     * <p>
     * Assembly example:        <pre>    swpeq r5, r5, [sp]</pre>
     * ARMAssembler equivalent: <pre>    asm.swpcond(asm.COND_EQ, asm.R5, asm.R5, asm.SP);</pre>
     *
     * @param cond condition code
     */
    public void swpcond(int cond, Register dst, Register src, Register addr) {
        Assert.that(dst != R15 && src != R15 && addr != R15, R15 + " cannot be used as a parameter");
        Assert.that(src != addr, "the same register cannot be used for both the source value and memory address");
        Assert.that(dst != addr, "the same register cannot be used for both the destination value and memory address");

        emitByte((cond << 4) | 0x1);
        emitByte(addr.getNumber());
        emitByte(dst.getNumber() << 4);
        emitByte(0x90 | src.getNumber());
    }

    /**
     * Swaps a byte between registers and memory. A byte is loaded from the memory location
     * specified by the address register and its zero-extended value stored in the destination
     * register. The least significant byte of the source register is then stored at the memory
     * location.
     * <p>
     * Assembly example:        <pre>    swpb r5, r5, [sp]</pre>
     * ARMAssembler equivalent: <pre>    asm.swpb(asm.R5, asm.R5, asm.SP);</pre>
     *
     * @param dst register where the value at the memory address will be written to
     * @param src value that will be written to the memory address
     * @param addr address of the memory location
     */
    public void swpb(Register dst, Register src, Register addr) {
        swpcondb(COND_AL, dst, src, addr);
    }

    /**
     * Similar to {@link #swpb(Register, Register, Register)} except this instruction is executed
     * only when the condition is satisfied.
     * <p>
     * Assembly example:        <pre>    swpeqb r5, r5, [sp]</pre>
     * ARMAssembler equivalent: <pre>    asm.swpcondb(asm.COND_EQ, asm.R5, asm.R5, asm.SP);</pre>
     *
     * @param cond condition code
     */
    public void swpcondb(int cond, Register dst, Register src, Register addr) {
        Assert.that(dst != R15 && src != R15 && addr != R15, R15 + " cannot be used as a parameter");
        Assert.that(src != addr, "the same register cannot be used for both the source value and memory address");
        Assert.that(dst != addr, "the same register cannot be used for both the destination value and memory address");

        emitByte((cond << 4) | 0x1);
        emitByte(0x40 | addr.getNumber());
        emitByte(dst.getNumber() << 4);
        emitByte(0x90 | src.getNumber());
    }

    /**
     * Compares a register value OR'd with another arithmetic value and updates the CPSR.
     * <p>
     * Assembly example:        <pre>    teq r8, r13, asr #17</pre>
     * ARMAssembler equivalent: <pre>    asm.teq(asm.R8, Operand2.asr(asm.R13, 17));</pre>
     *
     * @param op1 first operand register
     * @param op2 flexible second operand
     */
    public void teq(Register op1, Operand2 op2) {
        teqcond(COND_AL, op1, op2);
    }

    /**
     * Similar to {@link #teq(Register, Operand2)} except this instruction is executed only when
     * the condition is satisfied.
     * <p>
     * Assembly example:        <pre>    teqeq r8, r13, asr #17</pre>
     * ARMAssembler equivalent: <pre>    asm.teqcond(asm.COND_EQ, asm.R8, Operand2.asr(asm.R13, 17));</pre>
     *
     * @param cond condition code
     */
    public void teqcond(int cond, Register op1, Operand2 op2) {
        emitDataProc(cond, OPCODE_TEQ, true, Register.SBZ, op1, op2);
    }

    /**
     * Compares a register value AND'd with another arithmetic value and updates the CPSR.
     * <p>
     * Assembly example:        <pre>    tst r8, r13, asr #17</pre>
     * ARMAssembler equivalent: <pre>    asm.tst(asm.R8, Operand2.asr(asm.R13, 17));</pre>
     *
     * @param op1 first operand register
     * @param op2 flexible second operand
     */
    public void tst(Register op1, Operand2 op2) {
        tstcond(COND_AL, op1, op2);
    }

    /**
     * Similar to {@link #tst(Register, Operand2)} except this instruction is executed only when
     * the condition is satisfied.
     * <p>
     * Assembly example:        <pre>    tsteq r8, r13, asr #17</pre>
     * ARMAssembler equivalent: <pre>    asm.tstcond(asm.COND_EQ, asm.R8, Operand2.asr(asm.R13, 17));</pre>
     *
     * @param cond condition code
     */
    public void tstcond(int cond, Register op1, Operand2 op2) {
        emitDataProc(cond, OPCODE_TST, true, Register.SBZ, op1, op2);
    }

    /**
     * Multiplies the unsigned values in the two source registers to produce a 64-bit product
     * which is then added to the 64-bit value held in the two destination registers. The result
     * is written back to the two destination registers.
     * <p>
     * Assembly example:        <pre>    umlal r0, r1, r7, r14</pre>
     * ARMAssembler equivalent: <pre>    asm.umlal(asm.R0, asm.R1, asm.R7, asm.R14);</pre>
     *
     * @param dstLo destination register containing the low word of the 64-bit value
     * @param dstHi destination register containing the high word of the 64-bit value
     * @param op1 first source operand register
     * @param op2 second source operand register
     */
    public void umlal(Register dstLo, Register dstHi, Register op1, Register op2) {
        emitMulLong(COND_AL, 0, 1, false, dstLo, dstHi, op1, op2);
    }

    /**
     * Similar to {@link #umlal(Register, Register, Register, Register)} except this instruction
     * is executed only when the condition is sastisfied.
     * <p>
     * Assembly example:        <pre>    umlaleq r0, r1, r7, r14</pre>
     * ARMAssembler equivalent: <pre>    asm.umlalcond(asm.COND_EQ, asm.R0, asm.R1, asm.R7, asm.R14);</pre>
     *
     * @param cond condition code
     */
    public void umlalcond(int cond, Register dstLo, Register dstHi, Register op1, Register op2) {
        emitMulLong(cond, 0, 1, false, dstLo, dstHi, op1, op2);
    }

    /**
     * Similar to {@link #umlal(Register, Register, Register, Register)} except this instruction
     * also updates the CPSR and is executed only when the condition is sastisfied.
     * <p>
     * Assembly example:        <pre>    umlaleqs r0, r1, r7, r14</pre>
     * ARMAssembler equivalent: <pre>    asm.umlalconds(asm.COND_EQ, asm.R0, asm.R1, asm.R7, asm.R14);</pre>
     *
     * @param cond condition code
     */
    public void umlalconds(int cond, Register dstLo, Register dstHi, Register op1, Register op2) {
        emitMulLong(cond, 0, 1, true, dstLo, dstHi, op1, op2);
    }

    /**
     * Similar to {@link #umlal(Register, Register, Register, Register)} except this instruction
     * also updates the CPSR.
     * <p>
     * Assembly example:        <pre>    umlals r0, r1, r7, r14</pre>
     * ARMAssembler equivalent: <pre>    asm.umlals(asm.R0, asm.R1, asm.R7, asm.R14);</pre>
     *
     */
    public void umlals(Register dstLo, Register dstHi, Register op1, Register op2) {
        emitMulLong(COND_AL, 0, 1, true, dstLo, dstHi, op1, op2);
    }

    /**
     * Multiplies two 32-bit unsigned register values and stores the 64-bit product in two
     * destination registers.
     * <p>
     * Assembly example:        <pre>    umull r0, r1, r7, r14</pre>
     * ARMAssembler equivalent: <pre>    asm.umull(asm.R0, asm.R1, asm.R7, asm.R14);</pre>
     *
     * @param dstLo destination register containing the low word of the 64-bit product
     * @param dstHi destination register containing the high word of the 64-bit product
     * @param op1 first source operand register
     * @param op2 second source operand register
     */
    public void umull(Register dstLo, Register dstHi, Register op1, Register op2) {
        Assert.that (dstLo != dstHi && dstHi != op1 && dstLo != op1, "dstLo, dstHi and op1 must all be different");
        emitMulLong(COND_AL, 0, 0, false, dstLo, dstHi, op1, op2);
    }

    /**
     * Similar to {@link #umull(Register, Register, Register, Register)} except this instruction
     * is executed only when the condition is satisfied.
     * <p>
     * Assembly example:        <pre>    umulleq r0, r1, r7, r14</pre>
     * ARMAssembler equivalent: <pre>    asm.umullcond(asm.COND_EQ, asm.R0, asm.R1, asm.R7, asm.R14);</pre>
     *
     * @param cond condition code
     */
    public void umullcond(int cond, Register dstLo, Register dstHi, Register op1, Register op2) {
        Assert.that (dstLo != dstHi && dstHi != op1 && dstLo != op1, "dstLo, dstHi and op1 must all be different");
        emitMulLong(cond, 0, 0, false, dstLo, dstHi, op1, op2);
    }

    /**
     * Similar to {@link #umull(Register, Register, Register, Register)} except this instruction
     * also updates the CPSR and is executed only when the condition is satisfied.
     * <p>
     * Assembly example:        <pre>    umulleqs r0, r1, r7, r14</pre>
     * ARMAssembler equivalent: <pre>    asm.umullconds(asm.COND_EQ, asm.R0, asm.R1, asm.R7, asm.R14);</pre>
     *
     * @param cond condition code
     */
    public void umullconds(int cond, Register dstLo, Register dstHi, Register op1, Register op2) {
        Assert.that (dstLo != dstHi && dstHi != op1 && dstLo != op1, "dstLo, dstHi and op1 must all be different");
        emitMulLong(cond, 0, 0, true, dstLo, dstHi, op1, op2);
    }

    /**
     * Similar to {@link #umull(Register, Register, Register, Register)} except this instruction
     * also updates the CPSR.
     * <p>
     * Assembly example:        <pre>    umulls r0, r1, r7, r14</pre>
     * ARMAssembler equivalent: <pre>    asm.umulls(asm.R0, asm.R1, asm.R7, asm.R14);</pre>
     *
     */
    public void umulls(Register dstLo, Register dstHi, Register op1, Register op2) {
        Assert.that (dstLo != dstHi && dstHi != op1 && dstLo != op1, "dstLo, dstHi and op1 must all be different");
        emitMulLong(COND_AL, 0, 0, true, dstLo, dstHi, op1, op2);
    }
}
