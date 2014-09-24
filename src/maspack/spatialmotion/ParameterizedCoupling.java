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
import maspack.spatialmotion.projections.ProjectedCurve3D;

/**
 * Experimental coupling based on projected 2D curves
 */
public class ParameterizedCoupling extends RigidBodyCoupling {

   public static double EPSILON = 1e-10;
   
   public enum LimitType {
      NONE, ROLL_CURVE, CURVE, ROLL, FIXED
   }
   
   private double myMinRoll = -Math.PI;
   private double myMaxRoll = Math.PI;
   private ProjectedCurve3D myCurve;
  
   LimitType myLimitType = LimitType.NONE;

   
   public LimitType getLimitType() {
      return myLimitType;
   }
   
   private void setLimitType(LimitType type) {
      checkConstraintStorage();
      myLimitType = type;
      initializeConstraintInfo(myConstraintInfo);
   }
   
   /**
    * Sets the minimum and maximum values for the coupling's roll angle (in
    * radians). These only have an effect if the current range type is
    * {@link LimitType#ROLL} or {@link LimitType#ROLL_CURVE}.
    * 
    * @param min
    * Minimum roll angle
    * @param max
    * Maximum roll angle
    */
   public void setRollRange(double min, double max) {
      if (min > max) {
         throw new IllegalArgumentException("min exceeds max");
      }
      myMinRoll = min; // clip(min, -Math.PI, Math.PI);
      myMaxRoll = max; // clip(max, -Math.PI, Math.PI);
   }

   /**
    * Gets the minimum and maximum values for the coupling's roll angle (in
    * radians).
    * 
    * @param minmax
    * used to return the minimum and maximum values
    * @see #setRollRange
    */
   public void getRollRange(double[] minmax) {
      minmax[0] = myMinRoll;
      minmax[1] = myMaxRoll;
   }
   
   public void setLimitCurve(ProjectedCurve3D curve) {
      myCurve = curve;
   }
   
   public ProjectedCurve3D getLimitCurve() {
      return myCurve;
   }
   
   public ParameterizedCoupling (LimitType type) {
      super();
      setLimitType(type);
   }

//   public ParameterizedCoupling (RigidTransform3d TCA, RigidTransform3d XDB, LimitType type)
//   {
//      this(type);
//      setXDB(XDB);
//      setXFA(TCA);
//   }

   @Override
   public int maxUnilaterals() {
      
      switch(myLimitType) {
         case NONE:
            return 3;
         case ROLL_CURVE:
            return 3;
         case CURVE:
            return 2;
         case ROLL:
            return 1;
         case FIXED:
      }
      return 0;
      
   }

   @Override
   public int numBilaterals() {
      
      switch(myLimitType) {
         case NONE:
            return 3;
         case ROLL_CURVE:
            return 3;
         case CURVE:
            return 4;
         case ROLL:
            return 5;
         case FIXED:
      }
      return 6;
   }

   private RotationMatrix3d RzGarbage = new RotationMatrix3d();
   
   @Override
   public void projectToConstraint(RigidTransform3d TGD, RigidTransform3d TCD) {
      
      TGD.R.set(TCD.R);
      TGD.p.setZero();
      
      switch(myLimitType) {
         
         case NONE:
         case ROLL_CURVE:      
            return;
         case CURVE:            
            // decompose and set roll to zero (Rz = I)
            // XXX one of these frames is backwards
            // TGD.R.transpose();
            doRollDecomposition(TGD.R, TGD.R, RzGarbage);
            // TGD.R.transpose();
            return;
         case ROLL:
            TGD.R.rotateZDirection (Vector3d.Z_UNIT);
            return;
         case FIXED:
            TGD.R.setIdentity();
      }
      
   }
   
   
   private static double doRollDecomposition(RotationMatrix3d RDC, RotationMatrix3d Rxy, RotationMatrix3d Rz) {
     
      double r02 = RDC.m02;
      double r12 = RDC.m12;
      double r22 = RDC.m22;
      double r0011 = RDC.m00+RDC.m11;
      double r0110 = RDC.m01-RDC.m10;
      
      double d = (1+r22);
      
      // degenerate case, assume no roll
      if (Math.abs(d) < EPSILON) {
         Rxy.set(RDC);
         Rz.setIdentity();
         return 0;
      }
      
      
      // align z axis:
      // Rxy =  [  R22 + R12^2*/(1+R22)     -R02*R12*/(1+R22)      R02 
      //              -R02*R12*/(1+R22)     R22+R02^2/(1+R22)      R12
      //                           -R02                  -R12      R22  ] 
      Rxy.set( r22+r12*r12/d,    -r02*r12/d, r02, 
                  -r02*r12/d, r22+r02*r02/d, r12,
                        -r02,          -r12, r22  );

      // rotate about new z axis
      // Rz = [   (R00+R11)/(1+R22)    (R01-R10)/(1+R22)     0
      //          (R10-R01)/(1+R22)    (R00+R11)/(1+R22)     0
      //                          0                    0     1  ]
      Rz.set( r0011/d, r0110/d, 0,
             -r0110/d, r0011/d, 0,
                    0,       0, 1 );
         
      return Math.atan2(-r0110, r0011);
      
   }

   private double doGetRoll(RotationMatrix3d RDC) {

      checkConstraintStorage();

      double roll = 0;
      // non-degenerate case, compute (otherwise assume zero)
      if (RDC.m22 > -1 + EPSILON) {
         roll = Math.atan2(RDC.m10-RDC.m01, RDC.m00+RDC.m11);
      }
      myConstraintInfo[5].coordinate = roll;
      
      return roll;
   }
   
   public void setRoll (RigidTransform3d TGD, double roll) {
      
      
      // decompose matrix, update roll matrix
      RotationMatrix3d Rz = new RotationMatrix3d();
      RotationMatrix3d RDC = new RotationMatrix3d(TGD.R);
      RDC.transpose();
      
      doRollDecomposition(RDC, RDC, Rz);
      Rz.setRotZ(roll);
      RDC.mul(Rz);
      TGD.R.transpose(RDC);

      checkConstraintStorage();
      myConstraintInfo[5].coordinate = roll;
   }

   public void initializeConstraintInfo(ConstraintInfo[] info) {
      
      info[0].flags = (BILATERAL | LINEAR);
      info[1].flags = (BILATERAL | LINEAR);
      info[2].flags = (BILATERAL | LINEAR);
      info[3].flags = ROTARY;
      info[4].flags = ROTARY;
      info[5].flags = ROTARY;
      
      
      info[0].wrenchC.set(1, 0, 0, 0, 0, 0);
      info[1].wrenchC.set(0, 1, 0, 0, 0, 0);
      info[2].wrenchC.set(0, 0, 1, 0, 0, 0);
      
      switch(myLimitType) {
         case FIXED:
            info[5].flags |= BILATERAL;
            info[5].wrenchC.set(0,0,0,0,0,1);
         case ROLL:
            info[4].flags |= BILATERAL;
            info[4].wrenchC.set(0,0,0,0,1,0);
            info[3].flags |= BILATERAL;
            info[3].wrenchC.set(0,0,0,1,0,0);
            break;
         case CURVE:
            // 5 always used for roll
            info[5].flags |= BILATERAL;
            info[5].wrenchC.set(0,0,0,0,0,1);
         case ROLL_CURVE:
         case NONE:       
      }      
      
   }
   
   public double getRoll(RigidTransform3d TGD) {
      
      // On entry, TGD is set to TCD. It is then projected to TGD
      projectToConstraint(TGD, TGD);
      
      RotationMatrix3d RDC = new RotationMatrix3d();
      RDC.transpose(TGD.R);
      return doGetRoll(RDC);

   }
   
   /**
    * Returns true if this coupling has a range restriction;
    */
   public boolean hasRestrictedRollRange() {
      return (myMinRoll != Double.NEGATIVE_INFINITY ||
              myMaxRoll != Double.POSITIVE_INFINITY);
   }
   
   @Override
   public void getConstraintInfo(
      ConstraintInfo[] info, RigidTransform3d TGD, RigidTransform3d TCD,
      RigidTransform3d XERR, boolean setEngaged) {

      //projectToConstraint(TGD, TCD);

      //myXFC.mulInverseLeft(TGD, TCD);
      myErr.set(XERR);

      // set zeros
      switch(myLimitType) {
         case FIXED:
            setDistancesAndZeroDerivatives(info, 6, myErr);
            break;
         case ROLL:
            setDistancesAndZeroDerivatives(info, 5, myErr);
            break;
         case CURVE:
            setDistanceAndZeroDerivative(info[5], myErr);
         case ROLL_CURVE:
         case NONE:
            setDistancesAndZeroDerivatives(info, 3, myErr);
      }
      
      
      // roll limit
      if (myLimitType == LimitType.ROLL_CURVE || myLimitType == LimitType.ROLL) {
         
         RotationMatrix3d RDC = new RotationMatrix3d();
         RDC.transpose(TGD.R);
         double theta = doGetRoll(RDC);
         
         if (hasRestrictedRollRange()) {
            if (setEngaged) {
               maybeSetEngaged(info[5], theta, myMinRoll, myMaxRoll);
            }
            if (info[5].engaged != 0) {
               info[5].distance = getDistance (theta, myMinRoll, myMaxRoll);
               info[5].dotWrenchC.setZero();
               if (info[5].engaged == 1) {
                  info[5].wrenchC.set (0, 0, 0, 0, 0, 1);
               }
               else if (info[5].engaged == -1) {
                  info[5].wrenchC.set (0, 0, 0, 0, 0, -1);
               }
            }   
         }
      }
      
      // curve limit       
      if (myLimitType == LimitType.CURVE || myLimitType == LimitType.ROLL_CURVE) {
         // get relative z axis
         double [] z = new double[3]; 
         double [] axis = new double[3];
         double [] znew = new double[3];
         boolean engage = false;
         double theta = 0;
         
         TGD.R.getColumn(2, znew);
         TCD.R.getColumn(2, z);
         
         
         if ( Math.abs(z[0]-znew[0]) > EPSILON ){
            System.out.printf("XFDz = (%f, %f, %f),  XCDz = (%f, %f, %f)\n", 
               TCD.R.m02, TCD.R.m12, TCD.R.m22, TGD.R.m02,TGD.R.m12,TGD.R.m22);
         }
         
         
         theta = myCurve.getProjection(z,znew,axis);
         
         Vector3d v = new Vector3d(axis);
         v.normalize();
         v.inverseTransform(TGD);
         
         // check if z inside curve, and project to boundary if outside
         if (!myCurve.isWithin(z)) {
            if (theta > 1e-5) {
               engage = true;
               theta = -theta;
            } else {
               v.set(1,0,0);
            }
         }
         
         if (engage && Math.abs(theta) > Math.toRadians(5)) {
            System.out.println( "Axis: " + v + ", Angle: " + Math.toDegrees(theta) );
         }
                 
         // maybe set engaged
         if (setEngaged) {
            if (engage) {
               info[4].engaged = 1;
            } else {
               // info[4].engaged = 0; // ?? is this required?
            }
         }
         
         if (info[4].engaged != 0) {           
            info[4].distance = theta;
            info[4].wrenchC.set(0,0,0, -v.x, -v.y, -v.z);
            info[4].dotWrenchC.setZero();
            
         }
      }
   }
   
}
