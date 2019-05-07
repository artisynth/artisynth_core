package artisynth.core.mechmodels;

import java.io.File;
import artisynth.core.mechmodels.RigidBody.InertiaMethod;
import artisynth.core.modelbase.TransformGeometryContext;
import maspack.matrix.*;
import maspack.spatialmotion.*;
import maspack.geometry.*;
import maspack.util.*;

public class RigidBodyTest extends UnitTest {

   private static double EPS = 1e-10;

   private int numMassMeshes (RigidBody body) {
      int num = 0;
      for (RigidMeshComp mcomp : body.getMeshComps()) {
         if (mcomp.hasMass()) {
            num++;
         }
      }
      return num;
   }

   private SpatialInertia getInertia (RigidBody body) {
      SpatialInertia M = new SpatialInertia();
      body.getInertia (M);
      return M;
   }

   private Vector3d getCentroid (MeshBase mesh) {
      Vector3d cent = new Vector3d();
      mesh.computeCentroid(cent);
      return cent;
   }

   private void checkInertiaSettings (
      RigidBody body, double densityChk,
      SpatialInertia MChk, InertiaMethod methodChk) {

      checkEquals ("body.getDensity()", body.getDensity(), densityChk, EPS);
      SpatialInertia M = new SpatialInertia();
      body.getInertia(M);
      checkEquals ("body.getInertia()", M, MChk, EPS);
      checkEquals (
         "body.getInertiaMethod()", body.getInertiaMethod(), methodChk);

      double implicitVolume = body.sumImplicitMeshVolumes();
      if (implicitVolume > 0) {
         double implicitMass = body.getMass() - body.sumExplicitMeshMasses();
         double massSum = body.sumMeshMasses();
         checkEquals (
            "sumMeshMasses", massSum, body.getMass(), EPS);
         checkEquals (
            "computedDensity", implicitMass/implicitVolume,
            body.getDensity(), EPS);
      }
   }

   public void testInertiaMethods() {

      RigidBody body = new RigidBody();
      double density = 10;
      body.setDensity (density);
      PolygonalMesh box1 = MeshFactory.createBox (3, 2, 1);
      double volume = 6;
      SpatialInertia Mbox = SpatialInertia.createBoxInertia (
         /*mass=*/density*volume, 3, 2, 1);

      checkInertiaSettings (
         body, density, SpatialInertia.ZERO, InertiaMethod.DENSITY);

      // set up a basic box inertia
      body.addMesh (box1);
      checkInertiaSettings (
         body, density, Mbox, InertiaMethod.DENSITY);

      // reset the density to 20. Should scale the inertia
      density = 20;
      body.setDensity (density);
      Mbox.scale (2);
      checkInertiaSettings (
         body, density, Mbox, InertiaMethod.DENSITY);

      // reset the mass to 100. Should scale density
      body.setMass (100);
      density *= 5/6.0;
      Mbox.scale (5/6.0);
      checkInertiaSettings (
         body, density, Mbox, InertiaMethod.DENSITY);     

      // change the InertiaMethod to MASS, EXPLICIT and back to DENSITY.
      // Shouldn't change anything
      body.setInertiaMethod (InertiaMethod.MASS);
      checkInertiaSettings (body, density, Mbox, InertiaMethod.MASS);   
      body.setInertiaMethod (InertiaMethod.EXPLICIT);
      checkInertiaSettings (body, density, Mbox, InertiaMethod.EXPLICIT);   
      body.setInertiaMethod (InertiaMethod.DENSITY);
      checkInertiaSettings (body, density, Mbox, InertiaMethod.DENSITY);   

      // explicitly set the inertia to a random value. Density should scale
      SpatialInertia Mrand = new SpatialInertia();
      Mrand.setRandom();
      body.setInertia (Mrand);
      checkInertiaSettings (
         body, Mrand.getMass()/volume, Mrand, InertiaMethod.EXPLICIT);  

      // reset density. Mass should reset to 100
      body.setDensity (density);
      Mrand.setMass (100);
      checkInertiaSettings (body, density, Mrand, InertiaMethod.EXPLICIT);  
      
      // now change mode back to DENSITY. Should recompute inertia
      body.setInertiaMethod (InertiaMethod.DENSITY);
      checkInertiaSettings (body, density, Mbox, InertiaMethod.DENSITY);  

      // set inertia back to random value
      body.setInertia (Mrand);
      checkInertiaSettings (
         body, Mrand.getMass()/volume, Mrand, InertiaMethod.EXPLICIT);  

      // and then test setInertiaFromDensity
      body.setInertiaFromDensity (density);
      checkInertiaSettings (body, density, Mbox, InertiaMethod.DENSITY);  

      // and setInertiaFromMass
      body.setInertia (Mrand);
      body.setInertiaFromMass (Mbox.getMass());
      checkInertiaSettings (body, density, Mbox, InertiaMethod.MASS);  

      // now test changes at the mesh level:

      // explicitly set mesh mass. Inertia should double, density
      // should stay the same
      RigidMeshComp boxComp = body.getMeshComp(0);
      boxComp.setMass(200);
      Mbox.scale(2.0);
      checkInertiaSettings (body, density, Mbox, InertiaMethod.MASS);  

      // increase explicit mesh mass. Inertia should increase
      boxComp.setMass(300);
      Mbox.scale(1.5);
      checkInertiaSettings (body, density, Mbox, InertiaMethod.MASS);  

      // and then remove explicit mass. Inertia should be recomputed,
      // and since mass is fixed, density should be scaled up
      boxComp.setMass (-1);
      density *= 3.0;
      checkInertiaSettings (body, density, Mbox, InertiaMethod.MASS);  

      // now remove the mesh. Inertia should be unchanged
      body.removeMeshComp (boxComp);
      checkInertiaSettings (
         body, density, Mbox, InertiaMethod.MASS);  

      // add the mesh back
      body.addMeshComp (boxComp);
      checkInertiaSettings (
         body, density, Mbox, InertiaMethod.MASS);  

      // now transform the mesh geometry to double the mesh volume. Since we
      // are in MASS mode, mass should stay constant and density should half.
      // Inertia will change to that of a stretched box
      AffineTransform3d X = new AffineTransform3d();
      X.applyScaling (2.0, 1.0, 1.0);
      boxComp.transformGeometry (X);
      density *= 0.5;
      Mbox = SpatialInertia.createBoxInertia (/*mass=*/body.getMass(), 6, 2, 1);
      checkInertiaSettings (body, density, Mbox, InertiaMethod.MASS);  

      // Set inertia method to DENSITY. Nothing else should change ...
      body.setInertiaMethod (InertiaMethod.DENSITY);
      checkInertiaSettings (body, density, Mbox, InertiaMethod.DENSITY);  

      // Transform mesh transform back to original shape. Mass should reduce by
      // half.
      X.setScaling (0.5, 1.0, 1.0);
      double mass = body.getMass();
      boxComp.transformGeometry (X);
      Mbox = SpatialInertia.createBoxInertia (mass/2, 3, 2, 1);
      checkInertiaSettings (body, density, Mbox, InertiaMethod.DENSITY);  

      // now remove the mesh. Since inertia mode is DENSITY, inertia should
      // drop to zero
      body.removeMeshComp (boxComp);
      checkInertiaSettings (
         body, density, SpatialInertia.ZERO, InertiaMethod.DENSITY);  

      // and add it back
      body.addMeshComp (boxComp);
      checkInertiaSettings (
         body, density, Mbox, InertiaMethod.DENSITY);  

      // explicitly set mesh mass. Inertia should double, density
      // should stay the same
      boxComp.setMass(300);
      Mbox.scale(2.0);
      checkInertiaSettings (body, density, Mbox, InertiaMethod.DENSITY);  

      // increase explicit mesh mass. Inertia should increase
      boxComp.setMass(600);
      Mbox.scale(2.0);
      checkInertiaSettings (body, density, Mbox, InertiaMethod.DENSITY);  

      // and then remove explicit mass. Inertia should be recomputed,
      // and given fixed density, mass should be scaled down
      boxComp.setMass (-1);
      Mbox.scale (0.25);
      checkInertiaSettings (body, density, Mbox, InertiaMethod.DENSITY);  

      // Add another mesh. Resulting inertia should just be the sum of the two

      PolygonalMesh ball = MeshFactory.createSphere (/*radius=*/2, 4);
      //MeshFactory.createBox (2, 2, 2);
      SpatialInertia Mball = ball.createInertia (density);
      SpatialInertia Mbody = new SpatialInertia();
      Mbody.add (Mball, Mbox);
      body.addMesh (ball);
      RigidMeshComp ballComp = body.getMeshComp(1);
      checkInertiaSettings (body, density, Mbody, InertiaMethod.DENSITY);  

      // double the density. This should double the inertia
      density *= 2;
      body.setDensity (density);
      Mbody.scale (2.0);
      checkInertiaSettings (body, density, Mbody, InertiaMethod.DENSITY);  

      // explicitly set the inerta to Mrand
      body.setInertia (Mrand);
      checkInertiaSettings (
         body, Mrand.getMass()/body.getVolume(), Mrand, InertiaMethod.EXPLICIT);  

      // set the mass to 123 
      body.setMass (123);
      Mrand.setMass (123);
      checkInertiaSettings (
         body, 123/body.getVolume(), Mrand, InertiaMethod.EXPLICIT);  

      // explicitly set the inertia from mass
      body.setInertiaFromMass (150);
      Mbody.scale (150/Mbody.getMass());
      checkInertiaSettings (
         body, 150/body.getVolume(), Mbody, InertiaMethod.MASS);  

      // double the mass. Density should double too.
      body.setMass (300);
      Mbody.scale (2);
      density = 300/body.getVolume();
      checkInertiaSettings (body, density, Mbody, InertiaMethod.MASS);  

      // Explicitly set the mass of ball to its mass. Nothing should change.
      ballComp.setMass (ballComp.getMass());
      checkInertiaSettings (body, density, Mbody, InertiaMethod.MASS);  

      // Double the explicit mass of ball. 
      ballComp.setMass (2*ballComp.getMass());
      SpatialInertia Mcheck = new SpatialInertia();
      Mcheck.add (ball.createInertia (ballComp.getMass()/ballComp.getVolume()));
      Mcheck.add (box1.createInertia (density));
      checkInertiaSettings (body, density, Mcheck, InertiaMethod.MASS);  

      // Explicitly set the mass of the box to its mass. Nothing should change.
      boxComp.setMass (boxComp.getMass());
      checkInertiaSettings (body, density, Mcheck, InertiaMethod.MASS);  

      // Double the explicit mass of the box.
      boxComp.setMass (2*boxComp.getMass());
      Mcheck.setZero();
      Mcheck.add (ball.createInertia (ballComp.getMass()/ballComp.getVolume()));
      Mcheck.add (box1.createInertia (boxComp.getMass()/boxComp.getVolume()));
      checkInertiaSettings (body, density, Mcheck, InertiaMethod.MASS);  

      // Set the explicit mass of the ball to 0
      ballComp.setMass (0);
      Mcheck.set (box1.createInertia (boxComp.getMass()/boxComp.getVolume()));
      checkInertiaSettings (body, density, Mcheck, InertiaMethod.MASS);  

      // Make ball mass non-explicit. Density should go to zero since
      // all the mass is now contained in the ball
      ballComp.setMass (-1);
      density = 0;
      checkInertiaSettings (body, density, Mcheck, InertiaMethod.MASS);  

      // Reset inertia from density
      density = 50;
      body.setInertiaFromDensity (density);
      Mcheck.setZero();
      Mcheck.add (ball.createInertia (density));
      Mcheck.add (box1.createInertia (boxComp.getMass()/boxComp.getVolume()));
      checkInertiaSettings (body, density, Mcheck, InertiaMethod.DENSITY);  

      // Make the ball not have mass
      ballComp.setHasMass (false);
      Mbox = box1.createInertia (boxComp.getMass()/boxComp.getVolume());
      checkInertiaSettings (body, density, Mbox, InertiaMethod.DENSITY);  

      // Remove the ball comp
      body.removeMeshComp (ballComp);
      checkInertiaSettings (body, density, Mbox, InertiaMethod.DENSITY);  

      // Make ball have mass again and re-add
      ballComp.setHasMass (true);
      body.addMeshComp (ballComp);
      checkInertiaSettings (body, density, Mcheck, InertiaMethod.DENSITY);  

      // Now try transforming geometry
      RigidTransform3d TBall = new RigidTransform3d (1.0, 0, 0);
      Mball = ball.createInertia (density);
      Mball.transform (TBall);
      ballComp.transformGeometry (TBall);
      Mcheck.setZero();
      Mcheck.add (Mball);
      Mcheck.add (Mbox);
      checkInertiaSettings (body, density, Mcheck, InertiaMethod.DENSITY);

      // Transform the entire body rigidly - should not change the inertia
      RigidTransform3d TBody = new RigidTransform3d (1.2, 4.7, -3.5);
      body.transformGeometry (TBody);
      checkInertiaSettings (body, density, Mcheck, InertiaMethod.DENSITY);

      // Transform back
      RigidTransform3d TBodyInv = new RigidTransform3d();
      TBodyInv.invert (TBody);
      body.transformGeometry (TBodyInv);
      checkInertiaSettings (body, density, Mcheck, InertiaMethod.DENSITY);

      X.setScaling (1.2, 2.3, 2);
      PolygonalMesh ballX = (PolygonalMesh)ballComp.getMesh().copy();
      PolygonalMesh boxX = (PolygonalMesh)boxComp.getMesh().copy();
      ballX.transform (X);
      boxX.transform (X);
      TransformGeometryContext.transform (body, X, 0);
      SpatialInertia Mscaled = new SpatialInertia();

      Mscaled.add (ballX.createInertia (density));
      Mscaled.add (boxX.createInertia (boxComp.getMass()/boxX.computeVolume()));
      checkInertiaSettings (body, density, Mscaled, InertiaMethod.DENSITY);

      // transform back
      AffineTransform3d Xinv = new AffineTransform3d(X);
      Xinv.invert();
      TransformGeometryContext.transform (body, Xinv, 0);
      
      checkInertiaSettings (body, density, Mcheck, InertiaMethod.DENSITY);

      // transform components individually. Should have same result
      ballComp.transformGeometry (X);
      boxComp.transformGeometry (X);
      checkInertiaSettings (body, density, Mscaled, InertiaMethod.DENSITY);
   }

   public void testRigidCompositeBody() {
      // create and add the composite body and plate
      PolygonalMesh ball1 = MeshFactory.createIcosahedralSphere (0.8, 1);
      ball1.transform (new RigidTransform3d (1.5, 0, 0));
      PolygonalMesh ball2 = MeshFactory.createIcosahedralSphere (0.8, 1);
      ball2.transform (new RigidTransform3d (-1.5, 0, 0));
      PolygonalMesh axis = MeshFactory.createCylinder (0.2, 2.0, 12);
      axis.transform (new RigidTransform3d (0, 0, 0, 0, Math.PI/2, 0));

      RigidBody compBody = new RigidBody ("compBody");
      compBody.setDensity (10);
      compBody.addMesh (ball1);
      compBody.addMesh (ball2);
      compBody.addMesh (axis);
      
      RigidBody rigidBody = new RigidBody ("rigidBody");
      rigidBody.setDensity (10);
      rigidBody.addMesh (ball1.clone());
      rigidBody.addMesh (ball2.clone());
      rigidBody.addMesh (axis.clone());

      double volume = 2*ball1.computeVolume() + axis.computeVolume();
      double density = 10;
      SpatialInertia MCheck = new SpatialInertia();
      MCheck.add (ball1.createInertia(density));
      MCheck.add (ball2.createInertia(density));
      MCheck.add (axis.createInertia(density));

      checkInertiaSettings (rigidBody, density, MCheck, InertiaMethod.DENSITY);
      checkInertiaSettings (compBody, density, MCheck, InertiaMethod.DENSITY);

      density = 20;
      rigidBody.setDensity (density);
      compBody.setDensity (density);
      MCheck.scale (2);

      checkInertiaSettings (rigidBody, density, MCheck, InertiaMethod.DENSITY);
      checkInertiaSettings (compBody, density, MCheck, InertiaMethod.DENSITY);

      double mass = 100;
      rigidBody.setInertiaFromMass (mass);
      compBody.setInertiaFromMass (mass);
      density = mass/volume;
      MCheck.scale (mass/MCheck.getMass());
      checkInertiaSettings (rigidBody, density, MCheck, InertiaMethod.MASS);
      checkInertiaSettings (compBody, density, MCheck, InertiaMethod.MASS);

      mass = 123;
      rigidBody.setMass (mass);
      compBody.setMass (mass);
      density = mass/volume;
      MCheck.scale (mass/MCheck.getMass());

      checkInertiaSettings (rigidBody, density, MCheck, InertiaMethod.MASS);
      checkInertiaSettings (compBody, density, MCheck, InertiaMethod.MASS);

      density = 30;
      mass = density*volume;
      rigidBody.setInertiaFromDensity (density);
      compBody.setInertiaFromDensity (density);

      MCheck.scale (mass/MCheck.getMass());

      checkInertiaSettings (rigidBody, density, MCheck, InertiaMethod.DENSITY);
      checkInertiaSettings (compBody, density, MCheck, InertiaMethod.DENSITY);

      SpatialInertia MRand = new SpatialInertia();
      MRand.setRandom();
      rigidBody.setInertia (MRand);
      compBody.setInertia (MRand);
      density = MRand.getMass()/volume;

      checkInertiaSettings (rigidBody, density, MRand, InertiaMethod.EXPLICIT);
      checkInertiaSettings (compBody, density, MRand, InertiaMethod.EXPLICIT);

      rigidBody.setInertiaMethod (InertiaMethod.DENSITY);
      compBody.setInertiaMethod (InertiaMethod.DENSITY);
      MCheck.setZero();
      MCheck.add (ball1.createInertia(density));
      MCheck.add (ball2.createInertia(density));
      MCheck.add (axis.createInertia(density));

      checkInertiaSettings (rigidBody, density, MCheck, InertiaMethod.DENSITY);
      checkInertiaSettings (compBody, density, MCheck, InertiaMethod.DENSITY);

      rigidBody.setInertiaMethod (InertiaMethod.MASS);
      compBody.setInertiaMethod (InertiaMethod.MASS);
      checkInertiaSettings (rigidBody, density, MCheck, InertiaMethod.MASS);
      checkInertiaSettings (compBody, density, MCheck, InertiaMethod.MASS);


   }


   public void test() {
      testInertiaMethods();
      testRigidCompositeBody();
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      RigidBodyTest tester = new RigidBodyTest();
      tester.runtest();
   }

}
