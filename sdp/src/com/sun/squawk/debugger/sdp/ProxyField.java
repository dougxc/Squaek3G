/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk.debugger.sdp;

import com.sun.squawk.debugger.*;
import com.sun.squawk.debugger.DataType.FieldID;
import com.sun.squawk.*;

/**
 * A proxy for a field.
 *
 * @author  Doug Simon
 */
public final class ProxyField {

    /**
     * The proxied field.
     */
    private final Field field;

    /**
     * The JDWP identifier for the field.
     */
    private final FieldID id;

    /**
     * Creates a proxy for a field.
     *
     * @param id    the field's JDWP identifier
     * @param field the field
     */
    public ProxyField(FieldID id, Field field) {
        this.id = id;
        this.field = field;
    }

    /**
     * @return the proxied field
     */
    public Field getField() {
        return field;
    }

    /**
     * @return  the name of this field
     */
    public String getName() {
        return field.getName();
    }

    /**
     * @return the JNI signature of this field
     */
    public String getSignature() {
        return DebuggerSupport.getJNISignature(field);
    }

    /**
     * @return  the modifiers for this method
     * @see     Modifier#getJVMFielddModifiers
     */
    public int getModifiers() {
        return field.getModifiers() & Modifier.getJVMFieldModifiers();
    }

    public String toString() {
        return "<FIELD id: " + id + ", " + getName() + ": " + getSignature() + ">";
    }

    /**
     * @return  the JDWP identifier for this field
     */
    public FieldID getID() {
        return id;
    }
}
