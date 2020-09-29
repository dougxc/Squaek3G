/*  C++ version of the systems programming language benchmark    */
/*  Author:  J. J. Gibbons Sun Microsystems		       */
/*  Fixed timing bug: Mario Wolczko, Sun Microsystems */
/*  Added a timing loop: Alex Jacoby, Sun Microsystems */

#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <time.h>

const char alphabet[] = "0ABCDEFGHIJKLMNOPQRSTUVWXYZ";
const int MAXINT = 32767;

const int BUFSIZE         = 3;
const int PKTBIT          = 1;
const int WAITBIT         = 2;
const int HOLDBIT         = 4;
const int NOTHOLDBIT      = ~HOLDBIT;

enum state {
  S_RUN, 
  S_RUNPKT, 
  S_WAIT, 
  S_WAITPKT, 
  S_HOLD, 
  S_HOLDPKT, 
  S_HOLDWAIT,
  S_HOLDWAITPKT
};

enum id {I_IDLE=1, I_WORK, I_HANDLERA, I_HANDLERB, I_DEVA, I_DEVB};

const int K_DEV = 1000;
const int K_WORK = 1001;

static int qpktcount = 0;
static int holdcount = 0;
static int tracing   = 0;
static int layout    = 0;

class task;
   
struct packet {
  packet *link;
  int id;
  int kind;
  int a1;
  char a2[4];
  packet(packet *l, int i, int k);
  void append_to(packet *&);
};

packet::packet(packet *l, int i, int k) {
  link = l;
  id = i;
  kind = k;
  a1 = 0;
  for (int j=0; j<=BUFSIZE; j++) a2[j] = 0;
}

void
packet::append_to(packet *&list) {
  link = 0;
  packet **ptr = &list;
  while ( *ptr!=0 ) ptr = &( (*ptr)->link );
  *ptr = this;
}


class task {
  task *link;
  int id;
  int pri;
  packet *wkq;
  int state;
  enum {tasktabsize = 10};
  static task *tasktab[tasktabsize];
  static task *tasklist;
  friend task *findtcb(int);  // ideally should be a static member function
protected:
  task *wait();
  task *hold();
  task *release(int);
  task *qpkt(packet*);
public:
  task(int i, int p, packet *w, int s) {
    link = tasklist;
    id = i;
    pri = p;
    wkq = w;
    state = s;
    tasklist = this;
    tasktab[i] = this;
  }
  virtual task *fn(packet *);
  friend void schedule();
};
 
task *task::tasktab[] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
task *task::tasklist = 0;

task *
task::wait() {
    state |= WAITBIT;
    return this;
}

task *
task::hold() {
    ++holdcount;
    state |= HOLDBIT;
    return link;
}

task *
task::release(int i) {
    task *t = findtcb(i);
    t->state &= NOTHOLDBIT;
    if ( t->pri > pri ) return (t);

    return this;
}

task *
task::qpkt(packet *pkt) {
    task *t = findtcb(pkt->id);
    if (t==0) return (t);

    qpktcount++;

    pkt->link = 0;
    pkt->id = id;

    if (t->wkq==0) {
      t->wkq = pkt;
      t->state |= PKTBIT;
      if (t->pri > pri) return (t);
    } else {
      pkt->append_to(t->wkq);
    }

    return (this);
}

task *
task::fn(packet *) {
  exit(99);
  return (this);
}

task *
findtcb(int id) {
  task *t = 0;
  if (1<=id && id<=task::tasktabsize) t = task::tasktab[id];
  if (t==0) printf("\nBad task id %d\n", id);
  return(t);
}

void
trace(char a) {
  if ( --layout <= 0 ) {
    printf("\n");
    layout = 50;
  }
  printf("%c", a);
}


void
schedule()
{
    task *t = task::tasklist;
    while ( t != 0 )
    {
        packet *pkt = 0;

/*      if (tracing) printf("tcb = %d, state=%d\n", t, t->state); */

        switch ( t->state )
        {
            case S_WAITPKT:
                pkt = t->wkq;
                t->wkq = pkt->link;
                t->state = t->wkq == 0 ? S_RUN : S_RUNPKT;

            case S_RUN:
            case S_RUNPKT:
                if (tracing) trace(t->id+'0');
                t = t->fn(pkt);
                break;

            case S_WAIT:
            case S_HOLD:
            case S_HOLDPKT:
            case S_HOLDWAIT:
            case S_HOLDWAITPKT:
                t = t->link;
                break;

            default:
                return;
        }
    }
}

class idletask: public task {
  int v1;
  int v2;
public:
  idletask(int i, int a1, int a2)
      : task(i, 0, 0, S_RUN)  { v1 = a1;  v2 = a2; }
  task *fn(packet *) {
    --v2;
    if ( v2==0 ) {
      return ( hold() );
    } else if ( (v1&1) == 0 ) {
      v1 = ( v1>>1) & MAXINT;
      return ( release(I_DEVA) );
    } else {
      v1 = ( (v1>>1) & MAXINT) ^ 0XD008;
      return ( release(I_DEVB) );
    }
  }
};

class worktask: public task {
  int handler; 
  int n;
public:
  worktask(int i, int p, packet *w) 
      : task(i, p, w, (w ? S_WAITPKT : S_WAIT))  { handler = I_HANDLERA;  n = 0; }
  task *fn(packet *pkt) {
    if ( pkt==0 ) return ( wait() );
    else {
      handler = (handler == I_HANDLERA ? I_HANDLERB : I_HANDLERA);
      pkt->id = handler;

      pkt->a1 = 0;
      for (int i=0; i<=BUFSIZE; i++) { 
	n++;
	if ( n > 26 ) n = 1;
	(pkt->a2)[i] = alphabet[n];
      }
      return ( qpkt(pkt) );
    }
  }
};

class handlertask: public task {
  packet *workpkts;
  packet *devpkts;
public:
  handlertask(int i, int p, packet *w)
      : task(i, p, w, (w ? S_WAITPKT : S_WAIT))  { workpkts = devpkts = 0; }
  task *fn(packet *pkt) {
    if ( pkt!=0) 
      if (pkt->kind==K_WORK) 
         pkt->append_to(workpkts);
      else
         pkt->append_to(devpkts);

    if ( workpkts!=0 ) {
      packet *workpkt = workpkts;
      int count = workpkt->a1;

      if ( count > BUFSIZE ) {
        workpkts = workpkt->link;
        return ( qpkt(workpkt) );
      }

      if ( devpkts!=0 ) {
        packet *devpkt = devpkts;
        devpkts = devpkt->link;
        devpkt->a1 = workpkt->a2[count];
        workpkt->a1 = count+1;
        return( qpkt(devpkt) );
      }
    }
    return ( wait() );
  }
};

class devicetask: public task {
  packet *v1;
public:
  devicetask(int i, int p, packet *w)
      : task(i, p, w, (w ? S_WAITPKT : S_WAIT))  { v1 = 0; }
  task *fn(packet *pkt) {
    if ( pkt==0 ) {
      if ( v1==0 ) return ( wait() );
      pkt = v1;
      v1 = 0;
      return ( qpkt(pkt) );
    } else {
      v1 = pkt;
      if (tracing) trace(pkt->a1);
      return ( hold() );
    }
  }
};

#define ITER 100

main()
{
  printf("Richards #1 starting\n");
  clock_t start_time= clock();
  for (int i = 0; i < ITER; i++) {
    
    new idletask(I_IDLE, 1, 10000);
    
    packet *wkq = new packet(0, 0, K_WORK);
    wkq = new packet(wkq, 0, K_WORK);
    new worktask(I_WORK, 1000, wkq);
    
    wkq = new packet(0, I_DEVA, K_DEV);
    wkq = new packet(wkq, I_DEVA, K_DEV);
    wkq = new packet(wkq, I_DEVA, K_DEV);
    new handlertask(I_HANDLERA, 2000, wkq);
    
    wkq = new packet(0, I_DEVB, K_DEV);
    wkq = new packet(wkq, I_DEVB, K_DEV);
    wkq = new packet(wkq, I_DEVB, K_DEV);
    new handlertask(I_HANDLERB, 3000, wkq);
    
    wkq = 0;
    new devicetask(I_DEVA, 4000, wkq);
    new devicetask(I_DEVB, 5000, wkq);
    
    qpktcount = holdcount = 0;
    
    schedule();
    
    if (qpktcount == 23246 && holdcount == 9297)
      ; // correct
    else {
      printf("incorrect result!\n");
      exit(1);
    }
    
  }
  clock_t end_time= clock();
  double total_s= (end_time - start_time) / double(CLOCKS_PER_SEC);
  double avg_ms= total_s / ITER * 1000;
  printf("elapsed time for %d iterations: %.3g seconds\n", ITER, total_s);
  printf("average time per iteration: %.3g ms\n", avg_ms);
}

