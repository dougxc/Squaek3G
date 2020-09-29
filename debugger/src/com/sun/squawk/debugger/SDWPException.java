/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk.debugger;

import com.sun.squawk.debugger.*;

/**
 * A SDWPException is thrown when there is an error parsing or handling a JDWP/SDWP request.
 */
public class SDWPException extends Exception {

    private final int error;

    public SDWPException() {
        super("Generic SDWPException");
        this.error = JDWP.Error_INTERNAL;
    }

    public SDWPException(String msg) {
        super(msg);
        this.error = JDWP.Error_INTERNAL;
    }

    public SDWPException(int error, String msg) {
        super(msg);
        this.error = error;
    }

    public int getError() {
        return error;
    }

}
