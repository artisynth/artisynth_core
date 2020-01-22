package artisynth.demos.fem;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import maspack.geometry.Face;
import maspack.geometry.HalfEdge;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.MeshFactory;
import maspack.matrix.AffineTransform3d;
import maspack.matrix.AxisAngle;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.spatialmotion.SpatialInertia;
import maspack.render.RenderProps;
import maspack.render.Renderable;
import maspack.render.Renderer;
import maspack.render.Renderer.Shading;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.FemFactory;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.TetGenReader;
import artisynth.core.mechmodels.CollisionManager;
import artisynth.core.mechmodels.Collidable;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.MechSystemSolver.Integrator;
import artisynth.core.modelbase.StepAdjustment;
import artisynth.core.util.ArtisynthPath;
import artisynth.core.util.TimeBase;
import artisynth.core.workspace.RootModel;

public class FemCollision extends RootModel {
   public static String rbpath =
      ArtisynthPath.getHomeRelativePath (
         "src/maspack/geometry/sampleData/", ".");

   public static String fempath =
      ArtisynthPath.getHomeRelativePath (
         "src/artisynth/core/femmodels/meshes/", ".");

   Random rand = new Random();

   double lastResetTime = 0;

   public static FemModel3d fem0, fem1;

   RigidBody box0;

   double mu = 0.1;

   boolean incBox0 = true, incFem0 = true, incFem1 = true, wireFrame = false;

   // boolean incBox0=false, incFem0=false, incFem1=true, wireFrame=true;
   boolean testEdgeEdge = false;

   public void build (String[] args) throws IOException {

      try {

         MechModel mechmod = new MechModel();
         //mechmod.setIntegrator (Integrator.ConstrainedBackwardEuler);
         mechmod.setIntegrator(Integrator.BackwardEuler);
         //mechmod.setProfiling (true);

         CollisionManager collisions = mechmod.getCollisionManager();
         RenderProps.setVisible (collisions, true);
         RenderProps.setEdgeWidth (collisions, 4);      
         RenderProps.setEdgeColor (collisions, Color.YELLOW);
         RenderProps.setLineWidth (collisions, 3);      
         RenderProps.setLineColor (collisions, Color.GREEN);
         collisions.setDrawContactNormals (true);
                  
         RigidBody table = new RigidBody("table");
         table.setDynamic (false);
         //table.setMesh (new PolygonalMesh (new File (rbpath+ "box.obj")), null);
         table.setMesh (MeshFactory.createBox (2, 2, 2));
         AffineTransform3d trans = new AffineTransform3d();
         trans.setIdentity();
         trans.applyScaling (4, 2, 0.5);
         table.transformGeometry (trans);
         table.setPose (
            new RigidTransform3d (
               new Vector3d (1, 0, 0.8077474533228615),
               new AxisAngle (1, 0, 0, Math.toRadians (mu == 0 ? 0.0 : 1.5))));
         if (wireFrame) {
            RenderProps.setFaceStyle (table, Renderer.FaceStyle.NONE);
            RenderProps.setDrawEdges (table, true);
         }
         mechmod.addRigidBody (table);

         if (incBox0) {
            box0 = new RigidBody("box0");
            //box0.setMesh (
            //   new PolygonalMesh (new File (rbpath + "box.obj")), null);
            box0.setMesh (MeshFactory.createBox (2, 2, 2));
            trans.setIdentity();
            trans.applyScaling (1.5, 1.5, 0.5);
            box0.transformGeometry (trans);
            box0.setInertia (SpatialInertia.createBoxInertia (
               10000, 4, 4, 1));
            box0.setPose (new RigidTransform3d (
               new Vector3d (-0.5, 0, 3.5), new AxisAngle()));
            RenderProps.setFaceColor (box0, Color.GREEN.darker());
            if (wireFrame) {
               RenderProps.setFaceStyle (box0, Renderer.FaceStyle.NONE);
               RenderProps.setDrawEdges (box0, true);
            }
            mechmod.addRigidBody (box0);
         }

         /*
          * Tetgen t = new Tetgen("pvq1.2a0.03"); PolygonalMesh pm = new
          * PolygonalMesh(new File(rbpath + "box.obj"));
          * pm.writePoly("test.poly"); fem0 = t.createFemModel3d("fem0", pm,
          * 5000, new Vector3d(0.8, 0.8, 0.8));
          */

         if (incFem0) {
            String fem0Name =
            // "torus508";
               // "torus422";
               // "torus546";
               "sphere2";
            // "box0023";
            // "box0048";
            // "box0144";
            // "box0604";
            // "box1056";
            // "box2463";
            // "box4257";
            fem0 =
               TetGenReader.read (
                  "fem0", 5000, fempath + fem0Name + ".1.node", fempath
                  + fem0Name + ".1.ele", new Vector3d (0.8, 0.8, 0.8));
            fem0.transformGeometry (new RigidTransform3d (
               new Vector3d (2, 0, 3.5), new AxisAngle()));
            fem0.setLinearMaterial (1000000, 0.33, true);
            if (!wireFrame)
               fem0.setSurfaceRendering (SurfaceRender.Shaded);
            fem0.setParticleDamping (0.1);
            RenderProps.setFaceColor (fem0, new Color (0.5f, 0f, 0f));
//            RenderProps.setAlpha(fem0, 0.33);
            RenderProps.setAlpha(fem0, 0.5);
            RenderProps.setShading(fem0, Shading.NONE);
            RenderProps.setDrawEdges(fem0, true);
            RenderProps.setVisible(fem0.getElements(), false);
            RenderProps.setVisible(fem0.getNodes(), false);
//            RenderProps.setLineColor(fem0, Color.GRAY);
            mechmod.addModel (fem0);
         }

         if (incFem1) {

            // Use this code for a box
            /*
             * double mySize = 1; fem1 = createFem (name, mySize);
             * FemFactory.createTetGrid ( fem1, 1*mySize, 4*mySize, mySize, 1,
             * 1, 1);
             */
            // end box code

            // Use this code for a ball
            String fem1Name = "sphere2";
            fem1 =
               TetGenReader.read (
                  "fem1", 5000, fempath + fem1Name + ".1.node", fempath
                  + fem1Name + ".1.ele", new Vector3d (0.8, 0.8, 0.8));
            fem1.setLinearMaterial (1000000, 0.33, true);
            fem1.setParticleDamping (0.1);
            // end ball code
            fem1.setIncompressible (FemModel3d.IncompMethod.AUTO);
            fem1.transformGeometry (new RigidTransform3d (
               new Vector3d (1.25, 0, 2), new AxisAngle()));
            fem1.setSurfaceRendering (
               wireFrame ? SurfaceRender.None : SurfaceRender.Shaded);
            RenderProps.setAlpha(fem1, 0.5);
            RenderProps.setShading(fem1, Shading.NONE);
            RenderProps.setDrawEdges(fem1, true);
            RenderProps.setVisible(fem1.getElements(), false);
            RenderProps.setVisible(fem1.getNodes(), false);
            RenderProps.setFaceColor (fem1, new Color (0f, 0f, 0.8f));
            mechmod.addModel (fem1);
         }

         if (incFem0) {
            mechmod.setCollisionBehavior (fem0, table, true, mu);
            mechmod.setCollisionResponse (fem0, Collidable.Deformable);
            mechmod.setCollisionResponse (fem0, table);
         }
         if (incFem1 & incFem0) {
            mechmod.setCollisionBehavior (fem0, fem1, true, mu);
         }
         if (incBox0 & incFem0) {
            mechmod.setCollisionBehavior (box0, fem0, true, mu);
         }
         if (incFem1) {
            mechmod.setCollisionBehavior (fem1, table, true, mu);
            mechmod.setCollisionResponse (fem1, Collidable.AllBodies);
         }
         if (incBox0 & incFem1) {
            mechmod.setCollisionBehavior (box0, fem1, true, mu);
         }
         if (incBox0) {
            mechmod.setCollisionBehavior (box0, table, true, mu);
            mechmod.setCollisionResponse (box0, table);
         }

         addModel (mechmod);
         for (int i=1; i<=10; i++) {
            addWayPoint (i*0.5);
         }
         addBreakPoint (2.33);
      }
      catch (IOException e) {
         throw e;
      }
   }

   private FemModel3d createFem (String name, double mySize) {
      double myDensity = 1000;
      double myYoungsModulus = 100000;
      double myPoissonsRatio = 0.33;
      double myParticleDamping = 2.0;
      double myStiffnessDamping = 0.002;
      FemModel3d fem = new FemModel3d (name);
      fem.setDensity (myDensity);
      fem.setParticleDamping (myParticleDamping);
      fem.setStiffnessDamping (myStiffnessDamping);
//      fem.setPoissonsRatio (myPoissonsRatio);
//      fem.setYoungsModulus (myYoungsModulus);
      fem.setLinearMaterial (myYoungsModulus, myPoissonsRatio, true);
      RenderProps.setPointStyle (fem, Renderer.PointStyle.SPHERE);
      RenderProps.setPointRadius (fem, mySize / 50);
      return fem;
   }

   void positionBall (FemModel3d ball, Vector3d startPos) {
      if (ball == null)
         return;
      Vector3d avg = new Vector3d();
      for (FemNode3d n : ball.getNodes())
         avg.add (n.getPosition());
      avg.scale (1 / (double)ball.getNodes().size());
      Vector3d pos = new Vector3d (startPos);
      pos.sub (avg);
      ball.transformGeometry (new RigidTransform3d (pos, new AxisAngle()));
      for (FemNode3d n : ball.getNodes()) {
         n.setVelocity (0, 0, 0);
      }
   }

   void reset() {
      double r1 = rand.nextDouble();
      double r2 = rand.nextDouble();
      double r3 = rand.nextDouble();

      // case001: the blue ball has a very strange wobbling experience partly
      // inside the table, just after touching the red ball.
      r1 = 0.9339890095435555;
      r2 = 0.256644068300397;
      r3 = 0.005600826760283728;

      positionBall (fem0, new Vector3d (0.1, 0.1, 10 + r3 * 2));
      positionBall (fem1, testEdgeEdge ? new Vector3d (0, 0, 6 /* 1.7 */)
         : new Vector3d (0, 0, 2));
      if (box0 != null) {
         box0.setPose (new RigidTransform3d (new Vector3d (
            -0.5, r1 * 0.2, 3.0 + r2), new AxisAngle()));
         box0.setVelocity (0, 0, 0, 0, 0, 0);
      }

      System.out.println ("r1=" + r1 + "; r2=" + r2 + "; r3=" + r3 + ";");
   }

   public synchronized StepAdjustment advance (
      double t0, double t1, int flags) {
      StepAdjustment adj = super.advance (t0, t1, flags);
      if ((adj == null || adj.getScaling() >= 1)) {
         if (TimeBase.compare(t1-lastResetTime, 19.0) > 0) {
            reset();
            lastResetTime = t1;
         }
      }
      if (t1 == 2.34) {
         fem1.setIncompressible (FemModel3d.IncompMethod.OFF);
      }
      return adj;
   }

}
 
