/*
 * Copyright 2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM translator.
 */
 
package com.sun.squawk.pragma;

/**
 * Any method declared to throw this exception is only called
 * from within a hosted environment.  Such methods should be
 * either static or final.  They will not be translated, and
 * calls to them will be converted into dummy instructions by
 * the translator.
 *
 * @see NativePragma
 */
public class HostedPragma extends PragmaException {
    
}
