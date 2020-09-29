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

import com.sun.squawk.util.Assert;
import com.sun.squawk.vm.Native;

/**
 * The VM used when running the romizer.
 *
 * @author  Nik Shaylor, Doug Simon
 */
public class VM {

    /*
     * Create the dummy isolate for romizing.
     */
    private static Isolate currentIsolate;

    /**
     * Flag to show if the extend bytecode can be executed.
     */
    static boolean extendsEnabled;

    /*=======================================================================*\
     *                           Romizer support                             *
    \*=======================================================================*/

    /**
     * The system-dependent path-separator character. This character is used to
     * separate filenames in a sequence of files given as a <em>path list</em>.
     * On UNIX systems, this character is <code>':'</code>; on Windows
     * systems it is <code>';'</code>.
     *
     * @return  the system-dependent path-separator character
     */
    public static char getPathSeparatorChar() {
        return File.pathSeparatorChar;
    }

    private static int nextIsolateID;
    static int allocateIsolateID() {
        return nextIsolateID++;
    }

    /**
     * The system-dependent default name-separator character.  This field is
     * initialized to contain the first character of the value of the system
     * property <code>file.separator</code>.  On UNIX systems the value of this
     * field is <code>'/'</code>; on Microsoft Windows systems it is <code>'\'</code>.
     *
     * @see     java.lang.System#getProperty(java.lang.String)
     */
    public static char getFileSeparatorChar() {
        return File.separatorChar;
    }

    public static boolean isVerbose() {
        return false;
    }

    public static boolean isVeryVerbose() {
        return false;
    }

    /**
     * Determines if the Squawk system is being run in a hosted environment
     * such as the romizer or mapper application.
     *
     * @return true if the Squawk system is being run in a hosted environment
     */
    public static boolean isHosted() {
        return true;
    }

    /**
     * Get the endianess.
     *
     * @return true if the system is big endian
     */
    public static boolean isBigEndian() {
        return Romizer.bigEndian;
    }

    /**
     * Assert a condition.
     *
     * @param b a boolean value that must be true.
     */
    public static void assume(boolean b) {
        Assert.that(b);
    }

    /**
     * Get the isolate of the currently executing thread.
     *
     * @return the isolate
     */
    public static Isolate getCurrentIsolate() {
        return currentIsolate;
    }

    /**
     * Set the isolate of the currently executing thread.
     *
     * @param isolate the isolate
     */
    static void setCurrentIsolate(Isolate isolate) {
        currentIsolate = isolate;
    }

    /*=======================================================================*\
     *                              Symbols stripping                        *
    \*=======================================================================*/

    static abstract class Matcher {

        public static final int PRECEDENCE_PACKAGE = 0;
        public static final int PRECEDENCE_CLASS = 1;
        public static final int PRECEDENCE_MEMBER = 2;

        final boolean keep;
        final int precedence;

        Matcher(boolean keep, int precedence) {
            this.keep = keep;
            this.precedence = precedence;
        }
        abstract boolean matches(String s);

        /**
         * Determines if this matcher represents a pattern that is more specific
         * than a matcher that matched some input also matched by this matcher.
         *
         * @param m  a matcher that precedes this one in the properties file
         *           and has also matched the same input this matcher just matched
         */
        abstract boolean moreSpecificThan(Matcher m);
    }

    static class PackageMatcher extends Matcher {
        private final String pkg;
        private final boolean recursive;
        PackageMatcher(String pattern, boolean keep) {
            super(keep, PRECEDENCE_PACKAGE);
            if (pattern.endsWith(".**")) {
                pkg = pattern.substring(0, pattern.length() - 3);
                recursive = true;
            } else {
                if (!pattern.endsWith(".*")) {
                    throw new IllegalArgumentException("Package pattern must end with \".*\" or \".**\"");
                }
                pkg = pattern.substring(0, pattern.length() - 2);
                recursive = false;
            }
        }

        boolean moreSpecificThan(Matcher m) {
            if (m instanceof PackageMatcher) {
                if (!recursive) {
                    return true;
                }
                PackageMatcher pm = (PackageMatcher)m;
                if (!pm.recursive) {
                    return false;
                }

                // both recursive so the longer package prefix is stronger
                return pkg.length() >= pm.pkg.length();
            } else {
                return precedence > m.precedence;
            }
        }

        boolean matches(String s) {
            if (recursive) {
                return s.startsWith(pkg);
            } else {
                if (!s.startsWith(pkg)) {
                    return false;
                }

                // Strip package prefix
                s = s.substring(pkg.length());

                // Matches if no more '.'s in class name
                return s.indexOf('.') == -1;
            }
        }
    }

    static class ClassMatcher extends Matcher {
        private final String pattern;
        ClassMatcher(String pattern, boolean keep) {
            super(keep, PRECEDENCE_CLASS);
            if (pattern.indexOf('#') != -1 || pattern.indexOf('*') != -1) {
                throw new IllegalArgumentException("Class name must not contain '*' or '#'");
            }
            this.pattern = pattern;
        }

        boolean moreSpecificThan(Matcher m) {
            if (m instanceof ClassMatcher) {
                // The class pattern is identical so take the one later
                // in the properties file (i.e. this one)
                return true;
            } else {
                return precedence > m.precedence;
            }
        }

        public boolean matches(String s) {
            int index = s.indexOf('#');
            if (index != -1) {
                s = s.substring(0, index);
            }
            return pattern.equals(s);
        }
    }

    static class MemberMatcher extends Matcher {
        private final String pattern;
        MemberMatcher(String pattern, boolean keep) {
            super(keep, PRECEDENCE_MEMBER);
            if (pattern.indexOf('*') != -1) {
                throw new IllegalArgumentException("Member name must not contain '*'");
            }
            this.pattern = pattern;
        }

        boolean moreSpecificThan(Matcher m) {
            if (m instanceof MemberMatcher) {
                // take the longer pattern
                return pattern.length() >= ((MemberMatcher)m).pattern.length();
            } else {
                return precedence > m.precedence;
            }
        }

        public boolean matches(String s) {
            if (s.indexOf('#') == -1) {
                return false;
            }
            return s.startsWith(pattern);
        }
    }

    static List stripMatchers = new ArrayList();

    /**
     * Resets the settings used to determine what symbols are to be retained/stripped
     * when stripping a suite in library mode (i.e. -strip:l) or extendable library
     * mode (i.e. -strip:e). An element that is <code>private</code> will always be stripped
     * in these modes and an element that is package <code>private</code> will always be
     * stripped in library mode.
     * <p>
     * The key of each property in the file specifies a pattern that will be used to match
     * a class, field or method that may be stripped. The value for a given keep must be
     * "keep" or "strip". There are 3 different types of patterns that can be specified:
     *
     * 1. A package pattern ends with ".*" or ".**". The former is used to match an
     *    element in a package and the latter extends the match to include any sub-package.
     *    For example:
     *
     *    java.**=keep
     *    javax.**=keep
     *    com.sun.**=strip
     *    com.sun.squawk.*=keep
     *
     *    This will keep all the symbols in any package starting with "java." or "javax.".
     *    All symbols in a package starting with "com.sun." will also be stripped <i>except</i>
     *    for symbols in the "com.sun.squawk" package.
     *
     *    This also show the precedence between patterns. A more specific pattern takes
     *    precedence over a more general pattern. If two patterns matching some given input
     *    are identical, then the one occurring lower in the properties file has higher precedence.
     *
     *  2. A class pattern is a fully qualified class name. For example:
     *
     *    com.sun.squawk.Isolate=keep
     *
     *  3. A field or method pattern is a fully qualified class name joined to a field or method
     *     name by a '#' and (optionally) suffixed by parameter types for a method. For example:
     *
     *    com.sun.squawk.Isolate#isTckTest=strip
     *    com.sun.squawk.Isolate#clearErr(java.lang.String)=keep
     *    com.sun.squawk.Isolate#removeThread(com.sun.squawk.VMThread,boolean)=strip
     *
     * A member pattern takes precedence over a class pattern which in turn takes precedence
     * over a package pattern.
     *
     * @param path  the properties file with the settings
     */
    static void resetSymbolsStripping(final File path) {
        stripMatchers.clear();
        try {
            FileInputStream fis = new FileInputStream(path);
            new Properties() {
                public Object put(Object key, Object value) {
                    String k = (String)key;
                    String v = (String)value;

                    boolean keep;
                    if ("keep".equalsIgnoreCase(v)) {
                        keep = true;
                    } else if ("strip".equalsIgnoreCase(v)) {
                        keep = false;
                    } else {
                        throw new IllegalArgumentException("value for property " + k + " in " + path + " must be 'keep' or 'strip'");
                    }

                    if (k.endsWith("*")) {
                        stripMatchers.add(new PackageMatcher(k, keep));
                    } else if (k.indexOf('#') != -1) {
                        stripMatchers.add(new MemberMatcher(k, keep));
                    } else {
                        stripMatchers.add(new ClassMatcher(k, keep));
                    }
                    return value;
                }
            }.load(fis);
            fis.close();
            System.out.println("Loaded suite stripping settings from " + path);
        }
        catch (IOException e) {
            System.err.println("Error loading properties from " + path + ": " + e);
            stripMatchers.clear();
        }
    }

    /**
     * Determines if the symbols for a class, field or method should be stripped.
     *
     * @param s   a class, field or method identifier
     */
    static boolean strip(String s) {
        Matcher current = null;
        for (Iterator iter = stripMatchers.iterator(); iter.hasNext(); ) {
            Matcher m = (Matcher)iter.next();
            if (m.matches(s) && (current == null || m.moreSpecificThan(current))) {
                current = m;
            }
        }
        return current != null && !current.keep;
    }

    /**
     * Determines if all the symbolic information for a class should be stripped. This
     * is used during the bootstrap process by the romizer to strip certain classes
     * based on their names.
     *
     * @param klass         the class to consider
     * @param type          the stripping level specified by the user. Must be
     *                      {@link Suite#APPLICATION}, {@link Suite#LIBRARY},
     *                      {@link Suite#EXTENDABLE_LIBRARY} or {@link Suite#DEBUG}.
     * @return true if the class symbols should be stripped
     */
    static boolean stripSymbols(Klass klass) {
        return strip(klass.getName());
    }

    /**
     * Determines if all the symbolic information for a field or method should be stripped. This
     * is used during the bootstrap process by the romizer to strip certain fields and methods
     * based on their names.
     *
     * @param klass         the class to consider
     * @param type          the stripping level specified by the user. Must be
     *                      {@link Suite#APPLICATION}, {@link Suite#LIBRARY},
     *                      {@link Suite#EXTENDABLE_LIBRARY} or {@link Suite#DEBUG}.
     * @return true if the class symbols should be stripped
     */
    static boolean stripSymbols(Member member) {
        String s = member.getDefiningClass().getName() + "#" + member.getName();
        if (member instanceof Method) {
            Method method = (Method)member;
            Klass[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length == 0) {
                s += "()";
            } else {
                StringBuffer buf = new StringBuffer(15);
                buf.append('(');
                for (int i = 0 ; i < parameterTypes.length ; i++) {
                    buf.append(parameterTypes[i].getInternalName());
                    if (i != parameterTypes.length - 1) {
                        buf.append(',');
                    }
                }
                buf.append(')');
                s += buf.toString();
            }
        }
        return strip(s);
    }

    /*=======================================================================*\
     *                              Native methods                           *
    \*=======================================================================*/

    /**
     * Zero a block of memory.
     *
     * @param      start        the start address of the memory area
     * @param      end          the end address of the memory area
     */
    static void zeroWords(Address start, Address end) {
        // Not needed for the romizer.
    }

    /**
     * Determines if the VM was built with memory access type checking enabled.
     *
     * @return true
     */
    public static boolean usingTypeMap() {
        return /*VAL*/true/*TYPEMAP*/;
    }


    /*=======================================================================*\
     *                              Symbols dumping                          *
    \*=======================================================================*/


    public static final int STREAM_STDOUT = 0;
    public static final int STREAM_STDERR = 1;
    static final int STREAM_SYMBOLS = 2;
    static final int STREAM_HEAPTRACE = 3;

    static int stream = STREAM_STDOUT;
    static final PrintStream Streams[] = new PrintStream[4];
    static {
        Streams[STREAM_STDOUT] = System.out;
        Streams[STREAM_STDERR] = System.err;
    }


    /**
     * Sets the stream for the VM.print... methods to one of the STREAM_... constants.
     *
     * @param stream  the stream to use for the print... methods
     */
    public static int setStream(int stream) {
        Assert.always(stream >= STREAM_STDOUT && stream <= STREAM_HEAPTRACE, "invalid stream specifier");
        int old = VM.stream;
        VM.stream = stream;
        return old;
    }

    /**
     * Print an error message.
     *
     * @param msg the message
     */
    public static void println(String msg) {
        PrintStream out = Streams[stream];
        out.println(msg);
        out.flush();
    }

    public static void println(boolean x) {
        PrintStream out = Streams[stream];
        out.println(x);
        out.flush();
    }

    public static void println() {
        PrintStream out = Streams[stream];
        out.println();
        out.flush();
    }

    public static void print(String s) {
        PrintStream out = Streams[stream];
        out.print(s);
        out.flush();
    }

    public static void print(int i) {
        PrintStream out = Streams[stream];
        out.print(i);
        out.flush();
    }

    public static void print(char ch) {
        PrintStream out = Streams[stream];
        out.print(ch);
        out.flush();
    }

    static void printAddress(Address val) {
        printUWord(val.toUWord());
    }

    public static void printAddress(Object val) {
        PrintStream out = Streams[stream];
        out.print(val);
        out.flush();
    }

    public static void printUWord(UWord val) {
        PrintStream out = Streams[stream];
        out.print(val);
        out.flush();
    }

    public static void printOffset(Offset val) {
        PrintStream out = Streams[stream];
        out.print(val);
        out.flush();
    }

    /**
     * Stop fatally.
     */
    public static void fatalVMError() {
        throw new Error();
    }

    /*=======================================================================*\
     *                             Object graph copying                      *
    \*=======================================================================*/

    /**
     * Make a copy of the object graph rooted at a given object.
     *
     * @param object    the root of the object graph to copy
     * @return the ObjectMemorySerializer.ControlBlock instance that contains the serialized object graph and
     *                  its metadata. This will be null if there was insufficient memory
     *                  to do the serialization.
     */
    static ObjectMemorySerializer.ControlBlock copyObjectGraph(Object object) {
        assume(object instanceof Suite);
        return ObjectGraphSerializer.serialize(object);
    }


    /*=======================================================================*\
     *                          Native method lookup                         *
    \*=======================================================================*/

    /**
     * Hashtable to translate names into enumerations.
     */
    private static Hashtable table = new Hashtable();

    /**
     * Hashtable of unused native methods.
     */
    private static Hashtable unused = new Hashtable();

    /**
     * Initializer.
     */
    static {
        try {
            Class clazz = com.sun.squawk.vm.Native.class;
            java.lang.reflect.Field[] fields = clazz.getDeclaredFields();
            for (int i = 0 ; i < fields.length ; i++) {
                java.lang.reflect.Field field = fields[i];
                if (field.getType() == Integer.TYPE) {
                    String name = field.getName().replace('_', '.').replace('$', '.');
                    int number = field.getInt(null);
                    table.put(name, new Integer(number));
                    unused.put(name, name);
                }
            }
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
            System.exit(-1);
        }
    }

    /**
     * Determines if a given native method can be linked to by classes dynamically
     * loaded into the Squawk VM.
     *
     * @param name   the fully qualified name of a native method
     * @return true if the method can be linked to
     */
    static boolean isLinkableNativeMethod(String name) {
        String table = Native.LINKABLE_NATIVE_METHODS;
        String last = null;
        int id = 0;
        int start = 0;
        int end = table.indexOf(' ');
        while (end != -1) {
            int sharedSubstringLength = table.charAt(start++) - '0';
            String entryName = table.substring(start, end);

            // Prepend prefix shared with previous entry (if any)
            if (sharedSubstringLength != 0) {
                Assert.that(last != null);
                entryName = last.substring(0, sharedSubstringLength) + entryName;
            }

            if (entryName.equals(name)) {
                return true;
            }

            start = end + 1;
            end = table.indexOf(' ', start);
            last = entryName;
            id++;
        }
        return false;
    }

    /**
     * Gets the identifier for a registered native method.
     *
     * @param name   the fully qualified name of the native method
     * @return the identifier for the method or -1 if the method has not been registered
     */
    public static int lookupNative(String name) {
        Integer id = (Integer)table.get(name);
        if (id != null) {
            unused.remove(name);
            return id.intValue();
        }
        return -1;
    }

    /**
     * Get all the symbols in a form that will go into a properties file.
     *
     * @return a string with all the definitions.
     */
    public static void printNatives(PrintStream out) {
        Enumeration names = table.keys();
        Enumeration identifiers = table.elements();
        while (names.hasMoreElements()) {
            out.println("NATIVE."+identifiers.nextElement()+".NAME="+names.nextElement());
        }


/* Uncomment to get list of apparently unused native methods */
/*
        Enumeration keys = unused.keys();
        while (keys.hasMoreElements()) {
            System.err.println("Warning: Unused native method "+keys.nextElement());
        }
*/
    }
    
}
