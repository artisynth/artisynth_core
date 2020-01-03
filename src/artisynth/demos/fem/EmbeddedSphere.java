package artisynth.demos.fem;

import java.awt.Color;
import java.io.IOException;

import artisynth.core.femmodels.FemMeshComp;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.EmbeddedFem;
import artisynth.core.materials.LinearMaterial;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.Collidable.Collidability;
import artisynth.core.mechmodels.CollisionBehavior;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.RootModel;
import maspack.geometry.DistanceGrid;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.AxisAngle;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderProps;
import maspack.render.Renderer.Shading;
import maspack.render.GL.GLViewer;

/**
 * Demonstrates construction of a tight-fitting FEM about a sphere
 *
 * @author Antonio
 */
public class EmbeddedSphere extends RootModel {

   @Override
   public void build(String[] args) throws IOException {
      super.build(args);
      
      // build tight-fitting FEM
      PolygonalMesh sphere = MeshFactory.createOctahedralSphere(0.1, 3);
      FemModel3d fem = EmbeddedFem.createVoxelizedFem(null, sphere, RigidTransform3d.IDENTITY, 4, -1);
      DistanceGrid sd = new DistanceGrid(sphere.getFaces(), 0.1, 50, true);
      EmbeddedFem.trimBoundaryHexes(fem, sd, 0);

      // add sphere surface to model and make collidable
      FemMeshComp sphereSurface = fem.addMesh (sphere);
      sphereSurface.setCollidable (Collidability.ALL);
      
      // build model
      MechModel mech = new MechModel ("mech");
      addModel(mech);
      
      fem.setName ("sphere");
      fem.setMaterial (new LinearMaterial (20000, 0.45));
      fem.setDensity (1000);
      mech.addModel (fem);
      
      // create a rigid box for the sphere to fall on
      RigidBody box = RigidBody.createBox("box", 0.25, 0.25, 0.02, 0, /*addnormals*/ true);
      box.setPose(new RigidTransform3d(new Vector3d(0,0,-0.2), AxisAngle.IDENTITY));
      box.setDynamic(false);
      mech.addRigidBody(box);
      
      // enable collisions
      CollisionBehavior cb = mech.setCollisionBehavior (sphereSurface, box, true);
      cb.setReduceConstraints (true);
      
      // adjust sphere render props
      RenderProps.setFaceColor (sphereSurface, Color.ORANGE.darker ());
      RenderProps.setShading (sphereSurface, Shading.SMOOTH);
      
      // adjust table render properties
      RenderProps.setShading(box, Shading.METAL);
      RenderProps.setSpecular(box, new Color(0.8f,0.8f,0.8f));
   }
   
   @Override
   public void attach (DriverInterface driver) {
      super.attach (driver);
      
      GLViewer viewer = driver.getViewer ();
      if (viewer != null) {
         viewer.setBackgroundColor (Color.WHITE);
      }
   }
   
}
