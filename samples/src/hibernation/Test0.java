package hibernation;

import java.io.*;
import javax.microedition.io.*;
import com.sun.squawk.*;

/**
 * Most basic hibernation test. Tests that a "hello world" application can hibernate and resume.
 * Must be run with -testoms in JavaApplicationManager
 *
 */
public class Test0 {

    public static void main(String[] args) throws java.io.IOException {
        System.out.println("Hello " + args[0] +" ...");
//        DataOutputStream out = Connector.openDataOutputStream("file://blah");
//        out.writeChars("before hibernation");
        Isolate.currentIsolate().hibernate();
//        out.writeChars("after hibernation");
        System.out.println("...World");
    }
}

class Test0Harness {

    public static void main(String[] args) throws java.io.IOException {
        for (int i = 0; i != 10; ++i) {
            doIt(i);
        }
    }

    private static void doIt(int i) {
        String[] args = new String[] { Integer.toString(i) };
        Isolate iso = new Isolate("hibernation.Test0", args, "samples/j2meclasses", null);
        iso.start();
        iso.join();
        System.out.println("joined Test0 instance");
    }
}
