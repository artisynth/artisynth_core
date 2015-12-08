package artisynth.core.mechmodels;

import maspack.util.*;
import maspack.matrix.*;

public class NodalFrameTest {

   double EPS = 1e-8;

   RotationMatrix3d createFrame (Point3d n0, Point3d n1, Point3d n2) {
      Vector3d ex = new Vector3d();
      Vector3d ey = new Vector3d();
      Vector3d ez = new Vector3d();
      ex.sub (n1, n0);
      ex.normalize();
      ez.sub (n2, n0);
      ez.cross (ex, ez);
      ez.normalize();
      ey.cross (ez, ex);
      RotationMatrix3d R = new RotationMatrix3d();
      R.setXYDirections (ex, ey);
      return R;
   }

   private void perturb (Vector3d vr, double eps) {
      Vector3d e = new Vector3d();
      e.setRandom();
      e.scale (EPS);
      vr.add (vr, e);
   }

   public void test() {

      double eps = 1e-8;

      Point3d x0 = new Point3d ();
      Point3d x1 = new Point3d ();
      Point3d x2 = new Point3d ();

      Point3d y0 = new Point3d ();
      Point3d y1 = new Point3d ();
      Point3d y2 = new Point3d ();

      //x1.set (1, 0, 0);
      //x2.set (0.437, 0.66, 0);
      x1.setRandom();
      x2.setRandom();

      RotationMatrix3d R0 = createFrame (x0, x1, x2);

      Vector3d ex = new Vector3d();
      Vector3d ey = new Vector3d();
      Vector3d ez = new Vector3d();
      R0.getColumn (0, ex);
      R0.getColumn (1, ey);
      R0.getColumn (2, ez);
      Vector3d r1 = new Vector3d();
      Vector3d r2 = new Vector3d();
      r1.sub (x1, x0);
      r2.sub (x2, x0);

      y0.set (x0);
      y1.set (x1);
      y2.set (x2);
      //y1.add (new Vector3d (0, 0, -0.44*EPS));
      //y1.scaledAdd (0.77*EPS, ez);
      //y1.add (new Vector3d (0, 0.77*EPS, 0));
      perturb (y0, EPS);
      perturb (y1, EPS);
      perturb (y2, EPS);

      RotationMatrix3d R1 = createFrame (y0, y1, y2);

      Matrix3d DR = new Matrix3d();
      DR.sub (R1, R0);
      DR.mulTransposeRight (DR, R0);
      System.out.println ("[w]=\n" + DR.toString ("%14.10f"));

      Vector3d w = new Vector3d (-DR.m12, DR.m02, -DR.m01);
      w.scale (1/EPS);
      w.inverseTransform (R0);
      System.out.println ("w   =" + w.toString ("%12.8f"));

      Vector3d rdot1 = new Vector3d();
      Vector3d rdot2 = new Vector3d();

      rdot1.add (y1, x0);
      rdot1.sub (x1);
      rdot1.sub (y0);
      rdot1.scale (1/EPS);

      rdot2.add (y2, x0);
      rdot2.sub (x2);
      rdot2.sub (y0);
      rdot2.scale (1/EPS);

      Vector3d wchk = new Vector3d();
      wchk.set ((rdot2.dot(ez)-r2.dot(ex)*rdot1.dot(ez)/r1.norm())/r2.dot(ey),
                -rdot1.dot(ez)/r1.norm(), 
                rdot1.dot(ey)/r1.norm());
      //wchk.transform (R0);

      System.out.println ("wchk=" + wchk.toString ("%12.8f"));
   }

   public static void main (String[] args) {
      NodalFrameTest tester = new NodalFrameTest();
      RandomGenerator.setSeed (0x1234);
      tester.test();
   }

}
