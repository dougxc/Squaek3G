//  Based on original version written in BCPL by Dr Martin Richards
//  in 1981 at Cambridge University Computer Laboratory, England
//  Java version:  Copyright (C) 1995 Sun Microsystems, Inc.
//  by Jonathan Gibbons.
//  switch-less version with bool state, M Wolczko, Sun Labs.
//  Modified to run on Squawk platform by Erica Glynn (03/02/2005).

package com.sun.labs.kanban.richards_gibbons_no_switch;

//----- Packet -------------------------------------------------------

class Packet {
  Packet(Packet l, int i, int k) {
    link = l;
    id = i;
    kind = k;
    a1 = 0;
    for (int j=0;  j<=BUFSIZE;  j++) 
      a2[j] = 0;
  }

  // This does not have *exactly* the same semantics as the Richards
  // original, since that used to be passed the address of the list
  // and it indirected through that to modify the list head directly.
  // For now it must be called as 
  //    listHead = pkt.append_to(listHead);
  // This is OK for the demo; it would not be OK in a real Java Tripos :-)
  Packet append_to(Packet list) {
    link = null;
    if (list == null) 
      return this;
    else {
      Packet p = list;
      while (p.link != null)
        p = p.link;
      p.link = this;
      return list;
    }
  }

  static final int BUFSIZE = 3;

  Packet link;
  int id;
  int kind;
  int a1;
  int[] a2 = new int[4];
}


//----- Task ---------------------------------------------------------

abstract class Task {
  Task(int i, int p, Packet w, boolean pp, boolean tw, boolean th) {
    link = taskList;
    id = i;
    pri = p;
    wkq = w;
    packetPending = pp;
    taskWaiting = tw;
    taskHolding = th;    
    taskList = this;
    taskTab[i] = this;
  }

  abstract Task fn(Packet pkt);

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
    if (t.pri > pri) 
      return t;

    return this;
  }

  protected Task qpkt(Packet pkt) {
    Task t = findtcb(pkt.id);
    if (t == null) 
      return t;

    qpktCount++;

    pkt.link = null;
    pkt.id = id;

    if (t.wkq == null) {
      t.wkq = pkt;
      t.packetPending = true;
      if (t.pri > pri)
        return t;
    } else 
      t.wkq = pkt.append_to(t.wkq);

    return this;

  }

  static Task findtcb(int id) {
    Task t = null;
    if (1 <= id  &&  id <= Task.TaskTabSize) 
      t = Task.taskTab[id];
    if (t == null) 
      System.out.println("\nBad task id " + id);
    return t;
  }

  protected Task runTask() {
    Packet pkt;
    if (packetPending && taskWaiting && !taskHolding) {
        pkt = wkq;
	wkq = pkt.link;
	if (wkq == null)
	    packetPending = false;
	else
	    packetPending = true;
	taskWaiting = taskHolding = false;
    } else
        pkt = null;
    return fn(pkt);
  }

  static void schedule() {
    Task t = taskList;
    //System.out.println("before while: " + System.currentTimeMillis());
    //int numIter = 0;
    //long timebefore = System.currentTimeMillis();
    while (t != null) {
      //numIter++;

//      if (tracing) 
//	System.out.println("tcb=" + t.id);

	if (t.taskHolding || !t.packetPending && t.taskWaiting)
	  t = t.link;
	else {
	  if (tracing) trace((char)(t.id + '0'));
	  t = t.runTask();
	}
    }
//long timeTaken = System.currentTimeMillis() - timebefore;
  //  System.out.println("total for iterative switch: " + timeTaken + " num iterations: " + numIter + " time per iter: " + timeTaken /numIter );
  }

  static void trace(char a) {
    if (--layout <= 0) {
      System.out.println();
      layout = 50;
    }
    System.out.print(a);
  }

  static int layout = 0;

  Task link;
  int id;
  int pri;
  Packet wkq;
  boolean packetPending;
  boolean taskWaiting;
  boolean taskHolding;

  static final int TaskTabSize = 10;
  static Task[] taskTab = new Task[TaskTabSize];
  static Task taskList;

  static final boolean tracing = false;
  static int holdCount = 0;
  static int qpktCount = 0;

}

//----- DeviceTask ---------------------------------------------------

class DeviceTask extends Task
{
  DeviceTask(int i, int p, Packet w) {
    super(i, p, w, w != null, true, false);
    v1 = null; 
  }

  Task fn(Packet pkt) {
    if (pkt == null) {
      if (v1 == null ) 
        return waitTask();
      pkt = v1;
      v1 = null;
      return qpkt(pkt);
    } else {
      v1 = pkt;
      if (tracing) trace((char)pkt.a1);
      return hold();
    }
  }

  private Packet v1;
}


//----- HandlerTask --------------------------------------------------

class HandlerTask extends Task
{
  HandlerTask(int i, int p, Packet w) {
    super(i, p, w, w != null, true, false);
    workpkts = devpkts = null; 
  }

  Task fn(Packet pkt) {
    if (pkt != null) {
      if (pkt.kind == Richards.K_WORK)
	workpkts = pkt.append_to(workpkts);
      else
        devpkts = pkt.append_to(devpkts);
    }

    if (workpkts != null) {
      Packet workpkt = workpkts;
      int count = workpkt.a1;

      if (count > Packet.BUFSIZE) {
        workpkts = workpkts.link;
        return qpkt(workpkt);
      }

      if (devpkts != null) {
        Packet devpkt = devpkts;
        devpkts = devpkts.link;
        devpkt.a1 = workpkt.a2[count];
        workpkt.a1 = count+1;
        return qpkt(devpkt);
      }
    }

    return waitTask();
  }

  private Packet workpkts;
  private Packet devpkts;
}


//----- IdleTask -----------------------------------------------------

class IdleTask extends Task 
{
  IdleTask(int i, int a1, int a2) {
    super(i, 0, null, false, false, false);
    v1 = a1;  
    v2 = a2; 
  }

  Task fn(Packet pkt) {
    --v2;
    if (v2 == 0) {
      return hold();
    } else if ((v1 & 1) == 0) {
      v1 = (v1 >> 1);
      return release(Richards.I_DEVA);
    } else {
      v1 = (v1 >> 1) ^ 0XD008;
      return release(Richards.I_DEVB);
    }
  }

  private int v1;
  private int v2;
}


//----- WorkTask -----------------------------------------------------

class WorkTask extends Task 
{
  WorkTask(int i, int p, Packet w) {
    super(i, p, w, w != null, true, false);
    handler = Richards.I_HANDLERA;  
    n = 0;
  }

  Task fn(Packet pkt) {
    if (pkt == null) {
      return waitTask();
    } else {
      handler = (handler == Richards.I_HANDLERA ? 
			Richards.I_HANDLERB : 
			Richards.I_HANDLERA);
      pkt.id = handler;

      pkt.a1 = 0;
      for (int i=0; i <= Packet.BUFSIZE; i++) { 
	n++;
	if ( n > 26 ) n = 1;
	pkt.a2[i] = 'A' + (n - 1);
      }
      return qpkt(pkt);
    }
  }

  private int handler; 
  private int n;
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
    System.out.println("Richards benchmark (gibbons_no_switch) starting...");
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

  public boolean run() {
    for (int i= 0; i < iterations; i++){

      Task.holdCount= Task.qpktCount= 0;  // Added to allow repeated execution
					  // of the test.    Ole Agesen, 3/95.

      new IdleTask(I_IDLE, 1, 10000);
//System.out.println("idle: " + System.currentTimeMillis());
      Packet wkq = new Packet(null, 0, K_WORK);
      wkq = new Packet(wkq, 0, K_WORK);
      new WorkTask(I_WORK, 1000, wkq);
//System.out.println("work: " + System.currentTimeMillis());
      wkq = new Packet(null, I_DEVA, K_DEV);
      wkq = new Packet(wkq, I_DEVA, K_DEV);
      wkq = new Packet(wkq, I_DEVA, K_DEV);
      new HandlerTask(I_HANDLERA, 2000, wkq);
//System.out.println("handler a: " + System.currentTimeMillis());
      wkq = new Packet(null, I_DEVB, K_DEV);
      wkq = new Packet(wkq, I_DEVB, K_DEV);
      wkq = new Packet(wkq, I_DEVB, K_DEV);
      new HandlerTask(I_HANDLERB, 3000, wkq);
//System.out.println("handler b: " + System.currentTimeMillis());
      wkq = null;
      new DeviceTask(I_DEVA, 4000, wkq);
      new DeviceTask(I_DEVB, 5000, wkq);
//System.out.println("devices: " + System.currentTimeMillis());
      Task.schedule();
//System.out.println("after sched " + System.currentTimeMillis());
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

