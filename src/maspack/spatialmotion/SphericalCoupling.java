/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.spatialmotion;

import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.util.InternalErrorException;
import maspack.util.NumberFormat;

/**
 * Implements a three DOF spherical coupling. Frames C and D share a common
 * origin and are otherwise free to rotate about each other. Travel restrictions
 * can be placed on this coupling by bounding either the maximum tilt of the z
 * axis, or the maximum total rotation. Bounds on the maximum total rotation can
 * be weighted by the rotation direction, so that the maximum rotations about
 * the x, y, and z axes are different.
 */
public class SphericalCoupling extends RigidBodyCoupling {

   public boolean applyEuler = true;
   private double myMaxTilt = Math.PI;

   private double myMaxRotX = Math.PI;
   private double myMaxRotY = Math.PI;
   private double myMaxRotZ = Math.PI;

   public static final int TILT_LIMIT = 0x01;
   public static final int ROTATION_LIMIT = 0x02;

   private int myRangeType = 0;

   public int getRangeType() {
      return myRangeType;
   }

   public void setRangeType(int type) {
      if (type == TILT_LIMIT ||
         type == ROTATION_LIMIT ||
         type == 0) {
         myRangeType = type;
      }
      else {
         throw new IllegalArgumentException(
            "Illegal range type " + NumberFormat.formatHex(type));
      }
   }

   /**
    * Sets the maximum total rotation for this coupling. This will only have an
    * effect if the current range type is {@link #ROTATION_LIMIT}. The maximum
    * must be greater than 0, and will be clipped to the range (0, PI]. Setting
    * the maximum to PI will remove the range restriction.
    * 
    * @param max
    * maximum total rotation
    */
   public void setMaximumRotation(double max) {
      setMaximumRotation(max, max, max);
   }

   /**
    * Sets the maximum total rotation about the x, y, and z axes. This will only
    * have an effect if the current range type is {@link #ROTATION_LIMIT}. For a
    * general orientation, the maximum rotation angle will be computed from the
    * length of the rotation axis weighted by the maximum values along each
    * principal axis.
    * 
    * <p>
    * Each maximum must be greater than 0, and will be clipped to the range (0,
    * PI]. Setting all maximums to PI will remove the range restriction.
    * 
    * @param maxx
    * maximum total rotation about x
    * @param maxy
    * maximum total rotation about y
    * @param maxz
    * maximum total rotation about z
    */
   public void setMaximumRotation(double maxx, double maxy, double maxz) {
      if (maxx <= 0 || maxy <= 0 || maxz <= 0) {
         throw new IllegalArgumentException(
            "maximum rotations must be positive");
      }
      myMaxRotX = clip(maxx, 0, Math.PI);
      myMaxRotY = clip(maxy, 0, Math.PI);
      myMaxRotZ = clip(maxz, 0, Math.PI);
   }

   /**
    * Returns the maximum total rotation for each axis.
    * 
    * @param maxRot
    * returns the maximum rotation for each axis
    */
   public void getMaximumRotation(double[] maxRot) {
      maxRot[0] = myMaxRotX;
      maxRot[1] = myMaxRotY;
      maxRot[2] = myMaxRotZ;
   }

   /**
    * Returns the maximum total rotation for this coupling. If separate maximum
    * rotations have been specified for each axis, then the maximum rotation for
    * the x axis is returned.
    * 
    * @return the maximum rotation for this coupling, or for the x axis if
    * separate limits have been specified for each axis
    */
   public double getMaximumRotation() {
      return myMaxRotX;
   }

   /**
    * Sets the maximum tilt for this coupling. The tilt is the angle between the
    * initial and current z axis. This will only have an effect if the current
    * range type is {@link #TILT_LIMIT}. The maximum must be greater than 0, and
    * will be clipped to the range (0, PI].
    * 
    * @param max
    * maximum value for theta
    */
   public void setMaximumTilt(double max) {
      if (max <= 0) {
         throw new IllegalArgumentException("maximum must be greater than 0");
      }
      myMaxTilt = clip(max, 0, Math.PI);
   }

   /**
    * Returns the maximum value of theta for this revolute coupling.
    * 
    * @return maximum value for theta
    */
   public double getMaximumTilt() {
      return myMaxTilt;
   }

   public SphericalCoupling () {
      super();
   }

//   public SphericalCoupling (RigidTransform3d TCA, RigidTransform3d XDB) {
//      this();
//      setXDB(XDB);
//      setXFA(TCA);
//   }

   @Override
   public void projectToConstraints(
      RigidTransform3d TGD, RigidTransform3d TCD, VectorNd coords) {
      TGD.R.set(TCD.R);
      TGD.p.setZero();
   }

   private static double DOUBLE_PREC = 2.220446049250313e-16;
   private static double EPSILON = 10 * DOUBLE_PREC;

   public void initializeConstraints() {
      addConstraint (BILATERAL | LINEAR, new Wrench(1, 0, 0, 0, 0, 0));
      addConstraint (BILATERAL | LINEAR, new Wrench(0, 1, 0, 0, 0, 0));
      addConstraint (BILATERAL | LINEAR, new Wrench(0, 0, 1, 0, 0, 0));
      addConstraint (ROTARY);
//      addConstraint (ROTARY);
//      addConstraint (ROTARY);
   }

   @Override
   public void updateConstraints(
      RigidTransform3d TGD, RigidTransform3d TCD, Twist errC,
      Twist velGD, boolean updateEngaged) {

      RigidBodyConstraint cons = getConstraint(3); // limit constraint
      if (myRangeType == TILT_LIMIT) {
         Vector3d utilt = new Vector3d();

         // Tilt axis is is z(C) x z(D).
         // In C coordinates, z(C) = (0,0,1) and z(D) = last row of TGD.R.
         // In D coordinates, z(D) = (0,0,1) and z(C) = last col of TGD.R.

         utilt.set(TGD.R.m12, -TGD.R.m02, 0); // D coordinates
         // utilt.set (-TGD.R.m21, TGD.R.m20, 0); // in C coordinates
         double ulen = utilt.norm();
         double theta = 0;
         if (ulen > 1e-8) {
            theta = Math.atan2(ulen, TGD.R.m22);
            utilt.scale(1 / ulen);
         }
         if (updateEngaged) {
            updateEngaged (cons, theta, -INF, myMaxTilt, velGD);
         }
         if (cons.engaged != 0) {
            cons.distance = myMaxTilt - theta;
            utilt.inverseTransform(TGD.R);
            cons.wrenchG.set(0, 0, 0, -utilt.x, -utilt.y, -utilt.z);
            cons.dotWrenchG.setZero();
         }
      }
      else if (myRangeType == ROTATION_LIMIT) {
         Vector3d u = new Vector3d();
         // note: angle return by getAxisAngle is always positive
         double ang = TGD.R.getAxisAngle(u);
         u.normalize(); // paranoid
         Vector3d a =
            new Vector3d(u.x * myMaxRotX, u.y * myMaxRotY, u.z * myMaxRotZ);

         double maxAng = a.norm();
         if (updateEngaged) {
            updateEngaged (cons, ang, -INF, maxAng, velGD);
         }
         if (cons.engaged != 0) {
            cons.distance = maxAng - ang;
            u.x /= myMaxRotX;
            u.y /= myMaxRotY;
            u.z /= myMaxRotZ;
            u.normalize();
            cons.wrenchG.set(0, 0, 0, u.x, u.y, u.z);
            cons.dotWrenchG.setZero(); // TODO set this
         }
      }
      else if (myRangeType != 0) {
         throw new InternalErrorException(
            "Unimplemented range limits " + NumberFormat.formatHex(myRangeType));
      }
   }

   /**
    * {@inheritDoc}
    */
   public void coordinatesToTCD (
      RigidTransform3d TCD, VectorNd coords) {
      TCD.setIdentity();
   }

   public double getTilt (RigidTransform3d TGD) {
      if (TGD != null) {
         // on entry, TGD is set to TCD. It is then projected to TGD
         projectAndUpdateCoordinates (TGD, TGD);
         double s = Math.hypot (TGD.R.m12, TGD.R.m02);
         double c = TGD.R.m22;
         return Math.atan2 (s, c);
      }
      else {
         return 0;
      }
   }


}
