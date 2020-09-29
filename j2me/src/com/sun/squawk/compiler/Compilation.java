/*
 * Copyright 2004-2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: Compilation.java,v 1.13 2005/01/18 23:21:06 ccifue Exp $
 */

package com.sun.squawk.compiler;

import com.sun.squawk.compiler.Compiler;

/**
 * Steps of compilation: compile and link.
 *
 * @author Bernd Mathiske or Nik Shaylor (?? - check *****)
 */
public class Compilation {

    /**
     * Create a new compiler from a class name.
     *
     * @param compilerClassName  the name
     * @return                   the compiler
     */
    private static Compiler newCompilerPrim(String compilerClassName) {
        try {
            Class compilerClass = Class.forName(compilerClassName);
            return (Compiler) compilerClass.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Could not load and instantiate compiler class " + compilerClassName + " [" + e.getMessage() + "]");
        }
    }

    /**
     * Create a new compiler for a specific architecture.
     *
     * @param arch the architecture  name
     * @return                       the compiler
     */
    public static Compiler newCompiler(String arch) {
        return newCompilerPrim("com.sun.squawk.compiler."+arch+"Compiler");
    }

    /**
     * Create a new default compiler by determining which architecture the VM
     * is running on.  By default, set it to the X86 architecture.
     *
     * @return the compiler
     */
    public static Compiler newCompiler() {
        String arch = "${build.properties:ARCHITECTURE}"; // Will be edited by the romizer
        if (arch.endsWith("ARCHITECTURE}")) {             // If the code is not romized...
            arch = System.getProperty("squawk.architecture");
            if (arch == null) {
                arch = "X86"; // The platform du jour
            }
        }
        return newCompiler(arch);
    }

    /**
     * Create a new linker.
     *
     * @param compiler  the compiler to be linked
     * @return          the linker
     */
    public static Linker newLinker(Compiler compiler) {
        return new Linker(compiler);
    }

}
