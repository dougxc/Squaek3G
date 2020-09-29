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
 * Generator for the class com.sun.squawk.vm.JitterSwitch.
 *
 * @author  Doug Simon
 */
public class JitterSwitch extends Verifier {

    protected void printHeader(PrintWriter out) {
        out.println("package com.sun.squawk.vm;");
        out.println("");
        out.println("import com.sun.squawk.compiler.*;");
        out.println("import com.sun.squawk.util.Assert;");
        out.println();
        out.println("/**");
        out.println(" * This class defines the switch table used by the Squawk jitter.");
        out.println(" *");
        out.println(" * @author   Nik Shaylor");
        out.println(" */");
        out.println("abstract public class JitterSwitch extends Common implements Types {");
        out.println("");
        out.println("    /**");
        out.println("     * The the immediate operand value of the current bytecode.");
        out.println("     */");
        out.println("    protected int iparm;");
        out.println();
        out.println("    abstract protected void iparmNone();");
        out.println("    abstract protected void iparmByte();");
        out.println("    abstract protected void iparmUByte();");
        out.println();
        out.println("    /**");
        out.println("     * Generate the native code for a bytecode.");
        out.println("     *");
        out.println("     * @param opcode the opcode to jit");
        out.println("     */");
        out.println("    protected void do_switch(int opcode) {");
        out.println("        switch(opcode) {");
    }
    
    protected void printFooter(PrintWriter out) {
        out.println("            default: Assert.shouldNotReachHere(\"unknown opcode \" + opcode);");
        super.printFooter(out);
    }
    
    String getFirstLine() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public File getGeneratedFile(File baseDir) {
        return new File(baseDir, "com/sun/squawk/vm/JitterSwitch.java");
    }
}
