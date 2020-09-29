/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk.debugger;

import java.io.*;

import com.sun.squawk.*;
import com.sun.squawk.debugger.DataType.*;
import com.sun.squawk.util.*;

/**
 * An <code>EventNotifier</code> is used to synchronize communication between
 * a thread producing a JDWP event and consumer of such events. It also
 * encapsulates the details of the event as well as the JDWP identifier
 * of the thread in which the event occurred.
 *
 * @author Doug Simon
 */
public final class EventNotifier {

    /**
     * An EventConsumer consumes events delivered via an EventNotifier.
     */
    public static interface Consumer {

        /**
         * Determines if this consumer is still interested in consuming events.
         *
         * @return trus if this consumer in no longer consuming events
         */
        public boolean isDone();

        /**
         * Consumes an event.
         *
         * @param notifier  the object delivering the event and serializing the delivery
         *                  between the producer and this consumer
         * @throws IOException if an IO error occurs
         */
        public void consumeEvent(EventNotifier notifier) throws IOException;
    }

    private Debugger.Event event;
    private ObjectID threadID;
    private Thread thread;

    public EventNotifier() {

    }

    public EventNotifier(Debugger.Event event, Thread thread, ObjectID threadID) {
        this.event = event;
        this.thread = thread;
        this.threadID = threadID;
    }

    /**
     * @return  the produced event currently being consumed
     */
    public Debugger.Event getEvent() {
        return event;
    }

    /**
     * @return the thread that produced the event (may be null)
     */
    public Thread getThread() {
        return thread;
    }

    /**
     * @return the JDWP identifier of the thread that produced the event
     */
    public ObjectID getThreadID() {
        return threadID;
    }

    /**
     * Called by an event producer to notify a waiting consumer of an event.
     * This method will be called on the producer's thread that caused
     * the event. This thread will be blocked until a consumer has
     * consumed the event.
     *
     * @param event     the event being reported
     * @param thread    the thread that produced the event (may be null)
     * @param threadID  the JDWP identifier of the thread that produced the event
     */
    synchronized public void produceEvent(Debugger.Event event, Thread thread, ObjectID threadID, Consumer consumer) {

        if (Log.verbose()) {
            Log.log("Event producer (A): event produced: " + event);
        }

        // WAIT (A):
        // Block until the last producer has relinquished this object
        while (this.threadID != null) {
            if (consumer.isDone()) {
                notifyAll();
                return;
            }
            try {
                this.wait();
            } catch (InterruptedException e) {
            }
        }

        Assert.always(this.thread == null && this.threadID == null);
        this.threadID = threadID;
        this.thread = thread;
        this.event = event;

        if (Log.verbose()) {
            Log.log("Event producer (B): notifying consumers: " + event);
        }

        // Notify all threads blocked on this notifier. Any other producer threads will be
        // blocked in the while loop above and so a waiting consumer thread will eventually
        // be given a chance to run.
        this.notifyAll();

        if (Log.verbose()) {
            Log.log("Event producer (C): waiting for event consumer to finish: " + event);
        }

        // WAIT (B):
        // Wait until a consumer thread consumes the event.
        while (this.event != null) {
            if (consumer.isDone()) {
                notifyAll();
                return;
            }
            try {
                this.wait();
            } catch (InterruptedException e) {
            }
        }
        // Event has been handled, and the thread has been resumed.
        if (Log.verbose()) {
            Log.log("Event producer (D): resuming: " + event);
        }

        // Relinquish this object and notify the other producers that may be waiting for it
        this.thread = null;
        this.threadID = null;
        notifyAll();
    }

    /**
     * Consumes an event produced by a producer. The current thread will block until an
     * event is available for consumption. Once the given consumer has consumed the
     * event, the producer of the event is awakened and will read the result returned
     * by the consumer before reliquishing this notifier object for other event notifications
     * to take place.
     *
     * @param consumer   the object that will consume the event
     */
    public void consumeEvent(Consumer consumer) {
        synchronized (this) {
            // WAIT (C):
            // Wait for a producer to produce an event
            if (Log.verbose()) {
                Log.log("Event consumer (A): waiting for event");
            }

            while (this.event == null) {
                if (consumer.isDone()) {
                    this.event = null;
                    // Notify all waiting producers so that eventually the producer of the
                    // event just consumed will relinquish this notifier object and allow
                    // other producers to produce events.
                    this.notifyAll();
                    return;
                }
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }


            if (Log.verbose()) {
                Log.log("Event consumer (B): got event: " + event);
            }
            try {
                consumer.consumeEvent(this);
            } catch (IOException e) {
                if (Log.info()) {
                    Log.log("IO error while notifying debugger of " + event + ": " + e);
                }
            }
            this.event = null;

            // Notify all waiting producers so that eventually the producer of the
            // event just consumed will relinquish this notifier object and allow
            // other producers to produce events.
            this.notifyAll();
        }
    }
}

