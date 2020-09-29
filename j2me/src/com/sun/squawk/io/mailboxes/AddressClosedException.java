/*
 * Copyright 2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */

package com.sun.squawk.io.mailboxes;

import java.io.IOException;
import com.sun.squawk.util.Assert;

/**
 * This exception is thrown when trying to send to a closed MailboxAddress,
 * or when reading from a Mailbox, and the MailBoxHandler is managing
 * a private address that has closed.
 * 
 */
public class AddressClosedException extends IOException{
    
    private MailboxAddress closedAddress;
    
    /** Creates a new instance of AddressClosedException */
    public AddressClosedException(MailboxAddress closedAddress) {
        this.closedAddress = closedAddress;
    }
    
    public MailboxAddress getClosedAddress() {
        return closedAddress;
    }
    
}
