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
 * This class contains the offsets for methods that must be directly accessed by the
 * VM or other Squawk tools such as the mapper. The romizer ensures that these offsets
 * are correct when it creates the image for the bootstrap suite. The offset for a method
 * is its index in the relevant table of methods.
 *
 * The name of the constant must be composed of the name of the class that defines the
 * method (with '.'s replaced by '_'s) and the name of the method with a '$' separating them.
 * Virtual methods must be prefixed with "virtual$" and to disambiguate overloaded methods,
 * the parameter types can be appended to the identifier, each prefixed with a '$'. E.g.:
 *
 *   Method                                |  Constant identifier
 *  ---------------------------------------+-------------------------------------------------
 *   Klass.getInternalName()               | virtual$com_sun_squawk_Klass$getInternalName
 *   static Klass.getInternalName(Klass)   | com_sun_squawk_Klass$getInternalName
 *   static Klass.isOop(Klass, int)        | com_sun_squawk_Klass$isOop$com_sun_squawk_Klass$int
 *   static Klass.isOop(Klass, int, char)  | com_sun_squawk_Klass$isOop$com_sun_squawk_Klass$int$char
 */
public class MethodOffsets {
    public final static int com_sun_squawk_VM$startup                         = 1;
    public final static int com_sun_squawk_VM$undefinedNativeMethod           = 2;
    public final static int com_sun_squawk_VM$callRun                         = 3;
    public final static int com_sun_squawk_VM$getStaticOop                    = 4;
    public final static int com_sun_squawk_VM$getStaticInt                    = 5;
    public final static int com_sun_squawk_VM$getStaticLong                   = 6;
    public final static int com_sun_squawk_VM$putStaticOop                    = 7;
    public final static int com_sun_squawk_VM$putStaticInt                    = 8;
    public final static int com_sun_squawk_VM$putStaticLong                   = 9;
    public final static int com_sun_squawk_VM$yield                           = 10;
    public final static int com_sun_squawk_VM$nullPointerException            = 11;
    public final static int com_sun_squawk_VM$arrayIndexOutOfBoundsException  = 12;
    public final static int com_sun_squawk_VM$arithmeticException             = 13;
    public final static int com_sun_squawk_VM$abstractMethodError             = 14;
    public final static int com_sun_squawk_VM$arrayOopStore                   = 15;
    public final static int com_sun_squawk_VM$findSlot                        = 16;
    public final static int com_sun_squawk_VM$monitorenter                    = 17;
    public final static int com_sun_squawk_VM$monitorexit                     = 18;
    public final static int com_sun_squawk_VM$_instanceof                     = 19;
    public final static int com_sun_squawk_VM$checkcast                       = 20;
    public final static int com_sun_squawk_VM$lookup_b                        = 21;
    public final static int com_sun_squawk_VM$lookup_s                        = 22;
    public final static int com_sun_squawk_VM$lookup_i                        = 23;
    public final static int com_sun_squawk_VM$class_clinit                    = 24;
    public final static int com_sun_squawk_VM$_new                            = 25;
    public final static int com_sun_squawk_VM$newarray                        = 26;
    public final static int com_sun_squawk_VM$newdimension                    = 27;
    public final static int com_sun_squawk_VM$_lcmp                           = 28;
    public final static int com_sun_squawk_VM$reportException                 = 29;

    public final static int virtual$java_lang_Object$toString            = 8;
    public final static int virtual$java_lang_Object$abstractMethodError = 9;
/*if[FINALIZATION]*/
    public final static int virtual$java_lang_Object$finalize            = 10;
/*end[FINALIZATION]*/

}

