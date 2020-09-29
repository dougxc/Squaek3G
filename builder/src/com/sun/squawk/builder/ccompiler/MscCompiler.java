/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: MscCompiler.java,v 1.1 2005/02/14 08:46:22 dsimon Exp $
 */
package com.sun.squawk.builder.ccompiler;

import com.sun.squawk.builder.platform.*;
import com.sun.squawk.builder.*;
import java.io.File;

/**
 * The interface for the "cl" MS Visual C++ compiler.
 */
public class MscCompiler extends CCompiler {

    public MscCompiler(Build env, Platform platform) {
        super("msc", env, platform);
    }

    /**
     * {@inheritDoc}
     */
    public String options(boolean disableOpts) {
        StringBuffer buf = new StringBuffer();
        if (!disableOpts) {
            if (options.o1)             { buf.append("/O1 ");              }
            if (options.o2)             { buf.append("/O2 /Gs "); }
            if (options.o3)             { buf.append("/DMAXINLINE ");      }
        }
        if (options.tracing)            { buf.append("/DTRACE ");          }
        if (options.profiling)          { buf.append("/DPROFILING /MT ");  }
        if (options.macroize)           { buf.append("/DMACROIZE ");       }
        if (options.assume)             { buf.append("/DASSUME ");         }
        if (options.typemap)            { buf.append("/DTYPEMAP ");        }
        if (options.ioport)             { buf.append("/DIOPORT ");         }

        if (options.kernel) {
            throw new BuildException("-kernel option not supported by MscCompiler");
        }
        
        if (options.nativeVerification){ buf.append("/DNATIVE_VERIFICATION=true ");         }
        	

        // Only enable debug switch if not optimizing
        if (!options.o1 &&
            !options.o2 &&
            !options.o3)                { buf.append("/ZI ");              }

        if (options.is64) {
            throw new BuildException("-64 option not supported by MscCompiler");
        }

        buf.append("/DIOPORT ");

        buf.append("/DPLATFORM_BIG_ENDIAN=" + platform.isBigEndian()).append(' ');
        buf.append("/DPLATFORM_UNALIGNED_LOADS=" + platform.allowUnalignedLoads()).append(' ');

        return buf.append(options.cflags).append(' ').toString();
    }

    /**
     * {@inheritDoc}
     */
    public File compile(File[] includeDirs, File source, File dir, boolean disableOpts) {
        File object = new File(dir, source.getName().replaceAll("\\.c", "\\.obj"));
        env.exec("cl /c /nologo /wd4996 " +
                 options(disableOpts) + " " +
                 include(includeDirs, "/I") +
                 "/Fo" + object + " " + source);
        return object;
    }

    /**
     * {@inheritDoc}
     */
    public File link(File[] objects, String out, boolean dll) {
        String output;
        String exec;

        if (dll) {
            output = System.mapLibraryName(out);
            exec = " /Fe" + output + " /LD " + Build.join(objects) + " /link wsock32.lib /IGNORE:4089";
        } else {
            output = out + platform.getExecutableExtension();
            exec = " /Fe" + output + " " + Build.join(objects) + " /link wsock32.lib /IGNORE:4089";
        }
        env.exec("cl " + exec);
        return new File(output);
    }

    /**
     * {@inheritDoc}
     */
    public String getArchitecture() {
        return "X86";
    }
}
