package maspack.matrix;

import maspack.util.*;

/**
 * Describes some common ways to represent 3D rotations.
 */
public enum RotationRep {
   /**
    * Successive rotations, in radians, about the z-y-x axes.
    */
   ZYX(3,false),

   /**
    * Successive rotations, in degress, about the z-y-x axes.
    */
   ZYX_DEG(3,true),

   /**
    * Successive rotations, in radians, about the x-y-z axes.
    */
   XYZ(3,false),

   /**
    * Successive rotations, in degrees, about the x-y-z axes.
    */
   XYZ_DEG(3,true),

   /**
    * Axis-angle representation (u, ang), with the angle given in
    * radians. The axis u does not need to be normalized.
    */
   AXIS_ANGLE(4,false),

   /**
    * Axis-angle representation (u, ang), with the angle given in
    * radians. The axis u does not need to be normalized.
    */
   AXIS_ANGLE_DEG(4,true),

   /**
    * Unit quaternion.
    */
   QUATERNION(4,false);

   int mySize; // size of the subvector
   boolean myUsesDegrees; // angles are represented in degrees

   RotationRep (int size, boolean usesDegrees) {
      mySize = size;
      myUsesDegrees = usesDegrees;
   }

   /**
    * Queries the number of doubles required for this RotationRep.
    *
    * @return numerical size of this RotationRep
    */
   public int size() {
      return mySize;
   }
   
   /**
    * Queries whethar this RotationRep represents angles in degrees.
    *
    * @return {@code} true if angles are represented in degrees
    */
   public boolean usesDegrees() {
      return myUsesDegrees;
   }
   
   /**
    * Returns appropriate names for the numeric values of this RotationRep.
    *
    * @return names for the numeric fields
    */
   public String[] getFieldNames() {
      switch (this) {
         case ZYX: {
            return new String[] {"rotZ", "rotY", "rotX"};
         }
         case ZYX_DEG: {
            return new String[] {"rotZ", "rotY", "rotX"};
         }
         case XYZ: {
            return new String[] {"rotX", "rotY", "rotZ"};
         }
         case XYZ_DEG: {
            return new String[] {"rotX", "rotY", "rotZ"};
         }
         case AXIS_ANGLE: {
            return new String[] {"ux", "uy", "uz", "ang"};
         }
         case AXIS_ANGLE_DEG: {
            return new String[] {"ux", "uy", "uz", "deg"};
         }
         case QUATERNION: {
            return new String[] {"s", "vx", "vy", "vz"};
         }
         default: {
            throw new UnsupportedOperationException (
               "Unimplemented rotation representation " + this);
         }
      }
   }

   /** 
    * Given an angle <code>ang</code>, find an equivalent angle that is within
    * +/- PI of a given reference angle <code>ref</code>.
    * 
    * @param ang initial angle (radians)
    * @param ref reference angle (radians)
    * @return angle equivalent to <code>ang</code> within +/- PI
    * of <code>ref</code>.
    */
   public static final double nearestAngle (double ang, double ref) {
      while (ang - ref > Math.PI) {
         ang -= 2*Math.PI;
      }
      while (ang - ref < -Math.PI) {
         ang += 2*Math.PI;
      }
      return ang;
   }   

   /** 
    * Find the canocical representation of an angle <code>ang</code>,
    * such that it is in the range {@code (-PI, PI]}.
    * 
    * @param ang initial angle (radians)
    * @return canonocal representation of {@code ang}.
    */
   public static final double canonicalAngle (double ang) {
      while (ang > Math.PI) {
         ang -= 2*Math.PI;
      }
      while (ang <= -Math.PI) {
         ang += 2*Math.PI;
      }
      return ang;
   }   

}

