/*
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */

package java.lang.ref;

import com.sun.squawk.*;

/**
 * Abstract base class for reference objects.  This class defines the
 * operations common to all reference objects. Because reference objects are
 * implemented in close cooperation with the garbage collector, this class may
 * not be subclassed directly.
 *
 * @version  12/19/01 (CLDC 1.1)
 * @author   Mark Reinhold
 * @since    JDK1.2, CLDC 1.1
 */

public abstract class Reference {

    /**
     * We had to implement a delegation model here in order to keep the GC code simple and
     * allow the GC to perform direct field references to package private mebers of com.sun.squawk.Ref.
     */
    Ref ref;

    Reference(Object referent) {
        ref = new Ref(referent);
    }

    /**
     * Clears this reference object.
     */
    public void clear() {
        ref.clear();
    }

    /**
     * Returns this reference object's referent.  If this reference object has
     * been cleared, either by the program or by the garbage collector, then
     * this method returns <code>null</code>.
     *
     * @return   The object to which this reference refers, or
     *           <code>null</code> if this reference object has been cleared
     */
    public Object get() {
        return ref.get();
    }
}

