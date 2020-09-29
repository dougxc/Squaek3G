/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM translator.
 */
package com.sun.squawk.translator.ci;

import com.sun.squawk.*;

/**
 * An instance of <code>MethodSignature</code> encapsulates a method's signature.
 *
 * @author  Doug Simon
 */
public final class MethodSignature {

    /**
     * The return type of a method.
     */
    public final Klass returnType;

    /**
     * The declared parameter types of a method.
     */
    public final Klass[] parameterTypes;

    /**
     * Construct a method signature.
     */
    public MethodSignature(Klass returnType, Klass[] parameterTypes) {
        this.returnType = returnType;
        this.parameterTypes  = parameterTypes;
    }

    /**
     * Gets the number of words used by the parameters where double and long parameters
     * use two words. Also, one word is added for the implicit 'this' parameter of a
     * non-static method.
     */
    public int getParametersLength(boolean isStatic) {
        int length = isStatic ? 0 : 1;
        for (int i = 0; i != parameterTypes.length; ++i) {
            length += parameterTypes[i].isDoubleWord() ? 2 : 1;
        }
        return length;
    }

    /**
     * A sentinel object representing an invalid signature.
     */
    public static final MethodSignature INVALID = new MethodSignature(null, null);

    /**
     * Change the return type of a signature. If the new return type
     * is different from the existing one, a new instance of
     * <code>Signature</code> is created and returned.
     *
     * @param type the new return type.
     */
    public MethodSignature modifyReturnType(Klass type) {
        if (type == returnType) {
            return this;
        } else {
            return new MethodSignature(type, parameterTypes);
        }
    }
}

