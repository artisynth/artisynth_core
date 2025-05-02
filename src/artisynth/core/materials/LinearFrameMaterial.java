package artisynth.core.materials;

import maspack.matrix.Matrix3d;
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
 */
public class LinearFrameMaterial extends FrameMaterial {

   protected Vector3d myK = new Vector3d();
   protected Vector3d myD = new Vector3d();
   protected Vector3d myRotK = new Vector3d();
   protected Vector3d myRotD = new Vector3d();

   public static final boolean DEFAULT_USE_XYZ_ANGLES = false;
   protected boolean myUseXyzAngles = DEFAULT_USE_XYZ_ANGLES;

   public static PropertyList myProps =
      new PropertyList (LinearFrameMaterial.class, FrameMaterial.class);

   static {
      myProps.add ("stiffness", "linear spring stiffness", Vector3d.ZERO);
      myProps.add ("damping", "linear spring damping", Vector3d.ZERO);
      myProps.add ("rotaryStiffness", "linear spring stiffness", Vector3d.ZERO);
      myProps.add ("rotaryDamping", "linear spring damping", Vector3d.ZERO);
      myProps.add (
         "useXyzAngles",
         "if true, finite xyz rotation angles are used to compute the moment",
         DEFAULT_USE_XYZ_ANGLES);
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

   public boolean getUseXyzAngles () {
      return myUseXyzAngles;
   }

   public void setUseXyzAngles (boolean enable) {
      myUseXyzAngles = enable;
   }

   public LinearFrameMaterial () {
      this (0, 0, 0, 0);
   }

   public LinearFrameMaterial (double k, double kr, double d, double dr) {
      setStiffness (k);
      setRotaryStiffness (kr);
      setDamping (d);
      setRotaryDamping (dr);
   }

   public void computeF (
      Wrench wr, RigidTransform3d X21, Twist vel21, 
      RigidTransform3d initialX21) {

      // translational forces
      Vector3d p = X21.p;

      wr.f.x = myK.x*p.x;
      wr.f.y = myK.y*p.y;
      wr.f.z = myK.z*p.z;

      if (myUseXyzAngles) {
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

         // matrix Hinv maps angular velocity to x-y-z angle speeds
         double hi21 = -sx/cy;
         double hi22 = cx/cy;
         Matrix3d Hinv = new Matrix3d();
         Hinv.set (1, -sy*hi21, -sy*hi22, 0, cx, sx, 0, hi21, hi22);

         // compute generalized force f in angle space
         Vector3d f = new Vector3d();
         f.x = myRotK.x*angs[0];
         f.y = myRotK.y*angs[1];
         f.z = myRotK.z*angs[2];
         // map f to frame moment using transpose of Hinv
         Hinv.mulTranspose (wr.m, f);
      }
      else {
         // use these matrix entries as small angle approximations to 
         // the rotations about x, y, and z
         double sx =  X21.R.m21;
         double sy = -X21.R.m20;
         double sz =  X21.R.m10;

         wr.m.x = myRotK.x*sx;
         wr.m.y = myRotK.y*sy;
         wr.m.z = myRotK.z*sz;
      }

      // damping forces - currently computed using angular velocity instead of
      // angle speeds
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
      Jq.m00 = myK.x;
      Jq.m11 = myK.y;
      Jq.m22 = myK.z;

      // rotational stiffness
      RotationMatrix3d R = X21.R;

      if (myUseXyzAngles) {
         // get the x-y-z angles for computing the rotational forces
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

         // matrix Hinv maps angular velocity to x-y-z angle speeds
         double hi21 = -sx/cy;
         double hi22 = cx/cy;
         Matrix3d Hinv = new Matrix3d();
         Hinv.set (1, -sy*hi21, -sy*hi22, 0, cx, sx, 0, hi21, hi22);

         // compute generalized force f in angle space
         Vector3d f = new Vector3d();
         f.x = myRotK.x*angs[0];
         f.y = myRotK.y*angs[1];
         f.z = myRotK.z*angs[2];

         // compute rotary stiffness from f, dot f, and Hinv:
         Matrix3d Jr = new Matrix3d();
         Jr.transpose (Hinv);
         Jr.mulCols (myRotK);
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
      else {
         Jq.m33 = myRotK.x*R.m11;
         Jq.m44 = myRotK.y*R.m00;
         Jq.m55 = myRotK.z*R.m00;

         if (!symmetric) {
            Jq.m34 = -myRotK.x*R.m01;
            Jq.m43 = -myRotK.y*R.m10;
            Jq.m53 = -myRotK.z*R.m20;
         }
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
      if (!(mat instanceof LinearFrameMaterial)) {
         return false;
      }
      LinearFrameMaterial lm = (LinearFrameMaterial)mat;
      if (!myK.equals (lm.myK) ||
          !myD.equals (lm.myD) ||
          !myRotK.equals (lm.myRotK) ||
          !myRotD.equals (lm.myRotD)) {
         return false;
      }
      else {
         return super.equals (mat);
      }
   }

   public LinearFrameMaterial clone() {
      LinearFrameMaterial mat = (LinearFrameMaterial)super.clone();
      mat.myK = new Vector3d (myK);
      mat.myRotK = new Vector3d (myRotK);
      mat.myD = new Vector3d (myD);
      mat.myRotD = new Vector3d (myRotD);
      return mat;
   }
}

