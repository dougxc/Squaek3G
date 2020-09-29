/*
 * Copyright 2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk.vm2c;

import com.sun.tools.javac.tree.*;

/**
 * The exception thrown when a Java language construct is encountered in a
 * Java method that prevents the method from being converted to a C function.
 *
 * @author Doug Simon
 */
public class InconvertibleNodeException extends RuntimeException {
    public final JCTree node;
    public InconvertibleNodeException(JCTree node, String message) {
        super(message);
        this.node = node;
    }
}
