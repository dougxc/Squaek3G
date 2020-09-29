/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk.vm;

import java.util.Vector;

/**
 * Sentinal object used when waiting for events.
 */
public class EventQueue {

    /**
     * Pending events list.
     */
    private static Vector events = new Vector();

    /**
     * Next event number.
     */
    private static int nextEventNumber = 1;

    /**
     * Flag to control waiting. We could also have done this by posting
     * an event to the queue, but this saves on eventNumbers and really is
     * a spurious poking of the wait-queue.
     */
    private static boolean noWaiting = false;

    /*
     * waitFor
     */
    static void waitFor(long time) {
        if (ChannelIO.TRACING_ENABLED) ChannelIO.trace("++waitFor "+time);
        synchronized(events) {
            if (events.size() == 0 && noWaiting != true) {
                try { events.wait(time); } catch(InterruptedException ex) {}
            }
	    noWaiting = false;
        }
        if (ChannelIO.TRACING_ENABLED) ChannelIO.trace("--waitFor");
    }

    /*
     * sendNotify
     */
    static void sendNotify() {
        if (ChannelIO.TRACING_ENABLED) ChannelIO.trace("++sendNotify");
        synchronized(events) {
	    noWaiting = true;
            events.notifyAll();
        }
        if (ChannelIO.TRACING_ENABLED) ChannelIO.trace("--sendNotify");
    }

    /*
     * getNextEventNumber
     */
    static int getNextEventNumber() {
        if (nextEventNumber >= Integer.MAX_VALUE-1) {
            System.err.println("Reached event number limit"); // TEMP -- Need a way to recycle event numbers
            System.exit(0);
        }
        /*
         * Make sure all event numbers are odd so they will not be the same as
         * the ones that are allocated in the message system.
         */
        if ((nextEventNumber % 2) == 0) {
            ++nextEventNumber;
        }
        return nextEventNumber++;
    }

    /*
     * unblock
     */
    static void unblock(int event) {
        synchronized(events) {
            events.addElement(new Integer(event));
            events.notifyAll();
        }
        if (ChannelIO.TRACING_ENABLED) ChannelIO.trace("++unblock ");
    }

    /*
     * getEvent
     */
    static int getEvent() {
        int res = 0;
        synchronized(events) {
            if (events.size() > 0) {
                Integer event = (Integer)events.firstElement();
                events.removeElementAt(0);
                res = event.intValue();
            }
        }
        if (ChannelIO.TRACING_ENABLED) ChannelIO.trace("++getEvent = "+res);
        return res;
    }

}


