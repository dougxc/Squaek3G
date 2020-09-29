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
package com.sun.squawk.builder;

import java.io.*;

/**
 * A StreamGobbler is a thread that pipes data from a given input stream to
 * a given output stream. This is useful for reading data from a {@link Process}.
 *
 * @author  Nik Shaylor
 */
public class StreamGobbler extends Thread {
    final InputStream is;
    final OutputStream out;

    StreamGobbler(InputStream is, OutputStream out) {
        this.is = is;
        this.out = out;
    }

    public void run() {
        try {
            BufferedInputStream bis = new BufferedInputStream(is);
            int bite;
            while ((bite = bis.read()) != -1) {
                out.write(bite);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}

