/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk;



/**
 * The <code>TranslatorInterface</code> is the interface by which new
 * classes can be created and loaded into the runtime system.<p>
 *
 * The runtime system (per isolate) can have at most one open connection with
 * a translator (i.e. an object that implements this interface). The
 * correct usage of a translator is described by the following state
 * transistion machine:
 * <p><blockquote><pre>
 *
 *             +----------- open() ----------+     +---------+
 *             |                             |     |         |
 *             |                             V     V         |
 *        +--------+                       +---------+       |
 *   ---> | CLOSED |                       |  OPEN   |  load() / convert()
 *        +--------+                       +---------+       |
 *             ^                             |     |         |
 *             |                             |     |         |
 *             +---------- close() ----------+     +---------+
 *
 * </pre></blockquote><p>
 *
 * That is, a translator can be {@link #open opened} and then have any
 * number of {@link #load} and {@link #convert} operations
 * performed on it before being {@link #close closed}.<p>
 *
 * @author  Doug Simon
 */
public interface TranslatorInterface {

    /**
     * Opens a connection with the translator to load & create classes in
     * the context of a given suite.
     *
     * @param  suite  the suite in which classes created during the connection
     *                with this translator will be installed.
     */
    public void open(Suite suite, String classPath);

    /**
     * Determines if a given name is a valid class name according the JVM specification.
     *
     * @param name   the class name to test
     * @return true is <code>name</code> is a valid class name
     */
    public boolean isValidClassName(String name);

    /**
     * Ensures that a given class has had its definition initialized, loading
     * it from a class file if necessary. This does not include verifying the
     * bytecodes for the methods (if any) and converting them to Squawk
     * bytecodes.
     *
     * @param   klass  the class whose definition must be initialized
     * @throws LinkageError if there were any problems while loading and linking the class
     */
    public void load(Klass klass);

    /**
     * Ensures that all the methods (if any) in a given class have been verified
     * and converted to Squawk bytecodes.
     *
     * @param   klass  the class whose methods are to be verified and converted
     * @throws LinkageError if there were any problems while converting the class
     */
    public void convert(Klass klass);

    /**
     * Closes the connection with the translator. This computes the closure
     * of the classes in the current suite and ensures they are all loaded and
     * converted.
     */
    public void close();
    
    /**
     * Get the bytes for the resource named <code>name</code>.
     * The first resource found by combining each classPath entry of the currently active suite
     * will be returned.  Meaning that <code>name</code> is relative to the root/default package.
     * 
     * @param name of the resource to fetch
     * @return byte[] null if there is no resource <code>name</code> to be found.
     */
    public byte [] getResourceData(String name);
    
}
