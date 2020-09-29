/*
 * Copyright 2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk.vm2c;

import java.util.*;

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.tree.JCTree.*;

/**
 * A method that has been processed by a <code>CompilationUnitConverter</code>.
 */
public class ProcessedMethod {

    /**
     * Describes a call site.
     */
    public static class CallSite implements Comparable {

        final MethodSymbol callee;
        final MethodSymbol caller;
        final JCMethodInvocation call;

        public CallSite(MethodSymbol callee, MethodSymbol caller, JCMethodInvocation call) {
            assert callee != null;
            assert caller != null;
            assert call != null;
            this.callee = callee;
            this.caller = caller;
            this.call = call;
        }

        /**
         * Orders call sites within a method.
         */
        public int compareTo(Object o) {
            if (o instanceof CallSite) {
                CallSite cs = (CallSite)o;
                return call.getPreferredPosition() - cs.call.getPreferredPosition();
            }
            return -1;
        }

        public boolean equals(Object o) {
            if (o instanceof CallSite) {
                CallSite cs = (CallSite) o;
                return caller == cs.caller && callee == cs.callee && call == cs.call;
            }
            return false;
        }

        public int hashCode() {
            return caller.hashCode() + call.getPreferredPosition();
        }
    }

    /**
     * The method's symbol.
     */
    final MethodSymbol sym;

    /**
     * The AST for this method.
     */
    final JCMethodDecl tree;

    /**
     * The compilation unit in which this method was defined.
     */
    final JCCompilationUnit unit;

    InconvertibleNodeException error;

    /**
     * The methods that this method (directly) calls.
     */
    final Set<CallSite> calls = new TreeSet<CallSite>();

    public ProcessedMethod(MethodSymbol method, JCMethodDecl tree, JCCompilationUnit unit) {
        this.sym = method;
        this.tree = tree;
        this.unit = unit;
    }

    public final boolean equals(Object o) {
        return o instanceof ProcessedMethod && ((ProcessedMethod)o).sym == sym;
    }

    public final int hashCode() {
        return sym.hashCode();
    }

    public String toString() {
        return sym.enclClass().getQualifiedName() + "." + sym;
    }
}
