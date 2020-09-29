//if[EXCLUDE]
/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package drivers.alarm;

import java.io.*;
import javax.microedition.io.*;
import com.sun.squawk.vm.ChannelConstants;
import com.sun.squawk.*;
import com.sun.squawk.io.ServerConnectionHandler;
import com.sun.squawk.io.j2me.msg.MessageOutputStream;

/**
 * An alarm device driver.
 *
 * @author Alex Garthwaite
 */
public class AlarmDriver implements Driver {

    /**
     * The StreamConnection holding the active I/O request.
     */
    private StreamConnection activeRequest;

    /**
     * The output data stream for the active connection.
     */
    private DataOutputStream out;

    /**
     * Some timer information.
     *    requestTime: timestamp when the global timer is configured
     *    processTime: timestamp when a timer interrupt is received
     * Other timer information relating to the processing of timer
     * interrupts is gathered by the signal handler and reported through
     * the "////dev-alarm-status" interface (see below).
     *
     * These allow the timing characteristics of a platform's timer facility
     * to be measured (see SingleAlarmDriverExample.java).
     */
    private long requestTime;
    private long processTime;

    /**
     * The properties for the global timer.
     *     start:  time in microseconds to first interrupt
     *     period: time in microseconds until each subsequent interrupt
     * Note that if start is 0, the timer is disabled and if period is 0,
     * then the interrupt is only generated once.
     */
    int start = 0;
    int period = 0;

    /**
     * Initializes the driver.
     */
    public void initialize() {
        ServerConnectionHandler sch;

        sch = new ServerConnectionHandler("////dev-alarm-request") {
            public void processConnection(StreamConnection con) {
                processRequest(con);
            }
        };
        VM.addServerConnectionHandler(sch);

        sch = new ServerConnectionHandler("////dev-alarm-status") {
            public void processConnection(StreamConnection con) {
                processStatus(con);
            }
        };
        VM.addServerConnectionHandler(sch);

        sch = new ServerConnectionHandler(JavaDriverManager.deviceInterruptName(14)) {
            public void processConnection(StreamConnection con) {
                processInterrupt(con);
            }
        };
        VM.addServerConnectionHandler(sch);
        JavaDriverManager.setupInterrupt(14, null);

//        new Thread() {
//            public void run() {
//                VM.println("...");
//                Thread.yield();
//            }
//        }.start();

    }

    /**
     * Processes a new I/O operation.
     *
     * @param con the input message connection
     */
    private void processRequest(StreamConnection con) {
        try {
            activeRequest = con;
            DataInputStream in = con.openDataInputStream();
            MessageOutputStream messageOut = (MessageOutputStream) con.openOutputStream();
            out = new DataOutputStream(messageOut);

            if (in.available() != 8) {
                in.close();
                messageOut.setStatus(ChannelConstants.RESULT_BADPARAMETER);
                out.close();

                activeRequest.close();
                activeRequest = null;
                return;
            }

            start = in.readInt(); // Read parms.
            period = in.readInt(); // Read parms.
            in.close();

//System.err.println("Got request " + start + " " + period);
            requestTime = VM.getTimeMicros();
            JavaDriverManager.setupAlarmInterval(start, period);

            if (start == 0) {
                out.close();
                out = null;
                activeRequest.close();
                activeRequest = null;
            }
        } catch (IOException ex) {
            System.err.println("IOException " + ex);
            System.exit(1);
        }
    }

    /**
     * Processes an interrupt message.
     *
     * @param con the input message connection
     */
    private void processInterrupt(StreamConnection con) {
        try {
            /*
             * Note, the low-level C code has disabled the interrupt
             * for our hardware.
             */
            processTime = VM.getTimeMicros();

            /* Complete the I/O request */
            out.writeLong(requestTime);
            out.writeLong(processTime);

            if (period == 0) {
                out.close();
                out = null;
                activeRequest.close();
                activeRequest = null;
            } else {
                out.flush();
            }

            /*
             * Closing this connection signals to the low-level
             * C code that hardware interrupts should be enabled.
             */
            con.close();
        } catch (IOException ex) {
            System.err.println("IOException " + ex);
            System.exit(1);
        }
    }

    /**
     * Processes a status request.
     *
     * @param con the input message connection
     */
    private void processStatus(StreamConnection con) {
        try {
            /* No input for this one. Just report the stats. */
            int caught = (int)JavaDriverManager.getInterruptStatus(14, JavaDriverManager.STATUS_CAUGHT);
            int ignored = (int)JavaDriverManager.getInterruptStatus(14, JavaDriverManager.STATUS_IGNORED);
            long timestamp = JavaDriverManager.getInterruptStatus(14, JavaDriverManager.STATUS_TIMESTAMP);
            DataOutputStream out = con.openDataOutputStream();
            out.writeInt(caught);
            out.writeInt(ignored);
            out.writeLong(timestamp);
            out.close();
            con.close();
        } catch (IOException ex) {
            System.err.println("IOException " + ex);
            System.exit(1);
        }
    }

}
