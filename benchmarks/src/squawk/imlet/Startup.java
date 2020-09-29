/*
 * Copyright 2005,2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package squawk.imlet;

import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

/*
 * 
 * Start up driver for the DeltaBlue Benchmarks for the Squawk platform.
 *
 * The main method of this class is called by the bootstrap to start the application.
 *
 * @author Erica Glynn
 * @version 1.0 03/02/2005 
 * Upgraded to use MIDlet profile, Cristina, Mar 2006
 */
public class Startup extends MIDlet {

        protected void startApp() throws MIDletStateChangeException {
            String[] args = {""};
            squawk.application.StartupRichards.main(args);
            squawk.application.StartupDeltaBlue.main(args);
            squawk.application.StartupLife.main(args);
            squawk.application.StartupMath.main(args);
        }

        protected void pauseApp() {
            // This will never be called by the Squawk VM
        }

        protected void destroyApp(boolean arg0) throws MIDletStateChangeException {
            // Only called if startApp throws any exception other than MIDletStateChangeException
        }
}
