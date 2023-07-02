package artisynth.demos.test;

import java.awt.Color;
import java.io.*;
import java.util.*;

import artisynth.core.workspace.*;
import artisynth.core.mechmodels.*;
import artisynth.core.mechmodels.CollisionManager.*;
import artisynth.core.mechmodels.CollisionBehavior.*;
import artisynth.core.modelbase.*;
import artisynth.core.femmodels.*;
import artisynth.core.materials.*;
import artisynth.core.probes.*;
import artisynth.core.gui.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.geometry.*;
import maspack.render.*;
import maspack.render.Renderer.*;
import maspack.properties.*;

/**
 * Demo to check the relationship between force and average acceleration
 * on a rigid ball undergoing elastic foundation contact (EFC).
 */
public class EFCForceTest extends RootModel {

   MechModel myMech;
   RigidBody myBall;
   CollisionResponse myResp;
   double myStiffness = 2000;

   // make spring stiffness a property of the RootModel so that we can adjust
   // all 4 spring stiffnesses in one go
   public static PropertyList myProps =
      new PropertyList (EFCForceTest.class, RootModel.class);

   static {
      myProps.add ("springStiffness", "stiffness the springs", 2000);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public double getSpringStiffness() {
      return myStiffness;
   }

   public void setSpringStiffness (double k) {
      myStiffness = k;
      for (AxialSpring spr : myMech.axialSprings()) {
         spr.setMaterial(new LinearAxialMaterial (k, 0));
      }
   }

   /**
    * Connects two points by a spring and adds the spring to a MechModel.
    */
   private void addSpring (MechModel mech, Point p0, Point p1) {
      AxialSpring spr = new AxialSpring (myStiffness, 0, 0);
      mech.attachAxialSpring (p0, p1, spr);      
   }

   public void build (String[] args) {
      myMech = new MechModel ("mech");
      myMech.setIntegrator (
         MechSystemSolver.Integrator.ConstrainedForwardEuler);
      addModel (myMech);

      double rad = 1.5;  // sphere radius
      double pw = 8.0;   // width of the plate
      double pd = 0.6;   // depth of the plate
      double msep = 0.5; // plate marker separation

      // create the ball and the contacting plate
      myBall = RigidBody.createIcosahedralSphere ("ball", rad, 1, /*divs=*/2);
      myBall.setPose (new RigidTransform3d (0, 0, rad+pd/2, 0.4, 0.1, 0.1));
      myMech.addRigidBody (myBall);
      RigidBody plate = RigidBody.createBox ("plate", pw, pw, pd, 1);
      plate.setDynamic (false);
      myMech.addRigidBody (plate);
      myMech.setGravity (0, 0, -500);

      // add some inertial damping to the ball
      myBall.setInertialDamping (1.0);

      // attach markers to the plate
      FrameMarker[] mkrs = new FrameMarker[6];
      mkrs[0] = myMech.addFrameMarker (plate, new Point3d(-pw/2, -msep/2, pd/2));
      mkrs[1] = myMech.addFrameMarker (plate, new Point3d( pw/2, -msep/2, pd/2));
      mkrs[2] = myMech.addFrameMarker (plate, new Point3d(-pw/2,  msep/2, pd/2));
      mkrs[3] = myMech.addFrameMarker (plate, new Point3d( pw/2,  msep/2, pd/2));

      // attach markers to the ball
      double ang = Math.toRadians(10); // ball marker angle from top
      double mz = rad*Math.cos(ang) + rad + pd/2;
      double my = rad*Math.sin(ang);
      mkrs[4] = myMech.addFrameMarkerWorld (myBall, new Point3d(0, -my, mz));
      mkrs[5] = myMech.addFrameMarkerWorld (myBall, new Point3d(0,  my, mz));

      // add springs between the markers to apply force to the ball
      addSpring (myMech, mkrs[0], mkrs[4]);
      addSpring (myMech, mkrs[1], mkrs[4]);
      addSpring (myMech, mkrs[2], mkrs[5]);
      addSpring (myMech, mkrs[3], mkrs[5]);

      // turn on collisions between sphere and plate
      CollisionBehavior behav = new CollisionBehavior (true, 0);
      // collisions are calculated based on penetration of the rigid body
      // vertices
      behav.setMethod (
         CollisionBehavior.Method.VERTEX_PENETRATION);
      behav.setColliderType (ColliderType.AJL_CONTOUR);
      // enable elastic foundation contact (EFC)
      LinearElasticContact lec = new LinearElasticContact(3000,0.45,1,0.1);
      lec.setDampingMethod (ElasticContactBase.DampingMethod.FORCE);
      behav.setForceBehavior (lec);
      myMech.setCollisionBehavior (myBall,plate,behav);
      
      // object to collect collision responses
      myResp = myMech.setCollisionResponse (myBall, plate);

      // rendering properties:

      // make plate and ball transparent so that contact forces can be seen
      // clearly
      RenderProps.setDrawEdges (myBall, true);
      RenderProps.setEdgeColor (myBall, Color.WHITE);
      RenderProps.setFaceStyle (myBall, FaceStyle.NONE);
      RenderProps.setDrawEdges (plate, true);
      RenderProps.setEdgeColor (plate, Color.WHITE);
      RenderProps.setFaceStyle (plate, FaceStyle.NONE);

      // draw springs as red spindles and markers as white spheres
      RenderProps.setSpindleLines (myMech, 0.05, Color.RED);
      RenderProps.setSphericalPoints (myMech, 0.1, Color.WHITE);
      
      // enable rendering of contact forces
      CollisionManager cm = myMech.getCollisionManager();
      RenderProps.setVisible (cm, true);
      cm.setDrawContactForces (true);
      cm.setContactForceLenScale (0.0002);
      RenderProps.setSolidArrowLines (cm, 0.03, Color.GREEN);

      // create a control panel to adjust settings
      ControlPanel panel = new ControlPanel();
      panel.addWidget (myMech, "integrator");
      panel.addWidget (this, "springStiffness");
      panel.addWidget (myBall, "inertialDamping");
      panel.addWidget (cm, "reportNegContactForces");
      panel.addWidget (behav, "forceBehavior");
      addControlPanel (panel);
   }

   /**
    * Override the advance method so that we can collect the translational
    * forces acting on the ball and compare with average acceleration times
    * mass.
    */
   public StepAdjustment advance (double t0, double t1, int flags) {

      // ball translational velocity before advance
      Vector3d vel0 = new Vector3d(myBall.getVelocity().v);

      StepAdjustment sa = super.advance (t0, t1, flags);

      // ball translational velocity after advance
      Vector3d vel1 = new Vector3d(myBall.getVelocity().v);
      // compute macc: mass X average acceleration
      Vector3d macc = new Vector3d();
      macc.sub (vel1, vel0);
      macc.scale (myBall.getMass()/(t1-t0));

      // collect all forces acting on the ball:
      Vector3d fnet = new Vector3d();
      Vector3d fcon = new Vector3d();
      // contact forces
      Map<Vertex3d,Vector3d> colMap = myResp.getContactForces (0);
      for (Vector3d f : colMap.values()) {
         fcon.add (f);
      }
      fnet.add (fcon);
      // marker forces
      FrameMarker mkr4 = myMech.frameMarkers().get(4);
      FrameMarker mkr5 = myMech.frameMarkers().get(5);
      fnet.add (mkr4.getForce());
      fnet.add (mkr5.getForce());
      // gravity
      double mass = myBall.getMass();
      fnet.scaledAdd (mass, myMech.getGravity());
      // inertial damping
      fnet.scaledAdd (-mass*myBall.getInertialDamping(), vel0);

      // print out net forces on the ball and macc. For the
      // ConstrainedForwardEuler integrator, these should be the same within
      // floating point precision.
      System.out.println ("t1: " + t1);
      System.out.println (" fnet: " + fnet.toString("%14.8f"));
      System.out.println (" macc: " + macc.toString("%14.8f"));
      return sa;
   }

}
