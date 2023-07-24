package artisynth.core.opensim.customjoint;

import java.util.HashMap;

import artisynth.core.opensim.components.Coordinate;
import artisynth.core.opensim.components.FunctionBase;
import artisynth.core.opensim.components.TransformAxis;
import maspack.matrix.MatrixNd;
import maspack.matrix.VectorNi;
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
import maspack.util.DataBuffer;

/**
 * OpenSim Custom Joint coupling
 * - According to forums and documentation, OpenSim uses INTRINSIC rotations
 * - Translation is performed after rotation
 */
public class OpenSimCustomCoupling extends RigidBodyCoupling {

   protected Coordinate[] myCoords;
   protected TAxis[] myRotAxes;
   protected TAxis[] myTransAxes;

   // H matrix maps coordinate velocities onto spatial velocity of C wrt D
   protected MatrixNd myH; 
   // G wreches map spatial velocity of C wrt D onto coordinate velocities, QR
   // decomposition
   protected Wrench[] myG;

   // QR decomposition of H
   protected QRDecomposition myQRD; 
   protected MatrixNd myQ;

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
                  "No index found for coordinate "+coordsNames[i]);
            }
            myCidxs[i] = idx;
         }
      }

      /**
       * Apply translation for a translation axis
       */
      public void applyTranslation (Vector3d p, VectorNd coords) {
         FunctionBase func = myInfo.getFunction();
         if (func != null) {
            for (int i=0; i<myCvals.size(); i++) {
               myCvals.set (i, coords.get(myCidxs[i]));
            }
            double tval = func.eval(myCvals);
            p.scaledAdd (tval, myInfo.getAxis());
         }
      }     

      /**
       * For a translation axis, add the contribution to the v component of the
       * mobility vectors.
       */
      public void addToHv (MatrixNd H, VectorNd coords) {
         FunctionBase func = myInfo.getFunction();
         if (func != null) {
            for (int i=0; i<myCvals.size(); i++) {
               myCvals.set (i, coords.get(myCidxs[i]));
            }
            func.evalDeriv (myDvals, myCvals);
            Vector3d uaxis = myInfo.getAxis();
            for (int i=0; i<myDvals.size(); i++) {
               double dval = myDvals.get(i);
               int j = myCidxs[i];
               H.add (0, j, dval*uaxis.x);
               H.add (1, j, dval*uaxis.y);
               H.add (2, j, dval*uaxis.z);
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
            R.mulAxisAngle (myInfo.getAxis(), func.eval(myCvals));
         }
      }         

      /**
       * For a rotation axis, add the contribution to the w component of the
       * mobility vectors.
       */
      public void addToHw (
         MatrixNd H, RotationMatrix3d R, VectorNd coords, boolean last) {

         FunctionBase func = myInfo.getFunction();
         if (func != null) {
            for (int i=0; i<myCvals.size(); i++) {
               myCvals.set (i, coords.get(myCidxs[i]));
            }
            func.evalDeriv (myDvals, myCvals);
            Vector3d uaxis = new Vector3d();
            uaxis.transform (R, myInfo.getAxis());
            for (int i=0; i<myDvals.size(); i++) {
               double dval = myDvals.get(i);
               int j = myCidxs[i];
               H.add (3, j, dval*uaxis.x);
               H.add (4, j, dval*uaxis.y);
               H.add (5, j, dval*uaxis.z);
            }
            if (!last) {
               R.mulAxisAngle (myInfo.getAxis(), func.eval(myCvals));
            }
         }
      }

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
   public void initializeConstraints() {

      if (myCoords == null) {
         // XXX called in the RigidBodyCoupling constructor. Nothing to do
         // yet. Will be callled again in OpenSimCustomCoupling constructor
         return;
      }
      int numc = myCoords.length;
      int numb = 6 - numc;
      // add bilateral constraints
      for (int i=0; i<numb; i++) {
         addConstraint (BILATERAL);
      }
      // add unilateral constraints for coordinates
      for (int i=0; i<numc; i++) {
         addConstraint (0);
      }
      // create coordinates with associated constraints if needed
      int bidx = 6 - numc;
      for (int i=0; i<numc; i++) {
         Coordinate c = myCoords[i];
         CoordinateInfo cinfo;
         if (c.getClamped()) {
            // range limited
            cinfo = addCoordinate (
               c.getName(),
               c.getRange().getLowerBound(), 
               c.getRange().getUpperBound(), 
               0, getConstraint(bidx++));
         }
         else {
            cinfo = addCoordinate (
               c.getName(), -INF, INF, 0, getConstraint(bidx++));
         }
         if (c.getLocked()) {
            cinfo.setLocked (true);
         }
         cinfo.setValue (c.getDefaultValue());
      }
      myH = new MatrixNd (6, numc);
      myG = new Wrench[numc];
      for (int i=0; i<numc; i++) {
         myG[i] = new Wrench();
      }
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
      // start by computing H matrix
      myH.setZero();
      // rotation
      RotationMatrix3d R = new RotationMatrix3d(); // intermediate rotation
      for (int i=0; i<myRotAxes.length; i++) {
         myRotAxes[i].addToHw (
            myH, R, coords, /*last=*/i==myRotAxes.length-1);
      }
      // translation
      for (int i=0; i<myTransAxes.length; i++) {
         myTransAxes[i].addToHv (myH, coords);
      }

      myQRD.factorWithPivoting (myH);
      int[] perm = new int[numc];
      myQRD.get (myQ, null, perm);
      if (myQRD.rank (1e-8) < numc) {
         System.out.println (
            "WARNING: joint has rank "+myQRD.rank(1e-8)+" vs. " + numc);
         // System.out.println ("coupling=" + this);
         // System.out.println ("H=\n" + myH.toString ("%18.12f"));
         // MatrixNd RR = new MatrixNd();
         // myQRD.get (null, RR, null);
         // System.out.println ("R0=\n" + RR);
         // QRDecomposition qrd = new QRDecomposition(myH);
         // qrd.get (null, RR, null);
         // System.out.println ("R1=\n" + RR);
      }
      
      VectorNd gcol = new VectorNd(numc);
      for (int i=0; i<6; i++) {
         for (int j=0; j<numc; j++) {
            gcol.set (j, myQ.get(i, j));
         }
         myQRD.solveR (gcol, gcol);
         for (int j=0; j<numc; j++) {
            myG[j].set (i, gcol.get(j));
         }
      }
      // convert to G coords
      for (int j=0; j<numc; j++) {
         myG[j].inverseTransform (TGD.R);
      }

      // normalize and use them to set corresponding limit constraints
      Wrench wr = new Wrench(); 
      for (int j=0; j<numc; j++) {
         CoordinateInfo cinfo = getCoordinateInfo(j);
         if (cinfo.limitConstraint != null) {
            cinfo.limitConstraint.setWrenchG (myG[j]);
         }
      }
      if (numc < 6) {
         // non-coordinate constraints are given by the orthogonal complement
         // of H, which is given by the last numc-6 columns of Q
         for (int i=0; i<6-numc; i++) {
            RigidBodyConstraint cons = getConstraint(i);
            myQ.getColumn (numc+i, wr);
            wr.inverseTransform (TGD.R);
            cons.setWrenchG (wr);
         }
      }
      // System.out.println ("Updated constraints:");
      // for (int i=0; i<numConstraints(); i++) {
      //    RigidBodyConstraint cinfo = getConstraint(i);
      //    System.out.println (
      //       ""+i+" "+cinfo.getWrenchG().toString ("%8.5f") + " engaged=" +
      //       cinfo.getEngaged());
      // }
      myUpdateConstraintCnt = myCoordValueCnt;
   }

   @Override
   public void updateConstraints(
      RigidTransform3d TGD, RigidTransform3d TCD, Twist errC,
      Twist velGD, boolean updateEngaged) {

      // constraints are computed purely from the coordinates.
      updateConstraints(TGD);
   }


   private void doGetRpy (double[] rpy, RotationMatrix3d RDC) {

      Vector3d ang1 = new Vector3d();
      Vector3d ang2 = new Vector3d();
      Vector3d ang3 = new Vector3d();

      CoordinateInfo rcoord = myCoordinates.get(0); 
      CoordinateInfo pcoord = myCoordinates.get(1);
      CoordinateInfo ycoord = myCoordinates.get(2);

      ang1.x = rcoord.getValue(); // roll
      ang1.y = pcoord.getValue(); // pitch
      ang1.z = ycoord.getValue(); // yaw
      
      double[] rpyTrimmed = new double[3];
      rpyTrimmed[0] = rcoord.clipToRange (ang1.x);
      rpyTrimmed[1] = pcoord.clipToRange (ang1.y);
      rpyTrimmed[2] = ycoord.clipToRange (ang1.z);

      
      RDC.getRpy(rpy);

      ang2.set (rpy);

      // // adjust so that all angles as close as possible to mid-range
      // if (applyEuler) {
      //    // adjust so that all angles as close as possible to original angles
      //    EulerFilter.filter(rpyTrimmed, rpy, 1e-2, rpy);
      //    //       EulerFilter.filter(midRange, rpy, EPSILON, rpy);
      // } else {
         rpy[0] = findNearestAngle (ang1.x, rpy[0]);
         rpy[1] = findNearestAngle (ang1.y, rpy[1]);
         rpy[2] = findNearestAngle (ang1.z, rpy[2]);
         //}
      
      if (Math.abs(rpy[0]-ang1.x) > Math.PI/2 ) {
         System.out.println (
            "SphericalRpyCoupling: roll more that PI/2 from previous value");
      }
      ang3.set (rpy);

      Vector3d diff = new Vector3d();
      diff.sub (ang3, ang1);
      if (diff.norm() > Math.PI/4) {
         ang1.scale (RTOD);
         System.out.println ("deg1=" + ang1.toString ("%10.5f"));
         ang2.scale (RTOD);
         System.out.println ("deg2=" + ang2.toString ("%10.5f"));
         ang3.scale (RTOD);
         System.out.println ("deg3=" + ang3.toString ("%10.5f"));
         System.out.println ("");
      }
   }

   public void TCDToCoordinates (VectorNd coords, RigidTransform3d TCD) {
      double[] rpy = new double[3];
      RotationMatrix3d R = new RotationMatrix3d();

      R.set(TCD.R);
      doGetRpy(rpy, R);
      coords.set(0, rpy[0]);
      coords.set(1, rpy[1]);
      coords.set(2, rpy[2]);
   }


   @Override
   public void projectToConstraints(
      RigidTransform3d TGD, RigidTransform3d TCD, VectorNd coords) {

      if (TCD.containsNaN()) {
         System.out.println ("BAD TCD=\n" + TCD.toString("%10.5f"));
      }
      
      int numc = numCoordinates();
      // projection is based on current coordinate values, so we need these
      // regardless:
      if (coords == null) {
         coords = new VectorNd(numc);
      }
      doGetCoords (coords);
      coordinatesToTCD (TGD, coords);

      if (numc == 6) {
         // nothing more to do; there are no independent constraints
         return;
      }

      // find differential displacement del from TGD to TCD:
      Twist del = new Twist();
      RigidTransform3d TDEL = new RigidTransform3d ();
      TDEL.mulInverseLeft (TGD, TCD);
      del.set (TDEL);

      // make sure QR decomposition is updated
      if (myUpdateConstraintCnt != myCoordValueCnt) {
         updateConstraints(TGD);
      }
      VectorNd delq = new VectorNd(numc);
      boolean changed = false;
      for (int j=0; j<numc; j++) {
         if (myCoords[j].getLocked()) {
            delq.set (j, 0);
         }
         else {
            double dq = myG[j].dot (del);
            if (dq != 0) {
               delq.set (j, dq);
               changed = true;
            }
         }
      }
      if (changed) {
         coords.add (delq);
         coordinatesToTCD (TGD, coords);
      }

      // TGD.R.set(TCD.R);
      // TGD.p.setZero();
      // if (coords != null) {
      //    TCDToCoordinates (coords, TGD);
      // }  
      
   }

   // /**
   //  * {@inheritDoc}
   //  */
   // public void coordinatesToTCD (
   //    RigidTransform3d TCD, VectorNd coords) {
   //    coordinatesToTCD (TCD);
   // }

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

   public Wrench getCoordinateWrenchG (int idx) {
      return myG[idx];
   }
   
   public int getCoordIndex (String name) {
      for (int i=0; i<myCoords.length; i++) {
         if (myCoords[i].getName().equals(name)) {
            return i;
         }
      }
      return -1;
   }

}
