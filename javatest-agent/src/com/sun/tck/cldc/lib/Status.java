/*
 * @(#)Status.java	1.6 03/01/07
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.sun.tck.cldc.lib;

/**
 * A class to embody the result of a test: a status-code and a related message.
 *
 * @author Jonathan J Gibbons
 */

public class Status
{
    /**
     * Create a Status that represents the successful outcome of a test.
     */
    public static Status passed(String reason) {
	return new Status(PASSED, reason);
    }

    /**
     * Create a Status that represents the unsuccessful outcome of a test.
     */
    public static Status failed(String reason) {
	return new Status(FAILED, reason);
    }

    /**
     * Create a Status that represents that the test completed, but that further
     * analysis of the output of the test against reference files is required.
     */
    public static Status error(String reason) {
	return new Status(ERROR, reason);
    }

    /**
     * A return code indicating that the test was executed and was successful.
     * @see #passed
     * @see #getType
     */
    public static final int PASSED = 0;

    /**
     * A return code indicating that the test was executed but the test
     * reported that it failed.
     * @see #failed
     * @see #getType
     */
    public static final int FAILED = 1;

    /**
     * A return code indicating that the test was not run because some error
     * occurred before the test could even be attempted. This is generally
     * a more serious error than FAILED.
     * @see #getType
     */
    public static final int ERROR = 2;

    /**
     * A return code indicating that the test has not yet been run in this context.  
     * (More specifically, no status file has been recorded for this test in the 
     * current work directory.)  This is for the internal use of the harness only.
     * @see #getType
     */
    public static final int NOT_RUN = 3;

    /**
     * Number of states which are predefined as "constants".
     */
    public static final int NUM_STATES = 4;

    /**
     * Get a type code indicating the type of Status message this is.
     * @see #PASSED
     * @see #FAILED
     * @see #ERROR
     */
    public int getType() {
	return type;
    }

    /**
     * Get the message given when the status was created.
     */
    public String getReason() {
	return reason;
    }

    /**
     * Standard routine.
     */
    public String toString() {
	if (reason == null || reason.length() == 0)
	    return texts[type];	
	else
	    return texts[type] + " " + reason;
    }

    /**
     * Convenience exit() function for the main() of tests to exit in such a 
     * way that the status passes up across process boundaries without losing
     * information (ie exit codes don't give the associated text of the status
     * and return codes when exceptions are thrown could cause unintended 
     * results). <p>
     *
     * An identifying marker is written to the error stream, which the script
     * running the test watches for as the last output before returning, 
     * followed by the type and reason
     *
     * The method does not return.  It calls System.exit with a value
     * dependent on the type.
     */
    public void exit() {
	if (System.err != null) {
	    System.err.print(EXIT_PREFIX);
	    System.err.print(texts[type]);
	    System.err.println(reason);
	    System.err.flush();
	}
	System.exit(exitCodes[type]);
    }


    //-----internal routines-------------------------------------------

    private Status(int type, String reason) { 
	// if we find any bad characters in the reason string (e.g. newline)
	// we rewrite the string replacing all such characters with a space.
	for (int i = 0; i < reason.length(); i++) {
	    if (!isPrintable(reason.charAt(i))) {
		StringBuffer r = new StringBuffer(reason.length());
		for (int j = 0; j < reason.length(); j++) {
		    char c = reason.charAt(j);
		    r.append(isPrintable(c) ? c : ' ');
		}
		reason = r.toString();
		break;
	    }
	}

	this.type = type; 
//	this.reason = reason.trim(); 
	this.reason = reason; 
    }

    private static final boolean isPrintable(char c) {
	return (32 <= c && c < 127);
    }

    //----------Data members------------------------------------------

    private /*final*/ int type;
    private /*final*/ String reason;

    public static final String EXIT_PREFIX = "STATUS:";

    private static String[] texts = {  
	// correspond to PASSED, FAILED, ERROR, NOT_RUN
	"Passed.", 
        "Failed.", 
	"Error.",
	"Not run."
    };

    /**
     * Exit codes used by Status.exit corresponding to
     * PASSED, FAILED, ERROR, NOT_RUN.
     * The only values that should normally be returned from a test
     * are the first three; the other value is provided for completeness.
     * <small> Note: The assignment is historical and cannot easily be changed. </small>
     */
    public static final int[] exitCodes = { 95, 97, 98, 99 };
}
