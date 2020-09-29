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
import com.sun.squawk.*;

public class Example {

    public static void main(String args[]) {

        if (!JavaDriverManager.loadDriver("drivers.nop.NopDriver")) {
            System.exit(1);
        }
        for (int i = 0; i < 20; i++) {
            try {
                StreamConnection con = (StreamConnection) Connector.open("msg:////dev-null-status");
                DataInputStream in = con.openDataInputStream();
                DataOutputStream out = con.openDataOutputStream();

                long start = VM.getTimeMicros();
                out.writeInt(1234);
                out.close();

                long timestamp = in.readLong();
                long counter = in.readInt();
                in.close();
                long end = VM.getTimeMicros();

                con.close();

                System.out.println("time-in: " + (timestamp - start) +
                                   " time-back: " + (end - timestamp) +
                                   " round-trip: " + (end - start) +
                                   " counter: " + counter);
            } catch (IOException e) {
            }
        }
    }

}
