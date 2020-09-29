//  Based on original version written in BCPL by Dr Martin Richards
//  in 1981 at Cambridge University Computer Laboratory, England
//  and a C++ version derived from a Smalltalk version written by
//  L Peter Deutsch.
//  Java version:  Copyright (C) 1995 Sun Microsystems, Inc.
//  Translation from C++, Mario Wolczko
//  Outer loop added by Alex Jacoby
//  Modified to run on Squawk platform by Erica Glynn (03/02/2005).

package com.sun.labs.kanban.richards_deutsch_no_acc;

//----- Packet -------------------------------------------------------

class Packet {
  static final int BUFSIZE = 4;

  Packet link;
  int id;
  int kind;
  int datum;
  int[] data = new int[BUFSIZE];

  Packet(Packet l, int i, int k) {
    link = l;
    id = i;
    kind = k;
    datum = 0;
    for (int j = 0;  j < BUFSIZE;  j++) 
      data[j] = 0;
  }

  Packet append_to(Packet list) {
    link = null;
    if (list == null) 
      return this;
    else {
      Packet p = list;
      Packet next = p.link;
      while (next != null) {
        p = next;
	next = p.link;
      }
      p.link = this;
      return list;
    }
  }

}

//----- Task Records------------------------------

abstract class TaskRec { } // so we have a common type for all task records

class DeviceTaskRec extends TaskRec {
  Packet pending;

  DeviceTaskRec()           { pending = null; }
}


class IdleTaskRec extends TaskRec {
  int control, count;

  IdleTaskRec() { control = 1; count = 10000; }
}


class HandlerTaskRec extends TaskRec {
  Packet workIn, deviceIn;

  HandlerTaskRec() { workIn = deviceIn = null; }

  Packet   WorkInAdd(Packet p) { return workIn = p.append_to(workIn); }
  Packet DeviceInAdd(Packet p) { return deviceIn = p.append_to(deviceIn); }
}



class WorkerTaskRec extends TaskRec {
  int destination;
  int count;

  WorkerTaskRec() { destination = Richards.I_HANDLERA; count = 0; }
}


//----- Task ---------------------------------------------------------

class TaskState {

  boolean packetPending, taskWaiting, taskHolding;

  TaskState() {
    packetPending = true;
    taskWaiting = false;
    taskHolding = false;
  }

  TaskState PacketPending() {
    packetPending = true;
    taskWaiting = taskHolding = false;
    return this;
  }
  TaskState Waiting() {
    packetPending = taskHolding = false;
    taskWaiting = true;
    return this;
  }
  TaskState Running() {
    packetPending = taskWaiting = taskHolding = false;
    return this;
  }
  TaskState WaitingWithPacket() {
    packetPending = taskWaiting = true; taskHolding = false;
    return this;
  }

  /* testing */ 
  boolean IsTaskHoldingOrWaiting() {
    return taskHolding || !packetPending && taskWaiting;
  }
  boolean IsWaitingWithPacket() {
    return packetPending && taskWaiting && !taskHolding;
  }
}

abstract class Task extends TaskState {

  static int layout = 0;

  Task link;
  int id;
  int pri;
  Packet wkq;
  TaskRec handle;

  static final int TaskTabSize = 10;
  static Task[] taskTab = new Task[TaskTabSize];
  static Task taskList;

  static final boolean tracing = false;
  static int holdCount = 0;
  static int qpktCount = 0;

  Task(int i, int p, Packet w, TaskState initialState, TaskRec r) {
    link = taskList;
    id = i;
    pri = p;
    wkq = w;
    packetPending = initialState.packetPending;
    taskWaiting = initialState.taskWaiting;
    taskHolding = initialState.taskHolding;
    handle = r;
    taskList = this;
    taskTab[i] = this;
  }

  abstract Task fn(Packet pkt, TaskRec r);

  private Task AddPacket(Packet p, Task old) {
    if (wkq == null) {
      wkq = p;
      packetPending = true;
      if (pri > old.pri)
        return this;
    } else {
      p.append_to(wkq);
    }
    return old;
  }

  Task RunTask() {
    Packet msg;

    if (IsWaitingWithPacket()) {
      msg = wkq;
      wkq = wkq.link;
      if (wkq == null)
	Running();
      else
	PacketPending();
    } else {
      msg = null;
    }
    return fn(msg, handle);
  }

  protected Task waitTask() {
    taskWaiting = true;
    return this;
  }

  protected Task hold() {
    ++holdCount;
    taskHolding = true;
    return link;
  }

  protected Task release(int i) {
    Task t = findtcb(i);
    t.taskHolding = false;
    return t.pri > pri ? t : this;
  }

  protected Task qpkt(Packet pkt) {
    Task t = findtcb(pkt.id);
    qpktCount++;
    pkt.link = null;
    pkt.id = id;
    return t.AddPacket(pkt, this);
  }

  static Task findtcb(int id) {
    Task t = Task.taskTab[id];
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
  DeviceTask(int i, int p, Packet w, TaskState s, TaskRec r) {
    super(i, p, w, s, r);
  }

  Task fn(Packet pkt, TaskRec r) {
    DeviceTaskRec d = (DeviceTaskRec)r;
    if (pkt == null) {
      pkt = d.pending;
      if (pkt == null) 
        return waitTask();
      else {
	d.pending = null;
	return qpkt(pkt);
      }
    } else {
      d.pending = pkt;
      if (tracing) trace((char)pkt.datum);
      return hold();
    }
  }
}


//----- HandlerTask --------------------------------------------------

class HandlerTask extends Task
{
  HandlerTask(int i, int p, Packet w, TaskState s, TaskRec r) {
    super(i, p, w, s, r);
  }

  Task fn(Packet pkt, TaskRec r) {
    HandlerTaskRec h = (HandlerTaskRec)r;
    if (pkt != null) {
      if (pkt.kind == Richards.K_WORK)
	h.WorkInAdd(pkt);
      else
        h.DeviceInAdd(pkt);
    }
    Packet work = h.workIn;
    if (work == null)
      return waitTask();

    int count = work.datum;

    if (count >= Packet.BUFSIZE) {
      h.workIn = work.link;
      return qpkt(work);
    }

    Packet dev = h.deviceIn;
    if (dev == null)
      return waitTask();

    h.deviceIn = dev.link;
    dev.datum = work.data[count];
    work.datum = count + 1;
    return qpkt(dev);
  }
}


//----- IdleTask -----------------------------------------------------

class IdleTask extends Task 
{
  IdleTask(int i, int a1, int a2, TaskState s, TaskRec r) {
    super(i, 0, null, s, r);
  }

  Task fn(Packet pkt, TaskRec r) {
    IdleTaskRec i = (IdleTaskRec)r;

    if (--i.count == 0) {
      return hold();
    } else if ((i.control & 1) == 0) {
      i.control /= 2;
      return release(Richards.I_DEVA);
    } else {
      i.control = (i.control / 2) ^ 0XD008;
      return release(Richards.I_DEVB);
    }
  }

}


//----- WorkTask -----------------------------------------------------

class WorkTask extends Task 
{
  WorkTask(int i, int p, Packet w, TaskState s, TaskRec r) {
    super(i, p, w, s, r);
  }

  Task fn(Packet pkt, TaskRec r) {
    WorkerTaskRec w = (WorkerTaskRec)r;

    if (pkt == null)
      return waitTask();

    int dest = (w.destination == Richards.I_HANDLERA
		? Richards.I_HANDLERB
		: Richards.I_HANDLERA);
    w.destination = dest;
    pkt.id = dest;
    pkt.datum = 0;
    for (int i = 0; i < Packet.BUFSIZE; i++) { 
      if (++w.count > 26) w.count = 1;
      pkt.data[i] = 'A' + w.count - 1;
    }
    return qpkt(pkt);
  }
}


//----- Richards -----------------------------------------------------


public class Richards
{
  private long total_ms;
  public long getRunTime() { return total_ms; }

  public static void main(String[] args) {
    (new Richards()).inst_main(args);
  }

  static int iterations = 10;

  public void inst_main(String[] args) { 
    System.out.println("Richards benchmark (deutsch_no_acc) starting...");
    long startTime = System.currentTimeMillis();
    if (!run())
      return;
    long endTime = System.currentTimeMillis();
    System.out.println("finished.");
    total_ms= endTime - startTime;
    System.out.println("Total time for " + iterations + " iterations: "
		       + (total_ms) + " ms");
    System.out.println("Average time per iteration: "
		       + (total_ms / iterations) + " ms");
  }

  static void schedule() {
    Task t = Task.taskList;
    while (t != null) {
      Packet pkt = null;

      if (Task.tracing) 
	System.out.println("tcb=" + t.id);

      if (t.IsTaskHoldingOrWaiting()) 
        t = t.link;
      else {
        if (Task.tracing) Task.trace((char)('0' + t.id));
        t = t.RunTask();
      }
    }
  }

  public boolean run() {
    for (int i= 0; i < iterations; i++){
      Task.holdCount= Task.qpktCount= 0;  // Added to allow repeated execution
					  // of the test.    Ole Agesen, 3/95.

      new IdleTask(I_IDLE, 1, 10000, (new TaskState()).Running(),
		   new IdleTaskRec());

      Packet wkq = new Packet(null, 0, K_WORK);
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

      if (Task.qpktCount == 23246 && Task.holdCount == 9297) 
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

