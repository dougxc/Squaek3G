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
 * An ObjectMemoryFile encapsulates all the data in a serialized object graph.
 * The format of a serialized object graphic is
 * described by the following pseudo C struct:
 *
 * <p><hr><blockquote><pre>
 *    ObjectMemory {
 *        u4 magic               // 0xDEADBEEF
 *        u2 minor_version;
 *        u2 major_version;
 *        u4 attributes;         // mask of the ATTRIBUTE_* constants in this class
 *        u4 parent_hash;
 *        utf8 parent_uri;
 *        u4 root;               // offset (in bytes) in 'memory' of the root of the graph
 *        u4 size;               // size (in bytes) of memory
 *        u1 oopmap[((size / HDR.BYTES_PER_WORD) + 7) / 8];
 *        u1 padding[n];         // 0 <= n < HDR.BYTES_PER_WORD to align 'memory' on a word boundary
 *        u1 memory[size];
 *        u1 typemap[size];      // only present if ATTRIBUTE_TYPEMAP is set
 *    }
 * </pre></blockquote><hr><p>
 *
 *
 * @author  Doug Simon
 */
public final class ObjectMemoryFile {

    /**
     * Denotes a object memory file that has a type map describing the type of the value at every
     * address in the 'memory' component. The entries in the map are described in
     * {@link com.sun.squawk.vm.AddressType}.
     */
    public static final int ATTRIBUTE_TYPEMAP = 0x01;

    /**
     * Denotes a object memory file that is only compatible with a 32 bit system. Otherwise the object memory
     * file is only compatible with a 64 bit system.
     */
    public static final int ATTRIBUTE_32BIT = 0x02;

    /**
     * Denotes a object memory file that is in big endian format. Otherwise the object memory
     * file is in little endian format.
     */
    public static final int ATTRIBUTE_BIGENDIAN = 0x04;

    public final int minor;
    public final int major;
    public final int attributes;
    public final int parentHash;
    public final String parentURI;
    public final ObjectMemory objectMemory;

    public ObjectMemoryFile(int minor,
                            int major,
                            int attributes,
                            int parentHash,
                            String parentURI,
                            ObjectMemory objectMemory)
    {
        this.minor = minor;
        this.major = major;
        this.attributes = attributes;
        this.parentHash = parentHash;
        this.parentURI = parentURI;
        this.objectMemory= objectMemory;
    }

    /**
     * Determines if <code>attributes</code> value in this object memory file denoted a big endian format memory.
     *
     * @return true if <code>(this.attributes & ATTRIBUTE_BIGENDIAN) != 0</code>
     */
    public boolean isBigEndian() {
        return (attributes & ATTRIBUTE_BIGENDIAN) != 0;
    }
}
