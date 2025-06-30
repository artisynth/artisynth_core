package artisynth.demos.opensim;

import java.io.File;
import java.io.IOException;

import artisynth.core.mechmodels.MechModel;
import artisynth.core.opensim.OpenSimParser;
import artisynth.core.workspace.RootModel;
import artisynth.core.gui.ControlPanel;
import maspack.matrix.AxisAlignedRotation;
import maspack.util.PathFinder;

/**
 * Basic model that reads in the OpenSim "arm26" model by Reinbolt, Seth,
 * Habib, and Hamner, and based on an earlier model Kate Holzbaur.
 */
public class OpenSimArm26 extends RootModel {

   // Store MechModel and parser references for use by subclasses
   MechModel myMech;
   OpenSimParser myParser;

   public void build (String[] args) throws IOException {
      // create mech model to contain the model
      myMech = new MechModel ("mech");
      addModel (myMech);

      // model files are located relative the source of this model
      String osimDir = getSourceRelativePath("osim/");
      File osimFile = new File (osimDir + "arm26_v4.osim"); // V4 version
      File osimGeom = new File (osimDir + "Geometry");
      // create the parser and build the model
      myParser = new OpenSimParser (osimFile, osimGeom);
      myParser.createModel (myMech);
      
      // set view orientation so that Y is up
      setDefaultViewOrientation (AxisAlignedRotation.X_Y);

      // set basic damping factor for rigid bodies
      myMech.setInertialDamping (1.0);
      // create panels to control coordinates and excitations
      ControlPanel panel = myParser.createCoordinatePanel();
      addControlPanel (panel);
      panel = myParser.createExcitationPanel();
      addControlPanel (panel);
      // artisynth.core.modelbase.ComponentUtils.deleteComponentAndDependencies (
      //    myParser.findWrapObject("TRI"));
   }
}
