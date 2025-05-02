package artisynth.core.materials;

import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix6d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector3i;
import maspack.util.*;
import maspack.properties.PropertyList;
import maspack.spatialmotion.Twist;
import maspack.spatialmotion.Wrench;

/**
 * A FrameMaterial that computes restoring forces along the translational and
 * rotational directions according to a power law of the form
 * <pre>
 * f = K sgn(d) |d|^n
 * </pre>
 * where K is a stiffness, d is a displacement, and n is an exponent.  A value
 * of n = 1 results in a standard linear relationship {@code f = K d}.
 *
 * <p> Stiffnesses and exponents can be specified for each of the six
 * translational and rotational directions. Rotational displacements, and the
 * resulting forces, are computed with respect to the x-y-z rotation angles of
 * the rotational displacement matrix, as computed using {@link
 * RotationMatrix3d.getXyz}. For both translational and rotational
 * displacements, it is possible to specify a deadband {@code b}, such that the
 * displacement {@code d} applied to the force law is determined from the true
 * displacement {@code dt} by
 * <pre>
 * d = sgn(dt) max(0, |dt|-b)
 * </pre>
 */
public class PowerFrameMaterial extends FrameMaterial {

   protected Vector3d myK = new Vector3d();
   protected Vector3i myExps = new Vector3i(1,1,1);
   protected Vector3d myD = new Vector3d();
   protected Vector3d myUpperDeadband = new Vector3d();
   protected Vector3d myLowerDeadband = new Vector3d();
   protected Vector3d myRotK = new Vector3d();
   protected Vector3i myRotExps = new Vector3i(1,1,1);
   protected Vector3d myRotD = new Vector3d();
   protected Vector3d myUpperRotDeadband = new Vector3d();
   protected Vector3d myLowerRotDeadband = new Vector3d();

   public static PropertyList myProps =
      new PropertyList (PowerFrameMaterial.class, FrameMaterial.class);

   static {
      myProps.add (
         "stiffness", "translational stiffness", Vector3d.ZERO);
      myProps.add (
         "exponents",
         "exponents for translational forces", Vector3i.ONES, "[1,3]");
      myProps.add (
         "upperDeadband",
         "upper positive limits to the translational force deadband",
         Vector3d.ZERO, "[0,inf]");
      myProps.add (
         "lowerDeadband",
         "lower negative limits to the translational force deadband",
         Vector3d.ZERO, "[-inf,0]");
      myProps.add (
         "damping", "translational damping", Vector3d.ZERO);
      myProps.add (
         "rotaryStiffness", "rotational stiffness", Vector3d.ZERO);
      myProps.add (
         "rotaryExponents",
         "exponents for rotational forces", Vector3i.ONES, "[1,3]");
      myProps.add (
         "upperRotaryDeadband",
         "upper positive limits to the rotation force deadband (radians)",
         Vector3d.ZERO, "[0,inf]");
      myProps.add (
         "lowerRotaryDeadband",
         "lower negative limits to the rotation force deadband (radians)",
         Vector3d.ZERO, "[-inf,0]");
      myProps.add (
         "rotaryDamping", "rotational damping", Vector3d.ZERO);
   }   

   public PropertyList getAllPropertyInfo() {
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

   public Vector3i getExponents () {
      return myExps;
   }

   public void setExponents (int n) {
      setExponents (new Vector3i (n, n, n));
   }

   public void setExponents (int nx, int ny, int nz) {
      setExponents (new Vector3i (nx, ny, nz));
   }

   public void setExponents (Vector3i nvec) {
      for (int i=0; i<3; i++) {
         int n = nvec.get(i);
         if (n <= 0 || n > 3) {
            throw new IllegalArgumentException (
               "exponent has value "+n+"; must be in the range [1,3]");
         }
         myExps.set (i, n);
      }
   }

   public Vector3d getUpperDeadband () {
      return myUpperDeadband;
   }

   public void setUpperDeadband (double db) {
      setUpperDeadband (db, db, db);
   }

   public void setUpperDeadband (double dbx, double dby, double dbz) {
      dbx = Math.max (dbx, 0);
      dby = Math.max (dby, 0);
      dbz = Math.max (dbz, 0);
      myUpperDeadband.set (dbx, dby, dbz);
   }

   public void setUpperDeadband (Vector3d dbvec) {
      setUpperDeadband (dbvec.x, dbvec.y, dbvec.z);
   }

   public Vector3d getLowerDeadband () {
      return myLowerDeadband;
   }

   public void setLowerDeadband (double db) {
      setLowerDeadband (db, db, db);
   }

   public void setLowerDeadband (double dbx, double dby, double dbz) {
      dbx = Math.min (dbx, 0);
      dby = Math.min (dby, 0);
      dbz = Math.min (dbz, 0);
      myLowerDeadband.set (dbx, dby, dbz);
   }

   public void setLowerDeadband (Vector3d dbvec) {
      setLowerDeadband (dbvec.x, dbvec.y, dbvec.z);
   }

   public void setDeadband (double db) {
      setUpperDeadband (db);
      setLowerDeadband (-db);
   }

   public void setDeadband (double dbx, double dby, double dbz) {
      setUpperDeadband (dbx, dby, dbz);
      setLowerDeadband (-dbx, -dby, -dbz);
   }

   public Vector3d getDamping() {
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

   public Vector3d getRotaryStiffness() {
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
 
   public Vector3i getRotaryExponents () {
      return myRotExps;
   }

   public void setRotaryExponents (int n) {
      setRotaryExponents (new Vector3i (n, n, n));
   }

   public void setRotaryExponents (int nx, int ny, int nz) {
      setRotaryExponents (new Vector3i (nx, ny, nz));
   }

   public void setRotaryExponents (Vector3i nvec) {
      for (int i=0; i<3; i++) {
         int n = nvec.get(i);
         if (n < 0 || n > 3) {
            throw new IllegalArgumentException (
               "exponent has value "+n+"; must be in the range [0,3]");
         }
         myRotExps.set (i, n);
      }
   }

   public Vector3d getUpperRotaryDeadband () {
      return myUpperRotDeadband;
   }

   public void setUpperRotaryDeadband (double db) {
      setUpperRotaryDeadband (db, db, db);
   }

   public void setUpperRotaryDeadband (double dbx, double dby, double dbz) {
      dbx = Math.max (dbx, 0);
      dby = Math.max (dby, 0);
      dbz = Math.max (dbz, 0);
      myUpperRotDeadband.set (dbx, dby, dbz);
   }

   public void setUpperRotaryDeadband (Vector3d dbvec) {
      setUpperRotaryDeadband (dbvec.x, dbvec.y, dbvec.z);
   }

   public Vector3d getLowerRotaryDeadband () {
      return myLowerRotDeadband;
   }

   public void setLowerRotaryDeadband (double db) {
      setLowerRotaryDeadband (db, db, db);
   }

   public void setLowerRotaryDeadband (double dbx, double dby, double dbz) {
      dbx = Math.min (dbx, 0);
      dby = Math.min (dby, 0);
      dbz = Math.min (dbz, 0);
      myLowerRotDeadband.set (dbx, dby, dbz);
   }

   public void setLowerRotaryDeadband (Vector3d dbvec) {
      setLowerRotaryDeadband (dbvec.x, dbvec.y, dbvec.z);
   }

   public void setRotaryDeadband (double db) {
      setUpperRotaryDeadband (db);
      setLowerRotaryDeadband (-db);
   }

   public void setRotaryDeadband (double dbx, double dby, double dbz) {
      setUpperRotaryDeadband (dbx, dby, dbz);
      setLowerRotaryDeadband (-dbx, -dby, -dbz);
   }

   public Vector3d getRotaryDamping() {
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

   public PowerFrameMaterial () {
      this (0, 1, 0, 1, 0, 0);
   }

   public PowerFrameMaterial (
      double k, int n, double kr, int nr, double d, double dr) {
      setStiffness (k);
      setExponents (n);
      setRotaryStiffness (kr);
      setRotaryExponents (nr);
      setDamping (d);
      setRotaryDamping (dr);
   }

   private double applyDeadband (double dx, double lower, double upper) {
      if (dx > upper) {
         return dx-upper;
      }
      else if (dx < lower) {
         return dx-lower;
      }
      else {
         return 0;
      }
   }

   private void applyDeadband (Vector3d del, Vector3d p) {
      del.x = applyDeadband (p.x, myLowerDeadband.x, myUpperDeadband.x);
      del.y = applyDeadband (p.y, myLowerDeadband.y, myUpperDeadband.y);
      del.z = applyDeadband (p.z, myLowerDeadband.z, myUpperDeadband.z);
   }

   private void applyRotDeadband (double[] angs) {
      angs[0] = applyDeadband (
         angs[0], myLowerRotDeadband.x, myUpperRotDeadband.x);
      angs[1] = applyDeadband (
         angs[1], myLowerRotDeadband.y, myUpperRotDeadband.y);
      angs[2] = applyDeadband (
         angs[2], myLowerRotDeadband.z, myUpperRotDeadband.z);
   }

   private double evalPower (double dx, int exp) {
      if (dx == 0) {
         return 0;
      }
      double pow = 1.0;
      for (int i=0; i<exp; i++) {
         pow *= dx;
      }
      if (dx < 0 && pow > 0) {
         return -pow;
      }
      else {
         return pow;
      }
   }

   private double evalDeriv (double dx, int exp) {
      if (dx == 0) {
         return 0;
      }
      double deriv = 1.0;
      for (int i=0; i<exp-1; i++) {
         deriv *= dx;
      }
      deriv *= exp;
      if (dx < 0 && deriv < 0) {
         return -deriv;
      }
      else {
         return deriv;
      }
   }

   public void computeF (
      Wrench wr, RigidTransform3d X21, Twist vel21, 
      RigidTransform3d initialX21) {

      // translational forces
      Vector3d del = new Vector3d();
      applyDeadband (del, X21.p);

      wr.f.x = myK.x*evalPower (del.x, myExps.x);
      wr.f.y = myK.y*evalPower (del.y, myExps.y);
      wr.f.z = myK.z*evalPower (del.z, myExps.z);

      // get the x-y-z angles for computing the rotational forces
      RotationMatrix3d R = X21.R;
      double[] angs = new double[3];
      R.getXyz (angs);
      double cx = Math.cos(angs[0]);
      double sx = Math.sin(angs[0]);
      double cy = Math.cos(angs[1]);
      double sy = Math.sin(angs[1]);
      if (Math.abs(cy) < 1e-6) {
         // handle singularity
         cy = (cy > 0 ? 1e-6 : -1e-6);
      }
      // apply deadband
      applyRotDeadband (angs);

      // matrix Hinv maps angular velocity to x-y-z angle speeds
      double hi21 = -sx/cy;
      double hi22 = cx/cy;
      Matrix3d Hinv = new Matrix3d();
      Hinv.set (1, -sy*hi21, -sy*hi22, 0, cx, sx, 0, hi21, hi22);

      // compute generalized force f in angle space
      Vector3d f = new Vector3d();
      f.x = myRotK.x*evalPower (angs[0], myRotExps.x);
      f.y = myRotK.y*evalPower (angs[1], myRotExps.y);
      f.z = myRotK.z*evalPower (angs[2], myRotExps.z);
      // map f to frame moment using transpose of Hinv
      Hinv.mulTranspose (wr.m, f);

      // damping forces - currently computing using angular velocity
      // instead of angle speeds
      Vector3d v = vel21.v;
      Vector3d w = vel21.w;

      wr.f.x += myD.x*v.x;
      wr.f.y += myD.y*v.y;
      wr.f.z += myD.z*v.z;

      wr.m.x += myRotD.x*w.x;
      wr.m.y += myRotD.y*w.y;
      wr.m.z += myRotD.z*w.z;
   }

   public void computeDFdq (
      Matrix6d Jq, RigidTransform3d X21, Twist vel21, 
      RigidTransform3d initialX21, boolean symmetric) {

      Jq.setZero();
      
      // translational stiffness

      Vector3d del = new Vector3d();
      applyDeadband (del, X21.p);
      Jq.m00 = myK.x*evalDeriv (del.x, myExps.x);
      Jq.m11 = myK.y*evalDeriv (del.y, myExps.y);
      Jq.m22 = myK.z*evalDeriv (del.z, myExps.z);

      // get the x-y-z angles for computing the rotational forces
      RotationMatrix3d R = X21.R;
      double[] angs = new double[3];
      R.getXyz (angs);
      double cx = Math.cos(angs[0]);
      double sx = Math.sin(angs[0]);
      double cy = Math.cos(angs[1]);
      double sy = Math.sin(angs[1]);
      double ty;
      if (Math.abs(cy) < 1e-6) {
         // handle singularity
         cy = (cy > 0 ? 1e-6 : -1e-6);
      }
      ty = sy/cy;
      // apply deadband
      applyRotDeadband (angs);

      // matrix Hinv maps angular velocity to x-y-z angle speeds
      double hi21 = -sx/cy;
      double hi22 = cx/cy;
      Matrix3d Hinv = new Matrix3d();
      Hinv.set (1, -sy*hi21, -sy*hi22, 0, cx, sx, 0, hi21, hi22);

      // compute generalized force f in angle space
      Vector3d f = new Vector3d();
      f.x = myRotK.x*evalPower (angs[0], myRotExps.x);
      f.y = myRotK.y*evalPower (angs[1], myRotExps.y);
      f.z = myRotK.z*evalPower (angs[2], myRotExps.z);
      // compute generalized force derivatives in angle space
      Vector3d fderivs = new Vector3d();
      fderivs.x = myRotK.x*evalDeriv (angs[0], myRotExps.x);
      fderivs.y = myRotK.y*evalDeriv (angs[1], myRotExps.y);
      fderivs.z = myRotK.z*evalDeriv (angs[2], myRotExps.z);

      // compute rotary stiffness from f, fderivs, and Hinv:
      Matrix3d Jr = new Matrix3d();
      Jr.transpose (Hinv);
      Jr.mulCols (fderivs);
      if (!symmetric) {
         // add force dependent terms
         Jr.m10 += cx*ty*f.x - sx*f.y - cx*f.z/cy;
         Jr.m11 += (1+ty*ty)*sx*f.x - sx*ty*f.z/cy;
         Jr.m20 += sx*ty*f.x + cx*f.y - sx*f.z/cy;
         Jr.m21 += -(1+ty*ty)*cx*f.x + cx*ty*f.z/cy;
      }
      Jr.mul (Hinv);

      Jq.setSubMatrix (3, 3, Jr);
   }

   public void computeDFdu (
      Matrix6d Ju, RigidTransform3d X21, Twist vel21, 
      RigidTransform3d initialX21, boolean symmetric) {

      Ju.setZero();

      Ju.m00 = myD.x;
      Ju.m11 = myD.y;
      Ju.m22 = myD.z;
      
      Ju.m33 = myRotD.x;
      Ju.m44 = myRotD.y;
      Ju.m55 = myRotD.z;
   }

   public boolean equals (FrameMaterial mat) {
      if (!(mat instanceof PowerFrameMaterial)) {
         return false;
      }
      PowerFrameMaterial lm = (PowerFrameMaterial)mat;
      if (!myK.equals (lm.myK) ||
          !myExps.equals (lm.myExps) ||
          !myUpperDeadband.equals (lm.myUpperDeadband) ||
          !myLowerDeadband.equals (lm.myLowerDeadband) ||
          !myD.equals (lm.myD) ||
          !myRotK.equals (lm.myRotK) ||
          !myRotExps.equals (lm.myRotExps) ||
          !myUpperRotDeadband.equals (lm.myUpperRotDeadband) ||
          !myLowerRotDeadband.equals (lm.myLowerRotDeadband) ||
          !myRotD.equals (lm.myRotD)) {
         return false;
      }
      else {
         return super.equals (mat);
      }
   }

   public PowerFrameMaterial clone() {
      PowerFrameMaterial mat = (PowerFrameMaterial)super.clone();
      mat.myK = new Vector3d (myK);
      mat.myExps = new Vector3i (myExps);
      mat.myUpperDeadband = new Vector3d (myUpperDeadband);
      mat.myLowerDeadband = new Vector3d (myLowerDeadband);
      mat.myRotK = new Vector3d (myRotK);
      mat.myRotExps = new Vector3i (myRotExps);
      mat.myUpperRotDeadband = new Vector3d (myUpperRotDeadband);
      mat.myLowerRotDeadband = new Vector3d (myLowerRotDeadband);
      mat.myD = new Vector3d (myD);
      mat.myRotD = new Vector3d (myRotD);
      return mat;
   }
}

