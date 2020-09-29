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
import javax.lang.model.type.*;

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.JCTree.Visitor;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.List;
import java.util.regex.*;

/**
 * Converts a Java method to C functions.
 *
 * @author Doug Simon
 */
public class MethodConverter extends JCTree.Visitor {

    /**
     * The method being converted.
     */
    private final ProcessedMethod method;

    /**
     * A hashtable mapping the first statement in a block to the set of variable
     * declarations local to the block.
     */
    private final Map<JCTree, List<VarSymbol>> blockLocalDecls;

    /**
     * The conversion context.
     */
    private final Converter conv;

    /**
     * The buffer for the generated C functions.
     */
    private CCodeBuffer ccode;

    /**
     * Resolves character positions to source file line numbers.
     */
    private final LineNumberTable lnt;

    /**
     * Creates the converter for a given Java compilation unit (i.e. a Java source file).
     *
     * @param conv  the context in which multiple compilation units are being converted
     */
    public MethodConverter(ProcessedMethod method, Converter conv, LineNumberTable lnt) {
        this.method = method;
        this.conv = conv;
        this.lnt = lnt;
        this.blockLocalDecls = new LocalVarDeclScanner().run(method.tree);
        ccode = new CCodeBuffer();
    }

    /**
     * Converts the method to a C function.
     *
     * @return the C function definition
     * @throws InconvertibleNodeException if there was a construct in the method that could not be converted
     */
    public String convert() {
        visitMethodDef(method.tree);
        return ccode.toString();
    }

    static void inconvertible(JCTree inconvertibleNode, String desc) {
        String message = "converter cannot handle " + desc;
        throw new InconvertibleNodeException(inconvertibleNode, message);
    }

    /**************************************************************************
     * Traversal methods
     *************************************************************************/

    /** Visitor argument: the current precedence level.
     */
    int parentsPrec = TreeInfo.notExpression;
    int prec = TreeInfo.notExpression;

    /**
     * Visitor argument: denotes if the current expression is an lvalue or rvalue
     */
    boolean lvalue;

    /** Visitor method: traverse expression tree.
     *  @param prec  The current precedence level.
     */
    public void doExpr(JCTree tree, int prec, boolean lvalue) {
        int prevParentsPrec = this.parentsPrec;
        int prevPrec = this.prec;
        boolean prevLvalue = this.lvalue;
        try {
            this.parentsPrec = this.prec;
            this.prec = prec;
            this.lvalue = lvalue;
            if (tree == null)
                ccode.print("/*missing*/");
            else {
                tree.accept(this);
            }
        } catch (Error e) {
            // This is only for debugging the converter
            conv.log.rawError(tree.getPreferredPosition(), e.toString());
            e.printStackTrace();
            System.exit(1);
        } catch (RuntimeException e) {
            if (e instanceof InconvertibleNodeException) {
                throw e;
            }
            // This is only for debugging the converter
            conv.log.rawError(tree.getPreferredPosition(), e.toString());
            e.printStackTrace();
            System.exit(1);
        } finally {
            this.parentsPrec = prevParentsPrec;
            this.prec = prevPrec;
            this.lvalue = prevLvalue;
        }
    }

    public void doExpr(JCTree tree, int prec) {
        doExpr(tree, prec, false);
    }

    public String exprToString(JCExpression tree, int prec, boolean lvalue) {
        ccode = ccode.enter();
        doExpr(tree, prec, lvalue);
        String value = ccode.toString();
        ccode = ccode.leave();
        return value;
    }

    /** Derived visitor method: print expression tree at minimum precedence level
     *  for expression.
     */
    public void doExpr(JCTree tree) {
        doExpr(tree, tree instanceof JCExpression ? TreeInfo.noPrec : TreeInfo.notExpression, false);
    }

    /** Derived visitor method: print statement tree.
     */
    public void doStatement(JCTree tree) {
        doExpr(tree, TreeInfo.notExpression, false);
    }

    /** Derived visitor method: print list of expression trees, separated by given string.
     *  @param sep the separator string
     */
    public <T extends JCTree> void doExprs(List<T> trees, String sep) {
        if (trees.nonEmpty()) {
            doExpr(trees.head);
            for (List<T> l = trees.tail; l.nonEmpty(); l = l.tail) {
                ccode.print(sep);
                doExpr(l.head);
            }
        }
    }

    /** Derived visitor method: print list of expression trees, separated by commas.
     */
    public <T extends JCTree> void doExprs(List<T> trees) {
        doExprs(trees, ", ");
    }

    /** Derived visitor method: print list of statements, each on a separate line.
     */
    public void doStatements(List<? extends JCTree> trees, String sep) {
        for (List<? extends JCTree> l = trees; l.nonEmpty(); l = l.tail) {
            ccode.align();
            doStatement(l.head);
            ccode.println();
        }
    }

    /** Print a block.
     */
    public void doBlock(List<? extends JCTree> stats) {
        ccode.println("{");
        ccode.indent();

        if (!stats.isEmpty()) {
            List<VarSymbol> symbols = blockLocalDecls.get(stats.head);
            if (symbols != null) {
                for (VarSymbol sym : symbols) {
                    ccode.align();
                    printVarDecl(sym);
                    ccode.print(";");
                    ccode.println();
                }
            }
        }

        doStatements(stats, ccode.LINE_SEP);
        ccode.undent();
        ccode.align();
        ccode.print("}");
    }

    public void printVarDecl(VarSymbol var) {
        ccode.print(conv.asString(var.type));
        ccode.print(" " + conv.subVarName(var.name));
    }


    /**************************************************************************
     * Visitor methods
     *************************************************************************/

    public void visitClassDef(JCClassDecl tree) {
        inconvertible(tree, "method-local class definition");
    }

    private boolean isVirtual(MethodSymbol method) {
        return !method.isStatic() &&
            (method.flags() & Flags.FINAL) == 0 &&
            (method.enclClass().flags() & Flags.FINAL) == 0;
    }

    public void visitMethodDef(JCMethodDecl tree) {

        Map<String, String> annotations = new AnnotationParser().parse(method);
        String cRoot = annotations.get("root");
        boolean isRoot = cRoot != null;
        MethodSymbol method = this.method.sym;
        assert tree.sym == method;
        List<ProcessedMethod> impls = conv.getImplementersOf(method);

        String code = annotations.get("code");
        String proxy = annotations.get("proxy");
        String macro = annotations.get("macro");
        if (code == null && proxy == null && macro == null && impls.isEmpty()) {
            if (tree.name == tree.name.table.init) {
                inconvertible(tree, "constructor");
            }

            if (tree.body == null) {
                inconvertible(tree, "method with no body");
            }

            if (isVirtual(method)) {
                inconvertible(tree, "virtual method");
            }
        }

        ccode.align();
        ccode.print(AnnotationParser.getDocComment(this.method.unit, tree, ccode.lmargin));

        if (macro != null) {
            ccode.print("#define ");
            ccode.print(" " + conv.asString(method));
        } else {
            ccode.print("static ");
            ccode.print(conv.asString(tree.sym.getReturnType()));
            ccode.print(" " + (isRoot ? cRoot : conv.asString(method)));
        }
        ccode.print("(");

        ccode.print(conv.getReceiverDecl(tree.sym, false));
        if (macro != null) {
            int params = method.params().size();
            for (VarSymbol param : method.params()) {
                ccode.print(" " + conv.subVarName(param.name));
                if (--params != 0) {
                    ccode.print(", ");
                }
            }
        } else {
            doExprs(tree.params);
        }
        ccode.print(") ");

        if (!impls.isEmpty()) {
            this.dispatchAbstractMethod(tree, impls);
        } else if (proxy != null) {
            ccode.println("{");
            ccode.indent();
            StringBuilder buf = new StringBuilder();
            if (tree.sym.getReturnType().getKind() != TypeKind.VOID) {
                buf.append("return ");
            }
            buf.append(proxy.length() == 0 ? method.name.toString() : proxy);
            buf.append(makePassThroughInvocation(tree)).append(';');
            ccode.aprintln(buf);
            ccode.undent();
            ccode.aprintln("}");
        } else if (code != null) {
            // A sanity check as some C compilers quietly accept non-void functions without a return statement!
            boolean hasReturn = false;
            String[] idents = code.split("\\W+");
            for (String ident: idents) {
                if (ident.equals("return")) {
                    hasReturn = true;
                    break;
                }
            }
            if (method.getReturnType().getKind() != TypeKind.VOID && !hasReturn) {
                throw new InconvertibleNodeException(tree, "code annotation for non-void function does not include a return statement");
            }
            ccode.printFunctionBody(code);
        } else if (macro != null) {
            ccode.printMacroBody(macro);
        } else {
            doStatement(tree.body);
        }
    }

    private String makePassThroughInvocation(JCMethodDecl tree) {
        StringBuilder buf = new StringBuilder();
        buf.append('(');

        int params = tree.params.size();
        if (!tree.sym.isStatic()) {
            buf.append("this");
            if (params != 0) {
                buf.append(", ");
            }
        }
        for (JCVariableDecl var: tree.params) {
            buf.append(var.name.toString());
            if (--params != 0) {
                buf.append(", ");
            }
        }
        return buf.append(")").toString();
    }

    private void dispatchAbstractMethod(JCMethodDecl tree, List<ProcessedMethod> impls) {

        String invocation = makePassThroughInvocation(tree);
        String ret = tree.sym.getReturnType().getKind() != TypeKind.VOID ? "return " : "";

        ccode.println("{");
        ccode.indent();
        ccode.aprintln("Address klass = getClass(this);");
        ccode.aprintln("int id = com_sun_squawk_Klass_id(klass);");
        ccode.aprintln("int suiteID = id >= 0 ? id : -(id + 1);");
        ccode.aprintln("switch (suiteID) {");
        ccode.indent();
        for (ProcessedMethod impl: impls) {
            ccode.aprintln("case " + conv.asString(impl.sym.enclClass()) + ": " + ret + conv.asString(impl.sym) + invocation + ";");
        }
        ccode.aprintln("default: fatalVMError(\"bad abstract method dispatch\"); ");
        ccode.undent();
        ccode.aprintln("}");
        ccode.undent();
        ccode.aprintln("}");
    }

    public void visitVarDef(JCVariableDecl tree) {
        if (tree.sym.isLocal()) {
            // A parameter
            if ((tree.sym.flags() & Flags.PARAMETER) != 0) {
                printVarDecl(tree.sym);
            } else {
                // A local variable
                if (tree.init != null) {
                    ccode.print(conv.subVarName(tree.name));
                    ccode.print(" = ");
                    doExpr(tree.init);
                    if (prec == TreeInfo.notExpression && !inForInitOrStep) {
                        ccode.print(";");
                    }
                }
            }
        }
    }

    public void visitSkip(JCSkip tree) {
        ccode.print(";");
    }

    public void visitBlock(JCBlock tree) {
        if ((tree.flags & Flags.STATIC) != 0) {
            // This is a static initialization block
            return;
        }
        doBlock(tree.stats);
    }

    public void visitDoLoop(JCDoWhileLoop tree) {
        ccode.print("do ");
        doStatement(tree.body);
        ccode.align();
        ccode.print(" while ");
        if (tree.cond.tag == JCTree.PARENS) {
            doExpr(tree.cond);
        } else {
            ccode.print("(");
            doExpr(tree.cond);
            ccode.print(")");
        }
        ccode.print(";");
    }

    public void visitWhileLoop(JCWhileLoop tree) {
        ccode.print("while ");
        if (tree.cond.tag == JCTree.PARENS) {
            doExpr(tree.cond);
        } else {
            ccode.print("(");
            doExpr(tree.cond);
            ccode.print(")");
        }
        ccode.print(" ");
        doStatement(tree.body);
    }

    boolean inForInitOrStep;

    public void visitForLoop(JCForLoop tree) {
        ccode.print("for (");
        if (tree.init.nonEmpty()) {
            assert inForInitOrStep == false;
            inForInitOrStep = true;
            try {
                if (tree.init.head.tag == JCTree.VARDEF) {
                    doExpr(tree.init.head);
                    for (List<JCStatement> l = tree.init.tail; l.nonEmpty(); l = l.tail) {
                        JCVariableDecl vdef = (JCVariableDecl) l.head;
                        ccode.print(", " + vdef.name + " = ");
                        doExpr(vdef.init);
                    }
                } else {
                    doExprs(tree.init);
                }
            } finally {
                inForInitOrStep = false;
            }
        }
        ccode.print("; ");
        if (tree.cond != null) {
            doExpr(tree.cond);
        }
        ccode.print("; ");
        assert inForInitOrStep == false;
        inForInitOrStep = true;
        try {
            doExprs(tree.step);
        } finally {
            inForInitOrStep = false;
        }
        ccode.print(") ");
        doStatement(tree.body);
    }

    public void visitForeachLoop(JCEnhancedForLoop tree) {
        inconvertible(tree, "enhanced for loop");
    }

    public void visitLabelled(JCLabeledStatement tree) {
        inconvertible(tree, "label");
    }

    public void visitSwitch(JCSwitch tree) {
        ccode.print("switch ");
        if (tree.selector.tag == JCTree.PARENS) {
            doExpr(tree.selector);
        } else {
            ccode.print("(");
            doExpr(tree.selector);
            ccode.print(")");
        }
        ccode.println(" {");
        doStatements(tree.cases, ccode.LINE_SEP);
        ccode.align();
        ccode.print("}");
    }

    public void visitCase(JCCase tree) {
        if (tree.pat == null) {
            ccode.print("default");
        } else {
            ccode.print("case ");
            doExpr(tree.pat);
        }
        ccode.println(": ");
        ccode.indent();
        doStatements(tree.stats, ccode.LINE_SEP);
        ccode.undent();
        ccode.align();
    }

    public void visitSynchronized(JCSynchronized tree) {
        inconvertible(tree, "synchronized block");
    }

    public void visitTry(JCTry tree) {
        inconvertible(tree, "try block");
    }

    public void visitCatch(JCCatch tree) {
        inconvertible(tree, "catch statement");
    }

    public void visitConditional(JCConditional tree) {
        ccode.open(prec, TreeInfo.condPrec);
        doExpr(tree.cond, TreeInfo.condPrec);
        ccode.print(" ? ");
        doExpr(tree.truepart, TreeInfo.condPrec);
        ccode.print(" : ");
        doExpr(tree.falsepart, TreeInfo.condPrec);
        ccode.close(prec, TreeInfo.condPrec);
    }

    public void visitIf(JCIf tree) {
        boolean doThen = !tree.cond.type.isFalse();
        boolean doElse = !tree.cond.type.isTrue();

        if (doThen) {
            ccode.print("if ");
            if (tree.cond.tag == JCTree.PARENS) {
                doExpr(tree.cond);
            }
            else {
                ccode.print("(");
                doExpr(tree.cond);
                ccode.print(")");
            }
            ccode.print(" ");
            doStatement(tree.thenpart);
        }
        if (doElse && tree.elsepart != null) {
            if (doThen) {
                ccode.print(" else ");
            }
            doStatement(tree.elsepart);
        }
    }

    public void visitExec(JCExpressionStatement tree) {
        doExpr(tree.expr);
        if (prec == TreeInfo.notExpression && !inForInitOrStep) {
            ccode.print(";");
        }
    }

    public void visitBreak(JCBreak tree) {
        ccode.print("break");
        if (tree.label != null) {
            inconvertible(tree, "labelled break");
        }
        ccode.print(";");
    }

    public void visitContinue(JCContinue tree) {
        ccode.print("continue");
        if (tree.label != null) {
            inconvertible(tree, "labelled continue");
        }
        ccode.print(";");
    }

    public void visitReturn(JCReturn tree) {
        ccode.print("return");
        if (tree.expr != null) {
            ccode.print(" ");
            doExpr(tree.expr);
        }
        ccode.print(";");
    }

    public void visitThrow(JCThrow tree) {
        ccode.print("fatalVMError(\"" + tree.expr.type.tsym.getQualifiedName() + "\");");
    }

    public void visitAssert(JCAssert tree) {
        inconvertible(tree, "assertion");
    }

    public void visitApply(JCMethodInvocation tree) {
        MethodSymbol method = (MethodSymbol)Converter.getSymbol(tree.meth);

        if ((method.flags() & Flags.ABSTRACT) != 0) {
            List<ProcessedMethod> impls = conv.getImplementersOf(method);
            if (impls.isEmpty()) {
                inconvertible(tree, "abstract method invocation");
            }
        } else {
            if (isVirtual(method)) {
                inconvertible(tree, "virtual method invocation");
            }
        }
        if (method.isStatic()) {
            ccode.print(conv.asString(method));
            ccode.print("(");
        } else {
            String receiver;
            if (tree.meth.tag == JCTree.IDENT) {
                receiver = "this";
            } else {
                receiver = exprToString(tree.meth, TreeInfo.noPrec, false);
                Type receiverType = method.enclClass().asType();
                if (conv.isReferenceType(receiverType)) {
                    receiver = "nullPointerCheck(" + receiver + ')';
                }
            }

            ccode.print(conv.asString(method));
            ccode.print("(" + receiver);
            if (!tree.args.isEmpty()) {
                ccode.print(", ");
            }
        }
        doExprs(tree.args);
        ccode.print(")");
    }

    public void visitNewClass(JCNewClass tree) {
        inconvertible(tree, "method-local class definition");
    }

    public void visitNewArray(JCNewArray tree) {
        inconvertible(tree, "array allocator");
    }

    public void visitParens(JCParens tree) {
        ccode.print("(");
        doExpr(tree.expr);
        ccode.print(")");
    }

    public void visitAssign(JCAssign tree) {
        Symbol lhsSym = Converter.getSymbol(tree.lhs);
        boolean lhsLocal = lhsSym.isLocal();
        boolean lhsGlobal = !lhsLocal && conv.isGlobalVariable(lhsSym);

        if (parentsPrec != TreeInfo.notExpression && !lhsLocal && !lhsGlobal) {
            inconvertible(tree, "assignment to non-local, non-global variable as an expression");
        }

        ccode.open(prec, TreeInfo.assignPrec);
        doExpr(tree.lhs, TreeInfo.assignPrec + 1, true);

        if (lhsSym.isStatic() || lhsLocal) {
            ccode.print(" = ");
        }
        doExpr(tree.rhs, TreeInfo.assignPrec);
        if (!lhsSym.isStatic() && !lhsGlobal && !lhsLocal) {
            ccode.print(')');
        }
        ccode.close(prec, TreeInfo.assignPrec);
    }

    public String operatorName(int tag) {
        switch (tag) {
            case JCTree.POS:
                return "+";
            case JCTree.NEG:
                return "-";
            case JCTree.NOT:
                return "!";
            case JCTree.COMPL:
                return "~";
            case JCTree.PREINC:
                return "++";
            case JCTree.PREDEC:
                return "--";
            case JCTree.POSTINC:
                return "++";
            case JCTree.POSTDEC:
                return "--";
            case JCTree.NULLCHK:
                return "<*nullchk*>";
            case JCTree.OR:
                return "||";
            case JCTree.AND:
                return "&&";
            case JCTree.EQ:
                return "==";
            case JCTree.NE:
                return "!=";
            case JCTree.LT:
                return "<";
            case JCTree.GT:
                return ">";
            case JCTree.LE:
                return "<=";
            case JCTree.GE:
                return ">=";
            case JCTree.BITOR:
                return "|";
            case JCTree.BITXOR:
                return "^";
            case JCTree.BITAND:
                return "&";
            case JCTree.SL:
                return "<<";
            case JCTree.SR:
                return ">>";
            case JCTree.USR:
                return ">>>";
            case JCTree.PLUS:
                return "+";
            case JCTree.MINUS:
                return "-";
            case JCTree.MUL:
                return "*";
            case JCTree.DIV:
                return "/";
            case JCTree.MOD:
                return "%";
            default:
                throw new Error();
        }
    }

    public void visitAssignop(JCAssignOp tree) {
        if (parentsPrec != TreeInfo.notExpression && !Converter.getSymbol(tree.lhs).isLocal()) {
            inconvertible(tree, "compound assignment to non-local variable as an expression");
        }

        ccode.open(prec, TreeInfo.assignopPrec);
        TreeMaker maker = TreeMaker.instance(conv.context);
        maker.pos = tree.pos;
        JCBinary binary = maker.Binary(tree.tag - JCTree.ASGOffset, tree.lhs, tree.rhs);
        JCAssign assign = maker.Assign(tree.lhs, maker.Parens(binary));

        // Visit the replacement node directly without going through doExpr so that
        // the precedence level is preserved
        visitAssign(assign);

        ccode.close(prec, TreeInfo.assignopPrec);
    }

    public void visitUnary(JCUnary tree) {
        assert tree.arg.type.getKind() != TypeKind.FLOAT && tree.arg.type.getKind() != TypeKind.DOUBLE;
        String opname = operatorName(tree.tag).toString();
        if (tree.tag >= JCTree.PREINC && tree.tag <= JCTree.POSTDEC && !Converter.getSymbol(tree.arg).isLocal()) {
            inconvertible(tree, "side-effecting unary operator '" + opname + "' applied to non-local variable");
        }
        int ownprec = TreeInfo.opPrec(tree.tag);
        ccode.open(prec, ownprec);
        if (tree.tag <= JCTree.PREDEC) {
            ccode.print(opname);
            doExpr(tree.arg, ownprec);
        } else {
            doExpr(tree.arg, ownprec);
            ccode.print(opname);
        }
        ccode.close(prec, ownprec);
    }

    public void visitBinary(JCBinary tree) {

        int ownprec = TreeInfo.opPrec(tree.tag);
        String opname = operatorName(tree.tag).toString();

        ccode.open(prec, ownprec);

        JCExpression lhs = tree.lhs;
        JCExpression rhs = tree.rhs;

        if (tree.tag == JCTree.PLUS && (!lhs.type.isPrimitive() || !rhs.type.isPrimitive())) {
            inconvertible(tree, "string concatenation");
        }

        if (lhs.type.getKind() == TypeKind.FLOAT || rhs.type.getKind() == TypeKind.FLOAT ||
            lhs.type.getKind() == TypeKind.DOUBLE || rhs.type.getKind() == TypeKind.DOUBLE)
        {
            inconvertible(tree, "float or double operations");
        }

        boolean isLong = (lhs.type.getKind() == TypeKind.LONG);
        boolean infix = true;
        switch (tree.tag) {
            case JCTree.PLUS:
            case JCTree.MINUS:
            case JCTree.MUL:
            case JCTree.OR:
            case JCTree.AND:
            case JCTree.BITOR:
            case JCTree.BITXOR:
            case JCTree.BITAND:
            case JCTree.EQ:
            case JCTree.NE:
            case JCTree.LT:
            case JCTree.GT:
            case JCTree.LE:
            case JCTree.GE: {
                break;
            }
            case JCTree.SL: {
                ccode.print(isLong ? "slll" : "sll");
                infix = false;
                break;
            }
            case JCTree.SR: {
                ccode.print(isLong ? "sral" : "sra");
                infix = false;
                break;
            }
            case JCTree.USR: {
                ccode.print(isLong ? "srll" : "srl");
                infix = false;
                break;
            }
            case JCTree.DIV: {
                if (isLong) {
                    ccode.print("div_l");
                } else {
                    ccode.print("div_i");
                }
                infix = false;
                break;
            }
            case JCTree.MOD: {
                if (isLong) {
                    ccode.print("rem_l");
                } else {
                    ccode.print("rem_i");
                }
                infix = false;
                break;
            }
            default:
                assert false;
        }

        if (!infix) {
            ccode.print("(");
        }

        doExpr(lhs, ownprec);
        if (infix) {
            ccode.print(" " + opname + " ");
        } else {
            ccode.print(", ");
        }
        doExpr(rhs, ownprec);

        if (!infix) {
            ccode.print(")");
        }

        ccode.close(prec, ownprec);
    }

    public void visitTypeCast(JCTypeCast tree) {
        ccode.open(prec, TreeInfo.prefixPrec);
        ccode.print("(");
        ccode.print(conv.asString(tree.type));
        ccode.print(")");
        doExpr(tree.expr, TreeInfo.prefixPrec);
        ccode.close(prec, TreeInfo.prefixPrec);
    }

    public void visitTypeTest(JCInstanceOf tree) {
        inconvertible(tree, "instanceof");
    }

    public void visitIndexed(JCArrayAccess tree) {
        char componentType;
        switch (tree.type.getKind()) {
            case BOOLEAN:
            case BYTE:      componentType = 'b'; break;
            case CHAR:
            case SHORT:     componentType = 's'; break;
            case INT:       componentType = 'i'; break;
            case LONG:      componentType = 'l'; break;
            case FLOAT:     componentType = 'f'; break;
            case DOUBLE:    componentType = 'd'; break;
            default: {
                if (conv.isReferenceType(tree.type)) {
                    componentType = 'o';
                } else {
                    componentType = 'i';
                }
                break;
            }
        }

        String indexed = exprToString(tree.indexed, TreeInfo.noPrec, false);
        String index = exprToString(tree.index, TreeInfo.noPrec, false);

        if (lvalue) {
            if (componentType == 'o') {
                inconvertible(tree, "object array store");
            }
            ccode.print("astore_" + componentType + '(' + indexed + ", " + index + ", ");
        } else {
            ccode.print("aload_" + componentType + '(' + indexed + ", " + index + ')');
        }
    }

    public void visitSelect(JCFieldAccess tree) {
        Symbol sym = tree.sym;
        if (sym instanceof VarSymbol) {
            VarSymbol var = (VarSymbol)sym;
            String object = null;
            if (!var.isStatic()) {
                object = exprToString(tree.selected, TreeInfo.noPrec, false);
                if (conv.isReferenceType(var.type)) {
                    object = "nullPointerCheck(" + object + ")";
                }
            }
            ccode.print(conv.asString(tree, var, lvalue, object));
        } else {
            doExpr(tree.selected, TreeInfo.postfixPrec);
        }
    }

    public void visitIdent(JCIdent tree) {
        assert tree.sym != null;

        if (tree.sym instanceof VarSymbol) {
            VarSymbol var = (VarSymbol)tree.sym;
            String object = null;
            if (!var.isStatic() && !var.isLocal()) {
                object = "this";
            }
            ccode.print(conv.asString(tree, var, lvalue, object));
        } else {
            ccode.print(conv.asString((MethodSymbol) tree.sym));
        }
    }

    public void visitLiteral(JCLiteral tree) {
        switch (tree.typetag) {
            case TypeTags.INT:
                ccode.print(tree.value.toString());
                break;
            case TypeTags.LONG:
                ccode.print(tree.value + "L");
                break;
            case TypeTags.FLOAT:
                ccode.print(tree.value + "F");
                break;
            case TypeTags.DOUBLE:
                ccode.print(tree.value.toString());
                break;
            case TypeTags.CHAR:
                ccode.print("\'" +
                            Convert.quote(
                                String.valueOf((char) ((Number) tree.value).intValue())) +
                            "\'");
                break;
            default:
                String key = conv.getLiteralKey(method.sym.enclClass(), Convert.quote(tree.value.toString()));
                ccode.print("getObjectForCStringLiteral(" + key + ")");
                break;
        }
    }

    public void visitTypeIdent(JCPrimitiveTypeTree tree) {
        assert false;
    }

    public void visitTypeArray(JCArrayTypeTree tree) {
        inconvertible(tree, "generics");
    }

    public void visitTypeApply(JCTypeApply tree) {
        inconvertible(tree, "generics");
    }

    public void visitTypeParameter(JCTypeParameter tree) {
        inconvertible(tree, "generics");
    }

    public void visitWildcard(JCWildcard tree) {
        inconvertible(tree, "generics");
    }

    public void visitErroneous(JCErroneous tree) {
        ccode.print("(ERROR)");
    }

    public void visitLetExpr(LetExpr tree) {
        inconvertible(tree, "'let' expression");
    }

    public void visitModifiers(JCModifiers mods) {
    }

    public void visitAnnotation(JCAnnotation tree) {
        inconvertible(tree, "annotation");
    }

    public void visitTree(JCTree tree) {
        inconvertible(tree, "unknown construct");
    }
}
