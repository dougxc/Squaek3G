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
package com.sun.squawk.io;

import com.sun.squawk.io.j2me.msg.*;
import java.io.*;

import javax.microedition.io.*;

/**
 * This interface defines the call back interface for kernel mode Java device drivers.
 *
 * @author  Nik Shaylor
 */
public abstract class ServerConnectionHandler {

    /**
     * The next ServerConnectionHandler in the queue.
     */
    private ServerConnectionHandler next;

    /**
     * The name of the message stream being processed.
     */
    private final String name;

    /**
     * Creates a new ServerConnectionHandler.
     *
     * @param name the name of the message stream being processed
     */
    public ServerConnectionHandler(String name) {
        this.name = name;
    }

    /**
     * Sets the next field of the ServerConnectionHandler
     *
     * @param next the next ServerConnectionHandler
     */
    public void setNext(ServerConnectionHandler next) {
        this.next = next;
    }

    /**
     * Returns a name of the connection.
     *
     * @return the name of the message stream being processed
     */
    public String getConnectionName() {
        return name;
    }

    /**
     * Causes the pending server message to be processed.
     */
    public void processServerMessage() throws IOException {
        Message msg = Database.receiveFromClient(name);
        ServerProtocol con = MessageResourceManager.allocateServerProtocol(name, msg);
        processConnection(con);
    }

    /**
     * Searches a list of ServerConnectionHandlers for a handler whose {@link #getConnectionName name} matches a given string.
     *
     * @param sch    the handler to start searching from
     * @param name   the string to match
     * @return the ServerConnectionHandler whose name matches <code>name</code> or null if there isn't one
     */
    public static ServerConnectionHandler lookup(ServerConnectionHandler sch, String name) {
        while (sch != null) {
            if (sch.name.equals(name)) {
                break;
            }
            sch = sch.next;
        }
        return sch;
    }

    /**
     * Processes an incoming connection.
     *
     * @param con the incoming connection
     */
    public abstract void processConnection(StreamConnection con);
}


