package artisynth.demos.test;

import java.awt.Color;
import java.io.IOException;

import artisynth.core.mechmodels.FixedMeshBody;
import artisynth.core.workspace.RootModel;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.AxisAngle;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderProps;
import maspack.render.Renderable;
import maspack.spatialmotion.*;
import artisynth.core.workspace.*;
import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;
import artisynth.core.probes.*;
import maspack.collision.*;

public class CSGAnimation extends RootModel {

   RigidBody myMainBod;

   void setMainMesh (PolygonalMesh mesh) {
      myMainBod.setMesh (mesh);
      RenderProps.setEdgeColor (myMainBod, Color.WHITE);
      //RenderProps.setDrawEdges (myMainBod, true);
      RenderProps.setFaceColor (myMainBod, new Color (0.6f, 0.6f, 1f));
   }
   
   @Override
   public void build(String[] args) throws IOException {
      MechModel mech = new MechModel("mech");
      addModel (mech);

      PolygonalMesh torus = MeshFactory.createTorus (1.0, 0.3, 30, 30);
      torus.transform (
         new RigidTransform3d (0, 0, 0.75, 0, 0, Math.PI/2));
      myMainBod = RigidBody.createFromMesh ("mesh", torus, 1000, 1.0);
      myMainBod.setDynamic (false);
      setMainMesh (torus);
      mech.addRigidBody (myMainBod);

      RigidBody table = RigidBody.createBox ("table", 4, 4, 0.4, 1000);
      table.setDynamic (false);
      table.transformGeometry (new RigidTransform3d (0, 0, -3));
      mech.addRigidBody (table);
      mech.setDefaultCollisionBehavior (true, 0.3);
      mech.setGravity (new Vector3d (0, 0, -300));
   }

   private double[] computeEyeData (
      RootModel root, double totalDegrees, double time, int nsegs) {


      Vector3d center = new Vector3d (root.getViewerCenter());
      Vector3d eyec0 = new Vector3d (root.getViewerEye());      
      eyec0.sub (center);
      Vector3d eyec = new Vector3d();

      double[] data = new double[4*(nsegs+1)];
      for (int i=0; i<=nsegs; i++) {
         double ang = i*Math.toRadians(totalDegrees)/(nsegs);
         double s = Math.sin(ang);
         double c = Math.cos(ang);

         eyec.x = c*eyec0.x - s*eyec0.y;
         eyec.y = s*eyec0.x + c*eyec0.y;
         eyec.z = eyec0.z;

         data[i*4+0] = i*(time/nsegs);
         data[i*4+1] = eyec.x + center.x;
         data[i*4+2] = eyec.y + center.y;
         data[i*4+3] = eyec.z + center.z;
      }
      return data;
   }

   NumericInputProbe addPanProbe (
      RootModel root, double deg, double t0, double t1) {
      
      Vector3d eye0 = new Vector3d (root.getViewerEye());

      int nsegs = Math.max (3, (int)Math.ceil(deg/10.0));

      NumericInputProbe inprobe =
         new NumericInputProbe (
            this, "viewerEye", t0, t1);
      double z = 0.8;
      inprobe.addData (
         computeEyeData (root, deg, t1-t0, nsegs),
         NumericInputProbe.EXPLICIT_TIME);
      addInputProbe (inprobe);
      return inprobe;
   }

   NumericInputProbe addFadeOut (ModelComponent comp, double t0, double t1) {

      NumericInputProbe inprobe =
         new NumericInputProbe (
            comp, "renderProps.alpha", t0, t1);
      inprobe.addData (
         new double[] {
            0,     1.0,
            t1-t0, 0.0
         },
         NumericInputProbe.EXPLICIT_TIME);
      addInputProbe (inprobe);
      return inprobe;

   }

   NumericInputProbe addFadeIn (ModelComponent comp, double t0, double t1) {

      NumericInputProbe inprobe =
         new NumericInputProbe (
            comp, "renderProps.alpha", t0, t1);
      inprobe.addData (
         new double[] {
            0,     0.0,
            t1-t0, 1.0
         },
         NumericInputProbe.EXPLICIT_TIME);
      addInputProbe (inprobe);
      return inprobe;

   }

   public void attach (DriverInterface di) {
      addPanProbe (this, 360, 0, 15);
   }

   FixedMeshBody myTorusBod;

   PolygonalMesh getUnion (RigidBody bod0, FixedMeshBody bod1) {
      SurfaceMeshIntersector smi = new SurfaceMeshIntersector();      
      PolygonalMesh union = 
         smi.findUnion (
            bod0.getSurfaceMesh(), (PolygonalMesh)bod1.getMesh());
      return union;
   }

   PolygonalMesh getDifference (RigidBody bod0, FixedMeshBody bod1) {
      SurfaceMeshIntersector smi = new SurfaceMeshIntersector();      
      PolygonalMesh diff = 
         smi.findDifference01 (
            bod0.getSurfaceMesh(), (PolygonalMesh)bod1.getMesh());
      return diff;
   }

   public StepAdjustment advance (double t0, double t1, int flags) {
      if (t1 == 2) {
         PolygonalMesh torus = MeshFactory.createTorus (1.0, 0.3, 30, 30);
         torus.transform (
            new RigidTransform3d (0, 0, -0.75, 0, 0, Math.PI/2));
         myTorusBod = new FixedMeshBody (torus);
         addRenderable (myTorusBod);
         addFadeIn (myTorusBod, 2, 4);
      }
      if (t1 == 4) {
         SurfaceMeshIntersector smi = new SurfaceMeshIntersector();
         PolygonalMesh union = getUnion (myMainBod, myTorusBod);
         setMainMesh (union);
         removeRenderable (myTorusBod);
      }
      if (t1 == 6) {
         PolygonalMesh torus = MeshFactory.createTorus (1.0, 0.2, 30, 30);
         myTorusBod = new FixedMeshBody (torus);

         SurfaceMeshIntersector smi = new SurfaceMeshIntersector();
         PolygonalMesh diff = getDifference (myMainBod, myTorusBod);
         setMainMesh (diff);
         addFadeOut (myTorusBod, 6.5, 8.5);
         addRenderable (myTorusBod);
      }
      if (t1 == 8.5) {
         removeRenderable (myTorusBod);
      }
      if (t1 == 9.5) {
         myMainBod.setVelocity (
            new Twist (0.001, 0.001, 0.001, 0.01, 0.001, 0.001));
         myMainBod.setDynamic (true);
      }
      return super.advance (t0, t1, flags);
   }
}
