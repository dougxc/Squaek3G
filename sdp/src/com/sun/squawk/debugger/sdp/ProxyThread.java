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
import com.sun.squawk.debugger.DataType.*;
import com.sun.squawk.*;

/**
 * Mirrors a thread on the Squawk VM. This is used to reduce VM <-> proxy traffic
 * when a debug client (e.g. Netbeans) makes a large amount of thread status requests
 * even when there hasn't been any notified change in thread status.
 *
 * @author  Doug Simon
 */
class ProxyThread extends Thread {

    public final ObjectID id;
    private int suspendCount;
    private int status;

    public ProxyThread(ObjectID id, String name, int status, int suspendCount) {
        super(name);
        this.id = id;
        this.suspendCount = suspendCount;
        this.status = status;
    }

    public void setStatus(int s) {
        status = s;
    }

    public int getStatus() {
        return status;
    }

    public void setSuspendCount(int count) {
        suspendCount = count;
        if (count == 0) {
            if (Log.info()) {
                Log.log("resumed thread " + getName());
            }
        } else {
            if (Log.info()) {
                Log.log("suspended thread " + getName());
            }
        }
    }

    public int getSuspendCount() {
        return suspendCount;
    }

    public boolean isSuspended() {
        return suspendCount > 0;
    }

    public boolean isZombie() {
        return status == JDWP.ThreadStatus_ZOMBIE;
    }

    public String toString() {
        return "" + id + ": " + getName() + " status=" + status + " suspendCount=" + suspendCount;
    }
}
