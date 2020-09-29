/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk;

import com.sun.squawk.util.*;

/**
 * Key value pair of values found in {@link Suite#PROPERTIES_MANIFEST_RESOURCE_NAME} main section.
 *
 * @author David Liu
 */
public final class ManifestProperty {
	/**
	 * Creates a property object.
	 */
	public ManifestProperty(String name, String value) {
		this.name = name;
		this.value = value;
	}

	/**
	 * The name of the property, for example "MIDlet-Vendor".
	 */
	public final String name;
	
	/**
	 * The value of the property, for example "Sun Microsystems, Inc.".
	 */
	public final String value;
	
	/**
	 * Comparator for IMletProperty objects (which are sorted by the property names).
	 */
	public final static Comparer comparer = new Comparer() {
		/**
		 * Compares either two IMletProperty objects, or an IMletProperty object and a String object for their relative ordering based on the property names.
		 *
		 * @param o1 a IMletProperty object
		 * @param o2 either a IMletProperty object or a String object
		 */
		public int compare(Object o1, Object o2) {
			if (o1 == o2 || !(o1 instanceof ManifestProperty)) {
				return 0;
			}
			
			if (o2 instanceof ManifestProperty) {
				return ((ManifestProperty) o1).name.compareTo(((ManifestProperty) o2).name);
			} else if (o2 instanceof String) {
				return ((ManifestProperty) o1).name.compareTo((String) o2);
			} 
			
			return 0;
		}
	};
}
