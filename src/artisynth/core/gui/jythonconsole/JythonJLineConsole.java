package artisynth.core.gui.jythonconsole;

import java.io.*;
import org.python.util.*;
import org.python.core.*;

/**
 * A Jython console connected directly to console I/O and which provides
 * interactive emacs-style editing based on JLine.
 */
public class JythonJLineConsole extends JLineConsole {

   JythonConsoleImpl myImpl;

   public JythonJLineConsole() {
      // In Jython 2.7, JLineConsole doesn't have a no-args constructor
      super(null);
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

    public void runcode(PyObject code) {
       myImpl.runcode (code);
    }

   public static void main (String[] args) {

      JythonJLineConsole console = new JythonJLineConsole();

      try {
         console.executeScript ("test2.py");
         console.interact (null, null);
      }
      catch (Exception e) {
         System.out.println (e);
      }
   }

}
