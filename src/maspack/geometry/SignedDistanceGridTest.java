package maspack.geometry;

import maspack.matrix.*;
import maspack.util.*;

public class SignedDistanceGridTest extends UnitTest {

   MatrixNd myC0156;
   MatrixNd myC0126;
   MatrixNd myC0326;
   MatrixNd myC0456;
   MatrixNd myC0476;
   MatrixNd myC0376;

   private void createQuadraticInterpolationMatrices() {
      
      Vector3i v0 = new Vector3i (0, 0, 0);
      Vector3i v1 = new Vector3i (2, 0, 0);
      Vector3i v2 = new Vector3i (2, 0, 2);
      Vector3i v3 = new Vector3i (0, 0, 2);

      Vector3i v4 = new Vector3i (0, 2, 0);
      Vector3i v5 = new Vector3i (2, 2, 0);
      Vector3i v6 = new Vector3i (2, 2, 2);
      Vector3i v7 = new Vector3i (0, 2, 2);

      myC0156 = createQuadraticInterpolationMatrix (v0, v1, v5, v6);
      myC0126 = createQuadraticInterpolationMatrix (v0, v1, v2, v6);
      myC0326 = createQuadraticInterpolationMatrix (v0, v3, v2, v6);
      myC0376 = createQuadraticInterpolationMatrix (v0, v3, v7, v6);
      myC0476 = createQuadraticInterpolationMatrix (v0, v4, v7, v6);
      myC0456 = createQuadraticInterpolationMatrix (v0, v4, v5, v6);
   }

   private MatrixNd createQuadraticInterpolationMatrix (
      Vector3i v0, Vector3i v1, Vector3i v2, Vector3i v3) {

      Vector3i e01 = getEdge (v0, v1);
      Vector3i e12 = getEdge (v1, v2);
      Vector3i e23 = getEdge (v2, v3);
      Vector3i e02 = getEdge (v0, v2);
      Vector3i e13 = getEdge (v1, v3);
      Vector3i e03 = getEdge (v0, v3);

      MatrixNd C = new MatrixNd (10, 10);
      Vector3i[] nodes = new Vector3i[] {
         v0, v1, v2, v3, e01, e12, e23, e02, e13, e03 };
      for (int i=0; i<10; i++) {
         Vector3i n = nodes[i];
         double x = n.x/2.0;
         double y = n.y/2.0;
         double z = n.z/2.0;
         C.set (i, 0, x*x);
         C.set (i, 1, y*y);
         C.set (i, 2, z*z);
         C.set (i, 3, x*y);
         C.set (i, 4, x*z);
         C.set (i, 5, y*z);
         C.set (i, 6, x);
         C.set (i, 7, y);
         C.set (i, 8, z);
         C.set (i, 9, 1);
      }
      C.invert();
      return C;
   }

   protected SignedDistanceGridTest() {
      createQuadraticInterpolationMatrices();
      
   }

   private Matrix3d computeNumericNormalDerivative (
      SignedDistanceGrid grid, Point3d q) {

      Matrix3d Dchk = new Matrix3d();
      Point3d qh = new Point3d(q);

      Vector3d nrm = new Vector3d();
      
      double d0 = grid.getLocalDistanceAndNormal (nrm, null, q);
      Vector3d f0 = new Vector3d();
      f0.scale (d0, nrm);
      double h = 1e-8;

      for (int i=0; i<3; i++) {
         qh.set (q);
         qh.set (i, q.get(i)+h);
         double d = grid.getLocalDistanceAndNormal (nrm, null, qh);
         Vector3d df = new Vector3d();
         df.scale (d, nrm);
         df.sub (f0);
         df.scale (1/h);
         Dchk.setColumn (i, df);
      }
      return Dchk;
   }

   private double distanceCheck (SignedDistanceGrid grid, Point3d q) {
      Vector3d cellWidths = grid.getCellWidths();
      Vector3d minc = grid.getMinCoords();
      Vector3d maxc = grid.getMaxCoords();
      Vector3i res = grid.getResolution();
      if (q.x < minc.x || q.x > maxc.x ||
          q.y < minc.y || q.y > maxc.y ||
          q.z < minc.z || q.z > maxc.z) {
         return DistanceGrid.OUTSIDE;
      }
      double x = (q.x-minc.x)/cellWidths.x;
      double y = (q.y-minc.y)/cellWidths.y;
      double z = (q.z-minc.z)/cellWidths.z;
      int xi = (int)x;
      int yj = (int)y;
      int zk = (int)z;
      x -= xi;
      y -= yj;
      z -= zk;
      if (xi < 0) {
         xi = 0;
      }
      else if (xi == res.x) {
         xi--;
         x += 1;
      }
      if (yj < 0) {
         yj = 0;
      }
      else if (yj == res.y) {
         yj--;
         y += 1;
      }
      if (zk < 0) {
         zk = 0;
      }
      else if (zk == res.z) {
         zk--;
         z += 1;
      }
      int numVX = res.x+1;
      int numVY = res.y+1;
      double[] phi = grid.getDistances();
      int base = grid.xyzIndicesToVertex(xi, yj, zk);
      double d000 = phi[base];
      double d100 = phi[base + 1];
      double d110 = phi[base + 1 + numVX];
      double d010 = phi[base + numVX];
      double d001 = phi[base + numVX*numVY];
      double d101 = phi[base + 1 + numVX*numVY];
      double d111 = phi[base + 1 + numVX + numVX*numVY];
      double d011 = phi[base + numVX + numVX*numVY];

      double d =
         (1-z)*((1-y)*(d000*(1-x)+d100*x) + y*(d010*(1-x)+d110*x)) +
         z*((1-y)*(d001*(1-x)+d101*x) + y*(d011*(1-x)+d111*x));
      return d;
   }

   private Vector3i getEdge (Vector3i v0, Vector3i v1) {
      Vector3i edge = new Vector3i();
      edge.x = (v0.x+v1.x)/2;
      edge.y = (v0.y+v1.y)/2;
      edge.z = (v0.z+v1.z)/2;
      return edge;
   }

   private double quadInterpolateTet (
      SignedDistanceGrid grid, MatrixNd C,
      Vector3i v0, Vector3i v1, Vector3i v2, Vector3i v3, 
      double x, double y, double z) {

      Vector3i e01 = getEdge (v0, v1);
      Vector3i e12 = getEdge (v1, v2);
      Vector3i e23 = getEdge (v2, v3);
      Vector3i e02 = getEdge (v0, v2);
      Vector3i e13 = getEdge (v1, v3);
      Vector3i e03 = getEdge (v0, v3);

      VectorNd d = new VectorNd (10);
      VectorNd c = new VectorNd (10);
      Vector3i[] nodes = new Vector3i[] {
         v0, v1, v2, v3, e01, e12, e23, e02, e13, e03 };
      for (int i=0; i<10; i++) {
         d.set (i, grid.getVertexDistance (nodes[i]));
      }
      C.mul (c, d);
      // System.out.println (
      //    "check d=    " + new VectorNd(d).toString ("%10.6f"));
      // System.out.println (
      //    "check coefs=" + new VectorNd(c).toString ("%10.6f"));
      return (c.get(0)*x*x + c.get(1)*y*y + c.get(2)*z*z + 
              c.get(3)*x*y + c.get(4)*x*z + c.get(5)*y*z + 
              c.get(6)*x   + c.get(7)*y   + c.get(8)*z + c.get(9));         
   }

   private double quadDistanceCheck (SignedDistanceGrid grid, Point3d q) {
      // assume that the resolutions are all even
      Vector3d cellWidths = grid.getCellWidths();
      Vector3d minc = grid.getMinCoords();
      Vector3d maxc = grid.getMaxCoords();
      Vector3i res = grid.getResolution();
      if (q.x < minc.x || q.x > maxc.x ||
          q.y < minc.y || q.y > maxc.y ||
          q.z < minc.z || q.z > maxc.z) {
         return DistanceGrid.OUTSIDE;
      }
      double x = (q.x-minc.x)/cellWidths.x;
      double y = (q.y-minc.y)/cellWidths.y;
      double z = (q.z-minc.z)/cellWidths.z;
      int xi = (int)x;
      int yj = (int)y;
      int zk = (int)z;
      if (xi < 0) {
         xi = 0;
      }
      else if (xi == res.x) {
         xi -= 2;
      }
      else if ((xi%2) != 0) {
         xi -= 1;
      }
      x = (x-xi)/2;
      if (yj < 0) {
         yj = 0;
      }
      else if (yj == res.y) {
         yj -= 2;
      }
      else if ((yj%2) != 0) {
         yj -= 1;
      }
      y = (y-yj)/2;
      if (zk < 0) {
         zk = 0;
      }
      else if (zk == res.z) {
         zk -= 2;
      }
      else if ((zk%2) != 0) {
         zk -= 1;
      }
      z = (z-zk)/2;

      //System.out.println ("check vtx=("+xi+" "+yj+" "+zk+")");
      //System.out.println ("check xyz=("+x+" "+y+" "+z+")");

      Vector3i v0 = new Vector3i (xi  , yj  , zk);
      Vector3i v1 = new Vector3i (xi+2, yj  , zk);
      Vector3i v2 = new Vector3i (xi+2, yj  , zk+2);
      Vector3i v3 = new Vector3i (xi  , yj  , zk+2);

      Vector3i v4 = new Vector3i (xi  , yj+2, zk);
      Vector3i v5 = new Vector3i (xi+2, yj+2, zk);
      Vector3i v6 = new Vector3i (xi+2, yj+2, zk+2);
      Vector3i v7 = new Vector3i (xi  , yj+2, zk+2);

      if (x >= y) {
         if (y >= z) {
            return quadInterpolateTet (grid, myC0156, v0, v1, v5, v6, x, y, z);
         }
         else {
            if (x >= z) {
               return quadInterpolateTet (grid, myC0126, v0, v1, v2, v6, x, y, z);
            }
            else {
               return quadInterpolateTet (grid, myC0326, v0, v3, v2, v6, x, y, z);
            }
         }
      }
      else {
         if (y >= z) {
            if (x >= z) {
               return quadInterpolateTet (grid, myC0456, v0, v4, v5, v6, x, y, z);
            }
            else {
               return quadInterpolateTet (grid, myC0476, v0, v4, v7, v6, x, y, z);
            }
         }
         else {
            return quadInterpolateTet (grid, myC0376, v0, v3, v7, v6, x, y, z);
         }
      }
   }

   private Vector3d computeNumericGradient (
      SignedDistanceGrid grid, Point3d q) {

      Vector3d gchk = new Vector3d();
      Point3d qh = new Point3d(q);
      
      double d0 = grid.getLocalDistanceAndNormal (null, null, q);
      double h = 1e-8;

      for (int i=0; i<3; i++) {
         qh.set (q);
         qh.set (i, q.get(i)+h);
         double d = grid.getLocalDistanceAndNormal (null, null, qh);
         gchk.set (i, (d-d0)/h);
      }
      return gchk;
   }

   private Vector3d computeNumericQuadGradient (
      SignedDistanceGrid grid, Point3d q) {

      Vector3d gchk = new Vector3d();
      Point3d qh = new Point3d(q);
      
      double d0 = grid.getQuadraticDistanceAndGradient (null, q);
      double h = 1e-8;

      for (int i=0; i<3; i++) {
         qh.set (q);
         qh.set (i, q.get(i)+h);
         double d = grid.getQuadraticDistanceAndGradient (null, qh);
         gchk.set (i, (d-d0)/h);
      }
      return gchk;
   }

   public void test() {
      PolygonalMesh torus = MeshFactory.createTorus (1.0, 0.5, 24, 24);

      SignedDistanceGrid grid =
         new SignedDistanceGrid (torus, 0.1, new Vector3i (20, 20, 10));

      Vector3d minc = grid.getMinCoords();
      Vector3d maxc = grid.getMaxCoords();
      Vector3d widths = new Vector3d();
      Vector3d center = new Vector3d();
      widths.sub (maxc, minc);
      center.add (maxc, minc);
      center.scale (0.5);
      
      double EPS = 1e-14;

      int numtests = 1000;
      for (int i=0; i<numtests; i++) {
         Point3d q = new Point3d (
            RandomGenerator.nextDouble (0, widths.x),
            RandomGenerator.nextDouble (0, widths.y),
            RandomGenerator.nextDouble (0, widths.z));
         q.add (center);
         q.scaledAdd (-0.5, widths);
         
         Vector3d nrm = new Vector3d();
         Matrix3d Dnrm = new Matrix3d();

         double d = grid.getLocalDistanceAndNormal (nrm, Dnrm, q);
         double dchk = distanceCheck (grid, q);
         checkEquals ("localDistanceAndNormal, d=", d, dchk, EPS);
         if (d != SignedDistanceGrid.OUTSIDE) {
            // check Dnrm
            Matrix3d Dchk = computeNumericNormalDerivative (grid, q);
            Matrix3d Err = new Matrix3d();
            Err.sub (Dchk, Dnrm);
            double err = Err.frobeniusNorm()/Dnrm.frobeniusNorm();
            if (err > 1e-7) {
               System.out.println ("Dchk=\n" + Dchk.toString("%12.8f"));
               System.out.println ("Dnrm=\n" + Dnrm.toString("%12.8f"));
               System.out.println ("Err=\n" + Err.toString("%12.8f"));
               throw new TestException (
                  "Dnrm gradient error computation: err=" + err);
            }
         }

         Vector3d grad = new Vector3d();

         d = grid.getLocalDistanceAndGradient (grad, q);
         checkEquals ("localDistanceAndGradient, d=", d, dchk, EPS);
         if (d != SignedDistanceGrid.OUTSIDE) {
            // check grad
            Vector3d gchk = computeNumericGradient (grid, q);
            Vector3d gerr = new Vector3d();
            gerr.sub (gchk, grad);
            double err = gerr.norm()/gchk.norm();
            if (err > 1e-7) {
               System.out.println ("gchk=\n" + gchk.toString("%12.8f"));
               System.out.println ("grad=\n" + grad.toString("%12.8f"));
               throw new TestException (
                  "Distance gradient error computation: err=" + err);
            }
         }

         d = grid.getQuadraticDistanceAndGradient (grad, q);
         dchk = quadDistanceCheck (grid, q);
         checkEquals ("quadraticDistanceAndGradient, d=", d, dchk, EPS);
         if (d != SignedDistanceGrid.OUTSIDE) {
            // check grad
            Vector3d gchk = computeNumericQuadGradient (grid, q);
            Vector3d gerr = new Vector3d();
            gerr.sub (gchk, grad);
            double err = gerr.norm()/gchk.norm();
            if (err > 5e-7) {
               System.out.println ("gchk=\n" + gchk.toString("%12.8f"));
               System.out.println ("grad=\n" + grad.toString("%12.8f"));
               throw new TestException (
                  "Distance quadration gradient error computation: err=" + err);
            }
         }
      }
   }

   public static void main (String[] args) {
      SignedDistanceGridTest tester = new SignedDistanceGridTest();

      RandomGenerator.setSeed (0x1234);
      tester.runtest();
   }
}
