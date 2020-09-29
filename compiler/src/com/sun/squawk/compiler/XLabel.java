/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: XLabel.java,v 1.4 2005/01/18 23:21:06 ccifue Exp $
 */

package com.sun.squawk.compiler;

import java.io.PrintStream;
import com.sun.squawk.compiler.asm.x86.*;

/**
 * <code>Label</code> representation in the <code>Compiler</code> interface.
 *
 * @author   Cristina Cifuentes
 */

class XLabel implements Label {

    /**
     * The assembler label.
     */
    private ALabel asmLabel;

    /**
     * Constructor
     *
     * @param asm the assembler that allocates this label.
     */
    public XLabel(Assembler asm) {
        asmLabel = new ALabel(asm);
    }

    /**
     * Test to see if the label has been bound.
     *
     * @return true if it is
     */
    public boolean isBound() {
        return asmLabel.isBound();
    }

    /**
     * Get the offset to the label in the code buffer.
     *
     * @return the offset in bytes
     */
    public int getOffset() {
        return asmLabel.getPos();
    }

    public ALabel getAssemblerLabel(){
        return asmLabel;
    }

    /**
     * Prints information about this label.
     * This method is used for debugging purposes.
     */
    public void print() {
        asmLabel.print();
    }
}
