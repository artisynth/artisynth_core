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

/**
 * Base class for testing subclasses of InterpolatingGridBase
 */
public class InterpolatingGridTestBase extends UnitTest {

   ArrayList<Vector3d> myWidths = new ArrayList<>();
   ArrayList<Vector3i> myResolutions = new ArrayList<>();
   ArrayList<RigidTransform3d> myTCLs = new ArrayList<>();
   ArrayList<RigidTransform3d> myTLWs = new ArrayList<>();

   public InterpolatingGridTestBase() {
      myTLWs.add (new RigidTransform3d());
      RigidTransform3d TLW = new RigidTransform3d();
      TLW.setRandom();
      myTLWs.add (TLW);

      myResolutions.add (new Vector3i (1, 1, 1));
      myResolutions.add (new Vector3i (2, 2, 2));
      myResolutions.add (new Vector3i (5, 3, 2));

      myTCLs.add (new RigidTransform3d());
      myTCLs.add (new RigidTransform3d(1, 2, 3));
      myTCLs.add (new RigidTransform3d(1, 2, 3, 4, 5, 6));

      myWidths.add (new Vector3d (1, 1, 1));
      myWidths.add (new Vector3d (1, 2, 3));
   }

   Point3d createTestPoint (InterpolatingGridBase grid) {
      Vector3d widths = new Vector3d (grid.getWidths());
      Point3d pnt = new Point3d();
      pnt.x = RandomGenerator.nextDouble (-1.2*widths.x/2, 1.2*widths.x/2);
      pnt.y = RandomGenerator.nextDouble (-1.2*widths.y/2, 1.2*widths.y/2);
      pnt.z = RandomGenerator.nextDouble (-1.2*widths.z/2, 1.2*widths.z/2);
      RigidTransform3d TCL = new RigidTransform3d();
      RigidTransform3d TLW = new RigidTransform3d();
      if (grid.getLocalToWorld() != null) {
         TLW.set (grid.getLocalToWorld());
      }
      grid.getCenter (TCL.p);
      grid.getOrientation (TCL.R);
      pnt.transform (TCL);
      pnt.transform (TLW);
      return pnt;
   }

   protected boolean getCellVertexAndWeights (
      Vector3i xyzi, double[] wgts,
      InterpolatingGridBase grid, Point3d pnt) {

      Point3d loc = new Point3d(pnt);
      RigidTransform3d TCL = new RigidTransform3d();
      grid.getCenter (TCL.p);
      grid.getOrientation (TCL.R);
      loc.inverseTransform (TCL);
      Vector3d widths = grid.getWidths();
      Vector3i res = grid.getResolution();

      loc.x = res.x*(loc.x/widths.x + 0.5);
      loc.y = res.y*(loc.y/widths.y + 0.5);
      loc.z = res.z*(loc.z/widths.z + 0.5);

      if (loc.x < 0.0 || loc.x > res.x ||
          loc.y < 0.0 || loc.y > res.y || 
          loc.z < 0.0 || loc.z > res.z) {
         return false;
      }
      xyzi.x = (int)loc.x;
      xyzi.y = (int)loc.y;
      xyzi.z = (int)loc.z;
      if (xyzi.x > res.x) {
         xyzi.x = res.x;
      }
      if (xyzi.y > res.y) {
         xyzi.y = res.y;
      }
      if (xyzi.z > res.z) {
         xyzi.z = res.z;
      }
      double lamx = loc.x - xyzi.x;
      double lamy = loc.y - xyzi.y;
      double lamz = loc.z - xyzi.z;

      wgts[0] = (1-lamx)*(1-lamy)*(1-lamz); // w000
      wgts[1] = (1-lamx)*(1-lamy)*lamz;     // w001
      wgts[2] = (1-lamx)*lamy*(1-lamz);     // w010
      wgts[3] = (1-lamx)*lamy*lamz;         // w011
      wgts[4] = lamx*(1-lamy)*(1-lamz);     // w100
      wgts[5] = lamx*(1-lamy)*lamz;         // w101
      wgts[6] = lamx*lamy*(1-lamz);         // w110
      wgts[7] = lamx*lamy*lamz;             // w111
      return true;
   }
}
