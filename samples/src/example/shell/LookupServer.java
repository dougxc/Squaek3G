/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package example.shell;

import java.io.*;
import java.util.Enumeration;
import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.io.*;

import com.sun.squawk.util.Arrays;
import com.sun.squawk.util.Comparer;

/**
 * This is a basic server for federating a collection of {@link Shell shells}
 * that will accept incoming isolates.
 *
 * @author  Doug Simon
 */
public class LookupServer {

    private static final int DEFAULT_REGISTRATION_TIMEOUT = 30;
    public static final int DEFAULT_LISTENING_PORT = 9999;

    /**
     * Verbose execution.
     */
    private boolean verbose;

    /**
     * The collection of shells that have registered with this server.
     */
    private Hashtable shells = new Hashtable();

    /**
     * The amount of time (in seconds) a registration will last before it is removed.
     */
    private int registrationTimeout;

    /**
     * The load balancer currently in use
     */
    private LoadBalancer loadBalancer;


    /**
     * Creates a server.
     *
     * @param port    the listening port
     * @param verbose verbose execution flag
     */
    public LookupServer(int port, boolean verbose, int timeout, String loadBalancerName) {
        this.verbose = verbose;
        this.registrationTimeout = timeout;

        // Start the load balancer
        if(loadBalancerName != null) {
            if(loadBalancerName.equals("Isolate")) {
                loadBalancer = new IsolateLoadBalancer();
            } else if(loadBalancerName.equals("Cubes")) {
                loadBalancer = new CubesLoadBalancer();
            } else if(loadBalancerName.equals("ManyBalls")) {
                loadBalancer = new ManyBallsLoadBalancer();
            }
        }

        if(loadBalancer != null) {
            new Thread(loadBalancer).start();
        }


        StreamConnectionNotifier ssocket = null;
        try {
            String address = Shell.getHostSystemProperty("net.localhost.address");
            log("starting shell lookup service" + (address == null ? "" : " on " + address));
            ssocket = (StreamConnectionNotifier)Connector.open("serversocket://:" + port);
            while (true) {
                log("listening on port " + port);
                StreamConnection con = ssocket.acceptAndOpen();

                new Thread(new ShellConnectionListener(con)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
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
        out.println("usage: LookupServer [-options]");
        out.println("where options include:");
        out.println();
        out.println("    -timeout:<n>  registrations timeout after 'n' seconds (default=" + DEFAULT_REGISTRATION_TIMEOUT + "s)");
        out.println("    -port:<n>     set the listening port to 'n'.  (default=" + DEFAULT_LISTENING_PORT + ")");
        out.println("    -loadbalance:<t>");
        out.println("                  Load balance using <t> metric where <t> is one of:");
        out.println("                     ManyBalls - equal number of balls on each shell");
        out.println("                     Isolate - equal number of isolates on each shell");
        out.println("                     Cubes - maximise the fps in Cubes demo");
        out.println("    -verbose      verbose execution");
    }

    /**
     * Parse a set of command line arguments and configure a server based on them.
     *
     * @param args     the command line arguments
     * @return the server configured by <code>args</code> or null if the args were not well formed
     */
    private static LookupServer parseArgs(String[] args) {

        int argc = 0;
        boolean verbose = false;
        int port = DEFAULT_LISTENING_PORT;
        int timeout = DEFAULT_REGISTRATION_TIMEOUT;
        String loadBalancer = null;

        while (argc != args.length) {
            String arg = args[argc];
            if (arg.charAt(0) != '-') {
                break;
            } else if (arg.equals("-verbose")) {
                verbose = true;
            } else if (arg.startsWith("-loadbalance")) {
                String metric = arg.substring("-loadbalance:".length());
                if(!(metric.equals("Isolate") || metric.equals("Cubes") || metric.equals("ManyBalls"))) {
                    usage("Unknown load balancer: " + metric);
                    return null;
                }
                loadBalancer = metric;
            } else if (arg.startsWith("-timeout")) {
                String stringTimeout = arg.substring("-timeout:".length());
                try {
                    timeout = Integer.parseInt(stringTimeout);
                } catch (NumberFormatException ex) {
                    usage("Invalid timeout: " + stringTimeout);
                }
            } else if (arg.startsWith("-port")) {
                String stringPort = arg.substring("-port:".length());
                try {
                    port = Integer.parseInt(stringPort);
                } catch (NumberFormatException ex) {
                    usage("Invalid listening port: " + stringPort);
                }
            } else {
                usage("Unknown option: " + arg);
                return null;
            }
            argc++;
        }
        return new LookupServer(port, verbose, timeout, loadBalancer);
    }

    /**
     * Command line entry point.
     *
     * @param args  command line arguments
     */
    public static void main(String[] args) {
        // start the lookup server
        LookupServer server = parseArgs(args);
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
     * Removes timed out entries from the table of registered shells.
     */
    private void removeOldEntries() {

        long now = System.currentTimeMillis();

        Hashtable temp = new Hashtable(shells.size());
        for (Enumeration e = shells.keys(); e.hasMoreElements(); ) {
            ShellConnectionListener entry = (ShellConnectionListener) e.nextElement();
            if (now - entry.lastMessage <= (registrationTimeout * 1000)) {
                temp.put(entry, entry);
            } else {
                log("removed timed out registration for " + entry.hostAndPort);
            }
        }

        shells = temp;
    }

    /**
     * Handles a request to register a Shell that will accept incoming isolates.
     *
     * @param dis   the request
     * @throws IOException if there was an IO error
     */
    private void registerShellRequest(ShellConnectionListener remoteShell) {
        shells.put(remoteShell, remoteShell);
        log("registered '" + remoteShell.hostAndPort + "' at " + new Date());
    }

    /**
     * Handles a request to deregister a Shell.
     *
     * @param dis   the request
     * @throws IOException if there was an IO error
     */
    private void deregisterShellRequest(ShellConnectionListener remoteShell) {
        shells.remove(remoteShell);
        log("deregistered '" + remoteShell.hostAndPort + " at " + new Date());
    }

    /**
     * Handles a request to get a listing of all the registered shells.
     *
     * @param dos   where to write the response
     * @throws IOException if there was an IO error
     */
    private void lookupShellsRequest(DataOutputStream dos) throws IOException {

        // check for stale data
        removeOldEntries();

        dos.writeInt(shells.size());
        for (Enumeration keys = shells.keys(); keys.hasMoreElements(); ) {
            ShellConnectionListener entry = (ShellConnectionListener)keys.nextElement();
            dos.writeUTF(entry.hostAndPort);
        }
    }


   private class ManyBallsMetric {
       public String isolateIdentifier;
       public long metric;
       public ShellConnectionListener shell;

       ManyBallsMetric(String isolateIdentifier, long metric, ShellConnectionListener shell) {
           this.isolateIdentifier = isolateIdentifier;
           this.metric = metric;
           this.shell = shell;
       }
   }

    /**
     * An instance of this class handles a connection to a single client shell.
     */
    public class ShellConnectionListener implements Runnable {

        /**
         * Single client connection.
         */
        private final StreamConnection con;
        private volatile boolean finished;

        /**
         * Shell specific variables
         */
        private String hostAndPort;
        private Vector isolateList = new Vector();
        private int averageFPS = 0;
        private long lastMessage = System.currentTimeMillis();

        // set after a migration. If stale will not cause another migration
        // until this is unset
        private boolean stale = false;

        /**
         * Constructor.  Sets the stream connection to shell.
         *
         * @param con    the socket connection to remote shell
         */
        ShellConnectionListener(StreamConnection con) {
            this.con = con;
        }

        public int isolatesRunning() {
            return isolateList.size();
        }

        /**
         * Handles the performance metric data from this shell.
         *
         * @param request    the input stream to read the data
         * @throws IOException if problem reading from socket
         */
        private void handlePerformanceMetrics(DataInputStream request) throws IOException {

            int runningIsolates = request.readInt();
            isolateList = new Vector();

            long metricTotal = 0;
            for (int i = 0; i < runningIsolates; i++) {
                String isolateName = request.readUTF();
                long isolateMetric = request.readLong();
                isolateList.addElement(new ManyBallsMetric(isolateName, isolateMetric, this));
                metricTotal += isolateMetric;
            }

            if (runningIsolates > 0) {
                averageFPS = (int) (metricTotal / runningIsolates);
            } else {
                averageFPS = 0;
            }

            this.stale = false;
         }

         /**
          * Migrate a variable number of isolates to a destination shell
          *
          * @param isolatesToMigrate   the number of isolates to move to the destination shell
          * @param destination         the <code>ShellConnectionListener</code> associated with the destination
          * remote shell
          */
         public void migrateTo(int isolatesToMigrate, ShellConnectionListener destination) {
             try {

                 // Do not try to migrate more than what is running
                 isolatesToMigrate = Math.min(isolatesRunning(), isolatesToMigrate);

                 // make new connection
                 String dest = "socket://" + hostAndPort;

                 log("Opening connection to " + dest);
                 StreamConnection con = (StreamConnection) Connector.open(dest); ;
                 DataOutputStream output = con.openDataOutputStream();
                 DataInputStream input = con.openDataInputStream();

                 output.writeUTF("sendIsolate");
                 output.writeInt(isolatesToMigrate);

                 // This should probably move the longest running isolates, but there are
                 // no guarentees.
                 while(isolatesToMigrate > 0 && isolateList.size() > 0) {
                     ManyBallsMetric m = ((ManyBallsMetric)isolateList.firstElement());
                     isolateList.removeElement(m);
                     output.writeUTF(m.isolateIdentifier);
                     isolatesToMigrate--;
                 }

                 output.writeUTF("socket://" + destination.hostAndPort);

                 // block until finished moving
                 input.readUTF();

                 output.close();
                 input.close();
                 con.close();

                 log("Finished moving " + isolatesToMigrate + " isolate(s) from " + hostAndPort + " to " +
                     destination.hostAndPort);
             } catch (IOException ex) {
                 log("Can't migrate since cannot open connection to source shell");
             }

             // invalidate data until we receive another performance update
             stale = true;
             destination.stale = true;
        }

        /**
         * Migrate a given isolate to a destination shell
         *
         * @param isolatesToMigrate   the number of isolates to move to the destination shell
         * @param destination         the <code>ShellConnectionListener</code> associated with the destination
         * remote shell
         */
        public void migrateTo(String isolateName, ShellConnectionListener destination) {
            try {

                // invalidate data until we receive another performance update
                stale = true;

                // make new connection
                String dest = "socket://" + hostAndPort;

                log("Opening connection to " + dest);
                StreamConnection con = (StreamConnection) Connector.open(dest); ;
                DataOutputStream output = con.openDataOutputStream();
                DataInputStream input = con.openDataInputStream();

                output.writeUTF("sendIsolate");
                output.writeInt(1);
                output.writeUTF(isolateName);
                output.writeUTF("socket://" + destination.hostAndPort);

                // block until finished moving
                input.readUTF();

                output.close();
                input.close();
                con.close();

                log("Finished moving " + isolateName + " from " + hostAndPort + " to " +
                    destination.hostAndPort);
            } catch (IOException ex) {
                log("Can't migrate since cannot open connection to source shell");
            }

            // invalidate data until we receive another performance update
            stale = true;
            destination.stale = true;
        }

         /**
          * The main server loop.
          */
         public void run() {
             finished = false;

              try {
                  DataInputStream request = con.openDataInputStream();
                  DataOutputStream response = con.openDataOutputStream();

                  while(!finished) {

                      // Block waiting for info
                      String requestType = request.readUTF();

                      lastMessage = System.currentTimeMillis();

                      log("received '" + requestType + "' request");
                      if (requestType.equals("register")) {
                          hostAndPort = request.readUTF();
                          registerShellRequest(this);
                      } else if (requestType.equals("lookup")) {
                          lookupShellsRequest(response);
                      } else if (requestType.equals("perfMetric")) {
                          handlePerformanceMetrics(request);
                      } else {
                          log("unknown request type: " + requestType);
                      }

                      if(loadBalancer != null) {
                          loadBalancer.reportEvent();
                      }

                      log("completed handling request");
                  }
              } catch (IOException ex) {
                  deregisterShellRequest(this);
                  log("Client closed connection or IO error: " + ex.getMessage());
                  //ex.printStackTrace();
              }
          }
    }


    public abstract class LoadBalancer implements Runnable, Comparer {

        private static final int DEFAULT_LOAD_BALANCE_TIME = 15;
        private static final int MIGRATION_THRESHOLD = 2;

        /**
         * Indicates some event has occured
         */
        private volatile int events = 0;

        public void reportEvent() {
            events++;
        }

        public abstract void migrate(Vector v);

        public void run() {

            while (true) {

                try {
                    // Re-calculate every 15 seconds load balancing data
                    Thread.currentThread().sleep(DEFAULT_LOAD_BALANCE_TIME * 1000);
                } catch (InterruptedException e) {}

                // wait for event
                if(events == 0) {
                    log("LoadBalancer: no new events");
                    continue;
                }

                // Prune the timed out registrations
                removeOldEntries();

                // build an ordered vector
                SortableVector v = new SortableVector(shells.size());
                for (Enumeration keys = shells.keys(); keys.hasMoreElements(); ) {
                    ShellConnectionListener next = (ShellConnectionListener) keys.nextElement();
                    v.addElement(next);
                }

                v.sort(this);
                migrate(v);
            }
        }
    }

    /**
     * Class that moves isolates between shells such that the shells on each
     * run a fair number of isolates.
     *
     * @author Andrew Crouch
     * @version 1.0
     */
    public class IsolateLoadBalancer extends LoadBalancer  {

        private static final int MIGRATION_THRESHOLD = 2;

        // So we can order the nodes
        public int compare(Object o1, Object o2) {

            if(o1 == o2) {
                return 0;
            }

            ShellConnectionListener scl1 = (ShellConnectionListener)o1;
            ShellConnectionListener scl2 = (ShellConnectionListener)o2;

            if(scl1.isolatesRunning() == scl2.isolatesRunning()) {
                return 0;
            }
            if(scl1.isolatesRunning() < scl2.isolatesRunning()) {
                return -1;
            } else {
                return 1;
            }
        }

        public void migrate(Vector v) {

            // Look for potential shells to migrate isolates to
            while (v.size() > 1) {
                ShellConnectionListener min = (ShellConnectionListener) v.firstElement();
                ShellConnectionListener max = (ShellConnectionListener) v.lastElement();
                v.removeElement(min);
                v.removeElement(max);

                log("max = " + max.isolatesRunning() + " , min = " + min.isolatesRunning());

                int diff = (max.isolatesRunning() - min.isolatesRunning());
                if (diff >= MIGRATION_THRESHOLD) {
                    log("migrating " + diff/2 + " isolates");

                    // Don't migrate until we receive perf update from both max and min
                    if(max.stale != true && min.stale != true) {
                        // block while we migrate
                        max.migrateTo(diff/2, min);
                    }
                } else {
                    break;
                }
            }
        }
    }

    /**
     * Class that moves isolates between shells such that the fps (frames per second) metric
     * is maximised.  Only used in Cubes demo.
     *
     * @author Andrew Crouch
     * @version 1.0
     */
    public class CubesLoadBalancer extends LoadBalancer {

        private static final int MIGRATION_THRESHOLD = 5;

        // So we can order the nodes
        public int compare(Object o1, Object o2) {

            if(o1 == o2) {
                return 0;
            }

            ShellConnectionListener scl1 = (ShellConnectionListener)o1;
            ShellConnectionListener scl2 = (ShellConnectionListener)o2;

            if(scl1.averageFPS == scl2.averageFPS) {
                return 0;
            }

            // If not running isolates, give them a "trial" to see what fps they can get by
            // putting them at top of list
            if(scl1.isolatesRunning() == 0) {
                return 1;
            }

           if(scl2.isolatesRunning() == 0) {
               return -1;
           }

            if(scl1.averageFPS < scl2.averageFPS) {
                return -1;
            } else {
                return 1;
            }
        }

        public void migrate(Vector v) {
            // sort list based on # of running isolates
            while (v.size() > 1) {
                ShellConnectionListener min = (ShellConnectionListener) v.firstElement();
                ShellConnectionListener max = (ShellConnectionListener) v.lastElement();
                v.removeElement(min);
                v.removeElement(max);

                log("max = " + max.averageFPS + " , min = " + min.averageFPS);

                int diff = Math.abs(max.averageFPS - min.averageFPS);
                if (diff >= MIGRATION_THRESHOLD) {
                    log("migrating an isolates");

                    // Don't migrate until we receive perf update from both max and min
                    if(max.stale != true && min.stale != true) {
                        // block while we migrate
                        min.migrateTo(1, max);
                    }

                } else {
                    break;
                }
            }
        }
    }


    /**
     * Class that moves ManyBalls isolates between shells such that number of balls is distributed
     * evenly across all running shells.
     *
     * @author Andrew Crouch
     * @version 1.0
     */
    public class ManyBallsLoadBalancer extends LoadBalancer {

        // So we can order the nodes
        public int compare(Object o1, Object o2) {
            return 0;
        }

        /**
         * Get the total number of balls running on a particular shell.
         *
         * @param shell ShellConnectionListener
         * @return int
         */
        private int getTotalNumberOfBallsOnShell(ShellConnectionListener shell) {
            Vector isolateList = shell.isolateList;
            int total = 0;
            for(Enumeration e = isolateList.elements(); e.hasMoreElements();) {
                total += ((ManyBallsMetric)e.nextElement()).metric;
            }

            return total*1000;
        }

        /**
         * Determine how much this shell deviates from the average.
         *
         * @param shell    the shell in question
         * @param average  the established average
         * @return   the number of balls by which this shell deviates from the mean
         */
        private int getDeviation(ShellConnectionListener shell, int average) {
            return Math.abs(getTotalNumberOfBallsOnShell(shell) - average);
        }

        /**
         * Determines the average number of balls that should be running on each shell.
         *
         * @param v    the <code>Vector</code> containing the known shells
         * @return     the average number of balls that are on each shell.
         */
        private int getMeanNumberOfBalls(Vector v) {
            int total = 0;
            for(Enumeration e = v.elements(); e.hasMoreElements();) {
                total += getTotalNumberOfBallsOnShell((ShellConnectionListener)e.nextElement());
            }

            if(v.size() == 0) {
                return 0;
            }

            return total/v.size();
        }

        /**
         * Determine the average deviation from the mean
         *
         * @param v Vector
         * @return int
         */
        private int getMeanDeviation(Vector v) {
            int total = 0;
            int mean = getMeanNumberOfBalls(v);
            for(Enumeration e = v.elements(); e.hasMoreElements();) {
                total += getDeviation((ShellConnectionListener)e.nextElement(), mean);
            }

            if(v.size() == 0) {
                return 0;
            }

            return total/v.size();
        }

        /**
         * Determine the new deviation based on this move
         *
         * @param isolate      the isolate with running a ManyBallsInstance
         * @param destination  a candidate to move to
         * @return int the new deviation
         */
        private int getDeviationBasedOnMove(ManyBallsMetric isolate, ShellConnectionListener destination, Vector shells) {
            ShellConnectionListener src = isolate.shell;
            ShellConnectionListener dest = destination;

            dest.isolateList.addElement(isolate);
            src.isolateList.removeElement(isolate);

            int newDeviation = getMeanDeviation(shells);

            src.isolateList.addElement(isolate);
            dest.isolateList.removeElement(isolate);

            return newDeviation;
        }


        /**
         * Determine a single migration that will lower our average deviation.
         *
         * @param v   the list of shells
         */
        public void migrate(Vector v) {

            // The deviation from mean before the move
            int preMoveAverageDeviation = getMeanDeviation(v);
            int averageBalls = getMeanNumberOfBalls(v);


            log("Average number of balls: " + averageBalls/1000 + "." + averageBalls%1000);
            log("Average deviation: " + preMoveAverageDeviation/1000 + "." + preMoveAverageDeviation%1000);

            // build a list of all running many balls isolates
            Vector masterIsolateList = new Vector();
            for(Enumeration e = v.elements(); e.hasMoreElements();) {
                ShellConnectionListener shell = (ShellConnectionListener)e.nextElement();

                // Don't include shell if we've migrated and not heard from them yet
                if(shell.stale == true) {
                    continue;
                }

                for(Enumeration f = shell.isolateList.elements(); f.hasMoreElements();) {
                    masterIsolateList.addElement(f.nextElement());
                }
            }


            // Figure out available moves
            int bestDeviation = preMoveAverageDeviation;

            // Used to invoke move
            ShellConnectionListener destination = null;
            ManyBallsMetric isolate = null;

            // For each shell it looks at the effect of migrating 1 isolate to it.
            // If it improves the deviation (ie lowers it), the move is recorded
            for(Enumeration e = v.elements(); e.hasMoreElements();) {
                ShellConnectionListener dest = (ShellConnectionListener)e.nextElement();

                // Don't look at migrating isolates to this shell if already greater than
                // the average or if we've migrated an isolate
                if(getTotalNumberOfBallsOnShell(dest) > averageBalls || dest.stale == true) {
                    continue;
                }

                for(Enumeration f = masterIsolateList.elements(); f.hasMoreElements();) {
                    ManyBallsMetric m = (ManyBallsMetric)f.nextElement();

                    // Can't move isolate when src/dest the same
                    if (m.shell != dest) {
                        int dev = getDeviationBasedOnMove(m, dest, v);

                        // We found a good move if deviation is improved
                        if(dev < bestDeviation) {
                            bestDeviation = dev;
                            destination = dest;
                            isolate = m;
                        }
                    }
                }
            }

            if(bestDeviation < preMoveAverageDeviation) {
                int diff = preMoveAverageDeviation - bestDeviation;
                log("Found a move that improves deviation by " + diff/1000 + "." + diff%1000 + " units");

                isolate.shell.migrateTo(isolate.isolateIdentifier, destination);
            }
        }
    }



    /**
     * This is a subclass of Vector that can have its elements sorted by a
     * Comparer object.
     */
    public static class SortableVector extends Vector {

        /**
         * Constructs an empty vector with the specified initial capacity and
         * capacity increment.
         *
         * @param   initialCapacity     the initial capacity of the vector.
         * @param   capacityIncrement   the amount by which the capacity is
         *                              increased when the vector overflows.
         * @exception IllegalArgumentException if the specified initial capacity
         *            is negative
         */
        public SortableVector(int initialCapacity, int capacityIncrement) {
            super(initialCapacity, capacityIncrement);
        }

        /**
         * Constructs an empty vector with the specified initial capacity.
         *
         * @param   initialCapacity   the initial capacity of the vector.
         * @since   JDK1.0
         */
        public SortableVector(int initialCapacity) {
            super(initialCapacity, 0);
        }

        /**
         * Constructs an empty vector.
         *
         * @since   JDK1.0
         */
        public SortableVector() {
            super(10);
        }

        /**
         * Sort the elements in the vector using a given Comparer.
         * @param comparator
         */
        public void sort(Comparer comparer) {
            Arrays.sort(elementData, 0, elementCount, comparer);
        }
    }
}
