package com.sun.squawk.builder.commands;

import java.io.*;
import java.net.*;

import javax.microedition.io.*;

/**
 * An initial trial at using a SPOT in basestation mode to wirelessly communicate with other SPOTs.
 * This code is a little stale since the work on the DebugClient and SerialPortClient was done.
 * Should be replaced with implementation Martin is working on now.
 * 
 * @author Eric Arseneau
 *
 */
public class SpotStreamConnectionListener implements Runnable {
    protected boolean firstTime;
    
    public static byte[] getBytes(String urlString) throws IOException {
        InputStream input = null;
        try {
            URL url = new URL(urlString);
            input = new BufferedInputStream(url.openStream());
            ByteArrayOutputStream output = new ByteArrayOutputStream(128);
            byte buffer[] = new byte[1024];
            int readCount;
            while ((readCount = input.read(buffer, 0, buffer.length)) != -1) {
                output.write(buffer, 0, readCount);
            }
            byte [] bytes = output.toByteArray();
            return bytes;
        } finally {
            if (input != null) {try {input.close();} catch (IOException e) {}};
            input = null;
        }
    }

    public static byte[] putBytes(String urlString, byte[] bytes) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = null;
        OutputStream output = null;
        InputStream input = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            output = connection.getOutputStream();
            output.write(bytes, 0, bytes.length);
            output.flush();
            input = connection.getInputStream();
            ByteArrayOutputStream bytesOut = new ByteArrayOutputStream(128);
            byte[] buffer = new byte[256];
            int readCount;
            while ((readCount = input.read(buffer, 0, buffer.length)) != -1) {
                bytesOut.write(buffer, 0, readCount);
            }
            bytes = bytesOut.toByteArray();
            return bytes;
        } finally {
            if (output != null) {try {output.close();} catch (IOException e) {}};
            if (input != null) {try {input.close();} catch (IOException e) {}};
            if (connection != null) {connection.disconnect();};
        }
    }
    public SpotStreamConnectionListener(boolean firstTime) {
        this.firstTime = firstTime;
    }
    
    public void run() {
        if (firstTime) {
        }
        StreamConnection connection = null;
        DataInputStream input = null;
        DataOutputStream output = null;
        try {
            // TODO Need to parameterize this
            connection = (StreamConnection) Connector.open("radio://19:100");
            input = connection.openDataInputStream();
            output = connection.openDataOutputStream();
            System.out.println("Waiting");
            // TODO should really design this to loop until connection is closed, should
            // change design of client to only open connection once
            
            // Handle the GET from the client
            processNextCommand(input, output);
            // Handle the PUT from the client
            processNextCommand(input, output);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (input != null) {try {input.close();} catch (IOException e) {}};
            if (output != null) {try {output.close();} catch (IOException e) {}};
            if (connection != null) {try {connection.close();} catch (IOException e) {}};
        }
    }
    
    protected void processNextCommand(DataInputStream input, DataOutputStream output) throws IOException {
        String command = input.readUTF();
        System.out.println("Got command: " + command);
        if (command.equals("GET")) {
            String url = input.readUTF();
            System.out.println("  url: " + url);
            byte[] bytes = null;
            boolean gotException = false;
            try {
                bytes = SpotStreamConnectionListener.getBytes(url);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("      GET failed: " + e.getMessage());
                gotException = true;
                output.writeByte(-1);
                output.writeUTF(e.getMessage());
            }
            if (!gotException) {
                System.out.println("      GET RESULT: " + new String(bytes));
                output.write(0);
                output.writeInt(bytes.length);
                output.write(bytes);
            }
            output.flush();
        } else if (command.equals("PUT")) {
                String url = input.readUTF();
                int size = input.readInt();
                byte[] bytes = new byte[size];
                input.readFully(bytes);
                System.out.println("  url: " + url);
                System.out.println(" data: " + new String(bytes));
                boolean gotException = false;
                try {
                    bytes = SpotStreamConnectionListener.putBytes(url, bytes);
                    System.out.println("  got back: " + new String(bytes));
                } catch (IOException e) {
                    e.printStackTrace();
                    gotException = true;
                    output.writeByte(-1);
                    output.writeUTF(e.getMessage());
                }
                if (!gotException) {
                    output.write(0);
                    output.writeInt(0);
                }
                output.flush();
        } else {
            System.out.println("  unknown");
            output.write(-1);
            output.writeUTF("Unknown command");
        }
    }

}
