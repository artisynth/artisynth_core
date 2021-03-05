/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.spatialmotion;

import maspack.matrix.AxisAngle;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.util.*;

/**
 * Implements a fixed axis coupling - which is a roll-pitch coupling with no
 * translational constraint and no joint limits.
 */
public class FixedAxisCoupling extends RigidBodyCoupling {

   public static final int X_IDX = 0;
   public static final int Y_IDX = 1;
   public static final int Z_IDX = 2;
   public static final int ROLL_IDX = 3;
   public static final int PITCH_IDX = 4;

   public FixedAxisCoupling() {
      super();
   }

   /**                                                                          
    * Gets the roll-pitch angles for a rotation.                             
    *                                                                           
    * @param angs                                                               
    * returns the angles (roll, pitch) in radians.      
    * @param R                                                                  
    * rotation for which angles should be obtained                              
    * @see #setRollPitch(RotationMatrix3d,double,double)                        
    */
   void getRollPitch (double[] angs, RotationMatrix3d R) {
      angs[0] = Math.atan2 (-R.m01, R.m11);
      angs[1] = Math.atan2 (-R.m20, R.m22);
   }

   /**                                                                          
    * Sets a rotation from roll-pitch angles.
    *
    * @param R return the rotation
    * @param roll roll angle (radians)                         
    * @param pitch pitch angle (radians)                         
    */
   void setRollPitch (RotationMatrix3d R, double roll, double pitch) {
      double sr, sp, cr, cp;

      sr = Math.sin (roll);
      cr = Math.cos (roll);
      sp = Math.sin (pitch);
      cp = Math.cos (pitch);

      R.m00 = cr*cp;
      R.m10 = sr*cp;
      R.m20 = -sp;

      R.m01 = -sr;
      R.m11 = cr;
      R.m21 = 0;

      R.m02 = cr*sp;
      R.m12 = sr*sp;
      R.m22 = cp;         
   }

   // FINISH
   @Override
   public void projectToConstraints (
      RigidTransform3d TGD, RigidTransform3d TCD, VectorNd coords) {
      // same code as for RollPitchConstraint, with skewAngle = 0,
      // useRDC() == false, and no translation restriction
      RotationMatrix3d R = new RotationMatrix3d();
      R.set (TCD.R);
      double yx = R.m01;
      double yy = R.m11;
      double yz = R.m21;

      double r = Math.sqrt (yx*yx + yy*yy); // length of projection into x/y plane
      RotationMatrix3d RX = new RotationMatrix3d(); // rotation to adjust y
      if (r == 0) {
         // unlikely to happen. Just rotate about x by PI/2-skewAngle
         RX.setRotX (Math.PI/2);
      }
      else {
         double ang = Math.atan2 (yz, r);
         Vector3d axis = new Vector3d (yy, -yx, 0);
         RX.setAxisAngle (axis, -ang);
      }
      R.mul (RX, R);
      TGD.R.set (R);

      if (coords != null) {
         TCDToCoordinates (coords, TGD);
      }     
   }

   public void initializeConstraints () {
      addConstraint (BILATERAL|ROTARY);

      addCoordinate (); // x
      addCoordinate (); // y
      addCoordinate (); // z
      addCoordinate (); // roll
      addCoordinate (); // pitch
   }

   // FINISH
   @Override
   public void updateConstraints (
      RigidTransform3d TGD, RigidTransform3d TCD, Twist errC,
      Twist velGD, boolean updateEngaged) {

      double roll = myCoordinates.get(ROLL_IDX).value;
      double cr = Math.cos(roll);
      double sr = Math.sin(roll);

      Vector3d wvel = new Vector3d(); // angular velocity C wrt D, in D
      //wvel.set (velCD.w);
      wvel.transform (TGD.R);

      double dotp = -sr*wvel.x + cr*wvel.y;
      double dotr = wvel.z;

      // constraint to eliminate yaw rotation
      RigidBodyConstraint cinfo = getConstraint(0);
      cinfo.wrenchG.set (0, 0, 0, cr, sr, 0);
      cinfo.dotWrenchG.set (0, 0, 0, -sr*dotr, cr*dotr, 0);
      // transform from D to C
      transformDtoG (cinfo.wrenchG.m, cinfo.dotWrenchG.m, TGD.R, velGD.w);
   }

   public void TCDToCoordinates (VectorNd coords, RigidTransform3d TCD) {

      coords.set(X_IDX, TCD.p.x);
      coords.set(Y_IDX, TCD.p.y);
      coords.set(Z_IDX, TCD.p.z);
      double[] angs = new double[2];
      getRollPitch (angs, TCD.R);
      coords.set (ROLL_IDX, getCoordinate(ROLL_IDX).nearestAngle(angs[0]));
      coords.set (PITCH_IDX, getCoordinate(PITCH_IDX).nearestAngle(angs[1]));
   }

   /**
    * {@inheritDoc}
    */
   public void coordinatesToTCD (
      RigidTransform3d TCD, VectorNd coords) {

      TCD.p.x = coords.get(X_IDX);
      TCD.p.y = coords.get(Y_IDX);
      TCD.p.z = coords.get(Z_IDX);
      double roll = coords.get(ROLL_IDX);
      double pitch = coords.get(PITCH_IDX);
      setRollPitch (TCD.R, roll, pitch);
   }

   public FixedAxisCoupling clone() {
      FixedAxisCoupling copy = (FixedAxisCoupling)super.clone();

      return copy;
   }


}

