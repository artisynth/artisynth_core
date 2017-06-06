/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.InternalErrorException;

/**
 * Denotes an axis-aligned rotation i.e., a rotation in which the new axes
 * are aligned (in either direction) with the original axes. The
 * corresponding rotation matrix elements will therefore consist of only 1,
 * 0, and -1.
 *
 * <p>There are 24 such rotations, each identified by the directions
 * of the new x and y axes expressed with respect to the original axes.
 */
public enum AxisAlignedRotation {

   X_Y (Vector3d.X_UNIT, Vector3d.Y_UNIT),
   X_NZ (Vector3d.X_UNIT, Vector3d.NEG_Z_UNIT),
   X_NY (Vector3d.X_UNIT, Vector3d.NEG_Y_UNIT),
   X_Z (Vector3d.X_UNIT, Vector3d.Z_UNIT),
   NX_Y (Vector3d.NEG_X_UNIT, Vector3d.Y_UNIT),
   NX_NZ (Vector3d.NEG_X_UNIT, Vector3d.NEG_Z_UNIT),
   NX_NY (Vector3d.NEG_X_UNIT, Vector3d.NEG_Y_UNIT),
   NX_Z (Vector3d.NEG_X_UNIT, Vector3d.Z_UNIT), 

   Y_Z (Vector3d.Y_UNIT, Vector3d.Z_UNIT),
   Y_NX (Vector3d.Y_UNIT, Vector3d.NEG_X_UNIT),
   Y_NZ (Vector3d.Y_UNIT, Vector3d.NEG_Z_UNIT),
   Y_X (Vector3d.Y_UNIT, Vector3d.X_UNIT),
   NY_Z (Vector3d.NEG_Y_UNIT, Vector3d.Z_UNIT),
   NY_NX (Vector3d.NEG_Y_UNIT, Vector3d.NEG_X_UNIT),
   NY_NZ (Vector3d.NEG_Y_UNIT, Vector3d.NEG_Z_UNIT),
   NY_X (Vector3d.NEG_Y_UNIT, Vector3d.X_UNIT),

   Z_X (Vector3d.Z_UNIT, Vector3d.X_UNIT),
   Z_NY (Vector3d.Z_UNIT, Vector3d.NEG_Y_UNIT),
   Z_NX (Vector3d.Z_UNIT, Vector3d.NEG_X_UNIT),
   Z_Y (Vector3d.Z_UNIT, Vector3d.Y_UNIT),
   NZ_X (Vector3d.NEG_Z_UNIT, Vector3d.X_UNIT),
   NZ_NY (Vector3d.NEG_Z_UNIT, Vector3d.NEG_Y_UNIT),
   NZ_NX (Vector3d.NEG_Z_UNIT, Vector3d.NEG_X_UNIT),
   NZ_Y (Vector3d.NEG_Z_UNIT, Vector3d.Y_UNIT);

   private Vector3d myX;
   private Vector3d myY;

   AxisAlignedRotation (Vector3d x, Vector3d y) {
      myX = x;
      myY = y;
   }

   public RotationMatrix3d getMatrix() {
      RotationMatrix3d R = new RotationMatrix3d();
      getMatrix (R);
      return R;
   }
   
   public AxisAngle getAxisAngle() {
      AxisAngle axisAng = new AxisAngle();
      getAxisAngle (axisAng);
      return axisAng;
   }
   
   public void getAxisAngle (AxisAngle axisAng) {
      RotationMatrix3d R = getMatrix();
      R.getAxisAngle (axisAng);
   }

   public void getMatrix (RotationMatrix3d R) {
      R.m00 = myX.x;
      R.m10 = myX.y;
      R.m20 = myX.z;

      R.m01 = myY.x;
      R.m11 = myY.y;
      R.m21 = myY.z;

      R.m02 = myX.y*myY.z - myX.z*myY.y;
      R.m12 = myX.z*myY.x - myX.x*myY.z;
      R.m22 = myX.x*myY.y - myX.y*myY.x;
   }

   private static final int X = 0;
   private static final int Y = 1;
   private static final int Z = 2;

   private static final int NX = 3;
   private static final int NY = 4;
   private static final int NZ = 5;

   public static AxisAlignedRotation getNearest (RotationMatrix3d R) {

      // Works by determining which of the x, y, z, -x, -y, or -z unit
      // directions each of the first two columns of R are closest to.

      // xcol and ycol are the first two columns of R
      Vector3d xcol = new Vector3d(R.m00, R.m10, R.m20);
      Vector3d ycol = new Vector3d(R.m01, R.m11, R.m21);

      // start by determining the x, y, or z axis:
      int xmaxIdx = xcol.maxAbsIndex();
      // ensure that xcol and ycol axes are exclusive:
      ycol.set (xmaxIdx, 0);
      int ymaxIdx = ycol.maxAbsIndex();

      // adjust for direction along the axis:
      if (xcol.get (xmaxIdx) < 0) {
         xmaxIdx += 3;
      }
      if (ycol.get (ymaxIdx) < 0) {
         ymaxIdx += 3;
      }

      switch (xmaxIdx) {
         case X: {
            switch (ymaxIdx) {
               case Y: return X_Y;
               case NZ: return X_NZ;
               case NY: return X_NY;
               case Z: return X_Z;
            }
            break;
         }
         case NX: {
            switch (ymaxIdx) {
               case Y: return NX_Y;
               case NZ: return NX_NZ;
               case NY: return NX_NY;
               case Z: return NX_Z;
            }
            break;
         }
         case Y: {
            switch (ymaxIdx) {
               case Z: return Y_Z;
               case NX: return Y_NX;
               case NZ: return Y_NZ;
               case X: return Y_X;
            }
            break;
         }
         case NY: {
            switch (ymaxIdx) {
               case Z: return NY_Z;
               case NX: return NY_NX;
               case NZ: return NY_NZ;
               case X: return NY_X;
            }
            break;
         }
         case Z: {
            switch (ymaxIdx) {
               case X: return Z_X;
               case NY: return Z_NY;
               case NX: return Z_NX;
               case Y: return Z_Y;
            }
            break;
         }
         case NZ: {
            switch (ymaxIdx) {
               case X: return NZ_X;
               case NY: return NZ_NY;
               case NX: return NZ_NX;
               case Y: return NZ_Y;
            }
            break;
         }
      }
      throw new InternalErrorException (
         "Illegal axis combination: xmaxIdx="+xmaxIdx+", ymaxIdx="+ymaxIdx);
   }

   public void transform (Vector3d vr, Vector3d v0) {
      RotationMatrix3d R = new RotationMatrix3d();
      getMatrix (R);
      vr.transform (R, v0);
   }

   public void inverseTransform (Vector3d vr, Vector3d v0) {
      RotationMatrix3d R = new RotationMatrix3d();
      getMatrix (R);
      vr.inverseTransform (R, v0);
   }

}

