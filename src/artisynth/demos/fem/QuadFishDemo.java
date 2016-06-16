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
import artisynth.core.util.ArtisynthPath;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.RootModel;
import maspack.properties.PropertyDesc;
import maspack.properties.PropertyList;
import maspack.render.*;
import maspack.render.Renderer;
import maspack.spatialmotion.SpatialInertia;
import artisynth.core.driver.*;
import artisynth.core.mechmodels.*;

import java.awt.*;

public class QuadFishDemo extends RootModel {
   public static boolean debug = false;

   FemModel3d myFemMod;

   MechModel myMechMod;

   static double myDensity = 1000;

   public static PropertyList myProps =
      new PropertyList (QuadFishDemo.class, RootModel.class);

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
      //myMechMod.setProfiling (true);

      RigidBody collisionbody0 = new RigidBody();
      collisionbody0.setDynamic (false);

      try {
         collisionbody0.setMesh (new PolygonalMesh (new File (rbpath
         + "box.obj")), "");
         collisionbody0.getMesh().transform (
            new RigidTransform3d (new Vector3d (0, 0, -1.2), new AxisAngle (
               0, 1, 0, 0)));
      }
      catch (Exception e) {
      }

      AffineTransform3d A = new AffineTransform3d();
      A.setIdentity();
      A.applyScaling (0.5, 0.5, 0.5);
      collisionbody0.transformGeometry (A);

      // changes quads to tets
      boolean tet = false;

      // FemModel3d collisionbody1 = FemModel3d.createGrid("fem", 0.6, 0.6, 0.3,
      // 8, 8, 4, myDensity);
      // FemModel3d collisionbody2 = TetGenReader.read("fem1", myDensity,
      // fempath + "muscle.1.node", fempath + "muscle.1.ele", 0.2);
      FemModel3d collisionbody2 =
         new PointToPointMuscle ("fem1", myDensity, 0.2, "wayne_quad", false);
      collisionbody2.transformGeometry (new RigidTransform3d (new Vector3d (
         0, 0, 0.5), new AxisAngle (0, 1, 0, Math.PI / 2)));

      /* -------------------- convert to quad model ------------------------ */

      FemModel3d collisionbodyQuad;

      if (!tet) {
         collisionbodyQuad = new FemModel3d ("model");
         FemFactory.createQuadraticModel (collisionbodyQuad, collisionbody2);
         collisionbody2 = collisionbodyQuad;
      }

      /* -------------------------------------------------------------------- */
      collisionbody2.setSurfaceRendering (SurfaceRender.Shaded);
      System.out.println ("elements/nodes: " + collisionbody2.numElements()
      + " " + collisionbody2.numNodes());

      myMechMod.addRigidBody (collisionbody0);
      RenderProps.setFaceColor (collisionbody2, Color.RED);
      RenderProps.setLineColor (collisionbody2, Color.WHITE);
      RenderProps.setLineWidth (collisionbody2, 1);
      RenderProps.setShading (collisionbody2, Renderer.Shading.SMOOTH);
      if (tet) {
         RenderProps.setDrawEdges (collisionbody2, true);
         RenderProps.setVisible (collisionbody2.getElements(), false);
      }

      // myMechMod.addModel(collisionbody1);
      myMechMod.addModel (collisionbody2);
      myMechMod.setIntegrator (Integrator.BackwardEuler);

      myMechMod.setCollisionBehavior (collisionbody0, collisionbody2, true);
      myMechMod.getCollisionManager().setReduceConstraints (true);
      //myMechMod.setProfiling (true);

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
