package artisynth.demos.test;

import artisynth.core.mechmodels.*;
import artisynth.core.workspace.*;
import artisynth.core.gui.*;
import maspack.matrix.*;
import maspack.geometry.*;
import maspack.properties.*;
import maspack.render.*;

/**
 * Uses constraint compliance and damping to regularize a four-bar linkage.  A
 * single compliance and damping value is applied to all constrained DOFs.
 * These two values are bundled into properties of the root model so that they
 * can be set easily in a control panel.
 *
 * <p>A more precise implementation should probably use separate compliance and
 * damping values for the constrained translational and rotational degrees of
 * freedom.
 */
public class FourBar extends RootModel {

   RevoluteJoint myJoints[];
   double myCompliance = 0;
   double myDamping = 0;

   // Set up compliance and damping properties in the root model. 
   // A more 

   public static PropertyList myProps =
      new PropertyList (FourBar.class, RootModel.class);

   static {
      myProps.add ("compliance", "joint compliance", 0);
      myProps.add ("damping", "joint damping factor", 0);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public double getCompliance () {
      return myCompliance;
   }

   public void setCompliance (double c) {
      myCompliance = c;
      // Note: the compliance vector has 6 DOF, even though the revolute joints
      // normally have only 5 constrained DOFs.  The sixth DOF is for the
      // unilateral constraint associated with joint limits.
      VectorNd vec = new VectorNd(6);
      for (int i=0; i<5; i++) {
         // constrained translational DOFs ...
         vec.set (0, c);
         vec.set (1, c);
         vec.set (2, c);
         // constrained rotational DOFs ...
         vec.set (3, c);
         vec.set (4, c);
      }
      for (int i=0; i<4; i++) {
         myJoints[i].setCompliance (vec);
      }
   }

   public double getDamping () {
      return myDamping;
   }

   public void setDamping (double d) {

      myDamping = d;
      // See comments in setCompliance() about why vec has a size of 6
      VectorNd vec = new VectorNd(6);
      for (int i=0; i<5; i++) {
         // constrained translational DOFs ...
         vec.set (0, d);
         vec.set (1, d);
         vec.set (2, d);
         // constrained rotational DOFs ...
         vec.set (3, d);
         vec.set (4, d);
      }
      for (int i=0; i<4; i++) {
         myJoints[i].setDamping (vec);
      }
   }

   protected RigidBody createAndAddBody (
      MechModel mech, String name, 
      double lenx, double leny, double lenz, double x, double z, double deg) {
      int nslices = 20;
      RigidBody link1 = new RigidBody ("link1");
      PolygonalMesh mesh =
         MeshFactory.createRoundedBox (lenx, leny, lenz, nslices);
      mesh.transform (new RigidTransform3d (0, 0, 0,  0, Math.PI/2, 0));
      RigidBody body = RigidBody.createFromMesh (
         name, mesh, 1000.0, 1.0);
      body.setPose (new RigidTransform3d (x, 0, z, 0, Math.toRadians(deg), 0));
      mech.addRigidBody (body);
      return body;
   }

   protected RevoluteJoint createAndAddJoint (
      MechModel mech, String name, 
      RigidBody link0, RigidBody link1, double x, double z) {

      RigidTransform3d TCW =
         new RigidTransform3d (x, 0, z, 0, 0, Math.PI/2);
      RevoluteJoint joint = new RevoluteJoint (link0, link1, TCW);

      joint.setName (name);
      joint.setAxisLength (0.4);
      RenderProps.setLineRadius (joint, 0.02);
      mech.addBodyConnector (joint);
      return joint;
   }

   private void addControlPanel() {
      ControlPanel panel = new ControlPanel ("options", "");
      panel.addWidget (this, "compliance");
      panel.addWidget (this, "damping");
      addControlPanel (panel);
   }


   public void build (String[] args) {

      MechModel mech = new MechModel ("mech");
      addModel (mech);
      mech.setGravity (0, 0, -9.8);
      mech.setFrameDamping (1.0);
      mech.setRotaryDamping (4.0);
      mech.setIntegrator (MechSystemSolver.Integrator.SymplecticEuler);

      RigidBody[] links = new RigidBody[4];

      links[0] = createAndAddBody (
         mech, "link0", 1.2, 0.3, 0.3,   0, 0.5, 0);
      links[1] = createAndAddBody (
         mech, "link1", 1.2, 0.3, 0.2,  0.5, 0, 90);
      links[2] = createAndAddBody (
         mech, "link2", 1.2, 0.3, 0.3,   0, -0.5, 0);
      links[3] = createAndAddBody (
         mech, "link3", 1.2, 0.3, 0.2, -0.5, 0, 90);

      links[2].setDynamic (false);

      myJoints = new RevoluteJoint[4];
      myJoints[0] = createAndAddJoint (
         mech, "joint0", links[0], links[1],  0.5,  0.5);
      myJoints[1] = createAndAddJoint (
         mech, "joint1", links[1], links[2],  0.5, -0.5);
      myJoints[2] = createAndAddJoint (
         mech, "joint2", links[2], links[3], -0.5, -0.5);
      myJoints[3] = createAndAddJoint (
         mech, "joint3", links[3], links[0], -0.5,  0.5);


      // compliance and damping parameters (hand tuned)
      setCompliance (0.000001);
      setDamping (10000.0);

      addControlPanel();
   }

}
