/**
 * Copyright (c) 2014, by the Authors: Ian Stavness (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.inverse;

import java.awt.Color;
import java.util.ArrayList;

import artisynth.core.femmodels.FemModel3d;
import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.MotionTarget.TargetActivity;
import artisynth.core.util.TimeBase;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.MatrixNd;
import maspack.matrix.EigenDecomposition;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.matrix.Matrix;
import maspack.util.ArraySort;
import maspack.util.BooleanHolder;
import maspack.properties.PropertyList;
import maspack.render.RenderProps;
import maspack.render.Renderer.FaceStyle;
import maspack.render.Renderer.PointStyle;
import maspack.spatialmotion.Twist;

/**
 * Cost term that maximizes the stabilty of a mechanical system.  Can also be
 * employed as a constraint term.
 * 
 * @author John E Lloyd
 */
public class StabilityTerm extends LeastSquaresTermBase {

   boolean debug = false;

   public enum StiffnessType {
      FULL,
      SYMMETRIC,
      SYMPART_FULL
   };

   public static final boolean DEFAULT_USE_SYMMETRIC_PART = true;
   boolean myUseSymmetricPart = DEFAULT_USE_SYMMETRIC_PART;

   public static final StiffnessType DEFAULT_STIFFNESS_TYPE =
      StiffnessType.SYMMETRIC;
   StiffnessType myStiffnessType = DEFAULT_STIFFNESS_TYPE;

   // property attributes

   // other attributes

   protected ArrayList<SolveMatrixModifier> myModifiers =
      new ArrayList<>();

   // quantities used in the computation which are allocated on demand
   
   protected VectorNd myBs = new VectorNd();
   protected MatrixNd myHs = new MatrixNd();
   protected double myDet = 0;
   // delta excitation for numerical derivative:
   protected double myDeltaEx = 0.001;

   public static final boolean DEFAULT_IGNORE_POSITION = false;
   protected boolean myIgnorePosition = DEFAULT_IGNORE_POSITION;

   protected static final double DEFAULT_DET_TARGET = 0.0;
   protected double myDetTarget = DEFAULT_DET_TARGET;

   public static PropertyList myProps =
      new PropertyList(StabilityTerm.class, LeastSquaresTermBase.class);

   static {
      myProps.addReadOnly (
         "det", "determinant of the system");
      myProps.add (
         "detTarget",
         "target determinant for the system", DEFAULT_DET_TARGET);
      myProps.add (
         "ignorePosition",
         "ignore position variation when computing the determinant derivative",
         DEFAULT_IGNORE_POSITION);
      myProps.add (
         "useSymmetricPart",
         "use only the symmetric part of the stiffness matrix",
         DEFAULT_USE_SYMMETRIC_PART);
      myProps.add (
         "stiffnessType", "what type of stiffness to use for control",
         DEFAULT_STIFFNESS_TYPE);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public StabilityTerm () {
      setType (Type.INEQUALITY);
   }

   public double getDet() {
      return myDet;
   }

   public double getDetTarget() {
      return myDetTarget;
   }

   public void setDetTarget (double target) {
      myDetTarget = target;
   }

   public boolean getIgnorePosition () {
      return myIgnorePosition;
   }

   public void setIgnorePosition (boolean ignore) {
      myIgnorePosition = ignore;
   }

   public void addStiffnessModifier (SolveMatrixModifier modifier) {
      myModifiers.add (modifier);
   }
   
   public boolean removeStiffnessModifier (SolveMatrixModifier modifier) {
      return myModifiers.remove (modifier);
   }
   
   public void clearStiffnessModifiers () {
      myModifiers.clear();
   }
   
   public SolveMatrixModifier getStiffnessModifier (int idx) {
      return myModifiers.get (idx);
   }
   
   public int numStiffnessModifiers () {
      return myModifiers.size();
   }

   public boolean getUseSymmetricPart() {
      return myUseSymmetricPart;
   }
   
   public void setUseSymmetricPart (boolean enable) {
      myUseSymmetricPart = enable;
   }
   
   public StiffnessType getStiffnessType() {
      return myStiffnessType;
   }
   
   public void setStiffnessType (StiffnessType type) {
      myStiffnessType = type;
   }
   
   /**
    * Fills <code>H</code> and <code>b</code> with this motion term
    * @param A LHS matrix to fill
    * @param b RHS vector to fill
    * @param rowoff row offset to start filling term
    * @param t0 starting time of time step
    * @param t1 ending time of time step
    * @return next row offset
    */
   public int getTerm(
      MatrixNd A, VectorNd b, int rowoff, double t0, double t1) {

      TrackingController controller = getController();
      if (controller != null) {
         updateHs (controller, t0, t1);
         A.setSubMatrix(rowoff, 0, myHs);
         b.setSubVector(rowoff, myBs);
         rowoff += myHs.rowSize();
      }
      return rowoff;
   }      


   /**
    * Returns the negated stiffness matrix, determined from the MechSystem
    * according to the stiffness type and the modifiers (if any).
    */
   public static Matrix getStiffnessMatrix (
      MechSystemBase mech, StiffnessType type,
      ArrayList<SolveMatrixModifier> modifiers) {

      SparseBlockMatrix K;
      switch (type) {
         case FULL: {
            if (mech instanceof MechModel) {
               K = ((MechModel)mech).getYPRStiffnessMatrix(modifiers);
            }
            else {
               K = mech.getActiveStiffnessMatrix();
            }
            K.negate();
            return K;
         }
         case SYMPART_FULL: {
            if (mech instanceof MechModel) {
               K = ((MechModel)mech).getYPRStiffnessMatrix(modifiers);
            }
            else {
               K = mech.getActiveStiffnessMatrix();
            }
            K.negate();
            MatrixNd Ksym = new MatrixNd (K);
            MatrixNd KT = new MatrixNd();
            KT.transpose (Ksym);
            Ksym.add (KT);
            Ksym.scale (0.5);
            return Ksym;
         }
         case SYMMETRIC: {
            if (mech instanceof MechModel) {
               K = ((MechModel)mech).getStiffnessMatrix(modifiers);
            }
            else {
               K = mech.getActiveStiffnessMatrix();
            }
            K.negate();
            return K;
         }
         default: {
            throw new UnsupportedOperationException (
               "Unimplemented stiffness type " + type);
         }
      }
   }

   private double computeDet (
      MechSystemBase mech, BooleanHolder posDef, boolean debug) {

      Matrix K = getStiffnessMatrix (mech, myStiffnessType, myModifiers);
      EigenDecomposition ed = new EigenDecomposition();
      ed.factor (K);

      if (debug) {
         VectorNd eigs = ed.getEigReal();
         ArraySort.quickSort (eigs.getBuffer());
         System.out.println ("eigs: " + eigs.toString("%12.6f"));
      }
      if (posDef != null) {
         int nump = 0;
         VectorNd eigs = ed.getEigReal();
         for (int i=0; i<eigs.size(); i++) {
            if (eigs.get(i) > 0) {
               nump++;
            }
         }
         posDef.value = (nump == eigs.size());
      }
      return ed.determinant();
   }

   public void updateHs (TrackingController controller, double t0, double t1) {
      double h = TimeBase.round(t1 - t0);

      int numex = controller.numExciters();
      boolean incremental = controller.getComputeIncrementally();

      myBs.setSize (1);
      myHs.setSize (1, numex);
      MechSystemBase mech = controller.getMech();
      VectorNd curEx = new VectorNd (numex);
      controller.getExcitations (curEx, 0);
      VectorNd ex = new VectorNd (numex);

      // position state vectors - allocated if needed
      VectorNd q0 = null;  // current positions
      VectorNd qa = null;  // positions with no change in activation
      VectorNd qj = null;  // positions with small delta activation change in j
      
      if (!myIgnorePosition) {
         q0 = new VectorNd (mech.getActivePosStateSize());
         qa = new VectorNd (mech.getActivePosStateSize());
         qj = new VectorNd (mech.getActivePosStateSize());

         mech.getActivePosState (q0);
         qa.set (q0);
         VectorNd ua; // next velocity with no change in activation
         if (incremental) {
            ua = controller.getU0();
         }
         else {
            ua = new VectorNd (mech.getActiveVelStateSize());
            ua.set (controller.getU0());
            for (int j=0; j<numex; j++) {
               ua.scaledAdd (curEx.get(j), controller.getHuCol(j));
            }
         }
         if (controller.useTrapezoidalSolver()) {
            VectorNd u0 = new VectorNd (mech.getActiveVelStateSize());
            mech.getActiveVelState (u0);            
            mech.addActivePosImpulse (qa, h/2, ua);
            mech.addActivePosImpulse (qa, h/2, u0);
         }
         else {
            mech.addActivePosImpulse (qa, h, ua);
         }
         mech.setActivePosState (qa);
      }

      BooleanHolder posDef = new BooleanHolder();
      double det0 = computeDet (mech, posDef, true);
      //System.out.println ("det0=" + det0);
      //System.out.println ("ex=" + curEx.toString ("%12.9f"));
      for (int j = 0; j < numex; j++) {
         double dex = myDeltaEx;
         double ej = curEx.get(j);
         // XXX if ej + dex > 1 need to take derivative in other direction
         // becauses of excitation clipping in some components
         if (ej + dex > 1.0) {
            dex = -dex;
         }
         if (!myIgnorePosition) {
            qj.set (qa);
            if (controller.useTrapezoidalSolver()) {
               mech.addActivePosImpulse (qj, h/2*dex, controller.getHuCol(j));
            }
            else {
               mech.addActivePosImpulse (qj, h*dex, controller.getHuCol(j));
            }
            mech.setActivePosState (qj);            
         }
         ex.set (curEx);
         ex.set (j, ej+dex);
         controller.setExcitations (ex, 0);
         double detn = computeDet (mech, null, false);
         myHs.set (0, j, (detn-det0)/dex);
      }

      if (myDetTarget > 0 && posDef.value) {

         if (!incremental) {
            myBs.mul (myHs, curEx);
            myBs.add (0, myDetTarget - det0);
         }
         else {
            myBs.set (0, myDetTarget - det0);
         }

         //System.out.println ("Hs=\n" + myHs.toString ("%16.8f"));
         if (myWeight >= 0 && myWeight != 1.0) {
            myHs.scale(myWeight);
            myBs.scale(myWeight);
         }
         //System.out.println ("weight=" + myWeight);
         //System.out.println ("bs=" + myBs);
      }
      else {
         // make non-active:
         myHs.setZero();
         myBs.set (0, -10.0);
         //myHs.set (0, 0, 1.0);
      }
      myDet = det0;

      if (!myIgnorePosition) {
         // reset positions
         mech.getActivePosState (q0);
      }
      // reset excitations
      controller.setExcitations(curEx, 0);
   }
   
   public MatrixNd getH() {
      return myHs;
   }

   public VectorNd getB() {
      return myBs;
   }

   /**
    * Weight used to scale the contribution of this term in the quadratic
    * optimization problem
    */
   @Override
   public void setWeight(double w) {
      super.setWeight(w);
   }
   
   @Override
   public void getQP (MatrixNd Q, VectorNd p, double t0, double t1) {
      TrackingController controller = getController();
      if (controller != null) {  
         updateHs (controller, t0, t1);
         computeAndAddQP (Q, p, myHs, myBs);
      }
   }

   @Override
   public int numConstraints (int qpsize) {
      return 1;
   }
}
