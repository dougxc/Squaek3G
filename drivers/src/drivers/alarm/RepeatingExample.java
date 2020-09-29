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

public class RepeatingExample {

    public static void main(String args[]) {
        if (!JavaDriverManager.loadDriver("drivers.alarm.AlarmDriver")) {
            System.exit(1);
        }
        try {
            StreamConnection con = (StreamConnection) Connector.open("msg:////dev-alarm-request");
            DataInputStream in = con.openDataInputStream();
            DataOutputStream out = con.openDataOutputStream();
            long requestStart, requestEnd, requestTime, processTime;
            int i;

            requestStart = VM.getTimeMicros();
            out.writeInt(20000);
            out.writeInt(1000000);
            out.close();

            for (i = 0; i < 20; i++) {
                requestTime = in.readLong();
                processTime = in.readLong();
                in.reset();
                requestEnd = VM.getTimeMicros();

                System.out.println("request time: " + (requestEnd - requestStart));
                requestStart = requestEnd;
            }
            in.close();
            con.close();
        } catch (IOException e) {
            System.out.println(">>> IOException " + e);
        }
        System.exit(0);
    }

}
