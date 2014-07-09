package artisynth.core.gui.jythonconsole;

import javax.swing.*;
import java.io.*;
import org.python.util.InteractiveInterpreter;

public class JythonFrame extends JFrame {
   private static final long serialVersionUID = 4747183591331454317L;
   JythonConsole myConsole;

   public JythonFrame (String name) {
      super (name);
      myConsole = new JythonConsole();
      myConsole.setLineWrap (true);
      JScrollPane pane = new JScrollPane(myConsole);
      pane.setVerticalScrollBarPolicy (
         ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
      pane.setHorizontalScrollBarPolicy (
         ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);      
      getContentPane().add (pane);
      pack();
   }

   // not currently used
   public boolean requestAction (Runnable action) {
      return myConsole.requestAction (action);
   }

   public void executeScript (String fileName) throws IOException {
      myConsole.executeScript (fileName);
   }

   public void abortScript () {
      myConsole.abort ();
   }

   public void setInterpreter (InteractiveInterpreter interp) {
      myConsole.setInterpreter (interp);
   }

   public InteractiveInterpreter getInterpreter() {
      return myConsole.getInterpreter();
   }

   public static void main (String[] args) {
      JythonFrame frame = new JythonFrame ("Jython Console");
      frame.setVisible (true);
   }
}
