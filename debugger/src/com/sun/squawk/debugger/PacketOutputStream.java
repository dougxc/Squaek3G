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

import com.sun.squawk.debugger.DataType.*;
import com.sun.squawk.util.*;

/**
 * A PacketOutputStream is used to write data to the data part of a {@link Packet}.
 *
 * @author  Doug Simon
 */
public final class PacketOutputStream {

    private final DataOutputStream dos;

    public PacketOutputStream(DataOutputStream dis) {
        this.dos = dis;
    }

    /**
     * Writes one byte to this stream.
     *
     * @param value  a <code>byte</code> value to be written.
     * @param s      prefix to use if this write is logged. A value of null prevents logging altogether.
     * @throws IOException if there was an IO error while writing
     */
    public void writeByte(int value, String s) throws IOException {
        if (s != null && Log.verbose()) Log.log("out[byte]     " + s + "=" + value);
        dos.writeByte(value);
    }

    /**
     * Writes a boolean value to this stream.
     *
     * @param value  a <code>boolean</code> value to be written.
     * @param s      prefix to use if this write is logged. A value of null prevents logging altogether.
     * @throws IOException if there was an IO error while writing
     */
    public void writeBoolean(boolean value, String s) throws IOException {
        if (s != null && Log.verbose()) Log.log("out[boolean]  " + s + "=" + value);
        dos.writeBoolean(value);
    }

    /**
     * Writes a char value to this stream.
     *
     * @param value  a <code>char</code> value to be written.
     * @param s      prefix to use if this write is logged. A value of null prevents logging altogether.
     * @throws IOException if there was an IO error while writing
     */
    public void writeChar(char value, String s) throws IOException {
        if (s != null && Log.verbose()) Log.log("out[string]   " + s + "=" + value);
        dos.writeChar(value);
    }

    /**
     * Writes a short value to this stream.
     *
     * @param value  a <code>short</code> value to be written.
     * @param s      prefix to use if this write is logged. A value of null prevents logging altogether.
     * @throws IOException if there was an IO error while writing
     */
    public void writeShort(short value, String s) throws IOException {
        if (s != null && Log.verbose()) Log.log("out[short]    " + s + "=" + value);
        dos.writeShort(value);
    }

    /**
     * Writes an int value to this stream.
     *
     * @param value  an <code>int</code> value to be written.
     * @param s      prefix to use if this write is logged. A value of null prevents logging altogether.
     * @throws IOException if there was an IO error while writing
     */
    public void writeInt(int value, String s) throws IOException {
        if (s != null && Log.verbose()) Log.log("out[int]      " + s + "=" + value);
        dos.writeInt(value);
    }

    /**
     * Writes a long value to this stream.
     *
     * @param value  a <code>long</code> value to be written.
     * @param s      prefix to use if this write is logged. A value of null prevents logging altogether.
     * @throws IOException if there was an IO error while writing
     */
    public void writeLong(long value, String s) throws IOException {
        if (s != null && Log.verbose()) Log.log("out[long]     " + s + "=" + value);
        dos.writeLong(value);
    }

/*if[FLOATS]*/
    /**
     * Writes a float value to this stream.
     *
     * @param value  a <code>float</code> value to be written.
     * @param s      prefix to use if this write is logged. A value of null prevents logging altogether.
     * @throws IOException if there was an IO error while writing
     */
    public void writeFloat(float value, String s) throws IOException {
        if (s != null && Log.verbose()) Log.log("out[float]    " + s + "=" + (int)value);
        dos.writeInt(Float.floatToIntBits(value));
    }

    /**
     * Writes a double value to this stream.
     *
     * @param value  a <code>double</code> value to be written.
     * @param s      prefix to use if this write is logged. A value of null prevents logging altogether.
     * @throws IOException if there was an IO error while writing
     */
    public void writeDouble(double value, String s) throws IOException {
        if (s != null && Log.verbose()) Log.log("out[double]   " + s + "=" + (long)value);
        dos.writeLong(Double.doubleToLongBits(value));
    }
/*end[FLOATS]*/

    /**
     * Writes a string to the underlying output stream using UTF-8
     * encoding in a machine-independent manner.
     * <p>
     * First, four bytes are written to the output stream as if by the
     * <code>writeInt</code> method giving the number of bytes to
     * follow. This value is the number of bytes actually written out,
     * not the length of the string. Following the length, each character
     * of the string is output, in sequence, using the UTF-8 encoding
     * for the character.
     *
     * @param value  a string to be written.
     * @param s      prefix to use if this write is logged. A value of null prevents logging altogether.
     * @throws IOException if there was an IO error while writing
     */
    public void writeString(String value, String s) throws IOException {
        if (s != null && Log.verbose()) Log.log("out[string]   " + s + "=" + value);

        // A String is encoded in a JDWP packet as a UTF-8 encoded array, not zero
        // terminated, preceded by a *four-byte* integer length.
        com.sun.squawk.util.DataOutputUTF8Encoder.writeUTF(value, dos, false);
    }

    public void writeObjectID(ObjectID value, String s) throws IOException {
        if (s != null && Log.verbose()) Log.log("out[object]   " + s + "=" + value);
        dos.writeInt(value.id);
    }

    public void writeReferenceTypeID(ReferenceTypeID value, String s) throws IOException {
        if (s != null && Log.verbose()) Log.log("out[type]     " + s + "=" + value);
        dos.writeInt(value.id);
    }

    public void writeTaggedObjectID(TaggedObjectID value, String s) throws IOException {
        if (s != null && Log.verbose()) Log.log("out[t-object] " + s + "=" + value);
        dos.writeByte(value.tag);
        dos.writeInt(value.id);
    }

    public void writeMethodID(MethodID value, String s) throws IOException {
        if (s != null && Log.verbose()) Log.log("out[method]   " + s + "=" + value);
        dos.writeInt(value.id);
    }

    public void writeFieldID(FieldID value, String s) throws IOException {
        if (s != null && Log.verbose()) Log.log("out[field]    " + s + "=" + value);
        writeReferenceTypeID(value.definingClass, "defining class");
        dos.writeInt(value.encoding);
    }

    public void writeFrameID(FrameID value, String s) throws IOException {
        if (s != null && Log.verbose()) Log.log("out[frame]    " + s + "=" + value);
        writeObjectID(value.threadID, null);
        dos.writeInt(value.frame);
    }

    public void writeLocation(Location value, String s) throws IOException {
        if (s != null && Log.verbose()) Log.log("out[location] " + s + "=" + value);
        dos.writeByte(value.tag);
        writeReferenceTypeID(value.definingClass, null);
        writeMethodID(value.method, null);
        dos.writeLong(value.offset);
    }

    public void writeNullLocation(String s) throws IOException {
        if (s != null && Log.verbose()) Log.log("out[location]  " + s + "=null");
        dos.writeByte(JDWP.TypeTag_CLASS);
        writeReferenceTypeID(ReferenceTypeID.NULL, null);
        dos.writeInt(0);
        dos.writeLong(0);
    }

    /**
     * Writes a primitive value to this stream preceeded by a tag describing the type
     * and data size (in bytes) of the value.
     *
     * @param tag   a <code>JDWP.Tag_...</code> value
     * @param value the value to write
     * @throws IOException if there was an IO error while writing
     */
    public void writePrimitive(byte tag, long value, String s) throws IOException {
        if (s != null && Log.verbose()) Log.log("out[t-prim]:  " + s + "=" + value);
        dos.writeByte(tag);
        switch (tag) {
            case JDWP.Tag_VOID:                                   break;
            case JDWP.Tag_BYTE:
            case JDWP.Tag_BOOLEAN:  dos.writeByte((byte)value);   break;
            case JDWP.Tag_CHAR:
            case JDWP.Tag_SHORT:    dos.writeShort((short)value); break;
            case JDWP.Tag_INT:
            case JDWP.Tag_FLOAT:    dos.writeInt((int)value);     break;
            case JDWP.Tag_LONG:
            case JDWP.Tag_DOUBLE:   dos.writeLong(value);         break;
            default: Assert.shouldNotReachHere();
        }
    }

    /**
     * Closes this stream and its underlying stream.
     *
     * @throws IOException
     */
    public void close(String s) throws IOException {
        dos.close();
    }

    /**
     * Sends the packet on which this stream was created to the packet's {@link Packet#owner owner}.
     *
     * @throws IOException if there was an IO error while sending the packet
     */
//    public void send() throws IOException {
//        packet.send();
//    }
}
