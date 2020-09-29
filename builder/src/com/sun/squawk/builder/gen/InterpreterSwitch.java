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

/**
 * Generator for the class com.sun.squawk.translator.ir.Verifier.
 *
 * @author  Doug Simon
 */
public class InterpreterSwitch extends Verifier {

    protected void printHeader(PrintWriter out) {
        out.println("package com.sun.squawk.vm;");
        out.println("");
        out.println("import com.sun.squawk.compiler.*;");
        out.println();
        out.println("/**");
        out.println(" * This class defines the switch table used by the generated Squawk interpreter.");
        out.println(" *");
        out.println(" * @author   Nik Shaylor");
        out.println(" */");
        out.println("abstract public class InterpreterSwitch extends Common implements Types {");
        out.println();
        out.println("    /**");
        out.println("     * Flags to show how the loading of the next bytecode should be done.");
        out.println("     */");
        out.println("    protected final static int FLOW_NEXT   = 0, // The next bytecode is always executed after the current one.");
        out.println("                               FLOW_CHANGE = 1, // The bytecode changes the control flow.");
        out.println("                               FLOW_CALL   = 2; // The bytecode either calls a routine, or might throw an exception.");
        out.println();
        out.println("    abstract protected void pre(int code);");
        out.println("    abstract protected void post();");
        out.println("    abstract protected void bind(int opcode);");
        out.println("    abstract protected void iparmNone();");
        out.println("    abstract protected void iparmByte();");
        out.println("    abstract protected void iparmUByte();");
        out.println();
        out.println("    /**");
        out.println("     * Create the bytecode interpreter.");
        out.println("     */");
        out.println("    protected void do_switch() {");
        out.println("        {");
    }

    String getFirstLine() {
        return null;
    }

    String startCase(Instruction instruction) {
        return "bind(OPC." + instruction.mnemonic.toUpperCase() + ");";
    }

    String endCase() {
        return "";
    }

    /**
     * {@inheritDoc}
     */
    String getFunction(Instruction instruction, boolean call) {
        return "pre(FLOW_" + instruction.flow.name + "); " + super.getFunction(instruction, true) + " post();";
    }

    /**
     * {@inheritDoc}
     */
    public File getGeneratedFile(File baseDir) {
        return new File(baseDir, "com/sun/squawk/vm/InterpreterSwitch.java");
    }
}
