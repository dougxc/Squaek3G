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
 * Method buffer data structure. All the fields are UWords.
 */
public class MessageBuffer {

    /**
     * The buffer ID of the next buffer.
     */
    public final static int next = 0;

    /**
     * The read position in in bytes in the buffer.
     */
    public final static int pos = 1;

    /**
     * The number of bytes written in the buffer.
     */
    public final static int count = 2;

    /**
     * The offset to the start of the data.
     */
    public final static int buf = 3;

    /**
     * The start of the buffer.
     */
    public final static int HEADERSIZE = buf * HDR.BYTES_PER_WORD;

    /**
     * The size in bytes of the buf part of the buffer.
     */
    public final static int BUFFERSIZE = 128 - HEADERSIZE;
}
