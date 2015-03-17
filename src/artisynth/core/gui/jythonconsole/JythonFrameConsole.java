package artisynth.core.gui.jythonconsole;

import javax.swing.*;
import java.io.*;
import org.python.util.InteractiveInterpreter;

public class JythonFrameConsole extends JythonPanelConsole {

   private static final long serialVersionUID = 4747183591331454317L;
   JFrame myFrame;
   Thread myThread;

   public JythonFrameConsole () {
      this ("Jython Frame");
   }

   public JythonFrameConsole (String name) {
      super ();
      
      myFrame = new JFrame(name);
      JScrollPane pane = new JScrollPane(myConsole);
      pane.setVerticalScrollBarPolicy (
         ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
      pane.setHorizontalScrollBarPolicy (
         ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);      
      myFrame.getContentPane().add (pane);
      myFrame.pack();

      myThread = new Thread() {
            public void run() {
               interact ();
            }
         };
      myThread.start();
   }

   public JFrame getFrame() {
      return myFrame;
   }

   public Thread getThread() {
      return myThread;
   }

   public void setVisible (boolean visible) {
      myFrame.setVisible (true);
   }         

   public boolean isVisible () {
      return myFrame.isVisible();
   }         

   public void dispose() {
      if (myFrame != null) {
         myFrame.dispose();
      }
      if (myThread != null) {
         if (myThread == Thread.currentThread()) {
            quitThread();
         }
         else {
            myThread.stop();
         }
      }
   }

   public static void main (String[] args) {
      JythonFrameConsole frame = new JythonFrameConsole ("Jython Frame");
      frame.setVisible (true);
   }
}
