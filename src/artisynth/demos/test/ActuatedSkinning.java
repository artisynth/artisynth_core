package artisynth.demos.test;

import java.awt.Color;

import artisynth.demos.tutorial.AllBodySkinning;
import artisynth.core.femmodels.FemFactory;
import artisynth.core.femmodels.FemModel;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.SkinMeshBody.*;
import artisynth.core.femmodels.SkinMeshBody;
import artisynth.core.femmodels.SkinWeightingFunction;
import artisynth.core.femmodels.SkinMarker;
import artisynth.core.femmodels.PointSkinAttachment;
import artisynth.core.materials.LinearMaterial;
import artisynth.core.mechmodels.*;
import artisynth.core.probes.*;
import artisynth.core.mechmodels.Point;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.modelbase.*;
import artisynth.core.workspace.RootModel;
import artisynth.core.gui.*;
import maspack.geometry.*;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.*;
import maspack.render.RenderProps;
import maspack.render.Renderer.FaceStyle;

public class ActuatedSkinning extends AllBodySkinning {

   boolean testAttachmentDerivatives = false;
   MechModel myMech;
   SkinMeshBody mySkin;

   /**
    * Bends a model circularly about the y axis.
    */
   class BendTransformer extends DeformationTransformer {
      double myRadius;

      public BendTransformer (double radius) {
         myRadius = radius;
      }

      public void getDeformation (Vector3d p, Matrix3d F, Vector3d r) {
         double R = myRadius;
         double rad = R - r.z;
         double ang = r.x/R;
         double sin = Math.sin(ang);
         double cos = Math.cos(ang);
         if (p != null) {
            p.set (rad*sin, r.y, R-rad*cos);
         }
         if (F != null) {
            F.set (rad/R*cos, 0, -sin,  0, 1, 0,  rad/R*sin, 0, cos);
         }
      }
   }

   public void build (String[] args) {

      super.build (args);

      myMech = (MechModel)models().get ("mech");
      mySkin = (SkinMeshBody)myMech.meshBodies().get ("skin");

      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-dq")) {
            mySkin.setFrameBlending (FrameBlending.DUAL_QUATERNION_LINEAR);
         }
         else {
            System.out.println ("WARNING: unrecognized option "+args[i]);
         }
      }
      
      TransformGeometryContext.transform (myMech, new BendTransformer(1.0), 0);

      Point mkr0, mkr1, par0, par1;
      PointSkinAttachment pa;

      // add markers
      mkr0 = mySkin.addMarker ("mkr0", new Point3d(-0.5369, -0.1, 0.7064));
      mkr1 = mySkin.addMeshMarker ("mkr1", new Point3d( 0.5369, -0.1, 0.7064));

      // add particles and attachments
      double pointMass = 0.1;
      par0 = new Particle ("par0", pointMass, -0.5369, 0.1, 0.7064);
      myMech.addPoint (par0);
      pa = mySkin.createPointAttachment (par0);
      pa.setName ("pa0");
      myMech.addAttachment (pa);
      
      par1 = new Particle ("par1", pointMass, 0.5369, 0.1, 0.7064);
      myMech.addPoint (par1);
      pa = mySkin.createPointMeshAttachment (par1);
      pa.setName ("pa1");
      myMech.addAttachment (pa);

      // remove gravity
      myMech.setGravity (0, 0, 0);

      Muscle muscle0 = new Muscle ("muscle0");
      muscle0.setConstantMuscleMaterial (1.0);
      myMech.attachAxialSpring (mkr0, mkr1, muscle0);
      Muscle muscle1 = new Muscle ("muscle1");
      muscle1.setConstantMuscleMaterial (0.5); // half power
      myMech.attachAxialSpring (par0, par1, muscle1);

      RenderProps.setSphericalPoints (myMech.points(), 0.04, Color.BLUE);
      RenderProps.setSphericalPoints (mySkin, 0.04, Color.WHITE);
      RenderProps.setSpindleLines (myMech, 0.04, Color.RED);

      ControlPanel panel = new ControlPanel();
      panel.addWidget ("excitation 0", muscle0, "excitation");
      panel.addWidget ("excitation 1", muscle1, "excitation");
      addControlPanel (panel);

      NumericInputProbe inprobe0 =
         new NumericInputProbe (muscle0, "excitation", 0, 1.0);
      inprobe0.addData (
         new double[] { 0, 0, 0.5, 0.5, 1, 0}, NumericInputProbe.EXPLICIT_TIME);
      addInputProbe (inprobe0);
      NumericInputProbe inprobe1 =
         new NumericInputProbe (muscle1, "excitation", 0, 1.0);
      inprobe1.addData (
         new double[] { 0, 0, 0.5, 0.5, 1, 0}, NumericInputProbe.EXPLICIT_TIME);
      addInputProbe (inprobe1);
   }

   public StepAdjustment advance (double t0, double t1, int flags) {
      if (testAttachmentDerivatives) {
         // test derivatives for marker attachments:
         SkinMarker mkr0 = mySkin.markers().get ("mkr0");
         SkinMarker mkr1 = mySkin.markers().get ("mkr1");
         PointSkinAttachment pa = (PointSkinAttachment)mkr0.getAttachment();
         if (pa.checkMasterBlocks (pa.getMasterBlocks(), 5e-6)) {
            System.out.println ("Derivatives for marker0 OK");
         }     
         pa = (PointSkinAttachment)mkr1.getAttachment();
         if (pa.checkMasterBlocks (pa.getMasterBlocks(), 5e-6)) {
            System.out.println ("Derivatives for marker1 OK");
         }     
         pa = (PointSkinAttachment)myMech.attachments().get("pa0");
         if (pa.checkMasterBlocks (pa.getMasterBlocks(), 5e-6)) {
            System.out.println ("Derivatives for attachment0 OK");
         }     
         pa = (PointSkinAttachment)myMech.attachments().get("pa1");
         if (pa.checkMasterBlocks (pa.getMasterBlocks(), 5e-6)) {
            System.out.println ("Derivatives for attachment1 OK");
         }     
      }
      return super.advance (t0, t1, flags);
   }
}
