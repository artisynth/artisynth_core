package artisynth.demos.tutorial;

import java.awt.Color;
import java.io.IOException;

import artisynth.core.mechmodels.FixedMeshBody;
import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.PointMeshForce;
import artisynth.core.mechmodels.PointMeshForce.ForceType;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.workspace.RootModel;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.matrix.RigidTransform3d;
import maspack.render.RenderProps;
import maspack.util.PathFinder;

/**
 * A simple model demonstrating a cube colliding with a bowl shaped mesh using
 * a PointMeshForce.
 */
public class PointMeshForces extends RootModel {

   public void build (String[] args) throws IOException {
      // create a MechModel and add it to the root model
      MechModel mech = new MechModel ("mech");
      mech.setInertialDamping (1.0);
      addModel (mech);

      // create the cube as a rigid body
      RigidBody cube = RigidBody.createBox (
         "cube", /*wx*/0.5, /*wy*/0.5, /*wz*/0.5, /*density*/1000);
      mech.addRigidBody (cube);
      // position the cube to drop into the bowl
      cube.setPose (new RigidTransform3d (0.3, 0, 1));
      
      // add a marker to each vertex of the cube
      for (Vertex3d vtx : cube.getSurfaceMesh().getVertices()) {
         // vertex positions will be in cube local coordinates
         mech.addFrameMarker (cube, vtx.getPosition());
      }     
      // create the bowl for the cube to drop into
      String geoDir = PathFinder.getSourceRelativePath (this, "data/");
      PolygonalMesh mesh = new PolygonalMesh (geoDir + "bowl.obj");
      mesh.triangulate(); // mesh must be triangulated
      FixedMeshBody bowl = new FixedMeshBody (mesh);
      mech.addMeshBody (bowl);

      // Create a PointMeshForce to produce collision interaction between the
      // bowl and the cube. Use the markers as the mesh-colliding points.
      PointMeshForce pmf =
         new PointMeshForce ("pmf", bowl, /*stiffness*/2000000, /*damping*/1000);
      for (FrameMarker m : mech.frameMarkers()) {
         pmf.addPoint (m);
      }
      pmf.setForceType (ForceType.QUADRATIC);
      mech.addForceEffector (pmf);

      // render properties: make cube blue-gray, bowl light gray, and
      // the markers red spheres
      RenderProps.setFaceColor (cube, new Color (0.7f, 0.7f, 1f));
      RenderProps.setFaceColor (bowl, new Color (0.8f, 0.8f, 0.8f));
      RenderProps.setSphericalPoints (mech, 0.04, Color.RED);
   }
}
