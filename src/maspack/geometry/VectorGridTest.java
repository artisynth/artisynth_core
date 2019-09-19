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

public class VectorGridTest extends InterpolatingGridTestBase {

   public <T extends VectorObject<T>> T getCheckLocalValue (
      VectorGrid<T> grid, Point3d pnt) {

      double[] wgts = new double[8];
      Vector3i xyzi = new Vector3i();
      if (!getCellVertexAndWeights (xyzi, wgts, grid, pnt)) {
         return null;
      }
      T chk = grid.createInstance();
      int vi = xyzi.x;
      int vj = xyzi.y;
      int vk = xyzi.z;
      chk.scaledAddObj (wgts[0], grid.getVertexValue (vi  , vj  , vk  ));
      chk.scaledAddObj (wgts[1], grid.getVertexValue (vi  , vj  , vk+1));
      chk.scaledAddObj (wgts[2], grid.getVertexValue (vi  , vj+1, vk  ));
      chk.scaledAddObj (wgts[3], grid.getVertexValue (vi  , vj+1, vk+1));
      chk.scaledAddObj (wgts[4], grid.getVertexValue (vi+1, vj  , vk  ));
      chk.scaledAddObj (wgts[5], grid.getVertexValue (vi+1, vj  , vk+1));
      chk.scaledAddObj (wgts[6], grid.getVertexValue (vi+1, vj+1, vk  ));
      chk.scaledAddObj (wgts[7], grid.getVertexValue (vi+1, vj+1, vk+1));
      return chk;
   }
   
   public <T extends VectorObject<T>> T getCheckWorldValue (
      VectorGrid<T> grid, Point3d pnt) {

      RigidTransform3d TLW = new RigidTransform3d();
      if (grid.getLocalToWorld() != null) {
         TLW.set (grid.getLocalToWorld());
      }
      Point3d loc = new Point3d(pnt);
      loc.inverseTransform (TLW);
      return getCheckLocalValue (grid, loc);
   }

   public <T extends VectorObject<T>> void test (
      VectorGrid<T> grid, RigidTransform3d TLW) {

      grid.setLocalToWorld (TLW);

      // set random values for the grid
      int numv = grid.numVertices();
      for (int i=0; i<numv; i++) {
         T value = grid.createInstance();
         if (value instanceof Vector) {
            Vector vec = (Vector)value;
            VectorNd rvec = new VectorNd(vec.size());
            rvec.setRandom();
            vec.set (rvec);
         }
         else if (value instanceof Matrix) {
            Matrix mat = (Matrix)value;
            MatrixNd rmat = new MatrixNd(mat.rowSize(), mat.colSize());
            rmat.setRandom();
            mat.set (rmat);
         }
         grid.setVertexValue (i, value);
      }

      if (grid instanceof VectorNdGrid) {
         VectorNdGrid copy = new VectorNdGrid ((VectorNdGrid)grid);
         check ("copy != grid", copy.epsilonEquals ((VectorNdGrid)grid, 0));
      }
      else if (grid instanceof MatrixNdGrid) {
         MatrixNdGrid copy = new MatrixNdGrid ((MatrixNdGrid)grid);
         check ("copy != grid", copy.epsilonEquals ((MatrixNdGrid)grid, 0));
      }
      else {
         VectorGrid<T> copy = new VectorGrid<T> (grid);
         check ("copy != grid", copy.epsilonEquals (grid, 0));
      }

      Vector3i res = grid.getResolution();
      check ("number of vertices", numv == (res.x+1)*(res.y+1)*(res.z+1));

      for (int i=0; i<numv; i++) {
         T val = grid.getVertexValue (i);
         T chk = grid.getVertexValue (
            grid.vertexToXyzIndices (new Vector3i(), i));
         check ("value at vertex "+i, val.epsilonEquals (chk, 0));
      }

      int ntests = 100;
      for (int k=0; k<ntests; k++) {
         Point3d pnt = createTestPoint (grid);
         T val, chk;
         if (grid.getLocalToWorld() == null) {
            val = grid.getLocalValue (pnt);
            chk = getCheckLocalValue (grid, pnt);
         }
         else {
            val = grid.getWorldValue (pnt);
            chk = getCheckWorldValue (grid, pnt);
         }
         check ("value==null |= check==null", ((val==null) == (chk==null)));
         if (val != null) {
            check ("value at random point "+k, val.epsilonEquals (chk, 1e-14));
         }
      }

      StringWriter sw = new StringWriter (100000);
      IndentingPrintWriter pw = new IndentingPrintWriter (sw);

      try {
         grid.write (pw, new NumberFormat("%g"), null);
         pw.close();
         ReaderTokenizer rtok = new ReaderTokenizer (
            new StringReader (sw.toString()));
         if (grid instanceof VectorNdGrid) {
            VectorNdGrid copy = new VectorNdGrid ();
            copy.scan (rtok, null);
            check ("write/scan copy != grid",
                   copy.epsilonEquals ((VectorNdGrid)grid, 0));
         }
         else if (grid instanceof MatrixNdGrid) {
            MatrixNdGrid copy = new MatrixNdGrid ();
            copy.scan (rtok, null);
            check ("write/scan copy != grid",
                   copy.epsilonEquals ((MatrixNdGrid)grid, 0));
         }
         else {
            VectorGrid<T> copy = new VectorGrid<T> (grid.getParameterType());
            copy.scan (rtok, null);
            check ("write/scan copy != grid", copy.epsilonEquals (grid, 0));
         }
      }
      catch (Exception e) {
         e.printStackTrace();
         throw new TestException (
            "I/O error during write/scan: "+e.getMessage());
      }

      try {
         pw = new IndentingPrintWriter (new FileWriter ("grid.txt"));
         grid.write (pw, new NumberFormat("%g"), null);
         pw.close();
      }
      catch (Exception e) {
          throw new TestException (
            "I/O error during write: "+e.getMessage());        
      }
   }

   public void test() {
      for (Vector3d widths : myWidths) {
         for (Vector3i res : myResolutions) {
            for (RigidTransform3d TCL : myTCLs) {
               for (RigidTransform3d TLW : myTLWs) {
                  test (new VectorGrid<Vector2d>(
                           Vector2d.class, widths, res, TCL), TLW);
                  test (new VectorGrid<Vector3d>(
                           Vector3d.class, widths, res, TCL), TLW);
                  test (new VectorGrid<Matrix3d>(
                           Matrix3d.class, widths, res, TCL), TLW);
                  test (new VectorNdGrid(
                           5, widths, res, TCL), TLW);
                  test (new MatrixNdGrid(
                           2, 4, widths, res, TCL), TLW);
               }
            }
         }
      }
   }

   public static void main (String[] args) {

      RandomGenerator.setSeed (0x1234);

      VectorGridTest tester = new VectorGridTest();
      tester.runtest();
   }

}
