package artisynth.demos.fem;

import java.awt.Color;

import artisynth.core.femmodels.FemFactory;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.materials.LinearMaterial;
import artisynth.core.mechmodels.HingeJoint;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.RigidBody;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.RigidTransform3d;
import maspack.render.RenderProps;

/**
 * Simple model showing an FEM connected to two rigid body blocks and a hinge
 * joint.
 */
public class Hex3dBlock extends Fem3dBlock {

   private double DENSITY = 1000;

   /**
    * Attach an FEM model to a rigid body by attaching all nodes within 'tol'
    * of the body's surface.
    */
   private void attachFemToRigidBody (
      MechModel mech, FemModel3d fem, RigidBody body, double tol) {
      PolygonalMesh surface = body.getSurfaceMesh();
      for (FemNode3d n : fem.getNodes()) {
         if (surface.distanceToPoint (n.getPosition()) <= tol) {
            mech.attachPoint (n, body);
         }
      }
   }

   public void build (String[] args) {
      // create a MechModel and add it to the root model
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      // create a hexahedral FEM beam with dimensions 0.6 x 0.2 x 0.2 and
      // element resolutions 9 x 3 x 3
      FemModel3d fem = FemFactory.createHexGrid (
         new FemModel3d("fem"), 0.6, 0.2, 0.2, 9, 3, 3);
      // set FEM material and damping parameters
      fem.setMaterial (new LinearMaterial (200000.0, 0.4));
      fem.setStiffnessDamping (0.002);
      mech.addModel (fem);

      // create a rigid body block, add it to the MechModel
      RigidBody leftBlock = RigidBody.createBox (
         "leftBlock", 0.1, 0.3, 0.3, DENSITY);
      mech.addRigidBody (leftBlock);
      // move block left and attach it to the left end of the FEM
      leftBlock.setPose (new RigidTransform3d (-0.35, 0, 0));
      attachFemToRigidBody (mech, fem, leftBlock, 1e-8);

      // create another block and attach it to the right end of the FEM
      RigidBody rightBlock = RigidBody.createBox (
         "rightBlock", 0.1, 0.3, 0.3, DENSITY);
      mech.addRigidBody (rightBlock);
      rightBlock.setPose (new RigidTransform3d (0.35, 0, 0));
      attachFemToRigidBody (mech, fem, rightBlock, 1e-8);

      // create a hinge joint between the left block and ground
      RigidTransform3d TCW = new RigidTransform3d();
      // set TCW to the joint position/orientation world space:
      TCW.setXyz (-0.35, 0, 0.15);
      TCW.setRpy (0, 0, Math.PI/2);
      HingeJoint joint = new HingeJoint (leftBlock, TCW);
      mech.addBodyConnector (joint);

      // set model render properties

      // render the joint shaft blue:
      RenderProps.setFaceColor (joint, Color.BLUE);
      joint.setShaftLength (0.5);
      joint.setShaftRadius (0.01);
      // render the FEM surface as light blue-gray:
      fem.setSurfaceRendering (SurfaceRender.Shaded);
      RenderProps.setFaceColor (fem, new Color (0.8f, 0.8f, 1.0f));
      // render FEM nodes as purple spheres:
      RenderProps.setSphericalPoints (fem, 0.01, new Color (153, 0, 204));
      // render FEM edges blue:
      RenderProps.setLineWidth (fem, 2);
      RenderProps.setLineColor (fem, Color.BLUE);
   }
}
