package artisynth.core.mechmodels;

import maspack.util.*;
import maspack.matrix.*;

public class JointLimitForceTest extends UnitTest {

   public static final double DTOR = Math.PI/180;
   public static final double RTOD = 180/Math.PI;

   public void testJacobians() {
      // create a MechModel with a body and a hinge joint
      MechModel mech = new MechModel("mech");
      double density = 1000;
      double len = 1.0;
      RigidBody body = RigidBody.createBox (null, len, len/5, len/5, density);
      body.setPosition (new Point3d (len/2, 0, 0));
      RigidBody body1 = RigidBody.createBox (null, len, 0.2, 0.2, density);
      mech.addRigidBody (body);
      
      HingeJoint joint = new HingeJoint (
         body, null, new Point3d (0, 0, 0), new Vector3d (0, -1, 0));
      mech.addBodyConnector (joint);

      // add a joint limit force
      JointCoordinateHandle handle = new JointCoordinateHandle (joint, 0);
      JointLimitForce jlf = new JointLimitForce ("jlf", handle);
      // set the limit tp 10.5 so when we iterate through the angles by degrees
      // we don't land on the stiffness discontinuity:
      double limit = DTOR*10.5;
      double stiffness = RTOD*10;
      double damping = RTOD*0.5;
      jlf.setLower (-limit, stiffness, damping, /*transition*/0);
      jlf.setUpper ( limit, stiffness, damping, /*transition*/0);
      mech.addForceEffector (jlf);   

      // transform the mech model so that the hinge axis is arbitrary
      RigidTransform3d TX = new RigidTransform3d(1, 2, 3, 1, 2, 3);
      mech.transformGeometry (TX);

      SolveMatrixTest tester = new SolveMatrixTest();
      tester.setUpdateConstraints (true); 
      // test the solve matrices in the joint range -30 to 30
      for (double ang=-30; ang<=30; ang++) {
         joint.setTheta (ang);
         double derr = tester.testDamping (mech, 1e-8, /*fmtStr*/null);
         if (derr > 1e-6) {
            throw new TestException ("Damping error=" + derr);
         }
         double kerr = tester.testStiffness (mech, 1e-8, /*fmtStr*/null);
         if (kerr > 1e-6) {
            throw new TestException ("Stiffness error=" + kerr);
         }
      }

      double transition = DTOR*10;
      jlf.setLowerTransition (transition);
      jlf.setUpperTransition (transition);
      // test the solve matrices again with a non-zero transition
      for (double ang=-30; ang<=30; ang++) {
         joint.setTheta (ang);
         double derr = tester.testDamping (mech, 1e-8, /*fmtStr*/null);
         if (derr > 1e-6) {
            throw new TestException ("Damping error=" + derr);
         }
         double kerr = tester.testStiffness (mech, 1e-8, /*fmtStr*/null);
         if (kerr > 1e-6) {
            throw new TestException ("Stiffness error=" + kerr);
         }
      }

  }

   public void test(){ 
      testJacobians();
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      JointLimitForceTest tester = new JointLimitForceTest();
      tester.runtest();
   }

}
