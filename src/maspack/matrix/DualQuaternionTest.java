/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.*;

class DualQuaternionTest extends UnitTest {

   public void transformTest() {
      
      DualQuaternion q = new DualQuaternion();
      RigidTransform3d trans = new RigidTransform3d();
      RigidTransform3d trans2 = new RigidTransform3d();
      
      Vector3d v = new Vector3d();
      Vector3d vr1 = new Vector3d();
      Vector3d vr2 = new Vector3d();
      
      Point3d p = new Point3d();
      Point3d pr1 = new Point3d();
      Point3d pr2 = new Point3d();
      
      for (int i=0; i<10; i++) {
         trans.setRandom();
         q.set(trans);
         q.getRigidTransform3d(trans2);
         
         if (!trans.epsilonEquals(trans2, 1e-10)) {
            throw new TestException("Wrong transformation");
         }
         
         for (int j=0; j<10; j++) {
            p.setRandom();
            v.setRandom(); 
            
            vr1.transform(trans, v);
            q.transform(vr2, v);
            double err = vr2.distance(vr1);
            if (err > 1e-15) {
               throw new TestException("Uh oh, transformation is incorrect");
            }
            pr1.transform(trans, p);
            q.transform(pr2, p);
            err = pr2.distance(pr1);
            if (err > 1e-15) {
               throw new TestException("Uh oh, transformation is incorrect");
            }

            q.inverseTransform(vr2);
            if (vr2.distance(v) > 1e-15) {
               throw new TestException("Uh oh, transformation is incorrect");
            }
            q.inverseTransform(pr2);
            if (pr2.distance(p) > 1e-15) {
               throw new TestException("Uh oh, transformation is incorrect");
            }
            
         }
      }
   }
   
   public void dipTest(boolean printMe, int nSteps) {
      
      RigidTransform3d poseA = new RigidTransform3d(
         new Vector3d(-10,0,10), new AxisAngle(1,0,0,Math.toRadians(30)));
      RigidTransform3d poseB = new RigidTransform3d(
         new Vector3d(10,0,30), new AxisAngle(1,0,0,Math.toRadians(-60)));
      
      DualQuaternion qE[] = {new DualQuaternion(poseA), new DualQuaternion(poseB)};
      DualQuaternion qI = new DualQuaternion();
      
      Point3d pnts [] = new Point3d[] {
         new Point3d(0,0,0),
         new Point3d(1,0,0),
         new Point3d(0,1,0),
         new Point3d(0,0,1)
      };
      Point3d out = new Point3d();
      
      double dt = 1.0/(nSteps-1);
      double[] w = new double[2];
      
      if (printMe) {
         System.out.println("x = [");
      }
      
      for (int i=0; i<nSteps; i++) {
         w[0] = 1-i*dt;
         w[1] = i*dt;
         qI.dualQuaternionIterativeBlending(w,qE, w.length, 1e-8, 5);         
         for (Point3d pnt : pnts) {
            qI.transform(out, pnt);
            if (printMe) {
               System.out.println(out);
            }
         }       
      }
      
      if (printMe) {
         System.out.println("];\n\ny=[");
      }
      
      for (int i=0; i<nSteps; i++) {
         w[0] = 1-i*dt;
         w[1] = i*dt;
         qI.dualQuaternionLinearBlending(w,qE,w.length);         
         for (Point3d pnt : pnts) {
            qI.transform(out, pnt);
            if (printMe) {
               System.out.println(out);
            }
         }       
      }
      
      if (printMe) {
         System.out.println("];\n\nz=[");
      }
      
      for (int i=0; i<nSteps; i++) {
         w[0] = 1-i*dt;
         w[1] = i*dt;
         qI.screwLinearInterpolate(qE[0], w[1], qE[1]);         
         for (Point3d pnt : pnts) {
            qI.transform(out, pnt);
            if (printMe) {
               System.out.println(out);
            }
         }       
      }
      
      if (printMe) {
         System.out.println("];");
      }
   }
   
   public void sclerpTest(boolean printMe, int nSteps) {
      
      RigidTransform3d poseA = new RigidTransform3d(
         new Vector3d(-10,0,10), new AxisAngle(1,0,0,Math.toRadians(30)));
      RigidTransform3d poseB = new RigidTransform3d(
         new Vector3d(10,0,30), new AxisAngle(1,0,0,Math.toRadians(-60)));
      
      DualQuaternion qA = new DualQuaternion(poseA);
      DualQuaternion qB = new DualQuaternion(poseB);
      DualQuaternion q = new DualQuaternion();
      
      Point3d pnts [] = new Point3d[] {
         new Point3d(0,0,0),
         new Point3d(1,0,0),
         new Point3d(0,1,0),
         new Point3d(0,0,1)
      };
      Point3d out = new Point3d();
      
      double dt = 1.0/(nSteps-1);
      for (int i=0; i<nSteps; i++) {
         q.screwLinearInterpolate(qA, i*dt, qB);
         for (Point3d pnt : pnts) {
            q.transform(out, pnt);
            
            if (printMe) {
               System.out.println(out);
            }
         }
      }
      
   }
   
   public void test() {
      transformTest();
      sclerpTest(false, 100000);
      dipTest(false, 100000);
   }

   public static void main (String[] args) {
      DualQuaternionTest tester = new DualQuaternionTest();

      RandomGenerator.setSeed (0x1234);
      tester.runtest();
   }
}
