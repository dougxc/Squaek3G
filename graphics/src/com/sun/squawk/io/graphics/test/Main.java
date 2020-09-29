/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk.io.graphics.test;

import java.io.*;
import com.sun.squawk.*;
import com.sun.squawk.vm.ChannelConstants;

public class Main {

    /**
     * Decodes the events from the J2SE graphics subsystem.
     */
    private static void eventLoop() {
        while(true) {
            long key = VM.getGUIEvent();
            int key1 = (int)(key >> 32);
            int key2 = (int)(key);
            int key1_H = (key1 >> 16) & 0xFFFF;
            int key1_L =  key1 & 0xFFFF;
            int key2_H = (key2 >> 16) & 0xFFFF;
            int key2_L =  key2 & 0xFFFF;
            switch (key1_H) {
                case ChannelConstants.GUIIN_REPAINT: {
                    System.out.println("Repaint request");
                    break;
                }
                case ChannelConstants.GUIIN_KEY: {
                    System.out.println("Key press: "+(char)key2_L);
                    break;
                }
                case ChannelConstants.GUIIN_MOUSE: {
                    System.out.println("Mouse press: x="+key2_H+" y="+key2_L);
                    break;
                }
                case ChannelConstants.GUIIN_EXIT: {
                    System.exit(0);
                    break;
                }
                case ChannelConstants.GUIIN_HIBERNATE: {
                    try {
                        VM.getCurrentIsolate().hibernate();
                    } catch (IOException ex) {
                        System.err.println("Error while hibernating isolate: ");
                        ex.printStackTrace();
                    }
                    break;
                }
                default: {
                    System.out.println("Bad GUI input event "+key1_H+":"+key1_L+":"+key2_H+":"+key2_L);
                }
            }
        }
    }

    /**
     * Entry Point
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        VM.execGraphicsIO(ChannelConstants.SETCOLOR, 0, 0, 0, 0, 0, 0, null, null);
        VM.execGraphicsIO(ChannelConstants.DRAWLINE, 30, 30, 150, 150, 0, 0, null, null);
        VM.execGraphicsIO(ChannelConstants.DRAWSTRING, 100, 200, 0, 0, 0, 0, "Hello World", null);
        eventLoop();
    }

}
