//if[FLOATS]   /* This will selectively exclude the entire file from the build */

/*
 * @(#)FloatingDecimal.java 1.16 99/12/04
 *
 * Copyright 1996-1999 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */

package java.lang;

class FloatingDecimal {
    boolean isExceptional;
    boolean isNegative;
    int decExponent;
    char digits[];
    int nDigits;
    int bigIntExp;
    int bigIntNBits;
    boolean mustSetRoundDir = false;
    int roundDir; // set by doubleValue

    private FloatingDecimal(boolean negSign, int decExponent, char[] digits, int n, boolean e) {
        isNegative = negSign;
        isExceptional = e;
        this.decExponent = decExponent;
        this.digits = digits;
        this.nDigits = n;
    }

    /*
     * Constants of the implementation
     * Most are IEEE-754 related.
     * (There are more really boring constants at the end.)
     */
    static final long signMask = 0x8000000000000000L;
    static final long expMask = 0x7ff0000000000000L;
    static final long fractMask = ~ (signMask | expMask);
    static final int expShift = 52;
    static final int expBias = 1023;
    static final long fractHOB = (1L<<expShift); // assumed High-Order bit
    static final long expOne = ( (long) expBias)<<expShift; // exponent of 1.0
    static final int maxSmallBinExp = 62;
    static final int minSmallBinExp = - (63 / 3);
    static final int maxDecimalDigits = 15;
    static final int maxDecimalExponent = 308;
    static final int minDecimalExponent = -324;
    static final int bigDecimalExponent = 324; // i.e. abs(minDecimalExponent)

    static final long highbyte = 0xff00000000000000L;
    static final long highbit = 0x8000000000000000L;
    static final long lowbytes = ~highbyte;

    static final int singleSignMask = 0x80000000;
    static final int singleExpMask = 0x7f800000;
    static final int singleFractMask = ~ (singleSignMask | singleExpMask);
    static final int singleExpShift = 23;
    static final int singleFractHOB = 1 << singleExpShift;
    static final int singleExpBias = 127;
    static final int singleMaxDecimalDigits = 7;
    static final int singleMaxDecimalExponent = 38;
    static final int singleMinDecimalExponent = -45;

    static final int intDecimalDigits = 9;

    /*
     * count number of bits from high-order 1 bit to low-order 1 bit,
     * inclusive.
     */
    private static int
        countBits(long v) {
        //
        // the strategy is to shift until we get a non-zero sign bit
        // then shift until we have no bits left, counting the difference.
        // we do byte shifting as a hack. Hope it helps.
        //
        if (v == 0L)
            return 0;

        while ( (v & highbyte) == 0L) {
            v <<= 8;
        } while (v > 0L) { // i.e. while ((v&highbit) == 0L )
            v <<= 1;
        }

        int n = 0;
        while ( (v & lowbytes) != 0L) {
            v <<= 8;
            n += 8;
        } while (v != 0L) {
            v <<= 1;
            n += 1;
        }
        return n;
    }

    /*
     * Keep big powers of 5 handy for future reference.
     */
    private static FDBigInt b5p[];

    private static synchronized FDBigInt
        big5pow(int p) {
        if (p < 0)
            throw new RuntimeException("Assertion botch: negative power of 5");
        if (b5p == null) {
            b5p = new FDBigInt[p + 1];
        } else if (b5p.length <= p) {
            FDBigInt t[] = new FDBigInt[p + 1];
            System.arraycopy(b5p, 0, t, 0, b5p.length);
            b5p = t;
        }
        if (b5p[p] != null)
            return b5p[p];
        else if (p < small5pow.length)
            return b5p[p] = new FDBigInt(small5pow[p]);
        else if (p < long5pow.length)
            return b5p[p] = new FDBigInt(long5pow[p]);
        else {
            // construct the value.
            // recursively.
            int q, r;
            // in order to compute 5^p,
            // compute its square root, 5^(p/2) and square.
            // or, let q = p / 2, r = p -q, then
            // 5^p = 5^(q+r) = 5^q * 5^r
            q = p >> 1;
            r = p - q;
            FDBigInt bigq = b5p[q];
            if (bigq == null)
                bigq = big5pow(q);
            if (r < small5pow.length) {
                return (b5p[p] = bigq.mult(small5pow[r]));
            } else {
                FDBigInt bigr = b5p[r];
                if (bigr == null)
                    bigr = big5pow(r);
                return (b5p[p] = bigq.mult(bigr));
            }
        }
    }

    //
    // a common operation
    //
    private static FDBigInt
        multPow52(FDBigInt v, int p5, int p2) {
        if (p5 != 0) {
            if (p5 < small5pow.length) {
                v = v.mult(small5pow[p5]);
            } else {
                v = v.mult(big5pow(p5));
            }
        }
        if (p2 != 0) {
            v.lshiftMe(p2);
        }
        return v;
    }

    //
    // another common operation
    //
    private static FDBigInt
        constructPow52(int p5, int p2) {
        FDBigInt v = new FDBigInt(big5pow(p5));
        if (p2 != 0) {
            v.lshiftMe(p2);
        }
        return v;
    }

    /*
     * Make a floating double into a FDBigInt.
     * This could also be structured as a FDBigInt
     * constructor, but we'd have to build a lot of knowledge
     * about floating-point representation into it, and we don't want to.
     *
     * AS A SIDE EFFECT, THIS METHOD WILL SET THE INSTANCE VARIABLES
     * bigIntExp and bigIntNBits
     *
     */
    private FDBigInt
        doubleToBigInt(double dval) {
        long lbits = Double.doubleToLongBits(dval) & ~signMask;
        int binexp = (int) (lbits >>> expShift);
        lbits &= fractMask;
        if (binexp > 0) {
            lbits |= fractHOB;
        } else {
            if (lbits == 0L)
                throw new RuntimeException("Assertion botch: doubleToBigInt(0.0)");
            binexp += 1;
            while ( (lbits & fractHOB) == 0L) {
                lbits <<= 1;
                binexp -= 1;
            }
        }
        binexp -= expBias;
        int nbits = countBits(lbits);
        /*
         * We now know where the high-order 1 bit is,
         * and we know how many there are.
         */
        int lowOrderZeros = expShift + 1 - nbits;
        lbits >>>= lowOrderZeros;

        bigIntExp = binexp + 1 - nbits;
        bigIntNBits = nbits;
        return new FDBigInt(lbits);
    }

    /*
     * Compute a number that is the ULP of the given value,
     * for purposes of addition/subtraction. Generally easy.
     * More difficult if subtracting and the argument
     * is a normalized a power of 2, as the ULP changes at these points.
     */
    private static double
        ulp(double dval, boolean subtracting) {
        long lbits = Double.doubleToLongBits(dval) & ~signMask;
        int binexp = (int) (lbits >>> expShift);
        double ulpval;
        if (subtracting && (binexp >= expShift) && ( (lbits & fractMask) == 0L)) {
            // for subtraction from normalized, powers of 2,
            // use next-smaller exponent
            binexp -= 1;
        }
        if (binexp > expShift) {
            ulpval = Double.longBitsToDouble( ( (long) (binexp - expShift)) << expShift);
        } else if (binexp == 0) {
            ulpval = Double.MIN_VALUE;
        } else {
            ulpval = Double.longBitsToDouble(1L << (binexp - 1));
        }
        if (subtracting)
            ulpval = -ulpval;

        return ulpval;
    }

    /*
     * Round a double to a float.
     * In addition to the fraction bits of the double,
     * look at the class instance variable roundDir,
     * which should help us avoid double-rounding error.
     * roundDir was set in hardValueOf if the estimate was
     * close enough, but not exact. It tells us which direction
     * of rounding is preferred.
     */
    float
        stickyRound(double dval) {
        long lbits = Double.doubleToLongBits(dval);
        long binexp = lbits & expMask;
        if (binexp == 0L || binexp == expMask) {
            // what we have here is special.
            // don't worry, the right thing will happen.
            return (float) dval;
        }
        lbits += (long) roundDir; // hack-o-matic.
        return (float) Double.longBitsToDouble(lbits);
    }

    /*
     * This is the easy subcase --
     * all the significant bits, after scaling, are held in lvalue.
     * negSign and decExponent tell us what processing and scaling
     * has already been done. Exceptional cases have already been
     * stripped out.
     * In particular:
     * lvalue is a finite number (not Inf, nor NaN)
     * lvalue > 0L (not zero, nor negative).
     *
     * The only reason that we develop the digits here, rather than
     * calling on Long.toString() is that we can do it a little faster,
     * and besides want to treat trailing 0s specially. If Long.toString
     * changes, we should re-evaluate this strategy!
     */
    private void
        developLongDigits(int decExponent, long lvalue, long insignificant) {
        char digits[];
        int ndigits;
        int digitno;
        int c;
        //
        // Discard non-significant low-order bits, while rounding,
        // up to insignificant value.
        int i;
        for (i = 0; insignificant >= 10L; i++)
            insignificant /= 10L;
        if (i != 0) {
            long pow10 = long5pow[i] << i; // 10^i == 5^i * 2^i;
            long residue = lvalue % pow10;
            lvalue /= pow10;
            decExponent += i;
            if (residue >= (pow10 >> 1)) {
                // round up based on the low-order bits we're discarding
                lvalue++;
            }
        }
        if (lvalue <= Integer.MAX_VALUE) {
            if (lvalue <= 0L)
                throw new RuntimeException("Assertion botch: value " + lvalue + " <= 0");

            // even easier subcase!
            // can do int arithmetic rather than long!
            int ivalue = (int) lvalue;
            digits = new char[ndigits = 10];
            digitno = ndigits - 1;
            c = ivalue % 10;
            ivalue /= 10;
            while (c == 0) {
                decExponent++;
                c = ivalue % 10;
                ivalue /= 10;
            } while (ivalue != 0) {
                digits[digitno--] = (char) (c + '0');
                decExponent++;
                c = ivalue % 10;
                ivalue /= 10;
            }
            digits[digitno] = (char) (c + '0');
        } else {
            // same algorithm as above (same bugs, too )
            // but using long arithmetic.
            digits = new char[ndigits = 20];
            digitno = ndigits - 1;
            c = (int) (lvalue % 10L);
            lvalue /= 10L;
            while (c == 0) {
                decExponent++;
                c = (int) (lvalue % 10L);
                lvalue /= 10L;
            } while (lvalue != 0L) {
                digits[digitno--] = (char) (c + '0');
                decExponent++;
                c = (int) (lvalue % 10L);
                lvalue /= 10;
            }
            digits[digitno] = (char) (c + '0');
        }
        char result[];
        ndigits -= digitno;
        if (digitno == 0)
            result = digits;
        else {
            result = new char[ndigits];
            System.arraycopy(digits, digitno, result, 0, ndigits);
        }
        this.digits = result;
        this.decExponent = decExponent + 1;
        this.nDigits = ndigits;
    }

    //
    // add one to the least significant digit.
    // in the unlikely event there is a carry out,
    // deal with it.
    // assert that this will only happen where there
    // is only one digit, e.g. (float)1e-44 seems to do it.
    //
    private void
        roundup() {
        int i;
        int q = digits[i = (nDigits - 1)];
        if (q == '9') {
            while (q == '9' && i > 0) {
                digits[i] = '0';
                q = digits[--i];
            }
            if (q == '9') {
                // carryout! High-order 1, rest 0s, larger exp.
                decExponent += 1;
                digits[0] = '1';
                return;
            }
            // else fall through.
        }
        digits[i] = (char) (q + 1);
    }

    /*
     * FIRST IMPORTANT CONSTRUCTOR: DOUBLE
     */
    public FloatingDecimal(double d) {
        long dBits = Double.doubleToLongBits(d);
        long fractBits;
        int binExp;
        int nSignificantBits;

        // discover and delete sign
        if ( (dBits & signMask) != 0) {
            isNegative = true;
            dBits ^= signMask;
        } else {
            isNegative = false;
        }
        // Begin to unpack
        // Discover obvious special cases of NaN and Infinity.
        binExp = (int) ( (dBits & expMask) >> expShift);
        fractBits = dBits & fractMask;
        if (binExp == (int) (expMask >> expShift)) {
            isExceptional = true;
            if (fractBits == 0L) {
                digits = infinity;
            } else {
                digits = notANumber;
                isNegative = false; // NaN has no sign!
            }
            nDigits = digits.length;
            return;
        }
        isExceptional = false;
        // Finish unpacking
        // Normalize denormalized numbers.
        // Insert assumed high-order bit for normalized numbers.
        // Subtract exponent bias.
        if (binExp == 0) {
            if (fractBits == 0L) {
                // not a denorm, just a 0!
                decExponent = 0;
                digits = zero;
                nDigits = 1;
                return;
            } while ( (fractBits & fractHOB) == 0L) {
                fractBits <<= 1;
                binExp -= 1;
            }
            nSignificantBits = expShift + binExp + 1; // recall binExp is  - shift count.
            binExp += 1;
        } else {
            fractBits |= fractHOB;
            nSignificantBits = expShift + 1;
        }
        binExp -= expBias;
        // call the routine that actually does all the hard work.
        dtoa(binExp, fractBits, nSignificantBits);
    }

    /*
     * SECOND IMPORTANT CONSTRUCTOR: SINGLE
     */
    public FloatingDecimal(float f) {
        int fBits = Float.floatToIntBits(f);
        int fractBits;
        int binExp;
        int nSignificantBits;

        // discover and delete sign
        if ( (fBits & singleSignMask) != 0) {
            isNegative = true;
            fBits ^= singleSignMask;
        } else {
            isNegative = false;
        }
        // Begin to unpack
        // Discover obvious special cases of NaN and Infinity.
        binExp = (int) ( (fBits & singleExpMask) >> singleExpShift);
        fractBits = fBits & singleFractMask;
        if (binExp == (int) (singleExpMask >> singleExpShift)) {
            isExceptional = true;
            if (fractBits == 0L) {
                digits = infinity;
            } else {
                digits = notANumber;
                isNegative = false; // NaN has no sign!
            }
            nDigits = digits.length;
            return;
        }
        isExceptional = false;
        // Finish unpacking
        // Normalize denormalized numbers.
        // Insert assumed high-order bit for normalized numbers.
        // Subtract exponent bias.
        if (binExp == 0) {
            if (fractBits == 0) {
                // not a denorm, just a 0!
                decExponent = 0;
                digits = zero;
                nDigits = 1;
                return;
            } while ( (fractBits & singleFractHOB) == 0) {
                fractBits <<= 1;
                binExp -= 1;
            }
            nSignificantBits = singleExpShift + binExp + 1; // recall binExp is  - shift count.
            binExp += 1;
        } else {
            fractBits |= singleFractHOB;
            nSignificantBits = singleExpShift + 1;
        }
        binExp -= singleExpBias;
        // call the routine that actually does all the hard work.
        dtoa(binExp, ( (long) fractBits) << (expShift - singleExpShift), nSignificantBits);
    }

    private void
        dtoa(int binExp, long fractBits, int nSignificantBits) {
        int nFractBits; // number of significant bits of fractBits;
        int nTinyBits; // number of these to the right of the point.
        int decExp;

        // Examine number. Determine if it is an easy case,
        // which we can do pretty trivially using float/long conversion,
        // or whether we must do real work.
        nFractBits = countBits(fractBits);
        nTinyBits = Math.max(0, nFractBits - binExp - 1);
        if (binExp <= maxSmallBinExp && binExp >= minSmallBinExp) {
            // Look more closely at the number to decide if,
            // with scaling by 10^nTinyBits, the result will fit in
            // a long.
            if ( (nTinyBits < long5pow.length) && ( (nFractBits + n5bits[nTinyBits]) < 64)) {
                /*
                 * We can do this:
                 * take the fraction bits, which are normalized.
                 * (a) nTinyBits == 0: Shift left or right appropriately
                 *     to align the binary point at the extreme right, i.e.
                 *     where a long int point is expected to be. The integer
                 *     result is easily converted to a string.
                 * (b) nTinyBits > 0: Shift right by expShift-nFractBits,
                 *     which effectively converts to long and scales by
                 *     2^nTinyBits. Then multiply by 5^nTinyBits to
                 *     complete the scaling. We know this won't overflow
                 *     because we just counted the number of bits necessary
                 *     in the result. The integer you get from this can
                 *     then be converted to a string pretty easily.
                 */
                long halfULP;
                if (nTinyBits == 0) {
                    if (binExp > nSignificantBits) {
                        halfULP = 1L << (binExp - nSignificantBits - 1);
                    } else {
                        halfULP = 0L;
                    }
                    if (binExp >= expShift) {
                        fractBits <<= (binExp - expShift);
                    } else {
                        fractBits >>>= (expShift - binExp);
                    }
                    developLongDigits(0, fractBits, halfULP);
                    return;
                }
                /*
                 * The following causes excess digits to be printed
                 * out in the single-float case. Our manipulation of
                 * halfULP here is apparently not correct. If we
                 * better understand how this works, perhaps we can
                 * use this special case again. But for the time being,
                 * we do not.
                 * else {
                 *     fractBits >>>= expShift+1-nFractBits;
                 *     fractBits *= long5pow[ nTinyBits ];
                 *     halfULP = long5pow[ nTinyBits ] >> (1+nSignificantBits-nFractBits);
                 *     developLongDigits( -nTinyBits, fractBits, halfULP );
                 *     return;
                 * }
                 */
            }
        }
        /*
         * This is the hard case. We are going to compute large positive
         * integers B and S and integer decExp, s.t.
         *  d = ( B / S ) * 10^decExp
         *  1 <= B / S < 10
         * Obvious choices are:
         *  decExp = floor( log10(d) )
         *  B      = d * 2^nTinyBits * 10^max( 0, -decExp )
         *  S      = 10^max( 0, decExp) * 2^nTinyBits
         * (noting that nTinyBits has already been forced to non-negative)
         * I am also going to compute a large positive integer
         *  M      = (1/2^nSignificantBits) * 2^nTinyBits * 10^max( 0, -decExp )
         * i.e. M is (1/2) of the ULP of d, scaled like B.
         * When we iterate through dividing B/S and picking off the
         * quotient bits, we will know when to stop when the remainder
         * is <= M.
         *
         * We keep track of powers of 2 and powers of 5.
         */

        /*
         * Estimate decimal exponent. (If it is small-ish,
         * we could double-check.)
         *
         * First, scale the mantissa bits such that 1 <= d2 < 2.
         * We are then going to estimate
         *      log10(d2) ~=~  (d2-1.5)/1.5 + log(1.5)
         * and so we can estimate
         *      log10(d) ~=~ log10(d2) + binExp * log10(2)
         * take the floor and call it decExp.
         * FIXME -- use more precise constants here. It costs no more.
         */
        double d2 = Double.longBitsToDouble(
            expOne | (fractBits & ~fractHOB));
        decExp = (int) Math.floor(
            (d2 - 1.5D) * 0.289529654D + 0.176091259 + (double) binExp * 0.301029995663981);
        int B2, B5; // powers of 2 and powers of 5, respectively, in B
        int S2, S5; // powers of 2 and powers of 5, respectively, in S
        int M2, M5; // powers of 2 and powers of 5, respectively, in M
        int Bbits; // binary digits needed to represent B, approx.
        int tenSbits; // binary digits needed to represent 10*S, approx.
        FDBigInt Sval, Bval, Mval;

        B5 = Math.max(0, -decExp);
        B2 = B5 + nTinyBits + binExp;

        S5 = Math.max(0, decExp);
        S2 = S5 + nTinyBits;

        M5 = B5;
        M2 = B2 - nSignificantBits;

        /*
         * the long integer fractBits contains the (nFractBits) interesting
         * bits from the mantissa of d ( hidden 1 added if necessary) followed
         * by (expShift+1-nFractBits) zeros. In the interest of compactness,
         * I will shift out those zeros before turning fractBits into a
         * FDBigInt. The resulting whole number will be
         *  d * 2^(nFractBits-1-binExp).
         */
        fractBits >>>= (expShift + 1 - nFractBits);
        B2 -= nFractBits - 1;
        int common2factor = Math.min(B2, S2);
        B2 -= common2factor;
        S2 -= common2factor;
        M2 -= common2factor;

        /*
         * HACK!! For exact powers of two, the next smallest number
         * is only half as far away as we think (because the meaning of
         * ULP changes at power-of-two bounds) for this reason, we
         * hack M2. Hope this works.
         */
        if (nFractBits == 1)
            M2 -= 1;

        if (M2 < 0) {
            // oops.
            // since we cannot scale M down far enough,
            // we must scale the other values up.
            B2 -= M2;
            S2 -= M2;
            M2 = 0;
        }
        /*
         * Construct, Scale, iterate.
         * Some day, we'll write a stopping test that takes
         * account of the assymetry of the spacing of floating-point
         * numbers below perfect powers of 2
         * 26 Sept 96 is not that day.
         * So we use a symmetric test.
         */
        char digits[] = this.digits = new char[18];
        int ndigit = 0;
        boolean low, high;
        long lowDigitDifference;
        int q;

        /*
         * Detect the special cases where all the numbers we are about
         * to compute will fit in int or long integers.
         * In these cases, we will avoid doing FDBigInt arithmetic.
         * We use the same algorithms, except that we "normalize"
         * our FDBigInts before iterating. This is to make division easier,
         * as it makes our fist guess (quotient of high-order words)
         * more accurate!
         *
         * Some day, we'll write a stopping test that takes
         * account of the assymetry of the spacing of floating-point
         * numbers below perfect powers of 2
         * 26 Sept 96 is not that day.
         * So we use a symmetric test.
         */
        Bbits = nFractBits + B2 + ( (B5 < n5bits.length) ? n5bits[B5] : (B5 * 3));
        tenSbits = S2 + 1 + ( ( (S5 + 1) < n5bits.length) ? n5bits[ (S5 + 1)] : ( (S5 + 1) * 3));
        if (Bbits < 64 && tenSbits < 64) {
            if (Bbits < 32 && tenSbits < 32) {
                // wa-hoo! They're all ints!
                int b = ( (int) fractBits * small5pow[B5]) << B2;
                int s = small5pow[S5] << S2;
                int m = small5pow[M5] << M2;
                int tens = s * 10;
                /*
                 * Unroll the first iteration. If our decExp estimate
                 * was too high, our first quotient will be zero. In this
                 * case, we discard it and decrement decExp.
                 */
                ndigit = 0;
                q = (int) (b / s);
                b = 10 * (b % s);
                m *= 10;
                low = (b < m);
                high = (b + m > tens);
                if (q >= 10) {
                    // bummer, dude
                    throw new RuntimeException("Assertion botch: excessivly large digit " + q);
                } else if ( (q == 0) && !high) {
                    // oops. Usually ignore leading zero.
                    decExp--;
                } else {
                    digits[ndigit++] = (char) ('0' + q);
                }
                /*
                 * HACK! Java spec sez that we always have at least
                 * one digit after the . in either F- or E-form output.
                 * Thus we will need more than one digit if we're using
                 * E-form
                 */
                if (decExp <= -3 || decExp >= 8) {
                    high = low = false;
                } while (!low && !high) {
                    q = (int) (b / s);
                    b = 10 * (b % s);
                    m *= 10;
                    if (q >= 10) {
                        // bummer, dude
                        throw new RuntimeException("Assertion botch: excessivly large digit " + q);
                    }
                    if (m > 0L) {
                        low = (b < m);
                        high = (b + m > tens);
                    } else {
                        // hack -- m might overflow!
                        // in this case, it is certainly > b,
                        // which won't
                        // and b+m > tens, too, since that has overflowed
                        // either!
                        low = true;
                        high = true;
                    }
                    digits[ndigit++] = (char) ('0' + q);
                }
                lowDigitDifference = (b << 1) - tens;
            } else {
                // still good! they're all longs!
                long b = (fractBits * long5pow[B5]) << B2;
                long s = long5pow[S5] << S2;
                long m = long5pow[M5] << M2;
                long tens = s * 10L;
                /*
                 * Unroll the first iteration. If our decExp estimate
                 * was too high, our first quotient will be zero. In this
                 * case, we discard it and decrement decExp.
                 */
                ndigit = 0;
                q = (int) (b / s);
                b = 10L * (b % s);
                m *= 10L;
                low = (b < m);
                high = (b + m > tens);
                if (q >= 10) {
                    // bummer, dude
                    throw new RuntimeException("Assertion botch: excessively large digit " + q);
                } else if ( (q == 0) && !high) {
                    // oops. Usually ignore leading zero.
                    decExp--;
                } else {
                    digits[ndigit++] = (char) ('0' + q);
                }
                /*
                 * HACK! Java spec sez that we always have at least
                 * one digit after the . in either F- or E-form output.
                 * Thus we will need more than one digit if we're using
                 * E-form
                 */
                if (decExp <= -3 || decExp >= 8) {
                    high = low = false;
                } while (!low && !high) {
                    q = (int) (b / s);
                    b = 10 * (b % s);
                    m *= 10;
                    if (q >= 10) {
                        // bummer, dude
                        throw new RuntimeException("Assertion botch: excessivly large digit " + q);
                    }
                    if (m > 0L) {
                        low = (b < m);
                        high = (b + m > tens);
                    } else {
                        // hack -- m might overflow!
                        // in this case, it is certainly > b,
                        // which won't
                        // and b+m > tens, too, since that has overflowed
                        // either!
                        low = true;
                        high = true;
                    }
                    digits[ndigit++] = (char) ('0' + q);
                }
                lowDigitDifference = (b << 1) - tens;
            }
        } else {
            FDBigInt tenSval;
            int shiftBias;

            /*
             * We really must do FDBigInt arithmetic.
             * Fist, construct our FDBigInt initial values.
             */
            Bval = multPow52(new FDBigInt(fractBits), B5, B2);
            Sval = constructPow52(S5, S2);
            Mval = constructPow52(M5, M2);

            // normalize so that division works better
            Bval.lshiftMe(shiftBias = Sval.normalizeMe());
            Mval.lshiftMe(shiftBias);
            tenSval = Sval.mult(10);
            /*
             * Unroll the first iteration. If our decExp estimate
             * was too high, our first quotient will be zero. In this
             * case, we discard it and decrement decExp.
             */
            ndigit = 0;
            q = Bval.quoRemIteration(Sval);
            Mval = Mval.mult(10);
            low = (Bval.cmp(Mval) < 0);
            high = (Bval.add(Mval).cmp(tenSval) > 0);
            if (q >= 10) {
                // bummer, dude
                throw new RuntimeException("Assertion botch: excessivly large digit " + q);
            } else if ( (q == 0) && !high) {
                // oops. Usually ignore leading zero.
                decExp--;
            } else {
                digits[ndigit++] = (char) ('0' + q);
            }
            /*
             * HACK! Java spec sez that we always have at least
             * one digit after the . in either F- or E-form output.
             * Thus we will need more than one digit if we're using
             * E-form
             */
            if (decExp <= -3 || decExp >= 8) {
                high = low = false;
            } while (!low && !high) {
                q = Bval.quoRemIteration(Sval);
                Mval = Mval.mult(10);
                if (q >= 10) {
                    // bummer, dude
                    throw new RuntimeException("Assertion botch: excessivly large digit " + q);
                }
                low = (Bval.cmp(Mval) < 0);
                high = (Bval.add(Mval).cmp(tenSval) > 0);
                digits[ndigit++] = (char) ('0' + q);
            }
            if (high && low) {
                Bval.lshiftMe(1);
                lowDigitDifference = Bval.cmp(tenSval);
            } else
                lowDigitDifference = 0L; // this here only for flow analysis!
        }
        this.decExponent = decExp + 1;
        this.digits = digits;
        this.nDigits = ndigit;
        /*
         * Last digit gets rounded based on stopping condition.
         */
        if (high) {
            if (low) {
                if (lowDigitDifference == 0L) {
                    // it's a tie!
                    // choose based on which digits we like.
                    if ( (digits[nDigits - 1] & 1) != 0)
                        roundup();
                } else if (lowDigitDifference > 0) {
                    roundup();
                }
            } else {
                roundup();
            }
        }
    }

    public String
        toString() {
        // most brain-dead version
        StringBuffer result = new StringBuffer(nDigits + 8);
        if (isNegative) {
            result.append('-');
        }
        if (isExceptional) {
            result.append(digits, 0, nDigits);
        } else {
            result.append("0.");
            result.append(digits, 0, nDigits);
            result.append('e');
            result.append(decExponent);
        }
        return new String(result);
    }

    public String
        toJavaFormatString() {
        char result[] = new char[nDigits + 10];
        int i = 0;
        if (isNegative) {
            result[0] = '-';
            i = 1;
        }
        if (isExceptional) {
            System.arraycopy(digits, 0, result, i, nDigits);
            i += nDigits;
        } else {
            if (decExponent > 0 && decExponent < 8) {
                // print digits.digits.
                int charLength = Math.min(nDigits, decExponent);
                System.arraycopy(digits, 0, result, i, charLength);
                i += charLength;
                if (charLength < decExponent) {
                    charLength = decExponent - charLength;
                    System.arraycopy(zero, 0, result, i, charLength);
                    i += charLength;
                    result[i++] = '.';
                    result[i++] = '0';
                } else {
                    result[i++] = '.';
                    if (charLength < nDigits) {
                        int t = nDigits - charLength;
                        System.arraycopy(digits, charLength, result, i, t);
                        i += t;
                    } else {
                        result[i++] = '0';
                    }
                }
            } else if (decExponent <= 0 && decExponent > -3) {
                result[i++] = '0';
                result[i++] = '.';
                if (decExponent != 0) {
                    System.arraycopy(zero, 0, result, i, -decExponent);
                    i -= decExponent;
                }
                System.arraycopy(digits, 0, result, i, nDigits);
                i += nDigits;
            } else {
                result[i++] = digits[0];
                result[i++] = '.';
                if (nDigits > 1) {
                    System.arraycopy(digits, 1, result, i, nDigits - 1);
                    i += nDigits - 1;
                } else {
                    result[i++] = '0';
                }
                result[i++] = 'E';
                int e;
                if (decExponent <= 0) {
                    result[i++] = '-';
                    e = -decExponent + 1;
                } else {
                    e = decExponent - 1;
                }
                // decExponent has 1, 2, or 3, digits
                if (e <= 9) {
                    result[i++] = (char) (e + '0');
                } else if (e <= 99) {
                    result[i++] = (char) (e / 10 + '0');
                    result[i++] = (char) (e % 10 + '0');
                } else {
                    result[i++] = (char) (e / 100 + '0');
                    e %= 100;
                    result[i++] = (char) (e / 10 + '0');
                    result[i++] = (char) (e % 10 + '0');
                }
            }
        }
        return new String(result, 0, i);
    }

    public static FloatingDecimal readJavaFormatString(String in) throws NumberFormatException {
        boolean isNegative = false;
        boolean signSeen = false;
        int decExp;
        char c;

parseNumber:
        try {
            in = in.trim(); // don't mess around with white space.
            // throws NullPointerException if null
            int l = in.length();
            if (l == 0)
                throw new NumberFormatException("empty String");
            int i = 0;
            switch (c = in.charAt(i)) {
                case '-':
                    isNegative = true;
                    //FALLTHROUGH
                case '+':
                    i++;
                    signSeen = true;
            }
            // Would handle NaN and Infinity here, but it isn't
            // part of the spec!
            //
            char[] digits = new char[l];
            int nDigits = 0;
            boolean decSeen = false;
            int decPt = 0;
            int nLeadZero = 0;
            int nTrailZero = 0;
digitLoop:
            while (i < l) {
                switch (c = in.charAt(i)) {
                    case '0':
                        if (nDigits > 0) {
                            nTrailZero += 1;
                        } else {
                            nLeadZero += 1;
                        }
                        break; // out of switch.
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                    case '9':
                        while (nTrailZero > 0) {
                            digits[nDigits++] = '0';
                            nTrailZero -= 1;
                        }
                        digits[nDigits++] = c;
                        break; // out of switch.
                    case '.':
                        if (decSeen) {
                            // already saw one ., this is the 2nd.
                            throw new NumberFormatException("multiple points");
                        }
                        decPt = i;
                        if (signSeen) {
                            decPt -= 1;
                        }
                        decSeen = true;
                        break; // out of switch.
                    default:
                        break digitLoop;
                }
                i++;
            }
            /*
             * At this point, we've scanned all the digits and decimal
             * point we're going to see. Trim off leading and trailing
             * zeros, which will just confuse us later, and adjust
             * our initial decimal exponent accordingly.
             * To review:
             * we have seen i total characters.
             * nLeadZero of them were zeros before any other digits.
             * nTrailZero of them were zeros after any other digits.
             * if ( decSeen ), then a . was seen after decPt characters
             * ( including leading zeros which have been discarded )
             * nDigits characters were neither lead nor trailing
             * zeros, nor point
             */
            /*
             * special hack: if we saw no non-zero digits, then the
             * answer is zero!
             * Unfortunately, we feel honor-bound to keep parsing!
             */
            if (nDigits == 0) {
                digits = zero;
                nDigits = 1;
                if (nLeadZero == 0) {
                    // we saw NO DIGITS AT ALL,
                    // not even a crummy 0!
                    // this is not allowed.
                    break parseNumber; // go throw exception
                }

            }

            /* Our initial exponent is decPt, adjusted by the number of
             * discarded zeros. Or, if there was no decPt,
             * then its just nDigits adjusted by discarded trailing zeros.
             */
            if (decSeen) {
                decExp = decPt - nLeadZero;
            } else {
                decExp = nDigits + nTrailZero;
            }

            /*
             * Look for 'e' or 'E' and an optionally signed integer.
             */
            if ( (i < l) && ( (c = in.charAt(i)) == 'e') || (c == 'E')) {
                int expSign = 1;
                int expVal = 0;
                int reallyBig = Integer.MAX_VALUE / 10;
                boolean expOverflow = false;
                switch (in.charAt(++i)) {
                    case '-':
                        expSign = -1;
                        //FALLTHROUGH
                    case '+':
                        i++;
                }
                int expAt = i;
                expLoop:while (i < l) {
                    if (expVal >= reallyBig) {
                        // the next character will cause integer
                        // overflow.
                        expOverflow = true;
                    }
                    switch (c = in.charAt(i++)) {
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            expVal = expVal * 10 + ( (int) c - (int) '0');
                            continue;
                        default:
                            i--; // back up.
                            break expLoop; // stop parsing exponent.
                    }
                }
                int expLimit = bigDecimalExponent + nDigits + nTrailZero;
                if (expOverflow || (expVal > expLimit)) {
                    //
                    // The intent here is to end up with
                    // infinity or zero, as appropriate.
                    // The reason for yielding such a small decExponent,
                    // rather than something intuitive such as
                    // expSign*Integer.MAX_VALUE, is that this value
                    // is subject to further manipulation in
                    // doubleValue() and floatValue(), and I don't want
                    // it to be able to cause overflow there!
                    // (The only way we can get into trouble here is for
                    // really outrageous nDigits+nTrailZero, such as 2 billion. )
                    //
                    decExp = expSign * expLimit;
                } else {
                    // this should not overflow, since we tested
                    // for expVal > (MAX+N), where N >= abs(decExp)
                    decExp = decExp + expSign * expVal;
                }

                // if we saw something not a digit ( or end of string )
                // after the [Ee][+-], without seeing any digits at all
                // this is certainly an error. If we saw some digits,
                // but then some trailing garbage, that might be ok.
                // so we just fall through in that case.
                // HUMBUG
                if (i == expAt)
                    break parseNumber; // certainly bad
            }
            /*
             * We parsed everything we could.
             * If there are leftovers, then this is not good input!
             */
            if (i < l &&
                ( (i != l - 1) ||
                 (in.charAt(i) != 'f' &&
                  in.charAt(i) != 'F' &&
                  in.charAt(i) != 'd' &&
                  in.charAt(i) != 'D'))) {
                break parseNumber; // go throw exception
            }

            return new FloatingDecimal(isNegative, decExp, digits, nDigits, false);
        } catch (StringIndexOutOfBoundsException e) {}
        throw new NumberFormatException(in);
    }

    /*
     * Take a FloatingDecimal, which we presumably just scanned in,
     * and find out what its value is, as a double.
     *
     * AS A SIDE EFFECT, SET roundDir TO INDICATE PREFERRED
     * ROUNDING DIRECTION in case the result is really destined
     * for a single-precision float.
     */

    public double
        doubleValue() {
        int kDigits = Math.min(nDigits, maxDecimalDigits + 1);
        long lValue;
        double dValue;
        double rValue, tValue;

        roundDir = 0;

        /*
         * convert the lead kDigits to a long integer.
         */
        // (special performance hack: start to do it using int)
        int iValue = (int) digits[0] - (int) '0';
        int iDigits = Math.min(kDigits, intDecimalDigits);
        for (int i = 1; i < iDigits; i++) {
            iValue = iValue * 10 + (int) digits[i] - (int) '0';
        }
        lValue = (long) iValue;
        for (int i = iDigits; i < kDigits; i++) {
            lValue = lValue * 10L + (long) ( (int) digits[i] - (int) '0');
        }
        dValue = (double) lValue;
        int exp = decExponent - kDigits;
        /*
         * lValue now contains a long integer with the value of
         * the first kDigits digits of the number.
         * dValue contains the (double) of the same.
         */

        if (nDigits <= maxDecimalDigits) {
            /*
             * possibly an easy case.
             * We know that the digits can be represented
             * exactly. And if the exponent isn't too outrageous,
             * the whole thing can be done with one operation,
             * thus one rounding error.
             * Note that all our constructors trim all leading and
             * trailing zeros, so simple values (including zero)
             * will always end up here
             */
            if (exp == 0 || dValue == 0.0)
                return (isNegative) ? -dValue : dValue; // small floating integer
            else if (exp >= 0) {
                if (exp <= maxSmallTen) {
                    /*
                     * Can get the answer with one operation,
                     * thus one roundoff.
                     */
                    rValue = dValue * small10pow[exp];
                    if (mustSetRoundDir) {
                        tValue = rValue / small10pow[exp];
                        roundDir = (tValue == dValue) ? 0
                                   : (tValue < dValue) ? 1
                                   : -1;
                    }
                    return (isNegative) ? -rValue : rValue;
                }
                int slop = maxDecimalDigits - kDigits;
                if (exp <= maxSmallTen + slop) {
                    /*
                     * We can multiply dValue by 10^(slop)
                     * and it is still "small" and exact.
                     * Then we can multiply by 10^(exp-slop)
                     * with one rounding.
                     */
                    dValue *= small10pow[slop];
                    rValue = dValue * small10pow[exp - slop];

                    if (mustSetRoundDir) {
                        tValue = rValue / small10pow[exp - slop];
                        roundDir = (tValue == dValue) ? 0
                                   : (tValue < dValue) ? 1
                                   : -1;
                    }
                    return (isNegative) ? -rValue : rValue;
                }
                /*
                 * Else we have a hard case with a positive exp.
                 */
            } else {
                if (exp >= -maxSmallTen) {
                    /*
                     * Can get the answer in one division.
                     */
                    rValue = dValue / small10pow[ -exp];
                    tValue = rValue * small10pow[ -exp];
                    if (mustSetRoundDir) {
                        roundDir = (tValue == dValue) ? 0
                                   : (tValue < dValue) ? 1
                                   : -1;
                    }
                    return (isNegative) ? -rValue : rValue;
                }
                /*
                 * Else we have a hard case with a negative exp.
                 */
            }
        }

        /*
         * Harder cases:
         * The sum of digits plus exponent is greater than
         * what we think we can do with one error.
         *
         * Start by approximating the right answer by,
         * naively, scaling by powers of 10.
         */
        if (exp > 0) {
            if (decExponent > maxDecimalExponent + 1) {
                /*
                 * Lets face it. This is going to be
                 * Infinity. Cut to the chase.
                 */
                return (isNegative) ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
            }
            if ( (exp & 15) != 0) {
                dValue *= small10pow[exp & 15];
            }
            if ( (exp >>= 4) != 0) {
                int j;
                for (j = 0; exp > 1; j++, exp >>= 1) {
                    if ( (exp & 1) != 0)
                        dValue *= big10pow[j];
                }
                /*
                 * The reason for the weird exp > 1 condition
                 * in the above loop was so that the last multiply
                 * would get unrolled. We handle it here.
                 * It could overflow.
                 */
                double t = dValue * big10pow[j];
                if (Double.isInfinite(t)) {
                    /*
                     * It did overflow.
                     * Look more closely at the result.
                     * If the exponent is just one too large,
                     * then use the maximum finite as our estimate
                     * value. Else call the result infinity
                     * and punt it.
                     * ( I presume this could happen because
                     * rounding forces the result here to be
                     * an ULP or two larger than
                     * Double.MAX_VALUE ).
                     */
                    t = dValue / 2.0;
                    t *= big10pow[j];
                    if (Double.isInfinite(t)) {
                        return (isNegative) ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
                    }
                    t = Double.MAX_VALUE;
                }
                dValue = t;
            }
        } else if (exp < 0) {
            exp = -exp;
            if (decExponent < minDecimalExponent - 1) {
                /*
                 * Lets face it. This is going to be
                 * zero. Cut to the chase.
                 */
                return (isNegative) ? -0.0 : 0.0;
            }
            if ( (exp & 15) != 0) {
                dValue /= small10pow[exp & 15];
            }
            if ( (exp >>= 4) != 0) {
                int j;
                for (j = 0; exp > 1; j++, exp >>= 1) {
                    if ( (exp & 1) != 0)
                        dValue *= tiny10pow[j];
                }
                /*
                 * The reason for the weird exp > 1 condition
                 * in the above loop was so that the last multiply
                 * would get unrolled. We handle it here.
                 * It could underflow.
                 */
                double t = dValue * tiny10pow[j];
                if (t == 0.0) {
                    /*
                     * It did underflow.
                     * Look more closely at the result.
                     * If the exponent is just one too small,
                     * then use the minimum finite as our estimate
                     * value. Else call the result 0.0
                     * and punt it.
                     * ( I presume this could happen because
                     * rounding forces the result here to be
                     * an ULP or two less than
                     * Double.MIN_VALUE ).
                     */
                    t = dValue * 2.0;
                    t *= tiny10pow[j];
                    if (t == 0.0) {
                        return (isNegative) ? -0.0 : 0.0;
                    }
                    t = Double.MIN_VALUE;
                }
                dValue = t;
            }
        }

        /*
         * dValue is now approximately the result.
         * The hard part is adjusting it, by comparison
         * with FDBigInt arithmetic.
         * Formulate the EXACT big-number result as
         * bigD0 * 10^exp
         */
        FDBigInt bigD0 = new FDBigInt(lValue, digits, kDigits, nDigits);
        exp = decExponent - nDigits;

        correctionLoop:while (true) {
//com.sun.squawk.VM.print("dValue = ");
//com.sun.squawk.VM.println(dValue);
            /* AS A SIDE EFFECT, THIS METHOD WILL SET THE INSTANCE VARIABLES
             * bigIntExp and bigIntNBits
             */
            FDBigInt bigB = doubleToBigInt(dValue);

            /*
             * Scale bigD, bigB appropriately for
             * big-integer operations.
             * Naively, we multipy by powers of ten
             * and powers of two. What we actually do
             * is keep track of the powers of 5 and
             * powers of 2 we would use, then factor out
             * common divisors before doing the work.
             */
            int B2, B5; // powers of 2, 5 in bigB
            int D2, D5; // powers of 2, 5 in bigD
            int Ulp2; // powers of 2 in halfUlp.
            if (exp >= 0) {
                B2 = B5 = 0;
                D2 = D5 = exp;
            } else {
                B2 = B5 = -exp;
                D2 = D5 = 0;
            }
            if (bigIntExp >= 0) {
                B2 += bigIntExp;
            } else {
                D2 -= bigIntExp;
            }
            Ulp2 = B2;
            // shift bigB and bigD left by a number s. t.
            // halfUlp is still an integer.
            int hulpbias;
            if (bigIntExp + bigIntNBits <= -expBias + 1) {
                // This is going to be a denormalized number
                // (if not actually zero).
                // half an ULP is at 2^-(expBias+expShift+1)
                hulpbias = bigIntExp + expBias + expShift;
            } else {
                hulpbias = expShift + 2 - bigIntNBits;
            }
            B2 += hulpbias;
            D2 += hulpbias;
            // if there are common factors of 2, we might just as well
            // factor them out, as they add nothing useful.
            int common2 = Math.min(B2, Math.min(D2, Ulp2));
            B2 -= common2;
            D2 -= common2;
            Ulp2 -= common2;
            // do multiplications by powers of 5 and 2
            bigB = multPow52(bigB, B5, B2);
            FDBigInt bigD = multPow52(new FDBigInt(bigD0), D5, D2);
            //
            // to recap:
            // bigB is the scaled-big-int version of our floating-point
            // candidate.
            // bigD is the scaled-big-int version of the exact value
            // as we understand it.
            // halfUlp is 1/2 an ulp of bigB, except for special cases
            // of exact powers of 2
            //
            // the plan is to compare bigB with bigD, and if the difference
            // is less than halfUlp, then we're satisfied. Otherwise,
            // use the ratio of difference to halfUlp to calculate a fudge
            // factor to add to the floating value, then go 'round again.
            //
            FDBigInt diff;
            int cmpResult;
            boolean overvalue;
            if ( (cmpResult = bigB.cmp(bigD)) > 0) {
                overvalue = true; // our candidate is too big.
                diff = bigB.sub(bigD);
                if ( (bigIntNBits == 1) && (bigIntExp > -expBias)) {
                    // candidate is a normalized exact power of 2 and
                    // is too big. We will be subtracting.
                    // For our purposes, ulp is the ulp of the
                    // next smaller range.
                    Ulp2 -= 1;
                    if (Ulp2 < 0) {
                        // rats. Cannot de-scale ulp this far.
                        // must scale diff in other direction.
                        Ulp2 = 0;
                        diff.lshiftMe(1);
                    }
                }
            } else if (cmpResult < 0) {
                overvalue = false; // our candidate is too small.
                diff = bigD.sub(bigB);
            } else {
                // the candidate is exactly right!
                // this happens with surprising fequency
                break correctionLoop;
            }
            FDBigInt halfUlp = constructPow52(B5, Ulp2);
            if ( (cmpResult = diff.cmp(halfUlp)) < 0) {
                // difference is small.
                // this is close enough
                roundDir = overvalue ? -1 : 1;
                break correctionLoop;
            } else if (cmpResult == 0) {
                // difference is exactly half an ULP
                // round to some other value maybe, then finish
                dValue += 0.5 * ulp(dValue, overvalue);
                // should check for bigIntNBits == 1 here??
                roundDir = overvalue ? -1 : 1;
                break correctionLoop;
            } else {
                // difference is non-trivial.
                // could scale addend by ratio of difference to
                // halfUlp here, if we bothered to compute that difference.
                // Most of the time ( I hope ) it is about 1 anyway.
                dValue += ulp(dValue, overvalue);
                if (dValue == 0.0 || dValue == Double.POSITIVE_INFINITY)
                    break correctionLoop; // oops. Fell off end of range.
                continue; // try again.
            }

        }
        return (isNegative) ? -dValue : dValue;
    }

    /*
     * Take a FloatingDecimal, which we presumably just scanned in,
     * and find out what its value is, as a float.
     * This is distinct from doubleValue() to avoid the extremely
     * unlikely case of a double rounding error, wherein the converstion
     * to double has one rounding error, and the conversion of that double
     * to a float has another rounding error, IN THE WRONG DIRECTION,
     * ( because of the preference to a zero low-order bit ).
     */

    public float
        floatValue() {
        int kDigits = Math.min(nDigits, singleMaxDecimalDigits + 1);
        int iValue;
        float fValue;

        /*
         * convert the lead kDigits to an integer.
         */
        iValue = (int) digits[0] - (int) '0';
        for (int i = 1; i < kDigits; i++) {
            iValue = iValue * 10 + (int) digits[i] - (int) '0';
        }
        fValue = (float) iValue;
        int exp = decExponent - kDigits;
        /*
         * iValue now contains an integer with the value of
         * the first kDigits digits of the number.
         * fValue contains the (float) of the same.
         */

        if (nDigits <= singleMaxDecimalDigits) {
            /*
             * possibly an easy case.
             * We know that the digits can be represented
             * exactly. And if the exponent isn't too outrageous,
             * the whole thing can be done with one operation,
             * thus one rounding error.
             * Note that all our constructors trim all leading and
             * trailing zeros, so simple values (including zero)
             * will always end up here.
             */
            if (exp == 0 || fValue == 0.0f)
                return (isNegative) ? -fValue : fValue; // small floating integer
            else if (exp >= 0) {
                if (exp <= singleMaxSmallTen) {
                    /*
                     * Can get the answer with one operation,
                     * thus one roundoff.
                     */
                    fValue *= singleSmall10pow[exp];
                    return (isNegative) ? -fValue : fValue;
                }
                int slop = singleMaxDecimalDigits - kDigits;
                if (exp <= singleMaxSmallTen + slop) {
                    /*
                     * We can multiply dValue by 10^(slop)
                     * and it is still "small" and exact.
                     * Then we can multiply by 10^(exp-slop)
                     * with one rounding.
                     */
                    fValue *= singleSmall10pow[slop];
                    fValue *= singleSmall10pow[exp - slop];
                    return (isNegative) ? -fValue : fValue;
                }
                /*
                 * Else we have a hard case with a positive exp.
                 */
            } else {
                if (exp >= -singleMaxSmallTen) {
                    /*
                     * Can get the answer in one division.
                     */
                    fValue /= singleSmall10pow[ -exp];
                    return (isNegative) ? -fValue : fValue;
                }
                /*
                 * Else we have a hard case with a negative exp.
                 */
            }
        } else if ( (decExponent >= nDigits) && (nDigits + decExponent <= maxDecimalDigits)) {
            /*
             * In double-precision, this is an exact floating integer.
             * So we can compute to double, then shorten to float
             * with one round, and get the right answer.
             *
             * First, finish accumulating digits.
             * Then convert that integer to a double, multiply
             * by the appropriate power of ten, and convert to float.
             */
            long lValue = (long) iValue;
            for (int i = kDigits; i < nDigits; i++) {
                lValue = lValue * 10L + (long) ( (int) digits[i] - (int) '0');
            }
            double dValue = (double) lValue;
            exp = decExponent - nDigits;
            dValue *= small10pow[exp];
            fValue = (float) dValue;
            return (isNegative) ? -fValue : fValue;

        }
        /*
         * Harder cases:
         * The sum of digits plus exponent is greater than
         * what we think we can do with one error.
         *
         * Start by weeding out obviously out-of-range
         * results, then convert to double and go to
         * common hard-case code.
         */
        if (decExponent > singleMaxDecimalExponent + 1) {
            /*
             * Lets face it. This is going to be
             * Infinity. Cut to the chase.
             */
            return (isNegative) ? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY;
        } else if (decExponent < singleMinDecimalExponent - 1) {
            /*
             * Lets face it. This is going to be
             * zero. Cut to the chase.
             */
            return (isNegative) ? -0.0f : 0.0f;
        }

        /*
         * Here, we do 'way too much work, but throwing away
         * our partial results, and going and doing the whole
         * thing as double, then throwing away half the bits that computes
         * when we convert back to float.
         *
         * The alternative is to reproduce the whole multiple-precision
         * algorythm for float precision, or to try to parameterize it
         * for common usage. The former will take about 400 lines of code,
         * and the latter I tried without success. Thus the semi-hack
         * answer here.
         */
        mustSetRoundDir = true;
        double dValue = doubleValue();
        return stickyRound(dValue);
    }

    /*
     * All the positive powers of 10 that can be
     * represented exactly in double/float.
     */
    private static final double small10pow[] = {
                                               1.0e0,
                                               1.0e1, 1.0e2, 1.0e3, 1.0e4, 1.0e5,
                                               1.0e6, 1.0e7, 1.0e8, 1.0e9, 1.0e10,
                                               1.0e11, 1.0e12, 1.0e13, 1.0e14, 1.0e15,
                                               1.0e16, 1.0e17, 1.0e18, 1.0e19, 1.0e20,
                                               1.0e21, 1.0e22
    };

    private static final float singleSmall10pow[] = {
        1.0e0f,
        1.0e1f, 1.0e2f, 1.0e3f, 1.0e4f, 1.0e5f,
        1.0e6f, 1.0e7f, 1.0e8f, 1.0e9f, 1.0e10f
    };

    private static final double big10pow[] = {
                                             1e16, 1e32, 1e64, 1e128, 1e256};
    private static final double tiny10pow[] = {
                                              1e-16, 1e-32, 1e-64, 1e-128, 1e-256};

    private static final int maxSmallTen = small10pow.length - 1;
    private static final int singleMaxSmallTen = singleSmall10pow.length - 1;

    private static final int small5pow[] = {
                                           1,
                                           5,
                                           5 * 5,
                                           5 * 5 * 5,
                                           5 * 5 * 5 * 5,
                                           5 * 5 * 5 * 5 * 5,
                                           5 * 5 * 5 * 5 * 5 * 5,
                                           5 * 5 * 5 * 5 * 5 * 5 * 5,
                                           5 * 5 * 5 * 5 * 5 * 5 * 5 * 5,
                                           5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5,
                                           5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5,
                                           5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5,
                                           5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5,
                                           5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5
    };

    private static final long long5pow[] = {
                                           1L,
                                           5L,
                                           5L * 5,
                                           5L * 5 * 5,
                                           5L * 5 * 5 * 5,
                                           5L * 5 * 5 * 5 * 5,
                                           5L * 5 * 5 * 5 * 5 * 5,
                                           5L * 5 * 5 * 5 * 5 * 5 * 5,
                                           5L * 5 * 5 * 5 * 5 * 5 * 5 * 5,
                                           5L * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5,
                                           5L * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5,
                                           5L * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5,
                                           5L * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5,
                                           5L * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5,
                                           5L * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5,
                                           5L * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5,
                                           5L * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5,
                                           5L * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5,
                                           5L * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5,
                                           5L * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5,
                                           5L * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5,
                                           5L * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5,
                                           5L * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5,
                                           5L * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5,
                                           5L * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5,
                                           5L * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5,
                                           5L * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5,
    };

    // approximately ceil( log2( long5pow[i] ) )
    private static final int n5bits[] = {
                                        0,
                                        3,
                                        5,
                                        7,
                                        10,
                                        12,
                                        14,
                                        17,
                                        19,
                                        21,
                                        24,
                                        26,
                                        28,
                                        31,
                                        33,
                                        35,
                                        38,
                                        40,
                                        42,
                                        45,
                                        47,
                                        49,
                                        52,
                                        54,
                                        56,
                                        59,
                                        61,
    };

    private static final char infinity[] = {'I', 'n', 'f', 'i', 'n', 'i', 't', 'y'};
    private static final char notANumber[] = {'N', 'a', 'N'};
    private static final char zero[] = {'0', '0', '0', '0', '0', '0', '0', '0'};

}

/*
 * A really, really simple bigint package
 * tailored to the needs of floating base conversion.
 */
class FDBigInt {
    int nWords; // number of words used
    int data[]; // value: data[0] is least significant

    public FDBigInt(int v) {
        nWords = 1;
        data = new int[1];
        data[0] = v;
    }

    public FDBigInt(long v) {
        data = new int[2];
        data[0] = (int) v;
        data[1] = (int) (v >>> 32);
        nWords = (data[1] == 0) ? 1 : 2;
    }

    public FDBigInt(FDBigInt other) {
        data = new int[nWords = other.nWords];
        System.arraycopy(other.data, 0, data, 0, nWords);
    }

    private FDBigInt(int[] d, int n) {
        data = d;
        nWords = n;
    }

    public FDBigInt(long seed, char digit[], int nd0, int nd) {
        int n = (nd + 8) / 9; // estimate size needed.
        if (n < 2)
            n = 2;
        data = new int[n]; // allocate enough space
        data[0] = (int) seed; // starting value
        data[1] = (int) (seed >>> 32);
        nWords = (data[1] == 0) ? 1 : 2;
        int i = nd0;
        int limit = nd - 5; // slurp digits 5 at a time.
        int v;
        while (i < limit) {
            int ilim = i + 5;
            v = (int) digit[i++] - (int) '0';
            while (i < ilim) {
                v = 10 * v + (int) digit[i++] - (int) '0';
            }
            multaddMe(100000, v); // ... where 100000 is 10^5.
        }
        int factor = 1;
        v = 0;
        while (i < nd) {
            v = 10 * v + (int) digit[i++] - (int) '0';
            factor *= 10;
        }
        if (factor != 1) {
            multaddMe(factor, v);
        }
    }

    /*
     * Left shift by c bits.
     * Shifts this in place.
     */
    public void
        lshiftMe(int c) throws IllegalArgumentException {
        if (c <= 0) {
            if (c == 0)
                return; // silly.
            else
                throw new IllegalArgumentException("negative shift count");
        }
        int wordcount = c >> 5;
        int bitcount = c & 0x1f;
        int anticount = 32 - bitcount;
        int t[] = data;
        int s[] = data;
        if (nWords + wordcount + 1 > t.length) {
            // reallocate.
            t = new int[nWords + wordcount + 1];
        }
        int target = nWords + wordcount;
        int src = nWords - 1;
        if (bitcount == 0) {
            // special hack, since an anticount of 32 won't go!
            System.arraycopy(s, 0, t, wordcount, nWords);
            target = wordcount - 1;
        } else {
            t[target--] = s[src] >>> anticount;
            while (src >= 1) {
                t[target--] = (s[src] << bitcount) | (s[--src] >>> anticount);
            }
            t[target--] = s[src] << bitcount;
        } while (target >= 0) {
            t[target--] = 0;
        }
        data = t;
        nWords += wordcount + 1;
        // may have constructed high-order word of 0.
        // if so, trim it
        while (nWords > 1 && data[nWords - 1] == 0)
            nWords--;
    }

    /*
     * normalize this number by shifting until
     * the MSB of the number is at 0x08000000.
     * This is in preparation for quoRemIteration, below.
     * The idea is that, to make division easier, we want the
     * divisor to be "normalized" -- usually this means shifting
     * the MSB into the high words sign bit. But because we know that
     * the quotient will be 0 < q < 10, we would like to arrange that
     * the dividend not span up into another word of precision.
     * (This needs to be explained more clearly!)
     */
    public int
        normalizeMe() throws IllegalArgumentException {
        int src;
        int wordcount = 0;
        int bitcount = 0;
        int v = 0;
        for (src = nWords - 1; src >= 0 && (v = data[src]) == 0; src--) {
            wordcount += 1;
        }
        if (src < 0) {
            // oops. Value is zero. Cannot normalize it!
            throw new IllegalArgumentException("zero value");
        }
        /*
         * In most cases, we assume that wordcount is zero. This only
         * makes sense, as we try not to maintain any high-order
         * words full of zeros. In fact, if there are zeros, we will
         * simply SHORTEN our number at this point. Watch closely...
         */
        nWords -= wordcount;
        /*
         * Compute how far left we have to shift v s.t. its highest-
         * order bit is in the right place. Then call lshiftMe to
         * do the work.
         */
        if ( (v & 0xf0000000) != 0) {
            // will have to shift up into the next word.
            // too bad.
            for (bitcount = 32; (v & 0xf0000000) != 0; bitcount--)
                v >>>= 1;
        } else {
            while (v <= 0x000fffff) {
                // hack: byte-at-a-time shifting
                v <<= 8;
                bitcount += 8;
            } while (v <= 0x07ffffff) {
                v <<= 1;
                bitcount += 1;
            }
        }
        if (bitcount != 0)
            lshiftMe(bitcount);
        return bitcount;
    }

    /*
     * Multiply a FDBigInt by an int.
     * Result is a new FDBigInt.
     */
    public FDBigInt
        mult(int iv) {
        long v = iv;
        int r[];
        long p;

        // guess adequate size of r.
        r = new int[ (v * ( (long) data[nWords - 1] & 0xffffffffL) > 0xfffffffL) ? nWords + 1 : nWords];
        p = 0L;
        for (int i = 0; i < nWords; i++) {
            p += v * ( (long) data[i] & 0xffffffffL);
            r[i] = (int) p;
            p >>>= 32;
        }
        if (p == 0L) {
            return new FDBigInt(r, nWords);
        } else {
            r[nWords] = (int) p;
            return new FDBigInt(r, nWords + 1);
        }
    }

    /*
     * Multiply a FDBigInt by an int and add another int.
     * Result is computed in place.
     * Hope it fits!
     */
    public void
        multaddMe(int iv, int addend) {
        long v = iv;
        long p;

        // unroll 0th iteration, doing addition.
        p = v * ( (long) data[0] & 0xffffffffL) + ( (long) addend & 0xffffffffL);
        data[0] = (int) p;
        p >>>= 32;
        for (int i = 1; i < nWords; i++) {
            p += v * ( (long) data[i] & 0xffffffffL);
            data[i] = (int) p;
            p >>>= 32;
        }
        if (p != 0L) {
            data[nWords] = (int) p; // will fail noisily if illegal!
            nWords++;
        }
    }

    /*
     * Multiply a FDBigInt by another FDBigInt.
     * Result is a new FDBigInt.
     */
    public FDBigInt
        mult(FDBigInt other) {
        // crudely guess adequate size for r
        int r[] = new int[nWords + other.nWords];
        int i;
        // I think I am promised zeros...

        for (i = 0; i < this.nWords; i++) {
            long v = (long)this.data[i] & 0xffffffffL; // UNSIGNED CONVERSION
            long p = 0L;
            int j;
            for (j = 0; j < other.nWords; j++) {
                p += ( (long) r[i + j] & 0xffffffffL) + v * ( (long) other.data[j] & 0xffffffffL); // UNSIGNED CONVERSIONS ALL 'ROUND.
                r[i + j] = (int) p;
                p >>>= 32;
            }
            r[i + j] = (int) p;
        }
        // compute how much of r we actually needed for all that.
        for (i = r.length - 1; i > 0; i--)
            if (r[i] != 0)
                break;
        return new FDBigInt(r, i + 1);
    }

    /*
     * Add one FDBigInt to another. Return a FDBigInt
     */
    public FDBigInt
        add(FDBigInt other) {
        int i;
        int a[], b[];
        int n, m;
        long c = 0L;
        // arrange such that a.nWords >= b.nWords;
        // n = a.nWords, m = b.nWords
        if (this.nWords >= other.nWords) {
            a = this.data;
            n = this.nWords;
            b = other.data;
            m = other.nWords;
        } else {
            a = other.data;
            n = other.nWords;
            b = this.data;
            m = this.nWords;
        }
        int r[] = new int[n];
        for (i = 0; i < n; i++) {
            c += (long) a[i] & 0xffffffffL;
            if (i < m) {
                c += (long) b[i] & 0xffffffffL;
            }
            r[i] = (int) c;
            c >>= 32; // signed shift.
        }
        if (c != 0L) {
            // oops -- carry out -- need longer result.
            int s[] = new int[r.length + 1];
            System.arraycopy(r, 0, s, 0, r.length);
            s[i++] = (int) c;
            return new FDBigInt(s, i);
        }
        return new FDBigInt(r, i);
    }

    /*
     * Subtract one FDBigInt from another. Return a FDBigInt
     * Assert that the result is positive.
     */
    public FDBigInt
        sub(FDBigInt other) {
        int r[] = new int[this.nWords];
        int i;
        int n = this.nWords;
        int m = other.nWords;
        int nzeros = 0;
        long c = 0L;
        for (i = 0; i < n; i++) {
            c += (long)this.data[i] & 0xffffffffL;
            if (i < m) {
                c -= (long) other.data[i] & 0xffffffffL;
            }
            if ( (r[i] = (int) c) == 0)
                nzeros++;
            else
                nzeros = 0;
            c >>= 32; // signed shift.
        }
        if (c != 0L)
            throw new RuntimeException("Assertion botch: borrow out of subtract");
        while (i < m)
            if (other.data[i++] != 0)
                throw new RuntimeException("Assertion botch: negative result of subtract");
        return new FDBigInt(r, n - nzeros);
    }

    /*
     * Compare FDBigInt with another FDBigInt. Return an integer
     * >0: this > other
     *  0: this == other
     * <0: this < other
     */
    public int
        cmp(FDBigInt other) {
        int i;
        if (this.nWords > other.nWords) {
            // if any of my high-order words is non-zero,
            // then the answer is evident
            int j = other.nWords - 1;
            for (i = this.nWords - 1; i > j; i--)
                if (this.data[i] != 0)
                    return 1;
        } else if (this.nWords < other.nWords) {
            // if any of other's high-order words is non-zero,
            // then the answer is evident
            int j = this.nWords - 1;
            for (i = other.nWords - 1; i > j; i--)
                if (other.data[i] != 0)
                    return -1;
        } else {
            i = this.nWords - 1;
        }
        for (; i > 0; i--)
            if (this.data[i] != other.data[i])
                break;
        // careful! want unsigned compare!
        // use brute force here.
        int a = this.data[i];
        int b = other.data[i];
        if (a < 0) {
            // a is really big, unsigned
            if (b < 0) {
                return a - b; // both big, negative
            } else {
                return 1; // b not big, answer is obvious;
            }
        } else {
            // a is not really big
            if (b < 0) {
                // but b is really big
                return -1;
            } else {
                return a - b;
            }
        }
    }

    /*
     * Compute
     * q = (int)( this / S )
     * this = 10 * ( this mod S )
     * Return q.
     * This is the iteration step of digit development for output.
     * We assume that S has been normalized, as above, and that
     * "this" has been lshift'ed accordingly.
     * Also assume, of course, that the result, q, can be expressed
     * as an integer, 0 <= q < 10.
     */
    public int
        quoRemIteration(FDBigInt S) throws IllegalArgumentException {
        // ensure that this and S have the same number of
        // digits. If S is properly normalized and q < 10 then
        // this must be so.
        if (nWords != S.nWords) {
            throw new IllegalArgumentException("disparate values");
        }
        // estimate q the obvious way. We will usually be
        // right. If not, then we're only off by a little and
        // will re-add.
        int n = nWords - 1;
        long q = ( (long) data[n] & 0xffffffffL) / (long) S.data[n];
        long diff = 0L;
        for (int i = 0; i <= n; i++) {
            diff += ( (long) data[i] & 0xffffffffL) - q * ( (long) S.data[i] & 0xffffffffL);
            data[i] = (int) diff;
            diff >>= 32; // N.B. SIGNED shift.
        }
        if (diff != 0L) {
            // q is too big.
            // add S back in until this turns +. This should
            // not be very many times!
            long sum = 0L;
            while (sum == 0L) {
                sum = 0L;
                for (int i = 0; i <= n; i++) {
                    sum += ( (long) data[i] & 0xffffffffL) + ( (long) S.data[i] & 0xffffffffL);
                    data[i] = (int) sum;
                    sum >>= 32; // Signed or unsigned, answer is 0 or 1
                }
                /*
                 * Originally the following line read
                 * "if ( sum !=0 && sum != -1 )"
                 * but that would be wrong, because of the
                 * treatment of the two values as entirely unsigned,
                 * it would be impossible for a carry-out to be interpreted
                 * as -1 -- it would have to be a single-bit carry-out, or
                 * +1.
                 */
                if (sum != 0 && sum != 1)
                    throw new RuntimeException("Assertion botch: " + sum + " carry out of division correction");
                q -= 1;
            }
        }
        // finally, we can multiply this by 10.
        // it cannot overflow, right, as the high-order word has
        // at least 4 high-order zeros!
        long p = 0L;
        for (int i = 0; i <= n; i++) {
            p += 10 * ( (long) data[i] & 0xffffffffL);
            data[i] = (int) p;
            p >>= 32; // SIGNED shift.
        }
        if (p != 0L)
            throw new RuntimeException("Assertion botch: carry out of *10");

        return (int) q;
    }

    public long
        longValue() {
        // if this can be represented as a long,
        // return the value
        int i;
        for (i = this.nWords - 1; i > 1; i--) {
            if (data[i] != 0) {
                throw new RuntimeException("Assertion botch: value too big");
            }
        }
        switch (i) {
            case 1:
                if (data[1] < 0)
                    throw new RuntimeException("Assertion botch: value too big");
                return ( (long) (data[1]) << 32) | ( (long) data[0] & 0xffffffffL);
            case 0:
                return ( (long) data[0] & 0xffffffffL);
            default:
                throw new RuntimeException("Assertion botch: longValue confused");
        }
    }

    public String
        toString() {
        StringBuffer r = new StringBuffer(30);
        r.append('[');
        int i = Math.min(nWords - 1, data.length - 1);
        if (nWords > data.length) {
            r.append("(" + data.length + "<" + nWords + "!)");
        }
        for (; i > 0; i--) {
            r.append(Integer.toHexString(data[i]));
            r.append( (char) ' ');
        }
        r.append(Integer.toHexString(data[0]));
        r.append( (char) ']');
        return new String(r);
    }
}
