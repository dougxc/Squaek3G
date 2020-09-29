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
 * This class contains the offsets and types for fields that must be directly accessed
 * by the VM or other Squawk tools such as the mapper. The romizer ensures that these
 * offsets and types are correct when it creates the image for the bootstrap suite.
 * The value of the offset depends on whether the field is static or not - see
 * {@link Member#getOffset} for more detail.
 * <p>
 * A simple version of the field's type is also encoded into the high 32 bits of each entry
 * all of which are longs. The types are restricted to byte, char, short, int, long,
 * float, double, and Object and are encoded using the corresponding {@link CID} value
 * placed in the high 32 bits of the long.
 * <p>
 * This unfortunate encoding is needed if we want to keep the type and offset in one
 * place because these values are needed in parts of the VM that need to be executed
 * before normal object construction can take place. Two routines are provided to decode
 * the offset and type of a field, and the offset can also be obtained by casting the
 * field into an int.
 * <p>
 * The name of the constant must be composed of the name of the class that defines the
 * field (with '.'s replaced by '_'s) and the name of the field with a '$' separating them.
 */
public class FieldOffsets {

    private final static int  CIDSHIFT = 32;
    private final static long OOP   = ((long)CID.OBJECT) << CIDSHIFT;
    private final static long INT   = ((long)CID.INT)    << CIDSHIFT;
    private final static long SHORT = ((long)CID.SHORT)  << CIDSHIFT;

    /**
     * The offset of the 'self' field in in com.sun.squawk.Klass and the 'klass' field com.sun.squawk.ObjectAssociation
     * which must be identical.
     */
    public final static long com_sun_squawk_Klass$self = 0 + OOP;
    public final static long com_sun_squawk_ObjectAssociation$klass = com_sun_squawk_Klass$self;

    /**
     * The offset of the 'virtualMethods' field in com.sun.squawk.Klass and com.sun.squawk.ObjectAssociation.
     * which must be identical.
     */
    public final static long com_sun_squawk_Klass$virtualMethods = 1 + OOP;
    public final static long com_sun_squawk_ObjectAssociation$virtualMethods = com_sun_squawk_Klass$virtualMethods;

    /**
     * The offset of the 'staticMethods' field in com.sun.squawk.Klass.
     */
    public final static long com_sun_squawk_Klass$staticMethods = 2 + OOP;

    /**
     * The offset of the 'oopMap' field in com.sun.squawk.Klass.
     */
    public final static long com_sun_squawk_Klass$oopMap = 9 + OOP;

    /**
     * The offset of the 'oopMapWord' field in com.sun.squawk.Klass.
     */
    public final static long com_sun_squawk_Klass$oopMapWord = 10 + OOP;

    /**
     * The offset of the 'dataMap' field in com.sun.squawk.Klass.
     */
    public final static long com_sun_squawk_Klass$dataMap = 11 + OOP;

    /**
     * The offset of the 'dataMapWord' field in com.sun.squawk.Klass.
     */
    public final static long com_sun_squawk_Klass$dataMapWord = 12 + OOP;

    /**
     * The offset of the 'dataMapLength' field in com.sun.squawk.Klass.
     */
    public final static long com_sun_squawk_Klass$dataMapLength = (/*VAL*/false/*SQUAWK_64*/ ? 26 : 13) + INT;

    /**
     * The offset of the 'name' field in com.sun.squawk.Klass.
     */
    public final static long com_sun_squawk_Klass$name = 3 + OOP;

    /**
     * The offset of the 'componentType' field in com.sun.squawk.Klass.
     */
    public final static long com_sun_squawk_Klass$componentType = 4 + OOP;

    /**
     * The offset of the 'superType' field in com.sun.squawk.Klass.
     */
    public final static long com_sun_squawk_Klass$superType = 5 + OOP;

    /**
     * The offset of the 'objects' field in com.sun.squawk.Klass.
     */
    public final static long com_sun_squawk_Klass$objects = 8 + OOP;

    /**
     * The offset of the 'id' field in com.sun.squawk.Klass.
     */
    public final static long com_sun_squawk_Klass$id = (/*VAL*/false/*SQUAWK_64*/ ? 58 : 32) + SHORT;

    /**
     * The offset of the 'modifiers' field in com.sun.squawk.Klass.
     */
    public final static long com_sun_squawk_Klass$modifiers = (/*VAL*/false/*SQUAWK_64*/ ? 27 : 14) + INT;

    /**
     * The offset of the 'entryTable' field in com.sun.squawk.util.SquawkHashtable.
     */
    public final static long com_sun_squawk_util_SquawkHashtable$entryTable = 0 + OOP;

    /**
     * The offset of the 'vmThread' field in java.lang.Thread.
     */
    public final static long java_lang_Thread$vmThread = 0 + OOP;

    /**
     * The offset of the 'target' field in java.lang.Thread.
     */
    public final static long java_lang_Thread$target = 1 + OOP;

    /**
     * The offset of the 'vmThread' field in java.lang.Thread.
     */
    public final static long java_lang_Class$klass = 0 + OOP;

    /**
     * The offset of the 'isolate' field in com.sun.squawk.VMThread.
     */
    public final static long com_sun_squawk_VMThread$isolate = (/*VAL*/false/*SQUAWK_64*/ ? 0 : 2) + OOP;

    /**
     * The offset of the 'stack' field in com.sun.squawk.VMThread.
     */
    public final static long com_sun_squawk_VMThread$stack = (/*VAL*/false/*SQUAWK_64*/ ? 1 : 3) + OOP;

    /**
     * The offset of the 'stackSize' field in com.sun.squawk.VMThread.
     */
    public final static long com_sun_squawk_VMThread$stackSize = (/*VAL*/false/*SQUAWK_64*/ ? 2 : 4) + INT;

    /**
     * The offset of the 'savedStackChunks' field in com.sun.squawk.Isolate.
     */
    public final static long com_sun_squawk_Isolate$savedStackChunks = (/*VAL*/false/*SQUAWK_64*/ ? 0 : 24) + OOP;

    /**
     * Decodes a field's type from a given field descriptor.
     *
     * @param fieldDesc   an encoded field descriptor
     * @return the system ID of the type encoded in <code>fieldDesc</code>
     */
    public static int decodeSystemID(long fieldDesc) {
        return (int)(fieldDesc >> CIDSHIFT);
    }

    /**
     * Decodes a field's offset from a given field descriptor.
     *
     * @param fieldDesc   an encoded field descriptor
     * @return the offset encoded in <code>fieldDesc</code>. This is equivalent to <code>(int)field</code>
     */
    public static int decodeOffset(long fieldDesc) {
        return (int)fieldDesc;
    }
}

