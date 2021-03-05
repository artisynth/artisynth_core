package artisynth.demos.tutorial;

import java.awt.Color;
import java.io.IOException;

import artisynth.core.femmodels.SkinMeshBody;
import artisynth.core.gui.ControlPanel;
import artisynth.core.materials.SimpleAxialMuscle;
import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.Muscle;
import artisynth.core.mechmodels.HingeJoint;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.workspace.RootModel;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.util.PathFinder;

public class RigidBodySkinning extends RootModel {

   // create and add a rigid body using the "barm" mesh
   public RigidBody addBody (MechModel mech, String name) throws IOException {
      String meshPath = PathFinder.getSourceRelativePath (
         this, "../mech/geometry/barm.obj");
      PolygonalMesh mesh = new PolygonalMesh (meshPath);
      RigidBody body = RigidBody.createFromMesh (name, mesh, 1000.0, 0.1);

      mech.addRigidBody (body);
      return body;
   }
    
   public void build (String[] args) throws IOException {
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      // set damping parameters for rigid bodies
      mech.setFrameDamping (10);
      mech.setRotaryDamping (100.0);

      // create a toy "arm" conisting of upper and lower rigid bodies connected
      // by a revolute joint:
      double len = 2.0;
      RigidBody upper = addBody (mech, "upper");
      upper.setPose (new RigidTransform3d (0, 0, len/2));
      upper.setDynamic (false);  // upper body is fixed

      RigidBody lower = addBody (mech, "lower");
      // reposition the lower body"
      double angle = Math.toRadians(225);
      double sin = Math.sin(angle);
      double cos = Math.cos(angle);
      lower.setPose (new RigidTransform3d (sin*len/2, 0, cos*len/2, 0, angle, 0));

      // add the revolute joint between the upper and lower bodies:
      HingeJoint joint =
         new HingeJoint (lower, upper, new Point3d(), Vector3d.Y_UNIT);
      joint.setName ("elbow");
      mech.addBodyConnector (joint);

      // add two frame markers and a "muscle" to move the lower body
      FrameMarker mku = mech.addFrameMarker (
         upper, new Point3d(-len/20, 0, len/2.4));
      FrameMarker mkl = mech.addFrameMarker (
         lower, new Point3d(len/20, 0, -len/4));
      Muscle muscle = new Muscle("muscle");
      muscle.setMaterial (new SimpleAxialMuscle (1000.0, 0, 2000.0));
      mech.attachAxialSpring (mku, mkl, muscle);
        
      // create an ellipsoidal base mesh for the SkinMeshBody by scaling a
      // spherical mesh
      PolygonalMesh mesh = MeshFactory.createSphere (1.0, 12, 12);
      mesh.scale (1, 1, 2.5);
      mesh.transform (
         new RigidTransform3d (-0.6, 0, 0, 0, Math.toRadians(22.5),0));

      // create the skinMesh, with the upper and lower bodies as master bodies
      SkinMeshBody skinMesh = new SkinMeshBody (mesh);
      skinMesh.addMasterBody (upper);
      skinMesh.addMasterBody (lower);
      skinMesh.computeAllVertexConnections();
      mech.addMeshBody (skinMesh);

      // add a control panel to adjust the muscle excitation and frameBlending
      ControlPanel panel = new ControlPanel();
      panel.addWidget (muscle, "excitation");
      panel.addWidget (skinMesh, "frameBlending");
      addControlPanel (panel);

      // set up render properties
      RenderProps.setFaceStyle (skinMesh, Renderer.FaceStyle.NONE);
      RenderProps.setDrawEdges (skinMesh, true);
      RenderProps.setLineColor (skinMesh, Color.CYAN);
      RenderProps.setSpindleLines (muscle, 0.06, Color.RED);
      RenderProps.setSphericalPoints (mech, 0.05, Color.WHITE);
      RenderProps.setFaceColor (joint, Color.BLUE); 
      joint.setShaftLength(len/3);
      joint.setShaftRadius(0.05);
      RenderProps.setFaceColor (mech, new Color (0.8f, 0.8f, 0.8f));
   }
}
