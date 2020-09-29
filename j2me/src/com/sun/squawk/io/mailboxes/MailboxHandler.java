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

/**
 * The MailboxHandler is responsible for creating a logical connection to the client. It may modify the
 * MailboxAddress that the client will use for future messages, and it may create and manage custom state
 * for that logical connection.
 */
public interface MailboxHandler {

    /**
     * Called when a client looks up a MailboxAddress. A handler can decide create a new Mailbox to handle
     * this logical connection, or simply use the current Mailbox. A handler can record other state to manage
     * this logical connection.
     *
     * @param originalMailbox the original Mailbox connected to.
     * @param originalAddress a new private MailboxAddress created by the system for the client to use
     *                        communicate with Mailbox.
     * @param replyAddress the address to reply to.
     * @return a MailboxAddress that the client should continue to use for the rest of it's communication.
     *         The <code>originalAddress</code> is often returned, but a MailboxHandler can create a new
     *         new private Mailbox and return a MailboxAddress to this new
     */
    MailboxAddress handleOpen(Mailbox originalMailbox, MailboxAddress originalAddress, MailboxAddress replyAddress);

    /**
     * Called after a client closes a logical connection. The handler can clean up after a logical connection,
     * and can control if getting a close should cause any Mailbox.receive() calls on this Mailbox to throw a
     * AddressClosedException. Typically registered "server" mail boxes should not throw an exception, but
     * private mail boxes should.
     *
     * @param address the closed MailboxAddress.
     * @return null, or an exception to be thrown by Mailbox.receive().
     */
    AddressClosedException handleClose(MailboxAddress address);
    
}
