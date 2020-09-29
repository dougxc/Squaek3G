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
import com.sun.squawk.util.SimpleLinkedList;

/**
 * Given that a Channel is a one-to-one connection between two isolates, a ServerChannel 
 * provides a factory to create new Channels by name. It is similar to how network sockets
 * can use a port umber to accept a number of client connections.
 * 
 * A server can use a the <code>accept</code> method to accept new client connections, which will return a new Channel
 * that the server can use to talk to the client. A server may choose to service each Channel in a seperate thread.
 *
 */
public class ServerChannel {
    /**
     * The named mailbox that clients will initially talk to.
     */
    private Mailbox serverBox;
    
    /**
     * A queue of Chnnales for each client that has looked up this server channel, but have not yet been
     * accepted.
     */
    private SimpleLinkedList unacceptedChannels;
    
    /**
     * Creates a new instance of ServerChannel
     */
    private ServerChannel() {
        unacceptedChannels = new SimpleLinkedList();
    }
    
    /**
     * Creates a new ServerChannel and new Mailbox with the given name and registers it with the system.
     *
     * When a client does a lookup on this mailbox, the ServerChannel creates a new private mailbox,
     * and tells the client to use the private mailbox.
     * 
     * Given a ServerChannel, a server may call accept(), waiting for clients to connect to it. Accept will return
     * with a new Channel (and private mailbox) to handle communication with this client.
     *
     * @param name        the name that this Mailbox can be looked up under.
     * @param handler     the class used to manage clients opening and closing new logical connections to the new Mailbox.
     * @return the new ServerChannel
     * @throws MailboxInUseException if there is already a mailbox registered under the name <code>name</code>.
     */
    public static ServerChannel create(String name) throws MailboxInUseException {
        final ServerChannel server = new ServerChannel();
        
        final Mailbox serverBox = Mailbox.create(name, new SharedMailboxHandler() {
            public MailboxAddress handleOpen(Mailbox originalMailbox, MailboxAddress originalAddress, MailboxAddress replyAddress) {
                // Create a new mailbox, and private address for it. Create a Channel wrapping all, and enqueue it for
                // accept() to find.
                Mailbox channelInBox = originalMailbox.createSubMailbox();
                MailboxAddress channeInboxAddress = new MailboxAddress(channelInBox);
                Channel newChannel = new Channel(replyAddress, channelInBox);
                
                SimpleLinkedList unacceptedChannels = server.unacceptedChannels;
                synchronized (unacceptedChannels) {
                    unacceptedChannels.addFirst(newChannel);
                    unacceptedChannels.notifyAll();
                }
                
                return channeInboxAddress;
            }
        });
        server.serverBox = serverBox;
        return server;
    }
    
    /**
     * Get the name that this ServerChannel was registered under.
     *
     * @return the name
     */
    public String getName() {
        return serverBox.getName();
    }
    
    /**
     * Wait for a client to open a connection, then create an anonymous local mailbox to use or further communication.
     *
     * @todo What happens on close?
     * @return a new Channel to the client
     */
    public Channel accept() {
        synchronized (unacceptedChannels) {
            while (unacceptedChannels.size() == 0) {
                try {
                    unacceptedChannels.wait();
                } catch (InterruptedException e) {
                }
            }
            return (Channel)unacceptedChannels.removeLast();
        }
    }
    
    /**
     * Unregisters this ServerChannel and it's MailBox.
     * Should this close existing Channels that came from this channel?
     */
    public void close() {
        if (serverBox.isOpen()) {
            serverBox.close();
        }
    }
}
