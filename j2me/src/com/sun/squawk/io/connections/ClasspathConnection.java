/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 */

package com.sun.squawk.io.connections;

import java.io.*;
import javax.microedition.io.*;

public interface ClasspathConnection extends Connection {

    public InputStream openInputStream(String name) throws IOException;
    
    /**
     * Convenience method that provides access to the bytes to be found by doing a {@link #openInputStream(String) }
     * on file named <code>name</code>.
     * 
     * @param name Name of the resource to open and fetch bytes from
     * @return byte[] bytes read in
     * @throws IOException if any error occurs opening or reading bytes from the stream {@link #openInputStream(String) }
     */
    public byte[] getBytes(String name) throws IOException;

}

