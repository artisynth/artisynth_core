/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.*;
import java.io.*;

import maspack.matrix.*;
import maspack.properties.*;
import maspack.util.*;
import maspack.spatialmotion.*;
import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.MechSystem.FrictionInfo;
import artisynth.core.mechmodels.MechSystem.ConstraintInfo;
import artisynth.core.util.*;

public abstract class RigidBodyConnector extends RenderableComponentBase
   implements ScalableUnits, TransformableGeometry, RigidBodyConstrainer,
              Constrainer, HasCoordinateFrame {
              
   protected RigidBody myBodyA;
   protected RigidBody myBodyB;
   private boolean myEnabledP = true;
   protected RigidBodyCoupling myCoupling;

   MatrixNdBlock myBilateralBlkA;
   MatrixNdBlock myBilateralBlkB;
   MatrixNdBlock myUnilateralBlkA;
   MatrixNdBlock myUnilateralBlkB;

   protected VectorNd myCompliance = null;
   protected VectorNd myDamping = null;

   protected double myLinearCompliance = 0;
   protected double myRotaryCompliance = 0;

   ArrayList<RigidBodyConstraint> myBilaterals;
   ArrayList<RigidBodyConstraint> myUnilaterals;

   protected static final double DEFAULT_PENETRATION_TOL = 0.0001;
   protected double myPenetrationTol = DEFAULT_PENETRATION_TOL;
   protected PropertyMode myPenetrationTolMode = PropertyMode.Inherited;

   public static PropertyList myProps =
      new PropertyList (
         RigidBodyConnector.class, RenderableComponentBase.class);

   RigidTransform3d myTCA = new RigidTransform3d();
   RigidTransform3d myTDB = new RigidTransform3d();
   
   RigidTransform3d myTGA = new RigidTransform3d();
   RigidTransform3d myTGB = new RigidTransform3d();

   MatrixNd myPiAC;
   MatrixNd myPiBC;

   PolarDecomposition3d myPolarCA;
   PolarDecomposition3d myPolarCB;

   static {
      myProps.add (
         "linearCompliance", "compliance along linear directions", 0, "[0,inf]");
      myProps.add (
         "rotaryCompliance", "compliance along rotary directions", 0, "[0,inf]");
      myProps.add ("enabled isEnabled *", "constraint is enabled", true);
      // myProps.add (
      //    "penetrationTol", "collision penetration tolerance",
      //    DEFAULT_PENETRATION_TOL);
      myProps.addReadOnly (
         "bilateralForceInA", "bilateral constraint force as seen in body A");
      myProps.addReadOnly (
         "unilateralForceInA", "unilateral constraint force as seen in frame A");
      myProps.addInheritable (
         "penetrationTol:Inherited", "collision penetration tolerance",
         DEFAULT_PENETRATION_TOL);      
   }

   /**
    * Sets the penetration tolerance for this component. Setting a
    * value of -1 will cause a default value to be computed based
    * on the radius of the topmost MechModel.
    *
    * @param tol new penetration tolerance 
    */
   public void setPenetrationTol (double tol) {
      if (tol < 0) {
         tol = CollisionManager.getDefaultPenetrationTol (
            this, DEFAULT_PENETRATION_TOL);
      }
      myPenetrationTol = tol;
      myPenetrationTolMode =
         PropertyUtils.propagateValue (
            this, "penetrationTol", tol, myPenetrationTolMode);
   }

   public double getPenetrationTol () {
      return myPenetrationTol;
   }

   public PropertyMode getPenetrationTolMode() {
      return myPenetrationTolMode;
   }

   public void setPenetrationTolMode (PropertyMode mode) {
      myPenetrationTolMode =
         PropertyUtils.setModeAndUpdate (
            this, "penetrationTol", myPenetrationTolMode, mode);
   }

   /**
    * Returns true if the constraint is associated with linear compliance.
    * This will be true is the constraint is linear, and is also either
    * bilateral *or* there is only a single constraint associated with the
    * coupling. The latter condition is a hack to allow the planar constraints
    * to accept linear compliance in either bilateral or unilateral mode;
    * otherwise, unilateral constraints are not considered because we don't set
    * compliance for the joint limit constraints.
    */
   private boolean usesLinearCompliance (int flags) {
      if ((flags & RigidBodyCoupling.LINEAR) != 0) {
         if ((flags & RigidBodyCoupling.BILATERAL) != 0 ||
             myCoupling.maxConstraints() == 1) {
            return true;
         }
      }
      return false;
   }
 
   /** 
    * Returns true if the constraint is associated with rotary compliance.
    * This will be true is the constraint is rotary and bilateral.  Unilateral
    * constraints are not considered because we don't set compliance for the
    * joint limit constraints.
    */
   private boolean usesRotaryCompliance (int flags) {
      if ((flags & RigidBodyCoupling.ROTARY) != 0) {
         if ((flags & RigidBodyCoupling.BILATERAL) != 0) {
            return true;
         }
      }
      return false;
   }

   /**
    * Returns the most recently computed bilateral constraint force as seen in
    * the coordinate frame of body A. This method is more efficient than than
    * {@link #getBilateralForceInA} since it does not allocate a Wrench object.
    *
    * @param wr returns the constraint force
    */
   public void getBilateralForceInA (Wrench wr) {
      wr.set (myCoupling.getBilateralForceG());
      wr.transform (myTCA);
   }

   /**
    * Returns the most recently computed bilateral constraint force as seen in
    * the coordinate frame of body A.
    *
    * @return bilateral constraint force in body A
    */
   public Wrench getBilateralForceInA() {
      Wrench wr = new Wrench();
      getBilateralForceInA (wr);
      return wr;
   }

   /**
    * Returns the most recently computed unilateral constraint force as seen in
    * the coordinate frame of body A. This method is more efficient than than
    * {@link #getUnilateralForceInA} since it does not allocate a Wrench object.
    *
    * @param wr returns the constraint force
    */
   public void getUnilateralForceInA (Wrench wr) {
      wr.set (myCoupling.getUnilateralForceG());
      wr.transform (myTCA);
   }

   /**
    * Returns the most recently computed unilateral constraint force as seen in
    * the coordinate frame of body A.
    *
    * @return unilateral constraint force in body A
    */
   public Wrench getUnilateralForceInA() {
      Wrench wr = new Wrench();
      getUnilateralForceInA (wr);
      return wr;
   }

   public double getLinearCompliance () {
      return myLinearCompliance;
   }

   public void setLinearCompliance (double c) {
      myLinearCompliance = c;
      // compute damping to give critical damping
      double d = (c != 0) ? 2*Math.sqrt(getAverageBodyMass()/c) : 0;
      VectorNi flags = myCoupling.getConstraintInfo();
      VectorNd compliance = new VectorNd(myCoupling.getCompliance());
      VectorNd damping = new VectorNd(myCoupling.getDamping());
      for (int i=0; i<flags.size(); i++) {
         if (usesLinearCompliance (flags.get(i))) {
            compliance.set (i, c);
            damping.set (i, d);
         }
      }      
      myCoupling.setCompliance (compliance);
      myCoupling.setDamping (damping);
   }

   public double getRotaryCompliance () {
      return myRotaryCompliance;
   }

   public void setRotaryCompliance (double c) {
      if (c < 0) {
         c = 0;
      }
      myRotaryCompliance = c;
      // compute damping to give critical damping
      double d = (c != 0) ? 2*Math.sqrt(getAverageRevoluteInertia()/c) : 0;
      VectorNi flags = myCoupling.getConstraintInfo();
      VectorNd compliance = new VectorNd(myCoupling.getCompliance());
      VectorNd damping = new VectorNd(myCoupling.getDamping());
      for (int i=0; i<flags.size(); i++) {
         if (usesRotaryCompliance (flags.get(i))) {
            compliance.set (i, c);
            damping.set (i, d);
         }
      }      
      myCoupling.setCompliance (compliance);
      myCoupling.setDamping (damping);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public VectorNd getCompliance() {
      return myCoupling.getCompliance();
   }

   /** 
    * Check whether a specific set of compliance and damping values match those
    * determined from the linear and rotary compliance values. If not,
    * then the linear and/or rotary compliance values are set to -1.
    */   
   private void checkConformity (VectorNd compliance, VectorNd damping) {

      boolean hasLinear = false;
      boolean hasRotary = false;

      double lc = 0;
      double ld = 0;
      double rc = 0;
      double rd = 0;

      VectorNi flags = myCoupling.getConstraintInfo();
      for (int i=0; i<flags.size(); i++) {
         double c = compliance.get(i);
         double d = damping.get(i);
         if (usesLinearCompliance (flags.get(i))) {
            if (!hasLinear) {
               hasLinear = true;
               lc = (c >= 0) ? c : -1;
               ld = (lc > 0) ? 2*Math.sqrt(getAverageBodyMass()/lc) : 0;
            }
            if (lc != -1 && (c != lc || d != ld)) {
               lc = -1;
            }
         }
         else if (usesRotaryCompliance (flags.get(i))) {
            if (!hasRotary) {
               hasRotary = true;
               rc = (c >= 0) ? c : -1;
               rd = (rc > 0) ? 2*Math.sqrt(getAverageRevoluteInertia()/rc) : 0;
            }
            if (rc != -1 && (c != rc || d != rd)) {
               rc = -1;
            }
         }
      }
      if (hasLinear) {
         myLinearCompliance = lc;
      }
      if (hasRotary) {
         myRotaryCompliance = rc;
      }
   }

   public void setCompliance (VectorNd compliance) {
      myCoupling.setCompliance(compliance);
      VectorNd damping = new VectorNd(myCoupling.getDamping());
      checkConformity (compliance, damping);
   }
   
   public VectorNd getDamping() {
      return myCoupling.getDamping();
   }
   
   public void setDamping (VectorNd damping) {
      myCoupling.setDamping(damping);
      VectorNd compliance = new VectorNd(myCoupling.getCompliance());
      checkConformity (compliance, damping);
   }
   
   public void setEnabled (boolean enabled) {
      if (enabled != myEnabledP) {
         myEnabledP = enabled;
      }
      notifyParentOfChange (StructureChangeEvent.defaultEvent);
   }

   public boolean isEnabled() {
      return myEnabledP;
   }

   protected RigidBodyConnector() {
   }

   public int numBodies() {
      return myBodyB == null ? 1 : 2;
   }

   public RigidBody getBodyA() {
      return myBodyA;
   }

   public RigidBody getBodyB() {
      return myBodyB;
   }

   protected double getAverageBodyMass() {
      double mass = 0;
      if (myBodyA != null) {
         mass += myBodyA.getMass();
      }
      if (myBodyB != null) {
         mass += myBodyB.getMass();
      }
      return mass/2;
   }

   /** 
    * Returns the average inertia of a specified body about the compliance frame.
    */
   private double getAverageRevoluteInertia (RigidBody bod, RigidTransform3d T) {
      SpatialInertia inertia = bod.getInertia();
      Vector3d com = new Vector3d(); // vector from COM to compliance frame
      com.sub (T.p, inertia.getCenterOfMass());
      double l = com.norm();
      return inertia.getRotationalInertia().trace()/3 + inertia.getMass()*l*l;
   }

   protected double getAverageRevoluteInertia() {
      double revi = 0;
      if (myBodyA != null) {
         revi += getAverageRevoluteInertia (myBodyA, getTCA());
      }
      if (myBodyB != null) {
         revi += getAverageRevoluteInertia (myBodyB, getTDB());
      }
      return revi/2;
   }

   public MatrixNdBlock getBilateralBlockA (int numb) {
      if (myBilateralBlkA == null || myBilateralBlkA.colSize() != numb) {
         myBilateralBlkA = new MatrixNdBlock (myBodyA.getVelStateSize(), numb);
      }
      return myBilateralBlkA;
   }

   public MatrixNdBlock getBilateralBlockB (int numb) {
      if (myBilateralBlkB == null || myBilateralBlkB.colSize() != numb) {
         myBilateralBlkB = new MatrixNdBlock (myBodyB.getVelStateSize(), numb);
      }
      return myBilateralBlkB;
   }

   public MatrixNdBlock getUnilateralBlockA (int numb) {
      if (myUnilateralBlkA == null || myUnilateralBlkA.colSize() != numb) {
         myUnilateralBlkA = new MatrixNdBlock (myBodyA.getVelStateSize(), numb);
      }
      return myUnilateralBlkA;
   }

   public MatrixNdBlock getUnilateralBlockB (int numb) {
      if (myUnilateralBlkB == null || myUnilateralBlkB.colSize() != numb) {
         myUnilateralBlkB = new MatrixNdBlock (myBodyB.getVelStateSize(), numb);
      }
      return myUnilateralBlkB;
   }

   public RigidTransform3d getTCA() {
      //return myCoupling.getTCA();
      return myTCA;
   }

   public void getCurrentTCA (RigidTransform3d TCA) {
      if (myBodyA instanceof DeformableBody) {
         ((DeformableBody)myBodyA).computeDeformedFrame (TCA, myTCA);
      }
      else {
         TCA.set (myTCA);
      }
   }

   public void setTCA (RigidTransform3d TCA) {
      myTCA.set (TCA);
      //myCoupling.setTCA (TCA);
   }

   public RigidTransform3d getTDB() {
      // return myCoupling.getTDB();
      return myTDB;
   }

   public void getCurrentTDB (RigidTransform3d TDB) {
      if (myBodyB instanceof DeformableBody) {
         ((DeformableBody)myBodyB).computeDeformedFrame (TDB, myTDB);
      }
      else {
         TDB.set (myTDB);
      }
   }

   public void setTDB (RigidTransform3d TDB) {
      myTDB.set (TDB);
      //myCoupling.setTDB (TDB);
   }

   // protected abstract RigidBodyCouplingX getCoupling();

   public int numBilateralConstraints() {
      return myCoupling.numBilaterals();
   }
   
   public int numUnilateralConstraints() {
      return myCoupling.numUnilaterals();
   }

   // added to implement Constrainer

   public void getBilateralSizes (VectorNi sizes) {
      int numc = myCoupling.numBilaterals();
      if (numc > 0) {
         sizes.append (numc);
      }
   }

   public void getUnilateralSizes (VectorNi sizes) {
      int numc = myCoupling.numUnilaterals();
      if (numc > 0) {
         sizes.append (numc);
      }
   }

   public double updateConstraints (double t, int flags) {
      boolean setEngaged = (flags & MechSystem.UPDATE_CONTACTS) == 0;

      double maxpen = 0;

      updateBodyStates (t, setEngaged);
      if (numBilateralConstraints() > 0) {
         if (myBilaterals == null) {
            myBilaterals = new ArrayList<RigidBodyConstraint>();
         }
         myBilaterals.clear();
         getBilateralConstraints (myBilaterals);
      }
      if (hasUnilateralConstraints()) {
         if (myUnilaterals == null) {
            myUnilaterals = new ArrayList<RigidBodyConstraint>();
         }
         if (setEngaged) {
            myUnilaterals.clear();
            double dist = getUnilateralConstraints (myUnilaterals, setEngaged);
            if (dist > maxpen) {
               maxpen = dist;
            }            
         }
         else {
            updateUnilateralConstraints (
               myUnilaterals, 0, myUnilaterals.size());
         }
      }
      return maxpen;
   }

   static void setBlockCol (
      MatrixNdBlock blk, int j, Wrench wr,
      RigidBody body, RigidTransform3d TCtoBod,
      MatrixNd PiBodToC, boolean negate) {
      
      setBlockCol (blk, j, wr, body, TCtoBod, PiBodToC, negate, /*world=*/false);

   }

   static void setBlockCol (
      MatrixNdBlock blk, int j, Wrench wr,
      RigidBody body, RigidTransform3d TCtoBod,
      MatrixNd PiBodToC, boolean negate, boolean inWorld) {

      Wrench wtmp = new Wrench();
      wtmp.transform (TCtoBod, wr);
      if (Frame.dynamicVelInWorldCoords && !inWorld) {
         // transform wrench into world coords
         wtmp.transform (body.getPose().R, wtmp);
      }
      if (negate) {
         wtmp.negate();
      }
      double[] buf = blk.getBuffer();
      int w = blk.getBufferWidth();
      int off = blk.getBufferBase() + j;
      buf[off    ] = wtmp.f.x;
      buf[w*1+off] = wtmp.f.y;
      buf[w*2+off] = wtmp.f.z;
      buf[w*3+off] = wtmp.m.x;
      buf[w*4+off] = wtmp.m.y;
      buf[w*5+off] = wtmp.m.z;

      if (body instanceof DeformableBody && PiBodToC != null) {
         int numc = ((DeformableBody)body).numElasticCoords();
         double[] pbuf = PiBodToC.getBuffer();
         wtmp.set (wr);
         if (negate) {
            wtmp.negate();
         }
         // we want to multiply wtmp by the transpose of PiBodToC
         int k = 6;
         for (int i=0; i<numc; i++) {
           buf[w*k+off] = (pbuf[       i]*wtmp.f.x + 
                           pbuf[numc*1+i]*wtmp.f.y + 
                           pbuf[numc*2+i]*wtmp.f.z + 
                           pbuf[numc*3+i]*wtmp.m.x + 
                           pbuf[numc*4+i]*wtmp.m.y + 
                           pbuf[numc*5+i]*wtmp.m.z);
           k++;
         }
      }
   }

   static int getSolveIdx (RigidBody body) {
      if (body != null && body.getSolveIndex() != -1) {
         return body.getSolveIndex();
      }
      else {
         return -1;
      }
   }

   public int addBilateralConstraints (
      SparseBlockMatrix GT, VectorNd dg, int numb) {

      double[] dbuf = (dg != null ? dg.getBuffer() : null);

      int nc = numBilateralConstraints();
      if (nc > 0) {
         int bj = GT.numBlockCols();

         int idxA = getSolveIdx (getBodyA());
         MatrixNdBlock blkA = null;
         if (idxA != -1) {
            blkA = getBilateralBlockA (nc);
            GT.addBlock (idxA, bj, blkA);
         }
         int idxB = getSolveIdx (getBodyB());
         MatrixNdBlock blkB = null;
         if (idxB != -1) {
            blkB = getBilateralBlockB (nc);
            GT.addBlock (idxB, bj, blkB);
         }
         for (int j=0; j<nc; j++) {
            RigidBodyConstraint b = myBilaterals.get (j);
            if (blkA != null) {
               setBlockCol (
                  blkA, j, b.getWrenchC(), getBodyA(), myTGA, myPiAC, false);
            }
            if (blkB != null) {
               setBlockCol (
                  blkB, j, b.getWrenchC(), getBodyB(), myTGB, myPiBC, true);
            }
            if (dbuf != null) {
               dbuf[numb+j] = b.getDerivative();
            }
         }
      }
      return numb + nc;
   }

   public int getBilateralInfo (ConstraintInfo[] ginfo, int idx) {

      int nc = numBilateralConstraints();
      if (nc > 0) {
         for (int j=0; j<nc; j++) {
            RigidBodyConstraint bc = myBilaterals.get (j);
            ConstraintInfo gi = ginfo[idx++];
            gi.dist = bc.getDistance();
            gi.compliance = bc.getCompliance();
            gi.damping = bc.getDamping();
         }
      }
      return idx;
   }

   static void setContactSpeed (
      RigidBodyConstraint c, RigidBody bodyA, RigidBody bodyB,
      RigidTransform3d TGA, RigidTransform3d TGB) {

      double speed = 0;
      Twist bodyVel = new Twist();

      if (bodyA != null) {
         bodyA.getBodyVelocity (bodyVel);
         if (TGA != null) {
            bodyVel.inverseTransform (TGA);
            speed += c.getWrenchC().dot (bodyVel);
         }
         else {
            speed += c.getWrenchA().dot (bodyVel);
         }
      }
      if (bodyB != null) {
         bodyB.getBodyVelocity (bodyVel);
         if (TGB != null) {
            bodyVel.inverseTransform (TGB);
            speed -= c.getWrenchC().dot (bodyVel);
         }
         else {
            speed += c.getWrenchB().dot (bodyVel);
         }         
      }
      c.setContactSpeed (speed);
   }

   public int addUnilateralConstraints (
      SparseBlockMatrix NT, VectorNd dn, int numu) {

      int nc = (myUnilaterals != null ? myUnilaterals.size() : 0);

      if (nc > 0) {

         double[] dbuf = (dn != null ? dn.getBuffer() : null);
         int bj = NT.numBlockCols();

         RigidBody bodyA = getBodyA();
         RigidBody bodyB = getBodyB();

         int idxA = getSolveIdx (bodyA);
         MatrixNdBlock blkA = null;
         if (idxA != -1) {
            blkA = getUnilateralBlockA (nc);
            NT.addBlock (idxA, bj, blkA);
         }
         int idxB = getSolveIdx (bodyB);
         MatrixNdBlock blkB = null;
         if (idxB != -1) {
            blkB = getUnilateralBlockB (nc);
            NT.addBlock (idxB, bj, blkB);
         }

         for (int j=0; j<nc; j++) {
            RigidBodyConstraint u = myUnilaterals.get (j);
            if (blkA != null) {
               setBlockCol (
                  blkA, j, u.getWrenchC(), bodyA, myTGA, myPiAC, false);
            }
            if (blkB != null) {
               setBlockCol (
                  blkB, j, u.getWrenchC(), bodyB, myTGB, myPiBC, true);
            }
            if (dbuf != null) {
               dbuf[numu+j] = u.getDerivative();
            }
            if (!MechModel.addConstraintForces) {
               setContactSpeed (u, bodyA, bodyB, myTGA, myTGB);
            }
         }
      }
      return numu + nc;
   }

   public int getUnilateralInfo (ConstraintInfo[] ninfo, int idx) {

      int nc = (myUnilaterals != null ? myUnilaterals.size() : 0);

      if (nc > 0) {
         for (int j = 0; j < nc; j++) {
            RigidBodyConstraint uc = myUnilaterals.get (j);
            uc.setSolveIndex (idx);
            ConstraintInfo ni = ninfo[idx++];
            if (uc.getDistance() < -myPenetrationTol) {
               ni.dist = uc.getDistance() + myPenetrationTol;
            }
            else {
               ni.dist = 0;
            }
            ni.compliance = uc.getCompliance();
            ni.damping = uc.getDamping();
         }
      }
      return idx;
   }

   public int maxFrictionConstraintSets() {
      return (myUnilaterals != null ? myUnilaterals.size() : 0);
   }

   static void getRelativePoseAndVelocity (
      RigidTransform3d XBA, Twist velRel, RigidBody bodyA, RigidBody bodyB) {
      
      Twist bodyVel = new Twist();

      if (bodyA == null) {
         throw new InternalErrorException (
            "BodyA is assumed to be non-null here");
      }
      if (bodyB != null) {
         XBA.mulInverseLeft (bodyA.getPose(), bodyB.getPose());
         bodyB.getBodyVelocity (bodyVel);
         velRel.transform (XBA, bodyVel);
         velRel.negate();
      }
      else {
         XBA.invert (bodyA.getPose());
         velRel.setZero();
      }
      bodyA.getBodyVelocity (bodyVel);
      velRel.add (bodyVel);
   }

   private static void computeTangentDirection (
      Vector3d dir, Twist velRel, Vector3d nrm, Point3d pnt) {

      // compute relative velocity at contact point (wrt A)
      dir.crossAdd (velRel.w, pnt, velRel.v);
      // then remove normal component
      dir.scaledAdd (-nrm.dot (dir), nrm, dir);
            
      double dirNorm = dir.norm();
      if (dirNorm > 1e-8) { // XXX should use a better epsilon
         // align first friction direction along tangential velocity
         dir.scale (1/dirNorm);
      }
      else { // arbitrary first friction direction
         dir.perpendicular (nrm);
         dir.normalize();
      }
   }

   public int addFrictionConstraints (
      SparseBlockMatrix DT, FrictionInfo[] finfo, int numf) {

      return addFrictionConstraints (
         DT, finfo, numf, myUnilaterals, getBodyA(), getBodyB(), myTGA, false);
   }

   public static int addFrictionConstraints (
      SparseBlockMatrix DT, FrictionInfo[] finfo, int numf,
      ArrayList<RigidBodyConstraint> unilaterals,
      RigidBody bodyA, RigidBody bodyB, RigidTransform3d TGA, 
      boolean inWorld) {      

      int nu = (unilaterals != null ? unilaterals.size() : 0);

      if (nu > 0) {

         Vector3d dir = new Vector3d();
         RigidTransform3d XBA = new RigidTransform3d();
         Twist velRel = new Twist();
         Wrench wr = new Wrench();
         Wrench wrW = new Wrench();

         int bj = DT.numBlockCols();

         int idxA = getSolveIdx (bodyA);
         int idxB = getSolveIdx (bodyB);

         getRelativePoseAndVelocity (XBA, velRel, bodyA, bodyB);
         if (inWorld) {
            velRel.transform (bodyA.getPose().R);
            XBA.setIdentity();
            XBA.p.sub (bodyB.getPosition(), bodyA.getPosition());
         }

         Vector3d nrm = new Vector3d();
         for (int j=0; j<nu; j++) {
            FrictionInfo info = finfo[numf];
            RigidBodyConstraint u = unilaterals.get (j);

            Point3d pnt = u.getContactPoint();
            if (inWorld && pnt != null) {
               pnt = new Point3d(pnt);
            }
            double mu = u.getFriction();
            double lam = u.getMultiplier();
            // Hack here: TGA will be non-null for RigidBodyConnectors,
            // where we use wrenchC instead of wrenchA
            if (TGA != null) {
               nrm.set (u.getWrenchC().f);
               nrm.transform (TGA.R, nrm);
            }
            else {
               nrm.set (u.getWrenchA().f);
            }

            if (pnt == null || mu * lam < 1e-4) {
               // no friction, or friction too small to bother with
               continue;
            }
            info.mu = mu;
            info.contactIdx = u.getSolveIndex();
            info.flags = 0;

            Matrix6x2Block DA = null;
            if (idxA != -1) {
               DA = new Matrix6x2Block();
               DT.addBlock (idxA, bj, DA);
            }
            Matrix6x2Block DB = null;
            if (idxB != -1) {
               DB = new Matrix6x2Block();
               DT.addBlock (idxB, bj, DB);
            }

            if (inWorld) {
               pnt.sub (bodyA.getPosition());
            }
            computeTangentDirection (dir, velRel, nrm, pnt);

            wr.f.set (dir);
            wr.m.cross (pnt, wr.f);
            if (DA != null) {
               wrW.set (wr);
               if (!inWorld) {
                  wrW.transform (bodyA.getPose().R);
               }
               DA.setColumn (0, wrW.f, wrW.m);
            }
            wr.inverseTransform (XBA);
            wr.negate();
            if (DB != null) {
               wrW.set (wr);
               if (!inWorld) {
                  wrW.transform (bodyB.getPose().R);
               }
               DB.setColumn (0, wrW.f, wrW.m);     
            }
            wr.f.cross (nrm, dir);
            wr.m.cross (pnt, wr.f);
            if (DA != null) {
               wrW.set (wr);
               if (!inWorld) {
                  wrW.transform (bodyA.getPose().R);
               }
               DA.setColumn (1, wrW.f, wrW.m);
            }
            wr.inverseTransform (XBA);
            wr.negate();
            if (DB != null) {
               wrW.set (wr);
               if (!inWorld) {
                  wrW.transform (bodyB.getPose().R);
               }
               DB.setColumn (1, wrW.f, wrW.m);
            }
            numf++;
            bj++;
         }
      }
      return numf;
   }

   // end implement Constrainer

   public void setBodies (
      RigidBody bodyA, RigidTransform3d TCA, RigidBody bodyB,
      RigidTransform3d TDB) {
      myBodyA = bodyA;
      setTCA (TCA);
      myBodyB = bodyB;
      setTDB (TDB);
   }

   public void setBodies (
      RigidBody bodyA, RigidBody bodyB, RigidTransform3d TDW) {

      RigidTransform3d TCA = new RigidTransform3d();
      RigidTransform3d TDB = new RigidTransform3d();
      
      TCA.mulInverseLeft(bodyA.getPose(), TDW);
      if (bodyB != null) {
         TDB.mulInverseLeft(bodyB.getPose(), TDW);
      }
      else {
         TDB.set (TDW);
      }
      setBodies(bodyA, TCA, bodyB, TDB);
   }

   public RigidTransform3d getCurrentTDW() {
      RigidTransform3d TDW = new RigidTransform3d();
      getCurrentTDB (TDW);
      if (myBodyB != null) {
         TDW.mul (myBodyB.getPose(), TDW);
      }
      return TDW;
   }

   public void getPose (RigidTransform3d X) {
      X.set (getCurrentTDW());      
   }

   public RigidTransform3d getCurrentTCW() {
      RigidTransform3d TFW = new RigidTransform3d();
      getCurrentTCA (TFW);
      TFW.mul (myBodyA.getPose(), TFW);
      return TFW;
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAndStoreReference (rtok, "bodyA", tokens)) {
         return true;
      }
      else if (scanAndStoreReference (rtok, "bodyB", tokens)) {
         return true;
      }
      else if (scanAttributeName (rtok, "TCA") ||
               scanAttributeName (rtok, "TCA")) { // TCA for backward compatible
         RigidTransform3d TCA = new RigidTransform3d();
         TCA.scan (rtok);
         setTCA (TCA);
         return true;
      }
      else if (scanAttributeName (rtok, "TDB") ||
               scanAttributeName (rtok, "XDB")) { // XBD for backward compatible
         RigidTransform3d TDB = new RigidTransform3d();
         TDB.scan (rtok);
         setTDB (TDB);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "bodyA")) {
         myBodyA = postscanReference (
            tokens, RigidBody.class, ancestor);
         return true;
      }
      else if (postscanAttributeName (tokens, "bodyB")) {
         myBodyB = postscanReference (
            tokens, RigidBody.class, ancestor);
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }
   
   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      RigidTransform3d TCA = getTCA();
      RigidTransform3d TDB = getTDB();
      pw.println ("bodyA="+ComponentUtils.getWritePathName (ancestor, myBodyA));
      pw.println ("bodyB="+ComponentUtils.getWritePathName (ancestor, myBodyB));
      //printBodyReferences (pw, ancestor);
      int writeFormat = RigidTransform3d.AXIS_ANGLE_STRING;
      if (fmt.isFullPrecisionDouble()) {
         // need to do MATRIX_3X4_STRING since that's the only thing
         // that gives us full precision save/restore
         writeFormat = RigidTransform3d.MATRIX_3X4_STRING;
      }
      pw.println ("TCA=" + TCA.toString (fmt, writeFormat));
      pw.println ("TDB=" + TDB.toString (fmt, writeFormat));
      super.writeItems (pw, fmt, ancestor);
      //getAllPropertyInfo().writeNonDefaultProps (this, pw, fmt);
   }

   /**
    * Returns the activation level associated with a specific constraint.
    * 
    * @param idx
    * index of the constraint
    * @return activation level of the constraint
    */
   public double getActivation (int idx) {
      return myCoupling.getConstraint (idx).getMultiplier();
   }

   public void updateBodyStates (double t, boolean setEngaged) {
      RigidTransform3d XAW = myBodyA.getPose();
      // System.out.println ("XAW=\n" + XAW.toString("%8.3f"));
      RigidTransform3d XBW =
         myBodyB != null ? myBodyB.getPose() : RigidTransform3d.IDENTITY;

      RigidTransform3d TGD = new RigidTransform3d();
      RigidTransform3d TCD = new RigidTransform3d();
      RigidTransform3d XBA = new RigidTransform3d();
      RigidTransform3d TCG = new RigidTransform3d();

      RigidTransform3d XERR = new RigidTransform3d();
      
      DeformableBody defBodyA = null;
      DeformableBody defBodyB = null;

      XBA.mulInverseLeft (XAW, XBW);
      
      RigidTransform3d TCA = myTCA;
      if (myBodyA instanceof DeformableBody) {
         defBodyA = (DeformableBody)myBodyA;
         if (myPolarCA == null) {
            myPolarCA = new PolarDecomposition3d();
         }
         if (myPiAC == null) {
            myPiAC = new MatrixNd();
         }
         TCA = new RigidTransform3d();
         getCurrentTCA (TCA);
      }
      RigidTransform3d TDB = myTDB;
      if (myBodyB instanceof DeformableBody) {
         defBodyB = (DeformableBody)myBodyB;
         if (myPolarCB == null) {
            myPolarCB = new PolarDecomposition3d();
         }
         if (myPiBC == null) {
            myPiBC = new MatrixNd();
         }
         TDB = new RigidTransform3d();
         getCurrentTDB (TDB);
         
         RigidTransform3d TCB = new RigidTransform3d();
         RigidTransform3d TCB0 = new RigidTransform3d();
         TCB.mulInverseLeft (XBA, TCA);

         // use myPolarCB as a temp for polarTF
         defBodyB.computeUndeformedFrame (TCB0, myPolarCB, TCB);
         RigidTransform3d TCD0 = new RigidTransform3d();
         RigidTransform3d TGD0 = new RigidTransform3d();
         TCD0.mulInverseLeft (myTDB, TCB0);
         myCoupling.projectToConstraint (TGD0, TCD0);

         RigidTransform3d TGB0 = new RigidTransform3d();
         TGB0.mul (myTDB, TGD0);
         defBodyB.computeDeformedFrame (myTGB, myPolarCB, TGB0);   

         // compute XERR using C and F
         XERR.mulInverseBoth (myTGB, XBA);
         XERR.mul (TCA);

         // For update constraints, use TGD0 and TCD0 in place of TGD and TCD

         //System.out.println ("bodyB deformable");

         TGD.set (TGD0);
         TCD.set (TCD0);
      }
      else {
         TCD.mulInverseBoth (TDB, XBA);
         TCD.mul (TCA);
         myCoupling.projectToConstraint (TGD, TCD);
         myTGB.mul (myTDB, TGD);
         //System.out.println ("bodyB fixed");

         XERR.mulInverseLeft (TGD, TCD);
      }

      myTGA.mul (XBA, myTGB);
      if (defBodyA != null) {
         RigidTransform3d TGA0 = new RigidTransform3d();
         defBodyA.computeUndeformedFrame (TGA0, myPolarCA, myTGA);
      }

      Twist err = new Twist();
      err.set (XERR);

      Twist velA = new Twist();
      myBodyA.getBodyVelocity (velA);
      velA.inverseTransform (myTGA);
      Twist velB = null;
      if (myBodyB != null) {
         velB = new Twist();
         myBodyB.getBodyVelocity (velB);
         velB.inverseTransform (myTGB);
      }
      // System.out.println ("bodyVelA=" + myBodyA.getVelocity().toString("%13.9f"));
      // if (defBodyA != null) {
      //    System.out.println (
      //       "elasVelA=" + defBodyA.getElasticVel().toString("%13.9f"));
      // }

      //System.out.println ("myTGA=\n" + myTGA.toString ("%13.9f"));

      if (defBodyA != null) {
         Twist velx = new Twist(); 
         defBodyA.computeDeformedFrameVel (velx, myPolarCA, myTGA);
         defBodyA.computeElasticJacobian (myPiAC, myPolarCA, myTGA);
         //System.out.println ("PiAC=\n" + myPiAC.toString ("%13.8f"));
         //System.out.println ("vel*=" + velx.toString("%13.9f"));
         velA.add (velx);         
      }
      if (defBodyB != null) {
         Twist velx = new Twist(); 
         defBodyB.computeDeformedFrameVel (velx, myPolarCB, myTGB);
         defBodyB.computeElasticJacobian (myPiBC, myPolarCB, myTGB);
         velB.add (velx);
      }
      //System.out.println ("velC=" + velA.toString("%13.9f"));
      myCoupling.updateBodyStates (TCD, TGD, XERR, velA, velB, setEngaged);
   }

   /**
    * Returns the most recently updated value for TGA. This update is done
    * whenever <code>updateBodyStates()</code> is called. The returned value
    * must be treated as read-only and not modified.
    *
    * @return current value for TGA.
    */
   public RigidTransform3d getTGA() {
      return myTGA;
   }

   /**
    * Returns the most recently updated value for TGB. This update is done
    * whenever <code>updateBodyStates()</code> is called. The returned value
    * must be treated as read-only and not modified.
    *
    * @return current value for TGB.
    */
   public RigidTransform3d getTGB() {
      return myTGB;
   }

   /**
    * Nothing to do for scale mass.
    */
   public void scaleMass (double m) {
   }

   public void scaleDistance (double s) {
      myTCA.p.scale (s);
      myTDB.p.scale (s);
      myCoupling.scaleDistance (s);
   }

   public int getBilateralConstraints (
      ArrayList<RigidBodyConstraint> bilaterals) {

      return myCoupling.getBilateralConstraints (bilaterals);
   }

   public int setBilateralImpulses (VectorNd lam, double h, int idx) {
      return myCoupling.setBilateralImpulses (lam, h, idx);
   }

   public int getBilateralImpulses (VectorNd lam, int idx) {
      return myCoupling.getBilateralImpulses (lam, idx);
   }

   public void zeroImpulses() {
      myCoupling.zeroImpulses();
   }
   
   public boolean hasUnilateralConstraints() {
      return myCoupling.maxUnilaterals() > 0;
   }

   public void setContactDistance (double d) {
      myCoupling.setContactDistance (d);
   }

   public double getContactDistance() {
      return myCoupling.getContactDistance();
   }

   public void setBreakSpeed (double s) {
      myCoupling.setBreakSpeed (s);
   }

   public double getBreakSpeed() {
      return myCoupling.getBreakSpeed();
   }

   public void setBreakAccel (double a) {
      myCoupling.setBreakAccel (a);
   }

   public double getBreakAccel() {
      return myCoupling.getBreakAccel();
   }

   public double getUnilateralConstraints (
      ArrayList<RigidBodyConstraint> unilaterals, boolean setEngaged) {
      return myCoupling.getUnilateralConstraints (unilaterals, setEngaged);
   }

   public void updateUnilateralConstraints (
      ArrayList<RigidBodyConstraint> unilaterals, int offset, int numc) {
      myCoupling.updateUnilateralConstraints (unilaterals, offset, numc);
   }

   public int setUnilateralImpulses (VectorNd the, double h, int idx) {
      return myCoupling.setUnilateralImpulses (the, h, idx);
   }

   public int getUnilateralImpulses (VectorNd the, int idx) {
      return myCoupling.getUnilateralImpulses (the, idx);
   }

   /**
    * Returns true if this RigidBodyConnectorX is enabled and at least one of
    * it's associated RigidBodies is active.
    */
   public boolean isActive() {
      if (!isEnabled()) {
         return false;
      }
      if (myBodyA.isActive()) {
         return true;
      }
      if (myBodyB != null && myBodyB.isActive()) {
         return true;
      }
      else {
         return false;
      }

   }

   public void transformGeometry (AffineTransform3dBase X) {
      transformGeometry (X, this, 0);
   }

   /**
    * Adjusts the global location of this constraint, assuming the global
    * locations of A and B remain unchanged.
    * 
    * <p>
    * If TDW represents frame D in world coordinates, then TDW is adjusted
    * according to
    * 
    * <pre>
    *    TDW' = X TDW
    * </pre>
    * 
    * This transformation is done using
    * {@link maspack.matrix.RigidTransform3d#mulAffineLeft mulAffineLeft}, which
    * removes the stretching and shearing components from X when adjusting the
    * rotation matrix. The transforms TDB is then updated accordingly. A
    * similar procedure is used for TCA.
    * 
    * @param X
    * an affine transform applied to frames D and F in world coordinates
    * @param topObject
    * top-most object being transformed
    * @param flags
    */
   public void transformGeometry (
      AffineTransform3dBase X, TransformableGeometry topObject, int flags) {
      // Note: in normal operation, myBodyA is not null
      RigidTransform3d TAW = 
         myBodyA != null ? myBodyA.getPose() : RigidTransform3d.IDENTITY;
      RigidTransform3d TBW =
         myBodyB != null ? myBodyB.getPose() : RigidTransform3d.IDENTITY;
         
      RigidTransform3d TDW = new RigidTransform3d();
      RigidTransform3d TFW = new RigidTransform3d();
      RigidTransform3d TDBnew = new RigidTransform3d();
      RigidTransform3d TCAnew = new RigidTransform3d();
      
      RotationMatrix3d Ra = new RotationMatrix3d();
      SVDecomposition3d SVD = new SVDecomposition3d();
      SVD.leftPolarDecomposition ((Matrix3d)null, Ra, X.getMatrix());
      
      TDW.mul (TBW, myTDB);
      TFW.mul (TAW, myTCA);
      
      myCoupling.transformGeometry (X, Ra, TFW, TDW);
      
      //TDW.mulAffineLeft (X, null);
      TDW.p.mulAdd (X.getMatrix(), TDW.p, X.getOffset());
      TDW.R.mul (Ra, TDW.R);
      TDBnew.mulInverseLeft (TBW, TDW);
      setTDB (TDBnew);

      //TFW.mulAffineLeft (X, null);
      TFW.p.mulAdd (X.getMatrix(), TFW.p, X.getOffset());
      TFW.R.mul (Ra, TFW.R);      
      TCAnew.mulInverseLeft (TAW, TFW);
      setTCA (TCAnew);       

      //myCoupling.transformGeometry (X, TAW, TBW);
   }

   @Override
   public void getHardReferences (List<ModelComponent> refs) {
      super.getHardReferences (refs);
      refs.add (myBodyA);
      if (myBodyB != null) {
         refs.add (myBodyB);
      }
   }

   @Override
   public void connectToHierarchy () {
      super.connectToHierarchy ();
      // Note: in normal operation, bodyA is not null
      if (myBodyA != null) {
         myBodyA.addDependency (this);
      }
      if (myBodyB != null) {
         myBodyB.addDependency (this);
      }
   }

   @Override
   public void disconnectFromHierarchy() {
      super.disconnectFromHierarchy();
      // Note: in normal operation, bodyA is not null
      if (myBodyA != null) {
         myBodyA.removeDependency (this);
      }
      if (myBodyB != null) {
         myBodyB.removeDependency (this);
      }
   }

   public boolean isDuplicatable() {
      return true;
   }

   public boolean getCopyReferences (
      List<ModelComponent> refs, ModelComponent ancestor) {
      if (!ComponentUtils.addCopyReferences (refs, myBodyA, ancestor)) {
         return false;
      }
      if (myBodyB != null) {
         if (!ComponentUtils.addCopyReferences (refs, myBodyB, ancestor)) {
            return false;
         }
      }
      return true;
   }

   @Override
   public ModelComponent copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      RigidBodyConnector copy = (RigidBodyConnector)super.copy (flags, copyMap);

      copy.myBodyA = copyBody (copyMap, myBodyA, flags);
      if (myBodyB != null) {
         copy.myBodyB = copyBody (copyMap, myBodyB, flags);
      }
      copy.myEnabledP = myEnabledP;
      return copy;
   }

   private RigidBody copyBody (
      Map<ModelComponent,ModelComponent> copyMap, RigidBody body, int flags) {

      RigidBody copy = (RigidBody)ComponentUtils.maybeCopy (flags, copyMap, body);
      return copy;
   }

   public void updateBounds (Point3d pmin, Point3d pmax) {
      RigidTransform3d TDW = getCurrentTDW();
      TDW.p.updateBounds (pmin, pmax);
   }

   public void updateForBodyPositionChange (
      RigidBody body, RigidTransform3d XBodyToWorldNew) {
      RigidTransform3d TBW =
         (myBodyB != null ? myBodyB.getPose() : RigidTransform3d.IDENTITY);

      if (body == myBodyA) {
         RigidTransform3d TCA = new RigidTransform3d();
         TCA.mulInverseLeft (XBodyToWorldNew, myBodyA.getPose());
         TCA.mul (myTCA);
         setTCA (TCA);         
         //myCoupling.updateTCA (XBodyToWorldNew, myBodyA.getPose());
      }
      else if (body == myBodyB) {
         RigidTransform3d TDB = new RigidTransform3d();
         TDB.mulInverseLeft (XBodyToWorldNew, TBW);
         TDB.mul (myTDB);
         setTDB (TDB);
         //myCoupling.updateTDB (XBodyToWorldNew, TBW);
      }
      else {
         throw new InternalErrorException ("body " + body
         + " not known to this connector");
      }
   }

   protected void setPoses (RigidTransform3d TBA) {

      ArrayList<RigidBody> freeBodiesA = new ArrayList<RigidBody>();
      if (myBodyB != null) {
         myBodyB.setMarked (true);
      }
      boolean AIsFree = myBodyA.findFreeAttachedBodies (
         freeBodiesA, /*rejectSelected=*/false);
      if (myBodyB != null) {
         myBodyB.setMarked (false);
      }
      if (!AIsFree) {
         if (myBodyB != null) {
            ArrayList<RigidBody> freeBodiesB = new ArrayList<RigidBody>();
            myBodyA.setMarked (true);
            boolean BIsFree = myBodyB.findFreeAttachedBodies (
               freeBodiesB, /*rejectSelected=*/false);
            myBodyA.setMarked (false);
            if (BIsFree) {
               RigidTransform3d TBW = new RigidTransform3d();
               freeBodiesB.remove (myBodyA);
               ArrayList<RigidTransform3d> relPoses =
                  myBodyB.getRelativePoses (freeBodiesB);
               TBW.mul (myBodyA.getPose(), TBA);
               myBodyB.setPose (TBW);
               myBodyB.setRelativePoses (freeBodiesB, relPoses);
               return;
            }
         }
      }
      RigidTransform3d TAW = new RigidTransform3d();
      freeBodiesA.remove (myBodyB);
      ArrayList<RigidTransform3d> relPoses =
         myBodyA.getRelativePoses (freeBodiesA);
      TAW.mulInverseRight (myBodyB.getPose(), TBA);
      myBodyA.setPose (TAW);
      myBodyA.setRelativePoses (freeBodiesA, relPoses);      
   }

   public void advanceAuxState (double t0, double t1) {
   }

   /** 
    * {@inheritDoc}
    */
   public void skipAuxState (DataBuffer data) {
      myCoupling.skipAuxState (data);
   }

   public void getAuxState (DataBuffer data) {
      myCoupling.getAuxState (data);
   }

   public void setAuxState (DataBuffer data) {
      myCoupling.setAuxState (data);
      if (hasUnilateralConstraints()) {
         if (myUnilaterals == null) {
            myUnilaterals = new ArrayList<RigidBodyConstraint>();
         }
         else {
            myUnilaterals.clear();
         }
         getUnilateralConstraints (myUnilaterals, /*setEngaged=*/false);
      }
   }
   
   public void getInitialAuxState (
      DataBuffer newData, DataBuffer oldData) {
      myCoupling.getInitialAuxState (newData, oldData);
   }
   
   /**
    * {@inheritDoc}
    */
   @Override
      public boolean hasState() {
      return true;
   }
}
