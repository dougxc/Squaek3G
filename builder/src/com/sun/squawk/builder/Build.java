/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: Build.java,v 1.101 2006/05/09 05:32:46 ccifue Exp $
 */
package com.sun.squawk.builder;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;

import com.sun.squawk.builder.ccompiler.*;
import com.sun.squawk.builder.commands.*;
import com.sun.squawk.builder.gen.*;
import com.sun.squawk.builder.platform.*;
import com.sun.squawk.builder.util.*;

/**
 * This is the launcher for building parts (or all) of the Squawk VM as well as launching commands.
 * Run it with '-h' to see what commands and targets are available.
 *
 * @author Doug Simon
 */
public class Build {

    /*---------------------------------------------------------------------------*\
     *                               Runtime options                             *
    \*---------------------------------------------------------------------------*/

    /**
     * Enables/disables verbose execution.
     * <p>
     * The default value is <code>false</code>.
     */
    public boolean verbose;

    /**
     * Enables/disables information level output.
     * <p>
     * The default value is <code>false</code>.
     */
    public boolean info;

    /**
     * Enables/disables brief level output.
     * <p>
     * The default value is <code>true</code>.
     */
    public boolean brief = true;

    /**
     * Enables/disables checking (and updating if necessary) of a command's dependencies before running the command itself.
     * <p>
     * The default value is <code>true</code>.
     */
    public boolean checkDependencies = true;

    /**
     * Enables/disables running the javadoc checking tool after a Java compilation.
     * <p>
     * The default value is <code>false</code>.
     */
    public boolean runDocCheck;

    /**
     * Enables/disables generation of javadoc after a Java compilation.
     * <p>
     * The default value is <code>false</code>.
     */
    public boolean runJavadoc;

    /**
     * If {@link runJavadoc} is <code>true</code>, then javadoc is only generated for  public and protected members.
     * <p>
     * The default value is <code>false</code>.
     */
    public boolean runJavadocAPI;

    /**
     * Determines if the Squawk system will be built as a dynamic library.
     */
    public boolean dll;

    /**
     * Extra arguments to be passed to the Java compiler.
     */
    public String javacOptions = "";

    /**
     * Determines if {@link JavaCommand}s are run in the same JVM process as the builder.
     */
    public boolean forkJavaCommand;

    /**
     * Extra arguments to be passed to the Java VM when executing a JavaCommand.
     */
    public String javaOptions = "";

    /**
     * The C compiler.
     */
    private CCompiler ccompiler;

    /**
     * Gets the compiler used for compiling and linking C code.
     *
     * @return C compiler
     */
    public CCompiler getCCompiler() {
        return ccompiler;
    }

    /**
     * The object providing access to JDK tools.
     */
    private JDK jdk;

    /**
     * Gets the instance through which JDK tools can be accessed.
     */
    public JDK getJDK() {
        return jdk;
    }

    /**
     * The host platform. This is used to access tools required for running the builder and has not
     * relationship with the target platform that a Squawk executable will be built for.
     */
    private final Platform platform;

    /**
     * Gets the object that represents the host platform.
     *
     * @return  the object that represents this builder's host platform
     */
    public Platform getPlatform() {
        return platform;
    }

    /**
     * The Java source preprocessor.
     */
    private final Preprocessor preprocessor;

    /**
     * Gets the Java source preprocessor.
     *
     * @return the Java source preprocessor
     */
    public Preprocessor getPreprocessor() {
        return preprocessor;
    }

    /**
     * The C function macroizer.
     */
    private final Macroizer macroizer;

    /**
     * Gets the C function macroizer.
     *
     * @return the C function macroizer
     */
    public Macroizer getMacroizer() {
        return macroizer;
    }

    /**
     * The properties that drive the preprocessing.
     */
    private Properties properties;

    /**
     * The most recent date that the file(s) from which the properties are loaded was modified.
     */
    private long propertiesLastModified;

    /**
     * The interface to run a Java source compiler.
     */
    private final JavaCompiler javaCompiler;

    /**
     * Gets the object used to do a Java compilation as well as run javadoc.
     *
     * @return Java compiler
     */
    public JavaCompiler getJavaCompiler() {
        return javaCompiler;
    }

    /**
     * The arguments extracted by the last call to {@link #extractBuilderArgs}.
     */
    private List builderArgs = new ArrayList();

    /**
     * Gets the builder specific arguments passed to the last invocation of {@link #mainProgrammatic}.
     * These can be used to pass through to a invocation of the builder via another Build instance.
     *
     * @return  the builder specific arguments
     */
    public List getBuilderArgs() {
        return new ArrayList(builderArgs);
    }

    /*---------------------------------------------------------------------------*\
     *                              Commands  and Targets                        *
    \*---------------------------------------------------------------------------*/

    /**
     * The set of builder commands and targets.
     */
    private Map commands = new TreeMap();

    /**
     * Get the available commands as an iterator
     * @return Iterator for command entries
     */
    public Iterator getCommandList() {
        return commands.entrySet().iterator();
    }

    /**
     * Adds a command to the set of builder commands.
     *
     * @param command the command
     * @return <code>command</code>
     * @throws IllegalArgumentException if their is already a command registered with <code>command.getName()</code>
     */
    private Command addCommand(Command command) {
        String name = command.getName();
        if (commands.containsKey(name)) {
            throw new IllegalArgumentException("cannot overwrite existing command: " + command.getName());
        }
        commands.put(name, command);
        return command;
    }

    /**
     * Creates and installs a Target.
     *
     * @param j2me           compiles for a J2ME platform
     * @param baseDir        the parent directory for primary source directory and the output directory(s). This will also be the name of the target.
     * @param dependencies   the space separated names of the Java compilation targets that this target depends upon
     * @param extraClassPath the class path in addition to that derived from <code>dependencies</code>
     * @param extraSourceDirs any extra directories to be searched for sources (can be null)
     * @return the created and installed command
     */
    public Target addTarget(boolean j2me, String baseDir, String dependencies, String extraClassPath, String extraSourceDirs) {
        File primarySrcDir = new File (baseDir, "src");
        File[] srcDirs;
        if (extraSourceDirs != null) {
            StringTokenizer st = new StringTokenizer(extraSourceDirs);
            srcDirs = new File[st.countTokens() + 1];
            srcDirs[0] = primarySrcDir;
            for (int i = 1; i != srcDirs.length; ++i) {
                srcDirs[i] = new File(st.nextToken());
            }
        } else {
            srcDirs = new File[] { primarySrcDir };
        }

        String classPath;
        if (dependencies != null) {
            StringTokenizer st = new StringTokenizer(dependencies);
            int count = st.countTokens();
            StringBuffer buf = new StringBuffer(dependencies.length() + count * "/classes".length());
            while (st.hasMoreTokens()) {
                String dependency = st.nextToken();
                buf.append(dependency).append(File.separatorChar).append("classes");
                if (st.hasMoreTokens()) {
                    buf.append(File.pathSeparatorChar);
                }
            }
            if (extraClassPath != null && extraClassPath.length() != 0) {
                buf.append(File.pathSeparatorChar).append(toPlatformPath(extraClassPath, true));
            }
            classPath = buf.toString();
        } else {
            classPath = null;
        }

        Target command = new Target(classPath, j2me, baseDir, srcDirs, true, this);
        if (dependencies != null) {
            command.dependsOn(dependencies);
        }
        addCommand(command);
        return command;
    }

    /**
     * Creates and installs a Target.
     *
     * @param j2me           compiles for a J2ME platform
     * @param baseDir        the parent directory for primary source directory and the output directory(s). This will also be the name of the command.
     * @param dependencies   the space separated names of the Java compilation targets that this command depends upon
     * @param extraClassPath the class path in addition to that derived from <code>dependencies</code>
     * @return the created and installed command
     */
    public Target addTarget(boolean j2me, String baseDir, String dependencies, String extraClassPath) {
        return addTarget(j2me, baseDir, dependencies, extraClassPath, null);
    }

    /**
     * Creates and installs a Target.
     *
     * @param j2me           compiles for a J2ME platform
     * @param baseDir        the parent directory for primary source directory and the output directory(s). This will also be the name of the command.
     * @param dependencies   the space separated names of the Java compilation targets that this command depends upon
     * @return the created and installed command
     */
    public Target addTarget(boolean j2me, String baseDir, String dependencies) {
        return addTarget(j2me, baseDir, dependencies, null, null);
    }

    /**
     * Creates and installs a JavaCommand.
     *
     * @param j2me           compiles for a J2ME platform
     * @param baseDir        the directory in which the sources and output directories exist. This will also be the name of the command.
     * @param classPath      the class path to be used for compilation
     * @return the created and installed command
     */
    private JavaCommand addJavaCommand(String name, String classPath, boolean bootclasspath, String extraJVMArgs, String mainClassName, String dependencies) {
        JavaCommand command = new JavaCommand(name, classPath, bootclasspath, extraJVMArgs, mainClassName, this);
        command.dependsOn(dependencies);
        addCommand(command);
        return command;
    }

    /**
     * Creates and installs a command that launches an application in Squawk.
     *
     * @param name String
     * @param classPath String
     * @param extraVMArgs String
     * @param mainClassName String
     * @param args String
     * @return the created and installed command
     */
    public Command addSquawkCommand(String name, String classPath, String extraVMArgs, String mainClassName, String args, final String description) {
        final String commandWithArgs = "squawk" + getPlatform().getExecutableExtension() + " " +
                                       (classPath == null ? "" : "-cp:" + toPlatformPath(classPath, true) + " ") +
                                       (extraVMArgs == null ? "" : " " + extraVMArgs + " ") +
                                       mainClassName + " " +
                                       (args == null ? "" : args);
        Command command = new Command(this, name) {
            public void run(String[] args) throws BuildException {
                env.exec(commandWithArgs + " " + join(args));
            }
            public String getDescription() {
                if (description != null) {
                    return description;
                }
                return super.getDescription();
            }

        };
        addCommand(command);
        return command;
    }

    /**
     * Creates and installs a command that generates a source file.
     *
     * @param name     the name of the class in the package com.sun.squawk.builder.gen that does the generating
     * @param baseDir  the base directory in which the generated file is found
     * @return the created Command
     */
    private Command addGen(final String name, final String base) {
        Command command = new GeneratorCommand(name, base);
        addCommand(command);
        return command;
    }

    class GeneratorCommand extends Command {
        private final File baseDir;
        GeneratorCommand(final String name, final String base) {
            super(Build.this, name);
            this.baseDir = new File(base);
        }

        private Generator generator(String name) {
            String className = "com.sun.squawk.builder.gen." + name;
            try {
                return (Generator) Class.forName(className).newInstance();
            } catch (Exception e) {
                throw new BuildException("Error instantiating " + className, e);
            }
        }

        public void run(String[] args) throws BuildException {
            Generator gen = generator(name);
            boolean replaced = gen.run(baseDir);
            log(replaced && verbose, "[created/updated " + gen.getGeneratedFile(baseDir) + "...]");
        }

        public String getDescription() {
            Generator gen = generator(name);
            return "generates the file " + gen.getGeneratedFile(baseDir);
        }
    }

    /**
     * Installs commands from a properties file.
     *
     * @param pluginsFile  the properties file to load from
     */
    private void loadPlugins(File pluginsFile) {
        try {
            if (!pluginsFile.exists()) {
                throw new IOException("plugin properties file: " + pluginsFile.getAbsolutePath() + " does not seem to exist");
            }
            Properties plugins = new Properties();
            plugins.load(new FileInputStream(pluginsFile));

            ClassLoader loader = Build.class.getClassLoader();
            if (plugins.containsKey("classpath")) {
                URL[] urls = null;
                String[] classpath = Build.toPlatformPath(plugins.getProperty("classpath"), true).split(File.pathSeparator);
                try {
                    urls = new URL[classpath.length];
                    for(int i = 0; i < classpath.length; i++) {
                        File f = new File (classpath[i]);
                        String url = f.getAbsolutePath();

                        // Make sure the url class loader recognises directories
                        if (f.isDirectory()) {
                            url += "/";
                        }
                        url = "file://" + fixURL(url);
                        urls[i] = new URL(url);
                    }
                } catch (MalformedURLException e) {
                    throw new BuildException("badly formed plugins path", e);
                }
                plugins.remove("classpath");
                loader = new URLClassLoader(urls);
            }

            for (Enumeration names = plugins.propertyNames(); names.hasMoreElements(); ) {
                String name = (String)names.nextElement();
                String className = plugins.getProperty(name);

                try {
                    Class pluginClass = loader.loadClass(className);
                    Constructor cons = pluginClass.getConstructor(new Class[] {Build.class});
                    Command command = (Command)cons.newInstance(new Object[] {this});
                    addCommand(command);
                } catch (InvocationTargetException e) {
                    throw new BuildException("error creating " + name + " plugin: ", e);
                } catch (IllegalArgumentException e) {
                    throw new BuildException("error creating " + name + " plugin: ", e);
                } catch (IllegalAccessException e) {
                    throw new BuildException("error creating " + name + " plugin: ", e);
                } catch (InstantiationException e) {
                    throw new BuildException("error creating " + name + " plugin: ", e);
                } catch (NoSuchMethodException e) {
                    throw new BuildException("error creating " + name + " plugin: ", e);
                } catch (ClassNotFoundException e) {
                    throw new BuildException("error creating " + name + " plugin: ", e);
                }
            }
        } catch (IOException e) {
            throw new BuildException("error while loading plugins from " + pluginsFile + ": " + e);
        }
    }

    /**
     * Installs the built-in commands.
     */
    private void installBuiltinCommands() {

        addGen("OPC",                "j2me/src");
        addGen("OperandStackEffect", "translator/src");
        addGen("Mnemonics",          "translator/src");
        addGen("Verifier",           "translator/src");
        addGen("SwitchDotC",         "slowvm/src");
        addGen("JitterSwitch",       "vmgen/src");
        addGen("InterpreterSwitch",  "vmgen/src");
        addGen("BytecodeRoutines",   "vmgen/src");

        addTarget(true,  "j2me",              null).dependsOn("OPC");
        addTarget(true,  "imp",               "j2me");
        addTarget(true,  "translator",        "j2me").dependsOn("OperandStackEffect Mnemonics Verifier");
        addTarget(false, "j2se",              "j2me translator", "tools/bcel-5.1.jar").triggers("squawk.jar");
        addTarget(false, "romizer",           "j2me translator j2se");
        addTarget(true,  "graphics",          "j2me imp");
        addTarget(true,  "samples",           "j2me imp graphics");
        addTarget(true,  "benchmarks",        "j2me imp");
        addTarget(true,  "prototypecompiler", "j2me");
        addTarget(true,  "compiler",          "j2me");
        addTarget(false, "vmgen",             "j2me translator prototypecompiler").dependsOn("JitterSwitch InterpreterSwitch BytecodeRoutines");
        addTarget(false, "mapper",            "j2me translator j2se");
        addTarget(false, "compilertests",     "compiler j2me", "tools/junit-3.8.1.jar");
        addTarget(true,  "tck",               "j2me");
        addTarget(true,  "debugger",          "j2me");
        addTarget(true,  "drivers",           "j2me");
        addTarget(false, "sdp",               "j2me translator j2se debugger");
        addTarget(false, "spec",              "j2me");
        addTarget(true,  "javatest-agent",               "j2me");


        // Add the "clean" command
        addCommand(new Command(this, "clean") {
            public String getDescription() {
                return "cleans one or more targets (all targets if no argument is given)";
            }
            public void run(String[] args) {
                // We do this in order to clean up some references that may still exist to .jar files that may be about to be deleted
                // This is only really an issue for Windows, and it cannot really be fixed as the .jar files are used by javac code
                // We should file bugs against javac for this TODO
                // The following is ONLY a suggestion, there is no guarantee it will work, but hoping it will increase likelihood of success.
                // On Windows I have proven that this does indeed work, at end of GC files we're closed :(
                System.gc();
                System.runFinalization();
                if (args.length == 0) {
                    log(info, "[cleaning all...]");
                    for (Iterator iterator = commands.values().iterator(); iterator.hasNext(); ) {
                        Command cmd = (Command)iterator.next();
                        cmd.clean();
                    }
                    Build.clear(new File("builder", "classes"), true);
                } else {
                    for (int i = 0; i != args.length; ++i) {
                        Command cmd = getCommand(args[i]);
                        if (cmd == null) {
                            throw new BuildException("unknown target for cleaning: " + args[i]);
                        }
                        log(info, "[cleaning " + cmd + "...]");
                        cmd.clean();
                    }
                }
            }
        });

        // Add the "jvmenv" command
        addCommand(new Command(this, "jvmenv") {
            public String getDescription() {
                return "displays required environment variables for running the Squawk VM";
            }
            public void run(String[] args) {
                platform.showJNIEnvironmentMessage(System.out);
            }
        });

        // Add the "genspec" command
        addJavaCommand("genspec", "spec/classes:j2me/classes", false, "", "sbdocgen", "spec").
            setDescription("generates the Squawk bytecode specification in doc/spec");

        // Add the "romize" command
        addJavaCommand("romize", "j2se/classes:romizer/classes:j2me/classes:translator/classes", false, "", "com.sun.squawk.Romizer", "romizer").
            setDescription("processes a number of classes to produce a .suite file");

        // Add the "traceviewer" command
        addJavaCommand("traceviewer", "j2se/classes:j2me/classes:translator/classes", false, "", "com.sun.squawk.traces.TraceViewer", "j2se").
            setDescription("the Squawk VM execution trace GUI viewer");

        // Add the "profileviewer" command
        addJavaCommand("profileviewer", "j2se/classes:j2me/classes", false, "", "com.sun.squawk.traces.ProfileViewer", "j2se").
            setDescription("the Squawk VM execution profile GUI viewer");

        // Add the "gctf" command
        addJavaCommand("gctf", "j2se/classes:j2me/classes", false, "", "com.sun.squawk.traces.GCTraceFilter", "j2se").
            setDescription("filter that converts method addresses in a garbage collector trace to signatures");

        // Add the "ht2html" command
        addJavaCommand("ht2html", "j2se/classes:j2me/classes", false, "", "com.sun.squawk.ht2html.Main", "j2se").
            setDescription("converts a heap trace to a set of HTML files");

        // Add the "rom" command
        addCommand(new RomCommand(this)).dependsOn("SwitchDotC");

        // Add the "spp" command
        addCommand(new SppFilePreprocessCommand(this));

        // Add the "documentor" command
        addCommand(new DocumentorCommand(this));

        // Add the "export" command
        addCommand(new ExportCommand(this));

        // Add the "jam" command
        addCommand(new JamCommand(this));
        
        // Add the "regression" command
        addCommand(new RegressionCommand(this));

        // Add the "squawk" command
        addJavaCommand("squawk", "j2se/classes:j2me/classes", false, "-Djava.library.path=.", "com.sun.squawk.vm.Main", "j2se").
            setDescription("use Java based launcher to start Squawk VM");

        // Add the "makeapi" command
        addCommand(new MakeAPI(this));

        // Add the "map" command
        addJavaCommand("map", "j2se/classes:mapper/classes:j2me/classes:translator/classes", false, "", "com.sun.squawk.ObjectMemoryMapper", "mapper").
            setDescription("suite file symbolic mapper/disassembler");

        // Add the "omfconv" command
        addJavaCommand("omfconv", "j2se/classes:mapper/classes:j2me/classes:translator/classes", false, "", "com.sun.squawk.ObjectMemoryFileEndianessConverter", "mapper").
            setDescription("object memory file endianess converter");

        // Add the "sdproxy" command
        addJavaCommand("sdproxy", "romizer/classes:j2se/classes:debugger/classes:sdp/classes:j2me/classes:translator/classes", false, "", "com.sun.squawk.debugger.sdp.SDP", "sdp").
            setDescription("Debugger proxy for translating between Squawk VM and normal JDPA debuggers");

        // Add the "map" command
        addJavaCommand("listclass", "j2se/classes:j2me/classes:tools/bcel-5.1.jar", false, "", "com.sun.squawk.listclass.Main", "j2se").
            setDescription("class file disassembler");

        // Add the "hexdump" command
        addJavaCommand("hexdump", "j2se/classes", false, "", "com.sun.squawk.util.HexDump", "j2se").
            setDescription("hex file dump");

        // Add "systemproperties" command
        addJavaCommand("systemproperties", "j2se/classes:j2me/classes", false, "", "com.sun.squawk.io.j2se.systemproperties.Protocol", "j2se").
            setDescription("shows the default system properties");

        // Add "squawk.jar" command
        addCommand(new Command(this, "squawk.jar") {
            public void run(String[] args) {
                String cmd = "jar cf squawk.jar @" + new File("j2se", "squawk.jar.filelist");
                log(info, "[running '" + cmd + "' ...]");
                exec(cmd);
            }
            public String getDescription() {
                return "(re)builds squawk.jar (the classes required by the embedded JVM in Squawk)";
            }
        });

        // Add the "protocomp_test" command
        addCommand(new Command(this, "protocomp_test") { // compiler test: run only 1 test
            public void run(String[] args) {
                System.out.println("Running prototype compiler test ");
                String testName = "com.sun.squawk.compiler.tests." + args[0];
                System.out.println(testName);
                exec("java -cp " + toPlatformPath("prototypecompiler/classes:j2se/classes:j2me/classes:compilertests/classes ", true) + testName);
            }
        });

        // Add the "comp_test" command
        addCommand(new Command(this, "comp_test") {  // compiler test: run only 1 test
            public void run(String[] args) {
                System.out.println("Running compiler test ");
                String testName = "com.sun.squawk.compiler.tests." + args[0];
                System.out.println (testName);
                exec("java -cp " + toPlatformPath("compiler/classes:j2se/classes:j2me/classes ", true) + testName);
            }
        });

        addCommand(new Command(this, "arm_asm_tests") {  // run JUnit tests for the ARM assembler
            public void run(String[] args) {
                System.out.println("Running JUnit test for the ARM assembler ");
                exec("java -cp " + toPlatformPath("compilertests/junit-3.8.1.jar:compiler/classes:j2se/classes:j2me/classes:compilertests/classes ", true) +
                     "com.sun.squawk.compiler.asm.arm.tests.ArmTests");
            }
        });


        String ver = System.getProperty("java.version");
        // TODO: Find a better way to identify the version of Java ?
        if (ver.startsWith("1.5") || ver.startsWith("1.6")) {
            // Add "vm2c" target
            Target vm2c = addTarget(false,  "vm2c", null);
            String bootclasspath = toPlatformPath("vm2c/mustang-javac.jar:vm2c/classes:" +
                                                  System.getProperty("sun.boot.class.path"), true);
            vm2c.extraArgs = Arrays.asList(new String[] {"-bootclasspath", bootclasspath});
            vm2c.version = "1.5";

            // Add "runvm2c" target
            addCommand(new Command(this, "runvm2c") {
                public void run(String[] args) {
                    String javaExe = toPlatformPath(System.getProperty("java.home") + "/bin/java", true);
                    exec(javaExe + " -ea -esa -Xbootclasspath/p:" + toPlatformPath("vm2c/classes:vm2c/mustang-javac.jar ", true) +
                         "com.sun.squawk.vm2c.Main " + join(args));
                }
                public String getDescription() {
                    return "runs the VM Java source file to C converter";
                }

            }).dependsOn("vm2c");
        }
    }

    /**
     * Gets the command with a given name.
     *
     * @param name   the name of the command to get
     * @return the command registered with the given name or null if there is no such command
     */
    public Command getCommand(String name) {
        return (Command)commands.get(name);
    }

    /**
     * Runs a command. The dependencies of the command are run first if {@link #checkDependencies} is <code>true</code>.
     * The command will not be run if <code>hasBeenRun(getCommand(name)) == true</code>.
     *
     * @param name  the name of the command to run. The special value "<all>" will run all the targets.
     * @param args  its arguments
     */
    public void runCommand(final String name, String[] args) {
        if (name.equals("<all>")) {
            for (Iterator iterator = commands.values().iterator(); iterator.hasNext();) {
                Object target = iterator.next();
                if (target instanceof Target) {
                    run((Target)target, NO_ARGS);
                }
            }
            runCommand("squawk.jar", NO_ARGS);
        } else {
            Command command = getCommand(name);
            if (command == null) {
                throw new BuildException("unknown command: " + name);
            }
            run(command, args);
        }
    }

    /**
     * Runs a command. The dependencies of the command are run first if {@link #checkDependencies} is <code>true</code>.
     * The command will not be run if <code>hasBeenRun(command) == true</code>.
     *
     * @param command  the command to run
     * @param args     its arguments
     */
    public void run(Command command, String[] args) {
        if (checkDependencies) {
            for (Iterator iterator = command.getDependencies(); iterator.hasNext(); ) {
                Command dependency = (Command)iterator.next();
                if (!runSet.contains(dependency)) {
                    run(dependency, NO_ARGS);
                }
            }
        }
        if (!runSet.contains(command)) {
            if (command instanceof Target) {
                log(brief, "[building " + command.getName() + "...]");
            } else if (command instanceof GeneratorCommand) {
                log(info, "[generating " + command.getName() + "...]");
            } else {
                log(brief, "[running " + command.getName() + "...]");
            }
            command.run(args);
            runSet.add(command);

            for (Iterator iterator = command.getTriggeredCommands(); iterator.hasNext(); ) {
                Command triggeredCommand = (Command)iterator.next();
                run(triggeredCommand, NO_ARGS);
            }

        }
    }

    /**
     * The set of commands that have been run.
     */
    private final Set runSet = new HashSet();

    /**
     * Clears the set of commands that have been run.
     */
    public void clearRunSet() {
        runSet.clear();
    }

    /**
     * Determines if a given command has been run.
     *
     * @param command   the command to test
     * @return true if <code>command</code> has not been run since the last call to {@link #clearRunSet}.
     */
    public boolean hasBeenRun(Command command) {
        return runSet.contains(command);
    }

    public static final String[] NO_ARGS = {};

    /*---------------------------------------------------------------------------*\
     *                          General utilities                                *
    \*---------------------------------------------------------------------------*/

    /**
     * Converts a given file or class path to the correct format for the
     * underlying platform. For example, if the underlying platform uses
     * '/' to separate directories in a file path then any instances of
     * '\' in <code>path</code> will be converted to '/'.
     *
     * @param   path         to the path to convert
     * @param   isClassPath  specifies if <code>path</code> is a class path
     * @return  the value of <code>path</code> reformatted (if necessary) to
     *                be correct for the underlying platform
     */
    public static String toPlatformPath(String path, boolean isClassPath) {
        char fileSeparatorChar = File.separatorChar;
        if (fileSeparatorChar == '/') {
            path = path.replace('\\', '/');
        } else if (fileSeparatorChar == '\\') {
            path = path.replace('/', '\\');
        } else {
            throw new RuntimeException("OS with unknown separator: '" + fileSeparatorChar + "'");
        }
        if (isClassPath) {
            char pathSeparatorChar = File.pathSeparatorChar;
            if (pathSeparatorChar == ':') {
                path = path.replace(';', ':');
            } else if (pathSeparatorChar == ';') {
                // Need special processing so as to not convert "C:\" into "C;\"
                char[] pathChars = path.toCharArray();
                int start = 0;
                for (int i = 0; i != pathChars.length; ++i) {
                    if (pathChars[i] == ':' || pathChars[i] == ';') {
                        if (i - start == 1) {
                            // If there is only a single character between the start of the
                            // current path component and the next ':', we assume that this
                            // is a drive letter and so need to leave the ':' unchanged
                        } else {
                            pathChars[i] = ';';
                            start = i + 1;
                        }
                    }
                }

                path = new String(pathChars);
            } else {
                throw new RuntimeException("OS with unknown path separator: '"+ pathSeparatorChar+"'");
            }
        }
        return path;
    }

    /**
     * Fix a URL path if on Windows. This is a workaround that is implemented by javamake
     * as well. Here is the original description of the problem from the javamake sources:
     *
     *    On Windows, if a path is specified as "file://c:\...", (i.e. with the drive name) URLClassLoader works
     *    unbelievably slow. However, if an additional slash is added, like : "file:///c:\...", the speed becomes
     *    normal. To me it looks like a bug, but, anyway, I am taking measure here.
     *
     * @param path the path to fix
     */
    public String fixURL(String path) {
        if (getPlatform() instanceof Windows_X86) {
            if (path.charAt(1) == ':') {
                path = "/" + path;
            }
        }
        return path;
    }

    /**
     * Converts a given class path string to a URL array appropriate for creating a URLClassLoader.
     *
     * @param cp  a class path string
     * @return an array of "file://" URLs created from <code>cp</code>
     */
    public URL[] toURLClassPath(String cp) {
        String[] paths = toPlatformPath(cp, true).split(File.pathSeparator);
        URL[] urls = new URL[paths.length];
        for (int i = 0; i != paths.length; ++i) {
            File path = new File(paths[i]);
            try {
                String url = "file://" + fixURL(path.getAbsolutePath());
                if (path.isDirectory()) {
                    url += "/";
                }
                urls[i] = new URL(url);
            } catch (MalformedURLException e) {
                throw new BuildException("badly formed class path: " + cp, e);
            }
        }
        return urls;
    }

    /**
     * Folds an array of objects into a single string.
     *
     * @param   parts   the array to fold
     * @param   offset  the offset at which folding starts
     * @param   length  the numbers of elements to fold
     * @param   delim   the delimiter to place between the folded elements
     * @return  the folded string
     */
    public static String join(Object[] parts, int offset, int length, String delim) {
        StringBuffer buf = new StringBuffer(1000);
        for (int i = offset; i != (offset+length); i++) {
            buf.append(parts[i]);
            if (i != (offset+length)-1) {
                buf.append(delim);
            }
        }
        return buf.toString();
    }

    /**
     * Folds an array of objects into a single string.
     *
     * @param   parts   the array to fold
     * @return  the folded string
     */
    public static String join(Object[] parts) {
        return join(parts, 0, parts.length, " ");
    }

    /**
     * Folds a list of objects into a single string. The toString method is used to convert each object into a String.
     *
     * @param   list    the list to fold
     * @param   offset  the offset at which folding starts
     * @param   length  the numbers of elements to fold
     * @param   delim   the delimiter to place between the folded elements
     * @return  the folded string
     */
    public static String join(List list, int offset, int length, String delim) {
        StringBuffer buf = new StringBuffer(1000);
        for (int i = offset; i != (offset+length); i++) {
            buf.append(list.get(i));
            if (i != (offset+length)-1) {
                buf.append(delim);
            }
        }
        return buf.toString();
    }

    /**
     * Ensures a specified directory exists, creating it if necessary.
     *
     * @param  path  the directory to test
     * @return the directory
     * @throws BuildException if <code>path</code> is not a directory or could not be created
     */
    public static File mkdir(File path) {
        if (!path.exists()) {
            if (!path.mkdirs()) {
                throw new BuildException("Could not create directory: " + path, 1);
            }
        } else {
            if (!path.isDirectory()) {
                throw new BuildException("Path is not a directory: " + path, 1);
            }
        }
        return path;
    }

    /**
     * Ensures a specified sub-directory exists, creating it if necessary.
     *
     * @param baseDir   the file in which the sub-directory will or does exist
     * @param subDir    the name of the sub-directory
     * @return the sub-directory
     */
    public static File mkdir(File baseDir, String subDir) {
        return mkdir(new File(baseDir, subDir));
    }

    /**
     * Searches for a file (or directory if <code>isDir == true</code>) based on a given name.
     *
     * @param dir     the directory to search
     * @param name    the name of the file or directory to find
     * @param isDir   true if <code>name</code> denotes a directory, false otherwise
     * @return the found file or null if it wasn't found
     */
    public static File find(File dir, final String name, final boolean isDir) {
        final File[] result = new File[1];
        new FileVisitor() {
            public boolean visit(File file) {
                if (file.getName().equals(name) && file.isDirectory() == isDir) {
                    result[0] = file;
                    return false;
                } else {
                    return true;
                }
            }
        }.run(dir);
        return result[0];
    }

    /**
     * Deletes a file or directory. This method does nothing if the file or directory
     * denoted by <code>file</code> does not exist.
     *
     * @param  file       the file or directory to delete
     * @throws BuildException if the deletion did not succeed
     */
    public static void delete(File file) throws BuildException {
        if (file.exists()) {
            if (!file.delete()) {
                throw new BuildException("cannot remove file/directory " + file.getPath());
            }
        }
    }

    /**
     * Clears a directory. This method does nothing if <code>dir.exists() == false</code>.
     *
     * @param  dir       the file or directory to clear
     * @param  deleteDir if true, <code>dir</code> is deleted once cleared
     * @throws BuildException if the <code>dir</code> is a file or clearing did not succeed
     */
    public static void clear(final File dir, boolean deleteDir) throws BuildException {
        if (dir.exists()) {
            if (dir.isDirectory()) {
                new FileVisitor() {
                    public boolean visit(File file) {
                        if (!file.equals(dir)) {
                            if (!file.delete()) {
                                throw new BuildException("cannot remove file/directory " + file.getPath());
                            }
                        }
                        return true;
                    }
                }.run(dir);

                if(deleteDir) {
                    Build.delete(dir);
                }
            } else {
                throw new BuildException("cannot clear non-directory " + dir.getPath());
            }
        }
    }

    /**
     * Copies a file. The parent directory of <code>to</code> is created if it doesn't exist.
     *
     * @param from    the source file
     * @param to      the destination file
     * @param append  if true, the content of <code>from</code> is appended to <code>to</code>
     * @throws BuildException if the copy failed
     */
    public static void cp(File from, File to, boolean append) {
        File toFileDir = to.getParentFile();
        mkdir(toFileDir);
        try {
            DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(from)));
            OutputStream os = new BufferedOutputStream(new FileOutputStream(to, append));

            byte[] content = new byte[(int)from.length()];
            dis.readFully(content);
            os.write(content);
            dis.close();
            os.close();
        } catch (FileNotFoundException ex) {
            throw new BuildException("copying " + from + " to " + to + " failed", ex);
        } catch (IOException ex) {
            throw new BuildException("copying " + from + " to " + to + " failed", ex);
        }
    }

    /**
     * Executes the UNIX chmod command on a file.
     *
     * @param path   the file to chmod
     * @param mode   the mode of the chmod
     * @return true if the command succeeded, false otherwise
     */
    public boolean chmod(File path, String mode) {
        try {
            exec("chmod " + mode + " " + path);
            return true;
        } catch (BuildException e) {
            return false;
        }
    }

    /**
     * Logs a message to the console if a given condition is true.
     *
     * @param b   only write the message to the console if this is true
     * @param msg the message to write
     */
    public void log(boolean b, String msg) {
        if (b) {
            System.out.println(msg);
        }
    }

    /*---------------------------------------------------------------------------*\
     *                           Command line interface                          *
    \*---------------------------------------------------------------------------*/

    /**
     * Creates an instance of the builder.
     */
    public Build() {
        File defaultProperties = new File("build.properties");
        if (defaultProperties.exists()) {
            properties = loadProperties(defaultProperties, null);
            File overideProperties = new File("build.override");
            if (overideProperties.exists()) {

                // Make it very clear to the user which properties in the standard properties
                // file are potentially being overridden
                System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>> " + overideProperties.getPath() + " file found <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
                properties = loadProperties(overideProperties, properties);
            }
        } else {
            throw new BuildException("could not find build.properties");
        }

        platform = Platform.createPlatform(this);
        ccompiler = platform.createDefaultCCompiler();
        jdk = new JDK(platform.getExecutableExtension());
        preprocessor = new Preprocessor();
        preprocessor.properties = properties;
        macroizer = new Macroizer();
        installBuiltinCommands();
        javaCompiler = new JavaCompiler(this);
    }


    /**
     * Prints some information describing the builder's configuration.
     *
     * @param out  where to print
     */
    private void printConfiguration() {

        log(info, "os=" + platform);
        log(info, "java.home=" + jdk.getHome());
        log(info, "java.vm.version=" + System.getProperty("java.vm.version"));
        if (ccompiler != null) {
            log(info, "C compiler=" + ccompiler.name);
        }

        if (verbose) {
            log(info, "Builder properties:");
            Enumeration keys = properties.propertyNames();
            while (keys.hasMoreElements()) {
                String name = (String)keys.nextElement();
                log(info, "    " + name + '=' + properties.getProperty(name));
            }
        }
    }

    /**
     * The command line entry point for the builder.
     *
     * @param args  the command line arguments
     */
    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        Build builder = new Build();
        try {
            builder.mainProgrammatic(args);
            System.out.println("Total time: " + ((System.currentTimeMillis() - start) / 1000) + "s");
        } catch (BuildException e) {
            if (e.exitValue != 0) {
                System.err.println("build failed: " + e.getMessage());
                if (builder == null || builder.verbose) {
                    e.printStackTrace();
                } else {
                    if (e.getCause() != null) {
                        System.err.print("caused by: ");
                        e.getCause().printStackTrace(System.err);
                    }
                }
            }
            System.exit(e.exitValue);
        }
    }

    /**
     * The programmatic entry point for the builder.
     *
     * @param args  the equivalent to the command line arguments
     */
    public void mainProgrammatic(String[] args) {
        args = extractBuilderArgs(args);
        printConfiguration();

        String name = args[0];
        if (args.length > 1) {
            String[] newArgs = new String[args.length - 1];
            System.arraycopy(args, 1, newArgs, 0, newArgs.length);
            args = newArgs;
        } else {
            args = NO_ARGS;
        }

        runCommand(name, args);
    }

    /**
     * Lists the builder's commands.
     */
    private void listCommands(boolean javaCompilationCommands, PrintStream out) {
        for (Iterator iterator = commands.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry)iterator.next();
            Command c = (Command)entry.getValue();
            if (javaCompilationCommands == (c instanceof Target)) {
                String name = (String)entry.getKey();
                while (name.length() < 19) {
                    name += ' ';
                }
                out.println("    " + name + " " + c.getDescription());
            }
        }
    }

    /**
     * Print the usage message.
     *
     * @param errMsg  an optional error message
     */
    public void usage(String errMsg) {

        PrintStream out = System.err;

        if (errMsg != null) {
            out.println(errMsg);
        }
        out.println("Usage: build [ build-options] [ target | command [ command_options ] ] ");
        out.println("where build-options include:");
        out.println();
        out.println("    -jpda:<port>        start JVM listening for JPDA connection on 'port'");
        out.println("    -jmem:<mem>         java memory option shortcut for '-java:-Xmx<mem>'");
        out.println("    -java:<opts>        extra java options (e.g. '-java:-Xms128M')");
        out.println("    -fork               executes Java commands in a separate JVM process");
        out.println("    -info               informational execution");
        out.println("    -verbose            verbose execution");
        out.println("    -q                  quiet execution");
        out.println("    -64                 build for a 64 bit system");
        out.println("    -nodeps             do not check dependencies (default for commands)");
        out.println("    -deps               check dependencies (default for targets)");
        out.println("    -plugins:<file>     load commands from properties in 'file'");
        out.println("    -D<name>=<value>    sets a builder property");
        out.println("    -h                  show this usage message and exit");
        out.println();
        out.println("--- Options only applying to targets ---");
        out.println();
        out.println("    -javac:<opts>       extra javac options (e.g. '-javac:-g:none')");
        out.println("    -nojavamake         do not use javamake for compilation");
        out.println("    -doccheck           run javadoc checker after compilation");
        out.println("    -javadoc            generate complete javadoc after compilation");
        out.println("    -javadoc:api        generate API javadoc after compilation");
        out.println("    -kernel             Alex????");
        out.println("    -hosted             Alex????");
        out.println();
        out.println("--- C compilation only options ---");
        out.println();
        out.println("    -comp:<name>        the C compiler used to build native executables");
        out.println("                        Supported: 'msc', 'gcc' or 'cc' or 'gcc-macosx' (any");
        out.println("                        other value disables C compilaton)");
        out.println("    -nocomp             disables C compilation (but not C file preprocessing)");
        out.println("    -cflags:<opts>      extra C compiler options (e.g. '-cflags:-g')");
        out.println("    -dll                builds squawk DLL instead of executable");
        out.println("    -o1                 optimize C compilation/linking for minimal size");
        out.println("    -o2                 optimize C compilation/linking for speed");
        out.println("    -o3                 optimize C compilation/linking for max speed");
        out.println("    -prod               build the production version of the VM");
        out.println("    -tracing            enable tracing in the VM");
        out.println("    -profiling          enable profiling in the VM");
        out.println("    -assume             enable assertions in the VM");
        out.println("    -typemap            enable type checking in the VM");
        out.println();
        out.println();
        out.println();
        out.println("The supported targets are:");
        listCommands(true, out);
        out.println();
        out.println("The supported commands are:");
        listCommands(false, out);
        out.println();
        out.println("If no command or target is given then all targets are brought up to date.");
    }

    /**
     * Loads a set of properties from a properties file.
     *
     * @param file     the properties file from which to load
     * @param defaults the properties being extended/overridden (if any)
     * @return the loaded properties or null if <code>file</code> does not exist
     * @throws BuildException if <code>file</code> exists and there was an error while loading
     */
    private Properties loadProperties(File file, Properties defaults) throws BuildException {
        try {
            Properties properties;
            if (defaults == null) {
                properties = new SubstitutionProperties();
            } else {
                properties = new SubstitutionProperties(defaults) {
                    public Object put(Object key, Object value) {
                        String oldValue = defaults.getProperty((String)key);
                        if (oldValue != null && !oldValue.equals(value)) {
                            System.out.println(">>>>>>> Overwrote " + key + " property: " + oldValue + " --> " + value);
                        }
                        return super.put(key, value);
                    }
                };
            }
            FileInputStream inputStream =  new FileInputStream(file);
            try {
                properties.load(inputStream);
            } finally {
                inputStream.close();
            }

            if (file.lastModified() > propertiesLastModified) {
                propertiesLastModified = file.lastModified();
            }
            return properties;
        } catch (IOException e) {
            throw new BuildException("error loading properties from " + file, e);
        }
    }

    /**
     * Changes the C compiler based on a given name. The new compiler will share the options of the old compiler.
     * If <code>name</code> does not denote a compiler known to the builder, then C compilation is disabled.
     *
     * @param name  the name of the new C compiler to use
     */
    private void updateCCompiler(String name) {
        CCompiler.Options options = ccompiler.options;
        if (name.equals("msc")) {
            ccompiler = new MscCompiler(this, platform);
        } else if (name.equals("gcc")) {
            ccompiler = new GccCompiler(this, platform);
        } else if (name.equals("gcc-macox")) {
            ccompiler = new GccMacOSXCompiler(this, platform);
        } else if (name.equals("cc")) {
            ccompiler = new CcCompiler(this, platform);
        } else {
            System.out.println("Unknown C compiler '" + name + "' - C compilation disabled");
            ccompiler = null;
            return;
        }
        ccompiler.options = options;
    }

    /**
     * Updates one of the build properties and updates the {@link propertiesLastModified} field
     * if the new value differs from the old value.
     *
     * @param name    the property's name
     * @param value   the property's new value
     * @return isBooleanProperty specifies if this is a boolean property
     */
    private void updateProperty(String name, String value, boolean isBooleanProperty) {
        String old = isBooleanProperty ? properties.getProperty(name, "true") : properties.getProperty(name);
        if (!value.equals(old)) {
            properties.setProperty(name, value);
            propertiesLastModified = System.currentTimeMillis();
            log(verbose, "[build properties updated]");
        }
    }

    /**
     * Determines if a boolean property has the value "true". The value is implicitly
     * true if it does not have an explicit value.
     *
     * @param name  the property's name
     * @return true if the property is true
     */
    public boolean getBooleanProperty(String name) {
        return properties.getProperty(name, "true").equals("true");
    }

    /**
     * Gets the value of a builder property.
     *
     * @param name  the property's name
     * @return the property's value
     */
    public String getProperty(String name) {
        return properties.getProperty(name);
    }

    /**
     * Parses and extracts the command line arguments passed to the builder that apply to the
     * builder in general as opposed to the command about to be run.
     *
     * @param args  the command line arguments
     * @return <code>args</code> after the builder specific args have been extracted
     */
    private String[] extractBuilderArgs(String[] args) {

        builderArgs.clear();

        int argc = 0;
        CCompiler.Options cOptions = ccompiler.options;
        boolean production = false;

        // Reset the default state for -tracing and -assume
        cOptions.tracing = false;
        cOptions.assume = false;

        String depsFlag = null;
        boolean help = false;

        while (args.length > argc) {
            String arg = args[argc];
            if (arg.charAt(0) != '-') {
                // Finished parsing builder args
                break;
            }

            builderArgs.add(arg);
            if (arg.equals("-dll")) {
                dll = true;
            } else if (arg.startsWith("-D")) {
                try {
                    String name = arg.substring("-D".length(), arg.indexOf('='));
                    String value = arg.substring(arg.indexOf('=') + 1);
                    updateProperty(name, value, false);
                } catch (IndexOutOfBoundsException e) {
                    usage("malformed -D option: " + arg);
                    throw new BuildException("malformed option");
                }
            } else if (arg.startsWith("-comp:")) {
                String compName = arg.substring("-comp:".length()).toLowerCase();
                updateCCompiler(compName);
            } else if (arg.equals("-nocomp")) {
                RomCommand rom = (RomCommand)getCommand("rom");
                rom.enableCompilation(false);
            } else if (arg.equals("-comp")) {
                RomCommand rom = (RomCommand)getCommand("rom");
                rom.enableCompilation(true);
            } else if (arg.equals("-nojavamake")) {
                javaCompiler.javamake = false;
            } else if (arg.equals("-mac")) {
                cOptions.macroize = true;
            } else if (arg.startsWith("-plugins:")) {
                File pluginsFile = new File(arg.substring("-plugins:".length()));
                loadPlugins(pluginsFile);
            } else if (arg.startsWith("-cflags:")) {
                cOptions.cflags += " " + arg.substring("-cflags:".length());
            } else if (arg.startsWith("-jpda:")) {
                String port = arg.substring("-jpda:".length());
                javaOptions += " -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=" + port;
            } else if (arg.startsWith("-jmem:")) {
                String mem = arg.substring("-jmem:".length());
                javaOptions += " -Xms" + mem + " -Xmx" + mem;
            } else if (arg.equals("-fork")) {
                forkJavaCommand = true;
            } else if (arg.startsWith("-java:")) {
                javaOptions += " " + arg.substring("-java:".length());
            } else if (arg.startsWith("-javac:")) {
                javacOptions += " " + arg.substring("-javac:".length());
            } else if (arg.equals("-javadoc")) {
                runJavadoc = true;
            } else if (arg.equals("-javadoc:api")) {
                runJavadoc = true;
                runJavadocAPI = true;
            } else if (arg.equals("-doccheck")) {
                runDocCheck = true;
            } else if (arg.equals("-o1")) {
                cOptions.o1 = true;
            } else if (arg.equals("-o2")) {
                cOptions.o2 = true;
            } else if (arg.equals("-o3")) {
                cOptions.o2 = true;
                cOptions.o3 = true;
            } else if (arg.startsWith("-prod")) {
                production = true;
            } else if (arg.equals("-64")) {
                cOptions.is64 = true;
            } else if (arg.equals("-tracing")) {
                cOptions.tracing = true;
            } else if (arg.equals("-profiling")) {
                cOptions.profiling = true;
            } else if (arg.equals("-kernel")) {
                cOptions.kernel = true;
            } else if (arg.equals("-hosted")) {
                cOptions.hosted = true;
            } else if (arg.equals("-assume")) {
                cOptions.assume = true;
            } else if (arg.equals("-typemap")) {
                cOptions.typemap = true;
            } else if (arg.equals("-verbose")) {
                verbose = true;
                info = true;
                brief = true;
            } else if (arg.equals("-info")) {
                info = true;
                brief = true;
            } else if (arg.equals("-q")) {
                verbose = false;
                info = false;
                brief = false;
            } else if (arg.equals("-deps")) {
                depsFlag = "-deps";
            } else if (arg.equals("-nodeps")) {
                depsFlag = "-nodeps";
            } else if (arg.equals("-h")) {
                help = true;
            } else {
                usage("Unknown option "+arg);
                throw new BuildException("invalid option");
            }
            argc++;
        }

        if (help) {
            // Show help and exit
            usage(null);
            throw new BuildException("", 0);
        }

        // Synchronize MACROIZE property with '-mac'
        if (cOptions.macroize != getBooleanProperty("MACROIZE")) {
            cOptions.macroize |= getBooleanProperty("MACROIZE");
            updateProperty("MACROIZE", cOptions.macroize ? "true" : "false", true);
        }

        // Synchronize SQUAWK_64 property with '-64'
        if (cOptions.is64 != getBooleanProperty("SQUAWK_64")) {
            cOptions.is64 |= getBooleanProperty("SQUAWK_64");
            updateProperty("SQUAWK_64", cOptions.is64 ? "true" : "false", true);
        }
        preprocessor.processS64 = getBooleanProperty("SQUAWK_64");

        // Synchronize KERNEL_SQUAWK property with '-kernel'
        if (cOptions.kernel != getBooleanProperty("KERNEL_SQUAWK")) {
            cOptions.kernel |= getBooleanProperty("KERNEL_SQUAWK");
            updateProperty("KERNEL_SQUAWK", cOptions.kernel ? "true" : "false", true);
        }
        
        
          // Synchronize NATIVE_VERIFICATION property with '-nativeVerification'
        if (cOptions.nativeVerification != getBooleanProperty("NATIVE_VERIFICATION")) {
            cOptions.nativeVerification |= getBooleanProperty("NATIVE_VERIFICATION");
            updateProperty("NATIVE_VERIFICATION", cOptions.nativeVerification ? "true" : "false", true);
        }

        // Synchronize ASSERTIONS_ENABLED
        preprocessor.assertionsEnabled |= getBooleanProperty("ASSERTIONS_ENABLED");

        // Initialize C compiler floats flag
        cOptions.floatsSupported = getBooleanProperty("FLOATS");

        if (properties.getProperty("WRITE_BARRIER") != null) {
            log(info, "Warning: the WRITE_BARRIER is already set.  This property must not be set explicity as it is derived from the GC property");
        }
        if (properties.getProperty("LISP2_BITMAP") != null) {
            log(info, "Warning: the LISP2_BITMAP is already set.  This property must not be set explicity as it is derived from the GC property");
        }
        if (properties.getProperty("GC", "").equals("com.sun.squawk.Lisp2GenerationalCollector")) {
            properties.setProperty("WRITE_BARRIER", "true");
            cOptions.cflags += " -DWRITE_BARRIER";
        } else {
            properties.setProperty("WRITE_BARRIER", "false");
        }
        if (properties.getProperty("GC", "").startsWith("com.sun.squawk.Lisp2")) {
            properties.setProperty("LISP2_BITMAP", "true");
            cOptions.cflags += " -DLISP2_BITMAP";
        } else {
            properties.setProperty("LISP2_BITMAP", "false");
        }

        // The -tracing, and -assume options are turned on by default if -production was not specified
        if (!production) {
            cOptions.tracing = true;
            cOptions.assume = true;
        }

        // If no arguments were supplied, then
        if (argc == args.length) {

            checkDependencies = true;
            if (depsFlag != null) {
                checkDependencies = (depsFlag == "-deps");
            }

            return new String[] { "<all>" };
        } else {
            String[] cmdAndArgs = new String[args.length - argc];
            System.arraycopy(args, argc, cmdAndArgs, 0, cmdAndArgs.length);

            checkDependencies = (getCommand(args[argc]) instanceof Target);
            if (depsFlag != null) {
                checkDependencies = (depsFlag == "-deps");
            }

            return cmdAndArgs;
        }


    }

    /*---------------------------------------------------------------------------*\
     *                 JDK tools (javac, javadoc) execution                      *
    \*---------------------------------------------------------------------------*/


    /**
     * The selector used for finding the Java source files in a directory.
     */
    public static final FileSet.Selector JAVA_SOURCE_SELECTOR = new FileSet.SuffixSelector(".java");

    /**
     * The selector used for finding the Java class files in a directory.
     */
    public static final FileSet.Selector JAVA_CLASS_SELECTOR = new FileSet.SuffixSelector(".class");

    /**
     * The selector used for finding the HTML files in a directory.
     */
    public static final FileSet.Selector HTML_SELECTOR = new FileSet.SuffixSelector(".html");

    /**
     * Runs javadoc over a set of Java source files.
     *
     * @param   baseDir      the parent directory under which the "javadoc" directory exists (or will be created) for the output
     * @param   classPath    the class path option used to compile the source files. This is used to find the javadoc
     *                       generated for the classes in the class path.
     * @param   srcDirs      the directories containing the input files
     * @param   packages     the names of the packages whose sources are to be processed
     */
    public void javadoc(File baseDir, String classPath, File[] srcDirs, String packages) {
        File docDir = mkdir(new File("docs"), "Javadoc");
        File javadocDir = mkdir(docDir, baseDir.getName());

        log(info, "[running javadoc (output dir: " + javadocDir + ") ...]");

        String srcPath = "";
        String linkOptions = "";
        if (classPath != null) {
            StringTokenizer st = new StringTokenizer(classPath, File.pathSeparator);
            while (st.hasMoreTokens()) {
                File path = new File(st.nextToken());
                if (path.getName().equals("classes")) {
                    String pathBase = path.getParent();
                    linkOptions += " -link ../" + pathBase;             //" -link ../../" + pathBase + "/javadoc";
                    srcPath += File.pathSeparator + new File(pathBase, "src");
                }
            }
        }

        for (int i = 0; i != srcDirs.length; ++i) {
            srcPath += File.pathSeparator + srcDirs[i];
        }

        javaCompiler.javadoc((" -d "+javadocDir+
             (classPath == null ? "" : " -classpath " + classPath) +
             " -taglet com.sun.squawk.builder.ToDoTaglet -tagletpath build.jar " +
             linkOptions +
             (runJavadocAPI ? "" : " -private") +
             " -quiet" +
             " -breakiterator" +
             " -sourcepath " + srcPath + " "+
             packages).split("\\s+"), false);
    }

    /**
     * Runs the Doc Check javadoc doclet over a set of Java source files.
     *
     * @param   baseDir      the parent directory under which the "doccheck" directory exists (or will be created) for the output
     * @param   classPath    the class path option used to compile the source files. This is used to find the javadoc
     *                       generated for the classes in the class path.
     * @param   srcDirs      the directories containing the input files
     * @param   packages     the names of the packages whose sources are to be processed
     */
    public void doccheck(File baseDir, String classPath, File[] srcDirs, String packages) {

        File doccheckDir = mkdir(baseDir, "doccheck");
        log(info, "[running doccheck (output dir: " + doccheckDir + ") ...]");

        String srcPath = "";
        for (int i = 0; i != srcDirs.length; ++i) {
            srcPath += File.pathSeparator + srcDirs[i];
        }

        exec(jdk.javadoc() + " -d "+ doccheckDir +
             (classPath == null ? "" : " -classpath " + classPath) +
             " -execDepth 2" +
             " -evident 5" +
             " -private" +
             " -sourcepath " + srcPath +
             " -docletpath " + new File("tools", "doccheck.jar") +
             " -doclet com.sun.tools.doclets.doccheck.DocCheck " +
             packages
            );
    }

    /**
     * Extracts a list of packages from a given set of Java source files.
     *
     * @param fs   the Java source files
     * @param packages the list to append any unique package names to
     */
    private void extractPackages(FileSet fs, List packages) {
        for (Iterator iterator = fs.list().iterator(); iterator.hasNext();) {
            File file = (File)iterator.next();
            assert file.getPath().endsWith(".java");

            // Make file relative
            String packagePath = fs.getRelativePath(file.getParentFile());
            if (packagePath.length() > 0) {
                String pkg = packagePath.replace(File.separatorChar, '.');
                if (!packages.contains(pkg)) {
                    packages.add(pkg);
                }
            }
        }
    }

    /**
     * Runs javadoc and doccheck doclet over a set of sources that have just been compiled.
     *
     * @param classPath the class path that was used to compile the sources
     * @param baseDir   the directory under which the "preprocessed" and "classes" directories exist
     * @param srcDirs   the directories containing the input files
     */
    private void runDocTools(String classPath, File baseDir, File[] srcDirs) {
        boolean preprocessed = srcDirs.length == 1 && srcDirs[0].getName().equals("preprocessed");
        List packageList = new ArrayList();
        for (int i = 0; i != srcDirs.length; ++i) {
            File srcDir = srcDirs[i];
            extractPackages(new FileSet(srcDir, JAVA_SOURCE_SELECTOR), packageList);
        }
        String packages = join(packageList, 0, packageList.size(), " ");

        if (runDocCheck) {
            doccheck(baseDir, classPath, srcDirs, packages);
        }
        if (runJavadoc) {
            if (preprocessed) {
                File preprocessedDir = new File(baseDir, "preprocessed");
                // Copy all the .html files in the dirs to the "preprocessed" dir
                for (int i = 0; i != srcDirs.length; ++i) {
                    File srcDir = srcDirs[i];
                    FileSet htmlFiles = new FileSet(srcDir, HTML_SELECTOR);
                    for (Iterator iterator = htmlFiles.list().iterator(); iterator.hasNext(); ) {
                        File htmlFile = (File)iterator.next();
                        File toHtmlFile = htmlFiles.replaceBaseDir(htmlFile, preprocessedDir);
                        cp(htmlFile, toHtmlFile, false);
                    }
                }
            }

            // Run javadoc
            javadoc(baseDir, classPath, srcDirs, packages);

            if (preprocessed) {
                // Copy the "doc-files" directories
                for (int i = 0; i != srcDirs.length; ++i) {
                    File srcDir = srcDirs[i];
                    DirSet docFileDirs = new DirSet(srcDir, new FileSet.NameSelector("doc-files"));
                    for (Iterator iterator = docFileDirs.list().iterator(); iterator.hasNext(); ) {
                        File docFileDir = (File)iterator.next();
                        if (docFileDir.isDirectory()) {
                            File toDocFileDir = docFileDirs.replaceBaseDir(docFileDir, new File(baseDir, "javadoc"));
                            FileSet docFiles = new FileSet(docFileDir, (FileSet.Selector)null);
                            for (Iterator j = docFiles.list().iterator(); j.hasNext(); ) {
                                File docFile = (File)j.next();
                                if (docFile.getPath().indexOf("CVS") == -1) {
                                    File toDocFile = docFiles.replaceBaseDir(docFile, toDocFileDir);
                                    cp(docFile, toDocFile, false);
                                }
                            }
                        }
                    }
                }
            }
        }
    }



    /**
     * Compiles a set of Java sources into class files with a Java source compiler. The sources
     * are initially {@link Preprocessor preprocessed} if <code>preprocess == true</code> and
     * the output is written into the "preprocessed" directory. The sources are then compiled
     * with the Java compiler into the "classes" directory. If <code>j2me == true</code> then
     * the class files in "classes" are preverified and written to "j2meclasses".
     *
     * @param   classPath  the class path
     * @param   baseDir    the base directory for generated directories (i.e. "preprocessed", "classes" and "j2meclasses")
     * @param   srcDirs the set of directories that are searched recursively for the Java source files to be compiled
     * @param   j2me       specifies if the classes being compiled are to be deployed on a J2ME platform
     * @param   preprocess runs the {@link Preprocessor} over the sources if true
     * @return the directory the compiled classes were written to
     */
    public void javac(String classPath, File baseDir, File[] srcDirs, boolean j2me, String version, List extraArgs, boolean preprocess) {

        // Preprocess the sources if required
        if (preprocess) {
            srcDirs = new File[] { preprocess(baseDir, srcDirs, j2me) };
        }

        // Get the javac output directory
        File classesDir = mkdir(baseDir, "classes");

        // Prepare and run the Java compiler
        javaCompiler.reset();
        if (j2me) {
            // This is required to make the preverifier happy
            javaCompiler.arg("-target", "1.2").
                         arg("-source", "1.3");
        } else {
            if (version != null) {
                javaCompiler.arg("-target", version).
                             arg("-source", version);
            } else {
                javaCompiler.arg("-target", "1.4").
                             arg("-source", "1.4");
            }
        }

        if (extraArgs != null) {
            for (Iterator iter = extraArgs.iterator(); iter.hasNext(); ) {
                String arg = (String)iter.next();
                javaCompiler.arg(arg);
            }
        }

        javaCompiler.arg("-g").args(javacOptions);
        javaCompiler.compile(classPath, classesDir, srcDirs, j2me);

        // Run the doccheck and javadoc utils
        if (runDocCheck || runJavadoc) {
            runDocTools(classPath, baseDir, srcDirs);
        }

        // Run the preverifier for a J2ME compilation
        if (j2me) {
            preverify(classPath, baseDir);
        }
    }

    /**
     * Preprocess a given set of Java source files.
     *
     * @param   baseDir    the directory under which the "preprocessed" directory exists
     * @param   srcDirs    the set of directories that are searched recursively for the source files to be compiled
     * @param   j2me       specifies if the classes being compiled are to be deployed on a J2ME platform
     * @return the preprocessor output directory
     */
    public File preprocess(File baseDir, File[] srcDirs, boolean j2me) {
        // Get the preprocessor output directory
        final File preprocessedDir = mkdir(baseDir, "preprocessed");

        // Preprocess the sources
        preprocessor.processAssertions = j2me;
        preprocessor.verbose = verbose;

        for (int i = 0; i != srcDirs.length; ++i) {

            File sourceDir = srcDirs[i];

            // A selector that matches a source file whose preprocessed copy does not exist,
            // is younger than the source file or has a last modification date
            // earlier than the last modification date of the properties
            FileSet.Selector outOfDate = new FileSet.DependSelector(new FileSet.SourceDestDirMapper(sourceDir, preprocessedDir)) {
                public boolean isSelected(File file) {
                    if (super.isSelected(file)) {
                        return true;
                    }
                    File preprocessedFile = getMapper().map(file);
                    long fileLastModified = preprocessedFile.lastModified();
                    return (fileLastModified < propertiesLastModified);
                }
            };

            FileSet.Selector selector = new FileSet.AndSelector(JAVA_SOURCE_SELECTOR, outOfDate);
            FileSet fs = new FileSet(sourceDir, selector);
            preprocessor.execute(fs, preprocessedDir);
        }
        return preprocessedDir;
    }

    /**
     * Run the CLDC preverifier over a set of classes in the "classes" directory
     * and write the resulting classes to the "j2meclasses" directory.
     *
     * @param   classPath  directories in which to look for classes
     * @param   baseDir    the directory under which the "j2meclasses" and "classes" directories
     */
    public void preverify(String classPath, File baseDir) {


        // Get the preverifier input and output directories
        File classesDir = new File(baseDir, "classes");
        File j2meclassesDir = mkdir(baseDir, "j2meclasses");

        // See if any of the classes actually need re-preverifying
        FileSet.Selector outOfDate = new FileSet.DependSelector(new FileSet.SourceDestDirMapper(classesDir, j2meclassesDir));
        if (!new FileSet(classesDir, outOfDate).list().iterator().hasNext()) {
            return;
        }

        log(info, "[running preverifier ...]");

        // Ensure that the preverifier is executable which may not be the case if
        // Squawk was checked out with a Java CVS client (Java doesn't know anything
        // about 'execute' file permissions and so cannot preserve them).
        chmod(platform.preverifier(), "+x");

        if (classPath == null) {
            classPath = "";
        } else {
            classPath = "-classpath " + toPlatformPath(classPath, true);
        }
        exec(platform.preverifier() + " " + classPath + " -d " + j2meclassesDir + " " + new File(baseDir, "classes"));
    }

    /*---------------------------------------------------------------------------*\
     *                     Jar-like tool                                         *
    \*---------------------------------------------------------------------------*/

    /**
     * Creates a jar file from a given set of files. This is roughly equivalent to the
     * functionality of the jar command line tool when using the 'c' switch.
     *
     * @param out      the jar file to create
     * @param files    the files to put in the jar file
     * @param manifest the entries that will used to create manifest in the jar file.
     *                 If this value is null, then no manifest will be created
     */
    public void createJar(File out, FileSet[] fileSets, Manifest manifest) {
        try {
            FileOutputStream fos = new FileOutputStream(out);

            // Manifest version must exist otherwise a null manifest is written jar.
            if(manifest != null) {
                if(!manifest.getMainAttributes().containsKey(Attributes.Name.MANIFEST_VERSION)) {
                    manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "0.1 [Squawk Builder]");
                }
            }
            ZipOutputStream zos = manifest == null ? new JarOutputStream(fos) : new JarOutputStream(fos, manifest);

            for (int i = 0; i < fileSets.length; i++) {
                FileSet fs = fileSets[i];
                for (Iterator iterator = fs.list().iterator(); iterator.hasNext(); ) {
                    File file = (File)iterator.next();
                    String entryName = fs.getRelativePath(file).replace(File.separatorChar, '/');


                    ZipEntry e = new ZipEntry(entryName);
                    e.setTime(file.lastModified());
                    if (file.length() == 0) {
                        e.setMethod(ZipEntry.STORED);
                        e.setSize(0);
                        e.setCrc(0);
                    }
                    zos.putNextEntry(e);
                    byte[] buf = new byte[1024];
                    int len;
                    InputStream is = new BufferedInputStream(new FileInputStream(file));
                    while ((len = is.read(buf, 0, buf.length)) != -1) {
                        zos.write(buf, 0, len);
                    }
                    is.close();
                    zos.closeEntry();
                }
            }
            zos.close();
        } catch (IOException e) {
            throw new BuildException("IO error creating jar file", e);
        }
    }

    /*---------------------------------------------------------------------------*\
     *                     Java command execution                                *
    \*---------------------------------------------------------------------------*/

    /**
     * Executes a Java program. The program will be executed in a JVM as a sub-process if
     * the '-fork' command line switch was used or if the command requires special JVM
     * switches or if the command must be run by extending the boot class path.
     *
     * @param classPath         the classpath
     * @param bootclasspath     specifies if the class path should be appended to boot class path
     * @param vmArgs            extra arguments to be passed to the JVM
     * @param mainClassName     the main class of the program to run
     * @param appArgs           the arguments to be passed to the application
     * @throws BuildException if a sub-process is used and it does not exit with 0
     */
    public void java(String classPath, boolean bootclasspath, String vmArgs, String mainClassName, String[] appArgs) {

        if (!forkJavaCommand && !bootclasspath && (vmArgs == null || vmArgs.length() == 0)) {
            if (info) {
                StringBuffer buffer = new StringBuffer();
                buffer.append("[exec ");
                buffer.append(mainClassName);
                buffer.append(".main(");
                for (int i=0; i < appArgs.length; i++) {
                    if (i > 0) {
                        buffer.append(", ");
                    }
                    buffer.append('\"');
                    buffer.append(appArgs[i]);
                    buffer.append('\"');
                }
                buffer.append(" ...]");
                log(info, buffer.toString());
            }
            Method main = null;
            URL[] cp = toURLClassPath(classPath);
            ClassLoader loader = new URLClassLoader(cp);
            try {
                Class mainClass = loader.loadClass(mainClassName);
                main = mainClass.getMethod("main", new Class[] {String[].class});
            } catch (ClassNotFoundException e) {
                log(verbose, "[could not find class " + mainClassName + " - spawning new JVM]");
            } catch (NoSuchMethodException e) {
                log(verbose, "[could not find method \"main\" in class " + mainClassName + " - spawning new JVM]");
            }

            if (main != null) {
                try {
                    main.invoke(null, new Object[] { appArgs });
                    return;
                } catch (IllegalAccessException e) {
                    throw new BuildException("cannot reflectively invoke " + main, e);
                } catch (InvocationTargetException e) {
                    throw new BuildException("error invoking " + main, e.getTargetException());
                } catch (IllegalArgumentException e) {
                    throw new BuildException("error invoking " + main, e);
                }
            }
        }

        exec(jdk.java() + " " +
             (bootclasspath ? "-Xbootclasspath/a:" : "-cp ") +
             toPlatformPath(classPath, true) + " " +
             vmArgs + " " +
             javaOptions + " " +
             mainClassName + " " + join(appArgs));
    }

    /*---------------------------------------------------------------------------*\
     *                          System command execution                         *
    \*---------------------------------------------------------------------------*/

    /**
     * Executes a command in a sub-process
     *
     * @param cmd  the command to execute
     * @throws BuildException if the sub-process does not exit with 0
     */
    public void exec(String cmd) {
        exec(cmd, null, null);
    }

    /**
     * Executes a command in a sub-process
     *
     * @param cmd  the command to execute
     * @param envp array of strings, each element of which has environment
     *             variable settings in the format name=value, or null if the
     *             subprocess should inherit the environment of the current process.
     * @param dir  the working directory of the subprocess, or null if the subprocess
     *             should inherit the working directory of the current process.
     * @throws BuildException if the sub-process does not exit with 0
     */
    public void exec(String cmd, String[] envp, File dir) {
        log(info, "[exec '" + cmd + "' ...]");
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(cmd, envp, dir);
            StreamGobbler errorGobbler  = new StreamGobbler(process.getErrorStream(), System.err);
            StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), System.out);
            errorGobbler.start();
            outputGobbler.start();

            int exitVal = process.waitFor();
            errorGobbler.join();
            outputGobbler.join();
            log(verbose, "EXEC result =====> " + exitVal);
            if (exitVal != 0) {
                throw new BuildException("Process.exec("+cmd+") returned "+exitVal, exitVal);
            }
        } catch (IOException ioe) {
            throw new BuildException("IO error during Process.exec("+cmd+")", ioe);
        } catch (InterruptedException ie) {
            throw new BuildException("Process.exec("+cmd+") was interuppted ", ie);
        } finally {
            if (process != null) {
                process.destroy();
                process = null;
            }
        }
    }
}

