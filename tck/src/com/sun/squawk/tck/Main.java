/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk.tck;

import java.io.*;
import java.util.*;
import javax.microedition.io.*;

import com.sun.squawk.util.ArgsUtilities;
import com.sun.squawk.VM;
import com.sun.squawk.Isolate;

/**
 * TCK test harness.
 */
class Main {

    /**
     * These are the TCK tests expected to fail and have yet to be fixed/investigated.
     */
    private static final String[] EXPECTED_TCK_FAILURES = {

        // These 3 tests (first two are negative, last one is positive) fail due to an
        // assumption that the VM being tested does not do eager class loading which
        // does not hold true for Squawk
        "javasoft.sqe.tests.vm.instr.getfield.getfield013.getfield01303m1.getfield01303m1_wrapper",
        "javasoft.sqe.tests.lang.expr631.expr63101.expr63101_wrapper",
        "javasoft.sqe.tests.vm.constantpool.defaultLoader.defaultLoader002.defaultLoader00201m1.defaultLoader00201_wrapper"

    };

    /**
     * The path to the TCK tests.
     */
    private String classPath = "tck" + VM.getFileSeparatorChar() + "tck.jar";

    /**
     * The prefix used for the log files.
     */
    private String prefix = "tck";

    /**
     * The URL of the print stream to which the stdout and stderr for each test is redirected.
     */
    String logURL;

    /**
     * The print stream to which the stdout and stderr for each test is redirected.
     */
    PrintStream log;

    /**
     * The print stream to which the passed tests are logged.
     */
    PrintStream passed;

    /**
     * The print stream to which the failed tests are logged.
     */
    PrintStream failed;

    /**
     * The print stream to which timed out tests are logged.
     */
    PrintStream timedout;

    int timeout = 120;

    /**
     * The collection of TCK tests. Each element in this vector is a string with one or
     * more tokens separated by whitespaces. The first token is the wrapper class for a
     * TCK tests (i.e. the class with the 'main' method that runs the test). Any remaining
     * tokens are command line parameters that will be passed to the 'main' method in the
     * wrapper class.
     */
    private Vector tests = new Vector();

    /**
     * Flags whether or not the set of tests being run are positive or negative TCK tests.
     * Positive tests are expected to exit the VM with an exit code of 95 and negative tests
     * are expected to exit the VM with an exit code of anything but 95.
     */
    private boolean isPositive = true;


    /**
     * Prints the usage message.
     *
     * @param  errMsg  an optional error message
     */
    private void usage(String errMsg) {
        PrintStream out = System.out;
        if (errMsg != null) {
            out.println(errMsg);
        }
        out.println("Usage: TCK [-options] tests...");
        out.println("where options include:");
        out.println();
        out.println("    -cp:<directories and jar/zip files separated by '" + VM.getPathSeparatorChar() + "'>");
        out.println("               paths where TCK classes and resources can be found (default='" + classPath + "')");
        out.println("    -o:<name>  prefix to use for log files (default='" + prefix + "')");
        out.println("    -p         specifies that the tests are positive (default)");
        out.println("    -n         specifies that the tests are negative");
        out.println("    -t:<secs>  timeout after which test is killed (default=120)");
        out.println();
    }

    /**
     * Parses the command line arguments.
     *
     * @param args  the command line arguments
     * @return true if the arguments were well formed and this TCK harness is now ready for execution.
     */
    boolean parseArgs(String[] args) throws IOException {
        int argc = 0;
        while (argc != args.length) {
            String arg = args[argc];
            if (arg.charAt(0) != '-') {
                break;
            } else if (arg.startsWith("-cp:")) {
                classPath = arg.substring("-cp:".length());
            } else if (arg.startsWith("-t:")) {
                timeout = Integer.parseInt(arg.substring("-t:".length()));
            } else if (arg.startsWith("-o:")) {
                prefix = arg.substring("-o:".length());
            } else if (arg.equals("-p")) {
                isPositive = true;
            } else if (arg.equals("-n")) {
                isPositive = false;
            } else {
                usage("Invalid option: " + arg);
                return false;
            }

            ++argc;
        }

        if (argc == args.length) {
            usage("no TCK tests specified");
            return false;
        }

        while (argc != args.length) {
            Vector resources = new Vector();
            String arg = args[argc++];
            ArgsUtilities.processClassArg(arg, tests, resources);
        }

        // Initialize the logging streams
        logURL = "file://" + prefix + ".output.log;append=true";
        log = new PrintStream(Connector.openOutputStream(logURL));
        passed = new PrintStream(Connector.openOutputStream("file://" + prefix + ".passed.log"));
        failed = new PrintStream(Connector.openOutputStream("file://" + prefix + ".failed.log"));
        timedout = new PrintStream(Connector.openOutputStream("file://" + prefix + ".timedout.log"));

        return true;
    }

    /**
     * Creates the suite containing all the classes in "tck/agent.jar". These classes
     * provide the framework for running a TCK test. Upon returning, the suite will
     * be accessible via the URL "File://agent.suite".
     */
    private void createAgentSuite() {
        Isolate isolate = new Isolate("com.sun.squawk.SuiteCreator", new String[] { "-cp:tck/agent.jar", "-strip:l", prefix + ".agent" }, null, null);
        isolate.start();
        isolate.join();
        if (isolate.getExitCode() != 0) {
            throw new RuntimeException("SuiteCreator exited with exit code " + isolate.getExitCode());
        }
    }

    /**
     * Test to see if a TCK class is expected to fail.
     *
     * @param mainClassName the main class of the test
     * @return true if it should fail
     */
    private static boolean shouldFail(String mainClassName, String[] testsNotToRun) {
        for (int i = 0 ; i < testsNotToRun.length ; i++) {
            if (testsNotToRun[i].equals(mainClassName)) {
                return true;
            }
        }
        return false;
    }

    private static final String[] NO_ARGS = {};



    /**
     * Runs a single TCK test.
     *
     * @param test   the main class and arguments of the test to run
     * @param testNo the number of the test
     * @return true if the test passed, false otherwise
     */
    boolean runOneTest(String test, int testNo) {
        String[] nameAndArgs = ArgsUtilities.cut(test);
        String[] args = NO_ARGS;
        String mainClassName = nameAndArgs[0];

        if (nameAndArgs.length != 1) {
            args = new String[nameAndArgs.length - 1];
            System.arraycopy(nameAndArgs, 1, args, 0, args.length);
        }

        log.println("" + new Date() + ": " + testNo + "/" + tests.size() + ": " + test);

        /*
         * Run the TCK test in its own isolate.
         */
        final Isolate isolate = new Isolate(mainClassName, args, classPath, "file://" +prefix + ".agent.suite");
        isolate.setProperty("java.lang.System.out", logURL + ";append=true");
        isolate.setProperty("java.lang.System.err", logURL + ";append=true");

        isolate.start();
        new Thread() {
            public void run() {
                long start = System.currentTimeMillis();
                try {
                    while (isolate.isAlive() && (System.currentTimeMillis() - start) < (timeout * 1000)) {
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException ex) {
                }
                if (isolate.isAlive()) {
                    log.println("" + new Date() + ": killing timed out test");
                    isolate.exit(-42);
                    isolate.join();
                    try {
                        String url = "file://" + isolate.getMainClassName() + ".isolate";
                        DataOutputStream dos = Connector.openDataOutputStream(url);
                        isolate.save(dos, url, true);
                        dos.close();
                        log.println("" + new Date() + ": saved timed out test to " + url);
                    } catch (IOException e) {
                        log.println("" + new Date() + ": could not save timed out test: " + e);
                    }
                }
            }
        }.start();

        isolate.join();

        int exitCode = isolate.getExitCode();
        log.println("" + new Date() + ": " + testNo + "/" + tests.size() + ": exited [exit code = " + exitCode + "]");

        boolean pass;
        if (exitCode == -42) {
            timedout.println(mainClassName);
            pass = true;
        } else {

            pass = isPositive ? exitCode == 95 : exitCode == 97;
            if (pass) {
                passed.println(mainClassName);
            } else {
                failed.println(mainClassName);
            }
        }

        log.flush();
        passed.flush();
        failed.flush();
        timedout.flush();

        boolean shouldFail = shouldFail(mainClassName, EXPECTED_TCK_FAILURES);

        if (!pass && shouldFail) {
            return true;
        }

        if (pass && shouldFail) {
            log.println(mainClassName + " Was expected to fail");
        }

        return pass;
    }

    /**
     * Runs the set of TCK tests, logging the results to the appropriate log streams.
     *
     * @return the number of tests that failed
     * @throws IOException if there was an IO error
     */
    int run() throws IOException {

        int failedCount = 0;
        int testNo = 1;
        long start = System.currentTimeMillis();
        for (Enumeration e = tests.elements(); e.hasMoreElements(); ) {
            String test = (String)e.nextElement();
            if (!runOneTest(test, testNo++)) {
                ++failedCount;
            }
        }
        log.println("Total time: " + (System.currentTimeMillis() - start) + "ms");

        log.close();
        passed.close();
        failed.close();
        timedout.close();
    
        return failedCount;
    }

    /**
     * Entry point for running the TCK tests. This method always exits via {@link System.exit(int)}.
     * If my exitCode is less than 0, then some kind of unexpected exception occured.
     * Otherwise returns count of failed tests.
     *
     * @param args   comman line arguments as detailed by {@link usage(String)}.
     * @throws IOException if an IO error occurs
     */
    public static void main(String[] args) {
        Main tck = new Main();
        try {
            if (tck.parseArgs(args)) {
                tck.createAgentSuite();
                System.exit(tck.run());
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        System.exit(-1);
    }
}

