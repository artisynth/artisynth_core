package artisynth.core.gui.jythonconsole;

import java.io.*;
import org.python.util.*;
import org.python.core.*;

/**
 * A Jython console connected directly to console I/O and which provides
 * interactive emacs-style editing based on JLine.
 */
public class JythonJLineConsole extends InteractiveConsole {

   JythonConsoleImpl myImpl;

   public JythonJLineConsole() {
      try {
         Py.installConsole (new JLineConsole(null));
      }
      catch (IOException e) {
         System.out.println ("ERROR: cannot install JLineConsole: " + e);
      }
      InteractiveConsole iconsole = new InteractiveConsole();
      PySystemState state = iconsole.getSystemState();
      state.ps1 = new PyString(">>> ");
      state.ps2 = new PyString("... ");    
      myImpl = new JythonConsoleImpl (iconsole, /*usingJLine=*/true);
      myImpl.setupSymbols();
   }

   public void executeScript (String fileName) throws IOException {
      myImpl.executeScript (fileName);
   }

   public void interact(String banner, PyObject file) {
      myImpl.interact (banner, file);
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
