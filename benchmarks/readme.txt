Benchmarks used with Squawk: 

o Richards and DeltaBlue: from Mario's Benchmarking of Java collection at
     http://research.sun.com/people/mario/java_benchmarking/
o Game of Life
o Math (integer and long arithmetic benchmarking)

To run the benchmarks: 

o On the desktop: runs default Java VM and the Squawk VM 
  Build the squawk binary: 
     java -jar build.jar romizer 
     java -jar build.jar vm2c 
     java -jar build.jar -prod -o2 -mac rom j2me translator 
  Compile the benchmarks: 
     java -jar build.jar benchmarks
  Run the shell script benchmarks-run.sh: 
     sh benchmarks/benchmarks-run.sh
  Output will be in file benchmarks-run.log

o On the eSPOT: runs the Squawk VM
  Install a Sun SPOT SDK from 2006 that supports jar and imlets
  Compile and deploy to the eSPOT: 
     cd benchmarks
     ant jar-app
     ant jar-deploy
  Run the benchmarks:
     ant run
  Output will be displayed on the console


