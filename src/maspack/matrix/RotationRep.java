package maspack.matrix;

/**
 * Describes some common ways to represent 3D rotations.
 */
public enum RotationRep {
   /**
    * Successive rotations, in radians, about the z-y-x axes.
    */
   ZYX(3),

   /**
    * Successive rotations, in degress, about the z-y-x axes.
    */
   ZYX_DEG(3),

   /**
    * Successive rotations, in radians, about the x-y-z axes.
    */
   XYZ(3),

   /**
    * Successive rotations, in degrees, about the x-y-z axes.
    */
   XYZ_DEG(3),

   /**
    * Axis-angle representation (u, ang), with the angle given in
    * radians. The axis u does not need to be normalized.
    */
   AXIS_ANGLE(4),

   /**
    * Axis-angle representation (u, ang), with the angle given in
    * radians. The axis u does not need to be normalized.
    */
   AXIS_ANGLE_DEG(4),

   /**
    * Unit quaternion.
    */
   QUATERNION(4);

   int mySize; // size of the subvector

   RotationRep (int size) {
      mySize = size;
   }

   public int size() {
      return mySize;
   }
   
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
}
