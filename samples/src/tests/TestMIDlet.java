package tests;

import javax.microedition.midlet.*;

public class TestMIDlet extends MIDlet {

    protected void startApp() throws MIDletStateChangeException {
        System.out.println(this.getClass().getName() + ".startApp invoked !!!");
        ResourceTest.main(new String[0]);
    }

    protected void pauseApp() {
    }

    protected void destroyApp(boolean unconditional) throws MIDletStateChangeException {
    }

}
