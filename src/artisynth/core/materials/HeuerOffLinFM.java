package artisynth.core.materials;

import maspack.matrix.Matrix6d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;
import maspack.spatialmotion.Twist;
import maspack.spatialmotion.Wrench;

public class HeuerOffLinFM extends FrameMaterial {

   protected Vector3d myK = new Vector3d ();
   protected Vector3d myD = new Vector3d ();
   protected Vector3d myRotK = new Vector3d ();
   protected Vector3d myRotD = new Vector3d ();

   public static PropertyList myProps = new PropertyList (
      HeuerOffLinFM.class, FrameMaterial.class);

   static {
      myProps.add (
         "stiffness * *", "HeuerIntact offset linear spring stiffness",
         Vector3d.ZERO);
      myProps.add (
         "damping * *", "HeuerIntact offset linear spring damping",
         Vector3d.ZERO);
      myProps.add (
         "rotaryStiffness * *", "HeuerIntact offset linear spring stiffness",
         Vector3d.ZERO);
      myProps.add (
         "rotaryDamping * *", "HeuerIntact offset linear spring damping",
         Vector3d.ZERO);
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

   public HeuerOffLinFM () {
      this (0, 0, 0, 0);
   }

   public HeuerOffLinFM (double k, double kr, double d, double dr) {
      setStiffness (k);
      setRotaryStiffness (kr);
      setDamping (d);
      setRotaryDamping (dr);
   }

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

      double fp1 = 0.0312;
      double fp2 = -0.1818;
      double fp3 = 1.1181;
      double fp4 = 0;

      double ep1 = 0.0288;
      double ep2 = 0.0192;
      double ep3 = 1.248;
      double ep4 = 0;

      double lbp1 = 0.0081;
      double lbp2 = 0.1417;
      double lbp3 = 0.4741;
      double lbp4 = 0;

      double arp1 = 0.0291;
      double arp2 = -0.0044;
      double arp3 = 2.5695;
      double arp4 = 0;

      wr.f.x = myK.x * (p.x - initialp.x);
      wr.f.y = myK.y * (p.y - initialp.y);
      wr.f.z = myK.z * (p.z - initialp.z);

      wr.m.x =
         (lbp1 * Math.pow (absx, 2) + lbp2 * Math.pow (absx, 1) + lbp3)
         * Math.toDegrees (sx);
      wr.m.y =
         (arp1 * Math.pow (absy, 2) + arp2 * Math.pow (absy, 1) + arp3)
         * Math.toDegrees (sy);

      if (sz < 0)
         wr.m.z =
            (fp1 * Math.pow (absz, 2) + fp2 * Math.pow (absz, 1) + fp3)
            * Math.toDegrees (sz);
      else
         wr.m.z =
            (ep1 * Math.pow (absz, 2) + ep2 * Math.pow (absz, 1) + ep3)
            * Math.toDegrees (sz);

      Vector3d v = vel21.v;
      Vector3d w = vel21.w;

      wr.f.x += myD.x * v.x;
      wr.f.y += myD.y * v.y;
      wr.f.z += myD.z * v.z;

      wr.m.x += myRotD.x * w.x;
      wr.m.y += myRotD.y * w.y;
      wr.m.z += myRotD.z * w.z;
      
      // added for validaiton, and therefore should be removed
      wr.scale (1);
   }

   public void computeDFdq (
      Matrix6d Jq, RigidTransform3d X21, Twist vel21,
      RigidTransform3d initialX21, boolean symmetric) {

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

      double fp1 = 0.0312;
      double fp2 = -0.1818;
      double fp3 = 1.1181;
      double fp4 = 0;

      double ep1 = 0.0288;
      double ep2 = 0.0192;
      double ep3 = 1.248;
      double ep4 = 0;

      double lbp1 = 0.0081;
      double lbp2 = 0.1417;
      double lbp3 = 0.4741;
      double lbp4 = 0;

      double arp1 = 0.0291;
      double arp2 = -0.0044;
      double arp3 = 2.5695;
      double arp4 = 0;

      Jq.m00 = myK.x;
      Jq.m11 = myK.y;
      Jq.m22 = myK.z;

      Jq.m33 =
         (3 * lbp1 * Math.pow (absx, 2) + 2 * lbp2 * Math.pow (absx, 1) + lbp3)
         * Math.toDegrees (1) * R.m11;
      Jq.m44 =
         (3 * arp1 * Math.pow (absy, 2) + 2 * arp2 * Math.pow (absy, 1) + arp3)
         * Math.toDegrees (1) * R.m00;

      if (sz < 0)
         Jq.m55 =
            (3 * fp1 * Math.pow (absz, 2) + 2 * fp2 * Math.pow (absz, 1) + fp3)
            * Math.toDegrees (1) * R.m00;
      else
         Jq.m55 =
            (3 * ep1 * Math.pow (absz, 2) + 2 * ep2 * Math.pow (absz, 1) + ep3)
            * Math.toDegrees (1) * R.m00;

      if (!symmetric) {
         Jq.m34 =
            -(3 * lbp1 * Math.pow (absx, 2) + 2 * lbp2 * Math.pow (absx, 1) + lbp3)
            * Math.toDegrees (1) * R.m01;
         Jq.m43 =
            -(3 * arp1 * Math.pow (absy, 2) + 2 * arp2 * Math.pow (absy, 1) + arp3)
            * Math.toDegrees (1) * R.m10;

         if (sz < 0)
            Jq.m53 =
               -(3 * fp1 * Math.pow (absz, 2) + 2 * fp2 * Math.pow (absz, 1) + fp3)
               * Math.toDegrees (1) * R.m20;
         else
            Jq.m53 =
               -(3 * ep1 * Math.pow (absz, 2) + 2 * ep2 * Math.pow (absz, 1) + ep3)
               * Math.toDegrees (1) * R.m20;
      }
      
   // added for validaiton, and therefore should be removed
      Jq.scale (1);
   }

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
      if (!(mat instanceof HeuerOffLinFM)) {
         return false;
      }
      HeuerOffLinFM hom = (HeuerOffLinFM)mat;
      if (!myK.equals (hom.myK) ||
          !myD.equals (hom.myD) ||
          myRotK != hom.myRotK ||
          myRotD != hom.myRotD) {
         return false;
      }
      else {
         return super.equals (mat);
      }
   }

   public HeuerOffLinFM clone () {
      HeuerOffLinFM mat = (HeuerOffLinFM)super.clone ();
      mat.myK = new Vector3d (myK);
      mat.myRotK = new Vector3d (myRotK);
      mat.myD = new Vector3d (myD);
      mat.myRotD = new Vector3d (myRotD);
      return mat;
   }
}
