package artisynth.demos.fem;

import java.awt.Color;
import java.util.LinkedList;

import artisynth.core.femmodels.FemFactory;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.gui.ControlPanel;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.MechSystemSolver.Integrator;
import artisynth.core.mechmodels.HingeJoint;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.workspace.RootModel;
import maspack.geometry.MeshFactory;
import maspack.matrix.RigidTransform3d;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.spatialmotion.SpatialInertia;

public class ArticulatedFem extends RootModel {
   public static boolean debug = false;

   protected MechModel myMechMod;

   // MechFemConnector myConnector;
   LinkedList<FemNode3d> myLeftNodes = new LinkedList<FemNode3d>();

   LinkedList<FemNode3d> myRightNodes = new LinkedList<FemNode3d>();

   static double myDensity = 1000;

   static boolean addLastJoint = false;

   double boxLength = 0.1;

   double boxHeight = 0.3;

   double boxMass = boxLength * boxHeight * boxHeight * myDensity;

   private RigidBody makeBox() {
      RigidBody box = new RigidBody();
      box.setInertia (SpatialInertia.createBoxInertia (
         boxMass, boxLength, boxHeight, boxHeight));
      box.setMesh (MeshFactory.createBox (
         boxLength, boxHeight, boxHeight), null);
      return box;
   }

   public void build (String[] args) {

      int nlinks = 3;
      int nelemsx = 6;
      int nelemsz = 2;
      double femLength = 0.6;
      double femHeight = 0.2;
      double boxLength = 0.1;
      double boxHeight = 0.3;

      double linkLength = femLength + 2 * boxLength;

      myMechMod = new MechModel ("mech");

      RigidTransform3d X = new RigidTransform3d();
      RigidBody lastBox = null;
      double linkCenter;

      RigidBody leftAnchorBox = makeBox();
      linkCenter = linkLength * (-nlinks / 2.0 + 0.5);
      X.p.set (linkCenter - (boxLength + femLength) / 2, 0, boxHeight);
      leftAnchorBox.setPose (X);
      leftAnchorBox.setDynamic (false);
      // myMechMod.addRigidBody (leftAnchorBox);

      RigidBody rightAnchorBox = makeBox();
      linkCenter = linkLength * (-nlinks / 2.0 + (nlinks - 1) + 0.5);
      X.p.set (linkCenter + (boxLength + femLength) / 2, 0, boxHeight);
      rightAnchorBox.setPose (X);
      rightAnchorBox.setDynamic (false);
      // myMechMod.addRigidBody (rightAnchorBox);

      for (int i = 0; i < nlinks; i++) {
         linkCenter = linkLength * (-nlinks / 2.0 + i + 0.5);

         LinkedList<FemNode3d> leftNodes = new LinkedList<FemNode3d>();
         LinkedList<FemNode3d> rightNodes = new LinkedList<FemNode3d>();

         FemModel3d femMod = FemFactory.createTetGrid(
            null, femLength, femHeight, femHeight, nelemsx, nelemsz,
            nelemsz);
         femMod.setDensity(myDensity);
         
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
            femMod.getNodes(), Renderer.PointStyle.SPHERE);
         RenderProps.setPointRadius (femMod.getNodes(), 0.005);

         femMod.setSurfaceRendering (SurfaceRender.Shaded);
         RenderProps.setFaceColor (femMod, new Color (0f, 0.7f, 0.7f));

         RenderProps.setLineColor (femMod.getElements(), new Color (
            0f, 0.2f, 0.2f));
         RenderProps.setLineWidth (femMod.getElements(), 3);

         X.p.set (linkCenter, 0, 0);
         femMod.transformGeometry (X);
         myMechMod.addModel (femMod);

         RigidBody leftBox = makeBox();
         X.p.set (linkCenter - (boxLength + femLength) / 2, 0, 0);
         leftBox.setPose (X);
         myMechMod.addRigidBody (leftBox);

         RigidBody rightBox = makeBox();
         X.p.set (linkCenter + (boxLength + femLength) / 2, 0, 0);
         rightBox.setPose (X);
         myMechMod.addRigidBody (rightBox);

         for (FemNode3d n : leftNodes) {
            myMechMod.attachPoint (n, leftBox);
         }
         for (FemNode3d n : rightNodes) {
            myMechMod.attachPoint (n, rightBox);
         }

         RigidTransform3d TCA = new RigidTransform3d();
         RigidTransform3d TCB = new RigidTransform3d();
         RigidTransform3d TCW = new RigidTransform3d();
         HingeJoint joint;

         TCA.p.set (-boxLength / 2, 0, boxHeight / 2);
         TCA.R.mulAxisAngle (1, 0, 0, Math.PI / 2);
         TCW.mul (leftBox.getPose(), TCA);
         if (lastBox == null) {
            TCB.mul (leftBox.getPose(), TCA);
            // TCB.mulInverseLeft (leftAnchorBox.getPose(), TCB);
            //joint = new HingeJoint (leftBox, TCA, null, TCB);
            joint = new HingeJoint (leftBox, TCW);
         }
         else {
            TCB.p.set (boxLength / 2, 0, boxHeight / 2);
            TCB.R.mulAxisAngle (1, 0, 0, Math.PI / 2);
            //joint = new HingeJoint (leftBox, TCA, lastBox, TCB);
            joint = new HingeJoint (leftBox, lastBox, TCW);
         }
         RenderProps.setFaceColor (joint, new Color (0.15f, 0.15f, 1f));
         joint.setShaftLength (0.5);
         joint.setShaftRadius (0.01);
         myMechMod.addBodyConnector (joint);

         if (addLastJoint && i == nlinks - 1) {
            TCA.p.set (boxLength / 2, 0, boxHeight / 2);
            TCW.mul (rightBox.getPose(), TCA);
            TCB.mul (rightBox.getPose(), TCA);
            // TCB.mulInverseLeft (rightAnchorBox.getPose(), TCB);
            //joint = new HingeJoint (rightBox, TCA, TCB);
            joint = new HingeJoint (rightBox, TCW);
            RenderProps.setFaceColor (joint, new Color (0.15f, 0.15f, 1f));
            joint.setShaftLength (0.5);
            joint.setShaftRadius (0.01);
            myMechMod.addBodyConnector (joint);
         }

         lastBox = rightBox;
      }
      if (!addLastJoint) {
         lastBox.setDynamic (false);
      }
      myMechMod.setIntegrator (Integrator.BackwardEuler);
      addModel (myMechMod);
      addControlPanel (myMechMod);
   }

   protected void addControlPanel (MechModel mech) {
      myControlPanel = new ControlPanel ("options", "");
      myControlPanel.addWidget (mech, "integrator");
      myControlPanel.addWidget (mech, "matrixSolver");
      myControlPanel.addWidget (mech, "maxStepSize");

      addControlPanel (myControlPanel);
   }

   protected ControlPanel myControlPanel;

   /**
    * {@inheritDoc}
    */
   public String getAbout() {
      return "simple demo of a 3d fem";
   }
}
