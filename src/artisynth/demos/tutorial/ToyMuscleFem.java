package artisynth.demos.tutorial;

import java.awt.Color;
import java.io.IOException;

import artisynth.core.femmodels.FemElement3d;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.FemFactory;
import artisynth.core.femmodels.FemMuscleModel;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.MuscleBundle;
import artisynth.core.gui.ControlPanel;
import artisynth.core.materials.LinearMaterial;
import artisynth.core.materials.SimpleForceMuscle;
import artisynth.core.mechmodels.Frame;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.probes.PlotTraceInfo;
import artisynth.core.workspace.RootModel;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderProps;
import maspack.render.Renderer.AxisDrawStyle;
import maspack.widgets.LabeledComponentBase;

/**
 * A simple hex-based FEM beam with muscles added to allow it to move in the
 * x-z plane.
 */
public class ToyMuscleFem extends RootModel {

   protected MechModel myMech;      // overall mechanical model
   protected FemMuscleModel myFem;  // FEM muscle model
   protected Frame myFrame;         // frame attached to the FEM

   public void build (String[] args) throws IOException {
      myMech = new MechModel ("mech");
      myMech.setGravity (0, 0, 0);
      addModel (myMech);

      // create a FemMuscleModel and then create a hex grid with it.
      myFem = new FemMuscleModel ("fem");
      FemFactory.createHexGrid (
         myFem, /*wx*/1.2, /*wy*/0.3, /*wz*/0.3, /*nx*/12, /*ny*/3, /*nz*/3);
      // give it a material with Poisson's ratio 0 to allow it to compress
      myFem.setMaterial (new LinearMaterial (100000, 0.0));
      // fem muscles will be material-based, using a SimpleForceMuscle
      myFem.setMuscleMaterial (new SimpleForceMuscle(/*maxstress=*/100000));
      // set density + particle and stiffness damping for optimal stability
      myFem.setDensity (100.0);
      myFem.setParticleDamping (1.0);
      myFem.setStiffnessDamping (1.0);
      myMech.addModel (myFem);

      // fix the nodes on the left side
      for (FemNode3d n : myFem.getNodes()) {
         Point3d pos = n.getPosition();
         if (pos.x == -0.6) {
            n.setDynamic (false);
         }
      }
      // Create twelve muscle bundles, bottom to top and left to right. Muscles
      // are material-based, each with a set of 9 elements in the x-y plane and
      // a rest direction parallel to the x axis.
      Point3d pe = new Point3d(); // element center point
      for (int sec=0; sec<4; sec++) {
         for (int k=0; k<3; k++) {
            MuscleBundle bundle = new MuscleBundle();
            // locate elements based on their center positions
            pe.set (-0.55+sec*0.3, -0.1, -0.1+k*0.1);
            for (int i=0; i<3; i++) {
               pe.y = -0.1;
               for (int j=0; j<3; j++) {
                  FemElement3d e = myFem.findNearestVolumetricElement(null, pe);
                  bundle.addElement (e, Vector3d.X_UNIT);
                  pe.y += 0.1;
               }
               pe.x += 0.1;
            }
            // set the line color for each bundle using a color from the probe
            // display palette.
            RenderProps.setLineColor (
               bundle, PlotTraceInfo.getPaletteColors()[sec*3+k]);
            myFem.addMuscleBundle (bundle);
         }
      }
      // create a panel for controlling all 12 muscle excitations
      ControlPanel panel = new ControlPanel ();
      for (MuscleBundle b : myFem.getMuscleBundles()) {
         LabeledComponentBase c = panel.addWidget (
            "excitation "+b.getNumber(), b, "excitation");
         // color the exciter labels using the muscle bundle color
         c.setLabelFontColor (b.getRenderProps().getLineColor());
      }
      addControlPanel (panel);
      
      // create and attach a frame to the right end of the FEM
      myFrame = new Frame();
      myFrame.setPose (new RigidTransform3d (0.45, 0, 0));
      myMech.addFrame (myFrame);
      myMech.attachFrame (myFrame, myFem); // attach to the FEM

      // render properties:
      // show muscle bundles by rendering activation directions within elements
      RenderProps.setLineWidth (myFem.getMuscleBundles(), 4);
      myFem.setDirectionRenderLen (0.6); // 
      // draw frame by showing its coordinate axis
      myFrame.setAxisLength (0.3);
      myFrame.setAxisDrawStyle (AxisDrawStyle.ARROW);
      // set FEM line and surface colors to blue-gray
      RenderProps.setLineColor (myFem, new Color (0.7f, 0.7f, 1f));
      RenderProps.setFaceColor (myFem, new Color (0.7f, 0.7f, 1f));
      // render FEM surface transparent
      myFem.setSurfaceRendering (SurfaceRender.Shaded);
      RenderProps.setAlpha (myFem.getSurfaceMeshComp(), 0.4);
   }
}
