/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk.vm;

/**
 * Method data structure. All the fields are UWords.
 * Keep in sync with msg.c.
 */
public class MessageStruct {

    /**
     * The buffer ID of the next message struct.
     */
    public final static int next = 0;

    /**
     * The status of the connection and message.
     */
    public final static int status = 1;

    /**
     * The offset to the start of the data.
     */
    public final static int data = 2;

    /**
     * The offset to the start of the key naming this message.
     */
    public final static int key = 3;

    /**
     * The start of the buffer.
     */
    public final static int HEADERSIZE = key * HDR.BYTES_PER_WORD;

    /**
     * The size of the maxumum message key. This must be kept in sync with
     * the definition of a messageStruct (e.g., in slowvm/src/vm/msg.c.
     */
    public final static int MAX_MESSAGE_KEY_SIZE = 128 - HEADERSIZE;
}
