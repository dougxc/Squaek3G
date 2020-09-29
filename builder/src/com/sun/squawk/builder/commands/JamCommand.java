package com.sun.squawk.builder.commands;

import java.io.*;
import java.net.*;
import java.util.*;

import com.sun.squawk.builder.*;

/**
 * Command that performs the functions of a JAM as described in CLDC Technology Compatibility Kit Version 1.1 User’s Guide Appendix A.
 * Do an HTTP get from the specified URL and parse result as an Application Descriptor
 *      <code>Application-Name</code>: Test Suite
 *      <code>JAR-File-URL</code>: URL of next test bundle (Jar)
 *      <code>JAR-File-Size</code>: SizeInBytes Will not exceed limit set in Jar, File Size Limit interview question; see Section 4.2.2 “The Configuration Interview”
 *      <code>Main-Class</code>: com.sun.tck.cldc.javatest.agent.CldcAgent
 *      <code>Use-Once</code>: yes Means do not cache test bundles (which all have the same URL)
 * Fetch the JAR file from the <code>JAR-File-URL</code> specified, install JAR and invoke main on specified <code>Main-Class</code>
 * 
 * @author Eric Arseneau
 *
 */
public class JamCommand extends Command {

    /**
     * Holder for all information provided in an application descriptor as fetched from a JavaTest server.
     * 
     * @author Eric Arseneau
     *
     */
    protected static class ApplicationDescriptor {
        public String applicationName;
        public String jarFileUrlString;
        public int jarFileSize;
        public String mainClassName;
        public String midlet1Line;
        public boolean useOnce;
        
    }
    
    /**
     * URL to get the next application descriptor from a JavaTest server
     */
    protected String getNextAppUrlString;
    
    /**
     * How many times I should repeat execuction of runOnce.
     * A value of -1 indicated that repeat should be forever.
     */
    protected int repeatCount;
    
    /**
     * How many parallel instances/threads I should spawn to perform tests.  This is only useful
     * in case of running tests on desktop.
     */
    protected int parallelCount;
    
    /**
     * Flag indicating if as much information as possible should be logged to console while I am running.
     */
    protected boolean verbose;
    
    /**
     * Path to the folder containing the contents of a TCK.  This path should provide access such that adding
     * /lib/javatest.jar will be available. 
     */
    protected String tckPath;
    
    /**
     * Upon being run, should I go through a fresh build of all componens necessary to run TCK tests.
     */
    protected boolean doBuild;
    
    /**
     * Flag used to determine whether or not a getNextApp call succeeded at least once.  This is
     * done in order to simplify the output I put out when an IO occurs on attempting to fetch
     * the next application descriptor.
     */
    protected boolean suceededAtLeastOnceToGetNextApplicationDescriptor;
    
    /**
     * Flag indicating whether we should execute the TCK main in debug mode or not, ie
     * whether the TCK main will be run inside of an SDA or not.
     */
    protected boolean includeDebugger;
    
    /**
     * Flag indicating if the SuiteCreator should be run via an SDA.
     */
    protected boolean suiteCreatorDebug;
    
    /**
     * If true then skip cleaning up any temp files I may have/will created.
     */
    protected boolean noCleanup;
    
    /**
     * What arguments to pass of to the Squawk VM when running the TCK suite created.
     */
    protected Vector squawkArgs;
    
    /**
     * Flag indicating if the TCK Agent and Client to be used should be the cobbled source that
     * we have in our javatest-agent module.
     */
    protected boolean useOurAgent;
    
    /**
     * JAM ID to use with the URL specified to identify this instance of JAM command from another.
     */
    protected int jamIdArgument;

    /**
     * Create a new instance with its command string set to <code>"jam"</code>.
     * 
     * @param env
     */
    public JamCommand(Build env) {
        this(env, "jam");
    }
    
    /**
     * Create a new instance with its command string set to <code>name</code>.
     * 
     * @param env
     * @param name
     */
    protected JamCommand(Build env, String name) {
        super(env, name);
    }

    /**
     * Make a fresh build of all known modules, and then build the Squawk VM such that
     * it can be used to execute TCK test bundle jars.
     *
     */
    protected void build() {
        // Make sure we have a fresh build of everything needed to run a JAM session
        //   - bootstrap, translator and agent suites
        builder("clean");
        builder("");
        buildBootstrap();
    }

    /**
     * Build up Squawk VM, along with bootstrap and translator suites.
     *
     */
    protected void buildBootstrap() {
        String command = "-prod -mac -o2 rom -strip:d j2me imp";
        if (includeDebugger) {
            command += " debugger";
        }
        command += " -- translator";
        builder(command);
    }

    /**
     * Execute <code>commandLine</code> as a command passed to builder.
     * 
     * @param commandLine The command line containing all arguments to pass on to the builder
     */
    protected void builder(String commandLine) {
        // Pass through the command line options from the current builder
        List passThroughArgs = env.getBuilderArgs();
        for (Iterator iterator = passThroughArgs.iterator(); iterator.hasNext();) {
            commandLine = (String)iterator.next() + " " + commandLine;
        }

        StringTokenizer st = new StringTokenizer(commandLine);
        final String[] args = new String[st.countTokens()];
        for (int i = 0; i != args.length; ++i) {
            args[i] = st.nextToken();
        }
//        String command = "java -jar build.jar " + commandLine;
        new Build().mainProgrammatic(args);
    }

    /**
     * Append the necessary text necessary in order to be able to execute the SuiteCreator utility from a shell/command line.
     * 
     * @param buffer    Buffer to append appropriate text to
     * @param bootstrapSuitePath    Path to the suite that should be used as the boot suite of the VM, if null ignore.
     */
    protected void appendSuiteCreatorCommand(StringBuffer buffer, String bootstrapSuitePath) {
        buffer.append(getSquawkExecutable());
        if (verbose) {
            buffer.append(" -verbose");
        }
//        buffer.append(" -J-Xdebug -J-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=9999");
        buffer.append(" -J-Djava.awt.headless=true");
//        buffer.append(" -veryverbose");
        if (bootstrapSuitePath != null) {
            buffer.append(" -Xboot:");
            buffer.append(bootstrapSuitePath);
        }
        if (suiteCreatorDebug) {
            buffer.append(" com.sun.squawk.debugger.sda.SDA");
        }
        buffer.append(" com.sun.squawk.SuiteCreator");
    }
    
    /**
     * Append the necessary options in order to have the SuiteCreator utility create a suite with all of parameters specified.
     * 
     * @param buffer    Buffer to append appropriate text to
     * @param parent    Name of the parent suite of the suite to be created
     * @param bootstrapSuitePath    Path to the bootstrap suite of the suite to be created
     * @param classPath     Classpath containing all classes to be included in suite to be created
     * @param littleEndian  True if the suite to be created is to be little endian, false for big endian, and null to use the default of the platform SuiteCreator is being run on
     */
    protected void appendSuiteCreatorOptions(StringBuffer buffer, String parent, String bootstrapSuitePath, String[] classPath, Boolean littleEndian) {
        if (parent != null) {
            buffer.append(" -parent:");
            buffer.append(parent);
        }
        if (classPath.length > 0) {
            buffer.append(" -cp:");
            for (int i=0, maxI = classPath.length; i < maxI; i++) {
                buffer.append(classPath[i]);
                if (i < (maxI - 1)) {
                    buffer.append(':');
                }
            }
        }
        buffer.append(" -strip:d");
        if (littleEndian != null && littleEndian.booleanValue()) {
            buffer.append(" -endian:little");
        }
        if (verbose) {
            buffer.append(" -verbose");
        }
    }
    
    /**
     * Create a suite based on the parameters specified.
     * 
     * @param parent    Name of the parent suite of the suite to be created
     * @param bootstrapSuitePath    Path to the bootstrap suite of the suite to be created
     * @param classPath     Classpath containing all classes to be included in suite to be created
     * @param littleEndian  True if the suite to be created is to be little endian, false for big endian, and null to use the default of the platform SuiteCreator is being run on
     * @param suiteName     The name of the suite to be created
     */
    protected void createSuite(String parent, String bootstrapSuitePath, String[] classPath, Boolean littleEndian, String suiteName) {
        StringBuffer buffer = new StringBuffer();
        appendSuiteCreatorCommand(buffer, bootstrapSuitePath);
        appendSuiteCreatorOptions(buffer, parent, bootstrapSuitePath, classPath, littleEndian);
        buffer.append(" ");
        buffer.append(suiteName);
        String command = buffer.toString();
        env.exec(command);
    }

    /**
     * Create a suite named and located at <code>suitePath</code>, from the contents of the test bundle JAR <code>jarPath</code>.
     * 
     * @param jarPath   Path to the jar file to be included in suite
     * @param suitePath     Path to the suite to be created
     */
    protected void createTestSuite(String jarPath, String suitePath) {
        Vector classPathVector = getTestSuiteClassPath(jarPath);
        String[] classPath = (String[]) classPathVector.toArray(new String[classPathVector.size()]);
        createSuite(getTestSuiteParent(), getTestSuiteBootstrapSuitePath(), classPath, getTestSuiteLitteEndian(), suitePath);
    }

    /**
     * Deploy specified suite to device and execute the class named <code>mainClassName</code>/
     * 
     * @param suitePath     Path to the suite to be executed
     * @param mainClassName     Fully qualified name of the class to execute when this suite is executed
     */
    protected void deploySuite(String suitePath, String mainClassName) {
        
    }
    
    /**
     * For each JAM cycle, various files may be created, make sure to remove these.
     * 
     * @param jarFile   File of the jar file used to create the test suite
     * @param suitePath     Path, including filename of the suite file created
     */
    protected void doCleanup(File jarFile, String suitePath) {
        if (jarFile != null) {
            jarFile.delete();
        };
        if (suitePath != null) {
            new File(suitePath + ".suite").delete();
            new File(suitePath + ".suite.api").delete();
        }
    }

    /**
     * There are operations to be performed at the start and end of a series of JAM cycles.  This allows for
     * optimizing how much work is done on every cycle, by allowing behaviour to only happen at the very
     * beginning and at the very end of a series of cycles.  
     *
     */
    protected void endBatch() {
        
    }
    
    /**
     * Execute the suite named <code>suiteName</code> by invoking the main of <code>mainClassName</code>.
     * 
     * @param suiteName
     * @param mainClassName If null, follow MIDlet semantics and look for MIDlet-1 property.
     */
    protected void executeSuite(String suiteName, String mainClassName) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("squawk");
        buffer.append(env.getPlatform().getExecutableExtension());
        for (int i=0; i<squawkArgs.size(); i++) {
            buffer.append(" ");
            buffer.append(squawkArgs.get(i));
        }
        buffer.append(" -J-Djava.awt.headless=true");
        buffer.append(" -suite:");
        buffer.append(suiteName);
        if (includeDebugger) {
            buffer.append(" com.sun.squawk.debugger.sda.SDA");
        }
        if (mainClassName != null) {
            buffer.append(" ");
            buffer.append(mainClassName);
        }
        String command = buffer.toString();
        env.exec(command);
    }
    
    /**
     * Return the path to the HTTPClient binary, to be used as part of the classpath of building a test suite
     * @return  Path to HTTPClient class
     */
    protected String getHttpClientPath() {
        return tckPath + "lib/httpclnt.jar";
    }
    
    /**
     * Get the contents to be found at URL <code>urlString</code> and place it into the file <code>destination</code>.
     * 
     * @param urlString     URL to fetch content from
     * @param destination   File to write content of URL to
     * @throws BuildException
     */
    protected void getJar(String urlString, File destination) throws BuildException {
        InputStream input = null;
        OutputStream output = null;
        try {
            URL url = new URL(urlString);
            input = new BufferedInputStream(url.openStream());
            output = new BufferedOutputStream(new FileOutputStream(destination));
            byte buffer[] = new byte[1024];
            int readCount;
            while ((readCount = input.read(buffer, 0, buffer.length)) != -1) {
                output.write(buffer, 0, readCount);
            }
        } catch (IOException e) {
            throw new BuildException("error getJar \"" + urlString + "\" into \"" + destination.getName() + "\"", e);
        } finally {
            if (input != null) {try {input.close();} catch (IOException e) {}};
            input = null;
            if (output != null) {try {output.close();} catch (IOException e) {}};
            output = null;
        }
    }

    /**
     * Connect to getNextAppUrl and return the ApplicationDescriptor found there.
     * 
     * @param jamId     The JAM ID to pass on all requests to the JavaServer
     * @return
     */
    protected ApplicationDescriptor getNextApplicationDescriptor(int jamId) {
        boolean reportedError = false;
        
        while (true) {
            try {
                return getNextApplicationDescriptor0(jamId);
            } catch (BuildException e) {
                if (!reportedError) {
                    reportedError = true;
                    if (suceededAtLeastOnceToGetNextApplicationDescriptor) {
                        System.out.println("Seems JavaTest is no longer running, will keep trying.");
                    } else {
                        System.out.println("Seems you have not yet started JavaTest, will keep trying.");
                    }
                }
                try {
                    Thread.sleep(5 * 1000);
                } catch (InterruptedException e1) {
                }
            }
        }
    }
    
    /**
     * Connect to getNextAppUrl and return the result.
     * 
     * @param jamId     The JAM ID to pass on all requests to the JavaServer
     * @return
     */
    protected ApplicationDescriptor getNextApplicationDescriptor0(int jamId) throws BuildException {
        ApplicationDescriptor descriptor = new ApplicationDescriptor();
        BufferedReader fileReader = null;
        try {
            String urlString = getNextAppUrlString;
            if (jamId >= 0) {
                urlString += "/" + jamId;
            }
            URL url = new URL(urlString);
            fileReader = new BufferedReader(new InputStreamReader(url.openStream()));
            String line;
            while ((line = fileReader.readLine()) != null) {
                Reader lineReader = new StringReader(line);
                StringBuffer keyBuffer = new StringBuffer();
                StringBuffer valueBuffer = new StringBuffer();
                int read;
                while (((read = lineReader.read()) != -1) && (read != ':')) {
                    keyBuffer.append((char) read);
                }
                if (read == ':') {
                    while (((read = lineReader.read()) != -1)) {
                        valueBuffer.append((char) read);
                    }
                    String key = keyBuffer.toString().trim();
                    String value = valueBuffer.toString().trim();
                    if (key.equals("Application-Name")) {
                        descriptor.applicationName = value;
                    } else if (key.equals("JAR-File-URL") || key.equals("MIDlet-Jar-URL")) {
                        descriptor.jarFileUrlString = value;
                    } else if (key.equals("JAR-File-Size") || key.equals("MIDlet-Jar-Size")) {
                        descriptor.jarFileSize = Integer.parseInt(value);
                    } else if (key.equals("Main-Class")) {
                        descriptor.mainClassName = value;
                    } else if(key.equals("MIDlet-1")) {
                        descriptor.midlet1Line = value;
                        StringTokenizer tokenizer = new StringTokenizer(value);
                        String lastToken = null;
                        while (tokenizer.hasMoreTokens()) {
                            lastToken = tokenizer.nextToken();
                        }
                        if (lastToken != null) {
                            lastToken = lastToken.trim();
                        }
                        descriptor.mainClassName = lastToken;
                    } else if (key.equals("Use-Once")) {
                        descriptor.useOnce = value.equals("yes");
                    }
                }
            }
            suceededAtLeastOnceToGetNextApplicationDescriptor = true;
            return descriptor;
        } catch (IOException e) {
            throw new BuildException("unable to get application descriptor: " + e.getMessage());
        } finally {
            if (fileReader != null) {try {fileReader.close();} catch (IOException e) {}};
            fileReader = null;
        }
    }

    /**
     * Return the name of the Squawk VM executable that can be used to invoke it from within a shell/command box.
     * 
     * @return Name of the Squawk VM executable
     */
    protected String getSquawkExecutable() {
        return "squawk" + env.getPlatform().getExecutableExtension();
    }
    
    /**
     * Return the path to the boostrap suite to be used when building and running test bundle jars.
     * 
     * @return Path to boostrap, or null if none to be specified
     */
    protected String getTestSuiteBootstrapSuitePath() {
        return null;
    }
    
    /**
     * Return the classpath to be used in order to create a test bundle jar suite.
     * 
     * @param jarPath   Path to the jar containing the test bundle from which to create a suite
     * @return
     */
    protected Vector getTestSuiteClassPath(String jarPath) {
        Vector result = new Vector();
        if (useOurAgent) {
            result.add(new File("javatest-agent/j2meclasses").getAbsolutePath());
        } else {
            result.add(tckPath + "lib/agent.jar");
            result.add(tckPath + "lib/client.jar");
        }
        result.add(getHttpClientPath());
        result.add(jarPath);
        return result;
    }

    /**
     * Get the default little endian value to use when building the test bundle suite.
     * @return
     */
    protected Boolean getTestSuiteLitteEndian() {
        return null;
    }
    
    /**
     * Get the default parent suite to use when building the test bundle suite.
     * 
     * @return
     */
    protected String getTestSuiteParent() {
        return null;
    }
    
    /**
     * Parse the command line argument passed in.
     * 
     * @param args
     * @return boolean   true if the command was actually parsed by me, false if option unknown to me
     * @throws BuildException
     */
    protected boolean parseArg(String arg, String[] args, int i) throws BuildException {
        if (arg.equals("-repeat")) {
            repeatCount = -1;
        } else if (arg.startsWith("-repeat:")) {
            String string = arg.substring(8);
            try {
                repeatCount = Integer.parseInt(string);
            } catch (NumberFormatException e) {
                throw new BuildException("error in repeat number specified: " + e.getMessage());
            }
        } else if (arg.startsWith("-id:")) {
            String string = arg.substring(4);
            try {
                jamIdArgument = Integer.parseInt(string);
            } catch (NumberFormatException e) {
                throw new BuildException("error in jam ID specified: " + e.getMessage());
            }
        } else if (arg.equals("-debug")) {
            includeDebugger = true;
        } else if (arg.equals("-debugsc")) {
            suiteCreatorDebug = true;
        } else if (arg.equals("-nocleanup")) {
            noCleanup = true;
        } else if (arg.startsWith("-parallel:")) {
            repeatCount = -1;
            String string = arg.substring(10);
            try {
                parallelCount = Integer.parseInt(string);
            } catch (NumberFormatException e) {
                throw new BuildException("error in parallel number specified: " + e.getMessage());
            }
        } else if (arg.equals("-v") || arg.equals("-verbose")) {
            verbose = true;
        } else if (arg.equals("-nobuild")) {
            doBuild = false;
        } else if (arg.startsWith("-Xts:")) {
            squawkArgs.add(arg);
        } else if (i == (args.length - 1) && !arg.startsWith("-")) {
            getNextAppUrlString = arg;
        } else if (arg.equals("-ouragent")) {
            useOurAgent = true;
        } else {
            return false;
        }
        return true;
    }

    /**
     * Go through the command line options and pull out the information I need to run.
     * 
     * @param args
     * @throws BuildException
     */
    protected void parseArgs(String[] args) throws BuildException {
        for (int i = 0; i != args.length; ++i) {
            String arg = args[i];
            boolean parsed = parseArg(arg, args, i);
            if (!parsed) {
                String message = "Unknown option: " + arg;
                usage(message, System.out);
                throw new BuildException(message);
            }
        }
    }

    /**
     * Throw an exception if I am not in a state to be run, otherwise do nothing.
     * @throws BuildException
     */
    protected void checkCanRun() throws BuildException {
        
    }
    
    /**
     * @override
     */
    public void run(String[] args) throws BuildException {
        getNextAppUrlString = "http://localhost:8080/test/getNextApp";
        jamIdArgument = -1;
        tckPath = "../tck/CLDC-TCK_11/";
        doBuild = true;
        squawkArgs = new Vector();
        repeatCount = 1;
        parseArgs(args);
        checkCanRun();
        if (doBuild) {
            build();
        }
        
        // Run the JAM that will be controller by the JavaTest started by something else, likely the user
        // TODO How to incorporate setupBatch, teadDownBatch for parallel ?
        if (parallelCount > 1) {
            runParallel();
            return;
        }
        startBatch();
        try {
            runRepeat(jamIdArgument);
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            endBatch();
        }
    }

    /**
     * Run a single JAM cycle:
     * 1) get jar
     * 2) create suite
     * 3) deploy suite
     * 4) execute suite
     * 
     * @param jamId     The JAM ID to use when communicating with the JavaTest server
     * @throws BuildException
     */
    public void runOnce(int jamId) throws BuildException {
        ApplicationDescriptor descriptor;
        descriptor = getNextApplicationDescriptor(jamId);
        File jarFile = null;
        String suitePath = null;
        final String jarExtension = ".jar";
        try {
            try {
                env.clearRunSet();
                // This is a trick to get the correct dir, it seems that using new File("") as a direct argument
                // does not yield the same result.
                File currentDirectory = new File("").getCanonicalFile();
                jarFile = File.createTempFile("jam-", jarExtension, currentDirectory);
            } catch (IOException e) {
                throw new BuildException("unable to create temp file to store jar file: " + e.getMessage());
            }
            getJar(descriptor.jarFileUrlString, jarFile);
            String jarPath = jarFile.getPath();
            suitePath = jarPath.substring(0, jarPath.length() - jarExtension.length());
            try {
                createTestSuite(jarPath, suitePath);
                deploySuite(suitePath, descriptor.mainClassName);
                executeSuite(suitePath, descriptor.midlet1Line == null?descriptor.mainClassName:null);
            } finally {
                if (!noCleanup) {
                }
            }
        } finally {
            if (!noCleanup) {
                doCleanup(jarFile, suitePath);
                if (jarFile != null) {
                    jarFile.delete();
                };
                if (suitePath != null) {
                    new File(suitePath + ".suite").delete();
                    new File(suitePath + ".suite.api").delete();
                }
            }
        }
    }
    
    /**
     * Spawn off <code>parallelCount</code> threads to run JAM cycles in perpetuaity.
     *
     */
    public void runParallel() {
        Thread[] threads = new Thread[parallelCount];
        for (int i = 1; i < parallelCount; i++) {
            final int finalI = i;
            Runnable runnable = new Runnable() {
                public void run() {
                    runRepeat(finalI);
                }
            };
            threads[i] = new Thread(runnable, "JAM-" + i);
            System.out.println("Starting thread: " + threads[i].getName());
            threads[i].start();
        }
        runRepeat(0);
    }

    /**
     * Run n JAM cycles and return, where n is <code>repeatCount</code>.
     * 
     * @param jamId
     */
    public void runRepeat(int jamId) {
        runOnce(jamId);
        if (repeatCount == -1) {
            while(true) {
                runOnce(jamId);
            }
        } else {
            while (repeatCount > 1) {
                runOnce(jamId);
                repeatCount--;
            }
        }
    }

    /**
     * A number of JAM cycles are about to be started, do whatever is necessary to setup the tests
     * about to be run.
     *
     */
    protected void startBatch() {
        
    }
    
    /**
     * Usage message.
     *
     * @param errMsg   optional error message
     * @param out      where to write the usage message
     */
    public void usage(String errMsg, PrintStream out) {
        if (errMsg != null) {
            out.println(errMsg);
        }
        out.println("usage: jar [-options] url");
        out.println("where options include:");
        out.println("    -repeat    Stay in jam mode forever");
        out.println("    -parallel:<n>    Run n clients in parallel");
        out.println("    -id:<n>    JAM id to use when requesting next app from JavaTest server");
        out.println("    -debug    Include debugger in bootstrap, and launch JavaTest suite in debug mode");
        out.println("    -debugsc    Launch suite creator in debug mode, requires having being built at least once with -debug");
        out.println("    -nobuild    Do not rebuild the bootstrap, translator and agent suites");
        out.println("    -tck       Use TCK semantics to build the JAM suites");
        out.println("    -nocleanup    Do not delete the jar and suite files created");
        out.println("    -Xts:<n>       start tracing after 'n' backward branches\n");
        out.println("    -h                  show this help message and exit");
        out.println("    -verbose    Log more info to console");
        out.println("");
    }

}
