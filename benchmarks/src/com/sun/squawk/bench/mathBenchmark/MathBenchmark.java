/* All code Copyright 2004 Christopher W. Cowell-Shah */
/* Modified 22nd March, 2005 to allow benchmarking of Squawk on x86 and Arm7 */

package com.sun.squawk.bench;

import java.util.Date;

public class MathBenchmark
{
	static long startTime;
	static long stopTime;
	static long elapsedTime;

	public static void main(String[] args)
	{
		int intMax =        5000000;
	    long longMin = 10000000000L;
	    long longMax = 10005000000L;
		
		System.out.println("Starting Arithmetic Java benchmark");
		int iterations = 1;
		long intArithmeticTime = 0;
		long longCountTime = 0;
		long totalTime = 0;
		for (int i = 0; i < iterations; i++){
			intArithmeticTime = intArithmetic(intMax);
			longCountTime = longArithmetic(longMin, longMax);
			totalTime = intArithmeticTime + longCountTime;
		}
		System.out.println("finished");
		System.out.println("int arithmetic benchmark time: " + intArithmeticTime + " ms");
		System.out.println("long arithmetic benchmark time: " + longCountTime + " ms");
		System.out.println("Total Java benchmark time: " + totalTime + " ms");
		System.out.println("Average total time per iteration: " + totalTime/iterations + " ms");
		System.out.println("End Java benchmark");
	}


	/**
	 * Math benchmark using ints.
	 */
	static long intArithmetic(int intMax)
	{
		startTime = (new Date()).getTime();

		int intResult = 1;
		int i = 1;
		while (i < intMax)
		{
			intResult -= i++;
			intResult += i++;
			intResult *= i++;
			intResult /= i++;
		}

		stopTime = (new Date()).getTime();
		elapsedTime = stopTime - startTime;

		return elapsedTime;
	}

	/**
	 * Math benchmark using longs.
	 */
	static long longArithmetic(long longMin, long longMax)
	{
		startTime = (new Date()).getTime();

		long longResult = longMin;
		long i = longMin;
		while (i < longMax)
		{
			longResult -= i++;
			longResult += i++;
			longResult *= i++;
			longResult /= i++;
		}

		stopTime = (new Date()).getTime();
		elapsedTime = stopTime - startTime;
		return elapsedTime;
	}
}
