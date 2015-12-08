package artisynth.core.materials;

import maspack.matrix.AxisAngle;
import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix6d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;
import maspack.spatialmotion.Twist;
import maspack.spatialmotion.Wrench;

/**
 * A FrameMaterial that is linear with respect to the displacement between
 * origins and also with respect to the displacement about the current rotation
 * axis.
 */
public class RotAxisFrameMaterial extends FrameMaterial {

   protected Vector3d myK = new Vector3d();
   protected Vector3d myD = new Vector3d();
   protected double myRotK;
   protected double myRotD;

   public static PropertyList myProps =
      new PropertyList (RotAxisFrameMaterial.class, FrameMaterial.class);

   static {
      myProps.add ("stiffness * *", "linear spring stiffness", Vector3d.ZERO);
      myProps.add ("damping * *", "linear spring damping", Vector3d.ZERO);
      myProps.add ("rotaryStiffness * *", "linear spring stiffness", 0);
      myProps.add ("rotaryDamping * *", "linear spring damping", 0);
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

   public double getRotaryStiffness() {
      return myRotK;
   }

   public void setRotaryStiffness (double k) {
      myRotK = k;
   }

   public double getRotaryDamping() {
      return myRotD;
   }

   public void setRotaryDamping (double d) {
      myRotD = d;
   }

   public RotAxisFrameMaterial () {
      this (0, 0, 0, 0);
   }

   public RotAxisFrameMaterial (double k, double kr, double d, double dr) {
      setStiffness (k);
      setRotaryStiffness (kr);
      setDamping (d);
      setRotaryDamping (dr);
   }

   public void computeF (
      Wrench wr, RigidTransform3d X21, Twist vel21, 
      RigidTransform3d initialX21) {

      Vector3d p = X21.p;

      wr.f.x = myK.x*p.x;
      wr.f.y = myK.y*p.y;
      wr.f.z = myK.z*p.z;

      AxisAngle axisAng = new AxisAngle();
      X21.R.getAxisAngle (axisAng);
      wr.m.scale (myRotK * axisAng.angle, axisAng.axis);

      Vector3d v = vel21.v;
      Vector3d w = vel21.w;

      wr.f.x += myD.x*v.x;
      wr.f.y += myD.y*v.y;
      wr.f.z += myD.z*v.z;
      wr.m.scaledAdd (myRotD, w);
   }

   protected void computeU (Matrix3d U, AxisAngle axisAng, boolean symmetric) {

      if (Math.abs(axisAng.angle) < 1e-8) {
         U.setIdentity();
      }
      else {
      
         double ang = axisAng.angle;
         double a = ang/Math.tan (ang/2);
         double b = (2 - ang/Math.tan (ang/2));

         Vector3d u = axisAng.axis;
         U.outerProduct (u, u);
         U.scale (b);

         U.m00 += a;
         U.m11 += a;
         U.m22 += a;

         double vz = ang*u.z;
         double vy = ang*u.y;
         double vx = ang*u.x;

         if (!symmetric) {
            U.m01 += vz;
            U.m02 -= vy;
            U.m12 += vx;
            U.m10 -= vz;
            U.m20 += vy;
            U.m21 -= vx;
         }

         U.scale (0.5);
      }
   }

   public void computeDFdq (
      Matrix6d Jq, RigidTransform3d X21, Twist vel21, 
      RigidTransform3d initialX21, boolean symmetric) {

      AxisAngle axisAng = new AxisAngle();
      X21.R.getAxisAngle (axisAng);
      Matrix3d U = new Matrix3d();

      Jq.setZero();

      computeU (U, axisAng, symmetric);
      Jq.m00 = myK.x;
      Jq.m11 = myK.y;
      Jq.m22 = myK.z;

      // set lower right submatrix to myRotK*U

      Jq.m33 = myRotK*U.m00;
      Jq.m34 = myRotK*U.m01;
      Jq.m35 = myRotK*U.m02;

      Jq.m43 = myRotK*U.m10;
      Jq.m44 = myRotK*U.m11;
      Jq.m45 = myRotK*U.m12;

      Jq.m53 = myRotK*U.m20;
      Jq.m54 = myRotK*U.m21;
      Jq.m55 = myRotK*U.m22;
   }

   public void computeDFdu (
      Matrix6d Ju, RigidTransform3d X21, Twist vel21, 
      RigidTransform3d initialX21, boolean symmetric) {

      Ju.setZero();

      Ju.m00 = myD.x;
      Ju.m11 = myD.y;
      Ju.m22 = myD.z;
      
      Ju.m33 = myRotD;
      Ju.m44 = myRotD;
      Ju.m55 = myRotD;
   }

   public boolean equals (FrameMaterial mat) {
      if (!(mat instanceof RotAxisFrameMaterial)) {
         return false;
      }
      RotAxisFrameMaterial ram = (RotAxisFrameMaterial)mat;
      if (!myK.equals (ram.myK) ||
          !myD.equals (ram.myD) ||
          myRotK != ram.myRotK ||
          myRotD != ram.myRotD) {
         return false;
      }
      else {
         return super.equals (mat);
      }
   }

   public RotAxisFrameMaterial clone() {
      RotAxisFrameMaterial mat = (RotAxisFrameMaterial)super.clone();
      mat.myK = new Vector3d (myK);
      mat.myD = new Vector3d (myD);
      return mat;
   }
}

