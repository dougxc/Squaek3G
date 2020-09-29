//if[SUITE_VERIFIER]
/*
 * Copyright 2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM translator.
 */
package com.sun.squawk.translator.ir.verifier;

/**
 * Thrown whenever the {@link Verifier} fails to verify some
 * translated method.
 *
 * @author  Edward Carter
 */
public class VerifyError extends java.lang.Error {
    private int ip;

    public VerifyError(int ip) {
        this.ip = ip;
    }

    public VerifyError(String s, int ip) {
        super(s);
        this.ip = ip;
    }

    public int getIP() {
        return ip;
    }

    public static void check(boolean cond, int ip, String msg) {
        if (!cond) {
            throw new VerifyError(msg, ip);
        }
    }

    public static void check(boolean cond, int ip) {
        if (!cond) {
            throw new VerifyError(ip);
        }
    }
}
