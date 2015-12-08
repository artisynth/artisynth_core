package artisynth.demos.mech;

import java.awt.Color;

import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;
import artisynth.core.materials.RotAxisFrameMaterial;
import artisynth.core.materials.LinearFrameMaterial;
import artisynth.core.util.*;
import artisynth.core.workspace.RootModel;
import maspack.geometry.*;
import maspack.spatialmotion.*;
import maspack.matrix.*;
import maspack.render.*;
import maspack.util.*;

import java.io.*;
import java.net.*;
import java.util.*;

public class LumbarSpringDemo extends RootModel {
   MechModel myMechMod;

   private static double inf = Double.POSITIVE_INFINITY;

   RigidBody myLumbar1;
   RigidBody myLumbar2;

   double boneDensity = 1500;

   public void addFrameSpring (RigidBody bodyA, RigidBody bodyB,
                               double kt, double kr) {
      RigidTransform3d XBA = new RigidTransform3d();
      RigidTransform3d X1A = new RigidTransform3d();
      RigidTransform3d X2B = new RigidTransform3d();

      X2B.setIdentity();

      X1A.mulInverseLeft (bodyA.getPose(), bodyB.getPose());
      X1A.mul (X2B);

      FrameSpring spring = new FrameSpring (null);
      spring.setMaterial (new LinearFrameMaterial (kt, kr, 0, 0));
      RenderProps.setLineColor (spring, Color.RED);
      RenderProps.setLineRadius (spring, 0.0005);
      RenderProps.setLineWidth (spring, 3);
      //RenderProps.setLineStyle (spring, RenderProps.LineStyle.ELLIPSOID);
      spring.setAxisLength (0.02);
      //spring.setRotaryStiffness (kRot);
      spring.setAttachFrameA (X1A);
      spring.setAttachFrameB (X2B);
      myMechMod.attachFrameSpring (bodyA, bodyB, spring);
   }

   private String rigidBodyPath = ArtisynthPath.getSrcRelativePath (
      LumbarSpringDemo.class, "geometry/");

   public RigidBody addBone(String name, double density) {
      
      RigidBody rb = new RigidBody (name);
      PolygonalMesh mesh;
      String meshFileName = rigidBodyPath+name+".obj";
      try {
         mesh = new PolygonalMesh (new File (meshFileName));
      } catch (IOException e) {
         e.printStackTrace();
         return null;
      }
      rb.setMesh (mesh, meshFileName);
      rb.setDensity(boneDensity);
      myMechMod.addRigidBody (rb);
      return rb;
   }

   public void build (String[] args) throws IOException {

      myMechMod = new MechModel ("mech");
      myMechMod.setProfiling (false);
      myMechMod.setGravity (0, 0, 0);
      // myMechMod.setRigidBodyDamper (new FrameDamper (1.0, 4.0));
      myMechMod.setFrameDamping (0.10);
      myMechMod.setRotaryDamping (0.001);
      //myMechMod.setIntegrator (MechSystemSolver.Integrator.SymplecticEuler);

      myLumbar1 = addBone ("lumbar1", 1500);
      myLumbar2 = addBone ("lumbar2", 1500);

      myLumbar2.setDynamic (false);

      addModel (myMechMod);
      //addMonitor (new ArmMover(myLowerArm));

      RigidTransform3d X = new RigidTransform3d();
      X.setIdentity();
      X.R.setRpy (0, 0, Math.toRadians(90));
      myMechMod.transformGeometry (X);

      myLumbar1.getPose (X);
      X.p.set (-0.0160153, 0, 0.0392757);
      myLumbar1.setPose (X);

      addFrameSpring (myLumbar2, myLumbar1, 100, 0.01);
   }
}
