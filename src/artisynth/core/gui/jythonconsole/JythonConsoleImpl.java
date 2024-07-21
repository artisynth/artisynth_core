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
   protected int myExecLevel = 0;
   protected boolean myQuitReq = false;

   protected InteractiveConsole myConsole;
   protected boolean myUsingJLine;

   JythonConsoleImpl (InteractiveConsole console, boolean usingJLine) {
      myConsole = console;
      myUsingJLine = usingJLine;
   }

   synchronized void requestQuit () {
      myQuitReq = true;
   }

   synchronized void requestInterrupt () {
      myInterruptReq = true;
   }
   
   boolean interruptRequestPending() {
      return myInterruptReq;
   }

   public synchronized void raiseExecLevel () {
      myExecLevel++;
   }

   public synchronized void lowerExecLevel () {
      myExecLevel = Math.max (--myExecLevel, 0); // paranoid
   }

   public synchronized boolean isInsideExec() {
      return myExecLevel > 0;
   }

   public void sleep (int msec) throws InterruptedException {
      Thread.sleep (msec);
   }

   public void exit (int code) {
      System.exit (code);
   }

   boolean isBlank(String str) {
      for (int i=0; i<str.length(); i++) {
         if (!Character.isWhitespace(str.charAt(i))) {
            return false;
         }
      }
      return true;
   }

   void setupSymbols() {
      myConsole.set ("_interpreter_", myConsole);
      myConsole.set ("console", myConsole);
      myConsole.set ("consoleImpl", this);
      myConsole.runsource (
         "_interpreter_.set ('script', consoleImpl.executeScript)");
      myConsole.runsource (
         "_interpreter_.set ('sleep', consoleImpl.sleep)");
      myConsole.runsource (
         "_interpreter_.set ('exit', consoleImpl.exit)");
   }

   public void executeScript (String fileName) {
      boolean more = false;
      PyFile file = null;
      try {
         file = new PyFile (new FileInputStream (fileName));
      }
      catch (Exception e) {
         myConsole.write ("Error opening file '"+fileName+"'\n");
         return;
      }
      // Use a try/catch block to make sure we catch any exceptions
      // and hence close the file and clear myInsideScript
      boolean lastLineBlank = false;
      try {
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
               if (myUsingJLine && lastLineBlank) {
                  // XXX blank or commented lines at the end of the script seem
                  // to generate redundant prompt outputs in
                  // JLineConsole. Printing a newline and prompt seems to
                  // suppress this, at the cost of a singke extra prompt line
                  System.out.print ("\n" + state.ps1);
               }
               myPromptSent = true;
               break;
            }
            //System.out.print ("w"+cnt+" ");
            myConsole.write (line+"\n");
            more = myConsole.push(line);
            if (isBlank(line)) {
               lastLineBlank = true;
            }
            if (myInterruptReq) {
               break;
            }
         }
      }
      catch (Exception e) {
         throw e;
      }
      finally {
         file.close();
      }
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
         // Jython 2.7 seemed to have empty values in state.ps1,ps2
         //PyObject prompt = more ? new PyString("... ") : new PyString(">>> ");
         PyObject prompt = more ? state.ps2 : state.ps1;
         String line;
         try {
            if (file == null) {
               line = myConsole.raw_input(prompt);
            }
            else {
               line = myConsole.raw_input(prompt, file);
            }
         } catch(PyException exc) {
            if(!exc.match(Py.EOFError))
               throw exc;
            myConsole.write("\n");
            break;
         }
         more = myConsole.push(line);
         if (myInterruptReq) {
            myConsole.write ("Interrupted " + myExecLevel + "\n");
            myInterruptReq = false;
         }
      }
   }

   public void runcode(PyObject code) {
      try {
         raiseExecLevel();
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
         lowerExecLevel();
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
