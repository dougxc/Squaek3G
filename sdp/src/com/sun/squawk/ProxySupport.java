/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk;

import com.sun.squawk.translator.*;
import com.sun.squawk.pragma.*;


/**
 * Support routines for the Squawk Debugger Proxy (SDP) that must live in
 * package <code>java.lang</code>.
 *
 * @author Derek White
 */
public class ProxySupport {

    /**
     * Creates and initializes the translator.
     *
     * @param classPath   the class search path
     */
    public static void initializeTranslator(String classPath) throws HostedPragma {
        if (VM.getCurrentIsolate() == null) {
            Suite suite = new Suite("-proxy suite-", null);
            VM.setCurrentIsolate(null);
            Isolate isolate = new Isolate(null, null, suite);
            VM.setCurrentIsolate(isolate);
            isolate.setTranslator(new Translator());
            TranslatorInterface translator = isolate.getTranslator();
            translator.open(suite, classPath);
        }
    }
}
