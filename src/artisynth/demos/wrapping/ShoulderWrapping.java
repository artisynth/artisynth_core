package artisynth.demos.wrapping;

import java.awt.Color;
import java.io.*;
import java.util.*;

import artisynth.core.workspace.*;
import artisynth.core.mechmodels.*;
import artisynth.core.femmodels.*;
import artisynth.core.materials.*;
import artisynth.core.probes.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.geometry.*;
import maspack.geometry.io.VtkXmlReader;
import maspack.collision.*;
import maspack.render.*;
import maspack.render.Renderer.*;
import maspack.properties.*;

public class ShoulderWrapping extends RootModel {
   MechModel mech;
   Point3d glenohumeral_left = new Point3d(-0.017555, -0.007, -0.17);
//   Point3d superspinatus_insertion_anterior = new Point3d(-0.004107749730304505, 0.014835913809547088, -0.1769246865868313);
   Point3d superspinatus_insertion_anterior = new Point3d(-0.01208957, 0.01589837, -0.18227027);
   Point3d superspinatus_insertion_posterior = new Point3d(-0.02014608133362586, 0.012746715647106222, -0.18652378123514726);
   
   Point3d superspinatus_origin_anterior = new Point3d(-0.024012412, 0.019970868, -0.13371966);
   Point3d superspinatus_origin_posterior = new Point3d(-0.043480541, 0.019970868, -0.15330713);

   
   int superspinatus_num_fibers = 10;
   
   
   public void build (String[] args) throws IOException {
      // create a mech model with appropriate rigid body damping
      mech = new MechModel ("mech");
      mech.setFrameDamping (0.1);
      mech.setRotaryDamping (0.01);
      mech.setGravity (0, -9.8, 0); // model is y-up
      addModel (mech);

      // create and add the humerus bone
      double density = 1000;

      System.out.println ("creating humerus ...");
      String meshPath = PathFinder.getSourceRelativePath (
         this, "geometry/HumerusLeft.obj");
      PolygonalMesh humerusMesh = new PolygonalMesh (meshPath);
      
      RigidBody humerus = RigidBody.createFromMesh (
         "humerus", humerusMesh, density, /*scale=*/0.0036);
      
      // rotation mesh to neutral orientation
      RigidTransform3d X = new RigidTransform3d ();
      X.mulRotX (-Math.PI/2.0);
      X.mulRotZ (-Math.PI/2.0);
      humerusMesh.transform (X);
      
//      humerus.centerPoseOnCenterOfMass();
      mech.addRigidBody (humerus);
      
      addStaticBones();

      // create a smaller mesh for just the proximal end of the humerus, and
      // set this to be the collidable mesh. We do this because we need a
      // high-densoty distance grid to effect wrapping at the proximal end,
      // that the is easier with a localized mesh
      PolygonalMesh clipPlane = MeshFactory.createPlane (0.1, 0.1);
      clipPlane.transform (new RigidTransform3d (0, 0, -0.025));
      SurfaceMeshIntersector intersector = new SurfaceMeshIntersector();
      System.out.println ("creating proximal mesh ...");
      PolygonalMesh proximalMesh =
         intersector.findDifference01 (humerus.getSurfaceMesh(), clipPlane);
      humerus.addMesh (proximalMesh);
      proximalMesh.inverseTransform (humerus.getPose());
      humerus.getSurfaceMeshComp().setIsCollidable (false);
      
      RigidTransform3d humerusPose = new RigidTransform3d (glenohumeral_left, AxisAngle.IDENTITY);
      humerus.setPose (humerusPose);

      // add a spherical joint, centered at the world origin (which is happens
      // to be roughly at the center of the proximal end of the humerus)
      GimbalJoint joint = new GimbalJoint (humerus, null, glenohumeral_left);
      mech.addBodyConnector (joint);

      // add a frame spring to stabilize the joint
      FrameSpring fspring = new FrameSpring ("fspring", 0, 0.1, 0, 0); 
      mech.attachFrameSpring (humerus, null, fspring);
//      fspring.setAttachFrameA (X);

      // set the distance grid resolution
      System.out.println ("setting grid resolution ...");
      DistanceGridComp gcomp = humerus.getDistanceGridComp();
      gcomp.setResolution (new Vector3i (30, 30, 30));

      // add a muscle wrapping strand 
//      System.out.println ("adding muscle ...");
//      FrameMarker mkr = mech.addFrameMarkerWorld (
//         humerus, new Point3d (-0.0037, 0.012, -0.04));
//      mkr.setName ("insertion");
//      Particle org = new Particle ("origin", 1.0, 0.0078, -0.0368, 0.025);
//      org.setDynamic (false);
//      mech.addParticle (org);
//      MultiPointMuscle muscle = new MultiPointMuscle ("muscle");
//      muscle.setMaterial (new SimpleAxialMuscle (0, 0, 10.0));
//      muscle.addWrappable (humerus);
//      muscle.addPoint (org);
//      muscle.setSegmentWrappable (100, new Point3d[] {
//            new Point3d (-0.0038, 0.024, 0.028)
//         });
//      muscle.addPoint (mkr);
//      System.out.println ("updating wrap segments ...");
//      muscle.updateWrapSegments();
//      mech.addMultiPointSpring (muscle);

      // rotate the whole model able the z axis
//      mech.transformGeometry (new RigidTransform3d (0, 0, 0, Math.PI, 0, 0));

      // add probe to control excitation
//      NumericInputProbe inprobe =
//         new NumericInputProbe (
//            muscle, "excitation", 0.0, 6.0);
//      inprobe.addData (
//         new double[] { 0, 0, 1.5, 1.0, 3.0, 0, 4.5, 1.0, 6.0, 0},
//         NumericInputProbe.EXPLICIT_TIME);
//      addInputProbe (inprobe);

      // add probe to control the origin point
//      inprobe = new NumericInputProbe (
//            org, "targetPosition", 0.0, 6.0);
//      // note: origin data flipped 180 about from original pos
//      inprobe.addData (
//         new double[] { 0, -0.0078, 0.0368, 0.025,
//                        3, -0.0078, 0.0368, 0.010,
//                        6, -0.0078, 0.0368, 0.015},
//         NumericInputProbe.EXPLICIT_TIME);
//      addInputProbe (inprobe);

      // add pan controller
      //addController (new PanController (this, 6.0, 0.0, 6.0));
      
      addSuperspinatusTendon();

      // set rendering properties
      RenderProps.setSphericalPoints (mech, 0.003, new Color (102,102,255));
      RenderProps.setCylindricalLines (mech, 0.002, Color.RED);
      RenderProps.setFaceColor (mech, new Color (204, 204, 255));
      System.out.println ("build done");

      setDefaultViewOrientation (AxisAlignedRotation.NX_Y);
   }
   
   public void addStaticBones() throws IOException {
      String[] meshNames = new String[] {"thorax.obj", "scapula_l.obj", "clavicle_l.obj"};
      for (String meshName : meshNames) {
//         VtkXmlReader reader = new VtkXmlReader(new File(PathFinder.getSourceRelativePath (
//            this, "geometry/"+meshName)));
//         reader.parse ();
//         PolygonalMesh mesh = reader.getPolygonalMesh ();
         PolygonalMesh mesh = new PolygonalMesh (PathFinder.getSourceRelativePath (
            this, "geometry/"+meshName));
         RigidBody body = new RigidBody (meshName.substring (0, meshName.length ()-4));
         body.setDynamic (false);
         body.addMesh (mesh);
         mech.addRigidBody (body);
         
      }
   }
   
   public void addSuperspinatusTendon () {
      Point3d pos = new Point3d();
      RigidBody humerus = mech.rigidBodies ().get ("humerus");
      RigidBody scapula = mech.rigidBodies ().get ("scapula_l");      
      PolygonalMesh humerusMesh = humerus.getSurfaceMesh ();
      
      for (int i = 0; i < superspinatus_num_fibers; i++) {
         double alpha = i/(double)superspinatus_num_fibers;
         pos.scale (alpha, superspinatus_origin_anterior);
         pos.scaledAdd (1-alpha, superspinatus_origin_posterior);
         FrameMarker origin = mech.addFrameMarkerWorld (scapula, pos);

         pos.scale (alpha, superspinatus_insertion_anterior);
         pos.scaledAdd (1-alpha, superspinatus_insertion_posterior);
         humerusMesh.distanceToPoint (pos, pos);
         FrameMarker insertion = mech.addFrameMarkerWorld (humerus, pos);
         
         MultiPointMuscle muscle = new MultiPointMuscle ("muscle"+i);
         muscle.setMaterial (new SimpleAxialMuscle (0, 0, 10.0));
         muscle.addWrappable (humerus);
         muscle.addPoint (origin);
         muscle.setSegmentWrappable (100);
         muscle.addPoint (insertion);
         System.out.println ("updating wrap segments ...");
         muscle.updateWrapSegments();
         mech.addMultiPointSpring (muscle);
      }
      

   }

}
