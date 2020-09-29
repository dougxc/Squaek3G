/*
 * Copyright 2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */

package com.sun.squawk.io.mailboxes;

import com.sun.squawk.util.Assert;

/**
 * A Channel is a private bidirectional link between two MailBoxes. The Channel is a wrapper over the local mailbox and
 * the mailboxaddress of the remote mailbox.
 *
 * A Channel can be used to talk to any kind of registered Mailbox, including those created by ServerChannels, and 
 * those created by the raw Mailbox API.
 */
public class Channel {
    Mailbox inBox;
    MailboxAddress outBox;
    
    Channel(MailboxAddress outBox, Mailbox inBox) {
        this.outBox = outBox;
        this.inBox = inBox;
    }
    
    /**
     * Create a private MailBox, and connect to the named MailBox. Note that the server
     * side may or may not create a private mailbox to handle its side of the communication.
     */
    public static Channel lookup(String mailboxName) throws NoSuchMailboxException {
        Mailbox inBox = Mailbox.create(); // create anon. mailbox for messages from other isolate.
        MailboxAddress outBox = MailboxAddress.lookupMailbox(mailboxName, inBox);
        
        return new Channel(outBox, inBox);
    }
    
    /**
     * Sends a message to the remote Mailbox. Does not wait for acknowledgment. The Channel must not be closed
     * or an IllegateStateException will be thrown.
     *
     * @param env the message to send
     * @throws IllegateStateException if the address is not in the open state.
     */
    public void send(Envelope env) throws AddressClosedException {
        outBox.send(env);
    }
    
    /**
     * Wait for an envlope sent to this channel.
     *
     * Blocks waiting for messages.
     *
     * If the logical connection to this Mailbox is closed, an AddressClosedException will be thrown.
     *
     * @return an Envelope containing the sent message.
     * @throws AddressClosedException if the connection to this mailbox is closed.
     * @throws MailboxClosedException if the Channel itself is closed.
     */
    public Envelope receive() throws AddressClosedException, MailboxClosedException {
        return inBox.receive();
    }
    
    /**
     * Closes the Channel at both ends asynchronously.
     */
    public void close() {
        if (inBox.isOpen()) {
            inBox.close();
        }
        if (outBox.isOpen()) {
            outBox.close();
        }
    }

    /**
     * Return true if the channel is open, both from here to the remote mailbox, and from the remote mailbox back.
     *
     * @return true if open.
     */
    public boolean isOpen() {
        return inBox.isOpen() && outBox.isOpen();
    }
    
}
