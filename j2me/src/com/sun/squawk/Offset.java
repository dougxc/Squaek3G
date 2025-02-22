/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk;

import com.sun.squawk.pragma.*;
import com.sun.squawk.util.*;
import com.sun.squawk.vm.*;


/**
 * The offset type is used by the runtime system and collector to denote
 * the directed distance between two machine addresses. It is used instead
 * of 'int' or 'Object' for coding clarity, machine-portability (it can map
 * to 32 bit and 64 bit integral types) and access to unsigned operations
 * (Java does not have unsigned int types).
 * <p>
 * This class is known specially by the translator as a {@link Modifier#SQUAWKPRIMITIVE}
 * and programming with it requires adhering to the restrictions implied by this
 * attribute.
 * <p>
 * Only the public methods of this class which do not override any of the
 * methods in java.lang.Object will be available in a {@link VM#isHosted() non-hosted}
 * environment. The translator replaces any calls to these methods to native
 * method calls.
 * <p>
 * This mechanism was largely inspired by the VM_Address class in the Jikes RVM.
 *
 * @author Doug Simon
 */

public final class Offset {

    /**
     * Casts an offset expressed as the appropriate Java primitive type for the platform (i.e. int or long)
     * into a value of type Offset.
     *
     * @param  value   an offset expressed as an int or long
     * @return the canonical Offset instance for <code>offset</code>
     *
     * @vm2c code( return (Offset)value; )
     */
    public static Offset fromPrimitive(int/*S64*/ value) throws NativePragma {
        return get(value);
    }

    /**
     * Casts a value of type Offset into the appropriate Java primitive type for the platform (i.e. int or long).
     * This will cause a fatal error if this cast cannot occur without changing this offset's sign or
     * truncating its magnitude.
     *
     * @return this Offset value as an int or long
     *
     * @vm2c code( return this; )
     */
    public int/*S64*/ toPrimitive() throws NativePragma {
        return value;
    }

    /**
     * Casts a value of type Offset into an int. This will cause a fatal error if this offset
     * value cannot be expressed as a signed 32 bit Java int without changing its sign or
     * truncating its magnitude.
     *
     * @return this Offset value as an int
     *
     * @vm2c code( assume((int)this == this); return (int)this; )
     */
    public int toInt() throws NativePragma {
        Assert.that((int)value == value);
        return (int)value;
    }

    /**
     * Casts a value of type Offset into a UWord.
     *
     * @return this Offset value as a UWord
     *
     * @vm2c code( return (UWord)this; )
     */
    public UWord toUWord() throws NativePragma {
        return UWord.fromPrimitive(value);
    }

    /**
     * Gets the canonical Offset representation of <code>null</code>.
     *
     * @return the canonical Offset representation of <code>null</code>
     *
     * @vm2c code( return 0; )
     */
    public static Offset zero() throws NativePragma {
        return get(0);
    }

    /**
     * Adds a value to this offset and return the resulting offset.
     *
     * @param delta   the signed value to add
     * @return the result of adding <code>delta</code> to this offset
     *
     * @vm2c code( return this + delta; )
     */
    public Offset add(int delta) throws NativePragma {
        return get(value + delta);
    }

    /**
     * Subtracts a value from this offset and return the resulting offset.
     *
     * @param delta   the signed value to subract
     * @return the result of subtracting <code>delta</code> from this offset
     *
     * @vm2c code( return this - delta; )
     */
    public Offset sub(int delta) throws NativePragma {
        return get(value - delta);
    }

    /**
     * Scales this offset which currently expresses an offset in words to express
     * the same offset in bytes. That is, the value of this offset is multiplied by
     * the number of bytes in a machine word.
     *
     * @return  the scaled up offset
     *
     * @vm2c code( return this << HDR_LOG2_BYTES_PER_WORD; )
     */
    public Offset wordsToBytes() throws NativePragma {
        return get(value << HDR.LOG2_BYTES_PER_WORD);
    }

    /**
     * Scales this offset which currently expresses an offset in bytes to express
     * the same offset in words. That is, the value of this offset is divided by
     * the number of bytes in a machine word. This method should only be called for offsets
     * which are guaranteed to be a muliple of the number of bytes in a machine word.
     *
     * @return  the scaled down offset
     *
     * @vm2c code( return this >> HDR_LOG2_BYTES_PER_WORD; )
     */
    public Offset bytesToWords() throws NativePragma {
        Assert.that((value % HDR.BYTES_PER_WORD) == 0);
        return get(value >> HDR.LOG2_BYTES_PER_WORD);
    }

    /**
     * Determines if this offset is 0.
     *
     * @return true if this offset is 0.
     *
     * @vm2c code( return this == 0; )
     */
    public boolean isZero() throws NativePragma {
        return this == zero();
    }

    /**
     * Determines if this offset is equal to a given offset.
     *
     * @param offset2   the offset to compare this offset against
     * @return true if this offset is equal to <code>offset2</code>
     *
     * @vm2c code( return this == offset2; )
     */
    public boolean eq(Offset offset2) throws NativePragma {
        return this == offset2;
    }

    /**
     * Determines if this offset is not equal to a given offset.
     *
     * @param offset2   the offset to compare this offset against
     * @return true if this offset is not equal to <code>offset2</code>
     *
     * @vm2c code( return this != offset2; )
     */
    public boolean ne(Offset offset2) throws NativePragma {
        return this != offset2;
    }

    /**
     * Determines if this offset is less than a given offset.
     *
     * @param offset2   the offset to compare this offset against
     * @return true if this offset is less than or equals to <code>offset2</code>
     *
     * @vm2c code( return this < offset2; )
     */
    public boolean lt(Offset offset2) throws NativePragma {
        return this.value < offset2.value;
    }

    /**
     * Determines if this offset is less than or equal to a given offset.
     *
     * @param offset2   the offset to compare this offset against
     * @return true if this offset is less than or equal to <code>offset2</code>
     *
     * @vm2c code( return this <= offset2; )
     */
    public boolean le(Offset offset2) throws NativePragma {
        return (this == offset2) || lt(offset2);
    }

    /**
     * Determines if this offset is greater than a given offset.
     *
     * @param offset2   the offset to compare this offset against
     * @return true if this offset is greater than <code>offset2</code>
     *
     * @vm2c code( return this > offset2; )
     */
    public boolean gt(Offset offset2) throws NativePragma {
        return offset2.lt(this);
    }

    /**
     * Determines if this offset is greater than or equal to a given offset.
     *
     * @param offset2   the offset to compare this offset against
     * @return true if this offset is greater than or equal to <code>offset2</code>
     *
     * @vm2c code( return this >= offset2; )
     */
    public boolean ge(Offset offset2) throws NativePragma {
        return offset2.le(this);
    }

    /*-----------------------------------------------------------------------*\
     *                      Hosted execution support                         *
    \*-----------------------------------------------------------------------*/

    /**
     * Gets a hashcode value for this offset which is just the value itself.
     *
     * @return  the value of this offset
     */
    public int hashCode() throws HostedPragma {
        return (int)value;
    }

    /**
     * Gets a string representation of this offset.
     *
     * @return String
     */
    public String toString() throws HostedPragma {
        return ""+value;
    }

    /**
     * The offset value.
     */
    private final int/*S64*/ value;

    /**
     * Unique instance pool.
     */
    private static /*S64*/IntHashtable pool;

    /**
     * Gets the canonical Offset instance for a given offset.
     *
     * @param  value   the machine offset
     * @return the canonical Offset instance for <code>value</code>
     */
    private static Offset get(int/*S64*/ value) throws HostedPragma {
        if (pool == null) {
            pool = new /*S64*/IntHashtable();
        }
        Offset instance = (Offset)pool.get(value);
        if (instance == null) {
            instance = new Offset(value);
            try {
                pool.put(value, instance);
            } catch (OutOfMemoryError e) {
                throw new OutOfMemoryError("Failed to grow instance pool when adding " + value);
            }
        }
        return instance;
    }

    /**
     * Constructor.
     *
     * @param value  a machine offset
     */
    private Offset(int/*S64*/ value) throws HostedPragma {
        this.value = value;
    }
}
