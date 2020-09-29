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
 * Start up driver for the Richards Benchmarks for the Squawk platform.
 *
 * The main method of this class is called by the bootstrap to start the 
 * application.
 *
 * @author Erica Glynn
 * @version 1.0 03/02/2005 
 * Made class name unique, Cristina, Mar 2006
 */
public class StartupRichards {

	public static void main(String[] args) {
            /**
              * Richards-Gibbons benchmarks.
              */
            com.sun.labs.kanban.richards_gibbons.Richards.main(args);
	    com.sun.labs.kanban.richards_gibbons_final.Richards.main(args);
            com.sun.labs.kanban.richards_gibbons_no_switch.Richards.main(args);

            /**
              * Richards-Deutsch benchmarks.
              */
	    com.sun.labs.kanban.richards_deutsch_no_acc.Richards.main(args);
            com.sun.labs.kanban.richards_deutsch_acc_virtual.Richards.main(args);
	    com.sun.labs.kanban.richards_deutsch_acc_final.Richards.main(args);
	    com.sun.labs.kanban.richards_deutsch_acc_interface.Richards.main(args);
	    
	}
}
