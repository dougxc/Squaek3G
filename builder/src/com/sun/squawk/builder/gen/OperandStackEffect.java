/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk.builder.gen;

import java.util.*;
import java.io.*;

/**
 * Generator for the source of the class com.sun.squawk.vm.OperandStackEffect.
 *
 * @author  Doug Simon
 */
public class OperandStackEffect extends Generator {

    /**
     * {@inheritDoc}
     */
    void generate(PrintWriter out) {

        // Generate class header
        printCopyright(out);
        out.println("package com.sun.squawk.vm;");
        out.println();
        out.println("import com.sun.squawk.util.Assert;");
        out.println();
        out.println("/**");
        out.println(" * This class defines effect of the Squawk VM instructions on the operand stack.");
        out.println(" *");
        out.println(" * @author  Doug Simon");
        out.println(" */");
        out.println("public final class OperandStackEffect {");
        out.println();

        // Generate getEffect() function
        out.println();
        out.println("    /**");
        out.println("     * Gets the effect of an instruction on the operand stack. Each character");
        out.println("     * in the returned string preceeding the ':' character denotes a type of value");
        out.println("     * popped from the stack and a character after the ':' denotes a type of value");
        out.println("     * pushed to the stack. The possible types in the string are:");
        out.println("     *");
        out.println("     *    I  int");
        out.println("     *    O  object/reference");
        out.println("     *    F  float");
        out.println("     *    W  address/word/offset");
        out.println("     *    L  long");
        out.println("     *    D  double");
        out.println("     *    *  clears the stack (only preceeds ':')");
        out.println("     *");
        out.println("     * @param  opcode  an instruction opcode");
        out.println("     * @return the effect of the instruction on the operand stack");
        out.println("     * @throws IndexOutOfBoundsException if <code>opcode</code> is not valid");
        out.println("     * @throws IllegalArgumentException if <code>opcode</code> denotes a prefix instruction");
        out.println("     */");
        out.println("    public static String getEffect(int opcode) {");
        out.println("        if (effects[opcode] == null) { throw new IllegalArgumentException(); }");
        out.println("        return effects[opcode];");
        out.println("    }");
        out.println();

        // Generate operand stack effects table
        out.println("    private final static String[] effects = { ");
        int opcode = printEffectsDef(out, Instruction.getInstructions(), 0, false);
        out.println("/*if[FLOATS]*/");
        printEffectsDef(out, Instruction.getFloatInstructions(), opcode, true);
        out.println("/*end[FLOATS]*/");
        out.println("    };");

        out.println("}");
    }

    /**
     * {@inheritDoc}
     */
    public File getGeneratedFile(File baseDir) {
        return new File(baseDir, "com/sun/squawk/vm/OperandStackEffect.java");
    }

    private static int printEffectsDef(PrintWriter out, List list, int opcodeCheck, boolean closeArrayInitializer) {

        for (Iterator iterator = list.iterator(); iterator.hasNext(); ) {
            Instruction instruction = (Instruction) iterator.next();
            if (opcodeCheck != instruction.opcode) {
                throw new RuntimeException("instructions are not ordered by opcode");
            }

            String ose = instruction.operandStackEffect == null ? "null" : "\"" + instruction.operandStackEffect + "\"";

            out.print("        " + pad("/* " + instruction.mnemonic.toUpperCase() + " */", 32) + ose);
            if (closeArrayInitializer && !iterator.hasNext()) {
                out.println();
            } else {
                out.println(",");
            }
            opcodeCheck++;
        }
        return opcodeCheck;
    }
}
