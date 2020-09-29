/*
 *
 * @(#)Server.java	1.9 03/01/07
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */

package com.sun.cldc.communication;

/**
 * <p> The Server interface defines the high-level contract between the Server implementation 
 * and the JavaTest harness.
 * <p> This interface defines the means by which the JavaTest harness initializes 
 * the Server, changes its state, and passes in a TestProvider instance to 
 * exchange the test data with.
 * <p> An implementation of this interface should provide a no-arguments
 * public constructor.
 *
 */

public interface Server {

    /**
     * Initializes the Server with the specified arguments.<p>
     * This method is called by the JavaTest harness immediately 
     * after instantiating the Server.
     *
     * @param   args       initialization arguments.
     * @throws             IllegalArgumentException if the arguments
     *                     are invalid.
     */
    void init(String[] args);

    /**
     * Requests the Server to start execution. <p>
     * This method can only be called if {@link #init} returns 
     * successfully.
     * 
     */
    void start();

    /**
     * Requests the Server to stop execution.
     * 
     */
    void stop();

    /**
     * Sets the Test Provider. <p>
     * There can be only one Test Provider
     * per Server. The next call to setTestProvider removes the previous
     * Test Provider. A null argument removes the current Test Provider.
     * <br> This method is always guaranteed to be called when 
     * the Server is not active (e.g. before the {@link #start} 
     * method is called).
     *
     * @param   tp       the Test Provider.
     */
    void setTestProvider(TestProvider tp);

    /**
     * Returns the current Test Provider.
     *
     * @return    the current Test Provider. 
     */
    TestProvider getTestProvider();

}


