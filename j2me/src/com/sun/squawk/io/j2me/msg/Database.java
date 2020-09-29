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

import com.sun.squawk.*;
import com.sun.squawk.UWord;
import com.sun.squawk.Unsafe;
import com.sun.squawk.VM;
import com.sun.squawk.util.*;
import com.sun.squawk.vm.*;

/**
 * The message database interface.
 */
public class Database {

    /**
     * The size of the maxumum key.
     */
    private static int MAX_MESSAGE_KEY_SIZE = MessageStruct.MAX_MESSAGE_KEY_SIZE;

    /**
     * Allocates a message buffer.
     *
     * @return the addrss of the buffer or null is none was available
     */
    static Address allocateBuffer() throws IOException {
	Address res = VM.execMessageIO(ChannelConstants.INTERNAL_ALLOCATE_MESSAGE_BUFFER, null, null, 0);
        Assert.that(Unsafe.getUWord(res, MessageBuffer.pos).eq(UWord.zero()));
        Assert.that(Unsafe.getUWord(res, MessageBuffer.next).eq(UWord.zero()));
        Assert.that(Unsafe.getUWord(res, MessageBuffer.count).eq(UWord.zero()));
        return res;
    }

    /**
     * Frees a message buffer.
     *
     * @param buffer the buffer
     */
    static void freeBuffer(Address buffer) {
	try {
             VM.execMessageIO(ChannelConstants.INTERNAL_FREE_MESSAGE_BUFFER, null, buffer, 0);
	} catch (IOException e) {
	     /* discard -- should not happen */
	}
    }

    /**
     * Send a message.
     *
     * @param op the opcode
     * @param key the message key
     * @param message the message to be sent
     */
    private static void send(int op, String key, Message message, int status) throws IOException {
        Assert.always(key.length() <= MAX_MESSAGE_KEY_SIZE, "Message key must be less than "+MAX_MESSAGE_KEY_SIZE+1);
        VM.execMessageIO(op, key, message.getData(), status);
    }

    /**
     * Retrieve a message from a client.
     *
     * @param op the opcode
     * @param key the message key
     * @return the message
     */
    private static Message receive(int op, String key) throws IOException {
        Assert.always(key.length() <= MAX_MESSAGE_KEY_SIZE, "Message key must be less than "+MAX_MESSAGE_KEY_SIZE+1);
        Address data = VM.execMessageIO(op, key, null, 0);
        return MessageResourceManager.allocateMessage(data);
    }

    /**
     * Send a message to the server.
     *
     * @param key the message key
     * @param message the message to be sent
     */
    static void sendToServer(String key, Message message, int status) throws IOException {
        send(ChannelConstants.INTERNAL_SEND_MESSAGE_TO_SERVER, key, message, ChannelConstants.RESULT_OK);
    }

    /**
     * Send a message to the server.
     *
     * @param key the message key
     * @param message the message to be sent
     */
    static void sendToClient(String key, Message message, int status) throws IOException {
        send(ChannelConstants.INTERNAL_SEND_MESSAGE_TO_CLIENT, key, message, status);
    }

    /**
     * Retrieve a message from a client.
     *
     * @param key the message key
     * @return the message
     */
    public static Message receiveFromClient(String key) throws IOException {
        return receive(ChannelConstants.INTERNAL_RECEIVE_MESSAGE_FROM_CLIENT, key);
    }

    /**
     * Retrieve a message from a server.
     *
     * @param key the message key
     * @return the message
     */
    static Message receiveFromServer(String key) throws IOException {
        return receive(ChannelConstants.INTERNAL_RECEIVE_MESSAGE_FROM_SERVER, key);
    }

}


