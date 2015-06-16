package artisynth.core.gui.jythonconsole;

import java.io.*;
import org.python.util.*;
import org.python.core.*;

/**
 * Common attributes and structures used in the ArtiSynth extensions to
 * InteractiveConsole.
 */
public class JythonConsoleImpl {

   protected boolean myPromptSent = false;
   protected boolean myInterruptReq = false;
   protected boolean myInsideExec = false;
   protected boolean myQuitReq = false;

   protected InteractiveConsole myConsole;

   JythonConsoleImpl (InteractiveConsole console) {
      myConsole = console;
   }

   synchronized void requestQuit () {
      myQuitReq = true;
   }

   public synchronized void setInsideExec (boolean inside) {
      myInsideExec = inside;
   }

   public boolean isInsideExec() {
      return myInsideExec;
   }

   void setupSymbols() {
      myConsole.set ("_interpreter_", myConsole);
      myConsole.set ("console", myConsole);
      myConsole.runsource (
         "_interpreter_.set ('script', console.executeScript)");
      myConsole.runsource (
         "_interpreter_.set ('sleep', console.sleep)");
      myConsole.runsource (
         "_interpreter_.set ('exit', console.exit)");
   }

   void executeScript (String fileName) throws IOException {
      boolean more = false;
      PyFile file = null;
      try {
         file = new PyFile (new FileInputStream (fileName));
      }
      catch (Exception e) {
         myConsole.write ("Error opening file '"+fileName+"'\n");
         return;
      }
      // reset input buffer to clear existing "script('xxx')" input
      myConsole.resetbuffer(); 
      while(!myQuitReq) {
         PySystemState state = myConsole.getSystemState();
         PyObject prompt = more ? state.ps2 : state.ps1;
         if (myPromptSent) {
            prompt = new PyString("");
            myPromptSent = false;
         }
         String line = null;
         try {
            line = myConsole.raw_input(prompt, file);
         } catch(PyException exc) {
            if(!exc.match(Py.EOFError))
               throw exc;
            myPromptSent = true;
            break;
         }
         myConsole.write (line+"\n");
         more = myConsole.push(line);
         if (myInterruptReq) {
            break;
         }
      }
      file.close();
   }

   PyObject killRedundantPrompt (PyObject prompt) {
      if (myPromptSent) {
         myPromptSent = false;
         return new PyString("");
      }       
      else {
         return prompt;
      }
   }

   public void interact(String banner, PyObject file) {
      if (banner != null) {
         myConsole.write(banner);
         myConsole.write("\n");
      }
      // Dummy exec in order to speed up response on first command 
      myConsole.exec("2");
      // System.err.println("interp2"); 
      boolean more = false;
      while(true) {
         PySystemState state = myConsole.getSystemState();
         PyObject prompt = more ? state.ps2 : state.ps1;
         String line;
         try {
            if (file == null)
               line = myConsole.raw_input(prompt);
            else
               line = myConsole.raw_input(prompt, file);
         } catch(PyException exc) {
            if(!exc.match(Py.EOFError))
               throw exc;
            myConsole.write("\n");
            break;
         }
         more = myConsole.push(line);
         if (myInterruptReq) {
            myConsole.write ("Interrupted\n");
            myInterruptReq = false;
         }
      }
   }

   public void runcode(PyObject code) {
      try {
         setInsideExec (true);
         if (!myInterruptReq) {
            myConsole.exec(code);
         }
      } catch (PyException exc) {
         String excStr = exc.toString();
         if (!excStr.contains ("InterruptedException") &&
             !excStr.contains ("InterruptException")) {
            if (exc.match(Py.SystemExit)) throw exc;
            myConsole.showexception(exc);
         }
      }
      finally {
         setInsideExec (false);
      }
   }   

   /**
    * Return true if it appears that stdout has been routed to a file.  In that
    * case, we may want to invoke a JythonInteractiveConsole instead of a
    * JythonJLineConsole, since the latter will hang (trying to set up
    * non-buffered terminal input) if run as a background task.
    */
   public static boolean outputDirectedToFile() {
      if (Py.defaultSystemState == null) {
         PySystemState.initialize();
      }
      return ((PyFile)Py.defaultSystemState.stdout).isatty();
   }

}
