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
 * A <code>MethodMetadata</code> instance represents all the information
 * about a method body that is not absolutely required for execution. This
 * includes the information found in the JVM LineNumberTable and
 * LocalVariableTable class file attributes.
 *
 * @author  Doug Simon
 */
public final class MethodMetadata {

    private static boolean preserveLineNumberTables;
    private static boolean preserveLocalVariableTables;

    /**
     * @see #strip(MethodMetadata[])
     */
    static void preserveLineNumberTables() {
        preserveLineNumberTables = true;
    }

    /**
     * @see #strip(MethodMetadata[])
     */
    static void preserveLocalVariableTables() {
        preserveLocalVariableTables = true;
    }

    /**
     * The member ID for the method within the KlassMetadata.
     */
    private final int id;

    /**
     * The local variable table.
     *
     * @see  #getLocalVariableTable()
     */
    private final ScopedLocalVariable[] lvt;

    /**
     * The line number table.
     *
     * @see  #getLineNumberTable()
     */
    private final int [] lnt;

    /**
     * Creates a new <code>MethodMetadata</code> instance.
     *
     * @param methodID the Method the metadata is for
     * @param lnt      the table mapping instruction addresses to the
     *                 source line numbers that start at the addresses.
     *                 The table is encoded as an int array where the high
     *                 16-bits of each element is an instruction address and
     *                 the low 16-bits is the corresponding source line
     * @param lvt      the table describing the symbolic information for
     *                 the local variables in the method
     */
    public MethodMetadata(int methodID, ScopedLocalVariable[] lvt, int[] lnt) {
        this.id  = methodID;
        this.lvt = lvt;
        this.lnt = lnt;
    }

    /**
     * Creates a copy of this object with the line number table stripped if <code>lnt == false</code>
     * and the local variable table stripped if <code>lvt == false</code>. If both parameters are false, returns null.
     *
     * @param lnt  preserve the line number table
     * @param lvt  preserver the local variable table
     * @return the stripped copy of this object or null if <code>lnt == lvt == false</code>
     */
    MethodMetadata strip(boolean lnt, boolean lvt) {
        if (lnt || lvt) {
            return new MethodMetadata(id, lvt ? this.lvt : null, lnt ? this.lnt : null);
        } else {
            return null;
        }
    }

    /**
     * Creates a stripped copy of an array of MethodMetadata. The value of <code>metadatas</code>
     * is not modified. The line number tables are stripped if {@link #preserveLineNumberTables}
     * has not been called. The local variable tables are stripped if {@link #preserveLineNumberTables}
     * has not been called.
     *
     * @param metadatas  the array to create a stripped copy of
     * @return the stripped copy of <code>metadatas</code>
     */
    static MethodMetadata[] strip(MethodMetadata[] metadatas) {
        if (metadatas != null) {
            if (preserveLineNumberTables || preserveLocalVariableTables) {
                MethodMetadata[] mds = new MethodMetadata[metadatas.length];
                for (int i = 0; i != metadatas.length; ++i) {
                    MethodMetadata md = metadatas[i];
                    if (md != null) {
                        mds[i] = md.strip(preserveLineNumberTables, preserveLocalVariableTables);
                    }
                }
                return mds;
            }
        }
        return null;
    }

    /**
     * Get the member ID for the method.
     *
     * @return the id
     */
    int getID() {
        return id;
    }

    /**
     * Gets the table mapping instruction addresses to the source line numbers
     * that start at the addresses. The table is encoded as an int array where
     * the high 16-bits of each element is an instruction address and the low
     * 16-bits is the corresponding source line.
     *
     * @return the line number table or null if there is no line number
     *         information for the method
     */
    public int[] getLineNumberTable() {
        return lnt;
    }

    /**
     * Gets a table describing the scope, name and type of each local variable
     * in the method.
     *
     * @return the local variable table or null if there is no local variable
     *         information for the method
     */
    public ScopedLocalVariable[] getLocalVariableTable() {
        return lvt;
    }

}
