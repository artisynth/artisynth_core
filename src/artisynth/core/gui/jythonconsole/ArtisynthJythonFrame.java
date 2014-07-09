package artisynth.core.gui.jythonconsole;

import artisynth.core.driver.Main;
import artisynth.core.util.*;
import org.python.util.InteractiveInterpreter;
import org.python.core.*;
import java.io.*;

public class ArtisynthJythonFrame extends JythonFrame {
   private static final long serialVersionUID = 7237956997389820537L;
   protected Main myMain;

   public ArtisynthJythonFrame (String name) {
      super (name);
      PyStringMap locals = (PyStringMap)getInterpreter().getLocals();
      locals.update (JythonInit.getArtisynthLocals());
   }

   public void setMain (Main main) {
      InteractiveInterpreter interp = getInterpreter();
      myMain = main;
      interp.set ("main", myMain);
      //      interp.set ("_interpreter_", interp);
      interp.set ("sel", myMain.getSelectionManager().getCurrentSelection());
   }

   public void execfile (InputStream s, String name) {
      getInterpreter().execfile (s, name);
   }

   public static void main (String[] args) {
      ArtisynthJythonFrame frame = new ArtisynthJythonFrame ("Jython Console");
      frame.setVisible (true);
   }

}
