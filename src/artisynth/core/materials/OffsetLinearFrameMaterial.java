package artisynth.core.materials;

import maspack.matrix.Matrix6d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;
import maspack.spatialmotion.Twist;
import maspack.spatialmotion.Wrench;

/**
 * A FrameMaterial that is linear under small rotation assumptions and allows
 * separate stiffness and damping along each of the the six degrees of freedom.
 * Its difference from LinearFrameMaterial is that instead of zero, initialX21 
 * will be set as the resting length of the spring.   
 */
public class OffsetLinearFrameMaterial extends FrameMaterial {

   protected Vector3d myK = new Vector3d();
   protected Vector3d myD = new Vector3d();
   protected Vector3d myRotK = new Vector3d();
   protected Vector3d myRotD = new Vector3d();

   public static PropertyList myProps =
      new PropertyList (OffsetLinearFrameMaterial.class, FrameMaterial.class);

   static {
      myProps.add ("stiffness * *", "offset linear spring stiffness", Vector3d.ZERO);
      myProps.add ("damping * *", "offset linear spring damping", Vector3d.ZERO);
      myProps.add ("rotaryStiffness * *", "offset linear spring stiffness", Vector3d.ZERO);
      myProps.add ("rotaryDamping * *", "offset linear spring damping", Vector3d.ZERO);
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

   public OffsetLinearFrameMaterial () {
      this (0, 0, 0, 0);
   }

   public OffsetLinearFrameMaterial (double k, double kr, double d, double dr) {
      setStiffness (k);
      setRotaryStiffness (kr);
      setDamping (d);
      setRotaryDamping (dr);
   }

   public void computeF (
      Wrench wr, RigidTransform3d X21, Twist vel21, 
      RigidTransform3d initialX21) {

      Vector3d p = X21.p;
      Vector3d initialp = initialX21.p;

      // use these matrix entries as small angle approximations to 
      // the rotations about x, y, and z
      double sx =  X21.R.m21;
      double sy = -X21.R.m20;
      double sz =  X21.R.m10;

      wr.f.x = myK.x*(p.x - initialp.x );  
      wr.f.y = myK.y*(p.y - initialp.y );  
      wr.f.z = myK.z*(p.z - initialp.z );  

      wr.m.x = myRotK.x*sx;
      wr.m.y = myRotK.y*sy;
      wr.m.z = myRotK.z*sz;

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
      // use these matrix entries as small angle approximations to 
      // the rotations about x, y, and z
      double sx =  R.m21;
      double sy = -R.m20;
      double sz =  R.m10;

      Jq.m00 = myK.x;
      Jq.m11 = myK.y;
      Jq.m22 = myK.z;

      Jq.m33 =  myRotK.x*R.m11;
      Jq.m44 =  myRotK.y*R.m00;
      Jq.m55 =  myRotK.z*R.m00;

      if (!symmetric) {
         Jq.m34 = -myRotK.x*R.m01;
         Jq.m43 = -myRotK.y*R.m10;
         Jq.m53 = -myRotK.z*R.m20;
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
      if (!(mat instanceof OffsetLinearFrameMaterial)) {
         return false;
      }
      OffsetLinearFrameMaterial olm = (OffsetLinearFrameMaterial)mat;
      if (!myK.equals (olm.myK) ||
          !myD.equals (olm.myD) ||
          !myRotK.equals (olm.myRotK) ||
          !myRotD.equals (olm.myRotD)) {
         return false;
      }
      else {
         return super.equals (mat);
      }
   }

   public OffsetLinearFrameMaterial clone() {
      OffsetLinearFrameMaterial mat = (OffsetLinearFrameMaterial)super.clone();
      mat.myK = new Vector3d (myK);
      mat.myRotK = new Vector3d (myRotK);
      mat.myD = new Vector3d (myD);
      mat.myRotD = new Vector3d (myRotD);
      return mat;
   }
}

