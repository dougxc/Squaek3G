package com.sun.squawk.builder.commands;

import java.io.*;

import com.sun.spot.debug.*;
import com.sun.squawk.tck.communication.*;

/**
 * An IUI that can be attached to a DebugClient in order to enable the JamSpotCommand to deploy, execute,
 * and communicate with the TCK test bundles that need to be executed.  The DebugClient is in turn connected
 * to a single device.  The major functionality I provided
 * is the ability to perform simple HTTP GET and PUT requests.  This allows our custom TCK SerialPortClient
 * to get data to and from the JavaTest server.  However, the implementaton is generic and get GET and POST
 * to any URL specified.
 * 
 * One very interesting artifact of this code is the fact that it is dealing with a serial port.  Serial port IO is
 * somewhat different than dealing with a file.  Standard IO will have an EOF situation when the end of the
 * data stream is achieved.  With serial, it is really an infinite data stream, that is until the serial port is closed.
 * This means that there must be a way to semantically identify the end of a "command/request".  This is done
 * through the use of lines, where a line is ended with either a CR or LF, or CR/LF.  This means that we cannot
 * send binary data through the serial port, and that we must encode binary data in such a way as to fit in
 * with this concept of lines. 
 * 
 * @author Eric Arseneau
 *
 */
public class JamSpotCommandDebugClientUI implements IUI {
    /**
     * Initial state when instantiated, and indicates that we are waiting for the confirmation that
     * the debug client has been initialized and synchronized for the first time.
     */
    protected static final int STATE_WAITING_FOR_DEBUGCLIENT_INIT = 0;
    
    /**
     * State indicating waiting for the special command marker to be echoed.  Only once a command
     * marker is received, will the state be changed to waiting for an actual command.
     */
    protected static final int STATE_WAITING_FOR_COMMAND_MARKER = 1;
    
    /**
     * Whatever is echoed will be a command.
     */
    protected static final int STATE_WAITING_FOR_COMMAND = 2;
    
    /**
     * State indicating that GET parameter is next line to be echoed, which is the URL.
     */
    protected static final int STATE_WAITING_FOR_GET_PARAMETER = 3;

    /**
     * State indicating that PUT parameter is next line to be echoed, which is the URL.
     */
    protected static final int STATE_WAITING_FOR_PUT_PARAMETER = 4;
    
    /**
     * State indicating that the PUT byte count parameter is next line to be echoed.
     */
    protected static final int STATE_WAITING_FOR_PUT_BYTE_COUNT = 5;

    /**
     * State indicating that the PUT bytes parameter is next line to be echoed.  If line echoed is empty,
     * this indicates end of lines containing the bytes to be put.
     */
    protected static final int STATE_WAITING_FOR_PUT_BYTES = 6;

    /**
     * The SerialPortTarget connected to the device the DebugClient is connected to. 
     */
    protected SerialPortTarget serialPortTarget;
    
    /**
     * The JamSpotCommand that owns the DebugClient and UI.
     */
    protected JamSpotCommand builderCommand;
    
    /**
     * Keep track of what state UI is in, and how the next echoed line should be interpreted.  Should be one of <code>STATE_WAITING_FOR*</code>
     */
    protected int state;
    
    /**
     * The last URL specified by the last PUT command received.
     */
    protected String putUrl;
    
    /**
     * If in STATE_WAITING_FOR_PUT_BYTES state, then holds the accumulation of bytes received so far.
     * If in any other state, holds the last collection of bytes received for the last PUT command.
     */
    protected ByteArrayOutputStream putBytesOut;
    
    public JamSpotCommandDebugClientUI(JamSpotCommand builderCommand, SerialPortTarget serialPortTarget) {
        this.builderCommand = builderCommand;
        this.serialPortTarget = serialPortTarget;
        this.state = STATE_WAITING_FOR_DEBUGCLIENT_INIT;
    }

    public void info(String msg) {
        System.out.println(msg);
    }

    public void debugClientInitialized() {
        setState(STATE_WAITING_FOR_COMMAND_MARKER);
    }
    
    public void echoFromTarget(String message) {
        try {
            if (state == STATE_WAITING_FOR_DEBUGCLIENT_INIT) {
                // If we are waiting for init, then there may be a bunch of stuff coming in that we should ignore completely, until the init of debug client is completed
                return;
            }
            // Handle debug messages coming from CldcAgent, or from SerialPortClient
            if (message.startsWith("SERIAL CLIENT:") || message.startsWith("AGENT:")) {
                System.out.println(message);
                return;
            }
            boolean handled;
            switch (state) {
                case STATE_WAITING_FOR_COMMAND_MARKER:
                    handled = handleStateWaitingForCommandMarker(message);
                    break;
                case STATE_WAITING_FOR_COMMAND:
                    handled = handleStateWaitingForCommand(message);
                    break;
                case STATE_WAITING_FOR_GET_PARAMETER:
                    handled = handleStateWaitingForGetParameter(message);
                    break;
                case STATE_WAITING_FOR_PUT_PARAMETER:
                    handled = handleStateWaitingForPutParameter(message);
                    break;
                case STATE_WAITING_FOR_PUT_BYTE_COUNT:
                    handled = handleStateWaitingForPutByteCount(message);
                    break;
                case STATE_WAITING_FOR_PUT_BYTES:
                    handled = handleStateWaitingForPutBytes(message);
                    break;
                default:
                    handled = false;
                    break;
            }
            // Coded this way so I can more easily debug
            if (handled) {
//                System.out.println("Handled echo: " + message);
                return;
            } else {
                handleUnknown(message);
            }
        } catch (IOException e) {
            throw new RuntimeException("Problems on echoFromTarget with: " + message, e);
        }
    }

    /**
     * Since waiting for command, interepret <code>message</code> as a command and perform the requested operation.
     * Set our follow on state to be the processing of the command.
     * 
     * @param message Command being requested
     * @return true if command was recognized, false if not
     */
    protected boolean handleStateWaitingForCommand(String message) {
        if (message.equals(SerialPortClientConstants.COMMAND_GET)) {
            setState(STATE_WAITING_FOR_GET_PARAMETER);
            return true;
        } else if (message.equals(SerialPortClientConstants.COMMAND_PUT)) {
            setState(STATE_WAITING_FOR_PUT_PARAMETER);
            return true;
        }
        return false;
    }
    
    /**
     * Change state to be waiting for command, since we have received the special string that indicates that
     * the next line is an actual command, and not something to totally ignore.
     * 
     * If <code>message</code> is the special command marker, then our follow on state will be waiting for command.
     * 
     * @param message Any string, or special command marker sting {@link SerialPortClientConstants#COMMAND_MARKER}
     * @return true if message was special command marker pattern
     */
    protected boolean handleStateWaitingForCommandMarker(String message) {
        if (message.equals(SerialPortClientConstants.COMMAND_MARKER)) {
            setState(STATE_WAITING_FOR_COMMAND);
            return true;
        }
        return false;
    }
    
    /**
     * A GET commad was received, so not we are receiving the URL from which an HTTP GET should be done.
     * Go ahead and do the GET, and push the bytes down the serial line back to the device.
     * The format of the data sent back is a line indicating STATUS_OK, or STATUS_ERROR.  If ok, then
     * bytes follow, encoded in HEX and multiple lines.  If not ok, then a single line providing text
     * explaining possible cause of error.
     * 
     * Our follow on state will be waiting for command marker, since GET command processing is completed.
     * 
     * @param message
     * @return
     * @throws IOException
     */
    protected boolean handleStateWaitingForGetParameter(String message) throws IOException {
        String uri = message;
        try {
            byte[] bytes = SpotStreamConnectionListener.getBytes(uri);
            serialPortTarget.sendBootloaderCommand(SerialPortClientConstants.STATUS_OK);
            serialPortTarget.sendBootloaderCommand("\n");
            serialPortTarget.sendBootloaderCommand(String.valueOf(bytes.length));
            serialPortTarget.sendBootloaderCommand("\n");
            StringBuffer buffer = new StringBuffer(129);
            buffer = new StringBuffer(2);
            boolean atNewLine = true;
            // Encode the bytes into a series of lines of HEX numbers, to be decoded by device
            for (int i=0, maxI=bytes.length; i < maxI; i++) {
                int current = bytes[i] & 0xFF;
                buffer.setLength(0);
                buffer.append(Character.forDigit(current>>4, 16));
                buffer.append(Character.forDigit(current&15, 16));
                serialPortTarget.sendBootloaderCommand(buffer.toString());
                atNewLine = false;
                if (i > 0 && ((i % 64) == 0)) {
                    serialPortTarget.sendBootloaderCommand("\n");
                    atNewLine = true;
                }
            }
            if (!atNewLine) {
                serialPortTarget.sendBootloaderCommand("\n");
            }
            // blank line to indicate end of lines of hex digits
            serialPortTarget.sendBootloaderCommand("\n");
        } catch (IOException e) {
            serialPortTarget.sendBootloaderCommand(SerialPortClientConstants.STATUS_ERROR);
            serialPortTarget.sendBootloaderCommand("\n");
            serialPortTarget.sendBootloaderCommand(e.getMessage());
            serialPortTarget.sendBootloaderCommand("\n");
        }
        setState(STATE_WAITING_FOR_COMMAND_MARKER);
        return true;
    }
    
    /**
     * The PUT command has multiple parameters.  Message contains the number of bytes expected.
     * 
     * The follow on state is waiting for bytes.
     * 
     * @param message Number of bytes expected.
     * @return true
     */
    protected boolean handleStateWaitingForPutByteCount(String message) {
        putBytesOut = new ByteArrayOutputStream(Integer.parseInt(message));
        setState(STATE_WAITING_FOR_PUT_BYTES);
        return true;
    }
    
    /**
     * Process <code>message</code> as another line of bytes to be accumulated.  If <code>message</code> is empty,
     * this indicates there are no more bytes coming.
     * 
     * @param message Line of hex encoded bytes, or empty
     * @return true
     * @throws IOException
     */
    protected boolean handleStateWaitingForPutBytes(String message) throws IOException {
        if (message.length() == 0) {
            byte[] bytes = putBytesOut.toByteArray();
            SpotStreamConnectionListener.putBytes(putUrl, bytes);
            setState(STATE_WAITING_FOR_COMMAND_MARKER);
            return true;
        }
        // message consists of a line of HEX digits, decode the hex digits and build up the byte array
        char[] lineChars = message.toCharArray();
        boolean highNibble = true;
        int accumulator = 0;
        for (int i=0, maxI=lineChars.length; i < maxI; i++) {
            int digit = Character.digit(lineChars[i], 16);
            if (highNibble) {
                accumulator = digit <<= 4;
                highNibble = false;
            } else {
                accumulator |= digit;
                putBytesOut.write(accumulator);
                highNibble = true;
            }
        }
        return true;
    }
    
    /**
     * <code>message</code> is the URL to perform the PUT against.
     * 
     * @param message URL
     * @return true
     */
    protected boolean handleStateWaitingForPutParameter(String message) {
        putUrl = message;
        setState(STATE_WAITING_FOR_PUT_BYTE_COUNT);
        return true;
    }
    
    /**
     * <code>message</code> represents a line that was not parsed or handled by the current state.
     * 
     * @param message
     */
    protected void handleUnknown(String message) {
        System.err.println(message);
    }
    
    public void newProgress(int initialSteps, int totalSteps, String title) {
        System.out.println(title);
    }

    public void progressUpdate(int stepsComplete, String msg) {
        System.out.print(".");
        if (stepsComplete % 64 == 0) {
            System.out.println();
        }
    }

    public void progressEnd(String msg) {
        System.out.println();
        System.out.println(msg);
    }

    protected void setState(int state) {
        this.state = state;
    }
    
}
