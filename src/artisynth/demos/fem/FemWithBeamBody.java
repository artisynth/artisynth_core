package artisynth.demos.fem;

import maspack.geometry.*;
import maspack.render.*;
import maspack.matrix.*;
import artisynth.core.workspace.*;
import artisynth.core.modelbase.*;
import artisynth.core.femmodels.*;
import artisynth.core.mechmodels.*;
import artisynth.core.materials.*;
import artisynth.core.mechmodels.MechSystemSolver.Integrator;

import java.awt.*;
import java.util.*;

public class FemWithBeamBody extends RootModel {

   private final double EPS = 1e-8;
   protected MechModel mech;

   protected boolean addFem = true;
   protected boolean addSpring = false;
   protected boolean useRigidBody = false;

   public void build (String[] args) {

      // create MechModel and add to RootModel
      mech = new MechModel ("mech");
      mech.setGravity (0, 0, -9.8);
      addModel (mech);

      double density = 1;
      double stiffness = 10;
    
      // create and add the ball and plate
      FemModel3d fem = FemFactory.createHexGrid (
         null, 1.0, 0.25, 0.25, 3, 1, 1);
      fem.setDensity (density);
      fem.setMaterial (new LinearMaterial (5000, 0.33));

      // create and add the ball and plate
      FemModel3d fem2 = FemFactory.createHexGrid (
         null, 1.0, 0.25, 0.25, 1, 1, 1);
      fem.setDensity (density);

      PolygonalMesh mesh =
         MeshFactory.createQuadBox (1.0, 0.25, 0.25, Point3d.ZERO, 20, 5, 5);
      mesh.triangulate();

      RigidBody body = null;
      if (useRigidBody) {
         body = RigidBody.createFromMesh ("", mesh, density, 1);
      }
      else {
         BeamBody beamBody = new BeamBody (mesh, density, 1.0, stiffness); 
         beamBody.setMaterial (new LinearMaterial (10, 0.33));
         body = beamBody;
      }
      body.transformGeometry (new RigidTransform3d (1, 0, 0));

      fem2.transformGeometry (new RigidTransform3d (1, 0, 0));

      RenderProps.setSphericalPoints (mech, 0.02, Color.GREEN);

      if (addFem) {
         mech.addModel (fem);
      }
      //mech.addModel (fem2);
      mech.addRigidBody (body);

      FrameMarker mkr = mech.addFrameMarker (body, new Point3d(0.4, -0.125, 0));
      RenderProps.setSphericalPoints (mkr, 0.02, Color.BLUE);   

      SolidJoint joint = new SolidJoint ();
      joint.setBodies (body, null, new RigidTransform3d (0.5, 0, 0));
      //mech.addBodyConnector (joint);

      if (addFem) {
         for (FemNode n : fem.getNodes()) {
            Point3d pos = n.getPosition();
            if (pos.x <= -0.5+EPS) {
               n.setDynamic (false);
            }
            else if (pos.x >= 0.5-EPS) {
               //mech.attachPoint (n, fem2, 1e-8);
               mech.attachPoint (n, body);
            }
         }
      }
      if (addSpring) {
         Particle p0 = new Particle (1, -0.5, 0, 0);
         Particle p1 = new Particle (1, 0.5, 0, 0);
         p0.setDynamic (false);

         AxialSpring spr = new AxialSpring (10, 0, 0);
         mech.addParticle (p0);
         mech.addParticle (p1);
         mech.attachAxialSpring (p0, p1, spr);
         mech.attachPoint (p1, body);
      }

      SolveMatrixTest tester = new SolveMatrixTest();
      //System.out.println ("error=" + tester.testStiffness (mech, 1e-8));
   }

   public StepAdjustment advance (double t0, double t1, int flags) {
      StepAdjustment adj = super.advance (t0, t1, flags);

      SolveMatrixTest tester = new SolveMatrixTest();
      //System.out.println ("error=" + tester.testStiffness (mech, 1e-8));

      return adj;
   }
}
