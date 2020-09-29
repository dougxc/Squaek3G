/*
 * @(#)MIDlet.java	1.33 01/05/30
 *
 * Copyright 1998-2000 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Sun Microsystems, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Sun.
 */

package javax.microedition.midlet;

import com.sun.squawk.Suite;
import com.sun.squawk.VM;
import com.sun.squawk.imp.MIDletApiTunnelInterface;
import com.sun.squawk.imp.MIDletMainWrapper;

/**
 * A <code>MIDLet</code> is a MID Profile application.
 * The application must extend this class to allow the
 * application management software to control the MIDlet and to be
 * able to retrieve properties from the application descriptor
 * and notify and request state changes.
 * The methods of this class allow the application management
 * software to create,
 * start, pause, and destroy a MIDlet.
 * A <code>MIDlet</code> is a set of classes designed to be run and
 * controlled by the application management software via this interface.
 * The states allow the application management software to manage
 * the activities of multiple <CODE>MIDlets</CODE> within
 * a runtime environment.
 * It can select which <code>MIDlet</code>s are active at a given time
 * by starting and pausing them individually.
 * The application management software maintains the state of the
 * <code>MIDlet</code> and
 * invokes methods on the <code>MIDlet</code> to change states. 
 * The <code>MIDlet</code>
 * implements these methods to update its internal activities and
 * resource usage as directed by the application management software.
 * The <code>MIDlet</code> can initiate some state changes itself
 * and notifies
 * the application management software of those state changes
 * by invoking the appropriate methods.<p>
 *
 * <b>Note:</b> The methods on this interface signal state
 * changes. The state change is not considered complete until the state
 * change method has returned. It is intended that these methods return
 * quickly.<p>
 */
public abstract class MIDlet {
    
    private class MIDletApiTunnel implements MIDletApiTunnelInterface {

        public void pauseApp() {
            MIDlet.this.pauseApp();
        }

        public void startApp() throws MIDletStateChangeException {
            MIDlet.this.startApp();
        }

        public void destroyApp(boolean unconditional) throws MIDletStateChangeException {
            MIDlet.this.destroyApp(unconditional);
        }
        
        public MIDlet getMidlet() {
            return MIDlet.this;
        }
    }

    /**
     * Protected constructor for subclasses.
     */
    protected MIDlet() {
        MIDletMainWrapper.midletApiTunnel = new MIDletApiTunnel();
    }

    /**
     * Signals the <code>MIDlet</code> that it has entered the
     * <em>Active</em> state.
     * In the <em>Active</EM> state the <code>MIDlet</code> may
     * hold resources.
     * The method will only be called when
     * the <code>MIDlet</code> is in the <em>Paused</em> state.
     * <p>
     * Two kinds of failures can prevent the service from starting,
     * transient and non-transient.  For transient failures the
     * <code>MIDletStateChangeException</code> exception should be thrown.
     * For non-transient failures the <code>notifyDestroyed</code>
     * method should be called.
     * <p>
     * If a Runtime exception occurs during <code>startApp</code> the
     * MIDlet will be
     * destroyed immediately.  Its <code>destroyApp</code> will be
     * called allowing
     * the MIDlet to cleanup.
     *
     * @exception <code>MIDletStateChangeException</code>  is thrown
     * if the <code>MIDlet</code>
     *		cannot start now but might be able to start at a
     *		later time.
     */
    protected abstract void startApp() throws MIDletStateChangeException;

    /**
     *
     * Signals the <code>MIDlet</code> to stop and enter the
     * <em>Paused</em> state.
     * In the <em>Paused</em> state the <code>MIDlet</code> must
     * release shared
     * resources
     * and become quiescent. This method will only be called
     * called when the <code>MIDlet</code> is in the <em>Active</em>
     * state. <p>
     * <p>
     * If a Runtime exception occurs during <code>pauseApp</code> the
     * MIDlet will be
     * destroyed immediately.  Its <code>destroyApp</code> will be
     * called allowing
     * the MIDlet to cleanup.
     */
    protected abstract void pauseApp();


    /**
     * Signals the <code>MIDlet</code> to terminate and enter the
     * <em>Destroyed</em> state.
     * In the destroyed state the <code>MIDlet</code> must release
     * all resources and save any persistent state. This method may
     * be called from the <em>Paused</em> or
     * <em>Active</em> states. <p>
     * <code>MIDlet</code>s should
     * perform any operations required before being terminated, such as
     * releasing resources or saving preferences or
     * state. <p>
     *
     * <b>NOTE:</b> The <code>MIDlet</code> can request that it not
     * enter the <em>Destroyed</em>
     * state by throwing an <code>MIDletStateChangeException</code>. This
     * is only a valid response if the <code>unconditional</code>
     * flag is set to <code>false</code>. If it is <code>true</code>
     * the <code>MIDlet</code> is assumed to be in the <em>Destroyed</em> state
     * regardless of how this method terminates. If it is not an
     * unconditional request, the <code>MIDlet</code> can signify
     * that it wishes
     * to stay in its current state by throwing the 
     * <code>MIDletStateChangeException</code>.
     * This request may be honored and the <code>destroy()</code>
     * method called again at a later time.
     *
     * <p>If a Runtime exception occurs during <code>destroyApp</code> then
     * they are ignored and the MIDlet is put into the <em>Destroyed</em> state.
     *
     * @param unconditional If true when this method is called,
     * the <code>MIDlet</code> must cleanup and release all resources.
     * If false the <code>MIDlet</code> may throw 
     * <CODE>MIDletStateChangeException</CODE>
     * to indicate it does not want to be destroyed at this time.
     *
     * @exception <code>MIDletStateChangeException</code> is thrown
     * if the <code>MIDlet</code>
     *		wishes to continue to execute (Not enter the <em>Destroyed</em>
     *          state).
     *          This exception is ignored if <code>unconditional</code>
     *          is equal to <code>true</code>.
     */
    protected abstract void destroyApp(boolean unconditional)
	throws MIDletStateChangeException;


    /**
     *
     * Used by an <code>MIDlet</code> to notify the application
     * management software that it has entered into the
     * <em>Destroyed</em> state.  The application management software will not
     * call the MIDlet's <code>destroyApp</code> method, and all resources
     * held by the <code>MIDlet</code> will be considered eligible
     * for reclamation.
     * The <code>MIDlet</code> must have performed the same operations
     * (clean up, releasing of resources etc.) it would have if the
     * <code>MIDlet.destroyApp()</code> had been called.
     *
     */
    public final void notifyDestroyed() {
        System.exit(0);
    }

    /**
     * Notifies the application management software that the MIDlet
     * does not want to be active and has
     * entered the <em>Paused</em> state.  Invoking this method will
     * have no effect if the <code>MIDlet</code> is destroyed, or if it has not
     * yet been started. <p>
     * It may be invoked by the <code>MIDlet</code> when it is in the
     * <em>Active</em> state. <p>
     *
     * If a <code>MIDlet</code> calls <code>notifyPaused()</code>, in the
     * future its <code>startApp()</code> method may be called make
     * it active again, or its <code>destroyApp()</code> method may be
     * called to request it to destroy itself.
     */
    public final void notifyPaused() {
    }

    /**
     * Provides a <code>MIDlet</code> with a mechanism to retrieve named
     * properties from the application management software.
     * The properties are retrieved from the combination of 
     * the application descriptor file and the manifest.
     * If an attributes in the descriptor has the same name
     * as an attribute in the manifest the value from the
     * descriptor is used and the value from the manifest
     * is ignored.
     *
     * @param key the name of the property
     * @return A string with the value of the property.
     * 		<code>null</code> is returned if no value is
     *          available for the key.
     * @exception <code>NullPointerException</code> is thrown
     * if key is <code>null</code>.
     */
    public final String getAppProperty(String key) {
        return VM.getManifestProperty(key);
    }

    /**
     * Provides a <code>MIDlet</code> with a mechanism to indicate that it is
     * interested in entering the <em>Active</em> state. Calls to
     * this method can be used by the application management software
     * to determine which
     * applications to move to the <em>Active</em> state.
     * <p>
     * When the application management software decides to activate this  
     * application it will call the <code>startApp</code> method.
     * <p> The application is generally in the <em>Paused</em> state
     * when this is
     * called.  Even in the paused state the application may handle
     * asynchronous events such as timers or callbacks.
     */
    public final void resumeRequest() {
    }

}
