package maspack.collision;

import java.util.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.geometry.*;

public class RobustPredsTest extends UnitTest {

   ArrayList<int[]> myIndices5 = generateIndexPermutations (5);   

   Point3d createPlanarPoint (double x, double y, RotationMatrix3d R) {
      Point3d p = new Point3d (x, y, 0);
      p.transform (R);
      return p;
   }

   public void testCoplanar (
      double s0x, double s0y, double s1x, double s1y,
      double[] tri, boolean possible, double ix, double iy) {

      AxisAlignedRotation[] orientations = new AxisAlignedRotation[] {
         AxisAlignedRotation.X_Y, 
         AxisAlignedRotation.Y_Z,
         AxisAlignedRotation.Z_X,
         AxisAlignedRotation.Y_X, 
         AxisAlignedRotation.Z_Y,
         AxisAlignedRotation.X_Z
      };

      for (AxisAlignedRotation rot : orientations) {
         RotationMatrix3d R = rot.getMatrix();

         Point3d s0 = createPlanarPoint (s0x, s0y, R);
         Point3d s1 = createPlanarPoint (s1x, s1y, R);
         Point3d t0 = createPlanarPoint (tri[0], tri[1], R);
         Point3d t1 = createPlanarPoint (tri[2], tri[3], R);
         Point3d t2 = createPlanarPoint (tri[4], tri[5], R);
         
         Point3d ichk = createPlanarPoint (ix, iy, R);
         Point3d ipnt = new Point3d ();

         for (int i=0; i<myIndices5.size(); i++) {
            int[] idxs = myIndices5.get(i);
            int intersects = RobustPreds.intersectSegmentTriangle (
               ipnt, idxs[0], s0, idxs[1], s1,
               idxs[2], t0, idxs[3], t1, idxs[4], t2);
            if (!possible) {
               if (intersects != 0) {
                  throw new TestException (
                     "Intersection detected when not possible");
               }
            }
            else {
               if (intersects != 0) {
                  if (!ichk.epsilonEquals (ipnt, 1e-8)) {
                     System.out.println ("intersection point=" + ipnt);
                     System.out.println ("expected=" + ichk);
                     throw new TestException (
                        "Intersection point in wrong place");
                  }
               }
            }
         }
      }
   }

   public void testCoplanar() {
      double[] tri0 = new double[] {
         -1.5,  0.5, -1.5, -0.5, -0.5,  0.5};

      // completely outside
      testCoplanar (-2.5,  1.5, -2.5, -1.5, tri0, false, 0, 0);
      testCoplanar (-2.5,  0.5, -1.6,  0.5, tri0, false, 0, 0);
      testCoplanar (-2.5, -0.5, -1.6,  0.5, tri0, false, 0, 0);
      testCoplanar (-1.5,  0.6, -0.5,  0.6, tri0, false, 0, 0);
      testCoplanar (-1.5, -0.6, -0.5,  0.4, tri0, false, 0, 0);
      testCoplanar (-1.6,  0.5, -2.0,  0.9, tri0, false, 0, 0);
      testCoplanar (-1.6,  0.5, -1.6,  0.9, tri0, false, 0, 0);

      // cutting through the middle
      testCoplanar (-1.5,  0.5,  -0.5, -0.5, tri0, true, -1.25, 0.25);
      testCoplanar (-2.5,  1.5,  -0.5, -0.5, tri0, true, -1.25, 0.25);
      testCoplanar (-2.5,  1.5,  -1.0,  0.0, tri0, true, -1.25, 0.25);
      testCoplanar (-2.5,  1.5,  -1.0,  0.0, tri0, true, -1.25, 0.25);

      testCoplanar (-2.5,  0.0,   2.5,  0.0, tri0, true, -1.25, 0.0);
      testCoplanar (-1.5,  0.0,   2.5,  0.0, tri0, true, -1.25, 0.0);
      testCoplanar (-2.5,  0.0,  -1.0,  0.0, tri0, true, -1.25, 0.0);

      // just touching edge 01
      testCoplanar (-1.0, -0.5,  -1.0,  0.0, tri0, true, -1.0, 0.0);
      testCoplanar ( 2.5,  0.0,  -1.0,  0.0, tri0, true, -1.0, 0.0);

      // just touching edge 12
      testCoplanar (-1.5,  1.5,  -1.0,  0.5, tri0, true, -1.0, 0.5);
      testCoplanar (-1.5,  1.5,  -1.0,  0.5, tri0, true, -1.0, 0.5);
      testCoplanar ( 0.5,  1.5,  -1.0,  0.5, tri0, true, -1.0, 0.5);

      // just touching edge 20
      testCoplanar (-2.5,  0.5,  -1.5,  0.0, tri0, true, -1.5, 0.0);
      testCoplanar (-2.5,  0.0,  -1.5,  0.0, tri0, true, -1.5, 0.0);
      testCoplanar (-2.5, -0.5,  -1.5,  0.0, tri0, true, -1.5, 0.0);

      // just touching vertex 0
      testCoplanar (-2.5, -0.5,  -1.5, -0.5, tri0, true, -1.5, -0.5);
      testCoplanar ( 2.5, -0.5,  -1.5, -0.5, tri0, true, -1.5, -0.5);
      testCoplanar (-2.5, -2.0,  -1.5, -0.5, tri0, true, -1.5, -0.5);

      // just touching vertex 1
      testCoplanar (-2.5,  1.5,  -0.5,  0.5, tri0, true, -0.5,  0.5);
      testCoplanar ( 2.5,  1.5,  -0.5,  0.5, tri0, true, -0.5,  0.5);
      testCoplanar (-0.5,  1.5,  -0.5,  0.5, tri0, true, -0.5,  0.5);

      // completely inside
      testCoplanar (-1.5,  0.5,  -1.0,  0.0, tri0, true, -1.25, 0.25);
      testCoplanar (-1.4,  0.4,  -1.0,  0.0, tri0, true, -1.20, 0.20);
      testCoplanar (-1.5,  0.5,  -1.1,  0.1, tri0, true, -1.30, 0.30);
      testCoplanar (-1.4,  0.4,  -1.1,  0.1, tri0, true, -1.25, 0.25);

      // just touching vertex 0
      testCoplanar (-2.5, -0.5,  -1.5, -0.5, tri0, true, -1.5, -0.5);
      testCoplanar ( 2.5, -0.5,  -1.5, -0.5, tri0, true, -1.5, -0.5);
      testCoplanar (-2.5, -2.0,  -1.5, -0.5, tri0, true, -1.5, -0.5);

      // just touching vertex 1
      testCoplanar (-2.5,  1.5,  -0.5,  0.5, tri0, true, -0.5,  0.5);
      testCoplanar ( 2.5,  1.5,  -0.5,  0.5, tri0, true, -0.5,  0.5);
      testCoplanar (-0.5,  1.5,  -0.5,  0.5, tri0, true, -0.5,  0.5);

      // just touching vertex 2
      testCoplanar (-2.5,  1.5,  -1.5,  0.5, tri0, true, -1.5, 0.5);
      testCoplanar (-2.5,  0.5,  -1.5,  0.5, tri0, true, -1.5, 0.5);
      testCoplanar (-0.5,  1.5,  -1.5,  0.5, tri0, true, -1.5, 0.5);
   }

   boolean contains (int idx, int[] perms, int len) {
      for (int i=0; i<len; i++) {
         if (perms[i] == idx) {
            return true;
         }
      }
      return false;         
   }

   void generateIndexPermutations (
      ArrayList<int[]> perms, int[] current, int idx0, int numi) {

      int[] unused = new int[numi-idx0];
      int k=0;
      for (int i=0; i<numi; i++) {
         if (!contains (i, current, idx0)) {
            unused[k++] = i;
         }
      }
      if (idx0 == numi-1) {
         current[idx0] = unused[0];
         perms.add (Arrays.copyOf (current, current.length));
      }
      else {
         for (k=0; k<unused.length; k++) {
            current[idx0] = unused[k];
            generateIndexPermutations (perms, current, idx0+1, numi);
         }
      }
   }

   ArrayList<int[]> generateIndexPermutations (int numi) {

      ArrayList<int[]> perms = new ArrayList<int[]>();
      int[] current = new int[numi];
      generateIndexPermutations (perms, current, 0, numi);
      return perms;
   }

   String toString (int[] idxs) {
      StringBuilder stb = new StringBuilder();
      for (int i=0; i<idxs.length; i++) {
         if (i>0) {
            stb.append (' ');
         }
         stb.append (idxs[i]);
      }
      return stb.toString();
   }

   public void test() {
      testCoplanar();
   }

   public static void main (String[] args) {
      RobustPredsTest tester = new RobustPredsTest();
    
      tester.runtest();
   }
}
