/*
 * Copyright 1994-2001 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */
package java.lang;
import com.sun.squawk.vm.MathOpcodes;
import com.sun.squawk.*;

/**
 * The class <code>Math</code> contains methods for performing basic
 * numeric operations.
 *
 * @author  unascribed
 * @version 1.48, 12/04/99 (CLDC 1.0, Spring 2000)
 * @since   1.3
 */
public final strictfp class Math {

    /**
     * Don't let anyone instantiate this class.
     */
    private Math() {}

    /**
     * Returns the absolute value of an <code>int</code> value.
     * If the argument is not negative, the argument is returned.
     * If the argument is negative, the negation of the argument is returned.
     * <p>
     * Note that if the argument is equal to the value of
     * <code>Integer.MIN_VALUE</code>, the most negative representable
     * <code>int</code> value, the result is that same value, which is
     * negative.
     *
     * @param   a   an <code>int</code> value.
     * @return  the absolute value of the argument.
     * @see     java.lang.Integer#MIN_VALUE
     */
    public static int abs(int a) {
        return (a < 0) ? -a : a;
    }

    /**
     * Returns the absolute value of a <code>long</code> value.
     * If the argument is not negative, the argument is returned.
     * If the argument is negative, the negation of the argument is returned.
     * <p>
     * Note that if the argument is equal to the value of
     * <code>Long.MIN_VALUE</code>, the most negative representable
     * <code>long</code> value, the result is that same value, which is
     * negative.
     *
     * @param   a   a <code>long</code> value.
     * @return  the absolute value of the argument.
     * @see     java.lang.Long#MIN_VALUE
     */
    public static long abs(long a) {
        return (a < 0) ? -a : a;
    }

/*if[FLOATS]*/
    /**
     * Returns the absolute value of a <code>float</code> value.
     * If the argument is not negative, the argument is returned.
     * If the argument is negative, the negation of the argument is returned.
     * Special cases:
     * <ul><li>If the argument is positive zero or negative zero, the
     * result is positive zero.
     * <li>If the argument is infinite, the result is positive infinity.
     * <li>If the argument is NaN, the result is NaN.</ul>
     * In other words, the result is equal to the value of the expression:
     * <p><pre>Float.intBitsToFloat(0x7fffffff & Float.floatToIntBits(a))</pre>
     *
     * @param   a   a <code>float</code> value.
     * @return  the absolute value of the argument.
     * @since   CLDC 1.1
     */
    public static float abs(float a) {
        return (a <= 0.0F) ? 0.0F - a : a;
    }

    /**
     * Returns the absolute value of a <code>double</code> value.
     * If the argument is not negative, the argument is returned.
     * If the argument is negative, the negation of the argument is returned.
     * Special cases:
     * <ul><li>If the argument is positive zero or negative zero, the result
     * is positive zero.
     * <li>If the argument is infinite, the result is positive infinity.
     * <li>If the argument is NaN, the result is NaN.</ul>
     * In other words, the result is equal to the value of the expression:
     * <p><pre>Double.longBitsToDouble((Double.doubleToLongBits(a)<<1)>>>1)</pre>
     *
     * @param   a   a <code>double</code> value.
     * @return  the absolute value of the argument.
     * @since   CLDC 1.1
     */
    public static double abs(double a) {
        return (a <= 0.0D) ? 0.0D - a : a;
    }
/*end[FLOATS]*/

    /**
     * Returns the greater of two <code>int</code> values. That is, the
     * result is the argument closer to the value of
     * <code>Integer.MAX_VALUE</code>. If the arguments have the same value,
     * the result is that same value.
     *
     * @param   a   an <code>int</code> value.
     * @param   b   an <code>int</code> value.
     * @return  the larger of <code>a</code> and <code>b</code>.
     * @see     java.lang.Long#MAX_VALUE
     */
    public static int max(int a, int b) {
        return (a >= b) ? a : b;
    }

    /**
     * Returns the greater of two <code>long</code> values. That is, the
     * result is the argument closer to the value of
     * <code>Long.MAX_VALUE</code>. If the arguments have the same value,
     * the result is that same value.
     *
     * @param   a   a <code>long</code> value.
     * @param   b   a <code>long</code> value.
     * @return  the larger of <code>a</code> and <code>b</code>.
     * @see     java.lang.Long#MAX_VALUE
     */
    public static long max(long a, long b) {
        return (a >= b) ? a : b;
    }

/*if[FLOATS]*/
    /**
     * Returns the greater of two <code>float</code> values.  That is, the
     * result is the argument closer to positive infinity. If the
     * arguments have the same value, the result is that same value. If
     * either value is <code>NaN</code>, then the result is <code>NaN</code>.
     * Unlike the the numerical comparison operators, this method considers
     * negative zero to be strictly smaller than positive zero. If one
     * argument is positive zero and the other negative zero, the result
     * is positive zero.
     *
     * @param   a   a <code>float</code> value.
     * @param   b   a <code>float</code> value.
     * @return  the larger of <code>a</code> and <code>b</code>.
     */
    public static float max(float a, float b) {
        if (a != a) return a; // a is NaN
        if ((a == 0.0f) && (b == 0.0f)
            && (Float.floatToIntBits(a) == negativeZeroFloatBits)) {
            return b;
        }
        return (a >= b) ? a : b;
    }

    /**
     * Returns the greater of two <code>double</code> values.  That is, the
     * result is the argument closer to positive infinity. If the
     * arguments have the same value, the result is that same value. If
     * either value is <code>NaN</code>, then the result is <code>NaN</code>.
     * Unlike the the numerical comparison operators, this method considers
     * negative zero to be strictly smaller than positive zero. If one
     * argument is positive zero and the other negative zero, the result
     * is positive zero.
     *
     * @param   a   a <code>double</code> value.
     * @param   b   a <code>double</code> value.
     * @return  the larger of <code>a</code> and <code>b</code>.
     */
    public static double max(double a, double b) {
        if (a != a) return a; // a is NaN
        if ((a == 0.0d) && (b == 0.0d)
            && (Double.doubleToLongBits(a) == negativeZeroDoubleBits)) {
            return b;
        }
        return (a >= b) ? a : b;
    }
/*end[FLOATS]*/

    /**
     * Returns the smaller of two <code>int</code> values. That is, the
     * result the argument closer to the value of <code>Integer.MIN_VALUE</code>.
     * If the arguments have the same value, the result is that same value.
     *
     * @param   a   an <code>int</code> value.
     * @param   b   an <code>int</code> value.
     * @return  the smaller of <code>a</code> and <code>b</code>.
     * @see     java.lang.Long#MIN_VALUE
     */
    public static int min(int a, int b) {
        return (a <= b) ? a : b;
    }

    /**
     * Returns the smaller of two <code>long</code> values. That is, the
     * result is the argument closer to the value of
     * <code>Long.MIN_VALUE</code>. If the arguments have the same value,
     * the result is that same value.
     *
     * @param   a   a <code>long</code> value.
     * @param   b   a <code>long</code> value.
     * @return  the smaller of <code>a</code> and <code>b</code>.
     * @see     java.lang.Long#MIN_VALUE
     */
    public static long min(long a, long b) {
        return (a <= b) ? a : b;
    }

/*if[FLOATS]*/
    /**
     * Returns the smaller of two <code>float</code> values.  That is, the
     * result is the value closer to negative infinity. If the arguments
     * have the same value, the result is that same value. If either value
     * is <code>NaN</code>, then the result is <code>NaN</code>.  Unlike the
     * the numerical comparison operators, this method considers negative zero
     * to be strictly smaller than positive zero.  If one argument is
     * positive zero and the other is negative zero, the result is negative
     * zero.
     *
     * @param   a   a <code>float</code> value.
     * @param   b   a <code>float</code> value.
     * @return  the smaller of <code>a</code> and <code>b.</code>
     * @since   CLDC 1.1
     */
    public static float min(float a, float b) {
        if (a != a) return a; // a is NaN
        if ((a == 0.0f) && (b == 0.0f)
            && (Float.floatToIntBits(b) == negativeZeroFloatBits)) {
            return b;
        }
        return (a <= b) ? a : b;
    }

    /**
     * Returns the smaller of two <code>double</code> values.  That is, the
     * result is the value closer to negative infinity. If the arguments have
     * the same value, the result is that same value. If either value
     * is <code>NaN</code>, then the result is <code>NaN</code>.  Unlike the
     * the numerical comparison operators, this method considers negative zero
     * to be strictly smaller than positive zero. If one argument is
     * positive zero and the other is negative zero, the result is negative
     * zero.
     *
     * @param   a   a <code>double</code> value.
     * @param   b   a <code>double</code> value.
     * @return  the smaller of <code>a</code> and <code>b</code>.
     * @since   CLDC 1.1
     */
    public static double min(double a, double b) {
        if (a != a) return a; // a is NaN
        if ((a == 0.0d) && (b == 0.0d)
            && (Double.doubleToLongBits(b) == negativeZeroDoubleBits)) {
            return b;
        }
        return (a <= b) ? a : b;
    }

    public static double sin(double a)                       { return VM.math(MathOpcodes.SIN, a, 0);          }
    public static double cos(double a)                       { return VM.math(MathOpcodes.COS, a, 0);          }
    public static double tan(double a)                       { return VM.math(MathOpcodes.TAN, a, 0);          }
    public static double sqrt(double a)                      { return VM.math(MathOpcodes.SQRT, a, 0);         }
    public static double ceil(double a)                      { return VM.math(MathOpcodes.CEIL, a, 0);         }
    public static double floor(double a)                     { return VM.math(MathOpcodes.FLOOR, a, 0);        }

/*if[JDK1.0]*/
    public static double asin(double a)                      { return VM.math(MathOpcodes.ASIN, a, 0);         }
    public static double acos(double a)                      { return VM.math(MathOpcodes.ACOS, a, 0);         }
    public static double atan(double a)                      { return VM.math(MathOpcodes.ATAN, a, 0);         }
    public static double exp(double a)                       { return VM.math(MathOpcodes.EXP, a, 0);          }
    public static double log(double a)                       { return VM.math(MathOpcodes.LOG, a, 0);          }
    public static double atan2(double a, double b)           { return VM.math(MathOpcodes.ATAN2, a, b);        }
    public static double pow(double a, double b)             { return VM.math(MathOpcodes.POW, a, b);          }
    public static double IEEEremainder(double a, double b)   { return VM.math(MathOpcodes.IEEE_REMAINDER, a, b);}
/*end[JDK1.0]*/

    /**
     * Converts an angle measured in degrees to the equivalent angle
     * measured in radians.
     *
     * @param   angdeg   an angle, in degrees
     * @return  the measurement of the angle <code>angdeg</code>
     *          in radians.
     * @since   CLDC 1.1
     */
    public static double toRadians(double angdeg) {
        return angdeg / 180.0 * PI;
    }

    /**
     * Converts an angle measured in radians to the equivalent angle
     * measured in degrees.
     *
     * @param   angrad   an angle, in radians
     * @return  the measurement of the angle <code>angrad</code>
     *          in degrees.
     * @since   CLDC 1.1
     */
    public static double toDegrees(double angrad) {
        return angrad * 180.0 / PI;
    }

    private static long negativeZeroFloatBits = Float.floatToIntBits(-0.0f);
    private static long negativeZeroDoubleBits = Double.doubleToLongBits(-0.0d);

    /**
     * The <code>double</code> value that is closer than any other to
     * <code>e</code>, the base of the natural logarithms.
     * @since CLDC 1.1
     */
    public static final double E = 2.7182818284590452354;

    /**
     * The <code>double</code> value that is closer than any other to
     * <i>pi</i>, the ratio of the circumference of a circle to its diameter.
     * @since CLDC 1.1
     */
    public static final double PI = 3.14159265358979323846;


/*end[FLOATS]*/

}
