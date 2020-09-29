package hibernation;

import javax.microedition.io.*;
import java.io.*;
import com.sun.squawk.*;

public class Test1 {

    public static void main(String[] args) throws java.io.IOException {
        Hibernator.main(args);
    }
}

class Hibernator {

    private static String saveIsolate(Isolate isolate) throws IOException {
        String url = "file://" + isolate.getMainClassName() + "." + System.currentTimeMillis() + ".isolate";
        DataOutputStream dos = Connector.openDataOutputStream(url);
        isolate.save(dos, url, false);
        return url;
    }

    public static void main(String[] args) throws java.io.IOException {
        String cp = Isolate.currentIsolate().getClassPath();
        String suiteURI = Isolate.currentIsolate().getParentSuiteSourceURI();
        Isolate isolate = new Isolate("hibernation.Hibernatee", args, cp, cp == null ? suiteURI : null);

        isolate.start();
        isolate.join();

        while (isolate.isHibernated()) {
            String url = saveIsolate(isolate);
//            if (VM.isVeryVerbose()) {
//                VM.println("[loading and unhibernating " + url + "]");
//            }

            DataInputStream dis = Connector.openDataInputStream(url);
            isolate = Isolate.load(dis, url);
            dis.close();

            isolate.unhibernate();
            isolate.join();
        }
    }
}

class Hibernatee {


    public static void main(String[] args) {
        String runCountArg = args.length == 0 ? "10" : args[0];
        int runCount = Integer.parseInt(runCountArg);

        String threadCountArg = args.length < 2 ? "10" : args[1];
        int threadCount = Integer.parseInt(threadCountArg);
        Thread[] threads = new Thread[threadCount];

        final int value[] = { 0 };
        final int limit = runCount * threads.length;
        for (int i = 0; i != threads.length; ++i) {
            threads[i] = new Thread() {
                public void run() {
                    while (value[0] < limit) {
                        System.out.println(this +": " + value[0]);
                        value[0]++;
                        Thread.yield();
                    }
                }
            };
            threads[i].start();
        }

        while (runCount > 0) {
            Thread.yield();
            System.out.println("remaining runs: " + runCount + " (value=" + value[0] + ")");
            try {
                Isolate thisIsolate = Isolate.currentIsolate();
                System.out.println("Hibernating " + thisIsolate);
                thisIsolate.hibernate();
                System.out.println("Reawoke " + thisIsolate);
            }
            catch (java.io.IOException ex) {
                System.err.println("Error hibernating isolate: " + ex);
//                ex.printStackTrace();
            }
            runCount--;
        }
    }
}
