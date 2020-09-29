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
import com.sun.squawk.*;

public class SingleExample {

    public static void main(String args[]) {

//        new Thread() {
//            public void run() {
//                VM.println("...");
//                Thread.yield();
//            }
//        }.start();

        if (!JavaDriverManager.loadDriver("drivers.alarm.AlarmDriver")) {
            System.exit(1);
        }
        for (int i = 0; i < 20; i++) {
            try {
                StreamConnection con = (StreamConnection) Connector.open("msg:////dev-alarm-request");
                DataInputStream in = con.openDataInputStream();
                DataOutputStream out = con.openDataOutputStream();

                long requestStart = VM.getTimeMicros();
                out.writeInt(20000);
                out.writeInt(0);

                out.close();

                long requestTime = in.readLong();
                long processTime = in.readLong();
                in.close();
                long requestEnd = VM.getTimeMicros();

                con.close();

                con = (StreamConnection) Connector.open("msg:////dev-alarm-status");
                in = con.openDataInputStream();
                //out = con.openDataOutputStream();

                long statusStart = VM.getTimeMicros();
                //out.writeInt(0);
                //out.close();

                int caught = in.readInt();
                int ignored = in.readInt();
                long timestamp = in.readLong();
                in.close();
                long statusEnd = VM.getTimeMicros();

                con.close();

                System.out.println("request time-in: " + (requestTime - requestStart) +
                                   " to-interrupt: " + (timestamp - requestTime) +
                                   " to-process: " + (processTime - timestamp) +
                                   " back-to-app: " + (requestEnd - processTime) +
                                   " round-trip: " + (requestEnd - requestStart));
                System.out.println("  caught: " + caught + "  ignored: " + ignored);
                System.out.println("status round-trip: " + (statusEnd - statusStart));
            } catch (IOException e) {
                System.out.println(">>> IOException " + e);
            }
        }
        System.exit(0);
    }

}
