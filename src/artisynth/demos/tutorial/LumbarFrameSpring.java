package artisynth.demos.tutorial;

import java.io.IOException;
import java.io.File;
import java.awt.Color;
import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;
import artisynth.core.materials.*;
import artisynth.core.workspace.RootModel;
import maspack.matrix.*;
import maspack.geometry.*;
import maspack.render.*;
import maspack.util.PathFinder;

/**
 * Demo of two rigid bodies connected by a 6 DOF frame spring
 */
public class LumbarFrameSpring extends RootModel {

   double density = 1500;

   // path from which meshes will be read
   private String geometryDir = PathFinder.getSourceRelativePath (
      LumbarFrameSpring.class, "../mech/geometry/");

   // create and add a rigid body from a mesh
   public RigidBody addBone (MechModel mech, String name) throws IOException {
      PolygonalMesh mesh = new PolygonalMesh (new File (geometryDir+name+".obj"));
      RigidBody rb = RigidBody.createFromMesh (name, mesh, density, /*scale=*/1);
      mech.addRigidBody (rb);
      return rb;
   }

   public void build (String[] args) throws IOException {

      // create mech model and set it's properties
      MechModel mech = new MechModel ("mech");
      mech.setGravity (0, 0, -1.0);
      mech.setFrameDamping (0.10);
      mech.setRotaryDamping (0.001);
      addModel (mech);

      // create two rigid bodies and second one to be fixed
      RigidBody lumbar1 = addBone (mech, "lumbar1");
      RigidBody lumbar2 = addBone (mech, "lumbar2");
      lumbar1.setPose (new RigidTransform3d (-0.016, 0.039, 0));
      lumbar2.setDynamic (false);

      // flip entire mech model around
      mech.transformGeometry (
         new RigidTransform3d (0, 0, 0, 0, 0, Math.toRadians (90)));

      //create and add the frame spring
      FrameSpring spring = new FrameSpring (null);
      spring.setMaterial (
         new LinearFrameMaterial (
            /*ktrans=*/100, /*krot=*/0.01, /*dtrans=*/0, /*drot=*/0));
      spring.setFrames (lumbar1, lumbar2, lumbar1.getPose());
      mech.addFrameSpring (spring);

      // set render properties for components
      RenderProps.setLineColor (spring, Color.RED);
      RenderProps.setLineWidth (spring, 3);
      spring.setAxisLength (0.02);
      RenderProps.setFaceColor (mech, new Color (238, 232, 170)); // bone color
   }
}
