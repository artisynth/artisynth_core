package artisynth.demos.fem;

import java.awt.Point;
import java.util.*;
import java.io.*;

import javax.swing.*;

import java.awt.event.*;

import javax.swing.event.*;

import java.awt.*;
import java.util.*;

import maspack.geometry.*;
import maspack.matrix.*;
import maspack.properties.Property;
import maspack.properties.PropertyList;
import maspack.render.*;
import maspack.render.Renderer;
import maspack.util.*;
import maspack.widgets.DoubleFieldSlider;
import maspack.spatialmotion.*;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemFactory;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.FemNodeNeighbor;
import artisynth.core.femmodels.SkinMeshBody;
import artisynth.core.gui.*;
import artisynth.core.gui.selectionManager.SelectionEvent;
import artisynth.core.gui.selectionManager.SelectionListener;
import artisynth.core.modelbase.*;
import artisynth.core.probes.WayPoint;
import artisynth.core.util.*;
import artisynth.core.workspace.RootModel;
import maspack.render.*;
import artisynth.core.driver.*;
import artisynth.core.mechmodels.*;
import artisynth.core.mechmodels.MechSystemSolver.Integrator;

public class FemSkinDemo extends RootModel {
   double EPS = 1e-9;

   protected MechModel myMech;

   static double myDensity = 1000;

   public static PropertyList myProps =
      new PropertyList (FemSkinDemo.class, RootModel.class);

   static {
      //myProps.add (
      //   "connected * *", "rigid bodies connected to FEM", false, "NW");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   LinkedList<FemNode3d> getLeftNodes (FemModel3d femMod) {
      LinkedList<FemNode3d> nodes = new LinkedList<FemNode3d>();
      double minx = Double.POSITIVE_INFINITY;
      for (FemNode3d n : femMod.getNodes()) {
         if (n.getPosition().x < minx) {
            minx = n.getPosition().x;
         }
      }
      for (FemNode3d n : femMod.getNodes()) {
         if (n.getPosition().x < minx + EPS) {
            nodes.add (n);
         }
      }
      return nodes;
   }

   LinkedList<FemNode3d> getRightNodes (FemModel3d femMod) {
      LinkedList<FemNode3d> nodes = new LinkedList<FemNode3d>();
      double maxx = Double.NEGATIVE_INFINITY;
      for (FemNode3d n : femMod.getNodes()) {
         if (n.getPosition().x > maxx) {
            maxx = n.getPosition().x;
         }
      }
      for (FemNode3d n : femMod.getNodes()) {
         if (n.getPosition().x > maxx - EPS) {
            nodes.add (n);
         }
      }
      return nodes;
   }

   protected FemModel3d createFem (String name, double wx, double wy, double wz) {
      FemModel3d femMod = new FemModel3d (name);
      femMod.setDensity (myDensity);
      FemFactory.createHexGrid (femMod,  wx, wy, wz, 6, 2, 2);
      femMod.setLinearMaterial (200000, 0.4, true);
      femMod.setStiffnessDamping (0.002);

      Renderable elements = femMod.getElements();
      RenderProps.setLineWidth (elements, 2);
      RenderProps.setLineColor (elements, Color.BLUE);
      Renderable nodes = femMod.getNodes();
      RenderProps.setPointStyle (nodes, Renderer.PointStyle.SPHERE);
      RenderProps.setPointRadius (nodes, 0.010);
      RenderProps.setPointColor (nodes, new Color (153, 0, 204));
      RenderProps.setFaceColor (femMod, new Color (128, 128, 255));
      femMod.setSurfaceRendering (FemModel3d.SurfaceRender.Shaded);

      myMech.addModel (femMod);

      return femMod;
    }

   protected RigidBody createBlock (
      String name, double wx, double wy, double wz) {

      RigidBody body = RigidBody.createBox (name, wx, wy, wz, myDensity);
      RenderProps.setFaceColor (body, new Color (153, 102, 255));
      myMech.addRigidBody (body);
      return body;
   }

   public void build (String[] args) {

      myMech = new MechModel ("mech");
      RenderProps.setPointStyle (myMech, Renderer.PointStyle.SPHERE);

      double midw = 0.6;

      FemModel3d femMod1 = createFem ("fem1", midw, 0.2, 0.2);
      FemModel3d femMod2 = createFem ("fem2", midw, 0.2, 0.2);

      // fix the leftmost nodes

      LinkedList<FemNode3d> leftNodes1 = getLeftNodes (femMod1);
      LinkedList<FemNode3d> rightNodes1 = getRightNodes (femMod1);
      LinkedList<FemNode3d> leftNodes2 = getLeftNodes (femMod2);
      LinkedList<FemNode3d> rightNodes2 = getRightNodes (femMod2);

      RigidTransform3d X = new RigidTransform3d();

      double wx = 0.1;
      double wy = 0.3;
      double wz = 0.3;
      double transx = wx/2 + 1.5*midw;
      RigidBody leftBody = createBlock ("leftBlock", wx, wy, wz);
      leftBody.setPose (new RigidTransform3d (-transx, 0, 0));

      RigidTransform3d TCW = new RigidTransform3d();

      TCW.p.set (-transx-wx/2, 0, wz/2);
      TCW.R.mulAxisAngle (1, 0, 0, Math.PI / 2);
      HingeJoint joint = new HingeJoint (leftBody, TCW);
      RenderProps.setFaceColor (joint, Color.BLUE);
      joint.setShaftLength (0.4);
      joint.setShaftRadius (0.01);
      myMech.addBodyConnector (joint);

      RigidBody middleBody = createBlock ("middleBlock", midw, 0.21, 0.21);

      RigidBody rightBody = createBlock ("rightBlock", wx, wy, wz);
      rightBody.setPose (new RigidTransform3d (transx, 0, 0));

      TCW.p.set (transx+wx/2, 0, wz/2);
      TCW.R.setAxisAngle (1, 0, 0, Math.PI / 2);
      joint = new HingeJoint (rightBody, TCW);
      RenderProps.setFaceColor (joint, Color.BLUE);
      joint.setShaftLength (0.4);
      joint.setShaftRadius (0.01);
      myMech.addBodyConnector (joint);

      femMod1.transformGeometry (new RigidTransform3d (-midw, 0, 0));
      femMod2.transformGeometry (new RigidTransform3d (midw, 0, 0));

      for (FemNode3d n : leftNodes1) {
         myMech.attachPoint (n, leftBody);
      }
      for (FemNode3d n : rightNodes1) {
         myMech.attachPoint (n, middleBody);
      }
      for (FemNode3d n : leftNodes2) {
         myMech.attachPoint (n, middleBody);
      }
      for (FemNode3d n : rightNodes2) {
         myMech.attachPoint (n, rightBody);
      }

      PolygonalMesh mesh = 
         MeshFactory.createRoundedCylinder (
            /*r=*/0.3, /*h=*/1.8, /*nsclices=*/12, /*nsegs=*/10,
            /*flatbotton=*/false);
      // flip aout y axis
      mesh.transform (new RigidTransform3d (0, 0, 0, 0, Math.PI/2, 0));
      SkinMeshBody skinBody = new SkinMeshBody (mesh);
      skinBody.addMasterBody (rightBody);
      skinBody.addMasterBody (middleBody);
      skinBody.addMasterBody (leftBody);
      skinBody.addMasterBody (femMod1);
      skinBody.addMasterBody (femMod2);
      skinBody.computeWeights ();
      skinBody.setName("skin");
      
      RenderProps.setDrawEdges (skinBody, true);
      RenderProps.setFaceStyle (skinBody, Renderer.FaceStyle.NONE);
      
      myMech.addMeshBody (skinBody);

      addModel (myMech);
   }

   ControlPanel myControlPanel;

}
