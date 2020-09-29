# This script should be run as follows:
# 1, delete /define/src/com/sun/squawk/vm/Native.java
# 2, bld j2me
# 3, run this script
# 4, bld j2me a second time

javac NativeGen.java
java -cp .:../j2me/classes NativeGen 0 > tmp && chmod -w tmp && mv tmp ../j2me/src/com/sun/squawk/vm/Native.java
java -cp .:../j2me/classes NativeGen 1 > tmp && chmod -w tmp && mv tmp ../vmgen/src/com/sun/squawk/vm/InterpreterNative.java
java -cp .:../j2me/classes NativeGen 2 > tmp && chmod -w tmp && mv tmp ../translator/src/com/sun/squawk/translator/ir/verifier/NativeVerifierHelper.java
rm *.class
