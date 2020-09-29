/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk.io.j2me.multicast;

import java.io.*;

import com.sun.squawk.io.MulticastOutputStream;
import javax.microedition.io.Connection;

import com.sun.squawk.io.*;
import com.sun.squawk.util.StringTokenizer;
import java.util.Vector;

import javax.microedition.io.*;

/**
 * GCF connection that multicasts to a number of configured output streams. The connection
 * can be established without specifying any delegate streams and have the delegate streams
 * added as a separate operation as shown below:
 * <p><blockquote><pre>
 *     MulticastOutputStream mos = (MulticastOutputStream)Connector.openOutputStream("multicast:");
 *     OutputStream fos = Connector.openOutputStream("file://out.txt;append=true");
 *     OutputStream sos = Connector.openOutputStream("socket://host.domain.com:9999");
 *     DataOutputStream dos = new DataOutputStream(mos);
 *     mos.add(fos);
 *     mos.add(sos);
 *
 *     dos.writeUTF("Hello World");
 * </pre></blockquote></p>
 *
 * Or the connection can be established and have one or more initial delegate streams opened as shown below:
 *
 * <p><blockquote><pre>
 *     MulticastOutputStream mos = (MulticastOutputStream)Connector.openOutputStream("multicast:file://out.txt\;append=true;socket://host.domain.com:9999");
 *     DataOutputStream dos = new DataOutputStream(mos);
 *
 *     dos.writeUTF("Hello World");
 * </pre></blockquote></p>
 *
 * As indicated above, the URLs for any initial delegates are separated by <code>;</code> and as such must have
 * any embedded <code>;</code>s escaped with a backslash.
 *
 * @author Doug Simon
 */
public class Protocol extends ConnectionBase implements OutputConnection {

    private String[] initialConnections = {};

    /**
     * Opens the connection.
     *
     */
    public Connection open(String protocol, String name, int mode, boolean timeouts) throws IOException {

        if (name.length() != 0) {
            Vector names = new Vector();
            StringTokenizer st = new StringTokenizer(name, ";");

            String part = null;

            while (st.hasMoreTokens()) {
                String s = st.nextToken();
                if (s.endsWith("\\")) {
                    if (part != null) {
                        part += ";" + s;
                    } else {
                        part = s;
                    }
                } else {
                    if (part != null) {
                        s = part + ";" + s;
                        part = null;
                    }
                    names.addElement(s);
                }
            }

            initialConnections = new String[names.size()];
            names.copyInto(initialConnections);
        }

        return this;
    }

    /**
     * Opens and returns an output stream for this connection. The returned output stream
     * will be a {@link MulticastOutputStream} instance.
     *
     * @return                 a <code>MulticastOutputStream</code> instance
     * @exception IOException  if an I/O error occurs
     */
    public OutputStream openOutputStream() throws IOException {

        MulticastOutputStream mos = new MulticastOutputStream();
        for (int i = 0; i != initialConnections.length; ++i) {
            String name = initialConnections[i];
            OutputStream os = Connector.openOutputStream(name);
            mos.add(name, os);
        }

        return mos;
    }
}

