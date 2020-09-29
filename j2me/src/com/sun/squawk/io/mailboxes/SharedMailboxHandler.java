/*
 * Copyright 2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */

package com.sun.squawk.io.mailboxes;

import com.sun.squawk.Isolate;
import com.sun.squawk.util.Assert;


/*
 * SharedMailboxHandler is a utility handler for server-type Mailboxes that simply tells the system to use the default MailBoxAddress
 * when a new logical connection is opened, and doesn't do anything when these connections are closed.
 *
 * A more sophisticated use would use the handleOpen callback to associate other connection state with the 
 * Mailbox address used by the client for this connection. It could also use the handleClose callback to clean up the
 * associated state.
 */
public class SharedMailboxHandler implements MailboxHandler {
    
    /** Creates a new instance of SharedMailboxHandler */
    public SharedMailboxHandler() {
    }
    
    /**
     * The system has created an address to the Mailbox for the client to use, and we will pass that on to the client.
     *
     * @param originalAddress a new private MailboxAddress created by the system for the client to use
     *                        communicate with Mailbox.
     * @param replyAddress the address to reply to.
     * @return originalAddress
     */
    public MailboxAddress handleOpen(Mailbox originalMailbox, MailboxAddress originalAddress, MailboxAddress replyAddress) {
        return originalAddress;
    }

    /**
     * Called after a client closes a logical connection. The handler can clean up after a logical connection,
     * and can control if getting a close should cause any Mailbox.receive() calls on this Mailbox to throw a
     * AddressClosedException. Typically registered "server" mail boxes should not throw an exception, but
     * private mail boxes should.
     *
     * @param address the closed MailboxAddress.
     * @return null, or an exception to be thrown by Mailbox.receive().
     */
    public AddressClosedException handleClose(MailboxAddress address) {
        return null;
    }
   
}
