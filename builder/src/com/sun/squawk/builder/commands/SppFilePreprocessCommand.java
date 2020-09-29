/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: RomCommand.java,v 1.16 2005/06/02 20:46:11 dsimon Exp $
 */
package com.sun.squawk.builder.commands;

import java.io.*;
import java.util.*;
import com.sun.squawk.builder.*;
import com.sun.squawk.builder.ccompiler.*;

/**
 * This is the command that preprocesses the *.spp files in a given directory.
 *
 * @author Doug Simon
 */
public class SppFilePreprocessCommand extends Command {

    public SppFilePreprocessCommand(Build env) {
        super(env, "spp");
    }

    /**
     * {@inheritDoc}
     */
    public String getDescription() {
        return "preprocesses one or more *.spp files";
    }

    private void usage(String errMsg) {
        PrintStream out = System.out;

        out.println();
        out.println("usage: spp files...");
        out.println();
    }

    /**
     *
     * @param sppFile File
     * @return File
     * @throws IllegalArgumentException if <code>sppFile</code> does not end with ".spp"
     */
    private static File getFileDerivedFromSppFile(File sppFile) {
        String path = sppFile.getPath();
        if (!path.endsWith(".spp")) {
            throw new IllegalArgumentException("file does not end with \".spp\": " + path);
        }
        path = path.substring(0, path.length() - ".spp".length());
        return new File(path);
    }

    /**
     * Preprocesses a Squawk preprocessor input file (i.e. a file with a ".spp" suffix). The file is processed with
     * the {@link #preprocessSource() Java source preprocessor} and then with the {@link Macro} preprocessor.
     * The file generated after the preprocessing is the input file with the ".spp" suffix removed.
     *
     * @param sppFile         the input file that must end with ".spp"
     * @param generatedFiles  a list to which any generated files will be added
     * @param preprocessor    the Squawk preprocessor
     * @param macroizer       the Squawk macroizer
     * @param macroize        true if the macroizer should "do its stuff" or just convert the leading '$' in
     *                        any identifiers to '_' (which stops a C compiler complaining about invalid identifiers)
     * @throws IllegalArgumentException if <code>sppFile</code> does not end with ".spp"
     */
    public static void preprocess(File sppFile, List generatedFiles, Preprocessor preprocessor, Macroizer macroizer, boolean macroize) {
        File outputFile = getFileDerivedFromSppFile(sppFile);
        File preprocessedFile = new File(sppFile.getPath() + ".preprocessed");

        // Remove generated files
        Build.delete(outputFile);
        Build.delete(preprocessedFile);

        // Run the Java source preprocessor
        boolean save = preprocessor.disableWithComments;
        preprocessor.disableWithComments = false;
        preprocessor.execute(sppFile, preprocessedFile);
        preprocessor.disableWithComments = save;

        // Run the Macro conversion tool
        macroizer.execute(preprocessedFile, outputFile, macroize);

        generatedFiles.add(preprocessedFile);
        generatedFiles.add(outputFile);

        // Make the generated file read-only
        if (!outputFile.setReadOnly()) {
            throw new BuildException("could not make generated file read-only: " + outputFile);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void run(String[] args) {
        if (args.length == 0) {
            usage(null);
            return;
        }

        Preprocessor preprocessor = env.getPreprocessor();
        Macroizer macroizer = env.getMacroizer();
        CCompiler ccompiler = env.getCCompiler();

        List generatedFiles = new ArrayList();
        for (int i = 0; i != args.length; ++i) {
            File file = new File(args[i]);
            preprocess(file, generatedFiles, preprocessor, macroizer, ccompiler.options.macroize);
        }

        env.log(env.verbose, "Generated the following files:");
        for (Iterator iterator = generatedFiles.iterator(); iterator.hasNext(); ) {
            env.log(env.verbose, "    " + iterator.next());
        }

    }
}
