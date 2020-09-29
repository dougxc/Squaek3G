/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk.debugger.sdp;

import java.io.*;

import com.sun.squawk.debugger.*;
import com.sun.squawk.debugger.JDWPListener.*;

/*-----------------------------------------------------------------------*\
 *                            Proxy Command Set                          *
\*-----------------------------------------------------------------------*/

/**
 * Abstract base class for command set handlers that execute in the context of an SDP instance.
 */
abstract class SDPCommandSet extends CommandSet {
    SDP sdp;
    JDWPListener host;
    JDWPListener otherHost;

    /**
     * Handles a command packet by setting up the variables used to interpret and reply
     * to the command and then dispatching to the specific command handler.
     *
     * @param sdp        the SDP context
     * @param host       connection to the host that sent the command
     * @param otherHost  connection to the host for which the command was ultimately intended
     * @param command    the command to be handled
     * @return boolean   true if the command was recognised and a reply was sent to <code>myHost</code>
     * @throws IOException if there was an IO error while sending a reply
     */
    public boolean handle(SDP sdp, JDWPListener myHost, JDWPListener otherHost, CommandPacket command) throws IOException {
        this.sdp = sdp;
        this.host = myHost;
        this.otherHost = otherHost;
        return handle(myHost, command);
    }
}
