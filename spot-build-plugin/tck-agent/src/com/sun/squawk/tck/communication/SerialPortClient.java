/*
 * @(#)HttpClient.java	1.9 03/01/07
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.sun.squawk.tck.communication;

import java.io.*;

import javax.microedition.io.*;

import com.sun.squawk.util.*;

/**
 * An implementation of a Client that is able to communicate with the JavaTest HttpServer implementation.
 * 
 * @author Eric Arseneau
 *
 */
public class SerialPortClient implements com.sun.cldc.communication.MultiClient {
    private String stream_url;
    private String BundleID;
    private boolean verbose = false;
    protected StreamConnection connection;
    protected LineReader serialReader;
    protected BufferedWriter serialWriter;

    protected int characterForHexDigit(int digit) {
        if (digit >= 0 && digit <= 9) {
            return '0' + digit;
        } else if (digit >=10 && digit <= 15) {
            return 'a' + (digit - 10);
        }
        throw new RuntimeException("Illegal hex digit: " + digit);
    }
    
    public byte[] getNextTest() {
        try {
            String dest = stream_url + "getNextTest" + BundleID;
            trace("Opening connection to " + dest);
            serialWriter.write(SerialPortClientConstants.COMMAND_MARKER);
            serialWriter.newLine();
            serialWriter.write(SerialPortClientConstants.COMMAND_GET);
            serialWriter.newLine();
            serialWriter.write(dest);
            serialWriter.newLine();
            serialWriter.flush();
            String responseCode = serialReader.readLine();
            trace("Reading data..");
            if (responseCode.equals(SerialPortClientConstants.STATUS_OK)) {
                // status ok
                String bytesSizeString = serialReader.readLine();
                int bytesSize = Integer.parseInt(bytesSizeString);
                ByteArrayOutputStream bytesOut = new ByteArrayOutputStream(bytesSize);
                String line;
                while ((line = serialReader.readLine()).length() !=0) {
                    // the line constists of HEX digits, decode the hex digits and build up the byte array
                    char[] lineChars = line.toCharArray();
                    boolean highNibble = true;
                    int accumulator = 0;
                    for (int i=0, maxI=lineChars.length; i < maxI; i++) {
                        int digit = Character.digit(lineChars[i], 16);
                        if (highNibble) {
                            accumulator = digit <<= 4;
                            highNibble = false;
                        } else {
                            accumulator |= digit;
                            bytesOut.write(accumulator);
                            highNibble = true;
                        }
                    }
                }
                if (bytesSize == 0) {
                    return null;
                }
                byte[] bytes = bytesOut.toByteArray();
                return bytes;
            } if (responseCode.equals(SerialPortClientConstants.STATUS_ERROR)) {
                // status not ok
                String errorMessage = serialReader.readLine();
                throw new IOException(errorMessage);
            } else {
                throw new RuntimeException("Unknown response code: " + responseCode);
            }
        } catch (IOException e) {
            throw new RuntimeException ("Unexpected IOException: "+e);
        }
    }

    public void init(String[] args) {
        stream_url = args[0] + (args[0].endsWith("/") ? "" : "/");
        if ((args.length > 1) && "-verboseClient".equals(args[1])) {
            verbose = true;
        }
        try {
            connection = (StreamConnection) Connector.open("serial://");
            serialReader = new LineReader(new InputStreamReader(connection.openInputStream()));
            serialWriter = new BufferedWriter(new OutputStreamWriter(connection.openOutputStream()));
        } catch (IOException e) {
            throw new RuntimeException("Problems opening in/out serial://" + e);
        }
    }

    public void sendTestResult(byte[] result) {
        try {
            String dest = stream_url + "sendTestResult" + BundleID;
            trace("Opening connection to " + dest);
            serialWriter.write(SerialPortClientConstants.COMMAND_MARKER);
            serialWriter.newLine();
            serialWriter.write(SerialPortClientConstants.COMMAND_PUT);
            serialWriter.newLine();
            serialWriter.write(dest);
            serialWriter.newLine();
            serialWriter.flush();
            serialWriter.write(String.valueOf(result.length));
            serialWriter.newLine();
            trace("Posting data..");
            for (int i=0, maxI=result.length; i < maxI; i++) {
                int current = result[i] & 0xFF;
                serialWriter.write(characterForHexDigit(current>>4));
                serialWriter.write(characterForHexDigit(current&15));
                if (i > 0 && ((i % 64) == 0)) {
                    serialWriter.newLine();
                }
            }
            // blank line to indicate end of lines of hex digits
            serialWriter.newLine();
            serialWriter.newLine();
        } catch (IOException e) {
            throw new RuntimeException ("Unexpected IOException: "+e);
        }
    }
    
    public void setBundleID(String BundleID) {
        this.BundleID = "/" + BundleID;
    };
    
    protected void trace(String s) {
        if(verbose) {
            System.out.println("SERIAL CLIENT: " + s);
        }
    }

}
