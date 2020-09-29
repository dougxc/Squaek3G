/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk.regression;

import java.io.*;
import java.util.*;
import javax.microedition.io.*;

import com.sun.squawk.VM;
import com.sun.squawk.Isolate;
import com.sun.squawk.util.StringTokenizer;
import com.sun.squawk.util.ArgsUtilities;
import com.sun.squawk.util.LineReader;

/**
 * This is the harness for running a number of applications on Squawk as
 * a set of regression tests.
 */

public class Main {

    static class Test {

        /**
         * A name for this test.
         */
        final String name;

        /**
         * The main class of the application.
         */
        final String mainClass;

        /**
         * The command line arguments to pass to the application.
         */
        String[] args = {};

        /**
         * The class path to use.
         */
        String classPath;

        /**
         * The milliseconds to sleep before hibernating/exiting the test
         * or 0 if the test should be run to normal completion.
         */
        int time;

        /**
         * The exit code of the test if it is successful.
         */
        int exitCodeExpected;

        /**
         * Indicate whether the exit code has passed and failed counts encoded in it.
         */
        boolean exitCodeIncludesCounts = false;

        /**
         * The exitCode resulting from running this test.
         */
        int exitCode;
        
        Test(String name, String mainClass) {
            this.name = name;
            this.mainClass = mainClass;
        }

        public String toString() {
            StringBuffer buf = new StringBuffer(mainClass);
            for (int i = 0; i != args.length; ++i) {
                buf.append(' ').append(args[i]);
            }
            return buf.toString();
        }
    }

    /**
     * The stream used to log regression test status and results.
     */
    private PrintStream out;

    /**
     * The prefix to use for output files.
     */
    private String prefix = "";

    /**
     * Runs a given isolate for some duration that is measured in the number of yields
     * this thread should perform. Once the duration is completed, the isolate is hibernated
     * or exited.
     *
     * @param isolate     the isolate to run
     * @param delay       the milliseconds this thread should sleep before hibernating/exiting the isolate
     * @param hibernate   if true, the isolate is hibernated, otherwise it is exited
     */
    private void runForSometime(Isolate isolate, int delay, boolean hibernate) throws IOException {
        try {
            while (delay > 0) {
                int sleep = Math.min(delay, 5000);
                if (VM.isVeryVerbose()) {
                    VM.println("[regression: running " + isolate + " for another " + (delay/1000) + " seconds]");
                }
                Thread.sleep(sleep);
                delay = delay - sleep;
            }
        } catch (InterruptedException ie) {
        }

        if (isolate.isAlive()) {
            if (hibernate) {
                if (VM.isVeryVerbose()) {
                    VM.println("[regression: hibernating " + isolate + "]");
                }
                int attempts = 10;
                while (true) {
                    try {
                        isolate.hibernate();
                        break;
                    } catch (IOException ex) {
                        if (--attempts == 0) {
                            throw ex;
                        }
                        // Let the isolate run a bit more
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ie) {
                        }
                        if (VM.isVeryVerbose()) {
                            VM.println("[failed to hibernate (trying again): " + ex + "]");
                        }
                    }
                }
            } else {
                if (VM.isVeryVerbose()) {
                    VM.println("[regression: stopping " + isolate + "]");
                }
                isolate.exit(0);
            }
        } else {
            if (hibernate) {
                throw new RuntimeException(isolate + " stopped before it could be hibernated");
            }
        }
    }

    /**
     * Runs a given regression test in its own isolate.  Put the resulting exitCode into test.exitCode
     */
    void run0(Test test) {
        String cp = test.classPath == null ? null : ArgsUtilities.toPlatformPath(test.classPath, true);
        Isolate isolate = new Isolate(test.mainClass, test.args, cp, null);

        out.println(test + ":");

        long start = System.currentTimeMillis();
        Throwable exception = null;

        try {
            isolate.start();

            boolean hibernate = (test.time != 0);
            if (hibernate) {
                runForSometime(isolate, test.time, true);
            }

            isolate.join();

            // If the isolate was hibernated, make sure it can be saved and resumed
            if (isolate.isHibernated()) {
                String url = "file://" + isolate.getMainClassName() + "." + System.currentTimeMillis() + ".isolate";
                DataOutputStream dos = Connector.openDataOutputStream(url);
                isolate.save(dos, url, true);
                if (VM.isVeryVerbose()) {
                    VM.println("[regression: loading and unhibernating " + url + "]");
                }
                DataInputStream dis = Connector.openDataInputStream(url);
                isolate = Isolate.load(dis, url);
                dis.close();
                isolate.unhibernate();

                // Let it run again
                runForSometime(isolate, test.time, false);
                isolate.join();
            }
        } catch (Throwable t) {
            exception = t;
            out.println(exception.toString());
            exception.printStackTrace();
        }

        int exitCode = isolate.getExitCode();
        out.print("    exit code = " + exitCode);
        if (!test.exitCodeIncludesCounts) {
            out.print("(expected exit code = " + test.exitCodeExpected + ")");
        }
        out.println();
        out.println("    time = "  + (System.currentTimeMillis() - start) + "ms");
        if (exception != null) {
            exitCode = -1;
        }

        test.exitCode = exitCode;
    }

    /**
     * Execute test and update its exitCode to reflect the exit code returned from running test.
     * @param test
     */
    void run(Test test) {

        System.out.println("[running regression test: " + test.mainClass + "]");
        System.gc();
        long freeBefore = Runtime.getRuntime().freeMemory();
        run0(test);
        System.gc();
        long freeAfter = Runtime.getRuntime().freeMemory();
        if (freeAfter > freeBefore + 1000) {
            VM.println("******* Memory appears to have leaked after running " + test.mainClass + " [before="+freeBefore+", after="+freeAfter+"] *******");
        }
    }

    int run(Vector tests) {

        int failed = 0;

        for (Enumeration e = tests.elements(); e.hasMoreElements(); ) {
            Test test = (Test)e.nextElement();
            run(test);
            if (test.exitCodeIncludesCounts && (test.exitCode >= 0)) {
                failed += test.exitCode;
            } else {
                boolean pass = test.exitCodeExpected == test.exitCode;
                if (!pass) {
                    failed++;
                }
            }
        }
        out.close();
        return failed;
    }

    private static void usage(String errMsg) {
        PrintStream out = System.err;
        if (errMsg != null) {
            out.println("** " + errMsg + " **\n");
        }
        out.println("Usage: com.sun.squawk.regression.Main [-options] test_spec_files...");
        out.println();
        out.println("where options include:");
        out.println("    -p:<name> prefix to append to output files");
        out.println("    -h        display help message and exit");
        out.println();
    }


    private Vector parseArgs(String[] args) throws IOException {
        prefix = "";

        int argc = 0;
        while (argc != args.length) {
            String arg = args[argc];
            if (arg.charAt(0) != '-') {
                break;
            } else if (arg.startsWith("-p:")) {
                prefix = arg.substring("-p:".length());
            } else if (arg.equals("-h")) {
                usage(null);
                return null;
            } else {
                usage("Unknown option: " + arg);
                return null;
            }
            ++argc;
        }

        if (argc == args.length) {
            usage("missing spec file");
            return null;
        }

        Vector tests = new Vector();
        while (argc != args.length) {
            if (!parseSpec(args[argc++], tests)) {
                return null;
            }
        }

        return tests;
    }

    private static void logSpecFileError(String specFilePath, int lineNo, String msg) {
        System.err.println("parse error in " + specFilePath + ", line " + lineNo + ": " + msg);
    }

    /**
     * Parses a file containing the specification of the tests to be run.
     *
     * @param specFilePath
     * @param tests
     * @return
     */
    private boolean parseSpec(String specFilePath, Vector tests) {
        Hashtable properties = new Hashtable();

        try {
            Reader specFileReader = new InputStreamReader(Connector.
                openInputStream("file://" + specFilePath));
            LineReader lr = new LineReader(specFileReader);
            String line = lr.readLine();
            int lineNo = 0;
            while (line != null) {
                lineNo++;
                line = line.trim();

                // Skip comment and blank lines
                if (line.length() > 0 && line.charAt(0) != '#') {

                    int equals = line.indexOf('=');
                    if (equals == -1) {
                        logSpecFileError(specFilePath, lineNo, "missing '=' character");
                        return false;
                    }

                    final String key = line.substring(0, equals).trim();
                    String value = line.substring(equals + 1).trim();

                    if (properties.containsKey(key)) {
                        logSpecFileError(specFilePath, lineNo, "repeated property: '" + key + "'");
                        return false;
                    }

                    // To catch duplicate properties
                    properties.put(key, value);

                    int dot = key.indexOf('.');
                    if (dot == -1) {
                        logSpecFileError(specFilePath, lineNo, "property key is missing '.' character: '" + key + "'");
                        return false;
                    }

                    String name = key.substring(0, dot);
                    String property = key.substring(dot + 1);
                    Test test = (Test) properties.get(name);

                    if (property.equals("main")) {
                        test = new Test(name, value);
                        properties.put(name, test);
                        tests.addElement(test);
                    } else {
                        if (test == null) {
                            logSpecFileError(specFilePath, lineNo, "'main' property must be first for each test");
                            return false;
                        }

                        // Substitute value for ${prefix} in value
                        int macro = value.indexOf("${prefix}");
                        if (macro != -1) {
                            value = value.substring(0, macro) + prefix + value.substring(macro + "${prefix}".length());
                        }

                        try {
                            if (property.equals("time")) {
                                test.time = Integer.parseInt(value) * 1000;
                            } else if (property.equals("classpath")) {
                                test.classPath = value;
                            } else if (property.equals("exit")) {
                                test.exitCodeExpected = Integer.parseInt(value);
                            } else if (property.equals("args")) {
                                StringTokenizer st = new StringTokenizer(value);
                                test.args = new String[st.countTokens()];
                                for (int i = 0; i != test.args.length; ++i) {
                                    test.args[i] = st.nextToken();
                                }
                            } else if (property.equals("exitIncludesCounts")) {
                                test.exitCodeIncludesCounts = value.equals("true");
                            } else {
                                logSpecFileError(specFilePath, lineNo, "unrecognized property: '" + property + "'");
                                return false;
                            }
                        } catch (NumberFormatException e) {
                            logSpecFileError(specFilePath, lineNo, "invalid integer property: '" + property + "'");
                            return false;
                        }
                    }
                }
                line = lr.readLine();
            }

            specFileReader.close();
        } catch (ConnectionNotFoundException e) {
            System.err.println("Could not open " + specFilePath);
            return false;
        } catch (IOException e) {
            System.err.println("IO error while reading " + specFilePath);
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * If my exitCode is less than 0, then some kind of unexpected exception occured.
     * Otherwise returns count of failed tests.
     *
     * @param args
     */
    public static void main(String[] args) {
        int exitCode;

        try {
            Main instance = new Main();
            Vector tests = instance.parseArgs(args);
            if (tests != null) {
                instance.out = new PrintStream(Connector.openOutputStream("file://" + instance.prefix + "regression.log"));
                exitCode = instance.run(tests);
            } else {
                exitCode = -1;
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            exitCode = -1;
        }
        System.exit(exitCode);
    }
}
