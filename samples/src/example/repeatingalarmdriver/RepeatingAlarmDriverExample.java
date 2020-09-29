//if[EXCLUDE]
package example.repeatingalarmdriver;

import java.io.*;
import javax.microedition.io.*;
import com.sun.squawk.*;

public class RepeatingAlarmDriverExample {

    public static void main(String args[]) {
        if (!JavaDriverManager.loadDriver("java.lang.AlarmDriver")) {
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
