package artisynth.demos.opensim;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;

import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.GmshReader;
import artisynth.core.femmodels.NodeNumberReader;
import artisynth.core.materials.MooneyRivlinMaterial;
import artisynth.core.materials.PowerFrameMaterial;
import artisynth.core.materials.Thelen2003AxialMuscle;
import artisynth.core.mechmodels.CollisionBehavior;
import artisynth.core.mechmodels.FrameSpring;
import artisynth.core.mechmodels.JointBase;
import artisynth.core.mechmodels.MuscleComponent;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.modelbase.ComponentUtils;
import maspack.matrix.RigidTransform3d;
import maspack.render.RenderProps;

/**
 * Modified version of OpenSimArm26 that replaces the elbow with a toy FE
 * contact model.
 */
public class OpenSimFemElbow extends OpenSimArm26 {
   /**
    * Helper method to set material and render properties fo a FEM.
    */
   private void initializeFem (FemModel3d fem, String name) {
      // Mooney Rivlin value c01 of 2e6 is sometimes reported for cartilage
      MooneyRivlinMaterial mrmat = new MooneyRivlinMaterial (
         /*c10*/2000000, /*c01*/10000, 0, 0, 0, /*kappa*/1e8);
      fem.setMaterial (mrmat);
      fem.setParticleDamping (1.0);
      fem.setDensity (1000);
      fem.setName (name);
      // enable surface rending, turn off element rendering, change color
      fem.setSurfaceRendering (SurfaceRender.Shaded);
      RenderProps.setVisible (fem.getElements(), false);
      RenderProps.setDrawEdges (fem, true);
      RenderProps.setFaceColor (fem, new Color (0.8f, 0.8f, 1f));
      RenderProps.setLineColor (fem, new Color (0.6f, 0.6f, 1f));
   }

   public void build (String[] args) throws IOException {
      super.build (args); // create OpenSimArm26

      // geometry dir is "geometry" relative to source for this model
      String geodir = getSourceRelativePath ("geometry/");

      // locate the humerus, lower arm, and elbow joint
      RigidBody humerus = myParser.findBody ("r_humerus");
      RigidBody lowerArm = myParser.findBody ("r_ulna_radius_hand");
      JointBase elbow = myParser.findJoint ("r_elbow");

      // use the current TDW for the elbow joint to determine the transform to
      // locate the FEM meshes
      RigidTransform3d TDW = new RigidTransform3d();
      TDW.set (elbow.getCurrentTDW());

      // read FEM models for the inner and outer cartilage layers from
      // predefined Gmsh files, initialize their other properties, transform
      // them to the location of the elbow joint, and add them to the MechModel
      FemModel3d innerCart = GmshReader.read (geodir+"innerCart.gmsh");
      initializeFem (innerCart, "inner_cartilage");
      innerCart.transformGeometry (TDW);
      myMech.addModel (innerCart);

      FemModel3d outerCart = GmshReader.read (geodir+"outerCart.gmsh");
      initializeFem (outerCart, "outer_cartilage");
      outerCart.transformGeometry (TDW);
      myMech.addModel (outerCart);

      // read the nodes that need to be attached for each model, and
      // attach them to the humerus or lower arm
      ArrayList<FemNode3d> attach;
      attach = NodeNumberReader.read (geodir+"innerCartAttach.txt", innerCart);
      for (FemNode3d n : attach) {
         myMech.attachPoint (n, humerus);
      }
      attach = NodeNumberReader.read (geodir+"outerCartAttach.txt", outerCart);
      for (FemNode3d n : attach) {
         myMech.attachPoint (n, lowerArm);
      }

      // Remove the elbow joint and replace with a FrameSpring to provide
      // motion limits.
      ComponentUtils.deleteComponentAndDependencies (elbow);
      PowerFrameMaterial fmat = new PowerFrameMaterial();
      // set stiffnesses to limit sliding and rotation along/about z.
      fmat.setStiffness (0, 0, 10000);
      fmat.setRotaryStiffness (0, 0, 3000);
      // use a rotary deadband to provide a z rotation range of [0,PI]
      fmat.setUpperRotaryDeadband (0, 0, Math.PI); // 
      FrameSpring fspring = new FrameSpring ("elbow_spring", fmat);
      fspring.setUseTransformDC (false); // use same transform as for joints
      fspring.setFrames (lowerArm, humerus, TDW);
      myMech.addFrameSpring (fspring);

      // enable collsions between the inner and outer cartilage
      CollisionBehavior cb = myMech.setCollisionBehavior (
         innerCart, outerCart, true);
      // to improve stability, make the contact compliant and reduce step size
      double c = 1e-6;
      cb.setCompliance (c);
      cb.setDamping (2*Math.sqrt(/*mass*/5/c)); // aim for critical damping
      setMaxStepSize (0.005);

      // With the elbow joint removed, some muscles exert initial forces that
      // are too large. Reduce this by reseting their tendon slack length.
      for (String muscleName : new String[] { "BIClong", "BRA" }) {
         MuscleComponent muscle = myParser.findMuscle (muscleName);
         Thelen2003AxialMuscle tmat = (Thelen2003AxialMuscle)muscle.getMaterial();
         tmat.setTendonSlackLength (muscle.getLength()-tmat.getOptFibreLength());
      }
      // remove initial muscle excitations
      myParser.zeroMuscleExcitations();

      // make the TRI wrap object invisible
      RenderProps.setVisible (myParser.findWrapObject ("TRI"), false);
   }
}
