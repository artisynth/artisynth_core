package artisynth.demos.fem;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JFrame;

import maspack.geometry.MeshFactory;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderProps;
import maspack.render.Renderer.LineStyle;
import maspack.spatialmotion.SpatialInertia;
import artisynth.core.driver.Main;
import artisynth.core.femmodels.MuscleBundle;
import artisynth.core.gui.ControlPanel;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.Muscle;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.MechSystemSolver.Integrator;
import artisynth.core.util.ArtisynthIO;
import artisynth.core.util.ArtisynthPath;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.RootModel;
import artisynth.demos.fem.HydrostatModel.Element;
import artisynth.demos.fem.HydrostatModel.Shape;

public class HydrostatDemo extends RootModel {
   protected HydrostatModel hydro;

   // protected MechModel mech;
   protected boolean simple = false;

   protected String workspaceDirname = "hydrostat";

   protected String probeFilename = "0probes.art";

   protected String[] panelNames =
      new String[] { "hydrostat_fem",
                     "hydrostat_muscles_bundles"
//                     "hydrostat_groups",
//                    "hydrostat_muscles_exciters" 
                     };

   public void build (String[] args) throws IOException {
      build (Shape.Beam);
   }
   
   public void build (Shape shape) throws IOException {

      if (simple) {
	 hydro = new HydrostatModel("hydrostat", Element.Hex, shape, 100,
	       40, 3, 1, /*simpleExciters*/true, /*fullAPMuscles*/false);
      } else {
	 hydro = new HydrostatModel("hydrostat",  /*fullAPMuscles*/false, shape);
      }

      // mech = new MechModel("mech");
      // mech.addModel (hydro);
      // mech.setIntegrator (Integrator.ConstrainedBackwardEuler);
      // mech.setMaxStepSize (hydro.getMaxStepSize());
      // addModel(mech);
      addModel (hydro);

      hydro.setIntegrator (Integrator.ConstrainedBackwardEuler);
      // hydro.setIncompressible (FemModel.IncompMethod.AUTO);

      // addContactBlock(mech);
      // mech.setCollisions (mech.rigidBodies().get(0), hydro, true);

   }

   public void addContactBlock (MechModel mech) {
      RigidBody block = new RigidBody();
      block.setName ("block");
      block.setMesh (MeshFactory.createQuadBox (100, 100, 20), null);
      block.setInertia (
         SpatialInertia.createBoxInertia (1, 100, 100, 20));
      block.setPose (new RigidTransform3d (
         new Vector3d (-50, 0, 40), new RotationMatrix3d()));
      mech.addRigidBody (block);
   }

   public void loadControlPanels () {

      for (String name : panelNames) {
         ControlPanel panel =
            loadControlPanel (ArtisynthPath.getSrcRelativePath (
                                 getClass(), "data/" + name + ".art"));
      }

      if (hydro.getMusclePanel() != null) {
         addControlPanel (hydro.getMusclePanel());
      }
   }

   public void loadProbes (String probeFilename) {
      if (probeFilename == null)
         return;

      try {
         scanProbes (
            ArtisynthIO.newReaderTokenizer (
               ArtisynthPath.getWorkingDir().getPath()
               + "/" + probeFilename));
      }
      catch (Exception e) {
         System.out.println ("Error reading probe file");
         e.printStackTrace();
      }
   }

   public void attach (DriverInterface driver) {
      super.attach (driver);
      loadControlPanels ();

      File workingDir =
         new File (ArtisynthPath.getSrcRelativePath (
                      this, "data/" + workspaceDirname
                      + ((simple) ? "_simple" : "")));
      ArtisynthPath.setWorkingDir (workingDir);

       loadProbes (probeFilename);

      // for (Probe p : Main.getWorkspace().getOutputProbes())
      // if (NumericOutputProbe.class.isAssignableFrom (p.getClass()))
      // ((NumericOutputProbe)p).setShowHeader (false);
   }

   /**
    * {@inheritDoc}
    */
   public String getAbout() {
      return "3D Muscular Hydrostat Demo";
   }

}
