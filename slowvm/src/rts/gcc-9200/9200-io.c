/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */

#include "spi.h"
#include "flash.h"
#include "avr.h"

char serialRxBuffer[129]; // 128 plus one for the "magic" byte at the beginning
char serialTxBuffer[129];

// Forward declarations
int getEventPrim(int);
static int check_irq(int irq_mask, int clear_flag);
INLINE boolean checkForMessageEvent();

// General helper method
long long rebuildLongParam(int i1, int i2) {
	return ((long long)i1 << 32) | ((long long)i2 & 0xFFFFFFFF);
}

/**************************************************************************
 * Sleep support
 **************************************************************************/
int deepSleepEnabled = 0; // indicates whether the feature is currently enabled (=1)
int sleepManagerRunning = 1;	   // assume that sleepManager is running until it calls WAIT_FOR_DEEP_SLEEP
int outstandingDeepSleepEvent = 0; // whether the sleep manager thread should be unblocked at the next reschedule
long long storedDeepSleepWakeupTarget; // The millis that the next deep sleep should end at
long long minimumDeepSleepMillis = 0x7FFFFFFFFFFFFFFFLL;
 		// minimum time we're prepared to deep sleep for: avoid deep sleeping initially.
 
/*
 * Stop the processor clock - restarts on interrupt
 */
static stopProcessor() {
	*AT91C_PMC_SCDR = AT91C_PMC_PCK;
}

/*
 * Enter deep sleep
 */
static void doDeepSleep(long long targetMillis) {
	long long millisecondsToWait = targetMillis - getMilliseconds();
	diagnosticWithValue("In doDeepSleep", (int)targetMillis);
    deepSleep(millisecondsToWait);
	lowLevelSetup(); //need to repeat low-level setup after a restart
}

/*
 * Enter shallow sleep
 */
static void doShallowSleep(long long targetMillis) {
	while (1) {
		if (checkForEvents()) break;
		if (checkForMessageEvent()) break;
		if (getMilliseconds() > targetMillis) break;
		stopProcessor();
	}
}

static void setDeepSleepEventOutstanding(long long target) {
	storedDeepSleepWakeupTarget = target;
	outstandingDeepSleepEvent = 1;
}

/******************************************************************
 * Serial port support
 ******************************************************************/
#define SERIAL_PORT_EVENT_NUMBER 1
#define WAIT_FOR_DEEP_SLEEP_EVENT_NUMBER 2
#define FIRST_IRQ_EVENT_NUMBER (WAIT_FOR_DEEP_SLEEP_EVENT_NUMBER+1)
int serialPortInUse = 0;

/* Java has requested serial chars */
int getSerialPortEvent() {
	serialPortInUse = 1;
	return SERIAL_PORT_EVENT_NUMBER;
}

int isSerialPortInUse() {
	return serialPortInUse;
}

void freeSerialPort() {
	serialPortInUse = 0;
}

/*
 * ****************************************************************
 * Interrupt Handling Support
 *
 * See comment in AT91_AIC.java for details
 * ****************************************************************
 */

unsigned int java_irq_status = 0; // bit set = that irq has outstanding interrupt request

void usb_state_change()	{
	int cpsr = disableARMInterrupts();
	java_irq_status |= (1<<11); // USB Device ID
	setARMInterruptBits(cpsr);
}

struct irqRequest {
        int eventNumber;
        int irq_mask;
        struct irqRequest *next;
};
typedef struct irqRequest IrqRequest;

IrqRequest *irqRequests;

extern void java_irq_hndl();

void setup_java_interrupts() {
	// This routine is called from os.c
	// NB interrupt handler coded in java-irq-hndl.s

	at91_irq_setup (AT91C_ID_PIOA, &java_irq_hndl);
	at91_irq_setup (AT91C_ID_PIOB, &java_irq_hndl);
	at91_irq_setup (AT91C_ID_PIOC, &java_irq_hndl);
	at91_irq_setup (AT91C_ID_PIOD, &java_irq_hndl);
	at91_irq_setup (AT91C_ID_US0, &java_irq_hndl);
	at91_irq_setup (AT91C_ID_TC0, &java_irq_hndl);
	at91_irq_setup (AT91C_ID_TC1, &java_irq_hndl);
	at91_irq_setup (AT91C_ID_TC2, &java_irq_hndl);
	at91_irq_setup (AT91C_ID_TC3, &java_irq_hndl);
	at91_irq_setup (AT91C_ID_IRQ0, &java_irq_hndl);
	at91_irq_setup (AT91C_ID_IRQ3, &java_irq_hndl);
}

/*
 * Java has requested wait for an interrupt. Store the request,
 * and each time Java asks for events, signal the event if the interrupt has happened
 *
 * @return the event number
 */
int storeIrqRequest (int irq_mask) {
        IrqRequest* newRequest = (IrqRequest*)malloc(sizeof(IrqRequest));
        if (newRequest == NULL) {
        	//TODO set up error message for GET_ERROR and handle
        	//one per channel and clean on new requests.
        	return ChannelConstants_RESULT_EXCEPTION;
        }

        newRequest->next = NULL;
        newRequest->irq_mask = irq_mask;

        if (irqRequests == NULL) {
        	irqRequests = newRequest;
        	newRequest->eventNumber = FIRST_IRQ_EVENT_NUMBER;
        } else {
        	IrqRequest* current = irqRequests;
        	while (current->next != NULL) {
        		current = current->next;
        	}
        	current->next = newRequest;
        	newRequest->eventNumber = current->eventNumber + 1;
        }
        return newRequest->eventNumber;
}

/* ioPostEvent is a no-op for us */
static void ioPostEvent(void) { }

/*
 * If there are outstanding irqRequests and one of them is for an irq that has
 * occurred remove it and return its eventNumber. Otherwise return 0
 */
int getEvent() {
        return getEventPrim(1);
}

/*
 * If there are outstanding irqRequests and one of them is for an interrupt that has
 * occurred return its eventNumber. Otherwise return 0
 */
int checkForEvents() {
        return getEventPrim(0);
}

/*
 * If there are outstanding irqRequests and one of them is for an interrupt that has
 * occurred return its eventNumber. If removeEventFlag is true, then
 * also remove the event from the queue. If no requests match the interrupt status
 * return 0.
 */
int getEventPrim(int removeEventFlag) {
    int res = 0;
    if (irqRequests != NULL) {
    	IrqRequest* current = irqRequests;
        IrqRequest* previous = NULL;
        while (current != NULL) {
        	if (check_irq(current->irq_mask, removeEventFlag)) {
        		res = current->eventNumber;
        		//unchain
        		if (removeEventFlag) {
        			if (previous == NULL) {
        				irqRequests = current->next;
        			} else {
        				previous->next = current->next;
        			}
        			free(current);
        		}
        		break;
        	} else {
        		previous = current;
        		current = current->next;
        	}
        }
    }
    if (res == 0) {
    	// check for serial chars available
    	if (isSerialPortInUse() && sysAvailable()) {
    		res = SERIAL_PORT_EVENT_NUMBER;
    		if (removeEventFlag) {
    			freeSerialPort();
    		}
    	}
    }
   	if (res == 0) {
    	if (outstandingDeepSleepEvent) {
    		sleepManagerRunning = 1;
    		res = WAIT_FOR_DEEP_SLEEP_EVENT_NUMBER;
    	}
   	}
	if (removeEventFlag) {
		// always clear the deep sleep event, as we will want to reconsider
		// whether deep sleep is appropriate after any event.
		outstandingDeepSleepEvent = 0;
	}
    return res;
}

/**
 * Check if an irq bit is set in the status, return 1 if yes
 * Also, clear bit if it is set and clear_flag = 1
 */
static int check_irq(int irq_mask, int clear_flag) {
        int result;
        disableARMInterrupts();
        if ((java_irq_status & irq_mask) != 0) {
        	if (clear_flag) {
            	java_irq_status = java_irq_status & ~irq_mask;
        	}
            result = 1;
        } else {
        	result = 0;
        }
        enableARMInterrupts();
        return result;
}




int retValue = 0;  // holds the value to be returned on the next "get result" call
int avr_low_result = 0;

/**
 * Executes an operation on a given channel for an isolate.
 *
 * @param  context the I/O context
 * @param  op      the operation to perform
 * @param  channel the identifier of the channel to execute the operation on
 * @param  i1
 * @param  i2
 * @param  i3
 * @param  i4
 * @param  i5
 * @param  i6
 * @param  send
 * @param  receive
 * @return the operation result
 */
 static void ioExecute(void) {
	int     context = com_sun_squawk_ServiceOperation_context;
    int     op      = com_sun_squawk_ServiceOperation_op;
    int     channel = com_sun_squawk_ServiceOperation_channel;
    int     i1      = com_sun_squawk_ServiceOperation_i1;
    int     i2      = com_sun_squawk_ServiceOperation_i2;
    int     i3      = com_sun_squawk_ServiceOperation_i3;
    int     i4      = com_sun_squawk_ServiceOperation_i4;
    int     i5      = com_sun_squawk_ServiceOperation_i5;
    int     i6      = com_sun_squawk_ServiceOperation_i6;
    Address send    = com_sun_squawk_ServiceOperation_o1;
    Address receive = com_sun_squawk_ServiceOperation_o2;

    int res = ChannelConstants_RESULT_OK;

    switch (op) {
    	case ChannelConstants_GLOBAL_CREATECONTEXT:
    		res = 1; //let all Isolates share a context for now
    		break;
    	case ChannelConstants_CONTEXT_GETCHANNEL: {
            		int channelType = i1;
            		if (channelType == ChannelConstants_CHANNEL_IRQ) {
            			res = 1;
            		} else if (channelType == ChannelConstants_CHANNEL_SPI) {
            			res = 2;
            		} else {
            			res = ChannelConstants_RESULT_BADPARAMETER;
            		}
            	}
    		break;
    	case ChannelConstants_IRQ_WAIT: {
            		int irq_no = i1;
            		if (check_irq(irq_no, 1)) {
            			res = 0;
            		} else {
        	    		res = storeIrqRequest(irq_no);
        	    	}
    		}
    		break;
    		
    	case ChannelConstants_WAIT_FOR_SERIAL_CHAR: {
    			if (sysAvailable()) {
	    			// Return 0 if there are chars available
	    			res = 0;
	    		} else {
	    			// Otherwise return event number to say there might be later
	    			res = getSerialPortEvent();
	    		}
	    	}
    	    break;
    	    		
	    case ChannelConstants_GET_SERIAL_CHARS: {
		    	int offset = i1;
		    	int len = i2;
		    	char* buf = send;
		    	res = sysReadSeveral(buf + offset, len);
		    	freeSerialPort(); // free serial port for future use
    		}
    	    break;
    	    		
	    case ChannelConstants_WRITE_SERIAL_CHARS: {
		    	int offset = i1;
		    	int len = i2;
		    	char* buf = send;
		    	sysWriteSeveral(buf + offset, len);
		    	res = 0;
    		}
    	    break;
    	    		
	    case ChannelConstants_SPI_SEND_RECEIVE_8: {
	    		// CE pin in i1
	    		// SPI config in i2
	    		// data in i3
	    		res = spi_sendReceive8(i1, i2, i3);
		    }
		    break;
	    case ChannelConstants_SPI_SEND_RECEIVE_8_PLUS_SEND_16: {
	    		// CE pin in i1
	    		// SPI config in i2
	    		// data in i3
	    		// 16 bits in i4
	    		res = spi_sendReceive8PlusSend16(i1, i2, i3, i4);
		    }
		    break;
	    case ChannelConstants_SPI_SEND_RECEIVE_8_PLUS_SEND_N: {
	    		// CE pin in i1
	    		// SPI config in i2
	    		// data in i3
	    		// size in i4
	    		res = spi_sendReceive8PlusSendN(i1, i2, i3, i4, send);
		    }
		    break;
	    case ChannelConstants_SPI_SEND_RECEIVE_8_PLUS_RECEIVE_16: {
	    		// CE pin in i1
	    		// SPI config in i2
	    		// data in i3
	    		// 16 bits encoded in result
	    		res = spi_sendReceive8PlusReceive16(i1, i2, i3);
		    }
		    break;
	    case ChannelConstants_SPI_SEND_RECEIVE_8_PLUS_VARIABLE_RECEIVE_N: {
	    		// CE pin in i1
	    		// SPI config in i2
	    		// data in i3
	    		// fifo_pin in i4
	    		// fifo pio in i5
	    		// data in receive
	    		res = spi_sendReceive8PlusVariableReceiveN(i1, i2, i3, receive, i4, i5);
		    }
		    break;
		case ChannelConstants_SPI_SEND_AND_RECEIVE: {
				// CE pin in i1
				// SPI config in i2
	    		// tx size in i4
	    		// rx size in i5
	    		// rx offset in i6
	    		// tx data in send
	    		// rx data in receive
	    		spi_sendAndReceive(i1, i2, i4, i5, i6, send, receive);
			}
			break;
		case ChannelConstants_SPI_SEND_AND_RECEIVE_WITH_DEVICE_SELECT: {
				// CE pin in i1
				// SPI config in i2
				// device address in i3
	    		// tx size in i4
	    		// rx size in i5
	    		// rx offset in i6
	    		// tx data in send
	    		// rx data in receive
	    		spi_sendAndReceiveWithDeviceSelect(i1, i2, i3, i4, i5, i6, send, receive);
			}
			break;
    	case ChannelConstants_GET_SERIAL_RX_BUFFER_ADDR:
    		res = (int)serialRxBuffer;
    		break;
    	case ChannelConstants_GET_SERIAL_TX_BUFFER_ADDR:
    		res = (int)serialTxBuffer;
    		break;
    	case ChannelConstants_FLASH_ERASE:
    		data_cache_disable();
    		res = flash_erase_sector((Flash_ptr)i2);
    		data_cache_enable();
    		// the 9200 is stopped while flash busy, so need to reset the clock
    		synchroniseWithAVRClock();
    		break;
    	case ChannelConstants_FLASH_WRITE: {
				int i, d, address = i1, size = i2, offset = i3;
				char *buffer = (char*) send;
	    		data_cache_disable();
				for (i = 0; i < size; i+=2) {
					// construct 16 bit value to write, using FF in the top byte if no data
					d = ((i+1==size?0xFF:((int)buffer[i+offset+1])&0xFF) << 8) | (((int)buffer[i+offset])&0xFF);
					res = flash_write_word((Flash_ptr)(address+i), d);
				}
	    		data_cache_enable();
	    		// the 9200 is stopped while flash busy, so need to reset the clock
	    		synchroniseWithAVRClock();
	    	}
    		break;
    	case ChannelConstants_USB_GET_STATE:
    		res = usb_get_state();
    		break;

    	case ChannelConstants_CONTEXT_GETERROR:
    		res = *((char*)retValue);
    		if (res == 0)
    			retValue = 0;
    		else
    			retValue++;
    		break;
    		
    	case ChannelConstants_CONTEXT_GETRESULT:
    	case ChannelConstants_CONTEXT_GETRESULT_2:
    		res = retValue;
    		retValue = 0;
    		break;
    	case ChannelConstants_GLOBAL_GETEVENT:
    		res = getEvent();
    		break;
    	case ChannelConstants_GLOBAL_WAITFOREVENT: {
			long long millisecondsToWait = rebuildLongParam(i1, i2);
			long long target = ((long long)getMilliseconds()) + millisecondsToWait;
        	if (target <= 0) target = 0x7FFFFFFFFFFFFFFFLL; // overflow detected
//			diagnosticWithValue("GLOBAL_WAITFOREVENT - deepSleepEnabled", deepSleepEnabled);
//			diagnosticWithValue("GLOBAL_WAITFOREVENT - sleepManagerRunning", sleepManagerRunning);
//			diagnosticWithValue("GLOBAL_WAITFOREVENT - minimumDeepSleepMillis", minimumDeepSleepMillis);
			if ((millisecondsToWait < 0x7FFFFFFFFFFFFFFFLL) && deepSleepEnabled && !sleepManagerRunning && (millisecondsToWait > minimumDeepSleepMillis)) {
//				diagnosticWithValue("GLOBAL_WAITFOREVENT - deep sleeping for", (int)millisecondsToWait);
				setDeepSleepEventOutstanding(target);
			} else {
//				diagnosticWithValue("GLOBAL_WAITFOREVENT - shallow sleeping for", (int)millisecondsToWait);
				doShallowSleep(target);
			}
   			res = 0;
    		}
    		break;
    	case ChannelConstants_CONTEXT_DELETE:
    		// TODO delete all the outstanding events on the context
    		// But will have to wait until we have separate contexts for each isolate
    		res=0;
    		break;
        case ChannelConstants_CONTEXT_HIBERNATE:
            // TODO this is faked, we have no implementation currently.
            res = ChannelConstants_RESULT_OK;
            break;
        case ChannelConstants_CONTEXT_GETHIBERNATIONDATA:
            // TODO this is faked, we have no implementation currently.
            res = ChannelConstants_RESULT_OK;
            break;
        case ChannelConstants_DEEP_SLEEP: {
        	doDeepSleep(rebuildLongParam(i1, i2));
    		res = 0;
	        } 
            break;
        case ChannelConstants_SHALLOW_SLEEP: {
    		long long target = rebuildLongParam(i1, i2);
    		if (target <= 0) target = 0x7FFFFFFFFFFFFFFFLL; // overflow detected
    		doShallowSleep(target);
    		res = 0;
	        } 
            break;
        case ChannelConstants_DIAGNOSTIC: {
        	Address str = (Address)send;
        	int length = getArrayLength(str);
        	diagnosticPrimWithValue(str, length, i1);
        	res = 0;
        	}
        	break;
        case ChannelConstants_WAIT_FOR_DEEP_SLEEP:
    		minimumDeepSleepMillis = rebuildLongParam(i1, i2);
    		sleepManagerRunning = 0;
    		res = WAIT_FOR_DEEP_SLEEP_EVENT_NUMBER;
        	break;

        case ChannelConstants_DEEP_SLEEP_TIME_MILLIS_HIGH:
        	res = (int) (storedDeepSleepWakeupTarget >> 32);
        	break;

        case ChannelConstants_DEEP_SLEEP_TIME_MILLIS_LOW:
        	res = (int) (storedDeepSleepWakeupTarget & 0xFFFFFFFF);
        	break;
        	
        case ChannelConstants_AVR_GET_TIME_HIGH: {
        	jlong avr_time = avrGetTime();
        	avr_low_result = (int) avr_time;
        	res = (int)(avr_time >> 32);
        	}
        	break;

        case ChannelConstants_AVR_GET_TIME_LOW:
        	res = avr_low_result;
        	break;
        	
        case ChannelConstants_SET_DEEP_SLEEP_ENABLED:
        	deepSleepEnabled = i1;
        	res = 0;
        	break;
        	
        case ChannelConstants_WRITE_SECURED_SILICON_AREA:
        	data_cache_disable();
        	write_secured_silicon_area((Flash_ptr)i1, (short)i2);
        	data_cache_enable();
        	break;
        	
        case ChannelConstants_READ_SECURED_SILICON_AREA:
        	data_cache_disable();
        	read_secured_silicon_area((unsigned char*)send);
        	data_cache_enable();
        	break;
        	
        case ChannelConstants_SET_SYSTEM_TIME:
    		setMilliseconds(rebuildLongParam(i1, i2));
    		res = 0;
        	break;
        	
        case ChannelConstants_ENABLE_AVR_CLOCK_SYNCHRONISATION:
    		enableAVRClockSynchronisation(i1);
    		res = 0;
        	break;
        	
		case ChannelConstants_OPENCONNECTION:
			res = ChannelConstants_RESULT_EXCEPTION;
			retValue = (int)"javax.microedition.io.ConnectionNotFoundException";
			break;
			
		case ChannelConstants_GET_PUBLIC_KEY: {
		    	int maximum_length = i1;
		    	char* buffer_to_write_public_key_into = send;		    	
		    	res = write_public_key(buffer_to_write_public_key_into, maximum_length);
    		}
    	    break;
    	case ChannelConstants_COMPUTE_CRC16_FOR_MEMORY_REGION:{
			int address=i1;
			int numberOfBytes=i2;
			res = crc(address, numberOfBytes);
			}
			break;
    	    
        default:
    		res = ChannelConstants_RESULT_BADPARAMETER;
    }
    com_sun_squawk_ServiceOperation_result = res;
}

/**
 * Initializes the IO subsystem.
 *
 * @param  jniEnv      the table of JNI function pointers which is only non-null if Squawk was
 *                     launched via a JNI call from a Java based launcher
 * @param  classPath   the class path with which to start the embedded JVM (ignored if 'jniEnv' != null)
 * @param  args        extra arguments to pass to the embedded JVM (ignored if 'jniEnv' != null)
 * @param  argc        the number of extra arguments in 'args' (ignored if 'jniEnv' != null)
 */

void CIO_initialize(JNIEnv *jniEnv, char *classPath, char** args, int argc) {
}
