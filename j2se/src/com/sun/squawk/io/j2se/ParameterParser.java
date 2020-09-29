/*
 * Copyright 2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk.io.j2se;

import java.util.*;

/**
 * Helper class for parsing extra parameters from a connection URL.
 *
 * @author  Doug Simon
 */
public abstract class ParameterParser {

    /**
     * Parses the <name>=<value> pairs separated by ';' in a URL name. The pairs
     * start after the first ';' in the given name.
     *
     * @param name  the name part of a connection URL
     * @return the name stripped of the parameters (if any)
     */
    public String parse(String name) {
        int parmIndex = name.indexOf(';');
        if (parmIndex != -1) {
            String parms = name.substring(parmIndex);
            name = name.substring(0, parmIndex);
            StringTokenizer st = new StringTokenizer(parms, "=;", true);
            while (st.hasMoreTokens()) {
                try {
                    if (!st.nextToken().equals(";")) {
                        throw new NoSuchElementException();
                    }
                    String key = st.nextToken();
                    if (!st.nextToken().equals("=")) {
                        throw new NoSuchElementException();
                    }
                    String value = st.nextToken();
                    if (!parameter(key, value)) {
                        throw new IllegalArgumentException("Unknown parameter to protocol: " + key);
                    }
                } catch (NoSuchElementException nsee) {
                    throw new IllegalArgumentException("Bad param string: " + parms);
                }
            }
        }
        return name;
    }

    /**
     * Notifies a subclass of a <name>=<value> pair that has been parsed.
     *
     * @return true if the parameter was accepted
     *
     * @throws IllegalArgumentException
     */
    public abstract boolean parameter(String name, String value) throws IllegalArgumentException;
}
