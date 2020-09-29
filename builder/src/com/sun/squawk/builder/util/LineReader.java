/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: $
 */
package com.sun.squawk.builder.util;

import java.io.*;

/**
 * An LineReader instance is used to read some character input, line by line.
 */
public class LineReader {

    /**
     * The line number of the last successful call to {@link #readLine}.
     */
    private int lastLineNo;

    /**
     * The underlying buffered reader.
     */
    private final BufferedReader reader;

    /**
     * The description of the source from which the BufferedReader was constructed.
     */
    private final String source;

    /**
     * Creates a LineReader to read from a given Reader, line by line.
     *
     * @param reader   the reader to read from
     * @param source   a description of <code>reader</code>'s source
     * @param size     the size to use when creating the internal BufferedReader (-1 for default)
     */
    public LineReader(Reader reader, String source, int size) {
        this.source = source;
        this.reader = size == -1 ? new BufferedReader(reader) : new BufferedReader(reader, size);
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
                ++lastLineNo;
                if ((lastLineNo % 10000) == 0) {
                    System.err.println("read " + lastLineNo + " lines...");
                }
            }
            return line;
        } catch (IOException ex) {
            throw error(ex.toString(), ex);
        }
    }

    /**
     * Gets a description of this reader's source (e.g. a file path).
     *
     * @return String a description of this reader's source
     */
    public String getSource() {
        return source;
    }

    /**
     * Gets the line number of the last line {@link #read}.
     *
     * @return the last line number
     */
    public int getLastLineNumber() {
        return lastLineNo;
    }

    /**
     * Returns a textual description of this line reader's input source and last read line number.
     *
     * @return the context message
     */
    public String getContext() {
        return "at line " + lastLineNo + " in " + source;
    }

    /**
     * Creates a runtime exception detailing an error while reading from this file.
     *
     * @param msg  description of the error
     * @return  a runtime exception detailing the error
     */
    public RuntimeException error(String msg, Throwable cause) {
        msg = "Error " + getContext() + ": " + msg;
        if (cause != null) {
            return new RuntimeException(msg, cause);
        } else {
            return new RuntimeException(msg);
        }
    }

}
