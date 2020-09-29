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
 * A <code>MessageInputStream</code> contains
 * an internal buffer that contains bytes that
 * may be read from the stream. An internal
 * counter keeps track of the next byte to
 * be supplied by the <code>read</code> method.
 *
 * @author Nik Shaylor
 */
class MessageInputStream extends InputStream {

    /**
     * The input message to be read.
     */
    private Message message;

    /**
     * Parent object to call back to when various events occur.
     */
    private MessageStreamCallback parent;

    /**
     * Flag to show the stream has been used.
     */
    private boolean used;

    /**
     * Flag to show the stream has been disabled.
     */
    private boolean disabled = false;

    /**
     * Resets the instance state (except parent).
     *
     */
    void resetInstanceState() {
        message = null;
        used = false;
        disabled = false;
    }

    /**
     * Resets the instance state.
     *
     * @param parent the parent object.
     */
    void resetInstanceState(MessageStreamCallback parent) {
        this.parent = parent;
        resetInstanceState();
    }

    /**
     * Constructor.
     */
    MessageInputStream() {
    }

    /**
     * Set up the input message.
     *
     * @param message the data
     */
    void setMessage(Message message) {
        this.message = message;
    }

    /**
     * Disable the stream.
     */
    void disable() {
        disabled = true;
    }

    /**
     * Disable the stream.
     */
    private void checkEnabled() throws IOException {
        if (disabled) {
            throw new IOException("Accessing disabled stream");
        }
        if (!used) {
            // Loads in current message
            used = true;
            parent.inputUsed(this);
        }
    }

    /**
     * {@inheritDoc}
     */
    public int read() throws IOException {
        checkEnabled();
        return message.read();
    }

    /**
     * {@inheritDoc}
     */
    public int read(byte b[], int off, int len) throws IOException {
        checkEnabled();
        return message.read(b, off, len);
    }

    /**
     * {@inheritDoc}
     */
    public int available() throws IOException {
        checkEnabled();
        if (message == null) {
            return 0;
        }
        return message.available();

    }

    /**
     * {@inheritDoc}
     */
    public void close() throws IOException {
        if (message != null) {
            parent.inputClosed(this);
            message.freeAll();
            MessageResourceManager.freeMessage(message);
            MessageResourceManager.freeInputStream(this);
            disable();
        }
    }

    /**
     * Resets this input stream so that subsequent reads will be
     * against a different message packet.
     * <p>
     */
    public void reset() throws IOException {
        if (message != null) {
            message.freeAll();
            MessageResourceManager.freeMessage(message);
            resetInstanceState();
        }
    }

}

