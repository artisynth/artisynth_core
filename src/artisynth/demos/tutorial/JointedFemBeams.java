package artisynth.demos.tutorial;

import java.awt.Color;
import maspack.matrix.RigidTransform3d;
import maspack.render.RenderProps;
import artisynth.core.femmodels.FemFactory;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.materials.LinearMaterial;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.SlottedHingeJoint;
import artisynth.core.workspace.RootModel;

public class JointedFemBeams extends RootModel {

   Color myLinkColor = new Color (228/255f, 115/255f, 33/255f);
   Color myEdgeColor = new Color (144/255f, 52/255f, 0);
   Color myJointColor = new Color (93/255f, 93/255f, 168/255f);

   // create an FEM beam  with specified size and stiffness, and add it 
   // to a mech model
   private FemModel3d addFem (
      MechModel mech, double wx, double wy, double wz, double stiffness) {
      FemModel3d fem = FemFactory.createHexGrid (null, wx, wy, wz, 10, 1, 1);
      fem.setMaterial (new LinearMaterial (stiffness, 0.3));
      fem.setDensity (1.0);
      fem.setMassDamping (1.0);
      RenderProps.setFaceColor (fem, myLinkColor);
      RenderProps.setEdgeColor (fem, myEdgeColor);
      fem.setSurfaceRendering (FemModel3d.SurfaceRender.Shaded);
      mech.addModel (fem);
      return fem;
   }

   public void build (String[] args) {
      
      MechModel mech = new MechModel ("mechMod");
      addModel (mech);
      
      double stiffness = 5000;
      // create first fem beam and fix the leftmost nodes      
      FemModel3d fem1 = addFem (mech, 2.4, 0.6, 0.4, stiffness);
      for (FemNode3d n : fem1.getNodes()) {
         if (n.getPosition().x <= -1.2) {
            n.setDynamic(false);
         }
      }
      // create the second fem beam and shift it 1.5 to the right
      FemModel3d fem2 = addFem (mech, 2.4, 0.4, 0.4, 0.1*stiffness);
      fem2.transformGeometry (new RigidTransform3d (1.5, 0, 0));

      // create a slotted revolute joint that connects the two fem beams
      RigidTransform3d TDW = new RigidTransform3d(0.5, 0, 0, 0, 0, Math.PI/2);
      SlottedHingeJoint joint = new SlottedHingeJoint (fem2, fem1, TDW);
      mech.addBodyConnector (joint);
      
      // set ranges and rendering properties for the joint
      joint.setShaftLength (0.8);
      joint.setMinX (-0.5);
      joint.setMaxX (0.5);
      joint.setSlotDepth (0.63);
      joint.setSlotWidth (0.08);
      RenderProps.setFaceColor (joint, myJointColor);
   }
}
