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
 * A ObjectEnvelope can be used to pass a copy of an ICopiable object between isolates.
 */
public class ObjectEnvelope extends Envelope {
    protected ICopiable contents;
    
    public ObjectEnvelope(ICopiable contents) {
        this.contents = contents;
    }

   /**
     * Return the contents of the envelope.
     */
    public Object getContents() {
        checkCallContext();
        return contents;
    }
    
    /**
     * The copy() method copies the contents of the envelope by calling the ICopiable.copyFrom method.
     * 
     * Called by the system once per Envelope, sometime between when the message is sent by MailBoxAddress.send(),
     * and when it is received by MailBox.receive().
     *
     * If the ICopiable contents class does not a have a public, no-args constructor, the 
     * contents will not be copied.
     *
     * @return a copy of this Envelope
     */
    /* package-private*/ Envelope copy() {
        ObjectEnvelope theCopy = (ObjectEnvelope)super.copy();
        theCopy.contents = null;
        try {
            theCopy.contents = (ICopiable)this.contents.getClass().newInstance();
            theCopy.contents.copyFrom(this.contents);
        } catch (InstantiationException ex) {
            ex.printStackTrace();
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
        }
        // on failure, contents will be null

        return theCopy;
    }

}
