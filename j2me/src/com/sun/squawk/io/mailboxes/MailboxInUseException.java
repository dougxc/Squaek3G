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
 * This exception is thrown when trying to send to register a mailbox under 
 * a name, and that name is already used.
 * 
 */
public class MailboxInUseException extends IOException{
    
    /** Creates a new instance of AddressClosedException */
    public MailboxInUseException(String name) {
        super("There is already a Mailbox registered under the name \"" + name + "\"");
    }
    
}
