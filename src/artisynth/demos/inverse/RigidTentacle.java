package artisynth.demos.inverse;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JFrame;

import maspack.geometry.PolygonalMesh;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderProps;
import maspack.render.RenderProps.Faces;
import maspack.render.RenderProps.PointStyle;
import maspack.render.TextureProps;
import maspack.render.TextureProps.Mode;
import artisynth.core.gui.ControlPanel;
import artisynth.core.inverse.FrameExciter;
import artisynth.core.inverse.TrackingController;
import artisynth.core.materials.RotAxisFrameMaterial;
import artisynth.core.mechmodels.ForceEffector;
import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.FrameSpring;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.RevoluteJoint;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.femmodels.SkinMesh;
import artisynth.core.mechmodels.SphericalJoint;
import artisynth.core.util.ArtisynthPath;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.RootModel;
import artisynth.demos.mech.FrameSpringDemo;

public class RigidTentacle extends RootModel {
   MechModel myMechMod;

   private static double inf = Double.POSITIVE_INFINITY;

   RigidBody myBaseSegment;
   RigidBody myEndSegment;
   
   public static final boolean endPointTarget = true;
   
   int numSegments = 4;
   double rk = 0.5;
   double d = 0.1;
   double rd = 0.01;
   double maxForce = 100;
   double segmentLen = 1;
   
//   ArrayList<RigidBody> mySegments = new ArrayList<RigidBody> ();

    

   private double getHeight (RigidBody body) {
      Point3d min = new Point3d (inf, inf, inf);
      Point3d max = new Point3d (-inf, -inf, -inf);

      body.updateBounds (min, max);
      return max.z - min.z;
   }

   private Point3d getCenter (RigidBody body) {
      Point3d min = new Point3d (inf, inf, inf);
      Point3d max = new Point3d (-inf, -inf, -inf);

      body.updateBounds (min, max);
      Point3d center = new Point3d();
      center.add (min, max);
      center.scale (0.5);
      return center;
   }

   public void addFrameSpring (
      RigidBody bodyA, RigidBody bodyB, double x, double y, double z,
      double kRot) {
      RigidTransform3d TDW = new RigidTransform3d();
      RigidTransform3d TCA = new RigidTransform3d();
      RigidTransform3d XDB = new RigidTransform3d();
      TDW.p.set (x, y, z);

      TCA.mulInverseLeft (bodyA.getPose(), TDW);
      XDB.mulInverseLeft (bodyB.getPose(), TDW);

      FrameSpring spring = new FrameSpring (null);
      spring.setMaterial (new RotAxisFrameMaterial (0, kRot, 0, 0));
      //spring.setRotaryStiffness (kRot);
      spring.setAttachFrameA (TCA);
      spring.setAttachFrameB (XDB);
      myMechMod.attachFrameSpring (bodyA, bodyB, spring);
   }

   public RigidBody addBody (String bodyName, String meshName)
      throws IOException {
      double density = 1000;

      RigidBody body = new RigidBody (bodyName);
      PolygonalMesh mesh =
         new PolygonalMesh (new File (ArtisynthPath.getSrcRelativePath (
            FrameSpringDemo.class, "geometry/" + meshName + ".obj")));
      mesh.scale (0.1,0.1,0.2);
      mesh.triangulate();
      body.setMesh (mesh, null);
      myMechMod.addRigidBody (body);

      body.setInertiaFromDensity (density);
      return body;
   }
   
   
   public void addMarker(String name, RigidBody body, Point3d loc) {
      if (body==null) {
         return;
      }
        
      FrameMarker endpoint = new FrameMarker(name);
      myMechMod.addFrameMarker (endpoint, body, loc);
        
      RenderProps rp = new RenderProps(myMechMod.getRenderProps());
      rp.setShading(RenderProps.Shading.GOURAUD);
      rp.setPointColor(Color.ORANGE);
      rp.setPointStyle(PointStyle.SPHERE);
      rp.setPointRadius(0.02);
      endpoint.setRenderProps(rp);
   }

   public RevoluteJoint addRevoluteJoint (
      RigidBody bodyA, RigidBody bodyB, double x, double y, double z) {
      RigidTransform3d TCA = new RigidTransform3d();
      RigidTransform3d XDB = new RigidTransform3d();
      RigidTransform3d TDW = new RigidTransform3d();

      TDW.p.set (x, y, z);
      TDW.R.setAxisAngle (Vector3d.Y_UNIT, Math.toRadians (90));
      XDB.mulInverseLeft (bodyB.getPose(), TDW);
      TCA.mulInverseLeft (bodyA.getPose(), TDW);
      RevoluteJoint joint = new RevoluteJoint (bodyA, TCA, bodyB, XDB);
      RenderProps.setLineStyle (joint, RenderProps.LineStyle.CYLINDER);
      RenderProps.setLineColor (joint, Color.BLUE);
      RenderProps.setLineRadius (joint, 0.025);
      joint.setAxisLength (0.05);
      myMechMod.addRigidBodyConnector (joint);
      return joint;
   }

   public SphericalJoint addSphericalJoint (
      RigidBody bodyA, RigidBody bodyB, double x, double y, double z) {
      RigidTransform3d TCA = new RigidTransform3d();
      RigidTransform3d XDB = new RigidTransform3d();
      RigidTransform3d TDW = new RigidTransform3d();

      TDW.p.set (x, y, z);
      XDB.mulInverseLeft (bodyB.getPose(), TDW);
      TCA.mulInverseLeft (bodyA.getPose(), TDW);
      SphericalJoint joint = new SphericalJoint (bodyA, TCA, bodyB, XDB);
      RenderProps.setPointStyle (joint, RenderProps.PointStyle.SPHERE);
      RenderProps.setPointColor (joint, Color.BLUE);
      RenderProps.setPointRadius (joint, 0.025);
      joint.setAxisLength (0.05);
      myMechMod.addRigidBodyConnector (joint);
      return joint;
   }

   public SphericalJoint addSphericalJoint (
      RigidBody bodyA, RigidBody bodyB, RigidTransform3d TDW) {
      RigidTransform3d TCA = new RigidTransform3d();
      RigidTransform3d XDB = new RigidTransform3d();

      XDB.mulInverseLeft (bodyB.getPose(), TDW);
      TCA.mulInverseLeft (bodyA.getPose(), TDW);
      SphericalJoint joint = new SphericalJoint (bodyA, TCA, bodyB, XDB);
      RenderProps.setPointStyle (joint, RenderProps.PointStyle.SPHERE);
      RenderProps.setPointColor (joint, Color.BLUE);
      RenderProps.setPointRadius (joint, 0.025);
      joint.setAxisLength (0.05);
      myMechMod.addRigidBodyConnector (joint);
      return joint;
   }

   private void setBodyPose (
      RigidBody body, double x, double y, double z, double roll, double pitch,
      double yaw) {
      RigidTransform3d X = new RigidTransform3d();
      X.p.set (x, y, z);
      X.R.setRpy (Math.toRadians (roll),
                  Math.toRadians (pitch),
                  Math.toRadians (yaw));
      body.setPose (X);
   }

   
   public void addSkinMesh() throws IOException {
      String dataDir = ArtisynthPath.getSrcRelativePath (Tentacle.class, "data/");
      String meshFilename = dataDir+"/tentacle_t.obj";
      PolygonalMesh tentacle = new PolygonalMesh (new File(meshFilename));
//      tentacle.write (new PrintWriter (new File("tentacle_quads.obj")), "%g");
      
      TextureProps tp = new TextureProps ();
      tp.setFileName (dataDir+"/tongue.jpg");
      tp.setMode (Mode.DECAL);
      tp.setEnabled (true);
      
      RenderProps rp = new RenderProps(myMechMod. getRenderProps ());
      rp.setFaceStyle (Faces.FRONT);
      rp.setFaceColor (new Color(255, 187, 187));
      rp.setDrawEdges (false);
      rp.setLineColor (Color.BLACK);
      rp.setTextureProps (tp);
      tentacle.setRenderProps (rp);
      
//      RigidBody tent = new RigidBody("tentacle");
//      tent.setMesh (tentacle, meshFilename);
//      tent.setDynamic (false);
//      tent.setRenderProps (rp);
//      myMechMod.addRigidBody (tent);
      
      Point3d min = new Point3d (inf, inf, inf);
      Point3d max = new Point3d (-inf, -inf, -inf);
      tentacle.updateBounds (min, max);
      Vector3d tentacleBounds = new Vector3d ();
      tentacleBounds.sub (max, min);
      
      
       min = new Point3d (inf, inf, inf);
       max = new Point3d (-inf, -inf, -inf);

      for (RigidBody body : myMechMod.rigidBodies ()) {
         if (body.isDynamic ()) {
            body.updateBounds (min, max);
         }
      }
      Vector3d skeletonBounds = new Vector3d ();
      skeletonBounds.sub (max, min);
      
//      tentacle.scale (0.01); // scale to meters
      tentacle.scale (0.011); // scale to meters
      
//      tentacle.scale (
//         tentacleBounds.x/skeletonBounds.x, 
//         tentacleBounds.x/skeletonBounds.y, 
//         tentacleBounds.x/skeletonBounds.z);
      RigidTransform3d X = new RigidTransform3d ();
      X.p.x = -skeletonBounds.x/2;
      tentacle.transform (X);
      
      SkinMesh skinMesh =  new SkinMesh (tentacle);
      ArrayList<RigidBody> skinnedBodies = new ArrayList<RigidBody>();
      for (RigidBody body : myMechMod.rigidBodies ()) {
//         if (body.isDynamic ()) {
            skinnedBodies.add (body);
            skinMesh.addFrame (body);
//         }
      }
      //skinMesh.setBodies (skinnedBodies.toArray (new RigidBody[0]));
      skinMesh.computeWeights();
      skinMesh.setRenderProps (rp);
      myMechMod.addMeshBody (skinMesh);
//      RenderProps.setFaceStyle (skinMesh, RenderProps.Faces.NONE);
//      RenderProps.setDrawEdges (skinMesh, true);
//      RenderProps.setLineColor (skinMesh, Color.GRAY);
//      mySkinMesh = skinMesh;
   }
   
   public void build (String[] args) throws IOException {

      myMechMod = new MechModel ("RigidTentacle");
      myMechMod.setProfiling (false);
      myMechMod.setGravity (0, 0, 0);
      // myMechMod.setRigidBodyDamper (new FrameDamper (1.0, 4.0));
      myMechMod.setFrameDamping (d);
      myMechMod.setRotaryDamping (rd);
      //myMechMod.setIntegrator (MechSystemSolver.Integrator.SymplecticEuler);

      myBaseSegment = addBody ("lowerArm", "lowerArm");
      myBaseSegment.setDynamic (false);
      RenderProps.setVisible (myBaseSegment, false);
      double lowerArmZ = getHeight (myBaseSegment) / 2 + getCenter (myBaseSegment).z;
      setBodyPose (myBaseSegment, 0, 0, lowerArmZ, 0, 0, 0);

      RigidBody prevSegment = myBaseSegment; 
      RigidBody curSegment = null;
      double curHeight = 0;
      for (int i = 0; i < numSegments; i++) {
         curSegment = addBody ("hand"+i, "hand");
         double curSegmentZ = -(getCenter (curSegment).z + getHeight (curSegment) / 2);
         setBodyPose (curSegment, 0, 0, curSegmentZ - curHeight, -90, 0, 0);
   
         RigidTransform3d TDW = new RigidTransform3d();      
         TDW.p.set (0, 0, -curHeight);
         SphericalJoint joint = addSphericalJoint (curSegment, prevSegment, TDW);
   
         addFrameSpring (curSegment, prevSegment, 0, 0, 0, rk);
         curHeight += getHeight (curSegment);
         prevSegment = curSegment;
      }
      myEndSegment = curSegment;
      addMarker ("endpoint", myEndSegment, new Point3d(0, 0, -getHeight (myEndSegment)/2));

      addModel (myMechMod);
      //addMonitor (new ArmMover(myLowerArm));

      RigidTransform3d X = new RigidTransform3d();
      X.setIdentity();
      X.R.setRpy (0, Math.toRadians(90), 0);
      myMechMod.transformGeometry (X);
      addSkinMesh ();
   }
   
   
   protected ControlPanel panel;
   public void addPanel () {
      panel = new ControlPanel("Force Control", "");

      for (ForceEffector fe : myMechMod.forceEffectors ()) {
         if (fe instanceof FrameExciter) {
            FrameExciter fex = (FrameExciter)fe;
            panel.addWidget (fex.getName ()+" ex", fex, "excitation", -1.0, 1.0);
         }
      }
      addControlPanel (panel);
   }
   
   public void addTrackingController() {
      TrackingController tracker = new TrackingController(myMechMod, "tracker");

      if (endPointTarget) {
         FrameMarker endpoint = myMechMod.frameMarkers ().get ("endpoint");
         tracker.addMotionTarget(endpoint);   
         tracker.setTargetsPointRadius (0.021);
      } else {
         tracker.addMotionTarget (myEndSegment);
      }
      
      for (RigidBody body : myMechMod.rigidBodies ()) {
         if (body.isDynamic ()) {
            FrameExciter[] FEs = FrameExciter.addLinearFrameExciters (myMechMod, body, maxForce);
            for (FrameExciter frameExciter : FEs) {
               tracker.addExciter(frameExciter);
            }
         }
      }
      
      tracker.setExcitationBounds (-1d, 1d);
      tracker.addL2RegularizationTerm(1);
      tracker.createProbesAndPanel (this);
      addController (tracker);
      addPanel();
   }
   
   public void attach(DriverInterface driver) {    
      super.attach(driver);
      File workingDir = new File(ArtisynthPath.getSrcRelativePath (this, 
         "data/rigidTentacle/"+ (endPointTarget?"point":"frame")));
      ArtisynthPath.setWorkingDir(workingDir);

      addTrackingController ();
   }
    
   public void detach(DriverInterface driver) {    
      super.detach(driver);
   }
}
