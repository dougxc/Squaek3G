/*
 * Copyright 2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */

package com.sun.squawk.io.mailboxes;

import com.sun.squawk.VM;
import com.sun.squawk.Isolate;
import com.sun.squawk.GC;
//import com.sun.squawk.ObjectMemorySerializer;
import com.sun.squawk.util.Assert;

/**
 * Abstract class for messages passed to MailBoxes. An envelope
 * contains both contents as well as MailBoxAddress of the mail box
 * that the message was sent to and the MailBoxAddress that should be used for replies.
 *
 * The system defines several kinds of envelopes, including ObjectEnvelopes and ByteArrayEnvelopes.
 *
 * Note that the conntents of the envelope should only be looked at by the receiver, or
 * inter-isolate pointers could be created. The getContents() method enforces this.
 */
public abstract class Envelope {
    
    private MailboxAddress toAddress;
    
    /**
     * Returns the MailBoxAddress that the envelope was sent to.
     *
     * @return the to address of the sent envelope, or null if the envelope has not been sent.
     */
    public MailboxAddress getToAddress() {
        return toAddress;
    }

    /**
     * Returns the MailBoxAddress to be used for any replies.
     *
     * @return the reply address of the sent envelope, or null if the envelope has not been sent.
     */
    public MailboxAddress getReplyAddress() {
        if (toAddress == null) {
            return null;
        }
        return toAddress.getReplyAddress();
    }

    /**
     * Return the contents of the envelope. This should only be called by the receiver of the 
     * envelope. All implementations should call checkCallContext.
     *
     * @return the contents of the envelope
     * @throws IllegalStateException if called before the envelopesent, or called by the sender.
     */
    public abstract Object getContents();
    
    /**
     * Address this envelope. Called by MailboxAddress.send().
     */
    final void setAddresses(MailboxAddress toAddress) {
        this.toAddress = toAddress;
    }
    
    /**
     * Check that this envelope has been sent, and that the caller's
     * isolate is the receiver of the envelope.
     *
     * This should be called by all implementations of getContents().
     *
     * @throws IllegalStateException if the conditions are not met.
     */
    protected void checkCallContext() throws IllegalStateException {
        if (getReplyAddress() == null) {
            throw new IllegalStateException("Envelope has not been sent.");
        } else if (!getReplyAddress().isOwner(Isolate.currentIsolate())) {
            throw new IllegalStateException("Calling isolate is not the receiver of the envelope.");
        }
    }
    
    /**
     * The copy() method is similar to Object.clone() in j2se, but the original envelope is 
     * "owned" by the sending isolate, and the copy will be "owned" by the receiving isolate.
     * 
     * The default Envelope.copy() method creates a new envelope of the actual type of
     * "this", and performs a shallow copy of the entire envelope. 
     * 
     * Note that all subclasses must be certain that the copy does not contain any direct pointers to objects 
     * from the sender's Isolate
     *
     * The copy() method itself should be careful not to store pointers to either the original or copied
     * object in static variables or any in other data structure. It's not defined which isolate will 
     * execute the copy() method.
     *
     * Called by the system once per Envelope, sometime between when the message is sent by MailBoxAddress.send(),
     * and when it is received by MailBox.receive().
     *
     * @return a copy of this Envelope
     */
    /* package-private*/ Envelope copy() {
        // note that this does NOT call a contructor - and neither does Object.clone() as far as I can tell.
        Envelope newEnv = (Envelope)VM.shallowCopy(this);
        return newEnv;
    }
    
}