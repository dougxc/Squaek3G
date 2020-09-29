/*
 * Copyright 2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */

package com.sun.squawk.io.mailboxes;

import java.io.ByteArrayInputStream;
import com.sun.squawk.util.Assert;
import com.sun.squawk.Isolate;

/**
 * A ByteArrayInputStreamEnvelope can be used to pass a byte array, or subsection of a byte array, as a 
 * ByteArrayInputStream. This is a zero-copy way  (except for the envelope object itself) to send the 
 * contents of a byte array to another isolate.
 *
 * The main drawback of using ByteArrayInputStreamEnvelopes instead of ByteArrayEnvelopes is that
 * if the sender makes any changes to the byte array encapsulated by the ByteArrayInputStreamEnvelope,
 * these changes may be visible to the receiver of the envelope. 
 *
 * This can avoided in applications that implement replies or awknowledgements for all sent envelopes by 
 * not re-using the original byte array until the receiver sends some reply or acknowledgment back.
 * 
 */
public class ByteArrayInputStreamEnvelope extends Envelope {
    private ByteArrayInputStream contents;
    
    /**
     * Create a ByteArrayEnvelope for the subsection of the specified array.
     *
     * @param array the array of bytes to be sent.
     * @param offset offset to the first byte in the array to be sent.
     * @param len the number of bytes to be sent.
     */
    public ByteArrayInputStreamEnvelope(byte[] array, int offset, int len) {
        contents = new ByteArrayInputStream(array, offset, len);
    }
    
    /**
     * Create a ByteArrayEnvelope for the specified array.
     *
     * @param array the array of bytes to be sent.
     */
    public ByteArrayInputStreamEnvelope(byte[] array) {
        contents = new ByteArrayInputStream(array);
    }

    /**
     * Return the contents of the envelope.
     */
    public Object getContents() {
        checkCallContext();
        return contents;
    }
    
    /**
     * Return the contents of the envelope, which is a ByteArrayInputStream.
     * Note that this should not be called by the sending isolate.
     */
    public ByteArrayInputStream getData() {
        checkCallContext();
        return contents;
    }
    
    /**
     * Create a copy of this envelope.
     *
     * @return a copy
     */
    Envelope copy() {
        // a shallow copy is fine.
        return super.copy();
    }
   
    
}