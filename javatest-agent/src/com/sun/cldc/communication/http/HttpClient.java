/*
 * @(#)HttpClient.java	1.9 03/01/07
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.sun.cldc.communication.http;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

import com.sun.cldc.communication.Client;

public class HttpClient implements com.sun.cldc.communication.MultiClient {

    private String stream_url;
    private String BundleID = null;
    private boolean verbose = false;
    static private ByteArrayOutputStream baos = new ByteArrayOutputStream();

    public void init(String[] args) {
        stream_url = args[0] + (args[0].endsWith("/") ? "" : "/");
        if ((args.length > 1) && "-verboseClient".equals(args[1])) {
            verbose = true;
        }
    }

    public byte[] getNextTest() {

        try {
            String dest = stream_url + "getNextTest" + BundleID;
            trace("Opening connection to " + dest);
            InputStream is = HttpConnectionBase.openInputStream(dest);
            baos.reset();
            int i;
            trace("Reading data..");
            while((i=is.read())>=0) 
                baos.write(i);
            byte [] result = baos.toByteArray();
            baos.reset();
            is.close();
            if(result.length==0)
                return null;
            else
                return result;

        } catch (IOException e) {
            throw new RuntimeException ("Unexpected IOException: "+e);
        }
    }

    public void sendTestResult(byte[] res) {

        try {
            HttpConnectionBase hc;
            String dest = stream_url + "sendTestResult" + BundleID;
            trace("Opening connection to " + dest);
            hc = HttpConnectionBase.ConnectorOpen(dest, Connector.READ_WRITE, false);
            hc.setRequestMethod("POST");
            trace("Posting data..");
            OutputStream os = hc.openOutputStream();
            for(int i=0; i<res.length; i++)
                os.write(res[i]);
            os.close();
            hc.close();
//            hc.openInputStream().close();

        } catch (IOException e) {
            throw new RuntimeException ("Unexpected IOException: "+e);
        }
    }
    
    public void setBundleID(String BundleID) {
        this.BundleID = "/" + BundleID;
    };
    
    protected void trace(String s) {
        if(verbose) {
            System.out.println("HTTP CLIENT: " + s);
        }
    }

// DEBUG

    public static void main (String [] args) {
        HttpClient cli = new HttpClient();
        cli.init(args);
        byte [] ba = cli.getNextTest();
        cli.sendTestResult(ba);
    }
}
