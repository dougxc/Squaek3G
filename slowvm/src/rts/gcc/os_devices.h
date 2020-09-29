/*
 * Copyright 2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */

/**
 * Signal management.
 */

/**
 * Disable (and defer) the delivery of interrupts. Important,
 * for example, when entering the kernel or when interacting
 * with the message queues.
 *
 * returns 0 for success, the result from errno otherwise.
 */
int os_disableSignals(sigset_t *oldSigSet) {
    sigset_t sigSet;

    sigfillset(&sigSet);
    if (sigprocmask(SIG_SETMASK, &sigSet, oldSigSet) < 0) {
        return errno;
    }
    return 0;
}

/**
 * Enable delivery of interrupts.
 */
int os_enableSignals(sigset_t *sigSet) {
    if (sigprocmask(SIG_SETMASK, sigSet, NULL) < 0) {
        return errno;
    }
    return 0;
}

/**
 * Macros for enabling and disabling interrupts.
 */
#define deferInterruptsAndDo(action)                    \
    do {                                                \
        sigset_t savedSigSet;                           \
        os_disableSignals(&savedSigSet);                \
        disableInterrupts();                            \
        { action; }                                     \
        enableInterrupts();                             \
        os_enableSignals(&savedSigSet);                 \
    } while(0)

#define signalHandler_deferInterruptsAndDo(action)  deferInterruptsAndDo(action)

#define reenableInterrupts()                            \
    do {                                                \
        enableInterrupts();                             \
        os_enableSignals(&savedSigSet);                 \
    } while(0)

#define deferInterrupts()                               \
    do {                                                \
        os_disableSignals(NULL);                        \
        disableInterrupts();                            \
    } while(0)
