/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: $
 */
package com.sun.squawk.builder.util;

import java.io.File;

/**
 * A FileVisitor provides simple mechanism for traversing a set of files in the file system.
 */
public abstract class FileVisitor {

    /**
     * Exception thrown to terminate a visit.
     */
    static class TerminateVisitException extends Exception {
    }

    private static TerminateVisitException VISIT_TERMINATOR = new TerminateVisitException();

    /**
     * Traverses one or more files in the file system starting with <code>file</code>,
     * invoking the {@link #visit(File)} call back for each file or directory traversed.
     * If <code>file</code> is a directory, then the entries in the directory are visited
     * before the directory itself is visited. If <code>recursive</code> is <code>true</code>,
     * then the traversal recurses over subdirectories.
     *
     * @param file       the starting point of the traversal
     */
    public void run(File file) {
        try {
            run0(file);
        } catch (TerminateVisitException e) {
        }
    }

    private void run0(File file) throws TerminateVisitException {
        if (file.exists()) {
            if (file.isDirectory()) {
                File[] entries = file.listFiles();
                for (int i = 0; i != entries.length; i++) {
                    run0(entries[i]);
                }
            }
            if (!visit(file)) {
                throw VISIT_TERMINATOR;
            }
        }

    }

    /**
     * This method is invoked for every file or directory in a file system traversal.
     *
     * @param file    the file or directory currently being traversed
     * @return true to indicate that the traversal should continue, false to halt it immediately
     */
    public abstract boolean visit(File file);

    /**
     * Tests harness.
     *
     * @param args
     */
    public static void main(String[] args) {
        File file = new File(".");
        if (args.length != 0) {
            file = new File(args[0]);
        }

        FileVisitor visitor = new FileVisitor() {
            public boolean visit(File file) {
                System.out.println(file.getPath());
                return true;
            }
        };

        System.out.println("Recursive traversal:");
        visitor.run(file);
        System.out.println();
    }
}
