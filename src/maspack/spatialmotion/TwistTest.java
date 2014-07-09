/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.spatialmotion;

import maspack.matrix.*;
import maspack.util.*;

public class TwistTest {
   static double DOUBLE_PREC = 2.220446049250313e-16;
   static double EPS = 1000 * DOUBLE_PREC;

   public void testExtrapolate (RigidTransform3d X, Twist tw) {
      RigidTransform3d XCheck = new RigidTransform3d();
      RigidTransform3d XRes = new RigidTransform3d();
      RigidTransform3d XDel = new RigidTransform3d();
      double h = 3;
      Twist twh = new Twist();
      twh.scale (h, tw);
      twh.setTransform (XDel);
      XCheck.mul (X, XDel);
      XRes.set (X);
      tw.extrapolateTransform (XRes, h);
      if (!XCheck.epsilonEquals (XRes, EPS)) {
         System.out.println ("XCheck=\n" + XCheck);
         System.out.println ("XRes=\n" + XRes);
         throw new TestException ("testExtrapolate");
      }
   }

   public void execute() {
      RandomGenerator.setSeed (0x1234);
      int nrandom = 100;
      Twist tw0 = new Twist();
      RigidTransform3d X = new RigidTransform3d();
      for (int i = 0; i < nrandom; i++) {
         X.setRandom();
         tw0.setRandom();
         testExtrapolate (X, tw0);
      }
   }

   public static void main (String[] args) {
      RigidTransform3d X = new RigidTransform3d();
      X.p.set (0, 0, 10);

      Twist tw = new Twist (1, 1, 1, 0, 0, 0);
      Twist tw2 = new Twist();
      tw2.transform (X, tw);
      TwistTest tester = new TwistTest();
      tester.execute();
      System.out.println ("\nPassed\n");
   }
}
