/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */

package com.sun.squawk.util;

import java.io.*;

public class SerializableIntHashtable extends IntHashtable implements Serializable {

    public SerializableIntHashtable(int initialCapacity) {
        super(initialCapacity);
    }

    public SerializableIntHashtable() {
        super();
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        count = in.readInt();
        threshold = in.readInt();
        int tableLength = in.readInt();
        table = new IntHashtableEntry[tableLength];
        for (int i=0; i < tableLength; i++) {
            IntHashtableEntry entry = null;
            while (true) {
                if (!in.readBoolean()) {
                    break;
                }
                IntHashtableEntry nextEntry = new IntHashtableEntry();
                nextEntry.key = in.readInt();
                nextEntry.value = in.readObject();
                if (entry == null) {
                    table[i] = nextEntry;
                    entry = nextEntry;
                } else {
                    entry.next = nextEntry;
                    entry = nextEntry;
                }
            }
        }
        rehash();
    }
    
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeInt(count);
        out.writeInt(threshold);
        int tableLength = table.length;
        out.writeInt(tableLength);
        for (int i=0; i < tableLength; i++) {
            IntHashtableEntry entry = table[i];
            while (entry != null) {
                out.writeBoolean(true);
                out.writeInt(entry.key);
                out.writeObject(entry.value);
                entry = entry.next;
            }
            out.writeBoolean(false);
        }
    }
    
}
