package artisynth.demos.tutorial;

import java.awt.*;

import maspack.matrix.*;
import maspack.util.PathFinder;
import maspack.render.RenderProps;
import artisynth.core.mechmodels.*;
import artisynth.core.materials.SimpleAxialMuscle;
import artisynth.core.workspace.RootModel;

public class PhalanxWrapping extends RootModel {

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
      RigidBody middle = createBody (mech, "middle", "HP3MiddleLeft.obj");
      middle.setPose (new RigidTransform3d (0.02500, 0.00094, -0.03979));

      // make the proximal phalanx non dynamic; add damping to the middle
      proximal.setDynamic (false);
      middle.setFrameDamping (0.03);

      // create a cylindrical joint between the bones      
      RigidTransform3d TJW = 
         new RigidTransform3d (0.018, 0, -0.022, 0, 0, -DTOR*90);
      RevoluteJoint joint = new RevoluteJoint (proximal, middle, TJW);
      RenderProps.setCylindricalLines (joint, 0.001, Color.BLUE);
      joint.setAxisLength (0.02);
      mech.addBodyConnector (joint);

      // create markers for muscle origin and insertion points
      FrameMarker origin = mech.addFrameMarkerWorld (
         proximal, new Point3d (0.0098, -0.0001, -0.0037));
      FrameMarker insertion = mech.addFrameMarkerWorld (
         middle, new Point3d (0.0293, 0.0009, -0.0415));

      // create a massless RigidCylinder to use as a wrapping surface and
      // attach it to the middle bone
      RigidCylinder cylinder = new RigidCylinder (
         "wrapSurface", /*rad=*/0.005, /*h=*/0.04, /*density=*/0, /*nsegs=*/32);
      cylinder.setPose (TJW);
      mech.addRigidBody (cylinder);
      mech.attachFrame (cylinder, middle);

      // create a wrappable muscle using a SimpleAxialMuscle material
      MultiPointSpring muscle = new MultiPointMuscle ("muscle");
      muscle.setMaterial (
         new SimpleAxialMuscle (/*k=*/0.5, /*d=*/0, /*maxf=*/0.04));
      muscle.addPoint (origin);
      // add an initial point to the wrappable segment to make sure it wraps
      // around the cylinder the right way
      muscle.setSegmentWrappable (
         50, new Point3d[] { new Point3d (0.025, 0.0, -0.02) });            
      muscle.addPoint (insertion);
      muscle.addWrappable (cylinder);
      muscle.updateWrapSegments(); // ``shrink wrap'' around cylinder
      mech.addMultiPointSpring (muscle);

      // set render properties
      RenderProps.setSphericalPoints (mech, 0.002, Color.BLUE);
      RenderProps.setCylindricalLines (muscle, 0.001, Color.RED);
      RenderProps.setFaceColor (cylinder, new Color (200, 200, 230));
   }   
}
