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

package com.sun.squawk.io.j2se.socket;

import java.io.*;
import java.net.*;
import javax.microedition.io.*;
import com.sun.squawk.io.j2se.*;
import com.sun.squawk.io.*;

/**
 * GenericStreamConnection to the J2SE socket API.
 *
 * @author  Nik Shaylor
 * @version 1.0 10/08/99
 */

public class Protocol extends ConnectionBase implements StreamConnection {

    /** Socket object */
    Socket socket;

    /** Open count */
    int opens = 0;

    /** Extra parameters for a socket. */
    Parameters parameters = new Parameters();

    public static class Parameters extends ParameterParser {

        /** URL specifying connection to which socket input will be logged. */
        public String inLog;

        /** URL specifying connection to which socket output will be logged. */
        public String outLog;

        /**
         * The amount of time (in milliseconds) a socket blocks for in a
         * read before throwing an InterruptedIOException.
         */
        public int timeout;

        /**
         * The amount of time (in milliseconds) a server socket blocks for in a
         * call to acceptAndOpen() before throwing an InterruptedIOException.
         */
        public int acceptTimeout;

        /**
         * {@inheritDoc}
         */
        public boolean parameter(String key, String value) throws IllegalArgumentException {
            if (key.equals("inputLog")) {
                inLog = value;
            } else if (key.equals("outputLog")) {
                outLog = value;
            } else if (key.equals("acceptTimeout")) {
                try {
                    acceptTimeout = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("acceptTimeout value is not a well-formed integer: " + value);
                }
            } else if (key.equals("timeout")) {
                try {
                    timeout = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("timeout value is not a well-formed integer: " + value);
                }
            } else {
                return false;
            }
            return true;
        }
    }

    /**
     * Open the connection
     */
    public void open(String name, int mode, boolean timeouts) throws IOException {
        throw new RuntimeException("Should not be called");
    }

    /**
     * Open the connection
     * @param name the target for the connection
     * @param timeouts A flag to indicate that the called wants timeout exceptions
     * <p>
     * The name string for this protocol should be:
     * "<name or IP number>:<port number>
     */
    public Connection open(String protocol, String name, int mode, boolean timeouts) throws IOException {

        if(name.charAt(0) != '/' || name.charAt(1) != '/') {
            throw new IllegalArgumentException("Protocol must start with \"//\" "+name);
        }

        name = name.substring(2);

        try {
            /* Host name or IP number */
            String nameOrIP;

            /* Port number */
            int port;

            /* Look for the : */
            int colon = name.indexOf(':');

            if(colon == -1) {
                throw new IllegalArgumentException("Bad protocol specification in "+name);
            }

            /* Strip off the protocol name */
            nameOrIP = name.substring(0, colon);

            if(nameOrIP.length() == 0) {
                /*
                 * If the open string is "socket://:nnnn" then we regard this as
                 * "serversocket://:nnnn"
                 */
                com.sun.squawk.io.j2se.serversocket.Protocol con =
                    new com.sun.squawk.io.j2se.serversocket.Protocol();
                con.open("socket://", name, mode, timeouts);
                return con;
            }


            /* Get the port number */
            port = Integer.parseInt(name.substring(colon+1));

            /* Get the other parameters (if any) */
            parameters.parse(name.substring(2));

            /* Open the socket */
            socket = new Socket(nameOrIP, port);

            /* Set timeout value for sockets reads. */
            socket.setSoTimeout(parameters.timeout);

            opens++;
            return this;
        } catch(NumberFormatException x) {
            throw new IllegalArgumentException("Invalid port number in "+name);
        }
    }

    /**
     * Open the connection
     * @param socket an already formed socket
     * <p>
     * This function is only used by com.sun.kjava.system.palm.protocol.socketserver;
     */
    public void open(Socket socket, Parameters parameters) throws IOException {
        this.socket = socket;
        this.parameters = parameters;
    }

    /**
     * Returns an input stream for this socket.
     *
     * @return     an input stream for reading bytes from this socket.
     * @exception  IOException  if an I/O error occurs when creating the
     *                          input stream.
     */
    public InputStream openInputStream() throws IOException {
        InputStream is;
        if (parameters.inLog != null) {
            is = new UniversalFilterInputStream(this, socket.getInputStream(), parameters.inLog);
        } else {
            is = new UniversalFilterInputStream(this, socket.getInputStream());
        }
        opens++;
        return is;
    }

    /**
     * Returns an output stream for this socket.
     *
     * @return     an output stream for writing bytes to this socket.
     * @exception  IOException  if an I/O error occurs when creating the
     *                          output stream.
     */
    public OutputStream openOutputStream() throws IOException {
        OutputStream os;
        if (parameters.outLog != null) {
            os = new UniversalFilterOutputStream(this, socket.getOutputStream(), parameters.outLog);
        } else {
            os = new UniversalFilterOutputStream(this, socket.getOutputStream());
        }
        opens++;
        return os;
    }

    /**
     * Close the connection.
     *
     * @exception  IOException  if an I/O error occurs when closing the
     *                          connection.
     */
    public void close() throws IOException {
        if(--opens == 0) {
            socket.close();
        }
    }

















/*if[EXCLUDE]*/

// Test code to see if the GenericConnectionChannel DataSucker is working

    public static void main(String[] args) throws IOException {
        String hostAndPort = args[0];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        for (int i = 1; i < args.length; ++i) {
            int value = Integer.parseInt(args[i]);
            System.out.println("writing "+value);
            dos.writeInt(value);
        }
        dos.close();

        byte[] data = baos.toByteArray();

        OutputStream os = Connector.openOutputStream("socket://" + hostAndPort);
        for (int i = 0; i < data.length; ++i) {
            int value = data[i] & 0xFF;
            System.out.println("writing2 "+value);
            os.write(value);
            os.flush();
            try { Thread.sleep(10); } catch (Exception ex) {}
        }
        os.close();
    }

/*end[EXCLUDE]*/

}
