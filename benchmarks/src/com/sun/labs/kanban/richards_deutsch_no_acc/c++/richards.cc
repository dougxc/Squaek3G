/* richards.cc - Richards Benchmark in C++ */
/* Urs Hoelzle 2/2/89 */
/* mods, Mario Wolczko 4/2/96 */
/* mods, Alex Jacoby, 8/9/96 */

#include <time.h>
#include <stdio.h>
#include <ctype.h>
#include <assert.h>
#include <stdlib.h>
#include <sys/types.h>

// Some compilers predefine this; if so, comment it out.
// typedef enum { false, true } bool;

typedef enum {DevicePacket, WorkPacket} PacketKind;

typedef enum {Idler, Worker, HandlerA, HandlerB, DeviceA, DeviceB} Identity;

#define NTASKS 6                        /* # elements of Identity */
				 	/* = # of tasks		  */
#define NoWork NULL
#define NoTask NULL

/*
// Packet
*/

class Packet {

public:
  Packet *link;		// next packet in queue
  Identity ident;	// Idle, Worker, DeviceA, ...
  PacketKind kind;	// DevicePacket or WorkPacket
  int datum;
  char data[4];

public:
  Packet(Packet *l, Identity id, PacketKind k);

  Packet *AddToList(Packet *list);
};

Packet::Packet(Packet *l, Identity id, PacketKind k)
{
  link = l;
  ident = id;
  kind = k;
  datum = 1;
  for(int i = 0; i < 4; i++)
    data[i] = 0;
}

/*
// AddToList - list utility (append this at end of list, return head)
*/
Packet *Packet::AddToList(Packet *list)
{
  link = NoWork;
  if (list == NoWork)
    list = this;
  else {
    Packet *p, *next;
    p = list;
    while (next = p->link, next != NoWork)
      p = next;
    p->link = this;
  }    
  return list;
}



/*
// DeviceTaskRec
*/

class DeviceTaskRec {
public:
  Packet *pending;
public:
  DeviceTaskRec() { pending = NoWork; }
};



/*
// IdleTaskRec 
*/

class IdleTaskRec {
public:
  int control, count;
public:
  IdleTaskRec() { control = 1; count = 10000; }
};


/*
// HandlerTaskRec
*/

class HandlerTaskRec {
public:
  Packet *workIn, *deviceIn;
public:
  HandlerTaskRec() { workIn = deviceIn = NoWork; }

  Packet *  WorkInAdd(Packet *p) {
    return workIn = p->AddToList(workIn); }
  Packet *DeviceInAdd(Packet *p) {
    return deviceIn = p->AddToList(deviceIn); }
};


/*
// WorkerTaskRec
*/

class WorkerTaskRec {
public:
  Identity destination;
  int count;
public:
  WorkerTaskRec() { destination = HandlerA; count = 0; }
};



/*
// TaskState
*/

class TaskState {
public:
  bool packetPending, taskWaiting, taskHolding;
	
public:
  /* initializing */
  TaskState() {
    packetPending = true;
    taskWaiting = taskHolding = false;
  }
  void PacketPending() {
    packetPending = true;
    taskWaiting = taskHolding = false;
  }
  void Waiting() {
    packetPending = taskHolding = false;
    taskWaiting = true;
  }
  void Running() {
    packetPending = taskWaiting = taskHolding = false;
  }
  void WaitingWithPacket() {
    packetPending = taskWaiting = true; taskHolding = false;
  }

  /* testing */ 
  bool IsTaskHoldingOrWaiting() {
    return bool(taskHolding || !packetPending && taskWaiting);
  }
  bool IsWaitingWithPacket() {
    return bool(packetPending && taskWaiting && !taskHolding);
  }
};

/*
// TaskControlBlock
*/

class TaskControlBlock : public TaskState {

public:
  TaskControlBlock *link;
  Identity ident;
  int priority;
  Packet *input;
  void *handle;
  
public:
  /* initializing */
  TaskControlBlock(TaskControlBlock *l, Identity id, int prio,
		   Packet *initialWork, TaskState *initialState,
		   void *privateData);

  /* scheduling */
  TaskControlBlock *AddPacket(Packet *p, TaskControlBlock *old);
  virtual TaskControlBlock *ActionFunc(Packet *p, void *handle);
  TaskControlBlock *RunTask();
};

/*
// DeviceTCB 
*/

class DeviceTCB : public TaskControlBlock {
public:
  DeviceTCB(TaskControlBlock *l, Identity id, int prio,
	    Packet *initialWork, TaskState *initialState,
	    void *privateData) :
    TaskControlBlock(l, id, prio, initialWork, initialState, privateData) {}

  TaskControlBlock *ActionFunc(Packet *p, void *handle);
};


/*
// HandlerTCB
*/

class HandlerTCB : public TaskControlBlock {
public:
  HandlerTCB(TaskControlBlock *l, Identity id, int prio,
	     Packet *initialWork, TaskState *initialState,
	     void *privateData) :
    TaskControlBlock(l, id, prio, initialWork, initialState, privateData) {}

  TaskControlBlock *ActionFunc(Packet *p, void *handle);
};


/*
// IdlerTCB
*/

class IdlerTCB : public TaskControlBlock {
public:
  IdlerTCB(TaskControlBlock *l, Identity id, int prio,
	   Packet *initialWork, TaskState *initialState,
	   void *privateData) :
    TaskControlBlock(l, id, prio, initialWork, initialState, privateData) {}

  TaskControlBlock *ActionFunc(Packet *p, void *handle);
};


/*
// WorkerTCB
*/

class WorkerTCB : public TaskControlBlock {
public:
  WorkerTCB(TaskControlBlock *l, Identity id, int prio,
	    Packet *initialWork, TaskState *initialState,
	    void *privateData) :
    TaskControlBlock(l, id, prio, initialWork, initialState, privateData) {}

  TaskControlBlock *ActionFunc(Packet *p, void *handle);
};


/*
// RBench class definition
*/

class RBench {
public:
  TaskControlBlock *taskList;
  TaskControlBlock *currentTask;
  Identity          currentTaskIdent;
  TaskControlBlock *taskTable[NTASKS];
  int layout;
  int holdCount;
  int queuePacketCount;

  /* creation */
  void CreateDevice (Identity id, int prio,  Packet *work, TaskState *state);
  void CreateHandler(Identity id, int prio,  Packet *work, TaskState *state);
  void CreateIdler  (Identity id, int prio,  Packet *work, TaskState *state);
  void CreateWorker (Identity id, int prio,  Packet *work, TaskState *state);
  void EnterTask    (Identity id, TaskControlBlock *t);

  /* scheduling */
  void Schedule();
  
  /* initializing */
  void InitScheduler();
  void InitTrace();
public:
  /* task management */
  TaskControlBlock *FindTask(Identity id);
  TaskControlBlock *HoldSelf();
  TaskControlBlock *QueuePacket(Packet *p);
  TaskControlBlock *Release(Identity id);
  TaskControlBlock *Wait();

  /* tracing */
  bool tracing;
  void Trace(Identity id);
  
  void Start(bool askTrace);
};



class RBench bm;		// benchmark currently executing


/*
// TaskControlBlock
*/

TaskControlBlock::TaskControlBlock(TaskControlBlock *l,
				   Identity id,
				   int prio,
				   Packet *initialWork,
				   TaskState *initialState,
				   void *privateData)
{
  link = l;
  ident = id;
  priority = prio;
  input = initialWork;
  packetPending = initialState->packetPending;
  taskWaiting = initialState->taskWaiting;
  taskHolding = initialState->taskHolding;
  handle = privateData;
}


TaskControlBlock *TaskControlBlock::AddPacket(Packet *p,
                                              TaskControlBlock *oldTask)
{
  if (input == NoWork) {
    input = p;
    packetPending = true;
    if (priority > oldTask->priority)
      return this;
  } else {
    p->AddToList(input);
  }
  return oldTask;
}


TaskControlBlock *TaskControlBlock::RunTask()
{
  Packet *msg;

  if (IsWaitingWithPacket()) {
    msg = input;
    input = input->link;
    if (input == NoWork)
      Running();
    else
      PacketPending();
  } else {
    msg = NoWork;
  }
  return ActionFunc(msg, handle);
}


/*
// action functions
*/

TaskControlBlock *TaskControlBlock::ActionFunc(Packet *, void *)
{
  printf("***error: virtual TaskControlBlock.ActionFunc called!\n");
  return NoTask;
}


TaskControlBlock *DeviceTCB::ActionFunc(Packet *p, void *h)
{
  DeviceTaskRec *data = (DeviceTaskRec *)h;

  if (p == NoWork) {
    p = data->pending;
    if (p == NoWork)
      return bm.Wait();
    else {
      data->pending = NoWork;
      return bm.QueuePacket(p);
    }
  } else {
    data->pending = p;
    if (bm.tracing)
      bm.Trace((Identity)p->datum);
    return bm.HoldSelf();
  }
}


TaskControlBlock *HandlerTCB::ActionFunc(Packet *p, void *h)
{
  HandlerTaskRec *data = (HandlerTaskRec *)h;
  Packet *work, *devicePacket;
  int count;

  if (p != NoWork) {
    if (p->kind == WorkPacket)
      data->WorkInAdd(p);
    else
      data->DeviceInAdd(p);
  }
  work = data->workIn;
  if (work == NoWork)
    return bm.Wait();

  count = work->datum;
  if (count > 4) {
    data->workIn = work->link;
    return bm.QueuePacket(work);
  } else {
    devicePacket = data->deviceIn;
    if (devicePacket == NoWork)
      return bm.Wait();
    else {
      data->deviceIn = devicePacket->link;
      devicePacket->datum = work->data[count-1];
      work->datum = count + 1;
      return bm.QueuePacket(devicePacket);
    }
  }
}


TaskControlBlock *IdlerTCB::ActionFunc(Packet *, void *h)
{
  IdleTaskRec *data = (IdleTaskRec *)h;

  if (--data->count == 0)
    return bm.HoldSelf();
  else if ((data->control & 1) == 0) {
    data->control /= 2;
    return bm.Release(DeviceA);
  } else {
    data->control = (data->control / 2) ^ 53256;
    return bm.Release(DeviceB);
  }
}


TaskControlBlock *WorkerTCB::ActionFunc(Packet *p, void *h)
{
  WorkerTaskRec *data = (WorkerTaskRec *)h;

  if (p == NoWork)
    return bm.Wait();
  
  Identity dest = (data->destination == HandlerA) ? HandlerB : HandlerA;
  data->destination = dest;
  p->ident = dest;
  p->datum = 1;
  for(int i = 0; i < 4; i++) {
    if (++data->count > 26) data->count = 1;
    p->data[i] = 'A' + data->count;
  }
  return bm.QueuePacket(p);
}




/*
// creation
*/

void RBench::CreateDevice (Identity id, int prio,  Packet *work,
                           TaskState *state)
{   DeviceTaskRec *data;
    TaskControlBlock *t;

    data = new DeviceTaskRec;
    t = new DeviceTCB(taskList, id, prio, work, state, data);
    EnterTask(id, t);
}


void RBench::CreateHandler(Identity id, int prio,  Packet *work,
                           TaskState *state)
{   HandlerTaskRec *data;
    TaskControlBlock *t;

    data = new HandlerTaskRec;
    t = new HandlerTCB(taskList, id, prio, work, state, data);
    EnterTask(id, t);
}


void RBench::CreateIdler  (Identity id, int prio,  Packet *work,
                           TaskState *state)
{   IdleTaskRec *data;
    TaskControlBlock *t;

    data = new IdleTaskRec;
    t = new IdlerTCB(taskList, id, prio, work, state, data);
    EnterTask(id, t);
}


void RBench::CreateWorker (Identity id, int prio,  Packet *work,
                           TaskState *state)
{   WorkerTaskRec *data;
    TaskControlBlock *t;

    data = new WorkerTaskRec;
    t = new WorkerTCB(taskList, id, prio, work, state, data);
    EnterTask(id, t);
}


void RBench::EnterTask(Identity id, TaskControlBlock *t)
{
    taskList = t;
    taskTable[id] = t;
}


/*
//private
*/

TaskControlBlock *RBench::FindTask(Identity id)
{   TaskControlBlock *t;

    t = taskTable[id];
    if (t == NULL) printf("***error: FindTask failed! ");
    return t;
}


TaskControlBlock *RBench::HoldSelf()
{
    holdCount++;
    currentTask->taskHolding = true;
    return currentTask->link;
}


TaskControlBlock *RBench::QueuePacket(Packet *p)
{   TaskControlBlock *t;

    t = FindTask(p->ident);
    queuePacketCount++;
    p->link = NoWork;
    p->ident = currentTaskIdent;
    return t->AddPacket(p, currentTask);
}


TaskControlBlock *RBench::Release(Identity id)
{   TaskControlBlock *t;

    t = FindTask(id);
    t->taskHolding = false;
    return t->priority > currentTask->priority ? t : currentTask;
}


void RBench::Trace(Identity id)
{
    if (--layout != 0) { printf("\n"); layout = 50; } 
    printf("%d", id + 1);
}


TaskControlBlock *RBench::Wait()
{
    currentTask->taskWaiting = true;
    return currentTask;
}


/*
//scheduling
*/

void RBench::Schedule()
{
    currentTask = taskList;
    while (currentTask != NoTask) {
         if (currentTask->IsTaskHoldingOrWaiting()) 
              currentTask = currentTask->link;
         else {
              currentTaskIdent = currentTask->ident;
              if (tracing) Trace(currentTaskIdent);
              currentTask = currentTask->RunTask();
         }
    }
}

/*
//initializing
*/

void RBench::InitScheduler()
{
    queuePacketCount = 0;
    holdCount = 0;
    for (int i = 0; i < NTASKS; i++)
      taskTable[i] = NoTask;
    taskList = NoTask;
}


void RBench::InitTrace()
{   char c;

    printf("Trace (y/n)? ");
    c = getchar();
    tracing = bool(_toupper(c) == 'Y');
}


void RBench::Start(bool trace) {
    TaskState *t;
    Packet *workQ;

    if (trace) InitTrace(); else tracing = false;
    InitScheduler();
    t = new TaskState; t->Running();				// Idler
    CreateIdler(Idler, 0, NoWork, t);

    workQ = new Packet(NoWork, Worker, WorkPacket);		// Worker
    workQ = new Packet(workQ , Worker, WorkPacket);
    t = new TaskState; t->WaitingWithPacket();
    CreateWorker(Worker, 1000, workQ, t);

    workQ = new Packet(NoWork, DeviceA, DevicePacket);		// HandlerA
    workQ = new Packet(workQ , DeviceA, DevicePacket);
    workQ = new Packet(workQ , DeviceA, DevicePacket);
    t = new TaskState; t->WaitingWithPacket();
    CreateHandler(HandlerA, 2000, workQ, t);

    workQ = new Packet(NoWork, DeviceB, DevicePacket);		// HandlerB
    workQ = new Packet(workQ , DeviceB, DevicePacket);
    workQ = new Packet(workQ , DeviceB, DevicePacket);
    t = new TaskState; t->WaitingWithPacket();
    CreateHandler(HandlerB, 3000, workQ, t);

    t = new TaskState; t->Waiting();				// DeviceA
    CreateDevice(DeviceA, 4000, NoWork, t);
    t = new TaskState; t->Waiting();				// DeviceB
    CreateDevice(DeviceB, 5000, NoWork, t);

    Schedule();

    if (queuePacketCount == 23246 && holdCount == 9297)
      ; // correct
    else {
      printf("error: richards results are incorrect\n");
      exit(1);
    }
}

#define ITER  100		/* # of iterations in main loop  */

int main()
{
  printf("Richards #3 starting\n");
  clock_t start_time= clock();
  for (int i = 0; i < ITER; i++)
    bm.Start(false);
  clock_t end_time = clock();
  double total_s= (end_time - start_time) / double(CLOCKS_PER_SEC);
  double avg_ms= total_s / ITER * 1000;
  printf("elapsed time for %d iterations: %.3g seconds\n", ITER, total_s);
  printf("average time per iteration: %.3g ms\n", avg_ms);
}


