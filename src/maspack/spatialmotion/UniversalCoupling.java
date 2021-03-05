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

/**
 * Implements a two DOF roll-pitch coupling. Frames C and D share a common
 * origin, and the transform from C to D is given by a <code>roll</code>
 * rotation about the z axis, followed by a <code>pitch</code> rotation about
 * the subsequent <code>pitch</code> axis. The pitch axis is usually the
 * post-roll y-axis, but can be adjusted via the skewAngle to lie along some
 * other direction in the post-roll z/y plane. The angle between the roll and
 * pitch axes is given by PI/2 - skewAngle.
 *
 * <p>See the section "Skewed roll-pitch joint" in the mechmodel notes.
 */
public class UniversalCoupling extends RigidBodyCoupling {

   public static final int ROLL_IDX = 0;
   public static final int PITCH_IDX = 1;

   // 1 => use RCD to determine the angles, -1 => use RDC
   protected int myAngleSign = 1;

   // skew angle attributes
   private double mySkewAngle = 0;   
   private double mySa = 0; // sin of the skew angle
   private double myCa = 1; // cos of the skew angle
   private double myTa = 0; // tan of the skew angle
   // skew angle rotation matrix, or null if the skew angle = 0
   private RotationMatrix3d myRa = null;

   public UniversalCoupling() {
      super();
   }

   public UniversalCoupling (double skewAngle) {
      super();
      if (skewAngle <= -Math.PI || skewAngle >= Math.PI) {
         throw new IllegalArgumentException (
            "Skew angle must lie within the open interval (-PI,PI)");
      }
      mySa = Math.sin(skewAngle);
      myCa = Math.cos(skewAngle);
      myTa = mySa/myCa;
      if (skewAngle != 0) {
         myRa = new RotationMatrix3d();
         myRa.setRotX (skewAngle);
      }
      mySkewAngle = skewAngle;
   }

   public void setSkewAngle (double skewAngle) {
      if (skewAngle <= -Math.PI || skewAngle >= Math.PI) {
         throw new IllegalArgumentException (
            "Skew angle must lie within the open interval (-PI,PI)");
      }
      mySa = Math.sin(skewAngle);
      myCa = Math.cos(skewAngle);
      myTa = mySa/myCa;
      if (skewAngle != 0) {
         myRa = new RotationMatrix3d();
         myRa.setRotX (skewAngle);
      }
      mySkewAngle = skewAngle;
   }

   public double getSkewAngle () {
      return mySkewAngle;
   }

   public boolean getUseRDC() {
      return myAngleSign == -1;
   }

   public void setUseRDC (boolean enable) {
      myAngleSign = (enable ? -1 : 1);
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
      if (mySkewAngle == 0) {
         // no skew                                                             
         angs[0] = Math.atan2 (-R.m01, R.m11);
         angs[1] = Math.atan2 (-R.m20, R.m22);
      }
      else {
         // assumes |myCa| is not too small ...                                 
         angs[1] = Math.atan2 (-R.m20, (R.m22-mySa*mySa)/myCa);
         double sp = Math.sin(angs[1]);
         double cp = Math.cos(angs[1]);
         double vp = 1-cp;
         angs[0] = Math.atan2 (
            -R.m01*(vp*myCa*myCa + cp) - R.m00*mySa*sp - myCa*R.m02*mySa*vp,
            cp*R.m00 + myCa*R.m02*sp - R.m01*mySa*sp);
      }
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

      if (mySkewAngle == 0) { 
         // no skew
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
      else {
         double vp = 1 - cp;

         R.m00 = cp*cr - mySa*sp*sr;
         R.m10 = cp*sr + mySa*cr*sp;
         R.m20 = -myCa*sp;

         double vcp = vp*myCa*myCa + cp;

         R.m01 = -sr*vcp - mySa*cr*sp;
         R.m11 = cr*vcp - mySa*sp*sr;
         R.m21 = myCa*mySa*vp;

         R.m02 = myCa*(cr*sp - mySa*sr*vp);
         R.m12 = myCa*(sr*sp + mySa*cr*vp);
         R.m22 = vp*mySa*mySa + cp;
      }      
   }

   // FINISH
   @Override
   public void projectToConstraints (
      RigidTransform3d TGD, RigidTransform3d TCD, VectorNd coords) {

      // In the non-skew case, projection is done on the matrix
      //
      // R = RDC (if getUseRDC() == true) or R = RCD
      //
      // In the skew case, R is modified by
      //
      // R = R * inv(Ra)
      //
      // where Ra is a rotation about the x axis by skewAngle.

      RotationMatrix3d R = new RotationMatrix3d();
      if (getUseRDC()) {
         R.transpose (TCD.R); // RDC is the transpose of RCD
      }
      else {
         R.set (TCD.R);
      }
      if (myRa != null) {
         R.mul (myRa);
      }
      // Now ensure that the y axis (2nd column) of R forms an angle
      // 'skewAngle' with respect to the x/y plane. If it does not, apply a
      // rotation to bring it there.

      double yx = R.m01;
      double yy = R.m11;
      double yz = R.m21;

      double r = Math.sqrt (yx*yx + yy*yy); // length of projection into x/y plane
      RotationMatrix3d RX = new RotationMatrix3d(); // rotation to adjust y
      if (r == 0) {
         // unlikely to happen. Just rotate about x by PI/2-skewAngle
         RX.setRotX (Math.PI/2-mySkewAngle);
      }
      else {
         double ang = Math.atan2 (yz, r);
         Vector3d axis = new Vector3d (yy, -yx, 0);
         RX.setAxisAngle (axis, mySkewAngle-ang);
      }
      R.mul (RX, R);
      // now transform back: RGD = (R Ra)^T
      if (myRa != null) {
         R.mulInverseRight (R, myRa);
      }
      if (getUseRDC()) {
         TGD.R.transpose (R);
      }
      else {
         TGD.R.set (R);
      }

      // project translation
      TGD.p.setZero();
      if (coords != null) {
         TCDToCoordinates (coords, TGD);
      }     
   }

   public void initializeConstraints () {
      addConstraint (BILATERAL|LINEAR, new Wrench(1, 0, 0, 0, 0, 0));
      addConstraint (BILATERAL|LINEAR, new Wrench(0, 1, 0, 0, 0, 0));
      addConstraint (BILATERAL|LINEAR, new Wrench(0, 0, 1, 0, 0, 0));
      addConstraint (BILATERAL|ROTARY);
      addConstraint (ROTARY);
      addConstraint (ROTARY);

      addCoordinate (-Math.PI, Math.PI, 0, getConstraint(5));
      addCoordinate (-Math.PI, Math.PI, 0, getConstraint(4));
   }

   // FINISH
   @Override
   public void updateConstraints (
      RigidTransform3d TGD, RigidTransform3d TCD, Twist errC,
      Twist velGD, boolean updateEngaged) {

      CoordinateInfo rollCoord = myCoordinates.get(ROLL_IDX);
      CoordinateInfo pitchCoord = myCoordinates.get(PITCH_IDX);

      double roll = rollCoord.value;
      double pitch = pitchCoord.value;
      double cr = Math.cos(roll);
      double sr = Math.sin(roll);
      double cp = Math.cos(pitch);
      double sp = Math.sin(pitch);

      Vector3d wvel = new Vector3d(); // angular velocity in angle frame
      //wvel.set (velCD.w);
      if (getUseRDC()) {
         wvel.negate();
      }
      else {
         wvel.transform (TGD.R);
      }
      double dotp = (-sr*wvel.x + cr*wvel.y)/myCa;
      double dotr = wvel.z - mySa*dotp;

      // constraint to eliminate yaw rotation
      RigidBodyConstraint cinfo = getConstraint(3);
      cinfo.wrenchG.set (0, 0, 0, cr, sr, 0);
      cinfo.dotWrenchG.set (0, 0, 0, -sr*dotr, cr*dotr, 0);
      if (!getUseRDC()) {
         // transform from D to C
         transformDtoG (cinfo.wrenchG.m, cinfo.dotWrenchG.m, TGD.R, velGD.w);
      }

      // enforce roll limits
      cinfo = rollCoord.limitConstraint;
      if (cinfo.engaged != 0) {
         cinfo.wrenchG.set (0, 0, 0, myTa*sr, -myTa*cr, 1);
         cinfo.dotWrenchG.set (0, 0, 0, myTa*cr*dotr, myTa*sr*dotr, 0);
         if (getUseRDC()) {
            // negate wrenches
            cinfo.wrenchG.negate();
            cinfo.dotWrenchG.negate();
         }
         else {
            // transform from D to C
            transformDtoG (cinfo.wrenchG.m, cinfo.dotWrenchG.m, TGD.R, velGD.w);
         }
      }
      // enforce pitch limits
      cinfo = pitchCoord.limitConstraint;
      if (cinfo.engaged != 0) {
         cinfo.wrenchG.set (0, 0, 0, -sr/myCa, cr/myCa, 0);
         cinfo.dotWrenchG.set (0, 0, 0, -cr/myCa*dotr, -sr/myCa*dotr, 0);
         if (getUseRDC()) {
            // negate wrenches
            cinfo.wrenchG.negate();
            cinfo.dotWrenchG.negate();
         }
         else {
            // transform from D to C
            transformDtoG (cinfo.wrenchG.m, cinfo.dotWrenchG.m, TGD.R, velGD.w);
         }
      }
   }

   public void TCDToCoordinates (VectorNd coords, RigidTransform3d TCD) {
      double[] angs = new double[2];
      RotationMatrix3d R = new RotationMatrix3d();
      if (getUseRDC()) {
         R.transpose(TCD.R);
      }
      else {
         R.set (TCD.R);
      }
      getRollPitch (angs, R);
      coords.set (ROLL_IDX, getCoordinate(ROLL_IDX).nearestAngle(angs[0]));
      coords.set (PITCH_IDX, getCoordinate(PITCH_IDX).nearestAngle(angs[1]));
   }

   public void coordinatesToTCD (
      RigidTransform3d TCD, double roll, double pitch) {

      TCD.p.setZero();
      RotationMatrix3d R = new RotationMatrix3d();
      setRollPitch (R, roll, pitch);
      if (getUseRDC()) {
         TCD.R.transpose (R);
      }
      else {
         TCD.R.set (R);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void coordinatesToTCD (
      RigidTransform3d TCD, VectorNd coords) {

      coordinatesToTCD (TCD, coords.get(0), coords.get(1));
   }

   public UniversalCoupling clone() {
      UniversalCoupling copy = (UniversalCoupling)super.clone();

      if (myRa != null) {
         copy.myRa = new RotationMatrix3d(myRa);
      }
      return copy;
   }


}

