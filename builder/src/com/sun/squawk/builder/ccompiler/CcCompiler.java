/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: CcCompiler.java,v 1.1 2005/02/14 08:46:22 dsimon Exp $
 */
package com.sun.squawk.builder.ccompiler;

import com.sun.squawk.builder.platform.*;
import com.sun.squawk.builder.*;
import java.io.File;

/**
 * The interface for the "cc" compiler.
 */
public class CcCompiler extends CCompiler {

    public CcCompiler(Build env, Platform platform) {
        super("cc", env, platform);
    }

    /**
     * {@inheritDoc}
     */
    public String options(boolean disableOpts) {
        StringBuffer buf = new StringBuffer();
        if (!disableOpts) {
            if (options.o1)             { buf.append("-xO2 -xspace ");      }
            if (options.o2)             { buf.append("-xO2 ");              }
            if (options.o3)             { buf.append("-DMAXINLINE -xO5 ");  }
        }
        if (options.tracing)            { buf.append("-DTRACE ");           }
        if (options.profiling)          { buf.append("-DPROFILING ");       }
        if (options.macroize)           { buf.append("-DMACROIZE ");        }
        if (options.assume)             { buf.append("-DASSUME ");          }
        if (options.typemap)            { buf.append("-DTYPEMAP ");         }
        if (options.ioport)             { buf.append("-DIOPORT ");          }
        if (options.kernel)             { buf.append("-DKERNEL_SQUAWK=true ");          }
		if (options.nativeVerification) { buf.append("-DNATIVE_VERIFICATION=true ");          }
        
        if (options.is64)               { buf.append("-DSQUAWK_64=true ").append("-xarch=v9 "); }


        // Only enable debug switch if not optimizing
        if (!options.o1 &&
            !options.o2 &&
            !options.o3)                { buf.append("-xsb -g ");           }
        
        if (Platform.isX86Architecture()) {
            buf.append("-fstore ");
        }

        buf.append("-DPLATFORM_BIG_ENDIAN=" + platform.isBigEndian()).append(' ');
        buf.append("-DPLATFORM_UNALIGNED_LOADS=" + platform.allowUnalignedLoads()).append(' ');

        return buf.append(options.cflags).append(' ').toString();
    }

    /**
     * Gets the linkage options that must come after the input object files.
     *
     * @return the linkage options that must come after the input object files
     */
    public String getLinkSuffix() {
        String suffix = "";

        if (options.kernel && options.hosted) {
            /* Hosted by HotSpot and so we need the signal-interposition
             * library. */
            String jsigLib = env.getPlatform().getJVMLibraryPath();
            suffix += " -L" + jsigLib.replaceAll(File.pathSeparator, " -L") + " -ljsig";
        }

        return suffix;
    }

    /**
     * {@inheritDoc}
     */
    public File compile(File[] includeDirs, File source, File dir, boolean disableOpts) {
        File object = new File(dir, source.getName().replaceAll("\\.c", "\\.o"));
        env.exec("cc -c " +
                 options(disableOpts) + " " +
                 include(includeDirs, "-I") +
                 " -o " + object + " " + source);
        return object;
    }

    /**
     * {@inheritDoc}
     */
    public File link(File[] objects, String out, boolean dll) {
        String output;
        String exec;

        String libm = "";
        if (options.floatsSupported) {
            libm = "-lm";
        }

        if (dll) {
            output = System.mapLibraryName(out);
            exec = " -G -lthread -o " + output + " " + Build.join(objects) + " -ldl -lm -lsocket -lnsl " + getLinkSuffix();
        } else {
            output = out + platform.getExecutableExtension();
            exec = " -lthread -o " + output + " " + Build.join(objects) + " -ldl " + libm + " -lsocket -lnsl " + getLinkSuffix();
        }
        env.exec("cc " + exec);
        return new File(output);
    }

    /**
     * {@inheritDoc}
     */
    public String getArchitecture() {
        return (options.is64) ? "SV9_64" : "SV8";
    }
}
