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

class Base {
    static int x = 0;
    static int y = 0;
    static int z = 0;
}

class TestApp extends Base {
    // primitives
    static byte b = 5;
    static short s = 2;
    static long l = -1;

    // objects:k3
    static String str = "ABC";
    // objects:
    static Object obj = new Object();
    int[] intarray = {1, 2, 3};
    static String[] strarray = {"foo", "bar", "baz"};

    // primitive objects
    static Boolean booleanObj = new Boolean(true);
    static Byte byteObj = new Byte((byte)111);
    static Short shortObj = new Short((short)2222);
    static Character charObj = new Character('Z');
    static Integer intObj = new Integer(4444);
    static Long longObj = new Long(8888);

    static Class thisClass = TestApp.class;
    static Thread thisThread = Thread.currentThread();

    static void zorch1() {
        // zorchs should never appear on stack trace
    }

    static void f1(int i) {
        if (!(intObj instanceof Integer)) {
            throw new RuntimeException("inobj = " + intObj);
        }
        f2(i + 1);
    }

    static void zorch2() {
    }

    static void f2(int i) {
        x = 2; // create a different offset for each call.
        f3(i + 1);
    }

    static void zorch3() {
    }

    static void f3(int i) {
        x = 2;
        y = 2;
        f4(i + 1);
    }

    static void zorch4() {
    }

    static void f4(int i) {
        x = 2;
        y = 2;
        z = 2;
        f5(i + 1);
    }

    static void zorch5() {
    }

    static void f5(int i) {
        x += y;
        y += z;
        z += x+y;
        f6(i + 1);
        i = x;
        throw new RuntimeException("This is the expected exception in the TestApp.");
    }

    static void zorch6() {
    }

    static void f6(int i) {
        for (int j = 0; j < 2; j++) {
            int k = j;
        }

        int k = 2;
        while (k > 0) {
            k--;
        }

        int m = 2;
        do {
            m--;
        } while(m > 0);

        f7(i + 1);
    }

    static void f7(int i) {
        int j = 1;
        switch (j) {
            case 1:
                i = 1;
            case 2:
                i = 2;
                break;
            case 3:
                i = 3;
                break;

        }
        j = 4;

        f8(i + 1);
    }

    static void f8(int i) {
        for (int j = 0; j < 2; j++) {
            for (int k = 0; k < 2; k++) {
                i = i + 1;
            }
        }

        int j = 2;
        while (j > 0) {
            int k = 2;
            while (k > 0) {
                k--;
            }
            j--;
        }

        j = 2;
        while (j > 0) {
            for (int k = 0; k < 2; k++) {
                i = i + 1;
            }
            j--;
        }

        for (int l = 0; l < 2; l++) {
            int k = 2;
            while (k > 0) {
                k--;
            }
        }

        for (int l = 0; l < 2; l++) {
            int m = 2;
            do {
                m--;
            } while(m > 0);
        }

        f9(f9a(), f9b(), f9(f9a(), f9b(), 12));
        for (int p = 0; p < 2; p++) { }

    }

    static int f9(int i, int j, int k) {
        int l = i + j + k;
        return l;
    }

    static int f9a() {
        return 10;
    }

    static int f9b() {
        return 11;
    }

    static void mainLoop(boolean runExceptionThread) {
        int count = 0;
        System.err.println("Entering test loop...");

        while (true) {
            System.err.println("Loop iteration: " + count);
            try {
                f1(1);
            } catch (Exception e) {
                System.err.println("caught exception.");
            } finally {
                System.err.println("finally.");
            }
            count++;

            if (count % 5 == 0) {
                System.gc();

                if (runExceptionThread) {
                    new ExceptionThread().start();

                    System.out.println("for execution point in static initializer");
                }
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
            }
        }
    }

    static class ExceptionThread extends Thread {
        static int counter;
        public void run() {
            int j = StaticInitializer.value;
            for (int i = 0; i != 10; i++) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ex) {
                }
                System.err.println("spin " + counter + ": " + i);
            }
            counter++;
            throw new RuntimeException("uncaught exception");
        }
    }

//    public void run() {
//        mainLoop();
//    }

    public static void main(String[] args) {
        mainLoop(args.length == 0);
    }
} // TestIsolate

class StaticInitializer {
    static int showInitializer() {
        System.out.println("in StaticInitializer.<clinit>");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {

        }
        return 5;
    }
    static int value = showInitializer();
}
