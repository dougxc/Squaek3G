
/*
 * Copyright 2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM translator.
 */

package com.sun.squawk.pragma;

/**
 * The root of the pragma exception hierarchy.  These exceptions are all dummy exceptions
 * that are never actually thrown.  Methods declare them to be thrown as a way of indicating
 * various properties.
 */
public class PragmaException extends RuntimeException {

    /**
     * Bit flag for hosted methods.
     *
     * @see HostedPragma
     */
    public static final int HOSTED = 0x0001;

    /**
     * Bit flag for replacement constructors.
     *
     * @see ReplacementConstructorPragma
     */
    public static final int REPLACEMENT_CONSTRUCTOR = 0x0002;

    /**
     * Bit flag for methods that should only be invoked from the interpreter.
     *
     * @see InterpreterInvokedPragma
     */
    public static final int INTERPRETER_INVOKED = 0x0004;

    /**
     * Bit flag for methods that are made into native methods by the translator.
     *
     * @see NativePragma
     */
    public static final int NATIVE = 0x0008;

    /**
     * Bit flag for methods that are forceably inlined by the translator.
     *
     * @see ForceInlinedPragma
     */
    public static final int FORCE_INLINED = 0x0010;

    /**
     * Bit flag for methods that are never inlined by the translator.
     *
     * @see NotInlinedPragma
     */
    public static final int NOT_INLINED = 0x0020;

    /**
     * Given a bit mask, tells whether the method is run only in a hosted environment.
     */
    public static boolean isHosted(int pragmaFlags) {
        return (pragmaFlags & HOSTED) != 0;
    }

    /**
     * Given a bit mask, tells whether a method has its body replace a constructor
     * with the same signature.  Such methods should never be explicitly called.
     */
    public static boolean isReplacementConstructor(int pragmaFlags) {
        return (pragmaFlags & REPLACEMENT_CONSTRUCTOR) != 0;
    }

    /**
     * Given a bit mask, tells whether the method is to be invoked only from
     * the interpreter or JIT compiled code.
     */
    public static boolean isInterpreterInvoked(int pragmaFlags) {
        return (pragmaFlags & INTERPRETER_INVOKED) != 0;
    }

    /**
     * Given a bit mask, tells whether the method will be turned into a native
     * method by the translator.
     */
    public static boolean isNative(int pragmaFlags) {
        return (pragmaFlags & NATIVE) != 0;
    }

    /**
     * Given a bit mask, tells whether the method must be inlined by the translator.
     */
    public static boolean isForceInlined(int pragmaFlags) {
        return (pragmaFlags & FORCE_INLINED) != 0;
    }

    /**
     * Given a bit mask, tells whether the method is never inlined by the translator.
     */
    public static boolean isNotInlined(int pragmaFlags) {
        return (pragmaFlags & NOT_INLINED) != 0;
    }

    /**
     * Converts the name of a pragma class to a corresponding bit constant.
     *
     * @param className   the name of one of the hard-wired pragma exception classes
     * @return  the constant corresponding to <code>className</code> or 0 if
     *          <code>className</code> does not denote a pragma
     */
    public static int toModifier(String className) {
        if (className.equals("com.sun.squawk.pragma.HostedPragma")) {
            return HOSTED;
        } else if (className.equals("com.sun.squawk.pragma.ReplacementConstructorPragma")) {
            return REPLACEMENT_CONSTRUCTOR;
        } else if (className.equals("com.sun.squawk.pragma.InterpreterInvokedPragma")) {
            return INTERPRETER_INVOKED;
        } else if (className.equals("com.sun.squawk.pragma.NativePragma")) {
            return NATIVE;
        } else if (className.equals("com.sun.squawk.pragma.ForceInlinedPragma")) {
            return FORCE_INLINED;
        } else if (className.equals("com.sun.squawk.pragma.NotInlinedPragma")) {
            return NOT_INLINED;
        } else {
            return 0;
        }
    }
}
