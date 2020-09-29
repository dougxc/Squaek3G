/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package example.shell;

import java.awt.*;
import java.awt.event.*;
import awtcore.*;
import java.io.*;
import javax.microedition.io.*;
import java.util.*;
import java.util.Enumeration;
import com.sun.squawk.*;

/**
 * A Shell is a graphical launcher for starting various demos in the example.* package.
 *
 * @author  Nik Shaylor, Doug Simon
 */
public class Shell extends Applet implements ActionListener {

    /**
     * The connection to a lookup server. If this is null, then it means this shell
     * has no connection with a lookup server.
     */
    private Registration registration;

    /**
     * The endianess in which to save isolates. If this is null, the endianess of the host platform is used.
     */
    private Boolean endianessOverride;

    /**
     * Verbose execution flag.
     */
    private boolean verbose;

    /**
     * The list of applications to be programatically started by the shell.
     */
    private String[] startupApps;

    /**
     * This shell's main window.
     */
    private Frame frame;

    /**
     * The main panel within this shell's window.
     */
    private MainPanel mainPanel;

    Checkbox useDebugger;
    Choice debuggerLogLevel;
    Checkbox debuggerLogToFile;
    TextField debuggerLogFile;
    TextField debuggerPort;

    /**
     * List of the running isolates.
     */
    private Vector isolateList;

    private final Object communicationMutex = new Object();

    /**
     * The main panel of buttons for launching the apps.
     */
    class MainPanel extends Panel implements ActionListener {

        /**
         * Creates and initializes the panel.
         */
        MainPanel() {
            super(new GridLayout (0, 2));
            addButton("cubes");
            addButton("chess");
            addButton("mpeg");
            addButton("spaceinv");
            addButton("manyballs");
            addButton("awtdem");
        }

        /**
         * Adds an application to the panel.
         *
         * @param app  the unique name of the app
         */
        void addButton(String app) {
            Button b = new Button(app);
            add(b);
            b.addActionListener(this);
            b.setActionCommand("example." + app + ".Main");
        }

        /**
         * {@inheritDoc}
         */
        public void actionPerformed(ActionEvent ev) {
            String mainClassName = ev.getActionCommand();
            start(mainClassName);
        }

    }


    /**
     * Determine which url to save the given isolate.
     *
     * @param isolate  the <code>Isolate</code> to be saved or migrated
     * @return  the url to save this isolate
     */
    private String getURLtoSaveIsolate(Isolate isolate) {

        String url = null;

        if (registration != null) {
            Hashtable shells;
            try {
                shells = registration.getRemoteShells(false);
            } catch (IOException e) {
                System.err.println("IO error while getting registered shells from server: " + e);
                return null;
            }

            if (!shells.isEmpty()) {
                String hostAndPort = ChoiceDialog.show(frame, "Choose remote shell", shells);
                if (hostAndPort == null) {
                    return null;
                }
                url = "socket://" + hostAndPort;
            } else {
                // No remote shells available - save to file
                url = "file://" + getSaveFileForIsolate(isolate);
            }
        } else {
            url = "file://" + getSaveFileForIsolate(isolate);
        }

        return url;
    }

    /**
     * Creates a file name for saving an isolate.
     *
     * @param isolate   the isolate to be saved
     * @return a name based on the main class of <code>isolate</code> that should be unique within the current working directory
     */
    private String getSaveFileForIsolate(Isolate isolate) {
        Vector list = new Vector();
        listSavedIsolates(list);
        String name = isolate.getMainClassName() + ".isolate";
        int i = 1;
        while (list.contains(name)) {
            name = isolate.getMainClassName() + "_" + i + ".isolate";
            i++;
        }
        return name;
    }

    /**
     * Adds all the "*.isolate" files in the currently working directory to a given vector.
     *
     * @param   list  the vector to which the files are added
     */
    private void listSavedIsolates(Vector list) {
        try {
            DataInputStream dis = Connector.openDataInputStream("file://./");
            try {
                for (; ; ) {
                    String name = dis.readUTF();
                    if (name.endsWith(".isolate")) {
                        if (name.startsWith("./")) {
                            name = name.substring("./".length());
                        }
                        list.addElement(name);
                    }
                }
            } catch (EOFException ex) {
            }
            dis.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }


    private void saveIsolate(Isolate isolate, String url) {
        try {
            DataOutputStream dos = Connector.openDataOutputStream(url);

            // Let recipient know what its receiving
            if (!url.startsWith("file://")) {
                dos.writeUTF("runIsolate");
                dos.writeInt(1);
            }

            saveIsolate(isolate, url, dos);
            dos.close();
        } catch (IOException ex) {
            System.err.println("I/O error while trying to connect to: " + url);
            ex.printStackTrace();
        }
    }


    /**
     * Saves a given isolate to a URL. If this shell has a connection to a LookupServer
     * then it allows the user to choose one of the shells registered with the server as
     * the destination for the hibernated isolate. If there is no lookup server, the isolate
     * is saved to a file.
     *
     * @param isolate   the isolate to save
     * @param random    specifies if a randomly chosen remote shell should be chosen
     */
    private void saveIsolate(Isolate isolate, String url, DataOutputStream dos) {

        // Can't save to a null url
        if (url == null || dos == null) {
            return;
        }

        boolean savedToFile = url.startsWith("file://");

        // Save the isolate to the URL
        try {
            log((registration == null ? "saving" : "sending") + " isolate to " + url);

            if (endianessOverride != null) {
                boolean bigEndian = endianessOverride.booleanValue();
                log("saving isolate in " + (bigEndian ? "big" : "little") + " endian format");
                isolate.save(dos, url, bigEndian);
            } else {
                log("saving isolate in system default endian format");
                isolate.save(dos, url);
            }

            log((savedToFile ? "saved" : "sent") + " isolate to " + url);

            if (savedToFile) {
                String name = url.substring("file://".length(), url.length() - ".isolate".length());
                OptionDialog.showMessageDialog(frame, "Restart isolate by selecting '" + name + "' in the restart dialog.");
            }

        } catch (IOException ioe) {
            System.err.println("I/O error while trying to save/send isolate: ");
            ioe.printStackTrace();
        } catch (Error le) {
            System.err.println("Error error while trying to save/send isolate: ");
            le.printStackTrace();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent ev) {
        String action = ev.getActionCommand();
        if (action.equals("Restart")) {
            restart();
        } else if (action.equals("Exit")) {
            destroyApp(true);
            notifyDestroyed();
        }
    }

    /**
     * Starts an application in its own isolate.
     *
     * @param mainClassName  the name of the class whose 'main' method will be called
     */
    private void start(final String mainClassName) {

        // Use a separate thread so that action can be taken when the child isolate stops
        new Thread() {
            public void run() {
                Isolate shellIsolate = Isolate.currentIsolate();
                String cp = shellIsolate.getClassPath();

                // Resolve the parent URL of the application isolate
                String parentURI = shellIsolate.getParentSuiteSourceURI();

                // Create and start the application in its own isolate
                String[] args = new String[0];
                Isolate childIsolate;
                Shell shell = Shell.this;
                if (shell.useDebugger.getState()) {
                    String logFile = "";
                    Vector options = new Vector();
                    options.addElement("-cp:" + cp);
                    options.addElement("-log:" + shell.debuggerLogLevel.getSelectedItem());
                    options.addElement("-url:serversocket://:" + shell.debuggerPort.getText());
                    if (shell.debuggerLogToFile.getState()) {
                        options.addElement("-logURL:file://" + shell.debuggerLogFile.getText());
                    }
                    options.addElement(mainClassName);
                    args = new String[options.size()];
                    options.copyInto(args);

                    childIsolate = new Isolate("com.sun.squawk.debugger.sda.SDA", args, null, parentURI);
                } else {
                    childIsolate = new Isolate(mainClassName, args, cp, parentURI);
                }
                childIsolate.start();

                isolateList.addElement(childIsolate);

                // Wait for application to exit or be hibernated
                joinIsolate(childIsolate);
            }

        }.start();
    }

    /**
     * Waits for a running isolate to exit or hibernate and takes appropriate action.
     *
     * @param isolate  a running isolate
     */
    private void joinIsolate(Isolate isolate) {

        isolate.join();

        isolateList.removeElement(isolate);

        // Handle and isolate that was hibernated by serializing it somewhere
        if (isolate.isHibernated()) {
            log(isolate.getMainClassName() + " hibernated");

            String value = isolate.getProperty("save_after_join");
            if (value == null || !value.equals("false")) {
                saveIsolate(isolate, getURLtoSaveIsolate(isolate));
            }
        } else {
            log(isolate.getMainClassName() + " exited");
        }
    }

    /**
     * Restarts a hibernated isolate whose state was saved to a file.
     */
    private void restart() {

        Vector list = new Vector();
        listSavedIsolates(list);

        if (list.isEmpty()) {
            OptionDialog.showMessageDialog(frame, "No saved isolates available");
            return;
        }

        Hashtable savedIsolates = new Hashtable(list.size());
        for (Enumeration e = list.elements(); e.hasMoreElements(); ) {
            String file = (String)e.nextElement();
            String name = file.substring(0, file.length() - ".isolate".length());
            savedIsolates.put(name, file);
        }

        String file = ChoiceDialog.show(frame, "Select saved isolate", savedIsolates);
        if (file == null) {
            // Return now if user cancelled dialog
            return;
        }
        String url = "file://" + file;
        log("restarting isolate hibernated in " + url);

        Isolate isolate;
        try {
            DataInputStream dis = Connector.openDataInputStream(url);
            isolate = Isolate.load(dis, url);
            dis.close();

            // Delete intermediate isolate file.
            DataOutputStream dos = Connector.openDataOutputStream("deletefiles://");
            dos.writeUTF(file);
            dos.close();

        } catch (IOException e) {
            System.err.println("I/O error while trying to load isolate: ");
            e.printStackTrace();
            return;
        }
        restart(isolate);
    }

    /**
     * Restarts a hibernated isolate.
     *
     * @param isolate  the isolate to restart
     */
    void restart(final Isolate isolate) {
        isolateList.addElement(isolate);
        new Thread() {
            public void run() {
                isolate.unhibernate();
                joinIsolate(isolate);
            }
        }.start();
    }

    /**
     * Logs a status message to System.out if {@link verbose} is true.
     *
     * @param msg  the status message
     */
    private void log(String msg) {
        if (verbose) {
            System.out.println("[" + msg + "]");
            System.out.flush();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void startApp() {

        // Location to store list of running isolates
        isolateList = new Vector();

        if (registration != null) {
            new Thread(registration).start();
        }

        mainPanel = new MainPanel();
        frame = new Frame("Shell");
        frame.add("Center", mainPanel);

        Panel debug = new Panel();


        useDebugger = new Checkbox("Debug");
        debug.add(useDebugger);

        debug.add(new Label("Port:"));
        debuggerPort = new TextField(4);
        debuggerPort.setText("2800");
        debug.add(debuggerPort);

        debug.add(new Label("Log:"));
        debuggerLogLevel = new Choice();
        debuggerLogLevel.add("none");
        debuggerLogLevel.add("info");
        debuggerLogLevel.add("verbose");
        debuggerLogLevel.select(1);
        debug.add(debuggerLogLevel);

        debuggerLogToFile = new Checkbox("File:");
        debuggerLogFile = new TextField(6);
        debuggerLogFile.setText("debug.log");
        debug.add(debuggerLogToFile);
        debug.add(debuggerLogFile);

        debuggerPort.addTextListener(new TextListener() {
            public void textValueChanged(TextEvent e) {
                String text = debuggerPort.getText();
                try {
                    Integer.parseInt(text);
                } catch (NumberFormatException ex) {
                    debuggerPort .setText("2800");
                }
            }
        });

        frame.add("North", debug);


        Panel buttons = new Panel();
        Button b = new Button(" Restart ");
        buttons.add(b);
        b.addActionListener(this);
        b.setActionCommand("Restart");

        b = new Button(" Exit ");
        buttons.add(b);
        b.addActionListener(this);
        b.setActionCommand("Exit");
        frame.add("South", buttons);

        frame.pack();
        frame.show();

        if (startupApps != null) {
            for (int i = 0; i != startupApps.length; ++i) {
                String startupApp = startupApps[i];
                start(startupApp);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void destroyApp(boolean uncond) {
        frame.dispose();
        if (registration != null) {
            registration.close();
        }
    }

    /**
     * Displays a usage message to System.out.
     *
     * @param errMsg  an optional error message to display before the usage message
     */
    private static void usage(String errMsg) {
        PrintStream out = System.out;
        if (errMsg != null) {
            out.println(errMsg);
        }
        out.println("Usage: example.shell.Main [-options] [classes...]");
        out.println("where options include:");
        out.println();
        out.println("    -register:<name_server>,[<localhost>:][<port>][,<name>]");
        out.println("                     Registers this shell on the name server (host:port).");
        out.println("                     Optionally specify the local shell's host, listening");
        out.println("                     port and name.");
        out.println("    -endian:<value>  endianess ('big' or 'little') for output isolate");
        out.println("                     (default: runtime system endianess)");
        out.println("    -verbose         verbose execution");
    }

    /**
     * Command line entry point.
     *
     * @param args  command line arguments
     */
    public static void main (String [] args) {

        Shell shell = new Shell();
        String configuration = null;

        int argc = 0;
        while (argc != args.length) {
            String arg = args[argc];
            if (!arg.startsWith("-")) {
                break;
            } else if (arg.startsWith("-register:")) {
                configuration = arg.substring("-register:".length());
            } else if (arg.equals("-verbose")) {
                shell.verbose = true;
            } else if (arg.startsWith("-endian:")) {
                String value = arg.substring("-endian:".length());
                if (value.equals("big")) {
                    shell.endianessOverride = new Boolean(true);
                } else if (value.equals("little")) {
                    shell.endianessOverride = new Boolean(false);
                } else {
                    usage("invalid endianess: " + value);
                    return;
                }
            } else {
                usage("Unknown option: " + arg);
                return;
            }
            argc++;
        }

        if (configuration != null) {
            try {
                shell.register(configuration);
            } catch (NumberFormatException e) {
                usage("bad '-register' option: port must be a number");
                return;
            } catch (IllegalArgumentException e) {
                usage("bad '-register' option: " + e.getMessage());
                return;
            }
        }

        if (argc != args.length) {
            shell.startupApps = new String[args.length - argc];
            System.arraycopy(args, argc, shell.startupApps, 0, shell.startupApps.length);
        }

        shell.startApp();
    }

    /**
     * Gets a system property via the systemproperties GCF protocol.
     *
     * @param key   the key of the property
     * @return the value of the property or null if it isn't defined or there was an IO error
     *         finding/reading from the systemproperties protocol
     */
    static String getHostSystemProperty(String key) {
        try {
            DataInputStream in = Connector.openDataInputStream("systemproperties:");
            while (in.available() != 0) {
                String k = in.readUTF();
                String v = in.readUTF();

                if (k.equals(key)) {
                    in.close();
                    return v;
                }
            }
            in.close();
        } catch (IOException ex) {
        }
        return null;
    }

    /**
     * Registers this shell with a lookup server and starts the listener that can receive isolates.
     *
     * @param configuration  configuration details
     * @return  true if registration succeeded
     *
     * @throws IllegalArgumentException if <code>configuration</code> has a problem
     * @throws NumberFormatException if any port numbers in <code>configuration</code> were not well-formed numbers
     */
    private void register(String configuration) throws IllegalArgumentException, NumberFormatException {
        registration = new Registration(configuration);
    }



    /**
     * A Registration instance represents the connection a shell has to a lookup server,
     */
    class Registration implements Runnable {

        final String host;
        String name;
        String lookupServerURL;
        DataOutputStream nameServerRequest;
        DataInputStream nameServerResponse;

        /**
         * The isolate listener.
         */
        private final IsolateListener isolateListener;


        /**
         * Creates and configures a registration to a lookup server,
         *
         * @param configuration    the specification of the connection to lookup server
         * @throws IllegalArgumentException if <code>configuration</code> has a problem
         * @throws NumberFormatException if any port numbers in <code>configuration</code> were not well-formed numbers
         */
        public Registration(String configuration) throws IllegalArgumentException, NumberFormatException {

            int comma = configuration.indexOf(',');
            if (comma == -1) {
                comma = configuration.length();
                configuration += ",";
            }

            // Extract the address of the lookup server
            String lookupServerHostAndPort = configuration.substring(0, comma);

            // If port is not specified use default listening port
            if (lookupServerHostAndPort.indexOf(":") == -1) {
                lookupServerHostAndPort += ":" + LookupServer.DEFAULT_LISTENING_PORT;
            }

            lookupServerURL = "socket://" + lookupServerHostAndPort;
            configuration = configuration.substring(comma + 1);

            // Extract the local host details
            String localhost;
            comma = configuration.indexOf(',');
            if (comma == -1) {
                localhost = configuration;
                String n = getHostSystemProperty("net.localhost.address");
                if (n == null) {
                    name = "";
                } else {
                    name = n;
                }
            } else {
                localhost = configuration.substring(0, comma);
                name = configuration.substring(comma + 1);
            }

            // Split local host details into host and port
            int colon = localhost.indexOf(':');
            int initialPort = -1;
            if (colon == -1) {
                host = getHostSystemProperty("net.localhost.address");
                if (host == null) {
                    throw new IllegalArgumentException("can't determine local hostname - specify it explicity");
                }

                if (localhost.length() == 0) {
                    initialPort = IsolateListener.BASE_LISTENING_PORT;
                } else {
                    initialPort = Integer.parseInt(localhost);
                }

            } else {
                host = localhost.substring(0, colon);
                initialPort = Integer.parseInt(localhost.substring(colon + 1));
            }

            // Create the isolate listener
            isolateListener = new IsolateListener(initialPort);
        }

        /**
         * Gets the URL of the lookup server.
         *
         * @return  the URL of the lookup server
         */
        public String getLookupServerURL() {
            return lookupServerURL;
        }

        /**
         * Closes this registration with the lookup server and stops the isolate listener.
         */
        public void close() {
            isolateListener.stop();
        }


        /**
         * Send an updated registration request to the lookup server
         *
         * @throws IOException if cannot sent to socket
         */
        private void sendRegistration() throws IOException {

            String hostAndPort = host + ":" + isolateListener.getPort();

            log("registering with lookup server at " + lookupServerURL);

            nameServerRequest.writeUTF("register");
            nameServerRequest.writeUTF(hostAndPort);

            log("registered with lookup server as " + hostAndPort);

            frame.setTitle("Shell [" + hostAndPort + "]");
        }



        private void sendPerformanceMetrics() throws IOException {

            log("sending isolate performance metrics to " + lookupServerURL);

            nameServerRequest.writeUTF("perfMetric");
            nameServerRequest.writeInt(isolateList.size());



            // print list of isolate performances
            for (Enumeration e = isolateList.elements(); e.hasMoreElements(); ) {
                Isolate i = (Isolate) e.nextElement();



                String metricValue = i.getProperty("performance.metric");
                long metric;
                if (metricValue == null) {
                    metric = 1L;
                } else {
                    metric = Long.parseLong(metricValue);
                }

                nameServerRequest.writeUTF(i.getMainClassName() + "_" + isolateList.indexOf(i));
                nameServerRequest.writeLong(metric);
                //log(i.getMainClassName() + " = " + metric);
            }

        }

        /**
         * {@inheritDoc}
         */
        public void run() {
            log("Connecting to lookup server " + lookupServerURL);
            StreamConnection con = null;

            try {
                con = (StreamConnection) Connector.open(lookupServerURL);
                nameServerRequest = con.openDataOutputStream();
                nameServerResponse = con.openDataInputStream();
            } catch (IOException e) {
                System.err.println("Error establishing connection to lookup server: " + e.getMessage());
                e.printStackTrace();
                return;
            }

            while (lookupServerURL != null) {
                int timeout = 5;

                try {
                    // Block the isolate listener while communicating with the lookup server
                    synchronized (communicationMutex) {
                        sendRegistration();
                        sendPerformanceMetrics();
                    }

                    // Re-register every n or 20 seconds, whichever is lesser,
                    // where n is the timeout for registrations with the lookup server
                    Thread.currentThread().sleep(Math.min(timeout * 1000, 20000));

                } catch (InterruptedException e) {
                } catch (IOException ioe) {
                    System.err.println("Error communicating with lookup server: " + ioe.getMessage());
                    log("Stopping further registrations");
                    //ioe.printStackTrace();
                    lookupServerURL = null;
                }
            }

            try {
                nameServerRequest.close();
                nameServerResponse.close();
                con.close();
            } catch (IOException ex1) {
            }
        }

        /**
         * Gets the set of shells registered with the lookup server.
         *
         * @param includeThis  specifies if the set of shells returned should include this shell
         * @return  the set of shells registered with the lookup server
         * @throws  if there was an IO error communicating with the lookup server
         */
        public Hashtable getRemoteShells(boolean includeThis) throws IOException {

            Hashtable shells;

            // Block the isolate listener while communicating with the lookup server
            synchronized (communicationMutex) {
                nameServerRequest.writeUTF("lookup");
                int size = nameServerResponse.readInt();
                shells = new Hashtable(size);
                while (size-- > 0) {
                    String hostAndPort = nameServerResponse.readUTF();
                    shells.put(hostAndPort, hostAndPort);
                }
            }


            if (!includeThis) {
                String hostAndPort = host + ":" + isolateListener.getPort();
                shells.remove(hostAndPort);
            }

            return shells;
        }

    }

    /**
     * A server that listens for incoming isolates.
     */
    class IsolateListener implements Runnable {

        private final int port;
        private volatile boolean stop;
        private StreamConnectionNotifier ssocket;

        public static final int BASE_LISTENING_PORT = 8000;

        // Constructor.  Get a free listening port.
        public IsolateListener(int port) {

            // Find a free port to listen on
            boolean finished = false;

            // Find an empty port
            while (!finished) {
                try {
                    ssocket = (StreamConnectionNotifier) Connector.open("serversocket://:" + port);
                    finished = true;
                } catch (IOException ex) {
                    log("desired isolate listening port " + port + " is already in use. Trying " + (port + 1));
                    port++;
                }
            }

            this.port = port;

            new Thread(this).start();
        }


        public void run() {
            // Fork new threads to handle requests
            log("listening for isolates on port " + port);
            while (!stop) {
                StreamConnection con = null;

                try {
                    con = ssocket.acceptAndOpen();
                } catch (IOException ex1) {
                    stop = true;
                    log("Failed listening on server socket" + ex1.getMessage());
                    ex1.printStackTrace();
                }

                new Thread(new IsolateHandler(con, this)).start();
            }

            try {
                ssocket.close();
            } catch (IOException ex2) {
            }
        }

        /**
         * Get the port that this shell is listening for requests on.
         *
         * @return the port number it is listening on
         */
        public int getPort() {
            return port;
        }

        /**
         * Stop listening for new connections
         */
        public void stop() {
            stop = true;
        }

    }


    /**
     * Handles an incoming isolate migration request.
     *
     * @author Andrew Crouch
     * @version 1.0
     */
    class IsolateHandler implements Runnable {

        private StreamConnection socket;
        private IsolateListener isolateListener;

        IsolateHandler(StreamConnection socket, IsolateListener isolateListener) {
            this.socket = socket;
            this.isolateListener = isolateListener;
        }

        /**
         * Receive a number of isolates, then restart them.
         *
         * @param request    the <code>DataInputStream</code> on which to receive the isolate data
         * @throws an <code>IOException</code> if problem reading from stream
         */
        private void runIsolateRequest(DataInputStream request) throws IOException {
            int isolatesToReceive = request.readInt();
            try {
                while (isolatesToReceive > 0) {
                    Isolate isolate = Isolate.load(request, "serversocket://:" + isolateListener.getPort());
                    log("received " + isolate);
                    isolate.setProperty("save_after_join", "true");

                    restart(isolate);
                    isolatesToReceive--;
                }
            } catch (OutOfMemoryError e) {
                OptionDialog.showMessageDialog(frame, "Out of memory error loading isolate: " + e.getMessage());
            } catch (Error le) {
                log("Failed to receive Isolate");
            }
        }

        /**
         * Hibernates and sends a number of isolates to a remote shell.
         *
         * @param request    the <code>DataInputStream</code> on which to receive the isolate data
         * @throws an <code>IOException</code> if problem reading from stream
         */
        private void sendIsolateRequest(DataInputStream dis, DataOutputStream dos) throws IOException {
            int numberOfIsolatesToMigrate = Math.min(dis.readInt(), isolateList.size());
            log("sending " + numberOfIsolatesToMigrate + " isolates");

            String[] isolateNames = new String[numberOfIsolatesToMigrate];
            for(int i = 0; i < numberOfIsolatesToMigrate; i++) {
                isolateNames[i] = dis.readUTF();
            }

            String destination = dis.readUTF();
            log("sending isolate to " + destination);

            DataOutputStream isolatedos = Connector.openDataOutputStream(destination);
            isolatedos.writeUTF("runIsolate");
            isolatedos.writeInt(numberOfIsolatesToMigrate);

            while (numberOfIsolatesToMigrate > 0) {

                // IsolateID in form  name_vectorIndex
                String isolateID = isolateNames[numberOfIsolatesToMigrate - 1];
                int index = Integer.parseInt(isolateID.substring(isolateID.lastIndexOf('_') + 1));
                String isolateName = isolateID.substring(0, isolateID.lastIndexOf('_'));

                // check array bounds
                if( index < isolateList.size()) {
                    Isolate i = (Isolate) isolateList.elementAt(index);

                    // Only migrate if isolate name matches
                    if (i.getMainClassName().equals(isolateName)) {
                        isolateList.removeElement(i);

                        i.setProperty("save_after_join", "false");
                        i.hibernate();
                        saveIsolate(i, destination, isolatedos);
                        log("Sent isolate " + i.getMainClassName());
                    }
                }
                numberOfIsolatesToMigrate--;

            }
            isolatedos.close();

            dos.writeUTF("");
        }

        // The main listening socket
        public void run() {
            String action = "unknown";
            try {

                // Block registration/updates while handling an isolate request.
                // Allows connection setup, but no isolate migration until this
                // request is finished
                synchronized (communicationMutex) {


                    DataInputStream dis = socket.openDataInputStream();
                    DataOutputStream dos = socket.openDataOutputStream();

                    action = dis.readUTF();
                    log("received '" + action + "' request");


                    if (action.equals("runIsolate")) {
                        runIsolateRequest(dis);
                    } else if (action.equals("sendIsolate")) {
                        sendIsolateRequest(dis, dos);
                    } else {
                        log("unknown request type: " + action);
                    }

                    dis.close();
                    dos.close();
                    socket.close();
                }
            } catch (IOException e) {
                OptionDialog.showMessageDialog(frame, "IO error handling " + action + " request: " + e.getMessage());
                e.printStackTrace();
            }
            log("Finished handling " + action + " successfully");
        }
    }
}
