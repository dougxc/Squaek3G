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

import com.sun.squawk.vm.ChannelConstants;

/**
 * Message Notifier Connection
 */
public class NotifierProtocol extends ConnectionBase implements StreamConnectionNotifier {

    /**
     * The namespace target for the message.
     */
    private String name;

    /**
     * Client open.
     */
    public Connection open(String protocol, String name, int mode, boolean timeouts) throws IOException {
        if (protocol == null || name == null) {
            throw new NullPointerException();
        }
        this.name = name;
        return this;
    }

    /**
     * acceptAndOpen
     */
    public StreamConnection acceptAndOpen() throws IOException {
        Message msg = Database.receiveFromClient(name);
        return MessageResourceManager.allocateServerProtocol(name, msg);
    }
}
