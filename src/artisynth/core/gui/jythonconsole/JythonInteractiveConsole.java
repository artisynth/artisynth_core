package artisynth.core.gui.jythonconsole;

import java.io.*;
import org.python.util.*;
import org.python.core.*;

/**
 * A Jython console connected directly to console I/O, without any editing
 * features (as opposed to JythonJLineConsole, which does have editing).  This
 * is useful for running tasks in the background, since JythonJLineConsole
 * adjusts the console terminal to permit unbuffered input, which can result in
 * "tty output" suspensions for background tasks.
 */
public class JythonInteractiveConsole extends InteractiveConsole {

   JythonConsoleImpl myImpl;

   public JythonInteractiveConsole() {
      super();
      myImpl = new JythonConsoleImpl (this);
      myImpl.setupSymbols();
   }

   public void sleep (int msec) throws InterruptedException {
      Thread.sleep (msec);
   }

   public void exit (int code) {
      System.exit (code);
   }

   public void executeScript (String fileName) throws IOException {
      System.out.println ("executing " + fileName);
      myImpl.executeScript (fileName);
   }

   @Override
   public void interact(String banner, PyObject file) {
      myImpl.interact (banner, file);
   }

   @Override
   public String raw_input (PyObject prompt) {
      prompt = myImpl.killRedundantPrompt (prompt);
      return super.raw_input (prompt);
   }

   @Override
   public void runcode(PyObject code) {
      myImpl.runcode (code);
   }

   public static void main (String[] args) {

      PySystemState.initialize();
      System.out.println (
         "interactive=" + ((PyFile)Py.defaultSystemState.stdout).isatty());

      JythonInteractiveConsole console = new JythonInteractiveConsole();

      try {
         console.executeScript ("test2.py");
         console.interact (null, null);
      }
      catch (Exception e) {
         System.out.println (e);
      }
   }

}
