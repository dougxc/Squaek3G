/*MAKE_ASSERTIONS_FATAL[true]*/
/*
 * Copyright 2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk;

import com.sun.squawk.util.*;

/**
 * Stores a resource file (name and contents) in the suite file.
 *
 * @author David Liu
 */
public final class ResourceFile {
	/**
	 * Creates a resource file object.
	 */
	public ResourceFile(String name, byte [] data) {
		this.name = name;
		this.data = data;
	}

	/**
	 * The name of the resource file, for example "example/chess/br.bishop.gif".
	 */
	public final String name;
	
	/**
	 * The contents of the resource file.
	 */
	public final byte [] data;
	
	/**
	 * Comparator for ResourceFile objects (which are sorted by the resource files).
	 */
	public final static Comparer comparer = new Comparer() {
		/**
		 * Compares either two ResourceFile objects, or a ResourceFile object and a String object for their relative ordering based on their file names.
		 *
		 * @param o1 a ResourceFile object
		 * @param o2 either a ResourceFile object or a String object
		 */
		public int compare(Object o1, Object o2) {
			if (o1 == o2 || !(o1 instanceof ResourceFile)) {
				return 0;
			}
			
			if (o2 instanceof ResourceFile) {
				return ((ResourceFile) o1).name.compareTo(((ResourceFile) o2).name);
			} else if (o2 instanceof String) {
				return ((ResourceFile) o1).name.compareTo((String) o2);
			} 
			
			return 0;
		}
	};
}
