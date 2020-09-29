/*
 * Copyright 2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk.vm2c;

import java.io.*;
import java.util.*;
import javax.tools.*;

/**
 * Maps character positions to line numbers within a Java source file.
 *
 * @author  Doug Simon
 */
public class LineNumberTable {

    final JavaFileObject file;
    private final int maxPos;

    /**
     * Entry at index 'n' is position of first char in source line 'n+1'.
     */
    private final int[] table;

    public LineNumberTable(JavaFileObject file) throws IOException {
        this.file = file;

        char[] sourceBuf = file.getCharContent(true).toString().toCharArray();
        maxPos = sourceBuf.length - 1;
        ArrayList<Integer> newLinePosns = new ArrayList<Integer>();

        int pos = 0;
        newLinePosns.add(new Integer(pos));
        while (pos < sourceBuf.length) {
            if (sourceBuf[pos++] == '\n') {
                if (pos < sourceBuf.length) {
                    newLinePosns.add(new Integer(pos));
                }
            }
        }

        table = new int[newLinePosns.size()];
        int line = 0;
        for (Integer posn: newLinePosns) {
            table[line++] = posn.intValue();
        }
    }

    public int getLineNumber(int pos) {
        if (maxPos < pos || pos < 0) {
            return 0;
        }

        // Skip first line - always starts at pos 0
        for (int line = 1; ; ++line) {
            if (table[line] > pos) {
                return line;
            }
        }
    }
}
