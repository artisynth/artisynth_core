package artisynth.demos.test;

import java.io.*;
import artisynth.core.driver.*;

public class TaskManagerTest extends TaskManager {
   
   public void run() {
      Main main = getMain();
      String[] args = getArgs();
      System.out.print ("args: [ ");
      for (int i=0; i<args.length; i++) {
         System.out.print (args[i] + " ");
      }
      System.out.println ("]");

      // mask focus stealing so ArtiSynth won't steal the focus while we are
      // trying to do other things on the desktop
      main.maskFocusStealing (true);
      int cnt = 10;

      for (int i=0; i<cnt; i++) {
         main.loadModel (
            "artisynth.demos.mech.SpringMeshDemo", "SpringMeshDemo", null);
         main.play (2.0);
         main.waitForStop();
         main.reset();
         main.loadModel (
            "artisynth.demos.mech.RigidBodyDemo", "RigidBodyDemo", null);
         main.play (2.0);
         main.waitForStop();
         main.reset();
      }
   }
   
}
