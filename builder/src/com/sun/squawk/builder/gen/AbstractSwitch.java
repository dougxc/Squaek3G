/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk.builder.gen;

import java.io.*;
import java.util.*;

/**
 * Abstract generator for the code that has the form of a switch statement with a case for each instruction.
 *
 * @author  Doug Simon
 */
abstract public class AbstractSwitch extends Generator {


    /**
     * {@inheritDoc}
     */
    void generate(PrintWriter out) {

        List instructions = Instruction.getInstructions();
        List floatInstructions = Instruction.getFloatInstructions();

        String firstLine = getFirstLine();
        if (firstLine != null) {
            out.println(firstLine);
        }

        printCopyright(out);

        // Generate header
        printHeader(out);

        // Generate opcode constants
        printCases(out, instructions);
        out.println();
        out.println("/*if[FLOATS]*/");
        printCases(out, floatInstructions);
        out.println("/*end[FLOATS]*/");

        printFooter(out);
    }

    String getFirstLine() {
        return null;
    }

    abstract void printFooter(PrintWriter out);
    abstract void printHeader(PrintWriter out);

    /**
     * Prints the opcode constant definitions for the instructions in a given list.
     *
     * @param out      where to print
     * @param list     a list of Instructions
     */
    abstract void printCases(PrintWriter out, List list);
}
