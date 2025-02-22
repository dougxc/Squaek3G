/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk;

import java.util.*;

import com.sun.squawk.pragma.*;
import com.sun.squawk.util.*;
import com.sun.squawk.util.BitSet;
import com.sun.squawk.util.SquawkHashtable;
import com.sun.squawk.vm.*;


/**
 * A collection of methods for performing peek and poke operations on
 * memory addresses.
 * <p>
 * Only the public methods of this class which do not override any of the
 * methods in java.lang.Object will be available in a {@link VM#isHosted() non-hosted}
 * environment. The translator replaces any calls to these methods to native
 * method calls.
 *
 * @author  Nik Shaylor, Doug Simon
 */
public final class NativeUnsafe {

    private NativeUnsafe() {}

    /*-----------------------------------------------------------------------*\
     *                      Storing to/loading from memory                   *
    \*-----------------------------------------------------------------------*/

    /**
     * @see Unsafe#setByte
     *
     * @vm2c proxy
     */
     public static void setByte(Object base, int offset, int value) throws NativePragma {
         int index = ((Address)base).add(offset).asIndex();
         checkAddress(index);
         memory[index] = (byte)(value>>0);
         setType0(index, AddressType.BYTE);
     }

     /**
      * @see Unsafe#setShort
      *
      * @vm2c proxy
      */
    public static void setShort(Object base, int offset, int value) throws NativePragma {
        setChar(base, offset, value);
    }

    /**
     * @see Unsafe#setChar
     *
     * @vm2c proxy
     */
    public static void setChar(Object base, int offset, int value) throws NativePragma {
        int index = ((Address)base).add(offset * 2).asIndex();
        checkAddress(index + 1);
        if (VM.isBigEndian()) {
            memory[index+0] = (byte)(value>>8);
            memory[index+1] = (byte)(value>>0);
        } else {
            memory[index+0] = (byte)(value>>0);
            memory[index+1] = (byte)(value>>8);
        }
        setType0(index, AddressType.SHORT);
    }

    /**
     * @see Unsafe#setInt
     *
     * @vm2c proxy
     */
    public static void setInt(Object base, int offset, int value) throws NativePragma {
        int index = ((Address)base).add(offset * 4).asIndex();
        checkAddress(index + 3);
        if (VM.isBigEndian()) {
            memory[index + 0] = (byte) (value >> 24);
            memory[index + 1] = (byte) (value >> 16);
            memory[index + 2] = (byte) (value >> 8);
            memory[index + 3] = (byte) (value >> 0);
        }
        else {
            memory[index + 0] = (byte) (value >> 0);
            memory[index + 1] = (byte) (value >> 8);
            memory[index + 2] = (byte) (value >> 16);
            memory[index + 3] = (byte) (value >> 24);
        }
        setType0(index, AddressType.INT);
    }

    /**
     * @see Unsafe#setUWord
     *
     * @vm2c proxy
     */
    public static void setUWord(Object base, int offset, UWord value) throws NativePragma {
        setInt/*S64*/(base, offset, value.toPrimitive());
        int index = ((Address)base).add(offset * HDR.BYTES_PER_WORD).asIndex();
        setType0(index, AddressType.UWORD);
    }

    /**
     * @see Unsafe#setLong
     *
     * @vm2c proxy
     */
    public static void setLong(Object base, int offset, long value) throws NativePragma {
        int index = ((Address)base).add(offset * 8).asIndex();
        checkAddress(index + 7);
        if (VM.isBigEndian()) {
            memory[index+0] = (byte)(value>>56);
            memory[index+1] = (byte)(value>>48);
            memory[index+2] = (byte)(value>>40);
            memory[index+3] = (byte)(value>>32);
            memory[index+4] = (byte)(value>>24);
            memory[index+5] = (byte)(value>>16);
            memory[index+6] = (byte)(value>>8);
            memory[index+7] = (byte)(value>>0);
        } else {
            memory[index+0] = (byte)(value>>0);
            memory[index+1] = (byte)(value>>8);
            memory[index+2] = (byte)(value>>16);
            memory[index+3] = (byte)(value>>24);
            memory[index+4] = (byte)(value>>32);
            memory[index+5] = (byte)(value>>40);
            memory[index+6] = (byte)(value>>48);
            memory[index+7] = (byte)(value>>56);
        }
        setType0(index, AddressType.LONG);
    }

    /**
     * @see Unsafe#setLongAtWord
     *
     * @vm2c proxy
     */
    public static void setLongAtWord(Object base, int offset, long value) throws NativePragma {
        Address ea = ((Address)base).add(offset * HDR.BYTES_PER_WORD);
        setLong(ea, 0, value);
        setType0(ea.asIndex(), AddressType.LONG);
    }

    /**
     * @see Unsafe#setAddress
     *
     * @vm2c proxy( setObject )
     */
    public static void setAddress(Object base, int offset, Object value) throws NativePragma {
        Address ea = ((Address)base).add(offset * HDR.BYTES_PER_WORD);
        if (value instanceof Klass) {
            unresolvedClassPointers.put(ea, value);
            setUWord(ea, 0, UWord.zero());
        } else {
            Assert.that(value instanceof Address);
            unresolvedClassPointers.remove(ea);
            setUWord(ea, 0, ((Address)value).toUWord());
        }
        oopMap.set(ea.asIndex() / HDR.BYTES_PER_WORD);
        setType0(ea.asIndex(), AddressType.REF);
    }

    /**
     * @see Unsafe#setObject
     *
     * @vm2c proxy( setObjectAndUpdateWriteBarrier )
     */
    public static void setObject(Object base, int offset, Object value) throws NativePragma {
        setAddress(base, offset, value);
    }

    private static void setType0(int index, byte type) {
/*if[TYPEMAP]*/
        typeMap[index] = type;
/*end[TYPEMAP]*/
    }

    /**
     * Sets the type of a value at a given address.
     *
     * This operation is a nop when {@link VM#usingTypeMap()} returns false.
     *
     * @param ea   the address of the value
     * @param type the type of the value
     * @param size the size (in bytes) of the value
     */
    public static void setType(Address ea, byte type, int size) throws NativePragma {
/*if[TYPEMAP]*/
        setType0(ea.asIndex(), type);
/*end[TYPEMAP]*/
    }

    /**
     * Sets the type of each value in an array.
     *
     * This operation is a nop when {@link VM#usingTypeMap()} returns false.
     *
     * @param ea            the address of an array
     * @param componentType the component type of the array
     * @param componentSize the size (in bytes) of <code>componentType</code>
     * @param length        the length of the array
     */
    public static void setArrayTypes(Address ea, byte componentType, int componentSize, int length) throws NativePragma {
/*if[TYPEMAP]*/
        for (int i = 0; i != length; ++i) {
            setType0(ea.asIndex(), componentType);
            ea = ea.add(componentSize);
        }
/*end[TYPEMAP]*/
    }

    /**
     * Gets the type of a value at a given address.
     *
     * This operation is a nop when {@link VM#usingTypeMap()} returns false.
     *
     * @param ea   the address to query
     * @return the type of the value at <code>ea</code>
     *
     * @vm2c proxy
     */
    public static byte getType(Address ea) throws NativePragma {
/*if[TYPEMAP]*/
        return typeMap[ea.asIndex()];
/*else[TYPEMAP]*/
//      throw Assert.shouldNotReachHere();
/*end[TYPEMAP]*/
    }

    /**
     * Block copies the types recorded for a range of memory to another range of memory.
     *
     * @param src    the start address of the source range
     * @param dst    the start address of the destination range
     * @param length the length (in bytes) of the range
     *
     * @vm2c proxy
     */
    public static void copyTypes(Address src, Address dst, int length) throws NativePragma {
/*if[TYPEMAP]*/
        System.arraycopy(typeMap, src.asIndex(), typeMap, dst.asIndex(), length);
/*end[TYPEMAP]*/
    }

    /**
     * @see Unsafe#getByte
     *
     * @vm2c proxy
     */
    public static int getByte(Object base, int offset) throws NativePragma {
        int index = ((Address)base).add(offset).asIndex();
        checkAddress(index);
        return memory[index];
    }

    /**
     * @see Unsafe#getShort
     *
     * @vm2c proxy
     */
    public static int getShort(Object base, int offset) throws NativePragma {
        return (short)getChar(base, offset);
    }

    /**
     * @see Unsafe#getChar
     *
     * @vm2c proxy( getUShort )
     */
    public static int getChar(Object base, int offset) throws NativePragma {
        int index = ((Address)base).add(offset * 2).asIndex();
        checkAddress(index + 1);
        int b0 = memory[index] & 0xFF;
        int b1 = memory[index + 1] & 0xFF;
        if (VM.isBigEndian()) {
            return b0 << 8 | b1;
        } else {
            return b1 << 8 | b0;
        }
    }


    /**
     * @see Unsafe#getInt
     *
     * @vm2c proxy
     */
    public static int getInt(Object base, int offset) throws NativePragma {
        int index = ((Address)base).add(offset * 4).asIndex();
        checkAddress(index + 3);
        int b0 = memory[index + 0] & 0xFF;
        int b1 = memory[index + 1] & 0xFF;
        int b2 = memory[index + 2] & 0xFF;
        int b3 = memory[index + 3] & 0xFF;
        if (VM.isBigEndian()) {
            return (b0<<24) | (b1<<16) | (b2<<8) | b3;
        } else {
            return (b3<<24) | (b2<<16) | (b1<<8) | b0;
        }
    }

    /**
     * @see Unsafe#getUWord
     *
     * @vm2c proxy
     */
    public static UWord getUWord(Object base, int offset) throws NativePragma {
        return UWord.fromPrimitive(getInt/*S64*/(base, offset));
    }

    /**
     * @see Unsafe#getLong
     *
     * @vm2c proxy
     */
    public static long getLong(Object base, int offset) throws NativePragma {
        int index = ((Address)base).add(offset * 8).asIndex();
        checkAddress(index + 7);
        long b0 = memory[index + 0] & 0xFF;
        long b1 = memory[index + 1] & 0xFF;
        long b2 = memory[index + 2] & 0xFF;
        long b3 = memory[index + 3] & 0xFF;
        long b4 = memory[index + 4] & 0xFF;
        long b5 = memory[index + 5] & 0xFF;
        long b6 = memory[index + 6] & 0xFF;
        long b7 = memory[index + 7] & 0xFF;
        if (VM.isBigEndian()) {
            return (b0<<56) | (b1<<48) | (b2<<40) | (b3<<32) | (b4<<24) | (b5<<16) | (b6<<8) | b7;
        } else {
            return (b7<<56) | (b6<<48) | (b5<<40) | (b4<<32) | (b3<<24) | (b2<<16) | (b1<<8) | b0;
        }
    }

    /**
     * @see Unsafe#getLongAtWord
     *
     * @vm2c proxy
     */
    public static long getLongAtWord(Object base, int offset) throws NativePragma {
        return getLong(((Address)base).add(offset * HDR.BYTES_PER_WORD), 0);
    }

    /**
     * @see Unsafe#getObject
     *
     * @vm2c proxy
     */
    public static Object getObject(Object base, int offset) throws NativePragma {
        return Address.get(getUWord(base, offset).toPrimitive());
    }

    /**
     * @see Unsafe#getAddress
     *
     * @vm2c proxy( getObject )
     */
    public static Address getAddress(Object base, int offset) throws NativePragma {
        return Address.fromObject(getObject(base, offset));
    }

    /**
     * @see Unsafe#getAsUWord
     *
     * @vm2c code( return getUWordTyped(base, offset, AddressType_ANY); )
     */
    public static UWord getAsUWord(Object base, int offset) throws NativePragma {
        return getUWord(base, offset);
    }

    /**
     * @see Unsafe#getAsByte
     *
     * @vm2c code( return getByteTyped(base, offset, AddressType_ANY); )
     */
    public static int getAsByte(Object base, int offset) throws NativePragma {
        return getByte(base, offset);
    }

    /**
     * @see Unsafe#getAsShort
     *
     * @vm2c code( return getShortTyped(base, offset, AddressType_ANY); )
     */
    public static int getAsShort(Object base, int offset) throws NativePragma {
        return getShort(base, offset);
    }

    /**
     * @see Unsafe#getAsInt
     *
     * @vm2c code( return getIntTyped(base, offset, AddressType_ANY); )
     */
    public static int getAsInt(Object base, int offset) throws NativePragma {
        return getInt(base, offset);
    }

    /**
     * Gets character from a string.
     *
     * @param str   the string
     * @param index the index to the character
     * @return the value
     *
     * @vm2c code( Address cls = com_sun_squawk_Klass_self(getObject(str, HDR_klass));
     *             if (com_sun_squawk_Klass_id(cls) == com_sun_squawk_StringOfBytes) {
     *                 return getByte(str, index) & 0xFF;
     *             } else {
     *                 return getUShort(str, index);
     *             } )
     */
    public static char charAt(String str, int index) throws NativePragma {
        return str.charAt(index);
    }

    /*-----------------------------------------------------------------------*\
     *                      Endianess swapping                               *
    \*-----------------------------------------------------------------------*/

    /**
     * Swaps the endianess of a value.
     *
     * @param address   the address of the value
     * @param dataSize  the size (in bytes) of the value
     */
    public static void swap(Address address, int dataSize) throws NativePragma {
        switch (dataSize) {
            case 1:              break;
            case 2: swap2(address); break;
            case 4: swap4(address); break;
            case 8: swap8(address); break;
            default: Assert.shouldNotReachHere();
        }
    }

    /**
     * Swaps the endianess of a 2 byte value.
     *
     * @param address   the address of the value
     */
    public static void swap2(Address address) throws NativePragma {
/*if[TYPEMAP]*/
        byte type = NativeUnsafe.getType(address);
        NativeUnsafe.setType(address, AddressType.ANY, 2);
/*end[TYPEMAP]*/

        int val = NativeUnsafe.getChar(address, 0);

        int b0 = val        & 0xFF;
        int b1 = (val >> 8) & 0xFF;

        int newVal = (b0 << 8) | b1;

        NativeUnsafe.setChar(address, 0, newVal);

/*if[TYPEMAP]*/
        NativeUnsafe.setType(address, type , 2);
/*end[TYPEMAP]*/
    }

    /**
     * Swaps the endianess of a 4 byte value.
     *
     * @param address   the address of the value
     */
    public static void swap4(Address address) throws NativePragma {
/*if[TYPEMAP]*/
        byte type = NativeUnsafe.getType(address);
        NativeUnsafe.setType(address,AddressType.ANY, 4);
/*end[TYPEMAP]*/

        int val = NativeUnsafe.getInt(address, 0);

        int b0 = val         & 0xFF;
        int b1 = (val >> 8)  & 0xFF;
        int b2 = (val >> 16) & 0xFF;
        int b3 = (val >> 24) & 0xFF;

        int newVal = (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;

        NativeUnsafe.setInt(address, 0, newVal);

/*if[TYPEMAP]*/
        NativeUnsafe.setType(address, type , 4);
/*end[TYPEMAP]*/
    }

    /**
     * Swaps the endianess of a 8 byte value.
     *
     * @param address   the address of the value
     */
    public static void swap8(Address address) throws NativePragma {
/*if[TYPEMAP]*/
        byte type = NativeUnsafe.getType(address);
        NativeUnsafe.setType(address, AddressType.ANY, 8);
/*end[TYPEMAP]*/

        long val = NativeUnsafe.getLong(address, 0);

        long b0 = val         & 0xFF;
        long b1 = (val >> 8)  & 0xFF;
        long b2 = (val >> 16) & 0xFF;
        long b3 = (val >> 24) & 0xFF;
        long b4 = (val >> 32) & 0xFF;
        long b5 = (val >> 40) & 0xFF;
        long b6 = (val >> 48) & 0xFF;
        long b7 = (val >> 56) & 0xFF;

        long newVal = (b0 << 56) | (b1 << 48) | (b2 << 40) | (b3 << 32) | (b4 << 24) | (b5 << 16) | (b6 << 8) | b7;

        NativeUnsafe.setLong(address, 0, newVal);

/*if[TYPEMAP]*/
        NativeUnsafe.setType(address, type , 8);
/*end[TYPEMAP]*/
    }

    /*-----------------------------------------------------------------------*\
     *                      Hosted execution support                         *
    \*-----------------------------------------------------------------------*/

    /**
     * A table of all the addresses that hold a pointer to a class which has
     * not yet been written to memory.
     */
    private static SquawkHashtable unresolvedClassPointers = new SquawkHashtable();

    /**
     * Resolve all the deferred writes of unresolved class pointers.
     *
     * @param classMap a map from JVM objects to their addresses in the image. This
     *                 is used to patch up class pointers in objects that were
     *                 written to the image before their classes were.
     */
    static void resolveClasses(ArrayHashtable classMap) throws HostedPragma {
        Enumeration keys = unresolvedClassPointers.keys();
        Enumeration values = unresolvedClassPointers.elements();
        while (keys.hasMoreElements()) {
            Address address = (Address)keys.nextElement();
            Klass unresolvedClass = (Klass)values.nextElement();
            Address klassAddress = (Address)classMap.get(unresolvedClass);
            setAddress(address, 0, klassAddress);
        }
        unresolvedClassPointers.clear();
    }

    /**
     * Clears a pointer value in memory.
     *
     * @param base   the base address
     * @param offset the offset (in UWords) from <code>base</code> of the pointer to clear
     */
    public static void clearObject(Object base, int offset) throws HostedPragma {
        Address ea = ((Address)base).add(offset * HDR.BYTES_PER_WORD);
        setUWord(ea, 0, UWord.zero());
        unresolvedClassPointers.remove(ea);
        oopMap.clear(ea.asIndex() / HDR.BYTES_PER_WORD);
        setType0(ea.asIndex(), AddressType.UNDEFINED);
    }

    /*-----------------------------------------------------------------------*\
     *                      Memory model and initialization                  *
    \*-----------------------------------------------------------------------*/

    /**
     * The memory model.
     */
    private static byte[] memory = {};

/*if[TYPEMAP]*/
    /**
     * The type checking map for memory.
     */
    private static byte[] typeMap = {};
/*end[TYPEMAP]*/

    /**
     * The used amount of memory.
     */
    private static int memorySize = 0;

    /**
     * The oop map describing where the pointers in memory are.
     */
    private static final BitSet oopMap = new BitSet();

    /**
     * Verifies that a given address is within range of the currently allocated
     * memory.
     *
     * @param address  the address to check
     * @throws IndexOfOutBoundsException if the address is out of bounds
     */
    private static void checkAddress(int address) throws IndexOutOfBoundsException, HostedPragma {
        if (address < 0 || address >= memorySize) {
            throw new IndexOutOfBoundsException("address is out of range: " + address);
        }
    }

    /**
     * Ensures that the underlying buffer representing memory is at least a given size.
     *
     * @param size  the minimum size the memory buffer will be upon returning
     */
    private static void ensureCapacity(int size) throws HostedPragma {
        size = GC.roundUpToWord(size);
        if (memory.length < size) {
//System.err.println("growing memory: " + memory.length + " -> " + size*2);
            byte[] newMemory = new byte[size * 2];
            System.arraycopy(memory, 0, newMemory, 0, memory.length);
            memory = newMemory;
/*if[TYPEMAP]*/
            byte[] newTypeMap = new byte[memory.length];
            System.arraycopy(typeMap, 0, newTypeMap, 0, typeMap.length);
            typeMap = newTypeMap;
/*end[TYPEMAP]*/
        }
    }

    /**
     * Initialize or appends to the contents of memory.
     *
     * @param buffer  a buffer containing a serialized object memory relative to 0
     * @param oopMap  an oop map specifying where the pointers in the serialized object memory are
     * @param append  specifies if the memory is being appended to
     */
    public static void initialize(byte[] buffer, BitSet oopMap, boolean append) throws HostedPragma {
        if (!append) {
            setMemorySize(buffer.length);
            System.arraycopy(buffer, 0, memory, 0, buffer.length);

            // Set up the oop map
            NativeUnsafe.oopMap.or(oopMap);
        } else {
            int canonicalStart = memorySize;
            setMemorySize(memorySize + buffer.length);
            System.arraycopy(buffer, 0, memory, canonicalStart, buffer.length);

            // OR the given oop map onto the logical end of the existing oop map
            int shift = canonicalStart / HDR.BYTES_PER_WORD;
            NativeUnsafe.oopMap.or(oopMap, shift);
        }
    }

    /**
     * Sets the size of used/initialized memory. If the new size is less than the current size, all
     * memory locations at index <code>newSize</code> and greater are zeroed.
     *
     * @param   newSize   the new size of memory
     */
    public static void setMemorySize(int newSize) throws HostedPragma {
        Assert.always(newSize >= 0);
        if (newSize > memorySize) {
            ensureCapacity(newSize);
        } else {
            for (int i = newSize ; i < memory.length ; i++) {
                memory[i] = 0;
            }
        }
        memorySize = newSize;
    }

    /**
     * Gets the amount of used/initialized memory.
     *
     * @return the amount of used/initialized memory
     */
    static int getMemorySize() throws HostedPragma {
        return memorySize;
    }

    /**
     * Determines if the word at a given address is a reference. A word is a reference if
     * the last update at the address was via {@link #setObject(Object,int,Object)}.
     *
     * @param address  the address to test
     * @return true if <code>address</code> is a reference
     */
    static boolean isReference(Address address) throws HostedPragma {
        return (address.asIndex() % HDR.BYTES_PER_WORD) == 0 && oopMap.get(address.asIndex() / HDR.BYTES_PER_WORD);
    }

    /**
     * Copies a range of memory into a buffer.
     *
     * @param buffer        the buffer to copy into
     * @param memoryOffset  the offset in memory at which to start copying from
     * @param bufferOffset  the offset in <code>buffer</code> at which to start copying to
     * @param               length the number of bytes to copy
     */
    public static void copyMemory(byte[] buffer, int memoryOffset, int bufferOffset, int length) throws HostedPragma {
        System.arraycopy(memory, memoryOffset, buffer, bufferOffset, length);
    }

    /**
     * Gets the oop map that describes where all the pointers in the memory are.
     *
     * @return the oop map that describes where all the pointers in the memory are
     */
    static BitSet getOopMap() throws HostedPragma {
        return oopMap;
    }
}
