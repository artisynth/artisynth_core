package artisynth.core.gui.jythonconsole;

import org.python.util.*;
import org.python.core.*;
import org.python.jline.console.UserInterruptException;

import maspack.util.InternalErrorException;
import artisynth.core.driver.Main;
import artisynth.core.util.JythonInit;
import artisynth.core.util.ArtisynthPath;
import java.io.*;
import java.util.Properties;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class ArtisynthJythonConsole {

   protected InteractiveConsole myConsole;
   protected File myScriptFile = null;

   private ArtisynthJythonConsole (InteractiveConsole console) {
      myConsole = console;
      PyStringMap locals = (PyStringMap)myConsole.getLocals();
      locals.update (JythonInit.getArtisynthLocals());
   }
   
   private class ScriptRequester implements Runnable {
      String myFileName;
      String[] myArgs;

      public ScriptRequester (String fileName, String[] args) {
         myFileName = fileName;
         myArgs = args;
      }

      public void run() {
         ((JythonFrameConsole)myConsole).requestScript (myFileName, myArgs);
      }
   }

   /**
    * Return true if it appears that stdout has been routed to a file.
    */
   private static boolean outputDirectedToFile() {
      return System.console() == null;
   }

   public static ArtisynthJythonConsole createTerminalConsole() {
      Properties props = JythonInit.createDefaultProperties();
      boolean usingJLine = false;
      if (!outputDirectedToFile()) {
         // If stdout is not routed to a file, then we can instruct Jython to
         // initialize with a JLineConsole, which allows editing. Otherwise, we
         // use a PlainConsole, since JLineConsole may hang (trying to set up
         // non-buffered terminal input) if run as a background task.
         props.setProperty (
            RegistryKey.PYTHON_CONSOLE, "org.python.util.JLineConsole");
         usingJLine = true;
      }
      PySystemState.initialize(null, props);         
      InteractiveConsole console = new JythonInteractiveConsole(usingJLine);
      PySystemState state = Py.getSystemState();
      return new ArtisynthJythonConsole (console);
   }

   public static ArtisynthJythonConsole createFrameConsole() {
      Properties props = JythonInit.createDefaultProperties();
      PySystemState.initialize(null, props);         
      InteractiveConsole console = new JythonFrameConsole();
      return new ArtisynthJythonConsole (console);
   }

   public void setMain (Main main) {
      myConsole.set ("main", main);
      myConsole.set ("sel", main.getSelectionManager().getCurrentSelection());
   }
   
   public File getScriptFile() {
      return myScriptFile;
   }

   public void execfile (InputStream s, String name) {
      myConsole.execfile (s, name);
   }

   public void executeScript (String fileName) throws IOException {
      executeScript(fileName, null);
   }

   public void executeScript (String fileName, String[] args) throws IOException {

      if (File.separatorChar == '\\') {
         // Convert '\' to '/' on Windows for better readablity
         fileName = fileName.replace ('\\', '/');
      }

      if (myConsole instanceof JythonFrameConsole) {
         JythonFrameConsole jfc = (JythonFrameConsole)myConsole;
         if (jfc.getThread() != Thread.currentThread()) {
            if (!SwingUtilities.isEventDispatchThread()) {
               SwingUtilities.invokeLater (
                  new ScriptRequester (fileName, args));
            }      
            else {
               jfc.requestScript (fileName, args);
            }
            return;
         }
      }
      if (args == null) {
         args = new String[0];
      }

      // pass in sys arguments through this dummy object
      myConsole.exec(
         "import sys\n" +
         "class ArgSetter:\n"+
         "    def __init__(self, vargs):\n"+
         "        self.vargs = vargs\n"+
         "        sys.argv = vargs\n");
      PyObject argClass = myConsole.get("ArgSetter");
      PyObject[] pyargs = new PyObject[args.length];
      for (int i=0; i<args.length; ++i) {
         pyargs[i] = new PyString(args[i]);
      }
      PyList pylist = new PyList(pyargs);
      argClass.__call__(pylist);
      
      
      myScriptFile = new File(fileName);
      try {
         //myConsole.execfile(fileName);
         if (myConsole instanceof JythonFrameConsole) {
            ((JythonFrameConsole)myConsole).executeScript (fileName);
         }
         else if (myConsole instanceof JythonInteractiveConsole) {
            ((JythonInteractiveConsole)myConsole).executeScript (fileName);
         }
         else {
            throw new InternalErrorException (
               "Unknown console type: "+myConsole.getClass());
         }
      }
      catch (Exception e) {
         throw e;
      }
      finally {
         myScriptFile = null;
      }
   }       

   public void interact () {
      try {
         myConsole.interact (null, null);
      }
      catch (UserInterruptException e) {
         // If JLineConsole is used, a UserInterruptException may get thrown.
         // Just ignore and return.
      }
   }

   public InteractiveConsole getConsole() {
      return myConsole;
   }

   public JFrame getFrame() {
      if (myConsole instanceof JythonFrameConsole) {
         return ((JythonFrameConsole)myConsole).getFrame();
      }
      else {
         return null;
      }
   }

   public void dispose() {
      if (myConsole instanceof JythonFrameConsole) {
         ((JythonFrameConsole)myConsole).dispose();
         myConsole = null;
      }
   }

   public boolean requestInterrupt() {
      if (myConsole instanceof JythonFrameConsole) {
         return ((JythonFrameConsole)myConsole).interruptThread();
      }
      else {
         return false;
      }
   }
   
   public boolean interruptRequestPending() {
      if (myConsole instanceof JythonFrameConsole) {
         return ((JythonFrameConsole)myConsole).interruptRequestPending();
      }     
      else {
         return false;
      }
   }
   
   public static void main (String[] args) {
      
   }

}
