/*
 * Copyright 2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */

/**
 * Macros for enabling and disabling interrupts.
 */
#define deferInterruptsAndDo(action)                    \
    do {                                                \
        disableInterrupts();                            \
        { action; }                                     \
        enableInterrupts();                             \
    } while(0)

#define signalHandler_deferInterruptsAndDo(action)  deferInterruptsAndDo(action)

#define reenableInterrupts()                            \
    do {                                                \
        enableInterrupts();                             \
    } while(0)

#define deferInterrupts()                               \
    do {                                                \
        disableInterrupts();                            \
    } while(0)
