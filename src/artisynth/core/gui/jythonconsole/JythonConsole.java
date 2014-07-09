package artisynth.core.gui.jythonconsole;

import org.python.util.InteractiveInterpreter;
import org.python.core.*;

import java.io.*;

import javax.swing.*;

import java.util.*;

import maspack.util.*;

public class JythonConsole extends ReadlineConsole {
   private static final long serialVersionUID = -2462143623782010769L;
   InteractiveInterpreter myInterp;
   InterpreterThread myInterpThread;
   CompilerFlags myCflags = new CompilerFlags();
   CompileMode myCmode = CompileMode.getMode ("single");

   PipedWriter myCommandWriter;

   Writer myCout = new OutputWriter();
   Writer myCerr = new OutputWriter();

   String myBuffer;

   private class OutputWriter extends Writer {

      StringBuilder builder = new StringBuilder();

      public void close() {
         flush();
      }

      public void flush() {
         if (builder.length() > 0) {
            appendText (builder.toString());
         }
      }

      public void write (char[] cbuf, int off, int len) {
         for (int i=0; i<len; i++) {
            char c = cbuf[off+i];
            builder.append (c);
            if (c == '\n') {
               appendText (builder.toString());
               builder.setLength (0);
            }
         }
      }
   }

   public JythonConsole() {
      super();
      myInterp = new InteractiveInterpreter();
      myInterp.setOut (myCout);
      myInterp.setErr (myCerr);
      myInterp.set ("_interpreter_", myInterp);
      setLineWrap (true);
      myCommandWriter = new PipedWriter();
      BufferedReader interpInput = null;
      try {
         interpInput = new BufferedReader (new PipedReader (myCommandWriter));
      }
      catch (Exception e) {
         throw new InternalErrorException (
            "Can't create BufferedReader: " + e);
      }
      myInterpThread = new InterpreterThread (interpInput, myCout, myCerr);
      myInterpThread.start();
   }

   private class InterpreterThread extends Thread {

      LinkedList<Runnable> myActions = new LinkedList<Runnable>();
      BufferedReader myConsoleInput;
      BufferedReader myInput;
      LinkedList<BufferedReader> myInputStack;
      Writer myConsoleOutput;
      Writer myConsoleError;
      boolean myStopReq = false;
      PyException myLastException = null;

      private void processActions() {
         while (myActions.size() > 0) {
            myActions.poll().run();
         }
      }

      private void writeConsoleOut (String str) {
         try {
            myConsoleOutput.write (str);
         }
         catch (Exception e) {
            //can't happen
         }
      }

      private void writeConsoleErr (String str) {
         try {
            myConsoleError.write (str);
         }
         catch (Exception e) {
            //can't happen
         }
      }

      public InterpreterThread (BufferedReader input, Writer output, Writer err) {
         myConsoleInput = input;
         myConsoleOutput = output;
         myConsoleError = err;
         myInput = input;
         myInputStack = new LinkedList<BufferedReader>();
      }

      private void popInputStream() {
         if (myInputStack.size() > 0) {
            myInput = myInputStack.removeLast();
         }
      }

      private void abortScripts() {
         while (myInput != myConsoleInput) {
            try {
               myInput.close();
            }
            catch (Exception e) {
            }
            popInputStream();
         }
      }

      protected void executeScript (String fileName) throws IOException {
         executeScript (new FileReader (fileName));
      }

      public void executeScript (Reader reader) throws IOException {
         myInputStack.add (myInput);
         myInput = new BufferedReader (reader);
      }

      public boolean runsource (String source) {
         PyObject code;
         myLastException = null;
         try {
            code = Py.compile_command_flags(
               source, "<input>", myCmode, myCflags, true);
         } catch (PyException exc) {
            myLastException = exc;
            if (exc.match(Py.SyntaxError)) {
               // Case 1
               writeConsoleErr (exc.toString());
               //myInterp.showexception(exc);
               return false;
               
            } else if (exc.match(Py.ValueError) ||
                       exc.match(Py.OverflowError)) {
               // Should not print the stack trace, just the error.
               writeConsoleErr (exc.toString());
               return false;
            } else {
               throw exc;
            }
         }
         // Case 2
         if (code == Py.None)
            return true;
         // Case 3
         try {
            myInterp.exec(code);
         } catch (PyException exc) {
            myLastException = exc;
            if (exc.match(Py.SystemExit)) throw exc;
            writeConsoleErr (exc.toString());
         }
         return false;
      }

      private boolean isCommentLine (String str) {
         for (int i=0; i<str.length(); i++) {
            char c = str.charAt(i);
            if (!Character.isWhitespace (c)) {
               return c == '#';
            }
         }
         return false;
      }
      
      private boolean isBlankLine (String str) {
         for (int i=0; i<str.length(); i++) {
            char c = str.charAt(i);
            if (!Character.isWhitespace (c)) {
               return false;
            }
         }
         return true;
      }

      // console.executeScript ('test.py')
      public void run() {

         while (true) {
            String fullSource = null;
            boolean needMoreInput = true;
            while (needMoreInput) {
               String line = null;
               try {
                  line = myInput.readLine();
               }
               catch (Exception e) {
                  // XXX handle reader exception here
               }
               if (line == null) {
                  popInputStream();
                  break;
               }
               if (myInput != myConsoleInput) {
                  writeConsoleOut (line + "\n");
               }
               if (fullSource == null) {
                  fullSource = line;
               }
               else {
                  if (isCommentLine (line)) {
                     // hack: replace comment line with blank since the
                     // compiler doesn't seem to know how to handle it
                     line = "";
                  } else if (isBlankLine(line)) {
                     line = "";
                  }
                  fullSource = fullSource + "\n" + line;
               }
               needMoreInput = runsource (fullSource);
               processActions();
               if (myStopReq) {
                  writeConsoleErr ("Aborted\n");
               }
               if (needMoreInput) {
                  setPrompt ("... ");
               }
               else {
                  setPrompt (">>> ");
               }
               initializeNewLine (line);
               if (myStopReq || myLastException != null) {
                  abortScripts();
                  needMoreInput = false;
                  myStopReq = false;
               }
            }
         }
      }
   }

   private boolean isScriptAlive() {
      return false;
      //return myScriptThread != null && myScriptThread.myAlive;
   }

   // This is not currently used. It was used by the workspace to
   // execute update events back when the Jython console was
   // executed in the GUI thread.
   public synchronized boolean requestAction (Runnable action) {
      if (Thread.currentThread() == myInterpThread) {
         return myInterpThread.myActions.offer (action);
      }
      else {
         return false;
      }
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
   
   public void executeScript (String fileName) throws IOException {
      if (Thread.currentThread() == myInterpThread) {
         myInterpThread.executeScript (fileName);
      }
      else {
         // protect single slashes
         fileName = protectWindowsSlashes(fileName);
         
         appendText ("script('"+fileName+"')");
         acceptLine();
      }
   }

   public void test () throws IOException {
      executeScript ("test.py");
   }

   public void test2 () throws IOException {
      executeScript ("test2.py");
   }

//    public synchronized void executeScript (Reader reader) throws IOException {
//       if (Thread.currentThread() == myInterpThread) {
//          myInterpThread.executeScript (reader);         
//       }
//       else {
         
//       }
      
//    }

   @Override
      protected void interrupt() {
      abort ();
   }

   public synchronized void abort() {
      if (myInterpThread != null) {
         myInterpThread.myStopReq = true;
      }
   }

   public void setInterpreter (InteractiveInterpreter interp) {
      myInterp = interp;
   }

   public InteractiveInterpreter getInterpreter() {
      return myInterp;
   }

   protected void resetBuffer() {
      myBuffer = null;
   }

   protected String getBuffer() {
      return myBuffer;
   }

   protected void appendToBuffer (String str) {
      if (myBuffer == null) {
         myBuffer = str;
      }
      else {
         myBuffer = myBuffer + "\n" + str;
      }
   }

   // we synchronize acceptLine so that we can ensure that scripts
   // don't start running while we are still in this method
   protected synchronized void acceptLine() {
      String line = getLineText (getCaretPosition());
      appendText ("\n");
      try {
         myCommandWriter.write (line + "\n");
         myCommandWriter.flush();
      }
      catch (Exception e) {
         throw new InternalErrorException (
            "Error writing to commandWriter: " + e);
      }
   }


   public String processLine (String str) {
      try {
         myCommandWriter.write (str + "\n");
         myCommandWriter.flush();
      }
      catch (Exception e) {
         throw new InternalErrorException (
            "Error writing to commandWriter: " + e);
      }
      return null;
   }

   public static void main (String[] args) {
      JFrame frame = new JFrame ("console");

      JythonConsole console = new JythonConsole();
      console.setLineWrap (true);
      console.myInterp.set ("console", console);
      console.myInterp.runsource (
         "_interpreter_.set ('script', console.executeScript)");
      JScrollPane pane = new JScrollPane(console);
      pane.setVerticalScrollBarPolicy (
         ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
      pane.setHorizontalScrollBarPolicy (
         ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
      frame.getContentPane().add (pane);
      frame.pack();
      frame.setVisible (true);
   }
}
