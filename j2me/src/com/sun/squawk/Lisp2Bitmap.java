/*MAKE_ASSERTIONS_FATAL[true]*/
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
import com.sun.squawk.vm.*;
import com.sun.squawk.pragma.*;

/**
 * This class provides the interface to the bitmap created and used by the {@link Lisp2Collector}
 * as a write barrier and as mark bits for the young generation.
 *
 * @author  Doug Simon
 */
public final class Lisp2Bitmap implements GlobalStaticFields {

    /**
     * The address at which the bitmap starts. The bitmap is used as a
     * write barrier for the old generation as well as a marking bitvector
     * for the region being collected during a collection. As such, it has
     * a bit for every word in the heap.
     */
    private static Address start;

    /**
     * The logical starting address of the bitmap. This is the address at which
     * the bitmap would start if it had a bits for addresses starting from 0. The mutator
     * uses this base when updating the write barrier which removes the need for it
     * to convert the effective address of a pointer to be relative to
     * the start of the heap.
     */
    private static Address base;

    /**
     * This is the size (in bytes) of the bitmap.
     */
    private static int size;

    /**
     * Gets the real start of the bitmap. That is, the part of the bitmap
     * for which real memory has been allocated and whose bits correspond to addresses in the heap.
     *
     * @return the start address of the valid part of the bitmap
     */
    static Address getStart() {
        return start;
    }

    /**
     * Gets the logical start of the bitmap. That is, the address at which the bit for address 0 would be located.
     *
     * @return the logical start of the bitmap
     */
    static Address getBase() {
        return base;
    }

    /**
     * Gets the address of the word one past the end of the bitmap.
     *
     * @return  the address of the word one past the end of the bitmap
     */
    static Address getEnd() {
        return start.add(size);
    }

    /**
     * Gets the size (in bytes) of the bitmap.
     *
     * @return the size (in bytes) of the bitmap
     */
    static int getSize() {
        return size;
    }

    /**
     * Initializes or re-initializes the bitmap.
     *
     * @param start              see {@link #getStart}
     * @param size               the size (in bytes) of the bitmap
     * @param objectMemoryStart  the address at which the object memory to be covered by the bitmap starts
     */
    native static void initialize(Address start, int size, Address objectMemoryStart);

    /*---------------------------------------------------------------------------*\
     *                   Address based bitmap methods                            *
    \*---------------------------------------------------------------------------*/

    /**
     * Clears the bits in the bitmap corresponding to the range of memory <code>[start .. end)</code>.
     *
     * @param start   the start of the memory range for which the bits are to be cleared
     * @param end     the end of the memory range for which the bits are to be cleared
     *
     * @vm2c proxy
     */
    static native void clearBitsFor(Address start, Address end);

    /**
     * Gets the address of the word in the bitmap that contains the bit for a given address.
     *
     * @param ea   the address for which the corresponding bitmap word is required
     * @return     the address of the bitmap word that contains the bit for <code>ea</code>
     *
     * @vm2c proxy
     */
    static native Address getAddressOfBitmapWordFor(Address ea);

    /**
     * Gets the address that corresponds to the first bit in the bitmap word at a given address.
     *
     * @param bitmapWordAddress  the address of a word in the bitmap
     * @return the address corresponding to the first bit in the word at <code>bitmapWordAddress</code>
     *
     * @vm2c proxy
     */
    static native Address getAddressForBitmapWord(Address bitmapWordAddress);

    /**
     * Sets the appropriate bit in the bitmap for a given address.
     *
     * @param ea      the effective address for which the corresponding bit is to be set
     *
     * @vm2c proxy
     */
    static native void setBitFor(Address ea);

    /**
     * Sets the bits in the bitmap corresponding to the range of memory <code>[start .. end)</code>.
     *
     * @param start   the start of the memory range for which the bits are to be set
     * @param end     the end of the memory range for which the bits are to be set
     *
     * @vm2c proxy
     */
    static native void setBitsFor(Address start, Address end);

    /**
     * Clears the appropriate bit in the bitmap for a given address.
     *
     * @param ea      the effective address for which the corresponding bit is to be set
     *
     * @vm2c proxy
     */
    static native void clearBitFor(Address ea);

    /**
     * Determines if the bit in the bitmap for a given address is set.
     *
     * @param ea      the effective address for which the corresponding bit to be tested
     * @return true if the bit for <code>ea</code> is set
     *
     * @vm2c proxy
     */
    static native boolean testBitFor(Address ea);

    /**
     * Determines if the bit in the bitmap for a given address is set and sets it if it isn't.
     *
     * @param ea      the effective address for which the corresponding bit to be tested
     * @return true if the bit for <code>ea</code> was set before this call
     *
     * @vm2c proxy
     */
    static native boolean testAndSetBitFor(Address ea);

    public static void updateWriteBarrierForPointerArraycopy(Object dst, int dstPos, int length) {
        Address start = Address.fromObject(dst).add(dstPos * HDR.BYTES_PER_WORD);
        Address end = start.add(length * HDR.BYTES_PER_WORD);
        setBitsFor(start, end);
    }

    /*---------------------------------------------------------------------------*\
     *                                Iterators                                  *
    \*---------------------------------------------------------------------------*/

    /**
     * Guards use of the iterator to be serialized.
     */
    static boolean Iterator_inUse;

    /**
     * The limit of the current iteration.
     */
    static Address Iterator_end;

    /**
     * The iterator.
     */
    static Address Iterator_next;

    /**
     * Operations for iterating over words in an address range whose corresponding bits in the bitmap are set.
     */
    static class Iterator {

        /**
         * Starts an iteration over all the addresses in a given range whose corresponding bits in the bitmap are set.
         * If <code>isObjectRange</code> is false, the range is <code>[start .. end)</code> otherwise the range is
         * <code>(start .. end]</code>. The latter is used when traversing an object memory where the
         * bits set indicate the start of an object's body within the range (i.e. the value of an oop that references
         * the object). The iteration is complete when {@link #getNext()} returns {@link Address#zero() null} or {@link #terminate} is called.
         * A subsequent iteration cannot be performed until the current iteration is finished.
         *
         * @param start         the address at which to start iterating
         * @param end           the address one past the word at which to stop iterating
         * @param isObjectRange
         */
        static void start(Address start, Address end, boolean isObjectRange) {
            Assert.always(!Iterator_inUse);
            Iterator_inUse = true;
            if (isObjectRange) {
                // Bump up the start and end limits by one word to account for the fact that
                // the body of an object within the range will not lie right at the start of
                // the range and the body of a zero length object can lie right at the end
                // of the range
                Iterator_next = start.add(HDR.BYTES_PER_WORD);
                Iterator_end = end.add(HDR.BYTES_PER_WORD);
            } else {
                Iterator_next = start;
                Iterator_end = end;
            }
        }

        /**
         * Gets the next address whose bit in the bitmap is set.
         *
         * @return  the next address or {@link Address#zero() null} if the iteration is complete
         */
        static Address getNext() {
            Assert.that(Iterator_inUse);
            return iterate();
        }

        /**
         * Terminates the current iteration.
         */
        static void terminate() {
            Assert.that(Iterator_inUse);
            Iterator_inUse = false;
            Iterator_next = Address.zero();
        }
    }

    /**
     * Gets the next value in the iteration. This operation will update the values of {@link #Iterator_next},
     * {@link #Iterator_end} and {@link #Iterator_inUse} so that the iteration progresses or completes.
     *
     * @return   the next value in the iteration or {@link Address#zero()} if the iteration is complete
     *
     * @vm2c proxy( bitmapIterate )
     */
    static native Address iterate();

}
