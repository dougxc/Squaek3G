/*
 * @(#)HttpConnectionBase.java	1.14 03/01/07
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.sun.cldc.communication.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Hashtable;
import java.util.Enumeration;
import java.io.ByteArrayOutputStream;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

/**
 * This class implements the necessary functionality
 * for an HTTP connection. 
 */
public class HttpConnectionBase {
    public final static String GET = "GET";
    public static final int HTTP_OK = 200;
    public static final int HTTP_PARTIAL = 206;
    public final static String POST = "POST";
    public final static String HEAD = "HEAD";
    private int WAIT_RESPONSE = 60000;
    private int index;
    private String url;
    private String protocol;
    private String host;
    private String file;
    private String ref;
    private String query;
    private int port = 80;
    private int responseCode;
    private String responseMsg;
    private Hashtable reqProperties;
    private Hashtable headerFields;
    private String method;
    private int opens;
    private boolean connected;
    private boolean timeouts;
    private PrivateOutputStream poster;
    private int mode;
    private StreamConnection netIO;
    private DataOutputStream output;
    private DataInputStream input;
    private static final String encoding = "ISO8859_1";

    /**
     * create a new instance of this class.
     * We are initially unconnected.
     */
    public HttpConnectionBase() {
        reqProperties = new Hashtable();
        headerFields = new Hashtable();
        opens = 0;
        connected = false;
        method = GET;
        responseCode = -1;
        protocol = "http";
    }

    public void open(String url, int mode, boolean timeouts) throws IOException {
        if (opens > 0) {
            throw new IOException("already connected");
        }

        this.timeouts = timeouts;
        opens++;

        if (mode != Connector.READ && mode != Connector.WRITE
            && mode != Connector.READ_WRITE) {
            throw new IOException("illegal mode: " + mode);
        }

        this.url = new String(url);
        this.mode = mode;

        parseURL();
    }

    public void close() throws IOException {
        if (--opens == 0 && connected) disconnect();
    }

    public InputStream openInputStream() throws IOException {
        connect();
        opens++;
        return new PrivateInputStream(input);
    }

    public OutputStream openOutputStream() throws IOException {
        if (mode != Connector.WRITE && mode != Connector.READ_WRITE) {
            throw new IOException("read-only connection");
        }

        opens++;
        poster = new PrivateOutputStream();
        return poster;
    }

    class PrivateInputStream extends InputStream {
        private InputStream worker;

        public PrivateInputStream(InputStream worker) {
            this.worker = worker;
        }

        public int read() throws IOException {
            return worker.read();
        }

        public int read(byte[] buf, int offset, int length) throws IOException {
            return worker.read(buf, offset, length);
        }

        public int available() throws IOException {
            return worker.available();
        }

        public void close() throws IOException {
            worker.close();

            if (--opens == 0 && connected) disconnect();
        }
    }

    class PrivateOutputStream extends OutputStream {
        private ByteArrayOutputStream output;

        public PrivateOutputStream() {
            output = new ByteArrayOutputStream();
        }

        public void write(int b) throws IOException {
            output.write(b);
        }

        public void flush() throws IOException {
            if (output.size() > 0) connect();
        }

        public byte[] toByteArray() {
            return output.toByteArray();
        }

        public int size() {
            return output.size();
        }

        public void close() throws IOException {
            flush();

            if (--opens == 0 && connected) disconnect();
        }
    }

    public String getURL() {
        return url;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getHost() {
        return host;
    }

    public String getFile() {
        return file;
    }

    public String getRef() {
        return ref;
    }

    public String getQuery() {
        return query;
    }

    public int getPort() {
        return port;
    }

    public String getRequestMethod() {
        return method;
    }

    public void setRequestMethod(String method) throws IOException {
        if (connected) throw new IOException("connection already open");

        if (!method.equals(HEAD) && !method.equals(GET) && !method.equals(POST)) {
            throw new IOException("unsupported method: " + method);
        }

        this.method = new String(method);
    }

    public String getRequestProperty(String key) {
        return (String)reqProperties.get(key);
    }

    public void setRequestProperty(String key, String value) throws IOException {
        if (connected) throw new IOException("connection already open");
        reqProperties.put(key, value);
    }

    public int getResponseCode() throws IOException {
        connect();
        return responseCode;
    }

    public String getResponseMessage() throws IOException {
        connect();
        return responseMsg;
    }

    public long getLength() {
        try {connect();}
        catch (IOException x) {return -1;}
	    return getHeaderFieldInt("content-length", -1);
    }

    public String getType() {
        try {connect();}
        catch (IOException x) {return null;}
	    return getHeaderField("content-type");
    }

    public String getEncoding() {
        try {connect();}
        catch (IOException x) {return null;}
	    return getHeaderField("content-encoding");
    }

    public String getHeaderField(String name) {
        try {connect();}
        catch (IOException x) {return null;}
	    return (String)headerFields.get(name.toLowerCase());
    }

    public int getHeaderFieldInt(String name, int def) {
        try {connect();}
        catch (IOException x) {return def;}

    	try {
    	    return Integer.parseInt(getHeaderField(name));
    	} catch(Throwable t) {}

	    return def;
    }

    protected void connect() throws IOException {
        if (connected) return;
        int wait_time = 0;

        do {
            int attempt=1;
            while (true) {
                try {
                    // Open socket connection
                    netIO = (StreamConnection)Connector.open("socket://" + host + ":" + port);
                    break;
                }
                catch (IOException e) {
                    if (attempt++ > 10) // up to 10 attempts
                        throw e;
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ign) {}
                }
            }

            output = netIO.openDataOutputStream();

            if (poster != null && getRequestProperty("Content-Length") == null) {
                setRequestProperty("Content-Length", "" + poster.size());
            }

            String reqLine = method + " " + getFile()
                + (getRef().equals("") ? "" : "#" + getRef())
                + (getQuery().equals("") ? "" : "?" + getQuery())
                + " HTTP/1.0\r\n";
            output.write((reqLine).getBytes(encoding));

            Enumeration reqKeys = reqProperties.keys();
            while (reqKeys.hasMoreElements()) {
                String key = (String)reqKeys.nextElement();
                String reqPropLine = key + ": " + reqProperties.get(key) + "\r\n";
                output.write((reqPropLine).getBytes(encoding));
            }
            output.write("\r\n".getBytes(encoding));

            if (poster != null)
                output.write(poster.toByteArray());
            output.flush();

            input = netIO.openDataInputStream();
            readResponseMessage(input);
            readHeaders(input);

            if (responseCode < HTTP_OK || responseCode > HTTP_PARTIAL) {
                
                disconnect();
                input = null;
                if( timeouts && (wait_time+=100) > WAIT_RESPONSE ) {
                    throw new IOException("Can not connect to server (connection timeout)!");
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignore) {}
            }

        } while( input==null );

        connected = true;
    }

    private void readResponseMessage(InputStream in) throws IOException {
        String httpVer, line = readLine(in);
        int httpEnd, codeEnd;

        responseCode = -1;
        responseMsg = null;

        if ((line == null)
            || ((httpEnd = line.indexOf(' ')) < 0)
            || !(httpVer = line.substring(0, httpEnd)).startsWith("HTTP")
            || ((httpEnd + 1) >= line.length())
            || ((codeEnd = line.substring(httpEnd + 1).indexOf(' ')) < 0)
            || (line.length()  <= (codeEnd += (httpEnd + 1))))
            throw new IOException("malformed response message");
    
        try {
            responseCode = Integer.parseInt(line.substring(httpEnd + 1, codeEnd));
        } catch (NumberFormatException nfe) {
            throw new IOException("malformed response message");
        }
    
        responseMsg = line.substring(codeEnd + 1);
    }

    private void readHeaders(InputStream in) throws IOException {
        String line, key, value;
        int index;

        for (;;) {
            line = readLine(in);
            if (line == null || line.equals("")) return;

            index = line.indexOf(':');
            if (index < 0) throw new IOException("malformed header field");

            key = line.substring(0, index);
            if (key.length() == 0) throw new IOException("malformed header field");

            if (line.length() <= index + 2) value = "";
            else value = line.substring(index + 2);
            headerFields.put(key.toLowerCase(), value);
        }
    }

    private String readLine(InputStream input) {
        try {
            InputStreamReader in = new InputStreamReader(input, encoding);
            StringBuffer retVal = new StringBuffer();
            int c;
            while ((c = in.read()) != '\n') {
                if (c < 0) return null;
                if (c != '\r') retVal.append((char)c);
            }
            return retVal.toString();
        } catch (IOException e) {
            return null;
        }
    }

    protected void disconnect() throws IOException {
        if (netIO != null) {
            input.close();
            output.close();
    	    netIO.close();
    	}

        responseCode = -1;
        responseMsg = null;
        connected = false;
    }

    private String parseProtocol() throws IOException {
        int n = url.indexOf(':');
        if (n <= 0) throw new IOException("malformed URL");
        String token = url.substring(0, n);
        if (!token.equals("http")) {
            throw new IOException("protocol must be 'http'");
        }
        index = n + 1;
        return token;
    }

    private String parseHostname() throws IOException {
        String buf = url.substring(index);
        if (buf.startsWith("//")) {
            buf = buf.substring(2);
            index += 2;
        }
        int n = buf.indexOf(':');
        if (n < 0) n = buf.indexOf('/');
        if (n < 0) n = buf.length();
        String token = buf.substring(0, n);
        if (token.length() == 0) {
            throw new IOException("host name cannot be empty");
        }
        index += n;
        return token;
    }

    private int parsePort() throws IOException {
        int p = 80;
        String buf = url.substring(index);
        if (!buf.startsWith(":")) return p;
        buf = buf.substring(1);
        index++;
        int n = buf.indexOf('/');
        if (n < 0) n = buf.length();
        try { 
            p = Integer.parseInt(buf.substring(0, n));
            if (p <= 0) {
                throw new NumberFormatException();
            }
        }catch (NumberFormatException nfe) {
            throw new IOException("invalid port");
        }
        index += n;
        return p;
    }

    private String parseFile() throws IOException {
        String token = "/";
        String buf = url.substring(index);
        if (buf.length() == 0) return token;
        if (!buf.startsWith(token)) {
            throw new IOException("invalid path");
        }
        int n = buf.indexOf('#');
        if (n < 0) n = buf.indexOf('?');
        if (n < 0) n = buf.length();
        token = buf.substring(0, n);
        index += n;
        return token;
    }

    private String parseRef() throws IOException {
        String buf = url.substring(index);
        if (buf.length() == 0 || buf.charAt(0) == '?') return "";
        if (!buf.startsWith("#")) {
            throw new IOException("invalid ref");
        }
        int n = buf.indexOf('?');
        if (n < 0) n = buf.length();
        index += n;
        return buf.substring(1, n);
    }

    private String parseQuery() throws IOException {
        String buf = url.substring(index);
        if (buf.length() == 0) return "";
        if (!buf.startsWith("?")) {
            throw new IOException("invalid query");
        }
        return buf.substring(1);
    }

    protected synchronized void parseURL() throws IOException {
        index = 0;
        host = parseHostname();
        port = parsePort();
        file = parseFile();
        ref = parseRef();
        query = parseQuery();
    }

    public static InputStream openInputStream(String name)  throws IOException {
        HttpConnectionBase con = ConnectorOpen(name, Connector.READ, true);
        InputStream is = con.openInputStream();
        con.close();
        return is;
    }
    
    public static HttpConnectionBase ConnectorOpen(String name, int mode,
                                                   boolean timeouts)
        throws IOException {

        /* Test for null argument */
        if(name == null) {
            throw new IllegalArgumentException("Null URL");
        }

        /* Look for : as in "http:", "file:", or whatever */
        int colon = name.indexOf(':');

        /* Test for null argument */
        if(colon < 1) {
            throw new IllegalArgumentException("no ':' in URL");
        }

        String protocol;
        
        /* Strip off the protocol name */
        protocol = name.substring(0, colon);
        if (!(protocol.equals("http") || protocol.equals("testhttp")))
            throw new IllegalArgumentException("The " + protocol + " is not supported");
        
        /* Strip the protocol name from the rest of the string */
        name = name.substring(colon+1);
        
        /* Construct a new instance */
        HttpConnectionBase uc = new HttpConnectionBase();
        
        /* Open the connection, and return it */
        uc.open(name, mode, timeouts);
        return uc;
    }
}

