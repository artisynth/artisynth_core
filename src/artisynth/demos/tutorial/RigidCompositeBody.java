package artisynth.demos.tutorial;

import java.awt.Color;

import artisynth.core.mechmodels.*;
import artisynth.core.workspace.RootModel;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.render.RenderProps;

/**
 * Simple demo showing a dumbbell shaped rigid body made of multiple meshes
 */
public class RigidCompositeBody extends RootModel {

   public void build (String[] args) {

      // create MechModel and add to RootModel
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      // create the component meshes
      PolygonalMesh ball1 = MeshFactory.createIcosahedralSphere (0.8, 1);
      ball1.transform (new RigidTransform3d (1.5, 0, 0));
      PolygonalMesh ball2 = MeshFactory.createIcosahedralSphere (0.8, 1);
      ball2.transform (new RigidTransform3d (-1.5, 0, 0));
      PolygonalMesh axis = MeshFactory.createCylinder (0.2, 2.0, 12);
      axis.transform (new RigidTransform3d (0, 0, 0, 0, Math.PI/2, 0));

      // create the body and add the component meshes
      RigidBody body = new RigidBody ("body");
      body.setDensity (10);
      body.setFrameDamping (10); // add damping to the body
      body.addMesh (axis);
      RigidMeshComp bcomp1 = body.addMesh (ball1);
      RigidMeshComp bcomp2 = body.addMesh (ball2);
      mech.addRigidBody (body);

      // connect the body to a spring attached to a fixed particle
      Particle p1 = new Particle ("p1", /*mass=*/0, /*x,y,z=*/0, 0, 2);
      p1.setDynamic (false);
      mech.addParticle (p1);
      FrameMarker mkr = mech.addFrameMarkerWorld (body, new Point3d (0, 0, 0.2));
      AxialSpring spring =
         new AxialSpring ("spr", /*k=*/150, /*d=*/0, /*restLength=*/0);
      spring.setPoints (p1, mkr);
      mech.addAxialSpring (spring);

      // set the density for ball1 to be less than the body density
      bcomp1.setDensity (8);

      // set render properties for the component, with the ball
      // meshes having different colors
      RenderProps.setFaceColor (body, new Color (250, 200, 200));
      RenderProps.setFaceColor (bcomp1, new Color (200, 200, 250));
      RenderProps.setFaceColor (bcomp2, new Color (200, 250, 200));
      RenderProps.setSphericalPoints (mech, 0.06, Color.WHITE);
      RenderProps.setCylindricalLines (spring, 0.02, Color.BLUE);
   }
}
