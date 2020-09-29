/*
 * SignatureVerifierException.java
 *
 * Created on 14. März 2006, 18:34
 *
  */

package com.sun.squawk.security.verifier;

/**
 *
 * @author Christian Pühringer
 */
public class SignatureVerifierException extends Exception{
    
    /**
     * Creates a new instance of SignatureVerifierException
     */
    public SignatureVerifierException(String msg) {
        super(msg);
    }
    
}
