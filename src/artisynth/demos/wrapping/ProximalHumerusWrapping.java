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
import maspack.collision.*;
import maspack.render.*;
import maspack.render.Renderer.*;
import maspack.properties.*;

public class ProximalHumerusWrapping extends RootModel {

   public void build (String[] args) throws IOException {
      // create a mech model with appropriate rigid body damping
      MechModel mech = new MechModel ("mech");
      mech.setFrameDamping (0.1);
      mech.setRotaryDamping (0.01);
      addModel (mech);

      // create and add the humerus bone
      double density = 1000;

      System.out.println ("creating humerus ...");
      String meshPath = PathFinder.getSourceRelativePath (
         this, "geometry/HumerusLeft.obj");
      PolygonalMesh humerusMesh = new PolygonalMesh (meshPath);
      RigidBody humerus = RigidBody.createFromMesh (
         "humerus", humerusMesh, density, /*scale=*/0.0033);
      humerus.centerPoseOnCenterOfMass();
      mech.addRigidBody (humerus);

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

      // add a spherical joint, centered at the world origin (which is happens
      // to be roughly at the center of the proximal end of the humerus)
      SphericalJoint joint = new SphericalJoint (humerus, Point3d.ZERO);
      mech.addBodyConnector (joint);

      // add a frame spring to stabilize the joint
      FrameSpring fspring = new FrameSpring ("fspring", 0, 0.1, 0, 0);
      mech.attachFrameSpring (humerus, null, fspring);

      // set the distance grid resolution
      System.out.println ("setting grid resolution ...");
      DistanceGridComp gcomp = humerus.getDistanceGridComp();
      gcomp.setResolution (new Vector3i (30, 30, 30));

      // add a muscle wrapping strand 
      System.out.println ("adding muscle ...");
      FrameMarker mkr = mech.addFrameMarkerWorld (
         humerus, new Point3d (-0.0037, 0.012, -0.04));
      mkr.setName ("insertion");
      Particle org = new Particle ("origin", 1.0, 0.0078, -0.0368, 0.025);
      org.setDynamic (false);
      mech.addParticle (org);
      MultiPointMuscle muscle = new MultiPointMuscle ("muscle");
      muscle.setMaterial (new SimpleAxialMuscle (0, 0, 10.0));
      muscle.addWrappable (humerus);
      muscle.addPoint (org);
      muscle.setSegmentWrappable (100, new Point3d[] {
            new Point3d (-0.0038, 0.024, 0.028)
         });
      muscle.addPoint (mkr);
      System.out.println ("updating wrap segments ...");
      muscle.updateWrapSegments();
      mech.addMultiPointSpring (muscle);

      // rotate the whole model able the z axis
      mech.transformGeometry (new RigidTransform3d (0, 0, 0, Math.PI, 0, 0));

      // add probe to control excitation
      NumericInputProbe inprobe =
         new NumericInputProbe (
            muscle, "excitation", 0.0, 6.0);
      inprobe.addData (
         new double[] { 0, 0, 1.5, 1.0, 3.0, 0, 4.5, 1.0, 6.0, 0},
         NumericInputProbe.EXPLICIT_TIME);
      addInputProbe (inprobe);

      // add probe to control the origin point
      inprobe = new NumericInputProbe (
            org, "targetPosition", 0.0, 6.0);
      // note: origin data flipped 180 about from original pos
      inprobe.addData (
         new double[] { 0, -0.0078, 0.0368, 0.025,
                        3, -0.0078, 0.0368, 0.010,
                        6, -0.0078, 0.0368, 0.015},
         NumericInputProbe.EXPLICIT_TIME);
      addInputProbe (inprobe);

      // add pan controller
      //addController (new PanController (this, 6.0, 0.0, 6.0));

      // set rendering properties
      RenderProps.setSphericalPoints (mech, 0.003, new Color (102,102,255));
      RenderProps.setCylindricalLines (mech, 0.002, Color.RED);
      RenderProps.setFaceColor (mech, new Color (204, 204, 255));
      System.out.println ("build done");

   }

}
