package example.manyballs;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.DataInputStream;
import javax.microedition.io.Connector;
import com.sun.squawk.*;

public class Main {
    public static void main(String[] args) {

        for (int i = 0; i != args.length; ++i) {
            String arg = args[i];
            int sep = arg.indexOf('=');
            if (sep != -1) {
                String key = arg.substring(0, sep);
                String value = arg.substring(sep + 1);
// TODO I think we need to look at how these should be passed, jad, manifest ???
//              System.setProperty(key, value);
// Doing it this way, System.getProperty will get the value still, provided its in the same Isolate :(
                VM.getCurrentIsolate().setProperty(key, value);
            }
        }
        mojo.Main.main(new String[] {"example.manyballs.ManyBalls"});
    }
}

class Hibernator {

    private static String saveIsolate(Isolate isolate) throws IOException {
        String url = "file://" + isolate.getMainClassName() + ".isolate";
        DataOutputStream dos = Connector.openDataOutputStream(url);
        isolate.save(dos, url, false);
        return url;
    }

    public static void main(String[] args) throws java.io.IOException {
        String cp = Isolate.currentIsolate().getClassPath();
        Isolate isolate = new Isolate("example.manyballs.Main", args, cp, null);

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
