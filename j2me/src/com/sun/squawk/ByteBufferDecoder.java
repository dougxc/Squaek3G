/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk;



/**
 * A byte buffer dencoder can be used to decode a byte array of values encoded
 * with a {@link ByteBufferEncoder byte buffer encoder}.
 *
 * @author  Doug Simon
 */
public class ByteBufferDecoder extends GeneralDecoder {

    /**
     * The byte array of encoded values.
     */
    protected byte[] buf;

    /**
     * The current decoding position.
     */
    protected int pos;

    /**
     * Creates a ByteBufferDecoder to decode a byte array of values that
     * was encoded by a ByteArryEncoder.
     *
     * @param buf  the byte array of encoded values
     * @param pos  the initial decoding position
     */
    public ByteBufferDecoder(byte[] buf, int pos) {
        this.buf = buf;
        this.pos = pos;
    }

    /**
     * Get the next byte
     *
     * @return the next byte
     */
    int nextByte() {
        return buf[pos++];
    }

    /**
     * Decodes a UTF8 encoded string from the current position.
     *
     * @return  the decoded string
     */
    String readUtf8() {
        int length = readUnsignedShort();
        char[] chars = new char[length];
        for (int i = 0; i != chars.length; ++i) {
            chars[i] = readChar();
        }
        return new String(chars);
    }

}
