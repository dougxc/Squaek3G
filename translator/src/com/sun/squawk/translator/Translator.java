/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM translator.
 */
package com.sun.squawk.translator;

import java.io.*;

import javax.microedition.io.Connector;
import java.util.Hashtable;
import com.sun.squawk.util.Assert;
import com.sun.squawk.SuiteCreator.*;
import com.sun.squawk.io.connections.*;
import com.sun.squawk.translator.ci.*;
import com.sun.squawk.util.ComputationTimer;
import com.sun.squawk.util.Tracer;
import com.sun.squawk.*;

/**
 * The Translator class presents functionality for loading and linking
 * classes from class files (possibly bundled in jar files) into a
 * {@link Suite}.<p>
 *
 * @author  Doug Simon
 */
public final class Translator implements TranslatorInterface {

    /**
     * Property that turns on help about other translator properties.
     */
    private final static String HELP_PROPERTY = "translator.help";

    /**
     * Set to true if the translator should use as much memory as necessary to do a best effort translation.
     *  (This used to be based on VM.isHosted().
     */
    private final static String OPTIMIZECONSTANTOBJECTS_PROPERTY = "translator.optimizeConstantObjects";
    private final static boolean OPTIMIZECONSTANTOBJECTS = true;
    private static boolean optimizeConstantObjects = OPTIMIZECONSTANTOBJECTS;


    /**
     * Set to true if the translator should not spill/fill constant operands
     */
    private final static String OPTIMIZEPARAMETERPASSING_PROPERTY = "translator.optimizeParameterPassing";
    private final static boolean OPTIMIZEPARAMETERPASSING = true;
    private static boolean optimizeParameterPassing = OPTIMIZEPARAMETERPASSING;


    /**
     * Returns true if the translator should use as much memory as necessary to do a best effort translation.
     *  (This used to be based on VM.isHosted().
     */
    public static boolean optimizeConstantObjects() {
        return optimizeConstantObjects;
    }

    /**
     * Returns true if the translator should NOT spill/fill parameters that can easily be created (such as constants)
     */
    public static boolean optimizeParameterPassing() {
        return optimizeParameterPassing;
    }

    /*---------------------------------------------------------------------------*\
     *                     Implementation of TranslatorInterface                 *
    \*---------------------------------------------------------------------------*/

    /**
     * The suite context for the currently open translator.
     */
    private Suite suite;

    /**
     * The loader used to locate and load class files.
     */
    private ClasspathConnection classPath;

    /**
     * Parses the system property named <code>name</code> as a boolean. Use <code>defaultValue</code> if
     * there is no system property by that name, or if the value is not a boolean.
     *
     * @param name  the name pf the property.
     * @param defaultValue the default value to use.
     * @return the specified property value or the default value.
     */
    private boolean getBooleanProperty(String name, boolean defaultValue) {
        String result = System.getProperty(name);

        if (result != null) {
            result = result.toLowerCase();
            if (result.equals("true")) {
                return true;
            } else if (result.equals("false")) {
                return false;
            } else {
                System.err.println("Illformed boolean value " + result + "for translator property " + name + ". Using default value " + defaultValue);
                // fall through to pick up default
            }
        }
        return defaultValue;
    }

    /**
     * Parses the system property named <code>name</code> as an int. Use <code>defaultValue</code> if
     * there is no system property by that name, or if the value is not an int.
     *
     * @param name  the name pf the property.
     * @param defaultValue the default value to use.
     * @return the specified property value or the default value.
     */
    private int getIntProperty(String name, int defaultValue) {
        String result = System.getProperty(name);

        if (result != null) {
            try {
                return Integer.parseInt(result);
            } catch (NumberFormatException e) {
                System.err.println("Illformed integer value " + result + "for translator property " + name + ". Using default value " + defaultValue);
                // fall through to pick up default
            }
        }

        return defaultValue;
    }

    /**
     * Read translator properties and set corresponding options.
     */
    private void setOptions() {
        boolean showHelp = getBooleanProperty(HELP_PROPERTY ,  false);
        optimizeConstantObjects  = getBooleanProperty(OPTIMIZECONSTANTOBJECTS_PROPERTY,  OPTIMIZECONSTANTOBJECTS);
        optimizeParameterPassing = getBooleanProperty(OPTIMIZEPARAMETERPASSING_PROPERTY, OPTIMIZEPARAMETERPASSING);

        if (showHelp || VM.isVeryVerbose()) {
            VM.println("Translator options and current values:");
            VM.println("    " + HELP_PROPERTY                     + "=" + showHelp);
            VM.println("    " + OPTIMIZECONSTANTOBJECTS_PROPERTY  + "=" + optimizeConstantObjects);
            VM.println("    " + OPTIMIZEPARAMETERPASSING_PROPERTY + "=" + optimizeParameterPassing);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void open(Suite suite, String classPath) {
        this.suite = suite;
        this.classFiles = new Hashtable();
        setOptions();
        try {
            String url = "classpath://" +  classPath;
            this.classPath = (ClasspathConnection)Connector.open(url);
        } catch (IOException ioe) {
            throwLinkageError("Error while setting class path from '"+ classPath + "': " + ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isValidClassName(String name) {
        return name.indexOf('/') == -1 && ConstantPool.isLegalName(name.replace('.', '/'), ConstantPool.ValidNameFormat.CLASS);
    }

    /**
     * {@inheritDoc}
     */
    public void load(Klass klass) {
        Assert.that(VM.isHosted() || VM.getCurrentIsolate().getLeafSuite() == suite);
        int state = klass.getState();
        if (state < Klass.STATE_LOADED) {
            if (klass.isArray()) {
                load(klass.getComponentType());
            } else {
                ClassFile classFile = getClassFile(klass);
                load(classFile);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void convert(Klass klass) {
        int state = klass.getState();
        if (state < Klass.STATE_CONVERTED) {
            if (klass.isArray()) {
                convert(Klass.OBJECT);
                klass.changeState(Klass.STATE_CONVERTED);
            } else {
                ClassFile classFile = getClassFile(klass);
                classFile.convert(this);
                classFiles.remove(klass.getName());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void close() {
        computeClosure();
    }

    /*---------------------------------------------------------------------------*\
     *                    Class lookup, creation and interning                   *
    \*---------------------------------------------------------------------------*/

    /**
     * The table of class files for classes.
     */
    private Hashtable classFiles;

    /**
     * Gets the array dimensionality indicated by a given class name.
     *
     * @return  the number of leading '['s in <code>name</code>
     */
    public static int countArrayDimensions(String name) {
        int dimensions = 0;
        while (name.charAt(dimensions) == '[') {
            dimensions++;
        }
        return dimensions;
    }

    /**
     * Gets the class file corresponding to a given instance class. The
     * <code>klass</code> must not yet be converted and it must not be a
     * {@link Klass#isSynthetic() synthetic} class.
     *
     * @param   klass  the instance class for which a class file is requested
     * @return  the class file for <code>klass</code>
     */
    ClassFile getClassFile(Klass klass) {
        Assert.that(!klass.isSynthetic(), "synthethic class has no classfile");
        String name = klass.getName();
        ClassFile classFile = (ClassFile)classFiles.get(name);
        if (classFile == null) {
            classFile = new ClassFile(klass);
            classFiles.put(name, classFile);
        }
        return classFile;
    }

    /**
     * Gets the connection that is used to find the class files.
     *
     * @return  the connection that is used to find the class files
     */
    public ClasspathConnection getClassPath() {
        return classPath;
    }

    /*---------------------------------------------------------------------------*\
     *                     Class loading and resolution                          *
    \*---------------------------------------------------------------------------*/

    /**
     * Loads a class's defintion from a class file.
     *
     * @param  classFile  the class file definition to load
     */
    private void load(final ClassFile classFile) {
        final ClassFileLoader loader = new ClassFileLoader(this);
        ComputationTimer.time("loading", new ComputationTimer.Computation() {
            public Object run() {
                loader.load(classFile);
                return null;
            }
        });
    }

    /**
     * Load and converts the closure of classes in the current suite.
     */
    public void computeClosure() {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int cno = 0 ; cno < suite.getClassCount() ; cno++) {
                Klass klass = suite.getKlass(cno);
                Assert.always(klass != null);
                try {
                    if (klass.getState() < Klass.STATE_LOADED) {
                        load(klass);
                        changed = true;
                    }
                    if (klass.getState() < Klass.STATE_CONVERTED) {
                        convert(klass);
                        changed = true;
                    }
                } catch (Error ex) {
                    if (!klass.isArray()) {
                        klass.changeState(Klass.STATE_ERROR);
                    }
                    throw ex;
                }
            }
        }
    }
    
    public byte [] getResourceData(String name) {
        Assert.that(VM.isHosted() || VM.getCurrentIsolate().getLeafSuite() == suite);
        try {
            byte[] bytes = classPath.getBytes(name);
            ResourceFile resourceFile = new ResourceFile(name, bytes);
            suite.installResource(resourceFile);
            return bytes;
        } catch (IOException e) {
            return null;
        }
    }


    /*---------------------------------------------------------------------------*\
     *                           Reversable parameters                           *
    \*---------------------------------------------------------------------------*/

    public static final boolean REVERSE_PARAMETERS = /*VAL*/true/*REVERSE_PARAMETERS*/;

    /*---------------------------------------------------------------------------*\
     *                          Throwing LinkageErrors                           *
    \*---------------------------------------------------------------------------*/

    /*
     * Using the factory methods below means that the choice between using
     * more or less specific LinkageError classes can be contained here.
     */

    /**
     * Throws an error indicating that there was a general loading problem.
     *
     * @param  msg  the detailed error message
     */
    public static void throwLinkageError(String msg) {
        throw new Error(msg);
    }

    /**
     * Throws an error representing a format error in a class file.
     *
     * @param   msg  the detailed error message
     */
    public static void throwClassFormatError(String msg) {
//        throw new ClassFormatError(msg);
        throw new Error("ClassFormatError: " + msg);
    }

    /**
     * Throws an error indicating a class circularity error.
     *
     * @param  msg  the detailed error message
     */
    public static void throwClassCircularityError(String msg) {
//        throw new ClassCircularityError(msg);
        throw new Error("ClassCircularityError: " + msg);
    }

    /**
     * Throws an error indicating a class file with an unsupported class
     * was found.
     *
     * @param  msg  the detailed error message
     */
    public static void throwUnsupportedClassVersionError(String msg) {
//        throw new UnsupportedClassVersionError(msg);
        throw new Error("UnsupportedClassVersionError: " + msg);
    }

    /**
     * Throws an error indicating an incompatible class change has occurred
     * to some class definition.
     *
     * @param  msg  the detailed error message
     */
    public static void throwIncompatibleClassChangeError(String msg) {
//        throw new IncompatibleClassChangeError(msg);
        throw new Error("IncompatibleClassChangeError: " + msg);
    }

    /**
     * Throws an error indicating an application tried to use the Java new
     * construct to instantiate an abstract class or an interface.
     *
     * @param  msg  the detailed error message
     */
    public static void throwInstantiationError(String msg) {
//        throw new InstantiationError(msg);
        throw new Error("InstantiationError: " + msg);
    }

    /**
     * Throws an error indicating a non-abstract class does not implement
     * all its inherited abstract methods.
     *
     * @param  msg  the detailed error message
     */
    public static void throwAbstractMethodError(String msg) {
//        throw new AbstractMethodError(msg);
        throw new Error("AbstractMethodError: " + msg);
    }

    /**
     * Throws an error indicating a class attempts to access or modify a field,
     * or to call a method that it does not have access.
     *
     * @param  msg  the detailed error message
     */
    public static void throwIllegalAccessError(String msg) {
//        throw new IllegalAccessError(msg);
        throw new Error("IllegalAccessError: " + msg);
    }

    /**
     * Throws an error indicating an application tries to access or modify a
     * specified field of an object, and that object no longer has that field.
     *
     * @param  msg  the detailed error message
     */
    public static void throwNoSuchFieldError(String msg) {
//        throw new NoSuchFieldError(msg);
        throw new Error("NoSuchFieldError: " + msg);
    }

    /**
     * Throws an error indicating an application tries to call a specified
     * method of a class (either static or instance), and that class no
     * longer has a definition of that method.
     *
     * @param  msg  the detailed error message
     */
    public static void throwNoSuchMethodError(String msg) {
//        throw new NoSuchMethodError(msg);
        throw new Error("NoSuchMethodError: " + msg);
    }

    /**
     * Throws an error indicating the translator tried to load in the
     * definition of a class and no definition of the class could be found.
     *
     * @param  msg  the detailed error message
     */
    public static void throwNoClassDefFoundError(String msg) {
        throw new NoClassDefFoundError(msg);
    }

    /**
     * Throws an error indicating that the verifier has detected that a class
     * file, though well formed, contains some sort of internal inconsistency
     * or security problem.
     *
     * @param  msg  the detailed error message
     */
    public static void throwVerifyError(String msg) {
//        throw new VerifyError(msg);
        throw new Error("VerifyError: " + msg);
    }


    /*---------------------------------------------------------------------------*\
     *                          Debugging                                         *
    \*---------------------------------------------------------------------------*/

/*if[TRACING_ENABLED]*/
    public static void trace(MethodBody mb) {
        if (Klass.TRACING_ENABLED ) {
            Method method = mb.getDefiningMethod();
            Tracer.traceln("++++ Method for " + method + " ++++");
            new MethodBodyTracer(mb).traceAll();
            Tracer.traceln("---- Method for " + method + " ----");
        }
    }
/*end[TRACING_ENABLED]*/

}
