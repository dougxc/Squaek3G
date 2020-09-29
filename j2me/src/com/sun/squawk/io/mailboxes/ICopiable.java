/*
 * Copyright 2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */

package com.sun.squawk.io.mailboxes;

/**
 * Interface for objects that may be passed in ObjectEnvelopes between Isolates.
 * The copyFrom method allows a class to specify how objects should be copied.
 *
 * Note that an ICopiable object should a public no-args constructor in order to be
 * copiable.
 *
 * @author Dave Cleal
 */
public interface ICopiable {
    
    /** 
     * Set the state of this object based on the state of object <code>o</code>.
     *
     * This method should be careful not to store pointers to either the original or copied
     * object. The copyFrom is likely to be called in the context of the sending Isolate, but 
     * this object is destined for use by the receiving Isolate.
     *
     * @param o the object the copy from
     */
    public void copyFrom(Object o);
}
