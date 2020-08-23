package artisynth.core.materials;

import maspack.matrix.Matrix6d;
import maspack.matrix.RigidTransform3d;
import maspack.spatialmotion.Twist;
import maspack.spatialmotion.Wrench;

/**
 * Base material for a FrameSpring.
 */
public abstract class FrameMaterial extends MaterialBase {

   static Class<?>[] mySubClasses = new Class[] {
      LinearFrameMaterial.class,
      RotAxisFrameMaterial.class,                                              
      HeuerOffLinFM.class, 
      NonlinearlyStiffFrameMaterial.class,
      OffsetLinearFrameMaterial.class
   };

   public static Class<?>[] getSubClasses() {
      return mySubClasses;
   }

   protected void notifyHostOfPropertyChange() {
      // stub for future use
   }

   public abstract void computeF (
      Wrench wr, RigidTransform3d X21, Twist vel21, 
      RigidTransform3d initialX21);

   public abstract void computeDFdq (
      Matrix6d Jq, RigidTransform3d X21, Twist vel21, 
      RigidTransform3d initialX21, boolean symmetric);

   public abstract void computeDFdu (
      Matrix6d Ju, RigidTransform3d X21, Twist vel21, 
      RigidTransform3d initialX21, boolean symmetric);

   public boolean equals (FrameMaterial mat) {
      return true;
   }

   public boolean equals (Object obj) {
      if (obj instanceof FrameMaterial) {
         return equals ((FrameMaterial)obj);
      }
      else {
         return false;
      }
   }

   public FrameMaterial clone() {
      FrameMaterial mat = (FrameMaterial)super.clone();
      return mat;
   }
}

