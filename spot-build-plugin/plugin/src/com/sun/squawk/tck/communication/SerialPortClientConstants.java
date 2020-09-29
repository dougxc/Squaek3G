package com.sun.squawk.tck.communication;

/**
 * This is a manual copy of the source file from spot-plugin-tck-agent.  In order to reduce magical constants throughout source.
 * 
 * Magic constants used in {@link SerialPortClient} and {@link JamSpotCommandDebugClientUI}
 * 
 * @author Eric Arseneau
 *
 */
public interface SerialPortClientConstants {
    /**
     * Special string that should show up in a line, to indicate that the next line is an actual command.
     */
    public static final String COMMAND_MARKER = "!!!COMMAND:";
    
    /**
     * GET command.
     */
    public static final String COMMAND_GET = "GET";
    
    /**
     * PUT command.
     */
    public static final String COMMAND_PUT = "PUT";

    /**
     * Commands that require a result code, use this to indicate OK status.
     */
    public static final String STATUS_OK = "0";
    
    /**
     * Commands that require a result code, use this to indicate ERROR status.
     * Next line that follows should be the actual error text.
     */
    public static final String STATUS_ERROR = "1";
}
