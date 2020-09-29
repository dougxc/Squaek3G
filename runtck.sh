#Recommended options to build squawk executable for running TCK
#java -jar build.jar -prod -o2 -mac rom j2me translator tck

if [ "x$1" = "x1.0" ]; then
    POS_LIST=tck/positiveclasses.txt
    NEG_LIST=tck/negativeclasses.txt
    TCK_JAR=tck/tck.jar
else
    POS_LIST=tck/positiveclasses-1.1.txt
    NEG_LIST=tck/negativeclasses-1.1.txt
    TCK_JAR=tck/tck-1.1.jar
fi

rm -f neg_tck.output.log neg_tck.passed.log neg_tck.failed.log
rm -f pos_tck.output.log pos_tck.passed.log pos_tck.failed.log

cat $NEG_LIST | while read f; do
    cmd="squawk $SQUAWKVM_FLAGS -cp:$TCK_JAR $f"
    echo $cmd >> neg_tck.output.log
    eval $cmd >> neg_tck.output.log 2>&1
    
    # Most negative tests should exit with code 97. However, due to the eager nature
    # of class loading and verification in Squawk, some tests will exit with
    # an uncaught verification/linkage exception (i.e. exit code 1) that would otherwise
    # have been caught within a try-catch block if class loading was more lazy.				
    if [ $? -eq 1 -o $? -eq 97 ]; then
        echo $f >> neg_tck.passed.log
    else
        echo $f >> neg_tck.failed.log
    fi
    echo "" >> neg_tck.output.log
done

cat $POS_LIST | while read f; do
    cmd="squawk $SQUAWKVM_FLAGS -cp:$TCK_JAR $f"
    echo $cmd >> pos_tck.output.log
    eval $cmd >> pos_tck.output.log 2>&1
    if [ $? -eq 95 ]; then
        echo $f >> pos_tck.passed.log
    else
        echo $f >> pos_tck.failed.log
    fi
    echo "" >> pos_tck.output.log
done


