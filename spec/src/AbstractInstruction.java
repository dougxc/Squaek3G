/*
 * Copyright 2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
 
//
//  AbstractInstruction.java
//
//  Created by Edward Carter on 5/26/05.
//

import java.io.PrintWriter;
import java.lang.reflect.Field;

public abstract class AbstractInstruction implements Instruction {
    public void printOperation(PrintWriter out) {
        out.println("FIXME: Not done yet!");
    }

    public void printFormat(PrintWriter out) {
        out.println("<Table Border=\"1\"><tr><td><i>" + getName() + "</i></td></tr></Table>");
    }

    public void printForms(PrintWriter out) {
        printForm(out, getName(), getOpcode());
    }

    protected final void printForm(PrintWriter out, String name, int opcode) {
        sbdocgen.reportOpcode(opcode);

        out.print("<i>" + name + "</i> = ");
        out.print(opcode);
        String h = Integer.toHexString(opcode);
        while (h.length() < 2)
            h = "0" + h;
        out.println(" (0x" + h + ")");
    }

    static final Class opcClass;
    static {
        try {
            opcClass = Class.forName("com.sun.squawk.vm.OPC");
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException("Could not find class com.sun.squawk.vm.OPC");
        }
    }

    final int getOpcode() {
        String fieldName = getName().toUpperCase();
        try {
            Field field = opcClass.getDeclaredField(fieldName);
            int opcode = field.getInt(null);
            return opcode;
        } catch (IllegalAccessException ex) {
            throw new RuntimeException("could not get value of com.sun.squawk.vm.OPC." + fieldName, ex);
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("could not get value of com.sun.squawk.vm.OPC." + fieldName, ex);
        } catch (SecurityException ex) {
            throw new RuntimeException("could not get value of com.sun.squawk.vm.OPC." + fieldName, ex);
        } catch (NoSuchFieldException ex) {
            throw new RuntimeException("could not get value of com.sun.squawk.vm.OPC." + fieldName, ex);
        }
    }

    public void printOperandStack(PrintWriter out) {
        out.println("FIXME: Not done yet!");
    }

    public void printDescription(PrintWriter out) {
        out.println("FIXME: Not done yet!");
    }

    public boolean hasNotes() {
        return false;
    }

    public void printNotes(PrintWriter out) {
        return;
    }

    public boolean hasExceptions() {
        return false;
    }

    public void printExceptions(PrintWriter out) {
        return;
    }

    public int compareTo(Object o) {
        Instruction i = (Instruction)o;
        return getName().toLowerCase().compareTo(i.getName().toLowerCase());
    }
}




