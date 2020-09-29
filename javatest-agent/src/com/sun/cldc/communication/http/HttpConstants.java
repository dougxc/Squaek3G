/*
 * @(#)HttpConstants.java	1.4 03/01/07
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.sun.cldc.communication.http;
  
public interface HttpConstants {
    
    /**
     *  200: The request has succeeded.
     */
    public final int HTTP_OK             = 200;

    /**
     * 400: The request could not be understood by the server due to 
     *    malformed syntax.
     */
    public final int HTTP_BAD_REQUEST    = 400;

    /**
     * 404: The server has not found anything matching the Request-URI. 
     *    No indication is given of whether the condition is temporary 
     *    or permanent.
     */
    public final int HTTP_NOT_FOUND      = 404;

    /**
     * 405: The method specified in the Request-Line is not allowed for 
     *    the resource identified by the Request-URI.
     */
    public final int HTTP_BAD_METHOD     = 405;

    /**
     * 500: The server encountered an unexpected condition which prevented
     *    it from fulfilling the request.
     */
    public final int HTTP_SERVER_ERROR   = 500;

    /**
     * 503: The server is currently unable to handle the request due to a 
     *    temporary overloading or maintenance of the server.
     */
    public final int HTTP_UNAVAILABLE    = 503;
}
