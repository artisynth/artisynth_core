package artisynth.core.gui.jythonconsole;

import org.python.util.*;
import org.python.core.*;

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

   public static ArtisynthJythonConsole createJLineConsole() {
      return new ArtisynthJythonConsole (new JythonJLineConsole());
   }

   public static ArtisynthJythonConsole createFrameConsole() {
      return new ArtisynthJythonConsole (new JythonFrameConsole());
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

   public static void main (String[] args) {
      
   }

}
