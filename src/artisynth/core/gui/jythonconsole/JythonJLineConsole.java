package artisynth.core.gui.jythonconsole;

import java.io.*;
import org.python.util.*;
import org.python.core.*;

public class JythonJLineConsole extends JLineConsole {

   protected boolean myPromptSent = false;
   protected boolean myInterruptReq = false;
   protected boolean myInsideExec = false;

   public JythonJLineConsole() {
      super();
      set ("_interpreter_", this);
      set ("console", this);
      runsource (
         "_interpreter_.set ('script', console.executeScript)");
      runsource (
         "_interpreter_.set ('sleep', console.sleep)");
   }

   public void sleep (int msec) throws InterruptedException {
      Thread.sleep (msec);
   }

   public void executeScript (String fileName) throws IOException {
      boolean more = false;
      PyFile file = null;
      try {
         file = new PyFile (new FileInputStream (fileName));
      }
      catch (Exception e) {
         write ("Error opening file '"+fileName+"'\n");
         return;
      }
      // reset input buffer to clear existing "script('xxx')" input
      resetbuffer(); 
      while(true) {
         PyObject prompt = more ? systemState.ps2 : systemState.ps1;
         if (myPromptSent) {
            prompt = new PyString("");
            myPromptSent = false;
         }
         String line;
         try {
            line = raw_input(prompt, file);
         } catch(PyException exc) {
            if(!exc.match(Py.EOFError))
               throw exc;
            myPromptSent = true;
            break;
         }
         write (line+"\n");
         more = push(line);
         if (myInterruptReq) {
            break;
         }
      }
      file.close();
   }

   @Override
   public void interact(String banner, PyObject file) {
      if(banner != null) {
         write(banner);
         write("\n");
      }
      // Dummy exec in order to speed up response on first command 
      exec("2");
      // System.err.println("interp2"); 
      boolean more = false;
      while(true) {
         PyObject prompt = more ? systemState.ps2 : systemState.ps1;
         String line;
         try {
            if (file == null)
               line = raw_input(prompt);
            else
               line = raw_input(prompt, file);
         } catch(PyException exc) {
            if(!exc.match(Py.EOFError))
               throw exc;
            write("\n");
            break;
         }
         more = push(line);
         if (myInterruptReq) {
            write ("Interrupted\n");
            myInterruptReq = false;
         }
      }
   }

   @Override
   public String raw_input (PyObject prompt) {

      if (myPromptSent) {
         prompt = new PyString("");
         myPromptSent = false;
      }      
      return super.raw_input (prompt);
   }

    public void runcode(PyObject code) {
       try {
          myInsideExec = true;
          if (!myInterruptReq) {
             exec(code);
          }
       } catch (PyException exc) {
          String excStr = exc.toString();
          if (!excStr.contains ("InterruptedException") &&
              !excStr.contains ("InterruptException")) {
             if (exc.match(Py.SystemExit)) throw exc;
             showexception(exc);
          }
       }
       finally {
          myInsideExec = false;
       }
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
