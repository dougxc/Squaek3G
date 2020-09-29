/*MAKE_ASSERTIONS_FATAL[true]*/
/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk;

import java.io.*;
import java.util.*;
import javax.microedition.io.*;

import com.sun.squawk.io.MulticastOutputStream;
import com.sun.squawk.io.mailboxes.Mailbox;
import com.sun.squawk.io.mailboxes.MailboxAddress;
import com.sun.squawk.pragma.*;
import com.sun.squawk.util.*;
import com.sun.squawk.util.SquawkVector;
import com.sun.squawk.util.SquawkHashtable;
import com.sun.squawk.vm.*;

/**
 * The Squawk implementation of isolates.
 *
 * @author Nik Shaylor, Doug Simon
 */
public final class Isolate implements Runnable {

    /**
     * Constant denoting that an isolate has been created but not yet {@link #start() started}.
     */
    private final static int NEW = 0;

    /**
     * Constant denoting that an isolate has been {@link #start() started} and the
     * {@link #run()} method has been called on its initial thread.
     */
    private final static int ALIVE = 1;

    /**
     * Constant denoting that an isolate has been {@link #hibernate() hibernated}.
     */
    private final static int HIBERNATED = 2;

    /**
     * Constant denoting that an isolate has been {@link #exit exited}.
     */
    private final static int EXITED = 3;

    /**
     * The debugger agent under which this isolate is being debugged by (if any).
     */
    private Debugger debugger;

    /**
     * The system wide unique identifier for this isolate.
     */
    private final int id;

    /**
     * The name of the class to be executed.
     */
    private final String mainClassName;

    /**
     * The command line arguments for the class to be executed.
     */
    private final String[] args;

    /**
     * This is the starting point when doing class look up. It is also
     * the suite into which any dynamically loaded classes are installed if it
     * is not {@link Suite#isClosed closed}.
     */
    private Suite leafSuite;

    /**
     * The immutable bootstrap that is shared across all isolates.
     */
    private final Suite bootstrapSuite;

    /**
     * The child threads of the isolate.
     */
    private SquawkHashtable childThreads = new SquawkHashtable();

    /**
     * The parent isolate that created and started this isolate.
     */
    private Isolate parentIsolate;

    /**
     * The child isolates of the isolate.
     */
    private SquawkHashtable childIsolates;

    /**
     * Flag to show that class Klass has been initialized.
     */
    private boolean classKlassInitialized;

    /**
     * The current state of the isolate.
     */
    private int state;

    /**
     * Isolate exit code.
     */
    private int exitCode;

    /**
     * The source URI of the direct parent suite of the leaf suite.
     */
    private final String parentSuiteSourceURI;

    /**
     * The path where class files and suite files can be found.
     */
    private final String classPath;

    /**
     * The channel I/O handle.
     */
    private int channelContext;

    /**
     * The hibernated channel context.
     */
    private byte[] hibernatedChannelContext;

    /**
     * The GUI input channel.
     */
    private int guiIn;

    /**
     * The GUI output channel.
     */
    private int guiOut;

    /**
     * SquawkHashtable that holds the monitors for objects in ROM.
     */
    private SquawkHashtable monitorHashtable = new SquawkHashtable();

    /**
     * The translator that is to be used to locate, load and convert classes
     * that are not currently installed in the system.
     */
    private TranslatorInterface translator;

    /**
     * The class of the translator that is to be used to locate, load and convert classes
     * that are not currently installed in the system.
     */
    private Klass translatorClass;

    /**
     * Pointer to first class state record. These are the structures that store the static field values
     * and initialization state of a class.
     */
    private Object classStateQueue;

    /**
     * The interned strings for the isolate.
     */
    private SquawkHashtable internedStrings;

    /**
     * List of threads ready to run after return from hibernated state.
     */
    private VMThread hibernatedRunThreads;

    /**
     * List of threads to be placed on the timer queue after return from hibernated state.
     */
    private VMThread hibernatedTimerThreads;

    /**
     * List of stack chunks in an isolate {@link #save saved} to a stream. This is
     * only used by a generational collector that needs to track the thread stacks
     * in the system.
     */
    private Object savedStackChunks;

    /**
     * List of threads waiting for the isolate to exit or hibernate.
     */
    private VMThread joiners;

    /**
     * Properties that can be set by the owner of the isolate.
     */
    private SquawkHashtable properties;

/*if[FINALIZATION]*/
    /**
     * List of finalizers that need to be run.
     */
    private Finalizer finalizers;
/*end[FINALIZATION]*/

    /**
     * Table of registered & anonymous mailboxes owned by this isolate.
     * This is a table of all inward links to this isolate.
     */
    private SquawkHashtable mailboxes;
    
        
    /**
     * Table of all MailboxAddresses that this Isolate uses to refer to other Isolates.
     * This is a table of all outward links.
     * (Note that an isolate mighte use mailboxes internally, so some mailboxes
     * refered to by a MailboxAddress may in fact be local to the isolate.)
     */
    private SquawkHashtable mailboxAddresses;
    

    /**
     * Creates the root isolate.
     *
     * @param mainClassName  the name of the class with bootstrap main()
     * @param args           the command line arguments
     * @param suite          the initial leaf suite
     */
    Isolate(String mainClassName, String[] args,  Suite suite) {
        this.mainClassName        = mainClassName;
        this.args                 = args;
        this.leafSuite            = suite;
        this.classPath            = null;
        this.parentSuiteSourceURI = null;
        this.state                = NEW;
        this.id                   = VM.allocateIsolateID();

        while (suite.getParent() != null) {
            suite = suite.getParent();
        }
        this.bootstrapSuite = suite;

        if (!VM.isHosted()) {
            VM.registerIsolate(this);
        }
        Assert.always(VM.getCurrentIsolate() == null);
    }

    /**
     * Creates an new isolate.
     *
     * @param mainClassName the name of the class with main()
     * @param args          the command line arguments
     * @param classPath     the path where classes and suites can be found
     * @param parentSuiteSourceURI
     *
     * @throws NullPointerException if <code>mainClassName</code> or <code>args</code> is <code>null</code>
     */
    public Isolate(String mainClassName, String[] args,  String classPath, String parentSuiteSourceURI) {
        if (mainClassName == null || args == null) {
            throw new NullPointerException();
        }
        this.mainClassName        = mainClassName;
        this.args                 = args;
        this.classPath            = classPath;
        this.parentSuiteSourceURI = parentSuiteSourceURI;
        this.state                = NEW;
        this.id                   = VM.allocateIsolateID();

        Isolate currentIsolate = VM.getCurrentIsolate();
        Assert.that(currentIsolate != null);
        currentIsolate.addIsolate(this);
        bootstrapSuite = parentIsolate.bootstrapSuite;

        // Initialize the leafSuite to be the bootstrap suite for now in case it
        // is required sometime between now and 'updateLeafSuite'.
        leafSuite = bootstrapSuite;

        if (!VM.isHosted()) {
            VM.registerIsolate(this);
        }
    }

    /**
     * Determines if the current thread is not owned by this isolate.
     *
     * @return true if the current thread is not owned by this isolate
     */
    private boolean isCurrentThreadExternal() {
        if (!VM.isHosted()) {
            VMThread currentThread = VMThread.currentThread();
            if (currentThread != null && currentThread.getThreadNumber() != 0 && currentThread.getIsolate() != this) {
                return true;
            }
        }
        return false;
    }

    /**
     * Makes a copy of a given string if it is not null and the current thread is not owned by this isolate.
     *
     * @param s   the string to conditionally copy
     * @return the original or copy of <code>s</code>
     */
    private String copyIfCurrentThreadIsExternal(String s) {
        if (s != null && isCurrentThreadExternal()) {
            return new String(s);
        } else {
            return s;
        }
    }

    /**
     * Makes a copy of a given string array if it is not null and the current thread is not owned by this isolate.
     *
     * @param arr   the string array to conditionally copy
     * @return the original or copy of <code>arr</code>
     */
    private String[] copyIfCurrentThreadIsExternal(String[] arr) {
        if (arr != null && isCurrentThreadExternal()) {
            String[] result = new String[arr.length];
            for (int i = 0; i != arr.length; ++i) {
                String s = arr[i];
                result[i] = s == null ? s : new String(s);
            }
            return result;
        }
        return arr;
    }

    /**
     * Gets the class path for the isolate.
     *
     * @return the class path
     */
    public String getClassPath() {
        return copyIfCurrentThreadIsExternal(classPath);
    }

    /**
     * Returns an array of active Isolate objects. The array contains one entry for each isolate
     * in the invoker's aggregate that has been started but has not yet terminated. New isolates
     * may have been constructed or existing ones terminated by the time method returns.
     *
     * @return the active Isolate objects present at the time of the call
     */
    public static Isolate[] getIsolates() {
        SquawkVector set = new SquawkVector();
        VM.copyIsolatesInto(set);
        Isolate[] isolates = new Isolate[set.size()];
        set.copyInto(isolates);
        return isolates;
    }

    /**
     * @return the URI of suite from which this isolate was started. This
     *         value will be "memory:bootstrap" if the isolate was not given
     *         an explicit suite at creation time.
     */
    public String getParentSuiteSourceURI() {
        return parentSuiteSourceURI == null ? ObjectMemory.BOOTSTRAP_URI : parentSuiteSourceURI;
    }

    /**
     * Get the name of the main class.
     *
     * @return the name
     *
     * @vm2c code( return com_sun_squawk_Isolate_mainClassName(this); )
     */
    public String getMainClassName() {
        return copyIfCurrentThreadIsExternal(mainClassName);
    }

    /**
     * Gets the current isolate context.
     *
     * @return the current Isolate context.
     */
    public static Isolate currentIsolate() {
        return VM.getCurrentIsolate();
    }

    /**
     * Determines if this isolate can access trusted classes. A trusted class will call this
     * method in its static constructor.
     *
     * @return boolean
     */
    public boolean isTrusted() {
        // TODO: put authentication infrastructure in place
        return true;
    }

    /**
     * Get the arguments.
     *
     * @return the arguments
     */
    public String[] getMainClassArguments() {
        return copyIfCurrentThreadIsExternal(args);
    }

    /**
     * Gets the bootstrap suite.
     *
     * @return the bootstrap suite
     */
    Suite getBootstrapSuite() {
        return bootstrapSuite;
    }

    /**
     * Gets the suite that is the starting point for class lookup in this isolate.
     * If it is not {@link Suite#isClosed closed}, then it's also the suite into which
     * any dynamically loaded classes (i.e. those loaded via {@link Class#forName(String)})
     * are installed.
     *
     * @return the leaf suite
     */
    public Suite getLeafSuite() {
        return leafSuite;
    }

    /**
     * Gets the monitor hash table for the isolate
     *
     * @return the hash table
     */
    SquawkHashtable getMonitorHashtable() {
        return monitorHashtable;
    }

    /**
     * Sets the translator.
     *
     * @param translator the translator.
     */
    void setTranslator(TranslatorInterface translator) throws HostedPragma {
        this.translator = translator;
    }

    /**
     * Sets the translator class.
     *
     * @param translatorClass the class of the translator.
     */
    void setTranslatorClass(Klass translatorClass) {
        Assert.always(!VM.isHosted());
        if (GC.inRam(translatorClass)) {
            throw new IllegalArgumentException("translator class must be in read-only memory");
        }
        this.translatorClass = translatorClass;
    }

    /**
     * Gets a translator that is to be used to locate, load and convert
     * classes that are not currently installed in this isolate's runtime
     * environment.
     *
     * @return  a translator for installing new classes or null if the system does not support dynamic class loading
     */
    public TranslatorInterface getTranslator() throws IllegalThreadStateException {
        if (VM.isHosted()) {
            return translator;
        }

        /*
         * Create the translator instance reflectively. This (compile and runtime) dynamic
         * binding to the translator means that it can be an optional component.
         */
        try {
            Klass klass = translatorClass != null ? translatorClass : leafSuite.lookup("com.sun.squawk.translator.Translator");
            if (klass == null) {
                return null;
            }
            return (TranslatorInterface)klass.newInstance();
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
        } catch (InstantiationException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Adds a named property to this isolate. These properties are included in the
     * look up performed by {@link System#getProperty(String)}.
     *
     * @param key    the name of the property
     * @param value  the value of the property
     */
    public void setProperty(String key, String value) {
        if (properties == null) {
            properties = new SquawkHashtable();
        }

        key = copyIfCurrentThreadIsExternal(key);
        value = copyIfCurrentThreadIsExternal(value);
        properties.put(key, value);
    }

    /**
     * Gets a named property of this isolate.
     *
     * @param key  the name of the property to get
     * @return the value of the property named by 'key' or null if there is no such property
     */
    public String getProperty(String key) {
        if (properties == null) {
            return null;
        }

        return copyIfCurrentThreadIsExternal((String)properties.get(key));
    }

    /*---------------------------------------------------------------------------*\
     *                           Class state management                          *
    \*---------------------------------------------------------------------------*/

    /**
     * Get a class state.
     *
     * @param klass the class of the variable
     * @return the class state object or null if none exists
     */
    Object getClassState(Klass klass) {
        VM.extendsEnabled = false;
        Object first = classStateQueue;
        Object res = null;

        if (first != null) {
            /*
             * Do quick test for class state at the head of the queue.
             */
            if (NativeUnsafe.getObject(first, CS.klass) == klass) {
                res = first;
            } else {
                /*
                 * Start searching.
                 */
                Object last = first;
                Object ks = NativeUnsafe.getObject(first, CS.next);
                while (ks != null) {
                    if (NativeUnsafe.getObject(ks, CS.klass) == klass) {
                        /*
                         * Move to head of queue.
                         */
                        if (last != null) {
                            Object ksnext = NativeUnsafe.getObject(ks, CS.next);
                            NativeUnsafe.setObject(last, CS.next, ksnext);
                            NativeUnsafe.setObject(ks, CS.next, first);
//VM.extendsEnabled = true;
//VM.print("moved class state for ");
//VM.print(Klass.getInternalName(klass));
//VM.print(" to head of queue -> ");
//VM.printAddress(ks);
//VM.print("    bcount=");
//VM.println(VM.branchCount());
//VM.extendsEnabled = false;
                            classStateQueue = ks;
                        }
                        res = ks;
                        break;
                    }
                    last = ks;
                    ks = NativeUnsafe.getObject(ks, CS.next);
                }
            }
        }
        VM.extendsEnabled = true;
        VM.addToClassStateCache(klass, res);

        return res;
    }

    /**
     * Add a class state to the system.
     *
     * @param ks the class state to add
     */
    void addClassState(Object ks) {
        VM.extendsEnabled = false;
        Object first = classStateQueue;
        NativeUnsafe.setObject(ks, CS.next, first);
        classStateQueue = ks;
        VM.extendsEnabled = true;
    }

    /**
     * Get a class state in order to access a static variable.
     *
     * @param klass the class of the variable
     * @param offset the offset to the variable
     * @return the class state object
     */
    Object getClassStateForStaticVariableAccess(Klass klass, int offset) {
        /*
         * Lookup the class state in the isolate.
         */
        Object ks = getClassState(klass);

        /*
         * If the class state was not found in the list, then the class
         * is either not initialized, has suffered an initialization
         * failure, or is in the process of being initialized. In either
         * case calling initializeInternal() will either yield a pointer to the
         * class state object or result in an exception being thrown.
         */
        if (ks == null) {
            Assert.that(klass.getSystemID() != CID.KLASS);
            ks = klass.initializeInternal();
        }

        Assert.that(ks != null);
        Assert.that(offset >= CS.firstVariable);
        Assert.that(offset < GC.getArrayLength(ks));
        return ks;
    }


    /*---------------------------------------------------------------------------*\
     *                           String interning                                *
    \*---------------------------------------------------------------------------*/

    /**
     * Returns a canonical representation for the string object.
     * <p>
     * A pool of strings, initially empty, is maintained privately by the
     * class <code>Isolate</code>.
     * <p>
     * When the intern method is invoked, if the pool already contains a
     * string equal to this <code>String</code> object as determined by
     * the {@link #equals(Object)} method, then the string from the pool is
     * returned. Otherwise, this <code>String</code> object is added to the
     * pool and a reference to this <code>String</code> object is returned.
     * <p>
     * It follows that for any two strings <code>s</code> and <code>t</code>,
     * <code>s.intern() == t.intern()</code> is <code>true</code>
     * if and only if <code>s.equals(t)</code> is <code>true</code>.
     * <p>
     * All literal strings and string-valued constant expressions are
     * interned. String literals are defined in &sect;3.10.5 of the
     * <a href="http://java.sun.com/docs/books/jls/html/">Java Language
     * Specification</a>
     *
     * @return  a string that has the same contents as this string, but is
     *          guaranteed to be from a pool of unique strings.
     */
    public String intern(String value) {
        if (internedStrings == null) {
            internedStrings = new SquawkHashtable();
            if (!VM.isHosted()) {
                GC.getStrings(internedStrings);
            }
        }
        String internedString = (String)internedStrings.get(value);
        if (internedString == null) {
            if (internedString == null) {
                internedStrings.put(value, value);
                internedString = value;
            }
        }
        return internedString;
    }


    /*---------------------------------------------------------------------------*\
     *                            Isolate Execution                              *
    \*---------------------------------------------------------------------------*/

    /**
     * Start the isolate.
     */
    void primitiveThreadStart() {
        VMThread.asVMThread(new Thread(this)).primitiveThreadStart();
    }

    /**
     * Start the isolate.
     */
    public void start() {
        Thread t = new Thread(this, mainClassName + " - main");
        t.start();
    }

    /**
     * Manually initialize com.sun.squawk.Klass.
     */
    void initializeClassKlass() {
        if (!classKlassInitialized) {
            Klass klassKlass       = bootstrapSuite.getKlass(CID.KLASS);
            Klass klassGlobalArray = bootstrapSuite.getKlass(CID.GLOBAL_ARRAY);
            Object cs = GC.newClassState(klassKlass, klassGlobalArray);
            addClassState(cs);
            klassKlass.clinit();
            classKlassInitialized = true;
//VM.print("Klass initialized for ");
//VM.println(this);
        }
    }

    /**
     * Get the Channel I/O handle.
     *
     * @return the I/O handle
     */
    int getChannelContext() {
        if (channelContext == 0) {
            channelContext = VM.createChannelContext(hibernatedChannelContext);
            hibernatedChannelContext = null;
        }
        return channelContext;
    }

    /**
     * Get the GUI input channel.
     *
     * @return the I/O handle
     */
    int getGuiInputChannel() throws IOException {
        if (guiIn == 0) {
            guiIn = VM.getChannel(ChannelConstants.CHANNEL_GUIIN);
        }
        return guiIn;
    }

    /**
     * Get the GUI output channel.
     *
     * @return the I/O handle
     */
    int getGuiOutputChannel() throws IOException {
        if (guiOut == 0) {
            guiOut = VM.getChannel(ChannelConstants.CHANNEL_GUIOUT);
            VM.execGraphicsIO(ChannelConstants.SETWINDOWNAME, 0, 0, 0, 0, 0, 0, mainClassName, null);
        }
        return guiOut;
    }

    /**
     * Updates the leaf suite if this isolate was initialized with a non-null
     * parent suite URI or class path.
     */
    private void updateLeafSuite() {
        Assert.that(VM.getCurrentIsolate() == this);
        Assert.always(leafSuite == bootstrapSuite);
        if (parentSuiteSourceURI != null || classPath != null) {

            Suite parent = (parentSuiteSourceURI == null ? bootstrapSuite : Suite.getSuite(parentSuiteSourceURI));
            Assert.that(parent != null);

            String leafSuiteName = getProperty("leaf.suite.name");
            if (leafSuiteName == null) {
                leafSuiteName = "-leaf" + VM.getNextHashcode() + "-";
            }

            // Don't create a suite for loading new classes if the class path is null
            if (classPath == null) {
                leafSuite = parent;
            } else {
                leafSuite = new Suite(leafSuiteName, parent);
                String noClassDefFoundClasses = getProperty("leaf.suite.NoClassDefFoundClasses");
                if (noClassDefFoundClasses != null && noClassDefFoundClasses.length() != 0) {
                    leafSuite.setNoClassDefFoundClassesString(noClassDefFoundClasses);
                }
            }
        }
    }

    /**
     * Starts running this isolate.
     *
     * @throws IllegalStateException if this isolate has already been started
     */
    public final void run() throws IllegalStateException {

        // Check and set the class state.
        if (state != NEW) {
            throw new IllegalStateException("cannot restart isolate");
        }
        if (VMThread.currentThread().getIsolate() != this) {
            throw new IllegalStateException("cannot run isolate from external thread");
        }

        changeState(ALIVE);

        // Manually initialize com.sun.squawk.Klass.
        initializeClassKlass();

        // Update the leaf suite
        updateLeafSuite();

        // It is important that java.lang.System is initialized before com.sun.cldc.i18n.Helper
        // so initialized it now.
        System.currentTimeMillis();

        String initializerClassName = VM.getIsolateInitializerClassName();
        
        // Verbose trace.
        if (VM.isVeryVerbose()) {
            System.out.print("[Starting isolate for '" + mainClassName);
            if (args != null) {
                for (int i = 0; i != args.length; ++i) {
                    System.out.print(" " + args[i]);
                }
            }
            System.out.print("' with class path set to '" + classPath +"'");
            if (parentSuiteSourceURI != null) {
                System.out.print(" and parent suite URI set to '" + parentSuiteSourceURI + "'");
            }
            if (leafSuite != null) {
                System.out.print(" and leaf suite '" + leafSuite + "'");
            }
            if (initializerClassName != null) {
                System.out.print(" will invoke specified initializer " + initializerClassName);
            }
            System.out.println("]");
        }
        
        // Invoke the main of the specified Isolate initializer specified on command line as -isolateinit:
        if (initializerClassName != null) {
            Klass klass = null;
            try {
                klass = Klass.forName(initializerClassName);
            } catch (ClassNotFoundException e) {
                System.err.println("No such class " + initializerClassName + ": " + e);
                exit(998);
            }
            boolean wasFirstInitialized = VM.isFirstIsolateInitialized();
            if (!wasFirstInitialized) {
                VM.setFirstIsolateInitialized(true);
            }
            klass.main(new String[] {wasFirstInitialized?"false":"true"});
        }

        // Notify debugger of event:
        Debugger debugger = this.getDebugger();
        if (debugger != null) {
            debugger.notifyEvent(new Debugger.Event(Debugger.Event.VM_INIT, VMThread.currentThread()));

            // This gives the debugger a chance to receive the THREAD_START event for the
            // initial thread in an isolate
//            debugger.notifyEvent(new Debugger.Event(Debugger.Event.THREAD_START, Thread.currentThread()));
        }

        // Find the main class and call it's main().
        Klass klass = null;
        try {
            klass = Klass.forName(mainClassName);
            klass.main(args);

            System.out.flush();
            System.err.flush();
        } catch (ClassNotFoundException ex) {
            System.err.println("No such class " + mainClassName + ": " + ex);
            exit(999);
        }
    }

    /**
     * Waits for all the other threads and child isolates belonging to this isolate to stop.
     */
    public void join() {

        /*
         * If this isolate is has not yet been started or is still alive then wait until it has exited or been hibernated
         */
        if (state <= ALIVE) {
            VMThread.isolateJoin(this);
        }

        /*
         * Join all the child isolates.
         */
        if (childIsolates != null) {
            for (Enumeration e = childIsolates.elements() ; e.hasMoreElements() ;) {
                Isolate isolate = (Isolate)e.nextElement();
                isolate.join();
            }
        }

        /*
         * Eliminate child isolates from the isolate object graph.
         */
        childIsolates = null;

        /*
         * Remove this isolate from its parent's list of children. The parentIsolate pointer
         * will be null for the bootstrap isolate as well as for unhibernated isolates
         */
        if (parentIsolate != null) {
            parentIsolate.childIsolates.remove(this);
            parentIsolate = null;
        }
    }

    /**
     * Adds a child isolate to this isolate.
     *
     * @param childIsolate  the child isolate
     */
    void addIsolate(Isolate childIsolate) {

        Assert.that(childIsolate.parentIsolate == null && childIsolate != this);
        childIsolate.parentIsolate = this;

        if (childIsolates == null) {
            childIsolates = new SquawkHashtable();
        }
        Assert.that(!childIsolates.containsKey(childIsolate));
        childIsolates.put(childIsolate, childIsolate);
    }

    /**
     * Add a thread to the isolate.
     *
     * @param thread the thread
     */
    void addThread(VMThread thread) {
        Assert.that(!childThreads.containsKey(thread));
        childThreads.put(thread, thread);
    }

    /**
     * Remove a thread from the isolate.
     *
     * @param thread the thread
     * @param uncaughtException true if the thread was stopped as the result of an uncaught exception
     */
    void removeThread(VMThread thread, boolean uncaughtException) {
        Assert.that(childThreads.containsKey(thread));
        childThreads.remove(thread);

        /*
         * Check for rundown condition. That is, keep running the isolate
         * if at least one non-daemon threading is still running.
         */
        for (Enumeration e = childThreads.elements(); e.hasMoreElements(); ) {
            thread = (VMThread)e.nextElement();
            if (!thread.isDaemon()) {
                return;
            }
        }

        /*
         * If all the non-daemon threads are dead then stop the isolate.
         */
        abort(uncaughtException ? 1 : 0);
    }

    /**
     * Test to see if class Klass is initialized.
     *
     * @return true if it is
     */
    public boolean isClassKlassInitialized() {
        return classKlassInitialized;
    }

    /**
     * Stop the isolate.
     *
     * @param code the exit code
     * @throws IllegalStateException if this isolate has not yet been {@link #start() started} or is
     *               already hibernated or exited
     */
    public void exit(int code) {
        if (state != ALIVE) {
            throw new IllegalStateException("cannot re-exit an isolate: state=" + state);
        }
        abort(code);
    }

    /**
     * Stop the isolate.
     *
     * @param code the exit code
     */
    void abort(int code) {
        if (state == ALIVE) {
            exitCode = code;
        }
        try {
            hibernate(false, EXITED);
        } catch (IOException e) {
            e.printStackTrace();
            Assert.shouldNotReachHere();
        }
    }

    /**
     * Serializes the object graph rooted by this hibernated isolate and writes it to a given stream.
     * The endianess of the serialized object graph is the endianess of the unerdlying platform.
     *
     * @param  dos       the DataOutputStream to which the serialized isolate should be written
     * @param  uri       a URI identifying the serialized isolate
     *
     * @throws OutOfMemoryError if there was insufficient memory to do the save
     * @throws IOException if there was some IO problem while writing the output
     * @throws IllegalStateException if this isolate is not currently hibernated or exited
     */
    public void save(DataOutputStream dos, String uri) throws java.io.IOException {
        save(dos, uri, VM.isBigEndian());
    }

    /**
     * Serializes the object graph rooted by this hibernated isolate and writes it to a given stream.
     *
     * @param  dos       the DataOutputStream to which the serialized isolate should be written
     * @param  uri       a URI identifying the serialized isolate
     * @param  bigEndian the endianess to be used when serializing this isolate
     *
     * @throws OutOfMemoryError if there was insufficient memory to do the save
     * @throws IOException if there was some IO problem while writing the output
     * @throws IllegalStateException if this isolate is not currently hibernated or exited
     */
    public void save(DataOutputStream dos, String uri, boolean bigEndian) throws java.io.IOException {
        if (state != HIBERNATED && state != EXITED) {
            throw new IllegalStateException("cannot save unhibernated isolate");
        }

        // Null out the interned string cache as it will be rebuilt on demand
        internedStrings = null;

        Assert.always(savedStackChunks == null);
        ObjectMemorySerializer.ControlBlock cb;
        cb = VM.copyObjectGraph(this);
        Assert.always(savedStackChunks == null);

        Suite readOnlySuite = leafSuite;
        while (GC.inRam(readOnlySuite)) {
            readOnlySuite = readOnlySuite.getParent();
        }

        ObjectMemorySerializer.save(dos, uri, cb, readOnlySuite.getReadOnlyObjectMemory(), bigEndian);
    }

    /**
     * Loads a serialized isolate from an input stream into RAM. It is up to the caller to unhibernate the isolate.
     *
     * @param dis  the data input stream to load from
     * @param uri  a URI identifying the serialized isolate
     */
    public static Isolate load(DataInputStream dis, String uri) {
        ObjectMemory om = ObjectMemoryLoader.load(dis, uri, false).objectMemory;
        return load(om);
    }

    /**
     * Loads a serialized isolate into RAM. It is up to the caller to unhibernate the isolate.
     *
     * @param om the object memory loader to load from
     */
    private static Isolate load(ObjectMemory om) {
        Object root = om.getRoot();
        if (!(root instanceof Isolate)) {
            throw new Error("object memory with URI '" + om.getURI() + "' does not contain an isolate");
        }

        Isolate isolate = (Isolate)root;
        GC.getCollector().registerStackChunks(isolate.savedStackChunks);
        isolate.savedStackChunks = null;
        VM.registerIsolate(isolate);

        if (VM.isVerbose()) {
            int old = VM.setStream(VM.STREAM_SYMBOLS);
            VM.print("UNHIBERNATED_ISOLATE.RELOCATION=");
            VM.printUWord(om.getStart().toUWord());
            VM.println();
            VM.setStream(old);
        }

        return isolate;
    }

    /**
     * Hibernate the isolate. If the current thread is in this isolate then
     * this function will only return when the isolate is unhibernated.
     *
     * @throws IOException if the underlying IO system cannot be serialized
     * @throws IllegalStateException if this isolate has not yet been {@link #start() started} or is
     *               already hibernated or exited or has a debugger attached to it
     */
    public void hibernate() throws java.io.IOException, IllegalStateException {
        if (state == NEW) {
            throw new IllegalStateException("cannot hiberate an unstarted isolate");
        }
        if (state >= HIBERNATED) {
            throw new IllegalStateException("cannot hibernate a hibernated or exited isolate");
        }
        if (debugger != null) {
            throw new IllegalStateException("cannot hibernate an isolate with an attached debugger");
        }
        hibernate(true, HIBERNATED);
    }

    /**
     * Modifies the state of this isolate.
     *
     * @param newState  the state to which the current state should transition
     */
    private void changeState(int newState) {
        this.state = newState;
    }

    /**
     * Hibernate the isolate. If the current thread is in this isolate then
     * this function will only return when the isolate is unhibernated.
     *
     * @param  hibernateIO  if true, the underlying IO system is also serialized. Only an
     *                      isolate with a hibernated IO system can be {@link #unhibernate() resumed}
     * @param  newState    the state that this isolate should be put into once this method completes
     * @throws IOException if the underlying IO system cannot be serialized
     */
    private void hibernate(boolean hibernateIO, int newState) throws java.io.IOException {
        if (hibernateIO && VM.isVeryVerbose()) {
            System.out.print("[Hibernating isolate for '" +mainClassName + "' with class path set to '" + classPath +"'");
            if (parentSuiteSourceURI != null) {
                System.out.print(" and parent suite URI set to '" + parentSuiteSourceURI + "'");
            }
            if (leafSuite != null) {
                System.out.print(" and leaf suite '" + leafSuite + "'");
            }
            System.out.println("]");
        }
        if (state != newState) {
            cleanupMailboxes();

            /*
             * Serialize the underlying context if this is not an exiting isolate
             */
            int channelContext = getChannelContext();
            if (hibernateIO && channelContext > 0) {
                hibernatedChannelContext = VM.hibernateChannelContext(channelContext);
            }

            changeState(newState);

            /*
             * Close the channel I/O
             */
            if (channelContext > 0) {
                VM.deleteChannelContext(channelContext);
                this.channelContext = 0;
            }

            /*
             * Hibernate all the executing threads.
             */
            VMThread.hibernateIsolate(this, state == EXITED);
        }
    }

    /*
     * Add a thread to the list of hibernated run threads.
     *
     * @param thread the thread to add
     */
    void addToHibernatedRunThread(VMThread thread) {
        Assert.that(thread.nextThread == null);
        thread.nextThread = hibernatedRunThreads;
        hibernatedRunThreads = thread;
    }

    /*
     * Add a thread to the list of hibernated timer threads.
     *
     * @param thread the thread to add
     */
    void addToHibernatedTimerThread(VMThread thread) {
        Assert.that(thread.nextTimerThread == null);
        thread.nextTimerThread = hibernatedTimerThreads;
        hibernatedTimerThreads = thread;
    }

    /**
     * Unhibernate the isolate.
     */
    public void unhibernate() {
        if (state != HIBERNATED) {
            throw new RuntimeException("Cannot unhibernate isolate that is not in hibernation state");
        }
        changeState(ALIVE);

        // Attach to current isolate as a child
        Isolate currentIsolate = VM.getCurrentIsolate();
        Assert.that(currentIsolate != null);
        currentIsolate.addIsolate(this);

        VMThread.unhibernateIsolate(this);
    }

    /*
     * Get all the hibernated run threads.
     *
     * @return the thread linked by thread.nextThread
     */
    VMThread getHibernatedRunThreads() {
        VMThread res = hibernatedRunThreads;
        hibernatedRunThreads = null;
        return res;
    }

    /*
     * Get all the hibernated timer threads.
     *
     * @return the thread linked by thread.nextTimerThread
     */
    VMThread getHibernatedTimerThreads() {
        VMThread res = hibernatedTimerThreads;
        hibernatedTimerThreads = null;
        return res;
    }

    /**
     * Determines if this isolate is {@link #hibernate() hibernated}.
     *
     * @return true if it is
     */
    public boolean isHibernated() {
        return state == HIBERNATED;
    }

    /**
     * Determines if this isolate has been (re)started and not yet (re)hibernated or exited.
     *
     * @return true if it is
     */
    public boolean isAlive() {
        return state == ALIVE;
    }

    /**
     * Determines if this isolate is {@link #exit exited}.
     *
     * @return true if it is
     */
    public boolean isExited() {
        return state == EXITED;
    }

    /**
     * Determines if this isolate has not yet been {@link #start started}.
     *
     * @return true if it is
     */
    public boolean isNew() {
        return state == NEW;
    }

    /**
     * Determines whether this isolate is being debugged
     * 
     * @return true if it is
     */
    public boolean isBeingDebugged() {
    	return debugger != null;
    }

    /**
     * Get the isolate exit code.
     *
     * @return the exit code
     */
    public int getExitCode() {
        return exitCode;
    }

/*if[FINALIZATION]*/
    /**
     * Add a finalizer to the queue of pending finalizers.
     *
     * @param finalizer the finalizer to add
     */
    void addFinalizer(Finalizer finalizer) {
        finalizer.setNext(finalizers);
        finalizers = finalizer;
    }

    /**
     * Remove a finalizer.
     *
     * @return the finalizer or null if there are none.
     */
    Finalizer removeFinalizer() {
        Finalizer finalizer = finalizers;
        if (finalizer != null) {
            finalizers = finalizer.getNext();
            finalizer.setNext(null);
        }
        return finalizer;
    }
/*end[FINALIZATION]*/

    /**
     * Add a thread to the list of threads waiting for the isolate to finish.
     *
     * @param thread the thread
     */
    void addJoiner(VMThread thread) {
        thread.nextThread = joiners;
        joiners = thread;
    }

    /**
     * Get all the joining threads.
     *
     * @return all the threads
     */
    VMThread getJoiners() {
        VMThread res = joiners;
        joiners = null;
        return res;
    }

    /**
     * Get the string representation of the isolate.
     *
     * @return the string
     */
    public String toString() {
        String res = "isolate \"".concat(mainClassName).concat("\"");
        if (isAlive()) {
            res = res.concat(" (ALIVE)");
        } else if (isExited()) {
            res = res.concat(" (EXITED)");
        } else if (isHibernated()) {
            res = res.concat(" (HIBERNATED)");
        } else {
            res = res.concat(" (NEW)");
        }
        return res;
    }

    /*---------------------------------------------------------------------------*\
     *                            Standard streams                               *
    \*---------------------------------------------------------------------------*/

    /**
     * A DelayedURLOutputStream is used to write to a connection and ensure that the
     * connection is only opened in the context of the isolate that will use it.
     */
    static class DelayedURLOutputStream extends OutputStream {

        /**
         * The delegate output stream.
         */
        private OutputStream out;

        /**
         * The URL used to create the stream.
         */
        private final String url;

        /**
         * Gets the delegate output stream, creating it if it hasn't already been opened.
         *
         * @return the OutputStream
         * @throws IOException if something went wrong while attempting to open the stream
         */
        private synchronized OutputStream out() throws IOException {
            if (out == null) {
                try {
                    out = Connector.openOutputStream(url);
                } catch (IOException e) {
                    VM.println("IO error opening standard stream to " + url + ": " + e);
                    throw e;
                }
            }
            return out;
        }

        /**
         * Creates a DelayedURLOutputStream.
         *
         * @param url  specifies where to open the connection
         */
        public DelayedURLOutputStream(String url) {
            this.url = url;
        }

        /**
         * {@inheritDoc}
         */
        public void write(int b) throws IOException {
            out().write(b);
        }

        /**
         * {@inheritDoc}
         */
        public void write(byte b[]) throws IOException {
            out().write(b);
        }

        /**
         * {@inheritDoc}
         */
        public void write(byte b[], int off, int len) throws IOException {
            out().write(b, off, len);
        }

        /**
         * {@inheritDoc}
         */
        public synchronized void flush() throws IOException {
            if (out != null) {
                out.flush();
            }
        }

        /**
         * {@inheritDoc}
         */
        public synchronized void close() throws IOException {
            if (out != null) {
                out.close();
                out = null;
            }
        }
    }

    public final MulticastOutputStream stdout = new MulticastOutputStream();
    public final MulticastOutputStream stderr = new MulticastOutputStream();

    /**
     * Adds a new connection to which {@link System#out} will send its output.
     * <p>
     * If the {@link Thread#currentThread current thread} is not owned by this isolate,
     * opening of the connection is delayed until the next time <code>System.out</code>
     * is written to by one of this isolate's threads. Otherwise the connection is
     * opened as part of this call.
     * <p>
     * If there was an existing connection identified by <code>url</code> prior to this
     * call, it is replaced and will rely on {@link Object#finalize finalization} for being closed.
     * <p>
     * The following code snippet is an example of how to pipe the standard output of the
     * current isolate to a network connection:
     * <p><blockquote><pre>
     *     Thread.currentThread().getIsolate().addOut("socket://server.domain.com:9999").
     * </pre></blockquote>
     *
     * @param url     the URL used to open the connection via {@link Connector#openOutputStream}
     */
    public void addOut(String url) {
        addStream(stdout, url);
    }

    /**
     * Adds a new connection to which {@link System#err} will send its output.
     * <p>
     * If the {@link Thread#currentThread current thread} is not owned by this isolate,
     * opening of the connection is delayed until the next time <code>System.err</code>
     * is written to by one of this isolate's threads. Otherwise the connection is
     * opened as part of this call.
     * <p>
     * If there was an existing connection identified by <code>url</code> prior to this
     * call, it is replaced and will rely on {@link Object#finalize finalization} for being closed.
     *
     * @param url     the URL used to open the connection via {@link Connector#openOutputStream}
     */
    public void addErr(String url) {
        addStream(stderr, url);
    }

    /**
     * Removes the connection identified by <code>url</code> (if any) to which {@link System#out}
     * is currently sending its output. The removed connection relies upon
     * {@link Object#finalize finalization} for releasing any system resources it holds.
     *
     * @param url     the URL identifying the connection to be removed
     */
    public void removeOut(String url) {
        stdout.remove(url);
    }

    /**
     * Removes the connection identified by <code>url</code> (if any) to which {@link System#err}
     * is currently sending its output. The removed connection relies upon
     * {@link Object#finalize finalization} for releasing any system resources it holds.
     *
     * @param url     the URL identifying the connection to be removed
     */
    public void removeErr(String url) {
        stderr.remove(url);
    }

    /**
     * Removes all the connections to which {@link System#out} is sending its output.
     * The removed connections rely upon {@link Object#finalize finalization} for
     * releasing any system resources they hold.
     */
    public void clearOut() {
        stdout.removeAll();
    }

    /**
     * Removes all the connections to which {@link System#err} is sending its output.
     * The removed connections rely upon {@link Object#finalize finalization} for
     * releasing any system resources they hold.
     */
    public void clearErr() {
        stderr.removeAll();
    }

    private void addStream(MulticastOutputStream mos, String url) {
        if (isCurrentThreadExternal()) {
            url = new String(url);
            mos.add(url, new DelayedURLOutputStream(url));
        } else {
            try {
                mos.add(url, Connector.openOutputStream(url));
            } catch (IOException e) {
                VM.println(mainClassName + ": IO error opening standard stream to " + url + ": " + e);
            }
        }
    }

    /**
     * Gets a list of URLs denoting the streams to which {@link System#out} is currently sending its output.
     * Note that due to multi-threading, the returned list may not reflect the complete
     * set of streams. If a stream was {@link #addOut added} by another thread, then the returned list
     * may not include the URL of the added stream. If a stream was {@link #removeOut removed} by another thread,
     * then the returned list may include the URL of the removed stream.
     *
     * @return  the list of streams to which <code>System.out</code> is currently sending its output
     */
    public String[] listOut() {
        return listStreams(stdout);
    }

    /**
     * Gets a list of URLs denoting the streams to which {@link System#err} is currently sending its output.
     * Note that due to multi-threading, the returned list may not reflect the complete
     * set of streams. If a stream was {@link #addErr added} by another thread, then the returned list
     * may not include the URL of the added stream. If a stream was {@link #removeErr removed} by another thread,
     * then the returned list may include the URL of the removed stream.
     *
     * @return  the list of streams to which <code>System.err</code> is currently sending its output
     */
    public String[] listErr() {
        return listStreams(stderr);
    }

    private String[] listStreams(MulticastOutputStream mos) {
        String[] names = new String[mos.getSize()];
        Enumeration e = mos.listNames();
        int i = 0;
        try {
            for (; i != names.length; ++i) {
                names[i] = (String)e.nextElement();
            }
        } catch (NoSuchElementException ex) {
            // Another thread removed a stream - resize the array
            Object old = names;
            names = new String[i];
            System.arraycopy(old, 0, names, 0, i);
        }
        return names;
    }

    
    /*---------------------------------------------------------------------------*\
     *                            Inter-isolate messages                         *
    \*---------------------------------------------------------------------------*/

    /**
     * Record this mailbox with the system. Called by Mailbox().
     *
     * @param mailbox the mailbox to record.
     */
    public void recordMailbox(Mailbox mailbox) {
        if (mailboxes == null) {
            mailboxes = new SquawkHashtable();
        } else if (mailboxes.get(mailbox) != null) {
            throw new IllegalStateException(mailbox + " is already recorded");
        }
        
        mailboxes.put(mailbox, mailbox);
    }
    
    /**
     * Tell the system to forget about this mailbox. Called by Mailbox.close().
     *
     * @param mailbox the mailbox to forget.
     */
    public void forgetMailbox(Mailbox mailbox) {
        if (mailboxes == null ||
            mailboxes.get(mailbox) == null) {
            throw new IllegalStateException(mailbox + " is not recorded");
        }
        
        mailboxes.remove(mailbox);
    }
    
    /**
     * Record all MailboxAddress objects that this Isolate uses to send messages to.
     *
     * @param mailbox the mailbox to record.
     */
    public void recordMailboxAddress(MailboxAddress address) {
        if (mailboxAddresses == null) {
            mailboxAddresses = new SquawkHashtable();
        } else if (mailboxAddresses.get(address) != null) {
            throw new IllegalStateException(address + " is already recorded");
        }
        
        mailboxAddresses.put(address, address);
    }
    
    /**
     * Tell the system to forget about this mailbox. Called by Mailbox.close().
     *
     * @param mailbox the mailbox to forget.
     */
    public void forgetMailboxAddress(MailboxAddress address) {
        if (mailboxAddresses == null ||
            mailboxAddresses.get(address) == null) {
            throw new IllegalStateException(address + " is not recorded");
        }
        
        mailboxAddresses.remove(address);
    }
    
    /**
     * Tell remote isolates that we won't talk to them again,
     * and close our Mailboxes.
     *
     * After this call, remote isolates may have MailboxAddress objects that refer 
     * to the closed mailboxes, but when they try to use the address, they will get an exception.
     */
    public void cleanupMailboxes() {
        // this is a bad context to get an error in - so report and squash the error.
        try {
            // tell all remote mailboxes that these references are going away.
            if (mailboxAddresses != null) {
                if (VM.isVeryVerbose()) {
                    System.err.println("Closing addresses...");
                }
                Enumeration addressE = mailboxAddresses.elements();
                while (addressE.hasMoreElements()) {
                    MailboxAddress address = (MailboxAddress)addressE.nextElement();
                    if (VM.isVeryVerbose()) {
                        System.err.println("Closing address " + address);
                    }
                    address.close(); // tolerant of double closes()
                }
            }
            
            // close all local Mailboxes
            if (mailboxes != null) {
                if (VM.isVeryVerbose()) {
                    System.err.println("Closing mailboxes...");
                }
                Enumeration mailboxE = mailboxes.elements();
                while (mailboxE.hasMoreElements()) {
                    Mailbox mailbox = (Mailbox)mailboxE.nextElement();
                    if (VM.isVeryVerbose()) {
                        System.err.println("Closing mailbox " + mailbox);
                    }
                    mailbox.close(); // tolerant of double closes()
                }
            }
        } catch (RuntimeException e) {
            System.err.println("Uncaught exception while cleaning up mailboxes: " + e);
            e.printStackTrace();
        }
        
        // TODO: What about hibernation, where server may have thread(s) waiting for messages in inbox.
        // we just threw away unhandled messages in mailbox.close(). Upon unhibernation,
        // will threads wake up still waiting for messages? And what about re-registering the
        // Mailbox?
    }
    
/*if[EXCLUDE]*/

    // Notes from Doug: This is still a work in progress and depending on resources/requirements, may
    // take quite some time for extra thought/design/implementation. My main concern with what exists
    // so far is that we are introducing more inter-isolate pointers which have not-yet-thought-out
    // implications for isolate hibernation & migration.
    //
    // In the long run, I think an implementation closer to that of MVM will be required to make
    // reasoning and robustness of true isolation better. That implementation does not have
    // inter-isolate pointers (within Java code & data structures at least) and instead employs
    // the concept of 'task IDs' and 'link IDs' to refer to objects not owned by the current
    // isolate.

    /*---------------------------------------------------------------------------*\
     *                            Inter-isolate messages                         *
    \*---------------------------------------------------------------------------*/

    /**
     * A FIFO queue for the inbox of an isolate. This implementation is derived
     * the standard J2SE java.util.LinkedList class.
     */
    static final class ParcelQueue {

        Entry header = new Entry(null, null, null);
        int size = 0;

        public ParcelQueue() {
            header.next = header.previous = header;
        }

        public Parcel removeLast() {
            return remove(header.previous);
        }

        public void addFirst(Parcel parcel) {
            addBefore(parcel, header.next);
        }

        /**
         * Removes all of the elements from this list.
        public void clear() {
            Entry e = header.next;
            while (e != header) {
                Entry next = e.next;
                e.next = e.previous = null;
                e.element = null;
                e = next;
            }
            header.next = header.previous = header;
            size = 0;
        }
        */

        static class Entry {
            Parcel parcel;
            Entry next;
            Entry previous;

            Entry(Parcel parcel, Entry next, Entry previous) {
                this.parcel = parcel;
                this.next = next;
                this.previous = previous;
            }
        }

        private Entry addBefore(Parcel parcel, Entry e) {
            Entry newEntry = new Entry(parcel, e, e.previous);
            newEntry.previous.next = newEntry;
            newEntry.next.previous = newEntry;
            size++;
            return newEntry;
        }

        private Parcel remove(Entry e) {
            Assert.that(e != header, "can remove element from empty queue");
            Parcel result = e.parcel;
            e.previous.next = e.next;
            e.next.previous = e.previous;
            e.next = e.previous = null;
            e.parcel = null;
            size--;
            return result;
        }
    }

    private final ParcelQueue messageInbox = new ParcelQueue();

    public static class Message {
        Message copy() {
            return null;
        }
    }

    /**
     * A <code>Parcel</code> encapsulates a {@link Message} posted to an isolate
     * and contains a reference to the sending isolate.
     */
    public final static class Parcel {
        public final Message message;
        public final Isolate sender;
        Parcel(Message message, Isolate sender) {
            this.message = message;
            this.sender = sender;
        }
    }

    public static void sendMessage(Isolate to, Message message) {
        if (to == null || message == null) {
            throw new NullPointerException();
        }
        synchronized (message) {
            synchronized (to.messageInbox) {
                to.messageInbox.addFirst(new Parcel(message.copy(), VM.getCurrentIsolate()));

                // notify any thread (in the receiving isolate) that another
                // message has been deposited in its inbox

                to.messageInbox.notifyAll();
            }
        }
    }

    /**
     * Retrieves the next available message sent to this isolate, blocking until
     * a message is available. Once a message has been retrieved, it is removed from
     * the receiving isolate's <i>inbox</i>.
     *
     * @param from  if not <code>null</code>, only a message sent by <code>from</code>
     *              will be returned
     * @return the retrieved message
     */
    public static Parcel receiveMessage(Isolate from) {
        Isolate current = VM.getCurrentIsolate();
        ParcelQueue inbox = current.messageInbox;
        synchronized (inbox) {
            while (inbox.size == 0) {
                if (!current.isAlive()) {
                    // throw something???
                }
                try {
                    inbox.wait();
                } catch (InterruptedException e) {
                }
            }

        }
        return null;
    }
/*end[EXCLUDE]*/

    /*---------------------------------------------------------------------------*\
     *                            Debugger Support                               *
    \*---------------------------------------------------------------------------*/

    /**
     * A Breakpoint instance describes a point in a method at which a breakpoint has been set.
     */
    public static class Breakpoint {

        /**
         * The method context of the breakpoint.
         */
        public final Object mp;

        /**
         * The offset (in bytes) from <code>mp</code> of the breakpoint.
         */
        public final int ip;

        /**
         * Constructor.
         */
        public Breakpoint(Object mp, int ip) {
            this.mp = mp;
            this.ip = ip;
        }

        /**
         * {@inheritDoc}
         */
        public boolean equals(Object o) {
            if (o instanceof Breakpoint) {
                Breakpoint bp = (Breakpoint)o;
                return bp.mp == mp && bp.ip == ip;
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        public int hashCode() {
            return ip;
        }
    }

    /**
     * Sets or removes the debugger under which this isolate is executing.
     *
     * @param debugger  connection to a debugger or null to remove a connection
     */
    void setDebugger(Debugger debugger) {
        if (debugger == null && this.debugger != null) {
            // Removing debugger state in threads when detaching
            for (Enumeration e = childThreads.elements(); e.hasMoreElements();) {
                VMThread thread = (VMThread)e.nextElement();
                thread.clearStep();

                // Clear a hit breakpoint only if it is a non-exception breakpoint.
                // Exception breakpoints still need to re-throw the reported exception.
                HitBreakpoint hbp = thread.getHitBreakpoint();
                if (hbp != null && hbp.exception == null) {
                    thread.clearBreakpoint();
                }
            }
        }
        this.debugger = debugger;
    }

    /**
     * Gets the debugger under which this isolate is executing.
     *
     * @return  the debugger under which this isolate is executing (if any)
     */
    public Debugger getDebugger() {
        return debugger;
    }

    /**
     * Gets the child threads of this isolate.
     *
     * @return  an Enumeration over the child threads of this isolate
     */
    public Enumeration getChildThreads() {
        return childThreads.elements();
    }

    /**
     * Gets the number of child threads of this isolate.
     *
     * @return  the number of child threads of this isolate
     */
    public int getChildThreadCount() {
        return childThreads.size();
    }

    public void updateBreakpoints(Breakpoint[] breakpoints) {
        this.breakpoints = breakpoints;
    }

    /**
     * The breakpoints that have been set in this isolate.
     */
    private Breakpoint[] breakpoints;
}
