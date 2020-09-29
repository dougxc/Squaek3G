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
 * Generator for the class com.sun.squawk.translator.ir.Verifier.
 *
 * @author  Doug Simon
 */
public class BytecodeRoutines extends Verifier {

    protected void printHeader(PrintWriter out) {
        out.println("package com.sun.squawk.vm;");
        out.println("");
        out.println("import com.sun.squawk.compiler.*;");
        out.println();
        out.println("/**");
        out.println(" * This class defines the routines used by the Squawk interpreter and jitter.");
        out.println(" *");
        out.println(" * @author   Nik Shaylor");
        out.println(" */");
        out.println("abstract public class BytecodeRoutines implements Types {");
    }

    String getFirstLine() {
        return null;
    }

    void printFooter(PrintWriter out) {
        out.println("}");
    }

    private Set functionDefs = new HashSet();

    void printCases(PrintWriter out, List list) {
        for (Iterator iterator = list.iterator(); iterator.hasNext(); ) {
            Instruction instruction = (Instruction) iterator.next();
            if (instruction.compact == null) {
                String functionDef = getFunction(instruction, false);
                if (!functionDefs.contains(functionDef)) {
                    functionDefs.add(functionDef);
                    out.println("    abstract protected void " + functionDef);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public File getGeneratedFile(File baseDir) {
        return new File(baseDir, "com/sun/squawk/vm/BytecodeRoutines.java");
    }
}
