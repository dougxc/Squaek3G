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
import com.sun.squawk.vm.*;
import com.sun.squawk.util.*;
import com.sun.squawk.util.BitSet;

/**
 * This class provides a mechanism for copying an object graph from the host
 * VM (e.g. Hotspot) into a Squawk memory.
 *
 * @author  Doug Simon
 */
public final class ObjectGraphSerializer {

    /**
     * This maps host objects to their serialized addresses.
     */
    private static final ArrayHashtable objectMap = new ArrayHashtable();

    /**
     * Counts the number of methods in "com.sun.squawk.VM" that are annoted with InterpreterInvokedPragma.
     * These are the methods that the VM needs to know about as they are entry points back into Java code.
     */
    private static int entryPointCounter = 0;

    /*
     * Speeds up Class -> Klass conversion
     */
    private static final Map classKlassMap = new HashMap();

    /**
     * Serializes a graph of host objects into the object memory format used by the Squawk VM.
     *
     * @param object  the root of the object graph to serialize
     * @return the address of the root in the serialized memory
     */
    public static ObjectMemorySerializer.ControlBlock serialize(Object object) {

        // Serialize the graph
        int currentMemorySize = NativeUnsafe.getMemorySize();
        Address start = Address.fromPrimitive(currentMemorySize);

        if (currentMemorySize == 0) {
            // Initialize the allocator
            GC.initialize(VM.getCurrentIsolate().getBootstrapSuite());
        }

        ObjectGraphSerializer serializer = new ObjectGraphSerializer(object);

        ObjectMemorySerializer.ControlBlock cb = new ObjectMemorySerializer.ControlBlock();
        cb.root = ((Address)serializer.objectMap.get(object)).diff(start).toInt();

        int size = NativeUnsafe.getMemorySize() - currentMemorySize;
        cb.start = start;
        cb.memory = new byte[size];
        NativeUnsafe.copyMemory(cb.memory, currentMemorySize, 0, size);

        cb.oopMap = new BitSet();
        cb.oopMap.or(NativeUnsafe.getOopMap(), -currentMemorySize / HDR.BYTES_PER_WORD);

        return cb;
    }

    /**
     * Creates an ObjectGraphSerializer instance and serializes a given object graph.
     *
     * @param object  the root of the object graph to serialize
     */
    private ObjectGraphSerializer(Object object) {

        // Now serialize the given graph
        save(object);

        // Fix up the class pointers of the objects in the Squawk memory
        NativeUnsafe.resolveClasses(objectMap);
    }

    /**
     * Trace the allocation of a serialized object if tracing of the image building is enabled.
     *
     * @param klass             the class of the object
     * @param object            the host object
     * @param serializedObject  the serialized copy of <code>object</code>
     */
    private void traceAllocation(Klass klass, Object object, Address serializedObject) {
        if (Tracer.isTracing("image")) {
            Address block = GC.oopToBlock(klass, serializedObject);
            int bodySize = GC.getBodySize(klass, serializedObject);
            Tracer.traceln("[allocated " + klass.getName() + " instance @ " + serializedObject +
                           " (block=" + block + ", body size=" + bodySize + ")" + " (object.toString()=\"" + object + "\")]");

        }
    }

    /**
     * Guards against re-entry to the alloc method.
     */
    private boolean inAlloc = false;

    /**
     * Allocates a copy of a host object in the Squawk memory. The copy allocated
     * will have the same size and type as <code>object</code> and all of its fields
     * or components will be initialized to their default values.<p>
     *
     * There must be no object in the Squawk memory {@link Arrays#equals(Object) equal} to
     * <code>object</code> prior to this call.
     *
     * @param   object  the host object for which a Squawk copy is to be allocated
     * @param   klass   the class of <code>object</code>
     * @param   length  the number of elements in the array being allocated or -1 if a non-array object is being allocated
     * @return  the copy allocated in Squawk memory
     */
    private Object alloc(Object object, Klass klass, int length) {
        Assert.that(!inAlloc);
        inAlloc = true;

        Object serializedObject;

        /*
         * Allocate the Squawk object
         */
        if (klass.isArray()) {
            serializedObject = GC.newArray(klass, length);
        } else if (klass == Klass.STRING) {
            serializedObject = GC.newArray(Klass.CHAR_ARRAY, length);
            GC.setHeaderClass((Address)serializedObject, Klass.STRING);
        } else if (klass == Klass.STRING_OF_BYTES) {
            serializedObject = GC.newArray(Klass.BYTE_ARRAY, length);
            GC.setHeaderClass((Address)serializedObject, Klass.STRING_OF_BYTES);
        } else if (object instanceof MethodBody) {
            MethodBody mbody = (MethodBody)object;
            Klass definingClass = mbody.getDefiningClass();
            serializedObject = GC.newMethod(definingClass, mbody);

            /*
             * Write special symbol table entries for all the methods in java.lang.VM
             * whose names start "do_"
             */
            if (definingClass.getName().equals("com.sun.squawk.VM")) {
                String methodName = mbody.getDefiningMethod().getName();
                if (mbody.getDefiningMethod().isInterpreterInvoked()) {
                    int old = VM.setStream(VM.STREAM_SYMBOLS);
                    VM.println("ENTRYPOINT."+entryPointCounter+".NAME=com_sun_squawk_VM_"+methodName);
                    VM.println("ENTRYPOINT."+entryPointCounter+".ADDRESS="+serializedObject);
                    entryPointCounter++;
                    VM.setStream(old);
                }
            }
        } else {
            serializedObject = GC.newInstance(klass);
        }

        // Add the mapping between the host object and its serialized copy
        Object previous = objectMap.put(object, serializedObject);
        Assert.that(previous == null);

        traceAllocation(klass, object, Address.fromObject(serializedObject));

        inAlloc = false;
        return serializedObject;
    }

    /**
     * Determines if all the elements in a specified character array can be
     * encoded in 8 bits.
     *
     * @param   chars  the character array to check
     * Return   true if all the characters in <code>chars</code> can be
     *                 encoded in 8 bits
     */
    private boolean isEightBitEnc(char[] chars) {
        int length = chars.length;
        for (int i = 0; i < length; i++) {
            if (chars[i] > 0xFF) {
                return false;
            }
        }
        return true;
    }

    /**
     * Copies an object graph from the host memory to the Squawk memory.
     *
     * @param   object  the root of the host object graph to be copied
     * @return  the copy of <code>object</code> in Squawk memory
     */
    private Object save(Object object) {
        return save(object, classToKlass(object.getClass()));
    }

    /**
     * Copies an object graph from the host memory to the Squawk memory.
     *
     * @param   object  the root of the host object graph to be copied
     * @param   klass   the class of <code>object</code>
     * @return  the copy of <code>object</code> in Squawk memory
     */
    private Object save(Object object, Klass klass) {
        Object serializedObject = objectMap.get(object);
        if (serializedObject == null) {
            /*
             * Initialize the fields
             */
            if (klass.isSquawkArray()) {
                switch (klass.getSystemID()) {
                    case CID.BOOLEAN_ARRAY: {
                        boolean[] array = (boolean[])object;
                        serializedObject = alloc(object, klass, array.length);
                        for (int i = 0 ; i < array.length ; i++) {
                            NativeUnsafe.setByte(serializedObject, i, array[i] ? 1 : 0);
                        }
                        break;
                    }
                    case CID.BYTE_ARRAY: {
                        byte[] array = (byte[])object;
                        serializedObject = alloc(object, klass, array.length);
                        for (int i = 0 ; i < array.length ; i++) {
                            NativeUnsafe.setByte(serializedObject, i, array[i]);
                        }
                        break;
                    }
                    case CID.SHORT_ARRAY: {
                        short[] array = (short[])object;
                        serializedObject = alloc(object, klass, array.length);
                        for (int i = 0 ; i < array.length ; i++) {
                            NativeUnsafe.setShort(serializedObject, i, array[i]);
                        }
                        break;
                    }
                    case CID.CHAR_ARRAY: {
                        char[] array = (char[])object;
                        serializedObject = alloc(object, klass, array.length);
                        for (int i = 0 ; i < array.length ; i++) {
                            NativeUnsafe.setChar(serializedObject, i, array[i]);
                        }
                        break;
                    }
                    case CID.STRING_OF_BYTES: {
                        char[] value = ((String)object).toCharArray();
                        serializedObject = alloc(object, Klass.STRING_OF_BYTES, value.length);
                        for (int i = 0; i != value.length; ++i) {
                            NativeUnsafe.setByte(serializedObject, i, (byte)value[i]);
                        }
                        break;
                    }
                    case CID.STRING: {
                        String str = (String)object;
                        char[] value = str.toCharArray();
                        if (isEightBitEnc(value)) { // Will only occur during romizing
                            boolean done = false;
                            while (!done) {
                                done = true;
                                String buildProp = "${build.properties:";
                                int start = str.indexOf(buildProp);
                                if (start >= 0) {

                                    String head = str.substring(0, start);
                                    String rest = str.substring(start);
                                    int end = rest.indexOf('}');
                                    Assert.that(end > 0);
                                    String symbol = rest.substring(buildProp.length(), end);
                                    String tail   = rest.substring(end+1);


                                    Object prop = Romizer.buildProperties.get(symbol);
                                    if (prop == null) {
                                         throw new RuntimeException("Cannot find build property: "+symbol);
                                    }
                                    str = head+prop+tail;
                                    done = false;
//System.out.println("str="+str);
//System.out.println("head="+head);
//System.out.println("rest="+rest);
//System.out.println("symbol="+symbol);
//System.out.println("tail="+tail);
//System.out.println(str+"->"+object);
                                }
                                object = str;
                            }
                            return save(object, Klass.STRING_OF_BYTES);
                        }
                        serializedObject = alloc(object, Klass.STRING, value.length);
                        for (int i = 0; i != value.length; ++i) {
                            NativeUnsafe.setChar(serializedObject, i, value[i]);
                        }
                        break;
                    }
                    case CID.INT_ARRAY: {
                        int[] array = (int[])object;
                        serializedObject = alloc(object, klass, array.length);
                        for (int i = 0 ; i < array.length ; i++) {
                            NativeUnsafe.setInt(serializedObject, i, array[i]);
                        }
                        break;
                    }
                    case CID.UWORD_ARRAY: {
                        UWord[] array = (UWord[])object;
                        serializedObject = alloc(object, klass, array.length);
                        for (int i = 0 ; i < array.length ; i++) {
                            NativeUnsafe.setUWord(serializedObject, i, array[i]);
                        }
                        break;
                    }
                    case CID.FLOAT_ARRAY: {
                        float[] array = (float[])object;
                        serializedObject = alloc(object, klass, array.length);
                        for (int i = 0 ; i < array.length ; i++) {
                            NativeUnsafe.setInt(serializedObject, i, Float.floatToIntBits(array[i]));
                        }
                        break;
                    }
                    case CID.LONG_ARRAY: {
                        long[] array = (long[])object;
                        serializedObject = alloc(object, klass, array.length);
                        for (int i = 0 ; i < array.length ; i++) {
                            NativeUnsafe.setLong(serializedObject, i, array[i]);
                        }
                        break;
                    }
                    case CID.DOUBLE_ARRAY: {
                        double[] array = (double[])object;
                        serializedObject = alloc(object, klass, array.length);
                        for (int i = 0 ; i < array.length ; i++) {
                            NativeUnsafe.setLong(serializedObject, i, Double.doubleToLongBits(array[i]));
                        }
                        break;
                    }
                    case CID.GLOBAL_ARRAY:
                    case CID.LOCAL_ARRAY: {
                        Assert.shouldNotReachHere();
                        break;
                    }
                    default: {
                        Assert.that(Klass.OBJECT_ARRAY.isAssignableFrom(klass));
                        Object[] array = (Object[])object;
                        serializedObject = alloc(object, klass, array.length);
                        for (int i = 0 ; i < array.length ; i++) {
                            Object value = array[i];
                            Object serializedValue = Address.zero();
                            if (value != null) {
                                serializedValue = save(value);
                            }
                            NativeUnsafe.setObject(serializedObject, i, serializedValue);
                        }
                        break;
                    }
                }
            } else {

                /*
                 * Allocate the object
                 */
                serializedObject = alloc(object, klass, -1);
                if (!(object instanceof MethodBody)) {
                    saveFields(object, serializedObject);
                }
            }
        }
        return serializedObject;
    }

    /**
     * Copies all the fields of an object in the host memory to the fields of
     * the corresponding object in Squawk memory.<p>
     *
     * @param   object  the host object whose fields are to be copied
     * @param   serializedObject the object in Squawk memory corresponding to <code>object</code>
     */
    void saveFields(Object object, Object serializedObject) {
        Klass klass = classToKlass(object.getClass());
        while (klass != Klass.OBJECT) {
            saveDeclaredFields(object, serializedObject, klass);
            klass = klass.getSuperclass();
        }
    }

    /**
     * Copies the fields of a single class in an object's type hierarchy.
     *
     * @param object            the host object whose fields are to be copied
     * @param serializedObject  the object in Squawk memory corresponding to <code>object</code>
     * @param klass             a class in the objects type hierarchy
     */
    private void saveDeclaredFields(Object object, Object serializedObject, Klass klass) {
        int count = klass.getFieldCount(false);
        for (int i = 0 ; i < count ; i++) {
            Field field = klass.getField(i, false);
            Klass type = field.getType();
            switch (type.getSystemID()) {
                case CID.BOOLEAN:
                case CID.BYTE: {
                    int value = FieldReflector.getByte(object, field);
                    NativeUnsafe.setByte(serializedObject, field.getOffset(), value);
                    break;
                }
                case CID.SHORT: {
                    int value = FieldReflector.getShort(object, field);
                    NativeUnsafe.setShort(serializedObject, field.getOffset(), value);
                    break;
                }
                case CID.CHAR: {
                    int value = FieldReflector.getChar(object, field);
                    NativeUnsafe.setChar(serializedObject, field.getOffset(), value);
                    break;
                }
                case CID.FLOAT:
                case CID.INT: {
                    int value = FieldReflector.getInt(object, field);
                    NativeUnsafe.setInt(serializedObject, field.getOffset(), value);
                    break;
                }
                case CID.DOUBLE:
                case CID.LONG: {
                    long value = FieldReflector.getLong(object, field);
                    NativeUnsafe.setLongAtWord(serializedObject, field.getOffset(), value);
                    break;
                }
                case CID.UWORD: {
                    UWord value = FieldReflector.getUWord(object, field);
                    NativeUnsafe.setUWord(serializedObject, field.getOffset(), value);
                    break;
                }
                default: {
                    Object value = FieldReflector.getObject(object, field);
                    Object serializedValue = Address.zero();

                    if (value != null) {
                        serializedValue = save(value);
                    }
                    NativeUnsafe.setObject(serializedObject, field.getOffset(), serializedValue);
                    break;
                }
            }
        }
    }

    /**
     * Get the Squawk class name corresponding to a standard Java Class name.
     *
     * @param  c  a Class instance
     * @return the name of <code>c</code> in Squawk format
     */
    private String getKlassName(Class c) {
        if (c.isArray()) {
            return "[" + getKlassName(c.getComponentType());
        } else {
            return c.getName();
        }
    }

    /**
     * Gets the Klass instance corresponding to a Class instance.
     *
     * @param   cls  the Class instance to convert
     * @return  the  Klass instance corresponding to <code>cls</code>
     */
    private Klass classToKlass(Object cls) {
        Class c = (Class)cls;
        Klass klass = (Klass)classKlassMap.get(c);
        if (klass == null) {
            String name = getKlassName(c);
            /*
             * Convert to Squawk name
             */
            Suite suite = VM.getCurrentIsolate().getLeafSuite();
            klass = suite.lookup(name);
            Assert.that(klass != null, "Lookup failure for class "+name);
            classKlassMap.put(c, klass);
        }
        return klass;
    }
}
