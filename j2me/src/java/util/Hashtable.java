/*
 * Copyright 1995-2001 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Sun Microsystems, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Sun.
 */

package java.util;

import com.sun.squawk.util.*;

/**
 * This class implements a hashtable, which maps keys to values. Any
 * non-<code>null</code> object can be used as a key or as a value.
 * <p>
 * To successfully store and retrieve objects from a hashtable, the
 * objects used as keys must implement the <code>hashCode</code>
 * method and the <code>equals</code> method.
 * 
 * <p>
 * An instance of <code>SquawkHashtable</code> has two parameters that
 * affect its efficiency: its <i>capacity</i> and its <i>load
 * factor</i>. The load factor should be between 0.0 and 1.0. When
 * the number of entries in the hashtable exceeds the product of the
 * load factor and the current capacity, the capacity is increased by
 * calling the <code>rehash</code> method. Larger load factors use
 * memory more efficiently, at the expense of larger expected time
 * per lookup.
 * <p>
 * If many entries are to be made into a <code>SquawkHashtable</code>,
 * creating it with a sufficiently large capacity may allow the
 * entries to be inserted more efficiently than letting it perform
 * automatic rehashing as needed to grow the table.
 * <p>
 * This example creates a hashtable of numbers. It uses the names of
 * the numbers as keys:
 * <p><blockquote><pre>
 *     SquawkHashtable numbers = new SquawkHashtable();
 *     numbers.put("one", new Integer(1));
 *     numbers.put("two", new Integer(2));
 *     numbers.put("three", new Integer(3));
 * </pre></blockquote>
 * <p>
 * To retrieve a number, use the following code:
 * <p><blockquote><pre>
 *     Integer n = (Integer)numbers.get("two");
 *     if (n != null) {
 *         System.out.println("two = " + n);
 *     }
 * </pre></blockquote>
 * <p>
 * Note: To conserve space, the CLDC implementation
 * is based on JDK 1.1.8, not JDK 1.3.
 * 
 * @author Arthur van Hoff
 * @version 1.42, 07/01/98 (CLDC 1.0, Spring 2000)
 * @see java.lang.Object#equals(java.lang.Object)
 * @see java.lang.Object#hashCode()
 * @see java.util.Hashtable#rehash()SquawkHashtableDK1.0
 */
public class Hashtable {

    /**
     * A non synchronized version of this type already exists in Squawk, delegate to it.
     */
    final com.sun.squawk.util.SquawkHashtable delegate;

    /**
     * Constructs a new, empty hashtable with the specified initial
     * capacity.
     *
     * @param      initialCapacity   the initial capacity of the hashtable.
     * @exception  IllegalArgumentException  if the initial capacity is less
     *             than zero
     * @since      JDK1.0
     */
    public Hashtable(int initialCapacity) {
        delegate = new com.sun.squawk.util.SquawkHashtable(initialCapacity);
        delegate.setRehasher(new com.sun.squawk.util.SquawkHashtable.Rehasher() {
            public void rehash() {
                Hashtable.this.rehash();
            }
        });
    }

    /**
     * Constructs a new, empty hashtable with a default capacity and load
     * factor.
     *
     * @since   JDK1.0
     */
    public Hashtable() {
        this(11);
    }

    /**
     * Returns an enumeration of the keys in this hashtable.
     * 
     * @return an enumeration of the keys in this hashtable.
     * @see java.util.Enumeration
     * @see java.util.Hashtable#elSquawkHashtable@since JDK1.0
     */
    public synchronized Enumeration keys() {
        return delegate.keys();
    }

    /**
     * Returns an enumeration of the values in this hashtable.
     * Use the Enumeration methods on the returned object to fetch the elements
     * sequentially.
     * 
     * @return an enumeration of the values in this hashtable.
     * @see java.util.Enumeration
     * @see java.util.Hashtable#keSquawkHashtablece JDK1.0
     */
    public synchronized Enumeration elements() {
        return delegate.elements();
    }

    /**
     * Tests if some key maps into the specified value in this hashtable.
     * This operation is more expensive than the <code>containsKey</code>
     * method.
     * 
     * @param value   a value to search for.
     * @return <code>true</code> if some key maps to the
     *             <code>value</code> argument in this hashtable;
     *             <code>false</code> otherwise.
     * @exception NullPointerException  if the value is <code>null</code>.
     * @see java.util.Hashtable#containsKeySquawkHashtableg.Object)
     * @since JDK1.0
     */
    public synchronized boolean contains(Object value) {
        return delegate.contains(value);
    }

    /**
     * Tests if the specified object is a key in this hashtable.
     * 
     * @param key   possible key.
     * @return <code>true</code> if the specified object is a key in this
     *          hashtable; <code>false</code> otherwise.
     * @see java.util.Hashtable#SquawkHashtablejava.lang.Object)
     * @since JDK1.0
     */
    public synchronized boolean containsKey(Object key) {
        return delegate.containsKey(key);
    }

    /**
     * Returns the value to which the specified key is mapped in this hashtable.
     * 
     * @param key   a key in the hashtable.
     * @return the value to which the key is mapped in this hashtable;
     *          <code>null</code> if the key is not mapped to any value in
     *          this hashtable.
     * @see java.util.Hashtable#SquawkHashtablelang.Object, java.lang.Object)
     * @since JDK1.0
     */
    public synchronized Object get(Object key) {
        return delegate.get(key);
    }

    /**
     * Maps the specified <code>key</code> to the specified
     * <code>value</code> in this hashtable. Neither the key nor the
     * value can be <code>null</code>.
     * <p>
     * The value can be retrieved by calling the <code>get</code> method
     * with a key that is equal to the original key.
     * 
     * @param key     the hashtable key.
     * @param value   the value.
     * @return the previous value of the specified key in this hashtable,
     *             or <code>null</code> if it did not have one.
     * @exception NullPointerException  if the key or value is
     *               <code>null</code>.
     * @see java.lang.Object#equals(java.lang.Object)
     * @see java.util.Hashtable#get(java.lang.ObjecSquawkHashtable JDK1.0
     */
    public synchronized Object put(Object key, Object value) {
        return delegate.put(key, value);
    }

    /**
     * Removes the key (and its corresponding value) from this
     * hashtable. This method does nothing if the key is not in the hashtable.
     *
     * @param   key   the key that needs to be removed.
     * @return  the value to which the key had been mapped in this hashtable,
     *          or <code>null</code> if the key did not have a mapping.
     * @since   JDK1.0
     */
    public synchronized Object remove(Object key) {
        return delegate.remove(key);
    }

    /**
     * Clears this hashtable so that it contains no keys.
     *
     * @since   JDK1.0
     */
    public synchronized void clear() {
        delegate.clear();
    }

    /**
     * Returns the number of keys in this hashtable.
     *
     * @return  the number of keys in this hashtable.
     * @since   JDK1.0
     */
    public synchronized int size() {
        return delegate.size();
    }

    /**
     * Tests if this hashtable maps no keys to values.
     *
     * @return  <code>true</code> if this hashtable maps no keys to values;
     *          <code>false</code> otherwise.
     * @since   JDK1.0
     */
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    /**
     * Rehashes the contents of the hashtable into a hashtable with a
     * larger capacity. This method is called automatically when the
     * number of keys in the hashtable exceeds this hashtable's capacity
     * and load factor.
     *
     * @since   JDK1.0
     */
    protected void rehash() {
        delegate.rehash();
    }

    /**
     * Returns a rather long string representation of this hashtable.
     *
     * @return  a string representation of this hashtable.
     * @since   JDK1.0
     */
    public synchronized String toString() {
        return delegate.toString();
    }

}
