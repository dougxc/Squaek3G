/**
 * 
 */
package com.sun.squawk.imp;

import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

/**
 * This class exists solely to provide a public representations of the MIDlet protected corresponding methods.
 * The MIDlet class is supposed to initialize the {@link MIDletMainWrapper#midletApiTunnel} field with
 * an implementation of this interface, which maps to the MIDlet equivalent API.
 * 
 * @author ea149956
 *
 */
public interface MIDletApiTunnelInterface {
    public void pauseApp();
    public void startApp() throws MIDletStateChangeException;
    public void destroyApp(boolean unconditional) throws MIDletStateChangeException;
    public MIDlet getMidlet();
}