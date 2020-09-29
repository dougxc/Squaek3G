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
 * Interface to enable calling constructor of java.lang.Class.
 *
 * @author  Doug Simon
 */
public interface ClassTunnel {
    public Class create(Klass klass);
}
