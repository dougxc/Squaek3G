/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package java.lang;

import com.sun.squawk.*;
import com.sun.squawk.util.*;

/**
 * Instances of the class <code>Class</code> represent classes and interfaces
 * in a running Java application.  Every array also belongs to a class that is
 * reflected as a <code>Class</code> object that is shared by all arrays with
 * the same element type and number of dimensions.
 *
 * <p> <code>Class</code> has no public constructor. Instead <code>Class</code>
 * objects are constructed automatically by the Java Virtual Machine as classes
 * are loaded.
 *
 * <p> The following example uses a <code>Class</code> object to print the
 * class name of an object:
 *
 * <p> <blockquote><pre>
 *     void printClassName(Object obj) {
 *         System.out.println("The class of " + obj +
 *                            " is " + obj.getClass().getName());
 *     }
 * </pre></blockquote>
 *
 * @author  unascribed
 * @version 1.106, 12/04/99 (CLDC 1.0, Spring 2000)
 * @since   JDK1.0, CLDC 1.0
 */
public final class Class {

    private Klass klass;

    private Class() {}

    /**
     * Converts the object to a string. The string representation is the
     * string "class" or "interface", followed by a space, and then by the
     * fully qualified name of the class in the format returned by
     * <code>getName</code>.  If this <code>Class</code> object represents a
     * primitive type, this method returns the name of the primitive type.  If
     * this <code>Class</code> object represents void this method returns
     * "void".
     *
     * @return a string representation of this class object.
     */
    public String toString() {
        return klass.toString();
    }

    /**
     * Returns the <code>Class</code> object associated with the class
     * with the given string name.
     * Given the fully-qualified name for a class or interface, this
     * method attempts to locate, load and link the class.  If it
     * succeeds, returns the Class object representing the class.  If
     * it fails, the method throws a ClassNotFoundException.
     * <p>
     * For example, the following code fragment returns the runtime
     * <code>Class</code> descriptor for the class named
     * <code>java.lang.Thread</code>:
     * <ul><code>
     *   Class&nbsp;t&nbsp;= Class.forName("java.lang.Thread")
     * </code></ul>
     *
     * @param      className   the fully qualified name of the desired class.
     * @return     the <code>Class</code> descriptor for the class with the
     *             specified name.
     * @exception  ClassNotFoundException  if the class could not be found.
     * @since      JDK1.0
     */
    public static Class forName(String className) throws ClassNotFoundException {
        return Klass.asClass(Klass.forName(className));
    }


    /**
     * Creates a new instance of a class.
     *
     * @return     a newly allocated instance of the class represented by this
     *             object. This is done exactly as if by a <code>new</code>
     *             expression with an empty argument list.
     * @exception  IllegalAccessException  if the class or initializer is
     *               not accessible.
     * @exception  InstantiationException  if an application tries to
     *               instantiate an abstract class or an interface, or if the
     *               instantiation fails for some other reason.
     * @since     JDK1.0
     */
    public Object newInstance() throws InstantiationException, IllegalAccessException {
        /*
         * Check for a sensible object type.
         */
        if (klass.isSquawkArray() || klass.isInterface() || klass.isAbstract()) {
            throw new InstantiationException();
        }

        VM.StackTraceElement[] trace = VM.reifyCurrentStack(2);
        Assert.always(trace.length == 2);
        Klass callersClass = trace[1].getKlass();

        /*
         * Check that the calling method can access this klass and that there is a default constructor.
         */
        if (!klass.isAccessibleFrom(callersClass) || !klass.hasDefaultConstructor() ||
           /*
            * Note that access is not allowed to a protected constructor (even if
            * callersKlass is a subclass of this class). Protected constructors
            * can only be accessed from within the constructor of a direct
            * subclass of a class. They cannot be accessed via reflection.
            */
            !klass.isAccessibleFrom(klass, klass.getDefaultConstructorModifiers() & ~Modifier.PROTECTED, callersClass)
        ) {
            throw new IllegalAccessException();
        }

        return klass.newInstance();
    }


    /**
     * Determines if the specified <code>Object</code> is assignment-compatible
     * with the object represented by this <code>Class</code>.  This method is
     * the dynamic equivalent of the Java language <code>instanceof</code>
     * operator. The method returns <code>true</code> if the specified
     * <code>Object</code> argument is non-null and can be cast to the
     * reference type represented by this <code>Class</code> object without
     * raising a <code>ClassCastException.</code> It returns <code>false</code>
     * otherwise.
     *
     * <p> Specifically, if this <code>Class</code> object represents a
     * declared class, this method returns <code>true</code> if the specified
     * <code>Object</code> argument is an instance of the represented class (or
     * of any of its subclasses); it returns <code>false</code> otherwise. If
     * this <code>Class</code> object represents an array class, this method
     * returns <code>true</code> if the specified <code>Object</code> argument
     * can be converted to an object of the array class by an identity
     * conversion or by a widening reference conversion; it returns
     * <code>false</code> otherwise. If this <code>Class</code> object
     * represents an interface, this method returns <code>true</code> if the
     * class or any superclass of the specified <code>Object</code> argument
     * implements this interface; it returns <code>false</code> otherwise. If
     * this <code>Class</code> object represents a primitive type, this method
     * returns <code>false</code>.
     *
     * @param   obj the object to check
     * @return  true if <code>obj</code> is an instance of this class
     *
     * @since JDK1.1
     */
    public boolean isInstance(Object obj) {
        return klass.isInstance(obj);
    }


    /**
     * Determines if the class or interface represented by this
     * <code>Class</code> object is either the same as, or is a superclass or
     * superinterface of, the class or interface represented by the specified
     * <code>Class</code> parameter. It returns <code>true</code> if so;
     * otherwise it returns <code>false</code>. If this <code>Class</code>
     * object represents a primitive type, this method returns
     * <code>true</code> if the specified <code>Class</code> parameter is
     * exactly this <code>Class</code> object; otherwise it returns
     * <code>false</code>.
     *
     * <p> Specifically, this method tests whether the type represented by the
     * specified <code>Class</code> parameter can be converted to the type
     * represented by this <code>Class</code> object via an identity conversion
     * or via a widening reference conversion. See <em>The Java Language
     * Specification</em>, sections 5.1.1 and 5.1.4 , for details.
     *
     * @param cls the <code>Class</code> object to be checked
     * @return the <code>boolean</code> value indicating whether objects of the
     * type <code>cls</code> can be assigned to objects of this class
     * @exception NullPointerException if the specified Class parameter is
     *            null.
     * @since JDK1.1
     */
    public boolean isAssignableFrom(Class cls) {
        if (cls == null) {
            throw new NullPointerException();
        }
        return klass.isAssignableFrom(cls.klass);
    }

    /**
     * Determines if the specified <code>Class</code> object represents an
     * interface type.
     *
     * @return  <code>true</code> if this object represents an interface;
     *          <code>false</code> otherwise.
     */
    public boolean isInterface() {
        return klass.isInterface();
    }

    /**
     * Determines if this <code>Class</code> object represents an array class.
     *
     * @return  <code>true</code> if this object represents an array class;
     *          <code>false</code> otherwise.
     * @since   JDK1.1
     */
    public boolean isArray() {
        return klass.isArray();
    }

    /**
     * Returns the fully-qualified name of the entity (class, interface, array
     * class, primitive type, or void) represented by this <code>Class</code>
     * object, as a <code>String</code>.
     *
     * <p> If this <code>Class</code> object represents a class of arrays, then
     * the internal form of the name consists of the name of the element type
     * in Java signature format, preceded by one or more "<tt>[</tt>"
     * characters representing the depth of array nesting. Thus:
     *
     * <blockquote><pre>
     * (new Object[3]).getClass().getName()
     * </pre></blockquote>
     *
     * returns "<code>[Ljava.lang.Object;</code>" and:
     *
     * <blockquote><pre>
     * (new int[3][4][5][6][7][8][9]).getClass().getName()
     * </pre></blockquote>
     *
     * returns "<code>[[[[[[[I</code>". The encoding of element type names
     * is as follows:
     *
     * <blockquote><pre>
     * B            byte
     * C            char
     * D            double
     * F            float
     * I            int
     * J            long
     * L<i>classname;</i>  class or interface
     * S            short
     * Z            boolean
     * </pre></blockquote>
     *
     * The class or interface name <tt><i>classname</i></tt> is given in fully
     * qualified form as shown in the example above.
     *
     * @return  the fully qualified name of the class or interface
     *          represented by this object.
     */
    public String getName() {
        return klass.getName();
    }

    /**
     * Finds a resource with a given name.  This method returns null if no
     * resource with this name is found.  The rules for searching
     * resources associated with a given class are profile
     * specific.
     *
     * @param name  name of the desired resource
     * @return      a <code>java.io.InputStream</code> object.
     * @since JDK1.1
     */
    public java.io.InputStream getResourceAsStream(String name) {
        return klass.getResourceAsStream(name);
    }
}


