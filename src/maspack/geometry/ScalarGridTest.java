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

   public double getCheckLocalValue (ScalarGrid grid, Point3d pnt) {

      double[] wgts = new double[8];
      Vector3i xyzi = new Vector3i();
      if (!getCellVertexAndWeights (xyzi, wgts, grid, pnt)) {
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
   
   public double getCheckWorldValue (ScalarGrid grid, Point3d pnt) {

      RigidTransform3d TLW = new RigidTransform3d();
      if (grid.getLocalToWorld() != null) {
         TLW.set (grid.getLocalToWorld());
      }
      Point3d loc = new Point3d(pnt);
      loc.inverseTransform (TLW);
      return getCheckLocalValue (grid, loc);
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
      double OUTSIDE = ScalarGrid.OUTSIDE_GRID;
      for (int k=0; k<ntests; k++) {
         Point3d pnt = createTestPoint (grid);
         double val, chk;
         if (grid.getLocalToWorld() == null) {
            val = grid.getLocalValue (pnt);
            chk = getCheckLocalValue (grid, pnt);
         }
         else {
            val = grid.getWorldValue (pnt);
            chk = getCheckWorldValue (grid, pnt);
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
   }

   public static void main (String[] args) {

      RandomGenerator.setSeed (0x1234);

      ScalarGridTest tester = new ScalarGridTest();
      tester.runtest();
   }

}
