package artisynth.core.mechmodels;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import artisynth.core.mechmodels.CoordinateSetter.JointCoordRequest;
import artisynth.core.mechmodels.CoordinateSetter.SetStatus;
import maspack.matrix.Matrix;
import maspack.matrix.Matrix6d;
import maspack.matrix.Matrix6dBlock;
import maspack.matrix.MatrixBlock;
import maspack.matrix.MatrixBlockBase;
import maspack.matrix.MatrixNd;
import maspack.matrix.QRDecomposition;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.VectorNd;
import maspack.matrix.VectorNi;
import maspack.solvers.KKTSolver;
import maspack.spatialmotion.RigidBodyConstraint.MotionType;
import maspack.util.DoubleInterval;
import maspack.util.DynamicIntArray;
import maspack.util.InternalErrorException;
import maspack.util.NumberFormat;

/**
 * Used by CoordinateSetter to set the coordinate values for joints within a
 * specific joint node of a kinematic tree. This is needed in situations where
 * the joint nodes's joints and constrainers form a kinematic loop of some kind
 * and therefore coordinate values cannot be set independently.
 */
public class CoordinateSolver extends StaticRigidBodySolver {

   private static final double RTOD = 180/Math.PI;
   private static final double DTOR = Math.PI/180;

   private static final double RANK_TOL = 1e-7;

   public boolean uniformWeighting = false;
   public boolean printCtrlChanges = false;
   public boolean applyPolish = true;

   public final double MAX_REVERSE_COS = -0.2; // about 100 degrees
   public final double MAX_REVERSE_CNT = 3;

   private int MAX_ITER = 20;

   private static boolean newLimitMethod = true;

   JointBase myJoint;
   public boolean debug;

   SparseBlockMatrix myCoordinateJT;
   VectorNd myWeights = new VectorNd();
   VectorNd myDelCoordWeights = new VectorNd();
   int[] myCoordStatus = null;

   int myNumIterations;    // iteration count for the last loop solve
   int myTotalIterations;  // cummulation iteration count for loop solves
   double myLastStepNorm;  // norm of the most recent change in body poses

   /**
    * Creates a new CoordinateSolver for setting joint coordinates
    * within the specified MechModel.
    *
    * @param mech MechModel in which the coordinates are located
    */
   public CoordinateSolver (MechModel mech) {
      myKKTSolver = new KKTSolver();
      myAnalyze = true;
      myMech = mech;
      setPenetrationTolerance (1e-5);
   }

   void initialize (
      List<JointCoordRequest> reqs, Collection<RigidBody> bodies, 
      Collection<? extends BodyConstrainer> constrainers) {

      setBodiesAndConstrainers (bodies, constrainers);
      int numc = numCoordinates();
      computeDelCoordWeights();
      myWeights.setSize (numc);
      myCoordStatus = new int[numc];
      for (int i=0; i<numc; i++) {
         myCoordStatus[i] = SetStatus.DEPENDENT;
      }
      // local solve indices and saved locked values for all requests:
      for (JointCoordRequest req : reqs) {
         req.mySolveIdx = getSolveCoordIndex (req.myHandle);
      }
      myNumIterations = 0;
   }      

   private double clipToIncrementLimits (VectorNd dq, double slim) {
      VectorNd dcoords = new VectorNd (numCoordinates());
      myCoordinateJT.mulTranspose (dcoords, dq);
      
      int cidx = 0;
      for (JointCoordinateHandle handle : getCoordinateHandles()) {
         double max;
         if (handle.getMotionType() == MotionType.ROTARY) {
            max = DTOR*15;
         }
         else {
            double range = handle.getValueRange().getRange();
            if (range != INF) {
               max = 0.2*range;
            }
            else {
               max = 0.1*getModelSize();
            }
         }
         double dval = Math.abs(dcoords.get(cidx));
         if (dval > max) {
            slim = Math.min (slim, max/dval);
         }
         cidx++;
      }

      if (slim < 1) {
         if (debug) {
            MatrixNd J = new MatrixNd (myCoordinateJT);
            J.transpose();
            dcoords.scale (slim);          
            System.out.printf ("    CLIP INC slim=%s\n", slim);
            printCoords ("      del*=", dcoords);
         }
         dq.scale (slim);

      }
      return slim;
   }

   private double clipToCoordinateLimits (
      VectorNd dq, VectorNd coords, double slim) {
      VectorNd dcoords = new VectorNd (numCoordinates());
      myCoordinateJT.mulTranspose (dcoords, dq);
      VectorNd coordsx = new VectorNd (coords);
      coordsx.add (dcoords);

      double rtol = DTOR*2;
      double ttol = getModelSize()*0.01;

      int cidx = 0;
      for (JointCoordinateHandle handle : getCoordinateHandles()) {
         DoubleInterval range = handle.getValueRange();
         double min = range.getLowerBound();
         double max = range.getUpperBound();
         if (newLimitMethod) {
            MotionType mtype = handle.getMotionType();
            double tol = rtol;
            if (mtype != MotionType.ROTARY) {
               double rng = range.getRange();                     
               if (rng != INF) {
                  tol = 0.02*rng;
               }
               else {
                  tol = ttol;
               }
            }
            min -= tol;
            max += tol;
         }
         double cval = coords.get(cidx);
         double cvalx = coordsx.get(cidx);
         if (cvalx < min && dcoords.get(cidx) < 0) {
            double s = (cval >= min ? (cval-min)/(cval-cvalx) : 0);
            slim = Math.min (slim, s);
         }
         else if (cvalx > max && dcoords.get(cidx) > 0) {
            double s = (cval <= max ? (max-cval)/(cvalx-cval) : 0);
            slim = Math.min (slim, s);
         }
         cidx++;
      }
      if (slim < 1) {
         dq.scale (slim);
         dcoords.scale (slim);
         if (debug) {
            System.out.printf ("    CLIP LIMITS slim=%s\n", slim);
            printCoords ("      del*=", dcoords);
         }
      }
      return slim;
   }

   private void updateSolveMatrix (
      VectorNd f, VectorNd targetCoords, VectorNd coords, double lmDamping) {

      int numc = numCoordinates();
      SparseBlockMatrix JT = getCoordinateJT();

      //SVDecomposition svd = new SVDecomposition(JT);
      //System.out.println ("      JT svd=" + svd.getS().toString ("%10.6f"));

      // JTW = JT W, where W is the diagonal weighting matrix
      SparseBlockMatrix JTW = getCoordinateJTW (JT);

      VectorNd jforce = new VectorNd (numc);
      jforce.sub (targetCoords, coords);
      JTW.mul (f, jforce);
      //System.out.println ("JT=\n" + JT.toString ("%11.8f"));
      mySolveMatrix.setZero();
      int numb = numBodies();

      // compute upper triangular part of the (symmetric) solve matrix
      for (int bi=0; bi<numb; bi++) {
         for (int bj=bi; bj<numb; bj++) {
            MatrixBlock blk_i = JTW.firstBlockInRow (bi);
            MatrixBlock blk_j = JT.firstBlockInRow (bj);
            while (blk_i != null && blk_j != null) {
               if (blk_i.getBlockCol() == blk_j.getBlockCol()) {
                  MatrixBlock blk_k = mySolveMatrix.getBlock (bi, bj);
                  if (blk_k == null) {
                     blk_k = new Matrix6dBlock();
                     mySolveMatrix.addBlock (bi, bj, blk_k);
                  }
                  blk_k.mulTransposeRightAdd (blk_i, blk_j);
                  if (bi == bj && lmDamping > 0) {
                     addLMDampingTerm ((Matrix6dBlock)blk_k, lmDamping);
                  }
                  blk_i = blk_i.next();
                  blk_j = blk_j.next();
               }
               else if (blk_i.getBlockCol() < blk_j.getBlockCol()) {
                  blk_i = blk_i.next();
               }
               else {
                  blk_j = blk_j.next();
               }
            }
         }
      }
      // set lower triangular part of solve matrix from upper part
      for (int bi=0; bi<numb; bi++) {
         for (int bj=bi+1; bj<numb; bj++) {
            MatrixBlock blk_ij = mySolveMatrix.getBlock (bi, bj);
            if (blk_ij != null) {
               MatrixBlock blk_ji = mySolveMatrix.getBlock (bj, bi);
               if (blk_ji == null) {
                  blk_ji = new Matrix6dBlock();
                  mySolveMatrix.addBlock (bj, bi, blk_ji);
               }
               ((Matrix6d)blk_ji).transpose ((Matrix6d)blk_ij);
            }
         }
      }
      // add a regularization term to S in case it is only symmetric positive
      // semi-definite. Keep the term small or otherwise convergence will be
      // slowed.
      double reg = 1.0;
      if (mySolveMatrix.trace() != 0) {
         reg = 0.001*mySolveMatrix.trace(); // 0.000001
      }
      for (int bi=0; bi<numb; bi++) {
         Matrix6d dblk = (Matrix6d)mySolveMatrix.getBlock (bi, bi);
         dblk.addDiagonal (reg);
      }
      myCoordinateJT = JT;
   }

   /**
    * Returns the transpose of the matrix J which maps changes in body pose
    * onto changes in coordinate values (without accounting for joint
    * constraints).
    */
   private SparseBlockMatrix getCoordinateJT () {
      int[] rowSizes = new int[numBodies()];
      for (int i=0; i<rowSizes.length; i++) {
         rowSizes[i] = 6;
      }
      SparseBlockMatrix JT = new SparseBlockMatrix(rowSizes, new int[0]);
      for (BodyConstrainer bcon : myConstrainers) {
         if (bcon instanceof JointBase) {
            JointBase jnt = (JointBase)bcon;
            jnt.addCoordinateWrenches (JT, mySolveIndexMap);
         }
      }
      return JT;
   }

   /**
    * Compute
    * <pre>
    * JTW = JT W
    * </pre>
    * where W is the diagonal weighting matrix given by {@code myWeights}.
    */
   private SparseBlockMatrix getCoordinateJTW (SparseBlockMatrix JT) {
      if (JT.colSize() != numCoordinates()) {
         throw new InternalErrorException (
            "JT.colSize " + JT.colSize() +
            " != num of coordinates " + numCoordinates());
      }
      // create weighting matrices
      MatrixNd[] W = new MatrixNd[JT.numBlockCols()];
      int widx = 0;
      for (int bj=0; bj<JT.numBlockCols(); bj++) {
         int csize = JT.getBlockColSize (bj);
         W[bj] = new MatrixNd (csize, csize);
         for (int i=0; i<csize; i++) {
            W[bj].set (i, i, myWeights.get(widx++));
         }
      }
      // compute JTW = JT W
      SparseBlockMatrix JTW = new SparseBlockMatrix (
         JT.getBlockRowSizes(), JT.getBlockColSizes());
      for (int bi=0; bi<JT.numBlockRows(); bi++) {
         MatrixBlock blk;
         for (blk = JT.firstBlockInRow(bi); blk != null; blk = blk.next()) {
            int bj = blk.getBlockCol();
            MatrixBlock newBlk = MatrixBlockBase.alloc (6, blk.colSize());
            newBlk.mulAdd (blk, W[bj]);
            JTW.addBlock (bi, bj, newBlk);
         }
      }
      return JTW;
   }

   private int numCoordinates() {
      return myCoordHandles.size();
   }

   private int getSolveCoordIndex (JointCoordinateHandle jch) {
      int cidx = 0;
      for (JointCoordinateHandle handle : getCoordinateHandles()) {
         if (handle.equals (jch)) {
            return cidx;
         }
         cidx++;
      }
      return -1;
   }      

   VectorNd getCoordValues (VectorNd allCoords) {
      if (allCoords == null) {
         allCoords = new VectorNd (numCoordinates());
      }
      int cidx = 0;
      for (JointCoordinateHandle handle : getCoordinateHandles()) {
         allCoords.set (cidx++, handle.getValue());
      }
      return allCoords;
   }
   
   VectorNd toDegrees (VectorNd allCoords) {
      VectorNd coordsDeg = new VectorNd (numCoordinates());
      int cidx = 0;
      for (JointCoordinateHandle handle : getCoordinateHandles()) {
         double val = allCoords.get (cidx);
         if (handle.getMotionType() == MotionType.ROTARY) {
            val *= RTOD;
         }
         coordsDeg.set (cidx++, val);
      }
      return coordsDeg;
   }

   /*
    * Finds body and connector information.
    */
   protected void findBodiesAndConnectors () {
      boolean excludeGrounded = false;
      LinkedHashSet<BodyConstrainer> constrainerSet = new LinkedHashSet<>();
      LinkedHashSet<RigidBody> bodySet = new LinkedHashSet<>();
      if (myJoint.getBodyA() instanceof RigidBody) {
         RigidBody bodyA = (RigidBody)myJoint.getBodyA();
         if (!excludeGrounded || !bodyA.isGrounded()) {
            bodySet.add (bodyA);
         }
      }
      if (myJoint.getBodyB() instanceof RigidBody) {
         RigidBody bodyB = (RigidBody)myJoint.getBodyB();
         if (!excludeGrounded || !bodyB.isGrounded()) {
            bodySet.add (bodyB);
         }
      }
      findBodiesAndConstrainers (
         bodySet, constrainerSet, excludeGrounded);
   }

   private void computeDelCoordWeights() {
      myDelCoordWeights.setSize (numCoordinates());
      int cidx = 0;
      for (JointCoordinateHandle handle : getCoordinateHandles()) {
         if (handle.getMotionType() == MotionType.ROTARY) {
            myDelCoordWeights.set (cidx, 1);
         }
         else {
            double rng = handle.getValueRange().getRange();
            if (rng == INF) {
               rng = 0.1*getModelSize();
            }
            myDelCoordWeights.set (cidx, sqr(2*Math.PI/rng));
         }
         cidx++;
      }
   }

   private double computeDelCoordDot (VectorNd delc0, VectorNd delc1) {
      int numc = numCoordinates();
      double sum = 0;
      for (int cidx=0; cidx<numc; cidx++) {
         sum += delc0.get(cidx)*delc1.get(cidx)*myDelCoordWeights.get(cidx);
      }
      return sum;
   }

   private double computeDelCoordNorm (VectorNd delc) {
      return Math.sqrt(computeDelCoordDot (delc, delc));
   }

   private double computeDelCoordCos (
      VectorNd delc0, VectorNd delc1, double tol) {
      double dot = computeDelCoordDot (delc0, delc1);
      double norm0 = computeDelCoordNorm(delc0);
      double norm1 = computeDelCoordNorm(delc1);
      if (norm0 > tol && norm1 > tol) {
         return dot/(norm0*norm1);
      }
      else {
         return 0;
      }
   }

   private double reduceRange (double ang) {
      while (ang > Math.PI) {
         ang -= 2*Math.PI;
      }
      while (ang < -Math.PI) {
         ang += 2*Math.PI;
      }
      return ang;
   }


   private double computeDelCoords (
      VectorNd delc, VectorNd coords0, VectorNd coords1) {
      int cidx = 0;
      double norm = 0;
      for (JointCoordinateHandle handle : getCoordinateHandles()) {
         double del = coords0.get(cidx) - coords1.get(cidx);
         if (handle.getMotionType() == MotionType.ROTARY) {
            del = reduceRange (del);
         }
         norm += del*del*myDelCoordWeights.get(cidx);
         if (delc != null) {
            delc.set (cidx, del);
         }
         cidx++;
      }
      return Math.sqrt(norm);
   }

   private void unlockIndependentCoords (
      VectorNd weights, ArrayList<JointCoordRequest> reqs) {
      weights.setZero();
      for (int k=0; k<reqs.size(); k++) {
         reqs.get(k).unlockCoordinate();
      }
      updateConstraints();
      myAnalyze = true;
   }

   private void checkDofs (ArrayList<JointCoordRequest> reqs) {
 
      double rankTol = RANK_TOL;

      MatrixNd JTg = new MatrixNd();
      MatrixNd JTp = getConstrainedCoordinateJT (
         JTg, /*useUnilaterals*/true, rankTol);

      // Find the submatrix JTreq of JTp corresponding to request coordinates
      MatrixNd JTreq = new MatrixNd(JTp.rowSize(), reqs.size());
      VectorNd col = new VectorNd();
      for (int i=0; i<reqs.size(); i++) {
         int cidx = reqs.get(i).mySolveIdx;
         JTp.getColumn (cidx, col);
         JTreq.setColumn (i, col);
      }
      // Find independent coordinates among JTreq
      QRDecomposition qrd = new QRDecomposition();
      qrd.factorWithPivoting (JTreq);
      System.out.println (
         "    JTreq R diagonal: " +
         qrd.getR().getDiagonal().toString ("%11.8f"));
   }

   /**
    * Returns the transpose of the constrained matrix J which maps changes in
    * body pose onto changes in coordinate values, while accounting for
    * joint constraints. 
    */
   private MatrixNd getConstrainedCoordinateJT (
      MatrixNd JTg, boolean useUnilateral, double rankTol) {
      int numc = numCoordinates();

      // find QR decomposition of G^T with pivoting so we can find its rank
      QRDecomposition qrd = new QRDecomposition();
      MatrixNd GT = new MatrixNd (myGT);
      int[] nonLockingIdxs = getNonLockingGTCols();
      if (nonLockingIdxs.length < myGsize) {
         GT.setColumnSubMatrix (GT, nonLockingIdxs, nonLockingIdxs.length);
      }
      qrd.factorWithPivoting (GT);
      int rank = qrd.rank (rankTol);
      MatrixNd Q = qrd.getQ (rank);
      // JT maps body velocities onto coordinate velocities, *before*
      // constraints are applied.
      MatrixNd JT = new MatrixNd(getCoordinateJT());
      if (numc != JT.colSize()) {
         throw new InternalErrorException (
            "Coordinate JT matrix col size " + JT.colSize() + 
            " not equal number of coordinates " + numc);
      }
      // project JT onto the set of feasible velocities JTg implied by the
      // contraints. If G is the constraint matrix, this amounts to forming the
      // product
      // 
      // JTg = (I - G^T inv(G G^T) G) JT
      //
      // which can be simplified to
      //
      // JTg = (I - Q Q^T) JT
      //
      if (JTg == null) {
         JTg = new MatrixNd();
      }
      JTg.mulTransposeLeft (Q, JT);
      JTg.mul (Q, JTg);
      JTg.sub (JT, JTg);

      MatrixNd JTp;
      if (useUnilateral && myNsize > 0) {
         MatrixNd NT = new MatrixNd (myNT);
         MatrixNd NTp = new MatrixNd();
         NTp.mulTransposeLeft (Q, NT);
         NTp.mul (Q, NTp);
         NTp.sub (NT, NTp);

         qrd.factorWithPivoting (NTp);
         rank = qrd.absRank (rankTol);
         Q = qrd.getQ (rank);
         JTp = new MatrixNd();
         JTp.mulTransposeLeft (Q, JTg);
         JTp.mul (Q, JTp);
         JTp.sub (JTg, JTp);
      }
      else {
         JTp = JTg;
      }

      return JTp;
   }

   private int findIndependentCoords (
      VectorNd weights, VectorNd targetCoords,
      VectorNd coords, ArrayList<JointCoordRequest> reqs, int maxRank) {
 
      int numc = numCoordinates();
      double rankTol = RANK_TOL;

      // set default settings:
      for (int cidx=0; cidx<numc; cidx++) {
         if (uniformWeighting) {
            weights.set (cidx, 1.0);
         }
         else {
            weights.set (cidx, 0.0);
         }
      }
      MatrixNd JTg = new MatrixNd();
      MatrixNd JTp = getConstrainedCoordinateJT (
         JTg, /*useUnilateral*/true, rankTol);

      // Find the submatrix JTreq of JTp corresponding to request coordinates
      MatrixNd JTreq = new MatrixNd(JTp.rowSize(), reqs.size());
      VectorNd col = new VectorNd();
      for (int i=0; i<reqs.size(); i++) {
         int cidx = reqs.get(i).mySolveIdx;
         JTp.getColumn (cidx, col);
         JTreq.setColumn (i, col);
      }
      // Find independent coordinates among JTreq
      QRDecomposition qrd = new QRDecomposition();
      qrd.factorWithPivoting (JTreq);
      int rank = qrd.absRank(rankTol);
      if (maxRank >=0) {
         rank = Math.min (rank, maxRank);
      }
      int[] cperm = qrd.getColumnPermutation();
      boolean[] reqControllable = new boolean[reqs.size()];
      int reqDof = 0;
      for (int k=0; k<rank; k++) {
         if (reqs.get(cperm[k]).myHandle.getLimitEngagement() == 0) {
            reqControllable[cperm[k]] = true;
            reqDof++;
         }
      }

      if (reqDof == 0) {
         int cidx = reqs.get(cperm[0]).mySolveIdx;
         JTg.getColumn (cidx, col);
         if (col.norm() < rankTol) {
            //System.out.println ("FUDGE col.norm=" + col.norm());
            reqControllable[cperm[0]] = true;
            reqDof = 1;
         }
      }
      int numFree = 0;
      for (int k=0; k<reqs.size(); k++) {
         int cidx = reqs.get(k).mySolveIdx;
         if (reqControllable[k]) {
            if (myCoordStatus[cidx] != SetStatus.LIMITED) {
               reqs.get(k).lockCoordinate (targetCoords.get(cidx));
               myCoordStatus[cidx] = SetStatus.FREE;
            }
         }
         else {
            if (myCoordStatus[cidx] == SetStatus.FREE) {
               reqs.get(k).unlockCoordinate();
               myCoordStatus[cidx] = SetStatus.LIMITED;
            }
         }
         if (myCoordStatus[cidx] == SetStatus.FREE) {
            numFree++;
         }
         if (myCoordStatus[cidx] != SetStatus.LIMITED) {
            weights.set (cidx, 1.0);
         }
      }
      updateConstraints();
      myAnalyze = true;
      if (maxRank >= 0) {
         //System.out.printf ("    maxRank=%d numFree=%d\n", maxRank, numFree);
      }
      return numFree;
   }

   ArrayList<JointCoordRequest> findIndependentRequests (
      ArrayList<JointCoordRequest> reqs) {
 
      updateConstraints();
      double rankTol = RANK_TOL;

      MatrixNd JTg = getConstrainedCoordinateJT (
         null, /*useUnilateral*/false, rankTol);


      // Find the submatrix JTreq of JTg corresponding to request coordinates
      MatrixNd JTreq = new MatrixNd(JTg.rowSize(), reqs.size());
      VectorNd col = new VectorNd();
      for (int i=0; i<reqs.size(); i++) {
         int cidx = reqs.get(i).mySolveIdx;
         JTg.getColumn (cidx, col);
         JTreq.setColumn (i, col);
      }
      // Find independent coordinates among JTreq
      QRDecomposition qrd = new QRDecomposition();
      qrd.factorWithPivoting (JTreq);
      int rank = qrd.absRank(rankTol);
      int[] cperm = qrd.getColumnPermutation();
      ArrayList<JointCoordRequest> indepReqs = new ArrayList<>();
      for (int k=0; k<rank; k++) {
         indepReqs.add (reqs.get(cperm[k]));
      }
      if (indepReqs.size() == 0) {
         int cidx = reqs.get(cperm[0]).mySolveIdx;
         JTg.getColumn (cidx, col);
         if (col.norm() < rankTol) {
            //System.out.println ("FUDGE col.norm=" + col.norm());
            indepReqs.add (reqs.get(cperm[0]));
         }
      }
      for (JointCoordRequest req : indepReqs) {
         myCoordStatus[req.mySolveIdx] = SetStatus.UNKNOWN;
      }     
      return indepReqs;
   }

   private void printCoordinateNames (String label) {
      System.out.print (label);
      for (JointCoordinateHandle handle : getCoordinateHandles()) {
         String name =
            handle.getName() + handle.getJoint().getNumber();
         System.out.printf ("%10s ", name);
      }
      System.out.println("");
   }

   private void printCoordinateCtrls (String label) {
      System.out.print (label);
      int k = 0;
      NumberFormat fmt = new NumberFormat ("%3.1f");
      for (JointCoordinateHandle handle : getCoordinateHandles()) {
         double w = myWeights.get(k);
         String disp = (w == 0 ? "0" : fmt.format(myWeights.get(k)));
         if (handle.isLocked()) {
            disp += "(L)";
         }
         System.out.printf ("%10s ", disp);
         k++;
      }
      System.out.println("");
   }

   private static final int LIMIT_ON = 0x01;
   private static final int LIMIT_OFF = 0x02;

   private int updateLimitEngagement (int[] limitEngagement) {
      int cidx = 0;
      int changes = 0;
      for (JointCoordinateHandle handle : getCoordinateHandles()) {
         int engaged = handle.getLimitEngagement();
         if (limitEngagement[cidx] != engaged) {
            limitEngagement[cidx] = engaged;
            if (engaged != 0) {
               changes |= LIMIT_ON;
            }
            else {
               changes |= LIMIT_OFF;
            }
         }
         cidx++;
      }
      return changes;
   }

   SetStatus solveForStep (
      ArrayList<JointCoordRequest> reqs, VectorNd targetCoords,
      RigidBody fixedBody, RigidTransform3d TFW0, int stepNum, int numSteps) {

      int numc = numCoordinates();
      int[] limitEngagement = new int[numc];
      
      double starg = stepNum/(double)numSteps;
      
      int velSize = myTotalVelSize;
      int posSize = myTotalPosSize;

      VectorNd dq = new VectorNd (velSize);
      VectorNd q = new VectorNd (posSize);
      VectorNd f = new VectorNd (velSize);

      // find the rank of the constraint matrix G, in order to see how many
      // free dofs we have available for locking coordinate values
      updateConstraints();
      updateLimitEngagement (limitEngagement);

      VectorNd coords = new VectorNd(numc);
      getCoordValues (coords);

      for (int k=0; k<reqs.size(); k++) {
         JointCoordRequest req = reqs.get(k);
         int cidx = req.mySolveIdx;
         if (myCoordStatus[cidx] != SetStatus.LIMITED) {
            double value = req.computeTargetValue (starg);
            targetCoords.set (cidx, value);
         }
      }
      
      int numFree = findIndependentCoords (
            myWeights, targetCoords, coords, reqs, -1);

      double tol = stepNum < numSteps ? 1e-3 : 1e-10;
      int maxIter = stepNum < numSteps ? 10 : 20;

      if (numFree == 0) {
         tol = 1e-10;
         maxIter = MAX_ITER;
      }
      //System.out.printf (
      // "  start numFree=%d Gsize=%d Nsize=%d\n", numFree, myGsize, myNsize);

      int icnt = 0;
      SetStatus status = new SetStatus (/*converged*/false);

      VectorNd realDel = new VectorNd (numc);
      VectorNd realDelPrev = new VectorNd (numc);
      VectorNd targetDiff = new VectorNd (numc);
      double targetErr = -1;
      double targetErrPrev;
      VectorNd dqPrev = new VectorNd (velSize);

      int numReverse = 0;

      do {
         getPosState (q);
         
         updateSolveMatrix (f, targetCoords, coords, /*lmdamping*/0.0);
         if (icnt == 0 && hasGTStructureChanged()) {
            myAnalyze = true;
         }
         if (myAnalyze) {
            int matrixType = Matrix.SYMMETRIC;
            myKKTSolver.analyze (mySolveMatrix, velSize, myGT, myRg, matrixType);
            myAnalyze = false;
         }     
         myKKTSolver.factor (mySolveMatrix, velSize, myGT, myRg, myNT, myRn);
         myKKTSolver.solve (dq, myLam, myThe, f, myBg, myBn); 

         VectorNd dcoords = new VectorNd (coords);
         myCoordinateJT.mulTranspose (dcoords, dq);
         if (debug) {
            System.out.println ("   icnt=" + icnt);
         }
         if (icnt == 0 && (printCtrlChanges || debug)) {
            printCoordinateNames ("    joints=");
            printCoordinateCtrls ("     ctrls=");
            printCoords ("    TARGET=", targetCoords);
         }
         if (debug) {
            if (icnt==0) {
               printCoords ("    startc=", coords);
            }
            printCoords ("       del=", dcoords);
         }

         clipToIncrementLimits (dq, 1.0);
         clipToCoordinateLimits (dq, coords, 1.0);

         updatePosState (q, dq, 1.0);
         updateConstraints ();
         
         VectorNd oldcoords = new VectorNd(coords);
         getCoordValues (coords);

         realDelPrev.set (realDel);
         realDel.sub (coords, oldcoords);
         targetErrPrev = targetErr;
         targetErr = computeDelCoords (targetDiff, targetCoords, coords);

         double delcCos = 0;
         if (icnt > 0 &&
            (delcCos=computeDelCoordCos(realDelPrev, realDel, 1e-5)) <
             MAX_REVERSE_COS) {
            numReverse++;
         }
         else {
            numReverse = 0;
         }

         if (debug) {
            printCoords ("       DEL=", realDel);
            printCoords ("       ERR=", targetDiff);
            System.out.printf (
               "       COS=%g targErr=%g prevTargErr=%g\n",
               delcCos, targetErr, targetErrPrev);
            printCoords ("    coords=", coords);
            checkDofs (reqs);
         }

         int limitChanges = updateLimitEngagement(limitEngagement);

         if (limitChanges != 0 || myKKTSolver.numPerturbedPivots() > 0) {
            if (debug) {
               System.out.println (
                  "    CHANGED ENGAGED " + new VectorNi(limitEngagement));
            }
            numFree = findIndependentCoords (
               myWeights, targetCoords, coords, reqs, -1);

            if (debug) {
               System.out.println ("    numFree=" + numFree);
            }
            
            if (debug || printCtrlChanges) {
               printCoordinateNames ("    joints=");
               printCoordinateCtrls ("     ctrls=");
               printCoords ("    TARGET=", targetCoords);
            }
            if (numFree == 0) {
               tol = 1e-10;
               maxIter = MAX_ITER;
            }
         }
         else if (numFree > 0 && numReverse > MAX_REVERSE_CNT &&
                  targetErr < targetErrPrev) {
            numFree = findIndependentCoords (
               myWeights, targetCoords, coords, reqs, numFree-1);
            if (numFree == 0) {
               tol = 1e-10;
               maxIter = MAX_ITER;
            }
         }

         if (numFree == 0) {
            myWeights.setZero();
         }
         myLastStepNorm = computeDqNorm(dq);
         if (debug || myLastStepNorm > 0.5) {
            System.out.printf ("    dqnorm=%g\n", myLastStepNorm);
         }       
         if (icnt > 0 && // icnt should be > 0 to allow for polish step
             myLastStepNorm <= tol) {
            status.setConverged (true);
         }
         dqPrev.set (dq);
         icnt++;
      }
      while (icnt < maxIter && !status.converged());
      status.addIterations (icnt);

      if (applyPolish && stepNum==numSteps && !status.converged()) {
         // didn't converge, but at least put model into a correct
         // configuration by relaxing all joint constraints
         unlockIndependentCoords (myWeights, reqs);
         int icntx = 0;
         maxIter = 10;
         do {
            getPosState (q);
            updateSolveMatrix (f, targetCoords, coords, /*lmdamping*/0.0);
            if (myAnalyze) {
               int matrixType = Matrix.SYMMETRIC;
               myKKTSolver.analyze (mySolveMatrix, velSize, myGT, myRg, matrixType);
               myAnalyze = false;
            }     
            myKKTSolver.factor (mySolveMatrix, velSize, myGT, myRg, myNT, myRn);
            myKKTSolver.solve (dq, myLam, myThe, f, myBg, myBn); 

            updatePosState (q, dq, 1.0);
            updateConstraints ();

            myLastStepNorm = computeDqNorm(dq);
            icnt++;
            icntx++;
         }
         while (icntx < maxIter && myLastStepNorm > tol);
         status.addIterations (icntx);
         if (debug) {
            System.out.println ("    polish Nsize=" + myNsize);
         }
      }
      
      // restore lock status of coordinates
      for (int k=0; k<reqs.size(); k++) {
         reqs.get(k).restoreLockSetting();
      }
      updateAttachmentPosStates();
      myNumIterations += icnt;
      if (numFree == 0) {
         // done - outer loop should terminate
         status.myFinished = true;
      }
      if (debug) {
         System.out.printf ("  done icnt=%d status=%s\n", icnt, status);
      }
      return status;
   }

   private void printCoords (String msg, VectorNd coords, String fmtStr) {
      VectorNd coordsDeg = new VectorNd(coords);

      int cidx = 0;
      for (JointCoordinateHandle handle : getCoordinateHandles()) {
         coordsDeg.set (cidx, handle.getValueDeg());
         cidx++;
      }
      System.out.println (msg + coordsDeg.toString (fmtStr));
   }

   private void printCoords (String msg, VectorNd coords) {
      printCoords (msg, coords, "%10.5f");
   }

   /**
    * Returns the columns of GT that are not associated with coordinate locking.
    */
   private int[] getNonLockingGTCols() {
      DynamicIntArray colIdxs = new DynamicIntArray();
      for (int k=0; k<myGsize; k++) {
         if (!myGInfo[k].coordLimit) {
            colIdxs.add (k);
         }
      }
      return colIdxs.toArray();
   }

   int numIterations() {
      return myNumIterations;
   }

   void finalizeStatus (SetStatus status) {
      int cidx = 0;
      for (JointCoordinateHandle handle : getCoordinateHandles()) {
         if (status.myStatusMap.get (handle) != null) {
            if (myCoordStatus[cidx] == SetStatus.LIMITED ||
                myCoordStatus[cidx] == SetStatus.UNKNOWN) {
               status.myStatusMap.put (handle, SetStatus.LIMITED);
            }
            else if (myCoordStatus[cidx] == SetStatus.DEPENDENT) {
               status.myStatusMap.put (handle, SetStatus.DEPENDENT);
            }
         }
         cidx++;
      }
   }


}
