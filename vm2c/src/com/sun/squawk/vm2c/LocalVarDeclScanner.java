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
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.util.List;

/**
 * Scans a method's tree to find all the variable declarations local to a block (i.e.
 * a sequence of statements surrounded by '{' and '}'.
 *
 * @author  Doug Simon
 */
public class LocalVarDeclScanner extends TreeScanner {

    private Scope scope;
    private Map<JCTree, List<VarSymbol>> blockDecls;

    /**
     * Parses a class tree to find the block local declarations.
     *
     * @param clazz JCClassDecl
     * @return  a hashtable mapping the first statement in a block
     *          to the set of variable declared in the block
     */
    public Map<JCTree, List<VarSymbol>> run(JCMethodDecl method) {
        blockDecls = new HashMap<JCTree, List<VarSymbol>> ();
        scope = new Scope(method.sym);
        visitMethodDef(method);
        return blockDecls;
    }

    public void visitClassDef(JCClassDecl tree) {
        // skip inner classes
    }

    public void visitMethodDef(JCMethodDecl tree) {
        if (tree.body != null) {
            enter();
            super.visitMethodDef(tree);
            leave(tree.body.stats);
        }
    }

    public void visitBlock(JCBlock tree) {
        enter();
        super.visitBlock(tree);
        leave(tree.stats);
    }

    public void visitVarDef(JCVariableDecl tree) {
        if (tree.sym.isLocal()) {
            if ( (tree.sym.flags() & Flags.PARAMETER) == 0) {
                scope.enter(tree.sym);
            }
        }
    }

    private void enter() {
        scope = scope.dup();
    }

    private void leave(List<? extends JCTree> stats) {
        if (stats != null && !stats.isEmpty() && scope.nelems != 0) {
            List<VarSymbol> symbols = List.nil();
            int nelems = scope.nelems;
            for (Scope.Entry entry = scope.elems; nelems-- != 0; entry = entry.sibling) {
                assert entry.scope == scope;
                assert entry.sym.kind == Kinds.VAR;
                symbols = symbols.prepend( (VarSymbol) entry.sym);
            }
            blockDecls.put(stats.head, symbols);
        }
        scope = scope.leave();
    }
}
