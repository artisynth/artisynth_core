package artisynth.demos.tutorial;

import java.io.IOException;
import artisynth.core.gui.*;

public class SimpleMuscleWithPanel extends SimpleMuscle
{

   ControlPanel panel;

   public void build (String[] args) throws IOException {

      super.build (args);

      // add control panel to control muscle

      panel = new ControlPanel();
      panel.addWidget (muscle, "excitation");
      addControlPanel (panel);
   }

}
