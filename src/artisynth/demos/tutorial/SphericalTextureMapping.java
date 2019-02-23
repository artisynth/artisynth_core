package artisynth.demos.tutorial;

import java.awt.Color;

import maspack.geometry.*;
import maspack.matrix.RigidTransform3d;
import maspack.render.*;
import maspack.render.Renderer.ColorMixing;
import maspack.render.Renderer.Shading;
import maspack.util.PathFinder;
import maspack.spatialmotion.SpatialInertia;
import artisynth.core.mechmodels.*;
import artisynth.core.workspace.RootModel;

/**
 * Simple demo showing color and bump mapping applied to spheres to make them
 * look like tennis balls.
 */
public class SphericalTextureMapping extends RootModel {

   RigidBody createBall (
      MechModel mech, String name, PolygonalMesh mesh, double xpos) {
      double density = 500;
      RigidBody ball = 
         RigidBody.createFromMesh (name, mesh.clone(), density, /*scale=*/1);
      ball.setPose (new RigidTransform3d (/*x,y,z=*/xpos, 0, 0));
      mech.addRigidBody (ball);
      return ball;
   }

   public void build (String[] args) {

      // create MechModel and add to RootModel
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      double radius = 0.0686;
      // create the balls
      PolygonalMesh mesh = MeshFactory.createSphere (
         radius, 20, 10, /*texture=*/true);

      RigidBody ball0 = createBall (mech, "ball0", mesh, -2.5*radius);
      RigidBody ball1 = createBall (mech, "ball1", mesh, 0);
      RigidBody ball2 = createBall (mech, "ball2", mesh, 2.5*radius);

      // set up the basic render props: no shininess, smooth shading to enable
      // bump mapping, and an underlying diffuse color of white to combine with
      // the color map
      RenderProps.setSpecular (mech, Color.BLACK);
      RenderProps.setShading (mech, Shading.SMOOTH);
      RenderProps.setFaceColor (mech, Color.WHITE);
      // create and add the texture maps (provided free courtesy of
      // www.robinwood.com).
      String dataFolder = PathFinder.expand (
         "${srcdir SphericalTextureMapping}/data");

      ColorMapProps cprops = new ColorMapProps();
      cprops.setEnabled (true);
      // no specular coloring since ball should be matt
      cprops.setSpecularColoring (false);
      cprops.setFileName (dataFolder + "/TennisBallColorMap.jpg");

      BumpMapProps bprops = new BumpMapProps();
      bprops.setEnabled (true);
      bprops.setScaling ((float)radius/10);
      bprops.setFileName (dataFolder + "/TennisBallBumpMap.jpg");

      // apply color map to balls 0 and 2. Can do this by setting color map
      // properties in the MechModel, so that properties are controlled in one
      // place - but we must then also explicitly enable color mapping in
      // the surface mesh components for balls 0 and 2.
      RenderProps.setColorMap (mech, cprops);
      RenderProps.setColorMapEnabled (ball0.getSurfaceMeshComp(), true);
      RenderProps.setColorMapEnabled (ball2.getSurfaceMeshComp(), true);

      // apply bump map to balls 1 and 2. Again, we do this by setting
      // the render properties for their surface mesh components
      RenderProps.setBumpMap (ball1.getSurfaceMeshComp(), bprops);
      RenderProps.setBumpMap (ball2.getSurfaceMeshComp(), bprops);
   }
}
