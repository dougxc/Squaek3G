/*
 *  Copyright (c) 1999-2001 Sun Microsystems, Inc., 901 San Antonio Road,
 *  Palo Alto, CA 94303, U.S.A.  All Rights Reserved.
 *
 *  Sun Microsystems, Inc. has intellectual property rights relating
 *  to the technology embodied in this software.  In particular, and
 *  without limitation, these intellectual property rights may include
 *  one or more U.S. patents, foreign patents, or pending
 *  applications.  Sun, Sun Microsystems, the Sun logo, Java, KJava,
 *  and all Sun-based and Java-based marks are trademarks or
 *  registered trademarks of Sun Microsystems, Inc.  in the United
 *  States and other countries.
 *
 *  This software is distributed under licenses restricting its use,
 *  copying, distribution, and decompilation.  No part of this
 *  software may be reproduced in any form by any means without prior
 *  written authorization of Sun and its licensors, if any.
 *
 *  FEDERAL ACQUISITIONS:  Commercial Software -- Government Users
 *  Subject to Standard License Terms and Conditions
 */

package com.sun.squawk.io.j2me.msg;

import java.io.*;
import javax.microedition.io.*;
import com.sun.squawk.io.*;
import com.sun.squawk.util.Assert;

/**
 * Message Server Connection
 */
public class ServerProtocol extends ConnectionBase implements StreamConnection, MessageStreamCallback {

    /**
     * The number of things opened.
     */
    private int opens;

    /**
     * The namespace target for the message.
     */
    private String name;

    /**
     * The input data.
     */
    private Message message;

    /**
     * The output stream.
     */
    private MessageOutputStream out;

    /**
     * The input stream.
     */
    private MessageInputStream in;

    /**
     * Flag to show the message was sent to the client.
     */
    private boolean messageSent;

    /**
     * Resets the instance state.
     *
     * @param name     the namespace target for the message
     * @param message  the input data
     */
    void resetInstanceState(String name, Message message) {
        in = null;
        out = null;
        this.name = name;
        this.message = message;
        opens = 1;
    }

    /**
     * Server open.
     */
    public Connection open(String protocol, String name, int mode, boolean timeouts) throws IOException {
        throw Assert.shouldNotReachHere();
    }

    /**
     * openInputStream
     */
    public synchronized InputStream openInputStream() throws IOException {
        Assert.always(in == null);
        opens++;
        in = MessageResourceManager.allocateInputStream(this);
        in.setMessage(message);
        return in;
    }

    /**
     * openOutputStream
     */
    public synchronized OutputStream openOutputStream() throws IOException {
        Assert.always(out == null);
        opens++;
        out = MessageResourceManager.allocateOutputStream(this);
        return out;
    }

    /**
     * Closes this connection.
     */
    public synchronized void close() throws IOException {
        if (--opens == 0) {
            if (out == null) {
                openOutputStream();
                out.close();
            }
            if (!messageSent) {
                throw new IOException("Output stream was not closed");
            }
            MessageResourceManager.freeServerProtocol(this);
        }
    }

    /**
     * Signals that the input was opened.
     *
     * @param in the input stream
     */
    public void inputUsed(MessageInputStream in) throws IOException {
    }

    /**
     * Signals that the input was closed.
     *
     * @param in the input stream
     */
    public void inputClosed(MessageInputStream in) throws IOException {
        close();
    }

    /**
     * Flushes output to client.
     *
     * @param out the output stream
     */
    public void outputFlushed(MessageOutputStream out) throws IOException {
        Database.sendToClient(name, out.getMessage(), out.getMessageStatus());
	out.resetMessageState();
    }

    /**
     * Signals that the input was closed.
     *
     * @param out the output stream
     */
    public void outputClosed(MessageOutputStream out) throws IOException {
        Database.sendToClient(name, out.getMessage(), out.getMessageStatus());
        messageSent = true;
        close();
    }
}
