/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk;

import com.sun.squawk.util.*;



/**
 * A byte buffer dencoder can be used to decode a byte array of values encoded
 * with a {@link ByteBufferEncoder byte buffer encoder}.
 *
 * @author  Doug Simon
 */
public final class VMBufferDecoder extends GeneralDecoder {

    /**
     * The VM address of the object.
     */
    protected Object oop;

    /**
     * The offset of the next byte.
     */
    protected int offset;

    /**
     * Create a VMBufferDecoder.
     */
    VMBufferDecoder() {
    }

    /**
     * Create a VMBufferDecoder to decode from an absolute byte buffer.
     *
     * @param oop    the VM address of object
     * @param offset the offset of the first byte
     */
    VMBufferDecoder(Object oop, int offset) {
        this.oop = oop;
        this.offset = offset;
    }

    /**
     * Get the next byte
     *
     * @return the next byte
     */
    int nextByte() {
        int b = NativeUnsafe.getByte(oop, offset);
        offset += 1;
        return b;
    }

    /**
     * Check that the offset is s certain value
     *
     * @param offset the value to be checked.
     */
    void checkOffset(int offset) {
        Assert.that(this.offset == offset);
    }

    /**
     * Get the current offset.
     *
     * @return the offset
     */
    int getOffset() {
        return offset;
    }

    /**
     * Reset the VMBufferDecoder to decode from a new byte buffer.
     *
     * @param oop    the VM address of object
     * @param offset the offset of the first byte
     */
    void reset(Object oop, int offset) {
        this.oop = oop;
        this.offset = offset;
    }
}
