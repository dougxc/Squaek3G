/*
 * Copyright 2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */

package com.sun.squawk.io.mailboxes;

import com.sun.squawk.util.Assert;

/**
 * A ByteArrayEnvelope can be used to pass a byte array, or subsection of a byte array
 * to a MailBox.
 *
 * The specified section of the byte array will be copied as the envelope is being sent to
 * the remote mailbox.
 */
public class ByteArrayEnvelope extends Envelope {
    private byte[] contents;
    private int offset;
    private int len;

    /**
     * Create a ByteArrayEnvelope for the subsection of the specified array.
     *
     * @param array the array of bytes to be sent.
     * @param offset offset to the first byte in the array to be sent.
     * @param len the number of bytes to be sent.
     */
    public ByteArrayEnvelope(byte[] array, int offset, int len) {
        Assert.that((offset >= 0) && (len >= 0) && (offset + len <= array.length));
        this.contents = array;
        this.offset = offset;
        this.len = len;
    }
    
    /**
     * Create a ByteArrayEnvelope for the specified array.
     *
     * @param array the array of bytes to be sent.
     * @return a new ByteArrayEnvelope for the array.
     */
    public ByteArrayEnvelope(byte[] array) {
        this.contents = array;
        this.offset = 0;
        this.len = array.length;
    }
    
    /**
     * Return the contents of the envelope.
     */
    public Object getContents() {
        checkCallContext();
        return contents;
    }
    
    /**
     * Return the contents of the envelope, which is a byte array.
     */
    public byte[] getData() {
        checkCallContext();
        return contents;
    }
    
    /**
     * Create a copy of this envelope.
     *
     * @return a copy
     */
    Envelope copy() {
        ByteArrayEnvelope theCopy = (ByteArrayEnvelope)super.copy();
        theCopy.contents = new byte[this.len];
        theCopy.offset = 0;
        // theCopy.len = this.len; // shallow copy handled this.
        
        System.arraycopy(this.contents, this.offset, theCopy.contents, 0, this.len);
        return theCopy;
    }
   
}