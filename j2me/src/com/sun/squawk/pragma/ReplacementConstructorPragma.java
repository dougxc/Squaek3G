/*
 * Copyright 2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM translator.
 */

package com.sun.squawk.pragma;

/**
 * Denotes a method (that must be static) whose code replaces the body
 * of the constructor with the same declared parameter types. The denoted
 * method must return a value of the class type in which it is defined.
 * Any direct call to the method will result in a NoSuchMethodError in the translator.
 * <p>
 * This pragma is used by Squawk to create the data for an object that is
 * internally represented in the array object format.
 */
public class ReplacementConstructorPragma extends PragmaException {
}
