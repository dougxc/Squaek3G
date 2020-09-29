package com.sun.squawk.imp;

import javax.microedition.midlet.*;

import com.sun.squawk.*;
import com.sun.squawk.util.*;

/**
 * 
 *
 * @author Eric Arseneau
 *
 */
public class MIDletMainWrapper {
    
    /**
     * Place holder for the MIDletApiTunnel that is created whenever a MIDlet is created.
     */
    public static MIDletApiTunnelInterface midletApiTunnel = null;
    
    /**
     * Return a new MIDletApiWrapper that provides a publc interface to necessary lifecycle methods found on
     * midletClass specified.
     * 
     * @param midletClass
     * @return
     * @throws Exception
     */
    public static synchronized MIDletApiTunnelInterface newMIDletApiTunnel(Klass midletClass) throws Exception {
        // Creating a new instance will set my midletTunnel static member, can fetch it from there when needed
        midletClass.newInstance();
        Assert.always(midletApiTunnel != null, "midletTunnel should have been set by MIDlet constructor");
        return midletApiTunnel;
    }
    
    public static MIDletApiTunnelInterface newMIDletApiTunnel(String className) throws Exception {
        Klass klass;
        try {
            klass = Klass.forName(className);
        } catch (ClassNotFoundException e) {
            throw Assert.shouldNotReachHere("MIDlet class specified was not found");
        }
        Assert.always(MIDlet.class.isAssignableFrom(Klass.asClass(klass)), "Specified class must be subclass of javax.microedition.MIDlet");
        MIDletApiTunnelInterface midletTunnel = newMIDletApiTunnel(klass);
        return midletTunnel;
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        Assert.always(args.length == 1, "Expected to find MIDlet property name as argument nothing less or more");
        String midletPropertyName = args[0];
        String midletDescription = VM.getManifestProperty(midletPropertyName);
        if (midletDescription == null) {
            throw new IllegalArgumentException(midletPropertyName + " property must exist in " + Suite.PROPERTIES_MANIFEST_RESOURCE_NAME);
        }
        int index = midletDescription.lastIndexOf(',');
        if (index == -1) {
        	throw new IllegalArgumentException("Found property "+midletPropertyName+" not containing proper information [label, icon, class name]: " + midletDescription);
        }
        String className = midletDescription.substring(index + 1).trim(); 
        Assert.always(className != null, "Expecting MIDlet class to be specified");
        Klass klass;
        try {
            klass = Klass.forName(className);
        } catch (ClassNotFoundException e) {
            throw Assert.shouldNotReachHere("MIDlet class \"" + className + "\" was not found");
        }
        Assert.always(MIDlet.class.isAssignableFrom(Klass.asClass(klass)), "\"" + className + "\" must be subclass of javax.microedition.MIDlet");
        MIDletApiTunnelInterface midletTunnel;
        try {
            midletTunnel = newMIDletApiTunnel(klass);
        } catch (Exception e) {
            System.err.println("Problems instantiating the MIDletApiTunnel");
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
        while (true) {
            try {
                midletTunnel.startApp();
                break;
            } catch (MIDletStateChangeException e) {
                // Handle the transient oriented exceptions and try again in a while
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e1) {
                }
            } catch (Throwable t) {
                try {
                    t.printStackTrace();
                    midletTunnel.destroyApp(true);
                } catch (MIDletStateChangeException e) {
                    // We should ignore since we requested an unconditional exit
                }
                break;
            }
        }
    }

}
