/*
 *
 * @(#)TestProvider.java	1.11 03/01/07
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */

package com.sun.cldc.communication;

/**
 * <p> The TestProvider interface defines the contract for transferring 
 * test bundles and other test-related information between the Server 
 * and the JavaTest harness.
 *
 */


public interface TestProvider {

    /**
     * Returns the main application class for this Test Provider.
     * <p> This class is the same for all test bundles generated
     * by this Test Provider. 
     *
     * @return    the name of the main application class. 
     */
    String getAppMainClass();

    /**
     * Returns the directory in which this Test Provider stores application jar files (test bundles).
     * <p> This directory does not change
     * during the test execution.
     *
     * @return    the directory to take test bundles from. 
     */
    String getJarSourceDirectory();

    /**
     * <p>Returns information about the next test in the currently 
     * executing test bundle. This method assumes that only one bundle is
     * executed at a time.<p>
     * Returns null if all tests from the current test bundle have already
     * been executed.
     *
     * @return           the byte array with the encoded test information. 
     *                   Null if no tests are left to execute. 
     */
    byte[] getNextTest();

    /**
     * <p>Returns information about the next test in the specified 
     * test bundle.<p>
     * Returns null if all tests from that test bundle have already
     * been executed.
     *
     * @param   bundleId the Id of the test bundle.
     * @return           the byte array with the encoded test information.
     *                   Null if no tests are left to execute. 
     */
    byte[] getNextTest(String bundleId);

    /**
     * Passes back the result of the last executed test in the currently 
     * executing test bundle. This method assumes that only one bundle is
     * executed at a time.
     *
     * @param   res      the byte array with the encoded test result.
     */
    void sendTestResult(byte[] res);

    /**
     * Passes back the result of the last executed test in the  
     * specified test bundle.
     *
     * @param   res      the byte array with the encoded test result.
     * @param   bundleId the Id of the test bundle.
     */
    void sendTestResult(byte[] res, String bundleId);

    /**
     * Returns the next application (test bundle) to execute. This method 
     * assumes that only one test bundle is executed at a time.
     *
     * @return           A file name relative to the directory returned by 
     *                   {@link #getJarSourceDirectory}.<br>
     *                   An empty string if no test bundles are currently
     *                   available.<br>
     *                   Null if no test bundles will ever be available
     *                   from this Test Provider.
     */
    String getNextApp();

    /**
     * Returns the next application (test bundle) to execute on a
     * target device specified by unique JAM identifier.
     *
     * @param   JAMId    the Id of the JAM instance.
     * @return           A file name relative to the directory returned by 
     *                   {@link #getJarSourceDirectory}.<br>
     *                   An empty string if no test bundles are currently
     *                   available.<br>
     *                   Null if no test bundles will ever be available
     *                   from this Test Provider.
     */
    String getNextApp(String JAMId);
}


