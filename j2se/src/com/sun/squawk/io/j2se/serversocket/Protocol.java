/*
 *  Copyright (c) 1999 Sun Microsystems, Inc., 901 San Antonio Road,
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

package com.sun.squawk.io.j2se.serversocket;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.microedition.io.*;

import com.sun.squawk.io.*;
import com.sun.squawk.io.j2se.socket.Protocol.Parameters;

/**
 * StreamConnectionNotifier to the Palm Server Socket API.
 *
 * @author  Nik Shaylor
 * @version 1.0 10/08/99
 */
public class Protocol extends ConnectionBase implements StreamConnectionNotifier {

    /** Server Socket object */
    ServerSocket ssocket;

    /** Extra parameters for a socket. */
    Parameters parameters = new Parameters();

    /**
     * Open the connection
     * @param name the target for the connection
     * @param timeouts A flag to indicate that the called wants timeout exceptions
     * <p>
     * The name string for this protocol should be:
     * "<port number>
     */
    public Connection open(String protocol, String name, int mode, boolean timeouts) throws IOException {

        if(name.charAt(0) != '/' || name.charAt(1) != '/' || name.charAt(2) != ':') {
            throw new IllegalArgumentException("Protocol must start with \"//:\" "+name);
        }

        name = parameters.parse(name.substring(3));
        try {
            int port;

            /* Get the port number */
            port = Integer.parseInt(name);

            /* Open the socket */
            ssocket = new ServerSocket(port);

            /* Set timeout value for accept and subsequent sockets reads. */
            ssocket.setSoTimeout(parameters.acceptTimeout);

        } catch(NumberFormatException x) {
            throw new IllegalArgumentException("Invalid port number in "+name);
        }
        return this;
    }

    /**
     * Returns a GenericConnection that represents a server side
     * socket connection
     * @return     a socket to communicate with a client.
     * @exception  IOException  if an I/O error occurs when creating the
     *                          input stream.
     */
    public StreamConnection acceptAndOpen() throws IOException {
        try {
            Socket soc = ssocket.accept();
            com.sun.squawk.io.j2se.socket.Protocol con =
                new com.sun.squawk.io.j2se.socket.Protocol();
            con.open(soc, parameters);
            return con;
        } catch (SocketTimeoutException e) {
            throw new InterruptedIOException();
        }
    }

    /**
     * Returns an input stream for this socket.
     *
     * @return     an input stream for reading bytes from this socket.
     * @exception  IOException  if an I/O error occurs when creating the
     *                          input stream.
     */
    public InputStream openInputStream() throws IOException {
        throw new RuntimeException();
    }

    /**
     * Returns an output stream for this socket.
     *
     * @return     an output stream for writing bytes to this socket.
     * @exception  IOException  if an I/O error occurs when creating the
     *                          output stream.
     */
    public OutputStream openOutputStream() throws IOException {
        throw new RuntimeException();
    }

    /**
     * Close the connection.
     *
     * @exception  IOException  if an I/O error occurs when closing the
     *                          connection.
     */
    public void close() throws IOException {
        ssocket.close();
    }

}
