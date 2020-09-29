/* All code Copyright 2004 Christopher W. Cowell-Shah */

#include <time.h>
#include <math.h>
#include <stdio.h>

long intArithmetic(int);
long longArithmetic(long long int, long long int);

int main()
{
	int intMax =            5000000;
	long long int longMin = 10000000000LL;
	long long int longMax = 10005000000LL;
/*
	printf("Start C benchmark\n");

	long intArithmeticTime = (long)intArithmetic(intMax);
	long longArithmeticTime = (long)longArithmetic(longMin, longMax);
	long totalTime = intArithmeticTime + longArithmeticTime;
	printf("Total elapsed time: %d ms\n", totalTime);

	printf("Stop C benchmark\n");*/
	printf("Starting Arithmetic C benchmark\n");
	long intArithmeticTime = 0;
	long longCountTime = 0;
	long totalTime = 0;
	int iterations = 1;
	int i = 0;
	for (i = 0; i < iterations; i++){
		printf("iteration %d",i);
		intArithmeticTime = intArithmetic(intMax);
		printf("after int");
		longCountTime = longArithmetic(longMin, longMax);
		printf("after long");
		totalTime = intArithmeticTime + longCountTime;
	}
	printf("finished");
	printf("int arithmetic benchmark time: %d ms\n", intArithmeticTime);
	printf("long arithmetic benchmark time: %d ms\n", longCountTime);
	printf("Total C benchmark time: %d ms\n", totalTime);
	printf("Average total time per iteration: %d ms\n", totalTime/iterations);
	printf("End C benchmark\n");
	return 0;
}


long intArithmetic(int intMax)
{
	long elapsedTime;
	int stopTime;
	int startTime = clock();

	int intResult = 1;
	int i = 1;
	while (i < intMax)
	{
		intResult -= i++;
		intResult += i++;
		intResult *= i++;
		intResult /= i++;
	}

	stopTime = clock();
	elapsedTime = ((stopTime - startTime)* 1000)/ CLOCKS_PER_SEC;
	return elapsedTime;
}


long longArithmetic(long long int longMin, long long int longMax)
{
	long elapsedTime;
	int stopTime;
	int startTime = clock();

	long long longResult = longMin;
	long long i = longMin;
	while (i < longMax)
	{
		longResult -= i++;
		longResult += i++;
		longResult *= i++;
		longResult /= i++;
	}

	stopTime = clock();
	elapsedTime = ((stopTime - startTime)*1000)/ CLOCKS_PER_SEC;
	return elapsedTime;
}

