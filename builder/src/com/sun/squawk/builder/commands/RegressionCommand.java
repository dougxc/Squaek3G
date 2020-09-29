/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: RegressionCommand.java,v 1.29 2006/02/08 04:44:13 ea149956 Exp $
 */
package com.sun.squawk.builder.commands;

import com.sun.squawk.builder.*;

import java.io.*;
import java.text.*;
import java.util.*;
import java.net.*;
import com.sun.squawk.builder.util.FileSet;

/**
 * The regression test harness.
 *
 * @author  Doug Simon
 */
public class RegressionCommand extends Command {

    public RegressionCommand(Build env) {
        super(env, "regression");
    }

    /**
     * The object used to generate the regression report.
     */
    private ReportWriter out;

    /**
     * The base class for generating the report of a regression test run.
     */
    public static abstract class ReportWriter {

        /**
         * The underlying output stream.
         */
        final PrintWriter out;

        /**
         * Creates a ReportWriter that writes to a given stream.
         *
         * @param out   the output stream
         */
        public ReportWriter(PrintWriter out) {
            this.out = out;
        }

        /**
         * Reports the start of a regression test suite.
         *
         * @param hostName   the name of the host running the tests
         * @param date       the date at which the tests were started
         */
        public abstract void startRegression(String hostName, Date date);

        /**
         * Reports the end of a regression test suite.
         */
        public void endRegression() {}

        /**
         * Includes the output of the last started command into this report.
         *
         * @param is     the content to include (up to EOF)
         * @param source a description of the source (e.g. file name) - may be null
         */
        public abstract void includeOutput(InputStream is, String source);

        /**
         * Reports that a command is about to be run.
         *
         * @param commandLine  the command line being executed
         */
        public abstract void startCommand(String commandLine);

        /**
         * Reports that the last started command is complete.
         */
        public void endCommand() {}

        /**
         * Reports the start of a logically grouped set of commands.
         *
         * @param name  the name of the grouping
         */
        public abstract void startSection(String name);

        /**
         * Reports the end of a logically grouped set of commands.
         */
        public void endSection() {}

        /**
         * Includes a file (verbatim) into this report.
         *
         * @param file  the file to include
         */
        public void includeFile(File file) {
            try {
                FileReader in = new FileReader(file);
                int ch;
                while ((ch = in.read()) != -1) {
                    out.write(ch);
                }
            } catch (IOException e) {
                throw new BuildException("error including '" + file + "' into report", e);
            }
        }

        /**
         * Starts the test summary.
         *
         * @param hostName   the name of the host running the tests
         * @param platform   the test platform (OS-arch)
         * @param start      the starting time of the tests
         * @param finish     the finish time of the tests
         */
        public abstract void startSummary(String hostName, String arch, Date start, Date finish);

        /**
         * Writes the result for a group of tests.
         *
         * @param name    the group's name
         * @param exitCode  status of success
         */
        public abstract void writeSummaryResult(String name, int exitCode);

        /**
         * Ends the test summary.
         */
        public void endSummary() {}

        /**
         * Closes the report. This closes the underling output stream.
         */
        public void close() {
            out.close();
        }
    }

    public static class TextReportWriter extends ReportWriter {

        public TextReportWriter(PrintWriter out) {
            super(out);
        }

        /**
         * {@inheritDoc}
         */
        public void includeOutput(InputStream is, String source) {
            if (source != null) {
                out.println("++++ " + source + " ++++");
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            try {
                while ((line = br.readLine()) != null) {
                    out.println(line);
                }
            } catch (IOException ioe) {
                throw new BuildException("error appending outout to report", ioe);
            }
            if (source != null) {
                out.println("---- " + source + " ----");
            }
        }

        /**
         * {@inheritDoc}
         */
        public void startCommand(String command) {
            out.println();
            out.println(command + ":");
            out.println();
        }

        /**
         * {@inheritDoc}
         */
        public void startSection(String name) {
            out.println();
            out.println("++++++++ " + name + " ++++++++");
            out.println();
        }

        /**
         * {@inheritDoc}
         */
        public void startRegression(String hostName, Date date) {
            String title = "Squawk Regression Tests - " + hostName + " - " + date;
            out.println(title);
            out.println();
        }

        /**
         * {@inheritDoc}
         */
        public void startSummary(String hostName, String platform, Date start, Date finish) {
            out.println("Host:           " + hostName);
            out.println("Platform:       " + platform);
            out.println("Start:          " + start);
            out.println("Finish:         " + finish);
        }

        /**
         * {@inheritDoc}
         */
        public void writeSummaryResult(String name, int exitCode) {
            out.print(name + ":");
            for (int i = name.length() + 1; i < 16; ++i) {
                out.print(' ');
            }
            String message;
            if (exitCode < 0) {
                message = "failed due to exception";
            } else if (exitCode == 0) {
                message = "passed";
            } else {
                message = "failed due to " + exitCode + " failed tests";
            }
            out.println(message);
        }
    }

    public static class HTMLReportWriter extends ReportWriter {

        public HTMLReportWriter(PrintWriter out) {
            super(out);
        }

        /**
         * {@inheritDoc}
         */
        public void includeOutput(InputStream is, String source) {
            if (source != null) {
                out.println("<p>Contents of '<b>" + encode(source) + "</b>'</p>");
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            out.println("<table border=\"1\" cellspacing=\"0\" cellpadding=\"5\" width=\"100%\" bgcolor=\"#CCCCCC\"><tr><td><pre><code>");
            String line;
            try {
                while ((line = br.readLine()) != null) {
                    out.println(line);
                }
            } catch (IOException ioe) {
                throw new BuildException("error appending outout to report", ioe);
            }
            out.println("</code></pre></td></tr></table>");
            out.flush();
        }

        /**
         * {@inheritDoc}
         */
        public void startCommand(String command) {
            out.println("<hr />");
            out.println("<p><b>" + encode(command) + "</b></p>");
            out.flush();
        }

        /**
         * {@inheritDoc}
         */
        public void startSection(String name) {
            out.println();
            out.println("<h2> " + name + " </h2>");
            out.println();
            out.flush();
        }

        /**
         * {@inheritDoc}
         */
        public void endSection(String name) {
            out.println();
        }

        /**
         * {@inheritDoc}
         */
        public void startSummary(String hostName, String platform, Date start, Date finish) {
            out.println("<p><table border=\"1\">");
            out.println("<tr><td>Host</td><td>" + hostName + "</td></tr>");
            out.println("<tr><td>Platform</td><td>" + platform + "</td></tr>");
            out.println("<tr><td>Start</td><td>" + start + "</td></tr>");
            out.println("<tr><td>Finish</td><td>" + finish + "</td></tr>");
        }

        /**
         * {@inheritDoc}
         */
        public void writeSummaryResult(String name, int exitCode) {
            String message;
            String color;
            if (exitCode < 0) {
                message = "failed due to exception";
                color = "red";
            } else if (exitCode == 0) {
                message = "passed";
                color = "green";
            } else {
                message = " failed due to " + exitCode + " failed tests";
                color = "red";
            }
            out.println("<tr><td>" + name + "</td><td>" + "<span style=\"color:" + color + "\">" + message + "</span>" + "</td></tr>");
        }

        /**
         * {@inheritDoc}
         */
        public void endSummary() {
            out.println("</table></p>");

        }

        /**
         * {@inheritDoc}
         */
        private static String encode(String s) {
            StringBuffer buf = new StringBuffer(s.length() * 2);
            for (int i = 0 ; i < s.length() ; i++) {
                char ch = s.charAt(i);
                if (ch < ' ' || ch >= 0x7F || ch == '<' || ch == '>' || ch == '&' || ch == '"') {
                    buf.append("&#");
                    buf.append((int)ch);
                    buf.append(';');
                } else {
                    buf.append(ch);
                }
            }
            return buf.toString();
        }

        /**
         * {@inheritDoc}
         */
        public void startRegression(String hostName, Date date) {
            String title = "Squawk Regression Tests - " + hostName + " - " + date;
            out.println("<html>");
            out.println("<head>");
            out.println("<title>" + title + "</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("<p><h1>" + title + "</h1></p>");
        }

        /**
         * {@inheritDoc}
         */
        public void endRegression() {
            out.println("</body>");
            out.println("</html>");

        }
    }

    /**
     * Runs a task, redirecting the output to {@link #out}.
     *
     * @param task  the task to run
     */
    private void run(String command, Runnable task) {

        out.startCommand(command);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);

        PrintStream systemOut = System.out;
        PrintStream systemErr = System.err;

        System.setOut(ps);
        System.setErr(ps);

        try {
            task.run();
        } finally {
            System.setOut(systemOut);
            System.setErr(systemErr);
            out.includeOutput(new ByteArrayInputStream(baos.toByteArray()), null);
        }
    }

    /**
     * Runs a command in a new instance of Build.
     *
     * @param commandLine   the builder command line to execute
     */
    private void builder(String commandLine) {

        // Pass through the command line options from the current builder
        List passThroughArgs = env.getBuilderArgs();
        for (Iterator iterator = passThroughArgs.iterator(); iterator.hasNext();) {
            commandLine = (String)iterator.next() + " " + commandLine;
        }

        StringTokenizer st = new StringTokenizer(commandLine);
        final String[] args = new String[st.countTokens()];
        for (int i = 0; i != args.length; ++i) {
            args[i] = st.nextToken();
        }


        String command = "java -jar build.jar " + commandLine;
        run(command, new Runnable() {
            public void run() {
                new Build().mainProgrammatic(args);
            }
        });
    }

    /**
     * Executes a subproccess in the context of the current builder.
     *
     * @param command  the command to execute
     * @param args     the command's arguments
     */
    private void exec(String command, String args) {
        final String commandWithArgs = command + env.getPlatform().getExecutableExtension() + " " + args;
        run(commandWithArgs, new Runnable() {
            public void run() {
                env.exec(commandWithArgs);
            }
        });
    }

    /**
     * Usage message.
     *
     * @param errMsg   optional error message
     * @param out      where to write the usage message
     */
    public void usage(String errMsg, PrintStream out) {
        if (errMsg != null) {
            out.println(errMsg);
        }
        out.println("usage: regression [-options]");
        out.println("where options include:");
        out.println("    -production[:tck]   build and run regression in production/optimized mode,");
        out.println("                        including the TCK tests if ':tck' is specified");
        out.println("    -debug[:tck]        build and run regression in production/optimized mode,");
        out.println("                        including the TCK tests if ':tck' is specified");
        out.println("    -cldc1.0            builds and test as a CLDC 1.0 system, default is 1.1");
        out.println("    -cldc1.1            builds and test as a CLDC 1.1 system");
        out.println("    -html               format regression output and report as a HTML page");
        out.println("    -nosamples     exclude the sample tests");
        out.println("    -h                  show this help message and exit");
        out.println("");
    }

    /**
     * {@inheritDoc}
     */
    public String getDescription() {
        return "runs the regression tests";
    }

    /**
     * Gets the name of the current host.
     *
     * @return  the host's name
     */
    private String getHostName() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw new BuildException("could not get local host name", e);
        }
    }

    private boolean hasNativeGC() {
        return env.getProperty("GC").indexOf("Lisp2") != -1 && env.getBooleanProperty("VM2C");
    }

    /**
     * Builds and runs the regression tests in production/optimized mode.
     *
     * @param cldc11 build and test as CLDC 1.1 implementation
     * @param tck    specifies if the TCK tests should be included
     * @return 0 if the tests all executed successfully. {@link com.sun.squawk.regression.Main#encodePassedFailureCounts(int, int)}
     */
    private int production(String cldcProps, String regressionSpecs) {
        try {
            String name = "Production regression tests";
            out.startSection(name);
            String nativeGC = hasNativeGC() ? "-nativegc " : "";

            builder("clean");
            builder(cldcProps);
            builder("-DTYPEMAP=false " + cldcProps + " -prod -o2 -mac rom j2me translator imp graphics tck");
            exec("squawk", nativeGC + "-version");
            exec("squawk", (env.verbose ? "-veryverbose " : "") + nativeGC + "-stats -J-Djava.awt.headless=true com.sun.squawk.regression.Main -p:production. " + regressionSpecs);

            out.endSection();

            return 0;
        } catch (BuildException e) {
            appendThrowable(e);
            appendLogFiles("production.");
            return e.exitValue;
        }
    }

    /**
     * Builds and runs the regression tests in debug mode.
     *
     * @param cldc11 build and test as CLDC 1.1 implementation
     * @param tck    specifies if the TCK tests should be included
     * @return 0 if the tests all executed successfully. {@link com.sun.squawk.regression.Main#encodePassedFailureCounts(int, int)}
     */
    private int debug(String cldcProps, String regressionSpecs) {
        try {
            String name = "Debug regression tests";
            out.startSection(name);
            String nativeGC = hasNativeGC() ? "-nativegc " : "";
            
            try {
                builder("clean");
            } catch (BuildException e) {
                // TODO It seems that on Windows running a production followed by a debug will cause the j2me/classes.jar not being
                // able to delete it ?  For now ignore and go on.
                appendThrowable(e);
            }
            String commandLine = "-DTYPEMAP=true -DASSERTIONS_ENABLED=true -DDEBUG_CODE_ENABLED=true -DTRACING_ENABLED=true " + cldcProps;
            builder(commandLine);
            commandLine = "-DTYPEMAP=true " + cldcProps + " -typemap -assume -tracing rom -o:squawk_g -exclude:squawk.exclude j2me translator imp graphics tck";
            builder(commandLine);
            exec("squawk_g", nativeGC + "-Xboot:squawk_g.suite -version");
            exec("squawk_g", (env.verbose ? "-veryverbose " : "") + nativeGC + "-stats -Xboot:squawk_g.suite -J-Djava.awt.headless=true com.sun.squawk.regression.Main -p:debug. " + regressionSpecs);
            return 0;
        } catch (BuildException e) {
            appendThrowable(e);
            appendLogFiles("debug.");
            return e.exitValue;
        }
    }

    /**
     * Appends zero or more "*.log" files to the output.
     *
     * @param prefix   the prefix that the log files must match
     */
    private void appendLogFiles(String prefix) {
        FileSet logFiles = new FileSet(new File("."), new FileSet.PatternSelector(prefix + ".*\\.log", true));
        for (Iterator iterator = logFiles.list().iterator(); iterator.hasNext(); ) {
            File logFile = (File)iterator.next();
            try {
                out.includeOutput(new FileInputStream(logFile), logFile.toString());
            } catch (FileNotFoundException fnfe) {
                throw new BuildException("error including log file: " + logFile, fnfe);
            }
        }
    }

    /**
     * Append the stack trace to the output.
     * 
     * @param t
     */
    private void appendThrowable(Throwable t) {
        StringWriter writer = new StringWriter();
        t.printStackTrace(new PrintWriter(writer));
        String string = writer.toString();
        out.includeOutput(new ByteArrayInputStream(string.getBytes()), t.toString());
//        out.includeOutput(new StringBufferInputStream(string), t.toString());
    }
    
    /**
     * Opens a file for writing.
     *
     * @param file  the file to open
     * @return the writer to use
     */
    private PrintWriter openOutputFile(File file) {
        try {
            return new PrintWriter(new FileWriter(file));
        } catch (IOException e) {
            throw new BuildException("error opening output file '" + file.getAbsolutePath() + "': ", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void run(String[] args) {
        boolean production = false;
        boolean productionWithTck = false;
        boolean debugWithTck = false;
        boolean debug = false;
        boolean html = false;
        boolean cldc11 = true;
        // TODO Default withSamples to true for legacy reasons, fix later
        boolean withSamples = true;

        for (int i = 0; i != args.length; ++i) {
            String arg = args[i];
            if (arg.startsWith("-production")) {
                production = true;
                if (arg.equals("-production:tck")) {
                    productionWithTck = true;
                }
            } else if (arg.startsWith("-debug")) {
                debug = true;
                if (arg.equals("-debug:tck")) {
                    debugWithTck = true;
                }
            } else if (arg.equals("-cldc1.0")) {
                cldc11 = false;
            } else if (arg.equals("-cldc1.1")) {
                cldc11 = true;
            } else if (arg.equals("-html")) {
                html = true;
            } else if (arg.equals("-nosamples")) {
                withSamples = false;
            } else if (arg.equals("-h")) {
                usage(null, System.out);
                return;
            } else {
                usage(null, System.out);
                throw new BuildException("Unknown option: " + arg);
            }
        }

        Date start = new Date();
        String hostName = getHostName();

        // Run the tests and catch the output in a temp file
        File tempFile = new File ("regression.temp");
        PrintWriter pw = openOutputFile(tempFile);
        out = html ? (ReportWriter)new HTMLReportWriter(pw) : new TextReportWriter(pw);

        String regressionSpecs = "builder/regression-minimal.spec";
        if (withSamples) {
            regressionSpecs += " samples/regression.spec";
        }
        String tckTestSpecs;
        if (cldc11) {
            tckTestSpecs = "tck/regression-1.1.spec";
        } else {
            tckTestSpecs = "tck/regression.spec";
        }
        String cldcProps = cldc11 ? "-DFLOATS=true -DCLDC1.1=true" : "-DFLOATS=false -DCLDC1.1=false";
        if (false) {
            cldcProps += " -DSUITE_VERIFIER=true";
        }

        int productionResult;
        if (production) {
            productionResult = production(cldcProps, regressionSpecs + " " + (productionWithTck?tckTestSpecs:""));
        } else {
            productionResult = Integer.MIN_VALUE;
        }
        int debugResult;
        if (debug) {
            debugResult = debug(cldcProps, regressionSpecs + " " + (debugWithTck?tckTestSpecs:""));
        } else {
            debugResult = Integer.MIN_VALUE;
        }
        out.close();

        // Generate the report
        File reportFile = new File("regression_report." + (html ? "html" : "txt"));
        pw = openOutputFile(reportFile);
        out = html ? (ReportWriter)new HTMLReportWriter(pw) : new TextReportWriter(pw);
        out.startRegression(hostName, start);

        // Write the summary section
        out.startSection("Summary");
        out.startSummary(hostName, env.getPlatform().toString(), start, new Date());
        if (production) {
            out.writeSummaryResult("Production", productionResult);
        }
        if (debug) {
            out.writeSummaryResult("Debug", debugResult);
        }
        out.endSummary();

        // Include the output
        out.includeFile(tempFile);

        out.endRegression();
        out.close();

        if ((debugResult != 0) || (productionResult != 0)) {
            throw new BuildException("Regression tests failed");
        }
    }
}
