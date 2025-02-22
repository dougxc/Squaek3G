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
import java.util.*;

import com.sun.squawk.debugger.*;
import com.sun.squawk.debugger.DataType.*;
import com.sun.squawk.debugger.EventRequest.*;
import com.sun.squawk.util.*;
import com.sun.squawk.*;

/**
 * A JDBListener implements the Squawk Debugger Proxy side of the JDWP protocol
 * that communicates with jdb or some other JPDA compliant debugger.
 *
 * @author Derek White, Doug Simon
 */
class JDBListener extends JDWPListener {

    /**
     * The Squawk Debugger Proxy that owns this listener.
     */
    private final SDP sdp;

    /**
     * The JDWP command set handlers.
     */
    private final IntHashtable commandSets = new IntHashtable();

    public JDBListener(SDP sdp) {
        this.sdp = sdp;
        commandSets.put(JDWP.VirtualMachine_COMMAND_SET,       new VirtualMachine());
        commandSets.put(JDWP.ReferenceType_COMMAND_SET,        new ReferenceType());
        commandSets.put(JDWP.Method_COMMAND_SET,               new Method());
        commandSets.put(JDWP.ClassType_COMMAND_SET,            new ClassType());
        commandSets.put(JDWP.ThreadReference_COMMAND_SET,      new ThreadReference());
        commandSets.put(JDWP.ThreadGroupReference_COMMAND_SET, new ThreadGroupReference());
        commandSets.put(JDWP.EventRequest_COMMAND_SET,         new EventRequest());
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void processCommand(CommandPacket command) throws IOException {
        SDPCommandSet handler = (SDPCommandSet)commandSets.get(command.set());
        if (handler == null || !handler.handle(sdp, this, otherHost, command)) {
            if (Log.verbose()) {
                Log.log("Passing through to VM: " + command);
            }

            try {
                otherHost.sendCommand(command);
            } catch (SDWPException e) {
                // Just pass error replies through to debugger
            }
            ReplyPacket sdaReply = command.getReply();

            if (Log.verbose()) {
                Log.log("Got reply to pass through from VM to debugger: " + command);
            }

            sendReply(sdaReply);
        }
    }

    public String toString() {
        return "JDBListener";
    }


    /*-----------------------------------------------------------------------*\
     *                      VirtualMachine Command Set (1)                   *
    \*-----------------------------------------------------------------------*/

    /**
     * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_VirtualMachine">VirtualMachine</a>.
     */
    static final class VirtualMachine extends SDPCommandSet {

        private boolean dispose;

        protected boolean dispatch() throws IOException, SDWPException {
            dispose = false;
            switch (command.command()) {
                case JDWP.VirtualMachine_ClassesBySignature_COMMAND:   ClassesBySignature();   break;
                case JDWP.VirtualMachine_AllThreads_COMMAND:           AllThreads();           break;
                case JDWP.VirtualMachine_AllClasses_COMMAND:           AllClasses();           break;
                case JDWP.VirtualMachine_Version_COMMAND:              Version();              break;
                case JDWP.VirtualMachine_TopLevelThreadGroups_COMMAND: TopLevelThreadGroups(); break;
                case JDWP.VirtualMachine_Dispose_COMMAND:              Dispose();              break;
                case JDWP.VirtualMachine_IDSizes_COMMAND:              IDSizes();              break;
                case JDWP.VirtualMachine_Capabilities_COMMAND:         Capabilities();         break;
                case JDWP.VirtualMachine_CapabilitiesNew_COMMAND:      CapabilitiesNew();      break;
                case JDWP.VirtualMachine_ClassPaths_COMMAND:           ClassPaths();           break;
                case JDWP.VirtualMachine_DisposeObjects_COMMAND:       DisposeObjects();       break;
                default: return false;
            }
            return true;
        }

        protected void postDispatch() {
            if (dispose) {
                host.quit();
            }
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_VirtualMachine_AllThreads">AllThreads</a>
         */
        private void AllThreads() throws IOException {
            Collection threads = sdp.getTPM().getThreads();
            Assert.that(!threads.isEmpty());
            out.writeInt(threads.size(), "threads");
            for (Iterator i = threads.iterator(); i.hasNext(); ) {
                ProxyThread thread = (ProxyThread) i.next();
                out.writeObjectID(thread.id, "thread");
            }
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_VirtualMachine_AllClasses">AllClasses</a>
         */
        private void AllClasses() throws IOException {
            Collection types = sdp.getPTM().getTypes();

            // Only send back classes that came from class files
            Set classfileTypes = new HashSet();
            for (Iterator iter = types.iterator(); iter.hasNext(); ) {
                ProxyType type = (ProxyType) iter.next();
                Klass klass = type.getKlass();
                if (!klass.isSynthetic() && !klass.isInternalType() && !klass.isSquawkPrimitive()) {
                    classfileTypes.add(type);
                }
            }

            out.writeInt(classfileTypes.size(), "classes");
            for (Iterator iter = classfileTypes.iterator(); iter.hasNext(); ) {
                ProxyType type = (ProxyType)iter.next();

                out.writeByte(JDWP.getTypeTag(type.getKlass()), "refTypeTag");
                out.writeReferenceTypeID(type.getID(), "typeID");
                String sig = type.getSignature();
                out.writeString(sig, "signature");
                out.writeInt(JDWP.ClassStatus_VERIFIED_PREPARED_INITIALIZED, "status");
            }
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_VirtualMachine_ClassesBySignature">ClassesBySignature</a>
         */
        private void ClassesBySignature() throws IOException {
            String sig = in.readString("signature");
            Collection types = sdp.getPTM().getTypes();
            for (Iterator iter = types.iterator(); iter.hasNext(); ) {
                ProxyType type = (ProxyType)iter.next();
                if (type.getSignature().equals(sig)) {
                    byte tag = JDWP.getTypeTag(type.getKlass());
                    int status = JDWP.ClassStatus_VERIFIED_PREPARED_INITIALIZED;

                    out.writeInt(1, "classes");
                    out.writeByte(tag, "refTypeTag");
                    out.writeReferenceTypeID(type.getID(), "typeID");
                    out.writeInt(status, "status");
                    return;
                }
            }
            out.writeInt(0, "classes");
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_VirtualMachine_Version">Version</a>.
         */
        private void Version() throws IOException {
            out.writeString("Version 1.0", "description");
            out.writeInt(1, "jdwpMajor"); /* major version */
            out.writeInt(0, "jdwpMinor"); /* minor version */
            out.writeString("1.0.0", "vmVersion");
            out.writeString("Squawk VM", "vmName");
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_VirtualMachine_TopLevelThreadGroups">TopLevelThreadGroups</a>.
         */
        private void TopLevelThreadGroups() throws IOException {
            out.writeInt(1, "groups");
            out.writeObjectID(ObjectID.THREAD_GROUP, "group");
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_VirtualMachine_Dispose">Dispose</a>.
         */
        private void Dispose() throws IOException, SDWPException {
            otherHost.sendCommand(command);
            dispose = true;
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_VirtualMachine_IDSizes">IDSizes</a>.
         */
        private void IDSizes() throws IOException {
            out.writeInt(FieldID.SIZE, "fieldIDSize");
            out.writeInt(MethodID.SIZE, "methodIDSize");
            out.writeInt(ObjectID.SIZE, "objectIDSize");
            out.writeInt(ReferenceTypeID.SIZE, "referenceTypeIDSize");
            out.writeInt(FrameID.SIZE, "frameIDSize");
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_VirtualMachine_Capabilities">Capabilities</a>.
         */
        private void Capabilities() throws IOException {
            out.writeBoolean(false, "canWatchFieldModification");
            out.writeBoolean(false, "canWatchFieldAccess");
            out.writeBoolean(false, "canGetBytecodes");
            out.writeBoolean(false, "canGetSyntheticAttribute");
            out.writeBoolean(false, "canGetOwnedMonitorInfo");
            out.writeBoolean(false, "canGetCurrentContendedMonitor");
            out.writeBoolean(false, "canGetMonitorInfo");
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>.
         */
        private void CapabilitiesNew() throws IOException {
            out.writeBoolean(false, "canWatchFieldModification");       // Can the VM watch field modification, and therefore can it send the Modification Watchpoint Event?
            out.writeBoolean(false, "canWatchFieldAccess");             // Can the VM watch field access, and therefore can it send the Access Watchpoint Event?
            out.writeBoolean(false, "canGetBytecodes");                 // Can the VM get the bytecodes of a given method?
            out.writeBoolean(false, "canGetSyntheticAttribute");        // Can the VM determine whether a field or method is synthetic? (that is, can the VM determine if the method or the field was invented by the compiler?)
            out.writeBoolean(false, "canGetOwnedMonitorInfo");          // Can the VM get the owned monitors infornation for a thread?
            out.writeBoolean(false, "canGetCurrentContendedMonitor");   // Can the VM get the current contended monitor of a thread?
            out.writeBoolean(false, "canGetMonitorInfo");               // Can the VM get the monitor information for a given object?
            out.writeBoolean(false, "canRedefineClasses");              // Can the VM redefine classes?
            out.writeBoolean(false, "canAddMethod");                    // Can the VM add methods when redefining classes?
            out.writeBoolean(false, "canUnrestrictedlyRedefineClasses");// Can the VM redefine classesin arbitrary ways?
            out.writeBoolean(false, "canPopFrames");                    // Can the VM pop stack frames?
            out.writeBoolean(false, "canUseInstanceFilters");           // Can the VM filter events by specific object?
            out.writeBoolean(false, "canGetSourceDebugExtension");      // Can the VM get the source debug extension?
            out.writeBoolean(true,  "canRequestVMDeathEvent");          // Can the VM request VM death events?
            out.writeBoolean(false, "canSetDefaultStratum");            // Can the VM set a default stratum?
            out.writeBoolean(false, "reserved16");                      // Reserved for future capability
            out.writeBoolean(false, "reserved17");                      // Reserved for future capability
            out.writeBoolean(false, "reserved18");                      // Reserved for future capability
            out.writeBoolean(false, "reserved19");                      // Reserved for future capability
            out.writeBoolean(false, "reserved20");                      // Reserved for future capability
            out.writeBoolean(false, "reserved21");                      // Reserved for future capability
            out.writeBoolean(false, "reserved22");                      // Reserved for future capability
            out.writeBoolean(false, "reserved23");                      // Reserved for future capability
            out.writeBoolean(false, "reserved24");                      // Reserved for future capability
            out.writeBoolean(false, "reserved25");                      // Reserved for future capability
            out.writeBoolean(false, "reserved26");                      // Reserved for future capability
            out.writeBoolean(false, "reserved27");                      // Reserved for future capability
            out.writeBoolean(false, "reserved28");                      // Reserved for future capability
            out.writeBoolean(false, "reserved29");                      // Reserved for future capability
            out.writeBoolean(false, "reserved30");                      // Reserved for future capability
            out.writeBoolean(false, "reserved31");                      // Reserved for future capability
            out.writeBoolean(false, "reserved32");                      // Reserved for future capability

        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_VirtualMachine_ClassPaths">ClassPaths</a>.
         */
        private void ClassPaths() throws IOException {
            out.writeString(".", "baseDir");
            out.writeInt(0, "classpaths");
            out.writeInt(0, "bootclasspaths");
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_VirtualMachine_DisposeObjects">DisposeObjects</a>.
         */
        private void DisposeObjects() throws IOException {
        }

    }

    /*-----------------------------------------------------------------------*\
     *                      ReferenceType Command Set (2)                    *
    \*-----------------------------------------------------------------------*/

    /**
     * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_ReferenceType">ReferenceType</a>.
     */
    static final class ReferenceType extends SDPCommandSet {

        /**
         * {@inheritDoc}
         */
        protected boolean dispatch() throws IOException, SDWPException {
            ReferenceTypeID typeID = in.readReferenceTypeID("refType");
            ProxyType type = sdp.getPTM().lookup(typeID, true);

            switch (command.command()) {
                case JDWP.ReferenceType_Signature_COMMAND:     Signature(type);           break;
                case JDWP.ReferenceType_ClassLoader_COMMAND:   ClassLoader();             break;
                case JDWP.ReferenceType_Modifiers_COMMAND:     Modifiers(type);           break;
                case JDWP.ReferenceType_Fields_COMMAND:        Fields(type);              break;
                case JDWP.ReferenceType_Methods_COMMAND:       Methods(type);             break;
                case JDWP.ReferenceType_SourceFile_COMMAND:    SourceFile(type);          break;
                case JDWP.ReferenceType_Interfaces_COMMAND:    Interfaces(type);          break;
                case JDWP.ReferenceType_GetValues_COMMAND:     GetValues(type);           break;
                case JDWP.ReferenceType_Status_COMMAND:        Status(type);              break;
                default: return false;
            }
            return true;
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_ReferenceType_Interfaces">Interfaces</a>.
         */
        private void Interfaces(ProxyType type) throws IOException, SDWPException {
            List interfaces = type.getInterfaces();
            out.writeInt(interfaces.size(), "interfaces");
            for (int i = 0; i < interfaces.size(); i++) {
                ProxyType intf = (ProxyType) interfaces.get(i);
                out.writeReferenceTypeID(intf.getID(), "interfaceType");
            }
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_ReferenceType_Status">Status</a>.
         */
        private void Status(ProxyType type) throws IOException {
            out.writeInt(JDWP.ClassStatus_VERIFIED_PREPARED_INITIALIZED, "status");
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_ReferenceType_SourceFile">SourceFile</a>.
         */
        private void SourceFile(ProxyType type) throws IOException {
            String sourceName = type.getSourceName();
            if (sourceName != null) {
                out.writeString(sourceName, "sourceFile");
            } else {
                sourceName = type.getName();
                int lastDot = sourceName.lastIndexOf('.');
                if (lastDot != -1) {
                    sourceName = sourceName.substring(lastDot);
                }
                sourceName = sourceName + ".java";
                out.writeString(sourceName, "sourceFile");
            }
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_ReferenceType_Methods">Methods</a>.
         */
        private void Methods(ProxyType type) throws IOException {
            // Note: arrays implement dummy methods() method, handle same as other classes
            List methods = type.getMethods();
            out.writeInt(methods.size(), "declared");
            for (int i = 0; i < methods.size(); i++) {
                ProxyMethod method = (ProxyMethod) methods.get(i);
                out.writeMethodID(method.getID(), "methodID");
                out.writeString(method.getName(), "name");
                out.writeString(method.getSignature(), "signature");
                out.writeInt(method.getModifiers(), "modBits");
            }
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_ReferenceType_Fields">Fields</a>.
         */
        private void Fields(ProxyType type) throws IOException, SDWPException  {
            List fields = type.getFields();
            out.writeInt(fields.size(), "declared");
            for (int i = 0; i < fields.size(); i++) {
                ProxyField fi = (ProxyField) fields.get(i);
                out.writeFieldID(fi.getID(), "fieldID");
                out.writeString(fi.getName(), "name");
                out.writeString(fi.getSignature(), "signature");
                out.writeInt(fi.getModifiers(), "modBits");
            }
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_ReferenceType_Modifiers">Modifiers</a>.
         */
        private void Modifiers(ProxyType type) throws IOException {
            out.writeInt(type.getModifiers(), "signature");
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_ReferenceType_ClassLoader">ClassLoader</a>.
         */
        private void ClassLoader() throws IOException {
            out.writeReferenceTypeID(ReferenceTypeID.NULL, "classLoader");
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_ReferenceType_Signature">Signature</a>.
         */
        private void Signature(ProxyType type) throws IOException {
            out.writeString(type.getSignature(), "signature");
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_ReferenceType_GetValues">GetValues</a>.
         */
        private void GetValues(ProxyType requestKlass) throws SDWPException, IOException {
            int count = in.readInt("fields");
            FieldID[] fieldIDs = new FieldID[count];
            ProxyField[] fields = new ProxyField[count];
            int constantFields = 0;
            PacketInputStream sdaIn = null;

            // 1: Get proxy field objects
            for (int i = 0; i < count; i++) {
                FieldID fieldID = in.readFieldID("fieldID");
                fieldIDs[i] = fieldID;
                fields[i] = requestKlass.getField(fieldID);
                Field f = fields[i].getField();
                if (f.isFinal() && f.hasConstant()) {
                    constantFields++;
                }
            }

            // 2: Create simple(er) command to send to VM
            //    Note that some classes, particularly interfaces, may only have constant fields.
            int nonConstFields = count - constantFields;
            if (nonConstFields > 0) {
                CommandPacket sdaCommand = new CommandPacket(JDWP.ReferenceType_COMMAND_SET, JDWP.ReferenceType_GetValues_COMMAND, true);
                PacketOutputStream sdaOut = sdaCommand.getOutputStream();
                sdaOut.writeReferenceTypeID(requestKlass.getID(), "refType");
                sdaOut.writeInt(nonConstFields, "fields");
                for (int i = 0; i < count; i++) {
                    Field f = fields[i].getField();
                    if (!(f.isFinal() && f.hasConstant())) {
                        sdaOut.writeFieldID(fieldIDs[i], "fieldID");
                    }
                }
                if (Log.verbose()) {
                    Log.log("  {Sending command to VM - " + sdaCommand + "}");
                }
                ReplyPacket sdaReply = otherHost.sendCommand(sdaCommand);
                if (Log.verbose()) {
                    Log.log("  {Got reply from VM - " + sdaReply + "}");
                }
                sdaIn = sdaReply.getInputStream();

                int countFromVM = sdaIn.readInt("values");
                Assert.that(countFromVM == nonConstFields);
            }

            // 3: Parse reply from VM, and generate reply to debugger,
            //    inserting constant values as needed.
            out.writeInt(count, "values");
            for (int i = 0; i < count; i++) {
                Field f = fields[i].getField();
                if (f.isFinal() && f.hasConstant()) {
                    long value = f.getPrimitiveConstantValue();
                    byte tag = JDWP.getTag(f.getType());
                    out.writePrimitive(tag, value, "value");
                } else {
                    byte tag = sdaIn.readByte("tag");
                    switch (tag) {
                        case JDWP.Tag_BYTE:
                        case JDWP.Tag_BOOLEAN: out.writePrimitive(tag, sdaIn.readByte("value"), "value");  break;
                        case JDWP.Tag_CHAR:
                        case JDWP.Tag_SHORT:   out.writePrimitive(tag, sdaIn.readShort("value"), "value"); break;
                        case JDWP.Tag_INT:
                        case JDWP.Tag_FLOAT:   out.writePrimitive(tag, sdaIn.readInt("value"), "value");   break;
                        case JDWP.Tag_OBJECT:
                        case JDWP.Tag_STRING:
                        case JDWP.Tag_THREAD:
                        case JDWP.Tag_CLASS_OBJECT:
                        case JDWP.Tag_ARRAY:   out.writeTaggedObjectID(new TaggedObjectID(tag, sdaIn.readObjectID("value").id), "value");    break;
                        case JDWP.Tag_LONG:
                        case JDWP.Tag_DOUBLE:  out.writePrimitive(tag, sdaIn.readLong("value"), "value");            break;
                        default: Assert.shouldNotReachHere("Unknown value tag: " + tag);
                    }
                }
            }
        }


    }

    /*-----------------------------------------------------------------------*\
     *                        ThreadReference Command Set (11)               *
    \*-----------------------------------------------------------------------*/

    /**
     * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#ThreadReference">ThreadReference</a>.
     */
    static final class ThreadReference extends SDPCommandSet {

        /**
         * {@inheritDoc}
         */
        protected boolean dispatch() throws IOException, SDWPException {

            switch (command.command()) {
                case JDWP.ThreadReference_ThreadGroup_COMMAND: ThreadGroup(); break;
                case JDWP.ThreadReference_Name_COMMAND: Name();               break;
                case JDWP.ThreadReference_Status_COMMAND: Status();               break;
                case JDWP.ThreadReference_Frames_COMMAND:  Frames();          break;

                case JDWP.ThreadReference_OwnedMonitors_COMMAND:
                case JDWP.ThreadReference_CurrentContendedMonitor_COMMAND: unimplemented();      break;
                default: return false;
            }
            return true;
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_ThreadReference_ThreadGroup">ThreadGroup</a>.
         */
        private void ThreadGroup() throws IOException {
            out.writeObjectID(ObjectID.THREAD_GROUP, "group");
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_ThreadReference_Frames">Frames</a>.
         */
        private void Frames() throws SDWPException, IOException {
            ObjectID threadID = in.readObjectID("thread");
            ProxyThread thread = sdp.getTPM().getThread(threadID);
            if (!thread.isSuspended()) {
                out.writeInt(0, "frames");
                return;
            }


            // Filters out frames for which the class file could not be found
            PacketInputStream sdaIn = otherHost.sendCommand(command).getInputStream();

            int frameCount = sdaIn.readInt("frames");
            out.writeInt(frameCount, "frames");
            for (int i = 0; i != frameCount; ++i) {
                FrameID frameID = sdaIn.readFrameID("frameID");
                Location loc = sdaIn.readLocation("location");

                out.writeFrameID(frameID, "frameID");
                ProxyType type = sdp.getPTM().lookup(loc.definingClass, true);
                if (type instanceof UndefinedProxyType) {
                    out.writeLocation(new Location(JDWP.TypeTag_CLASS, loc.definingClass, MethodID.UNKNOWN, 0), "location");
                } else {
                    out.writeLocation(loc, "location");
                }
            }
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_ThreadReference_Name">Name</a>.
         */
        private void Name() throws SDWPException, IOException {
            ObjectID threadID = in.readObjectID("thread");
            ProxyThread thread = sdp.getTPM().getThread(threadID);
            out.writeString(thread.getName(), "threadname");
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_ThreadReference_Status">Status</a>.
         */
        private void Status() throws SDWPException, IOException {
            ObjectID threadID = in.readObjectID("thread");
            ProxyThread thread = sdp.getTPM().getThread(threadID);
            out.writeInt(thread.getStatus(), "threadStatus");
            out.writeInt(thread.isSuspended() ? JDWP.SuspendStatus_SUSPEND_STATUS_SUSPENDED : 0, "suspendStatus");
        }

    }


    /*-----------------------------------------------------------------------*\
     *                        ClassType Command Set (3)                      *
    \*-----------------------------------------------------------------------*/

    /**
     * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_ClassType">ClassType</a>
     */
    static final class ClassType extends SDPCommandSet {

        /**
         * {@inheritDoc}
         */
        protected boolean dispatch() throws IOException, SDWPException {
            switch (command.command()) {
                case JDWP.ClassType_Superclass_COMMAND: Superclass(); return true;
                default: return false;
            }
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_ClassType_Superclass">Superclass</a>
         */
        private void Superclass() throws SDWPException, IOException {
            ReferenceTypeID typeID = in.readReferenceTypeID("refType");
            ProxyType type = sdp.getPTM().lookup(typeID, true);
            ProxyType superType = type.getSuperclass();
            if (superType == null) {
                out.writeReferenceTypeID(ReferenceTypeID.NULL, "superclass");
            } else {
                out.writeReferenceTypeID(superType.getID(), "superclass");
            }
        }
    }

    /*-----------------------------------------------------------------------*\
     *                             Method Command Set (6)                    *
    \*-----------------------------------------------------------------------*/

    /**
     * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_Method">Method</a>
     */
    static final class Method extends SDPCommandSet {

        /**
         * {@inheritDoc}
         */
        protected boolean dispatch() throws IOException, SDWPException {
            ReferenceTypeID typeID = in.readReferenceTypeID("refType");
            MethodID methodID = in.readMethodID("methodID");
            ProxyType type = sdp.getPTM().lookup(typeID, true);

            ProxyMethod method = type.getMethod(methodID);

            if (method == null) {
                throw new SDWPException(JDWP.Error_INVALID_METHODID, "Couldn't find method for ID " + methodID);
            }

            switch (command.command()) {
                case JDWP.Method_LineTable_COMMAND:      LineTable(type, method); break;
                case JDWP.Method_VariableTable_COMMAND:  VariableTable(type, method); break;
                default: return false;
            }
            return true;
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_Method_LineTable">LineTable</a>
         */
        private void LineTable(ProxyType type, ProxyMethod method) throws SDWPException, IOException {
            ProxyMethod.LineNumberTable lnt = method.getLineNumberTable();
            Assert.that(lnt != null); // if no information, it generates an empty table
            ProxyMethod.LineNumberTable.Entry[] entries = lnt.entries;
            out.writeLong(lnt.start, "start");
            out.writeLong(lnt.end, "end");
            out.writeInt(entries.length, "lines");
            for (int i = 0; i != entries.length; ++i) {
                out.writeLong(entries[i].lineCodeIndex, "lineCodeIndex");
                out.writeInt(entries[i].lineNumber, "lineNumber");
            }
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_Method_VariableTable">VariableTable</a>
         */
        private void VariableTable(ProxyType type, ProxyMethod method) throws SDWPException, IOException {
            ScopedLocalVariable[] vt = method.getVariableTable();

            Assert.that(vt != null); // if no information, it generates an empty table
            out.writeInt(method.getArgCount(), "argCnt");
            out.writeInt(vt.length, "slots");
            for (int i = 0; i != vt.length; ++i) {
                out.writeLong(vt[i].start, "codeIndex");
                out.writeString(vt[i].name, "name");
                out.writeString(DebuggerSupport.getJNISignature(vt[i].type), "signature");
                out.writeInt(vt[i].length, "length");
                out.writeInt(vt[i].slot, "slot");
            }
        }
    }

    /*-----------------------------------------------------------------------*\
     *                   ThreadGroupReference Command Set (12)               *
    \*-----------------------------------------------------------------------*/

    /**
     * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_ThreadGroupReference">ThreadGroupReference</a>.
     */
    static final class ThreadGroupReference extends SDPCommandSet {

        /**
         * {@inheritDoc}
         */
        protected boolean dispatch() throws IOException, SDWPException {
            ObjectID threadGroupID = in.readObjectID("group");
            if (!threadGroupID.equals(ObjectID.THREAD_GROUP)) {
                throw new SDWPException(JDWP.Error_INVALID_THREAD_GROUP, "invalid thread group ID: " + threadGroupID);
            }
            switch (command.command()) {
                case JDWP.ThreadGroupReference_Name_COMMAND:     Name();     break;
                case JDWP.ThreadGroupReference_Parent_COMMAND:   Parent();   break;
                case JDWP.ThreadGroupReference_Children_COMMAND: Children(); break;
                default: return false;
            }
            return true;
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_ThreadGroupReference_Children">Children</a>.
         */
        private void Children() throws IOException, SDWPException {
            Collection threads = sdp.getTPM().getThreads();
            Assert.that(!threads.isEmpty());
            out.writeInt(threads.size(), "childThreads");
            for (Iterator i = threads.iterator(); i.hasNext(); ) {
                ProxyThread thread = (ProxyThread) i.next();
                out.writeObjectID(thread.id, "childThread");
            }

            // Number of child groups
            out.writeInt(0, "childGroups");
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_ThreadGroupReference_Parent">Parent</a>.
         */
        private void Parent() throws IOException {
            out.writeObjectID(ObjectID.NULL, "parentGroup");
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_ThreadGroupReference_Name">Name</a>.
         */
        private void Name() throws IOException {
            out.writeString("Squawk_System", "groupName");
        }
    }

    /*-----------------------------------------------------------------------*\
     *                        EventRequest Command Set (15)                  *
    \*-----------------------------------------------------------------------*/

    /**
     * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_EventRequest">EventRequest</a>
     */
    static final class EventRequest extends SDPCommandSet {

        /**
         * {@inheritDoc}
         */
        protected boolean dispatch() throws IOException, SDWPException {
            switch (command.command()) {
                case JDWP.EventRequest_Set_COMMAND:    return Set();
                case JDWP.EventRequest_Clear_COMMAND:  return Clear();
                default: return false;
            }
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_EventRequest_EventRequestSet">EventRequestSet</a>
         *
         * @return true if the event request is handled by the SDP, false if it should be passed through to the SDA
         */
        private boolean Set() throws SDWPException, IOException {
            int id = sdp.eventManager.registerEventRequest(in);
            if (id == -1) {
                return false;
            }
            out.writeInt(id, "requestID");
            return true;
        }

        /**
         * Implements <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_EventRequest_EventRequestClear">EventRequestClear</a>
         *
         * @return true if the event request is handled by the SDP, false if it should be passed through to the SDA
         */
        private boolean Clear() throws SDWPException, IOException {
            int eventKind = in.readByte("eventKind");
            int requestID = in.readInt("requestID");
            if (requestID == 0) {
                if (Log.info()) {
                    Log.log("Requests with ID of zero are reserved for automatic events, and can't be cleared. Fail without error.");
                }
                return true;
            }

            return sdp.eventManager.clear(eventKind, requestID);
        }

        /**
         * {@inheritDoc}
         *
         * This handler intercepts the reply to an initial request to register for a CLASS_PREPARE event
         * that some debugger clients send (e.g. NetBeans, JBuilder). The proxy will pass
         * the request onto the VM but will also send a CompositeEvent back to the debugger client
         * for all the classes that the VM sent over in its handshake. This minimizes
         * a lot of startup traffic between the proxy and the VM.
         */
        protected void sentReply(ReplyPacket reply) throws IOException {
        }
    }
}
