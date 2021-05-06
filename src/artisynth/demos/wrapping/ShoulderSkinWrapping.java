package artisynth.demos.wrapping;

import java.awt.Color;
import java.io.IOException;

import artisynth.core.femmodels.SkinMarker;
import artisynth.core.femmodels.SkinMeshBody;
import artisynth.core.femmodels.SkinMeshBody.FrameBlending;
import artisynth.core.materials.SimpleAxialMuscle;
import artisynth.core.mechmodels.DistanceGridComp;
import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.FrameSpring;
import artisynth.core.mechmodels.GimbalJoint;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.MultiPointMuscle;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.probes.NumericInputProbe;
import artisynth.core.workspace.RootModel;
import maspack.collision.SurfaceMeshIntersector;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.interpolation.Interpolation;
import maspack.matrix.AxisAlignedRotation;
import maspack.matrix.AxisAngle;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector3i;
import maspack.matrix.VectorNd;
import maspack.properties.Property;
import maspack.render.RenderProps;
import maspack.util.PathFinder;

public class ShoulderSkinWrapping extends RootModel {
 MechModel mech;
   
   Point3d glenohumeral_left = new Point3d(-0.017555, -0.007, -0.17);
   
//   Point3d supraspinatus_insertion_anterior = new Point3d(-0.004107749730304505, 0.014835913809547088, -0.1769246865868313);
   Point3d supraspinatus_insertion_anterior = new Point3d(-0.01208957, 0.01589837, -0.18227027);
   Point3d supraspinatus_insertion_posterior = new Point3d(-0.02014608133362586, 0.012746715647106222, -0.18652378123514726);
   
   Point3d supraspinatus_origin_anterior = new Point3d(-0.025174507, 0.025545887, -0.15162294);
   Point3d supraspinatus_origin_posterior = new Point3d(-0.041480541, 0.019970868, -0.15330713);

   Point3d wrapping_via_point_anterior = new Point3d(-0.02333586, 0.021560835, -0.16767841);
   Point3d wrapping_via_point_posterior = new Point3d(-0.031311948, 0.019261175, -0.17091899);

   int supraspinatus_num_fibers = 4;
   int skinwrapping_num_viapoints = 7;          //needs to be an odd number for now
   
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
      joint.setName ("shoulder");
      mech.addBodyConnector (joint);

      // add a frame spring to stabilize the joint
      FrameSpring fspring = new FrameSpring ("fspring", 0, 0.1, 0, 0); 
      mech.attachFrameSpring (humerus, null, fspring);
//      fspring.setAttachFrameA (X);

      // set the distance grid resolution
      System.out.println ("setting grid resolution ...");
      DistanceGridComp gcomp = humerus.getDistanceGridComp();
      gcomp.setResolution (new Vector3i (30, 30, 30));
      
      addsupraspinatusTendon();

      // set rendering properties
      RenderProps.setSphericalPoints (mech, 0.003, new Color (102,102,255));
      RenderProps.setCylindricalLines (mech, 0.002, Color.RED);
      RenderProps.setFaceColor (mech, new Color (204, 204, 255));
      System.out.println ("build done");

      setDefaultViewOrientation (AxisAlignedRotation.NX_Y);
      
      addShoulderController();
      RenderProps.setVisible (mech.rigidBodies ().get("thorax"), true);
      
      addBreakPoint (3);
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
   
   public void addsupraspinatusTendon () {
      Point3d pos = new Point3d();
      Point3d pos_skin = new Point3d();
      RigidBody humerus = mech.rigidBodies ().get ("humerus");
      RigidBody scapula = mech.rigidBodies ().get ("scapula_l");      
      PolygonalMesh humerusMesh = humerus.getSurfaceMesh ();
      Vector3d vec = new Vector3d ();
      
   // create a SkinMeshBody and use it to create "skinned" muscle via points
      SkinMeshBody skinBody = new SkinMeshBody();
      skinBody.addMasterBody (mech.rigidBodies ().get ("scapula_l"));
      skinBody.addMasterBody (mech.rigidBodies ().get ("humerus"));
      skinBody.setFrameBlending (FrameBlending.DUAL_QUATERNION_LINEAR);
      mech.addMeshBody (skinBody);
      

      
      for (int i = 0; i < supraspinatus_num_fibers; i++) {
         double alpha = i/(double)supraspinatus_num_fibers;
         pos.scale (alpha, supraspinatus_origin_anterior);
         pos.scaledAdd (1-alpha, supraspinatus_origin_posterior);
         FrameMarker origin = mech.addFrameMarkerWorld (scapula, pos);

         pos.scale (alpha, supraspinatus_insertion_anterior);
         pos.scaledAdd (1-alpha, supraspinatus_insertion_posterior);
         humerusMesh.distanceToPoint (pos, pos);
         FrameMarker insertion = mech.addFrameMarkerWorld (humerus, pos);
         
         pos.scale (alpha, wrapping_via_point_anterior);
         pos.scaledAdd (1-alpha, wrapping_via_point_posterior);

         MultiPointMuscle muscle = new MultiPointMuscle ("muscle"+i);
         muscle.setMaterial (new SimpleAxialMuscle (0, 0, 10.0));
         muscle.addWrappable (humerus);
         muscle.addPoint (origin);
         //muscle.addPoint (skinBody.addMarker (new Point3d (pos_skin)));
         for (int j = 1; j <= (skinwrapping_num_viapoints-1)/2+1; j++) {
            double b =
               (double)j * 1 / ((skinwrapping_num_viapoints - 1) / 2 + 1);
            vec.sub (pos, origin.getPosition ());
            vec.scale (b);
            pos_skin.add (origin.getPosition (), vec);
            muscle.addPoint (skinBody.addMarker (new Point3d (pos_skin)));
         }
            
         for (int j = 0; j < (skinwrapping_num_viapoints-1); j++) {
            double b =
               (double)j * 1 / ((skinwrapping_num_viapoints - 1) / 2 + 1);
            vec.sub (insertion.getPosition (), pos);
            vec.scale (b);
            pos_skin.add (pos, vec);
            muscle.addPoint (skinBody.addMarker (new Point3d (pos_skin)));
         }


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
      probe.addData (0,new VectorNd(0,0,0));
      probe.addData (2.5,new VectorNd(30,14,-90));
      probe.setInterpolationOrder (Interpolation.Order.CubicStep);
      probe.setName ("Shoulder Controller");
      addInputProbe (probe);
   }

}
