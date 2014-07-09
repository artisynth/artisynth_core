Artisynth Matlab scripting howto quick an dirty

- Install  Matlab >= R14sp2  which comes with jre-1.5
-  for Linux move in Matlab7sp2/sys/os/glnx86/   libgcc and libstdc++
out of the way to use system defaults

- add jars and artisynth/src to Matlab7sp2/toolbox/local/classpath.txt
- add artisynth/src/Linux to Matlab7sp2/toolbox/local/librarypath.txt

startup Matlab  (with jvm)

m = artisynth.core.driver.Main;
m.start('Tongue3d');
r = m.getRoot
t = r.getModel('Spring Mesh');
s = m.getScheduler
s.playRequest

Here you go the tongue is simulating.

Read the Matlab external interface manual for more info
 - tab completion works
 - inspect(m) start a property editor



Enjoy
-Florian
