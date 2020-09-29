/*
 * @(#)HttpClient.java	1.9 03/01/07
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.sun.squawk.tck.communication;

import java.io.*;

import javax.microedition.io.*;

/**
 * This is the Client implementation that works with SpotStreamConnectionListener.  It is an interim solution that was an experiment
 * and is likely to be stale.  Keeping code in place, until we can replace with Martin's implementation.
 * 
 * TODO
 * 
 * @author Eric Arseneau
 *
 */
public class StreamConnectionClient implements com.sun.cldc.communication.MultiClient {

    private String portSpec;
    private String stream_url;
    private String BundleID;
    private boolean verbose = false;

    public void init(String[] args) {
        // TODO Must parameterize this value
        portSpec = "radio://18:100";
        stream_url = args[0] + (args[0].endsWith("/") ? "" : "/");
        if ((args.length > 1) && "-verboseClient".equals(args[1])) {
            verbose = true;
        }
    }

    public byte[] getNextTest() {
        StreamConnection connection = null;
        DataOutputStream output = null;
        DataInputStream input = null;
        try {
            String dest = stream_url + "getNextTest" + BundleID;
            trace("Opening connection to " + portSpec);
            connection = (StreamConnection) Connector.open(portSpec);
            output = connection.openDataOutputStream();
            trace("GET " + dest);
            output.writeUTF("GET");
            output.writeUTF(dest);
            output.flush();
            input = connection.openDataInputStream();
            int status = input.readByte();
            byte[] result;
            if (status == 0) {
                trace("Reading data..");
                int size = input.readInt();
                result = new byte[size];
                input.readFully(result);
            } else {
                String message = input.readUTF();
                trace("Read error message: " + message);
                result = null;
            }
            if(result == null || result.length==0)
                return null;
            trace("GOT: " + new String(result));
            return result;
        } catch (IOException e) {
            throw new RuntimeException ("Unexpected IOException: "+e);
        } finally {
            if (input != null) {try {input.close();} catch (IOException e) {}};
            if (output != null) {try {output.close();} catch (IOException e) {}};
            if (connection != null) {try {connection.close();} catch (IOException e) {}};
        }
    }

    public void sendTestResult(byte[] res) {
        StreamConnection connection = null;
        DataOutputStream output = null;
        try {
            String dest = stream_url + "sendTestResult" + BundleID;
            trace("Opening connection to " + portSpec);
            connection = (StreamConnection) Connector.open(portSpec);
            output = connection.openDataOutputStream();
            trace("PUT " + dest);
            output.writeUTF("PUT");
            output.writeUTF(dest);
            trace("Writing data..");
            if (res == null) {
                output.writeInt(0);
            } else {
                output.writeInt(res.length);
                output.write(res);
            }
        } catch (IOException e) {
            throw new RuntimeException ("Unexpected IOException: "+e);
        } finally {
            if (output != null) {try {output.close();} catch (IOException e) {}};
            if (connection != null) {try {connection.close();} catch (IOException e) {}};
        }
    }
    
    public void setBundleID(String BundleID) {
        this.BundleID = "/" + BundleID;
    };
    
    protected void trace(String s) {
        if(verbose) {
            System.out.println("DATAGRAM CLIENT: " + s);
        }
    }

// DEBUG

    public static void main (String [] args) {
        if (args.length == 0) {
            args = new String[] {"-verboseClient", "radiogram://18:10", "http://localhost"};
        }
        StreamConnectionClient client = new StreamConnectionClient();
        client.init(args);
        byte [] ba = client.getNextTest();
        client.sendTestResult(ba);
    }
}
