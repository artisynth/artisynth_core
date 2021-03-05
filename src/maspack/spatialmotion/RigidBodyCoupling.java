/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.spatialmotion;

import java.util.ArrayList;

import maspack.geometry.GeometryTransformer;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.matrix.VectorNi;
import maspack.util.DataBuffer;
import maspack.util.DoubleInterval;
import maspack.util.InternalErrorException;

/**
 * Base class for the mechanism to enforce constraints between two rigid frame
 * C and D. This is the main worker class used by {@code BodyConnector} in the
 * core ArtiSynth code.
 *
 * Couplings for specific types of constraints should subclass this
 * base class and implement the abstract methods.
 */
public abstract class RigidBodyCoupling implements Cloneable {

   protected static final double INF = Double.POSITIVE_INFINITY;

   protected static final double RTOD = 180/Math.PI;
   protected static final double DTOR = Math.PI/180;

   protected double myLinearLimitTol = 0.0001;
   protected double myRotaryLimitTol = 0.0001;
   protected double myBreakSpeed = 0;//e-8; //Double.NEGATIVE_INFINITY;

   protected RigidTransform3d myTCG = new RigidTransform3d();
   protected Twist myErr = new Twist();
   protected Wrench myBilateralWrenchG = new Wrench();
   protected Wrench myUnilateralWrenchG = new Wrench();

   protected ArrayList<RigidBodyConstraint> myConstraints = new ArrayList<>();
   protected ArrayList<CoordinateInfo> myCoordinates = new ArrayList<>();
   protected VectorNd myCoordValues = new VectorNd(); //used for temp storage
   protected int myNumBilaterals = 0;
   protected int myNumUnilaterals = 0;

   // Flags imported from RigidBodyConstraint for convenience:

   protected static final int BILATERAL = RigidBodyConstraint.BILATERAL;
   protected static final int LINEAR    = RigidBodyConstraint.LINEAR;
   protected static final int ROTARY    = RigidBodyConstraint.ROTARY;
   protected static final int CONSTANT  = RigidBodyConstraint.CONSTANT;
   protected static final int LIMIT     = RigidBodyConstraint.LIMIT;

   protected double clip(double value, double min, double max) {
      if (value < min) {
         return min;
      }
      else if (value > max) {
         return max;
      }
      else {
         return value;
      }
   }

   /**
    * Information about joint coordinates
    */
   protected class CoordinateInfo {

      public double value;      // current coordinate value
      public double maxValue;   // maximum value
      public double minValue;   // minimum value

      // unilateral constraint for enforcing limits:
      public RigidBodyConstraint limitConstraint;

      public int infoFlags;     // information flags. Reserved for future use

      CoordinateInfo () {
         value = 0;
         maxValue = Double.POSITIVE_INFINITY;
         minValue = Double.NEGATIVE_INFINITY;
         limitConstraint = null;
      }

      CoordinateInfo (double min, double max) {
         value = 0;
         maxValue = max;
         minValue = min;
         limitConstraint = null;
      }

      CoordinateInfo (
         double min, double max, int flags, RigidBodyConstraint cons) {
         value = 0;
         maxValue = max;
         minValue = min;
         infoFlags = flags;
         limitConstraint = cons;
         cons.flags |= LIMIT;
      }

      public boolean hasRestrictedRange() {
         return (minValue != Double.NEGATIVE_INFINITY ||
                 maxValue != Double.POSITIVE_INFINITY);        
      }

      public void setNearestAngle (double ang) {
         value = findNearestAngle (value, ang);
      }
      
      public double nearestAngle (double ang) {
         return findNearestAngle (value, ang);
      }
      
      public void setValue (double val) {
         value = val;
      }
      
      public void clipAndSetValue (double val) {
         value = clipToRange (val);
         if (limitConstraint != null) {
            limitConstraint.resetEngaged = true;
         }
      }
            
      protected void updateLimitEngaged (Twist velCD) {
         updateEngaged (limitConstraint, value, minValue, maxValue, velCD);
      }

      protected double clipToRange (double value) {
         return clip (value, minValue, maxValue);
      }

      protected double distanceToLimit () {
         if (Math.abs (value-minValue) < Math.abs (value-maxValue)) {
            return value-minValue;
         }
         else {
            return maxValue-value;
         }
      }
   }      

   protected RigidBodyCoupling() {
      initializeConstraintInfo();
   }
   
   /**
    * Returns the penetration tolerance for linear limit constraints.  See
    * {@link #setLinearLimitTol}.
    *
    * @return penetration tolerance for linear limits
    */
   public double getLinearLimitTol() {
      return myLinearLimitTol;
   }
   
   /**
    * Sets the penetration tolerance for linear limit constraints.  Although
    * the coupling does not enforce these limits directly, this information may
    * be used in deciding when to break (set enabled to 0) limit constraints.
    * 
    * @param tol penetration tolerance for linear limits
    */
   public void setLinearLimitTol (double tol) {
      myLinearLimitTol = tol;
   }
   
   /**
    * Returns the penetration tolerance for rotary limit constraints.  See
    * {@link #setRotaryLimitTol}.
    *
    * @return penetration tolerance for rotary limits
    */
   public double getRotaryLimitTol() {
      return myRotaryLimitTol;
   }
   
   /**
    * Sets the penetration tolerance for rotary limit constraints.  Although
    * the coupling does not enforce these limits directly, this information may
    * be used in deciding when to break (set enabled to 0) limit constraints.
    * 
    * @param tol penetration tolerance for rotary limits
    */
   public void setRotaryLimitTol (double tol) {
      myRotaryLimitTol = tol;
   }
   
   /**
    * Sets the minimum speed normal to the constraint surface required to
    * disengage a unilateral constraint. The default value is 0.
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
    * Returns the compliances for all this coupling's constraint directions.
    * The default values are all zero.
    * 
    * @return compliances for this coupling
    */
   public VectorNd getCompliance() {
      int numc = myConstraints.size();
      VectorNd c = new VectorNd(numc);
      for (int i=0; i<numc; i++) {
         c.set(i, myConstraints.get(i).compliance);
      }
      return c;
   }

   /** 
    * Sets compliances for this coupling's constraints.
    * 
    * @param c new compliance values
    */
   public void setCompliance (VectorNd c) {
      int numc = myConstraints.size();
      for (int i=0; i<numc && i<c.size(); i++) {
         myConstraints.get(i).compliance = c.get(i);
      }
   }

   /** 
    * Returns the dampings for all this coupling's constraint directions.
    * The default values are all zero.
    * 
    * @return dampings for this coupling
    */
   public VectorNd getDamping() {
      int numc = myConstraints.size();
      VectorNd d = new VectorNd(numc);
      for (int i=0; i<numc; i++) {
         d.set(i, myConstraints.get(i).damping);
      }
      return d;
   }

   /** 
    * Sets dampings for this coupling's constraints.
    * 
    * @param c new damping values
    */
   public void setDamping (VectorNd c) {
      int numc = myConstraints.size();
      for (int i=0; i<numc && i<c.size(); i++) {
         myConstraints.get(i).damping = c.get(i);
      }
   }

   /** 
    * Returns flags for all this coupling's constraint directions.
    * Values are given as settings of the flags
    * {@link RigidBodyConstraint#BILATERAL}, 
    * {@link RigidBodyConstraint#LINEAR}, 
    * {@link RigidBodyConstraint#ROTARY}, and
    * {@link RigidBodyConstraint#CONSTANT}.
    * 
    * @return flags for this coupling
    */
   public VectorNi getConstraintFlags() {
      int numc = myConstraints.size();
      VectorNi info = new VectorNi(numc);
      for (int i=0; i<numc; i++) {
         info.set(i, myConstraints.get(i).flags);
      }
      return info;
   }

   /**
    * For debugging only
    */
   public void printConstraintInfo() {
      int idx = 0;
      for (RigidBodyConstraint cons : myConstraints) {
         System.out.println("constraint " + idx++ + ":");
         System.out.println("       .flags      = " + cons.flags);
         System.out.println("       .engaged    = " + cons.engaged);
         System.out.println("       .distance   = " + cons.distance);
         System.out.println("       .wrench     = " + cons.wrenchG);
      }
   }
   
   /**
    * Returns the most recently computed bilateral constraint forces, expressed
    * as a wrench in frame G.
    * 
    * @return current bilateral wrench in frame G
    */
   public Wrench getBilateralForceG() {
      return myBilateralWrenchG;
   }

   /**
    * Returns the most recently computed unilateral constraint forces,
    * expressed as a wrench in frame G.
    * 
    * @return current unilateral wrench in frame G
    */
   public Wrench getUnilateralForceG() {
      return myUnilateralWrenchG;
   }

   /**
    * Helper method to set a unilateral constraint as engaged if {@code val} is
    * outside the range ({@code min}, {@code max}).
    */
   protected void maybeSetEngaged (
      RigidBodyConstraint cons, double val, double min, double max) {
      if (val < min) {
         cons.engaged = 1;
      }
      else if (val > max) {
         cons.engaged = -1;
      }
   }

   protected void updateEngaged (
      RigidBodyConstraint cons, double val, 
      double min, double max, Twist velCD) {

      if (val <= min) {
         if (cons.engaged != 1) {
            cons.engaged = 1;
            cons.engagedCnt = 0;
         }
      }
      else if (val >= max) {
         if (cons.engaged != -1) {
            cons.engaged = -1;
            cons.engagedCnt = 0;
         }
      }
      else if (cons.engaged != 0) {
         double dist = getDistance (val, min, max);
         if (dist > 0 && 
             (cons.resetEngaged ||
              (cons.computeContactSpeed(velCD) > myBreakSpeed && 
               cons.engagedCnt > 1))) {
            cons.engaged = 0;
            cons.engagedCnt = 0;
            cons.setMultiplier (0);            
         }
      }
      if (cons.engaged != 0) {
         cons.engagedCnt++;
      }
   }

   /**
    * Helper method to return the distance of a value {@code val} with respect
    * to a range ({@code min}, {@code max}). The distance is always expressed
    * as a negative number.
    */
   protected static double getDistance (double val, double min, double max) {
      if (Math.abs (val-min) < Math.abs (val-max)) {
         return val-min;
      }
      else {
         return max-val;
      }
   }      

   /**
    * Initializes the constraint information, by clearing the constraint and
    * coordinate lists and then calling {@link #initializeConstraints}.  This
    * method is called inside the {@link RigidBodyCoupling} constructor, but
    * may be called in other situations when the constraint configuration
    * changes.
    */
   protected void initializeConstraintInfo() {
      myCoordinates = new ArrayList<>();
      myConstraints = new ArrayList<>();
      myNumUnilaterals = 0;
      myNumBilaterals = 0;
      initializeConstraints ();
      myCoordValues.setSize (myCoordinates.size());
   }

   /**
    * Updates the current constraint information. This is called by {@code
    * BodyConnector} in the ArtiSynth code after the position state of the
    * mechanical system has changed. It calls the coupling-specific
    * method {@link #updateConstraints} and also performs
    * the following default calculations:
    *
    * <ul>
    *   <li>Before calling {@link #updateConstraints}, it updates the {@code
    *   engaged} and {@code distance} attributes for all unilateral constraints
    *   associated with coordinate value limits, if {@code updateEngaged} is
    *   {@code true}.

    *   <li>After calling {@link #updateConstraints}, it computes the
    *   {@code distance} attribute for all bilateral constraints.
    *
    * </ul> The arguments {@code TCD}, {@code TGD}, {@code velCD} and {@code
    * updateEngaged} are passed through to {@link #updateConstraints}, along
    * with {@code TERR} represented as a {@code Twist}.
    * 
    * @param TCD transform from frame C to D
    * @param TGD transform from frame G to D
    * @param TERR error transform from frame C to G
    * @param velGD velocity of frame G wuth respect to D, as seen in frame G
    * @param updateEngaged if {@code true}, update {@code engaged} for the
    * unilateral constraints
    */
   public void updateBodyStates (
      RigidTransform3d TCD, RigidTransform3d TGD, RigidTransform3d TERR,
      Twist velGD, boolean updateEngaged) {

      if (numCoordinates() > 0) {
         for (CoordinateInfo coord : myCoordinates) {
            RigidBodyConstraint cons = coord.limitConstraint;
            if (cons != null) {
               if (updateEngaged) {
                  coord.updateLimitEngaged(velGD);
                  cons.resetEngaged = false;
               }
               if (cons.engaged != 0) {
                  cons.distance = coord.distanceToLimit();
               }
               else {
                  cons.distance = 0;
               }
            }
         }
      }
      myErr.set (TERR);
      updateConstraints (TGD, TCD, myErr, velGD, updateEngaged);
      for (RigidBodyConstraint cons : myConstraints) {
         // compute distance information for bilateral constraints
         if (cons.isBilateral()) {
            cons.distance = cons.wrenchG.dot (myErr);
         }
      }
   }
   
   /**
    * Returns the constraint force currently acting on the {@code idx}-th
    * constraint.
    * 
    * @param idx constraint index
    * @return constraint force 
    */
   public double getConstraintForce (int idx) {
      return myConstraints.get(idx).getMultiplier();
   }

   /**
    * Collects the bilateral constraints for this coupling by appending them to
    * the list {@code bilterals}. The collected constraints are used for the
    * mechanical system solve.

    * @param bilaterals collects the bilateral constraints
    * @return number of bilateral constraints
    */
   public int getBilateralConstraints (
      ArrayList<RigidBodyConstraint> bilaterals) {
      int numb = 0;
      for (RigidBodyConstraint cons : myConstraints) {
         if (cons.isBilateral()) {
            bilaterals.add (cons);
            numb++;
         }
      }
      return numb;
   }

   private int setBilateralForces (double[] buf, double s, int idx) {
      myBilateralWrenchG.setZero();
      for (RigidBodyConstraint cons : myConstraints) {
         if (cons.isBilateral()) {
            double l = buf[idx++]*s;
            cons.setMultiplier (l);
            myBilateralWrenchG.scaledAdd (l, cons.getWrenchG());
         }
      }
      myBilateralWrenchG.inverseTransform (myTCG);
      return idx;      
   }

   /**
    * Sets the bilateral constraint forces (i.e., Lagrange multipliers) that
    * have been computed for this coupling. These are supplied in {@code lam},
    * starting at location {@code idx}, as impulse values that should be scaled
    * by {@code h} to obtain forces. The method should return {@code idx}
    * incremented by the number of bilateral constraints.
    * 
    * @param lam supplies the bilateral impulse values
    * @param h time step value to scale impulses into forces
    * @param idx starting location for impulses in {@code lam}
    * @return updated value for idx
    */
   public int setBilateralForces (VectorNd lam, double h, int idx) {
      return setBilateralForces (lam.getBuffer(), h, idx);
   }
   
   /**
    * Zeros the constraint forces in the coupling.
    */
   public void zeroForces() {
      for (RigidBodyConstraint cons : myConstraints) {
         cons.setMultiplier (0);
      }
      myBilateralWrenchG.setZero();
      myUnilateralWrenchG.setZero();
   }

   private int getBilateralForces (double[] buf, int idx) {
      for (RigidBodyConstraint cons : myConstraints) {
         if (cons.isBilateral()) {
            buf[idx++] = cons.getMultiplier();
         }
      }
      return idx;      
   }
   
   /**
    * Gets the bilateral constraint forces (i.e., Lagrange multipliers) that
    * have most recently set in this coupling. These are returned in {@code
    * lam}, starting at location {@code idx}. The method should return {@code
    * idx} incremented by the number of bilateral constraints.
    * 
    * @param lam returns the bilateral forces
    * @param idx starting location for impulses in {@code lam}
    * @return updated value for idx
    */
   public int getBilateralForces (VectorNd lam, int idx) {
      return getBilateralForces (lam.getBuffer(), idx);
   }

   /**
    * Returns the number of currently engaged unilateral constraints.
    * 
    * @return number of engaged unilateral contraints
    */
   public int numEngagedUnilaterals() {
      int numu = 0;
      for (RigidBodyConstraint cons : myConstraints) {
         if (!cons.isBilateral()) {
            if (cons.engaged != 0) {
               numu++;
            }
         }
      }
      return numu;
   }

   /**
    * Collects the engaged unilateral constraints for this coupling by
    * appending them to the list {@code unilterals}. If {@code updatedEngaged}
    * is {@code true}, the method will first check the engagement of each
    * constraint and break it if certain conditions are met.
    *
    * @param unilaterals collects the unilateral constraints
    * @param updateEngaged if {@code true}, update constraint engagement
    * @return number of engaged unilateral constraints
    */
   public double getUnilateralConstraints (
      ArrayList<RigidBodyConstraint> unilaterals, boolean updateEngaged) {
      double maxpen = 0;
      for (RigidBodyConstraint cons : myConstraints) {
         if (!cons.isBilateral()) {
            if (cons.engaged != 0) {
               Wrench wrC = cons.wrenchG;
               double dist = cons.getDistance();
               if (wrC.m.equals (Vector3d.ZERO)) {
                  // XXX only consider purely translational constraints for
                  // maxdist at the moment
                  if (-dist > maxpen) {
                     maxpen = -dist;
                  }
               }
            }
            if (cons.engaged != 0) {
               unilaterals.add (cons);
            }
         }
      }
      return maxpen;
   }

   /**
    * Called by {@code BodyConnector} in ArtiSynth core to update the
    * unilateral constraints. This is usually done before the velocity solve to
    * account for changes resulting from the position correction step.
    * The constraints to be updated are supplied by {@code unilaterals}
    * starting at {@code idx}.
    *
    * <p>At present, no changes are made to the constraints and so this method
    * does nothing. However, it does perform a sanity check to ensure that the
    * constraints match the current settings in the coupling.
    * 
    * @param unilaterals constraints to be updated
    * @param idx starting index in {@code unilaterals}
    * @param numc number of constraints to update
    */
   public void updateUnilateralConstraints (
      ArrayList<RigidBodyConstraint> unilaterals, int idx, int numc) {
      // just do sanity checking for now;
      // constraints will have been updated in updateBodyStates
      int numu = 0;
      int k = idx;
      for (RigidBodyConstraint cons : myConstraints) {
         if (!cons.isBilateral()) {
            if (cons.engaged != 0) {
               if (k == unilaterals.size()) {
                  throw new IllegalArgumentException (
                     "unilaterals size "+unilaterals.size()+
                     " differs from that returned by getUnilateralConstraints()");
               }
               if (cons != unilaterals.get (k++)) {
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

   private int setUnilateralForces (double[] buf, double s, int idx) {
      myUnilateralWrenchG.setZero();
      for (RigidBodyConstraint cons : myConstraints) {
         if (!cons.isBilateral() && cons.engaged != 0) {
            double l = buf[idx++]*s;
            cons.setMultiplier (l);
            myUnilateralWrenchG.scaledAdd (l, cons.getWrenchG());
         }
      }
      myUnilateralWrenchG.inverseTransform (myTCG);
      return idx;      
   }

    /**
    * Sets the unilateral constraint forces (i.e., Lagrange multipliers) that
    * have been computed for the currently engaged unilateral constraints in
    * this coupling. These are supplied in {@code the}, starting at location
    * {@code idx}, as impulse values that should be scaled by {@code h} to
    * obtain forces. The method should return {@code idx} incremented by the
    * number of currently engaged unilateral constraints.
    * 
    * @param the supplies the unilateral impulse values
    * @param h time step value to scale impulses into forces
    * @param idx starting location for impulses in {@code the}
    * @return updated value for idx
    */
   public int setUnilateralForces (VectorNd the, double h, int idx) {
      return setUnilateralForces (the.getBuffer(), h, idx);
   }

   private int getUnilateralForces (double[] buf, int idx) {
      for (RigidBodyConstraint cons : myConstraints) {
         if (!cons.isBilateral() && cons.engaged != 0) {
            buf[idx++] = cons.getMultiplier();
         }
      }
      return idx;      
   }

   /**
    * Gets the unilateral constraint forces (i.e., Lagrange multipliers) that
    * have most recently been set for the currently engaged unilateral
    * constraints in this coupling. These are returned in {@code the}, starting
    * at location {@code idx}. The method should return {@code idx} incremented
    * by the number of currently engaged unilateral constraints.
    * 
    * @param the returns the unilateral forces
    * @param idx starting location for impulses in {@code the}
    * @return updated value for idx
    */
   public int getUnilateralForces (VectorNd the, int idx) {
      return getUnilateralForces (the.getBuffer(), idx);
   }

   /**
    * Returns the maximum number of constraints associated with this
    * coupling. This is the sum of {@link #numBilaterals} and {@link
    * #numUnilaterals}.
    * 
    * @return maximum number of constraints
    */
   public int numConstraints() {
      return myConstraints.size();
   }

   /**
    * Project a transform TCD onto the nearest transform TGD that is legal
    * given this coupling's <i>bilateral</i> constraints. In more detail, given
    * a transform TCD from frame C to D, find the frame G that is nearest to C
    * while lying on the constraint surface defined by the bilateral
    * constraints. Subclasses should implement this method as needed.
    *
    * <p>Optionally, the coupling may also extend the projection to include
    * unilateral constraints that are <i>not</i> associated with coordinate
    * limits. In particular, this should be done for constraints for which is
    * it desired to have the constraint error included in the {@code errC}
    * argument that is passed to {@link #updateConstraints}.
    * 
    * <p>If this coupling supports coordinates and {@code coords} is non-{@code
    * null}, then the coordinate values corresponding to {@code TGD} should
    * also be computed and returned in {@code coords}. The easiest way to do
    * this is to simply call {@link #TCDToCoordinates}, although in some cases
    * it may be computationally cheaper to compute both the coordinates and the
    * projection at the same time. The method should <i>not</i> clip
    * the resulting coordinates to their range limits.
    * 
    * @param TGD
    * returns the nearest transform to {@code TCD} that is legal with
    * respect to the bilateral (and possibly some unilateral) constraints
    * @param TCD
    * transform from frame C to D to be projected
    * @param coords if non-{@code null}, should be used to return coordinate
    * values
    */
   public abstract void projectToConstraints (
      RigidTransform3d TGD, RigidTransform3d TCD, VectorNd coords);

   public void projectAndUpdateCoordinates (
      RigidTransform3d TGD, RigidTransform3d TCD) {
      projectToConstraints (TGD, TCD, myCoordValues);
      doSetCoords (myCoordValues);
   }
   
   /**
    * Returns the number of bilateral constraints associated with this coupling.
    * 
    * @return number of bilateral constraints
    */
   public int numBilaterals() {
      return myNumBilaterals;
   }

   /**
    * Returns the number of unilateral constraints associated with this
    * coupling, engaged or otherwise.
    * 
    * @return number of unilateral constraints
    */
   public int numUnilaterals() {
      return myNumUnilaterals;
   }

   /**
    * Called from {@link #updateBodyStates} to update the constraints, usually
    * once per simulation step. This method is responsible for:
    *
    * <ul>
    *
    *   <li>Updating the values of all non-constant constraint wrenches,
    *   along with their derivatives;
    *
    *   <li>If {@code updateEngaged} is {@code true}, updating the {@code
    *   engaged} and {@code distance} attributes for all unilateral constraints
    *   not associated with a joint limit.
    *
    * </ul>
    *
    * <p>Wrenches and their derivatives should be computed with respect to
    * frame G, which is frame C with the constraint errors removed.
    *
    * <p>If the coupling supports coordinates, their values will be updated
    * before this method is called so as to correspond to {@code TGD}.
    * 
    * @param TGD idealized joint transform from frame G to D, obtained
    * by calling {@link #projectToConstraints} on {@code TCD}
    * @param TCD actual joint transform from frame C to D; included
    * for legacy reasons and not normally used
    * @param errC error transform from frame C to G, represented as a 
    * {@code Twist}
    * @param velGD velocity of frame G with respect to D, as seen in frame G
    * @param updateEngaged if {@code true}, requests the updating of unilateral
    * {@code engaged} and {@code distance} attributes as describe above.
    */
   public abstract void updateConstraints (
      RigidTransform3d TGD, RigidTransform3d TCD, Twist errC, 
      Twist velGD, boolean updateEngaged);

   /**
    * Called inside the {@link RigidBodyCoupling} constructor to allocate
    * constraint and coordinate information, using calls to the {@code
    * addConstraint} and {@code addCoordinate} methods. The method may be called
    * at other times if the constraints need to be reconfigured (such as when
    * switching constraints between bilateral and unilateral).  Subclasses
    * should implement this method as needed.
    */
   public abstract void initializeConstraints ();

   /**
    * Transforms the geometry of this coupling. Subclasses should reimplement
    * this as needed.
    * 
    * @param gtr
    * transformer implementing the transformation
    * @param TCW
    * current transform from C to world
    * @param TDW
    * current transform from D to world
    */
   public void transformGeometry (
      GeometryTransformer gtr, RigidTransform3d TCW, RigidTransform3d TDW) {
   
   }

   /**
    * Scales the distance units of this constraint. Subclasses should
    * reimplement this as needed.
    * 
    * @param s scale factor
    */
   public void scaleDistance (double s) {
   }

   /**
    * Stores the state for this coupling. Used by the {@code BodyConnector}
    * class in the ArtiSynth core code.
    * 
    * @param data buffer in which to store state
    */
   public void getState (DataBuffer data) {

      if (numUnilaterals() > 0) {
         for (RigidBodyConstraint cons : myConstraints) {
            if (!cons.isBilateral()) {
               data.zput (cons.engaged);
               data.zput (cons.engagedCnt);
               //data.dput (cons.coordinate);
            }
         }
      }
      if (numCoordinates() > 0) {
         for (int i=0; i<myCoordinates.size(); i++) {
            data.dput (myCoordinates.get(i).value);
         }
      }
   }
   
   /**
    * Loads the state for the coupling. Used by the {@code BodyConnector} class
    * in the ArtiSynth core code.
    * 
    * @param data buffer from which state is loaded
    */
   public void setState (DataBuffer data) {

      if (numUnilaterals() > 0) {
         for (RigidBodyConstraint cons : myConstraints) {
            if (!cons.isBilateral()) {
               cons.engaged = data.zget();
               cons.engagedCnt = data.zget();
               //cons.coordinate = data.dget();
            }
         }
      }
      if (numCoordinates() > 0) {
         for (int i=0; i<myCoordinates.size(); i++) {
            myCoordinates.get(i).value = data.dget();
         }
      }
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

   /**
    * Called inside {@link #initializeConstraints} to add a new coordinate.
    * It is assumed that the coordinate has no range limits.
    *
    * @return information structure for the new coordinate
    */
   protected CoordinateInfo addCoordinate () {
      CoordinateInfo cons = new CoordinateInfo();
      myCoordinates.add (cons);
      return cons;
   }

   /**
    * Called inside {@link #initializeConstraints} to add a new coordinate.
    * Range limits are specified, along with a unilateral constraint that will
    * be activated when the coordinate goes out of range. This constraint must
    * have been added previously in {@link #initializeConstraintInfo} using
    * {@link #addCoordinate}.
    *
    * @param min minimum range value 
    * @param min maximum range value
    * @param flags reserved for future use; currently ignore
    * @param cons unilateral constraint that gets activated when the coordinate
    * goes out of range
    * @return information structure for the new coordinate
    */
   protected CoordinateInfo addCoordinate (
      double min, double max, int flags, RigidBodyConstraint cons) {
      CoordinateInfo cinfo = new CoordinateInfo(min, max, flags, cons);
      myCoordinates.add (cinfo);
      return cinfo;
   }

   /**
    * Returns information for the {@code idx}-th coordinate that was added
    * inside {@link #initializeConstraintInfo}.
    *
    * @param idx coordinate index
    * @return information for the {@code idx}-th coordinate
    */
   protected CoordinateInfo getCoordinate (int idx) {
      return myCoordinates.get(idx);
   }

    /**
    * Returns the number of coordinates associated with this coupling. If
    * coordinates are not supported, this method returns 0.
    *
    * @return number of associated coordinates
    */
   public int numCoordinates() {
      return myCoordinates.size();
   }

   /**
    * Called inside {@link #initializeConstraints} to add a new constraint
    * to the coupling. Information about the constraint is described in {@code
    * flags}, which should be an or-ed combination of the flags {@link
    * #BILATERAL}, {@link #LINEAR}, and {@link #ROTARY}.
    *
    * @oaram flags bitmask of flags decsribing the constraint
    * @return newly allocated constraint structure
    */
   protected RigidBodyConstraint addConstraint (int flags) {
      return addConstraint (flags, null);
   }

   /**
    * Called inside {@link #initializeConstraints} to add a new constraint
    * to the coupling. Information about the constraint is described in {@code
    * flags}, which should be an or-ed combination of {@link #BILATERAL},
    * {@link #ROTARY}, {@link #LINEAR}, and {@link #CONSTANT}.
    *
    * A value for the constraint wrench (in frame G) may also be specified;
    * if this is done, it is assumed that the constraint is constant and
    * the flag {@link #CONSTANT} is automatically set.
    *
    * @oaram flags bitmask of flags describing the constraint
    * @oaram wrenchG optional value of the constraint wrench in frame G
    * @return newly allocated constraint structure
    */
   protected RigidBodyConstraint addConstraint (int flags, Wrench wrenchG) {
      RigidBodyConstraint cons = new RigidBodyConstraint();
      if (wrenchG != null) {
         flags |= CONSTANT;
         cons.wrenchG.set (wrenchG);
      }
      if ((flags & BILATERAL) != 0) {
         myNumBilaterals++;
      }
      else {
         myNumUnilaterals++;
      }
      cons.setFlags(flags);
      cons.index = myConstraints.size();
      myConstraints.add (cons);
      return cons;
   }

   /**
    * Returns the {@code idx}-th constraint that was added inside {@link
    * #initializeConstraintInfo}.
    *
    * @param idx constraint index
    * @return {@code idx}-th constraint
    */
   public RigidBodyConstraint getConstraint (int idx) {
      return myConstraints.get(idx);
   }

   /**
    * Returns (and possibly updates) the current coordinate values for this
    * coupling, if coordinates are supported. If {@code TGD} is non-{@code
    * null}, it supplies the current value of the TCD transform, which is then
    * projected to the constraint surface to form TGD and update the coordinate
    * values, with TGD returned in {@code TCD}. Otherwise, if {@code TGD} is
    * {@code null}, the method simply returns the currently stored coordinate
    * values. If {@link #numCoordinates} returns 0, this method does nothing.
    *
    * @param coords returns the coordinates. Its size will be set to {@link
    * #numCoordinates}.
    * @param TGD if non-{@code null}, provides TCD on input and TGD on output.
    */
   public void getCoordinates (VectorNd coords, RigidTransform3d TGD) {
      int numc = numCoordinates();
      if (numc > 0) {
         if (TGD != null) {
            // on entry, TGD is set to TCD. It is then projected to TGD
            projectAndUpdateCoordinates (TGD, TGD);
         }
         coords.setSize(numc);
         doGetCoords (coords);         
      }
   }

   /**
    * Sets the coordinate values for this coupling, if coordinates are
    * supported. Otherwise this method does nothing.
    *
    * @param coords new coordinate values. Must have a length &gt;= {@link
    * #numCoordinates}.
    * @param TGD if non-{@code null}, returns the corresponding TGD transform
    */
   public void setCoordinateValues (VectorNd coords, RigidTransform3d TGD) {
      int numc = numCoordinates();
      if (numc > 0) {
         if (coords.size() < numc) {
            throw new IllegalArgumentException (
               "coords has size "+coords.size()+", must be >= "+numc);
         }
         for (int i=0; i<numc; i++) {
            myCoordinates.get(i).clipAndSetValue (coords.get(i));
         }
         if (TGD != null) {
            coordinatesToTCD (TGD, coords);
         }
      }
   }

   /**
    * Computes the TCD transform for a set of coordinates, if coordinates are
    * supported. Otherwise this method does nothing. Subclasses should
    * implement this method as needed.
    *
    * @param TCD returns the TCD transform
    * @param coords supplies the coordinate values and
    * must have a length &gt;= {@link #numCoordinates}.
    */
   public abstract void coordinatesToTCD (
      RigidTransform3d TCD, VectorNd coords);
   
   /**
    * Computes the TCD transform for the connector's current coordinate values,
    * if coordinates are supported. Otherwise {@code TCD} is set to the
    * identity.
    *
    * @param TCD returns the TCD transform
    */
   public void coordinatesToTCD (RigidTransform3d TCD) {
      int numc = numCoordinates();
      if (numc > 0) {
         VectorNd coords = new VectorNd(numc);
         doGetCoords (coords);
         coordinatesToTCD (TCD, coords);
      }
      else {
         TCD.setIdentity();
      }
   }

   private void doGetCoords (VectorNd vec) {
      for (int i=0; i<myCoordinates.size(); i++) {
         vec.set (i, myCoordinates.get(i).value);
      }
   }      

   private void doSetCoords (VectorNd vec) {
      for (int i=0; i<vec.size(); i++) {
         myCoordinates.get(i).value = vec.get(i);
      }
   }      

   /**
    * If coordinates are supported by this coupling, compute their values based
    * on the supplied transform TCD from frame C to D, and return the result in
    * {@code coords}. Otherwise this method does nothing.
    *
    * <p> It is assumed that {@code TCD} is legal with respect the coupling's
    * bilateral constraints, as defined by {@link #projectToConstraints};
    * otherwise, {@link #projectToConstraints} should be used instead.
    *
    * <p>When setting coordinate values, they should <i>not</i> be clipped to
    * their maximum and minimum values (as defined by {@link
    * #getCoordinateRange}.
    * 
    * @param coords returns the coordinate values
    * @param TCD transform from frame C to D
    */
   public void TCDToCoordinates (VectorNd coords, RigidTransform3d TCD) {
   }

   /**
    * Sets the value for the {@code idx}-ith coordinate supported by this
    * coupling. An exception will be generated if coordinates are not
    * supported.
    *
    * <p>If {@code TGD} is non-{@code null}, it is assumed to contain a
    * transform TCD that this method will first use to update the other
    * coordinate values, after projecting it onto the nearest bilateral-legal
    * transform TGD using {@link #projectToConstraints}. The specified
    * coordinate value is then set, and {@code TGD} is recomputed from the
    * updated coordinates.
    * 
    * @param idx coordinate index
    * @param value new coordinate value
    * @param TGD if non-{@code null}, is used to update the other coordinate
    * values, and then return the final TGD resulting from the new coordinates
    */
   public void setCoordinateValue (
      int idx, double value, RigidTransform3d TGD) {
      if (TGD != null) {
         // on entry, TGD is set to TCD. It is then projected to TGD
         projectAndUpdateCoordinates (TGD, TGD);
      }    
      myCoordinates.get(idx).value = value; //setValue (value);
      VectorNd coords = new VectorNd (numCoordinates());
      doGetCoords (coords);
      if (TGD != null) {
         coordinatesToTCD (TGD, coords);
      }
   }

  /**
    * Returns the current value for the {@code idx}-ith coordinate supported by
    * this coupling. An exception will be generated if coordinates are not
    * supported.
    *
    * <p>If {@code TGD} is non-{@code null}, it is assumed to contain a
    * transform TCD that this method will first use to update the coordinate
    * values, after projecting it onto the nearest bilateral-legal transform
    * TGD using {@link #projectToConstraints}. The projected value is returned
    * in {@code TGD}.
    * 
    * @param idx coordinate index
    * @param TGD if non-{@code null}, is projected onto the contraints and used
    * to update the coordinate values
    * @return current value
    */
   public double getCoordinate (int idx, RigidTransform3d TGD) {
      if (TGD != null) {
         // on entry, TGD is set to TCD. It is then projected to TGD
         projectAndUpdateCoordinates (TGD, TGD);
      }
      return myCoordinates.get(idx).value;
   }

   /**
    * Returns the range for the {@code idx}-ith coordinate supported by this
    * coupling. An exception will be generated if coordinates are not
    * supported.
    * 
    * @param idx coordinate index
    * @return range for the coordinate
    */
   public DoubleInterval getCoordinateRange (int idx) {
      CoordinateInfo cons = myCoordinates.get(idx);
      return new DoubleInterval (cons.minValue, cons.maxValue);
   }

   /**
    * Clips {@code value} to the range for the {@code idx}-ith coordinate
    * supported by this coupling. An exception will be generated if coordinates
    * are not supported.
    * 
    * @param idx idx coordinate index
    * @param value value to be clipped
    * @return clipped value
    */
   public double clipCoordinate (int idx, double value) {
      return myCoordinates.get(idx).clipToRange (value);
   }

   /**
    * Sets the range for the {@code idx}-ith coordinate supported by this
    * coupling. An exception will be generated if coordinates are not
    * supported.
    * 
    * @param idx coordinate index
    * @param range range for the coordinate
    */
   public void setCoordinateRange (int idx, DoubleInterval range) {
      CoordinateInfo cons = myCoordinates.get(idx);
      cons.minValue = range.getLowerBound();
      cons.maxValue = range.getUpperBound();
   }

   /**
    * Transform, in place, a vector and its derivative from frame D to frame
    * G, given the rotation RGD and the angular velocity wGD of G wrt D (in
    * G).  This uses the formula
    * 
    * <pre>
    * G           D
    *  vec  = RDG  vec
    *  
    * G           D        G
    *  dvec = RDG  dvec +   vec X wGD
    * </pre>
    *
    * @param vec on input, vector in D; on output, vector in D
    * @param dvec on input, vector derivative in D; on output, vector
    * derivative in D
    * @param RGD rotation matrix from G to D (transpose will be used
    * to obtain RDG).
    * @param wGD angular velocity of G wrt D, in G
    */
   protected void transformDtoG (
      Vector3d vec, Vector3d dvec, RotationMatrix3d RGD, Vector3d wGD) {

      vec.inverseTransform (RGD);
      dvec.inverseTransform (RGD);
      dvec.crossAdd (vec, wGD, dvec);
   }

   public RigidBodyCoupling clone() {
      RigidBodyCoupling copy;
      try {
         copy = (RigidBodyCoupling)super.clone();
      }
      catch (Exception e) {
         throw new InternalErrorException ("Can't clone RigidBodyCoupling");
      }

      copy.myTCG = new RigidTransform3d();
      copy.myErr = new Twist();
      copy.myBilateralWrenchG = new Wrench();
      copy.myUnilateralWrenchG = new Wrench();
      copy.initializeConstraintInfo();

      // copy coordinate info
      for (int i=0; i<numCoordinates(); i++) {
         CoordinateInfo dest = copy.myCoordinates.get(i);
         CoordinateInfo src = myCoordinates.get(i);
         dest.minValue = src.minValue;
         dest.maxValue = src.maxValue;
         dest.value = src.value;
      }
      return copy;
   }

   public static void main (String[] args) {
      System.out.println (getDistance (1, -INF, INF));
      System.out.println (getDistance (1, -INF, 10));
      System.out.println (getDistance (10, -INF, 1));
      System.out.println (getDistance (10, -INF, 10));
      System.out.println (getDistance (1, -1, INF));
      System.out.println (getDistance (1, 2, INF));
      System.out.println (getDistance (1, 1, INF));
   }
}
