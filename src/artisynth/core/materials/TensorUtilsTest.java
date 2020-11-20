package artisynth.core.materials;

import maspack.util.*;
import maspack.matrix.*;

public class TensorUtilsTest extends UnitTest {

   void testRotateTangent (
      Matrix6d D, SymmetricMatrix3d eps, RotationMatrix3d R) {

      SymmetricMatrix3d sigR = new SymmetricMatrix3d();
      SymmetricMatrix3d sig = new SymmetricMatrix3d();
      SymmetricMatrix3d sigx = new SymmetricMatrix3d();
      Matrix6d DR = new Matrix6d();

      TensorUtils.rotateTangent (DR, D, R);

      SymmetricMatrix3d epsR = new SymmetricMatrix3d(eps);
      epsR.mulLeftAndTransposeRight (R);
      TensorUtils.mulTangent (sigR, DR, epsR);
      TensorUtils.mulTangent (sig, D, eps);
      sigx.set (sigR);
      sigx.mulTransposeLeftAndRight (R);
      checkEquals ("sigma", sigx, sig, 1e-12);

      Matrix6d DX = new Matrix6d();

      TensorUtils.unrotateTangent (DX, DR, R);
      checkEquals ("D rotated back", DX, D, 1e-12);
   }

   void testRotateTangent() {
      int ntests = 100;

      Matrix6d D = new Matrix6d();
      D.setRandom();
      // make symmetric
      D.mulTransposeRight (D, D);

      //D.setDiagonal (10, 20, 30, 40, 50, 60);
      D.setRandom();

      SymmetricMatrix3d eps = new SymmetricMatrix3d();
      RotationMatrix3d R = new RotationMatrix3d ();
      for (int i=0; i<ntests; i++) {
         eps.setRandom();
         R.setRandom();
         testRotateTangent (D, eps, R);
      }
   }

   public void test() {
      testRotateTangent();
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      TensorUtilsTest tester = new TensorUtilsTest();
      tester.runtest();
   }

}
