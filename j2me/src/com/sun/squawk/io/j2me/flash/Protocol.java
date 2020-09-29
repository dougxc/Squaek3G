//if[FLASH_MEMORY]
/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk.io.j2me.flash;

import java.io.*;
import javax.microedition.io.*;

import com.sun.squawk.*;
import com.sun.squawk.Unsafe;
import com.sun.squawk.io.*;

/**
 *
 */
public class Protocol extends ConnectionBase implements InputConnection {

   private int memoryBase;

   public int getMemoryBase() {
      return memoryBase;
   }
   public InputStream openInputStream( ) {
      return new FlashInputStream(this);
   }
   public DataInputStream openDataInputStream( ) {
      return new FlashDataInputStream(new FlashInputStream(this));
   }
   public Connection open(String protocol, String url, int mode, boolean timeouts) throws IOException {
      if (mode != Connector.READ) {
            throw new IOException("illegal mode: " + mode);
      }
      // strip any logical name off the end of the url
      int dotIndex = url.indexOf('.');
      if (dotIndex == -1) {
      	dotIndex =url.length();
      }
      this.memoryBase = Integer.parseInt(url.substring(2,dotIndex),16);
      //System.out.println("Got memoryBase " + Integer.toHexString(memoryBase) + " from url " + url);
      return this;
   }

   public interface Pointer {
       public int getCurrentAddress();
   }

   static class FlashDataInputStream extends DataInputStream implements Pointer {
       public FlashDataInputStream(FlashInputStream mis) {
           super(mis);
       }

       public int getCurrentAddress() {
           return ((FlashInputStream)in).getCurrentAddress();
       }
   }

   static class FlashInputStream extends InputStream implements Pointer {
      private Protocol parent;
      private int currentMemoryPointer;

      public FlashInputStream(Protocol protocol) {
         parent = protocol;
         currentMemoryPointer = 0;
      }

      public int read() throws IOException {
          int signedValue = Unsafe.getByte(Address.zero().add(getCurrentAddress()), 0);
          int result = signedValue & 0xff;
//            FlashObjectMemoryLoader.getByte(getCurrentAddress());
//         VM.println("reading a byte: at [" + getCurrentAddress() + "] value: [" + result + "]");
         currentMemoryPointer++;
         return result;
      }

      public long skip(long n) throws IOException {
         currentMemoryPointer = (int) (n + currentMemoryPointer);
         return n;
     }

      public int getCurrentAddress() {
         return parent.getMemoryBase() + currentMemoryPointer;
      }
   }
}

