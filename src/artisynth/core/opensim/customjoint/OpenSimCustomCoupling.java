package artisynth.core.opensim.customjoint;

import java.util.HashMap;

import artisynth.core.opensim.components.Coordinate;
import artisynth.core.opensim.components.FunctionBase;
import artisynth.core.opensim.components.TransformAxis;
import maspack.matrix.MatrixNd;
import maspack.matrix.QRDecomposition;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.spatialmotion.RigidBodyConstraint;
import maspack.spatialmotion.RigidBodyCoupling;
import maspack.spatialmotion.Twist;
import maspack.spatialmotion.Wrench;
import maspack.util.InternalErrorException;

/**
 * OpenSim Custom Joint coupling
 * - According to forums and documentation, OpenSim uses INTRINSIC rotations
 * - Tranlation is performed after rotation
 *
 */
public class OpenSimCustomCoupling extends RigidBodyCoupling {

   protected Coordinate[] myCoords;
   protected TAxis[] myRotAxes;
   protected TAxis[] myTransAxes;

   protected MatrixNd myH;
   protected Twist[] myHVecs;
   protected MatrixNd myQ;
   protected QRDecomposition myQRD;

   int myNumLocked = 0; // number of locked coordinates
   int myNumClamped = 0; // number of clamped (range limited) coordinates

   // value of myCoordValueCnt when constraints are last updated;
   // used to see if we need to update constraints
   int myUpdateConstraintCnt;

   // Wrapper for TransformAxis that maps the entire set of generalized
   // coordinates to those specifically used for the axis function.
   public static class TAxis {
      TransformAxis myInfo;

      VectorNd myCvals; // local coordinate values
      VectorNd myDvals; // local derivative values
      int[] myCidxs; // local coordinate indices

      public TAxis (TransformAxis info, HashMap<String,Integer> cmap) {
         myInfo = info;
         String[] coordsNames = info.getCoordinates ();
         int len = 0;
         if (coordsNames != null) {
            len = coordsNames.length;
         }
         myCvals = new VectorNd (len);
         myDvals = new VectorNd (len);
         myCidxs = new int[len];
         for (int i=0; i<len; ++i) {
            Integer idx = cmap.get(coordsNames[i]);
            if (idx == null) {
               throw new InternalErrorException (
                  "No index foound for coordinate "+coordsNames[i]);
            }
            myCidxs[i] = idx;
         }
      }

      // public double eval (VectorNd coords) {
      //    FunctionBase func = myInfo.getFunction();
      //    if (func != null) {
      //       for (int i=0; i<myCvals.size(); i++) {
      //          myCvals.set (i, coords.get(myCidxs[i]));
      //       }
      //       return func.evaluate(myCvals);
      //    }
      //    else {
      //       return 0;
      //    }
      // }

      /**
       * Apply translation for a translation axis
       */
      public void applyTranslation (Vector3d p, VectorNd coords) {
         FunctionBase func = myInfo.getFunction();
         if (func != null) {
            for (int i=0; i<myCvals.size(); i++) {
               myCvals.set (i, coords.get(myCidxs[i]));
            }
            double tval = func.evaluate(myCvals);
            p.scaledAdd (tval, myInfo.getAxis());
         }
      }     

      /**
       * For a translation axis, add the contribution to the v component of the
       * mobility vectors.
       */
      public void addToHv (Twist[] H, VectorNd coords) {
         FunctionBase func = myInfo.getFunction();
         if (func != null) {
            for (int i=0; i<myCvals.size(); i++) {
               myCvals.set (i, coords.get(myCidxs[i]));
            }
            func.evaluateDerivative (myCvals, myDvals);
            for (int i=0; i<myDvals.size(); i++) {
               H[myCidxs[i]].v.scaledAdd (myDvals.get(i), myInfo.getAxis());
            }
         }
      }

      /**
       * Apply rotation for a rotation axis
       */
      public void applyRotation (RotationMatrix3d R, VectorNd coords) {
         FunctionBase func = myInfo.getFunction();
         if (func != null) {
            for (int i=0; i<myCvals.size(); i++) {
               myCvals.set (i, coords.get(myCidxs[i]));
            }
            R.mulAxisAngle (myInfo.getAxis(), func.evaluate(myCvals));
         }
      }         

      /**
       * For a rotation axis, add the contribution to the w component of the
       * mobility vectors.
       */
      public void addToHw (
         Twist[] H, RotationMatrix3d R, VectorNd coords, boolean last) {

         FunctionBase func = myInfo.getFunction();
         if (func != null) {
            for (int i=0; i<myCvals.size(); i++) {
               myCvals.set (i, coords.get(myCidxs[i]));
            }
            func.evaluateDerivative (myCvals, myDvals);
            Vector3d uvec = new Vector3d();
            uvec.transform (R, myInfo.getAxis());
            for (int i=0; i<myDvals.size(); i++) {
               H[myCidxs[i]].w.scaledAdd (myDvals.get(i), uvec);
            }
            if (!last) {
               R.mulAxisAngle (myInfo.getAxis(), func.evaluate(myCvals));
            }
         }
      }

      // // return true if the derivative is non-zero
      // public boolean evalDeriv (VectorNd coords, VectorNd deriv) {
      //    FunctionBase func = myInfo.getFunction();
      //    deriv.setZero ();
      //    boolean nonZero = false;
      //    if (func != null) {
      //       for (int i=0; i<myCvals.size(); i++) {
      //          myCvals.set (i, coords.get(myCidxs[i]));
      //       }
      //       func.evaluateDerivative (myCvals, myDvals);
      //       for (int i=0; i<myDvals.size(); i++) {
      //          double dval = myDvals.get(i);
      //          if (dval != 0) {
      //             deriv.set (myCidxs[i], dval);
      //             nonZero = true;
      //          }
      //       }
      //    }
      //    return nonZero;
      // }

      public Vector3d getAxis() {
         return myInfo.getAxis();
      }
   }
   
   public OpenSimCustomCoupling (TransformAxis[] axes, Coordinate[] coords) {

      myCoords = new Coordinate[coords.length];
      HashMap<String,Integer> cmap = new HashMap<>();
      for (int i=0; i<coords.length; i++) {
         myCoords[i] = coords[i];
         // note: coordinates can be both clamped and locked; locked takes
         // priority
         if (coords[i].getLocked()) {
            myNumLocked++;
         }
         else if (coords[i].getClamped()) {
            myNumClamped++;
         }
         cmap.put (coords[i].getName(), i);
      }
      if (axes.length != 6) {
         throw new InternalErrorException (
            "expected 6 axes, got "+axes.length);
      }
      myRotAxes = new TAxis[3];
      myTransAxes = new TAxis[3];
      for (int i=0; i<3; i++) {
         myRotAxes[i] = new TAxis (axes[i], cmap);
         myTransAxes[i] = new TAxis (axes[i+3], cmap);
      }
      initializeConstraintInfo();
   }

   @Override
   public int numBilaterals() {
      return 6 - numCoordinates() + myNumLocked;
   }

   @Override
   public int numUnilaterals() {
      return myNumClamped;
   }

   @Override
   public void initializeConstraints() {

      if (myCoords == null) {
         // XXX called in the RigidBodyCoupling constructor. Nothing to do
         // yet. Will be callled again in OpenSimCustomCoupling constructor
         return;
      }
      int numc = myCoords.length;
      int numb = 6 - numc + myNumLocked;
      // add bilateral constraints
      for (int i=0; i<numb; i++) {
         addConstraint (BILATERAL);
      }
      // add unilateral constraints
      for (int i=0; i<myNumClamped; i++) {
         addConstraint (0);
      }
      // create coordinates with associated constraints if needed
      int bidx = 6 - numc;
      int uidx = numb;
      myHVecs = new Twist[numc];
      for (int i=0; i<numc; i++) {
         Coordinate c = myCoords[i];
         CoordinateInfo cinfo;
         if (c.getLocked()) {
            // coordinate is locked
            cinfo = addCoordinate (-INF, INF, 0, getConstraint(bidx++));
         }
         else if (c.getClamped()) {
            // range limited
            cinfo = addCoordinate (
               c.getRange().getLowerBound(), 
               c.getRange().getUpperBound(), 
               0, getConstraint(uidx++));
         }
         else {
            cinfo = addCoordinate ();
         }
         cinfo.setValue (c.getDefaultValue());
         myHVecs[i] = new Twist();
      }
      myH = new MatrixNd (6, numc);
      myQ = new MatrixNd (6, 6);
      myQRD = new QRDecomposition();

      updateConstraints(new RigidTransform3d());
      // automatically set LINEAR/ROTARY flags based on whether the wrench
      // constraint at the 0 position has a larger v or m component.
      for (int i=0; i<numConstraints(); i++) {
         RigidBodyConstraint cons = getConstraint(i);
         Wrench wr = cons.getWrenchG();
         if (wr.f.norm() >= wr.m.norm()) {
            cons.setFlag (LINEAR);
         }
         else {
            cons.setFlag (ROTARY);
         }
      }
   }
   
   private void updateConstraints (RigidTransform3d TGD) {
      int numc = numCoordinates();
      VectorNd coords = new VectorNd(numc);
      doGetCoords (coords);
      
      // start by computing mobility vectors (myHVecs)
      for (int j=0; j<numc; j++) {
         myHVecs[j].setZero();
      }
      // rotation
      RotationMatrix3d R = new RotationMatrix3d(); // intermediate rotation
      for (int i=0; i<myRotAxes.length; i++) {
         myRotAxes[i].addToHw (
            myHVecs, R, coords, /*last=*/i==myRotAxes.length-1);
      }
      // translation
      for (int i=0; i<myTransAxes.length; i++) {
         myTransAxes[i].addToHv (myHVecs, coords);
      }

      // normalize and use them to set corresponding limit constraints
      Wrench wr = new Wrench(); 
      for (int j=0; j<numc; j++) {
         myHVecs[j].normalize();
         CoordinateInfo cinfo = getCoordinate(j);
         if (cinfo.limitConstraint != null) {
            Twist Hj =  myHVecs[j];
            wr.set (Hj.v, Hj.w);
            wr.inverseTransform (TGD);
            cinfo.limitConstraint.setWrenchG (wr);
         }
      }
      if (numc < 6) {
         // non-coordinate constraints are given by the orthogonal complement
         // of H, which we find using the QR decomposition
         for (int j=0; j<numc; j++) {
            myH.setColumn (j, myHVecs[j]);
         }
         myQRD.factor (myH);
         myQRD.get (myQ, null);
         for (int i=0; i<6-numc; i++) {
            RigidBodyConstraint cons = getConstraint(i);
            myQ.getColumn (numc+i, wr);
            wr.inverseTransform (TGD);
            cons.setWrenchG (wr);
         }
      }
      myUpdateConstraintCnt = myCoordValueCnt;
   }

   @Override
   public void updateConstraints(
      RigidTransform3d TGD, RigidTransform3d TCD, Twist errC,
      Twist velGD, boolean updateEngaged) {

      // constraints are computed purely from the coordinates.
      updateConstraints(TGD);
   }

   @Override
   public void projectToConstraints(
      RigidTransform3d TGD, RigidTransform3d TCD, VectorNd coords) {

      int numc = numCoordinates();
      if (numc == 6) {
         // nothing to do; there are no independent constraints
         return;
      }
      
      // projection is based on current coordinate values, so we need these
      // regardless:

      if (coords == null) {
         coords = new VectorNd();
      }
      doGetCoords (coords);
      coordinatesToTCD (TGD, coords);
      // find differential displacement del from TGD to TCD:
      Twist del = new Twist();
      RigidTransform3d TDEL = new RigidTransform3d ();
      TDEL.mulInverseLeft (TGD, TCD);
      del.set (TDEL);

      // make sure QR decomposition is updated
      if (myUpdateConstraintCnt != myCoordValueCnt) {
         updateConstraints(TGD);
      }
      VectorNd dq = new VectorNd(numc);
      myQRD.solve (dq, del);
      boolean changed = false;
      for (int j=0; j<numc; j++) {
         if (myCoords[j].getLocked()) {
            dq.set (j, 0);
         }
         else if (dq.get(j) != 0) {
            changed = true;
         }
      }
      if (changed) {
         coords.add (dq);
         coordinatesToTCD (TGD, coords);
      }
   }

   // /**
   //  * {@inheritDoc}
   //  */
   // public void coordinatesToTCD (
   //    RigidTransform3d TCD, VectorNd coords) {
   //    coordinatesToTCD (TCD);
   // }

   public boolean debug = false;

   @Override
   public void coordinatesToTCD (RigidTransform3d TCD, VectorNd coords) {
      TCD.setIdentity();
      // rotation
      for (int i=0; i<myRotAxes.length; i++) {
         myRotAxes[i].applyRotation (TCD.R, coords);
      }
      // translation
      for (int i=0; i<myTransAxes.length; i++) {
         myTransAxes[i].applyTranslation (TCD.p, coords);
      }
   }
}
