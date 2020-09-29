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

import com.sun.squawk.*;
import com.sun.squawk.DebuggerSupport;
import com.sun.squawk.VMThread.*;
import com.sun.squawk.debugger.*;
import com.sun.squawk.debugger.DataType.*;
import com.sun.squawk.util.*;
import com.sun.squawk.vm.*;

/**
 * A SDPListener implements the Squawk VM side of the JDWP protocol
 * and communicates with a Squawk Debugger Proxy.
 */
class SDPListener extends JDWPListener {

    /**
     * The agent that interacts with the application being debugged.
     */
    private final SDA sda;

    /**
     * The JDWP command set handlers.
     */
    private final IntHashtable commandSets = new IntHashtable();

    public SDPListener(SDA debugger) {
        super();
        this.sda = debugger;

        commandSets.put(JDWP.VirtualMachine_COMMAND_SET,       new VirtualMachine());
        commandSets.put(JDWP.ReferenceType_COMMAND_SET,        new ReferenceType());
        commandSets.put(JDWP.ObjectReference_COMMAND_SET,      new ObjectReference());
        commandSets.put(JDWP.ClassType_COMMAND_SET,            new ClassType());
        commandSets.put(JDWP.EventRequest_COMMAND_SET,         new EventRequest());
        commandSets.put(JDWP.StackFrame_COMMAND_SET,           new StackFrame());
        commandSets.put(JDWP.StringReference_COMMAND_SET,      new StringReference());
        commandSets.put(JDWP.ThreadReference_COMMAND_SET,      new ThreadReference());
        commandSets.put(JDWP.ArrayReference_COMMAND_SET,       new ArrayReference());
        commandSets.put(JDWP.ClassObjectReference_COMMAND_SET, new ClassObjectReference());

        commandSets.put(SDWP.SquawkVM_COMMAND_SET,             new SquawkVM());
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void processCommand(CommandPacket command) throws IOException {
        SDACommandSet handler = (SDACommandSet) commandSets.get(command.set());
        if (handler == null || !handler.handle(this, sda, command)) {
            ReplyPacket reply = command.createReply(JDWP.Error_INTERNAL);
            sendReply(reply);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "SDPListener";
    }

    /*-----------------------------------------------------------------------*\
     *                            SDA Command Set                            *
    \*-----------------------------------------------------------------------*/

    /**
     * Abstract base class for command set handlers that execute in the context of an SDA instance.
     */
    static abstract class SDACommandSet extends CommandSet {
        SDA sda;

        /**
         * Handles a command packet by setting up the variables used to interpret and reply
         * to the command and then dispatching to the specific command handler.
         *
         * @param sda      the SDA context
         * @param command  the command to be handled
         * @return boolean true if the command was recognised and a reply was sent
         * @throws IOException if there was an IO error while sending a reply
         */
        public boolean handle(JDWPListener listener, SDA sda, CommandPacket command) throws IOException {
            this.sda = sda;
            return handle(listener, command);
        }
    }

    /*-----------------------------------------------------------------------*\
     *                      VirtualMachine Command Set (1)                   *
    \*-----------------------------------------------------------------------*/

    /**
     * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_VirtualMachine">VirtualMachine</a>.
     */
    static final class VirtualMachine extends SDACommandSet {

        protected boolean dispatch() throws IOException {
            switch (command.command()) {

                // Implemented commands
//                case JDWP.VirtualMachine_AllThreads_COMMAND:         AllThreads();         break;
                case JDWP.VirtualMachine_Suspend_COMMAND:            Suspend();            break;
                case JDWP.VirtualMachine_Resume_COMMAND:             Resume();             break;
                case JDWP.VirtualMachine_Dispose_COMMAND:            Dispose();            break;
                case JDWP.VirtualMachine_CreateString_COMMAND:       CreateString();       break;
                case JDWP.VirtualMachine_ClassesBySignature_COMMAND: ClassesBySignature(); break;

                // Unimplemented commands
                case JDWP.VirtualMachine_AllThreads_COMMAND:
                case JDWP.VirtualMachine_AllClasses_COMMAND:
                case JDWP.VirtualMachine_Version_COMMAND:
                case JDWP.VirtualMachine_TopLevelThreadGroups_COMMAND:
                case JDWP.VirtualMachine_IDSizes_COMMAND:
                case JDWP.VirtualMachine_Capabilities_COMMAND:
                case JDWP.VirtualMachine_ClassPaths_COMMAND:
                case JDWP.VirtualMachine_DisposeObjects_COMMAND:
                case JDWP.VirtualMachine_HoldEvents_COMMAND:
                case JDWP.VirtualMachine_Exit_COMMAND:
                case JDWP.VirtualMachine_ReleaseEvents_COMMAND:      unimplemented();      break;

                default: return false;
            }
            return true;
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_VirtualMachine_ClassesBySignature">ClassesBySignature</a>.
         */
        private void ClassesBySignature() throws IOException {
            String sig = in.readString("signature");
            Enumeration e = sda.getClasses();
            while (e.hasMoreElements()) {
                Klass klass = (Klass)e.nextElement();
                if (klass.getSignature().equals(sig)) {
                    out.writeInt(1, "classes");
                    out.writeByte(JDWP.getTypeTag(klass), "refTypeTag");
                    out.writeReferenceTypeID(sda.getIDForClass(klass), "typeID");
                    out.writeInt(JDWP.ClassStatus_VERIFIED_PREPARED_INITIALIZED, "status");
                    return;
                }
            }
            out.writeInt(0, "classes");
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_VirtualMachine_Resume">Resume</a>.
         */
        private void Resume() {
            sda.resumeThreads(null);
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_VirtualMachine_AllThreads">AllThreads</a>
         */
//        private void AllThreads() throws IOException {
//            Isolate debuggeeIsolate = sda.getDebuggeeIsolate();
//            int count = debuggeeIsolate.getChildThreadCount();
//            out.writeInt(count, "threads");
//            for (Enumeration e = debuggeeIsolate.getChildThreads(); e.hasMoreElements(); ) {
//                VMThread thread = (Thread)e.nextElement();
//                ObjectID threadID = sda.getObjectManager().getIDForObject(thread);
//                out.writeObjectID(threadID, "thread");
//            }
//        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_VirtualMachine_Suspend">Suspend</a>
         */
        private void Suspend() {
            sda.suspendThreads(null);
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_VirtualMachine_Dispose">Dispose</a>.
         */
        private void Dispose() throws IOException {
            sda.resumeIsolate(true);
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_VirtualMachine_CreateString">CreateString</a>.
         */
        private void CreateString() throws IOException {
            String s = in.readString("utf");
            out.writeObjectID(sda.getObjectManager().getIDForObject(s), "stringObject");
        }
    }

    /*-----------------------------------------------------------------------*\
     *                      ReferenceType Command Set (2)                    *
    \*-----------------------------------------------------------------------*/

    /**
     * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_ReferenceType">ReferenceType</a>.
     */
    static final class ReferenceType extends SDACommandSet {

        /**
         * {@inheritDoc}
         */
        protected boolean dispatch() throws IOException, SDWPException {
            ReferenceTypeID typeID = in.readReferenceTypeID("refType");
            Klass refType = sda.getClassForID(typeID, JDWP.Error_INVALID_CLASS);
            switch (command.command()) {

                // Implemented commands
                case JDWP.ReferenceType_Signature_COMMAND:   Signature(refType);     break;
                case JDWP.ReferenceType_GetValues_COMMAND:   GetValues(refType);     break;
                case JDWP.ReferenceType_ClassObject_COMMAND: ClassObject(refType);   break;

                // Unimplemented commands
                case JDWP.ReferenceType_Status_COMMAND:
                case JDWP.ReferenceType_ClassLoader_COMMAND:
                case JDWP.ReferenceType_Modifiers_COMMAND:
                case JDWP.ReferenceType_Fields_COMMAND:
                case JDWP.ReferenceType_Methods_COMMAND:
                case JDWP.ReferenceType_SourceFile_COMMAND:
                case JDWP.ReferenceType_NestedTypes_COMMAND:
                case JDWP.ReferenceType_Interfaces_COMMAND:  unimplemented();        break;
                default: return false;
            }
            return true;
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_ReferenceType_ClassObject">ClassObject</a>.
         */
        private void ClassObject(Klass refType) throws IOException {
            out.writeObjectID(sda.getObjectManager().getIDForObject(refType), "classObject");
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_ReferenceType_Signature">Signature</a>.
         */
        private void Signature(Klass klass) throws IOException {
            out.writeString(klass.getSignature(), "signature");
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_ReferenceType_GetValues">GetValues</a>.
         */
        private void GetValues(Klass klass) throws SDWPException, IOException {
            Isolate isolate = sda.getDebuggeeIsolate();
            int count = in.readInt("fields");
            out.writeInt(count, "values");
            for (int i = 0; i < count; i++) {
                FieldID fieldID = in.readFieldID("fieldID");
                Klass definingClass = sda.getClassForID(fieldID.definingClass, JDWP.Error_INVALID_CLASS);
                int offset = fieldID.getOffset();
                byte tag = fieldID.getTag();
                if (!fieldID.isStatic()) {
                    throw new SDWPException(JDWP.Error_INVALID_FIELDID, "field ID denotes an instance field");
                }

                if (Log.debug()) {
                    Log.log("    static field in " + klass + " of type " + (char) tag + " at offset " + offset);
                }
                // This should only occur if the proxy did not intercept a constant field or
                // it did not mask a global field from the debugger
                Assert.that(offset < definingClass.getStaticFieldsSize(), "field offset is out of range");

                switch (tag) {
                    case JDWP.Tag_BYTE:
                    case JDWP.Tag_BOOLEAN:
                    case JDWP.Tag_CHAR:
                    case JDWP.Tag_SHORT:
                    case JDWP.Tag_INT:
                    case JDWP.Tag_FLOAT:
                        out.writePrimitive(tag, DebuggerSupport.getStaticInt(isolate, definingClass, offset), "1-word value");
                        break;
                    case JDWP.Tag_LONG:
                    case JDWP.Tag_DOUBLE:
                        out.writePrimitive(tag, DebuggerSupport.getStaticLong(isolate, definingClass, offset), "2-word value");
                        break;
                    case JDWP.Tag_OBJECT:
                    case JDWP.Tag_STRING:
                    case JDWP.Tag_THREAD:
                    case JDWP.Tag_CLASS_OBJECT:
                    case JDWP.Tag_ARRAY: {
                        Object object = DebuggerSupport.getStaticOop(isolate, definingClass, offset);
                        sda.getObjectManager().writeTaggedObject(out, object, "object value");
                        break;
                    }
                    default:
                        Assert.shouldNotReachHere();
                }
            }
        }

    }

    /*-----------------------------------------------------------------------*\
     *                        ClassType Command Set (3)                      *
    \*-----------------------------------------------------------------------*/

    /**
     * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_ClassType">ClassType</a>
     */
    static final class ClassType extends SDACommandSet {

        /**
         * {@inheritDoc}
         */
        protected boolean dispatch() throws IOException, SDWPException {
            ReferenceTypeID id = in.readReferenceTypeID("clazz");
            Klass klass = sda.getClassForID(id, JDWP.Error_INVALID_CLASS);
            switch (command.command()) {

                // Implemented commands
                case JDWP.ClassType_Superclass_COMMAND:   Superclass(klass); break;

                // Unimplemented commands
                case JDWP.ClassType_SetValues_COMMAND:
                case JDWP.ClassType_InvokeMethod_COMMAND:
                case JDWP.ClassType_NewInstance_COMMAND:  unimplemented();   break;
                default: return false;
            }
            return true;
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_ClassType_Superclass">Superclass</a>
         */
        private void Superclass(Klass klass) throws IOException {
            Klass superClass = klass.getSuperclass();
            ReferenceTypeID superClassID = (superClass == null ? ReferenceTypeID.NULL : sda.getIDForClass(superClass));
            out.writeReferenceTypeID(superClassID, "superclass");
        }
    }

    /*-----------------------------------------------------------------------*\
     *                      ObjectReference Command Set (9)                  *
    \*-----------------------------------------------------------------------*/

    /**
     * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_ObjectReference">ObjectReference</a>
     */
    static final class ObjectReference extends SDACommandSet {

        /**
         * {@inheritDoc}
         */
        protected boolean dispatch() throws IOException, SDWPException {
            ObjectID id = in.readObjectID("object");
            Object object = sda.getObjectManager().getObjectForID(id);
            switch (command.command()) {

                // Implemented commands
                case JDWP.ObjectReference_ReferenceType_COMMAND:     ReferenceType(object);           break;
                case JDWP.ObjectReference_IsCollected_COMMAND:       IsCollected(object);             break;
                case JDWP.ObjectReference_GetValues_COMMAND:         GetValues(object);               break;
                case JDWP.ObjectReference_InvokeMethod_COMMAND:      InvokeMethod(object);            break;

                // Unimplemented commands
                case JDWP.ObjectReference_SetValues_COMMAND:
                case JDWP.ObjectReference_MonitorInfo_COMMAND:
                case JDWP.ObjectReference_DisableCollection_COMMAND:
                case JDWP.ObjectReference_EnableCollection_COMMAND:  unimplemented();                 break;
                default: return false;
            }
            return true;
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_ObjectReference_InvokeMethod">InvokeMethod</a>
         * <p>
         * Only "toString" is supported as a number of IDEs use this when inspecting variables. However, the value
         * returned is artificially generated for all but String object types as executing arbritary code in the context
         * of the debugger isolate is Not A Good Idea(tm).
         */
        private void InvokeMethod(Object object) throws IOException {
            ObjectID threadID = in.readObjectID("thread");
            ReferenceTypeID classID = in.readReferenceTypeID("clazz");
            MethodID methodID = in.readMethodID("methodID");

            // ignore arguments as only "toString()" is handled
            if (methodID.getOffset() == MethodOffsets.virtual$java_lang_Object$toString) {
                Object value = null;
                if (object instanceof String) {
                    value = object;
                } else {
                    value = object.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(object));
                }
                sda.getObjectManager().writeTaggedObject(out, value, "returnValue");
                sda.getObjectManager().writeTaggedObject(out, null, "exception");
            } else {
                unimplemented();
            }

        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_ObjectReference_IsCollected">IsCollected</a>
         */
        private void IsCollected(Object object) throws IOException {
            boolean wasCollected = (object == null);
            out.writeBoolean(wasCollected, "isCollected");
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_ObjectReference_ReferenceType">ReferenceType</a>
         */
        private void ReferenceType(Object object) throws IOException {
            Klass klass = GC.getKlass(object);
            out.writeByte(JDWP.getTypeTag(klass), "refTypeTag");
            out.writeReferenceTypeID(sda.getIDForClass(klass), "typeID");
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_ObjectReference_GetValues">GetValues</a>
         */
        private void GetValues(Object object) throws SDWPException, IOException {
            int count = in.readInt("count");
            out.writeInt(count, "values");
            for (int i = 0; i < count; i++) {
                FieldID fieldID = in.readFieldID("fieldID");
                int offset = fieldID.getOffset();
                byte tag = fieldID.getTag();
                if (fieldID.isStatic()) {
                    throw new SDWPException(JDWP.Error_INVALID_FIELDID, "field ID denotes a static field");
                }

                Klass klass = GC.getKlass(object);
                switch (tag) {
                    case JDWP.Tag_BYTE:
                    case JDWP.Tag_BOOLEAN:
                        out.writePrimitive(tag, Unsafe.getByte(object, offset), "value");
                        break;
                    case JDWP.Tag_CHAR:
                    case JDWP.Tag_SHORT:
                        out.writePrimitive(tag, Unsafe.getShort(object, offset), "value");
                        break;
                    case JDWP.Tag_INT:
                    case JDWP.Tag_FLOAT:
                        out.writePrimitive(tag, Unsafe.getInt(object, offset), "value");
                        break;
                    case JDWP.Tag_LONG:
                    case JDWP.Tag_DOUBLE:
                        out.writePrimitive(tag, Unsafe.getLong(object, offset), "value");
                        break;
                    case JDWP.Tag_OBJECT:
                    case JDWP.Tag_STRING:
                    case JDWP.Tag_THREAD:
                    case JDWP.Tag_CLASS_OBJECT:
                    case JDWP.Tag_ARRAY: {
                        Object value = Unsafe.getObject(object, offset);
                        sda.getObjectManager().writeTaggedObject(out, value, "value");
                        break;
                    }
                    default:
                        Assert.shouldNotReachHere();
                }
            }
        }

    }

    /*-----------------------------------------------------------------------*\
     *                        EventRequest Command Set (15)                  *
    \*-----------------------------------------------------------------------*/

    /**
     * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_EventRequest">EventRequest</a>
     */
    static final class EventRequest extends SDACommandSet {

        /**
         * {@inheritDoc}
         */
        protected boolean dispatch() throws IOException, SDWPException {
            switch (command.command()) {

                // Implemented commands
                case JDWP.EventRequest_Set_COMMAND:                 Set();     break;
                case JDWP.EventRequest_Clear_COMMAND:               Clear();   break;
                case JDWP.EventRequest_ClearAllBreakpoints_COMMAND: ClearAllBreakpoints(); break;
                default: return false;
            }
            return true;
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_EventRequest_EventRequestSet">EventRequestSet</a>
         */
        private void Set() throws SDWPException, IOException {
            int id = sda.getEventManager().registerEventRequest(in);
            out.writeInt(id, "requestID");
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_EventRequest_EventRequestClear">EventRequestClear</a>
         */
        private void Clear() throws SDWPException, IOException {
            int eventKind = in.readByte("eventKind");
            int requestID = in.readInt("requestID");
            if (requestID == 0) {
                if (Log.info()) {
                    Log.log("Events with ID of zero are reserved for automatic events, and can't be cleared. Fail without error.");
                }
                return;
            }

            sda.getEventManager().clear(eventKind, requestID);
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_EventRequest_ClearAllBreakpoints">ClearAllBreakpoints</a>
         */
        private void ClearAllBreakpoints() throws SDWPException {
            sda.getEventManager().clear(JDWP.EventKind_BREAKPOINT, 0);
        }
    }

    /*-----------------------------------------------------------------------*\
     *                        StackFrame Command Set (16)                    *
    \*-----------------------------------------------------------------------*/

    /**
     * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_StackFrame">StackFrame</a>
     */
    static final class StackFrame extends SDACommandSet {

        private VMThread thread;
        private ExecutionPoint from;
        private int frameNo;

        /**
         * {@inheritDoc}
         */
        protected boolean dispatch() throws IOException, SDWPException {
            ObjectID threadID = in.readObjectID("thread");
            thread = sda.getObjectManager().getThreadForID(threadID);
            from = thread.getEventExecutionPoint();
            FrameID frameID = in.readFrameID("frame");
            frameNo = frameID.frame;

            if (!frameID.threadID.equals(threadID)) {
                throw new SDWPException(JDWP.Error_INVALID_FRAMEID, "frameID.thread [" + frameID.threadID + "] is inconsistent with threadID ["+threadID+"]");
            }
            if (thread.getDebuggerSuspendCount() == 0) {
                throw new SDWPException(JDWP.Error_INVALID_FRAMEID, "cannot inspect non-suspended thread");
            }
            if (frameNo < 0 || frameNo > DebuggerSupport.countStackFrames(thread, from)) {
                throw new SDWPException(JDWP.Error_INVALID_FRAMEID, "invalid frame number " + frameNo);
            }

            switch (command.command()) {

                // Implemented commands
                case JDWP.StackFrame_GetValues_COMMAND:  GetValues();  break;
                case JDWP.StackFrame_ThisObject_COMMAND: ThisObject(); break;

                // Unimplemented commands
                case JDWP.StackFrame_SetValues_COMMAND:  unimplemented();           break;
                default: return false;
            }
            return true;
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_StackFrame_ThisObject ">GetValues</a>
         */
        private void ThisObject() throws SDWPException, IOException {

            DebuggerSupport.StackInspector inspector = new DebuggerSupport.StackInspector(thread, true) {

                private Exception exception;
                private boolean foundThis;
                private boolean isStatic;

                /**
                 * {@inheritDoc}
                 */
                public void inspectFrame(Object mp, Offset bci, int frame, Offset fpOffset) {
                    Assert.that(frame == frameNo);
                    Klass definingClass = DebuggerSupport.getDefiningClass(mp);
                    MethodID methodID = DebuggerSupport.getIDForMethodBody(definingClass, mp);
                    isStatic = methodID.isStatic();
                }

                /**
                 * {@inheritDoc}
                 */
                public void inspectSlot(boolean isParameter, int slot, Object value) {
                    if (!isStatic && isParameter && slot == 0) {
                        try {
                            sda.getObjectManager().writeTaggedObject(out, value, "objectThis");
                            foundThis = true;
                        } catch (IOException e) {
                            exception = e;
                        }
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public void postInspection() {
                    if (exception == null) {
                        if (isStatic) {
                            try {
                                sda.getObjectManager().writeTaggedObject(out, null, "objectThis");
                            } catch (IOException e) {
                                exception = e;
                            }
                        } else {
                            Assert.always(foundThis, "couldn't get 'this' for non-static method");
                        }
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public Object getResult() {
                    return exception;
                }
            };

            int inspectedFrames = DebuggerSupport.inspectStack(inspector, from, frameNo);
            Object result = inspector.getResult();
            if (inspectedFrames == 0) {
                throw new SDWPException(JDWP.Error_INVALID_FRAMEID, frameNo + " is an invalid frame number for thread " + thread);
            } else if (result instanceof IOException) {
                throw (IOException) result;
            } else if (result instanceof SDWPException) {
                throw (SDWPException) result;
            } else {
                Assert.that(result == null);
            }
        }



        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_StackFrame_GetValues">GetValues</a>
         */
        private void GetValues() throws SDWPException, IOException {

            DebuggerSupport.StackInspector inspector = new DebuggerSupport.StackInspector(thread, true) {

                private Exception exception;
                private int slotCount;
                private int[] slots;
                private byte[] tags;

                /**
                 * {@inheritDoc}
                 */
                public void inspectFrame(Object mp, Offset bci, int frame, Offset fpOffset) {
                    Assert.that(frame == frameNo);
                    try {
                        slotCount = in.readInt("slots");
                        out.writeInt(slotCount, "values");
                        slots = new int[slotCount];
                        tags = new byte[slotCount];

                        for (int i = 0; i != slotCount; ++i) {
                            slots[i] = in.readInt("slot");
                            tags[i] = in.readByte("sigbyte");
                        }
                    } catch (IOException e) {
                        exception = e;
                    }
                }

                /**
                 * Gets the JDWP tag denoting the type of a slot's value.
                 *
                 * @param slot   the slot index
                 * @return the type tag or -1 if the slot is not one of the requested slots
                 */
                private byte getTagForSelectedSlot(int slot) {
                    for (int i = 0; i != slots.length; ++i) {
                        if (slots[i] == slot) {
                            return tags[i];
                        }
                    }
                    return -1;
                }

                /**
                 * {@inheritDoc}
                 */
                public void inspectSlot(boolean isParameter, int slot, Object value) {
                    byte tag;
                    if (exception == null && (tag = getTagForSelectedSlot(slot)) != -1) {
//Log.log("*** slot = " + slot);
//Log.log("*** tag = " + (char)tag);
                        if (tag == JDWP.Tag_OBJECT ||
                            tag == JDWP.Tag_STRING ||
                            tag == JDWP.Tag_THREAD ||
                            tag == JDWP.Tag_CLASS_OBJECT ||
                            tag == JDWP.Tag_ARRAY)
                        {
                            try {
                                sda.getObjectManager().writeTaggedObject(out, value, "slotValue");
                                slotCount--;
                            } catch (IOException e) {
                                exception = e;
                            }
                        } else {
                            exception = new SDWPException(JDWP.Error_INVALID_SLOT, "slot type is invalid for request");
                        }
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public void inspectSlot(boolean isParameter, int slot, Klass type, long value) {
                    byte tag;
                    if (exception == null && (tag = getTagForSelectedSlot(slot)) != -1) {
//Log.log("*** slot = " + slot);
//Log.log("*** tag = " + (char)tag);
//Log.log("*** type = " + type.getInternalName());
                        switch (tag) {
                            case JDWP.Tag_BYTE:
                            case JDWP.Tag_BOOLEAN:
                            case JDWP.Tag_CHAR:
                            case JDWP.Tag_SHORT:
                            case JDWP.Tag_INT:
                            case JDWP.Tag_FLOAT:
                            case JDWP.Tag_LONG:
                            case JDWP.Tag_DOUBLE: {
                                try {
                                    out.writePrimitive(tag, value, "slotValue");
                                    slotCount--;
                                } catch (IOException e) {
                                    exception = e;
                                }
                                break;
                            }
                            default: {
                                exception = new SDWPException(JDWP.Error_INVALID_SLOT, "slot type is invalid for request");
                            }
                        }
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public void postInspection() {
                    if (exception == null && slotCount != 0) {
                        exception = new SDWPException(JDWP.Error_INVALID_SLOT, "not all slots were matched");
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public Object getResult() {
                    return exception;
                }
            };

            int inspectedFrames = DebuggerSupport.inspectStack(inspector, from, frameNo);
            Object result = inspector.getResult();
            if (inspectedFrames == 0) {
                throw new SDWPException(JDWP.Error_INVALID_FRAMEID, frameNo + " is an invalid frame number for thread " + thread);
            } else if (result instanceof IOException) {
                throw (IOException) result;
            } else if (result instanceof SDWPException) {
                throw (SDWPException) result;
            } else {
                Assert.that(result == null);
            }
        }
    }

    /*-----------------------------------------------------------------------*\
     *                        StringReference Command Set (10)               *
    \*-----------------------------------------------------------------------*/

    /**
     * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_StringReference">StringReference</a>.
     */
    static final class StringReference extends SDACommandSet {

        /**
         * {@inheritDoc}
         */
        protected boolean dispatch() throws IOException, SDWPException {
            switch (command.command()) {

                // Implemented commands
                case JDWP.StringReference_Value_COMMAND: StringValue(); break;

                default: return false;
            }
            return true;
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_StringReference_StringValue">StringValue</a>.
         */
        private void StringValue() throws SDWPException, IOException {
            ObjectID id = in.readObjectID("stringObject");
            String string = sda.getObjectManager().getStringForID(id);
            out.writeString(string, "stringValue");
        }
    }

    /*-----------------------------------------------------------------------*\
     *                        ThreadReference Command Set (11)               *
    \*-----------------------------------------------------------------------*/

    /**
     * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_ThreadReference">ThreadReference</a>.
     */
    static final class ThreadReference extends SDACommandSet {

        private VMThread thread;
        private ExecutionPoint from;

        /**
         * {@inheritDoc}
         */
        protected boolean dispatch() throws IOException, SDWPException {
            ObjectID threadID = in.readObjectID("thread");
            thread = sda.getObjectManager().getThreadForID(threadID);
            from = thread.getEventExecutionPoint();

            switch (command.command()) {
//                case JDWP.ThreadReference_Name_COMMAND:                    Name();         break;
                case JDWP.ThreadReference_Suspend_COMMAND:                 Suspend();      break;
                case JDWP.ThreadReference_Resume_COMMAND:                  Resume();       break;
//                case JDWP.ThreadReference_Status_COMMAND:                  Status();       break;
                case JDWP.ThreadReference_Frames_COMMAND:                  Frames();       break;
                case JDWP.ThreadReference_FrameCount_COMMAND:              FrameCount();   break;
                case JDWP.ThreadReference_SuspendCount_COMMAND:            SuspendCount(); break;

                case JDWP.ThreadReference_Name_COMMAND:
                case JDWP.ThreadReference_Status_COMMAND:
                case JDWP.ThreadReference_OwnedMonitors_COMMAND:
                case JDWP.ThreadReference_CurrentContendedMonitor_COMMAND: unimplemented();      break;
                default: return false;
            }
            return true;
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_ThreadReference_SuspendCount">SuspendCount</a>.
         */
        private void SuspendCount() throws IOException {
            out.writeInt(thread.getDebuggerSuspendCount(), "suspendCount");
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_ThreadReference_Name">Name</a>.
         */
//        private void Name() throws IOException {
//            out.writeString(thread.getName(), "threadName");
//        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_ThreadReference_Suspend">Suspend</a>.
         */
        private void Suspend() {
            sda.suspendThreads(thread);
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_ThreadReference_Resume">Resume</a>.
         */
        private void Resume() {
            sda.resumeThreads(thread);
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_ThreadReference_FrameCount">FrameCount</a>.
         */
        private void FrameCount() throws IOException {
            // The frames cannot be inspected if the thread is not suspended
            int count;
            if (thread.getDebuggerSuspendCount() != 0) {
                count = DebuggerSupport.countStackFrames(thread, from);
            } else {
                count = 0;
            }
            out.writeInt(count, "frameCount");
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_ThreadReference_Status">Status</a>.
         */
//        private void Status() throws IOException {
//            int status = DebuggerSupport.getThreadJDWPState(thread);
//            int suspendStatus = 0;
//
//            if (thread.getDebuggerSuspendCount() != 0) {
//                suspendStatus = JDWP.SuspendStatus_SUSPEND_STATUS_SUSPENDED;
//            }
//            out.writeInt(status, "threadStatus");
//            out.writeInt(suspendStatus, "suspendStatus");
//        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_ThreadReference_Frames">Frames</a>.
         */
        private void Frames() throws SDWPException, IOException {
            final int startFrame = in.readInt("startFrame");
            final int length = in.readInt("length");

            // The frames cannot be inspected if the thread is not suspended
            if (thread.getDebuggerSuspendCount() == 0) {
                out.writeInt(0, "frames");
                return;
            }

            final int totalFrames = DebuggerSupport.countStackFrames(thread, from);
            final int availFrames = totalFrames - startFrame;
            final int endFrame = (length == -1) ? availFrames : startFrame + length;

            if (startFrame < 0 || endFrame <= startFrame || endFrame > totalFrames) {
                throw new SDWPException(JDWP.Error_INVALID_THREAD, "'startFrame' or 'length' value is invalid");
            }

            out.writeInt(endFrame - startFrame, "frames");
            DebuggerSupport.StackInspector inspector = new DebuggerSupport.StackInspector(thread, false) {
                private Exception exception;
                public void inspectFrame(Object mp, Offset bciWord, int frame, Offset fpOffset) {
                    if (exception == null && startFrame <= frame && frame < endFrame) {

                        int bci = bciWord.toInt();
                        if (frame != 0 || DebuggerSupport.isAtExceptionBreakpoint(vmThread)) {
                            // if we're at an exception or in any frame but the inner most, ip is
                            // one after the currently executing instruction in the frame
                            bci--;
                        }

                        Klass definingClass = DebuggerSupport.getDefiningClass(mp);
                        MethodID methodID = DebuggerSupport.getIDForMethodBody(definingClass, mp);
                        ReferenceTypeID definingClassID = sda.getIDForClass(definingClass);
                        Location location = new Location(JDWP.getTypeTag(definingClass), definingClassID, methodID, bci);
                        Assert.that(DebuggerSupport.getMethodBody(definingClass, methodID.getOffset(), methodID.isStatic()) == mp, "bad method lookup");

                        if (Log.debug()) {
                            Log.log("    " + definingClass + "[mid=" + methodID + "]@" + bci);
                        }

                        try {
                            out.writeFrameID(new FrameID(sda.getObjectManager().getIDForObject(vmThread), frame), "frameID");
                            out.writeLocation(location, "location");
                        } catch (IOException e) {
                            exception = e;
                        }
                    }
                }
                public Object getResult() {
                    return exception;
                }
            };

            DebuggerSupport.inspectStack(inspector, from, -1);
            Object result = inspector.getResult();
            if (result instanceof IOException) {
                throw (IOException)result;
            } else if (result instanceof SDWPException) {
                throw (SDWPException)result;
            } else {
                Assert.that(result == null);
            }
        }
    }

    /*-----------------------------------------------------------------------*\
     *                        ArrayReference Command Set (13)                *
    \*-----------------------------------------------------------------------*/

    /**
     * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_ArrayReference">ArrayReference</a>.
     */
    static final class ArrayReference extends SDACommandSet {

        /**
         * {@inheritDoc}
         */
        protected boolean dispatch() throws IOException, SDWPException {
            ObjectID arrayID = in.readObjectID("arrayObject");
            Object array = sda.getObjectManager().getObjectForID(arrayID);
            Klass arrayKlass = GC.getKlass(array);
            Klass compType = arrayKlass.getComponentType();

            if (!arrayKlass.isArray()) {
                throw new SDWPException(JDWP.Error_INVALID_ARRAY, "object ID does not denote an array: " + arrayID);
            }
            int length = GC.getArrayLength(array);

            switch (command.command()) {
                case JDWP.ArrayReference_Length_COMMAND:    Length(length);                      break;
                case JDWP.ArrayReference_GetValues_COMMAND: GetValues(array, compType, length); break;
                case JDWP.ArrayReference_SetValues_COMMAND: unimplemented();                     break;
                default: return false;
            }
            return true;
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_ArrayReference_GetValues">GetValues</a>.
         */
        private void GetValues(Object array, Klass compType, int len) throws SDWPException, IOException {
            int start = in.readInt("firstIndex");
            int count = in.readInt("length");

            if (start + count > len) {
                throw new SDWPException(JDWP.Error_INVALID_LENGTH, "ArrayReference_GetValues: count + start  is too large " + (start + count));
            }
            byte tag = JDWP.getTag(compType);
            out.writeByte(tag, "arrayregion:tag");
            out.writeInt(count, "arrayregion:length");

            for (int i = 0; i < count; i++) {
                switch (tag) {
                    case JDWP.Tag_BYTE:
                    case JDWP.Tag_BOOLEAN:
                        out.writeByte((byte) Unsafe.getByte(array, start + i), "arrayregion:value");
                        break;
                    case JDWP.Tag_CHAR:
                    case JDWP.Tag_SHORT:
                        out.writeShort((short) Unsafe.getShort(array, start + i), "arrayregion:value");
                        break;
                    case JDWP.Tag_INT:
                    case JDWP.Tag_FLOAT:
                        out.writeInt(Unsafe.getInt(array, start + i), "arrayregion:value");
                        break;
                    case JDWP.Tag_LONG:
                    case JDWP.Tag_DOUBLE:
                        out.writeLong(Unsafe.getLong(array, start + i), "arrayregion:value");
                        break;
                    case JDWP.Tag_OBJECT:
                    case JDWP.Tag_STRING:
                    case JDWP.Tag_THREAD:
                    case JDWP.Tag_CLASS_OBJECT:
                    case JDWP.Tag_ARRAY: {
                        Object elem = Unsafe.getObject(array, start + i);
                        sda.getObjectManager().writeTaggedObject(out, elem, "arrayregion:value");
                        break;
                    }
                    default: Assert.shouldNotReachHere();
                }
            }
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_ArrayReference_Length">Length</a>.
         */
        private void Length(int len) throws IOException {
            out.writeInt(len, "arrayLength");
        }
    }

    /*-----------------------------------------------------------------------*\
     *                        ClassObjectReference Command Set (17)          *
    \*-----------------------------------------------------------------------*/

    /**
     * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_ClassObjectReference">ClassObjectReference</a>.
     */
    static final class ClassObjectReference extends SDACommandSet {

        /**
         * {@inheritDoc}
         */
        protected boolean dispatch() throws IOException, SDWPException {
            switch (command.command()) {
                case JDWP.ClassObjectReference_ReflectedType_COMMAND: ReflectedType();  break;
                default: return false;
            }
            return true;
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_ClassObjectReference_ReflectedType">ReflectedType</a>.
         */
        private void ReflectedType() throws SDWPException, IOException {
            ObjectID id = in.readObjectID("classObject");
            Klass klass = sda.getObjectManager().getClassForID(id);
            ReferenceTypeID classID = sda.getIDForClass(klass);
            out.writeByte(JDWP.getTypeTag(klass), "refTypeTag");
            out.writeReferenceTypeID(classID, "typeID");
        }
    }

    /*-----------------------------------------------------------------------*\
     *                         SquawkVM Command Set (128)                    *
    \*-----------------------------------------------------------------------*/

    final class SquawkVM extends SDACommandSet {

        /**
         * {@inheritDoc}
         */
        protected boolean dispatch() throws IOException, SDWPException {
            switch (command.command()) {
                case SDWP.SquawkVM_AllThreads_COMMAND:        AllThreads();        break;
                default: return false;
            }
            return true;
        }

        /**
         * Implements {@link SDWP#SquawkVM_AllThreads_COMMAND}.
         */
        private void AllThreads() throws SDWPException, IOException {
            sda.writeThreadState(out);
        }
    }
}
