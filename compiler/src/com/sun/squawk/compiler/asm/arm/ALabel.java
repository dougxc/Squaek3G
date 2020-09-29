/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: ALabel.java,v 1.4 2005/01/26 03:00:01 dl156546 Exp $
 */

package com.sun.squawk.compiler.asm.arm;

import com.sun.squawk.compiler.asm.*;

/**
 * Represents known or yet unknown target destinations for jumps and calls.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class ALabel {

    /**
     * The next label id
     */
    private static int nextid = 0;

    /**
     * Label id. Just used for printing a trace.
     */
    private int id = nextid++;

    /**
     * The assembler that allocated this label,
     */
    private ARMAssembler asm;

    /**
     * Encodes both the binding state and the binding position of this label.
     */
    private int pos = -1;

    /**
     * List of unbound branch instrutions;
     */
    private UnboundBranch branches;

    /**
     * List of unbound Addressing Mode 2 instructions.
     */
    private AddrInstruction addr2ins;

    /**
     * List of unbound Addressing Mode 3 instructions.
     */
    private AddrInstruction addr3ins;

    /**
     * List of unbound relocators;
     */
    private UnboundReloc rels;

    /**
     * Constructs a new unused label.
     *
     * @param asm the assember the label is bound to
     */
    public ALabel(ARMAssembler asm) {
        this.asm = asm;
        asm.unboundLabelCount(1);
    }

    /**
     * Returns the label id.
     *
     * @return  the id
     */
     public int getId() {
        return id;
    }

    /**
     * Returns the target position or the last displacement in the chain. The
     * meaning of the actual result depends on whether the label is bound or
     * unbound.
     *
     * @return  target position or last displacement
     */
     public int getPos() {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(pos != -1, "label is unused");
        }
        return pos;
    }

    /**
     * Binds this label to the specified code position. The position is stored
     * in this label for future backward jumps.
     *
     * @param  pos  target code position
     */
    public void bindTo(int pos) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(this.pos == -1, "label bound twice");
            Assert.that(pos >= 0, "illegal position");
        }

        this.pos = pos;
        asm.unboundLabelCount(-1);

        CodeBuffer code = asm.getCode();
        int save = code.getCodePos();

        /*
         * patch in any branch instructions
         */
        while(branches != null) {
            UnboundBranch branch = branches;
            branches = branches.next;
            code.setCodePos(branch.position);
            asm.bcond(branch.cond, branch.l, this);
        }

        /**
         * patch in any Addressing Mode 2 instructions
         */
        while (addr2ins != null) {
            AddrInstruction addr2 = addr2ins;
            addr2ins = addr2ins.next;
            asm.patchAddress2Label(addr2.position, this);
        }

        /**
         * patch in any Addressing Mode 3 instructions
         */
        while (addr3ins != null) {
            AddrInstruction addr3 = addr3ins;
            addr3ins = addr3ins.next;
            asm.patchAddress3Label(addr3.position, this);
        }

        code.setCodePos(save);

        /*
         * patch in any relocators
         */
        while(rels != null) {
            UnboundReloc rel = rels;
            rels = rels.next;
            rel.relocator.setValue(pos);
        }
    }

    /**
     * Returns whether or not this label is bound.
     *
     * @return  whether or not this label is bound
     */
    public boolean isBound() {
        return pos != -1;
    }

    /**
     * Returns whether or not this label is unbound.
     *
     * @return  whether or not this label is unbound
     */
    public boolean isUnbound() {
        return pos == -1;
    }

    /**
     * Add a record for a branch that needs to be inserted when the label is bound.
     *
     * @param  cond condition under which the branch will occur
     * @param  l whether the link register will store the return address
     * @param  position the positon of the branch instruction
     */
    public void addBranch(int cond, int l, int position) {
        branches = new UnboundBranch(cond, l, position, branches);
    }

    /**
     * Adds a record for an instruction using Addressing Mode 2 whose relative address needs to
     * be patched when the label is bound. The instruction must already have been emitted into
     * the code buffer.
     *
     * @param position position of the instruction that uses Addressing Mode 2
     */
    public void addAddress2Instr(int position) {
        addr2ins = new AddrInstruction(position, addr2ins);
    }

    /**
     * Adds a record for an instruction using Addressing Mode 3 whose relative address needs to
     * be patched when the label is bound. The instruction must already have been emitted into
     * the code buffer.
     *
     * @param position position of the instruction that uses Addressing Mode 3
     */
    public void addAddress3Instr(int position) {
        addr3ins = new AddrInstruction(position, addr3ins);
    }

    /**
     * Add a record for a relocator that needs to be updated when the label is bound.
     *
     * @param  reloc the relocator to add
     */
    public void addRelocator(Relocator reloc) {
        if (isUnbound()) {
            rels = new UnboundReloc(reloc, rels);
        } else {
            reloc.setValue(pos);
        }
    }

    /**
     * Prints information about this assembler label.
     * This method is used for debugging purposes.
     */
    public void print() {
        System.err.print("ALabel id = " + id + ", pos = " + pos);
    }

}

/**
 * Private class to hold the information for an instruction that uses labels as an address.
 */
class AddrInstruction {
    int position;
    AddrInstruction next;

    AddrInstruction(int position, AddrInstruction next) {
        this.position = position;
        this.next = next;
    }
}

/**
 * Private class to hold the information for an unbound branch instruction.
 */
class UnboundBranch {
    int cond;
    int l;
    int position;
    UnboundBranch next;

    UnboundBranch(int cond, int l, int position, UnboundBranch next) {
        this.cond     = cond;
        this.l        = l;
        this.position = position;
        this.next     = next;
    }
}

/**
 * Private class to hold the information for an unbound relocation information.
 */
class UnboundReloc {
    Relocator relocator;
    UnboundReloc next;

    UnboundReloc(Relocator relocator, UnboundReloc next) {
        this.relocator = relocator;
        this.next      = next;
    }
}
