#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <time.h>

#define LiveVal 16

int wantedTime = 1000;
long *benchmarks;
long light_benchmarks[10] = {2549,2724,3011,2985,3153,3056,2442,793,219,90};
long heavy_benchmarks[10] = {2536,2719,2996,1430,1047,716,402,88,21,8};
int useBenchmark = 1;
int use_Sizes[1] = {5}; //,6,8,10};

int max_x, max_y;
int **board;
int cell_live_set = (1<<3) | 1<<(2+LiveVal) | 1<<(3+LiveVal);
int *rowOne,*rowZero,*topRow,*midRow,*botRow,*rowUsage;
int show_table = 0;
int lightLoad = 1;
#define rerunMainLoopCount 4
int rateFactor;
long conv_to_msecs = 1000/CLOCKS_PER_SEC;  

long calcRate(long cycles_per_time, long max_time);
void calcPerformance(long it_rates[], int usecomma);
clock_t getTimeAtTick();
void pretty_number(long num, char* buff);

void Life(int size);
void set_bline(int y,int row[]);
void set_line(int y,int alive[]);
void display();
void calc_new();
void do_row(int y,int top_row[],int mid_row[],int bot_row[]);
void change_board_row(int y, int score[]);
void update(int x,int top_row[],int mid_row[],int bot_row[]);
void update_sides_m1(int score_row[], int index);
void update_sides(int score_row[], int index);
void update_sides_p1(int score_row[], int index);
void update_point_three_m1(int score_row[], int index);
void update_point_three(int score_row[], int index);
void update_point_three_p1(int score_row[], int index);
void init_arrays();


 int main(int argc, char **argv) {
	printf("Starting Game of Life benchmark\r\n");
	long startTime = clock();
	doLife(argc, argv);
	long timeTaken = (clock() - startTime)* conv_to_msecs;
	printf("Total time taken %d ms", timeTaken);
	return 0;
}

int doLife(int argc, char **argv) {
  int c,d;
  int alength = sizeof(use_Sizes) / sizeof(int);
  long rate,cycles;
  long it_rates[rerunMainLoopCount];

  if (lightLoad) {
      benchmarks = light_benchmarks;
  } else {
      benchmarks = heavy_benchmarks;
  }
  for (c=0; c<alength; c++) {

	    if (lightLoad) {
   	        benchmarks = light_benchmarks;
    	} else {
	        benchmarks = heavy_benchmarks;
	    }
	    Life(use_Sizes[c]);
	
	    long rate = calcRate(1,20);
        long cycles = (long)(rate*wantedTime/10);
        rate = calcRate(cycles,200);
        cycles = (long)(rate*wantedTime);
        for (d=0; d<rerunMainLoopCount; d++) {
	        rate = calcRate(cycles,(long)(wantedTime*9/10));
	        rate *= rateFactor;
	        it_rates[d] = rate;
        }
  }
  return 0;
}

long calcRate(long cycles_per_time, long max_time) {
  long i,big_counts, time_taken, now, was;
  long rate, time_taken_ms;
  
  big_counts = 0;
  time_taken = 0;
  was = now = getTimeAtTick();
  while (time_taken <= max_time) {
    big_counts ++;
    for (i=0; i<cycles_per_time; i++) {
      calc_new();
    }
    now = clock();
    time_taken = now-was;
  }
  time_taken_ms = time_taken * conv_to_msecs;
  rate = (long)(big_counts * cycles_per_time)/(long)time_taken_ms;
  rateFactor = max_x; /* more linear than max_x*max_y */
  return rate;
}

static int longcompare(const void *i, const void *j) {
    if (*(long*)i > *(long*)j) return (1);
    if (*(long*)i < *(long*)j) return (-1);
    return (0);
}
/*
void calcPerformance(long it_rates[], int usecomma) {
    int rep,j;
    long iter_rate;
    char *output, *outputs[3];
    output = (char*)calloc(200,sizeof(char));

    qsort((void*)it_rates,5,sizeof(long),longcompare);
    for (rep=0; rep<(rerunMainLoopCount-3); rep++) {
	it_rates[1] += it_rates[rep+2];
    }
    it_rates[1] /= (rerunMainLoopCount-2);
    it_rates[2] = it_rates[rerunMainLoopCount-1];
	
    for (rep=0; rep<3; rep++) {
	iter_rate = it_rates[rep];
	outputs[rep] = (char*)calloc(15,sizeof(char));
	pretty_number(iter_rate,outputs[rep]);
    }
    for (j=2; j>=0; j--) {
      if (j != 2) output = strcat(output,"\t");
      output = strcat(output,outputs[j]);
    }
    printf("%d\t%s\n", max_x,output);

    if (usecomma) {
      c_bmark_list = strcat(c_bmark_list,",");
    }
    c_bmark_list = strcat(c_bmark_list,outputs[1]);

    free(output);
    for (rep=0; rep<3; rep++) free(outputs[rep]);
}*/

clock_t getTimeAtTick() {
  clock_t now,was;
  was = now = clock();
  while (was >= now) {
    now = clock();
  }
  return now;
}
/*
void pretty_number(long num, char* buff) {
    int point;
    long cutoff = 120;

    if (num > cutoff) {
	sprintf(buff,"%d",num);
	return;
    }
    if (num < 0) {
	sprintf(buff,"%d",0);
	return;
    }

    sprintf(buff,"%d",num);
    return;
}*/

void Life(int size) {
  int setup1[] = {3,0,1,2};
  int setup2[] = {1,2};
  int setup3[] = {1,1};
  int block_width  = 5;
  int block_height = 5;
  int x,y;
  int *init0,*init1,*init2;

  max_x = size;
  max_y = size;
  init_arrays();    

  if (lightLoad) {
    set_line(2,setup1);
    set_line(1,setup2);
    set_line(0,setup3);
    return;
  }

  init0 = (int*)calloc(max_x,sizeof(int));
  init1 = (int*)calloc(max_x,sizeof(int));
  init2 = (int*)calloc(max_x,sizeof(int));
  for (x=0; x<max_x; x++) {
    init2[x] = 0;
    init1[x] = 0;
    init0[x] = 0;
  }

  for (x=0; x<=(max_x-block_width); x+= block_width) {
    init2[x+0] = 1;
    init2[x+1] = 1;
    init2[x+2] = 1;
    init1[x+2] = 1;
    init0[x+1] = 1;
  }
  for (y=0; y<=(max_y-block_height); y+= block_height) {
    set_bline(y+2,init2);
    set_bline(y+1,init1);
    set_bline(y+0,init0);
  }
  free(init0);
  free(init1);
  free(init2);

}

void set_line(int y,int alive[]) {
  int *board_row;
  int c;
  board_row = board[y];
  for (c=0; c<=alive[0]; c++) {
    board_row[c] = alive[c];
  }
}
void set_bline(int y,int row[]) {
  int *board_row;
  int c;
  int live_count = 0;

  for (c=0; c<max_x; c++) {
    if (row[c] == 1) live_count ++;
  }

  board_row = board[y];
  board_row[0] = live_count;
  live_count = 1;
  for (c=0; c<max_x; c++) {
    if (row[c] != 1) continue;
    board_row[live_count] = c;
    live_count ++;
  }
}

void show_board_edge_line() {
  int c;
  printf("+");
  for (c=0; c<max_x; c++) {
    printf("-");
  }
  printf("+\n");
}
/*
void display() {
  int y,c,poi,ov;
  int *board_row;
  char *output;

  output = (char*)calloc(max_x+1,sizeof(char));
  output[max_x] = 0;

  show_board_edge_line();
  for (y=max_y-1; y>=0; y--) {
    board_row = board[y];
    poi = 0;
    for (c=0; c<max_x; c++) output[c] = ' ';
    for (c=1;c<=board_row[0] ;c++) {
      ov = board_row[c];
      if (ov>max_x) {
      } else {
	output[ov] = '#';
      }
    }
    
    printf("|%s|\n",output);
  }
  free(output);
  show_board_edge_line();
}*/

void calc_new() {
  int *tmp_row;
  int y,c;
  do_row(0,rowOne,rowZero,topRow);
  do_row(max_y-1,rowZero,topRow,midRow);
  for (y=max_y-2; y>2; y--) {
    do_row(y,topRow,midRow,botRow);
    change_board_row(y+1,topRow);
    
    tmp_row = topRow;
    topRow = midRow;
    midRow = botRow;
    botRow = tmp_row;
    for (c=0;c<max_x;c++) botRow[c] = 0;
  }
  do_row(2,topRow,midRow,rowOne);
  change_board_row(3,topRow);
  do_row(1,midRow,rowOne,rowZero);
  change_board_row(2,midRow);
  change_board_row(1,rowOne);
  change_board_row(0,rowZero);
}

void do_row(int y,int top_row[],int mid_row[],int bot_row[]) {
  int *board_row;
  int num_live;
  int c,top,bot,x;

  board_row = board[y];
  num_live = board_row[0];
  for (c=1;c<=num_live ;c++) {
    x = board_row[c];
    update(x,top_row,mid_row,bot_row);
  }
  if (num_live==0) return;
  
  top = y+1;
  bot = y-1;
  if (top==max_y) top=0;
  if (bot==-1) bot=max_y-1;
  rowUsage[y]   += num_live;
  rowUsage[top] += num_live;
  rowUsage[bot] += num_live;
}

void change_board_row(int y, int score[]) {
  int insert_pos;
  int *board_row;
  int usage;
  int c;

  insert_pos = 1;
  board_row = board[y];
  usage = rowUsage[y];
  
  rowUsage[y] = 0;
  if (usage<3) {
    if (usage>0) {
      board_row[0] = 0;
      for (c=0;c<max_x;c++) score[c] = 0;
    }
    return;
  }
  for (c=0; c<max_x; c++) {
    if (((1<<score[c]) & cell_live_set)>0) {
      board_row[insert_pos] = c;
      insert_pos ++;
    }
    score[c] = 0;
  }
  board_row[0] = insert_pos-1;
}

void update(int x,int top_row[],int mid_row[],int bot_row[]) {
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

void update_sides_m1(int score_row[], int index) {
  score_row[max_x-1] += 1;
  score_row[index-0] += LiveVal;
  score_row[index+1] += 1;
  return;
}
void update_sides(int score_row[], int index) {
  score_row[index-1] += 1;
  score_row[index-0] += LiveVal;
  score_row[index+1] += 1;
  return;
}
void update_sides_p1(int score_row[], int index) {
  score_row[index-1] += 1;
  score_row[index-0] += LiveVal;
  score_row[0      ] += 1;
  return;
}

void update_point_three_m1(int score_row[], int index) {
  score_row[max_x-1] += 1;
  score_row[index-0] += 1;
  score_row[index+1] += 1;
  return;
}
void update_point_three(int score_row[], int index) {
  score_row[index-1] += 1;
  score_row[index-0] += 1;
  score_row[index+1] += 1;
  return;
}
void update_point_three_p1(int score_row[], int index) {
  score_row[index-1] += 1;
  score_row[index-0] += 1;
  score_row[0      ] += 1;
  return;
}

void init_arrays() {
  int c;
  if (board) {
    free(board);
    free(rowOne);
    free(rowZero);
    free(topRow);
    free(midRow);
    free(botRow);
    free(rowUsage);
  }

  board = (int**) calloc(max_y,sizeof(int*));
  for (c=0;c<max_y;c++) {
    board[c] = (int*)calloc(max_x,sizeof(int));
    board[c][0] = 0;
  }
  
  rowOne  = (int*)calloc(max_x,sizeof(int));
  rowZero = (int*)calloc(max_x,sizeof(int));
  topRow  = (int*)calloc(max_x,sizeof(int));
  midRow  = (int*)calloc(max_x,sizeof(int));
  botRow  = (int*)calloc(max_x,sizeof(int));

  rowUsage = (int*)calloc(max_y,sizeof(int));
  return;
}

