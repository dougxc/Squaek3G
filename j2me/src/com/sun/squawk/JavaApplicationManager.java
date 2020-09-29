/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk;

import java.io.*;
import java.util.Vector;
import java.util.Hashtable;
import java.util.NoSuchElementException;

import javax.microedition.io.Connector;

import com.sun.squawk.util.Tracer;
import com.sun.squawk.util.ArgsUtilities;
import com.sun.squawk.util.StringTokenizer;

/**
 * The Java application manager is the master isolate used to coordinate application execution.
 *
 * @author Nik Shaylor
 */
public class JavaApplicationManager {
    
    /**
     * The class path to use when loading through the translator instance (if any).
     */
    private static String classPath;

    /**
     * The suite to which the leaf suite will be bound (if any).
     */
    private static String parentSuiteURI = null;

    /**
     * Command line option to enable display of execution statistics before exiting.
     */
    private static boolean displayExecutionStatistics;

    /**
     * Specifies if the application is to be serialized, deserialized and then restarted when it
     * stops by hibernating itself.
     */
    private static boolean testoms;
    
    /**
     * Specify the MIDlet- property to extract to determine which MIDlet should be run from a suite.
     */
    private static String midletPropertyName;


    /**
     * new isolate's properties.
     */
    private static Vector newProps = new Vector();

    /**
     * Main routine.
     *
     * @param args the command line argument array
     */
    public static void main(String[] args) throws Exception {
 
        // If no name is specified for MIDlet, assume MIDlet-1
        midletPropertyName = "MIDlet-1";
        
        /*
         * Process any switches.
         */
        if (args.length != 0) {
            args = processVMOptions(args);
        }

        String mainClassName;
        String[] javaArgs;
        if (args.length == 0) {
            mainClassName = "com.sun.squawk.imp.MIDletMainWrapper";
            javaArgs = new String[] {midletPropertyName};
        } else {
            /*
             * Split out the class name from the other arguments.
             */
            mainClassName = args[0].replace('/', '.');
            javaArgs = new String[args.length - 1];
            for (int i = 0 ; i < javaArgs.length ; i++) {
                javaArgs[i] = args[i+1];
            }
        }

        /*
         * Get the start time.
         */
        long startTime = System.currentTimeMillis();

        /*
         * Create the application isolate and run it.
         */
        Isolate isolate = new Isolate(mainClassName, javaArgs, classPath, parentSuiteURI);
        java.util.Enumeration e = newProps.elements();
        while (e.hasMoreElements()) {
            String[] pair = (String[])e.nextElement();
            isolate.setProperty(pair[0], pair[1]);
        }

        /*
         * Start the isolate and wait for it to complete.
         */
        isolate.start();
        isolate.join();

        /*
         * If the isolate was hibernated then save it and restart it.
         */
        while (isolate.isHibernated() && testoms) {
            try {
                String url = "file://" + isolate.getMainClassName() + ".isolate";
                DataOutputStream dos = Connector.openDataOutputStream(url);
                isolate.save(dos, url, VM.isBigEndian());
                System.out.println("Saved isolate to " + url);
                dos.close();

                DataInputStream dis = Connector.openDataInputStream(url);
                /*isolate = */Isolate.load(dis, url);
                dis.close();

                isolate.unhibernate();
                isolate.join();

            } catch (java.io.IOException ioe) {
                System.err.println("I/O error while trying to save or re-load isolate: ");
                ioe.printStackTrace();
                break;
            }
        }

        /*
         * Get the exit status.
         */
        int exitCode = isolate.getExitCode();

        /*
         * Show execution statistics if requested
         */
        if (displayExecutionStatistics) {
            long endTime = System.currentTimeMillis();
            System.out.println();
            System.out.println("=============================");
            System.out.println("Squawk VM exiting with code "+exitCode);
            if (GC.getPartialCollectionCount() > 0) {
                System.out.println(""+GC.getPartialCollectionCount()+" partial collections");
            }
            if (GC.getFullCollectionCount() > 0) {
                System.out.println(""+GC.getFullCollectionCount()+" full collections");
            }
            GC.getCollector().dumpTimings(System.out);
            System.out.println("Execution time was "+(endTime-startTime)+" ms");
            System.out.println("=============================");
            System.out.println();
        }

        /*
         * Stop the VM.
         */
        VM.stopVM(exitCode);
    }

    /**
     * Process any VM command line options.
     *
     * @param args the arguments as supplied by the VM.startup code
     * @return the arguments needed by the main() routine of the isolate
     */
    private static String[] processVMOptions(String[] args) {
        int offset = 0;
        while (offset != args.length) {
            String arg = args[offset];
            if (arg.charAt(0) == '-') {
                processVMOption(arg);
            } else {
                break;
            }
             offset++;
        }
        String[] javaArgs = new String[args.length - offset];
        for (int i = 0 ; i < javaArgs.length ; i++) {
            javaArgs[i] = args[offset++];
        }
        return javaArgs;
    }

    /**
     * Shows the version information.
     *
     * @param out  the print stream to use
     */
    private static void showVersion(PrintStream out) {
        out.println((Klass.SQUAWK_64 ? "64" : "32") + " bit squawk:");
        out.println("    debug code " + (Klass.DEBUG_CODE_ENABLED ? "enabled" : "disabled"));
        out.println("    assertions " + (Klass.ASSERTIONS_ENABLED ? "enabled" : "disabled"));
        out.println("    tracing " + (Klass.TRACING_ENABLED ? "enabled" : "disabled"));
        boolean floatSupported = "${build.properties:FLOATS}".equals("true");
        if (floatSupported) {
            out.println("    floating point supported");
        } else {
            out.println("    no floating point support");
        }
        out.println("    bootstrap suite: ");
        StringTokenizer st = new StringTokenizer(VM.getCurrentIsolate().getBootstrapSuite().getConfiguration(), ",");
        while (st.hasMoreTokens()) {
            out.println("        " + st.nextToken().trim());
        }
        VM.printConfiguration();
    }

    /**
     * Shows the classes in the image.
     *
     * @param out  the print stream to use
     * @param packagesOnly if true, only a listing of the packages in the image is shown
     */
    private static void showImageContents(PrintStream out, boolean packagesOnly) {
        Suite bootstrapSuite = VM.getCurrentIsolate().getBootstrapSuite();
        if (packagesOnly) {
            out.println("Packages in image:");
            Hashtable packages = new Hashtable();
            int count = bootstrapSuite.getClassCount();
            for (int i = 0; i != count; ++i) {
                Klass klass = bootstrapSuite.getKlass(i);
                if (klass != null && !klass.isSynthetic()) {
                    String className = klass.getInternalName();
                    int index = className.lastIndexOf('.');
                    if (index != -1) {
                        String packageName = className.substring(0, className.lastIndexOf('.'));
                        if (packages.get(packageName) == null) {
                            out.println("  " + packageName);
                            packages.put(packageName, packageName);
                        }
                    }
                }
            }
        } else {
            out.println("Classes in image:");
            int count = bootstrapSuite.getClassCount();
            for (int i = 0; i != count; ++i) {
                Klass klass = bootstrapSuite.getKlass(i);
                if (klass != null && !klass.isSynthetic()) {
                    out.println("  " + klass.getName());
                }
            }
        }
    }

    /**
     * Process a VM command line option.
     *
     * @param arg the argument
     */
    private static void processVMOption(String arg) {
        if (arg.startsWith("-cp:")) {
            // Fix up the class path with respect to the system dependant separator characters
            classPath = ArgsUtilities.toPlatformPath(arg.substring("-cp:".length()), true);
        } else if (arg.startsWith("-suite:")) {
            parentSuiteURI = "file://" + arg.substring(7) + ".suite";
/*if[FLASH_MEMORY]*/
        } else if (arg.startsWith("-flashsuite:")) {
            parentSuiteURI = "flash://" + arg.substring(12);
/*end[FLASH_MEMORY]*/
        } else if (arg.equals("-egc")) {
            GC.setExcessiveGC(true);
        } else if (arg.equals("-nogc")) {
            VM.allowUserGC(false);
        } else if (arg.equals("-imageclasses")) {
            showImageContents(System.err, false);
            VM.stopVM(0);
        } else if (arg.equals("-imagepackages")) {
            showImageContents(System.err, true);
            VM.stopVM(0);
        } else if (arg.startsWith("-isolateinit:")) {
            String initializer = arg.substring(13);
            VM.setIsolateInitializerClassName(initializer);
        } else if (arg.startsWith("-MIDlet-")) {
            midletPropertyName = arg.substring(1);
        } else if (arg.equals("-version")) {
            showVersion(System.err);
            VM.stopVM(0);
        } else if (arg.equals("-verbose")) {
            if (!VM.isVerbose()) {
                VM.setVerboseLevel(1);
            }
        } else if (arg.equals("-veryverbose")) {
            if (!VM.isVeryVerbose()) {
                VM.setVerboseLevel(2);
            }
        } else if (arg.equals("-testoms")) {
            testoms = true;
        } else if (Klass.TRACING_ENABLED && arg.startsWith("-trace")) {
            String feature = arg.substring("-trace".length());
            Tracer.enableFeature(feature);
        } else if (arg.equals("-stats")) {
            displayExecutionStatistics = true;
        } else if (arg.startsWith("-D")) {
            String propAndValue = arg.substring("-D".length());
            int seperator = propAndValue.indexOf('=');
            String prop = propAndValue.substring(0, seperator);
            String val = propAndValue.substring(seperator+1);
                        System.out.println("Setting property " + prop + " to " + val);
            String[] pair = new String[2];
            pair[0] = prop;
            pair[1] = val;
            newProps.addElement(pair);
            // System properties are not "global global"
        } else if (arg.equals("-h")) {
            usage("");
            VM.stopVM(0);
        } else if (!processTranslatorOption(arg) && !GC.getCollector().processCommandLineOption(arg)) {
            usage("Unrecognised option: "+arg);
            VM.stopVM(0);
        }
    }

    /**
     * Process an option to see if it is an option for the translator.
     *
     * @param arg   the option to process
     * @return      true if <code>arg</code> was a translator option
     */
    private static boolean processTranslatorOption(String arg) {
        if (VM.getCurrentIsolate().getTranslator() != null) {
            if (Klass.TRACING_ENABLED && arg.startsWith("-trace")) {
                if (arg.startsWith("-tracefilter:")) {
                    Tracer.setFilter(arg.substring("-tracefilter:".length()));
                } else {
                    String feature = arg.substring("-trace".length());
                    Tracer.enableFeature(feature);
                    if (arg.equals("-traceconverting")) {
                        Tracer.enableFeature("loading"); // -traceconverting subsumes -traceloading
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Get a long form an argument.
     *
     * @param arg the argument
     * @param prefix part of the argument before the number
     */
    private static long parseLongArg(String arg, String prefix) {
        try {
            return Long.parseLong(arg.substring(prefix.length()));
        } catch (NumberFormatException ex) {
            usage("Bad numeric parameter: "+arg);
            return 0;
        }
    }

     /**
     * Print a usage message and exit.
     *
     * @param msg error message
     */
    private static void usage(String msg) {
        PrintStream out = System.out;
        String copyright = VM.isVerbose() ? VM.LONG_COPYRIGHT : VM.SHORT_COPYRIGHT;
        
        out.println();
        out.println(copyright);
        out.println();
        if (msg.length() > 0) {
            out.println("** " + msg + " **\n");
        }
        out.println("Usage: squawk [-options] class [args...] | [-MIDlet-x]");
        out.println();
        out.println("if there is no class specified, then try MIDlet-1 property to find a MIDlet");
        out.println("where options include:");
        out.println("    -cp:<directories and jar/zip files separated by ':' (Unix) or ';' (Windows)>");
        out.println("                            paths where classes, suites and sources can be found");
        translatorUsage(out);
        out.println("    -suite:<name>           suite name (without \".suite\") to load");
        out.println("    -imageclasses           show the classes in the boot image and exit");
        out.println("    -imagepackages          show the packages in the boot image and exit");
        out.println("    -isolateinit:<class>          Class whose main will be invoked on Isolate start, single arg \"true\" if first Isolate being initialized");
        out.println("    -MIDlet-x                Which MIDlet-x property to use from " + Suite.PROPERTIES_MANIFEST_RESOURCE_NAME);
        out.println("    -version                print product version and exit");
        out.println("    -verbose                report when a class is loaded");
        out.println("    -veryverbose            report when a class is initialized or looked up and");
        out.println("                            various other output");
        out.println("    -testoms                continually serialize, deserialize and restart the application if it hibernates itself");
        if (Klass.TRACING_ENABLED) {
        out.println("    -traceoms               trace object memory serialization");
        out.println("    -traceswapper           trace endianess swapping");
        }
        GC.getCollector().usage(out);
        out.println("    -egc                    enable excessive garbage collection");
        out.println("    -nogc                   disable application calls to Runtime.gc()");
        out.println("    -stats                  display execution statistics before exiting");
        out.println("    -D<name>=<value>        set a system property");
        out.println("    -h                      display this help message");
        out.println("    -X                      display help on native VM options");
        VM.stopVM(0);
    }

    /**
     * Prints the usage message for the translator specific options if the translator is present.
     *
     * @param out  the stream on which to print the message
     */
    private static void translatorUsage(PrintStream out) {
        if (VM.getCurrentIsolate().getTranslator() != null) {
            if (Klass.TRACING_ENABLED) {
            out.println("    -traceloading           trace class loading");
            out.println("    -traceconverting        trace method conversion (includes -traceloading)");
            out.println("    -tracejvmverifier       trace verification of JVM/CLDC bytecodes");
            out.println("    -traceemitter           trace Squawk bytecode emitter");
            out.println("    -tracesquawkverifier    trace verification of Squawk bytecodes");
            out.println("    -traceclassinfo         trace loading of class meta-info (i.e. implemented");
            out.println("                            interfaces, field meta-info & method meta-info)");
            out.println("    -traceclassfile         trace low-level class file elements");
            out.println("    -traceir0               trace the IR built from the JVM bytecodes");
            out.println("    -traceir1               trace optimized IR with JVM bytecode offsets");
            out.println("    -traceir2               trace optimized IR with Squawk bytecode offsets");
            out.println("    -tracemethods           trace emitted Squawk bytecode methods");
            out.println("    -tracefilter:<string>   filter trace with simple string filter");
            }
        }
    }
}
