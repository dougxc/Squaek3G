//  Based on original version written in BCPL by Dr Martin Richards
//  in 1981 at Cambridge University Computer Laboratory, England
//  and a C++ version derived from a Smalltalk version written by
//  L Peter Deutsch.
//  Java version:  Copyright (C) 1995 Sun Microsystems, Inc.
//  Translation from C++, Mario Wolczko
//  Outer loop added by Alex Jacoby
//  Modified to run on Squawk platform by Erica Glynn (03/02/2005).

package com.sun.labs.kanban.richards_deutsch_acc_interface;

//----- Packet -------------------------------------------------------

interface PacketIF {
  PacketIF Link();
  int 	Ident();
  int    Kind();
  int   Datum();
  void SetLink(PacketIF l);
  void SetIdent(int i);
  void SetKind(int k);
  void SetDatum(int d);

  int     Data(int i);
  void SetData(int i, int d);

  PacketIF append_to(PacketIF list);

}

class Packet implements PacketIF {
  static final int BUFSIZE = 4;

  private PacketIF link;
  private int id;
  private int kind;
  private int datum;
  private int[] data = new int[BUFSIZE];

  public Packet(PacketIF l, int i, int k) {
    SetLink(l);
    SetIdent(i);
    SetKind(k);
    SetDatum(0);
    for(int j = 0; j < BUFSIZE; j++)
      SetData(j, 0);
  }

  public PacketIF Link() { return link; }
  public int 	Ident() { return id; }
  public int    Kind() { return kind; }
  public int   Datum() { return datum; }

  public void SetLink(PacketIF l)     { link = l; }
  public void SetIdent(int i)       { id = i; }
  public void SetKind(int k)        { kind = k; }
  public void SetDatum(int d)       { datum = d; }

  public int     Data(int i)        { return data[i]; }
  public void SetData(int i, int d) { data[i] = d; }

  public PacketIF append_to(PacketIF list) {
    SetLink(null);
    if (list == null) 
      return this;
    else {
      PacketIF p = list;
      PacketIF next = p.Link();
      while (next != null) {
        p = next;
	next = p.Link();
      }
      p.SetLink(this);
      return list;
    }
  }

}

//----- Task Records------------------------------

interface TaskRec {
  
}

class DeviceTaskRec implements TaskRec {
  private PacketIF pending;

  DeviceTaskRec()           { pending = null; }
  PacketIF Pending()          { return pending; }
  void SetPending(PacketIF p) { pending = p; }
}


class IdleTaskRec implements TaskRec {
  private int control, count;

  IdleTaskRec() { control = 1; count = 10000; }
  int Control() { return control; }
  int Count()   { return count; }
  void SetControl(int n) { control = n; }
  void SetCount(int n)   { count = n; }
}


class HandlerTaskRec implements TaskRec {
  private PacketIF workIn, deviceIn;

  HandlerTaskRec() { workIn = deviceIn = null; }

  PacketIF   WorkIn() { return workIn; }
  PacketIF DeviceIn() { return deviceIn; }

  void SetDeviceIn(PacketIF p) { deviceIn = p; }
  void SetWorkIn  (PacketIF p) { workIn = p; }

  PacketIF   WorkInAdd(PacketIF p) { return workIn = p.append_to(workIn); }
  PacketIF DeviceInAdd(PacketIF p) { return deviceIn = p.append_to(deviceIn); }
}



class WorkerTaskRec implements TaskRec {
  private int destination;
  private int count;

  WorkerTaskRec() { destination = Richards.I_HANDLERA; count = 0; }

  int       Count() { return count; }
  int Destination() { return destination; }

  void SetCount      (int n) { count = n; }
  void SetDestination(int d) { destination = d; }
}


//----- Task ---------------------------------------------------------

interface TaskStateIF {
  TaskStateIF PacketPending();
  TaskStateIF Waiting();
  TaskStateIF Running();
  TaskStateIF WaitingWithPacket();
  boolean IsPacketPending();
  boolean IsTaskWaiting();
  boolean IsTaskHolding();

  void SetTaskHolding(boolean state);
  void SetTaskWaiting(boolean state);
  void SetPacketPending(boolean state);
  boolean IsTaskHoldingOrWaiting();
  boolean IsWaitingWithPacket();
}

class TaskState implements TaskStateIF {

  protected boolean packetPending, taskWaiting, taskHolding;

  TaskState() {
    packetPending = true;
    taskWaiting = false;
    taskHolding = false;
  }

  public TaskStateIF PacketPending() {
    packetPending = true;
    taskWaiting = taskHolding = false;
    return this;
  }
  public TaskStateIF Waiting() {
    packetPending = taskHolding = false;
    taskWaiting = true;
    return this;
  }
  public TaskStateIF Running() {
    packetPending = taskWaiting = taskHolding = false;
    return this;
  }
  public TaskStateIF WaitingWithPacket() {
    packetPending = taskWaiting = true; taskHolding = false;
    return this;
  }

  /* accessing */
  public boolean IsPacketPending() { return packetPending; }
  public boolean IsTaskWaiting()   { return taskWaiting; }
  public boolean IsTaskHolding()   { return taskHolding; }

  public void SetTaskHolding(boolean state) { taskHolding = state; }
  public void SetTaskWaiting(boolean state) { taskWaiting = state; }
  public void SetPacketPending(boolean state) { packetPending = state; }

  /* testing */ 
  public boolean IsTaskHoldingOrWaiting() {
    return IsTaskHolding() || !IsPacketPending() && IsTaskWaiting();
  }
  public boolean IsWaitingWithPacket() {
    return IsPacketPending() && IsTaskWaiting() && !IsTaskHolding();
  }
}

abstract class Task extends TaskState {

  static int layout = 0;

  protected Task link;
  protected int id;
  protected int pri;
  protected PacketIF wkq;
  protected TaskRec handle;

  Task      Link() { return link; }
  int      Ident() { return id; }
  int   Priority() { return pri; }
  PacketIF   Input() { return wkq; }
  TaskRec Handle() { return handle; }

  void     SetLink(Task x)    { link = x; }
  void    SetIdent(int x)     { id = x; }
  void SetPriority(int x)     { pri = x; }
  void    SetInput(PacketIF x)  { wkq = x; }
  void   SetHandle(TaskRec x) { handle = x; }

  static final int TaskTabSize = 10;
  private static Task[] taskTab = new Task[TaskTabSize];
  static Task    TaskTab(int i)         { return taskTab[i]; }
  static void SetTaskTab(int i, Task t) { taskTab[i] = t; }

  static Task taskList;

  static final boolean tracing = false;
  private static int holdCount = 0;
  private static int qpktCount = 0;

  static int  get_holdCount()      { return holdCount; }
  static void set_holdCount(int i) { holdCount = i; }

  static int  get_queuePacketCount()      { return qpktCount; }
  static void set_queuePacketCount(int i) { qpktCount = i; }

  Task(int i, int p, PacketIF w, TaskStateIF initialState, TaskRec r) {
    link = taskList;
    SetIdent(i);
    SetPriority(p);
    SetInput(w);
    SetPacketPending(initialState.IsPacketPending());
    SetTaskWaiting(initialState.IsTaskWaiting());
    SetTaskHolding(initialState.IsTaskHolding());
    SetHandle(r);
    taskList = this;
    SetTaskTab(i, this);
  }

  abstract Task fn(PacketIF pkt, TaskRec r);

  private Task AddPacket(PacketIF p, Task old) {
    if (Input() == null) {
      SetInput(p);
      SetPacketPending(true);
      if (Priority() > old.Priority())
        return this;
    } else {
      p.append_to(Input());
    }
    return old;
  }

  Task RunTask() {
    PacketIF msg;

    if (IsWaitingWithPacket()) {
      msg = Input();
      SetInput(msg.Link());
      if (Input() == null)
	Running();
      else
	PacketPending();
    } else {
      msg = null;
    }
    return fn(msg, Handle());
  }

  protected Task waitTask() {
    SetTaskWaiting(true);
    return this;
  }

  protected Task hold() {
    set_holdCount(get_holdCount() + 1);
    SetTaskHolding(true);
    return Link();
  }

  protected Task release(int i) {
    Task t = findtcb(i);
    t.SetTaskHolding(false);
    return t.Priority() > Priority() ? t : this;
  }

  protected Task qpkt(PacketIF pkt) {
    Task t = findtcb(pkt.Ident());
    set_queuePacketCount(get_queuePacketCount() + 1);
    pkt.SetLink(null);
    pkt.SetIdent(Ident());
    return t.AddPacket(pkt, this);
  }

  static Task findtcb(int id) {
    Task t = Task.TaskTab(id);
    if (t == null) 
      System.out.println("\nBad task id " + id);
    return t;
  }

  static void trace(char a) {
    if (--layout <= 0) {
      System.out.println();
      layout = 50;
    }
    System.out.print(a);
  }

}

//----- DeviceTask ---------------------------------------------------

class DeviceTask extends Task
{
  DeviceTask(int i, int p, PacketIF w, TaskStateIF s, TaskRec r) {
    super(i, p, w, s, r);
  }

  Task fn(PacketIF pkt, TaskRec r) {
    DeviceTaskRec d = (DeviceTaskRec)r;
    if (pkt == null) {
      pkt = d.Pending();
      if (pkt == null) 
        return waitTask();
      else {
	d.SetPending(null);
	return qpkt(pkt);
      }
    } else {
      d.SetPending(pkt);
      if (tracing) trace((char)pkt.Datum());
      return hold();
    }
  }
}


//----- HandlerTask --------------------------------------------------

class HandlerTask extends Task
{
  HandlerTask(int i, int p, PacketIF w, TaskStateIF s, TaskRec r) {
    super(i, p, w, s, r);
  }

  Task fn(PacketIF pkt, TaskRec r) {
    HandlerTaskRec h = (HandlerTaskRec)r;
    if (pkt != null) {
      if (pkt.Kind() == Richards.K_WORK)
	h.WorkInAdd(pkt);
      else
        h.DeviceInAdd(pkt);
    }
    PacketIF work = h.WorkIn();
    if (work == null)
      return waitTask();

    int count = work.Datum();

    if (count >= Packet.BUFSIZE) {
      h.SetWorkIn(work.Link());
      return qpkt(work);
    }

    PacketIF dev = h.DeviceIn();
    if (dev == null)
      return waitTask();

    h.SetDeviceIn(dev.Link());
    dev.SetDatum(work.Data(count));
    work.SetDatum(count + 1);
    return qpkt(dev);
  }
}


//----- IdleTask -----------------------------------------------------

class IdleTask extends Task 
{
  IdleTask(int i, int a1, int a2, TaskStateIF s, TaskRec r) {
    super(i, 0, null, s, r);
  }

  Task fn(PacketIF pkt, TaskRec r) {
    IdleTaskRec i = (IdleTaskRec)r;

    i.SetCount(i.Count() - 1);
    if (i.Count() == 0) {
      return hold();
    } else if ((i.Control() & 1) == 0) {
      i.SetControl(i.Control() / 2);
      return release(Richards.I_DEVA);
    } else {
      i.SetControl((i.Control() / 2) ^ 0XD008);
      return release(Richards.I_DEVB);
    }
  }

}


//----- WorkTask -----------------------------------------------------

class WorkTask extends Task 
{
  WorkTask(int i, int p, PacketIF w, TaskStateIF s, TaskRec r) {
    super(i, p, w, s, r);
  }

  Task fn(PacketIF pkt, TaskRec r) {
    WorkerTaskRec w = (WorkerTaskRec)r;

    if (pkt == null)
      return waitTask();

    int dest = (w.Destination() == Richards.I_HANDLERA
		? Richards.I_HANDLERB
		: Richards.I_HANDLERA);
    w.SetDestination(dest);
    pkt.SetIdent(dest);
    pkt.SetDatum(0);
    for (int i = 0; i < Packet.BUFSIZE; i++) { 
      w.SetCount(w.Count() + 1);
      if (w.Count() > 26) w.SetCount(1);
      pkt.SetData(i, 'A' + w.Count() - 1);
    }
    return qpkt(pkt);
  }
}


//----- Richards -----------------------------------------------------


public class Richards {
  private long total_ms;
  public long getRunTime() { return total_ms; }

  public static void main(String[] args) {
    (new Richards()).inst_main(args);
  }

  static int iterations = 10;

  public void inst_main(String[] args) { 
    System.out.println("Richards benchmark (deutsch_acc_interface) starting...");
    long startTime = System.currentTimeMillis();
    if (!run())
      return;
    long endTime = System.currentTimeMillis();
    System.out.println("finished.");
    total_ms= endTime - startTime;
    System.out.println("Total time for " + iterations + " iterations: "
		       + (total_ms) + " ms");
    //+ (total_ms/1000.0) + " secs");
    System.out.println("Average time per iteration: "
		       + (total_ms / iterations) + " ms");
  }

  static void schedule() {
    Task t = Task.taskList;
    while (t != null) {
      PacketIF pkt = null;

      if (Task.tracing) 
	System.out.println("tcb=" + t.Ident());

      if (t.IsTaskHoldingOrWaiting()) 
        t = t.Link();
      else {
        if (Task.tracing) Task.trace((char)('0' + t.Ident()));
        t = t.RunTask();
      }
    }
  }

  public boolean run() {
    for (int i= 0; i < iterations; i++){
      Task.set_holdCount(0);
      Task.set_queuePacketCount(0);  // Added to allow repeated execution
				     // of the test.    Ole Agesen, 3/95.

      new IdleTask(I_IDLE, 1, 10000, (new TaskState()).Running(),
		   new IdleTaskRec());

      PacketIF wkq = new Packet(null, 0, K_WORK);
      wkq = new Packet(wkq, 0, K_WORK);
      new WorkTask(I_WORK, 1000, wkq,
		   (new TaskState()).WaitingWithPacket(),
		   new WorkerTaskRec());

      wkq = new Packet(null, I_DEVA, K_DEV);
      wkq = new Packet(wkq, I_DEVA, K_DEV);
      wkq = new Packet(wkq, I_DEVA, K_DEV);
      new HandlerTask(I_HANDLERA, 2000, wkq,
		      (new TaskState()).WaitingWithPacket(),
		      new HandlerTaskRec());

      wkq = new Packet(null, I_DEVB, K_DEV);
      wkq = new Packet(wkq, I_DEVB, K_DEV);
      wkq = new Packet(wkq, I_DEVB, K_DEV);
      new HandlerTask(I_HANDLERB, 3000, wkq,
		      (new TaskState()).WaitingWithPacket(),
		      new HandlerTaskRec());

      wkq = null;
      new DeviceTask(I_DEVA, 4000, wkq, (new TaskState()).Waiting(),
		     new DeviceTaskRec());
      new DeviceTask(I_DEVB, 5000, wkq, (new TaskState()).Waiting(),
		     new DeviceTaskRec());

      schedule();

      if (Task.get_queuePacketCount() == 23246 && Task.get_holdCount() == 9297) 
        ; // correct
      else {
        System.out.println("Incorrect results!");
        return false;
      }
    }
    return true;
  }

  // Task IDs
  static final int
    I_IDLE = 1,
    I_WORK = 2,
    I_HANDLERA = 3,
    I_HANDLERB = 4,
    I_DEVA = 5,
    I_DEVB = 6;

  // Packet types
  static final int
    K_DEV = 1000,
    K_WORK = 1001;
}

