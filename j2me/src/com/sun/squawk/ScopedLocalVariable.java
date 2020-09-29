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
 * An instance of <code>ScopedLocalVariable</code> encapsulates the symbolic
 * information for a local variable that has a limited scope in a Squawk
 * bytecode method.
 *
 * @author  Doug Simon
 */
public final class ScopedLocalVariable {

    /**
     * The name of the local variable.
     */
    public final String name;

    /**
     * The type of the local variable.
     */
    public final Klass type;

    /**
     * The logical slot index of the local variable.
     */
    public final int slot;

    /**
     * The address at which the scope of the local variable starts.
     */
    public final int start;

    /**
     * The offset from 'start' at which the scope of the local variable ends.
     */
    public final int length;

    /**
     * Creates a <code>ScopedLocalVariable</code> instance representing the
     * symbolic information for a local variable in a Squawk bytecode method.
     *
     * @param  name    the local variable's name
     * @param  type    the local variable's type
     * @param  slot    the local variable's logical slot index
     * @param  start   the address at which the scope of the local variable starts
     * @param  length  the offset from <code>start</code> at which the scope of
     *                 the local variable ends
     */
    public ScopedLocalVariable(String name, Klass type, int slot, int start, int length) {
        this.name   = name;
        this.type   = type;
        this.slot   = slot;
        this.start  = start;
        this.length = length;
    }
}
