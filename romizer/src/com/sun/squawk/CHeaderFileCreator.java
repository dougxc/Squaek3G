/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk;

import java.util.*;
import java.io.*;

import com.sun.squawk.vm.*;
import com.sun.squawk.translator.ir.InstructionEmitter;

/**
 * This class is used to create/update the C header file required by the C implementation
 * of the Squawk interpreter. The header file provides definitions for the symbols
 * in the bootstrap suite that are required by the interpreter.
 *
 * @author Doug Simon
 */
public final class CHeaderFileCreator {

    /**
     * Properties from "squawk.sym".
     */
    private final Properties map;

    /**
     *
     * @param bootstrapSuite Suite
     * @param file File
     * @param properties Properties
     * @return  true if the file was overwritten or created, false if not
     * @throws IOException
     */
    public static boolean update(Suite bootstrapSuite, File file, Properties properties) throws IOException {
        CHeaderFileCreator creator = new CHeaderFileCreator(properties);

        CharArrayWriter caw = new CharArrayWriter(file.exists() ? (int)file.length() : 0);
        PrintWriter out = new PrintWriter(caw);
        creator.writeHeader(bootstrapSuite, out);

        char[] content = caw.toCharArray();
        char[] oldContent = null;

        if (file.exists()) {
            FileReader fr = new FileReader(file);
            int length = (int)file.length();
            int n = 0;
            oldContent = new char[length];
            while (n < length) {
                int count = fr.read(oldContent, n, length - n);
                if (count < 0) {
                    throw new EOFException();
                }
                n += count;
            }
            fr.close();
        }

        if (!Arrays.equals(content, oldContent)) {
            file.delete();
            file.getParentFile().mkdirs();
            FileWriter fw = new FileWriter(file);
            fw.write(content);
            fw.close();
            file.setReadOnly();
            return true;
        } else {
            return false;
        }

    }

    private CHeaderFileCreator(Properties properties) {
        this.map = properties;
    }

    /**
     * Gets the class corresponding to a given name.
     *
     * @param suite      the suite in which to start the lookup
     * @param name       the name of the class
     * @param fieldSpec  the field specification from which the class name was derived
     * @return the Klass instance corresponding to <code>name</code>
     */
    private Klass lookupClass(Suite suite, String name, String fieldSpec) {
        Klass klass = suite.lookup(name);
        if (klass == null) {
            throw new RuntimeException("Can't find the class '" + name + "' specified in '" + fieldSpec + "'");
        }
        return klass;
    }

    /**
     * Verify that the offset definitions in com.sun.squawk.vm.FieldOffsets are correct.
     */
    private void verifyFieldOffsets() {
        Suite suite = VM.getCurrentIsolate().getBootstrapSuite();
        Klass k = suite.lookup("com.sun.squawk.vm.FieldOffsets");
        int count = k.getFieldCount(true);
        for (int i = 0; i != count; ++i) {
            Field field = k.getField(i, true);
            if (field.isPrivate()) {
                continue;
            }
            String name = field.getName();
            long constantValue = field.getPrimitiveConstantValue();
            int offset = FieldOffsets.decodeOffset(constantValue);
            int typeID = FieldOffsets.decodeSystemID(constantValue);

            int indexOf$ = name.indexOf('$');
            if (indexOf$ == -1) {
                throw new RuntimeException("Constant defined in com.sun.squawk.vm.FieldOffsets does not include '$': " + name);
            }

            String className = name.substring(0, indexOf$).replace('_', '.');
            String fieldName = name.substring(indexOf$ + 1);

            Klass klass = lookupClass(suite, className, field.toString());
            boolean found = verifyFieldOffset(field.toString(), offset, typeID, fieldName, klass, false) ||
                            verifyFieldOffset(field.toString(), offset, typeID, fieldName, klass, true);
            if (!found) {
                throw new RuntimeException("Missing definition of '" + className + "." + fieldName + "'");
            }
        }

    }

    /**
     * Searches for a field and confirms that its offset and type are as expected.
     *
     * @param fieldSpec     the string form of the specification from which the field info to be verified was derived
     * @param fieldOffset   the expected offset of the field
     * @param fieldTypeID   the expected system ID of the field's type
     * @param fieldName     the name of the field
     * @param klass         the class in which the field is defined
     * @param isStatic      specifies if the instance or static fields of <code>klass</code> should be searched
     * @return  false if the field was not found, true if it was and verifies correctly
     * @throws RuntimeException if the field was found but did not verify
     */
    private boolean verifyFieldOffset(String fieldSpec, int fieldOffset, int fieldTypeID, String fieldName, Klass klass, boolean isStatic) {
        int fieldCount = klass.getFieldCount(isStatic);
        boolean found = false;
        for (int j = 0; j != fieldCount; ++j) {
            Field squawkField = klass.getField(j, isStatic);
            if (squawkField.getName().equals(fieldName)) {
                int offset = squawkField.getOffset();
                Klass type = squawkField.getType();
                int  systemID = type.getSystemID();
                switch (systemID) {
                    default:          systemID = CID.OBJECT; break;
                    case CID.BOOLEAN: systemID = CID.BYTE; break;
                    case CID.BYTE:
                    case CID.CHAR:
                    case CID.SHORT:
                    case CID.INT:
                    case CID.LONG:
                    case CID.FLOAT:
                    case CID.DOUBLE:  break;
                }
                if (offset != fieldOffset) {
                    throw new RuntimeException("The value of '" + fieldSpec + "' should be " + offset + " not " + fieldOffset);
                }
                if (fieldTypeID != systemID) {
                    throw new RuntimeException("The CID of '" + fieldSpec + "' should be " + fieldTypeID + " not " + systemID);
                }
                found = true;
                break;
            }
        }
        return found;
    }

    /**
     * Verify that the offset definitions in com.sun.squawk.vm.MethodOffsets are correct.
     */
    private void verifyMethodOffsets() {
        boolean errors = false;
        Suite suite = VM.getCurrentIsolate().getBootstrapSuite();
        Klass MethodOffsetsKlass = suite.lookup("com.sun.squawk.vm.MethodOffsets");
        int count = MethodOffsetsKlass.getFieldCount(true);
        for (int i = 0; i != count; ++i) {
            Field field = MethodOffsetsKlass.getField(i, true);
            String name = field.getName();
            int value = (int)field.getPrimitiveConstantValue();
            boolean isStatic = !name.startsWith("virtual$");
            if (!isStatic) {
                name = name.substring("virtual$".length());
            }

            int indexOf$ = name.indexOf('$');
            if (indexOf$ == -1) {
                System.err.println("Constant defined in com.sun.squawk.vm.MethodOffsets does not include '$': " + name);
                errors = true;
            } else {

                String className = name.substring(0, indexOf$).replace('_', '.');
                String nameAndParameters = name.substring(indexOf$ + 1);

                String methodName;
                Klass[] parameters;

                // get the parameter types (if any)
                indexOf$ = nameAndParameters.indexOf('$');
                if (indexOf$ != -1) {
                    // fix up the method name
                    methodName = nameAndParameters.substring(0, indexOf$);

                    // get the parameters
                    StringTokenizer st = new StringTokenizer(nameAndParameters.substring(indexOf$ + 1), "$");
                    parameters = new Klass[st.countTokens()];
                    for (int j = 0; j != parameters.length; ++j) {
                        String typeName = st.nextToken().replace('_', '.');
                        parameters[j] = lookupClass(suite, typeName, field.toString());
                    }
                } else {
                    methodName = nameAndParameters;
                    parameters = null;
                }


                Klass klass = lookupClass(suite, className, field.toString());
                int methodCount = klass.getMethodCount(isStatic);
                boolean found = false;
    nextMethod:
                for (int j = 0; j != methodCount; ++j) {
                    Method squawkMethod = klass.getMethod(j, isStatic);
                    if (squawkMethod.getName().equals(methodName)) {

                        if (parameters != null) {
                            Klass[] types = squawkMethod.getParameterTypes();
                            if (types.length != parameters.length) {
                                continue nextMethod;
                            }
                            for (int k = 0; k != types.length; ++k) {
                                if (types[k] != parameters[k]) {
                                    continue nextMethod;
                                }
                            }
                        }

                        int offset = squawkMethod.getOffset();
                        if (offset != value) {
                            System.err.println("The value of '" + field + "' should be " + offset + " not " + value);
                            errors = true;
                        }
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    System.err.println("Missing definition of '" + className + "." + nameAndParameters + "'");
                    errors = true;
                }
            }
        }
        if (errors) {
            System.exit(-1);
        }
    }

    /**
     * Outputs a C function definition that looks up the name of a global word or reference based
     * on a given index.
     *
     * @param out
     * @param globals
     * @param functionName
     */
    private void outputGlobalNames(PrintWriter out, Hashtable globals, String functionName) {
        out.println("const char* " + functionName + "(int index) {");
        out.println("    switch(index) {");
        for (Enumeration e = globals.keys(); e.hasMoreElements(); ) {
            String name = (String)e.nextElement();
            int offset = ((Integer)globals.get(name)).intValue();
            name = name.replace('.', '_');
            out.println("        case " + offset + ": return \"" + name + "\";");
        }
        out.println("        default: return \"" + functionName + ": unknown global index\";");
        out.println("    }");
        out.println("}");
    }

    /**
     * Create the "rom.h" for the slow VM.
     */
    private void writeHeader(Suite suite, PrintWriter out) throws IOException {

        out.println("/*");
        out.println("* Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.");
        out.println("*");
        out.println("* This software is the proprietary information of Sun Microsystems, Inc.");
        out.println("* Use is subject to license terms.");
        out.println("*");
        out.println("* This is a part of the Squawk JVM.");
        out.println("*/");

        // Write the CID definitions.
        int classCount = suite.getClassCount();
        for (int cid = 0; cid != classCount; cid++) {
            Klass klass = suite.getKlass(cid);

            if (klass.isArray() || klass.getInternalName().charAt(0) == '-') {
                continue;
            }
            out.println("#define "+fix(klass.getName())+" "+cid);

            if (klass.isSynthetic()) {
                continue;
            }

            // Write the instance field getters.
            int fieldCount = klass.getFieldCount(false);
            for (int fid = 0 ; fid != fieldCount; fid++) {
                Field field = klass.getField(fid, false);
                String wholeName = fix(klass.getName() + '_' + field.getName());
                out.print("#define "+wholeName+"(oop) ");
                switch (field.getType().getSystemID()) {
                    case CID.BOOLEAN:
                    case CID.BYTE:  out.print("getByte");       break;
                    case CID.CHAR:  out.print("getUShort");     break;
                    case CID.SHORT: out.print("getShort");      break;
                    case CID.FLOAT:
                    case CID.INT:   out.print("getInt");        break;
                    case CID.DOUBLE:
                    case CID.LONG:  out.print(Klass.SQUAWK_64 ?
                                              "getLong" :
                                              "getLongAtWord"); break;
                    case CID.UWORD:
                    case CID.OFFSET:out.print("getUWord");      break;
                    default:        out.print("getObject");     break;
                }
                out.println("((oop), " + field.getOffset() + ")");
            }

            // Write the instance field setters.
            for (int fid = 0 ; fid != fieldCount; fid++) {
                Field field = klass.getField(fid, false);
                String wholeName = fix(klass.getName() + '_' + field.getName());
                out.print("#define set_"+wholeName+"(oop, value) ");
                switch (field.getType().getSystemID()) {
                    case CID.BOOLEAN:
                    case CID.BYTE:  out.print("setByte");       break;
                    case CID.CHAR:
                    case CID.SHORT: out.print("setShort");      break;
                    case CID.FLOAT:
                    case CID.INT:   out.print("setInt");        break;
                    case CID.DOUBLE:
                    case CID.LONG:  out.print(Klass.SQUAWK_64 ?
                                              "setLong" :
                                              "setLongAtWord"); break;
                    case CID.UWORD:
                    case CID.OFFSET:out.print("setUWord");      break;
                    default:        out.print("setObject");     break;
                }
                out.println("((oop), " + field.getOffset() + ", value)");
            }

            // Write the constants.
            fieldCount = klass.getFieldCount(true);
nextField:  for (int fid = 0; fid != fieldCount; fid++) {
                Field field = klass.getField(fid, true);
                if (field.hasConstant()) {
                    String value;
                    switch (field.getType().getSystemID()) {
                        case CID.BOOLEAN: value = "" + (field.getPrimitiveConstantValue() != 0);     break;
                        case CID.BYTE:    value = "" + (byte) field.getPrimitiveConstantValue();     break;
                        case CID.CHAR:    value = "" + (int)(char)field.getPrimitiveConstantValue(); break;
                        case CID.SHORT:   value = "" + (short)field.getPrimitiveConstantValue();     break;
                        case CID.INT:     value = "" + (int)  field.getPrimitiveConstantValue();     break;
                        case CID.LONG:    value = "JLONG_CONSTANT(" + field.getPrimitiveConstantValue()+ ")";break;
                        case CID.DOUBLE:  value = "" + Double.longBitsToDouble(field.getPrimitiveConstantValue()); break;
                        case CID.FLOAT:   value = "" + Float.intBitsToFloat((int)field.getPrimitiveConstantValue()); break;
                        case CID.STRING:  continue nextField;
                        default: throw new RuntimeException("need another case statement for constants of type " + field.getType().getName());
                    }

                    String name = fix(klass.getName() + '_' + field.getName());
                    if (name.startsWith("com_sun_squawk_vm_")) {
                        name = name.substring("com_sun_squawk_vm_".length());
                    }
                    out.println("#define " + name + " " + value);
                }
            }
        }

        // Verify that the hard coded field and method offsets are correct
        verifyFieldOffsets();
        verifyMethodOffsets();

        // Write the do_XXX entrypoints.
        for (int i = 0 ;; i++) {
            String name = getStringProperty("ENTRYPOINT."+i+".NAME");
            if (name == null) {
                break;
            }
            int addr  = getIntProperty("ENTRYPOINT."+i+".ADDRESS");
            out.println("#define "+name+" Address_add(com_sun_squawk_VM_romStart, "+addr+")");
        }

        // Write the string constant that is the mnemonics for the types
        out.println("const char *AddressType_Mnemonics = \"" + AddressType.Mnemonics + "\";");
        out.println("#if TRACE");

        // Write function that will translate a bytecode into its name.
        out.println("char *getOpcodeName(int code) {");
        out.println("    switch(code) {");
        try {
            for (int i = 0; ; i++) {
                out.println("        case " + i + ": return \"" + Mnemonics.getMnemonic(i) + "\";");
            }
        } catch (IndexOutOfBoundsException e) {
        }
        out.println("        default: return \"Unknown opcode\";");
        out.println("    }");
        out.println("}");

        // Write function equivalent to OPC.hasWide()
        out.println("boolean opcodeHasWide(int code) {");
        out.println("    switch(code) {");
        try {
            for (int i = 0; ; i++) {
                if (OPC.hasWide(i)) {
                    String mnemonic = Mnemonics.getMnemonic(i).toUpperCase();
                    out.println("        case OPC_" + mnemonic + ":");
                }
            }
        } catch (IndexOutOfBoundsException e) {
        }
        out.println("                 return true;");
        out.println("        default: return false;");
        out.println("    }");
        out.println("}");

        // Write function that will translate a global word index into its name.
        outputGlobalNames(out, InstructionEmitter.getGlobalAddrVariables(), "getGlobalAddrName");
        outputGlobalNames(out, InstructionEmitter.getGlobalOopVariables(),  "getGlobalOopName");
        outputGlobalNames(out, InstructionEmitter.getGlobalIntVariables(),  "getGlobalIntName");

        out.println("#endif");

        // Write the accessors for the global Address ints
        Hashtable globalInts = InstructionEmitter.getGlobalIntVariables();
        for (Enumeration e = globalInts.keys() ; e.hasMoreElements() ;) {
            String name = (String)e.nextElement();
            int offset = ((Integer)globalInts.get(name)).intValue();
            name = name.replace('.', '_');
            out.println("#define " + name + " (Ints[" + offset + "])");
        }

        // Write the accessors for the global Address words
        Hashtable globalAddrs = InstructionEmitter.getGlobalAddrVariables();
        for (Enumeration e = globalAddrs.keys() ; e.hasMoreElements() ;) {
            String name = (String)e.nextElement();
            int offset = ((Integer)globalAddrs.get(name)).intValue();
            name = name.replace('.', '_');
            out.println("#define " + name + " (Addrs[" + offset + "])");
        }

        // Write the accessors for the global Oops.
        Hashtable globalOops = InstructionEmitter.getGlobalOopVariables();
        for (Enumeration e = globalOops.keys() ; e.hasMoreElements() ;) {
            String name = (String)e.nextElement();
            int offset = ((Integer)globalOops.get(name)).intValue();
            name = name.replace('.', '_');
            out.println("#define " + name + " (Oops[" + offset + "])");
        }

        // Write the endianess constant
        out.println("#define ROM_BIG_ENDIAN "          + getStringProperty("PMR.BIG_ENDIAN"));
        out.println("#define ROM_REVERSE_PARAMETERS "  + getStringProperty("PMR.REVERSE_PARAMETERS"));

        // Write the definition of the globals.
        out.println("#define ROM_GLOBAL_INT_COUNT  " + getIntProperty("ROM.GLOBAL.INT.COUNT"));
        out.println("#define ROM_GLOBAL_OOP_COUNT  " + getIntProperty("ROM.GLOBAL.OOP.COUNT"));
        out.println("#define ROM_GLOBAL_ADDR_COUNT " + getIntProperty("ROM.GLOBAL.ADDR.COUNT"));

        out.close();
    }

    /**
     * Fixup a sumbol.
     *
     * @param str the symbol name
     * @return the symbol with '.' and '$' turned into '_' and primitive types made upper case
     */
    private static String fix(String str) {
        str = str.replace('.', '_');
        str = str.replace('$', '_');
        if (str.indexOf('_') == -1) {
            str = str.toUpperCase(); // int, float, etc.
        }
        return str;
    }

    /**
     * Get a string property
     *
     * @param name the property name
     * @return the property value
     */
    private String getStringProperty(String name) {
        return map.getProperty(name);
    }

    /**
     * Get an int property
     *
     * @param name the property name
     * @return the property value
     */
    private int getIntProperty(String name) {
        try {
            return Integer.parseInt(getStringProperty(name));
        } catch(NumberFormatException ex) {
            throw new RuntimeException("in getIntProperty("+name+") = " + getStringProperty(name));
        }
    }

}
