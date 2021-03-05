package artisynth.demos.tutorial;

import artisynth.core.mechmodels.HingeJoint;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.workspace.RootModel;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.VectorNd;
import maspack.render.RenderProps;

/**
 * Uses constraint compliance and damping to regularize a four-bar linkage
 * constructed using hinge joints.  A single compliance and damping value is
 * applied to all bilateral constraints.  These two values are bundled into
 * properties of the root model so that they can be set easily in a control
 * panel.
 *
 * <p>A more general implementation should use separate compliance and damping
 * values for the constrained translational and rotational degrees of
 * freedom. Using the same values works in this case because the links have
 * unit length.
 */
public class FourBarLinkage extends RootModel {

   /**
    * Create a link with a length of 1.0, width of 0.25, and specified depth
    * and add it to the mech model. The parameters x, z, and deg specify the
    * link's position and orientation (in degrees) in the x-z plane.
    */
   protected RigidBody createLink (
      MechModel mech, String name, 
      double depth, double x, double z, double deg) {
      int nslices = 20; // num slices on the rounded mesh ends
      PolygonalMesh mesh =
         MeshFactory.createRoundedBox (1.0, 0.25, depth, nslices);
      RigidBody body = RigidBody.createFromMesh (
         name, mesh, /*density=*/1000.0, /*scale=*/1.0);
      body.setPose (new RigidTransform3d (x, 0, z, 0, Math.toRadians(deg), 0));
      mech.addRigidBody (body);
      return body;
   }

   /**
    * Create a hinge joint connecting one end of link0 with the other end of
    * link1, and add it to the mech model.
    */
   protected HingeJoint createJoint (
      MechModel mech, String name, RigidBody link0, RigidBody link1) {
      // easier to locate the link using TCA and TDB since we know where frames
      // C and D are with respect the link0 and link1
      RigidTransform3d TCA = new RigidTransform3d (0, 0,  0.5, 0, 0, Math.PI/2);
      RigidTransform3d TDB = new RigidTransform3d (0, 0, -0.5, 0, 0, Math.PI/2);
      HingeJoint joint = new HingeJoint (link0, TCA, link1, TDB);
      joint.setName (name);
      mech.addBodyConnector (joint);
      // set joint render properties
      joint.setAxisLength (0.4);
      RenderProps.setLineRadius (joint, 0.03);
      return joint;
   }

   public void build (String[] args) {
      // create a mech model and set rigid body damping parameters
      MechModel mech = new MechModel ("mech");
      addModel (mech);
      mech.setFrameDamping (1.0);
      mech.setRotaryDamping (4.0);

      // create four 'bars' from which to construct the linkage
      RigidBody[] bars = new RigidBody[4];
      bars[0] = createLink (mech, "link0", 0.2, -0.5,  0.0, 0);
      bars[1] = createLink (mech, "link1", 0.3,  0.0,  0.5, 90);
      bars[2] = createLink (mech, "link2", 0.2,  0.5,  0.0, 180);
      bars[3] = createLink (mech, "link3", 0.3,  0.0, -0.5, 270);
      // ground the left bar
      bars[0].setDynamic (false);

      // connect the bars using four hinge joints
      HingeJoint[] joints = new HingeJoint[4];
      joints[0] = createJoint (mech, "joint0", bars[0], bars[1]);
      joints[1] = createJoint (mech, "joint1", bars[1], bars[2]);
      joints[2] = createJoint (mech, "joint2", bars[2], bars[3]);
      joints[3] = createJoint (mech, "joint3", bars[3], bars[0]);

      // Set uniform compliance and damping for all bilateral constraints,
      // which are the first 5 constraints of each joint
      VectorNd compliance = new VectorNd(5);
      VectorNd damping = new VectorNd(5);
      for (int i=0; i<5; i++) {
         compliance.set (i, 0.000001);
         damping.set (i, 25000);
      }
      for (int i=0; i<joints.length; i++) {
         joints[i].setCompliance (compliance);
         joints[i].setDamping (damping);
      }
   }

}
