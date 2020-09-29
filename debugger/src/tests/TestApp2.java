//if[!FLASH_MEMORY]
/*
 * Copyright 2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package tests;

public class TestApp2 implements Runnable {

    static void f1() {
        try {
            throw new RuntimeException("This is the expected exception in the TestApp.");
        } catch (Exception e) {
            System.err.println("PASSED: caught exception in f1.");
        }
    }

    static void mainLoop() {
        try {
            f1();
        } catch (Exception e) {
            System.err.println("FAILED: caught exception in main");
        }

    }

    public void run() {
        mainLoop();
    }

    public static void main(String[] args) {
        mainLoop();
    }
} // TestApp2
