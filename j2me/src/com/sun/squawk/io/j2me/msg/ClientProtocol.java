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
 * Message Client Connection
 */
public class ClientProtocol extends ConnectionBase implements StreamConnection, MessageStreamCallback {

    /**
     * The number of things opened.
     */
    private int opens;

    /**
     * The namespace target for the message.
     */
    private String name;

    /**
     * The output stream.
     */
    private MessageOutputStream out;

    /**
     * The input stream.
     */
    private MessageInputStream in;

    /**
     * Flag to show the message was sent yo the server.
     */
    private boolean messageSent;

/*if[REUSEABLE_MESSAGES]*/
    /**
     * Resets the instance state.
     */
    void resetInstanceState() {
        in = null;
        out = null;
        name = null;
        opens = 0;
    }
/*end[REUSEABLE_MESSAGES]*/

    /**
     * Client open.
     */
    public synchronized Connection open(String protocol, String name, int mode, boolean timeouts) throws IOException {
        if (protocol == null || name == null) {
            throw new NullPointerException();
        }
        this.name = name;
        opens++;
        return this;
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
     * openInputStream
     */
    public synchronized InputStream openInputStream() throws IOException {
        Assert.always(in == null);
        opens++;
        in = MessageResourceManager.allocateInputStream(this);
        return in;
    }

    /**
     * Closes this connection.
     */
    public synchronized void close() throws IOException {
        if (--opens == 0) {
            sendMessage();
            MessageResourceManager.freeClientProtocol(this);
        }
    }

    /**
     * Sends the message to the server.
     */
    private void sendMessage() throws IOException {
        if (out == null) {
            openOutputStream();
            out.close();
        }
        if (!messageSent) {
            throw new IOException("Output stream was not closed");
        }
    }

    /**
     * Reads the reply message from the server.
     *
     * @return the reply message
     */
    private Message receiveMessage() throws IOException {
        return Database.receiveFromServer(name);
    }

    /**
     * Flushes output to client.
     *
     * @param out the output stream
     */
    public void outputFlushed(MessageOutputStream out) throws IOException {
        /* clients only send a single packet */
    }

    /**
     * Signals that the input was closed.
     *
     * @param out the output stream
     */
    public void outputClosed(MessageOutputStream out) throws IOException {
        Database.sendToServer(name, out.getMessage(), out.getMessageStatus());
        messageSent = true;
        close();
    }

    /**
     * Signals that the input was opened.
     *
     * @param in the input stream
     */
    public void inputUsed(MessageInputStream in) throws IOException {
        sendMessage();
        in.setMessage(receiveMessage());
    }

    /**
     * Signals that the input was closed.
     *
     * @param in the input stream
     */
    public void inputClosed(MessageInputStream in) throws IOException {
        close();
    }
}
