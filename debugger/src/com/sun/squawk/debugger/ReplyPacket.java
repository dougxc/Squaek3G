/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk.debugger;

import java.io.*;

/**
 * A <code>ReplyPacket</code> encapsulates a JDWP reply packet.
 */
public final class ReplyPacket extends Packet {

    /**
     * The error code of the reply.
     */
    private int errorCode;

    /**
     * Creates a packet to send a reply to a command packet.
     *
     * @param command   the packet to which this is a reply
     * @param errorCode the error code of the reply
     */
    public ReplyPacket(CommandPacket command, int errorCode) {
        super(command.getID());
        this.errorCode = errorCode;
    }

    /**
     * Creates a packet to encapsulate a received JDWP reply packet.
     *
     * @param id          the identifier in the reply
     * @param dataLength  the length of the data to read from <code>data</code>
     * @param data        the contents of the data field of the packet
     * @param errorCode   the error code
     * @throws IOException
     */
    public ReplyPacket(int id, int dataLength, DataInputStream data, int errorCode) throws IOException {
        super(id, dataLength, data);
        this.errorCode = errorCode;
    }

    public void updateErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    /**
     * {@inheritDoc}
     */
    protected void writeFields(DataOutputStream dos) throws IOException {
        dos.writeShort(errorCode);
    }

    /**
     * @return  the value of this reply's error code
     */
    public int getErrorCode() {
        return errorCode;
    }

    public int getFlags() {
        return FLAG_REPLY;
    }

    /**
     * {@inheritDoc}
     */
    public String toString(boolean appendData) {
        StringBuffer buf = new StringBuffer("ReplyPacket[id=").
            append(getID()).
            append(",size=").
            append(getSize());
        if (errorCode != JDWP.Error_NONE) {
            buf.append(",error=").append(errorCode);
        }
        buf.append("]");
        if (appendData) {
            appendData(buf);
        }
        return buf.toString();
    }
}
