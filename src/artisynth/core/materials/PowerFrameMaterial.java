package artisynth.core.materials;

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
 * A FrameMaterial that is linear under small rotation assumptions and allows
 * separate stiffness and damping along each of the the six degrees of freedom.
 */
public class PowerFrameMaterial extends FrameMaterial {

   protected Vector3d myK = new Vector3d();
   protected Vector3i myExps = new Vector3i(1,1,1);
   protected Vector3d myD = new Vector3d();
   protected Vector3d myDeadband = new Vector3d();
   protected Vector3d myRotK = new Vector3d();
   protected Vector3i myRotExps = new Vector3i(1,1,1);
   protected Vector3d myRotD = new Vector3d();

   public static PropertyList myProps =
      new PropertyList (PowerFrameMaterial.class, FrameMaterial.class);

   static {
      myProps.add (
         "stiffness", "translational stiffness", Vector3d.ZERO);
      myProps.add (
         "exponents",
         "exponents for translational forces", Vector3i.ONES, "[1,3]");
      myProps.add (
         "deadband", "translational deadband", Vector3d.ZERO, "[0,inf]");
      myProps.add (
         "damping", "translational damping", Vector3d.ZERO);
      myProps.add (
         "rotaryStiffness", "rotational stiffness", Vector3d.ZERO);
      myProps.add (
         "rotaryExponents",
         "exponents for rotational forces", Vector3i.ONES, "[1,3]");
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
         if (n < 0 || n > 3) {
            throw new IllegalArgumentException (
               "exponent has value "+n+"; must be in the range [1,3]");
         }
         myExps.set (i, n);
      }
   }

   // public Range getExponentsRange() {
   //    return new NumericIntervalRange (
   //       new IntegerInterval(0,3), /*allowEmpty*/false);
   // }

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

   public Vector3d getDeadband () {
      return myDeadband;
   }

   public void setDeadband (double db) {
      myDeadband.set (db, db, db);
   }

   public void setDeadband (double dbx, double dby, double dbz) {
      myDeadband.set (dbx, dby, dbz);
   }

   public void setDeadband (Vector3d dbvec) {
      myDeadband.set (dbvec);
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

   // public Range getRotaryExponentsRange() {
   //    return new NumericIntervalRange (
   //       new IntegerInterval(0,3), /*allowEmpty*/false);
   // }

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
      this (0, 0, 0, 0, 0, 0);
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

   private double applyDeadband (double dx, double deadband) {
      if (dx > deadband) {
         return dx-deadband;
      }
      else if (dx < -deadband) {
         return dx+deadband;
      }
      else {
         return 0;
      }
   }

   private double evalPower (double dx, double deadband, int exp) {
      dx = applyDeadband (dx, deadband);
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

   private double evalDeriv (double dx, double deadband, int exp) {
      dx = applyDeadband (dx, deadband);
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

      Vector3d p = X21.p;

      // use these matrix entries as small angle approximations to 
      // the rotations about x, y, and z
      double sx =  X21.R.m21;
      double sy = -X21.R.m20;
      double sz =  X21.R.m10;

      wr.f.x = myK.x*evalPower (p.x, myDeadband.x, myExps.x);
      wr.f.y = myK.y*evalPower (p.y, myDeadband.y, myExps.y);
      wr.f.z = myK.z*evalPower (p.z, myDeadband.z, myExps.z);

      wr.m.x = myRotK.x*evalPower (sx, 0, myRotExps.x);
      wr.m.y = myRotK.y*evalPower (sy, 0, myRotExps.y);
      wr.m.z = myRotK.z*evalPower (sz, 0, myRotExps.z);

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

      RotationMatrix3d R = X21.R;
      Vector3d p = X21.p;

      // use these matrix entries as small angle approximations to 
      // the rotations about x, y, and z
      double sx =  R.m21;
      double sy = -R.m20;
      double sz =  R.m10;

      Jq.m00 = myK.x*evalDeriv (p.x, myDeadband.x, myExps.x);
      Jq.m11 = myK.y*evalDeriv (p.y, myDeadband.y, myExps.y);
      Jq.m22 = myK.z*evalDeriv (p.z, myDeadband.z, myExps.z);

      double rkx = myRotK.x*evalDeriv (sx, 0, myRotExps.x);
      double rky = myRotK.y*evalDeriv (sy, 0, myRotExps.y);
      double rkz = myRotK.z*evalDeriv (sz, 0, myRotExps.z);

      Jq.m33 = rkx*R.m11;
      Jq.m44 = rky*R.m00;
      Jq.m55 = rkz*R.m00;

      if (!symmetric) {
         Jq.m34 = -rkx*R.m01;
         Jq.m43 = -rky*R.m10;
         Jq.m53 = -rkz*R.m20;
      }
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
          !myDeadband.equals (lm.myDeadband) ||
          !myD.equals (lm.myD) ||
          !myRotK.equals (lm.myRotK) ||
          !myRotExps.equals (lm.myRotExps) ||
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
      mat.myDeadband = new Vector3d (myDeadband);
      mat.myRotK = new Vector3d (myRotK);
      mat.myRotExps = new Vector3i (myRotExps);
      mat.myD = new Vector3d (myD);
      mat.myRotD = new Vector3d (myRotD);
      return mat;
   }
}

