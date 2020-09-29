/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk.builder.util;

import java.util.Properties;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 *
 * Class to substitute the values within a properties file
 *
 * Usage:
 *  base.dir = /usr/local/opt
 *  classes.dir = ${base.dir}/classes
 *
 * NB.  When using store() the resolved property values
 * will be written, not the referential ones.
 *
 * @author Doug Simon, Andrew Crouch
 * @version 1.0
 */
public class SubstitutionProperties extends Properties {

    /**
     * If we loaded the SubstitionProperties from a file,
     * we can refer to which properties file the properties/errors
     * originated.
     */
    private File propertiesFile;

    public SubstitutionProperties() {
        super();
    }

    public SubstitutionProperties(Properties defaults) {
        super(defaults);
    }

    public SubstitutionProperties(File f) throws IOException {
        super();
        load(f);
    }

    /**
     * Load the properties from the given input stream.
     *
     * @param is InputStream  the input stream to load the properties from
     * @throws IOException
     */
    public void load(InputStream is) throws IOException {
        super.load(is);
    }

    private String inFilename() {
        if(propertiesFile == null) {
            return "";
        }

        try {
            return "in " + propertiesFile.getCanonicalFile();
        } catch (IOException ex) {
            return "";
        }
    }

    /**
     * Load the properties from a file
     *
     * @param f File   the file to load the properties from
     * @throws IOException
     */
    public void load(File f) throws IOException {
        try {
            super.load(new FileInputStream(f));
        } catch (FileNotFoundException e) {
            throw new IOException("Couldn't find properties file " + f.getCanonicalFile());
        }

        propertiesFile = f;
    }

    /**
     * Perform the substitution.  Assumes that any property that is referred to
     * has already been declared previously.
     *
     * @param key String
     * @param value String
     * @return Object
     */
    private Object doSubstitution(String key, String value) {
        int start = value.indexOf("${");
        while (start != -1) {
            int end = value.indexOf('}', start + 2);
            if (end == -1) {
                throw new RuntimeException("value for '" + key + "' " + inFilename() + " has badly formatted substitution");
            }
            String var = value.substring(start + 2, end);
            String sub = (String) get(var);
            if (sub == null) {
                throw new RuntimeException("value for '" + key + "' " + inFilename() + " references undefined property '" + var + "'");
            }
//System.out.print(value + " -> ");
            value = value.substring(0, start) + sub + value.substring(end + 1);
//System.out.println(value);
            start = value.indexOf("${");
        }
        return value;
    }

    /**
     * Perform the substituion before putting the key/values into the hashtable.
     *
     * @param key Object
     * @param value Object
     * @return Object
     */
    public Object put(Object key, Object value) {
        value = doSubstitution( (String) key, (String) value);
        return super.put(key, value);
    }
}
