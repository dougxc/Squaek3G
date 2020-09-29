/*
 * @(#)Client.java	1.6 03/01/07
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.sun.cldc.communication;

/**
 * <p> The Client interface provides a means for the CldcAgent to exchange data with the 
 * {@link TestProvider} via a {@link Server} implementation.
 *
 * <p> An implementation of this class should provide a no-arguments public
 * constructor.
 */

public interface Client {

    /**
     * Initializes the Client with the specified arguments.<p>
     * This method is called by the CldcAgent immediately after 
     * instantiating the Client.
     *
     * @param   args   initialization arguments.
     * @throws         IllegalArgumentException if the arguments are invalid.
     */
    void init(String[] args);

    /**
     * Obtains information about the next test from the {@link Server}.<p>
     * Returns null if all tests from the current test bundle have already
     * been executed. In other words, null is an exit signal to the 
     * CldcAgent.
     *
     * @return    the byte array with the encoded information required to 
     *            start up the test. Null if no tests are left to execute. 
     */
    byte[] getNextTest();

    /**
     * Sends the test result back to the {@link Server}.
     *
     * @param   res   the byte array with the encoded test result.
     */
    void sendTestResult(byte[] res);

}
