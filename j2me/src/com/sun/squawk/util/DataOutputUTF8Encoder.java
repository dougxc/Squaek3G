/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk.util;

import java.io.*;

/**
 * This class provides one function for UTF-8 encoding a string to a
 * {@link DataOutput}. This provides almost the same functionality as
 * {@link DataOutputStream#writeUTF} except that it can be used
 * to encode a UTF-8 string with a 4-byte length header as opposed to
 * the standard 2-byte length header.
 *
 * @author  Doug Simon
 */
public final class DataOutputUTF8Encoder {

    private DataOutputUTF8Encoder() {
    }

    /**
     * Writes a string to <code>out</code> in UTF-8 encoded form.
     *
     * @param str           the string to encode
     * @param out           a data output stream.
     * @param twoByteLength if true, then the length of the encoded string is to be encoded in two bytes as opposed to 4
     * @return the decoded string
     */
    public final static int writeUTF(String str, DataOutput out, boolean twoByteLength) throws IOException {

        int strlen = str.length();
        int utflen = 0;
        char[] charr = new char[strlen];
        int c, count = 0;

        str.getChars(0, strlen, charr, 0);

        for (int i = 0; i < strlen; i++) {
            c = charr[i];
            if ((c >= 0x0001) && (c <= 0x007F)) {
                utflen++;
            } else if (c > 0x07FF) {
                utflen += 3;
            } else {
                utflen += 2;
            }
        }

        int maxLen = twoByteLength ? 65535 : Integer.MAX_VALUE;

        if (utflen > maxLen) {
            throw new UTFDataFormatException();
        }
        byte[] bytearr = new byte[utflen + (twoByteLength ? 2 : 4)];
        if (!twoByteLength) {
            bytearr[count++] = (byte) ((utflen >>> 24) & 0xFF);
            bytearr[count++] = (byte) ((utflen >>> 16) & 0xFF);
        }
        bytearr[count++] = (byte) ((utflen >>> 8) & 0xFF);
        bytearr[count++] = (byte) ((utflen >>> 0) & 0xFF);
        for (int i = 0; i < strlen; i++) {
            c = charr[i];
            if ((c >= 0x0001) && (c <= 0x007F)) {
                bytearr[count++] = (byte) c;
            } else if (c > 0x07FF) {
                bytearr[count++] = (byte) (0xE0 | ((c >> 12) & 0x0F));
                bytearr[count++] = (byte) (0x80 | ((c >>  6) & 0x3F));
                bytearr[count++] = (byte) (0x80 | ((c >>  0) & 0x3F));
            } else {
                bytearr[count++] = (byte) (0xC0 | ((c >>  6) & 0x1F));
                bytearr[count++] = (byte) (0x80 | ((c >>  0) & 0x3F));
            }
        }
        out.write(bytearr);
        return utflen + 2;
    }
}
