/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: ARMDisassembler.java,v 1.2 2006/04/21 16:33:20 dw29446 Exp $
 */

package com.sun.squawk.compiler.asm.arm;
import java.util.Hashtable;
import com.sun.squawk.compiler.asm.*;

/**
 * Prints the generated machine code together with the corresponding assembly
 * instructions. The disassembler supports all of the instructions implemented
 * by the ARM assembler. Unsupported instructions are represented by "?". An
 * example of the disassembler output is as follows:
 * <p>
 * <pre>
 *     00008000  e5 9f 00 14              ldr r0, [pc, #20] ; 0x801c
 *     00008004  ef 00 00 02              swi 0x2
 *     00008008  ef 00 00 11              swi 0x11
 *     0000800c  48 65 6c 6c              stmmida r5!, {r2, r3, r5, r6, r10, fp, sp, lr}
 *     00008010  6f 20 57 6f              swivs 0x20576f
 *     00008014  72 6c 64 21              rsbvc r6, ip, #0x21000000
 *     00008018  00 00 00 00              andeq r0, r0, r0
 *     0000801c  00 00 80 0c              andeq r8, r0, ip
 * </pre>
 *
 * @see      ARMAssembler
 * @author   David Liu
 * @version  1.00
 */
public class ARMDisassembler implements Constants {
    /**
     * The buffer that stores the generated code.
     */
    private CodeBuffer code;

    /**
     * The start address of the code.
     */
    private int codeStart;

    /**
     * The position of the next byte to be disassembled.
     */
    private int cur;

    /**
     * The current instruction being disassembled.
     */
    private int curInstr;

    private int instr [] = new int [4];

    /**
     * Condition code
     */
    private String cond;

    /**
     * Hashtable of offsets to comments.
     */
    private Hashtable comments = new Hashtable();

    /**
     * Constructs a new disassembler for the specified code buffer.
     *
     * @param  code  the code buffer
     */
    public ARMDisassembler(CodeBuffer code) {
        this.code      = code;
        this.codeStart = code.getCodeBegin();
        this.comments  = new Hashtable();
    }

    /**
     * Constructs a new disassembler for the specified code buffer.
     *
     * @param  code  the code buffer
     */
    public ARMDisassembler(CodeBuffer code, Hashtable comments) {
        this.code      = code;
        this.codeStart = code.getCodeBegin();
        this.comments  = comments;
    }

    /**
     * Returns the hexadecimal representation of the specified value. The
     * resulting string consists of the prefix <code>0x</code> appended to the
     * uppercase hexadecimal digits with no extra leading zeros.
     *
     * @param   value  value to be converted
     * @return  the hexadecimal representation
     */
    private static String hexString(int value) {
        char[] buf = new char[8];
        int charPos = 8;
        do {
            buf[--charPos] = Integer.toHexString(value & 0xf).charAt(0);
            value >>>= 4;
        } while (value != 0);
        return "0x" + new String(buf, charPos, (8 - charPos));
    }

    /**
     * Returns the hexadecimal representation of the specified address. The
     * resulting string is extended with leading zeros if necessary and does
     * not contain any appendix.
     *
     * @param   value  address to be converted
     * @return  the hexadecimal representation
     */
    private static String address(int value) {
        char[] buf = new char[8];
        for (int i = 7; i >= 0; i--) {
            buf[i] = Integer.toHexString(value & 0xf).charAt(0);
            value >>>= 4;
        }
        return new String(buf);
    }

    /**
     * Returns a string representation an unknown instruction.
     *
     * @return  the bad format string
     */
    private static String shouldNotReachHere() {
        Assert.that(false, "should not reach here");
        return "?";
    }

    /**
     * Returns the byte at the specified position in the code buffer.
     *
     * @param   pos  index into the code buffer
     * @return  the byte at the specified index
     */
    private int byteAt(int pos) {
        if (pos >= code.getCodeEnd()) {
            throw new EndException();
        }
        return code.byteAt(pos);
    }

    /**
     * Performs a rotate-right function on an integer.
     *
     * @param value integer to be rotated
     * @param rotate number of bits to rotate right
     * @return the rotated value
     */
    private int ror(int value, int rotate) {
        return (value >>> rotate) | (value << (32 - rotate));
    }

    /**
     * Returns the current instruction and advances the current position to the next instruction.
     *
     */
    private void next() {
        instr [3] = code.byteAt(cur);
        instr [2] = code.byteAt(cur + 1);
        instr [1] = code.byteAt(cur + 2);
        instr [0] = code.byteAt(cur + 3);

        curInstr = (instr [3] << 24) | (instr [2] << 16) | (instr [1] << 8) | instr [0];

        cur += 4;
    }

    /**
     * Performs a bit mask on the current instruction word and determines if the masked value
     * matches the specified value.
     *
     * @param mask bit mask
     * @param maskValue value that the masked instruction word is compared against
     * @return true if maskValue is equal to the masked value of the current instruction word
     */
    private boolean mask(int mask, int maskValue) {
        return (curInstr & mask) == maskValue;
    }

    /**
     * Decodes the predicate condition flag in the current instruction.
     *
     * @return the condition code
     */
    private String decodeCond() {
        int cond = curInstr >>> 28;
        return (cond == COND_AL) ? "" : CONDITION_MNEMONICS [cond & 0xf];
    }

    /**
     * Returns the string representation for an invalid (different to unsupported) instruction.
     *
     * @return String
     */
    private String decodeInvalid() {
        return "invalid_opcode";
    }

    /**
     * Decodes a swp{cond}{b} instruction.
     *
     * @return the decoded instruction
     */
    private String decodeSwap() {
        String size = ((curInstr >>> 22) & 1) == 1 ? "b" : "";
        int Rd = (curInstr >>> 12) & 0xf;
        int Rn = (curInstr >>> 16) & 0xf;
        int Rm = curInstr & 0xf;

        return "swp" + decodeCond() + size + " " + REGISTERS [Rd] + ", " +
            REGISTERS [Rm] + ", " + "[" + REGISTERS [Rn] + "]";
    }

    /**
     * Decodes a multiply instruction.
     *
     * @return the decoded instruction
     */
    private String decodeMultiply() {
        int wordLong = (curInstr >>> 23) & 1;
        int signed = (curInstr >>> 22) & 1;
        int accumulate = (curInstr >>> 21) & 1;
        int updateBase = (curInstr >>> 20) & 1;
        int Rs = (curInstr >>> 8) & 0xf;
        int Rm = curInstr & 0xf;
        String operation = accumulate == 1 ? "mla" : "mul";
        String predicateSuffix = decodeCond() + (updateBase == 1 ? "s" : "");

        if (wordLong == 0) {
            int Rd = (curInstr >>> 16) & 0xf;
            int Rn = (curInstr >>> 12) & 0xf;

            return operation + predicateSuffix + " " +
                REGISTERS [Rd] + ", " + REGISTERS [Rm] + ", " + REGISTERS [Rs] +
                (accumulate == 1 ? (", " + REGISTERS [Rn]) : (""));
        } else {
            int RdHi = (curInstr >>> 16) & 0xf;
            int RdLo = (curInstr >>> 12) & 0xf;

            return (signed == 1 ? "s" : "u") + operation + "l" + predicateSuffix +
                " " + REGISTERS [RdLo] + ", " + REGISTERS [RdHi] + ", " +
                REGISTERS [Rm] + ", " + REGISTERS [Rs];
        }
    }

    /**
     * Decodes multiplies and the extra load/store instructions (figure 3-2 in the "ARM Architecture
     * Reference Manual, Second Edition").
     *
     * @return the decoded instruction
     */
    private String decodeMultipliesExtraLoadStore() {
        if (mask (0x1c000f0, 0x90)) {
            /*
             * multiply (accumulate)
             */

            return decodeMultiply();
        } else if (mask (0x18000f0, 0x800090)) {
            /*
             * multiply (accumulate) long
             */

            return decodeMultiply();
        } else if (mask (0xfb000f0, 0x1000090)) {
            /*
             * swap/swap byte
             */

            return decodeSwap();
        } else {
            /*
             * load/store halfword register offset
             */

            return decodeLoadStoreHalfword();
        }
    }

    /**
     * Decodes an msr{cond} instruction.
     *
     * @return the decoded instruction
     */
    private String decodeMsr() {
        String status = ((curInstr >>> 22) & 1) == 0 ? "CPSR" : "SPSR";
        String fField = ((curInstr >>> 19) & 1) == 1 ? "f" : "";
        String sField = ((curInstr >>> 18) & 1) == 1 ? "s" : "";
        String xField = ((curInstr >>> 17) & 1) == 1 ? "x" : "";
        String cField = ((curInstr >>> 16) & 1) == 1 ? "c" : "";
        String source = "";

        if (mask (0xff0, 0)) {
            // register source operand
            int Rm = curInstr & 0xf;

            source = REGISTERS[Rm].toString();
        } else {
            // immediate source operand
            int rotate = (curInstr >>> 8) & 0xf;
            int imm8 = curInstr & 0xff;
            int value = ror(imm8, 2 * rotate);

            source = "#" + value + " ; " + hexString(value);
        }

        return "msr" + decodeCond() + " " + status + "_" + fField + sField + xField + cField + ", " + source;
    }

    /**
     * Decodes an mrs{cond} instruction.
     *
     * @return the decoded instruction
     */
    private String decodeMrs() {
        String status = ((curInstr >>> 22) & 1) == 0 ? "CPSR" : "SPSR";
        int Rd = (curInstr >>> 12) & 0xf;

        return "mrs" + decodeCond() + " " + REGISTERS[Rd] + ", " + status;
    }

    /**
     * Decodes a load/store halfword instruction.
     *
     * @return the decoded instruction
     */
    private String decodeLoadStoreHalfword() {
        int operation = (curInstr >>> 20) & 1;
        int signedHalfword = (curInstr >>> 6) & 1;
        int halfwordByteAccess = (curInstr >>> 5) & 1;
        String size = halfwordByteAccess == 1 ? (signedHalfword == 1 ? "sh" : "h") : ("sb");
        int Rd = (curInstr >>> 12) & 0xf;

        if (operation == 0 && signedHalfword == 1) {
            return decodeInvalid();
        }

        return (operation == 1 ? "ldr" : "str") + decodeCond() +
            size + " " + REGISTERS [Rd] + ", " + decodeAddress3();
    }

    /**
     * Decodes a miscellaneous instruction.
     *
     * @return the decoded instruction
     */
    private String decodeMisc() {
        if (mask (0xfb000f0, 0x1000000)) {
            /*
             * move status register to register
             */
            return decodeMrs();
        } else if (mask (0xfb000f0, 0x1200000)) {
            /*
             * move register to status register
             */
            return decodeMsr();
        } else if (mask (0xff000f0, 0x1600010)) {
            /*
             * count leading zeroes
             */
            int Rd = (curInstr >>> 12) & 0xf;
            int Rm = curInstr & 0xf;

            return "clz" + decodeCond() + " " + REGISTERS[Rd] + ", " + REGISTERS[Rm];
        } else if (mask (0xfff000f0, 0xe1200070)) {
            /*
             * software breakpoint
             */
            int info = ((curInstr >>> 4) & 0xfff0) | (curInstr & 0xf);
            return "bkpt " + info + " ; " + hexString(info);
        }

        return "?";
    }

    /**
     * Decodes a flexible second operand expression.
     *
     * @return the decoded expression
     */
    private String decodeOperand2() {
        if (mask (0x2000000, 0x2000000)) {
            /*
             * immediate value
             */
            int rotate_imm = (curInstr >>> 8) & 0xf;
            int immed_8 = curInstr & 0xff;

            return "#0x" + Integer.toHexString(ror(immed_8, rotate_imm * 2));
        } else if (mask (0x2000010, 0)) {
            /*
             * immediate shift
             */
            int shift_amount = (curInstr >>> 7) & 0x1f;
            int shift = (curInstr >>> 5) & 0x3;
            int Rm = curInstr & 0xf;

            if (shift == LSL && shift_amount == 0) {
                return REGISTERS[Rm].toString();
            } else if (shift == ROR && shift_amount == 0) {
                return REGISTERS[Rm] + ", rrx";
            } else {
                return REGISTERS[Rm] + ", " + SHIFT[shift] + " #" + shift_amount;
            }
        } else if (mask (0x2000010, 0x10)) {
            /*
             * register shift
             */
            int Rm = curInstr & 0xf;
            int Rs = (curInstr >>> 8) & 0xf;
            int shift = (curInstr >>> 5) & 0x3;

            return REGISTERS[Rm] + ", " + SHIFT[shift] + " " + REGISTERS[Rs];
        } else {
            return shouldNotReachHere();
        }
    }

    /**
     * Decodes an Addressing Mode 2 address.
     *
     * @return the decoded address
     */
    private String decodeAddress2() {
        int postIndexed = (curInstr >>> 24) & 1;
        int unsigned = (curInstr >>> 23) & 1;
        String sign = unsigned == 1 ? "" : "-";
        int baseUpdate = (curInstr >> 21) & 1;
        String baseUpdateStr = baseUpdate == 1 ? "!" : "";
        int Rn = (curInstr >>> 16) & 0xf;

        if (mask (0x2000000, 0)) {
            /*
             * immediate offset
             */
            int offset12 = curInstr & 0xfff;
            String addr = "";

            if (postIndexed == 1) {
                addr = "[" + REGISTERS[Rn] +
                    (offset12 != 0 ? (", #" + sign + offset12) : "") + "]" + baseUpdateStr;

                if (REGISTERS [Rn] == PC) {
                    /*
                     * calculate the target address if it is relative to the program counter
                     */
                    int pc = code.getRelocation() + cur + 4;
                    int target = pc + (unsigned == 1 ? 1 : -1) * offset12;

                    addr += " ; " + hexString (target);
                }
            } else {
                addr =  "[" + REGISTERS[Rn] + "]" + (offset12 != 0 ? (", #" + sign + offset12) : "");
            }

            return addr;
        } else if (mask (0xff0, 0)) {
            /*
             * register offset
             */
            int Rm = curInstr & 0xf;

            if (postIndexed == 1) {
                return "[" + REGISTERS[Rn] + ", " + sign + REGISTERS[Rm] + "]" + baseUpdateStr;
            } else {
                return "[" + REGISTERS[Rn] + "], " + sign + REGISTERS[Rm];
            }
        } else {
            /*
             * scaled register offset
             */
            int shift_amount = (curInstr >>> 7) & 0x1f;
            int shift = (curInstr >>> 5) & 0x3;
            int Rm = curInstr & 0xf;
            String shiftStr;

            if (shift == ROR && shift_amount == 0) {
                shiftStr = "rrx";
            } else {
                shiftStr = SHIFT[shift] + " #" + shift_amount;
            }

            if (postIndexed == 1) {
                return "[" + REGISTERS[Rn] + ", " + sign + REGISTERS[Rm] + ", " + shiftStr +
                    "]" + baseUpdateStr;
            } else {
                return "[" + REGISTERS[Rn] + "], " + sign + REGISTERS[Rm] + ", " + shiftStr;
            }
        }
    }

    /**
     * Decodes an Addressing Mode 3 address.
     *
     * @return the decoded address
     */
    private String decodeAddress3() {
        int postIndexed = (curInstr >>> 24) & 1;
        int unsigned = (curInstr >>> 23) & 1;
        String sign = unsigned == 1 ? "" : "-";
        int baseUpdate = (curInstr >> 21) & 1;
        String baseUpdateStr = baseUpdate == 1 ? "!" : "";
        int Rn = (curInstr >>> 16) & 0xf;

        if (mask (0x400000, 0x400000)) {
            /*
             * immediate offset
             */
            int offset8 = ((curInstr >>> 4) & 0xf0) | (curInstr & 0xf);
            String addr = "";

            if (postIndexed == 1) {
                addr = "[" + REGISTERS[Rn] +
                    (offset8 != 0 ? (", #" + sign + offset8) : "") + "]" + baseUpdateStr;

                if (REGISTERS [Rn] == PC) {
                    /*
                     * calculate the target address if it is relative to the program counter
                     */
                    int pc = code.getRelocation() + cur + 4;
                    int target = pc + (unsigned == 1 ? 1 : -1) * offset8;

                    addr += " ; " + hexString (target);
                }
            } else {
                addr =  "[" + REGISTERS[Rn] + "]" + (offset8 != 0 ? (", #" + sign + offset8) : "");
            }

            return addr;
        } else {
            /*
             * register offset
             */
            int Rm = curInstr & 0xf;

            if (postIndexed == 1) {
                return "[" + REGISTERS[Rn] + ", " + sign + REGISTERS[Rm] + "]" + baseUpdateStr;
            } else {
                return "[" + REGISTERS[Rn] + "], " + sign + REGISTERS[Rm];
            }
        }
    }

    /**
     * Decodes a data processing instruction.
     *
     * @return the decoded instruction
     */
    private String decodeDataProc() {
        int cond = curInstr >>> 28;
        int opcode = (curInstr >>> 21) & 0xf;
        int s = (curInstr >>> 20) & 0x1;
        int Rn = (curInstr >>> 16) & 0xf;
        int Rd = (curInstr >>> 12) & 0xf;
        int shift_amount = (curInstr >>> 7) & 0x1f;
        int shift = (curInstr >>> 5) & 0x3;
        int Rm = curInstr & 0xf;

        switch (opcode) {
            case OPCODE_MOV:
                if (cond == COND_AL && Rd == 0 && Rm == 0 && mask(0x2000000, 0)) {
                    return "nop ; mov r0, r0";
                }
                // fall through to mvn
            case OPCODE_MVN:
                return DATAPROC_INS [opcode] + decodeCond() + (s == 1 ? "s" : "") +
                    " " + REGISTERS [Rd] + ", " + decodeOperand2();

            case OPCODE_CMP:
            case OPCODE_CMN:
            case OPCODE_TST:
            case OPCODE_TEQ:
                return DATAPROC_INS [opcode] + decodeCond() + " " +
                    REGISTERS [Rn] + ", " + decodeOperand2();

            case OPCODE_ADD:
            case OPCODE_SUB:
            case OPCODE_RSB:
            case OPCODE_ADC:
            case OPCODE_SBC:
            case OPCODE_RSC:
            case OPCODE_AND:
            case OPCODE_BIC:
            case OPCODE_EOR:
            case OPCODE_ORR:
                return DATAPROC_INS [opcode] + decodeCond() + (s == 1 ? "s" : "") +
                    " " + REGISTERS [Rd] + ", " + REGISTERS [Rn] + ", " + decodeOperand2();

            default:
                return shouldNotReachHere();
        }
    }

    /**
     * Decodes a load/store word/byte instruction.
     *
     * @return the decoded instruction
     */
    private String decodeLoadStore() {
        int byteWordAccess = (curInstr >>> 22) & 1;
        int operation = (curInstr >>> 20) & 1;
        int Rd = (curInstr >>> 12) & 0xf;

        return (operation == 1 ? "ldr" : "str") + decodeCond() +
            (byteWordAccess == 1 ? "b" : "") +
            " " + REGISTERS [Rd] + ", " + decodeAddress2();
    }

    /**
     * Decodes an instruction with 000 in bits 27 - 25.
     *
     * @return the decoded instruction
     */
    private String decode000() {
        if (mask (0x90, 0x90)) {
            /*
             * multiplies and extra load/store instructions
             */

            return decodeMultipliesExtraLoadStore();
        } else if (mask (0x1900090, 0x1000010)) {
            /*
             * miscellaneous instructions
             */

            return decodeMisc();
        } else if (mask (0x1900010, 0x1000000)) {
            /*
             * miscellaneous instructions
             */

            return decodeMisc();
        } else if (mask (0x10, 0x10)) {
            /*
             * data processing instructions
             */

            return decodeDataProc();
        } else if (mask (0x10, 0x0)) {
            /*
             * data processing instructions
             */

            return decodeDataProc();
        }

        return decodeInvalid();
    }

    /**
     * Decodes an instruction with 001 in bits 27 - 25.
     *
     * @return the decoded instruction
     */
    private String decode001() {
        if (mask (0x1b00000, 0x1200000)) {
            // move immediate to status register
            return decodeMsr();
        } else if (mask (0x1b00000, 0x1000000)) {
            // undefined instruction
        } else {
           return decodeDataProc();
        }

        return decodeInvalid();
    }

    /**
     * Decodes an instruction with 010 in bits 27 - 25.
     *
     * @return the decoded instruction
     */
    private String decode010() {
        return decodeLoadStore();
    }

    /**
     * Decodes an instruction with 011 in bits 27 - 25.
     *
     * @return the decoded instruction
     */
    private String decode011() {
        if (mask (0x10, 0)) {
            return decodeLoadStore();
        } else {
            return decodeInvalid();
        }
    }

    /**
     * Decodes an Addressing Mode 4 address.
     *
     * @return the decoded address
     */
    private String decodeAddress4() {
        int topBottom = (curInstr >>> 24) & 1;
        int upDown = (curInstr >>> 23) & 1;
        int updateBase = (curInstr >>> 21) & 1;
        int Rn = (curInstr >>> 16) & 0xf;
        String regsList = "";

        int regs = curInstr & 0xffff;
        for (int i = 0; i < 16; i++) {
            if ((regs & 1) == 1) {
                if (regsList != "") {
                    regsList += ", ";
                }

                regsList += REGISTERS [i];
            }

            regs >>>= 1;
        }

        return (upDown == 0 ? "d" : "i") + (topBottom == 0 ? "a" : "b") + " " +
            REGISTERS [Rn] + ((updateBase == 1) ? "!" : "") + ", {" + regsList + "}";
    }

    /**
     * Decodes an instruction with 100 in bits 27 - 25.
     *
     * @return the decoded instruction
     */
    private String decode100() {
        if (!mask (0xf0000000, 0xf0000000)) {
            int loadStore = (curInstr >>> 20) & 1;

            return ((loadStore == 1) ? "ldm" : "stm") + decodeCond() + decodeAddress4();
        } else {
            return decodeInvalid();
        }
    }

    /**
     * Decodes an instruction with 101 in bits 27 - 25.
     *
     * @return the decoded instruction
     */
    private String decode101() {
        if (!mask(0xf0000000, 0xf0000000)) {
            // branch and branch with link
            int link = (curInstr >>> 24) & 0x1;
            int pc = code.getRelocation() + cur + 4;
            int branchTarget = ((curInstr << 8) >> 6) + pc;

            return "b" + ((link == 1) ? "l" : "") + decodeCond() + " " + hexString(branchTarget);
        }

        return "?";
    }

    /**
     * Decodes an instruction with 111 in bits 27 - 25.
     *
     * @return the decoded instruction
     */
    private String decode111() {
        if (mask (0x1000000, 0x1000000)) {
            /*
             * software interrupt
             */
            int interrupt = curInstr & 0xffffff;

            return "swi" + decodeCond() + " " + hexString(interrupt);
        }

        return "?";
    }

    /**
     * Returns the assembly instruction for the next machine code.
     *
     * @return  next assembly instruction
     */
    private String decode() {
        for (;;) {
            next();

            int cond = curInstr >>> 28;

            if (cond == 0xf) {
                return decodeInvalid();
            } else {
                int category = (curInstr >>> 25) & 0x7;
                switch (category) {
                    case 0x0: return decode000();
                    case 0x1: return decode001();
                    case 0x2: return decode010();
                    case 0x3: return decode011();
                    case 0x4: return decode100();
                    case 0x5: return decode101();
                    case 0x6: return "?";
                    case 0x7: return decode111();
                    default:  return shouldNotReachHere();
                }
            }
        }
    }

    /**
     * Prints the values of the bytes in the specified code area.
     *
     * @param  start  start of the code area
     * @param  size   number of bytes to be printed
     */
    private void printBytes(int start, int size) {
        StringBuffer line = new StringBuffer();
        line.append(address(code.getRelocation() + codeStart + start));
        line.append(' ');
        for (int i = 0; i < size; i++) {
            if (line.length() > 30) {
                System.out.println(line.toString());
                line = new StringBuffer("         ");
            }
            line.append(' ');
            int value = byteAt(start + i);
            line.append(Integer.toHexString(value >>> 4).charAt(0));
            line.append(Integer.toHexString(value & 0xf).charAt(0));
        }
        while (line.length() < 35) {
            line.append(' ');
        }
        System.out.print(line.toString());
    }

    /**
     * Prints the specified area of machine code without disassembling it.
     *
     * @param  start  start of the code area
     * @param  end    end of the code area
     */
    public void hexDump(int start, int end) {
        if (end > start) {
            StringBuffer line = new StringBuffer();
            for (int addr = start & ~0x0f; addr < end; addr++) {
                if ((addr & 0x0f) == 0) {
                    System.out.println(line.toString());
                    line = new StringBuffer();
                    line.append(address(addr));
                    line.append(' ');
                } else if ((addr & 0x07) == 0) {
                    line.append(' ');
                }
                if (addr < start) {
                    line.append("   ");
                } else {
                    line.append(' ');
                    int value = byteAt(addr - codeStart);
                    line.append(Integer.toHexString(value >>> 4).charAt(0));
                    line.append(Integer.toHexString(value & 0xf).charAt(0));
                }
            }
            System.out.println(line);
        }
    }

    /**
     * Sets the location where the next instruction will be disassembled.
     *
     * @param start start addressing
     */
    public void setStart(int start) {
        cur = start - codeStart;
    }

    /**
     * Disassembles the next instruction in the code buffer and returns its assembler source
     * representation.
     *
     * @return disassembled instruction source
     */
    public String disassembleNext() {
        return decode();
    }

    /**
     * Disassembles the specified area of the machine code.
     *
     * @param  start  start of the code area
     * @param  end    end of the code area
     */
    public void disassemble(int start, int end) {
        cur = start - codeStart;
        while (codeStart + cur < end) {
            int last = cur;
            try {
                /*
                 * Decode the instruction
                 */
                String comment = (String)comments.get(new Integer(cur));
                String instruction = decode();

                /*
                 * If there is a comment for this address then print it.
                 */
                if (comment != null) {
                    boolean nl = true;
                    for (int i = 0 ; i < comment.length() ; i++) {
                        //if (nl) {
                        //    System.out.print("                                   ; ");
                        //    System.out.print("; ");
                        //}
                        int ch = comment.charAt(i);
                        nl = (ch == '\n');
                        System.out.print((char)ch);
                    }
                }

                /*
                 * Dump the bytes for the instruction.
                 */
                printBytes(last, cur - last);

                /*
                 * Print the disassembled instruction.
                 */
                System.out.println(instruction);
            } catch (EndException ex) {
                /*
                 * Just dump the byte for the instruction that could not be decoded.
                 */
                cur = last + 1;
                printBytes(last, 1);
            }
        }
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(codeStart + cur <= end, "last instruction too long");
        }
    }

}

class EndException extends RuntimeException {
    EndException() {
    }
}

class DisassembledInstruction {
    public DisassembledInstruction(String comment, String instruction) {
        this.comment = comment;
        this.instruction = instruction;
    }

    public String comment;
    public String instruction;
}
