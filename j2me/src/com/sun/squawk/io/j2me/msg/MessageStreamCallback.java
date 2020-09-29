/*
 * Copyright 1994-2001 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */
package com.sun.squawk.io.j2me.msg;

import java.io.*;

/**
 * A <code>ByteArrayInputStream</code> contains
 * an internal buffer that contains bytes that
 * may be read from the stream. An internal
 * counter keeps track of the next byte to
 * be supplied by the <code>read</code> method.
 *
 * @author Nik Shaylor
 */
interface MessageStreamCallback {

    /**
     * Signals that the input was opened.
     *
     * @param in the input stream
     */
    public abstract void inputUsed(MessageInputStream in) throws IOException;

    /**
     * Signals that the input was closed.
     *
     * @param in the input stream
     */
    public abstract void inputClosed(MessageInputStream in) throws IOException;

    /**
     * Signals that the output was flushed.
     *
     * @param out the output stream
     */
    public abstract void outputFlushed(MessageOutputStream out) throws IOException;

    /**
     * Signals that the input was closed.
     *
     * @param out the output stream
     */
    public abstract void outputClosed(MessageOutputStream out) throws IOException;

}


