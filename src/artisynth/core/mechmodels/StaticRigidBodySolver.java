package artisynth.core.mechmodels;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;

import artisynth.core.mechmodels.BodyConnector;
import artisynth.core.mechmodels.BodyConstrainer;
import artisynth.core.mechmodels.Constrainer;
import artisynth.core.mechmodels.DynamicComponent;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.MechSystem;
import artisynth.core.mechmodels.MechSystem.ConstraintInfo;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.NumericState;
import maspack.matrix.*;
import maspack.matrix.Point3d;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.SparseBlockSignature;
import maspack.matrix.SparseNumberedBlockMatrix;
import maspack.matrix.VectorNd;
import maspack.matrix.VectorNi;
import maspack.solvers.KKTSolver;
import maspack.spatialmotion.SpatialInertia;

/**
 * Base for classes that perform static constraint-based solves on a set of
 * rigid bodies contained within a MechModel.
 */
public abstract class StaticRigidBodySolver {

   /**
    * Information about the rigid bodies associated with this solve
    */
   protected static class BodyInfo {
      public RigidBody myBody;  // rigid body
      public int myIndex;       // index within the bodyInfo list
      public boolean myDynamic; // true if body is dynamic at initialization
      public int mySolveIndex;  // body solve index wrt to the MechModel

      BodyInfo (RigidBody body) {
         myBody = body;
      }
   }

   protected class BodyInfoComparator implements Comparator<BodyInfo> {
      public int compare (BodyInfo info0, BodyInfo info1) {
         int idx0 = info0.myBody.getSolveIndex();
         int idx1 = info1.myBody.getSolveIndex();
         if (idx0 < idx1) {
            return -1;
         }
         else if (idx0 == idx1) {
            return 0;
         }
         else {
            return 1;
         }
      }
   }

   protected static final double DOUBLE_PREC = 1e-16;
   protected static final double INF = Double.POSITIVE_INFINITY;

   protected MechModel myMech;         // model containing the markers and bodies
   protected ArrayList<BodyInfo> myBodyInfo; // bodies associated with the solver
   protected ArrayList<BodyInfo> mySortedBodyInfo; // bodies sorted by solve index
   // constrainers acting on bodies:
   protected ArrayList<BodyConstrainer> myConstrainers; 
   protected int[] myBodyVelSizes;   // velocity vector size for each body
   protected int myTotalVelSize;     // velocity vector size for all active bodies
   protected int myTotalPosSize;     // position vector size for all active bodies
   protected NumericState myModelState; // place to save/ restore MechModel state

   // solveIndexMap maps body.getSolveIndex() to the index of the body within
   // this solver. Allows us to use MechModel methods for building the
   // constraint matrices.
   protected int[] mySolveIndexMap;

   // matrices and vectors used in the constraint-based solve
   protected SparseNumberedBlockMatrix mySolveMatrix;
   protected boolean myAnalyze = true; //KKTSolver needs to perform analyze step

   // bilateral constraint info
   protected SparseBlockMatrix myGT;
   protected SparseBlockSignature myGTSignature;
   protected int myGsize = -1;
   protected VectorNi myGsizes = new VectorNi();
   protected ConstraintInfo[] myGInfo = new ConstraintInfo[0];
   protected VectorNd myRg = new VectorNd();
   protected VectorNd myBg = new VectorNd();
   protected VectorNd myLam = new VectorNd();
   private int myGTVersion = -1;

   // unilateral constraint info
   protected SparseBlockMatrix myNT;
   protected int myNsize = -1;
   protected VectorNi myNsizes = new VectorNi();
   protected ConstraintInfo[] myNInfo = new ConstraintInfo[0];   
   protected VectorNd myRn = new VectorNd();
   protected VectorNd myBn = new VectorNd();
   protected VectorNd myThe = new VectorNd();

   // KKT system solver used for the computations
   protected KKTSolver myKKTSolver;

   private double myModelSize = -1; // characteristic size of the model
   private boolean myGroundedP;     // at least one body is connected to ground

   protected double sqr (double x) {
      return x*x;
   }

   protected abstract void findBodiesAndConnectors();

   protected ArrayList<BodyInfo> getBodyInfo() {
      return myBodyInfo;
   }

   protected ArrayList<BodyInfo> getSortedBodyInfo() {
      return mySortedBodyInfo;
   }

   /**
    * Finds body and connector information if necessary.
    */
   protected void updateBodiesAndConnectors() {
      if (getBodyInfo() == null) {
         findBodiesAndConnectors();
      }
   }

   /**
    * Queries the number of rigid bodies associated with this solver.
    *
    * @return number of bodies associated with this solver
    */
   public int numBodies() {
      updateBodiesAndConnectors();
      return getBodyInfo().size();
   }

   /**
    * Returns the bodies associated with this solver. This will return all the
    * bodies, not just those used to initialize the solver.
    *
    * @return bodies associated with this solver
    */
   public ArrayList<RigidBody> getBodies() {
      updateBodiesAndConnectors();
      ArrayList<RigidBody> bodies = new ArrayList<>();
      for (BodyInfo binfo : getBodyInfo()) {
         bodies.add (binfo.myBody);
      }
      return bodies;
   }
   
   /**
    * Returns the poses of all the bodies associated with this solver. This 
    * includes all the bodies, not just those needed to initialize the solver.
    */
   public ArrayList<RigidTransform3d> getBodyPoses() {
      updateBodiesAndConnectors();
      ArrayList<RigidTransform3d> poses = new ArrayList<>();
      for (BodyInfo binfo : getBodyInfo()) {
         poses.add (new RigidTransform3d(binfo.myBody.getPose()));
      }
      return poses;
   }
   
   /**
    * Returns the MechModel associated with this solver.
    *
    * @return MechModel associated with this solver
    */
   public MechModel getMechModel() {
      return myMech;
   }

   /**
    * Returns the constrainers (e.g., joints and other constrainers) used by
    * this solver.
    *
    * @return connectors associated with this solver
    */
   public ArrayList<BodyConstrainer> getConstrainers() {
      updateBodiesAndConnectors();
      ArrayList<BodyConstrainer> connectors = new ArrayList<>();
      connectors.addAll (myConstrainers);
      return connectors;
   }

   /**
    * Returns a characteristic size for the model. Used to determine
    * convergence tolerances.
    */
   public double getModelSize() {
      if (myModelSize == -1) {
         myModelSize = computeModelSize();
      }
      return myModelSize;
   }

   /**
    * Sets a characteristic size for the model. Used to determine convergence
    * tolerances. Specifying a value {@code <= 0} will cause the size to be
    * computed automatically the next time {@link #getModelSize} is called.
    */
   public void setModelSize (double size) {
      myModelSize = size;
   }

   /**
    * Computes a characteristic size for the model. Used to determine
    * convergence tolerances.
    */
   protected double computeModelSize() {
      Point3d min = new Point3d (INF, INF, INF);
      Point3d max = new Point3d (-INF, -INF, -INF);
      for (BodyInfo binfo : getBodyInfo()) {
         binfo.myBody.updateBounds (min, max);
      }
      double diag = max.distance(min);
      if (diag == 0) {
         // paranoid - just in case
         return 1.0;
      }
      else {
         return diag/2;
      }
   }

   /**
    * Compute a norm for dq that is normalized for the size of the model.
    */
   protected double computeDqNorm (VectorNd dq) {
      double sum = 0;
      for (int i=0; i<dq.size(); i++) {
         if ((i/3)%2 == 0) {
            // weight translations for model size
            sum += sqr(dq.get(i)/myModelSize);
         }
         else {
            sum += sqr(dq.get(i));
         }
      }
      return Math.sqrt(sum/dq.size());
   }

   /**
    * Saves the current state of the MechModel. This allows the MechModel state
    * to be restored after one or more solve steps.
    */
   public void saveModelState() {
      myModelState = new NumericState();
      myMech.getState (myModelState);
   }

   /**
    * Restores the state of the MechModel.
    */
   public void restoreModelState() {
      if (myModelState == null) {
         throw new IllegalStateException (
            "saveMechState() not previously called");
      }
      myMech.setState (myModelState);      
   }

   /**
    * Creates a solve matrix. This is a block diagonal matrix with a 6 x 6
    * entry for each body.
    */
   protected SparseNumberedBlockMatrix createSolveMatrix() {
      SparseNumberedBlockMatrix S = new SparseNumberedBlockMatrix();
      for (int i=0; i<numBodies(); i++) {
         SpatialInertia blk = new SpatialInertia();
         S.addBlock (i, i, blk);
      }
      return S;
   }

   /**
    * Check if the structure of GT has changed from its previous version.
    */   
   protected boolean hasGTStructureChanged() {
      // compute GT version from GT matrix signature
      SparseBlockSignature sig = myGT.getSignature();
      int prevVersion = myGTVersion;
      if (myGTSignature == null) {
         myGTVersion++;
      }
      else {
         boolean structureChanged = !myGTSignature.equals (sig);
         if (structureChanged) {
            myGTVersion++;
         }
      }
      myGTSignature = sig;
      return myGTVersion != prevVersion;
   }

   /**
    * Collects the transposed bilateral constraint matrix {@code GT} and its
    * right hand side {@code dg}.
    */
   protected int getBilateralConstraints (SparseBlockMatrix GT, VectorNd dg) {

      if (GT.numBlockRows() != 0 || GT.numBlockCols() != 0) {
         throw new IllegalStateException (
            "On entry, GT should be empty with zero size");
      }
      getBilateralConstraintInfo();

      GT.setColCapacity (myGsizes.size());
      GT.addRows (myBodyVelSizes, myBodyVelSizes.length);
      if (dg != null) {
         dg.setSize (myGsize);
      }
      int idx = 0;
      for (BodyConstrainer bcon : myConstrainers) {
         idx = bcon.addBilateralConstraints (GT, dg, idx, mySolveIndexMap);
      }      
      // XXX not sure if we need to reduce any attachments ...
      // for (DynamicAttachment a : getAttachments()) {
      //    myAttachmentWorker.reduceConstraints (a, GT, dg, false);
      // }
      // need this for now - would be good to get rid of it:
      GT.setVerticallyLinked (true);
      return myGsize;
   }

   /**
    * Collects the current bilateral constraint information. This consists of
    * {@code myGsizes}, {@code myBg}, {@code myRg}, and {@code myGInfo}.
    * {@code myLam} is also resized.
    */
   protected void getBilateralConstraintInfo () {
      myMech.updateForceComponentList();
      myMech.updateDynamicComponentLists();
      myGsizes.setSize(0);
      for (BodyConstrainer bcon : myConstrainers) {
         bcon.getBilateralSizes (myGsizes);
      }
      myGsize = myGsizes.sum();
      myBg.setSize (myGsize);
      myRg.setSize (myGsize);
      myLam.setSize (myGsize);
      if (myGsize > 0) {
         ensureGInfoCapacity (myGsize);
         int idx = 0;
         for (BodyConstrainer bcon : myConstrainers) {
            idx = bcon.getBilateralInfo (myGInfo, idx);
         }
         for (idx=0; idx<myGsize; idx++) {
            myBg.set (idx, -myGInfo[idx].dist);
            // XXX not exactly sure what Rg should be set to if compliance !=
            // 0, but we need to set it in case compliance is used to handle
            // constraint redundancy
            myRg.set (idx, myGInfo[idx].compliance);
         }
      }
   }
   
   /**
    * Collects the transposed unilateral constraint matrix {@code NT} and its
    * right hand side {@code dn}.
    */
   protected int getUnilateralConstraints (SparseBlockMatrix NT, VectorNd dn) {

      if (NT.numBlockRows() != 0 || NT.numBlockCols() != 0) {
         throw new IllegalStateException (
            "On entry, NT should be empty with zero size");
      }
      getUnilateralConstraintInfo();

      NT.setColCapacity (myNsizes.size());
      NT.addRows (myBodyVelSizes, myBodyVelSizes.length);
      if (dn != null) {
         dn.setSize (myNsize);
      }
      int idx = 0;
      for (BodyConstrainer bcon : myConstrainers) {
         idx = bcon.addUnilateralConstraints (NT, dn, idx, mySolveIndexMap);
      }
      // XXX not sure if we need to reduce any attachments ...
      // for (DynamicAttachment a : getAttachments()) {
      //    myAttachmentWorker.reduceConstraints (a, NT, dn, false);
      // }
      // need this for now - would be good to get rid of it:
      NT.setVerticallyLinked (true);
      return myNsize;
   }

   /**
    * Collects the current unilateral constraint information. This consists of
    * {@code myNsizes}, {@code myBn}, {@code myRn}, and {@code myNInfo}.
    * {@code myThe} is also resized.
    */
   protected void getUnilateralConstraintInfo () {   
      myMech.updateForceComponentList();
      //myMech.myUnilateralSizes.setSize (0);
      myNsizes.setSize(0);
      for (BodyConstrainer bcon : myConstrainers) {
         bcon.getUnilateralSizes (myNsizes);
      }
      myNsize = myNsizes.sum();
      myBn.setSize (myNsize);
      myRn.setSize (myNsize);
      myThe.setSize (myNsize);
      if (myNsize > 0) {
         ensureNInfoCapacity (myNsize);
         int idx = 0;
         for (BodyConstrainer bcon : myConstrainers) {
            idx = bcon.getUnilateralInfo (myNInfo, idx);
         }
         for (idx=0; idx<myNsize; idx++) {
            myBn.set (idx, -myNInfo[idx].dist);
            // XXX not exactly sure what Rn should be set to if compliance !=
            // 0, but we need to set it in case compliance is used to handle
            // constraint redundancy
            myRn.set (idx, myNInfo[idx].compliance);
         }
      }
   }

   /**
    * Expand GInfo array to length {@code cap}.
    */
   protected void ensureGInfoCapacity (int cap) {
      if (myGInfo.length < cap) {
         int oldcap = myGInfo.length;
         myGInfo = Arrays.copyOf (myGInfo, cap);
         for (int i=oldcap; i<cap; i++) {
            myGInfo[i] = new MechSystem.ConstraintInfo();
         }
      }
   }

   /**
    * Expand NInfo array to length {@code cap}.
    */
   protected void ensureNInfoCapacity (int cap) {
      if (myNInfo.length < cap) {
         int oldcap = myNInfo.length;
         myNInfo = Arrays.copyOf (myNInfo, cap);
         for (int i=oldcap; i<cap; i++) {
            myNInfo[i] = new MechSystem.ConstraintInfo();
         }
      }
   }

   /**
    * Update bilateral and unilateral constraint information and constraint
    * matrices.
    */
   protected void updateConstraints () {
      for (BodyConstrainer bcon : myConstrainers) {
         bcon.updateConstraints (/*time*/0, /*flags*/0);
      }
      myGT = new SparseBlockMatrix ();
      getBilateralConstraints (myGT, null);
      myNT = new SparseBlockMatrix ();
      getUnilateralConstraints (myNT, null);
   }

   /**
    * Update bilateral and unilateral constraint information, without the
    * constraint matrices.
    */
   protected void updateConstraintInfo () {
      for (BodyConstrainer bcon : myConstrainers) {
         bcon.updateConstraints (/*time*/0, /*flags*/0);
      }
      getBilateralConstraintInfo();
      getUnilateralConstraintInfo();
   }

   /**
    * Update the solve index map. This maps each body's solve index (as
    * allocated by the MechModel and returned by its {@code getSolveIndex()}
    * method) into the body's index within this solver. This allows as to
    * create bilateral and unilateral constraint matrices whose block row
    * indices correspond to the body indices with the solver. Since solve
    * indices may change within the MechModel, it is necessary to check the
    * index map before each solve step and rebuild it if necessary.
    */
   protected void updateSolveIndexMap (MechModel mech) {
      // calling numActiveComponents ensures solve indices are updated
      int numc = mech.numActiveComponents() + mech.numParametricComponents();
      int velSize = 0;
      int posSize = 0;
      // check if solve index map is still valid
      for (BodyInfo binfo : getBodyInfo()) {
         RigidBody body = binfo.myBody;
         if (binfo.mySolveIndex != body.getSolveIndex()) {
            // not valid
            mySolveIndexMap = null;
         }
         velSize += body.getVelStateSize();
         posSize += body.getPosStateSize();
      }
      if (velSize != myTotalVelSize ||
          posSize != myTotalPosSize) {
         myTotalVelSize = velSize;
         myTotalPosSize = posSize;
         mySolveIndexMap = null;
      }
      if (mySolveIndexMap == null) {
         mySortedBodyInfo = new ArrayList<>();
         mySortedBodyInfo.addAll (myBodyInfo);
         Collections.sort (mySortedBodyInfo, new BodyInfoComparator());
         int[] map = new int[numc];
         for (int i=0; i<numc; i++) {
            map[i] = -1;
         }
         int index = 0;
         myBodyVelSizes = new int[numBodies()];
         for (BodyInfo binfo : getSortedBodyInfo()) {
            int solveIndex = binfo.myBody.getSolveIndex();
            map[solveIndex] = index;
            binfo.mySolveIndex = solveIndex;
            myBodyVelSizes[index] = binfo.myBody.getVelStateSize();
            index++;
         }
         mySolveIndexMap = map;
      }
   }

   /**
    * Collects the current body position state into the vector {@code q}.
    */
   protected void getPosState (VectorNd q) {
      q.setSize (7*numBodies());
      double[] qbuf = q.getBuffer();
      int idx = 0;
      for (BodyInfo binfo : getSortedBodyInfo()) {
         idx = binfo.myBody.getPosState (qbuf, idx);
      }
   }

   /**
    * Sets the body position state from the vector {@code q}.
    */
   protected void setPosState (VectorNd q) {
      double[] qbuf = q.getBuffer();
      int idx = 0;
      for (BodyInfo binfo : getSortedBodyInfo()) {
         idx = binfo.myBody.setPosState (qbuf, idx);
      }
   }

   /**
    * Changes the body position state stored in {@code q} by applying a
    * differential increment {@code dq} multiplied by a step size {@code
    * h}. Note that {@code q} and {@code dq} have different sizes, since
    * rotations are stored as quaternions in the former while angular
    * velocities are 3-vectors in the latter.
    */
   protected void addPosImpulse (VectorNd q, VectorNd dq, double h) {
      double[] qbuf = q.getBuffer();
      double[] dbuf = dq.getBuffer();
      int qidx = 0;
      int didx = 0;
      for (BodyInfo binfo : getSortedBodyInfo()) {
         RigidBody body = binfo.myBody;
         body.addPosImpulse (qbuf, qidx, h, dbuf, didx);
         qidx += body.getPosStateSize();
         didx += body.getVelStateSize();
      }
   }

   /**
    * Changes the body positions by applying a differential increment {@code
    * dq} to the position state.
    */
   protected void updatePosState (VectorNd q, VectorNd dq, double scale) {
      VectorNd qnew = new VectorNd (q);
      addPosImpulse (qnew, dq, scale);
      setPosState (qnew);
   }

   /**
    * Performs a single linearized position update to project the current body
    * positions to the constraints.
    */
   protected VectorNd projectToConstraints () {
      int velSize = myTotalVelSize;
      int posSize = myTotalPosSize;

      VectorNd dq = new VectorNd (velSize);
      VectorNd q = new VectorNd (posSize);
      VectorNd fzero = new VectorNd(velSize); // zero valued force vector

      updateSolveMatrix (mySolveMatrix);
      updateConstraints ();
      getPosState (q);
      myKKTSolver.factor (mySolveMatrix, velSize, myGT, myRg, myNT, myRn);
      myKKTSolver.solve (dq, myLam, myThe, fzero, myBg, myBn);     
      updatePosState (q, dq, 1.0);
      return dq;
   }

   /**
    * Updates the system solve matrix {@code S}. This matrix is block
    * diagnonal, with 6 x 6 entries for each of the bodies. By default, each
    * block is updated with its body's current mass matrix. However, subclasses
    * may wish to override this method to supply different values.
    */
   protected void updateSolveMatrix (SparseNumberedBlockMatrix S) {
      
      int bidx = 0; // body index
      for (BodyInfo binfo : getSortedBodyInfo()) {
         RigidBody body = binfo.myBody;
         SpatialInertia blk = (SpatialInertia)S.getBlock (bidx, bidx);
         body.getEffectiveMass (blk, /*t=*/0);
         bidx++;
      }
   }

   /**
    * Performs repeated position updates until the error is below a specified
    * tolerance, or the number of iterations exceeds {@code maxIter}.
    *
    * @param tol desired tolerance for the error
    * @param maxIter maximum number of iterations
    * @return iteration count
    */
   public int projectToConstraints (double tol, int maxIter) {
      updateBodiesAndConnectors();
      updateSolveIndexMap(myMech);

      int velSize = myTotalVelSize;
      int posSize = myTotalPosSize;

      VectorNd dq = new VectorNd (velSize);
      VectorNd q = new VectorNd (posSize);
      VectorNd fzero = new VectorNd(velSize); // zero valued force vector
      
      int iter = 0;
      do {
         getPosState (q);
         updateSolveMatrix (mySolveMatrix);
         updateConstraints ();
         if (iter == 0 && hasGTStructureChanged()) {
            myAnalyze = true;
         }
         if (myAnalyze) {
            int matrixType = Matrix.SYMMETRIC;
            myKKTSolver.analyze (mySolveMatrix, velSize, myGT, myRg, matrixType);
            myAnalyze = false;
         }     
         myKKTSolver.factor (mySolveMatrix, velSize, myGT, myRg, myNT, myRn);
         myKKTSolver.solve (dq, myLam, myThe, fzero, myBg, myBn);     
         updatePosState (q, dq, 1.0);
         iter++;
      }
      while (iter < maxIter && computeDqNorm(dq) > tol);
      updateAttachmentPosStates();
      return iter;
   }

   /**
    * Searches the bodies used by this solver for one that should be considers
    * to have a fixed position. The first choice is any body that has its
    * {@code grounded} property set. The second choice is any body that was
    * non-dynamic when this solver was initialized.
    *
    * @return grounded body, or {@code null} if none found
    */
   public RigidBody findFixedBody() {
      updateBodiesAndConnectors();
      for (BodyInfo binfo : getBodyInfo()) {
         if (binfo.myBody.isGrounded()) {
            return binfo.myBody;
         }
      }
      for (BodyInfo binfo : getBodyInfo()) {
         if (!binfo.myDynamic) {
            return binfo.myBody;
         }
      }
      return null;
   }

   /**
    * Transform the poses of all bodies used by this solver, by applying
    * a transform {@code T} in world coordinates. If {@code TBW} is the
    * pose of a given body, then the new pose will be given by
    * <pre>
    * TBW = T TBW
    * </pre>
    *
    * @param T transform to apply to body poses
    */
   public void transformBodyPoses (RigidTransform3d T) {
      updateBodiesAndConnectors();
      for (BodyInfo binfo : getBodyInfo()) {
         RigidBody body = binfo.myBody;
         RigidTransform3d TBW = new RigidTransform3d(body.getPose());
         TBW.mul (T, TBW);
         body.setPose (TBW);
      }
   }

   protected void findBodiesAndConstrainers (
      HashSet<RigidBody> bodySet,
      HashSet<BodyConstrainer> constrainerSet, boolean excludeGrounded) {

      ArrayDeque<RigidBody> bodyQueue = new ArrayDeque<>();
      for (RigidBody body : bodySet) {
         bodyQueue.offer (body);
      }
      myGroundedP = false;
      while (!bodyQueue.isEmpty()) {
         RigidBody body = bodyQueue.remove();
         if (body.getConnectors() != null) {
            for (BodyConnector bcon : body.getConnectors()) {
               if (!constrainerSet.contains(bcon) &&
                   // ensure connector is part of the component hierarchy -
                   // won't be if connector was created but not added to model:
                   ComponentUtils.isAncestorOf (myMech, bcon) &&
                   bcon.isEnabled()) {
                  RigidBody cbody;
                  constrainerSet.add (bcon);
                  if (bcon.getBodyA() instanceof RigidBody) {
                     cbody = (RigidBody)bcon.getBodyA();
                     if (!bodySet.contains(cbody) &&
                         (!excludeGrounded || !cbody.isGrounded())) {
                        bodySet.add(cbody);
                        bodyQueue.add (cbody);
                     }
                  }
                  else if (bcon.getBodyA() == null) {
                     myGroundedP = true;
                  }
                  if (bcon.getBodyB() instanceof RigidBody) {
                     cbody = (RigidBody)bcon.getBodyB();
                     if (!cbody.isGrounded() &&
                         (!excludeGrounded || !bodySet.contains(cbody))) {
                        bodySet.add(cbody);
                        bodyQueue.add (cbody);
                     }
                  }
                  else if (bcon.getBodyB() == null) {
                     myGroundedP = true;
                  }
               }
            }
         }
      }
      // find additional constrainers within the MechModel
      LinkedList<Constrainer> allModelConstrainers = new LinkedList<>();
      myMech.getConstrainers (allModelConstrainers, /*level*/0);
      HashSet<DynamicComponent> connected = new HashSet<>();
      for (Constrainer c : allModelConstrainers) {
         if (c instanceof BodyConstrainer && !constrainerSet.contains(c)) {
            BodyConstrainer bcon = (BodyConstrainer)c;
            connected.clear();
            bcon.getConstrainedComponents (connected);
            for (RigidBody body : bodySet) {
               if (connected.contains (body)) {
                  constrainerSet.add (bcon);
                  break;
               }
            }
         }
      }

      // store constrainer information
      myConstrainers = new ArrayList<>();
      myConstrainers.addAll (constrainerSet);

      // store information about each body
      myBodyInfo = new ArrayList<>();
      int idx = 0;
      for (RigidBody body : bodySet) {
         BodyInfo binfo = new BodyInfo (body);
         binfo.myIndex = idx++;
         binfo.myDynamic = body.isDynamic();
         myBodyInfo.add (binfo);
      }
      setModelSize (-1);
      mySolveMatrix = createSolveMatrix();
   }

   /**
    * Sets the {@code dynamic} property of all the bodies associated
    * with this solver.
    *
    * @param dynamic setting for each body's {@code dynamic} property
    */
   public void setBodiesDynamic (boolean dynamic) {
      updateBodiesAndConnectors();
      for (BodyInfo binfo : myBodyInfo) {
         binfo.myBody.setDynamic (dynamic);
      }
   }

   /**
    * Restore the {@code dynamic} property setting of all the bodies associated
    * with this solver to their value at initialization.
    */
   public void resetBodiesDynamic () {
      updateBodiesAndConnectors();
      for (BodyInfo binfo : myBodyInfo) {
         binfo.myBody.setDynamic (binfo.myDynamic);
      }
   }

   /**
    * Returns {@code true} if at least one of the bodies associated with this
    * solver is connected to ground via a {@code BodyConnector}. Note that if
    * the bodies are arranged into two or more disconnected chains, this means
    * that at least one chain is grounded, but not necessarily all of them.
    *
    * @return {@code true} if one of the bodies is connected to ground.
    */
   public boolean isConnectedToGround() {
      return myGroundedP;
   }

   protected void updateAttachmentPosStates() {
      updateBodiesAndConnectors();
      for (BodyInfo binfo : myBodyInfo) {
         binfo.myBody.updateAttachmentPosStates();
      }
   }

}
