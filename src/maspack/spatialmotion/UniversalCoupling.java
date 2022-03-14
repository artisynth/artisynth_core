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
 * Implements a two DOF universal coupling, parameterized by roll and pitch
 * angles. These angles describe a sequence of intrinsic rotations about either
 * the Z-Y or the X-Y axes, depending on the axis settings as specified by
 * {@link #setAxes} and {@link #getAxes}. Frames C and D share a common origin,
 * while the orientation of C with respect to D is given by the roll-pitch
 * rotation. While the pitch axis is usually the (post-roll) Y axis, as
 * described above, it can be adjusted via a skewAngle to lie along some other
 * direction in the post-roll Z-Y (or X-Y) plane. The angle between the roll
 * and pitch axes is given by PI/2 - skewAngle.
 *
 * <p>See the section "Skewed roll-pitch joint" in the mechmodel notes.
 */
public class UniversalCoupling extends RigidBodyCoupling {

   public static final int ROLL_IDX = 0;
   public static final int PITCH_IDX = 1;

   public enum AxisSet {
      ZY,
      XY;

      /**
       * Same as valueOf but returns null instead of throwing an exception if
       * the value is unrecognized.
       */
      static public AxisSet fromString (String str) {
         try {
            return valueOf(str);
         }
         catch (Exception e) {
            return null;
         }
      }
   }

   // 1 => use RCD to determine the angles, -1 => use RDC
   protected int myAngleSign = 1;
   protected AxisSet myAxes = AxisSet.ZY;

   // skew angle attributes
   private double mySkewAngle = 0;   
   private double mySa = 0; // sin of the skew angle
   private double myCa = 1; // cos of the skew angle
   private double myTa = 0; // tan of the skew angle
   // skew angle rotation matrix, or null if the skew angle = 0
   private RotationMatrix3d myRa = null;

   public AxisSet getAxes() {
      return myAxes;
   }

   public void setAxes (AxisSet axes) {
      myAxes = axes;
   }

   public UniversalCoupling() {
      this (0, AxisSet.ZY);
   }

   public UniversalCoupling (double skewAngle) {
      this (skewAngle, AxisSet.ZY);
   }

   public UniversalCoupling (double skewAngle, AxisSet axes) {
      setAxes (axes);
      setSkewAngle (skewAngle);
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
         if (myAxes == AxisSet.ZY) {
            myRa.setRotX (skewAngle);
         }
         else {
            myRa.setRotZ (-skewAngle);
         }
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
         if (myAxes == AxisSet.ZY) {
            angs[0] = Math.atan2 (-R.m01, R.m11);
            angs[1] = Math.atan2 (-R.m20, R.m22);
         }
         else { // axes == AxisSet.XY
            angs[0] = Math.atan2 (R.m21, R.m11);
            angs[1] = Math.atan2 (R.m02, R.m00);
         }
      }
      else {
         // assumes |myCa| is not too small ...
         if (myAxes == AxisSet.ZY) {
            angs[1] = Math.atan2 (-R.m20, (R.m22-mySa*mySa)/myCa);
            double sp = Math.sin(angs[1]);
            double cp = Math.cos(angs[1]);
            double vp = 1-cp;
            angs[0] = Math.atan2 (
               //-R.m01*(vp*myCa*myCa + cp) - R.m00*mySa*sp - myCa*R.m02*mySa*vp,
               R.m10*cp - R.m11*mySa*sp + R.m12*myCa*sp,
               R.m00*cp - R.m01*mySa*sp + R.m02*myCa*sp);
         }
         else { // axes == AxisSet.XY
            angs[1] = Math.atan2 (R.m02, (R.m00-mySa*mySa)/myCa);
            double sp = Math.sin(angs[1]);
            double cp = Math.cos(angs[1]);
            double vp = 1-cp;
            angs[0] = Math.atan2 (
                R.m10*myCa*sp - R.m11*mySa*sp - R.m12*cp,
               -R.m20*myCa*sp + R.m21*mySa*sp + R.m22*cp);
         }
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
         if (myAxes == AxisSet.ZY) {
            R.m00 = cr*cp;
            R.m01 = -sr;
            R.m02 = cr*sp;

            R.m10 = sr*cp;
            R.m11 = cr;
            R.m12 = sr*sp;

            R.m20 = -sp;
            R.m21 = 0;
            R.m22 = cp;         
         }
         else { // axes == AxisSet.XY 
            R.m00 = cp;
            R.m01 = 0;
            R.m02 = sp;

            R.m10 = sp*sr;
            R.m11 = cr;
            R.m12 = -cp*sr;

            R.m20 = -cr*sp;
            R.m21 = sr;
            R.m22 = cp*cr;         
         }
      }
      else {
         double vp = 1 - cp;
         double beta = myCa*myCa + mySa*mySa*cp;

         if (myAxes == AxisSet.ZY) {
            R.m00 = cp*cr - mySa*sp*sr;
            R.m01 = -sr*beta - mySa*cr*sp;
            R.m02 = myCa*(cr*sp - mySa*sr*vp);

            R.m10 = cp*sr + mySa*cr*sp;
            R.m11 = cr*beta - mySa*sp*sr;
            R.m12 = myCa*(sr*sp + mySa*cr*vp);

            R.m20 = -myCa*sp;
            R.m21 = myCa*mySa*vp;
            R.m22 = vp*mySa*mySa + cp;
         }
         else { // axes == AxisSet.XY
            R.m00 = cp*myCa*myCa + mySa*mySa;
            R.m01 = myCa*mySa*vp;
            R.m02 = myCa*sp;

            R.m10 = myCa*(sp*sr + cr*mySa*vp);
            R.m11 = cr*beta - mySa*sr*sp;
            R.m12 = -sr*cp - cr*mySa*sp;

            R.m20 = myCa*(mySa*sr*vp - cr*sp);
            R.m21 = sr*beta + mySa*cr*sp;
            R.m22 = cr*cp - sr*mySa*sp;
         }
      }      
   }

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
         // If there is a skew angle, transform by Ra so that the
         // y axis now corresponds to the pitch axis
         R.mul (myRa);
      }
      // Ensure that the y axis (2nd column) of R forms an angle 'skewAngle'
      // with respect to the x/y plane (if myAxes == ZY) or the z/y plane (if
      // myAxes == XY). If it does not, apply a rotation to bring it there.
      double yx = R.m01;
      double yy = R.m11;
      double yz = R.m21;
      // rotation to correct pitch axis:
      RotationMatrix3d RC = new RotationMatrix3d(); 
      if (myAxes == AxisSet.ZY) {
         double r = Math.sqrt(yx*yx+yy*yy); // length of projection into x/y plane
         if (r == 0) {
            // unlikely to happen. Just rotate about x by PI/2-skewAngle
            RC.setRotX (Math.PI/2-mySkewAngle);
         }
         else {
            double ang = Math.atan2 (yz, r);
            Vector3d axis = new Vector3d (yy, -yx, 0);
            RC.setAxisAngle (axis, mySkewAngle-ang);
         }
         R.mul (RC, R);
      }
      else {
         double r = Math.sqrt(yy*yy+yz*yz); // length of projection into z/y plane
         if (r == 0) {
            // unlikely to happen. Just rotate about z by PI/2-skewAngles
            RC.setRotZ (Math.PI/2-mySkewAngle);
         }
         else {
            double ang = Math.atan2 (yx, r);
            Vector3d axis = new Vector3d (0, yz, -yy);
            RC.setAxisAngle (axis, mySkewAngle-ang);
         }
         R.mul (RC, R);
      }
      if (myRa != null) {
         // If there is a skew angle, transform back: R = R Ra^T
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

   @Override
   public void updateConstraints (
      RigidTransform3d TGD, RigidTransform3d TCD, Twist errC,
      Twist velGD, boolean updateEngaged) {

      CoordinateInfo rollCoord = myCoordinates.get(ROLL_IDX);
      CoordinateInfo pitchCoord = myCoordinates.get(PITCH_IDX);

      double roll = rollCoord.getValue();
      double pitch = pitchCoord.getValue();
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
      double dotp, dotr;
      if (myAxes == AxisSet.ZY) {
         dotp = (-sr*wvel.x + cr*wvel.y)/myCa;
         dotr = wvel.z - mySa*dotp;
      }
      else { // axes == AxisSet.XY
         dotp = (cr*wvel.y + sr*wvel.z)/myCa;
         dotr = wvel.x - mySa*dotp;
      }

      // constraint to eliminate yaw rotation
      RigidBodyConstraint cinfo = getConstraint(3);
      if (myAxes == AxisSet.ZY) {
         cinfo.wrenchG.set (0, 0, 0, cr, sr, 0);
         cinfo.dotWrenchG.set (0, 0, 0, -sr*dotr, cr*dotr, 0);
      }
      else { // axes == AxisSet.XY
         cinfo.wrenchG.set (0, 0, 0, 0, -sr, cr);
         cinfo.dotWrenchG.set (0, 0, 0, 0, -cr*dotr, -sr*dotr);
      }
      if (!getUseRDC()) {
         // transform from D to C
         transformDtoG (cinfo.wrenchG.m, cinfo.dotWrenchG.m, TGD.R, velGD.w);
      }

      // enforce roll limits
      cinfo = rollCoord.limitConstraint;
      if (cinfo.engaged != 0) {
         if (myAxes == AxisSet.ZY) {
            cinfo.wrenchG.set (0, 0, 0, myTa*sr, -myTa*cr, 1);
            cinfo.dotWrenchG.set (0, 0, 0, myTa*cr*dotr, myTa*sr*dotr, 0);
         }
         else { // axes == AxisSet.XY
            cinfo.wrenchG.set (0, 0, 0, 1, -myTa*cr, -myTa*sr);
            cinfo.dotWrenchG.set (0, 0, 0, 0, myTa*sr*dotr, -myTa*cr*dotr);
         }
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
         if (myAxes == AxisSet.ZY) {
            cinfo.wrenchG.set (0, 0, 0, -sr/myCa, cr/myCa, 0);
            cinfo.dotWrenchG.set (0, 0, 0, -cr/myCa*dotr, -sr/myCa*dotr, 0);
         }
         else { // axes == AxisSet.XY
            cinfo.wrenchG.set (0, 0, 0, 0, cr/myCa, sr/myCa);
            cinfo.dotWrenchG.set (0, 0, 0, 0, -sr/myCa*dotr, cr/myCa*dotr);
         }
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
      coords.set (ROLL_IDX, getCoordinateInfo(ROLL_IDX).nearestAngle(angs[0]));
      coords.set (PITCH_IDX, getCoordinateInfo(PITCH_IDX).nearestAngle(angs[1]));
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

