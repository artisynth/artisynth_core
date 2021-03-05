package artisynth.demos.tutorial;

import java.awt.Color;

import artisynth.core.femmodels.SkinMarker;
import artisynth.core.femmodels.SkinMeshBody;
import artisynth.core.femmodels.SkinMeshBody.FrameBlending;
import artisynth.core.gui.ControlPanel;
import artisynth.core.materials.SimpleAxialMuscle;
import artisynth.core.mechmodels.FixedMeshBody;
import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.HingeJoint;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.MultiPointMuscle;
import artisynth.core.mechmodels.MultiPointSpring;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.workspace.RootModel;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.render.RenderProps;
import maspack.util.PathFinder;

public class PhalanxSkinWrapping extends RootModel {

   private static Color BONE = new Color (1f, 1f, 0.8f);
   private static double DTOR = Math.PI/180.0;

   private RigidBody createBody (MechModel mech, String name, String fileName) {
      // creates a bone from its mesh and adds it to a MechModel
      String filePath = PathFinder.findSourceDir(this) + "/data/" + fileName;
      RigidBody body = RigidBody.createFromMesh (
         name, filePath, /*density=*/1000, /*scale=*/1.0);
      mech.addRigidBody (body);
      RenderProps.setFaceColor (body, BONE);
      return body;
   }

   public void build (String[] args) {
      
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      // create the two phalanx bones, and offset them
      RigidBody proximal = createBody (mech, "proximal", "HP3ProximalLeft.obj");
      RigidBody distal = createBody (mech, "distal", "HP3MiddleLeft.obj");
      distal.setPose (new RigidTransform3d (0.02500, 0.00094, -0.03979));

      // make the proximal phalanx non dynamic; add damping to the distal
      proximal.setDynamic (false);
      distal.setFrameDamping (0.03);

      // create a revolute joint between the bones      
      RigidTransform3d TJW = 
         new RigidTransform3d (0.018, 0, -0.022, 0, 0, -DTOR*90);
      HingeJoint joint = new HingeJoint (proximal, distal, TJW);
      RenderProps.setFaceColor (joint, Color.BLUE);
      joint.setShaftLength (0.02);
      mech.addBodyConnector (joint);

      // create markers for muscle origin and insertion points
      FrameMarker origin = mech.addFrameMarkerWorld (
         proximal, new Point3d (0.0098, -0.0001, -0.0037));
      FrameMarker insertion = mech.addFrameMarkerWorld (
         distal, new Point3d (0.0293, 0.0009, -0.0415));

      // create a SkinMeshBody and use it to create "skinned" muscle via points
      SkinMeshBody skinBody = new SkinMeshBody();
      skinBody.addMasterBody (proximal);
      skinBody.addMasterBody (distal);
      skinBody.setFrameBlending (FrameBlending.DUAL_QUATERNION_LINEAR);
      mech.addMeshBody (skinBody);
      SkinMarker via1 = skinBody.addMarker (new Point3d (0.0215, 0, -0.015));
      SkinMarker via2 = skinBody.addMarker (new Point3d (0.025, 0, -0.018));
      SkinMarker via3 = skinBody.addMarker (new Point3d (0.026, 0, -0.0225));

      // create a cylindrical mesh around the joint as a visualization aid to
      // see how well the via points "wrap" as the lower bone moves
      PolygonalMesh mesh = MeshFactory.createCylinder (
         /*rad=*/0.0075, /*h=*/0.04, /*nsegs=*/32);
      FixedMeshBody meshBody = new FixedMeshBody (
         MeshFactory.createCylinder (/*rad=*/0.0075, /*h=*/0.04, /*nsegs=*/32));
      meshBody.setPose (TJW);
      mech.addMeshBody (meshBody);

      // create a wrappable muscle using a SimpleAxialMuscle material
      MultiPointSpring muscle = new MultiPointMuscle ("muscle");
      muscle.setMaterial (
         new SimpleAxialMuscle (/*k=*/0.5, /*d=*/0, /*maxf=*/0.04));
      muscle.addPoint (origin);
      // add via points to the muscle
      muscle.addPoint (via1);
      muscle.addPoint (via2);
      muscle.addPoint (via3);
      muscle.addPoint (insertion);
      mech.addMultiPointSpring (muscle);

      // create control panel to allow frameBlending to be set
      ControlPanel panel = new ControlPanel();
      panel.addWidget (skinBody, "frameBlending");
      addControlPanel (panel);

      // set render properties
      RenderProps.setSphericalPoints (mech, 0.002, Color.BLUE);
      RenderProps.setSphericalPoints (skinBody, 0.002, Color.WHITE);
      RenderProps.setCylindricalLines (muscle, 0.001, Color.RED);
      RenderProps.setFaceColor (meshBody, new Color (200, 200, 230));
   }   
}
