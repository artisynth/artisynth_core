package artisynth.demos.test;

import java.util.ArrayList;
import java.io.File;

import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.RigidEllipsoid;
import artisynth.core.mechmodels.RigidSphere;
import artisynth.core.mechmodels.RigidCylinder;
import artisynth.core.mechmodels.RigidTorus;
import artisynth.core.mechmodels.FixedMeshBody;
import artisynth.core.workspace.RootModel;
import maspack.geometry.Face;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.MeshFactory;
import maspack.geometry.Vertex3d;
import maspack.matrix.AffineTransform3d;
import maspack.matrix.AxisAngle;
import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix3dBase;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.util.PathFinder;

public class ReflectedBodies extends RootModel {

   MechModel mech;

   public void build(String[] args) {

      mech = new MechModel("mech");
      addModel(mech);

      RigidSphere sphere =
         new RigidSphere ("sphere_L", 0.5, 1000, 32);
      sphere.setPose(new RigidTransform3d(-2, 0, 4, -0.5, 0.3, 15));      
      mech.addRigidBody(sphere);
      mech.addRigidBody (reflectBody ("sphere_R", sphere));

      RigidEllipsoid ellipsoid =
         new RigidEllipsoid ("ellipsoid_L", 0.5, 1, 2, 1, 12);
      ellipsoid.setPose(new RigidTransform3d(-2, 0, 2, -0.5, 0.3, 15));      
      mech.addRigidBody(ellipsoid);
      mech.addRigidBody (reflectBody ("ellipsoid_R", ellipsoid));

      RigidCylinder cylinder =
         new RigidCylinder ("cylinder_L", 1, 4, 1000, 32);
      cylinder.setPose(new RigidTransform3d(-2, 0, 0, -0.5, 0.3, 15));      
      mech.addRigidBody(cylinder);
      mech.addRigidBody (reflectBody ("cylinder_R", cylinder));

      RigidTorus torus =
         new RigidTorus ("torus_L", 1.0, 0.5, 1000, 36, 36);
      torus.setPose(new RigidTransform3d(-2, 0, -2, -0.5, 0.3, 15));      
      mech.addRigidBody(torus);
      mech.addRigidBody (reflectBody ("torus_R", torus));

      PolygonalMesh sphereMesh = MeshFactory.createSphere (1, 32);
      sphereMesh.setNormals (
         sphereMesh.getNormals(), sphereMesh.getNormalIndices());
           
      // try {
      //    sphereMesh.write (new File("sphere.obj"), null);
      //    sphereMesh = new PolygonalMesh ("sphere.obj");
      // }
      // catch (Exception e) {
      //    System.out.println();
      // }

      String sphereFile =
         PathFinder.getSourceRelativePath (this, "geometry/sphere.obj");

      RigidBody scaledSphere =
         RigidBody.createFromMesh ("scaledSphere_L", sphereFile, 1000, 1);
      scaledSphere.scaleSurfaceMesh (-1, 2, 1);
      scaledSphere.setPose(new RigidTransform3d(-2, 0, -4, -0.5, 0.3, 15));   
      mech.addRigidBody (scaledSphere);
      mech.addRigidBody (reflectBody ("scaledSphere_R", scaledSphere)); 

      FixedMeshBody ellipseBody_L = new FixedMeshBody ("ellipseMesh_L");
      ellipseBody_L.setMesh (sphereMesh.copy(), sphereFile, null);
      ellipseBody_L.scaleMesh (1, 2, 3);
      ellipseBody_L.setPose (new RigidTransform3d(-2, 0, -6, -0.5, 0.3, 15)); 
      mech.addMeshBody (ellipseBody_L);

      FixedMeshBody ellipseBody_R = new FixedMeshBody ("ellipseMesh_R");
      ellipseBody_R.setMesh (sphereMesh.copy(), sphereFile, null);
      ellipseBody_R.scaleMesh (1, 2, 3);
      ellipseBody_R.setPose (new RigidTransform3d(-2, 0, -6, -0.5, 0.3, 15)); 
      Matrix3d A = new Matrix3d(new double[] {-1,0,0, 0,1,0, 0,0,1});
      AffineTransform3d X = new AffineTransform3d(A, Vector3d.ZERO);
      ellipseBody_R.transformGeometry (X);
      mech.addMeshBody (ellipseBody_R);
   }

   protected RigidBody reflectBody (String name, RigidBody r) {
      //RigidEllipsoid newR = (RigidEllipsoid) r.copy(0, null);
      RigidBody newR = (RigidBody) r.copy (0, null);
      newR.setName (name);

      //Make and apply the reflection AffineTransform
      Matrix3d A = new Matrix3d(new double[] {-1,0,0, 0,1,0, 0,0,1});
      AffineTransform3d X = new AffineTransform3d(A, Vector3d.ZERO);
      newR.transformGeometry (X);
      return newR;
   }

}
