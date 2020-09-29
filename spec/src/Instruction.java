/*
 * Copyright 2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */

//
//  Instruction.java
//
//  Created by Edward Carter on 5/26/05.
//

import java.io.PrintWriter;

public interface Instruction extends Comparable {
    abstract public String getName();
    abstract public void printOperation(PrintWriter out);
    abstract public void printFormat(PrintWriter out);
    abstract public void printForms(PrintWriter out);
    abstract public void printOperandStack(PrintWriter out);
    abstract public void printDescription(PrintWriter out);
    abstract public boolean hasNotes();
    abstract public void printNotes(PrintWriter out);
    abstract public boolean hasExceptions();
    abstract public void printExceptions(PrintWriter out);
}
