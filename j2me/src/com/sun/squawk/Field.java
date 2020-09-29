/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk;

import com.sun.squawk.vm.CID;

/**
 * An instance of <code>Field</code> encapsulates the information about the
 * field of a class. This includes the name of the field, its type, access
 * flags etc.
 *
 * @author  Doug Simon
 */
public final class Field extends Member {

    /**
     * Creates a new <code>Field</code>.
     *
     * @param  metadata the metadata of the class that declared the field
     * @param  id       the index of this field within <code>metadata</code>
     */
    Field(KlassMetadata metadata, int id) {
        super(metadata, id);
    }


    /*---------------------------------------------------------------------------*\
     *              Access permissions and member property queries               *
    \*---------------------------------------------------------------------------*/

    /**
     * Determines if this field is transient.
     *
     * @return  true if this field is transient
     */
    public boolean isTransient() {
        return Modifier.isTransient(parser().getModifiers());
    }

    /**
     * Determines if this field had a ConstantValue attribute in its class file
     * definition. Note that this does not necessarily mean that the field is 'final'.
     *
     * @return  if there is a constant value associated with this field
     */
    public boolean hasConstant() {
        return Modifier.hasConstant(parser().getModifiers());
    }


    /*---------------------------------------------------------------------------*\
     *                        Field component getters                            *
    \*---------------------------------------------------------------------------*/

    /**
     * Gets this declared type of this field.<p>
     *
     * @return   this declared type of this field
     */
    public Klass getType() {
        return parser().getSignatureTypeAt(0);
    }

    /**
     * Gets the String constant value of this static field.
     *
     * @return  the value derived from the ConstantValue classfile attribute
     * @throws  IllegalArgumentException if this field did not have a ConstantValue
     *          attribute in its class file or if the constant is not a String
     */
    public String getStringConstantValue() throws IllegalArgumentException {
        if (!hasConstant() || getType().getSystemID() != CID.STRING) {
            throw new IllegalArgumentException();
        }
        return parser().getStringConstantValue();
    }

    /**
     * Gets the primitive constant value of this static field.
     *
     * @return  the value derived from the ConstantValue classfile attribute
     * @throws  IllegalArgumentException if this field did not have a ConstantValue
     *          attribute in its class file or if the constant is not a primitive value
     */
    public long getPrimitiveConstantValue() throws IllegalArgumentException {
        if (!hasConstant() || !getType().isPrimitive()) {
            throw new IllegalArgumentException();
        }
        return parser().getPrimitiveConstantValue();
    }
}
