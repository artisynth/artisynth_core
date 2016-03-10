package artisynth.demos.fem;

import java.awt.Color;
import java.awt.Point;
import java.util.*;
import java.io.*;

import javax.swing.*;

import maspack.geometry.*;
import maspack.matrix.*;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.*;
import artisynth.core.gui.*;
import artisynth.core.mechmodels.MechSystemSolver.Integrator;
import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;
import artisynth.core.util.ArtisynthPath;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.RootModel;
import maspack.properties.PropertyDesc;
import maspack.properties.PropertyList;
import maspack.render.*;
import maspack.render.Renderer;
import maspack.spatialmotion.SpatialInertia;
import artisynth.core.driver.*;

import java.awt.*;

public class FishDemo extends RootModel {
   public static boolean debug = false;

   FemModel3d myFemMod;

   MechModel myMechMod;

   static double myDensity = 1000;

   public static PropertyList myProps =
      new PropertyList (FishDemo.class, RootModel.class);

   static {
      myProps.add ("value * *", "a value", 0, "[-1,3] AE");
   }

   double v = 0;

   public double getValue() {
      return v;
   }

   public void setValue (double newv) {
      v = newv;
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public static String rbpath =
      ArtisynthPath.getHomeRelativePath (
         "src/maspack/geometry/sampleData/", ".");

   public static String fempath =
      ArtisynthPath.getHomeRelativePath (
         "src/artisynth/core/femmodels/meshes/", ".");

   public void build (String[] args) throws IOException {

      myMechMod = new MechModel();

      RigidBody collisionbody0 = new RigidBody();
      collisionbody0.setDynamic (false);
      RenderProps.setFaceStyle (
         collisionbody0, Renderer.FaceStyle.FRONT_AND_BACK);

      try {
         collisionbody0.setMesh (new PolygonalMesh (new File (rbpath
         + "box.obj")), "");
         collisionbody0.getMesh().transform (
            new RigidTransform3d (new Vector3d (0, 0, -1.2), new AxisAngle (
               0, 1, 0, 0)));
      }
      catch (Exception e) {
      }

      // FemModel3d collisionbody1 = FemModel3d.createGrid("fem", 0.6, 0.6, 0.3,
      // 8, 8, 4, myDensity);
      // FemModel3d collisionbody2 = TetGenReader.read("fem1", myDensity,
      // fempath + "muscle.1.node", fempath + "muscle.1.ele", 0.2);
      FemMuscleModel collisionbody2 =
         new PointToPointMuscle ("fem1", myDensity, 0.2, "muscle", true);

      RenderProps.setPointStyle (
         collisionbody2.markers(), Renderer.PointStyle.SPHERE);
      RenderProps.setPointColor (collisionbody2.markers(), new Color (
         0.5f, 0.5f, 1f));
      RenderProps.setLineStyle (
         collisionbody2.getMuscleBundles(), Renderer.LineStyle.SPINDLE);
      RenderProps.setLineRadius (collisionbody2.getMuscleBundles(), 0.05);

      collisionbody2.clearMuscleBundles();

      collisionbody2.transformGeometry (new RigidTransform3d (new Vector3d (
         0, 0, 0.5), new AxisAngle()));
      collisionbody2.setSurfaceRendering (SurfaceRender.Shaded);

      myMechMod.addRigidBody (collisionbody0);
      myMechMod.setProfiling (false);
      RenderProps p = new RenderProps();
      p.setFaceColor (Color.RED);
      p.setLineColor (Color.WHITE);
      p.setLineWidth (1);
      p.setFaceStyle (Renderer.FaceStyle.FRONT_AND_BACK);
      // myMechMod.addModel(collisionbody1);
      myMechMod.addModel (collisionbody2);
      collisionbody2.setRenderProps (p);
      myMechMod.setIntegrator (Integrator.BackwardEuler);

      myMechMod.setCollisionBehavior (collisionbody0, collisionbody2, true);
      myMechMod.transformGeometry (new RigidTransform3d (
         0, 0, 0, 0, 0, 1, -Math.PI / 2));

      RenderProps.setPointColor (getOutputProbes(), Color.GREEN);
      RenderProps.setLineColor (getOutputProbes(), Color.GREEN);

      addModel (myMechMod);
   }

   @Override
   public void attach (DriverInterface driver) {
   }

   @Override
   public void detach (DriverInterface driver) {
   }

   /**
    * {@inheritDoc}
    */
   public String getAbout() {
      return "simple demo of a 3d fem";
   }
}
