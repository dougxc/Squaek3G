/*
 * @(#)NoClassDefFoundError.java	1.16 99/12/04
 *
 * Copyright 1994-1999 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */

package java.lang;

import java.lang.*;

/**
 * Thrown if the Java Virtual Machine or a classloader tries to load 
 * in the definition of a class (as part of a normal method call or 
 * as part of creating a new instance using the <code>new</code> 
 * expression) and no definition of the class could be found. 
 * <p>
 * The searched-for class definition existed when the currently 
 * executing class was compiled, but the definition can no longer be 
 * found. 
 *
 * @author  unascribed
 * @version 1.16, 12/04/99
 * @since   JDK1.0
 */
public
class NoClassDefFoundError extends Error {
    /**
     * Constructs a <code>NoClassDefFoundError</code> with no detail message.
     */
    public NoClassDefFoundError() {
	super();
    }

    /**
     * Constructs a <code>NoClassDefFoundError</code> with the specified 
     * detail message. 
     *
     * @param   s   the detail message.
     */
    public NoClassDefFoundError(String s) {
	super(s);
    }
}
