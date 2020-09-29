/*
 * Copyright 1994-2001 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */
package com.sun.squawk.io.j2me.msg;

import java.io.*;
import com.sun.squawk.vm.ChannelConstants;

/**
 * This class implements an output stream in which the data is
 * written into a Message.
 *
 * @author Nik Shaylor
 */

public class MessageOutputStream extends OutputStream {

    /**
     * The message where data is stored.
     */
    private Message message;

    /**
     * The status for the current message.
     */
    private int status;

    /**
     * Flag indicating whether the stream has been closed.
     */
    private boolean isClosed;

    /**
     * Parent object to call back to when various events occur.
     */
    private MessageStreamCallback parent;

    /**
     * Allocates a new message structure.
     *
     */
    void resetMessageState() {
        message = MessageResourceManager.allocateMessage();
    }

    /**
     * Resets the instance state.
     *
     * @param parent the parent object
     */
    void resetInstanceState(MessageStreamCallback parent) {
        message = MessageResourceManager.allocateMessage();
        status = ChannelConstants.RESULT_OK;
        isClosed = false;
        this.parent = parent;
    }

    /**
     * Check to make sure that the stream has not been closed
     */
    private void ensureOpen() {
        if (isClosed) {
            throw new RuntimeException("Writing to closed MessageOutputStream");
        }
    }

    /**
     * Creates a new byte array output stream. The buffer capacity is
     * initially 32 bytes, though its size increases if necessary.
     */
    MessageOutputStream() {
    }

    /**
     * Sets out-of-band status for output stream. Useful, e.g., in
     * using MessageOutputStream to communicate that a bad set of
     * parameters were sent to a server connection.
     *
     * @param    status    an appropriate result value (ChannelConstants)
     */
    public void setStatus(int status) {
        this.status = status;
    }

    /**
     * Returns out-of-band status for output stream.
     *
     */
    public int getMessageStatus() {
        return status;
    }

    /**
     * Writes the specified byte to this byte array output stream.
     *
     * @param   b   the byte to be written.
     */
    public void write(int b) {
        ensureOpen();
        message.write(b);
    }

    /**
     * Flushes pending message to output stream.
     *
     */
    public void flush() throws IOException {
        ensureOpen();
        if (message.available() > 0)
            parent.outputFlushed(this);
    }

    /**
     * Writes <code>len</code> bytes from the specified byte array
     * starting at offset <code>off</code> to this byte array output stream.
     *
     * @param   b     the data.
     * @param   off   the start offset in the data.
     * @param   len   the number of bytes to write.
     */
    public void write(byte b[], int off, int len) {
        ensureOpen();
        message.write(b, off, len);
    }

    /**
     * Returns the message.
     *
     * @return  the message
     */
    public Message getMessage() {
        Message res = message;
        message = null;
        return res;
    }

    /**
     * Closes this input stream and releases any system resources
     * associated with the stream.
     * <p>
     */
    public void close() throws IOException {
        if (!isClosed) {
            isClosed = true;
            parent.outputClosed(this);
            MessageResourceManager.freeOutputStream(this);
        }
    }
}

