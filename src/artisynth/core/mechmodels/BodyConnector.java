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
import maspack.geometry.GeometryTransformer;
import maspack.properties.*;
import maspack.util.*;
import maspack.spatialmotion.*;
import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.MechSystem.FrictionInfo;
import artisynth.core.mechmodels.MechSystem.ConstraintInfo;
import artisynth.core.util.*;

public abstract class BodyConnector extends RenderableComponentBase
   implements ScalableUnits, TransformableGeometry, BodyConstrainer,
              Constrainer, HasCoordinateFrame {
              
   protected ConnectableBody myBodyA;
   protected ConnectableBody myBodyB;
   protected FrameAttachment myAttachmentA;
   protected FrameAttachment myAttachmentB;
   protected FrameAttachment myAttachmentBG;
   
   private boolean myEnabledP = true;
   protected RigidBodyCoupling myCoupling;
   protected int myStateVersion;
   
   // use an old (and presumably inaccurate) method for computing constraint
   // derivatives, simply for compatibility
   public static boolean useOldDerivativeMethod = false;
   private boolean myAdjustBodyAExplicitP = false;  // automatically select body to adjust

   protected VectorNd myCompliance = null;
   protected VectorNd myDamping = null;

   // Indicates that the uniformity of linear or rotary compliance values is
   // unknown and need to be calculated. This will only happen after compliance
   // and damping values are read from a file and the attachments have not yet
   // been initialized, making uniformity determination impossible.
   protected boolean myComplianceUniformityUnknown = false;

   protected double myLinearCompliance = 0;
   protected double myRotaryCompliance = 0;

   ArrayList<RigidBodyConstraint> myBilaterals;
   ArrayList<RigidBodyConstraint> myUnilaterals;

   protected static final double DEFAULT_PENETRATION_TOL = 0.0001;
   protected double myPenetrationTol = DEFAULT_PENETRATION_TOL;
   protected PropertyMode myPenetrationTolMode = PropertyMode.Inherited;

   public static PropertyList myProps =
      new PropertyList (
         BodyConnector.class, RenderableComponentBase.class);

   RigidTransform3d myTCG = new RigidTransform3d();

   // transform from D (but with world-aligned orientation) to G. 
   RigidTransform3d myTDwG = new RigidTransform3d();
   // transform from C (but with world-aligned orientation) to G. 
   RigidTransform3d myTCwG = new RigidTransform3d();

   // when responding to a transformGeometry request, transform only TDW
   // (instead of both TDW and TCW)
   protected boolean myTransformDGeometryOnly = false;

   Twist myDotXv = new Twist();
   Twist myVelBA = new Twist();

   static {
      myProps.add (
         "linearCompliance",
         "compliance along linear directions", 0, "[0,inf] NW");
      myProps.add (
         "rotaryCompliance",
         "compliance along rotary directions", 0, "[0,inf] NW");
      myProps.add ("enabled isEnabled *", "constraint is enabled", true);
      myProps.addReadOnly (
         "bilateralForceInA", "bilateral constraint force as seen in body A");
      myProps.addReadOnly (
         "unilateralForceInA", "unilateral constraint force as seen in frame A");
      myProps.addInheritable (
         "penetrationTol:Inherited", "collision penetration tolerance",
         DEFAULT_PENETRATION_TOL);      
   }

   protected boolean myHasTranslation = false;

   public boolean hasTranslation() {
      return myHasTranslation;
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
         tol = MechModel.getDefaultPenetrationTol (
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
   
   private void computeForceInA (Wrench wr, Wrench forceG) {
      if (myAttachmentA instanceof FrameFrameAttachment) {
         FrameFrameAttachment ffa = (FrameFrameAttachment)myAttachmentA;
         VectorNd f = new VectorNd(6);
         ffa.getMasterForces (f, forceG);
         // wrench values will be the first 6 values of f
         wr.set (f.getBuffer());
         Frame master = ffa.getMaster ();
         if (master != null) {
            wr.inverseTransform (master.getPose().R);
         }
      }
      else {
         wr.setZero();
      }      
   }

   /**
    * If body A is a Frame, computes the wrench acting on body A
    * in response to the most recent bilateral constraint forces.
    * If body A is not a Frame, the computed wrench is set to zero.
    *
    * @param wr returns the bilateral constraint wrench acting on A,
    * in the coordinates of A
    */
   public void getBilateralForceInA (Wrench wr) {
      computeForceInA (wr, myCoupling.getBilateralForceG());
   }

   /**
    * For debugging only
    */
   public void printConstraintInfo() {
      myCoupling.printConstraintInfo();
   }

   /**
    * If body A is a Frame, computes the wrench acting on body A
    * in response to the most recent bilateral constraint forces.
    * If body A is not a Frame, the computed wrench is set to zero.
    *
    * @return the bilateral constraint wrench acting on A,
    * in the coordinates of A
    */
   public Wrench getBilateralForceInA() {
      Wrench wr = new Wrench();
      getBilateralForceInA (wr);
      return wr;
   }

   /**
    * If body A is a Frame, computes the wrench acting on body A
    * in response to the most recent unilateral constraint forces.
    * If body A is not a Frame, the computed wrench is set to zero.
    *
    * @param wr returns the unilateral constraint wrench acting on A,
    * in the coordinates of A
    */
   public void getUnilateralForceInA (Wrench wr) {
      computeForceInA (wr, myCoupling.getUnilateralForceG());
   }

   /**
    * If body A is a Frame, computes the wrench acting on body A
    * in response to the most recent unilateral constraint forces.
    * If body A is not a Frame, the computed wrench is set to zero.
    *
    * @return the unilateral constraint wrench acting on A,
    * in the coordinates of A
    */
   public Wrench getUnilateralForceInA() {
      Wrench wr = new Wrench();
      getUnilateralForceInA (wr);
      return wr;
   }

   /**
    * Returns the uniform compliance value, if any, for the linear constraints
    * of this connector. A uniform compliance value is also associated with a
    * corresponding critical damping value. A value of {@code -1} indicate that
    * not uniform value exists.
    *  
    * @return linear compliance value, or {@code -1} if no
    * uniform value exists.
    */
   public double getLinearCompliance () {
      if (myComplianceUniformityUnknown) {
         if (attachmentsInitialized()) {
            checkComplianceUniformity();
         }
         else {
            return -1;
         }
      }
      return myLinearCompliance;
   }

   /**
    * Sets a uniform compliance for the linear constraints of this connector.
    * If the specified value {@code c} is {@code > 0}, then an appropriate
    * critical damping value is also set for each linear constraint. A value of
    * {@code c < 0} is ignored and leaves all compliance and damping values
    * unchanged.
    * 
    * @param c uniform linear compliance value
    */
   public void setLinearCompliance (double c) {
      if (c < 0) {
         c = -1;
      }
      myLinearCompliance = c;
      // compute damping to give critical damping
      if (c != -1) {
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
   }

   /**
    * Returns the uniform compliance value, if any, for the rotary constraints
    * of this connector. A uniform compliance value is also associated with a
    * corresponding critical damping value. A value of {@code -1} indicate that
    * not uniform value exists.
    *  
    * @return rotary compliance value, or {@code -1} if no
    * uniform value exists.
    */
   public double getRotaryCompliance () {
      if (myComplianceUniformityUnknown) {
         if (attachmentsInitialized()) {
            checkComplianceUniformity();
         }
         else {
            return -1;
         }
      }
      return myRotaryCompliance;
   }

   /**
    * Sets a uniform compliance for the rotary constraints of this connector.
    * If the specified value {@code c} is {@code > 0}, then an appropriate
    * critical damping value is also set for each rotary constraint. A value of
    * {@code c < 0} is ignored and leaves all compliance and damping values
    * unchanged.
    * 
    * @param c uniform rotary compliance value
    */
   public void setRotaryCompliance (double c) {
      if (c < 0) {
         c = -1;
      }
      myRotaryCompliance = c;
      // compute damping to give critical damping
      double d = (c > 0) ? 2*Math.sqrt(getAverageRevoluteInertia()/c) : 0;
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
    * Check whether a specific set of compliance and damping values are
    * consistent with uniform linear and/or rotary complaince values as would
    * be set by {@link #setLinearCompliance} and {@link setRotaryCompliance}.
    * If not, then the corresponding linear and/or rotary compliance values are
    * set to -1.
    */   
   private void checkComplianceUniformity () {

      VectorNd compliance = myCoupling.getCompliance();
      VectorNd damping = myCoupling.getDamping();

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
      myComplianceUniformityUnknown = false;
   }

   public void setCompliance (VectorNd compliance) {
      myCoupling.setCompliance(compliance);
      VectorNd damping = new VectorNd(myCoupling.getDamping());
      if (attachmentsInitialized()) {
         checkComplianceUniformity ();
      }
      else {
         // update later when attachments are initialized
         myComplianceUniformityUnknown = true;
      }
   }
   
   public VectorNd getDamping() {
      return myCoupling.getDamping();
   }
   
   public void setDamping (VectorNd damping) {
      myCoupling.setDamping(damping);
      VectorNd compliance = new VectorNd(myCoupling.getCompliance());
      if (attachmentsInitialized()) {
         checkComplianceUniformity ();
      }
      else {
         // update later when attachments are initialized
         myComplianceUniformityUnknown = true;
      }
   }
   
   public void setEnabled (boolean enabled) {
      if (enabled != myEnabledP) {
         myEnabledP = enabled;
      }
      myStateVersion++;
      notifyParentOfChange (StructureChangeEvent.defaultEvent);
   }

   public boolean isEnabled() {
      return myEnabledP;
   }

   protected BodyConnector() {
   }

   public int numBodies() {
      return myBodyB == null ? 1 : 2;
   }

   public ConnectableBody getBodyA() {
      return myBodyA;
   }

   public ConnectableBody getBodyB() {
      return myBodyB;
   }
   
   public ConnectableBody getOtherBody (ConnectableBody body) {
      if (body == myBodyA) {
         return myBodyB;
      }
      else if (body == myBodyB) {
         return myBodyA;
      }
      else {
         return null;
      }
   }

   /**
    * Queries whether or not this connector is connected to its bodies
    * via a component hierarchy.
    *
    * @return {@code true} if this connector is connected to its bodies
    */
   public boolean isConnectedToBodies() {
      if (myBodyA == null || !ComponentUtils.areConnected (this, myBodyA)) {
         return false;
      }
      if (myBodyB == null) {
         return true;
      }
      else {
         return ComponentUtils.areConnected (this, myBodyB);
      }
   }      

   protected double getAverageBodyMass() {
      double mass = 
         myAttachmentA.getAverageMasterMass() + 
         myAttachmentB.getAverageMasterMass();
      return mass/2;
   }

   protected double getAverageRevoluteInertia() {
      double inertia = 
         myAttachmentA.getAverageMasterInertia() + 
         myAttachmentB.getAverageMasterInertia();
      return inertia/2;
   }

   protected void getCurrentTCD (RigidTransform3d TCD) {
      RigidTransform3d TDW = new RigidTransform3d ();
      RigidTransform3d TCW = new RigidTransform3d ();
      getCurrentTCW (TCW);
      getCurrentTDW (TDW);
      TCD.mulInverseLeft (TDW, TCW);
   }

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

   private double[] adjustBodyPose (DynamicComponent body, double h) {
      
      double[] savePos = new double[body.getPosStateSize()];
      double[] pos = new double[body.getPosStateSize()];
      double[] vel = new double[body.getVelStateSize()];

      body.getPosState (savePos, 0);
      body.getPosState (pos, 0);
      body.getVelState (vel, 0);

      body.addPosImpulse (pos, 0, h, vel, 0);
      body.setPosState (pos, 0);
      return savePos;
   }

   private Twist computeDotXvNumerically (
      Matrix6dBlock dXAG, double[] velA, Matrix6dBlock dXBG, double[] velB) {

      double[] res = new double[6];
      if (dXBG != null) {
         dXBG.mulAdd (res, 0, velB, 0);
         for (int i=0; i<6; i++) {
            res[i] = -res[i];
         }
      }
      dXAG.mulAdd (res, 0, velA, 0);
      Twist dotXv = new Twist();
      dotXv.set (res);
      return dotXv;
   }

   private void computeDotXv (
      Twist dotXv, Twist velAinC, Twist velBinD, RigidTransform3d TCD,
      Twist dvelAinC, Twist dvelBinD) {

      if (useOldDerivativeMethod) {
         // incorrect results; provided for legacy use only
         Twist velBinC = new Twist(velBinD);
         velBinC.inverseTransform (TCD);
         dotXv.cross (velBinC, velAinC);
         return;
      }

      dotXv.set (dvelAinC);

      if (myAttachmentB.numMasters() > 0) {

         Twist dvelBinC = new Twist(dvelBinD);
         dvelBinC.inverseTransform (TCD);
         dotXv.sub (dvelBinC);

         Twist velBinC = new Twist(velBinD);
         velBinC.inverseTransform (TCD);

         Twist xprod = new Twist();
         xprod.cross (velAinC, velBinC);
         dotXv.add (xprod);
      }
   }
   
   protected double compare (Matrix M1, Matrix M2) {
      MatrixNd A1 = new MatrixNd(M1);
      MatrixNd A2 = new MatrixNd(M2);
      double norm = A1.frobeniusNorm();
      A1.sub (A2);
      return A1.frobeniusNorm()/norm;      
   }

   private void checkBilateralDerivatives () {

      int nc = myBilaterals.size();
      if (nc == 0) {
         return;
      }
      if (myAttachmentA.numMasters() == 1 && myAttachmentB.numMasters() <= 1) {
         DynamicComponent masterA = myAttachmentA.getMasters()[0];
         DynamicComponent masterB = null;
         if (myAttachmentB.numMasters() == 1) {
            masterB = myAttachmentB.getMasters()[0];
         }

         VectorNd dg = new VectorNd (nc);
         VectorNd chk = new VectorNd (nc);

         Twist dotXv = new Twist();

         Twist CvelA = new Twist();
         Twist dgA = new Twist();
         Twist DvelB = new Twist();
         Twist CvelB = new Twist();
         Twist dgB = new Twist();

         myAttachmentA.getCurrentVel (CvelA, dgA);
         myAttachmentB.getCurrentVel (DvelB, dgB);

         RigidTransform3d TCW = new RigidTransform3d();
         RigidTransform3d TDW = new RigidTransform3d();
         RigidTransform3d TCD = new RigidTransform3d();
         myAttachmentA.getCurrentTFW (TCW);
         myAttachmentB.getCurrentTFW (TDW);
         TCD.mulInverseLeft (TDW, TCW);

         computeDotXv (dotXv, CvelA, DvelB, TCD, dgA, dgB);
         CvelB.inverseTransform (TCD, DvelB);
         
         Twist CvelBA = new Twist();
         CvelBA.sub (CvelB, CvelA);

         double[] cbuf = chk.getBuffer();
         for (int j=0; j<nc; j++) {
            RigidBodyConstraint b = myBilaterals.get (j);
            dg.set (j, -b.getDotWrenchC().dot (CvelBA) + 
                        b.getWrenchC().dot (dotXv));
         }
         double h = 1e-8;

         double[] savePosA = null;
         double[] savePosB = null;

         MatrixNdBlock blkA0 = null;
         MatrixNdBlock blkB0 = null;
         MatrixNdBlock blkA1 = null;
         MatrixNdBlock blkB1 = null;

         double[] velA = null;
         double[] velB = null;

         blkA0 = getBilateralBlock (myAttachmentA);
         velA = new double[masterA.getVelStateSize()];
         masterA.getVelState (velA, 0);
         if (masterB != null) {
            blkB0 = getBilateralBlock (myAttachmentB);
            velB = new double[masterB.getVelStateSize()];
            masterB.getVelState (velB, 0);
         }

         savePosA = adjustBodyPose (masterA, h);
         if (masterB != null) {
            savePosB = adjustBodyPose (masterB, h);
         }

         updateBodyStates (0, false);
         myBilaterals.clear();
         getBilateralConstraints (myBilaterals);

         blkA1 = getBilateralBlock (myAttachmentA);
         blkA1.sub (blkA0);
         blkA1.scale (1/h);
         blkA1.mulTransposeAdd (cbuf, 0, velA, 0);
         if (myAttachmentB.numMasters() > 0) {
            blkB1 = getBilateralBlock (myAttachmentB);
            blkB1.sub (blkB0);
            blkB1.scale (1/h);
            blkB1.mulTransposeAdd (cbuf, 0, velB, 0);
         }

         masterA.setPosState (savePosA, 0);
         if (masterB != null) {
            masterB.setPosState (savePosB, 0);
         }
         updateBodyStates (0, false);
         myBilaterals.clear();
         getBilateralConstraints (myBilaterals);
         
         System.out.println ("dg= " + dg.toString("%12.8f"));
         System.out.println ("chk=" + chk.toString ("%12.8f"));

         System.out.println ("");
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
         if (setEngaged) {
            //checkBilateralDerivatives ();
            //checkBilateralDerivativesOld ();
         }
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
   
   public void getConstrainedComponents (List<DynamicComponent> list) {
      if (myAttachmentA != null) {
         for (DynamicComponent c : myAttachmentA.getMasters()) {
            list.add (c);
         }
      }
      if (myAttachmentB != null) {
         for (DynamicComponent c : myAttachmentB.getMasters()) {
            list.add (c);
         }
      }
   }
   
   public void addMasterBlocks (
      SparseBlockMatrix GT, int bj,
      MatrixNdBlock GC, FrameAttachment attachment) {

      DynamicComponent[] masters = attachment.getMasters();
      if (masters.length > 0) {
         MatrixBlock[] masterBlks = attachment.getMasterBlocks();
         for (int k=0; k<masters.length; k++) {
            int idx = masters[k].getSolveIndex();
            if (idx >= 0) {
               MatrixBlock mblk = masterBlks[k];
               MatrixBlock blk =
                  MatrixBlockBase.alloc (mblk.rowSize(), GC.colSize());
               blk.mulAdd (mblk, GC);
               GT.addBlock (idx, bj, blk);            
            }
         }
      }
   }

   private void setMatrixColumn (MatrixNd M, int j, Wrench wr) {
      double[] buf = M.getBuffer();
      int w = M.getBufferWidth();
      buf[0*w+j] = wr.f.x;
      buf[1*w+j] = wr.f.y;
      buf[2*w+j] = wr.f.z;
      buf[3*w+j] = wr.m.x;
      buf[4*w+j] = wr.m.y;
      buf[5*w+j] = wr.m.z;
   }

   protected MatrixNdBlock getConstraintMatrix (
      ArrayList<RigidBodyConstraint> constraints,
      RigidTransform3d TXwG, double scale) {

      int nc = constraints.size();
      
      Wrench wtmp = new Wrench();
      MatrixNdBlock GC = new MatrixNdBlock (6, nc);
      for (int j=0; j<nc; j++) {      
         RigidBodyConstraint c = constraints.get (j);
         wtmp.inverseTransform (TXwG, c.getWrenchC());
         if (scale != 1) {
            wtmp.scale (scale);
         }
         setMatrixColumn (GC, j, wtmp);
      }
      return GC;
   }

   protected MatrixNdBlock getFrictionConstraintMatrix (
      Wrench wr0, Wrench wr1, RigidTransform3d TXwG, double scale) {

      int nc = (wr1 == null ? 1 : 2);
      
      Wrench wtmp = new Wrench();
      MatrixNdBlock GC = new MatrixNdBlock (6, nc);
      wtmp.inverseTransform (TXwG, wr0);
      if (scale != 1) {
         wtmp.scale (scale);
      }
      setMatrixColumn (GC, 0, wtmp);      
      if (wr1 != null) {
         wtmp.inverseTransform (TXwG, wr1);
         if (scale != 1) {
            wtmp.scale (scale);
         }
         setMatrixColumn (GC, 1, wtmp);      
      }
      return GC;
   }

   protected MatrixNdBlock getBilateralBlock (FrameAttachment attachment) {
      int nc = myBilaterals.size();
      DynamicComponent[] masters = attachment.getMasters();
      MatrixNdBlock blk = null;
      if (nc > 0 && masters.length > 0) {
         // this method returns the block in C rather than G since
         // derivatives are currently computed in C
         RigidTransform3d TX = new RigidTransform3d();
         MatrixNdBlock G;
         if (attachment == myAttachmentA) {
            TX.mulInverseLeft (myTCG, myTCwG);
            G = getConstraintMatrix (myBilaterals, TX, 1);
         }
         else {
            TX.mulInverseLeft (myTCG, myTDwG);
            G = getConstraintMatrix (myBilaterals, TX, -1);
         }
         MatrixBlock mblk = attachment.getMasterBlocks()[0];
         blk = new MatrixNdBlock (mblk.rowSize(), nc);
         blk.mulAdd (mblk, G);
      }     
      return blk;
   }

   protected void setDerivativeTerm (
      VectorNd dg, ArrayList<RigidBodyConstraint> constraints,
      int nc, int idx) {

      double[] dbuf = dg.getBuffer();

      for (int j=0; j<nc; j++) {
         RigidBodyConstraint c = constraints.get (j);
         if (RigidBodyCoupling.useNewDerivatives) {
            dbuf[idx+j] = 
               -c.getDotWrenchC().dot (myVelBA) + c.getWrenchC().dot(myDotXv);
         }
         else {
            dbuf[idx+j] = c.getDerivative();                  
         }
      }
   }

   public int addBilateralConstraints (
      SparseBlockMatrix GT, VectorNd dg, int numb) {

      int nc = numBilateralConstraints();
      if (nc > 0) {
         MatrixNdBlock GC;
         int bj = GT.numBlockCols();
         GC = getConstraintMatrix (myBilaterals, myTCwG, 1);
         addMasterBlocks (GT, bj, GC, myAttachmentA);
         GC = getConstraintMatrix (myBilaterals, myTDwG, -1);
         addMasterBlocks (GT, bj, GC, myAttachmentB);
         if (dg != null) {
            setDerivativeTerm (dg, myBilaterals, nc, numb);
         }
      }
      return numb + nc;
   }

   public int addUnilateralConstraints (
      SparseBlockMatrix NT, VectorNd dn, int numu) {

      int nc = (myUnilaterals != null ? myUnilaterals.size() : 0);      

      if (nc > 0) {
         MatrixNdBlock GC;
         int bj = NT.numBlockCols();
         GC = getConstraintMatrix (myUnilaterals, myTCwG, 1);
         addMasterBlocks (NT, bj, GC, myAttachmentA);
         GC = getConstraintMatrix (myUnilaterals, myTDwG, -1);
         addMasterBlocks (NT, bj, GC, myAttachmentB);
         if (dn != null) {
            setDerivativeTerm (dn, myUnilaterals, nc, numu);
         }
      }         
      if (!MechModel.addConstraintForces) {
         for (int j=0; j<nc; j++) {
            RigidBodyConstraint u = myUnilaterals.get(j);
            u.setContactSpeed (-u.getWrenchC().dot(myVelBA));
         }
      }
      return numu + nc;
   }

   public int getBilateralInfo (ConstraintInfo[] ginfo, int idx) {

      int nc = numBilateralConstraints();
      if (nc > 0) {
         if (nc != myBilaterals.size()) {
            throw new InternalErrorException (
               "nc=" + nc + " bilaterals.size()=" + myBilaterals.size());
         }         
         for (int j=0; j<nc; j++) {
            RigidBodyConstraint bc = myBilaterals.get (j);
            ConstraintInfo gi = ginfo[idx++];
            gi.dist = bc.getDistance();
            gi.compliance = bc.getCompliance();
            gi.damping = bc.getDamping();
            gi.force = 0;
         }
      }
      return idx;
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
            ni.force = 0;
         }
      }
      return idx;
   }

   public int maxFrictionConstraintSets() {
      return (myUnilaterals != null ? myUnilaterals.size() : 0);
   }

   public int addFrictionConstraints (
      SparseBlockMatrix DT, FrictionInfo[] finfo, int numf) {

      numf = addFrictionConstraints (DT, finfo, numf, myUnilaterals);
      return numf;
   }

   private final double EPS = 1e-10;

   protected int accumulateFrictionDir (Vector3d dir, Vector3d vec, int dim) {

      switch (dim) {
         case -1: {
            // no constraints yet observed
            double mag = vec.norm();
            if (mag > EPS) {
               // first constraint oberved; set dir to its direction
               dir.scale (1/mag, vec);
               dim = 2;
            }
            break;
         }
         case 2: {
            // motion is constrained along direction dir
            Vector3d xvec = new Vector3d();
            xvec.cross (dir, vec);
            double mag = xvec.norm();
            if (mag > EPS) {
               // second constraint oberved; set dir to the remain free direction
               dir.scale (1/mag, xvec);
               dim = 1;
            }
            break;
         }
         case 1: {
            // motion is only free along direction dir
            if (Math.abs(dir.dot(vec)) > EPS) {
               // third constraint observed; dim = 0 means motion (and hence
               // friction) is not possible
               dim = 0;
            }
            break;
         }
         case 0: {
            break;
         }
         default: {
            throw new InternalErrorException ("Unknown friction dimension "+dim);
         }
      }
      return dim;
   }

   private void computeTangentDirection (
      Vector3d tdir, Twist velBA, Vector3d nrm) {

      tdir.set (velBA.v);
      // remove normal component of linear velocity
      tdir.scaledAdd (-nrm.dot (tdir), nrm, tdir);
            
      double dirNorm = tdir.norm();
      if (dirNorm > EPS) { // XXX should use a better epsilon
         // align first friction direction along tangential velocity
         tdir.scale (1/dirNorm);
      }
      else { // arbitrary first friction direction
         tdir.perpendicular (nrm);
         tdir.normalize();
      }
   }

   private static double ftol = 1e-2;

   protected int addFrictionConstraints (
      SparseBlockMatrix DT, FrictionInfo[] finfo, int numf,
      ArrayList<RigidBodyConstraint> constraints) {

      int nc = (constraints != null ? constraints.size() : 0);
      if (nc > 0) {
         int fdimv = -1;
         Vector3d vdir = new Vector3d();
         int[] idxs = new int[2];
         double mu = 0;  // friction coefficient
         double lam = 0; // current normal impulse value
         for (int j=0; j<nc; j++) {
            // check each constraint to determine direction(s) along which
            // frction should be allowed to act. Only examine constraints with
            // mu != 0
            RigidBodyConstraint c = constraints.get (j);
            Wrench g = c.getWrenchC();
            if (c.getFriction() > 0) {
               int dim = accumulateFrictionDir (vdir, g.f, fdimv);
               if (dim != fdimv) {
                  fdimv = dim;
                  if (dim == 2) {
                     idxs[0] = c.getSolveIndex();
                     mu = c.getFriction();
                     lam = c.getMultiplier();
                  }
                  else if (dim == 1) {
                     idxs[1] = c.getSolveIndex();
                     mu = (mu + c.getFriction())/2;
                     lam = Math.hypot (lam, c.getMultiplier());
                  }
               }
            }
         }
         // only add friction constraint if friction is large enough,
         // and if the friction dimension is 1 or 2
         if (mu*lam > ftol && (fdimv == 1 || fdimv == 2)) {
            Wrench wr0 = null;
            Wrench wr1 = null;

            if (fdimv == 2) {
               // friction can act in two directions perpendicular to vdir.
               // Compute these directions and set them in two wrenches wr0 and
               // wr1
               Vector3d tdir = new Vector3d();
               computeTangentDirection (tdir, myVelBA, vdir);
               wr0 = new Wrench();
               wr0.f.set (tdir);
               wr1 = new Wrench();
               wr1.f.cross (vdir, tdir);
               // friction calculation will use forces from 1 constraint
               finfo[numf].contactIdx0 = idxs[0];
               finfo[numf].contactIdx1 = -1;
            }
            else { // fdimv == 1
               // friction can act in only one directions parallel to vdir.
               // Set this directions in the wrenches wr0.
               wr0 = new Wrench();
               wr0.f.set (vdir);
               // friction calculation will use forces from 2 constraints
               finfo[numf].contactIdx0 = idxs[0];
               finfo[numf].contactIdx1 = idxs[1];
            }
            finfo[numf].mu = mu;
            finfo[numf].flags = 0;
            numf++;
            MatrixNdBlock GC;
            int bj = DT.numBlockCols();
            GC = getFrictionConstraintMatrix (wr0, wr1, myTCwG, 1);
            addMasterBlocks (DT, bj, GC, myAttachmentA);
            GC = getFrictionConstraintMatrix (wr0, wr1, myTDwG, -1);
            addMasterBlocks (DT, bj, GC, myAttachmentB); 
         }
      }
      return numf;
   }

   // end implement Constrainer

   protected FrameAttachment createAttachmentWithTSM (
      RigidBody body, RigidTransform3d TSM) {
      FrameFrameAttachment attachment = new FrameFrameAttachment();
      attachment.setWithTFM (body, TSM);
      attachment.updatePosStates();
      return attachment;
   }

   protected FrameAttachment createAttachment (
      ConnectableBody body, RigidTransform3d TSW) {
      if (body == null) {
         FrameFrameAttachment attachment = new FrameFrameAttachment();
         attachment.set (null, TSW);
         return attachment;
      }
      else {
         FrameAttachment attachment = body.createFrameAttachment (null, TSW);
         attachment.updatePosStates();
         return attachment;
      }
   }

   public void setBodies (
      RigidBody bodyA, RigidTransform3d TCA,
      RigidBody bodyB, RigidTransform3d TDB) {

      setBodies (bodyA, createAttachmentWithTSM (bodyA, TCA), 
                 bodyB, createAttachmentWithTSM (bodyB, TDB));
   }

   public void setBodies (
      ConnectableBody bodyA, FrameAttachment attachmentA, 
      ConnectableBody bodyB, FrameAttachment attachmentB) {

      disconnectBodies();
      myBodyA = bodyA;
      myAttachmentA = attachmentA;
      myBodyB = bodyB;
      myAttachmentB = attachmentB;
      connectBodies();
   }

   public void setBodies (
      ConnectableBody bodyA, ConnectableBody bodyB, RigidTransform3d TDW) {

      setBodies (bodyA, bodyB, TDW, TDW);
   }

   public void setBodies (
      ConnectableBody bodyA, ConnectableBody bodyB,
      RigidTransform3d TCW, RigidTransform3d TDW) {

      setBodies (bodyA, createAttachment (bodyA, TCW), 
                 bodyB, createAttachment (bodyB, TDW));
   }

   public RigidTransform3d getCurrentTDW() {
      RigidTransform3d TDW = new RigidTransform3d();
      getCurrentTDW (TDW);
      return TDW;
   }

   public void getCurrentTDW (RigidTransform3d TDW) {
      myAttachmentB.getCurrentTFW (TDW);
   }

   private void updateAttachment (FrameAttachment a, RigidTransform3d TFW) {
      DynamicComponent[] oldMasters = 
         Arrays.copyOf (a.getMasters(), a.numMasters());
      boolean mastersChanged = a.setCurrentTFW (TFW);
      if (mastersChanged) {
         // then masters have changed, so update constrainer back pointers
         disconnectAttachmentMasters (oldMasters);
         connectAttachmentMasters (a.getMasters());
      }
   }
   
   public void setCurrentTDW (RigidTransform3d TDW) {
      updateAttachment (myAttachmentB, TDW);
   }

   public void getPose (RigidTransform3d X) {
      X.set (getCurrentTDW());      
   }

   public RigidTransform3d getCurrentTCW() {
      RigidTransform3d TCW = new RigidTransform3d();
      getCurrentTCW (TCW);
      return TCW;
   }

   public void getCurrentTCW (RigidTransform3d TCW) {
      myAttachmentA.getCurrentTFW (TCW);
   }

   public void setCurrentTCW (RigidTransform3d TCW) {
      updateAttachment (myAttachmentA, TCW);
   }

   public RigidTransform3d getCurrentTXW (FrameAttachable body) {
      if (body == myBodyA) {
         return getCurrentTCW();
      }
      else if (body == myBodyB) {
         return getCurrentTDW();
      }
      else {
         return null;
      }
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
      else if (ScanWriteUtils.scanAndStoreComponent (
                  rtok, "attachmentA", tokens)) {
         return true;
      }
      else if (ScanWriteUtils.scanAndStoreComponent (
                  rtok, "attachmentB", tokens)) {
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "bodyA")) {
         myBodyA = postscanReference (
            tokens, ConnectableBody.class, ancestor);
         return true;
      }
      else if (postscanAttributeName (tokens, "bodyB")) {
         myBodyB = postscanReference (
            tokens, ConnectableBody.class, ancestor);
         return true;
      }
      else if (postscanAttributeName (tokens, "attachmentA")) {
         myAttachmentA =
            ScanWriteUtils.postscanComponent (
               tokens, FrameAttachment.class, ancestor);
         return true;
      }
      else if (postscanAttributeName (tokens, "attachmentB")) {
         myAttachmentB =
            ScanWriteUtils.postscanComponent (
               tokens, FrameAttachment.class, ancestor);
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }
   
   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      pw.println ("bodyA="+ComponentUtils.getWritePathName (ancestor, myBodyA));
      pw.println ("bodyB="+ComponentUtils.getWritePathName (ancestor, myBodyB));
      pw.print ("attachmentA=");
      ScanWriteUtils.writeComponent (pw, fmt, myAttachmentA, ancestor);
      pw.print ("attachmentB=");
      ScanWriteUtils.writeComponent (pw, fmt, myAttachmentB, ancestor);
      super.writeItems (pw, fmt, ancestor);
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

      RigidTransform3d TCD = new RigidTransform3d();
      RigidTransform3d TGD = new RigidTransform3d();
      RigidTransform3d TCW = new RigidTransform3d();
      RigidTransform3d TDW = new RigidTransform3d();
      RigidTransform3d XERR = new RigidTransform3d(); // same as TCG

      Twist CvelA = new Twist();
      Twist DvelB = new Twist();
      Twist velA = new Twist();
      Twist velB = new Twist();
      Twist dgA = new Twist();
      Twist dgB = new Twist();
      
      myAttachmentA.updatePosStates();
      myAttachmentB.updatePosStates();

      //RigidTransform3d TCA = myTCA;
      myAttachmentA.getCurrentTFW (TCW);
      myAttachmentB.getCurrentTFW (TDW);

      if (hasTranslation() && myBodyB.isDeformable()) {

         TCD.mulInverseLeft (TDW, TCW);
         if (myAttachmentBG == null) {
            myAttachmentBG = myBodyB.createFrameAttachment (null, TCW);
         }
         else {
            myAttachmentBG.setCurrentTFW (TCW);
         }
         RigidTransform3d TCW0 = new RigidTransform3d();
         RigidTransform3d TDW0 = new RigidTransform3d();
         RigidTransform3d TCD0 = new RigidTransform3d();
         RigidTransform3d TGD0 = new RigidTransform3d();
         myAttachmentB.getUndeformedTFW (TDW0);
         myAttachmentBG.getUndeformedTFW (TCW0);
         TCD0.mulInverseLeft (TDW0, TCW0);
         myCoupling.projectToConstraint (TGD0, TCD0);         

         XERR.mulInverseLeft (TGD0, TCD0);
         myTCG.set (XERR); // ????

         // TCwG maps master block velocities associated with C to G
         RigidTransform3d RWC = new RigidTransform3d();
         RWC.R.transpose (TCW.R);
         myTCwG.mul (XERR, RWC);

         myAttachmentA.getCurrentVel (CvelA, dgA);
         velA.transform (XERR, CvelA);

         myAttachmentBG.updatePosStates();
         myAttachmentBG.getCurrentVel (velB, dgB);
         DvelB.transform (TCD, velB);
         velB.transform (XERR);
         Twist err = new Twist();
         err.set (XERR);
         //System.out.println ("err=" + err.toString("%12.8f"));

         // TODO: make sure dgB is computed correctly!!
         dgB.transform (TCD);

         RigidTransform3d TGW = new RigidTransform3d();
         TGW.mulInverseRight (TCW, XERR);

         // TDwG maps master block velocities associated with D to G
         TGD.mulInverseRight (TCD, XERR);
         RigidTransform3d RWD = new RigidTransform3d();
         RWD.R.transpose (TDW.R);
         myTDwG.mulInverseLeft (TGD, RWD);

         computeDotXv (myDotXv, CvelA, DvelB, TCD, dgA, dgB);
         myVelBA.sub (velB, velA);
         myCoupling.updateBodyStates (TCD0, TGD0, XERR, setEngaged);
      }
      else {

         TCD.mulInverseLeft (TDW, TCW);
         myCoupling.projectToConstraint (TGD, TCD);
         XERR.mulInverseLeft (TGD, TCD);
         myTCG.set (XERR);

         // TCwG maps master block velocities associated with C to G
         RigidTransform3d RWC = new RigidTransform3d();
         RWC.R.transpose (TCW.R);
         myTCwG.mul (XERR, RWC);

         myAttachmentA.getCurrentVel (CvelA, dgA);
         velA.transform (XERR, CvelA);

         myAttachmentB.getCurrentVel (DvelB, dgB);
         velB.inverseTransform (TGD, DvelB);

         // TDwG maps master block velocities associated with D to G
         RigidTransform3d RWD = new RigidTransform3d();
         RWD.R.transpose (TDW.R);
         myTDwG.mulInverseLeft (TGD, RWD);

         computeDotXv (myDotXv, CvelA, DvelB, TCD, dgA, dgB);
         myVelBA.sub (velB, velA);
         myCoupling.updateBodyStates (TCD, TGD, XERR, setEngaged);
      }
      
   }

   /**
    * Nothing to do for scale mass.
    */
   public void scaleMass (double m) {
   }

   public void scaleDistance (double s) {
      myAttachmentA.scaleDistance (s);
      myAttachmentB.scaleDistance (s);
      myCoupling.scaleDistance (s);
   }

   public int getBilateralConstraints (
      ArrayList<RigidBodyConstraint> bilaterals) {

      return myCoupling.getBilateralConstraints (bilaterals);
   }

   public int setBilateralForces (VectorNd lam, double s, int idx) {
      idx = myCoupling.setBilateralForces (lam, s, idx);
      return idx;
   }

   public int getBilateralForces (VectorNd lam, int idx) {
      return myCoupling.getBilateralForces (lam, idx);
   }

   public void zeroForces() {
      myCoupling.zeroForces();
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

   public int setUnilateralForces (VectorNd the, double s, int idx) {
      idx = myCoupling.setUnilateralForces (the, s, idx);
      return idx;
   }

   public int getUnilateralForces (VectorNd the, int idx) {
      return myCoupling.getUnilateralForces (the, idx);
   }

   /**
    * Returns true if this RigidBodyConnectorX is enabled and at least one of
    * it's underlying master components is active.
    */
   public boolean isActive() {
      if (!isEnabled()) {
         return false;
      }
      if (myAttachmentA.oneMasterActive() ||
          myAttachmentB.oneMasterActive()) {
         return true;
      }
      else {
         return false;
      }
   }
   
   /**
    * Queries whether or not the attachments have been initialized.  
    * 
    * @return {@code true} if the attachments are initialized.  
    */
   protected boolean attachmentsInitialized() {
      return myAttachmentA != null && myAttachmentB != null;
   }

   public void transformGeometry (AffineTransform3dBase X) {
      TransformGeometryContext.transform (this, X, 0);
   }

   private class UpdateConnectorAction implements TransformGeometryAction {

      RigidTransform3d myTXW;
      FrameAttachment myAttachment;

      UpdateConnectorAction (RigidTransform3d TXW, FrameAttachment attachment) {
         myTXW = new RigidTransform3d (TXW);
         myAttachment = attachment;
      }

      public void transformGeometry (
         GeometryTransformer gtr, TransformGeometryContext context, int flags) {
         updateAttachment (myAttachment, myTXW);
      }
   }
   
   private class UpdatePosStateAction implements TransformGeometryAction {

      FrameAttachment myAttachment;

      UpdatePosStateAction (FrameAttachment attachment) {
         myAttachment = attachment;
      }

      public void transformGeometry (
         GeometryTransformer gtr, TransformGeometryContext context, int flags) {
         myAttachment.updatePosStates();
      }
   }
   
   

   private boolean allMastersTransforming (
      FrameAttachment a, TransformGeometryContext context) {
      if (a.numMasters() == 0) {
         return false;
      }
      else {
         return context.containsAll (a.getMasters());
      }
   }

   private boolean anyMastersTransforming (
      FrameAttachment a, TransformGeometryContext context) {
      return context.containsAny (a.getMasters());
   }

   private boolean frameMovingRelativeToMasters (
      FrameAttachment a, GeometryTransformer gtr, 
      TransformGeometryContext context, boolean transforming) {

      if (transforming) {
         return (!gtr.isRigid() || a.numMasters() > 1 ||                 
                 !allMastersTransforming (a, context));
      }
      else {
         return (anyMastersTransforming (a, context));
      }
   }
   
   public void transformGeometry (
      GeometryTransformer gtr, TransformGeometryContext context, int flags) {
      
      // See if this connector is actually being transformed. Even if it
      // is not, this method may still be called from the transformGeometry 
      // method of one of the attached master components, in order to
      // request any needed attachment updates. 
      boolean transforming = context.contains(this);

      // Get the current values of TCW and TDW. The transformGeometry methods
      // of the attached dynamic components take care to ensure that this
      // method is called *before* any of the attached master components are
      // transformed, so TCW and TDW can be set to their pre-transform values.
      RigidTransform3d TCW = new RigidTransform3d();
      myAttachmentA.getCurrentTFW (TCW);
      RigidTransform3d TDW = new RigidTransform3d();
      myAttachmentB.getCurrentTFW (TDW);

      if (transforming) {
         myCoupling.transformGeometry (gtr, TCW, TDW);
      }

      if ((flags & TG_ARTICULATED) != 0) {
         MechModel topMech = MechModel.topMechModel (this);
         if (topMech != null) {
            context.addAction (topMech.myRequestEnforceArticulationAction);
         }
         return;
      }
      
      // flags to indicate if attachments A and/or B should be updated
      boolean updateAttachmentA = false;
      boolean updateAttachmentB = false;

      if (transforming) {
         // transform TDW, and transform TCW unless it is not specified to
         // transform with this connector
         gtr.transform (TDW);
         if (!myTransformDGeometryOnly) {
            gtr.transform (TCW);
         }
      }

      if (frameMovingRelativeToMasters (
             myAttachmentB, gtr, context, transforming)) {
         // transform induces relative motion between frame D and its masters,
         // so attachment B needs to be updated
         updateAttachmentB = true;
      }
      if (!myTransformDGeometryOnly) {
         if (frameMovingRelativeToMasters (
               myAttachmentA, gtr, context, transforming)) {
            // transform induces relative motion between frame C and its masters,
            // so attachment A needs to be updated
            updateAttachmentA = true;
         }
      }
      else {
         // request attachmentA update if all of A masters are transforming
         // non-rigidly
         if (!gtr.isRigid() && allMastersTransforming (myAttachmentA, context)) {
            gtr.transform (TCW);
            updateAttachmentA = true;
         }
      }
      if (updateAttachmentA || updateAttachmentB) {
         boolean correctErrors = numBilateralConstraints() > 0;
         if (correctErrors) {
            // modify TCW or TDW to correct any constraint errors
            RigidTransform3d TCD = new RigidTransform3d();
            RigidTransform3d TGD = new RigidTransform3d();
            TCD.mulInverseLeft (TDW, TCW);
            myCoupling.projectToConstraint (TGD, TCD);
            if (updateAttachmentA) {
               // use C to correct the error
               TCW.mul (TDW, TGD);
            }
            else {
               // use D to correct the error
               TDW.mulInverseRight (TCW, TGD);
            }
         }
         if (updateAttachmentA) {
            context.addAction (new UpdateConnectorAction (TCW, myAttachmentA));
         }
         if (updateAttachmentB) {
            context.addAction (new UpdateConnectorAction (TDW, myAttachmentB));
         }
      }
      // for attachments that don't need updating, still issue a request
      // to update their pos states after the transforming is done (in
      // case we need to update things like world locations).
      if (!updateAttachmentA) {
         context.addAction (new UpdatePosStateAction (myAttachmentA));
      }
      if (!updateAttachmentB) {
         context.addAction (new UpdatePosStateAction (myAttachmentB));
      }
   }
   
   public void addTransformableDependencies (
      TransformGeometryContext context, int flags) {
      // no dependencies
   }

   @Override
   public void getHardReferences (List<ModelComponent> refs) {
      super.getHardReferences (refs);
      myAttachmentA.getHardReferences (refs);
      myAttachmentB.getHardReferences (refs);
   }

   private void connectAttachmentMasters (DynamicComponent[] masters) {
      if (masters != null) {
         for (DynamicComponent m : masters) {
            m.addConstrainer (this);
         }
      }
   }

   private void disconnectAttachmentMasters (DynamicComponent[] masters) {
      if (masters != null) {
         for (DynamicComponent m : masters) {
            m.removeConstrainer (this);
         }
      }
   }

   protected void connectNewlyConnectedBodies (
      CompositeComponent connector) {
      
      if (myBodyA != null && 
          ComponentUtils.areConnectedVia (this, myBodyA, connector)) {
         myBodyA.addConnector (this);
         connectAttachmentMasters (myAttachmentA.getMasters());        
      }
      if (myBodyB != null && 
          ComponentUtils.areConnectedVia (this, myBodyB, connector)) {
         myBodyB.addConnector (this);
         connectAttachmentMasters (myAttachmentB.getMasters());        
      }
   }
   
   protected void disconnectNewlyDisconnectedBodies (
      CompositeComponent connector) {
      
      if (myBodyA != null && 
          ComponentUtils.areConnectedVia (this, myBodyA, connector)) {
         myBodyA.removeConnector (this);
         disconnectAttachmentMasters (myAttachmentA.getMasters());        
      }
      if (myBodyB != null && 
          ComponentUtils.areConnectedVia (this, myBodyB, connector)) {
         myBodyB.removeConnector (this);
         disconnectAttachmentMasters (myAttachmentB.getMasters());        
      }
   }
   
   private void connectBodies() {
      // Note: in normal operation, bodyA is not null
      if (myBodyA != null && ComponentUtils.areConnected (this, myBodyA)) {
         myBodyA.addConnector (this);
         connectAttachmentMasters (myAttachmentA.getMasters());
      }
      if (myBodyB != null && ComponentUtils.areConnected (this, myBodyB)) {
         myBodyB.addConnector (this);
         connectAttachmentMasters (myAttachmentB.getMasters());
      }
   }

   private void disconnectBodies() {
      // Note: in normal operation, bodyA is not null
      if (myBodyA != null && ComponentUtils.areConnected (this, myBodyA)) {
         myBodyA.removeConnector (this);
         disconnectAttachmentMasters (myAttachmentA.getMasters());
      }
      if (myBodyB != null && ComponentUtils.areConnected (this, myBodyB)) {
         myBodyB.removeConnector (this);
         disconnectAttachmentMasters (myAttachmentB.getMasters());
      }
   }

   @Override
   public void connectToHierarchy (CompositeComponent hcomp) {
      super.connectToHierarchy (hcomp);
      connectNewlyConnectedBodies (hcomp);
   }

   @Override
   public void disconnectFromHierarchy(CompositeComponent hcomp) {
      super.disconnectFromHierarchy(hcomp);
      disconnectNewlyDisconnectedBodies (hcomp);
   }

   public boolean isDuplicatable() {
      return true;
   }

   public boolean getCopyReferences (
      List<ModelComponent> refs, ModelComponent ancestor) {

      if (!myAttachmentA.getCopyReferences(refs, ancestor)) {
         return false;
      }
      if (!myAttachmentB.getCopyReferences(refs, ancestor)) {
         return false;
      }
      return true;
   }

   @Override
   public ModelComponent copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      BodyConnector copy = (BodyConnector)super.copy (flags, copyMap);

      // the following are all allocated on demand so set them to null to
      // ensure we don't share them with the original :-)
      copy.myCoupling = null;
      copy.myBilaterals = null;
      copy.myUnilaterals = null;
      copy.myAttachmentBG = null;

      copy.myAttachmentA = myAttachmentA.copy (flags, copyMap);
      copy.myAttachmentB = myAttachmentB.copy (flags, copyMap);

      if (myBodyA instanceof CopyableComponent) {
         copy.myBodyA =
            (ConnectableBody)ComponentUtils.maybeCopy (
               flags, copyMap, (CopyableComponent)myBodyA);
      }
      else {
         // shouldn't be here since copy shouldn't be called in this case
         copy.myBodyA = null;
      }
      if (myBodyB instanceof CopyableComponent) {
         copy.myBodyB = 
            (ConnectableBody)ComponentUtils.maybeCopy (
               flags, copyMap, (CopyableComponent)myBodyB);
      }
      else {
         // shouldn't be here since copy shouldn't be called in this case
         copy.myBodyB = null;
      }
      copy.myEnabledP = myEnabledP;

      if (myCompliance != null) {
         copy.myCompliance = new VectorNd (myCompliance);
      }
      if (myDamping != null) {
         copy.myDamping = new VectorNd (myDamping);
      }

      // the following working variables need to be duplicated, though probably
      // without the current values; just being cautious ...
      copy.myTCG = new RigidTransform3d (myTCG);
      copy.myTDwG = new RigidTransform3d (myTDwG);
      copy.myTCwG = new RigidTransform3d (myTCwG);
      copy.myDotXv = new Twist(myDotXv);
      copy.myVelBA = new Twist(myVelBA);
      
      return copy;
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      RigidTransform3d TDW = getCurrentTDW();
      TDW.p.updateBounds (pmin, pmax);
   }

   public void updateForBodyPositionChange (
      ConnectableBody body, RigidTransform3d TXW) {

      if (body == myBodyA) {
         myAttachmentA.setCurrentTFW (TXW);
      }
      else if (body == myBodyB) {
         myAttachmentB.setCurrentTFW (TXW);
      }
      else {
         // ignore for now
      }
   }

   private void transformPoseAndUpdateConnectorAttachments (
      ConnectableBody body,  RigidTransform3d T) {
      body.transformPose (T);
      for (BodyConnector c : body.getConnectors()) {
         if (c.myAttachmentA != null) {
            c.myAttachmentA.updatePosStates();
         }
         if (c.myAttachmentB != null) {
            c.myAttachmentB.updatePosStates();
         }
      }
   }
   
   private void adjustBodyPoses (
      ConnectableBody body, ArrayList<ConnectableBody> freeBodies,
      RigidTransform3d Tnew, RigidTransform3d Told) {

      RigidTransform3d T = new RigidTransform3d();
      T.mulInverseRight (Tnew, Told);     
      freeBodies.remove (body);
      for (ConnectableBody bod : freeBodies) {
         transformPoseAndUpdateConnectorAttachments (bod, T);
      }
      transformPoseAndUpdateConnectorAttachments (body, T);
   }
   
   /**
    * Sets the connector to explicitly adjust body A's pose only when
    * pose needs to be manually corrected to account for constraints.
    * 
    * By default, any adjustments to pose are performed on the body that is
    * "free" (i.e. dynamic and only attached to other free bodies).
    * If both bodyA and bodyB are free, then the one with fewer attached
    * bodies is moved. 
    * 
    * This behavior can be overridden by setting bodyA to be moved
    * explicitly.
    * 
    * @param set if true, only adjusts body A
    */
   public void setAlwaysAdjustBodyA(boolean set) {
      myAdjustBodyAExplicitP = set;
   }

   protected void adjustPoses (RigidTransform3d TGD) {

      RigidTransform3d TCW = new RigidTransform3d();
      RigidTransform3d TDW = new RigidTransform3d();
      myAttachmentA.getCurrentTFW (TCW);
      myAttachmentB.getCurrentTFW (TDW);
      
      ArrayList<ConnectableBody> bodiesA = new ArrayList<ConnectableBody>();
      ArrayList<ConnectableBody> bodiesB = new ArrayList<ConnectableBody>();

      boolean AIsFree = findAttachedBodies (myBodyA, myBodyB, bodiesA);
      boolean BIsFree = false;
      boolean moveBodyA = true;
      if (!myAdjustBodyAExplicitP && myBodyB != null) {
         BIsFree = findAttachedBodies (myBodyB, myBodyA, bodiesB);
         if (AIsFree != BIsFree) {
            moveBodyA = AIsFree;
         }
         else {
            // if either or neither are free, move the one with fewer attached bodies
            moveBodyA = bodiesA.size() <= bodiesB.size();
         }
      }
      
      if (moveBodyA) {
         RigidTransform3d TCWnew = new RigidTransform3d();
         TCWnew.mul (TDW, TGD);
         bodiesA.remove (myBodyB);
         adjustBodyPoses (myBodyA, bodiesA, TCWnew, TCW);
         myAttachmentA.updatePosStates();
      }
      else {
         RigidTransform3d TDWnew = new RigidTransform3d();
         TDWnew.mulInverseRight (TCW, TGD);
         bodiesB.remove (myBodyA);
         adjustBodyPoses (myBodyB, bodiesB, TDWnew, TDW);
         myAttachmentB.updatePosStates();
      }
   }

   public void advanceState (double t0, double t1) {
   }

   public void getState (DataBuffer data) {
      myCoupling.getState (data);
   }

   public void setState (DataBuffer data) {
      myAttachmentA.updatePosStates();
      myAttachmentB.updatePosStates();
      myCoupling.setState (data);
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

   @Override
   public int getStateVersion () {
      return myStateVersion;
   }
   
   /**
    * {@inheritDoc}
    */
   @Override
      public boolean hasState() {
      return true;
   }
   
   private static boolean recursivelyFindAttachedBodies (
      ConnectableBody body, List<ConnectableBody> list,
      HashSet<ConnectableBody> visited) {

      if (body == null) {
         return false;
      }
      boolean isFree = true;     
      if (!visited.contains(body)) {
         visited.add (body);
         list.add (body);
         if (!body.isFreeBody()) {
            isFree = false;
         }
         if (body.getConnectors() != null) {
            for (BodyConnector c : body.getConnectors()) {
               ConnectableBody otherBody = c.getOtherBody (body);
               if (!recursivelyFindAttachedBodies (otherBody, list, visited)) {
                  if (c.myTransformDGeometryOnly) {
                     // still OK, just want to curtail list
                  }
                  else {
                     isFree = false;
                  }
               }
            }
         }
      }
      return isFree;
   }

   public static boolean findAttachedBodies (
      ConnectableBody body, ConnectableBody exclude,
      List<ConnectableBody> bodies) {

      HashSet<ConnectableBody> visited = new HashSet<ConnectableBody>();
      if (exclude != null) {
         visited.add (exclude);
      }
      boolean allFree = recursivelyFindAttachedBodies (
         body, bodies, visited);
      return allFree;
   }   
  
   // private static boolean isTransforming (
   //    ConnectableBody body, TransformGeometryContext context) {
   //    if (context != null && body instanceof TransformableGeometry) {
   //       return context.contains ((TransformableGeometry)body);
   //    }
   //    else {
   //       return false;
   //    }
   // }      

   // private static boolean recursivelyFindFreeAttachedBodies (
   //    ConnectableBody body,
   //    List<ConnectableBody> freeBodies,
   //    List<BodyConnector> connectors, 
   //    HashSet<ConnectableBody> visited,
   //    TransformGeometryContext context) {      

   //    ArrayList<ConnectableBody> free = new ArrayList<ConnectableBody>();
   //    ArrayList<BodyConnector> cons = new ArrayList<BodyConnector>();

   //    if (body.getConnectors() != null) {
   //       for (BodyConnector c : body.getConnectors()) {
   //          ConnectableBody otherBody = c.getOtherBody (body);
            
   //          if (otherBody == null) {
   //             if (!c.myTransformDGeometryOnly) {
   //                return false;
   //             }
   //          }
   //          else if (!visited.contains(otherBody) &&
   //                   !isTransforming (otherBody, context)) {
   //             if (!otherBody.isFreeBody()) {
   //                return false;
   //             }
   //             visited.add (otherBody);
   //             free.clear();
   //             cons.clear();
   //             free.add (otherBody);
   //             cons.add (c);
   //             if (recursivelyFindFreeAttachedBodies (
   //                    otherBody, free, cons, visited, context)) {
   //                if (freeBodies != null) {
   //                   freeBodies.addAll (free);
   //                }
   //                if (connectors != null) {
   //                   connectors.addAll (cons);
   //                }
   //             }
   //             else {
   //                return false;
   //             }
   //          }
   //       }
   //    }
   //    return true;
   // }
  
   // public static boolean findFreeAttachedBodies (
   //    ConnectableBody body,
   //    List<ConnectableBody> freeBodies, 
   //    List<BodyConnector> connectors,
   //    TransformGeometryContext context) {

   //    HashSet<ConnectableBody> visited = new HashSet<ConnectableBody>();
   //    boolean allFree = recursivelyFindFreeAttachedBodies (
   //       body, freeBodies, connectors, visited, context);
   //    return allFree;
   // }  
   
   public FrameAttachment getFrameAttachmentA() {
      return myAttachmentA;
   }
   
   public FrameAttachment getFrameAttachmentB() {
      return myAttachmentB;
   }

}
