package artisynth.demos.inverse;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;

import artisynth.core.femmodels.FemElement3d;
import artisynth.core.femmodels.FemFactory;
import artisynth.core.femmodels.FemMuscleModel;
import artisynth.core.femmodels.FemNode;
import artisynth.core.femmodels.MuscleBundle;
import artisynth.core.inverse.TargetPoint;
import artisynth.core.inverse.TrackingController;
import artisynth.core.materials.SimpleForceMuscle;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.MotionTargetComponent;
import artisynth.core.mechmodels.Point;
import artisynth.core.mechmodels.PointList;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.modelbase.ControllerBase;
import artisynth.core.modelbase.StepAdjustment;
import artisynth.core.workspace.RootModel;
import maspack.geometry.BVFeatureQuery;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector2d;
import maspack.matrix.Vector3d;
import maspack.render.RenderProps;
import maspack.render.Renderer.FaceStyle;
import maspack.render.Renderer.PointStyle;

public class FemSurfaceTargetDemo extends RootModel {

   MechModel mech;
   FemMuscleModel fem;
   RigidBody body;

   double l = 10;
   double w = 5;
   int nl = 2;
   int nw = 1;
   static final double eps = 1e-6;

   double muscleMaxStress = 300000.0;

   @Override
   public void build (String[] args) throws IOException {
      super.build (args);

      mech = new MechModel ("mech");
      mech.setGravity (Vector3d.ZERO);
      addModel (mech);

      fem = new FemMuscleModel ("fem");
      fem.setStiffnessDamping (0.1);
      mech.addModel (fem);

      // boundary conditions
      FemFactory.createHexGrid (fem, l, w, w, nl, nw, nw);
      for (FemNode n : fem.getNodes ()) {
         if (n.getPosition ().x > l / 2 - eps) {
            n.setDynamic (false);
         }
      }

      // muscles
      addMuscle ("vert", Color.RED, Vector3d.Z_UNIT);
      addMuscle ("trans", Color.BLUE, Vector3d.Y_UNIT);

      body = new RigidBody ("plate");
      body.setMesh (MeshFactory.createBox (l, l, l / 10));
      body.setPose (
         new RigidTransform3d (-1.2 * w - l / 20, 0, 0, 0, 1, 0, Math.PI / 2));
      body.setDynamic (false);
      mech.addRigidBody (body);

      mech.setCollisionBehavior (body, fem, true);

      addTrackingController ();
      setupRenderProps ();
   }

   public void addMuscle (String name, Color color, Vector3d dir) {
      MuscleBundle b = new MuscleBundle (name);
      for (FemElement3d e : fem.getElements ()) {
         b.addElement (e, dir);
      }
      b.setMuscleMaterial (new SimpleForceMuscle (muscleMaxStress));
      b.setDirectionRenderLen (w / 4);
      RenderProps.setLineColor (b, color);
      RenderProps.setLineWidth (b, 4);
      fem.addMuscleBundle (b);
   }

   public void addTrackingController () {
      TrackingController tcon = new TrackingController (mech, "tcon");

      for (FemNode n : fem.getNodes ()) {
         if (n.getPosition ().x < -l / 2 + eps) {
            tcon.addMotionTarget (n);
            RenderProps.setAlpha (n, 0.5);
         }
      }

      for (MuscleBundle b : fem.getMuscleBundles ()) {
         tcon.addExciter (b);
      }

      // project reference points to body
      addController (
         new SurfaceTargetController (
            body.getMesh (), tcon.getMotionSources (),
            tcon.getTargetPoints ()));
      for (TargetPoint p : tcon.getTargetPoints ()) {
         RenderProps.setPointRadius (p, l / 100);
      }

      tcon.addL2RegularizationTerm (0.1);

      tcon.setProbeDuration (10);
      tcon.createProbesAndPanel (this);
      addController (tcon);
   }

   public void setupRenderProps () {
      RenderProps.setFaceStyle (body, FaceStyle.NONE);
      RenderProps.setPointStyle (mech, PointStyle.SPHERE);
      RenderProps.setPointRadius (mech, l / 40);
      RenderProps.setLineWidth (mech, 4);
      RenderProps.setEdgeWidth (mech, 4);
      RenderProps.setPointColor (fem, Color.ORANGE.darker ());
      RenderProps.setDrawEdges (body, true);
      RenderProps.setLineColor (body, Color.CYAN.darker ());
   }

   @Override
   public StepAdjustment advance (double t0, double t1, int flags) {
      return super.advance (t0, t1, flags);
   }

   public class SurfaceTargetController extends ControllerBase {

      PolygonalMesh myMesh;
      Point[] myTargetPts;
      Point[] mySourcePts;
      BVFeatureQuery query = new BVFeatureQuery ();
      Point3d near = new Point3d ();
      Vector2d uv = new Vector2d ();

      public SurfaceTargetController (PolygonalMesh mesh,
      ArrayList<MotionTargetComponent> sourcePts,
      PointList<TargetPoint> targetPts) {
         myMesh = mesh;
         myTargetPts = new Point[targetPts.size ()];
         targetPts.toArray (myTargetPts);

         mySourcePts = sourcePts.toArray (new Point[0]);
      }

      @Override
      public void apply (double t0, double t1) {
         for (int i = 0; i < myTargetPts.length; ++i) {
            query.nearestFaceToPoint (
               near, uv, myMesh, mySourcePts[i].getPosition ());
            myTargetPts[i].setPosition (near);
         }

      }

   }

}
