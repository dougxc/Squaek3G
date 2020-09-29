/*
  * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
  *
  * This software is the proprietary information of Sun Microsystems, Inc.
  * Use is subject to license terms.
  *
  * This is a part of the Squawk JVM.
  *
  * $Id: AsmHelpers.java,v 1.2 2005/01/31 20:55:54 dl156546 Exp $
  */

package com.sun.squawk.compiler.asm.arm;

import com.sun.squawk.compiler.asm.*;

 /**
  * Helper methods used in the ARM assembler.
  *
  * @author David Liu
  * @version 1.0
  */
class AsmHelpers {
    private AsmHelpers() {
    }

    /**
     * Calculates the 8-bit immediate portion of a 32-bit word that can be represented as an 8-bit
     * immediate rotated right by an even number of bits.
     *
     * @param imm source word
     * @return 8-bit immediate portion of the word
     */
    public static int getPackedImm8(int imm) {
        for (int rotate_imm = 0; rotate_imm < 32; rotate_imm += 2) {
            int imm8 = (imm << rotate_imm) | (imm >>> (32 - rotate_imm));
            if ((imm8 & 0xff) == imm8) {
                return imm8;
            }
        }

        Assert.that(false, "should not reach here");
        return 0;
    }

    /**
     * Calculates the rotation amount of a 32-bit word that can be represented as an 8-bit
     * immediate rotated right by an even number of bits.
     *
     * @param imm source word
     * @return half of the number of bits to rotate right
     */
    public static int getPackedImmRot(int imm) {
        for (int rotate_imm = 0; rotate_imm < 32; rotate_imm += 2) {
            int imm8 = (imm << rotate_imm) | (imm >>> (32 - rotate_imm));
            if ((imm8 & 0xff) == imm8) {
                return rotate_imm / 2;
            }
        }

        Assert.that(false, "should not reach here");
        return 0;
    }

}
