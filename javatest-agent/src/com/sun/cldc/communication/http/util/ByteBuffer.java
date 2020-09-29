/*
 * @(#)ByteBuffer.java	1.2 03/01/07
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.sun.cldc.communication.http.util;

import java.io.*;

public final class ByteBuffer {

    private byte[] myBuf;
    private int currentOffset=0;
    private boolean verboseIsOn=false;

    public ByteBuffer(int init_capacity) {
        myBuf = new byte[init_capacity];
        verboseIsOn = false;
    }

    public void setVerbose(boolean val) {
        verboseIsOn = val;
    }

    public void write(DataInputStream from, int len) throws IOException {
        check(len);
        from.readFully(myBuf, currentOffset, len);
        currentOffset += len;
    }

    public int dataLength() {
        return currentOffset;
    }

    public byte[] dataCopy() {
        byte bb[] = new byte[currentOffset];
        System.arraycopy(myBuf, 0, bb, 0, currentOffset);
        return bb;
    }

    public byte[] data() {
        return myBuf;
    }

    public void reset() {
        currentOffset = 0;
    }


    private void check(int len) {
        int cap = currentOffset + len; // needed capacity
        if (myBuf.length < cap) {
            if (verboseIsOn) System.out.println("Reallocating buffer: " + myBuf.length + " --> " + cap);
            byte[] temp = new byte[cap];
            System.arraycopy(myBuf, 0, temp, 0, currentOffset);
            myBuf = temp;
        }
    }        
}
