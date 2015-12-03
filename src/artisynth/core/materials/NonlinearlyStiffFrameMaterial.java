package artisynth.core.materials;

import maspack.matrix.Matrix6d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;
import maspack.spatialmotion.Twist;
import maspack.spatialmotion.Wrench;
import artisynth.core.materials.FrameMaterial;
import artisynth.core.materials.OffsetLinearFrameMaterial;

public class NonlinearlyStiffFrameMaterial extends FrameMaterial {

   protected Vector3d myK = new Vector3d ();
   protected Vector3d myD = new Vector3d ();
   protected Vector3d myRotK = new Vector3d ();
   protected Vector3d myRotD = new Vector3d ();

   public static PropertyList myProps = new PropertyList (
      NonlinearlyStiffFrameMaterial.class, FrameMaterial.class);

   static {
      myProps.add (
         "stiffness * *", "NonlinearlyStiff spring stiffness", Vector3d.ZERO);
      myProps.add (
         "damping * *", "NonlinearlyStiff spring damping", Vector3d.ZERO);
      myProps.add (
         "rotaryStiffness * *", "NonlinearlyStiff spring stiffness",
         Vector3d.ZERO);
      myProps.add (
         "rotaryDamping * *", "NonlinearStiff spring damping", Vector3d.ZERO);
   }

   public PropertyList getAllPropertyInfo () {
      return myProps;
   }

   public Vector3d getStiffness () {
      return myK;
   }

   public void setStiffness (double k) {
      myK.set (k, k, k);
   }

   public void setStiffness (double kx, double ky, double kz) {
      myK.set (kx, ky, kz);
   }

   public void setStiffness (Vector3d kvec) {
      myK.set (kvec);
   }

   public Vector3d getDamping () {
      return myD;
   }

   public void setDamping (double d) {
      myD.set (d, d, d);
   }

   public void setDamping (double dx, double dy, double dz) {
      myD.set (dx, dy, dz);
   }

   public void setDamping (Vector3d dvec) {
      myD.set (dvec);
   }

   public Vector3d getRotaryStiffness () {
      return myRotK;
   }

   public void setRotaryStiffness (double k) {
      myRotK.set (k, k, k);
   }

   public void setRotaryStiffness (double kx, double ky, double kz) {
      myRotK.set (kx, ky, kz);
   }

   public void setRotaryStiffness (Vector3d kvec) {
      myRotK.set (kvec);
   }

   public Vector3d getRotaryDamping () {
      return myRotD;
   }

   public void setRotaryDamping (double d) {
      myRotD.set (d, d, d);
   }

   public void setRotaryDamping (double dx, double dy, double dz) {
      myRotD.set (dx, dy, dz);
   }

   public void setRotaryDamping (Vector3d dvec) {
      myRotD.set (dvec);
   }

   public NonlinearlyStiffFrameMaterial () {
      this (0, 0, 0, 0);
   }

   public NonlinearlyStiffFrameMaterial (double k, double kr, double d,
   double dr) {
      setStiffness (k);
      setRotaryStiffness (kr);
      setDamping (d);
      setRotaryDamping (dr);
   }

   @Override
   public void computeF (
      Wrench wr, RigidTransform3d X21, Twist vel21, RigidTransform3d initialX21) {

      Vector3d p = X21.p;
      Vector3d initialp = initialX21.p;

      // use these matrix entries as small angle approximations to
      // the rotations about x, y, and z
      double sx = X21.R.m21;
      double sy = -X21.R.m20;
      double sz = X21.R.m10;

      double absx = Math.abs (Math.toDegrees (sx));
      double absy = Math.abs (Math.toDegrees (sy));
      double absz = Math.abs (Math.toDegrees (sz));

      // double sx = X21.R.m21/X21.R.m22;
      // sx = Math.atan(sx);
      // double sy = -X21.R.m20;
      // sy = Math.asin(sy);
      // double sz = X21.R.m10/X21.R.m00;
      // sz = Math.atan(sz);

      wr.f.x = myK.x * (p.x - initialp.x);
      wr.f.y = myK.y * (p.y - initialp.y);
      wr.f.z = myK.z * (p.z - initialp.z);

      // wr.m.x = myRotK.x * sx;
      // wr.m.y = myRotK.y * sy;

      wr.m.x =
         -(-0.0087 * Math.pow (absx, 2) - 0.6989 * Math.pow (absx, 1))
         * Math.toDegrees (sx);
      wr.m.y =
         -(-0.0061 * Math.pow (absy, 3) - 1.0191 * Math.pow (absy, 1))
         * Math.toDegrees (sy);
      wr.m.z =
         -(-0.002 * Math.pow (absz, 3) + 0.0141 * Math.pow (absz, 2) - 0.4726 * Math
            .pow (absz, 1)) * Math.toDegrees (sz);

      Vector3d v = vel21.v;
      Vector3d w = vel21.w;

      wr.f.x += myD.x * v.x;
      wr.f.y += myD.y * v.y;
      wr.f.z += myD.z * v.z;

      wr.m.x += myRotD.x * w.x;
      wr.m.y += myRotD.y * w.y;
      wr.m.z += myRotD.z * w.z;

   }

   @Override
   public void computeDFdq (
      Matrix6d Jq, RigidTransform3d X21, Twist vel21,
      RigidTransform3d initialX21, boolean symmetric) {
      // TODO Auto-generated method stub

      Jq.setZero ();

      RotationMatrix3d R = X21.R;
      // use these matrix entries as small angle approximations to
      // the rotations about x, y, and z
      double sx = R.m21;
      double sy = -R.m20;
      double sz = R.m10;

      double absx = Math.abs (Math.toDegrees (sx));
      double absy = Math.abs (Math.toDegrees (sy));
      double absz = Math.abs (Math.toDegrees (sz));

      // double sx = R.m21/R.m22;
      // sx = Math.atan(sx);
      // double sy = -R.m20;
      // sy = Math.asin(sy);
      // double sz = R.m10/R.m00;
      // sz = Math.atan(sz);

      Jq.m00 = myK.x;
      Jq.m11 = myK.y;
      Jq.m22 = myK.z;

      // Jq.m33 = myRotK.x * R.m11;
      // Jq.m44 = myRotK.y * R.m00;
      // Jq.m55 = myRotK.z * R.m00;

      Jq.m33 =
         -(-0.0087 * 3 * Math.pow (absx, 2) - 0.6989 * 2 * Math.pow (absx, 1))
         * Math.toDegrees (1) * R.m11;
      Jq.m44 =
         -(-0.0061 * 4 * Math.pow (absy, 3) - 1.0191 * 2 * Math.pow (absy, 1))
         * Math.toDegrees (1) * R.m00;
      Jq.m55 =
         -(-0.002 * 4 * Math.pow (absz, 3) + 0.0141 * 3 * Math.pow (absz, 2) - 0.4726 * 2 * Math
            .pow (absz, 1)) * Math.toDegrees (1) * R.m00;

      if (!symmetric) {
         // Jq.m34 = -myRotK.x * R.m01;
         // Jq.m43 = -myRotK.y * R.m10;
         // Jq.m53 = -myRotK.z* R.m20;

         Jq.m34 =
            (-0.0087 * 3 * Math.pow (absx, 2) - 0.6989 * 2 * Math.pow (absx, 1))
            * Math.toDegrees (1) * R.m01;
         Jq.m43 =
            (-0.0061 * 4 * Math.pow (absy, 3) - 1.0191 * 2 * Math.pow (absy, 1))
            * Math.toDegrees (1) * R.m10;
         Jq.m53 =
            ((-0.002 * 4 * Math.pow (absz, 3) + 0.0141 * 3 * Math.pow (absz, 2) - 0.4726 * 2 * Math
               .pow (absz, 1)) * Math.toDegrees (1)) * R.m20;
      }

   }

   @Override
   public void computeDFdu (
      Matrix6d Ju, RigidTransform3d X21, Twist vel21,
      RigidTransform3d initialX21, boolean symmetric) {

      Ju.setZero ();

      Ju.m00 = myD.x;
      Ju.m11 = myD.y;
      Ju.m22 = myD.z;

      Ju.m33 = myRotD.x;
      Ju.m44 = myRotD.y;
      Ju.m55 = myRotD.z;

   }

   public boolean equals (FrameMaterial mat) {
      if (!(mat instanceof NonlinearlyStiffFrameMaterial)) {
         return false;
      }
      NonlinearlyStiffFrameMaterial nsm = (NonlinearlyStiffFrameMaterial)mat;
      if (!myK.equals (nsm.myK) ||
          !myD.equals (nsm.myD) ||
          !myRotK.equals (nsm.myRotK) ||
          !myRotD.equals (nsm.myRotD)) {
         return false;
      }
      else {
         return super.equals (mat);
      }
   }

   public NonlinearlyStiffFrameMaterial clone () {
      NonlinearlyStiffFrameMaterial mat =
         (NonlinearlyStiffFrameMaterial)super.clone ();
      mat.myK = new Vector3d (myK);
      mat.myRotK = new Vector3d (myRotK);
      mat.myD = new Vector3d (myD);
      mat.myRotD = new Vector3d (myRotD);
      return mat;
   }

}
