/*
 * @(#)MultiTest.java	1.8 03/01/07
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.sun.tck.cldc.lib;

import java.io.PrintStream;
import java.util.Vector;

/**
 * Base class for tests with multiple sub test cases.
 * This base class implements the standard com.sun.cldctck.lib.Test 
 * features so that you can provide the additional test cases with only
 * a little concern about the boilerplate needed to execute individual  
 * test case methods, namely, you need to update the runTestCases() method
 * to add check and invocation statement.
 * MultiTest is designed as a base class used during development 
 * of new test classes.  
 *
 * <P>You must add individual test case methods to your derived test class
 * to create a useful test class. Each test case method must take no 
 * arguments.  If you need to pass an argument into a test method, you should 
 * design a wrapper test case to calculate the argument values and then call 
 * the test method with the correct arguments.  The test case methods must 
 * implement this interface: 
 * <BLOCKQUOTE>
 * <STRONG><CODE>public Status methodName( )</CODE></STRONG>
 * </BLOCKQUOTE>
 *
 * <P>For examples of how to use this class see the 
 * TO BE PUT LATER test classes.
 *
 * <P>Possible future additions may include
 * <UL>
 * <LI>Method to assert a boolean expression and return a Status 
 * <LI>Method to assert string equality and return a Status
 * <LI>Method to parse argv for known and specified arrays from JavaTest
 * <LI>Methods to be called just before the test and just after the test 
 *     is executed.
 *</UL>
 *
 * @see com.sun.cldctck.lib.Test
 * @see com.sun.cldctck.lib.MultiStatus
 *
 * @version @(#)MultiTest.java	1.8 
 * @author Kevin A. Smith
 */

public abstract class MultiTest implements Test {
    public static class SetupException extends Exception {
	/**
	 * Construct a new SetupException object that signals failure
	 * with a corresponding message.
	 * 
	 * @param s the string containing a comment
	 */
	public SetupException(String s) {
	    super(s);
	}
	
	/**
	 * Creates SetupException object which indicates that 
	 * this test is not applicable. The cases when it is needed 
	 * are rare, so please think twice whether you really need it.
	 * 
	 * @param s the string containing a comment
	 */
	public static SetupException notApplicable(String msg) {
	    SetupException e = new SetupException("Test not applicable: " + msg);
	    e.passed = true;
	    return e;
	}
	
	/**
	 * Determines whether this SetupException signals failure or not.
	 * 
	 */
	public boolean isPassed() {
	    return passed;
	    
	}

	/**
	 * Indicate whether this exception was the result of calling {@notApplicable}.
	 * @serial
	 */
	private boolean passed = false;
    }


    /** 
     * Run the test contained in this object.
     *
     * <P>This method calls the decodeAllArgs method  with the value of argv to
     * decode command line arguments, and then calls the init method to perform
     * any other initialization.
     * Then it calls runTestCases to run actual tests.
     * <P>To add parsing for new arguments you need to override the decodeArg method.
     * The init method may also be overridden in case you need additional  
     * initialization.
     *
     *
     * @see #decodeAllArgs
     * @see #init
     * @see #runTestCases
     *
     * @param argv Execute arguments passed in from either the 
     *             command line or the execution harness.
     * @param log Output stream for general messages from the tests.
     *            Is assigned to this.log.
     * @param ref Output stream for reference output from the tests.
     *            Is assigned to this.ref.
     * @return Overall status of running all of the test cases.
     */
    public Status run(String[] argv, PrintStream log, PrintStream ref) {
        // assign log and reference output streams
        this.ref = ref;
        this.log = log;
	// perform initialization
	Status initStatus = init(argv);
	if (testNotApplicable 
		|| (initStatus != null && initStatus.getType() != Status.PASSED)) {
	    return initStatus;
	}
	// main work
	try {
	    runTestCases();
            return getStatus(); 
	}
	catch (Throwable t) {
	    return Status.failed("Unexpected Throwable (in " 
		    + lastTestCase + "): " + t); // since we don't have printStackTrace..
	}
    }


    /** 
     * Run the tests contained in this object after initialization.
     *
     */
    protected abstract void runTestCases();


    /** 
     * Checks whether the particular test case should be executed.
     * Should be called for each test case from runTestCases().
     *
     */
    protected boolean isSelected(String testCase) {
	if ((testAll || testCases.contains(testCase)) &&
                (excludeTestCases == null ||
                ! excludeTestCases.contains(testCase))) {
	    lastTestCase = testCase;
	    return true;
	} else {
	    return false;
	}
    }


    /** 
     * Add another test into the set for consideration.
     *
     * @param status The outcome of this test case
     */
    protected void addStatus(Status status) {
	log.println(lastTestCase + ": " + status);

	++iTests;
	
	if (status != null) {
	    switch (status.getType()) {
	    case Status.PASSED:
		++iPassed;
		return;
		
	    case Status.FAILED:
		if (iFail == 0 && iBad == 0) {
		    firstTest = lastTestCase;
		}
		++iFail;
		return;
		
	    default:
	    }
	}

	if (iBad == 0) {
	    firstTest = lastTestCase;
	}
	++iBad;
    }


    /**
     * Get the aggregate outcome of all the outcomes passed to "add".
     */
    public Status getStatus() {

	log.flush();

	String summary = "tests: " + iTests;
	if (iPassed > 0)
	    summary += "; passed: " + iPassed;
	if (iFail > 0)
	    summary += "; failed: " + iFail;
	if (iBad > 0)
	    summary += "; bad status: " + iBad;

	/* Return a status object that reflects the aggregate of the various test cases. */

        /* At least one test case was bad */
	if (iBad > 0) {
	    return Status.failed(summary + "; first bad test case found: " + firstTest);
	}
        /* At least one test case failed */
	else if (iFail > 0) {
	    return Status.failed(summary + "; first test case failure: " + firstTest);
	}
	/* All test cases passed */
	else {
	    return Status.passed(summary);
	}
    }


    /** 
     * Parses the arguments passed to the test.
     *
     * This method embodies the main for loop for all of the 
     * execute arguments. It calls <CODE>decodeArg</CODE> 
     * for successive arguments in the argv array.
     *
     * @param argv execute arguments from the test harness or from the
     *             command line.
     *
     * @exception SetupException raised when an invalid parameter is passed, 
     * or another error occurred.
     *
     * @see #decodeArg 
     */
    protected final void decodeAllArgs( String argv[] ) throws SetupException {
	/* Please note, we do not increment i
	 * that happens when decodeArg returns the 
	 * number of array elements consumed
	 */
	for (int i = 0 ; i < argv.length; ) {
	    int elementsConsumed = decodeArg(argv, i);
	    if (elementsConsumed == 0 ) {
		// The argument was not recognized.
		throw new SetupException("Could not recognize argument: " + argv[i]);
	    }
	    i += elementsConsumed;
	}
    }


    /** 
     * Decode the next argument in the argument array.
     * May be overridden to parse additional execute arguments.  
     *
     * The default behavior of decodeArg( String argv[], int index )
     * is to parse test case IDs starting from <CODE>index</CODE> 
     * from execute arguments <CODE>argv</CODE>.
     * <P>The derived class may override this method to provide 
     * parsing of additional arguments. However, it is recommended
     * for this method to return super.decodeArg() if it 
     * does not recognize the argument. So it has to parse
     * specific for derived class arguments only.
     *
     * <P>The required syntax for using this method is the execute
     * argument <EM>-TestCaseID</EM> followed by a space and then a
     * space delimited list of one or more test case method names.
     * Using the test case method name <STRONG>ALL</STRONG> specifies
     * that all of the individual test case methods that match the
     * required test method signature should be executed.  The method
     * getAllTestCases() is called to gather the list of all methods
     * that match the test method signature.
     *
     * <P>Once the execute argument <EM>-TestCaseID</EM> is found, all 
     * subsequent execute arguments will be treated as test case method 
     * names until either the execute argument array is exhausted or 
     * an execute argument that begins with <EM>-</EM> is found.
     *
     * @param argv execute arguments from the test harness or from the 
     *             command line
     * @param index current index into argv.
     *
     * @exception SetupException raised when an invalid argument is passed, 
     * or another error occurred.
     *
     * @see #decodeAllArgs 
     * @see #testCases 
     */
    protected int decodeArg( String argv[], int index ) throws SetupException {
	if (argv[index].equals("-exclude")) {
            excludeTestCases = new Vector(1,1);
	    split(argv[index + 1], excludeTestCases);
	    return 2;
	}
	    
	if (!argv[index].equals("-TestCaseID")) 
	    return 0;

	/* consume elements until it is done
	 * creating the array of test case id's
	 * return the number of elements consumed
	 */
	int i = index + 1;
	if (i < argv.length && argv[i].equals("ALL")) {
	    testAll = true;
	    return 2;
	}

        testCases = new Vector(1,1);
	while (i < argv.length && ! argv[i].startsWith("-")) {
	    testCases.addElement(argv[i++]);
	}

	if (testCases.isEmpty())
	    throw new SetupException("No test case(s) specified");

	return i - index;
    }

    private void split(String s, Vector v) {
	int start = 0;
	for (int i = s.indexOf(','); i != -1; i = s.indexOf(',', start)) {
	    v.addElement(s.substring(start, i));
	    start = i + 1;
	}
	if (start != s.length())
	    v.addElement(s.substring(start));
    }

    /** 
     * A setup method called after argument decoding is complete,
     * and before the test cases are executed. By default, it does
     * nothing; it may be overridden to provide additional behavior. 
     *
     * @throws SetupException if processing should not continue.
     * This may be due to some inconsistency in the arguments,
     * or if it is determined the test should not execute for
     * some reason.
     */
    protected void init() throws SetupException { }



    /** 
     * 
     * Initialize the test from the given arguments.
     * 
     */
    private Status init(String[] argv) { 
     	/* Decode test arguments and delegate all other
	 * test setup to the init method provided by
	 * derived classes
	 */
	try {
	    decodeAllArgs(argv);

	    if (testCases == null) {
		/* Assuming that all test cases should be run 
		 * if no "-TestCaseID" argument is specified.
		 */
		testAll = true;
	    }

	    init();
	    return null;
	}
	catch (SetupException e) {
	    testNotApplicable = true;
	    return (e.isPassed() 
			? Status.passed(e.getMessage()) 
		    	: Status.failed(e.getMessage()) );
	}
    }



    /** 
     * Output to be logged to result file.  
     *
     * Output to this PrintStream is not used during golden file comparison.
     * Also used to output the Status from each individual test case.
     */
    protected PrintStream log;


    /** 
     * Output that can be used as reference.
     *
     * Output to this PrintStream is used during golden file comparison.
     */
    protected PrintStream ref;



    /** 
     * The list of test case methods to be executed.
     */
    private Vector testCases = null;

    /** 
     * Determines whether we should run all tests
     */
    private boolean testAll = false;

    /**
     * The set of test cases to be excluded.
     */
    private Vector excludeTestCases = null;

    /**
     * may be set if SetupException is thrown during decodeArgs() or init
     */
    private boolean testNotApplicable = false;

    private String lastTestCase;

    // These values accumulate the aggregate outcome, without requiring
    // the individual outcomes be stored
    private int iTests = 0;
    private int iPassed = 0;
    private int iFail = 0;
    private int iBad = 0;
    private String firstTest = "";
}






