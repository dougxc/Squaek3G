JAVAC=javac
# Some installations of CYGWIN can hava GNU jar installed and is not compatible,
# so provide direct access to the one in JAVA_HOME
JAR=jar
if [ $# -gt 0 ]; then
    JAVAC=$1/bin/javac
    JAR=$1/bin/jar
fi;

rm -rf classes
mkdir classes
$JAVAC -target 1.4 -source 1.4 -classpath tools.jar -d classes -g `find src -name '*.java'`
echo "Main-Class: com.sun.squawk.builder.Build" > mf
mkdir doclet
cd doclet
$JAR xf ../tools.jar com/sun/javadoc/
cd ..
$JAR cfm ../build.jar mf -C classes . -C doclet com/sun/javadoc/
rm -rf mf doclet
