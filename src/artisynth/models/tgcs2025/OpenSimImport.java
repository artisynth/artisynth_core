package artisynth.models.tgcs2025;

import java.io.File;
import java.io.IOException;

import artisynth.core.gui.ControlPanel;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.opensim.OpenSimParser;
import artisynth.core.workspace.RootModel;
import artisynth.demos.opensim.OpenSimArm26;
import maspack.matrix.AxisAlignedRotation;
import maspack.util.PathFinder;

/**
 * Basic model that reads in the OpenSim "arm26" model by Reinbolt, Seth,
 * Habib, and Hamner, and based on an earlier model Kate Holzbaur.
 */
public class OpenSimImport extends RootModel {

   // Store MechModel and parser references for use by subclasses
   MechModel myMech;
   OpenSimParser myParser;

   public void build (String[] args) throws IOException {
      // create mech model to contain the model
      myMech = new MechModel ("mech");
      addModel (myMech);

      // model files are located relative the source of this model
      String osimDir = PathFinder.getSourceRelativePath(
         OpenSimArm26.class, "osim/");
      File osimFile = new File (osimDir + "arm26_v4.osim"); //// V4 version
      // create the parser and build the model. Geometry is located
      // in parent folder of the .osim file.
      myParser = new OpenSimParser (osimFile);
      myParser.createModel (myMech);
      
//      // set view orientation so that Y is up
//      setDefaultViewOrientation (AxisAlignedRotation.X_Y);
      
//      // set basic damping factor for rigid bodies
//      myMech.setInertialDamping (1.0);
//      // make wrap objects invisible
//      myParser.setWrapObjectsVisible (false);
      
//      // create panels to control coordinates and excitations
//      ControlPanel panel = myParser.createCoordinatePanel();
//      addControlPanel (panel);
//      panel = myParser.createExcitationPanel();
//      addControlPanel (panel);
   }
}
