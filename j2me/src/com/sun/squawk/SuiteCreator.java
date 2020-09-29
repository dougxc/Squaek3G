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

import javax.microedition.io.*;

import com.sun.squawk.io.connections.ClasspathConnection;
import com.sun.squawk.util.*;


public class SuiteCreator {

    /**
     * Type of suite to create. This controls how much of the symbolic information is retained
     * in the suite when it is closed.
     */
    private int suiteType = Suite.APPLICATION;

    /**
     * Specify if the output should be big or little endian
     */
    private boolean bigEndian = VM.isBigEndian();

    /**
     * Prints the usage message.
     *
     * @param  errMsg  an optional error message
     */
    final void usage(String errMsg) {
        PrintStream out = System.out;
        if (errMsg != null) {
            out.println(errMsg);
        }
        out.println("Usage: "+GC.getKlass(this).getName()+" [-options] suite_name [prefixes...]");
        out.println("where options include:");
        out.println();
        out.println("    -cp:<directories and jar/zip files separated by ':' (Unix) or ';' (Windows)>");
        out.println("                    paths where classes and suites can be found (required)");
        out.println("    -parent:<name>  name of parent suite (default is the bootstrap suite)");
        out.println("    -translator:<uri>[#<name>] the URI of a suite containing the class <name> which implements");
        out.println("                     (default=file://translator.suite#com.sun.squawk.translator.Translator)");
        out.println("    -strip:<t>      strip symbolic information according to <t>:");
        out.println("                      'd' - debug: retain all symbolic info");
        out.println("                      'a' - application (default): discard all symbolic info");
        out.println("                      'l' - library: discard symbolic info");
        out.println("                            for private/package-private fields and methods");
        out.println("                      'e' - extendable library: discard symbolic info");
        out.println("                            for private fields and methods");
        out.println("    -lnt            retain line number tables");
        out.println("    -lvt            retain local variable tables");
        out.println("    -endian:<value> endianess ('big' or 'little') for generated suite (default=" + (VM.isBigEndian() ? "'big'" : "'little'") + ")");
        out.println("    -verbose, -v     provide more output while running");
        out.println("    -help           show this help message and exit");
        out.println();
        out.println("Note: If no prefixes are specified, then all the classes found on the");
        out.println("      class path are used.");
    }

    /**
     * Processes the class prefixes to build the set of classes on the class path that must be loaded.
     *
     * @param   classPath the path to search for classes
     * @param   args      the command line arguments specifiying class name prefixes
     * @param   index     the index in <code>args</code> where the prefixes begin
     * @param   classes   the vector to which the matching class names will be added
     * @param   resourceNames   the vector t owhich the matching resource names will be added
     */
    void processClassPrefixes(String classPath, String[] args, int index, Vector classes, Vector resourceNames) {
        boolean all = (args.length == index);
        try {
            ClasspathConnection cp = (ClasspathConnection)Connector.open("classpath://" + classPath);
            DataInputStream dis = new DataInputStream(cp.openInputStream("//"));
            try {
                for (;;) {
                    String name = dis.readUTF();
                    String className = name;
                    boolean isClass = false;
                    if (className.endsWith(".class")) {
                        className = className.substring(0, name.length() - ".class".length());
                        int slashIndex = className.lastIndexOf('/');
                        boolean isValidClassName = true;
                        if (slashIndex != -1) {
                            String fileNamePart = className.substring(slashIndex + 1);
                            // Make sure the file name part is a valid class name, if not then it must be a resource
                            if (fileNamePart.indexOf('.') != -1) {
                                isValidClassName = false;
                            }
                        }
                        isClass = isValidClassName;
                    }
                    className = className.replace('/', '.');
                    boolean match = all;
                    if (!match) {
                        for (int i = index; i < args.length; ++i) {
                            if (className.startsWith(args[i])) {
                                match = true;
                                break;
                            }
                        }
                    }
                    if (match) {
                        if (isClass) {
                            classes.addElement(className);
                        } else {
                            // Adding the name as resource names should be with '/' and not '.'
                            resourceNames.addElement(name);
                        }
                    }
                }
            } catch (EOFException ex) {
            }
            dis.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * Commmand line interface.
     *
     * @param args
     */
    public static void main(String args[]) throws Exception {
        SuiteCreator sc = new SuiteCreator();
        sc.run(args);
        Assert.shouldNotReachHere();
    }

    /**
     * Attempts to initialize the translator class for a given isolate that will be used to load a number of classes.
     *
     * @param uri        the URI of the suite containing a class implementing {@link TranslatorInterface}
     * @param className  the name of the class in the suite implementing {@link TranslatorInterface}
     * @param isolate    the isolate that will load a number of classes into its leaf suite
     */
    private void initializeTranslator(String uri, String className, Isolate isolate) {

        try {
            Suite suite = Suite.getSuite(uri);
            Klass klass = suite.lookup(className);
            if (klass == null) {
                throw new Error("could not find the class '" + className + "' in the suite loaded from '" + uri + "'");
            }

            isolate.setTranslatorClass(klass);
        } catch (Error le) {
            if (VM.isVerbose()) {
                System.err.println("** error opening translator suite from '" + uri + "': " + le);
                System.err.println("** will use translator in bootstrap suite (if any)");
            }
        }
    }

    /**
     * Parses and processes a given set of command line arguments to translate
     * a single suite.
     *
     * @param   args        the command line arguments
     * @return an Isolate that will load a number of classes into its leaf suite when started or null
     *         if there was a problem processing <code>args</code>
     */
    private Isolate processArgs(String[] args, Vector ignore) {
        int argc = 0;

        String classPath = null;
        String parentSuiteURI = null;

        String translatorSuiteURI = "file://translator.suite";
        String translatorClassName = "com.sun.squawk.translator.Translator";
        boolean verbose = false;

        while (argc != args.length) {
            String arg = args[argc];

            if (arg.charAt(0) != '-') {
                break;
            } else if (arg.startsWith("-cp:")) {
                classPath = ArgsUtilities.toPlatformPath(arg.substring("-cp:".length()), true);
            } else if (arg.startsWith("-parent:")) {
                parentSuiteURI = "file://" + arg.substring("-parent:".length()) + ".suite";
            } else if (arg.startsWith("-translator:")) {
                String t = arg.substring("-translator:".length());
                int hash = t.indexOf('#');
                if (hash != -1) {
                    translatorSuiteURI = t.substring(0, hash);
                    translatorClassName = t.substring(hash + 1);
                } else {
                    translatorSuiteURI = t;
                }
            } else if (arg.startsWith("-strip:") || arg.startsWith("-prune:")) {
                char type = arg.substring("-strip:".length()).charAt(0);
                if (type == 'a') {
                    suiteType = Suite.APPLICATION;
                } else if (type == 'd') {
                    suiteType = Suite.DEBUG;
                } else if (type == 'l') {
                    suiteType = Suite.LIBRARY;
                } else if (type == 'e') {
                    suiteType = Suite.EXTENDABLE_LIBRARY;
                } else {
                    usage("invalid suite type: " + type);
                    throw new RuntimeException();
                }
            } else if (arg.equals("-lnt")) {
                MethodMetadata.preserveLineNumberTables();
            } else if (arg.equals("-lvt")) {
                MethodMetadata.preserveLocalVariableTables();
            } else if (arg.startsWith("-endian:")) {
                String value = arg.substring("-endian:".length());
                if (value.equals("big")) {
                    bigEndian = true;
                } else if (value.equals("little")) {
                    bigEndian = false;
                } else {
                    usage("invalid endianess: " + value);
                    return null;
                }
            } else if (arg.equals("-verbose") | arg.equals("-v")) {
                verbose = true;
            } else if (arg.startsWith("-h")) {
                usage(null);
                return null;
            } else {
                usage("Unknown option "+arg);
                return null;
            }
            argc++;
        }

        if (argc >= args.length) {
            usage("missing suite name");
            return null;
        }

        if (classPath == null) {
            usage("missing class path");
            return null;
        }

        String suiteName = args[argc++];

        // Parse class specifiers
        Vector classNames = new Vector();
        Vector resourceNames = new Vector();
        processClassPrefixes(classPath, args, argc, classNames, resourceNames);
        if (classNames.isEmpty()) {
            usage("no classes match the package specification");
            return null;
        }

        Vector argVector = new Vector(classNames.size() + resourceNames.size() + 2);
        if (verbose) {
            argVector.addElement("-verbose");
        }
        for (int i=0, maxI=classNames.size(); i < maxI; i++) {
            argVector.addElement(classNames.elementAt(i));
        }
        StringBuffer ignoredClassesBuffer = new StringBuffer();
        for (int i=0, maxI=ignore.size(); i < maxI; i++) {
            int index = ((Integer) ignore.elementAt(i)).intValue();
            ignoredClassesBuffer.append(' ');
            ignoredClassesBuffer.append(argVector.elementAt(index));
            argVector.removeElementAt(index);
        }
        if (resourceNames.size() > 0) {
            // Add -resources ... and names of all resources to include
            argVector.addElement("-resources");
            for (int i=0, maxI = resourceNames.size(); i < maxI; i++) {
                argVector.addElement(resourceNames.elementAt(i));
            }
        }
        args = new String[argVector.size()];
        argVector.copyInto(args);

        Isolate isolate = new Isolate("com.sun.squawk.SuiteCreator$Loader", args, classPath, parentSuiteURI);
        isolate.setProperty("leaf.suite.name", suiteName);
        isolate.setProperty("leaf.suite.NoClassDefFoundClasses", ignoredClassesBuffer.toString());
        initializeTranslator(translatorSuiteURI, translatorClassName, isolate);
        return isolate;
    }

    /**
     * Runs the suite creator.
     *
     * @param args   the command line args
     * @throws Exception if there was an error
     */
    private void run(String args[]) throws Exception {
        Isolate isolate;
        Vector ignore = new Vector();
        while (true) {
            isolate = processArgs(args, ignore);
            if (isolate == null) {
                System.exit(1);
            }
            isolate.start();
            isolate.join();
            if (isolate.getExitCode() == 0) {
                break;
            } else {
                // If an error occurred on creation of Suite, then remove the file indicated as causing the error and
                // attempt to create the suite again.  The file is encoded in the return code as the index + 1 of the
                // file that cause the problem, since index 0 would double as an exit code indicating success
                ignore.addElement(new Integer(isolate.getExitCode() - 1));
            }
        }

        
        // Strip the symbols from the suite and close the stripped copy
        Suite suite = isolate.getLeafSuite().strip(suiteType);
        suite.close();

        String uri = "file://" + suite.getName() + ".suite";
        DataOutputStream dos = Connector.openDataOutputStream(uri);
        suite.save(dos, uri, bigEndian);

        PrintStream out = new PrintStream(Connector.openOutputStream("file://" + suite.getName() + ".suite.api"));
        suite.printAPI(out);
        out.close();

        System.out.println("Created suite and wrote it into " + suite.getName() + ".suite");
        // Exit with the exitCode from the Loader status since its the one that does the heavy lifting of
        // loading and translating classes
        System.exit(isolate.getExitCode());
    }

    /**
     * This class is used to load a number of classes and resources into it's isolate's leaf suite.
     */
    public static class Loader {
        /**
         * Expecting command line that looks something like
         * 
         * className1 className2 ... [-resources resource1 resource2 ...] [-classes className1 className2 ...]
         * @param args
         * @throws Exception
         */
        public static void main(String[] args) throws Throwable {
            boolean inClasses = true;
            boolean verbose = false;
            
            Suite suite = VM.getCurrentIsolate().getLeafSuite();
            String url = "classpath://" +  VM.getCurrentIsolate().getClassPath();
            ClasspathConnection classPathConnection;
            try {
                classPathConnection  = (ClasspathConnection) Connector.open(url);
            } catch (IOException ioe) {
                if (VM.isHosted() || VM.isVeryVerbose()) {
                    System.err.println("IO error while opening class path : " + url);
                    ioe.printStackTrace();
                }
                throw new RuntimeException("IO error while opening class path : " + url);
            }
            // If an exception occurs while we are loading classes, capture it and ignore all classes from then on
            // We still keep processing argument in order to get to the resources specified however.
            // This is a simple HACK which allows us to run the TCK JavaTest Agent even if classes for test
            // do not get included.  For this to work, JavaTest Agent must either be in bootstrap suite, or a
            // suite of its own.  Then the test to execute goes into its own suite which will include the agent.dat
            // for that test.
            for (int i = 0, maxI = args.length; i < maxI; i++) {
                if (inClasses) {
                    i = loadClasses(args, i, verbose);
                } else {
                    i = loadResources(suite, args, i, classPathConnection);
                }
                if (i < maxI && args[i].startsWith("-")) {
                    String arg = args[i];
                    if (arg.equals("-classes")) {
                        inClasses = true;
                    } else if (arg.equals("-resources")) {
                        inClasses = false;
                    } else if (arg.equals("-verbose") || arg.equals("-v")) {
                        verbose = true;
                    } else {
                        throw new RuntimeException("Specified unknown option: " + arg);
                    }
                }
            }
        }
        
    public static void installResourceFile(Suite suite, ResourceFile resourceFile) throws IOException {
        if (VM.isVerbose()) {
            System.out.println("[Including resource: " + resourceFile.name + "]");
        }
        suite.installResource(resourceFile);
    }

    protected static int loadClasses(String[] args, int startIndex, boolean verbose) {
            for (int i = startIndex; i != args.length; ++i) {
                String name = args[i];
                if (name.startsWith("-")) {
                    return i;
                }
                Error error = null;
                String message1 = null;
                String message2 = null;
                try {
                    Klass.forName(name, true, false);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                    return i;
                } catch (NoClassDefFoundError e) {
                    message2 = "   " + e.getMessage();
                    error = e;
                } catch (Error e) {
                    error = e;
                }
                if (error != null) {
                    if (message1 == null) {
                        message1 = "Encountered error loading class: " + name;
                    }
                    System.out.println(message1);
                    if (message2 != null) {
                        System.out.println(message2);
                    }
                    if (verbose) {
                        error.printStackTrace();
                    }
                    // Add 1 in case it was the first parameter
                    // Report back the index of the argument that failed to load
                    VM.getCurrentIsolate().exit(i + 1);
                }
            }
            return args.length;
        }

        public static int loadResources(Suite suite, String[] args, int startIndex, ClasspathConnection classPathConnection) {
            for (int i = startIndex; i != args.length; ++i) {
                String name = args[i];
                if (name.startsWith("-")) {
                    return i;
                }
                try {
                    byte[] bytes = classPathConnection.getBytes(name);
                    ResourceFile resource = new ResourceFile(name, bytes);
                    installResourceFile(suite, resource);
                } catch (IOException ioe) {
                    if (VM.isHosted() || VM.isVeryVerbose()) {
                        System.err.println("IO error while loading resource: " + name);
                        ioe.printStackTrace();
                    }
                    throw new RuntimeException("IO error while loading resource: " + name);
                }
            }
            return args.length;
        }
    }
}
