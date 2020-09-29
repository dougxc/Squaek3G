/*
 * @(#)UTFConverter.java	1.3 02/07/10
 *
 * Copyright 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.sun.tck.cldc.javatest.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.EOFException;
import java.util.Vector;

public class UTFConverter {

    private UTFConverter() {}
    
    public static void stringsToUTFStream(String[] s, OutputStream os) {
        DataOutputStream dos = new DataOutputStream(os);
        try { 
            for (int i = 0; i < s.length; i++) {
                dos.writeUTF(s[i]);
            }
            dos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

     public static String[] UTFStreamToStrings(InputStream is) {
        DataInputStream dis = new DataInputStream(is);
        Vector v = new Vector();
        try { 
            while (true) {
                v.addElement(dis.readUTF());
            }
        } catch (EOFException e) {
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                dis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        String[] res = new String[v.size()];
        v.copyInto(res);
        return res;
    }

   public static byte[] stringsToBytes(String[] s) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        stringsToUTFStream(s, baos);
        return baos.toByteArray();
    }

    public static String[] bytesToStrings(byte[] b) {
        return UTFStreamToStrings(new ByteArrayInputStream(b));
    }

}
