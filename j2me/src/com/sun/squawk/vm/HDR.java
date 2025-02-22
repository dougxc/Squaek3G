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
 * This class contains constants that describe the object header layout.
 */
public class HDR {

    /**
     * The size (in bytes) of a pointer.
     */
    public final static int BYTES_PER_WORD = (/*VAL*/false/*SQUAWK_64*/ ? 8 : 4);

    /**
     * The amount by which to right shift a byte offset to find the offset of the word that contains the indexed byte.
     */
    public final static int LOG2_BYTES_PER_WORD = (/*VAL*/false/*SQUAWK_64*/ ? 3 : 2);

    /**
     * The number of bits per byte.
     */
    public final static int BITS_PER_BYTE = 8;

    /**
     * The number of bits in a pointer.
     */
    public final static int BITS_PER_WORD = BITS_PER_BYTE * BYTES_PER_WORD;

    /**
     * The amount by which to right shift a bit index to yield the word in a bitmap containing the indexed bit.
     */
    public final static int LOG2_BITS_PER_WORD = (/*VAL*/false/*SQUAWK_64*/ ? 6 : 5);

    /**
     * The amount by which to right shift a bit index to yield the byte in a bitmap containing the indexed bit.
     */
    public final static int LOG2_BITS_PER_BYTE = 3;

    /**
     * The offset (in words) from an object's address to the class pointer in the object's header.
     */
    public final static int klass = -1;

    /**
     * The offset (in words) from an object's address to array length in the object's header.
     */
    public final static int length = -2;

    /**
     * The offset (in words) from an method body's address to the defining class pointer in the method body's header.
     */
    public final static int methodDefiningClass = -3;

    /**
     * The offset (in bytes) from an method body's address to the start of the info block in the method body's header.
     */
    public final static int methodInfoStart = (methodDefiningClass * BYTES_PER_WORD) - 1;

    /**
     * The size (in bytes) of an object header for a non-array, non-method-body object.
     */
    public final static int basicHeaderSize = BYTES_PER_WORD;

    /**
     * The size (in bytes) of an object header for an array object.
     */
    public final static int arrayHeaderSize = BYTES_PER_WORD * 2;

    /**
     * The number of low order bits in the first word of an object header that specify the format of the object header.
     */
    public final static int headerTagBits = 2;

    /**
     * The mask that is applied to the first word of an object header to extract the format tag.
     */
    public final static int headerTagMask = 3;

    /**
     * The object header format tag value specifying a non-array, non-method-body object header.
     */
    public final static int basicHeaderTag = 0x0; /* 00 */

    /**
     * The object header format tag value specifying an array object header.
     */
    public final static int arrayHeaderTag = 0x1; /* 01 */

    /**
     * The object header format tag value specifying a mehod body object header.
     */
    public final static int methodHeaderTag = 0x3; /* 11 */

    /**
     * The bit in a class pointer word that is set if the object has been forwarded.
     */
    public final static int forwardPointerBit = 0x2; /* 10 */
}
