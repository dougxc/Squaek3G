/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
/*=======================================================================*\
 *                      Device-related routines                          *
\*=======================================================================*/

/**
 * Check that processor interrupts are disabled.
 */
INLINE void assumeInterruptsAreDisabled() {
    assume(interruptsDisabled != 0);
}

/**
 * Check that processor interrupts are enabled.
 */
INLINE void assumeInterruptsAreEnabled() {
    assume(interruptsDisabled == 0);
}

/**
 * Disables processor interrupts.
 */
INLINE void disableInterrupts() {
	os_disableInterrupts();
	interruptsDisabled ++;
}

/**
 * Enables processor interrupts.
 */
INLINE int enableInterrupts() {
    assumeInterruptsAreDisabled();
    interruptsDisabled  --;
    if (interruptsDisabled == 0) {
	    os_enableInterrupts();
    }
    return interruptsDisabled;
}

/**
 * Allocate memory checking that interrupts are disabled.
 *
 * @param length the length to allocate
 * @return the memory address or null if failure
 */
INLINE void *safeMalloc(unsigned length) {
    assumeInterruptsAreDisabled();
    return malloc(length);
}

/**
 * Post a dummy message to an interrupt queue to be handled by
 * JavaDriverManager.
 */
void postMessage(Address key, Message *msg) {
    assumeInterruptsAreDisabled();
    msg->data = NULL;        /* devices *might* want to send data some day. */
    msg->next = NULL;
    //fprintf(stderr, "posting message to %s\n", msg->name);
    addMessage(msg, &toServerMessages);
    addMessageEvent(&toServerWaiters, key);
    //dumpOutMessageQueues();
}

/**
 * Continues execution in the kernel context after the last OPC.PAUSE.
 */
void Squawk_kernelContinue(void) {
    void Squawk_continue(Globals *gp);
    Globals *save = gp;
    gp = &kernelGlobals;
//	deferInterruptsAndDo(
//		fprintf(stderr, "[switching to kernel context]\n");
//	);
    Squawk_continue(gp);
    gp = save;
}

/**
 * This routine sets the signals for transitioning into kernel mode.
 * It is a bit of a hack and may belong better in platform-specific code.
 */
void Squawk_setSignalHandlers(void) {
    os_setSignalHandlers();
}

void Squawk_enterKernel(void) {
    os_enterKernel();
//	deferInterruptsAndDo(
//		fprintf(stderr, "[returning to user context]\n");
//	);
}

/**
 * Kernel mode startup. This routine will startup the Java Driver Manager and return
 * when the system is initialized. The Squawk_continue() function needs to be called
 * periodically when driver operations are to be performed.
 */
void Squawk_kernelMain(int kernelArgsCount, char *kernelArgs[]) {
    void Squawk_run(int argc, char *argv[]);
    Globals *save = gp;

    /* Specify what we're going to run to manage the drivers: */
    kernelArgs[kernelArgsCount ++] = "java.lang.JavaDriverManager";

    initializeGlobals(&kernelGlobals);
    Squawk_run(kernelArgsCount, kernelArgs);

    gp = save;
}


