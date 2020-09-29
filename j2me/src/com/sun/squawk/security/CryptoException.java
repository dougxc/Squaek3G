/*
 * @(#)CryptoException.java	1.8 02/09/17 @(#)
 *
 * Copyright (c) 2000-2005 Sun Microsystems, Inc.  All rights reserved.
 * PROPRIETARY/CONFIDENTIAL
 * Use is subject to license terms.
 */

package com.sun.squawk.security;

/**
 * <code>CryptoException</code> represents a cryptography-related exception.
 *
 * @see com.sun.squawk.security.verifier.SHA
 * @see com.sun.squawk.security.signing.SHA 
 * @see com.sun.squawk.security.signing.ECDSASignature
 * @see com.sun.squawk.security.signing.PseudoRand
 */ 
public class CryptoException extends RuntimeException {

    /**
     * This reason code is used to indicate that one or more input parameters is
     * out of allowed bounds.
     */
    public static final int ILLEGAL_VALUE     = 1;
    
    /**
     * This reason code is used to indicate that the key is uninitialized.
     */
    public static final int UNINITIALIZED_KEY = 2;

   
    /**
     * This reason code is used to indicate that the signature or cipher object
     * has not been correctly initialized for the requested operation.
     */
    public static final int INVALID_INIT      = 4;

    /**
     * This reason code is used to indicate that the signature or cipher
     * algorithm does not pad the incoming message and the input message is not
     * block aligned.
     */
    public static final int ILLEGAL_USE       = 5;
    
    // Reason code for this crytographic exception.
    private int reason;
    

    /**
     * Constructs a <code>CryptoException</code> with the specified reason.
     * @param reason the reason for the exception
     */ 
    public CryptoException(int reason) {
	this.reason = reason;
    }
    
    /**
     * Gets the reason code
     * @return the reason for the exception
     */
    public int getReason() {
        return reason;
    }
    
    /**
     * Sets the reason code
     * @param reason the reason for the exception
     */
    public int setReason(int reason) {
        return reason;
    }
    
    /**
     * Throws a <code>CryptoException</code> with the specified reason.
     * @param reason the reason for the exception
     * @throws com.sun.squawk.security.CryptoException always
     */ 
    public static void throwIt(int reason) throws CryptoException {
        throw new CryptoException(reason);
    }
    
    /**
     * Returns a human readable string describing this exception.
     * @return string representation of this exception
     */ 
    public String toString() {
        StringBuffer tmp = new StringBuffer();
        tmp.append("javacard.security.CryptoException (");
	switch (reason) {
         case ILLEGAL_USE:
	    tmp.append("Illegal use");
	    break;
	 case ILLEGAL_VALUE:
	    tmp.append("Illegal value");
	    break;
	 case INVALID_INIT:
	    tmp.append("Invalid initialization");
	    break;
	 case UNINITIALIZED_KEY:
	    tmp.append("Uninitialized key");
	    break;
	}
        tmp.append(")");
	return tmp.toString();
    }
}
