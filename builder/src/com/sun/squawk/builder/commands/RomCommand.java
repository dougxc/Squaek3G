/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: RomCommand.java,v 1.36 2006/03/22 02:33:01 cp198493 Exp $
 */
package com.sun.squawk.builder.commands;

import java.io.*;
import java.util.*;
import com.sun.squawk.builder.*;
import com.sun.squawk.builder.ccompiler.*;
import com.sun.squawk.builder.util.*;

/**
 * This is the command that produces the bootstrap suite for the Squawk VM and
 * also compiles the Squawk VM.
 *
 * @author Doug Simon
 */
public class RomCommand extends Command {

    public static final File VM_SRC_DIR = new File("slowvm/src/vm");
    public static final File VM_BLD_DIR = new File("slowvm/build");
    public static final File FP_SRC_DIR = new File("slowvm/src/vm/fp");
    public static final File UTIL_SRC_DIR = new File("slowvm/src/vm/util");
    public static final File VM_SRC_RTS_DIR = new File("slowvm/src/rts");
    public static final File VM_SRC_FILE = new File(VM_SRC_DIR, "squawk.c");
    public static final File VM2C_SRC_FILE = new File(VM_SRC_DIR, "vm2c.c.spp");

    /**
     * The name of the bootstrap suite.
     */
    private String bootstrapSuiteName;

    /**
     * Determines if the C compilation step should be executed or not.
     */
    private boolean compilationEnabled = true;

    public RomCommand(Build env) {
        super(env, "rom");
    }

    /**
     * {@inheritDoc}
     */
    public String getDescription() {
        return "processes a collection of modules to produce the bootstrap suite and Squawk VM executable";
    }

    private void usage(String errMsg) {
        PrintStream out = System.out;

        out.println();
        out.println("usage: [-coptions] rom [-options] [modules...]");
        out.println("where options include:");
        out.println();
        out.println("    <any romizer option - run 'romize -h' for more details>");
        out.println();
        out.println("A module is a directory that has a 'j2meclasses' subdirectory or a jar/zip");
        out.println("file. More than one suite can be created by separating the modules for each");
        out.println("suite with '--'. For example:");
        out.println();
        out.println("  rom j2me graphics -- translator -- samples");
        out.println();
        out.println("will generate 'squawk.suite', 'translator.suite' and 'samples.suite' where");
        out.println("each suite is bound to the previous one");
        out.println("");
        out.println("The '-coptions' refer to the C compilations options supported by the builder.");
        out.println();
    }

    /**
     * Converts the arguments given to this command to romizer arguments.
     *
     * @param   args   the arguments passed to this command
     * @param   arch   the target architecture hint provided by the C compiler
     * @return  <code>args</code> reformatted for the romizer
     */
    private String[] convertToRomizerOptions(String[] args, String arch) {
        String extraCP = "";
        List romizerArgs = new ArrayList();
        String endian = env.getPlatform().isBigEndian() ? "big" : "little";

        int argc = 0;
        while (argc != args.length) {
            String arg = args[argc];
            if (arg.charAt(0) == '-') {
                if (arg.startsWith("-cp:")) {
                    extraCP = arg.substring("-cp:".length());
                } else if (arg.startsWith("-h")) {
                    usage(null);
                    // Stop the builder
                    throw new BuildException("", 0);
                } else if (arg.startsWith("-o:")) {
                    bootstrapSuiteName = arg.substring("-o:".length());
                } else if (arg.startsWith("-arch:")) {
                    arch = arg.substring("-arch:".length());
                } else if (arg.startsWith("-endian:")) {
                    endian = arg.substring("-endian:".length());
                } else {
                    romizerArgs.add(arg);
                }
            } else {
                break;
            }
            argc++;
        }

        // The remaining args are the modules making up one or more suites
        boolean isBootstrapSuite = true;
        while (argc != args.length) {

            List classesLocations = new ArrayList();
            String suiteName = null;
            String cp = "";
            boolean createJars = true;

            while (argc != args.length) {

                String module = args[argc++];
                String moduleClasses;

                if (module.equals("--")) {
                    break;
                }

                if (module.charAt(0) == '-') {
                    throw new BuildException("cannot specify romizer options for any suite except the first: " + module);
                }

                if (module.endsWith(".jar") || module.endsWith(".zip")) {
                    moduleClasses = module;
                    if (module.endsWith("_classes.jar")) {
                        // This is most likely the jar file build by a previous execution of the romizer
                        module = module.substring(0, module.length() - "_classes.jar".length());
                        createJars = false;
                    } else {
                        module = module.substring(0, module.length() - ".jar".length());
                    }
                } else {
                    File j2meclasses = new File(module, "j2meclasses");
                    if (!j2meclasses.exists() || !j2meclasses.isDirectory()) {
                        throw new BuildException("'" + module + "' module is not a jar/zip file and does not have a 'j2meclasses' subdirectory");
                    }
                    moduleClasses = j2meclasses.getPath();
                }

                // The default name of the current suite is the name of the first module
                if (suiteName == null) {
                    suiteName = module;
                }

                // Update the class path for the current suite
                if (cp == "") {
                    cp = moduleClasses;
                } else {
                    cp += File.pathSeparator + moduleClasses;
                }

                // Add the directory/jar to the set of locations scanned for classes
                classesLocations.add(moduleClasses);
                
                // Add the res folder to add the necessary resources
                File resources = new File(module, "res");
                if (resources.exists() && resources.isDirectory()) {
                    classesLocations.add(resources.getPath());
                }
            }

            if (isBootstrapSuite) {
                if (extraCP != "") {
                    cp += File.pathSeparator + extraCP;
                }
                romizerArgs.add("-cp:" + cp);
                romizerArgs.add("-arch:" + arch);
                romizerArgs.add("-endian:" + endian);
            } else {
                romizerArgs.add("--");
                romizerArgs.add("-cp:" + cp);
                romizerArgs.add("-o:" + suiteName);
            }

            if (createJars) {
                romizerArgs.add("-jars");
            }
            romizerArgs.addAll(classesLocations);

            isBootstrapSuite = false;
        }

        // Re-insert the -o option
        romizerArgs.add(0, "-o:" + bootstrapSuiteName);

        args = new String[romizerArgs.size()];
        romizerArgs.toArray(args);
        return args;
    }

    /**
     * Creates the "buildflags.h" file containing the string constant indicating what
     * C compiler flags the VM was built with.
     *
     * @param buildFlags   the C compiler flags
     */
    private void createBuildFlagsHeaderFile(String buildFlags) {
        File buildFlagsFile = new File(VM_SRC_DIR, "buildflags.h");
        try {
            FileOutputStream fos = new FileOutputStream(buildFlagsFile);
            PrintStream out = new PrintStream(fos);
            out.println("#define BUILD_FLAGS \"" + buildFlags + '"');
            fos.close();
        } catch (IOException e) {
            throw new BuildException("could not create " + buildFlagsFile, e);
        }
    }

    private File getFileDerivedFromSppFile(File sppFile) {
        String path = sppFile.getPath();
        path = path.substring(0, path.length() - ".spp".length());
        return new File(path);
    }

    private void updateVM2CGeneratedFile() {
        FileSet.Selector isOutOfDate = new FileSet.AndSelector(Build.JAVA_SOURCE_SELECTOR, new FileSet.DependSelector(new FileSet.Mapper() {
            public File map(File from) {
                return VM2C_SRC_FILE;
            }
        }));

        File srcDir = new File("j2me", "src");
        File preDir = new File("j2me", "preprocessed");

        // Rebuilds the generated file if any of the *.java files in j2me/src or j2me/preprocessed have
        // a later modification date than the generated file.
        if (!VM2C_SRC_FILE.exists() || (srcDir.exists() && !new FileSet(srcDir, isOutOfDate).list().isEmpty()) || (preDir.exists() && !new FileSet(preDir, isOutOfDate).list().isEmpty())) {
            // Verify that the vm2c commands were installed in my env, if not then throw an exception
            // This is to handle case where vm2c did not install itself, but we still wanted to run the rom command to do just compilation
            Command command = env.getCommand("runvm2c");
            if (command == null) {
            	throw new BuildException("The module vm2c and runvm2c we're not installed, this is very likely due to these modules requiring the use of JDK 1.5 or higher");
            }
            // Ensure that the *existing* preprocessed files in j2me are in sync with the original sources
            FileSet.Selector selector = new FileSet.AndSelector(Build.JAVA_SOURCE_SELECTOR, new FileSet.DependSelector(new FileSet.SourceDestDirMapper(srcDir, preDir)) {
                public boolean isSelected(File file) {
                    File dependentFile = mapper.map(file);
                    return dependentFile.exists() && dependentFile.lastModified() < file.lastModified();

                }
            });
            List ood = new FileSet(srcDir, selector).list();
            if (!ood.isEmpty()) {
                throw new BuildException("need to re-build 'j2me' as the preprocessed version of the following files are out of date: " + ood);
            }

            List args = new ArrayList();
            args.add("-o:" + VM2C_SRC_FILE);
            args.add("-cp:j2me/j2meclasses");
            args.add("-root:" + env.getProperty("GC"));
            args.addAll(new FileSet(preDir, Build.JAVA_SOURCE_SELECTOR).list());

            File argsFile = new File("vm2c", "vm2c.input");
            JavaCompiler.createArgsFile(args, argsFile);

            VM2C_SRC_FILE.delete();
            VM2C_SRC_FILE.getParentFile().mkdirs();
            env.log(env.info, "[running 'vm2c @" + argsFile + "'...]");
            env.runCommand("runvm2c", new String[] { "@" + argsFile.getPath() });
            VM2C_SRC_FILE.setReadOnly();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void run(String[] args) {
        CCompiler ccompiler = env.getCCompiler();

        args = runRomizer(args);

        JDK jdk = env.getJDK();
        createBuildFlagsHeaderFile(ccompiler.options(false).trim());
        updateVM2CGeneratedFile();

        // Preprocess any files with the ".spp" suffix
        List generatedFiles = new ArrayList();
        FileSet sppFiles = new FileSet(VM_SRC_DIR, new FileSet.SuffixSelector(".spp"));
        for (Iterator iterator = sppFiles.list().iterator(); iterator.hasNext(); ) {
            File sppFile = (File)iterator.next();
            SppFilePreprocessCommand.preprocess(sppFile, generatedFiles, env.getPreprocessor(), env.getMacroizer(), ccompiler.options.macroize);
        }

        // Run the C compiler to compile the slow VM
        if (compilationEnabled) {

            File[] includeDirs = new File[] {
                new File(jdk.getHome(), "include"),
                jdk.getJNI_MDIncludePath(),
                FP_SRC_DIR,
                new File(VM_SRC_RTS_DIR, ccompiler.getName())
            };

            Build.mkdir(VM_BLD_DIR);

            List objectFiles = new ArrayList();
            if (ccompiler.options.floatsSupported) {
                /*
                 * The floating point code must be compiled without optimization for the reasons stated in this
                 * excerpt from the KVM porting guide (Squawk's FP implementation is derived from the KVM):
                 *
                 * 10.1.2 Implementing Java virtual machine floating point semantics
                 *
                 *   Many processor architectures natively support IEEE 754 arithmetic on float and
                 *   double formats. Therefore, there is often a straightforward mapping between Java
                 *   virtual machine floating-point operations, C code implementing those operations,
                 *   and floating-point instructions on the underlying processor. However, various
                 *   complications are possible:
                 *
                 *   o The floating-point semantics of the Java virtual machine are tightly specified,
                 *     much more tightly specified than C language floating-point semantics. Therefore,
                 *     a C compiler could perform an  optimization  that was an allowed transformation
                 *     in C but broke Java virtual machine semantics. For example, in the Java virtual
                 *     machine and Java:
                 *
                 *        x + 0.0
                 *
                 *     cannot be replaced with
                 *
                 *        x
                 *
                 *     since different answers can be generated.
                 *
                 *     Therefore, the portions of the KVM that implement Java virtual machine floating-point
                 *     semantics should be compiled without aggressive optimization to help avoid such (in
                 *     this case) unhelpful code transformations. Many C compilers also have separate flags
                 *     affecting floating-point code generation, such as flags to improve floating-point
                 *     consistency and make the generated code have semantics more closely resembling a
                 *     literal translation of the source. Regardless of the processor architecture, using
                 *     such flags might be necessary to implement Java virtual machine semantics in C code.
                 */

                env.log(env.brief, "[compiling floating point sources in " + FP_SRC_DIR + " ...]");
                List sources = new FileSet(FP_SRC_DIR, new FileSet.SuffixSelector(".c")).list();
                for (Iterator iter = sources.iterator(); iter.hasNext(); ) {
                    File source = (File)iter.next();
                    objectFiles.add(ccompiler.compile(includeDirs, source, VM_BLD_DIR, true));
                }
            }
            
            
          
            if (ccompiler.options.nativeVerification) {    	   
            	env.log(env.brief, "[compiling native verification sources in " + UTIL_SRC_DIR + " ...]");
                List sources = new FileSet(UTIL_SRC_DIR, new FileSet.SuffixSelector(".c")).list();
                for (Iterator iter = sources.iterator(); iter.hasNext(); ) {
                    File source = (File)iter.next();
                    objectFiles.add(ccompiler.compile(includeDirs, source, VM_BLD_DIR, false));
                }
            }
        	
            

            env.log(env.brief, "[compiling '" + VM_SRC_FILE + "' ...]");
            objectFiles.add(ccompiler.compile(includeDirs, VM_SRC_FILE, VM_BLD_DIR, false));

            env.log(env.brief, "[linking '" + bootstrapSuiteName + "' ...]");
            ccompiler.link((File[])objectFiles.toArray(new File[objectFiles.size()]), bootstrapSuiteName, env.dll);

            if (!env.verbose) {
                for (Iterator iterator = generatedFiles.iterator(); iterator.hasNext(); ) {
                    Build.delete((File)iterator.next());
                    Build.clear(VM_BLD_DIR, true);
                }
            }
        }

        // Rebuild the jar of files used by the JVM embedded in Squawk if the bootstrap suite was (re)built
        if (args.length > 0) {
            env.runCommand("squawk.jar", Build.NO_ARGS);
        }
    }
    
    public String[] runRomizer(String[] args) {
        bootstrapSuiteName = "squawk";

        // Only run the romizer if there are arguments passed to this command
        if (args.length > 0) {
            CCompiler ccompiler = env.getCCompiler();
            String arch = ccompiler == null ? "X86" : ccompiler.getArchitecture();
            args = convertToRomizerOptions(args, arch);
            env.runCommand("romize", args);
        }
        return args;
    }

    /**
     * Sets the flag enabling the C compilation step.
     *
     * @param  flag    the valur to which the flag should be set (true enables compilation, false disables)
     * @return boolean the previous value of the flag
     */
    public boolean enableCompilation(boolean flag) {
        boolean oldFlag = compilationEnabled;
        compilationEnabled = flag;
        return oldFlag;
    }

    /**
     * {@inheritDoc}
     */
    public void clean() {
        Build.clear(VM_BLD_DIR, true);
        Build.delete(new File(VM_SRC_DIR, "rom.h"));
        Build.delete(new File(VM_SRC_DIR, "buildflags.h"));
        FileSet generatedFiles = new FileSet(VM_SRC_DIR, new FileSet.SuffixSelector(".spp"));
        for (Iterator iterator = generatedFiles.list().iterator(); iterator.hasNext(); ) {
            File sppFile = (File)iterator.next();
            File outputFile = getFileDerivedFromSppFile(sppFile);
            File preprocessedFile = new File(sppFile.getPath() + ".preprocessed");
            Build.delete(outputFile);
            Build.delete(preprocessedFile);
        }
        Build.delete(VM2C_SRC_FILE);
    }
}
