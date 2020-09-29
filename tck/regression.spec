pos_tck.main=com.sun.squawk.tck.Main
pos_tck.args=-o:${prefix}pos_tck @tck/positiveclasses.txt
pos_tck.classpath=.
pos_tck.exitIncludesCounts=true

neg_tck.main=com.sun.squawk.tck.Main
neg_tck.args=-o:${prefix}neg_tck -n @tck/negativeclasses.txt
neg_tck.classpath=.
neg_tck.exitIncludesCounts=true
