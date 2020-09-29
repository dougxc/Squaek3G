//if[FLASH_MEMORY]
/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk;

import java.io.*;

import com.sun.squawk.io.j2me.flash.*;
import com.sun.squawk.util.*;

/**
 * @author Dave
 *
 */
public class FlashObjectMemoryLoader extends ObjectMemoryLoader {

   private int objectMemorySize;

   private Address memoryAddress;

private int myHash;

private boolean isHashSet = false;

   /**
    * @param reader
    * @param loadIntoReadOnlyMemory
    */
   public FlashObjectMemoryLoader(ObjectMemoryReader reader, boolean loadIntoReadOnlyMemory) {
      super(reader, loadIntoReadOnlyMemory);
   }

   protected byte[] loadMemory(int size) {
      // record the current address of the reader (this is the location of memory)
      memoryAddress = ((FlashObjectMemoryReader)reader).getCurrentAddress();

      // skip ahead size bytes to simulate reading that off the suite file
      reader.skip(size,"simulating flash memory load");

      myHash = reader.readInt("getting hash");
      isHashSet = true;
      byte[] dummy = {}; //return a dummy value to satisfy our superclass
      return dummy;
  }

   protected int getHash(byte[] dummy) {
   		if (!isHashSet) {
   			throw new IllegalStateException("Attempt to get hash before reading it");
   		}
   		return myHash;
   }

   protected void skipMemoryPadding(String parentURI, int memorySize) {
   		FlashObjectMemoryReader r = (FlashObjectMemoryReader)reader;
   		Offset off = r.getCurrentAddressRoundedToWordBoundary().diff(r.getCurrentAddress());
   		r.skip(off.toInt(), "skipping pad");
   }

   protected Address relocateMemory(ObjectMemory parent, byte[] buffer, BitSet oopMap) {

      // Return the previously cached address
      return memoryAddress;
  }

   protected BitSet loadOopMap(int size) {
      //no-op: there is no oopmap in a rom-ized suite file
      return null;
   }

   /**
    * @param i
    */
   public static int getByte(int i) {
      int signedValue = NativeUnsafe.getByte(Address.zero().add(i), 0);
      return signedValue & 0xff;
   }

}


class FlashObjectMemoryReader extends ObjectMemoryReader {
   private Protocol.Pointer pointer;
   /**
    * Creates a <code>ObjectMemoryReader</code> that reads object memory file components
    * from a given input stream.
    *
    * @param   in        the input stream
    * @param   filePath  the file from which <code>in</code> was created
    */
   	public FlashObjectMemoryReader(InputStream in, String filePath) {
   		// replace filePath with a fake URL as the InputStream is already initialised with
   		// the URL that contains the memory address.
   		// The fake URL is designed to ensure that migrated isolates can "find" the application.
   		super(in, getLogicalURL(filePath));

   		//cache the underlying input stream - we will want to talk to that.
   		try {
   			pointer = (Protocol.Pointer) in;
   		} catch (ClassCastException e) {
   			Assert.shouldNotReachHere();
   		}
   	}

   private static String getLogicalURL(String physicalURL) {
   		int index = physicalURL.indexOf('.');
   		String result;
   		if (index == -1) {
   			result = "flash://application";
   		} else {
   			result = "flash://" + physicalURL.substring(index+1);
   		}
		//System.out.println("Calculated logical url " + result + " from physical url " + physicalURL);
		return result;
   }

   public Address getCurrentAddress() {
   		return Address.zero().add(pointer.getCurrentAddress());
   }

   public Address getCurrentAddressRoundedToWordBoundary() {
        return Address.zero().add(4 * ((pointer.getCurrentAddress() + 3) / 4));
   }

//   public final void readEOF() {
//      // no-op
//   }
}
