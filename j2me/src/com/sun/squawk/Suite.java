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
import java.util.NoSuchElementException;

import javax.microedition.io.*;

import com.sun.squawk.pragma.*;
import com.sun.squawk.util.*;
import com.sun.squawk.vm.*;

/**
 * A suite is the unit of deployment/compilation in the Squawk system.
 */
public final class Suite {

    /*---------------------------------------------------------------------------*\
     *                            Fields and constructor                         *
    \*---------------------------------------------------------------------------*/

    /**
     * The classes in the suite.
     */
    private Klass[] classes;

    /**
     * The name of the suite.
     */
    private final String name;

    /**
     * The array of metadata objects for the classes in this suite. The
     * metadata object for the class at index <i>idx</i> in the
     * <code>classes</code> array is at index <i>idx</i> in the
     * <code>metadata</code> array.
     */
    private KlassMetadata[] metadatas;

    /**
     * The suite that this suite is bound against. That is, the classes of this
     * suite reference classes in the parent suite and its parents.
     */
    private final Suite parent;

    /**
     * Specifies whether or not this suite is open. Only an open suite can have
     * classes installed in it.
     */
    private boolean closed;

	/**
	 * Resource files embedded in the suite.
	 */
    private ResourceFile[] resourceFiles;
	
	/**
	 * Manifest properties embedded in the suite.
	 */
	private ManifestProperty [] manifestProperties;
    
    /**
     * PROPERTIES_MANIFEST_RESOURCE_NAME has already been looked for or found.
     */
    private boolean isPropertiesManifestResourceInstalled;
    
    /**
     * List of classes that should throw a NoClassDefFoundError instead of a ClassNotFoundException.
     * See implementation of {@link Klass#forName(String, boolean, boolean)} for more information.
     */
    private String noClassDefFoundClassesString;
	
    /**
     * Creates a new <code>Suite</code> instance.
     *
     * @param  name        the name of the suite
     * @param  parent      suite whose classes are linked to by the classes of this suite
     */
    Suite(String name, Suite parent) {
        this.name = name;
        this.parent = parent;
        int count = (isBootstrap() ? CID.LAST_SYSTEM_ID + 1 : 0);
        classes = new Klass[count];
        metadatas = new KlassMetadata[count];
        resourceFiles = new ResourceFile[0];
		manifestProperties = new ManifestProperty [] {};
    }

    /*---------------------------------------------------------------------------*\
     *                                  Getters                                  *
    \*---------------------------------------------------------------------------*/

    /**
     * Gets this suite's name.
     *
     * @return  this suite's name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the parent suite of this suite.
     *
     * @return the parent suite of this suite
     */
    public Suite getParent() {
        return parent;
    }

    /**
     * Gets the URI identifier of the serialized form of this suite.
     *
     * @return the URI from which this suite was loaded or null if the suite was dynamically created
     */
    public String getURI() {
        ObjectMemory om = getReadOnlyObjectMemory();
        if (om != null) {
            return om.getURI();
        } else {
            return null;
        }
    }

    /**
     * Gets the number of classes in this suite.
     *
     * @return the number of classes in this suite
     */
    public int getClassCount() {
        return classes.length;
    }

    /**
     * Determines if this suite is closed. Open an open suite can have classes installed in it.
     *
     * @return boolean
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Determines if this is the bootstrap suite containing the system classes.
     *
     * @return true if this suite has no parent
     */
    public boolean isBootstrap() {
        return parent == null;
    }

    /**
     * Gets the next available number for a class that will be installed in this suite.
     * The value returned by this method will never be the same for this suite.
     *
     * @return the next available number for a class that will be installed in this suite
     */
    public int getNextAvailableClassNumber() {
        return getClassCount();
    }

    /**
     * Gets the class in this suite corresponding to a given class number.
     *
     * @param   suiteID  the class number of the class to retrieve
     * @return  the class corresponding to <code>suiteID</code>
     */
    public Klass getKlass(int suiteID) {
        Assert.that(suiteID < classes.length);
        return classes[suiteID];
    }

	/**
	 * Gets the contents of a resource file embedded in the suite.
	 *
	 * @param name the name of the resource file whose contents is to be retrieved
	 * @return the resource data, or null if the resource file doesn't exist
	 */
	public byte [] getResourceData(String name) {
        // Look in parents first
        if (!isBootstrap()) {
            byte[] bytes = parent.getResourceData(name);
            if (bytes != null) {
                return bytes;
            }
        }
        int index = Arrays.binarySearch(resourceFiles, name, ResourceFile.comparer);
        if (index < 0) {
            return null;
        }
		return resourceFiles[index].data;
	}

	/**
	 * Gets the value of an {@link Suite#PROPERTIES_MANIFEST_RESOURCE_NAME} property embedded in the suite.
	 *
	 * @param name the name of the property whose value is to be retrieved
	 * @return the property value
	 */
	public String getManifestProperty(String name) {
		int index = Arrays.binarySearch(manifestProperties, name, ManifestProperty.comparer);
        if (index < 0) {
            // To support dynamic class loading we need to follow the same semantics as Klass.forName
            // which is to look to see if we can't dynamically load a property if its not found
            if (isClosed() || isPropertiesManifestResourceInstalled) {
                return null;
            }
            // The following should automatically install the properties if there is a manifest
            InputStream input = getResourceAsStream(PROPERTIES_MANIFEST_RESOURCE_NAME, null);
            if (input != null) {
                try {input.close();} catch (IOException e) {};
            }
            isPropertiesManifestResourceInstalled = true;
            index = Arrays.binarySearch(manifestProperties, name, ManifestProperty.comparer);
            if (index < 0) {
                return null;
            }
        }
		return manifestProperties [index].value;
	}
	
    /**
     * Finds a resource with a given name.  This method returns null if no
     * resource with this name is found.  The rules for searching
     * resources associated with a given class are profile
     * specific.
     *
     * @param name  name of the desired resource
     * @param klass Used to get the absolute path to resource if name is not absolute, if null, then assume resource name is absolute
     * @return      a <code>java.io.InputStream</code> object.
     * @since JDK1.1
     */
    public final java.io.InputStream getResourceAsStream(String name, Klass klass) {
        if ((name.length() > 0 && name.charAt(0) == '/')) {
            name = name.substring(1);
        } else if (klass != null) {
            String className = klass.getName();
            int dotIndex = className.lastIndexOf('.');
            if (dotIndex >= 0) {
                name = className.substring(0, dotIndex + 1).replace('.', '/') + name;
            }
        }
        byte[] bytes = getResourceData(name);
        if (bytes == null) {
            // TODO Should we throw exceptions here like forName ?, I do not think so, since getting resources is not
            // as hard a requirement as being able to find a class ?
            if (isClosed()) {
                return null;
            }
            Isolate isolate = VM.getCurrentIsolate();
            TranslatorInterface translator = isolate.getTranslator();
            if (translator == null) {
                return null;
            }
            translator.open(isolate.getLeafSuite(), isolate.getClassPath());
            bytes = translator.getResourceData(name);
            if (bytes == null) {
                return null;
            }
        }
        return new ByteArrayInputStream(bytes);
    }

    /**
     * Gets a string representation of this suite. The string returned is
     * name of this suite with "suite " prepended.
     *
     * @return  the name of this suite with "suite " prepended
     */
    public String toString() {
        return "suite " + name;
    }

    /**
     * Gets a reference to the ObjectMemory containing this suite in read-only memory.
     *
     * @return  the ObjectMemory containing this suite if it is in read-only memory or null
     */
    private ObjectMemory getReadOnlyObjectMemoryHosted() throws HostedPragma {
        String uri = (isBootstrap() ? ObjectMemory.BOOTSTRAP_URI : "file://" + name + ".suite");
        return GC.lookupReadOnlyObjectMemoryBySourceURI(uri);
    }

    /**
     * Gets a reference to the ObjectMemory containing this suite in read-only memory.
     *
     * @return  the ObjectMemory containing this suite if it is in read-only memory or null
     */
    ObjectMemory getReadOnlyObjectMemory() {
        if (VM.isHosted()) {
            return getReadOnlyObjectMemoryHosted();
        } else {
            return GC.lookupReadOnlyObjectMemoryByRoot(this);
        }
    }

    /*---------------------------------------------------------------------------*\
     *                            Class installation                             *
    \*---------------------------------------------------------------------------*/

    /**
     * Installs a given class into this suite.
     *
     * @param klass  the class to install
     */
    public void installClass(Klass klass) {
        checkWrite();
        int suiteID = klass.getSuiteID();
        if (suiteID < classes.length) {
            Assert.that(classes[suiteID] == null, klass + " already installed");
        } else {
            Klass[] old = classes;
            classes = new Klass[suiteID + 1];
            System.arraycopy(old, 0, classes, 0, old.length);
        }
        classes[suiteID] = klass;
    }

    /**
     * Installs the metadata for a class into this suite. This class to which
     * the metadata pertains must already have been installed and there must
     * be no metadata currently installed for the class.
     *
     * @param metadata  the metadata to install
     */
    void installMetadata(KlassMetadata metadata) {
        checkWrite();
        Klass klass = metadata.getDefinedClass();
        int suiteID = klass.getSuiteID();
        Assert.that(suiteID < classes.length && classes[suiteID] == metadata.getDefinedClass(), klass + " not yet installed");
        if (suiteID < metadatas.length) {
            Assert.that(metadatas[suiteID] == null, "metadata for " + klass + "already installed");
        } else {
            KlassMetadata[] old = metadatas;
            metadatas = new KlassMetadata[suiteID + 1];
            System.arraycopy(old, 0, metadatas, 0, old.length);
        }
        metadatas[suiteID] = metadata;
    }
    
    /**
     * If a {@link Klass#forName(String)} is performed and class requested is not found AND
     * its added to our list of {@link #classesToNoClassDefFoundError} then we will throw a
     * {@link NoClassDefFoundError}.
     * 
     * @param className
     */
    void setNoClassDefFoundClassesString(String noClassDefFoundClasses) {
        noClassDefFoundClassesString = noClassDefFoundClasses;
    }
    
    boolean shouldThrowNoClassDefFoundErrorFor(String className) {
        if (noClassDefFoundClassesString == null) {
            return false;
        }
        String lookFor = ' ' + className;
        int index = noClassDefFoundClassesString.indexOf(lookFor);
        return index != -1;
    }

    /*---------------------------------------------------------------------------*\
     *                          MIDlet data installation                          *
    \*---------------------------------------------------------------------------*/

	/**
	 * Installs a collection of resource files into this suite. 
	 *
	 * @param resources array of resource files to install
	 */
	public void installResource(ResourceFile resourceFile) {
		checkWrite();
        System.arraycopy(resourceFiles, 0, resourceFiles = new ResourceFile[resourceFiles.length + 1], 0, resourceFiles.length - 1);
        resourceFiles[resourceFiles.length - 1] = resourceFile;
        Arrays.sort(resourceFiles, ResourceFile.comparer);
        if (resourceFile.name.equalsIgnoreCase(PROPERTIES_MANIFEST_RESOURCE_NAME)) {
            isPropertiesManifestResourceInstalled = true;
            // Add the properties defined in the manifest file
            loadProperties(resourceFile.data);
        }
	}

    /**
     * Must be called AFTER {@link #loadClasses(String[], int)}
     * @param suite
     * @param bytes
     * @throws IOException
     */
  /*  protected void loadProperties(byte[] bytes) {
        LineReader reader = new LineReader(new InputStreamReader(new ByteArrayInputStream(bytes)));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                StringTokenizer tokenizer = new StringTokenizer(line, ":");
                try {
                    String key = tokenizer.nextToken();
                    try {
                        String value = tokenizer.nextToken();
                        ManifestProperty property = new ManifestProperty(key, value);
                        if (VM.isVerbose()) {
                            System.out.println("[Adding property key: " + key + " value: " + value + "]");
                        }
                        installProperty(property);
                    } catch (NoSuchElementException e) {
                        // No value we are now outside of main section, we are done
                        return;
                    }
                } catch (NoSuchElementException e) {
                    // empty line, just ignore and go on
                }
            }
        } catch (IOException e) {
            if (VM.isVerbose()) {
                System.out.println("Error while loading properties: " + e.getMessage());
            }
        }
    }*/
    
    /**
     * Return true if character is a tab or space. Note that readLine() strips off '\n' and '\r'.
     */
    static boolean isWhiteSpace(char ch) {
        return (ch == ' ') || (ch == '\t');
    }
    
    /**
     * Strip the leading white space characters from string "Src", starting from index "start".
     */
    static String stripLeadingWS(String src, int start) {
        int len = src.length();
        while ((start < len) && isWhiteSpace(src.charAt(start))) {
            start++;
        }
        return src.substring(start);
    }
    
    /** 
     * Parse properties from jar manifest file. Based on manifest spec:
     *     http://java.sun.com/j2se/1.4.2/docs/guide/jar/jar.html
     *
     * ABOUT "application descriptors", WHICH ARE NOT SUPPORTED BY THIS METHOD:
     * Note that this syntax is slightly different than the "application descriptor" syntax in the IMP and MIDP specs.
     * An "application descriptor" does not support "continuation lines", or trailing spaces in a value. This is
     * an known annoyance of the MIDP spec.  In addition, the MIDP 1.0 and IMP 1.0 specs have in a bug in the BNF,
     * such that white space is REQUIRED before and after the value. The MIDP 2.0 specs correctly show that such 
     * white space is optional.
     */
    protected void loadProperties(byte[] bytes) {
        LineReader reader = new LineReader(new InputStreamReader(new ByteArrayInputStream(bytes)));
        String line;
        try {
            String key = null;
            String value = null;
            while ((line = reader.readLine()) != null) {
                if (line.length() == 0) {
                    // empty line, just ignore and go on
                    // NOTE - spec says that this ends the main section. Is this right?
                    continue;
                }
                
                int keyEnd = line.indexOf(':');
                boolean continuationLine = isWhiteSpace(line.charAt(0));
                if (continuationLine) {
                    if ((key == null || value == null)) {
                        throw new IOException("Illformed continuation line :" + line);
                    }
                    value = value + stripLeadingWS(line, 0);
                } else if (keyEnd > 0) {
                    if (key != null) {
                        addProperty(key, value);
                    }
                    key = line.substring(0, keyEnd);
                    value = stripLeadingWS(line, keyEnd+1);
                    // leave this data until next time around.
                } else {
                    throw new IOException("Illformed property line :" + line);
                }
            }
            
            if (key != null) {
                addProperty(key, value);
            }
        } catch (IOException e) {
            if (VM.isVerbose()) {
                System.out.println("Error while loading properties: " + e.getMessage());
            }
        }
    }
    
    private void addProperty(String key, String value) {
        Assert.that(value != null);
        ManifestProperty property = new ManifestProperty(key, value);
        if (VM.isVerbose()) {
            System.out.println("[Adding property key: " + key + " value: " + value + "]");
        }
        installProperty(property);
    }
    
	/**
	 * Installs a collection of IMlet property values into this suite.
	 *
	 * @param properties IMlet properties array to install
	 */
	public void installProperty(ManifestProperty property) {
		checkWrite();
		
        System.arraycopy(manifestProperties, 0, manifestProperties = new ManifestProperty[manifestProperties.length + 1], 0, manifestProperties.length - 1);
        manifestProperties[manifestProperties.length - 1] = property;
        Arrays.sort(manifestProperties, ManifestProperty.comparer);
	}

    /*---------------------------------------------------------------------------*\
     *                              Class lookup                                 *
    \*---------------------------------------------------------------------------*/

    /**
     * Gets the <code>KlassMetadata</code> instance from this suite and its parents
     * corresponding to a specified class.
     *
     * @param    klass  a class
     * @return   the <code>KlassMetadata</code> instance corresponding to
     *                <code>klass</code> or <code>null</code> if there isn't one
     */
    KlassMetadata getMetadata(Klass klass) {
        // Look in parents first
        if (!isBootstrap()) {
            KlassMetadata metadata = parent.getMetadata(klass);
            if (metadata != null) {
                return metadata;
            }
        }

        if (metadatas != null) {
            int suiteID = klass.getSuiteID();
            if (suiteID < metadatas.length) {
                KlassMetadata metadata = metadatas[suiteID];
                if (metadata != null && metadata.getDefinedClass() == klass) {
                    return metadata;
                }
            }
        }
        return null;
    }

    /**
     * Gets the <code>Klass</code> instance from this suite corresponding
     * to a specified class name in internal form.
     *
     * @param   name     the name (in internal form) of the class to lookup
     * @return  the <code>Klass</code> instance corresponding to
     *                   <code>internalName</code> or <code>null</code> if there
     *                   isn't one.
     */
    public Klass lookup(String name) {
        // Look in parents first
        if (!isBootstrap()) {
            Klass klass = parent.lookup(name);
            if (klass != null) {
                return klass;
            }
        }

        for (int i = 0 ; i < classes.length ; i++) {
            Klass klass = classes[i];
            if (klass != null) {
                if (klass.getInternalName().compareTo(name) == 0) { // bootstrapping issues prevent the use of equals()
                    return klass;
                }
            }
        }
        return null;
    }

    /**
     * Ensures that this suite is not in read-only memory before being updated.
     *
     * @throws IllegalStateException if this suite is closed
     * @throws IllegalStoreException if this suite is in read-only memory
     */
    private void checkWrite() {
        if (closed) {
            throw new IllegalStateException(this + " is closed");
        }
        if (!VM.isHosted() && !GC.inRam(this)) {
            throw new IllegalStoreException("trying to update read-only object: " + this);
        }
    }

    /*---------------------------------------------------------------------------*\
     *                            hashcode & equals                              *
    \*---------------------------------------------------------------------------*/

    /**
     * Compares this suite with another object for equality. The result is true
     * if and only if <code>other</code> is a <code>Suite</code> instance
     * and its name is equal to this suite's name.
     *
     * @param   other   the object to compare this suite against
     * @return  true if <code>other</code> is a <code>Suite</code> instance
     *                  and its name is equal to this suite's name
     */
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof Suite) {
            return name.equals(((Suite)other).name);
        }
        return false;
    }

    /**
     * Returns a hashcode for this suite which is derived solely from the
     * suite's name.
     *
     * @return  the hashcode of this suite's name
     */
    public final int hashCode() {
        return name.hashCode();
    }

    /**
     * Gets the Suite corresponding to a given URI, loading it if necessary.
     *
     * @param uri   the URI identifying the object memory
     * @return the Suite inside the object memory identified by <code>uri</code>
     * @throws LinkageError if the suite denoted by URI is not available or there was
     *         a problem while loading it
     */
    public static Suite getSuite(String uri) {
        ObjectMemory om = GC.lookupReadOnlyObjectMemoryBySourceURI(uri);
        if (om == null) {
            try {
                DataInputStream dis = Connector.openDataInputStream(uri);
                om = ObjectMemoryLoader.load(dis, uri, true).objectMemory;
                dis.close();
            } catch (IOException e) {
                throw new Error("IO error while loading suite from '" + uri + "': " + e);
            }
        }
        Object root = om.getRoot();
        if (!(root instanceof Suite)) {
            throw new Error("object memory in '" + om.getURI() + "' does not contain a suite");
        }
        return (Suite)root;
    }

    /**
     * Describes the configuration of the suite.
     */
    private String configuration;

    /**
     * Gets the configuration of the suite.
     *
     * @return the configuration of the suite
     */
    public String getConfiguration() {
        if (configuration == null) {
            return "complete symbolic information available";
        }
        return configuration;
    }

    /**
     * Serializes the object graph rooted by this suite and writes it to a given stream.
     * The endianess of the serialized object graph is the endianess of the unerdlying platform.
     *
     * @param  dos       the DataOutputStream to which the serialized suite should be written
     * @param  uri       the URI identifier of the serialized suite
     *
     * @throws OutOfMemoryError if there was insufficient memory to do the save
     * @throws IOException if there was some IO problem while writing the output
     */
    public void save(DataOutputStream dos, String uri) throws java.io.IOException {
        save(dos, uri, VM.isBigEndian());
    }

    /**
     * Serializes the object graph rooted by this suite and writes it to a given stream.
     *
     * @param  dos       the DataOutputStream to which the serialized suite should be written
     * @param  uri       the URI identifier of the serialized suite
     * @param  bigEndian the endianess to be used when serializing this suite
     *
     * @throws OutOfMemoryError if there was insufficient memory to do the save
     * @throws IOException if there was some IO problem while writing the output
     */
    public void save(DataOutputStream dos, String uri, boolean bigEndian) throws java.io.IOException {
        ObjectMemorySerializer.ControlBlock cb = VM.copyObjectGraph(this);
        ObjectMemory parentMemory = null;
        if (!isBootstrap()) {
            parentMemory = parent.getReadOnlyObjectMemory();
            Assert.that(parentMemory != null);
        }
        ObjectMemorySerializer.save(dos, uri, cb, parentMemory, bigEndian);

        if (VM.isHosted()) {
            saveHosted(uri, cb, parentMemory);
        }
    }

    /**
     * Serializes the object graph rooted by this suite and writes it to a given stream.
     * FIXME: what does this method REALLY do?
     *
     * @param  uri       the URI identifier of the serialized suite
     */
    private void saveHosted(String uri, ObjectMemorySerializer.ControlBlock cb, ObjectMemory parentMemory) throws HostedPragma {
        Address start = parentMemory == null ? Address.zero() : parentMemory.getEnd();
        int hash = ObjectMemoryLoader.hash(cb.memory);
        ObjectMemory om = new ObjectMemory(start, cb.memory.length, uri, this, hash, parentMemory);
        GC.registerReadOnlyObjectMemory(om);
    }

    /**
     * Denotes a suite that encapsulates an application. The classes of an application
     * can not be linked against.
     */
    public static final int APPLICATION = 0;

    /**
     * Denotes a suite that encapsulates a library. The classes of a library
     * can be linked against but the library itself cannot be extended by virtue
     * of other classes linking against it's package private components.
     */
    public static final int LIBRARY = 1;

    /**
     * Denotes a suite that encapsulates an open library. The classes of an open library
     * can be linked against and the library itself can be extended by virtue
     * of other classes linking against it's package private components.
     */
    public static final int EXTENDABLE_LIBRARY = 2;

    /**
     * Denotes a suite that is being debugged. This suite retains all its symbolic information
     * when closed.
     */
    public static final int DEBUG = 3;

    /**
     * Denotes the name of the resource that represents the resource name from which I extract
     * properties from when an {@link #installResource(ResourceFile)} is done.
     */
    public static final String PROPERTIES_MANIFEST_RESOURCE_NAME = "META-INF/MANIFEST.MF";
    
    /**
     * Closes this suite. Once closed, a suite is immutable (and may well reside in
     * read-only memory) and cannot have any more classes installed in it
     */
    public void close() {
        closed = true;
    }

    /**
     * Creates a copy of this suite with its symbolic information stripped according to
     * the given parameters.
     *
     * @param type  specifies the type of the suite after closing. Must be
     *              {@link #APPLICATION}, {@link #LIBRARY}, {@link #EXTENDABLE_LIBRARY} or {@link #DEBUG}.
     */
    public Suite strip(int type) {
        if (type < APPLICATION || type > DEBUG) {
            throw new IllegalArgumentException();
        }

        Suite copy = new Suite(name, parent);

        copy.classes = new Klass[classes.length];
        System.arraycopy(classes, 0, copy.classes, 0, classes.length);
        
        copy.noClassDefFoundClassesString = noClassDefFoundClassesString;

        copy.resourceFiles = new ResourceFile[resourceFiles.length];
        System.arraycopy(resourceFiles, 0, copy.resourceFiles, 0, resourceFiles.length);

		copy.manifestProperties = new ManifestProperty [manifestProperties.length];
		System.arraycopy(manifestProperties, 0, copy.manifestProperties, 0, manifestProperties.length);

        copy.metadatas = KlassMetadata.strip(this, metadatas, type);
        copy.updateConfiguration(type);
        return copy;
    }

    /**
     * Updates the configuration description of this suite based on the parameters that it
     * is {@link #strip stripped} with.
     */
    private void updateConfiguration(int type) {
        if (type == DEBUG) {
            configuration = "symbols not stripped";
        } else {
            configuration = "symbols stripped in ";
            switch (type) {
                case APPLICATION: configuration += "application"; break;
                case LIBRARY: configuration += "library"; break;
                case EXTENDABLE_LIBRARY: configuration += "extendable library"; break;
            }
            configuration += " mode";
        }
    }

    /*---------------------------------------------------------------------------*\
     *                            API Printing                                   *
    \*---------------------------------------------------------------------------*/

    /**
     * Prints a textual description of the components in this suite that can be linked
     * against. That is, the components whose symbolic information has not been stripped.
     *
     * @param out where to print the description
     */
    public void printAPI(PrintStream out) {

        out.println(".suite " + name);
        for (int i = 0; i != classes.length; ++i) {
            Klass klass = classes[i];
            KlassMetadata metadata;
            if (klass == null ||
                klass.isSynthetic() ||
                klass.isSourceSynthetic() ||
                klass == Klass.STRING_OF_BYTES ||
                isAnonymousOrPrivate(klass.getName()) ||
                (metadata = getMetadata(klass)) == null)
            {
                continue;
            }

            out.println(".class " + klass.getName());
            printFieldsAPI(out, metadata, SymbolParser.STATIC_FIELDS);
            printFieldsAPI(out, metadata, SymbolParser.INSTANCE_FIELDS);
            printMethodsAPI(out, metadata, SymbolParser.STATIC_METHODS);
            printMethodsAPI(out, metadata, SymbolParser.VIRTUAL_METHODS);
        }
    }

    private static boolean isAnonymousOrPrivate(String className) {
        int index = className.indexOf('$');
        if (index == -1) {
            return false;
        }
        if (className.length() > index + 1) {
            char c = className.charAt(index + 1);
            return c >= '0' && c <= '9';
        }
        return false;
    }

    private void printFieldsAPI(PrintStream out, KlassMetadata klass, int category) {
        SymbolParser symbols = klass.getSymbolParser();
        int count = symbols.getMemberCount(category);
        for (int i = 0; i != count; ++i) {
            int id = symbols.getMemberID(category, i);
            Field field = new Field(klass, id);
            if (!field.isSourceSynthetic()) {
                out.println("    .field " + field.getName() + ' ' + field.getType().getSignature());
            }
        }
    }

    private void printMethodsAPI(PrintStream out, KlassMetadata klass, int category) {
        SymbolParser symbols = klass.getSymbolParser();
        int count = symbols.getMemberCount(category);
        for (int i = 0; i != count; ++i) {
            int id = symbols.getMemberID(category, i);
            Method method = new Method(klass, id);

            if (method.isNative() && !VM.isLinkableNativeMethod(method.getFullyQualifiedName())) {
                continue;
            }

            if (method.isInterpreterInvoked()) {
                continue;
            }

            if (method.isSourceSynthetic() || method.isClassInitializer()) {
                continue;
            }

            out.print("    .method " + method.getName() + " (");
            Klass[] types = method.getParameterTypes();
            for (int j = 0; j != types.length; ++j) {
                Klass type = types[j];
                out.print(type.getSignature());
            }
            out.println(")" + (method.isConstructor() ? "V" : method.getReturnType().getSignature()));
        }
    }
}
