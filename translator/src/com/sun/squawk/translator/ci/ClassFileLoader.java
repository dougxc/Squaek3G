/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM translator.
 */
package com.sun.squawk.translator.ci;

import java.io.*;
import java.util.*;

import com.sun.squawk.io.connections.*;
import com.sun.squawk.translator.*;
import com.sun.squawk.util.Assert;
import com.sun.squawk.util.Tracer;
import com.sun.squawk.util.Arrays;
import com.sun.squawk.util.Comparer;
import com.sun.squawk.vm.CID;
import com.sun.squawk.util.SquawkVector;
import com.sun.squawk.translator.ci.ClassFileReader.Attribute;
import com.sun.squawk.pragma.PragmaException;
import com.sun.squawk.*;


/**
 * The <code>ClassFileLoader</code> class provides the functionality
 * by which class definitions are loaded from class files using a class path.
 *
 * @author  Doug Simon
 */
public final class ClassFileLoader implements Context {


    /*---------------------------------------------------------------------------*\
     *                          Constructor and fields                           *
    \*---------------------------------------------------------------------------*/

    /**
     * Creates the class file loader.
     */
    public ClassFileLoader(Translator translator) {
        this.translator = translator;
    }

    /**
     * The translation context
     */
    private final Translator translator;

    /**
     * The class file being loaded.
     */
    private ClassFile cf;

    /**
     * The class being loaded.
     */
    private Klass klass;

    /**
     * The class file reader.
     */
    private ClassFileReader cfr;

    /**
     * The contant pool of the class being loaded.
     */
    private ConstantPool pool;

    /*---------------------------------------------------------------------------*\
     *                             Loading methods                               *
    \*---------------------------------------------------------------------------*/

    /**
     * Gets the file path for a given class name. The file path is constructed
     * from the given fully qualified name of the class with each (sub)package
     * corresponding to a (sub)directory.
     *
     * @param   name  the class name to process
     * @return  the file path for the class file for the class named by <code>name</code>
     */
    public static String getClassFilePath(String name) {
        return name.replace('.', '/') + ".class";
    }

    /**
     * Gets the file path for a given class. The file path is constructed
     * from the fully qualified name of the class with each (sub)package
     * corresponding to a (sub)directory.
     *
     * @param   klass  the class to process
     * @return  the file path for the class file for <code>klass</code>
     */
    public static String getClassFilePath(Klass klass) {
        return getClassFilePath(klass.getInternalName());
    }

    /**
     * Loads the definition of a class from its class file.
     *
     * @param cf  the class file definition to load
     */
    public void load(ClassFile cf) {
        this.cf = cf;
        this.klass = cf.getDefinedClass();
        Assert.that(klass.getState() < Klass.STATE_LOADED);

        String classFilePath = getClassFilePath(klass);
        InputStream is = null;
        try {
            ClasspathConnection classPath = translator.getClassPath();
            if (classPath == null) {
                throw new IOException("null class path");
            }
            is = classPath.openInputStream(classFilePath);
            load(classFilePath, is);
        } catch (IOException ioe) {
            if (VM.isHosted() || VM.isVeryVerbose()) {
                System.err.println("IO error while loading: " + klass);
                ioe.printStackTrace();
            }
            Translator.throwNoClassDefFoundError(prefix(ioe.toString()));
        } catch (Error le) {
            if (VM.isHosted() || VM.isVeryVerbose()) {
                System.err.println("Linkage error while loading: " + klass);
                le.printStackTrace();
            }
            klass.changeState(Klass.STATE_ERROR);
            throw le;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                    Assert.shouldNotReachHere();
                }
            }
        }
    }

    /**
     * Loads a class from a class file input stream, filling in the
     * corresponding fields of the <code>ClassFile</code> object.
     *
     * @param   fileName  the file from which the class is being loaded
     * @param   is        the input stream into the class file
     * @throws LinkageException
     */
    private void load(String fileName, InputStream is) {

        /*
         * Write trace message
         */
        if (Klass.TRACING_ENABLED && Tracer.isTracing("loading", klass.getName())) {
            Tracer.traceln("[loading " +  klass + "]");
        }

        /*
         * Wrap the input stream in a ClassFileReader
         */
        cfr = new ClassFileReader(is, fileName);

        if (klass.getState() == Klass.STATE_LOADING) {
            Translator.throwClassCircularityError(klass.toString());
        }

        /*
         * Transition the class from "defined" to "loading"
         */
        Assert.that(klass.getState() == Klass.STATE_DEFINED);
        klass.changeState(Klass.STATE_LOADING);

        /*
         * Read the magic values
         */
        loadMagicValues();

        /*
         * Read the constant pool
         */
        loadConstantPool();
        cf.setConstantPool(pool);

        /*
         * Read the class information
         */
        Klass superClass = loadClassInfo();

        /*
         * Read the interface definitions
         */
        Klass[] interfaces = loadInterfaces();
        Assert.that(interfaces != null);

        if (Klass.TRACING_ENABLED && Tracer.isTracing("classinfo", klass.getName())) {
            Tracer.traceln("class: "+klass.getInternalName());
            //if (klass.getSuperclass() != null) {
            //    Tracer.traceln("  extends: "+klass.getSuperclass().getInternalName());
            //}
            if (superClass != null) {
                Tracer.traceln("  extends: "+superClass.getInternalName());
            }
            for (int i = 0 ; i < interfaces.length ; i++) {
                Tracer.traceln("  implements: "+interfaces[i].getInternalName());
            }
        }

        /*
         * Read the field definitions
         */
        ClassFileField[][] fieldTables = { ClassFileField.NO_FIELDS, ClassFileField.NO_FIELDS };
        loadFields(fieldTables);

        /*
         * Read the method definitions
         */
        ClassFileMethod[][] methodTables = { ClassFileMethod.NO_METHODS, ClassFileMethod.NO_METHODS  };
        loadMethods(methodTables);

        /*
         * Read the extra attributes
         */
        String sourceFile = loadExtraAttributes();

        /*
         * Ensure there are no extra bytes
         */
        cfr.readEOF();

        /*
         * Close the input stream
         */
        cfr.close();
        cfr = null;

        /*
         * Transition the class from "loading" to "loaded"
         */
        Assert.that(klass.getState() == Klass.STATE_LOADING);

        if (klass.isSquawkPrimitive() || klass.isSquawkArray()) {
            fieldTables[0] = ClassFileField.NO_FIELDS;
            fieldTables[1] = ClassFileField.NO_FIELDS;
        }

        klass.setClassFileDefinition(superClass,
                                     interfaces,
                                     methodTables[0],
                                     methodTables[1],
                                     fieldTables[0],
                                     fieldTables[1],
                                     sourceFile);

        klass.changeState(Klass.STATE_LOADED);

        /*
         * Write trace message
         */
        if (Klass.TRACING_ENABLED && Tracer.isTracing("loading", klass.getName())) {
            Tracer.traceln("[loaded " +  klass + "]");
        }
    }


    /*---------------------------------------------------------------------------*\
     *                       Class file header loading                           *
    \*---------------------------------------------------------------------------*/

    /**
     * Loads the class file magic values.
     */
    private void loadMagicValues() {
        int magic = cfr.readInt("magic");
        int minor = cfr.readUnsignedShort("minor");
        int major = cfr.readUnsignedShort("major");
        if (magic != 0xCAFEBABE) {
            throw cfr.formatError("Bad magic value = "+Integer.toHexString(magic));
        }
        /*
         * CLDC supports JDK 1.1, 1.2, 1.3, 1.4 classfiles
         */
        if (major < 45 || major > 48) {
            throw cfr.formatError("Unsupported class file version: " + major + ":" + minor);
        }
    }


    /*---------------------------------------------------------------------------*\
     *                          Constant pool loading                            *
    \*---------------------------------------------------------------------------*/

    /**
     * Loads the constant pool.
     */
    private void loadConstantPool() {
        pool = new ConstantPool(translator, cfr, cf.getDefinedClass());
    }


    /*---------------------------------------------------------------------------*\
     *                   Super class and access flags loading                    *
    \*---------------------------------------------------------------------------*/

    /**
     * Loads the class information.
     *
     * @return  the super type of the class being loaded
     */
    private Klass loadClassInfo() {
        int modifiers = cfr.readUnsignedShort("cls-flags");
        int classIndex = cfr.readUnsignedShort("cls-index");
        int superIndex = cfr.readUnsignedShort("cls-super index");

        modifiers = pool.verifyClassModifiers(modifiers);

        /*
         * Loading the constant pool will have created the 'thisClass' object.
         */
        Klass thisClass = pool.getKlass(classIndex);
        if (thisClass != klass) {
            /*
             * VMSpec 5.3.5:
             *
             *   Otherwise, if the purported representation does not actually
             *   represent a class named N, loading throws an instance of
             *   NoClassDefFoundError or an instance of one of its
             *   subclasses.
             */
             Translator.throwNoClassDefFoundError(prefix("'this_class' indicates wrong type"));
        }

        if (klass == null) {
            throw cfr.formatError("invalid 'this_class' item");
        }

        /*
         * Set the modifiers
         */
        modifiers &= Modifier.getJVMClassModifiers();
        modifiers |= klass.getModifiers();
        klass.updateModifiers(modifiers);

        if (superIndex != 0) {
            Klass superClass = pool.getKlass(superIndex);
            Assert.that(superClass != null);

            /*
             * If this is an interface class, its superclass must be
             * java.lang.Object.
             */
            if (klass.isInterface() && superClass != Klass.OBJECT) {
                throw cfr.formatError(
                    "interface class must inherit from java.lang.Object");
            }

            /*
             * Now ensure the super class is resolved
             */
            superClass = pool.getResolvedClass(superIndex, this);

            /*
             * Cannot inherit from an array class.
             */
            if (superClass.isArray()) {
                throw cfr.formatError("cannot inherit from array class");
            }

            /*
             * The superclass cannot be an interface. From the
             * JVM Spec section 5.3.5:
             *
             *   If the class of interface named as the direct
             *   superclass of C is in fact an interface, loading
             *   throws an IncompatibleClassChangeError.
             */
            if (superClass.isInterface()) {
                throw cfr.formatError("cannot extend an interface class");
            }

            /*
             * The superclass cannot be final.
             * Inheriting from a final class is a VerifyError according
             * to J2SE JVM behaviour. There is no explicit
             * documentation in the JVM Spec.
             */
            if (superClass.isFinal()) {
                Translator.throwVerifyError(prefix("cannot extend a final class"));
            }

            return superClass;
        } else if (klass != Klass.OBJECT) {
            throw cfr.formatError("class must have super-type");
        } else {
            return null;
        }
    }


    /*---------------------------------------------------------------------------*\
     *                          Interface loading                                *
    \*---------------------------------------------------------------------------*/

    /**
     * Loads the class's interfaces.
     *
     * @return  the interfaces implemented by the class being loaded
     */
    private Klass[] loadInterfaces() {
        int count = cfr.readUnsignedShort("i/f-count");
        if (count == 0) {
            return Klass.NO_CLASSES;
        }
        Klass[] interfaces = new Klass[count];
        for (int i = 0; i < count; i++) {
            Klass iface = pool.getResolvedClass(cfr.readUnsignedShort("i/f-index"), this);
            if (!iface.isInterface()) {
                Translator.throwIncompatibleClassChangeError(prefix("cannot implement non-interface class"));
            }
            interfaces[i] = iface;

            if (iface.getInternalName().equals("com.sun.squawk.pragma.GlobalStaticFields")) {
                klass.updateModifiers(Modifier.GLOBAL_STATICS);
            }
        }
        return interfaces;
    }

    /*---------------------------------------------------------------------------*\
     *                             Field loading                                 *
    \*---------------------------------------------------------------------------*/

    /**
     * Loads the class's fields.
     *
     * @param   fieldTables  the 2-element array in which the table of
     *                 instance fields will be returned at index 0 and the
     *                 table of static fields at index 1
     */
    private void loadFields(ClassFileField[][] fieldTables) {

        /*
         * Get count of fields
         */
        final int count = cfr.readUnsignedShort("fld-count");
        if (count == 0) {
            return;
        }

        /*
         * Allocate SquawkVector to collect the fields
         */
        SquawkVector instanceFields = new SquawkVector(count);
        SquawkVector staticFields = new SquawkVector(count);

        /*
         * Read in all the fields
         */
        for (int i = 0; i < count; i++) {
            ClassFileField field = loadField();

            /*
             * Verify that there are no duplicate fields.
             */
            verifyFieldIsUnique(instanceFields, field);
            verifyFieldIsUnique(staticFields, field);

            /*
             * Add the field to the appropriate collection
             */
            if (field.isStatic()) {
                staticFields.addElement(field);
            } else {
                instanceFields.addElement(field);
            }
        }

        /*
         * Partition the fields into the static and instance field tables.
         */
        fieldTables[0] = getFieldTable(instanceFields);
        fieldTables[1] = getFieldTable(staticFields);

        /*
         * Sort the instance fields by size in decreasing order. This ensures
         * that the fields will be aligned according to their data size. It
         * also provides a simple form of object packing
         */
        if (fieldTables[0].length > 1) {
            sortFields(fieldTables[0]);
        }
    }

    /**
     * Copies a vector of <code>ClassFileField</code>s into an array of
     * <code>ClassFileField</code>s.
     *
     * @param   fields  the vector of <code>ClassFileField</code>s to copy
     * @return  an array of <code>ClassFileField</code>s corresponding to the
     *                  contents of <code>fields</code>
     */
    private ClassFileField[] getFieldTable(SquawkVector fields) {
        if (fields.isEmpty()) {
            return ClassFileField.NO_FIELDS;
        } else {
            ClassFileField[] table = new ClassFileField[fields.size()];
            fields.copyInto(table);
            return table;
        }
    }

    /**
     * Verifies that a given field does not match any of the fields in a
     * given collection of fields.
     *
     * @param  fields  the collection of fields to test
     * @param  field   the field to match
     */
    private void verifyFieldIsUnique(SquawkVector fields, ClassFileField field) {
        for (Enumeration e = fields.elements(); e.hasMoreElements(); ) {
            ClassFileField f = (ClassFileField)e.nextElement();
            if (f.getName().equals(field.getName()) && f.getType() == field.getType()) {
                throw cfr.formatError("duplicate field found");
            }
        }
    }

    /**
     * Sorts an array of fields by the data size of their types in
     * descending order.
     *
     * @param fields  the array of fields to sort
     */
    private void sortFields(ClassFileField[] fields) {
        Arrays.sort(fields, new Comparer() {
            public int compare(Object o1, Object o2) {
                if (o1 == o2) {
                    return 0;
                }

                Klass t1 = ((ClassFileField)o1).getType();
                Klass t2 = ((ClassFileField)o2).getType();

                /*
                 * Sort by data size of field's type
                 */
                if (t1.getDataSize() < t2.getDataSize()) {
                    return 1;
                } else if (t1.getDataSize() > t2.getDataSize()) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });
    }

    /**
     * Loads one of the class's fields.
     *
     * @return  the loaded field
     */
    private ClassFileField loadField() {
        int modifiers = cfr.readUnsignedShort("fld-flags");
        int nameIndex = cfr.readUnsignedShort("fld-nameIndex");
        int descriptorIndex = cfr.readUnsignedShort("fld-descIndex");
        int attributesCount = cfr.readUnsignedShort("fld-AttbCount");
        int constantValueIndex  = 0;

        String fieldName = pool.getUtf8(nameIndex);
        String fieldSig  = pool.getUtf8(descriptorIndex);

        pool.verifyFieldModifiers(modifiers, klass.getModifiers());
        pool.verifyName(fieldName, ConstantPool.ValidNameFormat.FIELD);
        Klass fieldType = pool.verifyFieldType(fieldSig);

        modifiers &= Modifier.getJVMFieldModifiers();

        /*
         * Process the field's attributes
         */
        for (int j = 0; j < attributesCount; j++) {
            Attribute attribute = cfr.openAttribute(pool);
            if (attribute.name.equals("ConstantValue")) {
                if (attribute.length != 2) {
                    throw cfr.formatError("ConstantValue attribute length is not 2");
                }
                if (constantValueIndex != 0) {
                    throw cfr.formatError("duplicate ConstantValue attribute");
                }

                /*
                 * Get the variable initialzation value
                 */
                constantValueIndex = cfr.readUnsignedShort("fld-ConstantValue");
                if (constantValueIndex == 0) {
                    throw cfr.formatError("bad ConstantValue index");
                }

                /*
                 * A field_info structure for a non-static field that has a ConstantValue
                 * attribute must be silently ignored.
                 */
                if ((modifiers & Modifier.STATIC) == 0) {
                    //throw in.classFormatError("ConstantValue attribute for non-static field " + fieldName);
                    constantValueIndex = 0;
                }
            } else if (attribute.name.equals("Synthetic")) {
                modifiers |= Modifier.SOURCE_SYNTHETIC;
            } else {
                attribute.skip();
            }
            attribute.close();
        }

        /*
         * Get the constant value attribute (if any). The value itself is
         * currently discarded as it is expected that the field is either
         * initialized in <clinit> or it is a primitive constant that is
         * inlined everywhere it is accessed. This is not completely correct
         * and will have to be fixed as there is at least one TCK test
         * (i.e. "javasoft.sqe.tests.vm.classfmt.atr.atrcvl004.atrcvl00401m1")
         * that expects some compile-time constant static fields to be
         * initialized from "ConstantValue" attributes as they are
         * subsequently accessed by 'getstatic' (i.e. the access was not
         * inlined) and the class contains no <clinit> method.
         *
         * UPDATE: To pass the above mentioned TCK test, the constant value is
         * now retained for the lifetime of a translation unit.
         */
        Object constantValue = getFieldConstantValue(fieldType, constantValueIndex);

        /*
         * Create the field
         */
        ClassFileField field;
        if (constantValue != null) {
            modifiers |= Modifier.CONSTANT;
            if (constantValue instanceof String) {
                field = new ClassFileConstantField(fieldName, modifiers, fieldType, (String)constantValue);
            } else {
                long value = 0;
                if (constantValue instanceof Integer) {
                    value = ((Integer)constantValue).intValue();
                } else if (constantValue instanceof Long) {
                    value = ((Long)constantValue).longValue();
/*if[FLOATS]*/
                } else if (constantValue instanceof Double) {
                    value = Double.doubleToLongBits(((Double)constantValue).doubleValue());
                } else if (constantValue instanceof Float) {
                    value = Float.floatToIntBits(((Float)constantValue).floatValue());
/*end[FLOATS]*/
                } else {
                    Assert.shouldNotReachHere("Unknown constant value type: " + constantValue);
                }
                field = new ClassFileConstantField(fieldName, modifiers, fieldType, value);
            }
        } else {
            field = new ClassFileField(fieldName, modifiers, fieldType);
        }



        /*
         * Tracing
         */
        if (Klass.TRACING_ENABLED && Tracer.isTracing("classinfo", klass.getName() + "." + fieldName)) {
            String constantStr = constantValue == null ? "" : "  (constantValue=" + constantValue + ")";
            String staticStr = (field.isStatic()) ? "static " : "";
            Tracer.traceln("  field: " + staticStr + fieldType.getName() + " " + fieldName + constantStr);
        }

        return field;
    }

    /**
     * Gets the object corresponding to the ConstantValue attribute for a field
     * if it has one.
     *
     * @param   fieldType the type of the field currently being loaded
     * @param   constantValueIndex the index of the ConstantValue attribute
     * @return  the value of the ConstantValue attribute or null if there is
     *                    no such attribute
     */
    private Object getFieldConstantValue(Klass fieldType, int constantValueIndex) {
        if (constantValueIndex != 0) {
            /*
             * Verify that the initial value is of the right klass for the field
             */
            switch (fieldType.getSystemID()) {
                case CID.LONG:    return pool.getEntry(constantValueIndex, ConstantPool.CONSTANT_Long);
                case CID.FLOAT:   return pool.getEntry(constantValueIndex, ConstantPool.CONSTANT_Float);
                case CID.DOUBLE:  return pool.getEntry(constantValueIndex, ConstantPool.CONSTANT_Double);
                case CID.INT:     // fall through ...
                case CID.SHORT:   // fall through ...
                case CID.CHAR:    // fall through ...
                case CID.BYTE:    // fall through ...
                case CID.BOOLEAN: return pool.getEntry(constantValueIndex, ConstantPool.CONSTANT_Integer);
                case CID.STRING:  return pool.getEntry(constantValueIndex, ConstantPool.CONSTANT_String);
                default:          throw cfr.formatError("invalid ConstantValue attribute value");
            }
        } else {
            return null;
        }
    }


    /*---------------------------------------------------------------------------*\
     *                              Method loading                               *
    \*---------------------------------------------------------------------------*/

    /**
     * Loads the class's methods.
     */
    private void loadMethods(ClassFileMethod[][] methodTables) {

        /*
         * Get count of methods and return if there are none
         */
        int count = cfr.readUnsignedShort("mth-count");
        if (count == 0 && (klass.isInterface() || klass.isAbstract())) {
            return;
        }

        /*
         * Allocate the method vector
         */
        SquawkVector virtualMethods = new SquawkVector(count);
        SquawkVector staticMethods = new SquawkVector(count);
        SquawkVector replacementConstructors = null;

        /*
         * Flags whether or not a constructor was read
         */
        boolean hasConstructor = false;

        /*
         * Read in all the methods
         */
        for (int i = 0; i < count; i++) {
            ClassFileMethod method = loadMethod();
            if (!hasConstructor && Modifier.isConstructor(method.getModifiers())) {
                hasConstructor = true;
            }

            /*
             * Verify that there are no duplicate methods.
             */
            verifyMethodIsUnique(virtualMethods, method);
            verifyMethodIsUnique(staticMethods, method);

            /*
             * Add the method to the appropriate collection
             */
            if (PragmaException.isReplacementConstructor(method.getPragmas())) {
                if (replacementConstructors == null) {
                    replacementConstructors = new SquawkVector();
                }
                replacementConstructors.addElement(method);
            } else if (method.isStatic()) {
                staticMethods.addElement(method);
            } else {
                virtualMethods.addElement(method);
            }
        }

        // Process replacement constructors.
        replaceConstructors(staticMethods, replacementConstructors);

        /*
         * Synthesize a default constructor for a class which has no constructors
         */
        if (!hasConstructor && !klass.isAbstract() && !klass.isInterface()) {
            ClassFileMethod method = new ClassFileMethod("<init>",
                                                         Modifier.PUBLIC | Modifier.STATIC | Modifier.CONSTRUCTOR,
                                                         klass,
                                                         Klass.NO_CLASSES,
                                                         0);
            method.setCode(Code.SYNTHESIZED_DEFAULT_CONSTRUCTOR_CODE);
            staticMethods.addElement(method);
        }

        /*
         * Partition the methods into the static and virtual method tables.
         */
        methodTables[0] = getMethodTable(virtualMethods);
        methodTables[1] = getMethodTable(staticMethods);
        cf.setVirtualMethods(getCodeTable(virtualMethods));
        cf.setStaticMethods(getCodeTable(staticMethods));
    }

    /**
     * Processes the methods of a non-array class that represents its instance data internally as an array
     * to substitute the body of each of its constructors with the body of a method
     * annotated by the {@link ReplacementConstructorPragma}.
     *
     * @param methods      the methods containing the original constructors
     * @param replacements the methods annotated by the {@link ReplacementConstructorPragma}
     */
    private void replaceConstructors(SquawkVector methods, SquawkVector replacements) {
        boolean constructorsNeedReplacing = klass.isSquawkArray() && !klass.isArray();
        if (constructorsNeedReplacing) {
            for (int m = 0; m != methods.size(); ++m) {
                ClassFileMethod method = (ClassFileMethod)methods.elementAt(m);
                if (Modifier.isConstructor(method.getModifiers())) {
                    methods.setElementAt(replaceConstructor(method, replacements), m);
                }
            }
        }
    }

    /**
     * Substitutes the body of a given constructor method with the body of a matching method
     * from a set of given replacement methods. A replacement method matches the constructor
     * if it takes the same set of parameter types.
     *
     * @param ctor         the constructor whose body is to be replaced
     * @param replacements the set of methods to search for a match
     * @return the new constructor once the substitution has been done
     * @throws LinkageError if no substitution could be found
     */
    private ClassFileMethod replaceConstructor(ClassFileMethod ctor, SquawkVector replacements) {
        if (replacements != null) {

            Klass[] types = ctor.getParameterTypes();
            Klass[] matchTypes = new Klass[types.length + 1];
            System.arraycopy(types, 0, matchTypes, 1, types.length);
            matchTypes[0] = klass;

            for (Enumeration e = replacements.elements(); e.hasMoreElements(); ) {
                ClassFileMethod method = (ClassFileMethod)e.nextElement();

                if (Arrays.equals(matchTypes, method.getParameterTypes())) {
                    ctor = new ClassFileMethod("<init>",
                        ctor.getModifiers() | Modifier.HAS_PRAGMAS,
                        ctor.getReturnType(),
                        ctor.getParameterTypes(),
                        ctor.getPragmas() | PragmaException.REPLACEMENT_CONSTRUCTOR);
                    ctor.setCode(method.getCode());
                    return ctor;
                }
            }
        }
        Translator.throwVerifyError(prefix("could not match original constructor with a replacement constructor"));
        return null;
    }

    /**
     * Verifies that a given method does not match any of the methods in a
     * given collection of methods.
     *
     * @param  methods  the collection of methods to test
     * @param  method   the method to match
     */
    private void verifyMethodIsUnique(SquawkVector methods, ClassFileMethod method) {
        for (Enumeration e = methods.elements(); e.hasMoreElements(); ) {
            ClassFileMethod m = (ClassFileMethod)e.nextElement();
            if (m.getName().equals(method.getName()) && Arrays.equals(m.getParameterTypes(), method.getParameterTypes()) && m.getReturnType() == method.getReturnType()) {
                throw cfr.formatError("duplicate method found");
            }
        }
    }

    /**
     * Copies a vector of <code>ClassFileMethod</code>s into an array of
     * <code>ClassFileMethod</code>s.
     *
     * @param   methods  the vector of <code>ClassFileMethod</code>s to copy
     * @return  an array of <code>ClassFileMethod</code>s corresponding to the
     *                   contents of <code>methods</code>
     */
    private ClassFileMethod[] getMethodTable(SquawkVector methods) {
        if (methods.isEmpty()) {
            return ClassFileMethod.NO_METHODS;
        } else {
            ClassFileMethod[] table = new ClassFileMethod[methods.size()];
            methods.copyInto(table);
            return table;
        }
    }

    /**
     * Extracts the bytecode arrays from a vector of
     * <code>ClassFileMethod</code>s and copies them into an array of
     * <code>Code</code>s.
     *
     * @param   methods  the vector of <code>ClassFileMethod</code>s
     * @return  an array of <code>Code</code>s corresponding to the bytecode
     *                   arrays of the contents of <code>methods</code>. The
     *                   entries for abstract or native methods will be null
     */
    private Code[] getCodeTable(SquawkVector methods) {
        if (methods.isEmpty()) {
            return Code.NO_CODE;
        } else {
            Code[] table = new Code[methods.size()];
            int index = 0;
            for (Enumeration e = methods.elements(); e.hasMoreElements(); ) {
                ClassFileMethod method = (ClassFileMethod)e.nextElement();
                if (!PragmaException.isHosted(method.getPragmas()) && !method.isAbstract() && !method.isNative()) {
                    byte[] code = method.getCode();
                    Assert.that(code != null);
                    table[index] = new Code(code);
                }
                ++index;
            }
            return table;
        }
    }

    /**
     * Loads one of the class's methods.
     *
     * @return  the loaded method
     */
    private ClassFileMethod loadMethod() {
        int modifiers = cfr.readUnsignedShort("mth-flags");
        int nameIndex = cfr.readUnsignedShort("mth-nameIndex");
        int descriptorIndex = cfr.readUnsignedShort("mth-descIndex");
        int attributesCount = cfr.readUnsignedShort("mth-AttbCount");
        boolean isDefaultInit = false;

        String methodName = pool.getUtf8(nameIndex);
        String methodSig  = pool.getUtf8(descriptorIndex);

        if (methodName.equals("<clinit>")) {
            /*
             * JVM Spec 4.6:
             *
             * Class and interface initialization methods are called
             * implicitly by the Java virtual machine; the value of their
             * access_flags item is ignored exception for the settings of the
             * ACC_STRICT flag.
             */
            modifiers = (modifiers & (Modifier.STRICT)) | Modifier.STATIC;
        } else {
            pool.verifyMethodModifiers(modifiers, klass.getModifiers(), methodName.equals("<init>"));
            modifiers &= Modifier.getJVMMethodModifiers();
        }

        pool.verifyName(methodName, ConstantPool.ValidNameFormat.METHOD);
        MethodSignature methodSignature = pool.verifyMethodType(methodSig, (methodName.endsWith("init>")), Modifier.isStatic(modifiers));

        /*
         * If this is a constructor, then change its return type to be the parent.
         */
        if (methodName.equals("<init>")) {
            Assert.that(methodSignature.returnType == Klass.VOID);
            methodSignature = methodSignature.modifyReturnType(klass);
            modifiers |= (Modifier.CONSTRUCTOR | Modifier.STATIC);
        }

        /*
         * Process the method's attributes
         */
        boolean hasCodeAttribute = false;
        boolean hasExceptionTable = false;
        int pragmas = 0;
        byte[] code = null;
        for (int j = 0; j < attributesCount; j++) {
            Attribute attribute = cfr.openAttribute(pool);
            if (attribute.name.equals("Code")) {
                if (hasCodeAttribute) {
                    throw cfr.formatError("duplicate Code attribute in method");
                }
                hasCodeAttribute = true;
                if (!Modifier.isAbstract(modifiers) && !Modifier.isNative(modifiers)) {
                    code = new byte[attribute.length];
                    cfr.readFully(code, "code");
                } else {
                    attribute.skip();
                }
            } else if (attribute.name.equals("Exceptions")) {
                if (hasExceptionTable) {
                    throw cfr.formatError("duplicate Exceptions attribute in method");
                }
                hasExceptionTable = true;

                int numExceptions = cfr.readUnsignedShort("mth-att-num-exceptions");
                for (int i = 0; i < numExceptions; i++) {
                    Klass exceptionClass = pool.getKlass(cfr.readUnsignedShort("mth-att-exception"));
                    pragmas |= PragmaException.toModifier(exceptionClass.getName());
                }
            } else if (attribute.name.equals("Synthetic")) {
                modifiers |= Modifier.SOURCE_SYNTHETIC;
            } else {
                attribute.skip();
            }
            attribute.close();
        }

        /*
         * Verify that the methods that require a Code attribute actually
         * have one and vice-versa.
         */
        if ((Modifier.isAbstract(modifiers) || Modifier.isNative(modifiers)) == hasCodeAttribute) {
            if (hasCodeAttribute) {
                throw cfr.formatError("code attribute supplied for native or abstract method");
            } else {
                throw cfr.formatError("missing Code attribute for method");
            }
        }

        // Verify that a method marked with a pragma is non-virtual and does not override a
        // method in a superclass. This means the translator and/or compiler can always
        // known for every single invoke site whether or not a pragma method is being invoked.
        if (pragmas != 0) {
            if (!(Modifier.isStatic(modifiers) || Modifier.isPrivate(modifiers) ||
                  (Modifier.isFinal(modifiers) || klass.isFinal()))) {
                throw cfr.formatError("method with pragma is not non-virtual: " + methodName);
            }

            modifiers |= Modifier.HAS_PRAGMAS;
            if (PragmaException.isNative(pragmas)) {
                modifiers |= Modifier.NATIVE;
            }
        }

        /*
         * Create the method structure
         */
        ClassFileMethod method = new ClassFileMethod(methodName,
                                                     modifiers,
                                                     methodSignature.returnType,
                                                     methodSignature.parameterTypes,
                                                     pragmas);
        if (!Modifier.isNative(modifiers) && !PragmaException.isHosted(pragmas) && code != null) {
            method.setCode(code);
        }

        /*
         * Tracing
         */
        if (Klass.TRACING_ENABLED && Tracer.isTracing("classinfo", klass.getName() + "." + methodName)) {
            String staticStr = ((method.isStatic()) ? "static " : "");
            Tracer.traceln("  method: " + staticStr + klass.getName() + "." + methodName);
        }

        return method;
    }

    /*---------------------------------------------------------------------------*\
     *                           Class attribute loading                         *
    \*---------------------------------------------------------------------------*/

    /**
     * Loads the class's other attributes.
     *
     * @return  the value of the "SourceFile" attribute if there is one
     */
    private String loadExtraAttributes() {
        int attributesCount = cfr.readUnsignedShort("ex-count");
        String sourceFile = null;
        boolean hasInnerClassesAttribute = false;
        for (int i = 0; i < attributesCount; i++) {
            Attribute attribute = cfr.openAttribute(pool);
            if (attribute.name.equals("SourceFile")) {
                int index = cfr.readUnsignedShort("sourcefile-index");
                sourceFile = pool.getUtf8(index);
            } else if (attribute.name.equals("InnerClasses")) {
                if (hasInnerClassesAttribute) {
                    cfr.formatError("duplicate InnerClasses attribute in class");
                }
                hasInnerClassesAttribute = true;
                int count = cfr.readUnsignedShort("inc-number_of_classes");
                while (count-- != 0) {
                    int innerClassIndex = cfr.readUnsignedShort("inc-inner_class_info_index");
                    int outerClassIndex = cfr.readUnsignedShort("inc-outer_class_info_index");
                    int innerNameIndex = cfr.readUnsignedShort("inc-inner_name_index");
                    int innerAccessFlags = cfr.readUnsignedShort("inc-inner_class_access_flags");
                }
            } else if (attribute.name.equals("Synthetic")) {
                klass.updateModifiers(Modifier.SOURCE_SYNTHETIC);
            } else {
                attribute.skip();
            }
            attribute.close();
        }
        return sourceFile;
    }


    /*---------------------------------------------------------------------------*\
     *                           Context interface                               *
    \*---------------------------------------------------------------------------*/

    /**
     * {@inheritDoc}
     */
    public String prefix(String msg) {
        return klass.getName() + ": " + msg;
    }
}
