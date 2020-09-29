//if[FINALIZATION]
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
 * Class to record objects that require finalization.
 *
 * @author Nik Shaylor
 */
public final class Finalizer implements Runnable {

    /**
     * The object requiring finalization.
     */
    private Object object;

    /**
     * The isolate of the thread that created the object.
     */
    private Isolate isolate;

    /**
     * Pointer to next finalizer in the garbage collector or isolate queue.
     */
    private Finalizer next;

    /**
     * A flag used by the Lisp2Collector to determine which queue a finalizer should be
     * put on after a collection. This collector cannot manipulate the queue during collection
     * as that would set invalid bits in the write barrier/marking bit map.
     */
    private boolean referenced;

    /**
     * Constructor.
     *
     * @param object the object that needs finalization
     */
    Finalizer(Object object) {
        this.object  = object;
        this.isolate = VM.getCurrentIsolate();
    }

    /**
     * Get the object.
     *
     * @return the object.
     */
    Object getObject() {
        return object;
    }

    /**
     * Set the next finalizer.
     *
     * @param nextFinalizer the finalizer
     */
    void setNext(Finalizer nextFinalizer) {
        next = nextFinalizer;
    }

    /**
     * Get the next finalizer.
     *
     * @return the next finalizer.
     */
    Finalizer getNext() {
        return next;
    }

    /**
     * Get the isolate.
     *
     * @return the isolate.
     */
    Isolate getIsolate() {
        return isolate;
    }

    /**
     * Queue the finalizer onto the isolate for execution.
     */
    void queueToIsolate() {
        isolate.addFinalizer(this);
    }

    /**
     * Determines if the last execution of the garbage collector determined that there were
     * no more references to the object associated with this finalizer.
     *
     * @return  true if there was at least reference to the object associated with this finalizer
     */
    boolean isReferenced() {
        return referenced;
    }

    /**
     * Sets or unsets the flag indicating that the last execution of the garbage collector determined that there were
     * no more references to the object associated with this finalizer.
     *
     * @param  flag  the new value of the flag
     */
    void setReferenced(boolean flag) {
        referenced = flag;
    }


    /**
     * Run the finalzer.
     */
    public void run() {
        try {
            VM.finalize(object);
        } catch(Throwable ex) {
        }
    }
}


