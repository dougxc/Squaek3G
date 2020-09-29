//if[EXCLUDE]
/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package drivers.nop;

import java.io.*;
import javax.microedition.io.*;
import com.sun.squawk.vm.ChannelConstants;
import com.sun.squawk.*;
import com.sun.squawk.io.ServerConnectionHandler;
import com.sun.squawk.io.j2me.msg.MessageOutputStream;

/**
 * A null device driver.
 *
 * @author Alex Garthwaite
 */
public class NopDriver implements Driver {

    int queryCount = 0; // Number of times null-status invoked so far

    /**
     * Initializes the driver.
     */
    public void initialize() {
        ServerConnectionHandler sch;

        sch = new ServerConnectionHandler("////dev-null-status") {
            public void processConnection(StreamConnection con) {
                processStatus(con);
            }
        };
        VM.addServerConnectionHandler(sch);
    }

    /**
     * Processes a status request.
     *
     * @param con the input message connection
     */
    private void processStatus(StreamConnection con) {
        try {
            DataInputStream in = con.openDataInputStream();
            int p1;

            if (in.available() != 4) {
                MessageOutputStream messageOut = (MessageOutputStream) con.openOutputStream();
                DataOutputStream out = new DataOutputStream(messageOut);
                in.close();
                messageOut.setStatus(ChannelConstants.RESULT_BADPARAMETER);
                out.close();
                con.close();
                return;
            }

            p1 = in.readInt(); // get status command parameter
            in.close();

            DataOutputStream out = con.openDataOutputStream();
            out.writeLong(VM.getTimeMicros()); // Some result value
            out.writeInt(queryCount++); // Some result value
            out.close();

            con.close();
        } catch (IOException ex) {
            System.err.println("IOException " + ex);
            System.exit(1);
        }
    }

}
