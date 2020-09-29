/*           
 * Copyright 2005,2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * U.S. Government Rights - Commercial software. Government users are   
 * subject to the Sun Microsystems, Inc. standard license agreement and   
 * applicable provisions of the FAR and its supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo   
 * and Java are trademarks or registered trademarks of Sun Microsystems,   
 * Inc. in the U.S. and other countries.
 */
package squawk.application;

/*
 * 
 * Start up driver for the DeltaBlue Benchmarks for the Squawk platform.
 *
 * The main method of this class is called by the bootstrap to start the 
 * application.
 *
 * @author Erica Glynn
 * @version 1.0 03/02/2005 
 * Made class name unique, Cristina, Mar 2006
 */
public class StartupDeltaBlue {

	public static void main(String[] args) {
            com.sun.labs.kanban.DeltaBlue.DeltaBlue.main(args);	
	}
}
