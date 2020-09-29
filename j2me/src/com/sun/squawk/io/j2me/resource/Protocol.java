//if[RESOURCE.CONNECTION]
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

package com.sun.squawk.io.j2me.resource;

import java.io.*;
import javax.microedition.io.*;
import com.sun.squawk.io.*;
import com.sun.squawk.io.connections.*;
import com.sun.squawk.*;
import com.sun.squawk.VMThread;

/**
 * This class implements the default "resource:" protocol for KVM.
 *
 * The default is to open a file based upon the resource name.
 *
 * @author  Nik Shaylor
 * @version 1.0 2/12/2000
 */
public class Protocol extends ConnectionBase implements InputConnection {

    private ClasspathConnection pathConnection;
	private byte [] resourceData;
    private String name;

    /**
     * Open the connection
     */
    public Connection open(String protocol, String name, int mode, boolean timeouts) throws IOException {
		pathConnection = null;
		resourceData = null;
        this.name = name;

		// look for the resource file in the current leaf suite, and then up the chain of parent suites until we find it
		Suite suite = VM.getCurrentIsolate().getLeafSuite();			
		while (suite != null) {
			resourceData = suite.getResourceData(name);
			if (resourceData != null) {
				return this;
			}
			suite = suite.getParent();
		}

		// couldn't find the specified resource file in the suite, so load it from the classpath
        String resourcePath = VMThread.currentThread().getIsolate().getClassPath();
        if (resourcePath == null) {
            resourcePath = ".";
        }
        if (pathConnection == null) {
            pathConnection = (ClasspathConnection)Connector.open("classpath://" + resourcePath);
        }
        return this;
    }

    public InputStream openInputStream() throws IOException {
		if (resourceData != null) {
			// the resource file is stored in one of the suites in memory, so create a new input stream from there...
			return new ByteArrayInputStream(resourceData);
		}  else {
			// otherwise open the resource file from the class path
			return pathConnection.openInputStream(name);
		}
    }

    public void close() throws IOException {
    }
}
