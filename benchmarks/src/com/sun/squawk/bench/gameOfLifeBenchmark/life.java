
/**
  * Original code downloaded from http://www.aceshardware.com/articles/technical/java_vs_c/files/life.java
  *
  * Modified 22nd March, 2005 by Erica Glynn, Sun Microsystems Labs to remove reliance on floating point arithmetic
  * to allow Benchmarking of the Squawk JVM Embedded platform.
  */

package com.sun.squawk.bench.gameOfLifeBenchmark;

import java.util.*;

public class life {
    static long wantedTime = 1000;
    static long[] light_benchmarks = new long[] {2549, 2724, 3011, 2985, 3153, 3056, 2442, 793, 219, 90}; // compared to C results
    static long[] heavy_benchmarks = new long[] {2536, 2719, 2996, 1430, 1047, 716, 402, 88, 21, 8}; // compared to C results
    static long[] benchmarks;
    //static boolean useBenchmark = true;
    static boolean useBenchmark = false;

    static String java_bmark_list = "";
    static long rateFactor;
    static int rerunMainLoopCount = 4; // has to be at least 3
    static boolean show_table = false;
    static boolean lightLoad = true;
    static int[] use_Sizes = new int[] {5}; //, 6, 8, 10};

    int max_x, max_y;
    int[][] board;
    final int LiveVal = 16;
    int cell_live_set = (1<<3) | 1<<(3+LiveVal) | 1<<(2+LiveVal);
    int[] rowOne, rowZero, topRow, midRow, botRow, rowUsage;

    public static void main(String[] args) {
    	System.out.println("Starting Game of Life benchmark");
    	long startTime = System.currentTimeMillis();
    	doLife(args);
    	long timeTaken = System.currentTimeMillis() - startTime;
    	System.out.println("Total time taken " + timeTaken + " ms");
    }
    private static void doLife(String [] args){
    	
	    long[] it_rates = new long[rerunMainLoopCount];

	    if (lightLoad) {
   	        benchmarks = light_benchmarks;
    	} else {
	        benchmarks = heavy_benchmarks;
	    }
	
	    for (int c=0; c<use_Sizes.length; c++) {
	        life life_ob = new life(use_Sizes[c]);
	        long rate = life_ob.calcRate(1,20);
	        long cycles = (long)(rate*wantedTime/10);
	        rate = life_ob.calcRate(cycles,200);
	        cycles = (long)(rate*wantedTime);
	        for (int d=0; d<rerunMainLoopCount; d++) {
		        rate = life_ob.calcRate(cycles,(long)(wantedTime*9/10));
		        rate *= rateFactor;
		        it_rates[d] = rate;
	        }
	       //life_ob.calcPerformance(it_rates,(c==0));
	    }
    }

    long calcRate(long cycles_per_time, long max_time) {
		long big_counts = 0;
		long time_taken = 0;
		long now = getTimeAtTick();
		long was = now;
		while (time_taken <= max_time) {
		    big_counts ++;
		    for (long i=0; i<cycles_per_time; i++) {
			    calc_new();
		    }
		    now = System.currentTimeMillis();
		    time_taken = now-was;
		}
		long rate = (long)(big_counts * cycles_per_time)/(long)time_taken;
		rateFactor = max_x;
		return rate;
    }
    
    static long getTimeAtTick() {
	    long now = System.currentTimeMillis();
	    final long was = now;
	    while (was >= now) {
	        now = System.currentTimeMillis();
	    }
	    return now;
    }
    
    private void calcPerformance(long[] it_rates, boolean is_first) {
	sortArray(it_rates);
	for (int rep=0; rep<(rerunMainLoopCount-3); rep++) {
	    it_rates[1] += it_rates[rep+2];
	}
	it_rates[1] /= (rerunMainLoopCount-2);
	it_rates[2] = it_rates[rerunMainLoopCount-1];
	
	//String[] outputs = new String[3];
	//for (int rep=0; rep<3; rep++) {
	  //  outputs[rep] = pretty_number(it_rates[rep]);
	//}
	//String output = "";
	//for (int j=2; j>=0; j--) {
	   // if (j != 2) output += "\t";
	  //  output += outputs[j];
	//}
	//if (!is_first) java_bmark_list += ",";
	//java_bmark_list += outputs[1];
	//System.out.println(max_x+"\t"+output);
    }

    static private void sortArray(long[] array) {
	if (array.length < 2) return;
	boolean changed = true;
	while (changed) {
	    changed = false;
	    for (int c=1; c<array.length; c++) {
		if (array[c-1] > array[c]) {
		    changed = true;
		    long tmp = array[c];
		    array[c] = array[c-1];
		    array[c-1] = tmp;
		}
	    }
	}
    }

    static private String pretty_number(long num) {
	long cutoff = 120;
	if (num > cutoff) return ""+(int)num;
	if (num < 0) return "0";
	long fac = 1;
	while (num < cutoff) {
	    num *= 10;
	    fac *= 10;
	}
	int tt = (int)num;
	num = tt / fac;
	return ""+num;
    }

    public life(int size) {
	max_x = size;
	max_y = size;
	init_arrays();    

	if (lightLoad) {
	    set_line(2,new int[] {0,1,2});
	    set_line(1,new int[] {2});
	    set_line(0,new int[] {1});
	    return;
	}

	int block_width  = 5;
	int block_height = 5;
	boolean init[][] = new boolean[3][max_x];
	for (int x=0; x<=(max_x-block_width); x+= block_width) {
	    init[2][x+0] = init[2][x+1] = init[2][x+2] = true;
	                                  init[1][x+2] = true;
	                   init[0][x+1]                = true;
	}
	for (int y=0; y<=(max_y-block_height); y+= block_height) {
	    set_line(y+2,init[2]);
	    set_line(y+1,init[1]);
	    set_line(y+0,init[0]);
	}
    }
    
    public void set_line(int y,int[] alive) {
	int[] board_row = board[y];
	for (int c=1; c<=alive.length; c++) {
	    board_row[c] = alive[c-1];
	}
	board_row[0] = alive.length;
    }
    public void set_line(int y,boolean[] row) {
	int[] board_row = board[y];
	int live_count = 0;
	for (int c=0; c<row.length; c++) {
	    if (row[c]) live_count ++;
	}

	board_row[0] = live_count;
	live_count = 1;
	for (int c=0; c<row.length; c++) {
	    if (!row[c]) continue;
	    board_row[live_count] = c;
	    live_count ++;
	}
    }
    /*
    public void display() {
	StringBuffer output = new StringBuffer();
	boolean fl;
	for (int y=max_y; y>=-1; y--) {
	    if ((y==max_y) || (y==-1)) fl=true; else fl=false;
	    if (fl) output.append("\t+"); else output.append(y+"\t|");
	    int r_base = output.length();
	    for (int c=0; c<max_x; c++)
		if (fl) output.append("-"); else output.append(" ");
	    if (fl) output.append("+\n"); else output.append("|\n");
	    if (fl) continue;
	    
	    int[] board_row = board[y];
	    
	    for (int c=1;c<=board_row[0] ;c++) {
		int ov = r_base + board_row[c];
		if (ov>output.length()) {
		    System.out.println("Bad "+y+" "+c+" "+ov);
		} else {
		    output.setCharAt(ov,'#');
		}
	    }
	}    
	System.out.println(output);
    }
    */
    public void calc_new() {
	int[] tmp_row;
	
	do_row(0,rowOne,rowZero,topRow);
	do_row(max_y-1,rowZero,topRow,midRow);
	for (int y=max_y-2; y>2; y--) {
	    do_row(y,topRow,midRow,botRow);
	    change_board_row(y+1,topRow);
	    
	    tmp_row = topRow;
	    topRow = midRow;
	    midRow = botRow;
	    botRow = tmp_row;
	    for (int c=0;c<botRow.length;c++) botRow[c] = 0;
	}
	do_row(2,topRow,midRow,rowOne);
	change_board_row(3,topRow);
	do_row(1,midRow,rowOne,rowZero);
	change_board_row(2,midRow);
	change_board_row(1,rowOne);
	change_board_row(0,rowZero);
    }
    
    private void do_row(int y,int[] top_row,int[] mid_row,int[] bot_row) {
	int[] board_row = board[y];
	int num_live = board_row[0];
	for (int c=1;c<=num_live ;c++) {
	    int x = board_row[c];
	    update(x,top_row,mid_row,bot_row);
	}
	if (num_live==0) return;
	
	int top = y+1;
	int bot = y-1;
	if (top==max_y) top=0;
	if (bot==-1) bot=max_y-1;
	rowUsage[y]   += num_live;
	rowUsage[top] += num_live;
	rowUsage[bot] += num_live;
    }
    
    private void change_board_row(int y, int[] score) {
	int[] board_row = board[y];
	int usage = rowUsage[y];
	rowUsage[y] = 0;
	if (usage<3) {
	    //System.out.println();
	    if (usage>0) {
		board_row[0] = 0;
		for (int c=0;c<score.length;c++) score[c] = 0;
	    }
	    return;
	}
	//System.out.print("Upd "+y+","+usage+"\t:");
	int insert_pos = 1;
	for (int c=0; c<score.length; c++) {
	    if (((1<<score[c]) & cell_live_set)>0) {
		//System.out.print("["+score[c]+"]");
		board_row[insert_pos] = c;
		insert_pos ++;
	    } else {
		//System.out.print("<"+score[c]+">");
	    }
	    score[c] = 0;
	}
	//System.out.println();
	board_row[0] = insert_pos-1;
    }
    
    private void update(int x,int[] top_row,int[] mid_row,int[] bot_row) {
	if (x==0) {
	    update_point_three_m1(top_row,x);
	    update_sides_m1(mid_row,x);
	    update_point_three_m1(bot_row,x);
	    return;
	}
	if (x==(max_x-1)) {
	    update_point_three_p1(top_row,x);
	    update_sides_p1(mid_row,x);
	    update_point_three_p1(bot_row,x);
	    return;
	}
	update_point_three(top_row,x);
	update_sides(mid_row,x);
	update_point_three(bot_row,x);
    }
    
    private void update_sides_m1(int[] score_row, int index) {
	score_row[max_x-1] += 1;
	score_row[index-0] += LiveVal;
	score_row[index+1] += 1;
    }
    private void update_sides(int[] score_row, int index) {
	score_row[index-1] += 1;
	score_row[index-0] += LiveVal;
	score_row[index+1] += 1;
    }
    private void update_sides_p1(int[] score_row, int index) {
	score_row[index-1] += 1;
	score_row[index-0] += LiveVal;
	score_row[0      ] += 1;
    } 
    
    private void update_point_three_m1(int[] score_row, int index) {
	score_row[max_x-1] += 1;
	score_row[index-0] += 1;
	score_row[index+1] += 1;
    }
    private void update_point_three(int[] score_row, int index) {
	score_row[index-1] += 1;
	score_row[index-0] += 1;
	score_row[index+1] += 1;
    }
    private void update_point_three_p1(int[] score_row, int index) {
	score_row[index-1] += 1;
	score_row[index-0] += 1;
	score_row[0      ] += 1;
    }
    
    private void init_arrays() {
	board = new int[max_y][];
	for (int c=0;c<max_y;c++) {
	    board[c] = new int[max_x];
	}
	
	rowOne  = new int[max_x];
	rowZero = new int[max_x];
	topRow  = new int[max_x];
	midRow  = new int[max_x];
	botRow  = new int[max_x];
	
	rowUsage = new int[max_y];
    }
}
