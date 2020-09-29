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
 * A collection of methods for performing peek and poke operations on memory addresses.
 *
 * @author  Nik Shaylor, Doug Simon
 */
public final class Unsafe {

    private Unsafe() {}

    /**
     * Performs access check.
     */
    static {
        if (!VM.getCurrentIsolate().isTrusted()) {
            throw new Error("illegal access to com.sun.squawk.Unsafe");
        }
    }

    /*-----------------------------------------------------------------------*\
     *                      Storing to/loading from memory                   *
    \*-----------------------------------------------------------------------*/


    /**
     * Sets an 8 bit value in memory.
     *
     * @param base   the base address
     * @param offset the offset (in bytes) from <code>base</code> at which to write
     * @param value the value to write
     */
     public static void setByte(Object base, int offset, int value) {
         NativeUnsafe.setByte(base, offset, value);
     }

     /**
      * Sets a signed 16 bit value in memory.
      *
      * @param base   the base address
      * @param offset the offset (in 16 bit words) from <code>base</code> at which to write
      * @param value  the value to write
      */
    public static void setShort(Object base, int offset, int value) {
        NativeUnsafe.setShort(base, offset, value);
    }

    /**
     * Sets an unsigned 16 bit value in memory.
     *
     * @param base   the base address
     * @param offset the offset (in 16 bit words) from <code>base</code> at which to write
     * @param value  the value to write
     */
    public static void setChar(Object base, int offset, int value) {
        NativeUnsafe.setChar(base, offset, value);
    }

    /**
     * Sets a 32 bit value in memory.
     *
     * @param base   the base address
     * @param offset the offset (in 32 bit words) from <code>base</code> at which to write
     * @param value  the value to write
     */
    public static void setInt(Object base, int offset, int value) {
        NativeUnsafe.setInt(base, offset, value);
    }

    /**
     * Sets a UWord value in memory.
     *
     * @param base   the base address
     * @param offset the offset (in UWords) from <code>base</code> at which to write
     * @param value  the value to write
     */
    public static void setUWord(Object base, int offset, UWord value) {
        NativeUnsafe.setUWord(base, offset, value);
    }

    /**
     * Sets a 64 bit value in memory.
     *
     * @param base   the base address
     * @param offset the offset (in 64 bit words) from <code>base</code> at which to write
     * @param value  the value to write
     */
    public static void setLong(Object base, int offset, long value) {
        NativeUnsafe.setLong(base, offset, value);
    }

    /**
     * Sets a 64 bit value in memory at a 32 bit word offset.
     *
     * @param base   the base address
     * @param offset the offset (in 32 bit words) from <code>base</code> at which to write
     * @param value  the value to write
     */
    public static void setLongAtWord(Object base, int offset, long value) {
        NativeUnsafe.setLongAtWord(base, offset, value);
    }

    /**
     * Sets a pointer value in memory without updating the write barrier.
     *
     * If this method is being called in a
     * {@link VM#isHosted() hosted} environment then the corresponding bit in the
     * oop map (if any) is also set.
     *
     * @param base   the base address
     * @param offset the offset (in UWords) from <code>base</code> at which to write
     * @param value  the value to write
     */
    public static void setAddress(Object base, int offset, Object value) {
        NativeUnsafe.setAddress(base, offset, value);
    }

    /**
     * Sets a pointer value in memory and updates the write barrier.
     *
     * @param base   the base address
     * @param offset the offset (in UWords) from <code>base</code> at which to write
     * @param value  the value to write
     */
    public static void setObject(Object base, int offset, Object value) {
        NativeUnsafe.setObject(base, offset, value);
    }

    /**
     * Gets a signed 8 bit value from memory.
     *
     * @param base   the base address
     * @param offset the offset (in bytes) from <code>base</code> from which to load
     * @return the value
     */
    public static int getByte(Object base, int offset) {
        return NativeUnsafe.getByte(base, offset);
    }

    /**
     * Gets a signed 16 bit value from memory.
     *
     * @param base   the base address
     * @param offset the offset (in 16 bit words) from <code>base</code> from which to load
     * @return the value
     */
    public static int getShort(Object base, int offset) {
        return NativeUnsafe.getShort(base, offset);
    }

    /**
     * Gets an unsigned 16 bit value from memory.
     *
     * @param base   the base address
     * @param offset the offset (in 16 bit words) from <code>base</code> from which to load
     * @return the value
     */
    public static int getChar(Object base, int offset) {
        return NativeUnsafe.getChar(base, offset);
    }


    /**
     * Gets a signed 32 bit value from memory.
     *
     * @param base   the base address
     * @param offset the offset (in 32 bit words) from <code>base</code> from which to load
     * @return the value
     */
    public static int getInt(Object base, int offset) {
        return NativeUnsafe.getInt(base, offset);
    }

    /**
     * Gets an unsigned 32 or 64 bit value from memory.
     *
     * @param base   the base address
     * @param offset the offset (in UWords) from <code>base</code> from which to load
     * @return the value
     */
    public static UWord getUWord(Object base, int offset) {
        return NativeUnsafe.getUWord(base, offset);
    }

    /**
     * Gets a 64 bit value from memory using a 64 bit word offset.
     *
     * @param base   the base address
     * @param offset the offset (in 64 bit words) from <code>base</code> from which to load
     * @return the value
     */
    public static long getLong(Object base, int offset) {
        return NativeUnsafe.getLong(base, offset);
    }

    /**
     * Gets a 64 bit value from memory using a 32 bit word offset.
     *
     * @param base   the base address
     * @param offset the offset (in 32 bit words) from <code>base</code> from which to load
     * @return the value
     */
    public static long getLongAtWord(Object base, int offset) {
        return NativeUnsafe.getLongAtWord(base, offset);
    }

    /**
     * Gets a pointer from memory as an Object.
     *
     * @param base   the base address
     * @param offset the offset (in UWords) from <code>base</code> from which to load
     * @return the value
     */
    public static Object getObject(Object base, int offset) {
        return NativeUnsafe.getObject(base, offset);
    }

    /**
     * Gets a pointer from memory as an Address.
     *
     * @param base   the base address
     * @param offset the offset (in UWords) from <code>base</code> from which to load
     * @return the value
     */
    public static Address getAddress(Object base, int offset) {
        return NativeUnsafe.getAddress(base, offset);
    }

    /**
     * Gets a UWord value from memory ignoring any recorded type of the value at the designated location.
     * This operation is equivalent to {@link #getUWord(Object, int)} when {@link VM#usingTypeMap() runtime type checking}
     * is disabled.
     *
     * @param base   the base address
     * @param offset the offset (in words) from <code>base</code> from which to load
     * @return the value
     */
    public static UWord getAsUWord(Object base, int offset) {
        return NativeUnsafe.getAsUWord(base, offset);
    }

    /**
     * Gets a signed 8 bit value from memory ignoring any recorded type of the value at the designated location.
     * This operation is equivalent to {@link #getByte(Object, int)} when {@link VM#usingTypeMap() runtime type checking}
     * is disabled.
     *
     * @param base   the base address
     * @param offset the offset (in 8 bit words) from <code>base</code> from which to load
     * @return the value
     */
    public static int getAsByte(Object base, int offset) {
        return NativeUnsafe.getAsByte(base, offset);
    }

    /**
     * Gets a signed 16 bit value from memory ignoring any recorded type of the value at the designated location.
     * This operation is equivalent to {@link #getShort(Object, int)} when {@link VM#usingTypeMap() runtime type checking}
     * is disabled.
     *
     * @param base   the base address
     * @param offset the offset (in 16 bit words) from <code>base</code> from which to load
     * @return the value
     */
    public static int getAsShort(Object base, int offset) {
        return NativeUnsafe.getAsByte(base, offset);
    }
    /**
     * Gets a signed 32 bit value from memory ignoring any recorded type of the value at the designated location.
     * This operation is equivalent to {@link #getInt(Object, int)} when {@link VM#usingTypeMap() runtime type checking}
     * is disabled.
     *
     * @param base   the base address
     * @param offset the offset (in 32 bit words) from <code>base</code> from which to load
     * @return the value
     */
    public static int getAsInt(Object base, int offset) {
        return NativeUnsafe.getAsByte(base, offset);
    }
}
