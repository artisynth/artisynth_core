package artisynth.demos.mech;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import maspack.geometry.PolygonalMesh;
import maspack.matrix.AffineTransform3d;
import maspack.matrix.AxisAngle;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.*;
import maspack.spatialmotion.SpatialInertia;
import artisynth.core.mechmodels.CollisionManager;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.MechSystemSolver.Integrator;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.modelbase.StepAdjustment;
import artisynth.core.util.ArtisynthPath;
import artisynth.core.util.TimeBase;
import artisynth.core.workspace.RootModel;
import artisynth.core.gui.ControlPanel;
import artisynth.core.driver.Main;

public class RigidBodyCollision extends RootModel {
   public static String rbpath =
      ArtisynthPath.getHomeRelativePath (
         "src/maspack/geometry/sampleData/", ".");

   RigidBody table, box0, box1, box2, box3, box4;

   Random rand = new Random (0x1234);

   double lastResetTime = 0;

   ArrayList<RigidBody> boxes;

   MechModel mechmod;

   boolean wireFrame = true;

   void setWireFrame (RigidBody body) {
      RenderProps.setWireFrame (body, true);
   }

   public void build (String[] args) throws IOException {

      try {
         mechmod = new MechModel();
         mechmod.setMaxStepSize (0.005);
         boxes = new ArrayList<RigidBody>();
         table = new RigidBody("table");
         table.setDynamic (false);
         table.setMesh (new PolygonalMesh (new File (rbpath + "box.obj")), null);
         AffineTransform3d trans = new AffineTransform3d();
         trans.setIdentity();
         trans.applyScaling (4, 2, 0.5);
         table.transformGeometry (trans);
         table.setPose (new RigidTransform3d (new Vector3d (
            0, 0, 0.8077474533228615), new AxisAngle()));
         table.setInertia (SpatialInertia.createBoxInertia (1, 1, 1, 1));


         mechmod.addRigidBody (table);
         boxes.add (table);
         if (wireFrame) {
            setWireFrame (table);
         }

         box0 = new RigidBody("box0"); // middle box in pile
         box0.setMesh (new PolygonalMesh (new File (rbpath + "box.obj")), null);
         trans.setIdentity();
         trans.applyScaling (0.5, 0.5, 0.5);
         box0.transformGeometry (trans);
         box0.setInertia (SpatialInertia.createBoxInertia (4, 1, 1, 1));

         addBox (box0, Color.GREEN);
         if (wireFrame) {
            setWireFrame (box0);
         }

         box1 = new RigidBody("box1"); // long thin box, bottom of pile
         box1.setMesh (new PolygonalMesh (new File (rbpath + "box.obj")), null);
         trans.setIdentity();
         trans.applyScaling (0.6, 0.1, 1.9);
         box1.transformGeometry (trans);
         box1.setInertia (SpatialInertia.createBoxInertia (1, 1, 0.1, 4));
         addBox (box1, Color.YELLOW);
         if (wireFrame) {
            setWireFrame (box1);
         }

         box2 = new RigidBody("box2"); // left hand box falling on unsupported end of
         // box1
         box2.setMesh (new PolygonalMesh (new File (rbpath + "box.obj")), null);
         trans.setIdentity();
         trans.applyScaling (0.5, 0.5, 0.5);
         box2.transformGeometry (trans);
         box2.setInertia (SpatialInertia.createBoxInertia (20, 1, 1, 1));
         addBox (box2, Color.BLUE);
         if (wireFrame) {
            setWireFrame (box2);
         }

         box3 = new RigidBody("box3"); // top box in pile
         box3.setMesh (new PolygonalMesh (new File (rbpath + "box.obj")), null);
         trans.setIdentity();
         trans.applyScaling (0.4, 0.4, 0.4);
         box3.transformGeometry (trans);
         box3.setInertia (SpatialInertia.createBoxInertia (
            0.5, 0.5, 0.5, 4));
         addBox (box3, Color.CYAN);
         //box3.getMesh().name = "box3";
         if (wireFrame) {
            setWireFrame (box3);
         }

         box4 = new RigidBody("box4"); // solo box off to the right.
         box4.setMesh (new PolygonalMesh (new File (rbpath + "box.obj")), null);
         trans.setIdentity();
         trans.applyScaling (0.6, 0.6, 0.3);
         box4.transformGeometry (trans);
         box4.setInertia (SpatialInertia.createBoxInertia (
            0.5, 0.5, 0.5, 4));
         box4.setPose (new RigidTransform3d (
            new Vector3d (1, 0.0, 5), new AxisAngle (0, 0, 0, 0)));
         addBox (box4, Color.RED);
         //box4.getMesh().name = "box4";
         if (wireFrame) {
            setWireFrame (box4);
         }

         mechmod.setDefaultCollisionBehavior (true, 0.05);

         reset();
         addModel (mechmod);

         ControlPanel panel = new ControlPanel();
         panel.addWidget (mechmod, "integrator");
         panel.addWidget (mechmod, "maxStepSize");
         addControlPanel (panel);
         Main.getMain().arrangeControlPanels (this);

         CollisionManager cm = mechmod.getCollisionManager();
         RenderProps.setVisible (cm, true);
         RenderProps.setLineWidth (cm, 3);      
         RenderProps.setLineColor (cm, Color.RED);
         cm.setDrawContactNormals (true);

         // mechmod.setProfiling (true);
         //mechmod.setIntegrator (Integrator.ConstrainedBackwardEuler);
         // mechmod.setPrintState ("%12.7f");
         //addBreakPoint (0.74);
         for (int i=1; i<=10; i++) {
            addWayPoint (0.1*i);
         }
      }
      catch (IOException e) {
         throw e;
      }
   }

   void addBox (RigidBody box, Color col) {
      mechmod.addRigidBody (box);
      RenderProps.setFaceColor (box, col);
      boxes.add (box);
   }

   void reset() {
      System.out.println ("Reset");
      double q = (rand.nextDouble() - 0.5) * 2;
      double q1 = (rand.nextDouble() - 0.5) * 0.2;
      double q2 = (rand.nextDouble() - 0.5) * 0.2;
      double q3 = rand.nextDouble();
      double t = rand.nextDouble();
      double t1 = rand.nextDouble();
      double t2 = rand.nextDouble();
      double t3 = rand.nextDouble();
      double t1a = rand.nextDouble();
      double t2a = rand.nextDouble();
      double t3a = rand.nextDouble();

      q = 0.24457661495520133;
      q1 = 0.07143622990642744;
      q2 = -0.09679961441554841;
      q3 = 0.147067025570912;
      t = 0.4178296424474287;
      t1 = 0.6413202748594918;
      t2 = 0.0721263882620764;
      t3 = 0.8925255651397567;
      t1a = 0.4168287232167902;
      t2a = 0.2168593863506586;
      t3a = 0.4951442499550106;

      double twist = t * 4;

      if (box0 != null) {
         box0.setPose (new RigidTransform3d (
            new Vector3d (-3 + q, 0.0, 2.05), new AxisAngle (0, 0, 0, 0)));
         box0.setVelocity (0, 0, 0, 0, 0, 0);
      }
      if (box1 != null) {
         box1.setPose (
            new RigidTransform3d (
               new Vector3d (-4.3135585102416233 + q, 0.0, 1.44),
               new AxisAngle (0.5794,-0.5763,-0.5763, Math.toRadians(119.824))));
         box1.setVelocity (0, 0, 0, 0, 0, 0);
      }
      if (box2 != null) {
         box2.setPose (new RigidTransform3d (new Vector3d (
            -6 + q + q1, 0.0, 5.05), new AxisAngle (0, 0, 0, 0)));
         box2.setVelocity (0, 0, 0, twist * t1, twist * t2, twist * t3);
      }
      if (box3 != null) {
         box3.setPose (new RigidTransform3d (new Vector3d (
            -3.2 + q + q1 + q2, 0.0, 3.05), new AxisAngle (0, 0, 0, 0)));
         box3.setVelocity (0, 0, 0, 0, 0, 0);
      }
      if (box4 != null) {
         box4.setPose (new RigidTransform3d (new Vector3d (
            1, 0.0, 2 /* 5 */+ q3), new AxisAngle (0, 0, 0, 0)));
         box4.setVelocity (0, 0, 0, twist * t1a, twist * t2a, twist
         * t3a);
      }

      // // This prints a line of java code that can be copied from the log and
      // pasted above to recreate a specific starting configuration.
      // System.out.println(
      // "q="+q+"; q1="+q1+"; q2="+q2+"; q3="+q3+"; t="+t+"; t1="+t1+"; t2="+t2+"; t3="+t3+"; t1a="+t1a+"; t2a="+t2a+"; t3a="+t3a+";"
      // );
   }

   public synchronized StepAdjustment advance (
      double t0, double t1, int flags) {
      StepAdjustment adj = super.advance (t0, t1, flags);
      if ((adj == null || adj.getScaling() >= 1) && TimeBase.compare (t1-lastResetTime, 6) > 0) {
         reset();
         lastResetTime = t1;
      }
      return adj;
   }
}
