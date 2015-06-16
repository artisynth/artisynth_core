package artisynth.core.gui.jythonconsole;

import org.python.util.*;
import org.python.core.*;

import maspack.util.InternalErrorException;
import artisynth.core.driver.Main;
import artisynth.core.util.JythonInit;
import java.io.*;
import javax.swing.JFrame;

public class ArtisynthJythonConsole {

   protected InteractiveConsole myConsole;

   private ArtisynthJythonConsole (InteractiveConsole console) {
      myConsole = console;
      PyStringMap locals = (PyStringMap)myConsole.getLocals();
      locals.update (JythonInit.getArtisynthLocals());
   }

   /**
    * Return true if it appears that stdout has been routed to a file.
    */
   private static boolean outputDirectedToFile() {
      if (Py.defaultSystemState == null) {
         PySystemState.initialize();
      }
      return !((PyFile)Py.defaultSystemState.stdout).isatty();
   }

   public static ArtisynthJythonConsole createTerminalConsole() {
      // If stdout has been routed to a file, create a ythonInteractiveConsole
      // instead of a JythonJLineConsole, since the latter will hang (trying to
      // set up non-buffered terminal input) if run as a background task.
      InteractiveConsole console;
      if (outputDirectedToFile()) {
         console = new JythonInteractiveConsole();
      }
      else {
         console = new JythonJLineConsole();
      }
      return new ArtisynthJythonConsole (console);
   }

   public static ArtisynthJythonConsole createFrameConsole() {
      InteractiveConsole console = new JythonFrameConsole();
      return new ArtisynthJythonConsole (console);
   }

   public void setMain (Main main) {
      myConsole.set ("main", main);
      myConsole.set ("sel", main.getSelectionManager().getCurrentSelection()); 
   }

   public void execfile (InputStream s, String name) {
      myConsole.execfile (s, name);
   }

   public void executeScript (String fileName) throws IOException {
      if (myConsole instanceof JythonFrameConsole) {
         ((JythonFrameConsole)myConsole).executeScript (fileName);
      }
      else if (myConsole instanceof JythonJLineConsole) {
         ((JythonJLineConsole)myConsole).executeScript (fileName);
      }
      else if (myConsole instanceof JythonInteractiveConsole) {
         ((JythonInteractiveConsole)myConsole).executeScript (fileName);
      }
      else {
         throw new InternalErrorException (
            "Unknown console type: "+myConsole.getClass());
      }
   }       

   public void interact () {
      myConsole.interact (null, null);
   }

   public InteractiveConsole getConsole() {
      return myConsole;
   }

   public JFrame getFrame() {
      if (myConsole instanceof JythonFrameConsole) {
         return ((JythonFrameConsole)myConsole).getFrame();
      }
      else {
         return null;
      }
   }

   public void dispose() {
      if (myConsole instanceof JythonFrameConsole) {
         ((JythonFrameConsole)myConsole).dispose();
         myConsole = null;
      }
   }

   public static void main (String[] args) {
      
   }

}
