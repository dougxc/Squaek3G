/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk.debugger.sdp;

import java.io.*;
import java.util.*;
import java.util.Arrays;
import java.util.Iterator;

import com.sun.squawk.debugger.*;
import com.sun.squawk.debugger.DataType.*;
import com.sun.squawk.util.*;
import com.sun.squawk.vm.CID;
import com.sun.squawk.*;

/**
 * A proxy for a class that exists on or is loaded into a Squawk VM. Given that
 * a class on a Squawk VM may have had its symbolic information stripped, this
 * proxy type always gets information for fields and methods of a class from a
 * classfile. This means that the classfiles on the path given to the SDP
 * must be in sync with the classfiles used to create the classes on the
 * Squawk VM. However, there is (currently) no test for this classfile compatibility.
 *
 * @author  Doug Simon
 */
public class ProxyType {

    /**
     * The object that created this proxy type and is used to lookup other proxy types.
     */
    private final ProxyTypeManager ptm;

    /**
     * The proxied class.
     */
    private final Klass klass;

    /**
     * The JDWP identifier for the proxied class.
     */
    private final ReferenceTypeID id;

    /**
     * Is the class initialized.
     */
    private boolean initialized;

    /**
     * Gets the class proxied by this object.
     *
     * @return the class represented by this object
     */
    public Klass getKlass() {
        return klass;
    }

    /**
     * The list of fields defined by this class.
     */
    private List fields;

    /**
     * The list of methods defined by the class.
     */
    private List methods;

    /**
     * Creates a ProxyType for a class.
     *
     * @param klass    the class
     * @param id       the JDWP identifier for the class
     * @param ptm      the object that created this proxy type and is used to lookup other proxy types
     */
    ProxyType(Klass klass, ReferenceTypeID id, ProxyTypeManager ptm) {
        this.ptm = ptm;
        this.id = id;
        Assert.that(klass != null);
        this.klass = klass;
    }

    /**
     * Gets the super class of this class.
     *
     * @return the super class of this class
     */
    public final ProxyType getSuperclass() throws IOException, SDWPException {
        Klass superClass = klass.getSuperclass();
        // the "-bytecode-" class actually inherits from INT, but don't tell jdwp that!
        if (superClass != null && !superClass.isPrimitive()) {
            return ptm.lookup(superClass, true);
        }
        return null;
    }

    /**
     * @return the JDWP identifier for this class
     */
    public final ReferenceTypeID getID() {
        return id;
    }

    /**
     * Returns the JNI-style signature for this type.
     * <p>
     * For primitive classes
     * the returned signature is the signature of the corresponding primitive
     * type; for example, "I" is returned as the signature of the class
     * represented by {@link java.lang.Integer#TYPE}.
     *
     * @see <a href="doc-files/signature.html">Type Signatures</a>
     * @return the string containing the type signature.
     */
    public String getSignature() {
        return DebuggerSupport.getJNISignature(klass);
    }


    /**
     * Gets the fully qualified name of this type. The returned name
     * is formatted as it might appear in a Java programming langauge
     * declaration for objects of this type.
     * <p>
     * For primitive classes
     * the returned name is the name of the corresponding primitive
     * type; for example, "int" is returned as the name of the class
     * represented by {@link java.lang.Integer#TYPE Integer.TYPE}.
     * @return a string containing the type name.
     */
    public String getName() {
        String name = klass.getName();
        if (!klass.isArray()) {
            return name;
        }

        int dimensions = 0;
        while (name.charAt(dimensions) == '[') {
            ++dimensions;
        }

        name = name.substring(dimensions);
        char first = name.charAt(0);
        if (first == 'L') {
            name = name.substring(1, name.length() - 2).replace('/', '.');
        } else {
            switch (first) {
                case 'I': name = "int";     break;
                case 'J': name = "long";    break;
                case 'F': name = "float";   break;
                case 'D': name = "double";  break;
                case 'Z': name = "boolean"; break;
                case 'C': name = "char";    break;
                case 'S': name = "short";   break;
                case 'B': name = "byte";    break;
                case 'V': name = "void";    break;
            }
        }
        while (dimensions-- != 0) {
            name += "[]";
        }
        return name;
    }

    /**
     * Returns the modifiers for the reference type. ACC_PUBLIC, ACC_FINAL, etc.
     * Undefined for arrays and primitive type.
     *
     * @return the class modifiers in JVM Spec format.
     */
    public int getModifiers() {
        return klass.getModifiers() & Modifier.getJVMClassModifiers();
    }

    /**
     * Returns a list containing each {@link Field} declared in this type.
     * Inherited fields are not included. Any synthetic fields created
     * by the compiler are included in the list.
     * <p>
     * For arrays and primitive classes, the returned list is always empty.
     *
     * @return a list {@link Field} objects; the list has length 0
     * if no fields exist.
     */
    public List getFields() {
        if (fields == null) {
            fields = new ArrayList();
            addFields(fields, true);
            addFields(fields, false);
        }
        return fields;
    }

    private void addFields(List list, boolean isStatic) {
        int count = klass.getFieldCount(isStatic);
        for (int i = 0; i != count; ++i) {
            Field field = klass.getField(i, isStatic);
            FieldID id = new FieldID(JDWP.getTag(field.getType()), field.getOffset(), isStatic, getID());
            ProxyField proxyField = new ProxyField(id, field);
            list.add(proxyField);

        }
    }

    /**
     * Gets the field corresponding to a given field ID.
     *
     * Note that the field may be defined in <code>this</code> class,
     * or in a superclass or implemented interface class.
     *
     * @param id  the identifier of the field to retrieve
     * @return the ProxyField object corresponding to <code>id</code> or null
     * @throws SDWPException if the FieldID <code>id</code> is not defined in
     * <code>this</code> class or in a superclass or implemented interface class.
     */
    public ProxyField getField(FieldID id) throws IOException, SDWPException {
        if (id.definingClass.equals(this.id)) {
            for (Iterator iterator = getFields().iterator(); iterator.hasNext();) {
                ProxyField field = (ProxyField)iterator.next();
                if (field.getID().equals(id)) {
                    return field;
                }
            }
            throw new SDWPException(JDWP.Error_INVALID_FIELDID, id + " is not a field of " + this);
        }

        ProxyType definingProxyType = ptm.lookup(id.definingClass, true);

        // make sure that "this" class is a suptype or interface of the field's defining class
        if (!definingProxyType.getKlass().isAssignableFrom(this.getKlass())) {
            throw new SDWPException(JDWP.Error_INVALID_FIELDID, id + " is not a field of " + this);
        }

        return definingProxyType.getField(id);
    }

    /**
     * Returns a list containing each {@link Method} declared
     * directly in this type.
     * Inherited methods are not included. Constructors,
     * the initialization method if any, and any synthetic methods created
     * by the compiler are included in the list.
     * <p>
     * For arrays and primitive classes, the returned list is always empty.
     *
     * @return a list {@link Method} objects; the list has length 0
     * if no methods exist.
     */
    public List getMethods() {
        if (methods == null) {
            if (klass.isArray()) {
                methods = Collections.EMPTY_LIST;
            } else {
                methods = new ArrayList();
                addMethods(methods, true);
                addMethods(methods, false);
            }
        }
        return methods;
    }

    private void addMethods(List list, boolean isStatic) {
        int count = klass.getMethodCount(isStatic);
        for (int i = 0; i != count; ++i) {
            Method method = klass.getMethod(i, isStatic);
            if (!method.isHosted()) {
                MethodID id = new MethodID(method.getOffset(), isStatic);
                ProxyMethod proxyMethod = new ProxyMethod(id, method);
                list.add(proxyMethod);
            }
        }
    }

    /**
     * Gets the method corresponding to a given method ID.
     *
     * @param id  the identifier of the method to retrieve
     * @return the ProxyMethod object corresponding to <code>id</code> or null
     */
    public ProxyMethod getMethod(MethodID id) {
        for (Iterator iterator = getMethods().iterator(); iterator.hasNext();) {
            ProxyMethod proxyMethod = (ProxyMethod)iterator.next();
            if (proxyMethod.getID().equals(id)) {
                return proxyMethod;
            }
        }
        return null;
    }

    /**
     * Returns interfaces directly implemented by this type.
     *
     * @return a List of the interfaces directly implemented by this type
     */
    public List getInterfaces() throws IOException, SDWPException {
        Klass[] interfaces = klass.getInterfaces();
        List list = new ArrayList(interfaces.length);
        for (int i = 0; i != interfaces.length; ++i) {
            list.add(ptm.lookup(interfaces[i], true));
        }
        return list;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "<" + klass + ", id: " + getID() + ", " + getSignature() + ">";
    }

    /**
     * Gets an identifying name for the source corresponding to the
     * declaration of this type. Interpretation of this string is
     * the responsibility of the source repository mechanism.
     * <P>
     * The returned name is dependent on VM's default stratum
     * ({@link VirtualMachine#getDefaultStratum()}).
     * In the reference implementation, when using the base stratum,
     * the returned string is the
     * unqualified name of the source file containing the declaration
     * of this type.  In other strata the returned source name is
     * the first source name for that stratum.  Since other languages
     * may have more than one source name for a reference type,
     * the use of {@link Location#sourceName()} or
     * {@link #sourceNames(String)} is preferred.
     * <p>
     *
     * @return the string source file name or null if it is not known
     */
    public String getSourceName() {
        return klass.getSourceFileName();
    }

    /* Compare based on class signature */
    public int compareTo(Object o) {
        if (o == this) {
            return 0;
        }
        if (!(o instanceof ProxyType)) {
            return -1;
        }
        ProxyType other = (ProxyType) o;
        return getSignature().compareTo(other.getSignature());
    }

}

/**
 * Proxy for a type that exists on a Squawk VM but for which no valid classfile can be found
 * on the class path supplied to the SDP.
 *
 * @author Doug Simon
 */
class UndefinedProxyType extends ProxyType {
    UndefinedProxyType(Klass klass, ReferenceTypeID id, ProxyTypeManager ptm) {
        super(klass, id, ptm);
    }

    public List getMethods() {
        return METHODS;
    }

    public List getFields() {
        return Collections.EMPTY_LIST;
    }

    static final ProxyMethod UNKNOWN_METHOD = new ProxyMethod(MethodID.UNKNOWN, null) {

        public String getName() {
            return "-unknown-";
        }

        public String getSignature() {
            return "()V";
        }

        public int getModifiers() {
            return Modifier.PUBLIC | Modifier.STATIC;
        }

        public MethodID getID() {
            return MethodID.UNKNOWN;
        }

        public ProxyMethod.LineNumberTable getLineNumberTable() {
            return ProxyMethod.LineNumberTable.EMPTY_TABLE;
        }

        public synchronized ScopedLocalVariable[] getVariableTable() {
            return new ScopedLocalVariable[0];
        }

        public int getArgCount() {
            return 0;
        }
    };

    private static final List METHODS = Arrays.asList(new ProxyMethod[] { UNKNOWN_METHOD });
}
