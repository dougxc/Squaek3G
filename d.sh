# Doug's version of the launcher

#----------------------------------------------------------#
#              Setup environment                           #
#----------------------------------------------------------#

if [ ! -f d ]; then
    if [ -z "`uname | grep 'Windows'`" ]; then
        ln -s d.sh d
        ln -s d_g.sh d_g
        chmod +x `find . -name '*.sh'`
    fi
fi

if [ -z "$JAVA_HOME" ]; then
  JAVA_HOME=`which java`
  JAVA_HOME=`dirname $JAVA_HOME`
  JAVA_HOME=`dirname $JAVA_HOME`
fi

#echo "JAVA_HOME=$JAVA_HOME"
builder="${JAVA_HOME}/bin/java $EXTRA_BUILDER_VMFLAGS -Xms128M -Xmx256M -ea -jar build.jar $BUILDER_FLAGS"
#echo $builder

#----------------------------------------------------------#
#              Rebuild the builder                         #
#----------------------------------------------------------#

if [ $# -gt 0 -a "X$1" = "Xbuilder" ]; then 
    cd builder;
    ./bld.sh $JAVA_HOME
    cd ..
    exit
fi

#----------------------------------------------------------#
#              Rebuild the CSystem.dll                     #
#----------------------------------------------------------#

if [ $# -gt 0 -a "X$1" = "Xcsystem" ]; then 
    cl "/I${JAVA_HOME}\include" "/I${JAVA_HOME}\include\win32" /c \
        prototypecompiler/src/com/sun/squawk/compiler/jni/CSystem.c \
        prototypecompiler/src/com/sun/squawk/compiler/jni/dispatch_x86.c
    link /nologo /debug /dll /out:CSystem.dll CSystem.obj dispatch_x86.obj
    exit
fi

#----------------------------------------------------------#
#              Launch Squawk in SDA                        #
#----------------------------------------------------------#

if [ $# -gt 0 -a "X$1" = "Xsda" ]; then 
    shift
    eval squawk -verbose com.sun.squawk.debugger.sda.SDA $*
    exit
fi

#----------------------------------------------------------#
#           Run CLDC TCK 1.0a Static Signature Test        #
#----------------------------------------------------------#

if [ $# -gt 0 -a "X$1" = "Xsigtest10a" ]; then 
    if [ "X$TCK10a_DIR" = "X" ]; then
        echo "Need to set TCK10a_DIR variable to base dir of CLDC TCK 1.0a"
        exit 1
    fi
    cmd="java -cp $TCK10a_DIR/javatest.jar:$TCK10a_DIR/sigtest.jar javasoft.sqe.tests.api.signaturetest.cldc.CLDCSignatureTest -TestURL file:$TCK10a_DIR/tests/api/signaturetest/ -Classpath j2me/j2meclasses"
    echo $cmd
    eval $cmd
    exit
fi

#----------------------------------------------------------#
#           Run CLDC TCK 1.1 Static Signature Test         #
#----------------------------------------------------------#

if [ $# -gt 0 -a "X$1" = "Xsigtest11" ]; then 
    if [ "X$TCK11_DIR" = "X" ]; then
        TCK11_DIR=../tck/CLDC-TCK_11
        if [ ! -d $TCK11_DIR ]; then
            echo "Need to set TCK11_DIR variable to base dir of CLDC TCK 1.1"
            exit 1
        fi
    fi
    cmd="java -jar $TCK11_DIR/lib/sigtest.jar -TCK_ROOT file:$TCK11_DIR -Classpath j2me/j2meclasses"
    echo $cmd
    eval $cmd
    exit
fi

#----------------------------------------------------------#
#           Start the JavaTest  UI                         #
#----------------------------------------------------------#

if [ $# -gt 0 -a "X$1" = "Xjavatest11" ]; then 
    if [ "X$TCK11_DIR" = "X" ]; then
        TCK11_DIR=../tck/CLDC-TCK_11
        if [ ! -d $TCK11_DIR ]; then
            echo "Need to set TCK11_DIR variable to base dir of CLDC TCK 1.1"
            exit 1
        fi
    fi
    cmd="java -jar $TCK11_DIR/lib/javatest.jar"
    echo $cmd
    eval $cmd
    exit
fi

#----------------------------------------------------------#
#           Run IMP TCK 1.0 Static Signature Teste         #
#----------------------------------------------------------#

if [ $# -gt 0 -a "X$1" = "Ximpsigtest10" ]; then 
    TCK_DIR=../tck/IMP-TCK_10
    if [ ! -d $DIR ]; then
        echo "Cannot find $TCK_DIR"
        exit 1
    fi
    cmd="java -jar $TCK_DIR/lib/sigtest.jar -TCK_ROOT file:$TCK_DIR -Classpath j2me/j2meclasses -Package javax"
    echo $cmd
    eval $cmd
    exit
fi

#----------------------------------------------------------#
#              Rebuild the native methods                  #
#----------------------------------------------------------#

if [ $# -gt 0 -a "X$1" = "Xnmethods" ]; then 
    eval $builder j2me
    cd bytecodes;
    ./nbld.sh
    cd ..
    eval $builder clean
    eval $builder j2me
    exit
fi


#----------------------------------------------------------#
#             Macros for Andrew's demo                     #
#----------------------------------------------------------#

if [ $# -gt 0 -a "X$1" = "Xserver" ]; then 
    cmd="squawk -verbose -cp:samples/j2meclasses example.shell.LookupServer -verbose -loadbalance:ManyBalls"
    echo $cmd
    $cmd
    exit
fi
	
if [ $# -gt 0 -a "X$1" = "Xshell" ]; then 
    localhost=""
    if [ $# -gt 2 ]; then
        localhost=",$3"
    fi
    cmd="squawk -verbose -cp:samples/j2meclasses example.shell.Main -verbose -register:$2$localhost"
    echo $cmd
    $cmd
    exit
fi
	
#----------------------------------------------------------#
#              Start jamspot command                       #
#----------------------------------------------------------#

if [ $# -gt 0 -a "X$1" = "Xjamspot" ]; then 
    exec $builder -plugins:spot-build-plugin/plugin/spot-plugin.properties $*
fi

#----------------------------------------------------------#
#              Fall through to build.jar                   #
#----------------------------------------------------------#

#echo $builder $*
exec $builder $*
