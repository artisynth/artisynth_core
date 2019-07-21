package maspack.geometry;

import java.io.*;

import maspack.matrix.*;
import maspack.util.*;
import maspack.geometry.DistanceGrid.*;

public class DistanceGridTest extends UnitTest {

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

   protected DistanceGridTest() {
      createQuadraticInterpolationMatrices();
      
   }

   private Matrix3d computeNumericNormalDerivative (
      DistanceGrid grid, Point3d q) {

      Matrix3d Dchk = new Matrix3d();
      Point3d qh = new Point3d(q);

      Vector3d nrm = new Vector3d();
      
      double d0 = grid.getLocalDistanceAndNormal (nrm, q);
      Vector3d f0 = new Vector3d();
      f0.scale (d0, nrm);
      double h = 1e-8;

      for (int i=0; i<3; i++) {
         qh.set (q);
         qh.set (i, q.get(i)+h);
         double d = grid.getLocalDistanceAndNormal (nrm, qh);
         Vector3d df = new Vector3d();
         df.scale (d, nrm);
         df.sub (f0);
         df.scale (1/h);
         Dchk.setColumn (i, df);
      }
      return Dchk;
   }
   
   private Matrix3d computeNumericQuadGradDerivative (
      DistanceGrid grid, Point3d q) {

      Matrix3d Dchk = new Matrix3d();
      Point3d qh = new Point3d(q);

      Vector3d grad0 = new Vector3d();
      Vector3d grad = new Vector3d();
      
      grid.getQuadDistanceAndGradient (grad0, null, q);
      double h = 1e-8;

      for (int i=0; i<3; i++) {
         qh.set (q);
         qh.set (i, q.get(i)+h);
         grid.getQuadDistanceAndGradient (grad, null, qh);
         Vector3d dgrad = new Vector3d();
         dgrad.sub (grad, grad0);
         dgrad.scale (1/h);
         Dchk.setColumn (i, dgrad);
      }
      return Dchk;
   }
   
   private RigidTransform3d getGridToLocalTransform (
      DistanceGrid grid, Vector3d widths) {
      RigidTransform3d TGL = new RigidTransform3d();
      grid.getOrientation (TGL.R);
      grid.getCenter (TGL.p);
      // add an offset of -widths/2, transformed to local coords, to TGL.p
      Vector3d offset = new Vector3d();
      offset.scale (-0.5, widths);
      offset.transform (TGL);
      TGL.p.add (offset);
      return TGL;
   }

   private double distanceCheck (DistanceGrid grid, Point3d q) {
      Vector3d cellWidths = grid.getCellWidths();
      Vector3d widths = new Vector3d();
      grid.getWidths(widths);
      
      RigidTransform3d TGL = getGridToLocalTransform (grid, widths);
      
      Point3d g = new Point3d();
      g.inverseTransform (TGL, q); 
      Vector3i res = grid.getResolution();
      if (g.x < 0 || g.x > widths.x ||
          g.y < 0 || g.y > widths.y ||
          g.z < 0 || g.z > widths.z) {
         return DistanceGrid.OUTSIDE_GRID;
      }
      double x = g.x/cellWidths.x;
      double y = g.y/cellWidths.y;
      double z = g.z/cellWidths.z;
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
      double[] phi = grid.getVertexDistances();
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
      DistanceGrid grid, MatrixNd C,
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

   private double quadDistanceCheck (DistanceGrid grid, Point3d q) {
      // assume that the resolutions are all even
      Vector3d cellWidths = grid.getCellWidths();
      Vector3d widths = new Vector3d();
      grid.getWidths(widths);
      
      RigidTransform3d TGL = getGridToLocalTransform (grid, widths);
      
      Point3d g = new Point3d();
      g.inverseTransform (TGL, q); 
      if (g.x < 0 || g.x > widths.x ||
          g.y < 0 || g.y > widths.y ||
          g.z < 0 || g.z > widths.z) {
         return DistanceGrid.OUTSIDE_GRID;
      }
      Vector3i res = grid.getResolution();
      double x = g.x/cellWidths.x;
      double y = g.y/cellWidths.y;
      double z = g.z/cellWidths.z;
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
      DistanceGrid grid, Point3d q) {

      Vector3d gchk = new Vector3d();
      Point3d qh = new Point3d(q);
      
      double d0 = grid.getLocalDistance (q);
      double h = 1e-8;

      for (int i=0; i<3; i++) {
         qh.set (q);
         qh.set (i, q.get(i)+h);
         double d = grid.getLocalDistance (qh);
         gchk.set (i, (d-d0)/h);
      }
      return gchk;
   }

   private Vector3d computeNumericQuadGradient (
      DistanceGrid grid, Point3d q) {

      Vector3d gchk = new Vector3d();
      Point3d qh = new Point3d(q);
      
      double d0 = grid.getQuadDistanceAndGradient (null, null, q);
      double h = 1e-8;

      for (int i=0; i<3; i++) {
         qh.set (q);
         qh.set (i, q.get(i)+h);
         double d = grid.getQuadDistanceAndGradient (null, null, qh);
         gchk.set (i, (d-d0)/h);
      }
      return gchk;
   }

   private void testScanWrite (DistanceGrid grid) {
      StringWriter sw = new StringWriter();
      IndentingPrintWriter pw = new IndentingPrintWriter (sw);
      DistanceGrid newGrid = null;
      try {
         grid.write (pw, new NumberFormat ("%g"), null);
         newGrid = new DistanceGrid ();
         newGrid.scan (
            new ReaderTokenizer (new StringReader (sw.toString())), null);
      }
      catch (Exception e) {
         e.printStackTrace(); 
         throw new TestException ("exception during write/scan test");
      }
      if (!grid.epsilonEquals (newGrid, 0)) {
         throw new TestException ("write/scan test failed");
      }
   }

   public void timing() {

      double a = 3.0;
      double b = 2.0;
      double c = 1.0;

      PolygonalMesh ellipsoid = MeshFactory.createEllipsoid (a, b, c, 48);

      Vector3i resolution = new Vector3i (20, 20, 20);
      RigidTransform3d TEW = new RigidTransform3d();
      TEW.setRandom();

      DistanceGrid grid =
         new DistanceGrid (ellipsoid.getFaces(), 0.1, resolution,/*signed=*/true);
      grid.setLocalToWorld (TEW);

      // create a bunch of random points just inside the ellispoid, in
      // both local and world coordinates
      int npnts = 10000;
      int cnt = 3000000;
      Point3d[] localPnts = new Point3d[npnts];
      Point3d[] worldPnts = new Point3d[npnts];
      for (int i=0; i<npnts; i++) {
         Point3d plocal = new Point3d();
         double u = RandomGenerator.nextDouble (-Math.PI, Math.PI);
         double v = RandomGenerator.nextDouble (0, Math.PI);
         plocal.x = 0.9*a*Math.cos(u)*Math.sin(v);
         plocal.y = 0.9*b*Math.sin(u)*Math.sin(v);
         plocal.z = 0.9*c*Math.cos(u);
         localPnts[i] = plocal;
         Point3d pworld = new Point3d();
         pworld.transform (TEW, plocal);
         worldPnts[i] = pworld;
      }

      Vector3d grad = new Vector3d();
      Matrix3d dgrad = new Matrix3d();

      // warm up the simulation
      for (int i=0; i<cnt; i++) {
         int k = (i%npnts);
         grid.getWorldQuadDistanceAndGradient (grad, dgrad, worldPnts[k]);
         grid.getWorldQuadDistance (worldPnts[k]);
         grid.getQuadDistanceAndGradient (grad, dgrad, localPnts[k]);
         grid.getQuadDistance (localPnts[k]);
      }
      FunctionTimer timer = new FunctionTimer();

      timer.start();
      for (int i=0; i<cnt; i++) {
         int k = (i%npnts);
         Vector3d coords = new Vector3d();
         Vector3i vidx = new Vector3i();
         // grid.getQuadCellCoords (vidx, coords, localPnts[k]);

         vidx.set (2, 2, 2);
         coords.set (0.5, 0.5, 0.5);

         double dx = coords.x;
         double dy = coords.y;
         double dz = coords.z;
         
         int ncx = (grid.myNx-1)/2;
         int ncy = (grid.myNy-1)/2;
         int ncz = (grid.myNz-1)/2;
         TetID tetId = TetID.findSubTet (dx, dy, dz);
         int cidx =
            6*(vidx.x/2 + ncx*vidx.y/2 + ncx*ncy*vidx.z/2) + tetId.intValue();
         // double[] aa = grid.myQuadCoefs[cidx];
         //grid.computeQuadDistance (aa, dx, dy, dz);
      }
      timer.stop();   
      System.out.println ("quad cell test2:            " + timer.result(cnt));

      timer.start();
      for (int i=0; i<cnt; i++) {
         int k = (i%npnts);
         grid.getQuadDistance (localPnts[k]);
      }
      timer.stop();  
      System.out.println ("local quad dist:            " + timer.result(cnt));

      timer.start();
      for (int i=0; i<cnt; i++) {
         int k = (i%npnts);
         grid.getQuadDistanceAndGradient (grad, null, localPnts[k]);
      }
      timer.stop();
      System.out.println ("local quad dist/grad:       " + timer.result(cnt));

      timer.start();
      for (int i=0; i<cnt; i++) {
         int k = (i%npnts);
         grid.getQuadDistanceAndGradient (grad, dgrad, localPnts[k]);
      }
      timer.stop();
      System.out.println ("local quad dist/grad/dgrad: " + timer.result(cnt));

      System.out.println ("");
      timer.start();
      for (int i=0; i<cnt; i++) {
         int k = (i%npnts);
         grid.getWorldQuadDistance (worldPnts[k]);
      }
      timer.stop();
      System.out.println ("world quad dist:            " + timer.result(cnt));

      timer.start();
      for (int i=0; i<cnt; i++) {
         int k = (i%npnts);
         grid.getWorldQuadDistanceAndGradient (grad, null, worldPnts[k]);
      }
      timer.stop();
      System.out.println ("world quad dist/grad:       " + timer.result(cnt));

      timer.start();
      for (int i=0; i<cnt; i++) {
         int k = (i%npnts);
         grid.getWorldQuadDistanceAndGradient (grad, dgrad, worldPnts[k]);
      }
      timer.stop();
      System.out.println ("world quad dist/grad/dgrad: " + timer.result(cnt));

   }

   public void test() {
      double EPS = 1e-14;

      PolygonalMesh torus = MeshFactory.createTorus (1.0, 0.5, 24, 24);
      PolygonalMesh torusT = torus.copy();

      RigidTransform3d TGL = new RigidTransform3d();
      RigidTransform3d TLW = new RigidTransform3d();
      RigidTransform3d TGW = new RigidTransform3d();
      TGL.setRandom();
      TLW.setRandom();
      TGW.mul (TLW, TGL);

      torusT.transform(TGL);

      Vector3i resolution = new Vector3i (20, 20, 10);

      DistanceGrid grid =
         new DistanceGrid (torus.getFaces(), 0.1, resolution, /*signed=*/true);
      grid.setLocalToWorld (TLW);

      Vector3d widths = new Vector3d();
      Vector3d center = new Vector3d();
      grid.getWidths (widths);
      grid.getCenter (center);
      
      // For now, just set the distances, since precision issues can result in
      // differing distances computed during the sweep process
      DistanceGrid gridT = new DistanceGrid (widths, resolution, TGL);
      gridT.setVertexDistances (grid.getVertexDistances(), /*signed=*/true);
      gridT.setLocalToWorld (TLW);

      int numtests = 1000;
      for (int i=0; i<numtests; i++) {
         Point3d q = new Point3d (
            RandomGenerator.nextDouble (0, widths.x),
            RandomGenerator.nextDouble (0, widths.y),
            RandomGenerator.nextDouble (0, widths.z));
         q.add (center);
         q.scaledAdd (-0.5, widths);

         Point3d qw = new Point3d(q);
         qw.transform (TLW);

         Point3d qt = new Point3d ();
         qt.transform (TGL, q);

         Point3d qwt = new Point3d (qt);
         qwt.transform (TLW);

         Vector3d nrm = new Vector3d();
         Matrix3d Dnrm = new Matrix3d();

         Vector3d nrmW = new Vector3d();
         Matrix3d DnrmW = new Matrix3d();

         double d = grid.getLocalDistanceAndNormal (nrm, Dnrm, q);
         double dw = grid.getWorldDistanceAndNormal (nrmW, qw);
         double dchk = distanceCheck (grid, q);
         checkEquals ("localDistanceAndNormal, d", d, dchk, EPS);
         checkEquals ("worldDistanceAndNormal, d", dw, dchk, EPS);
         if (d != DistanceGrid.OUTSIDE_GRID) {
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
            Vector3d NchkW = new Vector3d (nrm);
            NchkW.transform (TLW);
            if (!NchkW.epsilonEquals (nrmW, 1e-12)) {
               System.out.println ("nrmW=\n" + nrmW.toString("%12.8f"));
               System.out.println ("chkW=\n" + NchkW.toString("%12.8f"));
               throw new TestException (
                  "error computing world normal");
            }
         }

         Vector3d nrmT = new Vector3d();
         double dt = gridT.getLocalDistanceAndNormal (nrmT, qt);
         
         if ((dt == DistanceGrid.OUTSIDE_GRID) !=
             (d == DistanceGrid.OUTSIDE_GRID)) {
            System.out.println (
               "grid outside=" + (d == DistanceGrid.OUTSIDE_GRID));
            System.out.println (
               "transformed grid outside=" + (dt == DistanceGrid.OUTSIDE_GRID));
            throw new TestException (
               "Outside differs for grid and transformed grid");
         }
         if (dt != DistanceGrid.OUTSIDE_GRID) {
            if (Math.abs (d-dt) > EPS) {
               System.out.println ("grid d=" + d);
               System.out.println ("transformed grid d=" + dt);
               throw new TestException (
                  "Distance differs for grid and transformed grid");
            }
         }

         Vector3d grad = new Vector3d();
         Vector3d gradW = new Vector3d();
         Vector3d gradT = new Vector3d();
         Vector3d gradWT = new Vector3d();

         d = grid.getLocalDistanceAndGradient (grad, q);
         checkEquals ("localDistanceAndGradient, d", d, dchk, EPS);
         if (d != DistanceGrid.OUTSIDE_GRID) {
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

         Matrix3d Dgrad = new Matrix3d();
         Matrix3d DgradW = new Matrix3d();
         Matrix3d DgradT = new Matrix3d();
         Matrix3d DgradWT = new Matrix3d();

         d = grid.getQuadDistance (q);
         dchk = quadDistanceCheck (grid, q);
         dw = grid.getWorldQuadDistance (qw);
         checkEquals ("getQuadDistance, d", d, dchk, EPS);
         checkEquals ("getWorldQuadDistance, d", dw, dchk, EPS);

         d = gridT.getQuadDistance (qt);
         dw = gridT.getWorldQuadDistance (qwt);
         checkEquals ("getQuadDistanceT, d", d, dchk, EPS);
         checkEquals ("getWorldQuadDistanceT, d", dw, dchk, EPS);
 
         d = grid.getQuadDistanceAndGradient (grad, Dgrad, q);
         dw = grid.getWorldQuadDistanceAndGradient (gradW, DgradW, qw);
         checkEquals ("getQuadDistanceAndGradient, d", d, dchk, EPS);
         checkEquals ("getWorldQuadDistanceAndGradient, d", dw, dchk, EPS);

         d = gridT.getQuadDistanceAndGradient (gradT, DgradT, qt);
         dw = gridT.getWorldQuadDistanceAndGradient (gradWT, DgradWT, qwt);
         checkEquals ("getQuadDistanceAndGradientT, d", d, dchk, EPS);
         checkEquals ("getWorldQuadDistanceAndGradientT, d", dw, dchk, EPS);

         if (d != DistanceGrid.OUTSIDE_GRID) {
            // check grad
            Vector3d gchk = computeNumericQuadGradient (grid, q);
            checkEquals (
               "grad in getQuadDistanceAndGradient",
               grad, gchk, grad.norm()*5e-7);

            gchk.transform (TLW.R, grad);
            checkEquals (
               "grad in getWorldQuadDistanceAndGradient",
               gradW, gchk, grad.norm()*1e-12);

            gchk.transform (TGL.R, grad);
            checkEquals (
               "grad in getQuadDistanceAndGradient(T)",
               gradT, gchk, grad.norm()*1e-12);

            gchk.transform (TGW.R, grad);
            checkEquals (
               "grad in getWorldQuadDistanceAndGradient(T)",
               gradWT, gchk, grad.norm()*1e-12);


            // check Dgrad
            double DgradNorm = Dgrad.frobeniusNorm();
            Matrix3d Dgchk = computeNumericQuadGradDerivative (grid, q);
            checkEquals (
               "Dgrad in getQuadDistanceAndGradient",
               Dgrad, Dgchk, DgradNorm*5e-7);

            Dgchk.transform (TLW.R, Dgrad);
            checkEquals (
               "Dgrad in getWorldQuadDistanceAndGradient",
               DgradW, Dgchk, DgradNorm*1e-12);

            Dgchk.transform (TGL.R, Dgrad);
            checkEquals (
               "Dgrad in getQuadDistanceAndGradient(T)",
               DgradT, Dgchk, DgradNorm*1e-12);

            Dgchk.transform (TGW.R, Dgrad);
            checkEquals (
               "Dgrad in getWorldQuadDistanceAndGradient(T)",
               DgradWT, Dgchk, DgradNorm*1e-12);

         }
      }

      testScanWrite (gridT);

   }

   public static void main (String[] args) {

      DistanceGridTest tester = new DistanceGridTest();
      boolean doTiming = false;
      boolean doTesting = true;

      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-timing")) {
            doTiming = true;
            doTesting = false;
         }
         else {
            System.out.println (
               "Usage: java "+tester.getClass().getName()+" [-timing]");
            System.exit(1);
         }
      }      

      RandomGenerator.setSeed (0x1234);
      if (doTesting) {
         tester.runtest();
      }
      if (doTiming) {
         tester.timing();
      }
   }
}
