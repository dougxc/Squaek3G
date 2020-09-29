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
 * Any method declared to throw this exception will be
 * turned into a native method by the translator.  The
 * translator will leave calls to the method intact.
 *
 * @see HostedPragma
 */
public class NativePragma extends PragmaException {

}
