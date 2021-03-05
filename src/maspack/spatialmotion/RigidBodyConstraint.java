/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.spatialmotion;

import maspack.matrix.Vector3d;

/**
 * Contains information about individual constraints between rigid bodies
 * which are used to enforce the couplings implemented by subclasses of
 * {@link RigidBodyCoupling}.
 */
public class RigidBodyConstraint {
   
   /** 
    * Constraint is bilateral.
    */
   public static final int BILATERAL = 0x1;

   /** 
    * Constraint primarily restricts translational motion.
    */
   public static final int LINEAR    = 0x2;

   /** 
    * Constraint primarily restricts rotary motion.
    */
   public static final int ROTARY    = 0x4;

   /** 
    * Constraint in frame G is constant.
    */
   public static final int CONSTANT  = 0x8;

   /** 
    * Constraint enforces a coordinate limit.
    */
   public static final int LIMIT  = 0x10;

   // parameters describing the constraint and its behavior:

   int flags;           // flags describing the constraint
   int index;           // index within constraint list
   double compliance;   // compliance; makes constraint soft if > 0
   double damping;      // damping; used only if compliance > 0
   double friction;     // friction coefficient

   // quantities that (may) evolve as the simulation progresses

   Wrench wrenchG;      // wrench in coordinate frame G
   Wrench dotWrenchG;   // time derivative of wrenchG
   double distance;     // distance from the constraint
   double multiplier;   // Lagrange multiplier (force) enforcing the constraint
   int engaged;         // always 1 for bilaterals; -1, 0, or 1 for unilaterals
   int engagedCnt;      // no. of time steps a unilateral has been engaged
   
   // for unilateral contraints:
   
   boolean resetEngaged;// explicitly requests engaged to be reset
   // note: coordinate is deprecated - use RigidBodyCoupling.CoordinateInfo
   //public double coordinate;   // coordinate value, if any

   int solveIndex;      // column index of this constraint in either GT or NT, 
                        // where GT and NT are the transposed bilateral and 
                        // unilateral constraint matrices used to solve the 
                        // mechanical system
   
   public Wrench getWrenchG() {
      return wrenchG;
   }
   
   public void setWrenchG (
      double fx, double fy, double fz, double mx, double my, double mz) {
      wrenchG.set (fx, fy, fz, mx, my, mz);
   }

   public void setWrenchG (Vector3d f, Vector3d m) {
      if (f != null) {
         wrenchG.f.set(f);
      }
      if (m != null) {
         wrenchG.m.set(m);
      }
   }

   public void setWrenchG (Wrench wr) {
      wrenchG.set (wr);
   }

   public void zeroWrenchG () {
      wrenchG.setZero();
   }
   
   public void negateWrenchG () {
      wrenchG.negate();
   }
   
   public Wrench getDotWrenchG() {
      return dotWrenchG;
   }
   
   public void setDotWrenchG (
      double fx, double fy, double fz, double mx, double my, double mz) {
      dotWrenchG.set (fx, fy, fz, mx, my, mz);
   }

   public void setDotWrenchG (Vector3d f, Vector3d m) {
      if (f != null) {
         dotWrenchG.f.set(f);
      }
      if (m != null) {
         dotWrenchG.m.set(m);
      }
   }

   public void setDotWrenchG (Wrench wr) {
      dotWrenchG.set (wr);
   }

   public void zeroDotWrenchG () {
      dotWrenchG.setZero();
   }
   
   public void negateDotWrenchG () {
      dotWrenchG.negate();
   }
   
   public RigidBodyConstraint() {
      wrenchG = new Wrench();
      dotWrenchG = new Wrench();
      distance = 0;
      engaged = 1;
   }

   public void setFlags (int flags) {
      this.flags = flags;
      engaged = (isBilateral() ? 1 : 0);
   }
   
   public void setUnilateral (boolean enabled) {
      if (enabled) {
         flags = (flags & ~BILATERAL);
         engaged = 0;
      }
      else {
         flags = (flags | BILATERAL);
         engaged = 1;
      }
   }
   
   public boolean isBilateral() {
      return (flags & BILATERAL) != 0;
   }

   public boolean isUnilateral() {
      return (flags & BILATERAL) == 0;
   }

   public boolean isConstant() {
      return (flags & CONSTANT) != 0;
   }

   public boolean isRotary() {
      return (flags & ROTARY) != 0;
   }

   public boolean isLinear() {
      return (flags & LINEAR) != 0;
   }

   public int getSolveIndex() {
      return solveIndex;
   }

   public void setSolveIndex (int idx) {
      solveIndex = idx;
   }

   public void setMultiplier (double lam) {
      multiplier = lam;
   }

   public double getMultiplier() {
      return multiplier;
   }

   public void setDistance (double d) {
      distance = d;
   }

   public double getDistance() {
      return distance;
   }

   public void setCompliance (double c) {
      compliance = c;
   }

   public double getCompliance() {
      return compliance;
   }

   public void setDamping (double c) {
      damping = c;
   }

   public double getDamping() {
      return damping;
   }

   public double computeContactSpeed (Twist velCD) {
      return engaged*wrenchG.dot (velCD);
   }

   public void setFriction (double f) {
      friction = f;
   }

   public double getFriction() {
      return friction;
   }
   
   public int getIndex() {
      return index;
   }

   public int getEngaged() {
      return engaged;
   }
   
   public void setEnaged (int eng) {
      engaged = eng;
   }
   
   public int getEngagedCnt() {
      return engagedCnt;
   }
   
   
   
}
