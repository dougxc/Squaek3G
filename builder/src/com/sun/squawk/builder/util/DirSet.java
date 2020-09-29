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
import java.util.List;
import java.util.ArrayList;

/**
 * A FileSet instance represents a selection of directories under a given directory that match a criteria
 * defined by a {@link File.Selector}.
 *
 * @author Doug Simon
 */
public class DirSet extends FileSet {

    /**
     * Creates a FileSet.
     *
     * @param baseDir   the base directory for finding directories in the set.
     * @param selector  the selector for matching files in the set if all directories in the directory are to be matched
     */
    public DirSet(File baseDir, Selector selector) {
        super(baseDir, selector);
    }

    /**
     * Creates a FileSet.
     *
     * @param baseDir    the base directory for finding directories in the set.
     * @param expression an expression that is {@link SelectionExpressionParser parsed} to create a Selector
     */
    public DirSet(File baseDir, String expression) {
        super(baseDir, expression);
    }

    /**
     * Gets the directories in the set as a list.
     *
     * @return the directories in the set
     */
    public List list() {
        final ArrayList list = new ArrayList();
        final Selector selector = getSelector();
        new FileVisitor() {
            public boolean visit(File file) {
                if (file.isDirectory()) {
                    if (selector == null || selector.isSelected(file)) {
                        list.add(file);
                    }
                }
                return true;
            }
        }.run(getBaseDir());
        return list;
    }


}
