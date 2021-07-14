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
import maspack.interpolation.Interpolation;
import maspack.collision.*;
import maspack.render.*;
import maspack.render.Renderer.*;
import maspack.properties.*;

public class ShoulderWrapping extends RootModel {
   /**
    * Bone meshes for the humerus, clavicle and scapula were derived from the
    * "skeleton19" data set of the PHuman bone scanning project.
    */
   MechModel mech;
   
   Point3d glenohumeral_left = new Point3d(-0.017555, -0.007, -0.17);
   
//   Point3d supraspinatus_insertion_anterior = new Point3d(-0.004107749730304505, 0.014835913809547088, -0.1769246865868313);
////   Point3d supraspinatus_insertion_anterior = new Point3d(-0.01208957, 0.01589837, -0.18227027);
//   Point3d supraspinatus_insertion_posterior = new Point3d(-0.02014608133362586, 0.012746715647106222, -0.18652378123514726);
//   
//   Point3d supraspinatus_origin_anterior = new Point3d(-0.025174507, 0.025545887, -0.15162294);
//   Point3d supraspinatus_origin_posterior = new Point3d(-0.041480541, 0.019970868, -0.15330713);

   // ELEVATED POSITION
   
   Point3d supraspinatus_origin_anterior = new Point3d(-0.052564318, 0.035091686, -0.071851561);
   Point3d supraspinatus_origin_posterior = new Point3d(-0.078850004, 0.02177388, -0.076913862);
   
   Point3d supraspinatus_insertion_anterior = new Point3d(-0.0079808647, 0.015329992, -0.15374636);
   Point3d supraspinatus_insertion_posterior = new Point3d(-0.019527846, 0.018219469, -0.16589307);
   
   
   
   int supraspinatus_num_fibers = 4;

   public void build (String[] args) throws IOException {
      // create a mech model with appropriate rigid body damping
      mech = new MechModel ("mech");
      mech.setFrameDamping (0.1);
      mech.setRotaryDamping (0.01);
      mech.setGravity (0, -9.8, 0); // model is y-up
      addModel (mech);

      RigidBody humerus = addHumerus();
      addStaticBody ("thorax", "thorax.obj");
      addStaticBody ("scapula", "ScapulaLeft.obj");
      addStaticBody ("clavicle", "ClavicleLeft.obj");

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
      joint.setName ("shoulder");
      mech.addBodyConnector (joint);
      joint.setYaw (90);


      // add a frame spring to stabilize the joint
      FrameSpring fspring = new FrameSpring ("fspring", 0, 0.1, 0, 0); 
      mech.attachFrameSpring (humerus, null, fspring);
//      fspring.setAttachFrameA (X);
      RenderProps.setVisible (fspring, false);

      // set the distance grid resolution
      System.out.println ("setting grid resolution ...");
      DistanceGridComp gcomp = humerus.getDistanceGridComp();
      gcomp.setResolution (new Vector3i (30, 30, 30));
      
      addSupraspinatus();
      
      // set rendering properties
      RenderProps.setSphericalPoints (mech, 0.003, new Color (102,102,255));
      RenderProps.setCylindricalLines (mech, 0.002, Color.RED);
      RenderProps.setFaceColor (mech, new Color (204, 204, 255));
      System.out.println ("build done");

      setDefaultViewOrientation (AxisAlignedRotation.NX_Y);
      
      addShoulderController();
      //RenderProps.setVisible (mech.rigidBodies ().get("thorax"), true);
    
      addBreakPoint (3);
   }


   protected PolygonalMesh readMesh (String meshName) {
      String meshPath =
         PathFinder.getSourceRelativePath (this, "geometry/"+meshName);
      PolygonalMesh mesh = null;
      try {
         mesh = new PolygonalMesh (meshPath);
      }
      catch (Exception e) {
         System.out.println ("Can't read mesh file "+meshPath+": "+e);
      }
      return mesh;
   }

   public RigidBody addHumerus () {
      System.out.println ("creating humerus ...");
      PolygonalMesh mesh = readMesh ("HumerusLeft.obj");
      // rotate mesh to neutral orientation
      RigidTransform3d X = new RigidTransform3d ();
      X.mulRotX (-Math.PI/2.0);
      X.mulRotZ (-Math.PI/2.0);
      mesh.transform (X);
      RigidBody humerus = RigidBody.createFromMesh (
         "humerus", mesh, /*density=*/1000.0, /*scale=*/0.0036);
      mech.addRigidBody (humerus);
      return humerus;
   }

   public RigidBody addStaticBody (String name, String meshName) {
      PolygonalMesh mesh = readMesh (meshName);
      RigidBody body = RigidBody.createFromMesh (
         name, mesh, /*density=*/1000, /*scale=*/1.0);
      body.setDynamic (false);
      body.centerPoseOnCenterOfMass();
      mech.addRigidBody (body);
      return body;
   }      
   
   public void addSupraspinatus () {
      Point3d pos = new Point3d();
      RigidBody humerus = mech.rigidBodies ().get ("humerus");
      RigidBody scapula = mech.rigidBodies ().get ("scapula");      
      PolygonalMesh humerusMesh = humerus.getSurfaceMesh ();
      
      for (int i = 0; i < supraspinatus_num_fibers; i++) {
         double alpha = i/(double)supraspinatus_num_fibers;
         pos.scale (alpha, supraspinatus_origin_anterior);
         pos.scaledAdd (1-alpha, supraspinatus_origin_posterior);
         FrameMarker origin = mech.addFrameMarkerWorld (scapula, pos);

         pos.scale (alpha, supraspinatus_insertion_anterior);
         pos.scaledAdd (1-alpha, supraspinatus_insertion_posterior);
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
   
   public void addShoulderController (){
      mech.rigidBodies ().get ("humerus").setDynamic (false);
      NumericInputProbe  probe = new NumericInputProbe ();
      Property[] props ={mech.bodyConnectors ().get ("shoulder").getProperty ("roll"),mech.bodyConnectors ().get ("shoulder").getProperty ("pitch"),mech.bodyConnectors ().get ("shoulder").getProperty ("yaw")};
      probe.setInputProperties (props);
      probe.addData (0,new VectorNd(0,0,90));
      probe.addData (2.5,new VectorNd(0,0,0));
      probe.setInterpolationOrder (Interpolation.Order.CubicStep);
      probe.setName ("Shoulder Controller");
      addInputProbe (probe);
   }

}

