package artisynth.demos.fem;

import java.awt.Point;
import java.util.*;
import java.io.*;

import javax.swing.*;

import java.awt.event.*;

import javax.swing.event.*;

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
import artisynth.core.gui.*;
import artisynth.core.gui.selectionManager.SelectionEvent;
import artisynth.core.gui.selectionManager.SelectionListener;
import artisynth.core.modelbase.*;
import artisynth.core.probes.WayPoint;
import artisynth.core.util.*;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.RootModel;
import maspack.render.*;
import artisynth.core.driver.*;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.MechSystem;
import artisynth.core.mechmodels.HingeJoint;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.MechSystemSolver.Integrator;

import java.awt.*;
import java.util.*;

public class Fem3dBlock extends RootModel {
   double EPS = 1e-9;

   double myDensity = 1000;

   boolean myConnectedP = false;

   public static PropertyList myProps =
      new PropertyList (Fem3dBlock.class, RootModel.class);

   static {
      myProps.add (
         "connected * *", "rigid bodies connected to FEM", false);
   }

   public synchronized boolean getConnected() {
      return myConnectedP;
   }

   public synchronized void setConnected (boolean connect) {
      if (myConnectedP != connect) {
         MechModel mechMod = (MechModel)findComponent ("models/mech");
         if (mechMod != null) {
            FemModel3d femMod = (FemModel3d)mechMod.findComponent ("models/fem");
            LinkedList<FemNode3d> rightNodes = new LinkedList<FemNode3d>();
            rightNodes.addAll (getRightNodes(femMod));
            if (connect && !rightNodes.get (0).isAttached()) {
               RigidBody rightBody =
                  (RigidBody)mechMod.findComponent ("rigidBodies/rightBody");
               // position the block so that it lies at the current
               // end of the beam
               Plane plane = new Plane();
               Point3d centroid = new Point3d();
               int numPnts = rightNodes.size();
               Point3d[] pnts = new Point3d[numPnts];
               for (int i = 0; i < numPnts; i++) {
                  pnts[i] = rightNodes.get (i).getPosition();
                  centroid.add (pnts[i]);
               }
               centroid.scale (1 / (double)numPnts);
               plane.fit (pnts, numPnts);
               Vector3d normal = new Vector3d (plane.getNormal());
               // use a node that is not part of rightNodes
               // to determine the appropriate sign of the normal
               for (FemNode3d node : femMod.getNodes()) {
                  if (!rightNodes.contains (node)) {
                     Vector3d diff = new Vector3d();
                     diff.sub (node.getPosition(),
                               rightNodes.get (0).getPosition());
                     if (diff.dot (normal) > 0) {
                        normal.negate();
                     }
                     break;
                  }
               }
               RigidTransform3d X = new RigidTransform3d();

               X.R.setZDirection (normal);
               X.R.mulAxisAngle (0, 1, 0, -Math.PI / 2);
               X.p.set (centroid);
               X.mulXyz (0.05, 0, 0);
               rightBody.setPose (X);
               rightBody.setVelocity (new Twist());
               for (FemNode3d n : rightNodes) {
                  mechMod.attachPoint (n, rightBody);
               }
            }
            else if (!connect && rightNodes.get (0).isAttached()) {
               for (FemNode3d n : rightNodes) {
                  mechMod.detachPoint (n);
               }
            }
         }
         myConnectedP = connect;
      }
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   private String femPath;

   private String modPath;

   LinkedList<FemNode3d> getLeftNodes (FemModel3d femMod) {
      LinkedList<FemNode3d> nodes = new LinkedList<FemNode3d>();
      for (FemNode3d n : femMod.getNodes()) {
         if (n.getRestPosition().x < -0.3 + EPS) {
            nodes.add (n);
         }
      }
      return nodes;
   }

   LinkedList<FemNode3d> getRightNodes (FemModel3d femMod) {
      LinkedList<FemNode3d> nodes = new LinkedList<FemNode3d>();
      for (FemNode3d n : femMod.getNodes()) {
         if (n.getRestPosition().x > 0.3 - EPS) {
            nodes.add (n);
         }
      }
      return nodes;
   }

   public void build (String[] args) {
      femPath = "models/mech/models/fem/";
      modPath = "models/mech/";
      build ("tet", 3, 1, 0);
   }

   public void build (String type, int nz, int nxy, int options) {

      FemModel3d femMod = new FemModel3d ("fem");
      femMod.setDensity (myDensity);
      if (type.equals ("tet")) {
         FemFactory.createTetGrid (
            femMod, 0.6, 0.2, 0.2, nz, nxy, nxy);
      }
      else if (type.equals ("hex")) {
         FemFactory.createHexGrid (
            femMod, 0.6, 0.2, 0.2, nz, nxy, nxy);
      }
      else if (type.equals ("quadtet")) {
         FemFactory.createQuadtetGrid (
            femMod, 0.6, 0.2, 0.2, nz, nxy, nxy);
      }
      else if (type.equals ("quadhex")) {
         FemFactory.createQuadhexGrid (
            femMod, 0.6, 0.2, 0.2, nz, nxy, nxy);
      }
      else {
         throw new UnsupportedOperationException (
            "Unsupported element type " + type);
      }
      
      femMod.setBounds (new Point3d (-0.6, 0, 0), new Point3d (0.6, 0, 0));
      femMod.setLinearMaterial (200000, 0.4, true);

      femMod.setStiffnessDamping (0.002);
      Renderable elements = femMod.getElements();
      RenderProps.setLineWidth (elements, 2);
      RenderProps.setLineColor (elements, Color.BLUE);
      Renderable nodes = femMod.getNodes();
      RenderProps.setPointStyle (nodes, Renderer.PointStyle.SPHERE);
      RenderProps.setPointRadius (femMod, 0.01);
      RenderProps.setPointColor (nodes, new Color (153, 0, 204));

      // fix the leftmost nodes

      double wx, wy, wz;
      double mass;
      RigidTransform3d X = new RigidTransform3d();
      PolygonalMesh mesh;

      MechModel mechMod = new MechModel ("mech");
      // mechMod.setPrintState ("%10.5f");
      wx = 0.1;
      wy = 0.3;
      wz = 0.3;
      RigidBody leftBody = new RigidBody ("leftBody");
      mass = wx * wy * wz * myDensity;
      leftBody.setInertia (SpatialInertia.createBoxInertia (
         mass, wx, wy, wz));
      mesh = MeshFactory.createBox (wx, wy, wz);
      // mesh.setRenderMaterial (Material.createSpecial (Material.GRAY));
      leftBody.setMesh (mesh, /* fileName= */null);
      X.R.setIdentity();
      X.p.set (-0.35, 0, 0);
      leftBody.setPose (X);
      leftBody.setDynamic (true);
      mechMod.addRigidBody (leftBody);

      RenderProps.setPointStyle (mechMod, Renderer.PointStyle.SPHERE);

      RigidTransform3d TCW = new RigidTransform3d();
      RigidTransform3d TCA = new RigidTransform3d();
      TCA.p.set (0, 0, wz / 2);
      TCA.R.mulAxisAngle (1, 0, 0, Math.PI / 2);
      TCW.mul (X, TCA);
      HingeJoint joint = new HingeJoint (leftBody, TCW);
      RenderProps.setFaceColor (joint, Color.BLUE);
      joint.setShaftLength (0.5);
      joint.setShaftRadius (0.01);
      mechMod.addBodyConnector (joint);

      // right box
      RigidBody rightBody = new RigidBody ("rightBody");
      rightBody.setInertia (SpatialInertia.createBoxInertia (
         mass, wx, wy, wz));
      mesh = MeshFactory.createBox (wx, wy, wz);
      // mesh.setRenderMaterial (Material.createSpecial (Material.GRAY));
      rightBody.setMesh (mesh, /* fileName= */null);
      X.p.set (0.35, 0, 0);
      rightBody.setPose (X);
      rightBody.setDynamic (true);
      mechMod.addRigidBody (rightBody);

      mechMod.addModel (femMod);

      for (FemNode3d n : getLeftNodes(femMod)) {
         mechMod.attachPoint (n, leftBody);
      }

      // femMod.setProfile (true);

      mechMod.setIntegrator (Integrator.BackwardEuler);

      addModel (mechMod);
      setConnected (true);

      // mechMod.setProfiling (true);

//       int numWays = 10;
//       double res = 0.1;
//       for (int i = 0; i < numWays; i++) {
//          addWayPoint (new WayPoint (TimeBase.secondsToTicks ((i + 1) * res)));
//       }

      // addWayPoint (new WayPoint (TimeBase.secondsToTicks (5)));
      //addBreakPoint(5);

      // setupOutputProbes();
      addControlPanel (mechMod, femMod);

      for (int i=1; i<=4; i++) {
         addWayPoint (0.5*i);
      }
      
   }

   ControlPanel myControlPanel;

   public void addControlPanel (MechModel mechMod, FemModel3d femMod) {

      myControlPanel = new ControlPanel ("options", "");
      FemControlPanel.addFem3dControls (myControlPanel, femMod, mechMod);
      myControlPanel.addWidget (this, "connected");
      addControlPanel (myControlPanel);
   }

   @Override
   public void attach (DriverInterface driver) {
      setConnected (getConnected());
   }

   /**
    * {@inheritDoc}
    */
   public String getAbout() {
      return "simple demo of a 3d fem";
   }

}
