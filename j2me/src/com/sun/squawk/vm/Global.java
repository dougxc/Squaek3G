/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk.vm;

import java.util.Hashtable;

/**
 * Definition of all the Squawk classes that use global variables.
 * <p>
 * The Squawk VM supports four types of variables. These are local
 * variables, instance variables, static varibles, and global variables.
 * Static variables are those defined in a class using the static keyword
 * these are allocated dynamically by the VM when their classes are
 * initialized, and these variables are created on a per-isolate basis.
 * Global variables are allocated by the romizer and used in place of
 * the static variables in this hard-wired set of system classes. This is
 * done in cases where certain components of the system must have static
 * state before the normal system that support things like static variables
 * are running. Global variables are shared between all isolates.
 *
 * @author  Nik Shaylor
 */
public final class Global {

    /**
     * The tables of int, addresss, and reference globals.
     */
    private static Hashtable intGlobals  = new Hashtable(),
                                   addrGlobals = new Hashtable(),
                                   oopGlobals  = new Hashtable();

    /**
     * Fields specified here will be allocated constant offsets.
     */
    public final static long
          com_sun_squawk_VM$currentIsolate                   = Oop("com.sun.squawk.VM.currentIsolate")
        , com_sun_squawk_VM$extendsEnabled                   = Int("com.sun.squawk.VM.extendsEnabled")
        , com_sun_squawk_VM$usingTypeMap                     = Int("com.sun.squawk.VM.usingTypeMap")

        , com_sun_squawk_GC$traceFlags                       = Int("com.sun.squawk.GC.traceFlags")
        , com_sun_squawk_GC$collecting                       = Int("com.sun.squawk.GC.collecting")
        , com_sun_squawk_GC$monitorExitCount                 = Int("com.sun.squawk.GC.monitorExitCount")
        , com_sun_squawk_GC$monitorReleaseCount              = Int("com.sun.squawk.GC.monitorReleaseCount")

        , com_sun_squawk_VMThread$nextThreadNumber             = Int("com.sun.squawk.VMThread.nextThreadNumber")
        , com_sun_squawk_VMThread$currentThread                = Oop("com.sun.squawk.VMThread.currentThread")
        , com_sun_squawk_VMThread$otherThread                  = Oop("com.sun.squawk.VMThread.otherThread")
        , com_sun_squawk_VMThread$serviceThread                = Oop("com.sun.squawk.VMThread.serviceThread")

        , com_sun_squawk_ServiceOperation$pendingException   = Oop("com.sun.squawk.ServiceOperation.pendingException")
        , com_sun_squawk_ServiceOperation$code               = Int("com.sun.squawk.ServiceOperation.code")
        , com_sun_squawk_ServiceOperation$context            = Int("com.sun.squawk.ServiceOperation.context")
        , com_sun_squawk_ServiceOperation$op                 = Int("com.sun.squawk.ServiceOperation.op")
        , com_sun_squawk_ServiceOperation$channel            = Int("com.sun.squawk.ServiceOperation.channel")
        , com_sun_squawk_ServiceOperation$i1                 = Int("com.sun.squawk.ServiceOperation.i1")
        , com_sun_squawk_ServiceOperation$i2                 = Int("com.sun.squawk.ServiceOperation.i2")
        , com_sun_squawk_ServiceOperation$i3                 = Int("com.sun.squawk.ServiceOperation.i3")
        , com_sun_squawk_ServiceOperation$i4                 = Int("com.sun.squawk.ServiceOperation.i4")
        , com_sun_squawk_ServiceOperation$i5                 = Int("com.sun.squawk.ServiceOperation.i5")
        , com_sun_squawk_ServiceOperation$i6                 = Int("com.sun.squawk.ServiceOperation.i6")
        , com_sun_squawk_ServiceOperation$o1                 = Add("com.sun.squawk.ServiceOperation.o1")
        , com_sun_squawk_ServiceOperation$o2                 = Add("com.sun.squawk.ServiceOperation.o2")
        , com_sun_squawk_ServiceOperation$result             = Int("com.sun.squawk.ServiceOperation.result")
        , com_sun_squawk_ServiceOperation$addressResult      = Add("com.sun.squawk.ServiceOperation.addressResult")

        , branchCountHigh                               = Int("branchCountHigh")
        , branchCountLow                                = Int("branchCountLow")
        , traceStartHigh                                = Int("traceStartHigh")
        , traceStartLow                                 = Int("traceStartLow")
        , traceEndHigh                                  = Int("traceEndHigh")
        , traceEndLow                                   = Int("traceEndLow")
        , tracing                                       = Int("tracing")
        , runningOnServiceThread                        = Int("runningOnServiceThread")
        , currentThreadID                               = Int("currentThreadID")
        , cheneyStartMemoryProtect                      = Add("cheneyStartMemoryProtect")
        , cheneyEndMemoryProtect                        = Add("cheneyEndMemoryProtect")
        , newCount                                      = Int("newCount")
        , newHits                                       = Int("newHits")
        ;

    /**
     * Tags
     */
    private final static long INT     = 0x8888888800000000L,
                              OOP     = 0x9999999900000000L,
                              ADDR    = 0xAAAAAAAA00000000L,
                              TAGMASK = 0xFFFFFFFF00000000L;


    /**
     * Add a global int.
     *
     * @param name the field name
     * @return the field constant
     */
    private static long Int(String name) {
        int index = intGlobals.size();
        intGlobals.put(name, new Integer(index));
        return INT | index;
    }

    /**
     * Add a global address.
     *
     * @param name the field name
     * @return the field constant
     */
    private static long Add(String name) {
        int index = addrGlobals.size();
        addrGlobals.put(name, new Integer(index));
        return ADDR | index;
    }

    /**
     * Add a global oop reference.
     *
     * @param name the field name
     * @return the field constant
     */
    private static long Oop(String name) {
        int index = oopGlobals.size();
        oopGlobals.put(name, new Integer(index));
        return OOP | index;
    }

    /**
     * Get the hashtable of global ints.
     *
     * @return the hashtable
     */
    public static Hashtable getGlobalInts() {
        return intGlobals;
    }

    /**
     * Get the hashtable of global addresses.
     *
     * @return the hashtable
     */
    public static Hashtable getGlobalAddrs() {
        return addrGlobals;
    }

    /**
     * Get the hashtable of global oops.
     *
     * @return the hashtable
     */
    public static Hashtable getGlobalOops() {
        return oopGlobals;
    }

    /**
     * Test to see if the field constant is a global int.
     *
     * @param field the field constant
     * @return true if it is
     */
    public static boolean isGlobalInt(long field) {
        return (field & TAGMASK) == INT;
    }

    /**
     * Test to see if the field constant is a global address
     *
     * @param field the field constant
     * @return true if it is
     */
    public static boolean isGlobalAddr(long field) {
        return (field & TAGMASK) == ADDR;
    }

    /**
     * Test to see if the field constant is a global ref
     *
     * @param field the field constant
     * @return true if it is
     */
    public static boolean isGlobalOop(long field) {
        return (field & TAGMASK) == OOP;
    }

    /**
     * Get offset
     *
     * @param field the field constant
     * @return the offset
     */
    public static int getOffset(long field) {
        return (short)field;
    }

}
