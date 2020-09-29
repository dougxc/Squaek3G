package com.sun.squawk.builder.commands;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import com.sun.spot.debug.*;
import com.sun.spot.peripheral.radio.*;
import com.sun.squawk.builder.*;

/**
 * A specialization of {@link JamCommand} that provides specific implementation details to have TCK test run on an actual SPOT device.
 *  
 * @author Eric Arseneau
 *
 */
public class JamSpotCommand extends JamCommand {
    
    /**
     * Serial port to the SPOT to be run as a basestation.
     */
    protected String baseStationSerialPort;
    
    /**
     * The serial ports of all the devices to use as slaves to run tests on.
     */
    protected String[] deviceSerialPorts;
    
    /**
     * The DebugClient responsible for managing, flashing, and running applications on a SPOT.
     */
    protected DebugClient debugClient;
    
    /**
     * Properties read from the SPOT SDK.
     */
    protected Properties properties;

    public JamSpotCommand(Build env) {
        super(env, "jamspot");
    }
    
    /**
     * {@inheritDoc }
     */
    protected void appendSuiteCreatorOptions(StringBuffer buffer, String parent, String bootstrapSuitePath, String[] classPath, Boolean littleEndian) {
        super.appendSuiteCreatorOptions(buffer, parent, bootstrapSuitePath, classPath, littleEndian);
        buffer.append(" -translator:file://");
        buffer.append(getProperty("sunspot.lib"));
        buffer.append("/translator.suite");
    }

    /**
     * {@inheritDoc }
     */
    protected void build() {
    }

    /**
     * Throw an exception if I am not in a state to be run, otherwise do nothing.
     * @throws BuildException
     */
    protected void checkCanRun() throws BuildException {
        if ((baseStationSerialPort == null) && (deviceSerialPorts.length == 0)) {
            throw new BuildException("Must specify -basetation:<port> and/or -device:<port>");
        }
        if ((baseStationSerialPort != null) && (deviceSerialPorts.length == 0)) {
            throw new BuildException("Must specify -device:<port> when using the -basestation:<port> option");
        }
    }
    
    /**
     * {@inheritDoc }
     */
    protected void endBatch() {
        if (baseStationSerialPort == null) {
            // Handle closing serial communication
        } else {
            // Handle closing base station
            System.out.println("Preparing to close base station on port: " + baseStationSerialPort);
            System.setProperty("SERIAL_PORT", baseStationSerialPort);
//            LowPanPacketDispatcher.getInstance().closeBaseStation();
            System.out.println("Base station closed");
        }
        System.out.println(">>> DOING an EXPLICIT EXIT");
        super.endBatch();
        System.exit(0);
    }

    /**
     * {@inheritDoc }
     */
    protected void executeSuite(String suiteName, String mainClassName) {
        // TODO Handle debug case
        // We ignore the suite and main class information, as we assume this was already done on deploy
        try {
            debugClient.processBLCommandStartApp();
        } catch (DebugTargetResetException resetException) {
            try {
                debugClient.processBLCommandSync(resetException.getMessage());
            } catch (IOException e) {
                throw new BuildException("Failed to sync on app exit", e);
            }
        } catch (IOException e) {
            throw new BuildException("Failed to execute suite", e);
        }
    }
    
    /**
     * {@inheritDoc }
     */
    protected boolean parseArg(String arg, String[] args, int i) throws BuildException {
        boolean parsed = super.parseArg(arg, args, i);
        if (parsed) {
            return true;
        }
        if (arg.startsWith("-basestation:")) {
            baseStationSerialPort = arg.substring("-basestation:".length());
        } else if (arg.startsWith("-device:")) {
            String[] copy = new String[deviceSerialPorts.length + 1];
            System.arraycopy(deviceSerialPorts, 0, copy, 0, deviceSerialPorts.length);
            String port = arg.substring("-device:".length());
            copy[deviceSerialPorts.length] = port;
            deviceSerialPorts = copy;
        } else {
            return false;
        }
        return true;
    }
    
    /**
     * {@inheritDoc }
     */
    protected void deploySuite(String suitePath, String mainClassName) {
        try {
            int count = 0;
            while (count < 5) {
                try {
                    debugClient.processBLCommandSetCmdLineParams(getProperty("squawk.startup.arguments") + " " + mainClassName);
                    debugClient.processBLCommandFlashApp(suitePath);
                    break;
                } catch (DebugClientFailureException e) {
                    count++;
                    System.out.println("- failed to flash, retry " + count + e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new BuildException("Failed to deploy", e);
        }
    }
    
    /**
     * {@inheritDoc }
     */
    protected void doCleanup(File jarFile, String suitePath) {
        super.doCleanup(jarFile, suitePath);
        if (suitePath != null) {
            // .bintemp files seem to be created by debug client
            new File(suitePath + ".bintemp").delete();
        }
    }

    protected void doListen(boolean firstTime) {
        Thread thread = new Thread(new SpotStreamConnectionListener(firstTime), "ConnectionListner");
        thread.start();
    }

    /**
     * {@inheritDoc }
     */
    protected String getHttpClientPath() {
        // TODO Paremeterize whether we use the build in HttpClient or not
        return new File("spot-build-plugin/tck-agent/j2meclasses").getAbsolutePath();
    }

    /**
     * Return the value of the property named <code>key</code> found in the SPOT DSK default.properties file.
     * 
     * @param key
     * @return value found at key, or null
     */
    protected String getProperty(String key) {
        if (properties == null) {
            // Read in the SPOT SDK properties and resolve any of the references to properties made in any of the values
            properties = new Properties();
            loadProperties(new File(System.getProperty("user.home"), ".sunspot.properties"), properties);
            loadProperties(new File(properties.getProperty("sunspot.home"), "default.properties"), properties);
        }
        return properties.getProperty(key);
    }
    
    /**
     * {@inheritDoc }
     */
    protected String getTestSuiteBootstrapSuitePath() {
        return getProperty("sunspot.lib") + "/squawk.suite";
    }
    
    /**
     * {@inheritDoc }
     */
    protected Boolean getTestSuiteLitteEndian() {
        return Boolean.TRUE;
    }
    
    /**
     * {@inheritDoc }
     */
    protected String getTestSuiteParent() {
        return getProperty("spot.library.path") + '/' + getProperty("spot.library.name");
    }

    /**
     * Load the properties found in file <code>file</code> into <code>properties</code>.  If <code>properties</code> already
     * contains a key defined in <code>file</file>, then follow the Ant semantics and keep the first value placed into a given property.
     * 
     * @param file
     * @param properties
     */
    protected void loadProperties(File file, Properties properties) {
        Properties newProperties ;
        try {
            newProperties = new Properties();
            FileInputStream input = new FileInputStream(file);
            newProperties.load(input);
        } catch (IOException e) {
            throw new BuildException("Problems loading properties: " + file.getPath(), e);
        }
        // Follow ant properties semantics, the first definition of a property is kept and is NOT
        // over-written by subsequent properties files defining the same property.
        for (Iterator i=newProperties.keySet().iterator(); i.hasNext(); ) {
            String key = (String) i.next();
            if (!properties.containsKey(key)) {
                properties.setProperty(key, newProperties.getProperty(key));
            }
        }
        Object[] keys = properties.keySet().toArray();
        for (int i=0; i < keys.length; i++) {
            String key = (String) keys[i];
            String value = properties.getProperty(key);
            int index = 0;
            while ((index = value.indexOf("${", index)) > -1) {
                int closeIndex = value.indexOf('}', index);
                if (closeIndex == -1) {
                    index = value.length();
                    continue;
                }
                String subKey = value.substring(index + 2, closeIndex);
                String otherValue = properties.getProperty(subKey);
                if (otherValue == null) {
                    otherValue = "";
                }
                String newValue = value.substring(0, index) + otherValue + value.substring(closeIndex + 1);
                value = newValue;
            }
            properties.setProperty(key, value);
        }
    }

    /**
     * {@inheritDoc }
     */
    protected String getSquawkExecutable() {
        return getProperty("squawk.executable") + env.getPlatform().getExecutableExtension();
    }
    
    /**
     * {@inheritDoc }
     */
    public void run(String[] args) throws BuildException {
        deviceSerialPorts = new String[0];
        super.run(args);
    }

    /**
     * {@inheritDoc }
     */
    protected void startBatch() {
        super.startBatch();
        try {
            SerialPortTarget target = new SerialPortTarget();
            JamSpotCommandDebugClientUI ui = new JamSpotCommandDebugClientUI(this, target);
            target.initialise(deviceSerialPorts[0], ui);
            debugClient  = new DebugClient(getProperty("sunspot.arm"), getProperty("sunspot.lib"), getProperty("sunspot.arm"), "" , null, null);
            // TODO Get syntropy to make IDebugClient.setTarget public for me ?
            try {
                Method method = debugClient.getClass().getDeclaredMethod("setTarget", new Class[] {ITarget.class});
                method.setAccessible(true);
                method.invoke(debugClient, new Object[] {target});
            } catch (Exception e) {
                throw new BuildException("Problems with hack", e);
            }
            debugClient.setUI(ui);
            System.out.println("If you do not get confirmation of DebugClient initialized, then please reset device connected to port " + deviceSerialPorts[0]);
            debugClient.processBLCommandSync(null);
            ui.debugClientInitialized();
            System.out.println("  DebugClient initialized");
        } catch (IOException e) {
            throw new BuildException("Problems initializing DebugClient", e);
        }
        if (baseStationSerialPort == null) {
            // Use serial communication through the debug client to talk with devices running tests
            return;
        }
        // Use basestation specified to communicate with SPOTs that will be running the tests
        System.out.println("Preparing to initialize base station on port: " + baseStationSerialPort);
        System.setProperty("SERIAL_PORT", baseStationSerialPort);
        LowPanPacketDispatcher.getInstance().initBaseStation();
        System.out.println("Base station initialized");
    }

}
