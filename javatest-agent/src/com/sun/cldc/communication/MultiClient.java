/*
 * @(#)MultiClient.java	1.3 03/01/07
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.sun.cldc.communication;

/**
 * <p> The MultiClient interface defines functionality required for 
 * parallel test execution.
 *
 * <p> It extends the Client with ability to pass the Id of the 
 * currently executing test bundle along with other data during 
 * each communication session. 
 *
 */


public interface MultiClient extends Client {

    /**
     * Sets the bundle Id for the Client.<p>
     * This method is called by the CldcAgent after calling
     * {@link Client#init}, but before calling any communication 
     * methods of the {@link Client}.
     *
     * @param   bundleId   the Id of the currently executing test bundle.
     */
    void setBundleID(String bundleId);
}

