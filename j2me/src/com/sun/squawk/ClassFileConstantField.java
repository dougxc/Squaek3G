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
 * An instance of <code>ClassFileConstantField</code> encapsulates all the
 * symbolic information of a field declaration in a class file that has a
 * ConstantValue attribute.
 * This class is provided for a subsystem (such as the translator) that
 * loads a class definition from a class file.
 *
 * @author  Doug Simon
 */
public final class ClassFileConstantField extends ClassFileField {

    /**
     * The primitive value in the ConstantValue attribute.
     */
    final long primitiveConstantValue;

    /**
     * The String value in the ConstantValue attribute.
     */
    final String stringConstantValue;

    /**
     * Creates a new <code>ClassFileConstantField</code> instance for a field with a
     * primitive ConstantValue attribute.
     *
     * @param   name          the name of the field
     * @param   modifiers     the modifiers of the field
     * @param   type          the type of the field
     * @param   constantValue the primitive constant value (as a long)
     */
    public ClassFileConstantField(String name, int modifiers, Klass type, long constantValue) {
        super(name, modifiers, type);
        this.primitiveConstantValue = constantValue;
        this.stringConstantValue = null;
    }

    /**
     * Creates a new <code>ClassFileConstantField</code> instance for a field with a
     * String ConstantValue attribute.
     *
     * @param   name          the name of the field
     * @param   modifiers     the modifiers of the field
     * @param   type          the type of the field
     * @param   constantValue the string constant value
     */
    public ClassFileConstantField(String name, int modifiers, Klass type, String constantValue) {
        super(name, modifiers, type);
        this.primitiveConstantValue = 0;
        this.stringConstantValue = constantValue;
    }
}
