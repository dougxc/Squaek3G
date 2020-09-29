/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk.traces;

import java.io.*;

/**
 * An InputFile instance is used to read a file, line by line. Any error that occurs while
 * opening or reading the file results in a RuntimException.
 */
public class InputFile {

    /**
     * The current line number.
     */
    private int lineNo;

    /**
     * The underlying reader.
     */
    private final BufferedReader reader;

    /**
     * The file being read.
     */
    private final File file;

    /**
     * Opens a file for reading.
     *
     * @param file   the file
     */
    public InputFile(File file) {
        this.lineNo = 1;
        this.file = file;
        try {
            reader = new BufferedReader(new FileReader(file), 1000000);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Error opening input file: " + file, e);
        }
    }

    /**
     * Reads a line from the input.
     *
     * @return  the line read or null if EOF is reached
     */
    public String readLine() {
        try {
            String line = reader.readLine();
            if (line != null) {
                ++lineNo;
                if ((lineNo % 10000) == 0) {
                    System.err.println("read " + lineNo + " lines...");
                }
            }
            return line;
        } catch (IOException ex) {
            throw error(ex.toString(), ex);
        }
    }

    /**
     * Gets the line number of the current read position.
     *
     * @return the line number of the current read position
     */
    public int getLineNumber() {
        return lineNo;
    }

    /**
     * Formats an error message by prepending the name of this file and the previous line number.
     *
     * @param msg  the error message
     * @return the contextual error message
     */
    public String formatErrorMessage(String msg) {
        return "Error at line " + lineNo + " in " + file + ": " + msg;
    }

    /**
     * Creates a runtime exception detailing an error while reading from this file.
     *
     * @param msg  description of the error
     * @return  a runtime exception detailing the error
     */
    public RuntimeException error(String msg, Throwable cause) {
        msg = formatErrorMessage(msg);
        if (cause != null) {
            return new RuntimeException(msg, cause);
        } else {
            return new RuntimeException(msg);
        }
    }
}
