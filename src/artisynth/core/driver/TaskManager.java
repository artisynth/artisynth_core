package artisynth.core.driver;

/**
 * Base class for a thread that can be run to provide executive control of
 * simulations.
 */
public class TaskManager extends Thread {

   Main myMain;
   String[] myArgs = new String[0];

   public Main getMain() {
      return myMain;
   }

   void setMain (Main main) {
      myMain = main;
   }

   public String[] getArgs() {
      return myArgs;
   }

   void setArgs (String[] args) {
      myArgs = new String[args.length];
      for (int i=0; i<args.length; i++) {
         myArgs[i] = args[i];
      }
   }
   
}
