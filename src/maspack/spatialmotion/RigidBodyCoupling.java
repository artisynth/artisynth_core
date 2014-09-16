/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.spatialmotion;

import maspack.matrix.*;
import maspack.util.*;

import java.util.ArrayList;

/**
 * Enforces constraints between two rigid bodies A and B, or between one rigid
 * body A and ground. In the latter case, B is equated with the (fixed) world
 * frame.
 * 
 * <p>
 * The coupling has its own coordinate frame D, which is anchored to B via a
 * fixed transform TDB. Body A is associated with an operation frame F,
 * described with respect to A by the fixed transform TFA. Enforcement of the
 * constraint involves trying to keep F on a <i>constraint surface</i> whose
 * location is constant with respect to D. The constraint frame C is the frame
 * on the constraint surface which is nearest to F. Wrenches to enforce the
 * constraint are defined (initially) with repsect to C and then transformed
 * into the body frames A and B. Ideally, F and C coincide; otherwise, the
 * transform TFC defines the constraint error and a linearization of this,
 * projected onto the constraint wrenches in C, gives the linearized distance
 * associated with each constraint.
 */
public abstract class RigidBodyCoupling {

   protected double myContactDistance = 0;
   protected double myBreakSpeed = Double.NEGATIVE_INFINITY;
   protected double myBreakAccel = Double.NEGATIVE_INFINITY;

   // internal temp variables
   protected RigidTransform3d myTFD = new RigidTransform3d();
   protected RigidTransform3d myTCD = new RigidTransform3d();
   protected RigidTransform3d myTFC = new RigidTransform3d();
   protected Twist myErr = new Twist();
   // protected RigidTransform3d myTBA = new RigidTransform3d();
   protected Twist myVelBA = new Twist();
   protected Twist myVelBxA = new Twist();
   protected boolean myVelocitiesZeroP = true;
   protected Wrench myBilateralWrenchF = new Wrench();
   protected Wrench myUnilateralWrenchF = new Wrench();

   // if true, wrenches should originate in frame F instead of C
//   private boolean myConstraintsInF = false;

//   int myBodyIdxA = -1;
//   int myBodyIdxB = -1;

   protected RigidBodyConstraint[] myConstraints;
   protected ConstraintInfo[] myConstraintInfo;

   // Flags that provide information about the constraint types
   // associated with this coupling

   /** 
    * Constraint is bilateral.
    */
   public static final int BILATERAL = 0x1;

   /** 
    * Constraint is linear.
    */
   public static final int LINEAR    = 0x2;

   /** 
    * Constraint is rotary.
    */
   public static final int ROTARY    = 0x4;

   /**
    * Returns constraint information along a particular degree of freedom.
    */
   protected class ConstraintInfo {
      Wrench wrenchC;
      Wrench dotWrenchC;
      double distance;
      double compliance;
      double damping;
      double coordinate; 
      int engaged;
      int flags;

      ConstraintInfo (ConstraintInfo info) {
         this();
         set (info);
      }

      ConstraintInfo() {
         wrenchC = new Wrench();
         dotWrenchC = new Wrench();
         distance = 0;
         engaged = 0;
         coordinate = 0;
      }

      public void set (ConstraintInfo info) {
         wrenchC.set (info.wrenchC); 
         dotWrenchC.set (info.dotWrenchC); 
         distance = info.distance;
         compliance = info.compliance;
         damping = info.damping;
         coordinate = info.coordinate;
         engaged = info.engaged;
         flags = info.flags;         
      }

      boolean isBilateral() {
         return (flags & BILATERAL) != 0;
      }
   }

   protected RigidBodyCoupling() {
   }

   /**
    * Sets the distance to the constraint surface required to engage a
    * unilateral constraint. The default value is 0. A negative value will cause
    * the constraint surface to be pentrated before the unilateral constraint is
    * engaged.
    * 
    * @param d
    * contact distance for unilateral constraints
    */
   public void setContactDistance (double d) {
      myContactDistance = d;
   }

   /**
    * Returns the distance to the constraint surface required to engage a
    * unilateral constraint.
    * 
    * @return contact distance for unilateral constraints
    */
   public double getContactDistance() {
      return myContactDistance;
   }

   /**
    * Sets the minimum speed normal to the constraint surface required to
    * disengage a unilateral constraint. This feature is used to prevent contact
    * chattering. The default value is -infinity, implying that the feature is
    * disabled.
    * 
    * @param v
    * minimum normal speed for breaking unilateral constraints
    */
   public void setBreakSpeed (double v) {
      myBreakSpeed = v;
   }

   /**
    * Returns the minimum speed normal to the constraint surface required to
    * disengage a unilateral constraint.
    * 
    * @return minimum normal speed for breaking unilateral constraints
    */
   public double getBreakSpeed() {
      return myBreakSpeed;
   }

   /**
    * Sets the minimum acceleration normal to the constraint surface required to
    * disengage a unilateral constraint. This feature is used to prevent contact
    * chattering. The default value is -infinity, implying that the feature is
    * disabled.
    * 
    * @param a
    * minimum normal acceleration for breaking unilateral constraints
    */
   public void setBreakAccel (double a) {
      myBreakAccel = a;
   }

   /**
    * Returns the minimum acceleration normal to the constraint surface required
    * to disengage a unilateral constraint.
    * 
    * @return minimum normal acceleration for breaking unilateral constraints
    */
   public double getBreakAccel() {
      return myBreakAccel;
   }

   /** 
    * Returns the compliances for all this coupling's constraint directions.
    * The default values are all zero.
    * 
    * @return compliances for this coupling
    */
   public VectorNd getCompliance() {
      checkConstraintStorage();
      VectorNd c = new VectorNd(myConstraintInfo.length);
      for (int i=0; i<myConstraintInfo.length; i++) {
         c.set(i, myConstraintInfo[i].compliance);
      }
      return c;
   }

   /** 
    * Sets compliances for all this coupling's constraint directions.
    * 
    * @param c new compliance values
    */
   public void setCompliance (VectorNd c) {
      checkConstraintStorage();
      for (int i=0; i<myConstraintInfo.length && i<c.size(); i++) {
         myConstraintInfo[i].compliance = c.get(i);
      }
   }

   /** 
    * Returns the dampings for all this coupling's constraint directions.
    * The default values are all zero.
    * 
    * @return dampings for this coupling
    */
   public VectorNd getDamping() {
      checkConstraintStorage();
      VectorNd d = new VectorNd(myConstraintInfo.length);
      for (int i=0; i<myConstraintInfo.length; i++) {
         d.set(i, myConstraintInfo[i].damping);
      }
      return d;
   }

   /** 
    * Sets dampings for all this coupling's constraint directions.
    * 
    * @param c new damping values
    */
   public void setDamping (VectorNd c) {
      checkConstraintStorage();
      for (int i=0; i<myConstraintInfo.length && i<c.size(); i++) {
         myConstraintInfo[i].damping = c.get(i);
      }
   }

   /** 
    * Returns info for all this coupling's constraint directions.
    * Values are given as settings of the flags
    * {@link #BILATERAL}, {@link #LINEAR}, and {@link #ROTARY}.
    * 
    * @return dampings for this coupling
    */
   public VectorNi getConstraintInfo() {
      checkConstraintStorage();
      VectorNi info = new VectorNi(myConstraintInfo.length);
      for (int i=0; i<myConstraintInfo.length; i++) {
         info.set(i, myConstraintInfo[i].flags);
      }
      return info;
   }

   public Wrench getBilateralForceF() {
      return myBilateralWrenchF;
   }

   public Wrench getUnilateralForceF() {
      return myUnilateralWrenchF;
   }

   protected void maybeSetEngaged (
      ConstraintInfo info, double val, double min, double max) {
      if (val < min) {
         info.engaged = -1;
      }
      else if (val > max) {
         info.engaged = 1;
      }
   }

   protected double getDistance (double val, double min, double max) {
      if (Math.abs (val-min) < Math.abs (val-max)) {
         return val-min;
      }
      else {
         return max-val;
      }
   }      

   protected void initializeConstraintStorage() {
      int numb = numBilaterals();
      int maxu = maxUnilaterals();
      int maxc = numb + maxu;
      myConstraintInfo = new ConstraintInfo[maxc];
      myConstraints = new RigidBodyConstraint[maxc];
      for (int i = 0; i < maxc; i++) {
         RigidBodyConstraint c = new RigidBodyConstraint();
//         c.setBodyIndexA (myBodyIdxA);
//         c.setBodyIndexB (myBodyIdxB);
         myConstraints[i] = c;
         myConstraintInfo[i] = new ConstraintInfo();
         // if (i < numb) {
         //    myConstraintInfo[i].engaged = 1;
         // }
      }
   }

   protected void checkConstraintStorage() {
      if (myConstraintInfo == null) {
         initializeConstraintStorage();
         initializeConstraintInfo (myConstraintInfo);
      }
   }

   public void updateConstraintsFromC () {
//      RigidTransform3d TCA = new RigidTransform3d();
//
//      // convert wrenchC to frame A and frame B
//      TCA.mulInverseRight (TFA, myTFD);
//      TCA.mul (myTCD);

      for (int i = 0; i < myConstraintInfo.length; i++) {
         ConstraintInfo info = myConstraintInfo[i];
         if (info.isBilateral() || info.engaged != 0) {
            RigidBodyConstraint c = myConstraints[i];

//            c.myWrenchA.transform (TCA, info.wrenchC);
//            c.myWrenchB.inverseTransform (myTBA, c.myWrenchA);
//            c.myWrenchB.negate();
            c.myWrenchC.set (info.wrenchC);
            
            c.setDistance (info.distance);
            c.setCompliance (info.compliance);
            c.setDamping (info.damping);

            double deriv = info.dotWrenchC.dot (myVelBA);
            deriv += info.wrenchC.dot (myVelBxA);
            c.setDerivative (deriv); 
//            if (this instanceof RevoluteCoupling) {
//               if (info.isBilateral()) {
//                  System.out.println (
//                     " deriv=" + info.dotWrenchC.dot (myVelBA) + " "+
//                     info.wrenchC.dot (myVelBxA));
//               }
//            }
         }
      }
   }

//   /**
//    * Just an experimental method right now to see if we can easily apply
//    * constraints from frame F instead of C
//    */
//   public void updateConstraintsFromF (RigidTransform3d TFA) {
//      RigidTransform3d TCF = new RigidTransform3d();
//
//      // convert wrenchC to frame A and frame B
//      TCF.mulInverseLeft (myTFD, myTCD);
//      myErr.set (TCF);
//
//      for (int i = 0; i < myConstraintInfo.length; i++) {
//         ConstraintInfo info = myConstraintInfo[i];
//         if (info.isBilateral() || info.engaged != 0) {
//            RigidBodyConstraint c = myConstraints[i];
//
//            Wrench wrenchF = c.myWrenchC; // tmp storage
//
//            // rotate from C into F
//            wrenchF.f.transform (TCF.R, info.wrenchC.f);
//            wrenchF.m.transform (TCF.R, info.wrenchC.m);
//
//            c.setDistance (-wrenchF.dot (myErr));
//            c.setCompliance (info.compliance);
//
////            c.myWrenchA.transform (TFA, wrenchF);
////            c.myWrenchB.inverseTransform (myTBA, c.myWrenchA);
////            c.myWrenchB.negate();
//            c.myWrenchC.set (info.wrenchC);
//
//            // ignore derivatives for now ...
//            c.setDerivative (0);
//         }
//      }
//   }

//   private void updateConstraintInfo (
//      RigidTransform3d TFA, RigidTransform3d TDB, boolean setEngaged) {
//      checkConstraintStorage();
//      myTFD.mulInverseBoth (TDB, myTBA);
//      myTFD.mul (TFA);
//      getConstraintInfo (myTCD, myConstraintInfo, myTFD, setEngaged);
//
//      if (myConstraintsInF) {
//         updateConstraintsFromF (TFA);
//      }
//      else {
//         updateConstraintsFromC (TFA);
//      }
//   }

   /**
    * Assumes that TBA has been updated
    */
   private void updateVelocityInfo (
      Twist velA, Twist velB) {
      if (velB == null) {
         velB = Twist.ZERO;
      }
      myVelBA.sub (velB, velA);
      myVelBxA.cross (velB, velA);

      // The code below is from when velA and velB were presented in frames
      // A and B, respectively, instead of frame C:
      
//      myVelBA.inverseTransform (myTBA, velA); // transform velA into B frame
//      myVelBA.sub (velB, myVelBA); // form velB-velA in B
//      myVelBA.inverseTransform (TDB); // transform into D
//      myVelBA.inverseTransform (myTCD);
//      if (velB != Twist.ZERO) {
//         myVelBxA.inverseTransform (myTBA, velA); // xform velA into B frame
//         myVelBxA.cross (velB, myVelBxA);
//         myVelBxA.inverseTransform (TDB); // transform into D
//         myVelBxA.inverseTransform (myTCD);
//      }
//      else {
//         myVelBxA.setZero();
//      }
     
   }


   /**
    * Special method to numerically check the derivative values for bilateral
    * constraints.
    */
   private void checkBilateralDerivsNumerically (
      RigidTransform3d TBA, RigidTransform3d TFA, RigidTransform3d TDB, 
      Twist velA, Twist velB, boolean setEngaged) {

      RigidTransform3d TCA = new RigidTransform3d();
      RigidTransform3d TCB = new RigidTransform3d();
      RigidTransform3d TCF = new RigidTransform3d();
      
      TCA.mulInverseRight (TFA, myTFD);
      TCA.mul (myTCD);
      TCB.mulInverseLeft (TBA, TCA);
      TCF.mulInverseLeft (myTFD, myTCD);
      
      Twist velA_A = new Twist(velA);
      Twist velB_B = new Twist();
      if (velB != null) {
         velB_B.set (velB);
      }
      
      // transform velA from frame C to frame A, and velB to frame B:
      velA_A.transform (TCA);
      velB_B.transform (TCB);
      
      int numc = myConstraintInfo.length;
      ConstraintInfo[] newConstraintInfo = new ConstraintInfo[numc];
      for (int i=0; i<numc; i++) {
         newConstraintInfo[i] = new ConstraintInfo(myConstraintInfo[i]);
      }
         
      double eps = 1e-8;
      Twist velBA = new Twist();
      RigidTransform3d newTBA = new RigidTransform3d();
      RigidTransform3d newTFD = new RigidTransform3d();
      RigidTransform3d newTCD = new RigidTransform3d();
      RigidTransform3d newTERR = new RigidTransform3d();
      velBA.inverseTransform (TBA, velA_A);
      velBA.sub (velB_B, velBA);
      
      newTBA.set (TBA);
      velBA.extrapolateTransform (newTBA, 1e-8);
         
      newTFD.mulInverseBoth (TDB, newTBA);
      newTFD.mul (TFA);
      newTERR.mulInverseLeft (newTCD, newTFD);
      getConstraintInfo (newConstraintInfo, newTCD, newTFD, newTERR, setEngaged);

      RigidTransform3d newTCA = new RigidTransform3d();
      RigidTransform3d newTCB = new RigidTransform3d();
      RigidTransform3d newTCF = new RigidTransform3d();
      RigidTransform3d diffTCA = new RigidTransform3d();
      RigidTransform3d diffTCB = new RigidTransform3d();
      RigidTransform3d diffTCF = new RigidTransform3d();

      newTCA.mulInverseRight (TFA, newTFD);
      newTCA.mul (newTCD);
      newTCB.mulInverseLeft (newTBA, newTCA);
      newTCF.mulInverseLeft (newTFD, newTCD);

      diffTCA.mulInverseLeft (TCA, newTCA);
      diffTCB.mulInverseLeft (TCB, newTCB);
      diffTCF.mulInverseLeft (TCF, newTCF);

      Twist velCA_A = new Twist();
      Twist velCB_B = new Twist();
      Twist velCF_A = new Twist();
      Twist velCF_B = new Twist();

      velCA_A.set (diffTCA);
      velCA_A.transform (TCA);
      velCB_B.set (diffTCB);
      velCB_B.transform (TCB);
      velCF_A.set (diffTCF);
      velCF_A.transform (TCA);
         
      velCA_A.scale(1/eps);
      velCB_B.scale(1/eps);
      velCF_A.scale(1/eps);
      velCF_B.inverseTransform (TBA, velCF_A);

      Twist velACxA = new Twist();
      Twist velBCxB = new Twist();
      Twist velCFxA = new Twist();
      Twist velCFxB = new Twist();
      Twist velBxA_B = new Twist();

      velACxA.cross (velCA_A, velA_A);
      velACxA.scale(-1);
      velBCxB.cross (velCB_B, velB_B);
      velBCxB.scale(-1);

      velCFxA.cross (velCF_A, velA_A);
      velCFxB.cross (velCF_B, velB_B);
      velBxA_B.transform (TCB, myVelBxA);

      Wrench[] dotWrenchA = new Wrench[numc];
      Wrench[] dotWrenchB = new Wrench[numc];
      for (int i=0; i<numc; i++) {
         ConstraintInfo newinfo = newConstraintInfo[i];
         ConstraintInfo oldinfo = myConstraintInfo[i];
         if (newinfo.isBilateral()) {
            Wrench oldWrenchA = new Wrench();
            Wrench oldWrenchB = new Wrench();
            
            oldWrenchA.transform (TCA, oldinfo.wrenchC);
            oldWrenchB.inverseTransform (TBA, oldWrenchA);
            oldWrenchB.negate();
            
            dotWrenchA[i] = new Wrench();
            dotWrenchB[i] = new Wrench();
            dotWrenchA[i].transform (newTCA, newinfo.wrenchC);
            dotWrenchB[i].inverseTransform (newTBA, dotWrenchA[i]);
            dotWrenchB[i].negate();

            RigidBodyConstraint c = myConstraints[i];
            dotWrenchA[i].sub (oldWrenchA);
            dotWrenchA[i].scale (1/eps);
            dotWrenchB[i].sub (oldWrenchB);
            dotWrenchB[i].scale (1/eps);
            double deriv = (dotWrenchA[i].dot (velA_A) + 
                             dotWrenchB[i].dot (velB_B));
            System.out.printf (
               "nderiv=%10.5f %10.5f %10.5f\n",
               dotWrenchA[i].dot (velA_A), 
               dotWrenchB[i].dot (velB_B), deriv);
            System.out.printf (
               "xderiv=%10.5f %10.5f %10.5f\n",
               oldWrenchA.dot (velACxA),
               oldWrenchB.dot (velBCxB),
               oldWrenchA.dot (velACxA)+oldWrenchB.dot (velBCxB));
            System.out.printf (
               "yderiv=%10.5f %10.5f\n",
               -oldWrenchA.dot (velCFxA), 
               -oldWrenchB.dot (velCFxB) + oldWrenchB.dot(velBxA_B));
         }
      }
   }

   public void updateBodyStates (
      RigidTransform3d TFD, RigidTransform3d TCD, RigidTransform3d TERR,
      Twist velA, Twist velB, boolean setEngaged) {

      myVelocitiesZeroP = false;

      checkConstraintStorage();
      myTFD.set (TFD);
      
      getConstraintInfo (myConstraintInfo, TCD, TFD, TERR, setEngaged);

      updateVelocityInfo (velA, velB);
      
      updateConstraintsFromC ();
   }

   public RigidBodyConstraint getConstraint (int idx) {
      checkConstraintStorage();
      return myConstraints[idx];
   }

   public int getBilateralConstraints (
      ArrayList<RigidBodyConstraint> bilaterals) {
      checkConstraintStorage();
      int numb = 0;
      for (int i = 0; i < myConstraintInfo.length; i++) {
         if (myConstraintInfo[i].isBilateral()) {
            bilaterals.add (myConstraints[i]);
            numb++;
         }
      }
      return numb;
   }

   public int setBilateralImpulses (VectorNd lam, double h, int idx) {
      checkConstraintStorage();
      double[] buf = lam.getBuffer();
      myBilateralWrenchF.setZero();
      for (int i=0; i<myConstraintInfo.length; i++) {
         if (myConstraintInfo[i].isBilateral()) {
            double l = buf[idx++];
            myConstraints[i].setMultiplier (l);
            myBilateralWrenchF.scaledAdd (l, myConstraints[i].getWrenchC());
         }
      }
      myBilateralWrenchF.scale (1/h);
      myBilateralWrenchF.inverseTransform (myTFC);
      return idx;      
   }
   
   
   public void zeroImpulses() {
      checkConstraintStorage();
      for (int i=0; i<myConstraintInfo.length; i++) {
         myConstraints[i].setMultiplier (0);
      }
      myBilateralWrenchF.setZero();
      myUnilateralWrenchF.setZero();
   }

   public int getBilateralImpulses (VectorNd lam, int idx) {
      checkConstraintStorage();
      double[] buf = lam.getBuffer();
      for (int i=0; i<myConstraintInfo.length; i++) {
         if (myConstraintInfo[i].isBilateral()) {
            buf[idx++] = myConstraints[i].getMultiplier();
         }
      }
      return idx;      
   }
   
   /**
    * Returns the number of currently engaged unilateral constraints.
    * 
    * @return number of engaged unilateral contraints
    */
   public int numUnilaterals() {
      checkConstraintStorage();
      int numu = 0;
      for (int i = 0; i < myConstraintInfo.length; i++) {
         ConstraintInfo info = myConstraintInfo[i];
         if (!info.isBilateral()) {
            if (info.engaged != 0) {
               numu++;
            }
         }
      }
      return numu;
   }

   public double getUnilateralConstraints (
      ArrayList<RigidBodyConstraint> unilaterals, boolean setEngaged) {
      checkConstraintStorage();
      double maxpen = 0;
      for (int i = 0; i < myConstraintInfo.length; i++) {
         ConstraintInfo info = myConstraintInfo[i];
         if (!info.isBilateral()) {
            RigidBodyConstraint c = myConstraints[i];
            if (info.engaged != 0) {
               Wrench wrC = info.wrenchC;
               double dist = c.getDistance();
               if (wrC.m.equals (Vector3d.ZERO)) {
                  // XXX only consider purely translational constraints for
                  // maxdist at the moment
                  if (-dist > maxpen) {
                     maxpen = -dist;
                  }
               }
               // System.out.println ("dist=" + dist + " " +
               //                     myContactDistance);
               // System.out.println ("speed=" + c.getContactSpeed() + " vs " +
               //                           myBreakSpeed);
               if (setEngaged && dist >= myContactDistance) {
                  // need a positive velocity to break contact
                  if (c.getContactSpeed() > myBreakSpeed) {
                     info.engaged = 0;
                     c.setMultiplier (0);
                     //System.out.println ("BREAK: speed="+c.getContactSpeed());
                  }
                  else if (c.getContactAccel() > myBreakAccel) {
                     info.engaged = 0;
                     c.setMultiplier (0);
                     //System.out.println ("BREAK: accel="+c.getContactAccel());
                  }
               }
            }
            if (info.engaged != 0) {
               unilaterals.add (c);
            }
         }
      }
      return maxpen;
   }

   public void updateUnilateralConstraints (
      ArrayList<RigidBodyConstraint> unilaterals, int offset, int numc) {
      // just do sanity checking for now;
      // constraints will have been updated in updateBodyStates
      checkConstraintStorage();
      int numu = 0;
      int k = offset;
      for (int i = 0; i < myConstraintInfo.length; i++) {
         ConstraintInfo info = myConstraintInfo[i];
         if (!info.isBilateral()) {
            if (info.engaged != 0) {
               if (k == unilaterals.size()) {
                  throw new IllegalArgumentException (
                     "unilaterals size "+unilaterals.size()+
                     " differs from that returned by getUnilateralConstraints()");
               }
               if (myConstraints[i] != unilaterals.get (k++)) {
                  throw new IllegalArgumentException (
"unilaterals differ from those returned by getUnilateralConstraints()");
               }
               numu++;
            }
         }
      }
      if (numu != numc) {
         throw new IllegalArgumentException (
            "unilateral count "+numc+" differs from "+numu+
            " returned by getUnilateralConstraints(), constraint " + this);
      }
   }

   public int setUnilateralImpulses (VectorNd the, double h, int idx) {
      checkConstraintStorage();
      double[] buf = the.getBuffer();
      myUnilateralWrenchF.setZero();
      for (int i=0; i<myConstraintInfo.length; i++) {
         ConstraintInfo info = myConstraintInfo[i];
         if (!info.isBilateral() && info.engaged != 0) {
            double l = buf[idx++];
            myConstraints[i].setMultiplier (l);
            myUnilateralWrenchF.scaledAdd (l, myConstraints[i].getWrenchC());
         }
      }
      myUnilateralWrenchF.scale (1/h);
      myUnilateralWrenchF.inverseTransform (myTFC);
      return idx;      
   }

   public int getUnilateralImpulses (VectorNd the, int idx) {
      int idx0 = idx;
      checkConstraintStorage();
      double[] buf = the.getBuffer();
      for (int i=0; i<myConstraintInfo.length; i++) {
         ConstraintInfo info = myConstraintInfo[i];
         if (!info.isBilateral() && info.engaged != 0) {
            buf[idx++] = myConstraints[i].getMultiplier();
         }
      }
      return idx;      
   }

   public int maxConstraints() {
      checkConstraintStorage();
      return myConstraints.length;
   }

   /**
    * Computes the frame C on the constraint surface which is closest to a given
    * frame F. The input consists of the transform from F to D.
    * 
    * @param TCD
    * returns the transform from C to D
    * @param TFD
    * transform from frame F to D
    */
   public abstract void projectToConstraint (
      RigidTransform3d TCD, RigidTransform3d TFD);

   /**
    * Returns the number of bilateral constraints associated with this coupling.
    * 
    * @return number of bilateral constraints
    */
   public abstract int numBilaterals();

   /**
    * Returns the maximum number of unilateral constraints associated with this
    * coupling.
    * 
    * @return maximum number of unilateral constraints
    */
   public abstract int maxUnilaterals();

   /**
    * Computes the constraint frame C and the associated constraint information.
    * C is determined by projecting F onto the constraint surface.
    * 
    * <p>
    * Information for each constraint wrench is returned through an array of
    * ConstraintInfo objects supplied by the argument <code>info</code>. This
    * array should have a fixed number of elements equal to the number of
    * bilateral constraints plus the maximum number of unilateral constraints.
    * Bilateral constraints appear first, followed by the unilateral
    * constraints. Constraint wrenches and their derivatives (with respect to
    * frame C) are set within the fields <code>wrenchC</code> and
    * <code>dotWrenchC</code>, repsectively. Distances to set within the
    * <code>distance</code>; each of these should be the dot product of the
    * wrench with the linearization of the constraint error TFC. For computing
    * wrench derivatives, this method may use <code>myVelBA</code>, which
    * gives the current velocity of B with repsect to A, in coordinate frame D.
    * 
    * Information only needs to be returned for constraints which are 
    * potentially active, or <i>engaged</i>. Constraints which are
    * engaged have their <code>ConstraintInfo.engaged</code> field set to a
    * non-zero value. Bilateral constraints are always engaged, and
    * their <code>ConstraintInfo.engaged</code> field is automatically 
    * set to 1 by the system. For unilateral constraints, the determination
    * of whether or not the constraint is engaged, and the setting of
    * the <code>engaged</code> field, should be done
    * by this method if the argument <code>setEngaged</code> is 
    * <code>true</code>. Otherwise, if <code>setEngaged</code> is 
    * false, the method should take the engaged settings as given.
    * 
    * Constraints which are engaged are those which
    * are returned by the calls {@link
    * maspack.spatialmotion.RigidBodyCoupling#getBilateralConstraints
    * getBilateralConstraints} or {@link
    * maspack.spatialmotion.RigidBodyCoupling#getUnilateralConstraints
    * getUnilateralConstraints}.
    * @param info
    * used to return information for each possible constraint wrenches
    * @param TCD
    * returns the transform from C to D
    * @param TFD
    * transform from operation frame F to D
    * @param TERR TODO
    * @param setEngaged if <code>true</code>, this method should determine
    * if the constraint is engaged.
    */
   public abstract void getConstraintInfo (
      ConstraintInfo[] info, RigidTransform3d TCD, RigidTransform3d TFD, 
      RigidTransform3d TERR, boolean setEngaged);

   public abstract void initializeConstraintInfo (ConstraintInfo[] info);


   /**
    * Transforms the geometry of this coupling, in response to an affine
    * transform X applied in world coordinates. In order to facilitate the
    * application of this transform, this method also provides the current
    * transforms from the F and D frames to world coordinates. It also provides
    * the rotational component Ra of X.M, which should be determined from the
    * left polar decomposition of X.M:
    * 
    * <pre>
    * X.M = P Ra
    * </pre>
    *
    * Given these arguments, a point p of the coupling defined with respect to
    * frame D would transform according to
    *
    * <pre>
    * p' = TDW.R^T Ra^T X.M TDW.R p
    * </pre>
    *
    * which is equivalent to transforming p to world coordinates (using TDW),
    * applying X, and then transforming back to D using the modified TDW
    * produced by applying X.
    * 
    * @param X
    * affine transform applied to the coupling in world coordinates
    * @param Ra
    * rotational component of the matrix part of X.
    * @param TFW
    * current transform from F to world
    * @param TDW
    * current transform from D to world
    */
   public void transformGeometry (
      AffineTransform3dBase X, RotationMatrix3d Ra, 
      RigidTransform3d TFW, RigidTransform3d TDW) {
   
   }

   public void scaleDistance (double s) {
      //myTDB.p.scale (s);
      //myTFA.p.scale (s);
   }


   public void skipAuxState (DataBuffer data) {
      checkConstraintStorage();
      int maxu = maxUnilaterals();
      data.zskip (maxu);
      data.dskip (maxu);
   }

   
   public void getAuxState (DataBuffer data) {

      checkConstraintStorage();
      if (maxUnilaterals() > 0) {
         for (int i = 0; i < myConstraintInfo.length; i++) {
            if (!myConstraintInfo[i].isBilateral()) {
               data.zput (myConstraintInfo[i].engaged);
               data.dput (myConstraintInfo[i].coordinate);
            }
         }
      }
   }

   public void setAuxState (DataBuffer data) {

      checkConstraintStorage();
      if (maxUnilaterals() > 0) {
         for (int i = 0; i < myConstraintInfo.length; i++) {
            if (!myConstraintInfo[i].isBilateral()) {
               myConstraintInfo[i].engaged = data.zget();
               myConstraintInfo[i].coordinate = data.dget();
            }
         }
      }
   }
   
   public void getInitialAuxState (
      DataBuffer newData, DataBuffer oldData) {

      if (oldData == null) {
         getAuxState (newData);
      }
      else {
         // copy data from oldData
         checkConstraintStorage();
         int maxu = maxUnilaterals();
         if (maxu > 0) {
            newData.putData (oldData, maxu, maxu);
         }
      }
   }
   
   /**
    * Sets the <code>distance</code> fields within the <code>info</code>
    * array to the dot product of <code>wrenchC</code> with <code>err</code>,
    * and sets the <code>dotWrenchC</code> fields to 0.
    */
   public void setDistancesAndZeroDerivatives (
      ConstraintInfo[] info, int numc, Twist err) {
      for (int i = 0; i < numc; i++) {
         setDistanceAndZeroDerivative(info[i], err);
      }
   }
   public void setDistanceAndZeroDerivative (
      ConstraintInfo info, Twist err) {
      info.distance = info.wrenchC.dot (err);
      info.dotWrenchC.setZero();
   }
   
   /** 
    * Given an angle <code>ang</code>, find an equivalent angle that is within
    * +/- PI of a given reference angle <code>ref</code>.
    * 
    * @param ref reference angle (radians)
    * @param ang initial angle (radians)
    * @return angle equivalent to <code>ang</code> within +/- PI
    * of <code>ref</code>.
    */
   public double findNearestAngle (double ref, double ang) {
      while (ang - ref > Math.PI) {
         ang -= 2*Math.PI;
      }
      while (ang - ref < -Math.PI) {
         ang += 2*Math.PI;
      }
      return ang;
   }

}
