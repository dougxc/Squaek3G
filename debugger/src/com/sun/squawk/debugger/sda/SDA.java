/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk.debugger.sda;

import java.io.*;
import java.util.*;
import java.util.Hashtable;
import java.util.Vector;
import javax.microedition.io.*;

import com.sun.squawk.*;
import com.sun.squawk.DebuggerSupport;
import com.sun.squawk.VMThread.*;
import com.sun.squawk.debugger.*;
import com.sun.squawk.debugger.DataType.*;
import com.sun.squawk.debugger.EventRequest;
import com.sun.squawk.debugger.EventRequestModifier.*;
import com.sun.squawk.util.*;

/**
 * A SDA (Squawk Debugger Agent) instance handles the Squawk VM side of
 * a JDWP connection to a Squawk Debugger Proxy for a particular isolate.
 *
 * This class implements the abstract class (@link java.lang.Debugger}.
 * It runs as an isolate that is tightly connected to the isolate
 * being debugged. An instance of this class is installed in the isolate
 * being debugged as the debugging context for the application isolate.
 *
 * @author Derek White, Doug Simon
 */
public class SDA extends Debugger {

    /*-----------------------------------------------------------------------*\
     *                            Class requests                             *
    \*-----------------------------------------------------------------------*/

    /**
     * The mappings between JDWP identifiers and classes.
     */
    private Hashtable classToIdTable;
    private Hashtable idToClassTable;

    /**
     * Creates identifiers for all the classes in a given suite chain. Note that identifiers for
     * {@link Klass#isInternalType internal} classes are not created.
     *
     * @param suite     the tail in a chain of suites
     * @param processed the set of suites that have already been processed
     */
    private void initializeClassIDs(Suite suite, Hashtable processed) {
        if (!suite.isBootstrap()) {
            initializeClassIDs(suite.getParent(), processed);
        }

        if (processed.containsKey(suite)) {
            return;
        }

        int count = suite.getClassCount();
        for (int i = 0; i != count; ++i) {
            Klass klass = suite.getKlass(i);

            // Ignore all VM internal types
            if (!klass.isInternalType()) {
                getIDForClass(klass);
            }
        }

        processed.put(suite, suite);
    }

    /**
     * Ensures that the list of all the classes accessible by the debuggee isolate is
     * initialized. This
     * will include all the classes in the debuggee isolate's suite chain as well as
     * the classes in the debugger isolate's suite chain. The latter is required as
     * there are methods in the debugger implementation that implement abstract
     * methods in the {@link Debugger} class. These methods can be part of a call stack
     * in one of the debuggee isolate's threads.
     *
     * @return list of all the classes are accessible by the debuggee isolate.
     */
    private void initializeClassIDTable() {
        if (classToIdTable == null) {
            classToIdTable = new Hashtable();
            idToClassTable = new Hashtable();
            Hashtable processed = new Hashtable();

            // Add the classes of the debugger isolate first
            Suite suite = debuggerIsolate.getLeafSuite();
            initializeClassIDs(suite, processed);

            // Add the classes of the debuggee isolate next
            suite = debuggeeIsolate.getLeafSuite();
            initializeClassIDs(suite, processed);
        }
    }

    /**
     * Gets an enumeration over all the classes for which a JDWP identifier has been allocated.
     *
     * @return  the JDWP identifiable classes
     */
    Enumeration getClasses() {
        initializeClassIDTable();
        return classToIdTable.keys();
    }

    /**
     * Gets the class corresponding to a given JDWP identifier that was issued by this agent.
     *
     * @param classID    the unique identifier for a class within the context of the debugeee isolate
     * @param errorCode the <code>JDWP.Error_...</code> constant to use when throwing a SDWPException
     * @return  the class identified by <code>classID</code>
     * @throws SDWPException if there is no class corresponding to <code>classID</code>
     */
    Klass getClassForID(ReferenceTypeID classID, int errorCode) throws SDWPException {
        Assert.that(classToIdTable != null, "a class ID cannot exist before the class ID table is created");
        Klass klass = (Klass)idToClassTable.get(classID);
        if (klass == null) {
            throw new SDWPException(errorCode, "cannot resolve class for identifier: " + classID);
        }
        return klass;
    }

    /**
     * Gets a JDWP identifier for a given class, creating it first if necessary.
     *
     * @param klass  the class to query
     * @return  the JDWP identifier for <code>klass</code>
     */
    final ReferenceTypeID getIDForClass(Klass klass) {
        initializeClassIDTable();
        ReferenceTypeID typeID = (ReferenceTypeID)classToIdTable.get(klass);
        if (typeID == null) {
            synchronized(classToIdTable) {
                int id = classToIdTable.size() + 1;
                typeID = new ReferenceTypeID(id);
                classToIdTable.put(klass, typeID);
                idToClassTable.put(typeID, klass);
                Assert.that(classToIdTable.size() == idToClassTable.size());
            }
        }
        return typeID;
    }

    void writeAllClasses(PacketOutputStream out) throws IOException {
        initializeClassIDTable();
        int size = classToIdTable.size();
        out.writeInt(size, "classes");
        Enumeration classes = classToIdTable.keys();
        Enumeration ids = classToIdTable.elements();
        String lastName = "";
        for (int i = 0; i < size; i++) {
            Klass klass = (Klass)classes.nextElement();
            ReferenceTypeID id = (ReferenceTypeID)ids.nextElement();
            String name = klass.getInternalName();

            int commonPrefix = 0;
            int minLength = Math.min(name.length(), lastName.length());
            while (commonPrefix < minLength && name.charAt(commonPrefix) == lastName.charAt(commonPrefix)) {
                commonPrefix++;
            }

            if (commonPrefix > 0xFF) {
                commonPrefix = 0;
            }

            out.writeReferenceTypeID(id, "typeID");
            out.writeByte(commonPrefix, "commonPrefix");
            out.writeString(commonPrefix == 0 ? name : name.substring(commonPrefix), "name");
            out.writeInt(getClassStatus(klass), "status");

            lastName = name;
        }
    }

    /*-----------------------------------------------------------------------*\
     *                            Thread requests                            *
    \*-----------------------------------------------------------------------*/

    /**
     * Resume all the threads of the debuggee isolate.
     *
     * @return an abritary thread whose suspend count is 0
     */
    void resumeIsolate(boolean forDetach) {
        Enumeration e = debuggeeIsolate.getChildThreads();
        while (e.hasMoreElements()) {
            VMThread vmThread = (VMThread) e.nextElement();
            vmThread.resumeForDebugger(forDetach);
        }

        if (forDetach) {
            // Clear all breakpoints
            debuggeeIsolate.updateBreakpoints(null);
        }
    }

    /*-----------------------------------------------------------------------*\
     *                           Options                                     *
    \*-----------------------------------------------------------------------*/

    /**
     * The URL of the channel which the debugger agent listens on for SDWP & JDWP requests.
     */
    private String url;

    /*-----------------------------------------------------------------------*\
     *                           Listener Support                            *
    \*-----------------------------------------------------------------------*/

    /**
     * The connection to the Squawk Debug Proxy.
     */
    private SDPListener sdp;

    SDPListener getListener() {
        return sdp;
    }

    /*-----------------------------------------------------------------------*\
     *                     VMAgent interface methods                         *
    \*-----------------------------------------------------------------------*/

    Klass getClassForID(ReferenceTypeID id) throws SDWPException {
        return getClassForID(id, JDWP.Error_INVALID_CLASS);
    }

    Object getObjectForID(ObjectID id) throws SDWPException {
        return objectManager.getObjectForID(id);
    }

    ObjectID getIDForObject(Object object) {
        return objectManager.getIDForObject(object);
    }

    void suspendThreads(VMThread vmThread) {
        if (vmThread != null) {
            if (Log.info()) {
                Log.log("Suspending thread: " + vmThread.getName());
            }
            vmThread.suspendForDebugger();
        } else {
            if (Log.info()) {
                Log.log("Suspending all threads");
            }
            Enumeration e = debuggeeIsolate.getChildThreads();
            while (e.hasMoreElements()) {
                vmThread = (VMThread) e.nextElement();
                vmThread.suspendForDebugger();
            }
        }

        sendThreadStateChanged();
    }

    /**
     * Sends a message to the proxy informing it that the debugger status of one or more
     * threads has been changed. This enables the proxy to update its collection of
     * thread mirrors.
     */
    void sendThreadStateChanged() {
        CommandPacket command = new CommandPacket(SDWP.SquawkVM_COMMAND_SET, SDWP.SquawkVM_ThreadStateChanged_COMMAND, false);
        try {
            PacketOutputStream out = command.getOutputStream();
            writeThreadState(out);
            sdp.sendCommand(command);
        } catch (SDWPException e) {
            // This is an asynchronous send without a reply
            Assert.shouldNotReachHere();
        } catch (IOException e) {
            System.err.println("IO error when sending SquawkVM_ThreadStateChanged_COMMAND to proxy");
            e.printStackTrace();
        }
    }

    void writeThreadState(PacketOutputStream out) throws IOException {
        int count = debuggeeIsolate.getChildThreadCount();
        out.writeInt(count, "threads");
        for (Enumeration e = debuggeeIsolate.getChildThreads(); e.hasMoreElements(); ) {
            VMThread vmThread = (VMThread)e.nextElement();
            out.writeObjectID(getIDForObject(vmThread), "thread");
            out.writeInt(DebuggerSupport.getThreadJDWPState(vmThread), "state");
            out.writeInt(vmThread.getDebuggerSuspendCount(), "suspendCount");
            out.writeString(vmThread.getName(), "name");
        }
    }

    /**
     * {@inheritDoc}
     */
    void resumeThreads(VMThread thread) {

        if (thread != null) {
            if (Log.info()) {
                Log.log("Resuming thread: " + thread.getName());
            }
            thread.resumeForDebugger(false);
        } else {
            if (Log.info()) {
                Log.log("Resuming all threads");
            }
            resumeIsolate(false);
        }

        sendThreadStateChanged();
    }

    /**
     * {@inheritDoc}
     */
    int getClassStatus(Klass klass) {
        if (klass.isArray() || klass.isPrimitive() || klass.isSquawkPrimitive()) {
            return 0;
        }
        int status = JDWP.ClassStatus_VERIFIED | JDWP.ClassStatus_PREPARED | JDWP.ClassStatus_INITIALIZED;
        return status;
    }

    /*-----------------------------------------------------------------------*\
     *                           Isolate  Handling                           *
    \*-----------------------------------------------------------------------*/

    /**
     * Reference to application being debugged.
     */
    private Isolate debuggeeIsolate;

    /**
     * @return the isolate being debugged
     */
    Isolate getDebuggeeIsolate() {
        return debuggeeIsolate;
    }

    /**
     * Reference to the debugger agent isolate.
     */
    private Isolate debuggerIsolate;

    /*-----------------------------------------------------------------------*\
     *                           Object Management                           *
    \*-----------------------------------------------------------------------*/

    private ObjectManager objectManager;

    ObjectManager getObjectManager() {
        return objectManager;
    }

    /*-----------------------------------------------------------------------*\
     *                           Event Management                           *
    \*-----------------------------------------------------------------------*/

    class MatcherImpl implements EventRequestModifier.Matcher {

        /**
         * {@inheritDoc}
         */
        public boolean matches(ClassMatch modifier, EventNotifier notifier) {
            String name = null;
            switch (modifier.eventKind) {
                case JDWP.EventKind_CLASS_PREPARE:
                    name = ((Klass)notifier.getEvent().object).getName();
                    break;
                default:
                    if (!(notifier.getEvent() instanceof Debugger.LocationEvent)) {
                        if (Log.info()) {
                            Log.log("No location to match on for " + this);
                        }
                        return false;
                    }
                    name = DebuggerSupport.getDefiningClass(((Debugger.LocationEvent)notifier.getEvent()).location.mp).getName();
                    break;
            }

            boolean result = false;
            switch (modifier.matchKind) {
                case ClassMatch.EQUALS:      result = name.equals(modifier.pattern);        break;
                case ClassMatch.STARTS_WITH: result = name.startsWith(modifier.pattern);    break;
                case ClassMatch.ENDS_WITH:   result = name.endsWith(modifier.pattern);      break;
                case ClassMatch.CONTAINS:    result = name.indexOf(modifier.pattern) != -1; break;
                default: Assert.shouldNotReachHere();
            }
            return modifier.exclude ^ result;
        }

        /**
         * {@inheritDoc}
         */
        public boolean matches(ClassOnly modifier, EventNotifier notifier) {
            Klass klass;
            if (modifier.eventKind == JDWP.EventKind_CLASS_PREPARE) {
                klass = (Klass)notifier.getEvent().object;
            } else {
                if (! (notifier.getEvent() instanceof Debugger.LocationEvent)) {
                    if (Log.info()) {
                        Log.log("No location to match on for " + this);
                    }
                    return false;
                }
                klass = DebuggerSupport.getDefiningClass(((Debugger.LocationEvent)notifier.getEvent()).location.mp);
            }
            try {
                return getClassForID(modifier.clazz).isAssignableFrom(klass);
            } catch (SDWPException e) {
                System.err.println("Class ID in ClassOnly modifier is invalid: " + e);
                return false;
            }
        }

        /**
         * {@inheritDoc}
         */
        public boolean matches(ExceptionOnly modifier, EventNotifier notifier) {
            Debugger.ExceptionEvent event = (Debugger.ExceptionEvent)notifier.getEvent();
            if ((modifier.caught & event.isCaught) ||
                (modifier.uncaught & !event.isCaught)) {
                try {
                    Klass exceptionKlass = (modifier.exceptionOrNull.id == 0 ? null : getClassForID(modifier.exceptionOrNull));
                    return exceptionKlass == null || exceptionKlass.isInstance(event.object);
                } catch (SDWPException e) {
                    System.err.println("Class ID in ExceptionOnly modifier is invalid: " + e);
                    return false;
                }
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        public boolean matches(LocationOnly modifier, EventNotifier notifier) {
            Debugger.LocationEvent event = (Debugger.LocationEvent)notifier.getEvent();
            Klass definingClass = DebuggerSupport.getDefiningClass(event.location.mp);
            MethodID methodID = DebuggerSupport.getIDForMethodBody(definingClass, event.location.mp);
            Location location = modifier.location;
            try {
                return (definingClass == getClassForID(location.definingClass)) && (methodID.equals(location.method)) && (event.location.bci.toPrimitive() == location.offset);
            } catch (SDWPException e) {
                System.err.println("Class ID in LocationOnly modifier is invalid: " + e);
                return false;
            }
        }
    }

    class SDAEventManager extends EventManager {

        /**
         * Event IDs originating from the SDA will be even and events from the SDP will be odd.
         */
        private int nextEventID = 0;

        private Hashtable stepRequests = new Hashtable();

        SDAEventManager(EventRequestModifier.Matcher matcher) {
            super(matcher);
        }

        /**
         * Processes an event request packet.
         *
         * @param in   the packet
         * @return the ID of the registered event request
         */
        public int registerEventRequest(PacketInputStream in) throws IOException, SDWPException {
            int kind = in.readByte("eventKind");
            com.sun.squawk.debugger.EventRequest request;
            int id = nextEventID += 2;
            switch (kind) {
                case JDWP.EventKind_BREAKPOINT:      request = new Breakpoint(id, in);       break;
                case JDWP.EventKind_SINGLE_STEP:     SingleStep ss = new SingleStep(id, in);
                                                     stepRequests.put(ss.step.threadID, ss);
                                                     request = ss;                           break;

                case JDWP.EventKind_VM_INIT:         request = new VMStart(id, in);                break;
                case JDWP.EventKind_THREAD_START:
                case JDWP.EventKind_THREAD_END:      request = new ThreadStartOrEnd(id, in, kind); break;
                case JDWP.EventKind_VM_DEATH:        request = new VMDeath(id, in);                break;
                case JDWP.EventKind_EXCEPTION:       request = new ExceptionRequest(id, in);       break;
                case JDWP.EventKind_CLASS_PREPARE:   request = new ClassPrepare(id, in, kind);     break;


                case JDWP.EventKind_FRAME_POP:
                case JDWP.EventKind_USER_DEFINED:
                case JDWP.EventKind_CLASS_UNLOAD:
                case JDWP.EventKind_CLASS_LOAD:
                case JDWP.EventKind_FIELD_ACCESS:
                case JDWP.EventKind_FIELD_MODIFICATION:
                case JDWP.EventKind_EXCEPTION_CATCH:
                case JDWP.EventKind_METHOD_ENTRY:
                case JDWP.EventKind_METHOD_EXIT:     request = new Unsupported(id, in, kind);      break;
                default: throw new SDWPException(JDWP.Error_INVALID_EVENT_TYPE, "event kind = " + kind);
            }
            register(request);
            return id;
        }

        /**
         * {@inheritDoc}
         */
        public void send(EventNotifier notifier, MatchedRequests mr) throws IOException, SDWPException {

            if (notifier == null) {
                notifier = this.notifier;
            }

            // do thread suspension:
            if (mr.suspendPolicy != JDWP.SuspendPolicy_NONE) {
                VMThread vmThread = VMThread.asVMThread(notifier.getThread());
                suspendThreads(mr.suspendPolicy == JDWP.SuspendPolicy_ALL ? null : vmThread);
            }

            CommandPacket command = new CommandPacket(JDWP.Event_COMMAND_SET, JDWP.Event_Composite_COMMAND, false);
            PacketOutputStream out = command.getOutputStream();
            out.writeByte(mr.suspendPolicy, "suspendPolicy");
            out.writeInt(mr.requests.size(), "events");
            for (Enumeration e = mr.requests.elements(); e.hasMoreElements(); ) {
                SDAEventRequest request = (SDAEventRequest)e.nextElement();
                out.writeByte(request.kind, "eventKind");
                out.writeInt(request.id, "requestID");
                request.write(out, notifier, mr.suspendPolicy);

                if (Log.info()) {
                    Log.log("Added notification: " + request);
                }
            }

            // Clear the pending SingleStep requests for each thread that was suspended
            if (mr.suspendPolicy == JDWP.SuspendPolicy_ALL) {
                clear(JDWP.EventKind_SINGLE_STEP, 0);
            } else if (mr.suspendPolicy == JDWP.SuspendPolicy_EVENT_THREAD) {
                SingleStep ss = (SingleStep)stepRequests.remove(notifier.getThreadID());
                if (ss != null) {
                    clear(ss.kind, ss.id);
                }
            }

            if (Log.info()) {
                Log.log("Sent: " + command.toString(Log.debug()));
            }
            sdp.sendCommand(command);
        }

    }

    /**
     * The base class for event requests that are processed in the context of the SDA.
     */
    abstract class SDAEventRequest extends EventRequest {
        protected SDAEventRequest(int kind, int suspendPolicy) {
            super(kind, suspendPolicy);
        }
        protected SDAEventRequest(int id, PacketInputStream in, int kind) throws SDWPException, IOException {
            super(id, in, kind);
        }

        /**
         * {@inheritDoc}
         */
        protected EventRequestModifier readModifier(PacketInputStream in, int kind) throws SDWPException, IOException {
            int modKind = in.readByte("modKind");
            EventRequestModifier modifier;
            switch (modKind) {
                case JDWP.EventRequest_MOD_COUNT:          modifier = new Count(in);                    break;
                case JDWP.EventRequest_MOD_CLASS_ONLY:     modifier = new ClassOnly(in, kind);          break;
                case JDWP.EventRequest_MOD_CLASS_MATCH:    modifier = new ClassMatch(in, kind, false);  break;
                case JDWP.EventRequest_MOD_CLASS_EXCLUDE:  modifier = new ClassMatch(in, kind, true);   break;
                case JDWP.EventRequest_MOD_LOCATION_ONLY:  modifier = new LocationOnly(in, kind);       break;
                case JDWP.EventRequest_MOD_EXCEPTION_ONLY: modifier = new ExceptionOnly(in, kind);      break;
                case JDWP.EventRequest_MOD_STEP:           modifier = new Step(in, kind);               break;
                case JDWP.EventRequest_MOD_THREAD_ONLY:    modifier = new ThreadOnly(in, kind);         break;
                default: throw new SDWPException(JDWP.Error_NOT_IMPLEMENTED, "Unimplemented modkind " + modKind);
            }
            return modifier;
        }

        /**
         * Adds the event specific details to the packet delivering the event notification.
         *
         * @param suspendPolicy  the suspension triggered by the event
         */
        abstract void write(PacketOutputStream out, EventNotifier notifier, int suspendPolicy) throws IOException, SDWPException;
    }

    /**
     * This class encapsulates a request for notification of a <code>JDWP.EventKind.CLASS_PREPARE</code> event.
     */
    class ClassPrepare extends SDAEventRequest {

        /**
         * @see EventRequest#EventRequest(int, int)
         */
        public ClassPrepare(int suspendPolicy) {
            super(JDWP.EventKind_CLASS_PREPARE, suspendPolicy);
        }

        /**
         * @see EventRequest#EventRequest(PacketInputStream, EventManager.VMAgent, int)
         */
        public ClassPrepare(int id, PacketInputStream in, int kind) throws SDWPException, IOException {
            super(id, in, kind);
        }

        /**
         * {@inheritDoc}
         */
        public void write(PacketOutputStream out, EventNotifier notifier, int suspendPolicy) throws IOException {
            out.writeObjectID(notifier.getThreadID(), "thread");
            Klass klass = (Klass) notifier.getEvent().object;

            out.writeByte(JDWP.getTypeTag(klass), "refTypeTag");
            out.writeReferenceTypeID(getIDForClass(klass), "typeID");
            String sig = DebuggerSupport.getJNISignature(klass);
            out.writeString(sig, "signature");
            out.writeInt(getClassStatus(klass), "status");
        }
    }


    class Breakpoint extends SDAEventRequest {
        public Breakpoint(int id, PacketInputStream in) throws SDWPException, IOException {
            super(id, in, JDWP.EventKind_BREAKPOINT);
        }
        /**
         * {@inheritDoc}
         */
        public void registered() {
            updateBreakpoints();
        }

        /**
         * {@inheritDoc}
         */
        public void cleared() {
            updateBreakpoints();
        }

        /**
         * {@inheritDoc}
         */
        public boolean matchKind(int eventKind) {
            return eventKind == Debugger.Event.BREAKPOINT || eventKind == Debugger.Event.SINGLE_STEP;
        }

        /**
         * {@inheritDoc}
         */
        public void write(PacketOutputStream out, EventNotifier notifier, int suspendPolicy) throws IOException {
            out.writeObjectID(notifier.getThreadID(),"thread");

            // location:
            Debugger.LocationEvent event = (Debugger.LocationEvent)notifier.getEvent();
            Assert.that(event != null);
            Klass definingKlass = DebuggerSupport.getDefiningClass(event.location.mp);
            MethodID methodID = DebuggerSupport.getIDForMethodBody(definingKlass, event.location.mp);
            ReferenceTypeID typeID = getIDForClass(definingKlass);
            Location location = new Location(JDWP.getTypeTag(definingKlass), typeID, methodID, event.location.bci.toPrimitive());
            out.writeLocation(location, "location");
            if (Log.debug()) {
                Log.log("   Breakpoint in : " + definingKlass + "." + methodID + ":" + event.location.bci.toPrimitive());
            }
        }

    }

    class SingleStep extends SDAEventRequest {

        final Step step;

        /**
         * @see EventRequest#EventRequest(PacketInputStream, EventManager.VMAgent, int)
         */
        public SingleStep(int id, PacketInputStream in) throws SDWPException, IOException {
            super(id, in, JDWP.EventKind_SINGLE_STEP);

            if (Log.debug()) {
                Log.log("[SingleStep] Creating SingleStep...");
            }
            step = getStep();
            VMThread steppingThread = objectManager.getThreadForID(step.threadID);

            // Cannot issue a single step for a thread until a previous single step has completed.
            // No sensible debugger client should do this anyway.
            if (steppingThread.getStep() != null) {
                throw new SDWPException(JDWP.Error_INVALID_THREAD, "thread already has a pending step event");
            }

            if (steppingThread.getDebuggerSuspendCount() == 0) {
                throw new SDWPException(JDWP.Error_INVALID_THREAD, "cannot step a non-suspended thread");
            }

            ExecutionPoint start = steppingThread.getEventExecutionPoint();
            Assert.that(start != null);
            Klass definingClass = DebuggerSupport.getDefiningClass(start.mp);

            CommandPacket sdpCommand = new CommandPacket(SDWP.SquawkVM_COMMAND_SET, SDWP.SquawkVM_SteppingInfo_COMMAND, true);
            PacketOutputStream sdpOut = sdpCommand.getOutputStream();

            /* Send this data to proxy and expect the rest of the stepping info in return */
            sdpOut.writeReferenceTypeID(getIDForClass(definingClass), "refType");
            sdpOut.writeMethodID(DebuggerSupport.getIDForMethodBody(definingClass, start.mp), "method");
            sdpOut.writeLong(start.bci.toPrimitive(), "bci");
            sdp.sendCommand(sdpCommand);
            if (Log.debug()) {
                Log.log("[SingleStep] Received reply from proxy, constructing stepInfo");
            }
            /* Read reply packet and call on debugger to pass this on to the stepThread */
            ReplyPacket sdpReply = sdpCommand.getReply();
            Assert.that(sdpReply.getErrorCode() == JDWP.Error_NONE, "Error in response from proxy: " + sdpReply.getErrorCode());
            PacketInputStream sdpIn = sdpReply.getInputStream();
            int targeBCI = (int)sdpIn.readLong("targetBCI");
            int dupBCI = (int)sdpIn.readLong("dupBCI");
            int afterDupBCI = (int)sdpIn.readLong("afterDupBCI");

            steppingThread.setStep(new Debugger.SingleStep(start.frame, start.bci, targeBCI, dupBCI, afterDupBCI, step.size, step.depth));
        }

        /**
         * {@inheritDoc}
         */
        public boolean matchKind(int eventKind) {
            return eventKind == Debugger.Event.BREAKPOINT || eventKind == Debugger.Event.SINGLE_STEP;
        }

        /**
         * {@inheritDoc}
         */
        public void cleared() {
            try {
                VMThread steppingThread = objectManager.getThreadForID(step.threadID);
                steppingThread.clearStep();
                return;
            } catch (SDWPException e) {
                if (Log.info()) {
                    Log.log("cannot find thread while clearing single step");
                }
            }
        }

        /**
         * Iterates through the modifiers and gets the step modifier.
         *
         * @throws SDWPException if there is no Step modifier
         */
        private Step getStep() throws SDWPException {
            EventRequestModifier mod;
            int len = modifiers.length;
            Step sm = null;
            for (int i = 0; i < len; i++) {
                mod = modifiers[i];
                if (mod instanceof Step) {
                    sm = (Step)mod;
                    break;
                }
            }
            if (sm == null) {
                throw new SDWPException(JDWP.Error_ABSENT_INFORMATION, "no valid modifier provided for step command");
            }
            return sm;
        }

        /**
         * {@inheritDoc}
         */
        public void write(PacketOutputStream out, EventNotifier notifier, int suspendPolicy) throws IOException {
            /* thread */
            out.writeObjectID(notifier.getThreadID(), "thread");

            /* location: */
            Debugger.LocationEvent event = (Debugger.LocationEvent)notifier.getEvent();
            Klass definingKlass = DebuggerSupport.getDefiningClass(event.location.mp);
            MethodID methodID = DebuggerSupport.getIDForMethodBody(definingKlass, event.location.mp);
            ReferenceTypeID typeID = getIDForClass(definingKlass);
            Location location = new Location(JDWP.getTypeTag(definingKlass), typeID, methodID, event.location.bci.toPrimitive());
            out.writeLocation(location, "location");

            if (Log.debug()) {
                Log.log("   Stepped to : " + definingKlass + "." + methodID + ":" + event.location.bci.toPrimitive());
            }
        }
    }

    /**
     * This class encapsulates a request for notification of a <code>JDWP.EventKind.THREAD_START</code> or
     * <code>JDWP.EventKind.THREAD_END</code> event.
     */
    class ThreadStartOrEnd extends SDAEventRequest {

        public ThreadStartOrEnd(int kind, int suspendPolicy) {
            super(kind, suspendPolicy);
        }

        public ThreadStartOrEnd(int id, PacketInputStream in, int kind) throws SDWPException, IOException {
            super(id, in, kind);
        }

        /**
         * {@inheritDoc}
         */
        public void write(PacketOutputStream out, EventNotifier notifier, int suspendPolicy) throws IOException {
            VMThread vmThread = (VMThread)notifier.getEvent().object;
            ObjectID threadID = getIDForObject(vmThread);
            out.writeObjectID(threadID, "thread");
            if (id == 0) {
                out.writeInt(DebuggerSupport.getThreadJDWPState(vmThread), "state");
                out.writeInt(vmThread.getDebuggerSuspendCount(), "suspendCount");
                out.writeString(vmThread.getName(), "name");
            }
        }
    }

    /**
     * This class encapsulates a request for notification of a <code>JDWP.EventKind.VM_INIT</code> event.
     */
    class VMStart extends SDAEventRequest {

        public VMStart(int suspendPolicy) {
            super(JDWP.EventKind_VM_START, suspendPolicy);
        }

        public VMStart(int id, PacketInputStream in) throws SDWPException, IOException {
            super(id, in, JDWP.EventKind_VM_START);
        }

        /**
         * {@inheritDoc}
         */
        public void write(PacketOutputStream out, EventNotifier notifier, int suspendPolicy) throws IOException {
            Debugger.Event event = notifier.getEvent();
            VMThread vmThread;
            if (event == null) {
                // This occurs when the VM_INIT notification is being sent after re-attaching
                // to an already running isolate
                vmThread = null;
            } else {
                vmThread = (VMThread) event.object;
            }
            ObjectID threadID = getIDForObject(vmThread);
            out.writeObjectID(threadID, "thread");
            if (id == 0) {
                // If the VM just started then the currently loaded classes are sent to the proxy
                // so that its triggers the CLASS_PREPARE events
                writeAllClasses(out);
            }
        }
    }

    /**
     * This class encapsulates a request for notification of a <code>JDWP.EventKind.EXCEPTION</code> event.
     */
    class ExceptionRequest extends SDAEventRequest {

        public ExceptionRequest(int id, PacketInputStream in) throws SDWPException, IOException {
            super(id, in, JDWP.EventKind_EXCEPTION);
        }

        /**
         * {@inheritDoc}
         */
        public void write(PacketOutputStream out, EventNotifier notifier, int suspendPolicy) throws IOException {
            out.writeObjectID(notifier.getThreadID(), "thread");

            // throw location:
            Debugger.ExceptionEvent event = (Debugger.ExceptionEvent)notifier.getEvent();
            Klass definingKlass = DebuggerSupport.getDefiningClass(event.location.mp);
            MethodID methodID = DebuggerSupport.getIDForMethodBody(definingKlass, event.location.mp);
            ReferenceTypeID typeID = getIDForClass(definingKlass);
            Location location = new Location(JDWP.getTypeTag(definingKlass), typeID, methodID, event.location.bci.toPrimitive());
            out.writeLocation(location, "location");

            if (Log.debug()) {
                Log.log("   Throw from : " + definingKlass + "." + methodID + ":" + event.location.bci.toPrimitive());
            }
            // exception:
            Assert.that(event.object instanceof Throwable);
            out.writeTaggedObjectID(new TaggedObjectID(JDWP.Tag_OBJECT, getIDForObject(event.object).id), "exception");

            // catch location:
            if (!event.isCaught) {
                out.writeNullLocation("catchLocation");
            } else {
                Klass catchType = DebuggerSupport.getDefiningClass(event.catchLocation.mp);
                MethodID catchMethodID = DebuggerSupport.getIDForMethodBody(catchType, event.catchLocation.mp);
                ReferenceTypeID catchTypeID = getIDForClass(catchType);
                location = new Location(JDWP.getTypeTag(catchType), catchTypeID, catchMethodID, event.catchLocation.bci.toPrimitive());
                out.writeLocation(location, "catchLocation");
                if (Log.debug()) {
                    Log.log("   Catch at : " + catchType + "." + catchMethodID + ":" + event.catchLocation.bci.toPrimitive());
                }
            }
        }
    }

    /**
     * This class encapsulates a request to be notified when the VM exits. For Squawk,
     * this translates to the isolate being debugged exiting.
     */
    class VMDeath extends SDAEventRequest {

        public VMDeath(int suspendPolicy) {
            super(JDWP.EventKind_VM_DEATH, suspendPolicy);
        }

        public VMDeath(int id, PacketInputStream in) throws SDWPException, IOException {
            super(id, in, JDWP.EventKind_VM_DEATH);
        }

        /**
         * {@inheritDoc}
         */
        public boolean matchKind(int eventKind) {
            return eventKind == Debugger.Event.VM_DEATH ||
                   (eventKind == Debugger.Event.THREAD_DEATH &&
                    debuggeeIsolate.getChildThreadCount() <= 1);
        }

        /**
         * {@inheritDoc}
         */
        public void write(PacketOutputStream out, EventNotifier notifier, int suspendPolicy) throws IOException {
        }
    }

    /**
     * This class encapsulates a request for notification of an event kind that is not applicable to Squawk.
     * These events are registered so that a EventRequest.Set command from a debugger client is
     * successful. It also means that the debugger can later clear the event.
     */
    class Unsupported extends SDAEventRequest {
        public Unsupported(int id, PacketInputStream in, int kind) throws SDWPException, IOException {
            super(id, in, kind);
        }

        /**
         * {@inheritDoc}
         */
        public void write(PacketOutputStream out, EventNotifier notifier, int suspendPolicy) throws IOException {
            Assert.shouldNotReachHere();
        }

        /**
         * {@inheritDoc}
         */
        public boolean matchKind(int eventKind) {
            return false;
        }
    }

    private SDAEventManager eventManager;

    SDAEventManager getEventManager() {
        return eventManager;
    }


    private Debugger.Event currentEvent;

    /**
     * {@inheritDoc}
     */
    public void notifyEvent(Debugger.Event event) {
        // Can not recursively notify events. A recursive event would
        // occur for example if there was a breakpoint inside Thread.reschedule.
        // Recursive events are simply ignored.
        if (currentEvent == null) {
            currentEvent = event;
            Thread thread = Thread.currentThread();
            ObjectID threadID = objectManager.getIDForObject(VMThread.asVMThread(thread));
            eventManager.produceEvent(event, thread, threadID);
            currentEvent = null;
        }
    }

    /**
     * Updates the list of set breakpoints in the debuggee isolate.
     */
    public void updateBreakpoints() {
        Enumeration e = eventManager.getEventsOfKind(JDWP.EventKind_BREAKPOINT);
        if (!e.hasMoreElements()) {
            debuggeeIsolate.updateBreakpoints(null);
            return;
        }

        Vector breakpoints = new Vector(5);
        while (e.hasMoreElements()) {
            EventRequest request = (EventRequest)e.nextElement();
            EventRequestModifier[] mods = request.modifiers;
            for (int j = 0; j < mods.length; j++) {
                if (mods[j] instanceof LocationOnly) {
                    LocationOnly loc = (LocationOnly)mods[j];
                    DataType.Location location = loc.location;
                    MethodID methodID = location.method;
                    try {
                        Klass definingClass = getClassForID(loc.location.definingClass);
                        Object method = DebuggerSupport.getMethodBody(definingClass, methodID.getOffset(), methodID.isStatic());
                        long offset = location.offset;

                        Isolate.Breakpoint bp = new Isolate.Breakpoint(method, (int) offset);
                        if (!breakpoints.contains(bp)) {
                            breakpoints.addElement(bp);
                        }
                    } catch (SDWPException ex) {
                        if (Log.info()) {
                            Log.log("Error while updating breakpoints: " + ex);
                        }
                    }
                }
            }
        }

        Isolate.Breakpoint[] bps = new Isolate.Breakpoint[breakpoints.size()];
        breakpoints.copyInto(bps);
        debuggeeIsolate.updateBreakpoints(bps);

/*if[DEBUG_CODE_ENABLED]*/
        for (int i = 0; i < bps.length; i++) {
            VM.print("updateBreakpoints: bp# ");
            VM.print(i);
            VM.print(" method: ");
            VM.printAddress(bps[i].mp);
            VM.print(" offsets: ");
            VM.println(bps[i].ip);
        }
/*end[DEBUG_CODE_ENABLED]*/

    }

    /*-----------------------------------------------------------------------*\
     *                              Startup                                  *
    \*-----------------------------------------------------------------------*/

    /**
     * Create Squawk Debugger Agent object.
     */
    private SDA() {
        debuggerIsolate = VM.getCurrentIsolate();
    }

    private static final String DEFAULT_MAINCLASSNAME = "tests.TestApp";
    private static final String DEFAULT_APPCLASSPATH = "file://.";
    private static final String DEFAULT_URL = "serversocket://:2800;acceptTimeout=2000";

    /**
     * Prepends "file://" to an arg if it does not contain a ':' character.
     */
    private static String makeURL(String arg) {
        if (arg.indexOf(':') == -1) {
            arg = "file://" + arg;
        }
        return arg;
    }

    /**
     * Parses the command line arguments to configure this debugger agent and initializes the
     * debuggee application isolate.
     *
     * @param args  the command line arguments
     * @return      true if there were no errors in the arguments and the debuggee application isolate was initialized
     */
    boolean parseArgs(String args[]) {

        // This should be initialized to null after TestApp has served its purpose...
        String mainClassName = DEFAULT_MAINCLASSNAME;
        String[] mainClassArgs = {};
        String appSuite = VMThread.currentThread().getIsolate().getParentSuiteSourceURI();
        String logLevel = "none";
        String logURL = null;
        String appClassPath = DEFAULT_APPCLASSPATH;
        url = DEFAULT_URL;
        String outURL = null;
        String errURL = null;

        int argc = 0;
        while (argc != args.length) {
            String arg = args[argc];
            try {
                if (arg.charAt(0) != '-') {
                    break;
                } else if (arg.startsWith("-cp:")) {
                    appClassPath = arg.substring("-cp:".length());
                } else if (arg.startsWith("-suite:")) {
                    appSuite = arg.substring("-suite:".length());
                    // If the -suite arg does not look like a URI, convert it
                    // to be a file URI relative to the current directory
                    if (appSuite.indexOf(':') == -1) {
                        appSuite = "file://" + appSuite + ".suite";
                    }
                } else if (arg.startsWith("-url:")) {
                    url = arg.substring("-url:".length());
                } else if (arg.startsWith("-log:")) {
                    logLevel = arg.substring("-log:".length());
                } else if (arg.startsWith("-logURL:")) {
                    logURL = makeURL(arg.substring("-logURL:".length()));
                } else if (arg.startsWith("-outURL:")) {
                    outURL = makeURL(arg.substring("-outURL:".length()));
                } else if (arg.startsWith("-errURL:")) {
                    errURL = makeURL(arg.substring("-errURL:".length()));
                } else if (arg.equals("-h")) {
                    usage(null);
                    return false;
                } else {
                    usage("Unknown option: " + arg);
                    return false;
                }
            } catch (NumberFormatException e) {
                System.err.println("Badly formatted option: " + arg);
                return false;
            }
            ++argc;
        }

        debuggerIsolate.setProperty("squawk.debugger.log.level", logLevel);
        if (logURL != null) {
            // Both the debugger and debuggee isolates will write to the same log stream.
            // So if the log stream is a file:// connection, it must be opened in append
            // mode so that logging from each isolate is preserved
            if (logURL.indexOf("file://") != -1) {
                // Clear log file
                try {
                    Connector.openOutputStream(logURL).close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (logURL.indexOf("append=true") == -1) {
                    logURL += ";append=true";
                }
            }
            debuggerIsolate.setProperty("squawk.debugger.log.url", logURL);
        }

        if (argc != args.length) {
            mainClassName = args[argc++];
            if (argc != args.length) {
                int argsCount = args.length - argc;
                mainClassArgs = new String[argsCount];
                System.arraycopy(args, argc, mainClassArgs, 0, argsCount);
            }
        }

        if (mainClassName == null) {
            usage("Missing application main class name");
            return false;
        }


        debuggeeIsolate = new Isolate(mainClassName, mainClassArgs, appClassPath, appSuite);
        debuggeeIsolate.setProperty("squawk.debugger.log.level", logLevel);
        if (logURL != null) {
            debuggeeIsolate.setProperty("squawk.debugger.log.url", logURL);
        }

        if (outURL != null) {
            debuggeeIsolate.clearOut();
            if (outURL.length() > 0) {
                debuggeeIsolate.addOut(outURL);
            }
        }

        if (errURL != null) {
            debuggeeIsolate.clearErr();
            if (errURL.length() > 0) {
                debuggeeIsolate.addErr(errURL);
            }
        }
        return true;
    }

    /**
     * Prints a usage message to the console.
     *
     * @param errMsg  an optional error message to print first
     */
    private void usage(String errMsg) {
        PrintStream out = System.out;
        if (errMsg != null) {
            out.println(errMsg);
        }
        out.println("Usage: SDA [-options] class [args...]");
        out.println("where options include:");
        out.println();
        out.println("    -cp:<path>     specifies the class path for the application");
        out.println("                   (default is " + DEFAULT_APPCLASSPATH + ")");
        out.println("    -suite:<suite> specifies the suite containing the application");
        out.println("    -url:<url>     specifies the URL of the channel that the debug agent will");
        out.println("                   listen on for a connection from a debugger proxy.");
        out.println("                   (default is " + DEFAULT_URL + ")");
        out.println("    -log:<level>   sets logging level to 'none, 'info', 'verbose' or 'debug'");
        out.println("    -logURL:<url>  where messages should be logged (default is stdout)");
        out.println("    -outURL:<url>  where the debugee's output stream should be logged. If no <url>");
        out.println("                   is specified, then output is dropped. (default is stdout)");
        out.println("    -errURL:<url>  where the debugee's err stream should be logged. If no <url>");
        out.println("                   is specified, then err is dropped. (default is stderr)");
        out.println("    -h             shows this usage message");
        out.println();
    }

    /**
     * Starts a single debug session between this VM and a debug client. Returns
     * when the session is closed from either end.
     *
     * @return true if the application is still running
     */
    boolean go() {

        if (debuggeeIsolate.isHibernated() || debuggeeIsolate.isExited()) {
            return false;
        }

        // If you wish to have the debugee start without waiting, then use _NONE
        int defaultSuspendPolicy = JDWP.SuspendPolicy_ALL;
//        int defaultSuspendPolicy = JDWP.SuspendPolicy_NONE;
        
        VMStart autoVMStartEvent = new VMStart(JDWP.SuspendPolicy_ALL);
        objectManager = new ObjectManager();
        eventManager = new SDAEventManager(new MatcherImpl());
        eventManager.register(autoVMStartEvent);
        eventManager.register(new ThreadStartOrEnd(JDWP.EventKind_THREAD_START, JDWP.SuspendPolicy_NONE));
        eventManager.register(new ThreadStartOrEnd(JDWP.EventKind_THREAD_END, JDWP.SuspendPolicy_NONE));
        eventManager.register(new VMDeath(JDWP.SuspendPolicy_NONE));
        eventManager.register(new ClassPrepare(JDWP.SuspendPolicy_NONE));
        
        // If we do not which to suspend on start, then lets just go ahead and start the debuggee now.
        if (defaultSuspendPolicy == JDWP.SuspendPolicy_NONE && debuggeeIsolate.isNew()) {
            debuggeeIsolate.start();
        }

        Thread evtThread = new Thread(eventManager, "EventManager");

        while (true) {
            try {
                sdp = new SDPListener(this);
                System.out.println("Listening for connection from proxy on " + url);
                sdp.open(url, "SDWP-Handshake".getBytes(), false, false);
                break;
            } catch (InterruptedIOException e) {
                // Time-out waiting for accept
                if (debuggeeIsolate.isHibernated() || debuggeeIsolate.isExited()) {
                    return false;
                }
            } catch (IOException e) {
                System.out.println("Failed to establish connection to proxy: " + e.getMessage());
                e.printStackTrace();
                return e.getMessage().indexOf("BindException") == -1;
            }
        }

        try {
            //
            // At this point we have successfully connected the debugger
            // to the proxy. we can now sit back and
            // wait for packets to start flowing.
            //
            Thread sdpThread = new Thread(sdp, "SDPListener");
            sdpThread.start();

            // start up event manager thread to handle events from the isolate:
            evtThread.start();

            // Start or attach to the debuggee application
            DebuggerSupport.setDebugger(debuggeeIsolate, this, true);
            if (debuggeeIsolate.isNew()) {
                if (Log.info()) {
                    Log.log("Starting application: " + getMainAndArgs(debuggeeIsolate));
                }
                debuggeeIsolate.start();
            } else {
                if (Log.info()) {
                    Log.log("Attaching to application: " + getMainAndArgs(debuggeeIsolate));
                }

                // Send an event notification for the VM_INIT event that enables the proxy
                // and debugger to sync up with the loaded classes and current threads in
                // the running application that has just been re-attached.
                try {
                    Vector oneEvent = new Vector(1);
                    oneEvent.addElement(autoVMStartEvent);
                    eventManager.send(null, new EventManager.MatchedRequests(oneEvent, false, JDWP.SuspendPolicy_ALL));
                } catch (SDWPException e) {
                    System.out.println("Error while sending VM_INIT to proxy: " + e.getMessage());
                    e.printStackTrace();
                } catch (IOException e) {
                    System.out.println("IO error while sending VM_INIT to proxy: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // Wait until the remote debugger disconnects
            try {
                sdpThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        } finally {
            if (Log.info()) {
                Log.log("Detaching debuggee isolate...");
            }
            // Detach from the debuggee isolate and allow it continue
            resumeIsolate(true);
            DebuggerSupport.setDebugger(debuggeeIsolate, this, false);

            sdp.quit();
        }

        // Wait until the event manager finishes. This must happen after
        // the debuggee has been detached and allowed to continue as it
        // may have a thread holding a monitor required by the event manager
        try {
            eventManager.quit();
            evtThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (Log.info()) {
            Log.log("Completed shutdown");
        }

        return debuggeeIsolate.isAlive();
    }

    private static String getMainAndArgs(Isolate isolate) {
        StringBuffer buf = new StringBuffer(isolate.getMainClassName());
        String[] args = isolate.getMainClassArguments();
        for (int i = 0; i != args.length; ++i) {
            buf.append(' ').append(args[i]);
        }
        return buf.toString();
    }

    /**
     * Program entry point.
     *
     * @param args  command line arguments
     * @throws IOException
     */
    public static void main(String args[]) {
        Assert.always(!VM.isHosted(), "Squawk Debugger Agent can only run on a native Squawk VM, not hosted");
        VMThread.currentThread().setName("SDA");

        SDA debugger = new SDA();
        if (!debugger.parseArgs(args)) {
            System.exit(1);
        }

        try {
            while (debugger.go()) {
            }
        } catch (RuntimeException e) {
            if (debugger.sdp != null) {
                debugger.sdp.quit();
            }
            throw e;
        } catch (Error e) {
            if (debugger.sdp != null) {
                debugger.sdp.quit();
            }
            throw e;
        } finally {
            System.out.println("Debug agent disconnected from application");
        }
        // Make sure we exit with the exit code of the debuggee and not the debugger
        if (debugger.getDebuggeeIsolate() != null) {
            System.exit(debugger.getDebuggeeIsolate().getExitCode());
        }
    }
}
