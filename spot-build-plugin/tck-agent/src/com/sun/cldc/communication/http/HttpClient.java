/*
 * @(#)HttpClient.java	1.9 03/01/07
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.sun.cldc.communication.http;

import com.sun.cldc.communication.*;
import com.sun.squawk.tck.communication.*;

/**
 * Created as a proxy to an actual client instance.  This class must be located and named as is in order
 * to allow us to use the HttpServer implementation of the TCK as-is.  For some reason, cannot totally
 * figure out how to re-use the HttpServer and provide a different Client on device.
 * 
 * @author ea149956
 *
 */
public class HttpClient implements MultiClient {

    protected MultiClient delegate;
    
    public HttpClient() {
        // TODO Parameterize the class instantiated
        boolean useSerial = true;
        MultiClient client;
        if (useSerial) {
            client = new SerialPortClient();
        } else {
            client = new StreamConnectionClient();
        }
        delegate = client;
    }

    public void init(String[] args) {
        delegate.init(args);
    }

    public byte[] getNextTest() {
        return delegate.getNextTest();
    }

    public void sendTestResult(byte[] result) {
        delegate.sendTestResult(result);
    }
    
    public void setBundleID(String bundleID) {
        delegate.setBundleID(bundleID);
    };
    
// DEBUG

    public static void main (String [] args) {
        if (args.length == 0) {
            args = new String[] {"-verboseClient", "radiogram://18:10", "http://localhost"};
        }
        HttpClient client = new HttpClient();
        client.init(args);
        byte [] ba = client.getNextTest();
        client.sendTestResult(ba);
    }
}
