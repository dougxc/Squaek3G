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

private:
  Packet *link;		// next packet in queue
  Identity ident;	// Idle, Worker, DeviceA, ...
  PacketKind kind;	// DevicePacket or WorkPacket
  int datum;
  char data[4];

public:
  Packet(Packet *l, Identity id, PacketKind k);

  virtual Identity  Ident()  { return ident; }
  virtual PacketKind Kind()  { return kind; }
  virtual Packet    *Link()  { return link; }
  virtual int       Datum()  { return datum; }

  virtual void SetLink (Packet *l)    { link = l; }
  virtual void SetIdent(Identity i)   { ident = i; }
  virtual void SetKind (PacketKind k) { kind = k; }
  virtual void SetDatum(int n)        { datum = n; }

  virtual char    Data(int i)          { return data[i]; }
  virtual void SetData(int i, char d)  { data[i] = d; }

  virtual Packet *AddToList(Packet *list);
};


Packet::Packet(Packet *l, Identity id, PacketKind k)
{
  SetLink(l);
  SetIdent(id);
  SetKind(k);
  SetDatum(1);
  for(int i = 0; i < 4; i++)
    SetData(i, 0);
}

/*
// AddToList - list utility (append this at end of list, return head)
*/
Packet *Packet::AddToList(Packet *list)
{
  SetLink(NoWork);
  if (list == NoWork)
    list = this;
  else {
    Packet *p, *next;
    p = list;
    while (next = p->Link(), next != NoWork)
      p = next;
    p->SetLink(this);
  }    
  return list;
}



/*
// DeviceTaskRec
*/

class DeviceTaskRec {
private:
  Packet *pending;
public:
  DeviceTaskRec()            { pending = NoWork; }
  virtual Packet *Pending()          { return pending; }
  virtual void SetPending(Packet *p) { pending = p; }
};



/*
// IdleTaskRec 
*/

class IdleTaskRec {
private:
  int control, count;
public:
  IdleTaskRec() { control = 1; count = 10000; }
  virtual int Control() { return control; }
  virtual int Count()   { return count; }
  virtual void SetControl(int n) { control = n; }
  virtual void SetCount(int n)   { count = n; }
};


/*
// HandlerTaskRec
*/

class HandlerTaskRec {
private:
  Packet *workIn, *deviceIn;
public:
  HandlerTaskRec() { workIn = deviceIn = NoWork; }

  virtual Packet *  WorkIn() { return workIn; }
  virtual Packet *DeviceIn() { return deviceIn; }

  virtual void SetDeviceIn(Packet *p) { deviceIn = p; }
  virtual void SetWorkIn  (Packet *p) { workIn = p; }

  virtual Packet *  WorkInAdd(Packet *p) {
    return workIn = p->AddToList(workIn); }
  virtual Packet *DeviceInAdd(Packet *p) {
    return deviceIn = p->AddToList(deviceIn); }
};


/*
// WorkerTaskRec
*/

class WorkerTaskRec {
private:
  Identity destination;
  int count;
public:
  WorkerTaskRec() { destination = HandlerA; count = 0; }

  virtual int            Count() { return count; }
  virtual Identity Destination() { return destination; }

  virtual void SetCount      (int n)      { count = n; }
  virtual void SetDestination(Identity d) { destination = d; }
};



/*
// TaskState
*/

class TaskState {
protected:
  bool packetPending, taskWaiting, taskHolding;
	
public:
  /* initializing */
  TaskState() {
    packetPending = true;
    taskWaiting = taskHolding = false;
  }
  virtual void PacketPending() {
    packetPending = true;
    taskWaiting = taskHolding = false;
  }
  virtual void Waiting() {
    packetPending = taskHolding = false;
    taskWaiting = true;
  }
  virtual void Running() {
    packetPending = taskWaiting = taskHolding = false;
  }
  virtual void WaitingWithPacket() {
    packetPending = taskWaiting = true; taskHolding = false;
  }

  /* accessing */
  virtual bool IsPacketPending() { return packetPending; }
  virtual bool IsTaskWaiting()   { return taskWaiting; }
  virtual bool IsTaskHolding()   { return taskHolding; }

  virtual void SetPacketPending(bool b)   { packetPending = b; }
  virtual void SetTaskHolding(bool state) { taskHolding = state; }
  virtual void SetTaskWaiting(bool state) { taskWaiting = state; }

  /* testing */ 
  virtual bool IsTaskHoldingOrWaiting() {
    return bool(taskHolding || !packetPending && taskWaiting);
  }
  virtual bool IsWaitingWithPacket() {
    return bool(packetPending && taskWaiting && !taskHolding);
  }
};

/*
// TaskControlBlock
*/

class TaskControlBlock : public TaskState {

protected:
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

  /* accessing */
  virtual Identity         Ident()                    { return ident; }
  virtual void          SetIdent(Identity x)          { ident = x; }
  virtual int           Priority()                    { return priority; }
  virtual void       SetPriority(int x)               { priority = x; }
  virtual TaskControlBlock *Link()                    { return link; }
  virtual void           SetLink(TaskControlBlock *x) { link = x; }
  virtual Packet*          Input()                    { return input; }
  virtual void          SetInput(Packet* x)           { input = x; }
  virtual void*           Handle()                    { return handle; }
  virtual void         SetHandle(void* x)             { handle = x; }

  /* scheduling */
  virtual TaskControlBlock *AddPacket(Packet *p, TaskControlBlock *old);
  virtual TaskControlBlock *ActionFunc(Packet *p, void *handle);
  virtual TaskControlBlock *RunTask();
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

  virtual TaskControlBlock *ActionFunc(Packet *p, void *handle);
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

  virtual TaskControlBlock *ActionFunc(Packet *p, void *handle);
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

  virtual TaskControlBlock *ActionFunc(Packet *p, void *handle);
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

  virtual TaskControlBlock *ActionFunc(Packet *p, void *handle);
};




/*
// RBench class definition
*/

class RBench {
private:
  TaskControlBlock *taskList;
  TaskControlBlock *currentTask;
  Identity          currentTaskIdent;
  TaskControlBlock *taskTable[NTASKS];
  int layout;
  int holdCount;
  int queuePacketCount;

  /* creation */
  virtual void CreateDevice (Identity id, int prio,  Packet *work, TaskState *state);
  virtual void CreateHandler(Identity id, int prio,  Packet *work, TaskState *state);
  virtual void CreateIdler  (Identity id, int prio,  Packet *work, TaskState *state);
  virtual void CreateWorker (Identity id, int prio,  Packet *work, TaskState *state);
  virtual void EnterTask    (Identity id, TaskControlBlock *t);

  /* scheduling */
  virtual void Schedule();
  
  /* initializing */
  virtual void InitScheduler();
  virtual void InitTrace();

  /* accessing */
  virtual TaskControlBlock *get_taskList()        { return taskList; }
  virtual void  set_taskList(TaskControlBlock* x) { taskList = x; }

  virtual TaskControlBlock *get_currentTask()       { return currentTask; }
  virtual void set_currentTask(TaskControlBlock* x) { currentTask = x; }
  
  virtual Identity get_currentTaskIdent()       { return currentTaskIdent; }
  virtual void set_currentTaskIdent(Identity x) { currentTaskIdent = x; }
  
  virtual TaskControlBlock*& TaskTable(int i) { return taskTable[i]; }
  
  virtual int  get_layout()      { return layout; }
  virtual void set_layout(int i) { layout = i; }
  
  virtual int  get_holdCount()      { return holdCount; }
  virtual void set_holdCount(int i) { holdCount = i; }
  
  virtual int  get_queuePacketCount()      { return queuePacketCount; }
  virtual void set_queuePacketCount(int i) { queuePacketCount = i; }

public:
  /* task management */
  virtual TaskControlBlock *FindTask(Identity id);
  virtual TaskControlBlock *HoldSelf();
  virtual TaskControlBlock *QueuePacket(Packet *p);
  virtual TaskControlBlock *Release(Identity id);
  virtual TaskControlBlock *Wait();

  /* tracing */
  bool tracing;
  virtual void Trace(Identity id);
  
  virtual void Start(bool askTrace);
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
  SetLink(l);
  SetIdent(id);
  SetPriority(prio);
  SetInput(initialWork);
  SetPacketPending(initialState->IsPacketPending());
  SetTaskWaiting(initialState->IsTaskWaiting());
  SetTaskHolding(initialState->IsTaskHolding());
  SetHandle(privateData);
}


TaskControlBlock *TaskControlBlock::AddPacket(Packet *p,
                                              TaskControlBlock *oldTask)
{
  if (Input() == NoWork) {
    SetInput(p);
    SetPacketPending(true);
    if (Priority() > oldTask->Priority())
      return this;
  } else {
    p->AddToList(Input());
  }
  return oldTask;
}


TaskControlBlock *TaskControlBlock::RunTask()
{
  Packet *msg;

  if (IsWaitingWithPacket()) {
    msg = Input();
    SetInput(msg->Link());
    if (Input() == NoWork)
      Running();
    else
      PacketPending();
  } else {
    msg = NoWork;
  }
  return ActionFunc(msg, Handle());
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
    p = data->Pending();
    if (p == NoWork)
      return bm.Wait();
    else {
      data->SetPending(NoWork);
      return bm.QueuePacket(p);
    }
  } else {
    data->SetPending(p);
    if (bm.tracing)
      bm.Trace((Identity)p->Datum());
    return bm.HoldSelf();
  }
}


TaskControlBlock *HandlerTCB::ActionFunc(Packet *p, void *h)
{
  HandlerTaskRec *data = (HandlerTaskRec *)h;
  Packet *work, *devicePacket;
  int count;

  if (p != NoWork) {
    if (p->Kind() == WorkPacket)
      data->WorkInAdd(p);
    else
      data->DeviceInAdd(p);
  }
  work = data->WorkIn();
  if (work == NoWork)
    return bm.Wait();

  count = work->Datum();
  if (count > 4) {
    data->SetWorkIn(work->Link());
    return bm.QueuePacket(work);
  } else {
    devicePacket = data->DeviceIn();
    if (devicePacket == NoWork)
      return bm.Wait();
    else {
      data->SetDeviceIn(devicePacket->Link());
      devicePacket->SetDatum(work->Data(count-1));
      work->SetDatum(count + 1);
      return bm.QueuePacket(devicePacket);
    }
  }
}


TaskControlBlock *IdlerTCB::ActionFunc(Packet *, void *h)
{
  IdleTaskRec *data = (IdleTaskRec *)h;

  data->SetCount(data->Count() - 1);
  if (data->Count() == 0)
    return bm.HoldSelf();
  else if ((data->Control() & 1) == 0) {
    data->SetControl(data->Control() / 2);
    return bm.Release(DeviceA);
  } else {
    data->SetControl((data->Control() / 2) ^ 53256);
    return bm.Release(DeviceB);
  }
}


TaskControlBlock *WorkerTCB::ActionFunc(Packet *p, void *h)
{
  WorkerTaskRec *data = (WorkerTaskRec *)h;

  if (p == NoWork)
    return bm.Wait();
  
  Identity dest = (data->Destination() == HandlerA) ? HandlerB : HandlerA;
  data->SetDestination(dest);
  p->SetIdent(dest);
  p->SetDatum(1);
  for(int i = 0; i < 4; i++) {
    data->SetCount(data->Count() + 1);
    if (data->Count() > 26) data->SetCount(1);
    p->SetData(i, 'A' + data->Count());
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
    t = new DeviceTCB(get_taskList(), id, prio, work, state, data);
    EnterTask(id, t);
}


void RBench::CreateHandler(Identity id, int prio,  Packet *work,
                           TaskState *state)
{   HandlerTaskRec *data;
    TaskControlBlock *t;

    data = new HandlerTaskRec;
    t = new HandlerTCB(get_taskList(), id, prio, work, state, data);
    EnterTask(id, t);
}


void RBench::CreateIdler  (Identity id, int prio,  Packet *work,
                           TaskState *state)
{   IdleTaskRec *data;
    TaskControlBlock *t;

    data = new IdleTaskRec;
    t = new IdlerTCB(get_taskList(), id, prio, work, state, data);
    EnterTask(id, t);
}


void RBench::CreateWorker (Identity id, int prio,  Packet *work,
                           TaskState *state)
{   WorkerTaskRec *data;
    TaskControlBlock *t;

    data = new WorkerTaskRec;
    t = new WorkerTCB(get_taskList(), id, prio, work, state, data);
    EnterTask(id, t);
}


void RBench::EnterTask(Identity id, TaskControlBlock *t)
{
    set_taskList(t);
    TaskTable(id) = t;
}


/*
//private
*/

TaskControlBlock *RBench::FindTask(Identity id)
{   TaskControlBlock *t;

    t = TaskTable(id);
    if (t == NULL) printf("***error: FindTask failed! ");
    return t;
}


TaskControlBlock *RBench::HoldSelf()
{
    set_holdCount(get_holdCount() + 1);
    get_currentTask()->SetTaskHolding(true);
    return get_currentTask()->Link();
}


TaskControlBlock *RBench::QueuePacket(Packet *p)
{   TaskControlBlock *t;

    t = FindTask(p->Ident());
    set_queuePacketCount(get_queuePacketCount() + 1);
    p->SetLink(NoWork);
    p->SetIdent(get_currentTaskIdent());
    return t->AddPacket(p, get_currentTask());
}


TaskControlBlock *RBench::Release(Identity id)
{   TaskControlBlock *t;

    t = FindTask(id);
    t->SetTaskHolding(false);
    return (t->Priority() > get_currentTask()->Priority())
      ? t : get_currentTask();
}


void RBench::Trace(Identity id)
{
    set_layout(get_layout() - 1);
    if(! get_layout()) {printf("\n"); set_layout(50);} 
    printf("%d", id + 1);
}


TaskControlBlock *RBench::Wait()
{
    TaskControlBlock *t = get_currentTask();
    t->SetTaskWaiting(true);
    return t;
}


/*
//scheduling
*/

void RBench::Schedule()
{
    set_currentTask(get_taskList());
    TaskControlBlock *t;
    while (t = get_currentTask(), t != NoTask) {
         if (t->IsTaskHoldingOrWaiting()) 
              set_currentTask(t->Link());
         else {
              set_currentTaskIdent(t->Ident());
              if (tracing) Trace(get_currentTaskIdent());
              set_currentTask(t->RunTask());
         }
    }
}

/*
//initializing
*/

void RBench::InitScheduler()
{
    set_queuePacketCount(0);
    set_holdCount(0);
    for (int i = 0; i < NTASKS; i++) {TaskTable(i) = NoTask;}
    set_taskList(NoTask);
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
  printf("Richards #5 starting\n");
  clock_t start_time= clock();
  for (int i = 0; i < ITER; i++)
    bm.Start(false);
  clock_t end_time = clock();
  double total_s= (end_time - start_time) / double(CLOCKS_PER_SEC);
  double avg_ms= total_s / ITER * 1000;
  printf("elapsed time for %d iterations: %.3g seconds\n", ITER, total_s);
  printf("average time per iteration: %.3g ms\n", avg_ms);
}

