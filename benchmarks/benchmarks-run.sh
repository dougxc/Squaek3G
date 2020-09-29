#           
# Copyright 2006 Sun Microsystems, Inc. All Rights Reserved.
#
# U.S. Government Rights - Commercial software. Government users are   
# subject to the Sun Microsystems, Inc. standard license agreement and   
# applicable provisions of the FAR and its supplements.
#
# Use is subject to license terms. Sun, Sun Microsystems, the Sun logo   
# and Java are trademarks or registered trademarks of Sun Microsystems,   
# Inc. in the U.S. and other countries.
#

#!/bin/sh

uname -a > benchmarks-run.log
java -version 2>> benchmarks-run.log

echo Richards benchmarking >> benchmarks-run.log
echo "java -Xint -classpath benchmarks/classes:j2me/classes squawk.application.StartupRichards" >> benchmarks-run.log
java -Xint -classpath benchmarks/classes:j2me/classes squawk.application.StartupRichards >> benchmarks-run.log
echo "" >> benchmarks-run.log
echo "squawk -cp:benchmarks/j2meclasses squawk.application.StartupRichards" >> benchmarks-run.log
./squawk -cp:benchmarks/j2meclasses squawk.application.StartupRichards >> benchmarks-run.log
echo "" >> benchmarks-run.log

echo DeltaBlue benchmarking >> benchmarks-run.log
echo "java -Xint -classpath benchmarks/classes:j2me/classes squawk.application.StartupDeltaBlue" >> benchmarks-run.log
java -Xint -classpath benchmarks/classes:j2me/classes squawk.application.StartupDeltaBlue >> benchmarks-run.log
echo "" >> benchmarks-run.log
echo "squawk -cp:benchmarks/j2meclasses squawk.application.StartupDeltaBlue" >> benchmarks-run.log
./squawk -cp:benchmarks/j2meclasses squawk.application.StartupDeltaBlue >> benchmarks-run.log
echo "" >> benchmarks-run.log

echo Game of Life benchmarking >> benchmarks-run.log
echo "java -Xint -classpath benchmarks/classes:j2me/classes squawk.application.StartupLife" >> benchmarks-run.log
java -Xint -classpath benchmarks/classes:j2me/classes squawk.application.StartupLife >> benchmarks-run.log
echo "" >> benchmarks-run.log
echo "squawk -cp:benchmarks/j2meclasses squawk.application.StartupLife" >> benchmarks-run.log
./squawk -cp:benchmarks/j2meclasses squawk.application.StartupLife >> benchmarks-run.log
echo "" >> benchmarks-run.log

echo Math benchmarking >> benchmarks-run.log
echo "java -Xint -classpath benchmarks/classes:j2me/classes squawk.application.StartupMath" >> benchmarks-run.log
java -Xint -classpath benchmarks/classes:j2me/classes squawk.application.StartupMath >> benchmarks-run.log
echo "" >> benchmarks-run.log
echo "squawk -cp:benchmarks/j2meclasses squawk.application.StartupMath" >> benchmarks-run.log
./squawk -cp:benchmarks/j2meclasses squawk.application.StartupMath >> benchmarks-run.log
echo "" >> benchmarks-run.log

