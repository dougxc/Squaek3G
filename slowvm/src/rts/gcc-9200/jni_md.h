/*
 * @(#)jni_md.h	1.12 00/02/02
 *
 * Copyright 1996-2000 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */

#ifndef _JAVASOFT_JNI_MD_H_
#define _JAVASOFT_JNI_MD_H_

#define JNIEXPORT 
#define JNIIMPORT
#define JNICALL

#ifdef _LP64 /* 64-bit Solaris */
typedef int jint;
#else
typedef long jint;
#endif
typedef signed char jbyte;

typedef long long int64_t;
typedef unsigned long long u_int64_t;
#define jlong int64_t

#endif /* !_JAVASOFT_JNI_MD_H_ */
