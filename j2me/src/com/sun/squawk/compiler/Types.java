/*
 * Copyright 2003-2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: Types.java,v 1.7 2005/01/18 23:21:06 ccifue Exp $
 */

package com.sun.squawk.compiler;

/**
 * Data types supported by the <code>Compiler</code> interface.
 *
 * @author   Nik Shaylor
 */
public interface Types {

    /**
     * Define primary types.
     */
    public static final Type REF     = Type.REF,        // A 16/32/64 bit address (depending on the system)
                             OOP     = Type.OOP,        // A reference to a Java object
                             INT     = Type.INT,        // 32 bit signed integer
                             UINT    = Type.UINT,       // 32 bit unsigned integer
                             LONG    = Type.LONG,       // 64 bit signed integer
                             ULONG   = Type.ULONG,      // 64 bit unsigned integer
                             FLOAT   = Type.FLOAT,      // 32 bit floating point number
                             DOUBLE  = Type.DOUBLE;     // 64 bit floating point number

    /**
     * Define secondary types.
     */
    public static final Type BYTE    = Type.BYTE,       // 8 bit signed integer
                             UBYTE   = Type.UBYTE,      // 8 bit unsigned integer
                             SHORT   = Type.SHORT,      // 16 bit signed integer
                             USHORT  = Type.USHORT;     // 16 bit unsigned integer


    /**
     * Special dummy type for call and return.
     */
    public static final Type VOID    = Type.VOID;       // Pseudo type

    /**
     * Special dummy types for supporting building of the <code>Interpreter</code>.
     */
    public static final Type MP      = Type.MP,         // Pseudo types
                             IP      = Type.IP,
                             LP      = Type.LP,
                             SS      = Type.SS;

    /**
     * Define the size of stack entries.
     */
    public static final Type WORD    = Type.WORD,
                             UWORD   = Type.UWORD;

    /**
     * Relocation type for absolute integer addresses.
     */
    public static final int RELOC_ABSOLUTE_INT = Type.RELOC_ABSOLUTE_INT;

    /**
     * Relocation type for relative integer addresses.
     */
    public static final int RELOC_RELATIVE_INT = Type.RELOC_RELATIVE_INT;

}
