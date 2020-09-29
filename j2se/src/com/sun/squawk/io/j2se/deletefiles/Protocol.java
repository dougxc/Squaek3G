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
package com.sun.squawk.io.j2se.deletefiles;

import java.io.*;
import javax.microedition.io.*;
import com.sun.squawk.io.j2se.UniversalFilterOutputStream;
import com.sun.squawk.io.ConnectionBase;

/**
 * GenericStreamConnection to delete files using the J2SE File API.
 *
 * @author  Andrew Crouch
 * @version 1.0 04/04/2005
 */
public class Protocol extends ConnectionBase implements StreamConnection {

    /**
     * Opens the connection
     */
    public void open(String name, int mode, boolean timeouts) throws IOException {
        throw new RuntimeException("Should not be called");
    }

    /**
     * Opens a connection to delete one or more files.  Files are deleted when the
     * connection is closed.
     *
     * @param name the target for the connection (including any parameters).
     * @param timeouts A flag to indicate that the called wants timeout exceptions
     */
    public Connection open(String protocol, String name, int mode, boolean timeouts) throws IOException {
        if (name.charAt(0) != '/' || name.charAt(1) != '/') {
            throw new IllegalArgumentException("Protocol must start with \"//\" " + name);
        }

        return this;
    }

    /**
     * Returns an input stream for this socket.
     *
     * @return     an input stream for reading bytes from this socket.
     * @exception  IOException  if an I/O error occurs when creating the
     *                          input stream.
     */
    public InputStream openInputStream() throws IOException {
       throw new IOException("Input stream not available");
    }

    /**
     * Returns an output stream for this socket.
     *
     * @return     an output stream for writing bytes to this socket.
     * @exception  IOException  if an I/O error occurs when creating the
     *                          output stream.
     */
    public OutputStream openOutputStream() throws IOException {
        OutputStream output = new ByteArrayOutputStream() {
            public void close() throws IOException {
                // Look for each file.
//System.err.println("closing deletefiles:// output stream");
                DataInputStream dis = new DataInputStream(new ByteArrayInputStream(buf, 0, count));
                try {
                    String path = dis.readUTF();
                    File file = new File(path);
                    file.delete();
                } catch (EOFException e) {
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        OutputStream res = new UniversalFilterOutputStream(this, output);
        return res;
    }

    /**
     * Closes the connection and deletes the files.
     *
     * @exception  IOException  if an I/O error occurs when closing the
     *                          connection.
     */
    public void close() throws IOException {
//System.err.println("closing deletefiles://");
        }
    }
