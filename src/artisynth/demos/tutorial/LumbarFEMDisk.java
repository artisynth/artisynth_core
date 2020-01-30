package artisynth.demos.tutorial;

import java.io.IOException;
import java.io.File;
import java.awt.Color;
import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;
import artisynth.core.femmodels.*;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.materials.*;
import artisynth.core.materials.*;
import artisynth.core.workspace.RootModel;
import maspack.util.PathFinder;
import maspack.matrix.*;
import maspack.geometry.*;
import maspack.render.*;

/**
 * Demo of two rigid bodies connected by a simple FEM disk
 */
public class LumbarFEMDisk extends RootModel {

   double density = 1500;

   // path from which meshes will be read
   private String geometryDir = PathFinder.getSourceRelativePath (
      LumbarFEMDisk.class, "../mech/geometry/");

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

      // create a torus shaped FEM model for the disk
      FemModel3d fem = new FemModel3d();
      fem.setDensity (1500);
      fem.setMaterial (new LinearMaterial (20000, 0.4));
      FemFactory.createHexTorus (fem, 0.011, 0.003, 0.008, 16, 30, 2);
      mech.addModel (fem);

      // position it betweem the disks
      double DTOR = Math.PI/180.0;
      fem.transformGeometry (
         new RigidTransform3d (-0.012, 0.0, 0.040, 0, -DTOR*25, DTOR*90));

      // find and attach nearest nodes to either the top or bottom mesh
      double tol = 0.001;
      for (FemNode3d n : fem.getNodes()) {
         // top vertebra
         double d = lumbar1.getSurfaceMesh().distanceToPoint(n.getPosition());
         if (d < tol) {
            mech.attachPoint (n, lumbar1);
         }
         // bottom vertebra
         d = lumbar2.getSurfaceMesh().distanceToPoint(n.getPosition());
         if (d < tol) {
            mech.attachPoint (n, lumbar2);
         }
      }
      // set render properties ...
      fem.setSurfaceRendering (SurfaceRender.Shaded);
      RenderProps.setFaceColor (fem, new Color (153/255f, 153/255f, 1f));
      RenderProps.setFaceColor (mech, new Color (238, 232, 170)); // bone color
   }
}
