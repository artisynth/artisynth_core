package artisynth.core.mechmodels;

import java.util.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.function.*;

public class BodyConnectorTest extends UnitTest {

   private RigidBody addLink (MechModel mech, double x, double y) {
      RigidBody link = RigidBody.createBox (null, 1.0, 0.2, 0.2, /*density*/1000);
      link.setPose (new RigidTransform3d (x, y, 0));
      mech.addRigidBody (link);
      return link;
   }

   private HingeJoint addJoint (
      MechModel mech, RigidBody bodyA, RigidBody bodyB, double x, double y) {
      HingeJoint joint =
         new HingeJoint (bodyA, bodyB, new RigidTransform3d (x, y, 0));
      mech.addBodyConnector (joint);
      return joint;
   }

   private JointCoordinateCoupling addCoupling (
      MechModel mech, HingeJoint slaveJoint, HingeJoint masterJoint) {
      ArrayList<JointCoordinateHandle> coords = new ArrayList<>();
      coords.add (new JointCoordinateHandle (slaveJoint, 0));
      coords.add (new JointCoordinateHandle (masterJoint, 0));
      JointCoordinateCoupling coupling =
         new JointCoordinateCoupling(coords, new LinearFunction1x1(1.5, 0));
      mech.addConstrainer (coupling);
      return coupling;
   }

   public void testWithinLoop() {
      // Create some linkage configuarations and test withinLoop() on the
      // joints.  The configurations don't need to be physically realizable
      // since withinLoop() only works with the topology.
      RigidBody[] links = new RigidBody[4];
      HingeJoint[] joints = new HingeJoint[5];

      MechModel mech = new MechModel();
      links[0] = addLink (mech, 0, 0);
      links[1] = addLink (mech, 1, 0);
      links[2] = addLink (mech, 2, 0);
      links[3] = addLink (mech, 3, 0);

      joints[1] = addJoint (mech, links[1], links[0], 0.5, 0);
      joints[2] = addJoint (mech, links[2], links[1], 1.5, 0);
      joints[3] = addJoint (mech, links[3], links[2], 2.5, 0);

      checkEquals ("joints[1] withinLoop", joints[1].withinLoop(mech), false);
      checkEquals ("joints[2] withinLoop", joints[2].withinLoop(mech), false);
      checkEquals ("joints[3] withinLoop", joints[3].withinLoop(mech), false);

      joints[0] = addJoint (mech, links[0], null, -0.5, 0);
      checkEquals ("joints[0] withinLoop", joints[0].withinLoop(mech), false);

      // attach the last joint to ground, and now all should be in a loop
      joints[4] = addJoint (mech, links[3], null, 3.5, 0);
      for (int j=0; j<5; j++) {
         checkEquals (
            "joints["+j+"] withinLoop", joints[j].withinLoop(mech), true);
      }

      // remove the last joint; should free up the other joints
      mech.removeBodyConnector (joints[4]);
      for (int j=0; j<4; j++) {
         checkEquals (
            "joints["+j+"] withinLoop", joints[j].withinLoop(mech), false);
      }

      // remove first joint and replace it with one connecting links 0 and 3
      mech.removeBodyConnector (joints[0]);
      joints[0] = addJoint (mech, links[0], links[3], -0.5, 0);
      for (int j=0; j<4; j++) {
         checkEquals (
            "joints["+j+"] withinLoop", joints[j].withinLoop(mech), true);
      }

      // replace first joint with one connected to ground
      mech.removeBodyConnector (joints[0]);
      joints[0] = addJoint (mech, links[0], null, -0.5, 0);
      // add constrainer between joints 1 and 2
      JointCoordinateCoupling coupling0 =
         addCoupling (mech, joints[1], joints[2]);
      checkEquals ("joints[0] withinLoop", joints[0].withinLoop(mech), false);
      checkEquals ("joints[1] withinLoop", joints[1].withinLoop(mech), true);
      checkEquals ("joints[2] withinLoop", joints[2].withinLoop(mech), true);
      checkEquals ("joints[3] withinLoop", joints[3].withinLoop(mech), false);
   }

   public void test() {
      testWithinLoop();
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      BodyConnectorTest tester = new BodyConnectorTest();
      tester.runtest();
   }

}
