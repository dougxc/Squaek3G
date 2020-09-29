/*
 *  Copyright (c) 1999-2001 Sun Microsystems, Inc., 901 San Antonio Road,
 *  Palo Alto, CA 94303, U.S.A.  All Rights Reserved.
 *
 *  Sun Microsystems, Inc. has intellectual property rights relating
 *  to the technology embodied in this software.  In particular, and
 *  without limitation, these intellectual property rights may include
 *  one or more U.S. patents, foreign patents, or pending
 *  applications.  Sun, Sun Microsystems, the Sun logo, Java, KJava,
 *  and all Sun-based and Java-based marks are trademarks or
 *  registered trademarks of Sun Microsystems, Inc.  in the United
 *  States and other countries.
 *
 *  This software is distributed under licenses restricting its use,
 *  copying, distribution, and decompilation.  No part of this
 *  software may be reproduced in any form by any means without prior
 *  written authorization of Sun and its licensors, if any.
 *
 *  FEDERAL ACQUISITIONS:  Commercial Software -- Government Users
 *  Subject to Standard License Terms and Conditions
 */

package com.sun.squawk.io.j2me.channel;

import java.io.*;
import javax.microedition.io.*;
import com.sun.squawk.io.*;
import com.sun.squawk.vm.ChannelConstants;
import com.sun.squawk.*;
import java.lang.ref.WeakReference;
import com.sun.squawk.util.Assert;

/**
 * This connection is used to 'channel' other connections through to
 * an embedded J2SE VM (if any) that may support additional
 * connection types not supported by the Squawk core code.
 *
 * @author Nik Shaylor, Doug Simon
 */
public class Protocol extends ConnectionBase implements StreamConnection, StreamConnectionNotifier {

    /**
     * A weak reference is used (as finalization is only conditionally available)
     * to inform the VM that Squawk has finished with a given channel and it can
     * therefore release the associated resources.
     */
    static final class ChannelID extends WeakReference {

        /**
         * The identifier obtained from the embedded VM describing a resource
         * it opened on behalf of a GCF connection in Squawk.
         */
        int id;

        /**
         * Linked list link.
         */
        ChannelID next;

        /**
         * Used purely for debugging.
         */
        String protocolAndName;

        /**
         * The list of channel identifiers that come from the embedded
         * VM they have yet to be released.
         */
        static ChannelID channelIDs;

        /**
         * The thread used to release the channel identifiers. This
         * thread is lazily created and started when the first channel
         * connection is created.
         */
        static Thread channelIDFinalizer;
        static Object channelIDFinalizerLock;

        /**
         * A runnable that wakes up every second and processes the list
         * of channel identifiers. Any identifiers for which the associated
         * Squawk-side connection object has been garbage collected are
         * released in the embedded VM.
         */
        static class Finalizer extends Thread {

            public void run() {
                while (true) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                    }
                    synchronized (channelIDFinalizerLock) {
                        ChannelID head = null;
                        ChannelID c = channelIDs;
                        channelIDs = null;

                        while (c != null) {
                            ChannelID next = c.next;
                            if (c.get() == null) {
                                c.free();
                            } else if (c.id != 0) {
                                c.next = head;
                                head = c;
                            }
                            c = next;
                        }
                        channelIDs = head;
                        if (channelIDs == null) {
                            channelIDFinalizer = null;
                            break;
                        }
                    }
                }
            }
        }

        static void registerChannelID(ChannelID id) {
            synchronized (channelIDFinalizerLock) {
                // Set channelIDs prior to starting the Finalizer thread
                Assert.always(id.next == null);
                id.next = channelIDs;
                channelIDs = id;
/*
                if (channelIDFinalizer == null) {
                    channelIDFinalizer = new Finalizer();
                    VM.setAsDaemonThread(channelIDFinalizer);
                    channelIDFinalizer.setPriority(Thread.MIN_PRIORITY);
                    channelIDFinalizer.start();
                }
                */
            }
        }

        static {
            channelIDFinalizerLock = new Object();
        }
        
        /**
         * Creates an association between a embedded VM channel identifier
         * and the Squawk connection object using it.
         */
        ChannelID(int id, Protocol channel) {
            super(channel);
            Assert.always(id != 0);
            this.id = id;
            this.protocolAndName = channel.protocolAndName;
            registerChannelID(this);
        }

        /**
         * Releases the underlying channel resource in the embedded VM if
         * it has not already been released.
         */
        void free() {
//VM.println("calling ChannelID.free on " + VM.getCurrentIsolate().getMainClassName() + ": " + id + " " + protocolAndName);
            if (id != 0) {
                try {
                    VM.freeChannel(id);
                } catch (IOException ioe) {
                    System.err.println(ioe);
                }
                id = 0;
            }
        }
    }

    /**
     * Channel number.
     */
    ChannelID channelID;

    /**
     * The underlying protocol and channel.
     */
    String protocolAndName;

    /**
     * Channel use count.
     */
    int useCount = 0;

    /**
     * Public constructor
     */
    public Protocol() {
    }

    /**
     * Private constructor
     */
    private Protocol(int channelID) {
        this.channelID = new ChannelID(channelID, this);
        useCount++;
    }

    /**
     * open
     */
    public Connection open(String protocol, String name, int mode, boolean timeouts) throws IOException {
        if (protocol == null || name == null) {
            throw new NullPointerException();
        }
        protocolAndName = protocol + ":" + name;
        channelID = new ChannelID(VM.getChannel(ChannelConstants.CHANNEL_GENERIC), this);
        useCount++;
        VM.execIO(ChannelConstants.OPENCONNECTION, channelID.id, mode, timeouts?1:0, 0, 0, 0, 0, protocolAndName, null);
        return this;
    }

    /**
     * openInputStream
     */
    public InputStream openInputStream() throws IOException {
        useCount++;
        InputStream is = new ChannelInputStream(this);
/*if[BUFFERCHANNELINPUT]*/
        is = new BufferedInputStream(is);
/*end[BUFFERCHANNELINPUT]*/
        return is;
    }

    /**
     * openOutputStream
     */
    public OutputStream openOutputStream() throws IOException {
        useCount++;
        return new ChannelOutputStream(this);
    }

    /**
     * acceptAndOpen
     */
    public StreamConnection acceptAndOpen() throws IOException {
        int newChan = VM.execIO(ChannelConstants.ACCEPTCONNECTION, channelID.id, 0, 0, 0, 0, 0, 0, null, null);
        return new Protocol(newChan);
    }

    /**
     * Close the connection.
     */
    synchronized public void close() throws IOException {
        VM.execIO(ChannelConstants.CLOSECONNECTION, channelID.id, 0, 0, 0, 0, 0, 0, null, null);
        decrementCount();
    }

    /**
     * Decrement channel use count.
     */
    void decrementCount() {
        if (useCount > 0) {
            useCount--;
            if (useCount == 0) {
                channelID.free();
            }
        }
    }
}
