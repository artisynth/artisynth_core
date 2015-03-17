package artisynth.core.gui.jythonconsole;

import java.io.*;
import javax.swing.*;
import org.python.util.*;
import org.python.core.*;

public class JythonPanelConsole extends InteractiveConsole {

   protected ReadlinePanel myConsole;
   protected ReadlinePanel.OutputWriter myWriter;
   protected BufferedReader myReader;
   protected Thread myThread;
   protected boolean myPromptSent = false;
   protected boolean myInterruptReq = false;
   protected boolean myQuitReq = false;
   protected boolean myInsideExec = false;

   protected class InterruptHandler implements ReadlinePanel.InterruptHandler {

      public InterruptHandler () {
      }

      public void interrupt() {
         interruptThread();
      }
   }

   public synchronized void interruptThread() {
      myInterruptReq = true;
      if (myThread != null && myInsideExec) {
         myThread.interrupt();
      }
   }

   public synchronized void quitThread() {
      myQuitReq = true;
   }

   public synchronized void setInsideExec (boolean inside) {
      myInsideExec = inside;
   }

   public JythonPanelConsole() {
      super();

      myConsole = new ReadlinePanel();
      myConsole.setLineWrap (true);
      myWriter = new ReadlinePanel.OutputWriter(myConsole);
      myReader = myConsole.createReader();
      setErr (myWriter);
      setOut (myWriter);
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

   private static String protectWindowsSlashes(String filename) {
      StringBuilder out = new StringBuilder();
      char[] chars = filename.toCharArray();
      
      for (int idx=0; idx <chars.length; idx++) {
         if (chars[idx] == '\\' ) {
            out.append("\\\\");  // double it up
            if (idx+1 < chars.length && chars[idx+1] == '\\') {
               // skip next char if was already doubled
               idx++;
            }
         } else  {
            out.append(chars[idx]);
         }
      }
      return out.toString();
   }

   protected void requestScript (String fileName) {
      // protect single slashes
      fileName = protectWindowsSlashes(fileName);
      myConsole.setLineText ("script('"+fileName+"')");
      myConsole.acceptLine();
   }

   private class ScriptRequester implements Runnable {
      String myFileName;

      public ScriptRequester (String fileName) {
         myFileName = fileName;
      }

      public void run() {
         requestScript (myFileName);
      }
   }

   public void executeScript (String fileName) throws IOException {
      if (Thread.currentThread() == myThread) {
         doExecuteScript (fileName);
      }
      else if (!SwingUtilities.isEventDispatchThread()) {
         SwingUtilities.invokeLater (new ScriptRequester (fileName));
      }      
      else {
         requestScript (fileName);
      }
   }

   public void doExecuteScript (String fileName) throws IOException {
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
      boolean more = false;
      while(true) {
         PyObject prompt = more ? systemState.ps2 : systemState.ps1;
         if (myPromptSent) {
            prompt = new PyString("");
            myPromptSent = false;
         }
         String line = null;
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
      while(!myQuitReq) {
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

      String promptStr = prompt.toString();
      if (myPromptSent) {
         promptStr = "";
         myPromptSent = false;
      }      
      myConsole.requestInput (promptStr);
      String line;
      try {
         line = myReader.readLine();
      }
      catch (IOException e) {
         System.out.println (e);
         line = "";
      }
      return line;
   }

    public void runcode(PyObject code) {
       try {
          try {
             setInsideExec (true);
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
             setInsideExec (false);
          }
       }
       catch (Exception e) {
          // paranoid - just in case an InterruptException slips through
          // between calls to setInsideExec. Not need to do anything; just
          // catch the signal
          if (e instanceof RuntimeException) {
             // shouldn't happen ...
             throw (RuntimeException)e;
          }
          else if (!(e instanceof InterruptedException)) {
             // shouldn't happen ...
             throw new RuntimeException(e);
          }
       }
       if (myThread != null) {
          // cleaning interrupted status, in case exec was interrupted
          // but an exception was not actually thrown
          myThread.interrupted();
       }
    }

   public void interact () {
      myThread = Thread.currentThread();
      myConsole.setInterruptHandler (new InterruptHandler());
      interact (null, null);
   }

   public static void main (String[] args) {

      JythonPanelConsole console = new JythonPanelConsole();

      JFrame frame = new JFrame();
      JScrollPane pane = new JScrollPane(console.myConsole);
      pane.setVerticalScrollBarPolicy (
         ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
      pane.setHorizontalScrollBarPolicy (
         ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
      frame.getContentPane().add (pane);
      frame.pack();
      frame.setVisible (true);

      console.interact();
   }

}

