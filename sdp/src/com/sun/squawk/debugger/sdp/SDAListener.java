/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk.debugger.sdp;

import java.io.*;

import com.sun.squawk.debugger.*;
import com.sun.squawk.debugger.DataType.*;
import com.sun.squawk.util.*;
import java.util.*;
import com.sun.squawk.*;

/**
 * A SDAListener implements the Squawk Debugger Proxy side of the JDWP protocol
 * and communicates with a (SDA) Squawk Debugger Agent running in a Squawk VM.
 *
 * @author Derek White, Doug Simon
 * @author Gary Yee - single stepping support
 */
final class SDAListener extends JDWPListener {

    /**
     * The Squawk Debugger Proxy that owns this listener.
     */
    private final SDP sdp;

    /**
     * The JDWP command set handlers.
     */
    private final IntHashtable commandSets = new IntHashtable();

    public SDAListener(SDP sdp) {
        this.sdp = sdp;
        Event eventHandler = new Event();
        commandSets.put(JDWP.Event_COMMAND_SET,       eventHandler);
        commandSets.put(SDWP.SquawkVM_COMMAND_SET,    new SquawkVM(eventHandler));
    }

    /**
     * {@inheritDoc}
     */
    public void processCommand(CommandPacket command) throws IOException {

        try {
            SDPCommandSet handler = (SDPCommandSet) commandSets.get(command.set());
            if (handler == null || !handler.handle(sdp, this, otherHost, command)) {
                System.err.println("Unrecognized command: " + command);
            }
        } catch (IOException e) {
            System.err.println(command + " caused: " + e);
            if (command.needsReply()) {
                ReplyPacket reply = command.createReply(JDWP.Error_INTERNAL);
                if (Log.info()) {
                    Log.log("Sending error reply: " + reply);
                }
                sendReply(reply);
            }
        }
    }

    /**
     * Blocks the current thread until an event is received from the
     * VM indicating that at least one thread is running.
     */
    void waitForEvent() {
        Event eventHandler = (Event)commandSets.get(JDWP.Event_COMMAND_SET);
        synchronized (eventHandler) {
            try {
                eventHandler.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "SDAListener";
    }

    /*-----------------------------------------------------------------------*\
     *                         Event Command Set (64)                        *
    \*-----------------------------------------------------------------------*/

    /**
     * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_Event">Event</a>.
     */
    static final class Event extends SDPCommandSet {

        /**
         * The bootstrap classes that have yet to be processed.
         */
        private List bootstrapClasses;

        private boolean vmDeath;

        /**
         * {@inheritDoc}
         */
        protected boolean dispatch() throws IOException {
            vmDeath = false;
            if (command.command() == JDWP.Event_Composite_COMMAND) {
                Composite();
                return true;
            }
            return false;
        }

        protected void postDispatch() {
            if (vmDeath) {
                host.quit();
            }
        }

        static class ForwardedEvent {
            final int eventKind;
            final int requestID;
            ForwardedEvent(int eventKind, int requestID) {
                this.eventKind = eventKind;
                this.requestID = requestID;
            }

            void writeHeader(PacketOutputStream out) throws IOException {
                out.writeByte(eventKind, "eventKind");
                out.writeInt(requestID, "requestID");
            }

            void writeBody(PacketOutputStream out) throws IOException {
            }
        }


        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_Event_Composite">Composite</a>.
         */
        private void Composite() throws IOException {
            int suspendPolicy = in.readByte("suspendPolicy");
            int eventCount = in.readInt("events");
            List events = new ArrayList(eventCount);
            for (int i = 0; i != eventCount; ++i) {
                int eventKind = in.readByte("eventKind");
                int requestID = in.readInt("requestID");
                switch (eventKind) {
                    case JDWP.EventKind_VM_DEATH: {
                        vmDeath = true;
                        events.add(new ForwardedEvent(eventKind, requestID));
                        break;
                    }
                    case JDWP.EventKind_THREAD_START:
                    case JDWP.EventKind_THREAD_END: {
                        final ObjectID threadID = in.readObjectID("thread");
                        if (requestID == 0) {
                            // These special events are not forwarded
                            int status = in.readInt("status");
                            int suspendCount = in.readInt("suspendCount");
                            String name = in.readString("name");
                            sdp.getTPM().updateThread(threadID, name, status, suspendCount);
                        } else {
                            events.add(new ForwardedEvent(eventKind, requestID) {
                                void writeBody(PacketOutputStream out) throws IOException {
                                    out.writeObjectID(threadID, "thread");
                                }
                            });
                        }
                        break;
                    }
                    case JDWP.EventKind_CLASS_PREPARE: {
                        try {
                            ObjectID threadID = in.readObjectID("thread");
                            ProxyThread thread = sdp.getTPM().getThread(threadID);
                            byte refTypeTag = in.readByte("refTypeTag");
                            ReferenceTypeID typeID = in.readReferenceTypeID("typeID");
                            String sig = in.readString("signature");
                            int status = in.readInt("status");

                            ProxyType type = sdp.getPTM().addClass(typeID, sig, true);
                            if (type != null) {
                                proposeClassPrepareEvent(type, thread);
                            }
                        } catch (SDWPException e) {
                            e.printStackTrace();
                        }
                        break;
                    }

                    case JDWP.EventKind_VM_INIT: {
                        final ObjectID threadID = in.readObjectID("thread");
                        if (requestID == 0) {
                            // Read data describing all the classes that were available
                            // to the application before it started (i.e. all the classes in the
                            // suite chain against which the application is bound).
                            int classes = in.readInt("classes");
                            String lastName = "";
                            bootstrapClasses = new ArrayList();
                            for (int j = 0; j != classes; ++j) {
                                ReferenceTypeID typeID = in.readReferenceTypeID("typeID");
                                int commonPrefix = in.readByte("commonPrefix") & 0xFF;
                                String name = in.readString("name");
                                if (commonPrefix != 0) {
                                    name = lastName.substring(0, commonPrefix) + name;
                                }
                                int status = in.readInt("status");
                                lastName = name;
                                ProxyType type = sdp.getPTM().addClass(typeID, name, false);
                                if (type != null) {
                                    bootstrapClasses.add(type);
                                }
                            }
                        }
                        events.add(new ForwardedEvent(eventKind, requestID) {
                            void writeBody(PacketOutputStream out) throws IOException {
                                out.writeObjectID(threadID, "thread");
                            }
                        });
                        break;
                    }

                    case JDWP.EventKind_BREAKPOINT: {
                        final ObjectID threadID = in.readObjectID("thread");
                        final Location location = in.readLocation("location");
                        events.add(new ForwardedEvent(eventKind, requestID) {
                            void writeBody(PacketOutputStream out) throws IOException {
                                out.writeObjectID(threadID, "thread");
                                out.writeLocation(location, "location");
                            }
                        });
                        break;
                    }

                    case JDWP.EventKind_EXCEPTION: {
                        final ObjectID threadID = in.readObjectID("thread");
                        final Location location = in.readLocation("location");
                        final TaggedObjectID exception = in.readTaggedObjectID("exception");
                        final Location catchLocation = in.readLocation("catchLocation");
                        events.add(new ForwardedEvent(eventKind, requestID) {
                            void writeBody(PacketOutputStream out) throws IOException {
                                out.writeObjectID(threadID, "thread");
                                out.writeLocation(location, "location");
                                out.writeTaggedObjectID(exception, "exception");
                                out.writeLocation(catchLocation, "catchLocation");
                            }
                        });
                        break;
                    }

                    case JDWP.EventKind_SINGLE_STEP: {
                        final ObjectID threadID = in.readObjectID("thread");
                        final Location location = in.readLocation("location");
                        events.add(new ForwardedEvent(eventKind, requestID) {
                            void writeBody(PacketOutputStream out) throws IOException {
                                out.writeObjectID(threadID, "thread");
                                out.writeLocation(location, "location");
                            }
                        });
                        break;
                    }
                }
            }

            if (!events.isEmpty()) {
                try {
                    command = new CommandPacket(JDWP.Event_COMMAND_SET, JDWP.Event_Composite_COMMAND, false);
                    out = command.getOutputStream();
                    out.writeByte(suspendPolicy, "suspendPolicy");
                    out.writeInt(events.size(), "events");
                    for (Iterator iter = events.iterator(); iter.hasNext(); ) {
                        ForwardedEvent fe = (ForwardedEvent)iter.next();
                        fe.writeHeader(out);
                        fe.writeBody(out);
                    }

                    if (Log.verbose()) {
                        Log.log("SDAListener: Passing through to debugger: " + command);
                    }

                    otherHost.sendCommand(command);
                } catch (SDWPException e) {
                    // This is an asynchronous send without a reply
                    Assert.shouldNotReachHere();
                }

                // Notify any threads waiting for an event to have been received and passed through to the debugger
                synchronized (this) {
                    this.notifyAll();
                }

                proposeBootstrapClassPrepareEvents();
            }
        }

        void proposeBootstrapClassPrepareEvents() {

            if (bootstrapClasses == null || bootstrapClasses.isEmpty()) {
                return;
            }

            // Generate CLASS_PREPARE events for the bootstrap classes that have
            // yet to be processed. This stops at the first registered request
            // that suspends one or more threads.
            //
            // These events will all be sent to the debugger before it receives
            // a reply to any suspension command it sent after receiving the
            // first event in the sequence. That is, the debugger should never
            // be receiving events when it thinks the VM is suspended.
            ProxyThread thread = sdp.getTPM().getRunningThread();
            if (thread != null) {
                for (Iterator iter = bootstrapClasses.iterator(); iter.hasNext(); ) {
                    ProxyType type = (ProxyType)iter.next();
                    iter.remove();
                    if (Log.verbose()) {
                        Log.log("proposing class prepare for " + type);
                    }
                    boolean suspendedThread = proposeClassPrepareEvent(type, thread);
                    if (suspendedThread) {
                        if (Log.verbose()) {
                            Log.log("suspended on class prepare for " + type);
                        }
                        break;
                    }
                }
            }
        }

        boolean proposeClassPrepareEvent(ProxyType type, ProxyThread thread) {
            // Only send back classes that came from class files
            Klass klass = type.getKlass();
            if (!klass.isSynthetic() && !klass.isInternalType() && !klass.isSquawkPrimitive()) {
                Debugger.Event event = new Debugger.Event(Debugger.Event.CLASS_PREPARE, type);
                EventNotifier notifier = new EventNotifier(event, thread, thread.id);
                EventManager.MatchedRequests mr = sdp.eventManager.matchRequests(notifier);
                if (mr.requests != null) {
                    try {
                        sdp.eventManager.send(notifier, mr);
                    } catch (SDWPException e) {
                        System.out.println("Error while sending CLASS_PREPARE event to debugger: " + e.getMessage());
                        e.printStackTrace();
                    } catch (IOException e) {
                        System.out.println("IO error while sending CLASS_PREPARE event to debugger: " + e.getMessage());
                        e.printStackTrace();
                    }
                    return mr.suspendPolicy != JDWP.SuspendPolicy_NONE;
                }
            }
            return false;
        }
    }

    /*-----------------------------------------------------------------------*\
     *                         SquawkVM Command Set (128)                    *
    \*-----------------------------------------------------------------------*/

    static final class SquawkVM extends SDPCommandSet {

        private final SDAListener.Event event;

        SquawkVM(SDAListener.Event event) {
            this.event = event;
        }

        /**
         * {@inheritDoc}
         */
        protected boolean dispatch() throws IOException {
            try {
                switch (command.command()) {
                    case SDWP.SquawkVM_SteppingInfo_COMMAND:       SteppingInfo();       break;
                    case SDWP.SquawkVM_ThreadStateChanged_COMMAND: ThreadStateChanged(); break;
                    default: return false;
                }
                return true;
            } catch (SDWPException e) {
                e.printStackTrace();
                Assert.shouldNotReachHere();
                return false;
            }
        }

        /**
         * Implements {@link SDWP#SquawkVM_SteppingInfo_COMMAND}.
         */
        private void SteppingInfo() throws IOException, SDWPException {
            if (Log.debug()) {
                Log.log("SteppingInfo: getting stepping info");
            }
            ReferenceTypeID typeID = in.readReferenceTypeID("refType");  // The class
            MethodID methodID = in.readMethodID("method");               // The method
            long bci = in.readLong("bci");

            if (Log.debug()) {
                Log.log("SteppingInfo: class typeID = " + typeID + ", methodID = " + methodID);
            }
            ProxyType definingClass = sdp.getPTM().lookup(typeID, false);
            ProxyMethod method = null;

            /* Verify that definingClass and method is not null */
            if (definingClass == null || (method = definingClass.getMethod(methodID)) == null) {
                if (Log.debug()) {
                    Log.log("SteppingInfo: definingClass and/or method was null");
                }
                throw new SDWPException(JDWP.Error_ABSENT_INFORMATION, "definingClass and/or method was null");
            }

            ProxyMethod.LineNumberTable table = method.getLineNumberTable();

            int line = table.getLineNumber(bci);
            if (Log.debug()) {
                Log.log("SteppingInfo: (offset " + bci + ") --> (line " + line + ")");
            }
            /* Calculate targetOffset, dupOffset, and afterDupOffset */
            Assert.thatFatal(table != null, "SteppingInfo: unable to acquire line number table");

            /* targetOffset:
             *   To get the target offset of line L we find the line number table entry of L and get the next entry (or -1 if not found)
             *   Tricky case: If there are TWO offsets that have L as a line number, we only look at the earliest one.
             */
            long targetBCI = table.getOffsetOfLineAfter(line);
            /* offset that has same line number as 'line' but is not equal to 'offset' */
            long dupBCI = table.getDuplicateOffset(table.getDuplicateOffset(bci, line), line);
            /* offset of next line after duplicate */
            long afterDupBCI = table.getOffsetOfLineAfter(dupBCI, table.getLineNumber(dupBCI));

            if (Log.debug()) {
                Log.log("SteppingInfo [current bci = " + bci +
                        ", target bci = " + targetBCI +
                        ", dup current line bci = " + dupBCI +
                        ", bci after current dup = " + afterDupBCI +
                        "]");
            }

            out.writeLong(targetBCI, "targetBCI");
            out.writeLong(dupBCI, "dupBCI");
            out.writeLong(afterDupBCI, "afterDupBCI");
        }

        /**
         * Implements {@link SDWP#SquawkVM_ThreadStateChanged_COMMAND}.
         */
        private void ThreadStateChanged() throws IOException {
            sdp.getTPM().updateThreads(in);
            event.proposeBootstrapClassPrepareEvents();
        }
    }
}
