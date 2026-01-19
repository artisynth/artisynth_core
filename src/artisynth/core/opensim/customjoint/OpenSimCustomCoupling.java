package artisynth.core.opensim.customjoint;

import java.io.*;
import java.util.HashMap;
import java.util.ArrayList;

import artisynth.core.opensim.components.Coordinate;
import artisynth.core.opensim.components.FunctionBase;
import artisynth.core.opensim.components.TransformAxis;
import artisynth.core.modelbase.ScanWriteUtils;
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
import maspack.util.*;
import maspack.function.*;

/**
 * OpenSim Custom Joint coupling
 * - According to forums and documentation, OpenSim uses INTRINSIC rotations
 * - Translation is performed after rotation
 */
public class OpenSimCustomCoupling
   extends RigidBodyCoupling implements Scannable {

   private static final double INF = Double.POSITIVE_INFINITY;

   protected Coordinate[] myCoords; // only used by initializeConstraints()
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
      Vector3d myAxis;
      Diff1FunctionNx1 myFunction;
      VectorNd myCvals; // local coordinate values
      VectorNd myDvals; // local derivative values
      int[] myCidxs; // local coordinate indices

      /**
       * No-args constructor needed for scan/write.
       */
      public TAxis() {
      }

      public TAxis (TransformAxis info, HashMap<String,Integer> cmap) {
         myInfo = info;
         myAxis = info.getAxis();
         FunctionBase func = info.getFunction();
         if (func != null) {
            myFunction = func.getFunction();
         }
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
         if (myFunction != null) {
            for (int i=0; i<myCvals.size(); i++) {
               myCvals.set (i, coords.get(myCidxs[i]));
            }
            double tval = myFunction.eval(myCvals);
            p.scaledAdd (tval, myAxis);
         }
      }     

      /**
       * For a translation axis, add the contribution to the v component of the
       * mobility vectors.
       */
      public void addToHv (MatrixNd H, VectorNd coords) {
         if (myFunction != null) {
            for (int i=0; i<myCvals.size(); i++) {
               myCvals.set (i, coords.get(myCidxs[i]));
            }
            myFunction.evalDeriv (myDvals, myCvals);
            Vector3d uaxis = myAxis;
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
         if (myFunction != null) {
            for (int i=0; i<myCvals.size(); i++) {
               myCvals.set (i, coords.get(myCidxs[i]));
            }
            R.mulAxisAngle (myAxis, myFunction.eval(myCvals));
         }
      }         

      /**
       * For a rotation axis, add the contribution to the w component of the
       * mobility vectors.
       */
      public void addToHw (
         MatrixNd H, RotationMatrix3d R, VectorNd coords, boolean last) {

         if (myFunction != null) {
            for (int i=0; i<myCvals.size(); i++) {
               myCvals.set (i, coords.get(myCidxs[i]));
            }
            myFunction.evalDeriv (myDvals, myCvals);
            Vector3d uaxis = new Vector3d();
            uaxis.transform (R, myAxis);
            for (int i=0; i<myDvals.size(); i++) {
               double dval = myDvals.get(i);
               int j = myCidxs[i];
               H.add (3, j, dval*uaxis.x);
               H.add (4, j, dval*uaxis.y);
               H.add (5, j, dval*uaxis.z);
            }
            if (!last) {
               R.mulAxisAngle (myAxis, myFunction.eval(myCvals));
            }
         }
      }

      public Vector3d getAxis() {
         return myAxis;
      }

      private void scanAttributeName (
         ReaderTokenizer rtok, String name) throws IOException {
         rtok.scanWord (name);
         rtok.scanToken ('=');
      }

      public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
         rtok.scanToken ('[');
         scanAttributeName (rtok, "coordIdxs");
         rtok.scanToken ('[');
         DynamicIntArray idxs = new DynamicIntArray();
         while (rtok.nextToken() != ']') {
            if (!rtok.tokenIsInteger()) {
               throw new IOException ("coordinate index expected, "+rtok);
            }
            idxs.add ((int)rtok.lval);
         }
         myCidxs = idxs.getArray();
         scanAttributeName (rtok, "axis");
         myAxis = new Vector3d();
         myAxis.scan (rtok);
         scanAttributeName (rtok, "function");
         myFunction = FunctionUtils.scan (rtok, Diff1FunctionNx1.class);
         rtok.scanToken (']');

         int nc = myCidxs.length;
         myCvals = new VectorNd (nc);
         myDvals = new VectorNd (nc);
      }

      public void write (PrintWriter pw, NumberFormat fmt, Object ref)
         throws IOException {

         IndentingPrintWriter.printOpening (pw, "[ ");
         IndentingPrintWriter.addIndentation (pw, 2);
         pw.println ("coordIdxs=[" + new VectorNi(myCidxs) + "]");
         pw.print ("axis=");
         myAxis.write (pw, fmt, /*withBrackets=*/true);
         pw.println ("");
         pw.print ("function=");
         FunctionUtils.write (pw, myFunction, fmt);
         IndentingPrintWriter.addIndentation (pw, -2);
         pw.println ("]");
      }
   }
   
    /**
     * No-args constructor needed for scan/write.
     */
   public OpenSimCustomCoupling () {
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
         // yet. Will be called again in OpenSimCustomCoupling constructor
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
         cinfo.setValue (c.getDefaultValue());
         if (c.getLocked()) {
            cinfo.setLocked (true);
         }       
      }
      myH = new MatrixNd (6, numc);
      myG = new Wrench[numc];
      for (int i=0; i<numc; i++) {
         myG[i] = new Wrench();
      }
      myQ = new MatrixNd (6, 6);
      myQRD = new QRDecomposition();

      RigidTransform3d TGD = new RigidTransform3d();
      VectorNd coords = new VectorNd(numc);
      doGetCoords (coords);  
      coordinatesToTCD (TGD, coords);
      updateConstraints(TGD);
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

      // Normalize and use them to set corresponding limit constraints.
      // Also set coordinate twists from the columns of H.
      Wrench wr = new Wrench(); 
      Twist tw = new Twist(); 
      for (int j=0; j<numc; j++) {
         CoordinateInfo cinfo = getCoordinateInfo(j);
         if (cinfo.limitConstraint != null) {
            cinfo.limitConstraint.setWrenchG (myG[j]);
         }
         myH.getColumn (j, tw);
         tw.inverseTransform (TGD.R);
         setCoordinateTwist (j, tw);
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

   public void TCDToCoordinates (VectorNd coords, RigidTransform3d TCD) {

      RigidTransform3d TGD = new  RigidTransform3d();      
      int numc = numCoordinates();
      coords.setSize (numc);
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
      VectorNd delq = new VectorNd(numc);
      boolean changed = false;
      for (int j=0; j<numc; j++) {
         double dq = myG[j].dot (del);
         if (dq != 0) {
            delq.set (j, dq);
            changed = true;
         }
      }
      if (changed) {
         coords.add (delq);
      }
   }

   @Override
   public void projectToConstraints(
      RigidTransform3d TGD, RigidTransform3d TCD, VectorNd coords) {

      if (TCD == TGD) {
         TCD = new RigidTransform3d (TGD);
      }
      
      int numc = numCoordinates();
      // projection is based on current coordinate values, so we need these
      // regardless:
      if (coords == null) {
         coords = new VectorNd(numc);
      }
      doGetCoords (coords);
      coordinatesToTCD (TGD, coords);

      // we used to return when numc == 6, since there are no independant
      // constraints, but this doesn't account for joint limits and also
      // doesn't allow coordinate values to be updated.
      // if (numc == 6) {
      //    return;
      // }

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
//       John Lloyd, Oct 2025: if coordinate is locked, need to update
//       value anyway so we can compare is against the locked distance
//       if (isCoordinateLocked(j)) {
//          delq.set (j, 0);
//       }
         double dq = myG[j].dot (del);
         if (dq != 0) {
            delq.set (j, dq);
            changed = true;
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

   private void setCoordForWriting (
      Coordinate coord, CoordinateInfo cinfo) {

      coord.setName (cinfo.getName());
      coord.setDefaultValue (cinfo.getValue());
      coord.setLocked (cinfo.isLocked());      
      Range range = cinfo.getRange();
      coord.setRange (cinfo.getRange());
      if (cinfo.hasRestrictedRange()) {
         coord.setClamped (true);
      }
      switch (cinfo.getMotionType()) {
         case LINEAR: {
            coord.setMotionType (Coordinate.MotionType.TRANSLATIONAL);
            break;
         }
         case COUPLED: {
            coord.setMotionType (Coordinate.MotionType.COUPLED);
            break;
         }
         default: {
            coord.setMotionType (Coordinate.MotionType.ROTATIONAL);
            break;
         }
      }
   }
   
   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      rtok.scanToken ('[');

      while (rtok.nextToken() != ']') {
         if (ScanWriteUtils.scanAttributeName (rtok, "coordinates")) {
            ArrayList<Coordinate> clist = new ArrayList<>();
            rtok.scanToken ('[');
            while (rtok.nextToken() != ']') {
               rtok.pushBack();
               Coordinate coord = new Coordinate();
               coord.scan (rtok, ref);
               clist.add (coord);
            }
            myCoords = clist.toArray(new Coordinate[0]);
         }
         else if (ScanWriteUtils.scanAttributeName (rtok, "axes")) {
            ArrayList<TAxis> alist = new ArrayList<>();
            rtok.scanToken ('[');
            while (rtok.nextToken() != ']') {
               rtok.pushBack();
               TAxis taxis = new TAxis();
               taxis.scan (rtok, ref);
               alist.add (taxis);
            }
            if (alist.size() != 6) {
               throw new IOException (
                  "Expecting 6 axis definitions, got " +
                  alist.size() + ", line "+rtok.lineno());
            }
            myRotAxes = new TAxis[3];
            for (int i=0; i<3; i++) {
               myRotAxes[i] = alist.get(i);
            }
            myTransAxes = new TAxis[3];
            for (int i=0; i<3; i++) {
               myTransAxes[i] = alist.get(i+3);
            }
         }
         else {
            throw new IOException (
               "Expected attrubute name, got " + rtok);
         }
      }
      if (myCoords == null) {
         throw new IOException (
            "No coordinates specified, line " + rtok.lineno());
      }
      if (myRotAxes == null) {
         throw new IOException (
            "No axes specified, line " + rtok.lineno());
      }
      initializeConstraintInfo();      
   }

   public void write (PrintWriter pw, NumberFormat fmt, Object ref)
      throws IOException {

      pw.println ("[ ");
      IndentingPrintWriter.addIndentation (pw, 2);
      pw.println ("coordinates=[");
      IndentingPrintWriter.addIndentation (pw, 2);
      Coordinate coord = new Coordinate();
      for (int i=0; i<numCoordinates(); i++) {
         CoordinateInfo cinfo = getCoordinateInfo(i);
         setCoordForWriting (coord, cinfo);
         coord.write (pw, fmt, ref);
      }
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
      pw.println ("axes=[");
      IndentingPrintWriter.addIndentation (pw, 2);
      for (int i=0; i<myRotAxes.length; i++) {
         myRotAxes[i].write (pw, fmt, ref);
      }
      for (int i=0; i<myTransAxes.length; i++) {
         myTransAxes[i].write (pw, fmt, ref);
      }
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
   }

   public boolean isWritable() {
      return true;


   }
}
