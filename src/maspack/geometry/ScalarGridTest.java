package maspack.geometry;

import java.io.FileWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;

import maspack.matrix.Matrix;
import maspack.matrix.Matrix3d;
import maspack.matrix.MatrixNd;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector;
import maspack.matrix.Vector2d;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector3i;
import maspack.matrix.VectorNd;
import maspack.matrix.VectorObject;
import maspack.util.IndentingPrintWriter;
import maspack.util.NumberFormat;
import maspack.util.RandomGenerator;
import maspack.util.ReaderTokenizer;
import maspack.util.TestException;
import maspack.util.UnitTest;

public class ScalarGridTest extends InterpolatingGridTestBase {

   double OUTSIDE = ScalarGrid.OUTSIDE_GRID;

   public double getCheckLocalValue (
      ScalarGrid grid, Point3d pnt, boolean clipToGrid) {

      double[] wgts = new double[8];
      Vector3i xyzi = new Vector3i();
      if (!getCellVertexAndWeights (xyzi, wgts, grid, pnt, clipToGrid)) {
         return ScalarGrid.OUTSIDE_GRID;
      }
      int vi = xyzi.x;
      int vj = xyzi.y;
      int vk = xyzi.z;

      double res = 0;
      res += wgts[0]*grid.getVertexValue (vi  , vj  , vk  );
      res += wgts[1]*grid.getVertexValue (vi  , vj  , vk+1);
      res += wgts[2]*grid.getVertexValue (vi  , vj+1, vk  );
      res += wgts[3]*grid.getVertexValue (vi  , vj+1, vk+1);
      res += wgts[4]*grid.getVertexValue (vi+1, vj  , vk  );
      res += wgts[5]*grid.getVertexValue (vi+1, vj  , vk+1);
      res += wgts[6]*grid.getVertexValue (vi+1, vj+1, vk  );
      res += wgts[7]*grid.getVertexValue (vi+1, vj+1, vk+1);
      return res;
   }
   
   public double getCheckWorldValue (
      ScalarGrid grid, Point3d pnt, boolean clipToGrid) {

      RigidTransform3d TLW = new RigidTransform3d();
      if (grid.getLocalToWorld() != null) {
         TLW.set (grid.getLocalToWorld());
      }
      Point3d loc = new Point3d(pnt);
      loc.inverseTransform (TLW);
      return getCheckLocalValue (grid, loc, clipToGrid);
   }

   public void test (ScalarGrid grid, RigidTransform3d TLW) {

      grid.setLocalToWorld (TLW);

      // set random values for the grid
      int numv = grid.numVertices();
      for (int i=0; i<numv; i++) {
         grid.setVertexValue (i, RandomGenerator.nextDouble());
      }

      ScalarGrid copy = new ScalarGrid (grid);
      check ("copy != grid", copy.epsilonEquals (grid, 0));
      Vector3i res = grid.getResolution();
      check ("number of vertices", numv == (res.x+1)*(res.y+1)*(res.z+1));

      for (int i=0; i<numv; i++) {
         double val = grid.getVertexValue (i);
         double chk = grid.getVertexValue (
            grid.vertexToXyzIndices (new Vector3i(), i));
         check ("value at vertex "+i, val==chk);
      }

      int ntests = 100;
      for (int k=0; k<ntests; k++) {
         Point3d pnt = createTestPoint (grid);
         double val, chk;
         boolean clipToGrid = (k%2 == 0);
         if (grid.getLocalToWorld() == null) {
            val = grid.getLocalValue (pnt, clipToGrid);
            chk = getCheckLocalValue (grid, pnt, clipToGrid);
         }
         else {
            val = grid.getWorldValue (pnt, clipToGrid);
            chk = getCheckWorldValue (grid, pnt, clipToGrid);
         }
         check ("value==OUTSIDE |= check==OUTSIDE",
                ((val==OUTSIDE) == (chk==OUTSIDE)));
         if (val != OUTSIDE) {
            check ("value at random point "+k, Math.abs(chk-val) <= 1e-14);
         }
      }

      StringWriter sw = new StringWriter (100000);
      IndentingPrintWriter pw = new IndentingPrintWriter (sw);

      try {
         grid.write (pw, new NumberFormat("%g"), null);
         pw.close();
         ReaderTokenizer rtok = new ReaderTokenizer (
            new StringReader (sw.toString()));
         copy = new ScalarGrid ();
         copy.scan (rtok, null);
         check ("write/scan copy != grid", copy.epsilonEquals (grid, 0));
      }
      catch (Exception e) {
         e.printStackTrace();
         throw new TestException (
            "I/O error during write/scan: "+e.getMessage());
      }
   }

   private void checkValue (
      ScalarGrid grid, double x, double y, double z, double chk) {

      Point3d pos = new Point3d(x, y, z);
      double val = grid.getLocalValue (pos);
      if (Math.abs(val-chk) > 1e-10) {
         throw new TestException (
            "value at "+pos+" is "+val+", expecting "+chk);
      }
      // expand position to push it outside
      pos.scale (1.1);
      val = grid.getLocalValue (pos);
      if (val != OUTSIDE) {
         throw new TestException (
            "value at "+pos+" is "+val+", expecting OUTSIDE");
      }
      val = grid.getLocalValue (pos, /*clipped=*/true);
      if (Math.abs(val-chk) > 1e-10) {
         throw new TestException (
            "clipped value at "+pos+" is "+val+", expecting "+chk);
      }
   }

   /**
    * Test points outside the grid directly.
    */
   public void hardTest() {
      double w = 2.0;
      ScalarGrid grid =
         new ScalarGrid (
            new Vector3d (w, w, w),
            new Vector3i (2, 2, 2), null);
      for (int k=0; k<grid.numVertices(); k++) {
         Vector3d pos = new Vector3d();
         grid.getLocalVertexCoords (pos, k);
         grid.setVertexValue (k, pos.norm());
      }
      double SQR3 = Math.sqrt (3);
      double SQR2 = Math.sqrt (2);

      // corners

      checkValue (grid, -1.0, -1.0, -1.0, SQR3);
      checkValue (grid, -1.0, -1.0,  1.0, SQR3);
      checkValue (grid, -1.0,  1.0, -1.0, SQR3);
      checkValue (grid, -1.0,  1.0,  1.0, SQR3);
      checkValue (grid,  1.0, -1.0, -1.0, SQR3);
      checkValue (grid,  1.0, -1.0,  1.0, SQR3);
      checkValue (grid,  1.0,  1.0, -1.0, SQR3);
      checkValue (grid,  1.0,  1.0,  1.0, SQR3);

      // edge midpoints

      checkValue (grid,  0.0, -1.0, -1.0, SQR2);
      checkValue (grid,  0.0, -1.0,  1.0, SQR2);
      checkValue (grid,  0.0,  1.0, -1.0, SQR2);
      checkValue (grid,  0.0,  1.0,  1.0, SQR2);
      checkValue (grid, -1.0,  0.0, -1.0, SQR2);
      checkValue (grid, -1.0,  0.0,  1.0, SQR2);
      checkValue (grid,  1.0,  0.0, -1.0, SQR2);
      checkValue (grid,  1.0,  0.0,  1.0, SQR2);
      checkValue (grid, -1.0, -1.0,  0.0, SQR2);
      checkValue (grid, -1.0,  1.0,  0.0, SQR2);
      checkValue (grid,  1.0, -1.0,  0.0, SQR2);
      checkValue (grid,  1.0,  1.0,  0.0, SQR2);

      // face midpoints

      checkValue (grid,  1.0,  0.0,  0.0, 1.0);
      checkValue (grid, -1.0,  0.0,  0.0, 1.0);
      checkValue (grid,  0.0,  1.0,  0.0, 1.0);
      checkValue (grid,  0.0, -1.0,  0.0, 1.0);
      checkValue (grid,  0.0,  0.0,  1.0, 1.0);
      checkValue (grid,  0.0,  0.0, -1.0, 1.0);
   }

   public void test() {
      for (Vector3d widths : myWidths) {
         for (Vector3i res : myResolutions) {
            for (RigidTransform3d TCL : myTCLs) {
               for (RigidTransform3d TLW : myTLWs) {
                  test (new ScalarGrid (widths, res, TCL), TLW);
               }
            }
         }
      }
      hardTest();
   }

   public static void main (String[] args) {

      RandomGenerator.setSeed (0x1234);

      ScalarGridTest tester = new ScalarGridTest();
      tester.runtest();
   }

}
