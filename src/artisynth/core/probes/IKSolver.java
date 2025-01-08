package artisynth.core.probes;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import artisynth.core.mechmodels.BodyConnector;
import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.MechSystem;
import artisynth.core.mechmodels.MechSystem.ConstraintInfo;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.NumericState;
import artisynth.core.modelbase.PostScannable;
import artisynth.core.modelbase.ScanWriteUtils;
import artisynth.core.util.ScanToken;
import artisynth.core.util.StringToken;
import maspack.interpolation.NumericList;
import maspack.interpolation.NumericListKnot;
import maspack.matrix.Matrix;
import maspack.matrix.Matrix3d;
import maspack.matrix.Point3d;
import maspack.matrix.Quaternion;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.RotationRep;
import maspack.matrix.SVDecomposition3d;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.SparseNumberedBlockMatrix;
import maspack.matrix.SymmetricMatrix3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.matrix.VectorNi;
import maspack.solvers.KKTSolver;
import maspack.spatialmotion.SpatialInertia;
import maspack.spatialmotion.Twist;
import maspack.util.FunctionTimer;
import maspack.util.IndentingPrintWriter;
import maspack.util.IntHolder;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

/**
 * Performs inverse kinematics computations to compute rigid body positions
 * from marker data, while enforcing constraints between the bodies imposed by
 * joints and other body connectors. In multibody biomechanics, can be used to
 * filter marker data to determine feasible body trajectories, prior to inverse
 * dynamic calculations.
 */
public class IKSolver implements PostScannable {

   private static final double DOUBLE_PREC = 1e-16;
   private static final double INF = Double.POSITIVE_INFINITY;
   // If true, use the derivative of J in constructing the main mass block.
   // Results in a non-symmetric solve matrix. Set to false by default since we
   // have not yet seen that this improves convergence.
   private static final boolean myUseJDerivative = false;

   /**
    * Information about the rigid bodies associated with this solve
    */
   protected class BodyInfo {
      RigidBody myBody; // rigid body
      ArrayList<MarkerInfo> myMarkerInfo; // markers (if any) attached to the body
      int mySolveIndex; // current body solve index, as defined by the MechModel
      SpatialInertia myJTJ; // "inertia" imparted by the markers

      BodyInfo (RigidBody body, ArrayList<MarkerInfo> minfos) {
         myBody = body;
         myMarkerInfo = minfos;
      }

      void updateJTJMatrix (double avgWeight) {
         double massRegularization = avgWeight*myMassRegularization;
         if (myMarkerInfo.size() == 0) {
            myJTJ = new SpatialInertia();
            double s = massRegularization;
            myBody.getInertia (myJTJ);
            if (myJTJ.getMass() == 0) {
               myJTJ.setDiagonal (s, s, s, s, s, s);
            }
            else {
               myJTJ.scale (s/myJTJ.getMass());
            }
         }
         else {
            ArrayList<Vector3d> pnts = new ArrayList<>();
            VectorNd wgts = new VectorNd (myMarkerInfo.size());
            int i = 0;
            for (MarkerInfo minfo : myMarkerInfo) {
               pnts.add (minfo.myMarker.getLocation());
               wgts.set (i++, minfo.myWeight);
            }
            myJTJ = IKSolver.buildJTJMatrix (
               pnts, wgts, massRegularization, /*rank*/null);
         }
      }
   }

   /**
    * Information about a marker and its weight
    */
   protected class MarkerInfo {
      FrameMarker myMarker;
      double myWeight;

      MarkerInfo (FrameMarker mkr, double w) {
         myMarker = mkr;
         myWeight = w;
      }

      MarkerInfo (FrameMarker mkr) {
         this (mkr, 1.0);
      }
   }

   // computation control properties

   static final double DEFAULT_MASS_REGULARIZATION = 0.001;
   double myMassRegularization = DEFAULT_MASS_REGULARIZATION;

   static final int DEFAULT_MAX_ITERATIONS = 30;
   int myMaxIterations = DEFAULT_MAX_ITERATIONS;

   static final double DEFAULT_CONVERGENCE_TOL = 1e-8;
   double myConvergenceTol = DEFAULT_CONVERGENCE_TOL;
   
   private double myModelSize = -1; // characteristic size of the model

   // fundamental components
   MechModel myMech;                   // model containing the markers and bodies
   // markers to be tracked, together with their weights
   ArrayList<MarkerInfo> myMarkerInfo;  
   int myNumMarkers; // store separately in case this is needed during scan

   // component information obtained from the fundamental components
   ArrayList<BodyInfo> myBodyInfo; // all bodies associated with the markers
   ArrayList<BodyConnector> myConnectors; // all body-contraining connectors
   int[] myBodyVelSizes;          // velocity vector size for each body
   int myTotalVelSize;            // velocity vector size for all bodies
   NumericState myModelState;     // place to save and restore MechModel state

   int myNumIterations;           // cummulative iteration count
   int myNumSolves;               // cummulative solve count

   // solveIndexMap maps body.getSolveIndex() to the index of the body within
   // this solver. Allows us to use MechModel methods for building the
   // constraint matrices.
   int[] mySolveIndexMap;

   // matrices and vectors used in the inverse kinematic computation
   SparseNumberedBlockMatrix mySolveMatrix;
   boolean myAnalyze = true; // if true, KKTSolver needs to perform analyze step
   VectorNd myBd = new VectorNd();

   // bilateral constraint info
   SparseBlockMatrix myGT;
   int myGsize = -1;
   private ConstraintInfo[] myGInfo = new ConstraintInfo[0];
   VectorNd myRg = new VectorNd();
   VectorNd myBg = new VectorNd();
   VectorNd myLam = new VectorNd();

   // unilateral constraint info
   SparseBlockMatrix myNT;
   int myNsize = -1;
   private ConstraintInfo[] myNInfo = new ConstraintInfo[0];   
   VectorNd myRn = new VectorNd();
   VectorNd myBn = new VectorNd();
   VectorNd myThe = new VectorNd();

   // KKT system solver used for the computations
   KKTSolver myKKTSolver;

   /**
    * Creates an empty IKSolver.
    */
   public IKSolver () {
      myMarkerInfo = new ArrayList<>();
      myKKTSolver = new KKTSolver();
   }

   /**
    * Creates an IKSolver for a set of markers contained within a prescribed
    * {@code MechModel}. All markers are assumed to have a weight of 1. The
    * rigid bodies and connectors are determined automatically from the
    * markers. The frames associated with each marker must be a rigid body, and
    * the set of all such bodies is known as the <i>marker bodies</i>. The
    * bodies whose positions are updating by the solver includes the marker
    * bodies, plus any other rigid bodies bodies connected to them via {@code
    * BodyConnector}s, up to and excluding any rigid body whose {@code
    * grounded} property is set to {@code true}.
    * 
    * @param mech MechModel containing the markers and bodies
    * @param mkrs markers determining which bodies will be controlled
    */
   public IKSolver (MechModel mech, Collection<FrameMarker> mkrs) {
      this (mech, mkrs, /*weights*/null);
   }

   /**
    * Creates an IKSolver for a set of markers contained within a prescribed
    * {@code MechModel}. Markers weights can be specified by the optional
    * vector {@code weights}. The rigid bodies and connectors are determined
    * automatically from the markers. The frames associated with each marker
    * must be a rigid body, and the set of all such bodies is known as the
    * <i>marker bodies</i>. The bodies whose positions are updating by the
    * solver includes the marker bodies, plus any other rigid bodies bodies
    * connected to them via {@code BodyConnector}s, up to and excluding any
    * rigid body whose {@code grounded} property is set to {@code true}.
    * 
    * @param mech MechModel containing the markers and bodies
    * @param mkrs markers determining which bodies will be controlled
    * @param weights if non-{@code null}, specifies weights for the markers
    */
   public IKSolver (
      MechModel mech, Collection<FrameMarker> mkrs, VectorNd weights) {
      this();
      if (mkrs.size() == 0) {
         throw new IllegalArgumentException (
            "marker set 'mkrs' is empty");
      }
      myMech = mech;
      if (weights == null) {
         weights = new VectorNd (mkrs.size());
         weights.setAll (1.0);
      }
      if (weights.size() < mkrs.size()) {
         throw new IllegalArgumentException (
            "weights size "+weights.size()+
            " incompatible with number of markers "+mkrs.size());
      }
      int i = 0;
      for (FrameMarker mkr : mkrs) {
         myMarkerInfo.add (new MarkerInfo (mkr, weights.get(i++)));
      }
      myNumMarkers = myMarkerInfo.size();
      findBodiesAndConnectors ();
   }

   /**
    * Creates an IKSolver which is a copy of another IKSolver.
    * 
    * @param solver IKSolver to copy
    */
   public IKSolver (IKSolver solver) {
      this (solver.getMechModel(), solver.getMarkers());
   }

   /**
    * Queries the mass regulaization coefficient for this solver.
    *
    * @see #setMassRegularization
    * @return mass regulaization coefficient
    */
   public double getMassRegularization() {
      return myMassRegularization;
   }

   /**
    * Sets the mass regularization coefficent {@code c} for this solver. The 
    * default value is 0.001.
    *
    * <p>Each body is associated with a solve matrix defined by {@code J^T J},
    * where {@code J} is the matrix mapping differential changes in body pose
    * to changes in marker position. This matrix provides the Hessian for the
    * minimization. If the number of markers is less than 3, or if the markers
    * all lie along a line, the solve matrix will be rank deficient. In that
    * case, regularization terms are added to the matrix in such a way as to
    * make it non-singular while preserving its range space. The size of these
    * terms, relative to the weight of the matrix itself, is given by {@code
    * c}.
    *
    * @see #getMassRegularization
    * @param c mass regulaization coefficient
    */
   public void setMassRegularization (double c) {
      myMassRegularization = c;
   }

   /**
    * Queries the maximum number of iterations allowed in each solve step.
    *
    * @see #setMaxIterations
    * @return maximum number of solve iterations
    */
   public int getMaxIterations() {
      return myMaxIterations;
   }

   /**
    * Set the maximum number of iterations allowed in each solve step.  The
    * default value is 20.
    *
    * @see #getMaxIterations
    * @param maxi maximum number of solve iterations
    */
   public void setMaxIterations (int maxi) {
      myMaxIterations = maxi;
   }

   /**
    * Queries the convergance tolerance used in each solve step.
    * 
    * #see #setConvergenceTol
    * @return convergence tolerance
    */
   public double getConvergenceTol() {
      return myConvergenceTol;
   }

   /**
    * Sets the convergance tolerance {@code tol} used in each solve step. A
    * solve step will be considered converged with the incremental translation
    * and rotation for all bodies is less then {@code tol*bodyLen} and
    * {@code tol}, respectively, where {@code bodyLen} is the charateristic
    * length of the body as inferrd from its spatial inertia. The
    * default value of {@code tol} is {@code 1e-8}.
    * 
    * #see #getConvergenceTol
    * @param tol convergence tolerance
    */
   public void setConvergenceTol (double tol) {
      myConvergenceTol = tol;
   }

   /**
    * Finds body and connector information if necessary.
    */
   private void updateBodiesAndConnectors() {
      if (myBodyInfo == null) {
         findBodiesAndConnectors();
      }
   }

   /**
    * Finds body and connector information.
    */
   private void findBodiesAndConnectors () {
      LinkedHashMap<RigidBody,ArrayList<MarkerInfo>> bodyMarkerMap =
         new LinkedHashMap<>();

      if (myMech == null) {
         throw new IllegalStateException (
            "Solver does not reference a MechModel");
      }
      int i = 0; // marker index
      double avgWeight = 0;
      for (MarkerInfo minfo : myMarkerInfo) {
         FrameMarker mkr = minfo.myMarker;
         String name = mkr.getName();
         if (name == null) {
            name = Integer.toString(i);
         }
         if (!ComponentUtils.isAncestorOf (myMech, mkr)) {
            throw new IllegalStateException (
               "Marker '"+name+"' not found in the MechModel");
         }
         if (!(mkr.getFrame() instanceof RigidBody)) {
            throw new IllegalStateException (
               "Marker '"+name+"' not attached to a rigid body");
         }
         RigidBody body = (RigidBody)mkr.getFrame();
         if (body.getVelStateSize() != 6) {
            throw new IllegalStateException (
               "Marker '"+name+"' attached to a body with more than 6 DOF");
         }
         ArrayList<MarkerInfo> mlist = bodyMarkerMap.get(body);
         if (mlist == null) {
            mlist = new ArrayList<>();
            bodyMarkerMap.put (body, mlist);
         }
         avgWeight += minfo.myWeight;
         mlist.add (minfo);
         i++;
      }
      avgWeight /= myMarkerInfo.size();
      // find all connectors attached to the frames. Find additional bodies
      // that are attached via connectors. Stop the search at bodies whose
      // {@code grounded} property is {@code true}.
      LinkedHashSet<BodyConnector> connectors = new LinkedHashSet<>();
      ArrayDeque<RigidBody> bodyQueue = new ArrayDeque<>();
      bodyQueue.addAll (bodyMarkerMap.keySet());
      while (!bodyQueue.isEmpty()) {
         RigidBody body = bodyQueue.remove();
         if (body.getConnectors() != null) {
            for (BodyConnector bcon : body.getConnectors()) {
               if (!connectors.contains(bcon) &&
                   // ensure connector is part of the component hierarchy -
                   // won't be if connector was created but not added to model:
                   ComponentUtils.isAncestorOf (myMech, bcon) &&
                   bcon.isEnabled()) {
                  RigidBody cbody;
                  connectors.add (bcon);
                  if (!(bcon.getBodyA() instanceof RigidBody)) {
                     throw new IllegalStateException (
                        "Connector " + pathName(bcon) +
                        " attached to non-rigid body " +
                        pathName(bcon.getBodyA()));
                  }
                  cbody = (RigidBody)bcon.getBodyA();
                  if (!cbody.isGrounded() && 
                      bodyMarkerMap.get(cbody) == null) {
                     bodyMarkerMap.put(cbody, new ArrayList<>());
                     bodyQueue.add (cbody);
                  }
                  if (bcon.getBodyB() != null) {
                     if (!(bcon.getBodyB() instanceof RigidBody)) {
                        throw new IllegalStateException (
                           "Connector " + pathName(bcon) +
                           " attached to non-rigid body " +
                           pathName(bcon.getBodyB()));
                     }
                     cbody = (RigidBody)bcon.getBodyB();
                     if (!cbody.isGrounded() &&
                         bodyMarkerMap.get(cbody) == null) {
                        bodyMarkerMap.put(cbody, new ArrayList<>());
                        bodyQueue.add (cbody);
                     }
                  }
               }
            }
         }
      }
      // store connector information
      myConnectors = new ArrayList<>();
      myConnectors.addAll (connectors);

      // store information about each body
      myBodyInfo = new ArrayList<>();
      int numBodies = bodyMarkerMap.size();
      myBodyVelSizes = new int[numBodies];
      myTotalVelSize = 0;
      int bi = 0; // body index
      for (RigidBody body : bodyMarkerMap.keySet()) {
         int vsize = body.getVelStateSize();
         myBodyVelSizes[bi] = vsize;
         myTotalVelSize += vsize;
         BodyInfo binfo = new BodyInfo (body, bodyMarkerMap.get(body));
         // precompute the JTJ matrix in body coordinates
         binfo.updateJTJMatrix (avgWeight);
         myBodyInfo.add (binfo);
         bi++;
      }
      myModelSize = findModelSize();
      mySolveMatrix = createSolveMatrix();
   }

   /**
    * Creates a solve matrix. This is a block diagonal matrix with a 6 x 6
    * entry for each body.
    */
   private SparseNumberedBlockMatrix createSolveMatrix() {
      SparseNumberedBlockMatrix S = new SparseNumberedBlockMatrix();
      for (int i=0; i<numBodies(); i++) {
         SpatialInertia blk = new SpatialInertia();
         S.addBlock (i, i, blk);
      }
      return S;
   }

   /**
    * Finds a characteristic size for the model. Used to determine
    * convergence tolerances.
    */
   private double findModelSize() {
      Point3d min = new Point3d (INF, INF, INF);
      Point3d max = new Point3d (-INF, -INF, -INF);
      for (BodyInfo binfo : myBodyInfo) {
         binfo.myBody.updateBounds (min, max);
      }
      for (MarkerInfo minfo : myMarkerInfo) {
         minfo.myMarker.updateBounds (min, max);
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
    * Computes a vector of displacements from the current marker positions to
    * their desired postions {@code mtargs}.
    */
   private double computeDisplacements (
      VectorNd disps, VectorNd mtargs) {
      
      Vector3d targ = new Vector3d();
      Vector3d disp = new Vector3d(); // required marker displacements
      double chiSqr = 0;
      int j = 0; // marker index
      for (BodyInfo binfo : myBodyInfo) {
         for (MarkerInfo minfo : binfo.myMarkerInfo) {
            mtargs.getSubVector (3*j, targ);
            disp.sub (targ, minfo.myMarker.getPosition());
            disps.setSubVector (3*j, disp);
            chiSqr += disp.dot(disp)*minfo.myWeight;
            j++;
         }
      }
      return chiSqr;
   }

   /**
    * Adds the second derivative term related to the time derivative of locw to
    * a body's JTJ matrix.
    *
    * @param r marker position relative to body, rotated to world coordinated
    * @param y weighted displacement to the marker target position
    */
   private void addHj (SpatialInertia blk, Vector3d r, Vector3d y) {
      blk.m33 += (r.y*y.y + r.z*y.z);
      blk.m44 += (r.x*y.x + r.z*y.z);
      blk.m55 += (r.x*y.x + r.y*y.y);

      blk.m34 -= r.y*y.x;
      blk.m35 -= r.z*y.x;
      blk.m45 -= r.z*y.y;

      blk.m43 -= r.x*y.y;
      blk.m53 -= r.x*y.z;
      blk.m54 -= r.y*y.z;
   }

   /**
    * Updates the system solve matrix {@code S} and its right hand side {@code
    * b} prior to the next iteration step.
    */
   private void updateSolveMatrix (
      SparseNumberedBlockMatrix S, VectorNd b, VectorNd mtargs, VectorNd disps) {

      Vector3d locw = new Vector3d();
      Vector3d disp = new Vector3d(); // required marker displacements
      Twist cb = new Twist();
      int k = 0; // body index
      int j = 0; // marker index
      for (BodyInfo binfo : myBodyInfo) {
         RigidBody body = binfo.myBody;
         SpatialInertia blk = (SpatialInertia)S.getBlock (k, k);
         RotationMatrix3d R = body.getPose().R; // rotates body coords to world
         binfo.myJTJ.getRotated (blk, R);
         cb.setZero();
         for (MarkerInfo minfo : binfo.myMarkerInfo) {
            locw.transform (R, minfo.myMarker.getLocation());
            disps.getSubVector (3*j, disp);
            disp.scale (minfo.myWeight);
            cb.v.add (disp);
            disp.cross (locw, disp);
            cb.w.add (disp);
            if (myUseJDerivative) {
               addHj (blk, locw, disp);
            }
            j++;
         }
         b.setSubVector (6*k, cb);         
         k++;
      }
   }

   /**
    * Collects the transposed bilateral constraint matrix {@code GT} and its
    * right hand side {@code dg}.
    */
   private int getBilateralConstraints (SparseBlockMatrix GT, VectorNd dg) {

      if (GT.numBlockRows() != 0 || GT.numBlockCols() != 0) {
         throw new IllegalStateException (
            "On entry, GT should be empty with zero size");
      }
      myMech.updateForceComponentList();
      myMech.updateDynamicComponentLists();
      VectorNi bilateralSizes = new VectorNi();
      for (BodyConnector bcon : myConnectors) {
         bcon.getBilateralSizes (bilateralSizes);
      }
      GT.setColCapacity (bilateralSizes.size());
      GT.addRows (myBodyVelSizes, myBodyVelSizes.length);
      
      int sizeG = bilateralSizes.sum();
      if (dg != null) {
         dg.setSize (sizeG);
      }
      updateSolveIndexMap(myMech);
      int idx = 0;
      for (BodyConnector bcon : myConnectors) {
         idx = bcon.addBilateralConstraints (GT, dg, idx, mySolveIndexMap);
      }      
      // XXX not sure if we need to reduce any attachments ...
      // for (DynamicAttachment a : getAttachments()) {
      //    myAttachmentWorker.reduceConstraints (a, GT, dg, false);
      // }
      // need this for now - would be good to get rid of it:
      GT.setVerticallyLinked (true);

      myBg.setSize (sizeG);
      myRg.setSize (sizeG);
      myLam.setSize (sizeG);

      if (sizeG > 0) {
         ensureGInfoCapacity (sizeG);
         idx = 0;
         for (BodyConnector bcon : myConnectors) {
            idx = bcon.getBilateralInfo (myGInfo, idx);
         }
         for (idx=0; idx<sizeG; idx++) {
            myBg.set (idx, -myGInfo[idx].dist);
            // XXX not exactly sure what Rg should be set to if compliance !=
            // 0, but we need to set it in case compliance is used to handle
            // constraint redundancy
            myRg.set (idx, myGInfo[idx].compliance);
         }
      }
      return sizeG;
   }
   
   /**
    * Collects the transposed unilateral constraint matrix {@code NT} and its
    * right hand side {@code dn}.
    */
   private int getUnilateralConstraints (SparseBlockMatrix NT, VectorNd dn) {

      if (NT.numBlockRows() != 0 || NT.numBlockCols() != 0) {
         throw new IllegalStateException (
            "On entry, NT should be empty with zero size");
      }
      myMech.updateForceComponentList();
      //myMech.myUnilateralSizes.setSize (0);
      VectorNi unilateralSizes = new VectorNi();
      for (BodyConnector bcon : myConnectors) {
         bcon.getUnilateralSizes (unilateralSizes);
      }
      NT.setColCapacity (unilateralSizes.size());
      NT.addRows (myBodyVelSizes, myBodyVelSizes.length);
      int sizeN = unilateralSizes.sum();
      if (dn != null) {
         dn.setSize (sizeN);
      }
      updateSolveIndexMap(myMech);
      int idx = 0;
      for (BodyConnector bcon : myConnectors) {
         idx = bcon.addUnilateralConstraints (NT, dn, idx, mySolveIndexMap);
      }
      // XXX not sure if we need to reduce any attachments ...
      // for (DynamicAttachment a : getAttachments()) {
      //    myAttachmentWorker.reduceConstraints (a, NT, dn, false);
      // }
      // need this for now - would be good to get rid of it:
      NT.setVerticallyLinked (true);

      myBn.setSize (sizeN);
      myRn.setSize (sizeN);
      myThe.setSize (sizeN);

      if (sizeN > 0) {
         ensureNInfoCapacity (sizeN);
         idx = 0;
         for (BodyConnector bcon : myConnectors) {
            idx = bcon.getUnilateralInfo (myNInfo, idx);
         }
         for (idx=0; idx<sizeN; idx++) {
            myBn.set (idx, -myNInfo[idx].dist);
            // XXX not exactly sure what Rn should be set to if compliance !=
            // 0, but we need to set it in case compliance is used to handle
            // constraint redundancy
            myRn.set (idx, myNInfo[idx].compliance);
         }
      }
      return sizeN;
   }

   /**
    * Expand GInfo array to length {@code cap}.
    */
   private void ensureGInfoCapacity (int cap) {
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
   private void ensureNInfoCapacity (int cap) {
      if (myNInfo.length < cap) {
         int oldcap = myNInfo.length;
         myNInfo = Arrays.copyOf (myNInfo, cap);
         for (int i=oldcap; i<cap; i++) {
            myNInfo[i] = new MechSystem.ConstraintInfo();
         }
      }
   }

   /**
    * Update bilateral and unilateral constraint information.
    */
   private void updateConstraints () {
      for (BodyConnector bcon : myConnectors) {
         bcon.updateConstraints (/*time*/0, /*flags*/0);
      }
      myGT = new SparseBlockMatrix ();
      myGsize = getBilateralConstraints (myGT, null);
      myNT = new SparseBlockMatrix ();
      myNsize = getUnilateralConstraints (myNT, null);
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
   private int[] updateSolveIndexMap (MechModel mech) {
      // calling numActiveComponents ensures solve indices are updated
      int numc = mech.numActiveComponents() + mech.numParametricComponents();
      if (mySolveIndexMap != null) {
         // check if solve index map is still valid
         for (BodyInfo binfo : myBodyInfo) {
            if (binfo.mySolveIndex != binfo.myBody.getSolveIndex()) {
               // not valid
               mySolveIndexMap = null;
               break;
            }
         }
      }
      if (mySolveIndexMap == null) {
         int[] map = new int[numc];
         for (int i=0; i<numc; i++) {
            map[i] = -1;
         }
         int k = 0;
         for (BodyInfo binfo : myBodyInfo) {
            int solveIndex = binfo.myBody.getSolveIndex();
            map[solveIndex] = k;
            binfo.mySolveIndex = solveIndex;
            k++;
         }
         mySolveIndexMap = map;
      }
      return mySolveIndexMap; 
   }

   /**
    * Convenience method to get the path name for a component.
    */
   private String pathName (ModelComponent comp) {
      return ComponentUtils.getPathName (comp);
   }

   /**
    * Collects the current body position state into the vector {@code q}.
    */
   private void getPosState (VectorNd q) {
      double[] qbuf = q.getBuffer();
      q.setSize (7*numBodies());
      int idx = 0;
      for (BodyInfo binfo : myBodyInfo) {
         idx = binfo.myBody.getPosState (qbuf, idx);
      }
   }

   /**
    * Sets the body position state from the vector {@code q}.
    */
   private void setPosState (VectorNd q) {
      double[] qbuf = q.getBuffer();
      int idx = 0;
      for (BodyInfo binfo : myBodyInfo) {
         idx = binfo.myBody.setPosState (qbuf, idx);
         for (MarkerInfo minfo : binfo.myMarkerInfo) {
            minfo.myMarker.updatePosState();
         }
      }
   }

   /**
    * Changes the body position state stored in {@code q} by applying a
    * differential increment {@code dq} multiplied by a step size {@code
    * h}. Note that {@code q} and {@code dq} have different sizes, since
    * rotations are stored as quaternions in the former while angular
    * velocities are 3-vectors in the latter.
    */
   private void addPosImpulse (VectorNd q, VectorNd dq, double h) {
      double[] qbuf = q.getBuffer();
      double[] dbuf = dq.getBuffer();
      int qidx = 0;
      int didx = 0;
      for (BodyInfo binfo : myBodyInfo) {
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
   private void updatePosState (VectorNd dq) {
      VectorNd q = new VectorNd (7*numBodies());
      getPosState (q);
      addPosImpulse (q, dq, 1.0);
      setPosState (q);
   }

   /**
    * Returns the bodies associated with this IKSolver.
    *
    * @return bodies associated with this solver
    */
   public ArrayList<RigidBody> getBodies() {
      ArrayList<RigidBody> bodies = new ArrayList<>();
      updateBodiesAndConnectors();
      for (BodyInfo binfo : myBodyInfo) {
         bodies.add (binfo.myBody);
      }
      return bodies;
   }

   /**
    * Queries the number of rigid bodies associated with this solver.
    *
    * @return number of bodies associated with this solver
    */
   public int numBodies() {
      updateBodiesAndConnectors();
      return myBodyInfo.size();
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
    * Returns the markers used by this IKSolver.
    *
    * @return markers used by this solver
    */
   public ArrayList<FrameMarker> getMarkers() {
      ArrayList<FrameMarker> markers = new ArrayList<>();
      for (MarkerInfo minfo : myMarkerInfo) {
         markers.add (minfo.myMarker);
      }
      return markers;
   }

   /**
    * Returns the number of markers used by this IKSolver.
    *
    * @return number of markers used by this solver
    */
   public int numMarkers() {
      return myNumMarkers;
   }

   /**
    * Returns the marker weights used by this IKSolver.
    *
    * @return marker weights used by this solver
    */
   public VectorNd getMarkerWeights() {
      VectorNd weights = new VectorNd(numMarkers());
      int i = 0;
      for (MarkerInfo minfo : myMarkerInfo) {
         weights.set (i++, minfo.myWeight);
      }
      return weights;
   }

   /**
    * Sets the marker weights used by this IKSolver.
    *
    * @param weights new marker weights
    */
   public void setMarkerWeights (VectorNd weights) {
      if (weights.size() < numMarkers()) {
         throw new IllegalArgumentException (
            "weights size "+weights.size()+
            " incompatible with number of markers "+numMarkers());
      }
      int i = 0;
      for (MarkerInfo minfo : myMarkerInfo) {
         minfo.myWeight = weights.get (i++);
      }
      double avgWeight = weights.sum()/weights.size();
      if (myBodyInfo != null) {
         for (BodyInfo binfo : myBodyInfo) {
            binfo.updateJTJMatrix(avgWeight);
         }
      }
   }

   /**
    * Returns the MechModel associated with this IKSolver.
    *
    * @return MechModel associated with this solver
    */
   public MechModel getMechModel() {
      return myMech;
   }

   /**
    * Returns the connectors associated with this IKSolver.
    *
    * @return connectors associated with this solver
    */
   public ArrayList<BodyConnector> getConnectors() {
      ArrayList<BodyConnector> connectors = new ArrayList<>();
      updateBodiesAndConnectors();
      connectors.addAll (myConnectors);
      return connectors;
   }

   private double sqr (double x) {
      return x*x;
   }

   /**
    * Updates the positions of the bodies associated with this solver to bring
    * the markers as close as possible to the target positions specified by
    * {@code mtargs}.
    *
    * @param mtargs target positions for each marker. Should have a size {@code
    * >= 3*numMarkers()}.
    */
   public int solve (VectorNd mtargs) {
      updateBodiesAndConnectors();
      if (mtargs.size() < 3*numMarkers()) {
         throw new IllegalStateException (
            "Marker target size is "+mtargs.size() + 
            "; expected " + 3*numMarkers());
      }
      VectorNd disps = new VectorNd (mtargs.size());
      int velSize = myTotalVelSize;
      VectorNd dq = new VectorNd (velSize);
      myBd.setSize (velSize);
      int icnt = 0;
      boolean converged = false;
      double prevChiSqr = computeDisplacements (disps, mtargs);
      //System.out.println ("solve");
      do {
         updateSolveMatrix (mySolveMatrix, myBd, mtargs, disps);
         updateConstraints ();

         if (myAnalyze) {
            int matrixType =
               myUseJDerivative ? Matrix.INDEFINITE : Matrix.SYMMETRIC;
            myKKTSolver.analyze (
               mySolveMatrix, velSize, myGT, myRg, matrixType);
            myAnalyze = false;
         }
         myKKTSolver.factor (mySolveMatrix, velSize, myGT, myRg, myNT, myRn);
         myKKTSolver.solve (dq, myLam, myThe, myBd, myBg, myBn);     
         updatePosState (dq);
         double chiSqr = computeDisplacements (disps, mtargs);
         
         // check if dq is within convergence tolerance
         double sum = 0;
         for (int i=0; i<velSize; i++) {
            if ((i/3)%2 == 0) {
               // weight translations for model size
               sum += sqr(dq.get(i)*myModelSize);
            }
            else {
               sum += sqr(dq.get(i));
            }
         }
         double dqnorm = Math.sqrt(sum/velSize);
         if (dqnorm <= myConvergenceTol) {
            converged = true;
         }
         // System.out.printf (
         //    " i=%d dqnorm=%g delf=%g chiSqr=%g %s\n",
         //    icnt, dqnorm, (chiSqr-prevChiSqr)/sqr(myModelSize), chiSqr,
         //    (chiSqr > prevChiSqr ? "+" : " "));


         // converged = true;
         // for (int i=0; i<velSize; i++) {
         //    double tol = myConvergenceTol;
         //    if ((i/3)%2 == 0) {
         //       // scale tolerance for translations
         //       tol *= myModelSize;
         //    }
         //    if (Math.abs(dq.get(i)) > tol) {
         //       converged = false;
         //       break;
         //    }
         // }
         prevChiSqr = chiSqr;
         icnt++;
      }
      while (!converged && icnt < myMaxIterations);
      myNumIterations += icnt;
      myNumSolves++;
      return converged ? icnt : -1;
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
    * Return the cummulative number of solve iterations.
    */
   public int numIterations() {
      return myNumIterations;
   }

   /**
    * Return the cummulative number of solves.
    */
   public int numSolves() {
      return myNumSolves;
   }

   /**
    * Return the average number of iterations per solve
    */
   public double avgNumIterations() {
      if (myNumSolves == 0) {
         return 0;
      }
      else {
         return myNumIterations/(double)myNumSolves;
      }
   }

   /**
    * Clears the cummulative solve and iteration counts.
    */
   public void clearSolveCounts() {
      myNumIterations = 0;
      myNumSolves = 0;
   }

   // I/O methods used to implement PostScannable

   /**
    * {@inheritDoc}
    */
   public boolean isWritable() {
      return true;
   }

   /**
    * {@inheritDoc}
    */
   public void write (PrintWriter pw, NumberFormat fmt, Object ref)
      throws IOException {
      CompositeComponent ancestor = ComponentUtils.castRefToAncestor(ref);
      IndentingPrintWriter.printOpening (pw, "[ ");
      IndentingPrintWriter.addIndentation (pw, 2);
      pw.println (
         "mechModel="+ComponentUtils.getWritePathName (ancestor,myMech));
      pw.print ("markers=");
      pw.println ("[");
      IndentingPrintWriter.addIndentation (pw, 2);
      for (MarkerInfo minfo : myMarkerInfo) {
         pw.println (
            ComponentUtils.getWritePathName (ancestor, minfo.myMarker)+" "+
            minfo.myWeight);
      }
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
      if (myModelSize != -1) {
         pw.println ("modelSize=" + fmt.format(myModelSize));
      }
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
   }

   /**
    * {@inheritDoc}
    */
   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      Deque<ScanToken> tokens = (Deque<ScanToken>)ref;
      if (tokens == null) {
         tokens = new ArrayDeque<> ();
      }
      myModelSize = -1;
      rtok.scanToken ('[');
      tokens.offer (ScanToken.BEGIN);
      while (rtok.nextToken() != ']') {
         if (ScanWriteUtils.scanAndStoreReference (rtok, "mechModel", tokens)) {
         }
         else if (ScanWriteUtils.scanAttributeName (rtok, "markers")) {
            tokens.offer (new StringToken ("markers", rtok.lineno()));
            myNumMarkers = 
               ScanWriteUtils.scanComponentsAndWeights (rtok, tokens);
         }
         else if (ScanWriteUtils.scanAttributeName (rtok, "modelSize")) {
            myModelSize = rtok.scanNumber();
         }
         else {
            throw new IOException (
               "Error scanning " + getClass().getName() +
               ": unexpected token: " + rtok);
         }
      }
      tokens.offer (ScanToken.END); // terminator token
   }

   /**
    * {@inheritDoc}
    */
   public void postscan (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      ScanWriteUtils.postscanBeginToken (tokens, this);
      while (tokens.peek() != ScanToken.END) {
         if (ScanWriteUtils.postscanAttributeName (tokens, "mechModel")) {
            myMech = ScanWriteUtils.postscanReference (
               tokens, MechModel.class, ancestor);
         }
         else if (ScanWriteUtils.postscanAttributeName (tokens, "markers")) {
            FrameMarker[] mkrs = ScanWriteUtils.postscanReferences (
            tokens, FrameMarker.class, ancestor);
            double[] weights = (double[])tokens.poll().value();
            myMarkerInfo.clear();
            for (int i=0; i<mkrs.length; i++) {
               myMarkerInfo.add (new MarkerInfo (mkrs[i], weights[i]));
            }
         }
         else {
            throw new IOException (
               "Unexpected token for IKSolver: " + tokens.poll());
         }
      }      
      tokens.poll(); // eat END token
   }

   /**
    * Build the J^T J matrix that is used for each rigid body in the solve.
    * This matrix has the same form as a spatial inertia formed from the
    * markers, treating each marker as a point mass, with the mass given by the
    * marker's weight.
    *
    * <p>If the number of points is less than three, or if all points are
    * colinear, then the matrix will be rank deficient. This is resolved by
    * adding regularization terms of size {@code regc} to the matrix in such as
    * way as to leave the orginal range space unchanged.
    *
    * @param pnts marker point locations, in body coordinates. There must be at
    * least 1 point.
    * @param wgts marker point weights
    * @param regc size of the regularization term to add if the matrix is rank
    * deficient
    * @param rank if non-null, returns the original rank of the
    * matrix before any regularization
    * @return J^T J matrix, regularized if necessary
    */
   static protected SpatialInertia buildJTJMatrix (
      ArrayList<Vector3d> pnts, VectorNd wgts, double regc, IntHolder rank) {
      SpatialInertia S = new SpatialInertia();
      int i=0; 
      for (Vector3d p : pnts) {
         S.addPointMass (wgts.get(i++), p);
      }

      RigidTransform3d T = new RigidTransform3d();
      T.p.set (S.getCenterOfMass());
      SpatialInertia Sreg = new SpatialInertia (S);
      Sreg.inverseTransform (T);
      SymmetricMatrix3d Ri = new SymmetricMatrix3d();
      Sreg.getRotationalInertia (Ri);
      
      Matrix3d U = new Matrix3d();

      SVDecomposition3d svd3 = new SVDecomposition3d (U, null, Ri);
      int r = svd3.rank (1e-8*S.getMass());
      if (r < 3) {
         Vector3d eig = new Vector3d();
         svd3.getS (eig);
         double c = regc*S.getMass();
         switch (r) {
            case 0: {
               eig.set (c, c, c);
               break;
            }
            case 1: {
               // parnoid - shouldn't happen
               eig.set (1, c);
               eig.set (2, c);
               break;
            }
            case 2: {
               eig.set (2, c);
               break;
            }
         }
         Ri.setDiagonal (eig);
         Ri.mulLeftAndTransposeRight (U);
         Sreg.setRotationalInertia (Ri);         
         Sreg.transform (T);
         S = Sreg;
      }
      if (rank != null) {
         // 
         rank.value = r+3;
      }
      return S;
   }

   /* --- methods for creating probes with IK solutions --- */

   /**
    * Collects numeric data from the system and stores it in an input probe
    * for a particular time.
    */
   private abstract class DataCollector {
      VectorNd myVec;
      
      public abstract NumericListKnot addData (
         NumericInputProbe probe, double t, NumericListKnot last);
   }

   /**
    * Collects marker positions and stores them an input probe.
    */
   private class MarkerPositionCollector extends DataCollector {
      ArrayList<FrameMarker> myMarkers;

      MarkerPositionCollector (ArrayList<FrameMarker> markers) {
         myMarkers = markers;
         myVec = new VectorNd(3*markers.size());
      }

      public NumericListKnot addData (
         NumericInputProbe probe, double t, NumericListKnot last) {
         int k = 0;
         for (FrameMarker mkr : myMarkers) {
            myVec.setSubVector (3*k, mkr.getPosition());
            k++;
         }
         return probe.addData (t, myVec);
      }
   }

   /**
    * Collects body positions and stores them an input probe, using a specified
    * rotation representation.
    */
   private class BodyPoseCollector extends DataCollector {
      ArrayList<RigidBody> myBodies;
      RotationRep myRotRep;
      int myPosSize;

      BodyPoseCollector (ArrayList<RigidBody> bodies, RotationRep rotRep) {
         myBodies = bodies;
         myPosSize = 3+rotRep.size();
         myVec = new VectorNd(myPosSize*bodies.size());
         myRotRep = rotRep;
      }

      public NumericListKnot addData (
         NumericInputProbe probe, double t, NumericListKnot last) {
         int k = 0;
         double[] vbuf = myVec.getBuffer();
         double[] rbuf = (last != null ? last.v.getBuffer() : null);
         for (RigidBody body : myBodies) {
            myVec.setSubVector (myPosSize*k, body.getPosition());
            Quaternion quat = body.getRotation();
            quat.get (vbuf, rbuf, myPosSize*k+3, myRotRep, /*scale*/1.0);
            k++;
         }
         return probe.addData (t, myVec);
      }
   }

   /**
    * Creates an input probe for controlling the poses of the bodies associated
    * with this solver. The data is generated by performing an inverse
    * kinematic solve on marker data stored in a specified probe data file.
    *
    * <p> The vector size of the data in this file must be {@code 3 *
    * numMarkers()}. The start and stop times and scale of the created probe is
    * also determined from the data file. Knots are added to the probe at a
    * time interval specified by {@code interval}; if this is given as {@code
    * -1}, then the knot times are the same as those in the data file.
    *
    * <p>The state of the MechModel associated with this solver is saved before
    * the solve and restored afterward.
    *
    * @param name if non-{@code null}, specifies the probe's name
    * @param targDataFilePath path name of the probe data file containing
    * the target marker positions
    * @param rotRep rotation representation for the body orientations
    * @param interval knot time spacing interval, or {@code -1} if knot
    * times should be determined from the data file
    * @return the created input probe
    * @throws IOException if an I/O error occurs reading the data file
    */
   public PositionInputProbe createBodyPoseProbe (
      String name, String targDataFilePath, RotationRep rotRep, double interval)
      throws IOException {

      PositionInputProbe targProbe = new PositionInputProbe (
         null, getMarkers(), /*rotRep*/null, targDataFilePath);

      return createBodyPoseProbe (name, targProbe, rotRep, interval);
   }

   /**
    * Creates an input probe for controlling the poses of the bodies associated
    * with this solver. The data is generated by performing an inverse
    * kinematic solve on marker target data containing in a probe {@code
    * targProbe}.
    *
    * <p> The vector size of the data in {@code mkrdata} must be {@code 3 *
    * numMarkers()}. The start and stop times and scale of the created probe is
    * also determined from {@code mkrdata}. Knots are added to the probe at a
    * time interval specified by {@code interval}; if this is given as {@code
    * -1}, then the knot times are the same as those in {@code mkrdata}.
    *
    * <p>The state of the MechModel associated with this solver is saved before
    * the solve and restored afterward.
    *
    * @param name if non-{@code null}, specifies the probe's name
    * @param targProbe probe containing target position data for the markers
    * @param rotRep rotation representation for the body orientations
    * @param interval knot time spacing interval, or {@code -1} if knot
    * times should be determined from {@code mrkdata}
    * @return the created input probe
    */
   public PositionInputProbe createBodyPoseProbe (
      String name, NumericProbeBase targProbe,
      RotationRep rotRep, double interval) {

      if (targProbe.getVsize() != 3*numMarkers()) {
         throw new IllegalArgumentException (
            "'targProbe' vector size "+targProbe.getVsize() + " != " +
            (3*numMarkers()) + " required for " + numMarkers() + " markers");
      }

      double startTime = targProbe.getStartTime();
      double stopTime = targProbe.getStopTime();
      double scale = targProbe.getScale();

      ArrayList<RigidBody> bodies = getBodies();
      PositionInputProbe newProbe = new PositionInputProbe (
         name, bodies, rotRep, startTime, stopTime);
      newProbe.setScale (scale);
      solveForProbeData (
         newProbe, targProbe, new BodyPoseCollector(bodies, rotRep), interval);
      return newProbe;
   }

   /**
    * Creates an input probe for controlling the positions of the markers
    * associated with this solver. The data is generated by performing an
    * inverse kinematic solve on marker data stored in a specified probe data
    * file. Effectively, this method takes marker data stored in the data file
    * and filters it for kinematic feasibility.
    *
    * <p>The vector size of the data in this file must be {@code 3 *
    * numMarkers()}. The start and stop times and scale of the created probe is
    * also determined from the data file. Knots are added to the probe at a
    * time interval specified by {@code interval}; if this is given as {@code
    * -1}, then the knot times are the same as those in the data file.
    *
    * <p>The state of the MechModel associated with this solver is saved before
    * the solve and restored afterward.
    *
    * @param name if non-{@code null}, specifies the probe's name
    * @param targDataFilePath path name of the probe data file containing
    * the target marker positions
    * @param interval knot time spacing interval, or {@code -1} if knot
    * times should be determined from the data file.
    * @return the created input probe
    * @throws IOException if an I/O error occurs reading the data file
    */
   public PositionInputProbe createMarkerPositionProbe (
      String name, String targDataFilePath, double interval)
      throws IOException {

      PositionInputProbe targProbe = new PositionInputProbe (
         null, getMarkers(), /*rotRep*/null, targDataFilePath);

      return createMarkerPositionProbe (name, targProbe, interval);
   }

   /**
    * Creates an input probe for controlling the positions of the markers
    * associated with this solver. The data is generated by performing an
    * inverse kinematic solve on marker target data containing in a probe
    * {@code targProbe}.  Effectively, this method takes marker data stored in
    * {@code targProbe} and filters it for kinematic feasibility.
    *
    * <p> The vector size of the data in {@code mkrdata} must be {@code 3 *
    * numMarkers()}. The start and stop times and scale of the created probe is
    * also determined from {@code mkrdata}. Knots are added to the probe at a
    * time interval specified by {@code interval}; if this is given as {@code
    * -1}, then the knot times are the same as those in {@code mkrdata}.
    *
    * <p>The state of the MechModel associated with this solver is saved before
    * the solve and restored afterward.
    *
    * @param name if non-{@code null}, specifies the probe's name
    * @param targProbe probe containing target position data for the markers
    * @param interval knot time spacing interval, or {@code -1} if knot
    * times should be determined from {@code mrkdata}
    * @return the created input probe
    */
   public PositionInputProbe createMarkerPositionProbe (
      String name, NumericProbeBase targProbe, double interval) {

      if (targProbe.getVsize() != 3*numMarkers()) {
         throw new IllegalArgumentException (
            "'targProbe' vector size "+targProbe.getVsize() + " != " +
            (3*numMarkers()) + " required for " + numMarkers() + " markers");
      }

      double startTime = targProbe.getStartTime();
      double stopTime = targProbe.getStopTime();
      double scale = targProbe.getScale();

      ArrayList<FrameMarker> mkrs = getMarkers();
      PositionInputProbe newProbe = new PositionInputProbe (
         name, mkrs, /*rotRep*/null, startTime, stopTime);
      newProbe.setScale (scale);
      solveForProbeData (
         newProbe, targProbe, new MarkerPositionCollector(mkrs), interval);
      return newProbe;
   }

   void solveForProbeData (
      NumericInputProbe newProbe, NumericProbeBase targProbe,
      DataCollector collector, double interval) {

      VectorNd mpos = new VectorNd (newProbe.getVsize());
      FunctionTimer timer = new FunctionTimer();
      timer.start();
      saveModelState();
      NumericList nlist = targProbe.getNumericList();
      NumericListKnot last = null;
      if (interval == -1) {
         NumericListKnot knot;
         for (knot=nlist.getFirst(); knot!=null; knot=knot.getNext()) {
            solve (knot.v);
            last = collector.addData (newProbe, knot.t, last);
         }
      }
      else {
         double startTime = targProbe.getStartTime();
         double stopTime = targProbe.getStopTime();
         double scale = targProbe.getScale();


         double tend = (stopTime-startTime)/scale;
         double tinc = interval/scale;
         double ttol = 100*DOUBLE_PREC*(stopTime-startTime);

         double tloc = 0;
         while (tloc <= tend) {
            nlist.interpolate (mpos, tloc);
            solve (mpos);
            last = collector.addData (newProbe, tloc, last);
            // make sure we include the end point while avoiding very small
            // knot spacings
            if (tloc < tend && tloc+tinc > tend-ttol) {
               tloc = tend;
            }
            else {
               tloc += tinc;
            }
         }
      }
      restoreModelState();
      timer.stop();
      System.out.println ("IK probe created in "+timer.result(1));
   }
}
