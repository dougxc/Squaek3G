/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: ArmTests.java,v 1.4 2005/02/03 00:56:40 dl156546 Exp $
 */

package com.sun.squawk.compiler.asm.arm.tests;

import com.sun.squawk.compiler.asm.*;
import junit.framework.*;

/**
 * Test suite for all of the ARM assembler instruction tests. These tests can be run using the
 * builder:
 * <pre>
 *     java -jar build.jar arm_asm_tests
 * </pre>
 *
 * @author David Liu
 * @version 1.0
 */
public class ArmTests {
    private ArmTests() {
    }

    public static void main (String[] args) {
        junit.textui.TestRunner.run (suite());
    }

    public static junit.framework.Test suite ( ) {
        TestSuite suite = new TestSuite("All JUnit Tests For ARM Assembler");
        suite.addTest(AdcTest.suite());
        suite.addTest(AddTest.suite());
        suite.addTest(AddrPatchingTest.suite());
        suite.addTest(AndTest.suite());
        suite.addTest(BTest.suite());
        suite.addTest(BicTest.suite());
        suite.addTest(BkptTest.suite());
        suite.addTest(ClzTest.suite());
        suite.addTest(CmnTest.suite());
        suite.addTest(CmpTest.suite());
        suite.addTest(EorTest.suite());
        suite.addTest(LdmTest.suite());
        suite.addTest(LdrTest.suite());
        suite.addTest(MlaTest.suite());
        suite.addTest(MovTest.suite());
        suite.addTest(MrsTest.suite());
        suite.addTest(MsrTest.suite());
        suite.addTest(MvnTest.suite());
        suite.addTest(MulTest.suite());
        suite.addTest(NopTest.suite());
        suite.addTest(OrrTest.suite());
        suite.addTest(RelocatorTest.suite());
        suite.addTest(RsbTest.suite());
        suite.addTest(RscTest.suite());
        suite.addTest(SbcTest.suite());
        suite.addTest(SmlalTest.suite());
        suite.addTest(SmullTest.suite());
        suite.addTest(StmTest.suite());
        suite.addTest(StrTest.suite());
        suite.addTest(SubTest.suite());
        suite.addTest(SwiTest.suite());
        suite.addTest(SwpTest.suite());
        suite.addTest(TeqTest.suite());
        suite.addTest(TstTest.suite());
        suite.addTest(UmlalTest.suite());
        suite.addTest(UmullTest.suite());
        return suite;
    }

    /**
     * Compares the assembler's code buffer with the expected output.  If there is a mismatch, both
     * the expected and actual code will be printed to System.err in hex.
     *
     * @param buffer the assembler's code buffer
     * @param expectedCode the expected code
     * @return whether the actual and expected code buffers are identical
     */
    public static boolean compareCode(CodeBuffer buffer, int [] expectedCode) {
        final int codeLength = buffer.getCodeSize();
        if (codeLength != expectedCode.length) {
            System.err.println("Code length mismatch: expected " + expectedCode.length + ", actual: " + codeLength);
            System.err.flush();
            return false;
        }

        if (codeLength == expectedCode.length) {
            byte [] code = buffer.getBytes();
            for (int i = 0; i < codeLength; i++) {
                if (code[i] != (byte) expectedCode[i]) {
                    System.err.println("Code mismatch:");

                    System.err.print ("Expected: 0x");
                    for (int j = 0; j < expectedCode.length; j++) {
                        if (j > 0 && j % 4 == 0) System.err.print (" ");
                        System.err.print (Integer.toHexString((expectedCode [j] & 0xf0) >>> 4));
                        System.err.print (Integer.toHexString(expectedCode [j] & 0xf));
                    }
                    System.err.println ("");

                    System.err.print ("Actual:   0x");
                    for (int j = 0; j < expectedCode.length; j++) {
                        if (j > 0 && j % 4 == 0) System.err.print (" ");
                        System.err.print (Integer.toHexString((code [j] & 0xf0) >>> 4));
                        System.err.print (Integer.toHexString(code [j] & 0xf));
                    }
                    System.err.println ("");

                    System.err.flush();

                    return false;
                }
            }
        }

        return true;
    }
}
