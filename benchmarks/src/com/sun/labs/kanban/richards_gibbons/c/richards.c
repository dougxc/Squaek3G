/*  C version of the systems programming language benchmark    */
/*  Author:  M. J. Jordan  Cambridge Computer Laboratory.      */
/*  Revised: J. J. Gibbons Sun Microsystems		       */
/*  Fixed timing bug: Mario Wolczko, Sun Microsystems */

#include <sys/types.h>
#include <time.h>

#
# define                TRUE            1
# define                FALSE           0
# define                MAXINT          32767

# define                BUFSIZE         3
# define                I_IDLE          1
# define                I_WORK          2
# define                I_HANDLERA      3
# define                I_HANDLERB      4
# define                I_DEVA          5
# define                I_DEVB          6
# define                PKTBIT          1
# define                WAITBIT         2
# define                HOLDBIT         4
# define                NOTPKTBIT       !1
# define                NOTWAITBIT      !2
# define                NOTHOLDBIT      0XFFFB

# define                S_RUN           0
# define                S_RUNPKT        1
# define                S_WAIT          2
# define                S_WAITPKT       3
# define                S_HOLD          4
# define                S_HOLDPKT       5
# define                S_HOLDWAIT      6
# define                S_HOLDWAITPKT   7

# define                K_DEV           1000
# define                K_WORK          1001

struct packet
{
   struct packet        *p_link;
    int         p_id;
    int         p_kind;
    int         p_a1;
    char        p_a2[4];
};

struct task
{
    struct task *t_link;
    int         t_id;
    int         t_pri;
    struct packet       *t_wkq;
    int         t_state;
    struct task *(*t_fn)();
    int         t_v1;
    int         t_v2;
};

union data 
{
    struct task *t;
    struct packet *p;
    int i;
};

char    	alphabet[28]	= "0ABCDEFGHIJKLMNOPQRSTUVWXYZ";
struct task 	*tasktab[11]	= {0,0,0,0,0,0,0,0,0,0,0};
int		tasktabsize	= 10;
struct task 	*tasklist   = 0;
struct task 	*tcb;
int     	taskid;
union data     	v1;
union data	v2;
int     qpktcount       = 0;
int     holdcount       = 0;
int     tracing         = 1;
int     layout          = 0;


createtask(id,pri,wkq,state,fn,v1,v2)
    int id,pri,state,v1,v2;
    struct packet *wkq;
    struct task *(*fn)();
{
    struct task *t;

    t = (struct task *)malloc(sizeof(*t));

    tasktab[id] = t;
    t->t_link = tasklist;
    t->t_id = id;
    t->t_pri = pri;
    t->t_wkq = wkq;
    t->t_state = state;
    t->t_fn = fn;
    t->t_v1 = v1;
    t->t_v2 = v2;
    tasklist = t;
}

struct packet *pkt(link, id, kind)
    struct packet *link;
    int id, kind;
{
    struct packet *p;
    int i;

    p = (struct packet *)malloc(sizeof(*p));

    for (i=0; i<=BUFSIZE; i++)
        p->p_a2[i] = 0;

    p->p_link = link;
    p->p_id = id;
    p->p_kind = kind;
    p->p_a1 = 0;

    return (p);
}

trace(a) char a;
{
   if ( --layout <= 0 )
   {
        printf("\n");
        layout = 50;
    }

    printf("%c", a);
}

schedule()
{
	int numIter = 0;
	int timeBefore = clock();
    while ( tcb != 0 )
    {
    	numIter = numIter + 1;
        struct packet *pkt;
        struct task *newtcb;
        struct task *(*funcp)();

/*     if (tracing==TRUE) printf("tcb = %d, state=%d\n", tcb, tcb->t_state); */
        
        pkt=0;

        switch ( tcb->t_state )
        {
            case S_WAITPKT:
                pkt = tcb->t_wkq;
                tcb->t_wkq = pkt->p_link;
                tcb->t_state = tcb->t_wkq == 0 ? S_RUN : S_RUNPKT;

            case S_RUN:
            case S_RUNPKT:
                taskid = tcb->t_id;
                v1.i = tcb->t_v1;
                v2.i = tcb->t_v2;
                if (tracing==TRUE) trace(taskid+'0');

                funcp = tcb->t_fn;
                newtcb = (*funcp)(pkt);
                tcb->t_v1 = v1.i;
                tcb->t_v2 = v2.i;
                tcb = newtcb;
                break;

            case S_WAIT:
            case S_HOLD:
            case S_HOLDPKT:
            case S_HOLDWAIT:
            case S_HOLDWAITPKT:
                tcb = tcb->t_link;
                break;

            default:
                return;
        }
    }
    int timeTaken = clock() - timeBefore;
    printf("Total: %d numIter: %d average: %d\n", timeTaken, numIter, timeTaken/numIter);
}

struct task *wait()
{
    tcb->t_state |= WAITBIT;
    return (tcb);
}

struct task *holdself()
{
    ++holdcount;
    tcb->t_state |= HOLDBIT;
    return (tcb->t_link) ;
}

struct task *findtcb(id) int id;
{
    struct task *t;

    t=0;
    if (1<=id && id<=tasktabsize) t = tasktab[id];
    if (t==0) printf("\nBad task id %d\n", id);
    return(t);
}
struct task *release(id)  int id;
{
    struct task *t;

    t = findtcb(id);
    if ( t==0 ) return (0);

    t->t_state &= NOTHOLDBIT;
    if ( t->t_pri > tcb->t_pri ) return (t);

    return (tcb) ;
}


struct task *qpkt(pkt) struct packet *pkt;
{
    struct task *t;

    t = findtcb(pkt->p_id);
    if (t==0) return (t);

    qpktcount++;

    pkt->p_link = 0;
    pkt->p_id = taskid;

   if (t->t_wkq==0)
    {
        t->t_wkq = pkt;
        t->t_state |= PKTBIT;
        if (t->t_pri > tcb->t_pri) return (t);
    }
    else
    {
        append(pkt, &(t->t_wkq));
    }

    return (tcb);
}

struct task *idlefn(pkt) struct packet *pkt;
{
    --v2.i;
    if ( v2.i==0 ) return ( holdself() );

    if ( (v1.i&1) == 0 )
    {
        v1.i = ( v1.i>>1) & MAXINT;
        return ( release(I_DEVA) );
    }
    else
    {
        v1.i = ( (v1.i>>1) & MAXINT) ^ 0XD008;
        return ( release(I_DEVB) );
    }
}

struct task *workfn(pkt) struct packet *pkt;
{
    if ( pkt==0 ) return ( wait() );
    else
    {
        int i;

        v1.i = I_HANDLERA + I_HANDLERB - v1.i;
        pkt->p_id = v1.i;

        pkt->p_a1 = 0;
        for (i=0; i<=BUFSIZE; i++)
        {
            v2.i++;
            if ( v2.i > 26 ) v2.i = 1;
            (pkt->p_a2)[i] = alphabet[v2.i];
        }
        return ( qpkt(pkt) );
    }
}

struct task *handlerfn(pkt) struct packet *pkt;
{
    if ( pkt!=0) append(pkt, pkt->p_kind==K_WORK ? &v1.p : &v2.p);

   if ( v1.p!=0 )
    {
        struct packet *workpkt;
        int count;

        workpkt = v1.p;
        count = workpkt->p_a1;

        if ( count > BUFSIZE )
        {
            v1.p = v1.p->p_link;
           return ( qpkt(workpkt) );
        }

        if ( v2.p!=0 )
        {
            struct packet *devpkt;

            devpkt = v2.p;
            v2.p = v2.p->p_link;
            devpkt->p_a1 = workpkt->p_a2[count];
            workpkt->p_a1 = count+1;
            return( qpkt(devpkt) );
        }
    }
    return ( wait() );
}

struct task *devfn(pkt) struct packet *pkt;
{
    if ( pkt==0 )
    {
        if ( v1.p==0 ) return ( wait() );
        pkt = v1.p;
        v1.p = 0;
        return ( qpkt(pkt) );
    }
    else
    {
        v1.p = pkt;
        if (tracing==TRUE) trace(pkt->p_a1);
        return ( holdself() );
    }
}

append(pkt, ptr) struct packet *pkt;  struct packet **ptr;
{
    pkt->p_link = 0;

    while ( *ptr!=0 )
        ptr = &( (*ptr)->p_link );

    *ptr = pkt;
}

#define ITER 100

main()
{
  clock_t start_time, end_time;
  double total_s, avg_ms;
  int i;

  printf("Richards #1 starting...\n");
  
  start_time = clock();
  
  for (i = 0;  i < ITER;  ++i) {
    struct packet *wkq= 0;
    
    createtask(I_IDLE, 0, wkq, S_RUN, idlefn, 1, 10000);
    printf("idle: %d\n", clock());
    
    wkq = pkt(0, 0, K_WORK);
    wkq = pkt(wkq, 0, K_WORK);
    
    createtask(I_WORK, 1000, wkq, S_WAITPKT, workfn, I_HANDLERA, 0);
    printf("work: %d\n", clock());
    
    wkq = pkt(0, I_DEVA, K_DEV);
    wkq = pkt(wkq, I_DEVA, K_DEV);
    wkq = pkt(wkq, I_DEVA, K_DEV);
    
    createtask(I_HANDLERA, 2000, wkq, S_WAITPKT, handlerfn, 0, 0);
    printf("handler a: %d\n", clock());
    
    wkq = pkt(0, I_DEVB, K_DEV);
    wkq = pkt(wkq, I_DEVB, K_DEV);
    wkq = pkt(wkq, I_DEVB, K_DEV);
    
    createtask(I_HANDLERB, 3000, wkq, S_WAITPKT, handlerfn, 0, 0);
    printf("handler b: %d\n", clock());
    
    wkq = 0;
    createtask(I_DEVA, 4000, wkq, S_WAIT, devfn, 0, 0);
    createtask(I_DEVB, 5000, wkq, S_WAIT, devfn, 0, 0);
    printf("devices: %d\n", clock());
    
    tcb = tasklist;
    
    qpktcount = holdcount = 0;
    
    tracing = FALSE;
    layout = 0;
    
    schedule();
    printf("after sched: %d\n", clock());
    
    if (qpktcount == 23246 && holdcount == 9297)
      ; /* correct */
    else {
      printf("incorrect results!\n");
      exit(1);
    }
  }
  
  end_time = clock();
  
  printf("finished\n");
  
  total_s= (end_time - start_time) / (double)CLOCKS_PER_SEC;
  avg_ms= total_s / ITER * 1000;
  printf("elapsed time for %d iterations: %.3g seconds\n", ITER, total_s);
  printf("average time per iteration: %.3g ms\n", avg_ms);
}

