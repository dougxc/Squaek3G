#include "system.h"
#include "AT91RM9200.h"
#include "systemtimer.h"
#include <syscalls-9200-IO.h>
#include "syscalls-impl.h"
#include "spi.h"

/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */

// Define the maximum number of user-supplied command line args we can accept
#define SQUAWK_STARTUP_ARGS_MAX 20

// Override setting from platform.h
#undef PLATFORM_UNALIGNED_LOADS
#define PLATFORM_UNALIGNED_LOADS false

#define SERVICE_CHUNK_SIZE (8*1024)
#define IODOTC "9200-io.c"

#include <stdlib.h>
#include <sys/time.h>
#include "jni.h"

#define printf iprintf
#define fprintf fiprintf
#define sprintf siprintf

int main(int argc, char *argv[]);
extern int disableARMInterrupts();
extern void enableARMInterrupts();
void initTrapHandlers();

volatile jlong sysTimeMillis(void) {
    jlong res = getMilliseconds();
    return res;
}

jlong sysTimeMicros() {
    return sysTimeMillis() * 1000;
}

void startTicker(int interval) {
    fprintf(stderr, "Profiling not implemented");
    exit(0);
}

extern void setup_java_interrupts();
extern void usb_state_change();

int flash_addr_of_bootstrap;

/* low-level setup task required after cold and warm restarts */
void lowLevelSetup() {
//	diagnostic("in low level setup");
	mmu_enable();
	data_cache_enable();
	spi_init();
	register_usb_state_callback(usb_state_change);
	setup_java_interrupts();
//	diagnostic("java interrupts setup");
    synchroniseWithAVRClock();
    init_system_timer();
    diagnosticWithValue("Current time is ", getMilliseconds());
//	diagnostic("system timer inited");
}

/**
 * Program entrypoint (cold start).
 */
void arm_main(int bootstrap_address, int application_suite_address, int enable_debug, int cmdLineParamsAddr) {
	extern clock_counter;
	int i;

#ifdef DB_DEBUG
	extern int db_debug_enabled;
	db_debug_enabled = enable_debug;
	// record the app address for the debugger's benefit
	extern int db_app_addr;
	db_app_addr = application_suite_address;
#endif

	diagnosticWithValue("in vm", application_suite_address);

	// remember bootstrap address for suite.c
	flash_addr_of_bootstrap = bootstrap_address;

	lowLevelSetup();
//	diagnostic("low level setup complete");
    
    iprintf("\n");
    iprintf("Squawk VM Starting (");
	iprintf(BUILD_DATE);
	iprintf(")...\n");
	
	char* startupArgs = (char*)cmdLineParamsAddr;
	char *fakeArgv[SQUAWK_STARTUP_ARGS_MAX + 2];
	fakeArgv[0] = "dummy"; // fake out the executable name

	char suiteArg[] = "-flashsuite:00000000"; // unsafe?? isn't this in flash?
	for (i = 19; i >= 12; i--) {
		int digit = application_suite_address & 0xF;
		application_suite_address >>= 4;
		suiteArg[i] = asHexChar(digit);
	}
	diagnostic(suiteArg);

	fakeArgv[1] = suiteArg;
	
	int fakeArgc = 2;
	int index = 0;
	/* The startupArgs structure comprises a sequence of null-terminated string
	 * with another null to indicate the end of the structure
	 */
	while (startupArgs[index] != 0) {
		fakeArgv[fakeArgc] = &startupArgs[index];
		//iprintf("Parsed arg: %s\n", fakeArgv[fakeArgc]);
		fakeArgc++;
		if (fakeArgc > SQUAWK_STARTUP_ARGS_MAX + 2) {
			iprintf("Number of startup args exceeds maximum permitted\n");
			exit(-1);
		}
		while (startupArgs[index] != 0) {
			index++;
		}
		// skip over the terminating null
		index++;
	}
	
    main(fakeArgc, fakeArgv);
    sysPrint("\r\nmain function returned, restarting\r\n");
    disableARMInterrupts();
    hardwareReset();
}

/**
 * Support for util.h
 */

long sysconf(int code) {
    if (code == _SC_PAGESIZE) {
        return 4;
    } else {
        return -1; // failure
    }
}

INLINE void osloop() {
	//no-op on spot platform
}

INLINE void osbackbranch() {
}

void osfinish() {
	disable_system_timer();
	diagnostic("OSFINISH");
    asm(SWI_ATTENTION_CALL);
}

