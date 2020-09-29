/**
 * Copyright 2005 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 * @test    @(#)TestJavacTask.java	1.1 05/10/14
 * @bug     4813736
 * @summary Provide a basic test of access to the Java Model from javac
 * @author  Peter von der Ah\u00e9
 * @run main TestJavacTask TestJavacTask.java
 */
package com.sun.squawk.vm2c;

import java.io.*;
import java.util.*;
import java.util.List;
import javax.lang.model.element.*;
import javax.tools.*;

import com.sun.source.tree.*;
import com.sun.tools.javac.api.*;
import com.sun.tools.javac.main.*;
import com.sun.tools.javac.util.*;
import com.sun.tools.javadoc.*;

/**
 * The command line interface for the tool that converts the call graphs
 * of one or more methods in the Squawk VM code base to C functions.
 *
 * @author  Doug Simon
 */
public class Main {

    static void usage(String errMsg) {
        PrintStream out = System.err;
        if (errMsg != null) {
            out.println(errMsg);
        }
        out.println("usage: vm2c [-options] <source files>");
        out.println("where options include:");
        out.println("    -cp:<path>  class path");
        out.println("    -root:<name>  treats methods annotated with \"@vm2c root\"");
        out.println("                in any class whose name includes 'name' as a");
        out.println("                substring as the root methods for conversion");
        out.println("    -o:<file>   generate to 'file' (default=stdout)");
        out.println("    -laf        inserts #line directives in output for Java source file positions");
        out.println("    -orc        omits runtime null pointer and array bounds checks");
        out.println("    -h          shows this help message and quit");
        out.println();
    }

    /**
     * The command line entry point. See {@link #usage} for more details.
     */
    public static void main(String... args) throws IOException {

        try {
            // Expands '@' arg files
            args = CommandLine.parse(args);
        } catch (IOException e) {
            System.err.println("IO error while parsing args: " + e.getMessage());
            System.exit(1);
        }

        JavaCompilerTool tool = ToolProvider.defaultJavaCompiler();
        JavaFileManager jfm = (DefaultFileManager)tool.getStandardFileManager();

        int argc = 0;
        String classPath = null;
        String outFile = null;
        boolean lineAndFile = false;
        boolean omitRuntimeChecks = false;
        Set<String> rootClassNames = new HashSet<String>();
        while (argc != args.length) {
            String arg = args[argc];
            if (arg.charAt(0) != '-') {
                break;
            } else if (arg.startsWith("-root:")) {
                rootClassNames.add(arg.substring("-root:".length()));
            } else if (arg.startsWith("-cp:")) {
                classPath = arg.substring("-cp:".length());
            } else if (arg.startsWith("-orc")) {
                omitRuntimeChecks = true;
            } else if (arg.startsWith("-h")) {
                usage(null);
                return;
            } else if (arg.startsWith("-laf")) {
                lineAndFile = true;
            } else if (arg.startsWith("-o:")) {
                outFile = arg.substring("-o:".length());
            } else {
                usage("Unknown option ignored: " + arg);
                System.exit(1);
            }
            argc++;
        }

        // Set up the class path
        if (classPath == null) {
            System.err.println("missing -cp option");
            System.exit(1);
        } else {
            List<File> paths = new LinkedList<File> ();
            for (String path : classPath.split(File.pathSeparator)) {
                paths.add(new File(path));
            }
            tool.setClassPath(paths);
        }

        if (rootClassNames.isEmpty()) {
            System.err.println("No root classes specified with '-root' option.");
            System.exit(1);
        }

        if (argc == args.length) {
            System.err.println("No input files found.");
            System.exit(1);
        }

        // Parse the input + output file pairs
        JavaFileObject[] files = new JavaFileObject[args.length - argc];
        int i = 0;
        while (argc != args.length) {
            JavaFileObject jfo = jfm.getFileForInput(args[argc++]);
            File file = new File(jfo.getPath());
            if (!file.exists() || !file.canRead()) {
                System.err.println("Cannot find or read file: " + file);
                System.exit(1);
            }
            files[i++] = jfo;
        }

        JavacTaskImpl task = (JavacTaskImpl)tool.run(null, files);

        Context context = task.getContext();

        // force the use of the scanner that captures Javadoc comments
        DocCommentScanner.Factory.preRegister(context);
        JavaCompiler.instance(context).keepComments = true;

        Iterable<? extends CompilationUnitTree> units = task.parse();
        Iterable<? extends TypeElement> elements = task.enter(units);
        task.analyze(elements);

        Converter converter = new Converter(context);
        converter.lineAndFile = lineAndFile;
        converter.omitRuntimeChecks = omitRuntimeChecks;
        converter.parse(units, rootClassNames);

        StringWriter buf = new StringWriter();
        PrintWriter out = new PrintWriter(buf);
        converter.emit(out);
        out.close();

        if (Log.instance(context).nerrors == 0) {
            Writer os = (outFile == null ? new OutputStreamWriter(System.out) : new FileWriter(outFile));
            os.write(buf.toString());
            os.close();
        } else {
            System.exit(1);
        }
    }
}
