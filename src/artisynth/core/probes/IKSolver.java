package artisynth.core.probes;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

import artisynth.core.mechmodels.BodyConnector;
import artisynth.core.mechmodels.BodyConstrainer;
import artisynth.core.mechmodels.Constrainer;
import artisynth.core.mechmodels.DynamicComponent;
import artisynth.core.mechmodels.Frame;
import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.MechSystem;
import artisynth.core.mechmodels.MechSystem.ConstraintInfo;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.StaticRigidBodySolver;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.NumericState;
import artisynth.core.modelbase.PostScannable;
import artisynth.core.modelbase.ScanWriteUtils;
import artisynth.core.util.ScanToken;
import artisynth.core.util.StringToken;
import maspack.function.Function1x1;
import maspack.function.FunctionValuePair;
import maspack.interpolation.NumericList;
import maspack.interpolation.NumericListKnot;
import maspack.matrix.CholeskyDecomposition;
import maspack.matrix.Matrix;
import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix6d;
import maspack.matrix.MatrixBlock;
import maspack.matrix.MatrixNd;
import maspack.matrix.Point3d;
import maspack.matrix.Quaternion;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.RotationRep;
import maspack.matrix.SVDecomposition3d;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.SparseBlockSignature;
import maspack.matrix.SparseNumberedBlockMatrix;
import maspack.matrix.SymmetricMatrix3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.matrix.VectorNi;
import maspack.numerics.BrentMinimizer;
import maspack.solvers.KKTSolver;
import maspack.spatialmotion.RigidBodyConstraint.MotionType;
import maspack.spatialmotion.SpatialInertia;
import maspack.spatialmotion.Twist;
import maspack.util.FunctionTimer;
import maspack.util.IndentingPrintWriter;
import maspack.util.IntHolder;
import maspack.util.InternalErrorException;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

/**
 * Performs inverse kinematics computations to compute rigid body positions
 * from marker data, while enforcing constraints between the bodies imposed by
 * joints and other body connectors. In multibody biomechanics, can be used to
 * filter marker data to determine feasible body trajectories, prior to inverse
 * dynamic calculations.
 *
 * Note: this class is still under development to improve the convergence rate
 * of its solve method.
 */
public class IKSolver extends StaticRigidBodySolver implements PostScannable {

   /**
    * Describes the search strategy used to help ensure convergence of
    * the constrained non-linear least squares problem.
    */
   public enum SearchStrategy {
      /**
       * No strategy - simply do a (constrained) Newton solve to try and bring
       * the gradient to zero at each step.
       */
      NONE,

      /**
       * If a Newton step does not decrease the least-squares energy
       * sufficiently, use an approximate line search to backtrack.
       */
      BACKTRACK,

      /**
       * Change step direction and size using Levenberg Marquardt
       * stabilization.
       */
      LEVENBERG_MARQUARDT,
   };

   /**
    * Information about the rigid bodies associated with this solve
    */
   protected class BodyMarkerInfo {
      RigidBody myBody;
      ArrayList<MarkerInfo> myMarkerInfo; // markers (if any) attached to the body

      // marker stiffness matrix in body coordinates. Mathematically equivalent
      // to a point-mass based inertia:
      SpatialInertia myJTWJ; 

      // Cholesy decomposition of JTWJ - needed for the (currently unsued)
      // factorAndSolve() method:
      CholeskyDecomposition myChol;

      BodyMarkerInfo (RigidBody body, ArrayList<MarkerInfo> minfos) {
         myBody = body;
         if (minfos == null) {
            myMarkerInfo = new ArrayList<>();
         }
         else {
            myMarkerInfo = minfos;
         }
      }

      void updateJTWJMatrix (double avgWeight) {
         double massRegularization = avgWeight*myMassRegularization;
         if (myMarkerInfo.size() == 0) {
            myJTWJ = new SpatialInertia();
            double s = massRegularization;
            myBody.getInertia (myJTWJ);
            if (myJTWJ.getMass() == 0) {
               myJTWJ.set (/*mass*/s, /*Jxx*/s, /*Jyy*/s, /*Jzz*/s);
            }
            else {
               myJTWJ.scale (s/myJTWJ.getMass());
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
            myJTWJ = IKSolver.buildJTWJMatrix (
               pnts, wgts, massRegularization, /*rank*/null);
         }
         myChol = null; // refactor on demand if necessary
      }

      /**
       * Solve the JTWJ matrix using its Cholesky decomposition.  Only needed by
       * the factorAndSolve() method, which is currently unused.
       */
      public void solve (VectorNd r, VectorNd b) {
         RotationMatrix3d R = myBody.getPose().R;
         double tmpx, tmpy, tmpz;

         double[] rbuf = r.getBuffer();
         double[] bbuf = b.getBuffer();

         // rotate b into body frame:

         tmpx = bbuf[0]; tmpy = bbuf[1]; tmpz = bbuf[2];
         rbuf[0] = R.m00*tmpx + R.m10*tmpy + R.m20*tmpz;
         rbuf[1] = R.m01*tmpx + R.m11*tmpy + R.m21*tmpz;
         rbuf[2] = R.m02*tmpx + R.m12*tmpy + R.m22*tmpz;

         tmpx = bbuf[3]; tmpy = bbuf[4]; tmpz = bbuf[5];
         rbuf[3] = R.m00*tmpx + R.m10*tmpy + R.m20*tmpz;
         rbuf[4] = R.m01*tmpx + R.m11*tmpy + R.m21*tmpz;
         rbuf[5] = R.m02*tmpx + R.m12*tmpy + R.m22*tmpz;
      
         if (myChol == null) {
            myChol = new CholeskyDecomposition(myJTWJ);
         }
         myChol.solve (r, r);

         // rotate r back into world frame:

         tmpx = rbuf[0]; tmpy = rbuf[1]; tmpz = rbuf[2];
         rbuf[0] = R.m00*tmpx + R.m01*tmpy + R.m02*tmpz;
         rbuf[1] = R.m10*tmpx + R.m11*tmpy + R.m12*tmpz;
         rbuf[2] = R.m20*tmpx + R.m21*tmpy + R.m22*tmpz;

         tmpx = rbuf[3]; tmpy = rbuf[4]; tmpz = rbuf[5];
         rbuf[3] = R.m00*tmpx + R.m01*tmpy + R.m02*tmpz;
         rbuf[4] = R.m10*tmpx + R.m11*tmpy + R.m12*tmpz;
         rbuf[5] = R.m20*tmpx + R.m21*tmpy + R.m22*tmpz;
      }
   }

   /**
    * Evaluates marker displacement energy at a point s on an interval [0, 1]
    * parameterizing the set of positions q and q + dq; i.e., between an
    * initial position q and a final position defined by adding a position
    * increment dq to q.
    */
   protected class LineEnergy implements Function1x1 {

      VectorNd myQ;          // initial position
      VectorNd myDq;         // position increment
      VectorNd myMtargs;     // marker target positions
      int myCnt;
      double myDqNorm;

      LineEnergy (VectorNd q, VectorNd dq, VectorNd mtargs) {
         myQ = q;
         myDq = dq;
         myMtargs = mtargs;
         myDqNorm = computeDqNorm (dq);
      }

      public double eval (double s) {
         myCnt++;
         updateAndProjectPosState (null, myQ, myDq, s);
         return computeEnergy (myMtargs);
      }
   }

   /**
    * Evaluates a merit function at a point s on an interval [0, 1]
    * parameterizing the set of positions q and q + dq. This function is the
    * sum of the marker displacement energy and the constraint penalty.
    */
   protected class LineMerit implements Function1x1 {

      VectorNd myQ;          // initial position
      VectorNd myDq;         // position increment
      VectorNd myMtargs;     // marker target positions
      int myCnt;

      LineMerit (VectorNd q, VectorNd dq, VectorNd mtargs) {
         myQ = q;
         myDq = dq;
         myMtargs = mtargs;
      }

      public double eval (double s) {
         myCnt++;
         updatePosState (myQ, myDq, s);
         updateConstraintInfo();
         return computeEnergy (myMtargs) + computeCPenalty();
      }
   }

   /**
    * Information about a marker and its weight
    */
   protected class MarkerInfo {
      FrameMarker myMarker;
      double myWeight;
      int myIdx; // index within the original list of markers

      MarkerInfo (FrameMarker mkr, double w, int idx) {
         myMarker = mkr;
         myWeight = w;
         myIdx = idx;
      }
   }

   /* --- parameters and attributes specific to IK solves --- */

   private static final double LM_DAMPING_MIN = 0.1;
   private static final double LM_DAMPING_MAX = 100;

   // gain factor for the constraint penalty function
   private static final double CPENALTY_GAIN = 100;

   // If true, use the derivative of J in constructing the main mass block.
   // Results in a non-symmetric solve matrix. Set to false by default since we
   // have not yet seen that this improves convergence and sometimes makes the
   // simulation unstable.
   private static final boolean myUseJDerivative = false;

   private static final boolean myScaleLmDamping = true;

   // If true, add a second order term to the displacement computed in the
   // Levenberg Marquardt search strategy. As currently implemented, this does
   // not appear to work and actually increases the required number of
   // interations.
   private static final boolean myUseLMAcceleration = false;

   public boolean debug = false; // turns on debugging messages
   public boolean debugMask = false; // masks debug messages internally

   // computation control properties

   // static final boolean DEFAULT_FIX_GROUNDED_BODIES = true;
   // boolean myFixGroundedBodies = DEFAULT_FIX_GROUNDED_BODIES;

   static final double DEFAULT_MASS_REGULARIZATION = 0.001;
   double myMassRegularization = DEFAULT_MASS_REGULARIZATION;

   static final int DEFAULT_MAX_ITERATIONS = 30;
   int myMaxIterations = DEFAULT_MAX_ITERATIONS;

   static final double DEFAULT_CONVERGENCE_TOL = 1e-6;
   double myConvergenceTol = DEFAULT_CONVERGENCE_TOL;
   
   protected static final SearchStrategy DEFAULT_SEARCH_STRATEGY = 
      //SearchStrategy.BACKTRACK;
      //SearchStrategy.NONE;
      SearchStrategy.LEVENBERG_MARQUARDT;
   private SearchStrategy mySearchStrategy = DEFAULT_SEARCH_STRATEGY;

   // damping factor to resist changes in body positions. Currently unused.
   static final double DEFAULT_DAMPING = 0.0;
   double myDamping = DEFAULT_DAMPING;

   // markers to be tracked, together with their weights
   ArrayList<BodyMarkerInfo> myBodyMarkerInfo;  
   ArrayList<MarkerInfo> myMarkerInfo;  
   int myNumMarkers; // store separately in case this is needed during scan

   static int myTotalNumIterations;

   int myNumIterations;           // cummulative iteration count
   int myNumSolves;               // cummulative solve count

   VectorNd myF = new VectorNd(); // force vector
   VectorNd myFZero = new VectorNd(); // zero force vector (for pos correction)

   // Variables used by the solver
   VectorNd myQPrev = new VectorNd();  // previous body positions
   VectorNd myDq = new VectorNd();     // current step direction
   VectorNd myDqPrev = new VectorNd(); // previos step direction
   VectorNd myDisps = new VectorNd();  // composite displacement vector (x_targ-x
   double myEnergyPrev;                // previous energy value
   double myCPenaltyPrev;      // previous constraint energy value
   double myDqnormPrev;                // norm of myDqPrev

   double myLmDamping;                 // Levenberg Marquardt damping paramater

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
      int idx = 0;
      for (FrameMarker mkr : mkrs) {
         myMarkerInfo.add (new MarkerInfo (mkr, weights.get(idx), idx));
         idx++;
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

   // /**
   //  * Queries whether grounded bodies are fixed.
   //  *
   //  * @see #setFixGroundedBodies
   //  * @return {@code true} if grounded bodies are fixed
   //  */
   // public boolean getFixGroundedBodies() {
   //    return myFixGroundedBodies;
   // }

   // /**
   //  * Set whether grounded bodies should be fixed. If {@code true}, then the
   //  * solver will not change the pose of any body which is not attached to any
   //  * marker and whose '{@code grounded}' property is set to {@code true}.
   //  *
   //  * @see #getFixGroundedBodies
   //  * @param enable if {@code true}, grounded bodies are fixed
   //  */
   // public void setFixGroundedBodies (boolean enable) {
   //    myFixGroundedBodies = enable;
   // }

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
      if (myMassRegularization != c) {
         myMassRegularization = c;
         updateJTWJMatrices();
      }
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
    * Queries the least-squares search strategy for each solve step.
    * 
    * #see #setSearchStrategy
    * @return search strategy
    */
   public SearchStrategy getSearchStrategy() {
      return mySearchStrategy;
   }

   /**
    * Sets the least-squares search strategy for each solve step.
    * 
    * #see #getSearchStrategy
    * @param strat search strategy
    */
   public void setSearchStrategy (SearchStrategy strat) {
      mySearchStrategy = strat;
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
    * Override setPosState to update marker positions.
    */
   protected void setPosState (VectorNd q) {
      super.setPosState (q);
      for (BodyInfo binfo : mySortedBodyInfo) {
         BodyMarkerInfo bminfo = myBodyMarkerInfo.get(binfo.myIndex);
         for (MarkerInfo minfo : bminfo.myMarkerInfo) {
            minfo.myMarker.updatePosState();
         }
      }
   }

   /**
    * Finds body and connector information.
    */
   protected void findBodiesAndConnectors () {
      LinkedHashMap<RigidBody,ArrayList<MarkerInfo>> bodyMarkerMap =
         new LinkedHashMap<>();

      if (myMech == null) {
         throw new IllegalStateException (
            "Solver does not reference a MechModel");
      }
      double avgWeight = 0;
      for (MarkerInfo minfo : myMarkerInfo) {
         FrameMarker mkr = minfo.myMarker;
         String name = mkr.getName();
         if (name == null) {
            name = Integer.toString(minfo.myIdx);
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
      }
      avgWeight /= myMarkerInfo.size();
      // find all connectors attached to the frames. Find additional bodies
      // that are attached via connectors. Stop the search at bodies whose
      // {@code grounded} property is {@code true}.
      LinkedHashSet<BodyConstrainer> constrainerSet = new LinkedHashSet<>();
      LinkedHashSet<RigidBody> bodySet = new LinkedHashSet<>();
      bodySet.addAll (bodyMarkerMap.keySet());
      findBodiesAndConstrainers (
         bodySet, constrainerSet, /*excludeGrounded*/true);

      myBodyMarkerInfo = new ArrayList<>();
      for (BodyInfo binfo : myBodyInfo) {
         RigidBody body = binfo.myBody;
         BodyMarkerInfo minfo =
            new BodyMarkerInfo (body, bodyMarkerMap.get(body));
         // precompute the JTWJ matrix in body coordinates
         minfo.updateJTWJMatrix (avgWeight);
         myBodyMarkerInfo.add (minfo);
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
      double energy = 0;
      for (BodyMarkerInfo bminfo : myBodyMarkerInfo) {
         for (MarkerInfo minfo : bminfo.myMarkerInfo) {
            mtargs.getSubVector (3*minfo.myIdx, targ);
            disp.sub (targ, minfo.myMarker.getPosition());
            disps.setSubVector (3*minfo.myIdx, disp);
            energy += disp.dot(disp)*minfo.myWeight;
         }
      }
      return energy/2;
   }

   /**
    * Computes the right-hand side used to solve for a geodesic acceleration
    * vector that provides a second order correction to displacements computed
    * for the Levenbergy Marquardt strategy. At present, geodesic does not
    * appear to work.
    */
   private VectorNd computeLMAccelRhs (
      VectorNd qprev, VectorNd mtargs, double h) {

      VectorNd rhs = new VectorNd (myTotalVelSize);
      updatePosState (qprev, myDq, h);

      Vector3d locw = new Vector3d();
      Vector3d disp = new Vector3d(); 
      Vector3d dely = new Vector3d(); 

      Vector3d tmp = new Vector3d();
      Vector3d targ = new Vector3d();
      int bidx = 0; // body index
      Twist dq = new Twist();
      Twist cb = new Twist();
      for (BodyInfo binfo : mySortedBodyInfo) {
         BodyMarkerInfo bminfo = myBodyMarkerInfo.get(binfo.myIndex);
         RigidBody body = binfo.myBody;
         RotationMatrix3d R = body.getPose().R; // rotates body coords to world
         myDq.getSubVector (6*bidx, dq);
         cb.setZero();
         for (MarkerInfo minfo : bminfo.myMarkerInfo) {
            locw.transform (R, minfo.myMarker.getLocation());
            myDisps.getSubVector (3*minfo.myIdx, disp);
            mtargs.getSubVector (3*minfo.myIdx, targ);
            dely.sub (targ, minfo.myMarker.getPosition());
            dely.sub (disp);
            dely.scale (1/h);

            dely.add (dq.v);
            dely.crossAdd (dq.w, locw, dely);
            dely.scale (minfo.myWeight*2/h);
            cb.v.add (dely);
            cb.w.crossAdd (locw, dely, cb.w);
         }
         rhs.setSubVector (6*bidx, cb);     
         bidx++;
      }
      setPosState (qprev);
      return rhs;
   }

   /**
    * For testing only: computes the same result as {@code computeLMAccelRhs()}
    * but by alternate means.
    */
   private VectorNd computeLMAccelRhsChk (
      VectorNd qprev, VectorNd mtargs, double h) {

      VectorNd rhs = new VectorNd (myTotalVelSize);
      updatePosState (qprev, myDq, h);

      Vector3d locw = new Vector3d();
      Vector3d oldy = new Vector3d(); 
      Vector3d newy = new Vector3d(); 
      Vector3d dely = new Vector3d(); 
      VectorNd ddy = new VectorNd(); 
      Vector3d targ = new Vector3d();

      VectorNd dq = new VectorNd(6);
      VectorNd cb = new VectorNd(6);

      MatrixNd J = new MatrixNd (3, 6);
      Matrix3d XP = new Matrix3d();
      MatrixNd JT = new MatrixNd (6, 3);
      JT.set(0, 0, 1);
      JT.set(1, 1, 1);
      JT.set(2, 2, 1);
      int bidx = 0; // body index
      for (BodyInfo binfo : mySortedBodyInfo) {
         BodyMarkerInfo bminfo = myBodyMarkerInfo.get(binfo.myIndex);
         RigidBody body = binfo.myBody;
         RotationMatrix3d R = body.getPose().R; // rotates body coords to world
         myDq.getSubVector (6*bidx, dq);
         cb.setZero();
         for (MarkerInfo minfo : bminfo.myMarkerInfo) {
            locw.transform (R, minfo.myMarker.getLocation());
            XP.setSkewSymmetric (locw);
            JT.setSubMatrix (3, 0, XP);

            // compute dely
            mtargs.getSubVector (3*minfo.myIdx, targ);
            newy.sub (targ, minfo.myMarker.getPosition());
            myDisps.getSubVector (3*minfo.myIdx, oldy);
            dely.sub (newy, oldy);
            dely.scale (1/h);

            J.transpose (JT);
            J.mul (ddy, dq);
            ddy.add (new VectorNd(dely), ddy);
            ddy.scale (minfo.myWeight*2/h);
            
            JT.mulAdd (cb, ddy);
         }
         rhs.setSubVector (6*bidx, cb);     
         bidx++;
      }
      setPosState (qprev);
      return rhs;
   }

   /**
    * Computes the energy associated with current marker displacements.
    */
   private double computeEnergy (VectorNd mtargs) {
      
      Vector3d targ = new Vector3d();
      Vector3d disp = new Vector3d(); // required marker displacements
      double energy = 0;
      for (BodyMarkerInfo bminfo : myBodyMarkerInfo) {
         for (MarkerInfo minfo : bminfo.myMarkerInfo) {
            mtargs.getSubVector (3*minfo.myIdx, targ);
            disp.sub (targ, minfo.myMarker.getPosition());
            energy += disp.dot(disp)*minfo.myWeight;
         }
      }
      return energy/2;
   }

   /**
    * Computes an penalty associated with constraint violations.
    */
   private double computeCPenalty() {
      double sum = 0;
      for (int idx=0; idx<myGsize; idx++) {
         ConstraintInfo ginfo = myGInfo[idx];
         if (ginfo.motionType == MotionType.ROTARY) {
            sum += sqr(ginfo.dist*getModelSize());
         }
         else {
            sum += sqr(ginfo.dist);
         }
      }
      for (int idx=0; idx<myNsize; idx++) {
         ConstraintInfo ninfo = myNInfo[idx];
         if (ninfo.motionType == MotionType.ROTARY) {
            sum += sqr(ninfo.dist*getModelSize());
         }
         else {
            sum += sqr(ninfo.dist);
         }
      }
      return CPENALTY_GAIN*sum/2;
   }

   /**
    * Adds the second derivative term related to the time derivative of locw to
    * a body's JTWJ matrix.
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
    * Adds the Levenberg Marquardt scaling term to a specific block of the
    * Hessian.
    */
   private void addLMDampingTerm (SpatialInertia blk, double lambda) {
      if (myScaleLmDamping) {
         blk.m00 += lambda*blk.m00;
         blk.m11 += lambda*blk.m11;
         blk.m22 += lambda*blk.m22;
         blk.m33 += lambda*blk.m33;
         blk.m44 += lambda*blk.m44;
         blk.m55 += lambda*blk.m55;
      }
      else {
         blk.m00 += lambda;
         blk.m11 += lambda;
         blk.m22 += lambda;
         blk.m33 += lambda;
         blk.m44 += lambda;
         blk.m55 += lambda;
      }
   }

   /**
    * Updates the system solve matrix {@code S}. This overrides the default
    * behaviour to set each bodies 6 x 6 block to the stiffness matrix
    * associated with the energy minimization.
    */
   protected void updateSolveMatrix (SparseNumberedBlockMatrix S) {
      int bidx = 0; // body index
      for (BodyInfo binfo : mySortedBodyInfo) {
         BodyMarkerInfo bminfo = myBodyMarkerInfo.get(binfo.myIndex);
         RigidBody body = binfo.myBody;
         SpatialInertia blk = (SpatialInertia)S.getBlock (bidx, bidx);
         bminfo.myJTWJ.getRotated (blk, body.getPose().R);
         bidx++;
      }
   }

   /**
    * Updates the system solve matrix {@code S} and its right hand side {@code
    * b} prior to the next iteration step.
    */
   private void updateSolveMatrix (
      SparseNumberedBlockMatrix S, VectorNd b, VectorNd delDq,
      double lmDamping, VectorNd disps) {
      
      Vector3d locw = new Vector3d();
      Vector3d disp = new Vector3d(); // required marker displacements
      Twist cb = new Twist();
      int bidx = 0; // body index
      Matrix6d D = new Matrix6d();
      for (BodyInfo binfo : mySortedBodyInfo) {
         BodyMarkerInfo bminfo = myBodyMarkerInfo.get(binfo.myIndex);
         RigidBody body = binfo.myBody;
         SpatialInertia blk = (SpatialInertia)S.getBlock (bidx, bidx);
         RotationMatrix3d R = body.getPose().R; // rotates body coords to world
         bminfo.myJTWJ.getRotated (blk, R);
         if (myDamping != 0) {
            D.scale (myDamping, blk);
            blk.add (D);
         }
         if (lmDamping > 0) {
            addLMDampingTerm (blk, lmDamping);
         }
         cb.setZero();
         for (MarkerInfo minfo : bminfo.myMarkerInfo) {
            locw.transform (R, minfo.myMarker.getLocation());
            disps.getSubVector (3*minfo.myIdx, disp);
            disp.scale (minfo.myWeight);
            cb.v.add (disp);
            cb.w.crossAdd (locw, disp, cb.w);
            if (myUseJDerivative) {
               addHj (blk, locw, disp);
            }
         }
         if (myDamping != 0) {
            VectorNd vec = new VectorNd(6);
            Twist dforce = new Twist();
            delDq.getSubVector (6*bidx, vec);
            D.mul (vec, vec);
            dforce.set (vec);
            cb.sub (dforce);
         }
         b.setSubVector (6*bidx, cb);     
         bidx++;
      }
   }

   /**
    * Convenience method to get the path name for a component.
    */
   private String pathName (ModelComponent comp) {
      return ComponentUtils.getPathName (comp);
   }

   /**
    * Computes the difference between two body position states.
    */
   private VectorNd diffPosState (VectorNd q0, VectorNd q1) {
      VectorNd dq = new VectorNd (myTotalVelSize);
      RigidTransform3d TDIFF = new RigidTransform3d();
      Twist tw = new Twist();
      Frame frame0 = new Frame();
      Frame frame1 = new Frame();
      int idx = 0;
      for (int k=0; k<numBodies(); k++) {
         frame0.setPosState (q0.getBuffer(), idx);
         idx = frame1.setPosState (q1.getBuffer(), idx);
         TDIFF.mulInverseLeft (frame0.getPose(), frame1.getPose());
         tw.set (TDIFF);
         dq.setSubVector (k*6, tw);
      }
      return dq;
   }

   /**
    * Computes the difference between a specified position state and the
    * current position state.
    */
   private VectorNd diffPosState (VectorNd q0) {
      VectorNd q1 = new VectorNd();
      getPosState (q1);
      return diffPosState (q0, q1);
   }

   /**
    * Calls {@link #updatePosState} and then calls {@link
    * #projectToConstraints} to ensure constraint compliance.
    */
   private VectorNd updateAndProjectPosState (
      VectorNd dqProj, VectorNd q, VectorNd dq, double scale) {
      if (scale == 0) {
         setPosState (q);
         return null;
      }
      else {
         updatePosState (q, dq, scale);
         VectorNd ddq = projectToConstraints();
         if (dqProj != null) {
            dqProj.scaledAdd (scale, dq, ddq);
         }
         return ddq;
      }
   }

   /**
    * Returns the bodies associated with this IKSolver which have markers
    * attached to them.
    *
    * @return bodies associated with this solver
    */
   public ArrayList<RigidBody> getBodiesWithMarkers() {
      ArrayList<RigidBody> bodies = new ArrayList<>();
      updateBodiesAndConnectors();
      for (BodyInfo binfo : myBodyInfo) {
         BodyMarkerInfo bminfo = myBodyMarkerInfo.get(binfo.myIndex);
         if (bminfo.myMarkerInfo.size() > 0) {
            bodies.add (binfo.myBody);
         }
      }
      return bodies;
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
      for (MarkerInfo minfo : myMarkerInfo) {
         weights.set (minfo.myIdx, minfo.myWeight);
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
      for (MarkerInfo minfo : myMarkerInfo) {
         minfo.myWeight = weights.get (minfo.myIdx);
      }
      updateJTWJMatrices();
   }

   /**
    * Updates the Hessian matrices formed from J^T W J.
    */
   private void updateJTWJMatrices () {
      double sum = 0;
      for (MarkerInfo minfo : myMarkerInfo) {
         sum += minfo.myWeight;
      }
      double avgWeight = sum/numMarkers();
      if (myBodyInfo != null) {
         for (BodyMarkerInfo bminfo : myBodyMarkerInfo) {
            bminfo.updateJTWJMatrix(avgWeight);
         }
      }
   }

   /**
    * Compute a dot product for dq that is weighted model size
    */
   private double computeDqDot (VectorNd dq0, VectorNd dq1) {
      double dot = 0;
      double weight = sqr(1/getModelSize());
      for (int i=0; i<dq0.size(); i++) {
         double prod = dq0.get(i)*dq1.get(i);
         if ((i/3)%2 == 0) {
            // translation components - apply weighting
            prod *= weight;
         }
         dot += prod;
      }
      return dot;
   }

   /**
    * Compute the average 'cosine' between two dq vectors
    */
   private double computeDqCos (VectorNd dq0, VectorNd dq1) {
      double dot = computeDqDot (dq0, dq1);
      return dot/(computeDqNorm(dq0)*computeDqNorm(dq1));
   }

   /**
    * Compute the maximum angle deviation in dq.
    */
   private double computeDqAng (VectorNd dq) {
      double maxAng = 0;
      for (int i=0; i<dq.size(); i++) {
         if ((i/3)%2 != 0) {
            // angular data
            double ang = dq.get(i);
            if (ang > maxAng) {
               maxAng = ang;
            }
         }
      }
      return maxAng;
   }

   // /**
   //  * Performs a single linearized position update to project the current body
   //  * positions to the constraints.
   //  */
   // private VectorNd projectToConstraints () {
   //    int velSize = myTotalVelSize;

   //    VectorNd dq = new VectorNd (velSize);
   //    VectorNd q = new VectorNd (7*numBodies());
   //    getPosState (q);

   //    updateSolveMatrix (mySolveMatrix);
   //    updateConstraints ();
   //    myKKTSolver.factor (mySolveMatrix, velSize, myGT, myRg, myNT, myRn);
   //    myKKTSolver.solve (dq, myLam, myThe, myFZero, myBg, myBn);     
   //    updatePosState (q, dq, 1.0);
   //    return dq;
   // }

   /**
    * Used for debugging. Write the energy along a search direction out to a
    * file.
    */
   public void writeLineEnergy (String fileName, LineEnergy func) {
      try {
         PrintWriter pw = new PrintWriter (new FileWriter (fileName));
         for (int i=0; i<=100; i++) {
            double s = i/100.0;
            double e = func.eval(s);
            pw.printf ("%.10g %.10g\n", s, e);
         }
         pw.close();
      }
      catch (IOException e) {
         e.printStackTrace(); 
      }
   }

   /**
    * Increases the damping factor for the Levenberg Marquardt strategy.
    */
   private void increaseLMDamping (double s, double min, double max) {
      double lmd = myLmDamping;
      double lmdnew;
      if (lmd == 0) {
         lmdnew = min;
      }
      else {
         lmdnew = Math.min (max, s*lmd);
      }
      if (debug && lmdnew != lmd) {
         System.out.printf ("      LMD UP %g\n", lmdnew);
      }
      myLmDamping = lmdnew;
   }

   /**
    * Decreases the damping factor for the Levenberg Marquardt strategy.
    */
   private void decreaseLMDamping (double s, double min) {
      double lmd = myLmDamping;
      double lmdnew = s*lmd;
      if (lmdnew < min) {
         lmdnew = 0;
      }
      if (debug && lmdnew != lmd) {
         System.out.printf ("      LMD DOWN %g\n", lmdnew);
      }
      myLmDamping = lmdnew;
   }

   /**
    * Computes the Hessian and force vector and then does a constrained solve
    * to find the nominal step direction for the next solve iteration.
    */
   void updateAndSolveForStep (double lmDamping) {
      int velSize = myTotalVelSize;

      updateSolveMatrix (mySolveMatrix, myF, /*delDq*/null, lmDamping, myDisps);
      updateConstraints ();

      if (hasGTStructureChanged()) {
         myAnalyze = true;
      }
      if (myAnalyze) {
         int matrixType =
            myUseJDerivative ? Matrix.INDEFINITE : Matrix.SYMMETRIC;
         myKKTSolver.analyze (mySolveMatrix, velSize, myGT, myRg, matrixType);
         myAnalyze = false;
      }
      myKKTSolver.factor (mySolveMatrix, velSize, myGT, myRg, myNT, myRn);
      myKKTSolver.solve (myDq, myLam, myThe, myF, myBg, myBn); 
      VectorNd dq = new VectorNd (6);
      myDq.getSubVector (0, dq);

      // add constraint forces to myF. (Note though that these will be
      // orthogonal to myDq.)
      if (myGsize > 0) {
         myGT.mulAdd (myF, myLam, velSize, myGsize);
      }
      if (myNsize > 0) {
         myNT.mulAdd (myF, myThe, velSize, myNsize);
      }
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
      updateSolveIndexMap (myMech);
      
      if (mtargs.size() < 3*numMarkers()) {
         throw new IllegalStateException (
            "Marker target size is "+mtargs.size() + 
            "; expected " + 3*numMarkers());
      }
      myDisps.setSize (mtargs.size());
      int velSize = myTotalVelSize;
      myDq.setSize (velSize);
      myQPrev.setSize (7*numBodies());
      myDqPrev.setSize (velSize);
      myF.setSize (velSize);
      myFZero.setSize (velSize);
      int icnt = 0;
      boolean converged = false;
      double energy0 = computeDisplacements (myDisps, mtargs);
      myEnergyPrev = energy0;

      getPosState (myQPrev);
      if (debug) {
         System.out.println ("Begin solve");
      }
      do {
         switch (mySearchStrategy) {
            case NONE: {
               converged = basicSolveStep (icnt, mtargs);
               break;
            }
            case BACKTRACK: { 
               converged = backtrackSolveStepEnergy (icnt, mtargs);
               break;
            }
            case LEVENBERG_MARQUARDT: { 
               converged = levenbergMarquardtSolveStep (icnt, mtargs);
               break;
            }
            default: {
               throw new InternalErrorException (
                  "Unimplemented search strategy "+mySearchStrategy);
            }
         }
         icnt++;
      }
      while (!converged && icnt < myMaxIterations);               
      projectToConstraints();
      updateAttachmentPosStates(); // update frame markers, etc.
      if (debug) {
         System.out.printf ("Done. e0=%g efinal=%g\n", energy0, myEnergyPrev);
      }

      myNumIterations += icnt;
      myTotalNumIterations += icnt;
      myNumSolves++;
      return converged ? icnt : -1;
   }

   /**
    * Executes a single Levenberg Marquardt solve step.
    */
   boolean levenbergMarquardtSolveStep (int icnt, VectorNd mtargs) {
      if (icnt == 0) {
         myLmDamping = 0;
      }
      updateAndSolveForStep (myLmDamping);

      if (myUseLMAcceleration) {
         VectorNd rhs = computeLMAccelRhs (myQPrev, mtargs, /*h*/0.1);
         VectorNd chk = computeLMAccelRhsChk (myQPrev, mtargs, /*h*/0.1);
         VectorNd acc = new VectorNd (myTotalVelSize);
         if (!rhs.epsilonEquals (chk, 1e-10)) {
            System.out.println ("rhs: " + rhs.toString ("%12.8f"));
            System.out.println ("chk: " + chk.toString ("%12.8f"));
            System.out.println ("NOT EQUAL");
         }
         myKKTSolver.solve (acc, myLam, myThe, rhs, myBg, myBn); 
         if (debug) {
            System.out.printf (
               "  acc=%g rhs=%g F=%g\n", acc.norm(), rhs.norm(), myF.norm());
               
         }
         if (2*computeDqNorm(acc)/computeDqNorm(myDq) < 0.5) {
            System.out.println ("acc OK");
            myDq.scaledAdd (-0.5, acc);
         }
      }

      //updateAndProjectPosState (null, myQPrev, myDq, 1.0);
      updatePosState (myQPrev, myDq, 1.0);
      //projectToConstraints();

      double energy = computeEnergy (mtargs);
      double delE = energy-myEnergyPrev;
         
      // check stopping conditions
      double dqnorm = computeDqNorm(myDq);
      double dqDotPrev = computeDqDot (myDq, myDqPrev);
      double dqCos;
      if (icnt == 0) {
         dqCos = 1;
      }
      else {
         dqCos = dqDotPrev/(dqnorm*myDqnormPrev);
      }
         
      if (debug) {
         System.out.printf (
            "   icnt=%d delE=%g dqnorm=%g deriv=%g %s%s\n", 
            icnt, 
            delE, dqnorm, -myDq.dot(myF),
            dqDotPrev < 0 ? "NEG_DQ " : "",
            delE > 0 ? "E_INC " : "");
      }

      boolean converged = false;
      double tol = myConvergenceTol;
      if (myLmDamping > 1) {
         tol /= myLmDamping;
      }
      boolean rejectStep = false;
      if (dqnorm <= tol) {
         myDqnormPrev = dqnorm;
         converged = true;
      }
      else {
         if ((1-dqCos)*energy > myEnergyPrev || (icnt > 3 && dqCos < 0)) {
            //rejectStep = true;
            increaseLMDamping (10, 0.1, 1000);
         }
         else {
            decreaseLMDamping (0.1, 0.1);
         }
         if (!rejectStep) {
            computeDisplacements (myDisps, mtargs);
         }
      }
      if (!rejectStep) {
         if (debug) {
            System.out.printf ("    e=%g delE=%g\n", energy, delE);
         }
         myDqPrev.set (myDq);
         myDqnormPrev = dqnorm;
         getPosState (myQPrev);
         myEnergyPrev = energy;
      }
      else {
         setPosState (myQPrev);
      }
      return converged;
   }

   /**
    * Executes a single backtracking solve step, where the backtracking is
    * based on a merit function that combines marker displacement energy and a
    * constraint penalty.
    */
   boolean backtrackSolveStepMerit (int icnt, VectorNd mtargs) {
      boolean converged = false;

      if (icnt == 0) {
         updateConstraintInfo();
         myCPenaltyPrev = computeCPenalty();
      }
      
      updateAndSolveForStep (/*lmDamping*/0);

      updatePosState (myQPrev, myDq, 1.0);
      updateConstraintInfo();

      double energy = computeEnergy (mtargs);
      double cpenalty = computeCPenalty();
      double penalty = energy + cpenalty;
      double penaltyPrev = myEnergyPrev + myCPenaltyPrev;
      double delE = energy-myEnergyPrev;
      double delC = cpenalty-myCPenaltyPrev;
      double delP = penalty-penaltyPrev;
         
      // check stopping conditions
      double dqnorm = computeDqNorm(myDq);
      double dqDotPrev = computeDqDot (myDq, myDqPrev);
         
      if (debug) {
         System.out.printf (
            "   icnt=%d P=%g delP=%g delE=%g delC=%g dqnorm=%g %s%s\n", 
            icnt, 
            penalty, delP, delE, delC, dqnorm,
            dqDotPrev < 0 ? "NEG_DQ " : "",
            delP > 0 ? "P_INC " : "");
      }

      double alpha = 1.0;
      if (dqnorm <= myConvergenceTol) {
         converged = true;
      }
      else {
         if (delP > 0) {
            LineMerit efunc = new LineMerit (myQPrev, myDq, mtargs);
            FunctionValuePair min = BrentMinimizer.findMin (
               efunc, 0, penaltyPrev, 1, penalty, 0.1);
            alpha = min.x;
            if (debug) {
               System.out.printf (
                  "    BACKTRACK: alpha=%s cnt=%d\n", 
                  alpha, efunc.myCnt);
            }
            if (alpha == 0) {
               // Can happen sometimes if there was insufficient
               // position correction applied to myQPrev
               alpha = 0.5;
            }
         }

         if (alpha != 1.0) {
            updatePosState (myQPrev, myDq, alpha);
            updateConstraintInfo();
            energy = computeEnergy (mtargs);
            cpenalty = computeCPenalty();
         }
         computeDisplacements (myDisps, mtargs);
      }
      myDqPrev.set (myDq);
      myDqnormPrev = dqnorm;
      getPosState (myQPrev);
      myEnergyPrev = energy;
      myCPenaltyPrev = cpenalty;
      return converged;
   }

   /**
    * Executes a single backtracking solve step, where the backtracking is
    * based on marker displacement energy only.
    */
   boolean backtrackSolveStepEnergy (int icnt, VectorNd mtargs) {
      boolean converged = false;
      updateAndSolveForStep (/*lmDamping*/0);

      VectorNd ddq = updateAndProjectPosState (null, myQPrev, myDq, 1.0);
      //projectToConstraints();

      double energy = computeEnergy (mtargs);
      double delE = energy-myEnergyPrev;
         
      // check stopping conditions
      double dqnorm = computeDqNorm(myDq);
      double dqDotPrev = computeDqDot (myDq, myDqPrev);
         
      if (debug) {
         System.out.printf (
            "   icnt=%d delE=%g dqnorm=%g ddqnorm=%g deriv=%g %s%s\n", 
            icnt, 
            delE, dqnorm, computeDqNorm(ddq), -myDq.dot(myF),
            dqDotPrev < 0 ? "NEG_DQ " : "",
            delE > 0 ? "E_INC " : "");
      }

      double alpha = 1.0;
      if (dqnorm <= myConvergenceTol) {
         converged = true;
      }
      else {
         double armijo = -0.01*myDq.dot(myF);
         LineEnergy efunc = new LineEnergy (myQPrev, myDq, mtargs);
         if (delE > armijo) { // || dqDotPrev < 0) {
            FunctionValuePair min = BrentMinimizer.findMin (
               efunc, 0, myEnergyPrev, 1, energy, 0.1);
            alpha = min.x;
            if (debug) {
               System.out.printf (
                  "    energy=%g delE=%g armijp=%g\n",
                  energy, delE, armijo);
               System.out.printf (
                  "    BACKTRACK: alpha=%s cnt=%d deriv=%s e=%g\n",
                  alpha, efunc.myCnt, -myDq.dot(myF), energy);
            }
            if (alpha == 0) {
               // Can happen sometimes if there was insufficient
               // position correction applied to myQPrev
               alpha = 0.5;
            }
         }

         if (alpha != 1.0) {
            updateAndProjectPosState (null, myQPrev, myDq, alpha);
            //projectToConstraints();
         }
         else if (alpha == 0) {
            setPosState (myQPrev);
         }
         energy = computeDisplacements (myDisps, mtargs);
      }
      if (debug) {
         System.out.printf ("    e=%g delE=%g\n", energy, delE);
      }
      myDqPrev.set (myDq);
      myDqnormPrev = dqnorm;
      getPosState (myQPrev);
      myEnergyPrev = energy;
      return converged;
   }

   /**
    * Executes a single solve step with no backtracking or dynamic adjustment to
    * the step direction.
    */
   boolean basicSolveStep (int icnt, VectorNd mtargs) {
      boolean converged = false;
      updateAndSolveForStep (/*lmDamping*/0);

      VectorNd ddq = updateAndProjectPosState (null, myQPrev, myDq, 1.0);
      //projectToConstraints();

      double energy = computeEnergy (mtargs);
      double delE = energy-myEnergyPrev;
         
      // check stopping conditions
      double dqnorm = computeDqNorm(myDq);
      double dqDotPrev = computeDqDot (myDq, myDqPrev);
      
      if (debug) {
         System.out.printf (
            "   icnt=%d delE=%g dqnorm=%g ddqnorm=%g deriv=%g %s%s\n", 
            icnt, 
            delE, dqnorm, computeDqNorm(ddq), -myDq.dot(myF),
            dqDotPrev < 0 ? "NEG_DQ " : "",
            delE > 0 ? "E_INC " : "");
      }

      if (dqnorm <= myConvergenceTol) {
         converged = true;
      }
      else {
         computeDisplacements (myDisps, mtargs);
      }
      if (debug) {
         System.out.printf ("    e=%g delE=%g\n", energy, delE);
      }
      myDqPrev.set (myDq);
      myDqnormPrev = dqnorm;
      getPosState (myQPrev);
      myEnergyPrev = energy;
      return converged;
   }

   /**
    * Method to solve the constrained multibody problem that does not use
    * sparse matrices but instead relies on the fact that the primary solve
    * matrix associated with the bodies is block diagonal. However, this still
    * proved to be 2-3 times slower than solving a KKT system, since it
    * requires forming a Schur complement matrix that has a relatively high
    * dimension due to the large number of constraints. A more efficient, but
    * more complicated, approach would be to use an O(n) Featherstone dynamics
    * algorithm.
    */
   void factorAndSolve (VectorNd dq, VectorNd bd) {
      // create Schur complement
      MatrixNd S = new MatrixNd (myGsize, myGsize);
      VectorNd col = new VectorNd (6);
      VectorNd row = new VectorNd (6);
      VectorNd sol = new VectorNd (6);
      VectorNd gx = new VectorNd (myGsize);
      VectorNd sub = new VectorNd (6);
      VectorNd bx = new VectorNd (myTotalVelSize);

      gx.negate (myBg);
      for (int bj=0; bj<myGT.numBlockCols(); bj++) {
         MatrixBlock cblk0 = myGT.firstBlockInCol (bj);
         int j = myGT.getBlockColOffset (bj);
         for (int cidx=0; cidx<cblk0.colSize(); cidx++) {
            for (MatrixBlock cblk=cblk0; cblk!=null; cblk=cblk.down()) {
               int bi = cblk.getBlockRow();
               if (bd != null) {
                  bd.getSubVector (mySolveMatrix.getBlockRowOffset(bi), sub);
               }               
               BodyInfo binfo = mySortedBodyInfo.get(bi);
               BodyMarkerInfo bminfo = myBodyMarkerInfo.get(binfo.myIndex);
               cblk.getColumn (cidx, col);
               bminfo.solve (sol, col);
               MatrixBlock rblk = myGT.firstBlockInRow (bi);
               while (rblk != null) {
                  int i = myGT.getBlockColOffset (rblk.getBlockCol());
                  for (int ridx=0; ridx<rblk.colSize(); ridx++) {
                     rblk.getColumn (ridx, row);
                     S.add (i, j, row.dot(sol));
                     i++;
                  }
                  rblk = rblk.next();
               }
               if (bd != null) {
                  gx.add (j, sub.dot(sol));
               }
            }
            j++;
         }
      }
      for (int i=0; i<myGsize; i++) {
         S.add (i, i, -myRg.get(i));
      }
      CholeskyDecomposition chol = new CholeskyDecomposition(S);
      chol.solve (myLam, gx);
      if (bd != null) {
         bx.set (bd);
      }
      for (int bj=0; bj<myGT.numBlockCols(); bj++) {
         MatrixBlock cblk0 = myGT.firstBlockInCol (bj);
         int j = myGT.getBlockColOffset (bj);
         for (int cidx=0; cidx<cblk0.colSize(); cidx++) {
            for (MatrixBlock cblk=cblk0; cblk!=null; cblk=cblk.down()) {
               int i = myGT.getBlockRowOffset(cblk.getBlockRow());
               cblk.getColumn (cidx, col);
               bx.addScaledSubVector (i, -myLam.get(j), col);
            }
            j++;
         }
      }
      for (int bi=0; bi<numBodies(); bi++) {
         BodyInfo binfo = mySortedBodyInfo.get(bi);
         BodyMarkerInfo bminfo = myBodyMarkerInfo.get (binfo.myIndex);
         int i = mySolveMatrix.getBlockRowOffset (bi);
         bx.getSubVector (i, sub);
         bminfo.solve (sub, sub);
         dq.setSubVector (i, sub);
      }
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
      if (getModelSize() != -1) {
         pw.println ("modelSize=" + fmt.format(getModelSize()));
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
      setModelSize (-1);
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
            setModelSize (rtok.scanNumber());
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
               myMarkerInfo.add (new MarkerInfo (mkrs[i], weights[i], i));
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
   static protected SpatialInertia buildJTWJMatrix (
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

      clearSolveCounts();
      VectorNd mpos = new VectorNd (newProbe.getVsize());
      FunctionTimer timer = new FunctionTimer();
      int convergedCnt = 0;
      timer.start();
      saveModelState();
      NumericList nlist = targProbe.getNumericList();
      NumericListKnot last = null;
      if (interval == -1) {
         NumericListKnot knot;
         for (knot=nlist.getFirst(); knot!=null; knot=knot.getNext()) {
            int niter = solve (knot.v);
            if (niter != -1) {
               convergedCnt++;
            }
            last = collector.addData (newProbe, knot.t, last);
            debugMask = true;
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
            int niter = solve (mpos);
            if (niter != -1) {
               convergedCnt++;
            }
            debugMask = true;
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
      debugMask = false;
      restoreModelState();
      timer.stop();
      System.out.printf (
         "IK probe created in %s, converged %d/%d, avg iters=%g\n",
         timer.result(1), convergedCnt, numSolves(), 
         numIterations()/(double)numSolves());
   }

   public static double getAvgDqRatio() {
      return 0;
   }
}

