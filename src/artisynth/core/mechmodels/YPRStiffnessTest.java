package artisynth.core.mechmodels;

import maspack.util.*;
import maspack.matrix.*;

public class YPRStiffnessTest extends UnitTest {

   MechModel myMech;   
   RigidBody myBody;

   public void testGravityLoading() {
      MechModel mech = new MechModel();
      RigidBody body = RigidBody.createBox ("box", 1.0, 0.2, 0.2, 1000.0);
      Vector3d com = new Vector3d();
      com.setRandom();
      body.transformCoordinateFrame (
         new RigidTransform3d (-com.x, -com.y, -com.z));
      mech.addRigidBody (body);

      SparseBlockMatrix K = mech.getTrueStiffnessMatrix();
      Matrix6dBlock M = (Matrix6dBlock)K.getBlock(0,0);
      Matrix3d Kg = new Matrix3d();
      Kg.setSkewSymmetric (mech.getGravity());
      Kg.scale (body.getMass());
      Kg.crossProduct (Kg, com);
      M.addSubMatrix33 (Kg);
      
      System.out.println ("K=\n" + K.toString ("%12.8f"));         
      VectorNd f = new VectorNd (mech.getActiveVelStateSize());
      mech.updateForces (/*time=*/0.0);
      mech.getActiveForces (f);
      YPRStiffnessUtils.convertStiffnessToYPR (
         K, f, mech.getActiveDynamicComponents());
      System.out.println ("K YPR=\n" + K.toString ("%12.8f"));         
   }

   public void test() {
      testGravityLoading();
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      YPRStiffnessTest tester = new YPRStiffnessTest();
      tester.runtest();
   }

}
