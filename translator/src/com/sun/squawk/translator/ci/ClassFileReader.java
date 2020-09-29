/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM translator.
 */
package com.sun.squawk.translator.ci;

import com.sun.squawk.translator.*;
import com.sun.squawk.util.Assert;
import com.sun.squawk.util.Tracer;
import com.sun.squawk.util.StructuredFileInputStream;

import java.io.*;

/**
 * An instance of <code>ClassFileReader</code> is used to read an input
 * stream opened on a class file.
 *
 * @author  Doug Simon
 */
public final class ClassFileReader extends StructuredFileInputStream {

    /**
     * Creates a <code>ClassFileReader</code> that reads class components
     * from a given input stream.
     *
     * @param   in        the input stream
     * @param   filePath  the file from which <code>in</code> was created
     */
    public ClassFileReader(InputStream in, String filePath) {
        super(in, filePath, "classfile");
    }

    /**
     * Throw a ClassFormatError instance to indicate there was an IO error
     * or malformed class file error while reading the class.
     *
     * @param   msg  the cause of the error
     * @return  the LinkageError raised
     */
    public Error formatError(String msg) {
        if (msg == null) {
            Translator.throwClassFormatError(getFileName());
        }
        Translator.throwClassFormatError(getFileName()+": "+msg);
        return null;
    }

    /**
     * Starts the decoding of an attribute from the class file. Once the body of the
     * attribute has been decoded, there should be a call to {@link Attribute#close}
     * so that the number of bytes decoded can be verified against the number of
     * bytes expected to be decoded.
     *
     * @param pool   the pool used to decode to the attribute's name
     * @return the header of the attribute about to be decoded
     */
    public Attribute openAttribute(ConstantPool pool) {
        int    nameIndex = readUnsignedShort("attribute_name_index");
        int    length    = readInt("attribute_length");
        String name      = pool.getUtf8(nameIndex);
        return new Attribute(length, getBytesRead(), name);
    }

    /**
     * An Attribute instance encapsulates the common details of all class file attributes.
     */
    public final class Attribute {
        /**
         * The number of bytes in the attribute.
         */
        public final int length;

        /**
         * The name of the attribute.
         */
        public final String name;

        /**
         * The class file offset at which the attribute's body starts.
         */
        private final int start;

        Attribute(int length, int start, String name) {
            this.length = length;
            this.start = start;
            this.name = name;
        }

        /**
         * Forwards the read position of the encapsulating ClassFileReader to the
         * byte immediately after this attribute in the class file.
         */
        public void skip() {
            ClassFileReader.this.skip(length, name);
        }

        /**
         * Ensures that the number of bytes read from the class file while decoding this
         * attribute is equal to the number of bytes specified in this attribute's constructor.
         *
         * @throws ClassFormatError if the number of bytes read is wrong
         */
        public void close() {
            if (getBytesRead() - start != length) {
                formatError("invalid attribute_length for " + name + " attribute");
            }
        }
    }
}
