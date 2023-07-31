package maspack.geometry; 

import java.util.*;

import maspack.matrix.*;
import maspack.util.*;

public class Polygon3dTest extends UnitTest {

   private double DOUBLE_PREC = 1e-16;
   private double EPS = 1000*DOUBLE_PREC;

   protected Polygon3d createPoly (double[] coords2d, RigidTransform3d TPW) {
      Polygon3d poly = new Polygon3d();
      int npts = coords2d.length/2;
      for (int i=0; i<2*npts; i += 2) {
         Point3d pnt = new Point3d (coords2d[i+0], coords2d[i+1], 0);
         pnt.inverseTransform (TPW);
         poly.appendVertex (new PolygonVertex3d (pnt));
      }
      return poly;
   }

   void testCentroidAndArea (
      double[] coords2d, Point2d centChk2d, double areaChk) {

      RigidTransform3d TPW = new RigidTransform3d();
      TPW.setRandom();
      Point3d centChk = new Point3d (centChk2d.x, centChk2d.y, 0);
      centChk.inverseTransform (TPW);

      Polygon3d poly = createPoly (coords2d, TPW);
      Point3d cent = poly.computeCentroid();
      double area = poly.computePlanarArea();
      
      if (poly.numVertices() > 0) {
         checkEquals ("centroid", cent, centChk, EPS);
      }
      else {
         checkEquals ("centroid", cent, new Point3d(), EPS);
      }
      checkEquals ("area", area, areaChk, EPS);
   }

   private void testCentroidAndArea() {

      testCentroidAndArea (
         new double[] { }, new Point2d(), 0);
      testCentroidAndArea (
         new double[] { 1,2 }, new Point2d(1,2), 0);
      testCentroidAndArea (
         new double[] { 0,0, 1,1 }, new Point2d(0.5,0.5), 0);
      testCentroidAndArea (
         new double[] { 0,0, 1,1 }, new Point2d(0.5,0.5), 0);
      testCentroidAndArea (
         new double[] { 0,0, 1,0, 1,1 }, new Point2d(2/3.0,1/3.0), 0.5);
      testCentroidAndArea (
         new double[] { 0,0, 1,0, 1,1, 0,1}, new Point2d(0.5,0.5), 1.0);
      testCentroidAndArea (
         new double[] { 0,0, 2,0, 2,1, 1,1, 1,2, 0,2},
         new Point2d(1,1), 3.0);
      // other way around:
      testCentroidAndArea (
         new double[] { 0,0, 0,2, 1,2, 1,1, 2,1, 2,0},
         new Point2d(1,1), 3.0);
      testCentroidAndArea (
         new double[] { 0,0, 3,0, 1,1, 1,2, 2,1, 3,1, 3,3, 0,3},
         new Point2d(13.0/8,11.0/8), 7.5);

   }

   public void test() {
      RandomGenerator.setSeed (0x1234);
      testCentroidAndArea();
   }

   public static void main (String[] args) {
      Polygon3dTest tester = new Polygon3dTest();

      tester.runtest();
   }

}
