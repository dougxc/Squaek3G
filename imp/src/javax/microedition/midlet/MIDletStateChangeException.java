/*
 * @(#)MIDletStateChangeException.java	1.11 01/08/21
 * Copyright (c) 1999-2001 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of Sun
 * Microsystems, Inc. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Sun.
 *
 * SUN MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF THE
 * SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. SUN SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 */

package javax.microedition.midlet;

import java.lang.String;

/**
 * Signals that a requested <code>MIDlet</code> state change failed. This
 * exception is thrown by the <code>MIDlet</code> in response to
 * state change calls
 * into the application via the <code>MIDlet</code> interface
 *
 * @see MIDlet
 */

public class MIDletStateChangeException extends Exception {

    /**
     * Constructs an exception with no specified detail message.
     */

    public MIDletStateChangeException() {}

    /**
     * Constructs an exception with the specified detail message.
     *
     * @param s the detail message
     */

    public MIDletStateChangeException(String s) {
	super(s);
    }

}
