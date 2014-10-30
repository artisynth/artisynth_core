package artisynth.demos.fem;

import java.awt.Color;
import java.awt.Point;
import java.util.*;
import java.io.*;

import javax.swing.*;

import java.awt.event.*;

import javax.swing.event.*;

import org.python.modules.math;

import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.RevoluteJoint;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.MechSystemSolver.Integrator;
import artisynth.core.mechmodels.MechSystemSolver.MatrixSolver;
import maspack.geometry.*;
import maspack.spatialmotion.*;
import maspack.matrix.*;
import maspack.properties.PropertyList;
import maspack.render.*;
import maspack.util.*;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.gui.*;
import artisynth.core.gui.selectionManager.SelectionEvent;
import artisynth.core.gui.selectionManager.SelectionListener;
import artisynth.core.modelbase.*;
import artisynth.core.util.*;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.RootModel;
import maspack.render.*;
import maspack.render.RenderProps.LineStyle;
import artisynth.core.driver.*;

import java.awt.*;
import java.util.*;

public class ArticulatedFem extends RootModel {
   public static boolean debug = false;

   // MechFemConnector myConnector;
   LinkedList<FemNode3d> myLeftNodes = new LinkedList<FemNode3d>();

   LinkedList<FemNode3d> myRightNodes = new LinkedList<FemNode3d>();

   static double myDensity = 1000;

   static boolean addLastJoint = false;

   double boxLength = 0.1;

   double boxHeight = 0.3;

   double boxMass = boxLength * boxHeight * boxHeight * myDensity;

   public ArticulatedFem() {
      super (null);
   }

   private RigidBody makeBox() {
      RigidBody box = new RigidBody();
      box.setInertia (SpatialInertia.createBoxInertia (
         boxMass, boxLength, boxHeight, boxHeight));
      box.setMesh (MeshFactory.createBox (
         boxLength, boxHeight, boxHeight), null);
      return box;
   }

   public ArticulatedFem (String name) {
      this();
      setName (name);

      int nlinks = 3;
      int nelemsx = 6;
      int nelemsz = 2;
      double femLength = 0.6;
      double femHeight = 0.2;
      double boxLength = 0.1;
      double boxHeight = 0.3;

      double linkLength = femLength + 2 * boxLength;

      MechModel model = new MechModel ("mech");

      RigidTransform3d X = new RigidTransform3d();
      RigidBody lastBox = null;
      double linkCenter;

      RigidBody leftAnchorBox = makeBox();
      linkCenter = linkLength * (-nlinks / 2.0 + 0.5);
      X.p.set (linkCenter - (boxLength + femLength) / 2, 0, boxHeight);
      leftAnchorBox.setPose (X);
      leftAnchorBox.setDynamic (false);
      // model.addRigidBody (leftAnchorBox);

      RigidBody rightAnchorBox = makeBox();
      linkCenter = linkLength * (-nlinks / 2.0 + (nlinks - 1) + 0.5);
      X.p.set (linkCenter + (boxLength + femLength) / 2, 0, boxHeight);
      rightAnchorBox.setPose (X);
      rightAnchorBox.setDynamic (false);
      // model.addRigidBody (rightAnchorBox);

      for (int i = 0; i < nlinks; i++) {
         linkCenter = linkLength * (-nlinks / 2.0 + i + 0.5);

         LinkedList<FemNode3d> leftNodes = new LinkedList<FemNode3d>();
         LinkedList<FemNode3d> rightNodes = new LinkedList<FemNode3d>();

         FemModel3d femMod =
            FemModel3d.createGrid (
               /*name=*/null, femLength, femHeight, femHeight, nelemsx, nelemsz,
               nelemsz, myDensity);
         femMod.setLinearMaterial (200000, 0.4, true);
         femMod.setGravity (0, 0, -9.8);

         double eps = 0.000001;
         for (FemNode3d n : femMod.getNodes()) {
            double x = n.getPosition().x;
            if (x <= -femLength / 2 + eps) {
               leftNodes.add (n);
            }
            else if (x >= femLength / 2 - eps) {
               rightNodes.add (n);
            }
         }

         femMod.setStiffnessDamping (0.002);
         RenderProps.setLineWidth (femMod.getElements(), 2);
         RenderProps.setLineColor (femMod.getElements(), Color.BLUE);

         RenderProps.setPointStyle (
            femMod.getNodes(), RenderProps.PointStyle.SPHERE);
         RenderProps.setPointRadius (femMod.getNodes(), 0.005);

         femMod.setSurfaceRendering (SurfaceRender.Shaded);
         RenderProps.setFaceColor (femMod, new Color (0f, 0.7f, 0.7f));

         RenderProps.setLineColor (femMod.getElements(), new Color (
            0f, 0.2f, 0.2f));
         RenderProps.setLineWidth (femMod.getElements(), 3);

         X.p.set (linkCenter, 0, 0);
         femMod.transformGeometry (X);
         model.addModel (femMod);

         RigidBody leftBox = makeBox();
         X.p.set (linkCenter - (boxLength + femLength) / 2, 0, 0);
         leftBox.setPose (X);
         model.addRigidBody (leftBox);

         RigidBody rightBox = makeBox();
         X.p.set (linkCenter + (boxLength + femLength) / 2, 0, 0);
         rightBox.setPose (X);
         model.addRigidBody (rightBox);

         for (FemNode3d n : leftNodes) {
            model.attachPoint (n, leftBox);
         }
         for (FemNode3d n : rightNodes) {
            model.attachPoint (n, rightBox);
         }

         RigidTransform3d TCA = new RigidTransform3d();
         RigidTransform3d TCB = new RigidTransform3d();
         RevoluteJoint joint;

         TCA.p.set (-boxLength / 2, 0, boxHeight / 2);
         TCA.R.mulAxisAngle (1, 0, 0, Math.PI / 2);
         if (lastBox == null) {
            TCB.mul (leftBox.getPose(), TCA);
            // TCB.mulInverseLeft (leftAnchorBox.getPose(), TCB);
            joint = new RevoluteJoint (leftBox, TCA, TCB);
         }
         else {
            TCB.p.set (boxLength / 2, 0, boxHeight / 2);
            TCB.R.mulAxisAngle (1, 0, 0, Math.PI / 2);
            joint = new RevoluteJoint (leftBox, TCA, lastBox, TCB);
         }
         RenderProps.setLineRadius (joint, 0.01);
         RenderProps.setLineColor (joint, new Color (0.15f, 0.15f, 1f));
         joint.setAxisLength (0.5);
         model.addRigidBodyConnector (joint);

         if (addLastJoint && i == nlinks - 1) {
            TCA.p.set (boxLength / 2, 0, boxHeight / 2);
            TCB.mul (rightBox.getPose(), TCA);
            // TCB.mulInverseLeft (rightAnchorBox.getPose(), TCB);
            joint = new RevoluteJoint (rightBox, TCA, TCB);
            RenderProps.setLineRadius (joint, 0.01);
            RenderProps.setLineColor (joint, new Color (0.15f, 0.15f, 1f));
            joint.setAxisLength (0.5);
            model.addRigidBodyConnector (joint);
         }

         lastBox = rightBox;
      }
      if (!addLastJoint) {
         lastBox.setDynamic (false);
      }
      model.setIntegrator (Integrator.BackwardEuler);
      addModel (model);
      addControlPanel (model);
   }

   protected void addControlPanel (MechModel mech) {
      myControlPanel = new ControlPanel ("options", "");
      myControlPanel.addWidget (mech, "integrator");
      myControlPanel.addWidget (mech, "matrixSolver");
      myControlPanel.addWidget (mech, "maxStepSize");

      addControlPanel (myControlPanel);
   }

   ControlPanel myControlPanel;

   /**
    * {@inheritDoc}
    */
   public String getAbout() {
      return "simple demo of a 3d fem";
   }
}
