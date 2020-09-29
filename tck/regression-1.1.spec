pos_tck.main=com.sun.squawk.tck.Main
pos_tck.args=-cp:tck/tck-1.1.jar -o:${prefix}pos_tck @tck/positiveclasses-1.1.txt
pos_tck.classpath=.
pos_tck.exitIncludesCounts=true

neg_tck.main=com.sun.squawk.tck.Main
neg_tck.args=-cp:tck/tck-1.1.jar -o:${prefix}neg_tck -n @tck/negativeclasses-1.1.txt
neg_tck.classpath=.
neg_tck.exitIncludesCounts=true
