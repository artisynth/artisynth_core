package artisynth.core.mechmodels;

import java.util.*;

import maspack.matrix.*;
import maspack.function.*;
import maspack.spatialmotion.*;
import maspack.render.*;
import maspack.render.Renderer.AxisDrawStyle;

import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.MechSystem.ConstraintInfo;

import java.io.PrintWriter;
import java.io.IOException;

import artisynth.core.util.ScanToken;
import artisynth.core.util.StringToken;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import maspack.matrix.Vector3d;
import maspack.util.ReaderTokenizer;
import maspack.util.NumberFormat;
import maspack.function.FunctionUtils;

public class JointCoordinateCoupling
   extends ConstrainerBase implements BodyConstrainer {

   private final static double RTOD = 180.0/Math.PI;
   private final static double DTOR = Math.PI/180.0;

   Diff1FunctionNx1 myCouplingFunction;
   ArrayList<JointCoordinateHandle> myCoords;
   double myScaleFactor = 1;

   VectorNd myICoords;
   VectorNd myFxnDeriv;
   double myLambda;

   public JointCoordinateCoupling() {
   }

   public JointCoordinateCoupling (
      List<JointCoordinateHandle> coords, Diff1FunctionNx1 fxn) {
      setCoordinates (coords);
      setFunction (fxn);
   }

   public JointCoordinateCoupling (
      String name, List<JointCoordinateHandle> coords, Diff1FunctionNx1 fxn) {
      this (coords, fxn);
      setName (name);
   }

   void setFunction (Diff1FunctionNx1 fxn) {
      myCouplingFunction = fxn; // XXX clone this?      
   }

   void setCoordinates (List<JointCoordinateHandle> coords) {
      if (coords.size() < 2) {
         throw new IllegalArgumentException (
            "'coords' size is " + coords.size() +
            "; must have one dependent and one or more independent coordinates");
      }
      ArrayList<JointCoordinateHandle> clist = new ArrayList<>();
      for (JointCoordinateHandle ch : coords) {
         clist.add (new JointCoordinateHandle (ch));
      }
      doSetCoordinates (clist);
   }

   private void doSetCoordinates (ArrayList<JointCoordinateHandle> coords) {
      myCoords = coords;
      myICoords = new VectorNd (coords.size()-1);
      myFxnDeriv = new VectorNd (coords.size()-1);
   }

   public double getScaleFactor () {
      return myScaleFactor;
   }

   public void setScaleFactor (double s) {
      myScaleFactor = s;
   }

   public void getBilateralSizes (VectorNi sizes) {
      sizes.append (1);
   }

   private VectorNd getICoordValues() {
      for (int i=1; i<myCoords.size(); i++) {
         myICoords.set (i-1, myCoords.get(i).getStoredValue());
      }
      return myICoords;
   }

   /**
    * Explicitly update the coordinate value associated with this coupling,
    * based on the current value of the independent coordinates.
    */
   public void updateCoordinateValue() {
      double cval = myScaleFactor*myCouplingFunction.eval (getICoordValues());
      myCoords.get(0).setValue(cval);
   }


   /**
    * Returns the coordinate handles used by this coupling. These should not be
    * modified.
    */
   public ArrayList<JointCoordinateHandle> getCoordinateHandles() {
      return myCoords;
   }

   /**
    * {@inheritDoc}
    */
   public int addBilateralConstraints (
      SparseBlockMatrix GT, VectorNd dg, int numb) {
      return addBilateralConstraints (GT, dg, numb, /*solveIndexMap*/null);
   }
      
   /**
    * {@inheritDoc}
    */
   public int addBilateralConstraints (
      SparseBlockMatrix GT, VectorNd dg, int numb, int[] solveIndexMap) {
      myCouplingFunction.evalDeriv (myFxnDeriv, getICoordValues());

      Matrix6x1 GC = new Matrix6x1();
      int bj = GT.numBlockCols();
      Wrench wr;
      wr = myCoords.get(0).getWrench();
      artisynth.core.mechmodels.JointBase joint = myCoords.get(0).getJoint();
      joint.computeConstraintMatrixA (GC, wr, 1);
      joint.addMasterBlocks (
         GT, bj, GC, joint.getFrameAttachmentA(), solveIndexMap);
      joint.computeConstraintMatrixB (GC, wr, -1);
      joint.addMasterBlocks (
         GT, bj, GC, joint.getFrameAttachmentB(), solveIndexMap);

      for (int i=1; i<myCoords.size(); i++) {
         JointCoordinateHandle ch = myCoords.get(i);
         wr = ch.getWrench();
         joint = ch.getJoint();
         double s = myScaleFactor*myFxnDeriv.get(i-1);
         joint.computeConstraintMatrixA (GC, wr, -s);
         joint.addMasterBlocks (
            GT, bj, GC, joint.getFrameAttachmentA(), solveIndexMap);
         joint.computeConstraintMatrixB (GC, wr, s);
         joint.addMasterBlocks (
            GT, bj, GC, joint.getFrameAttachmentB(), solveIndexMap);
      }
      if (dg != null) {
         dg.set (numb, 0);
      }
      return numb + 1;
   }

   public int getBilateralInfo (ConstraintInfo[] ginfo, int idx) {
      ConstraintInfo gi = ginfo[idx];
      double coord0 = myCoords.get(0).getStoredValue();
      double fval = myScaleFactor*myCouplingFunction.eval (getICoordValues());
      //System.out.printf ("dist=%g\n", RTOD*(coord0-fval));
      gi.dist = (coord0-fval);
      gi.compliance = 0;
      gi.damping = 0;
      gi.force = 0;
      idx++;      
      return idx;
   }

   public int setBilateralForces (VectorNd lam, double s, int idx) {
      myLambda = s*lam.get (idx++);
      return idx;
   }

   public int getBilateralForces (VectorNd lam, int idx) {
      lam.set (idx++, myLambda);
      return idx;
   }

   /**
    * {@inheritDoc}
    */
   public int addUnilateralConstraints (
      SparseBlockMatrix NT, VectorNd dn, int numu, int[] solveIndexMap) {
      return numu;
   }
      
   public void zeroForces() {
      myLambda = 0;
   }

   public double updateConstraints (double t, int flags) {
      // nothing to do here, since the needed quantities are updated in the
      // joint components.
      return 0;
   }
   
   public void getConstrainedComponents (HashSet<DynamicComponent> comps) {
      for (JointCoordinateHandle ch : myCoords) {
         ch.getJoint().getConstrainedComponents (comps);
      }
   }

   /* --- begin I/O methods --- */

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      pw.print ("coordinates=");
      JointCoordinateHandle.writeHandles (pw, myCoords, ancestor);
      pw.println ("scaleFactor=" + fmt.format(myScaleFactor));
      pw.print ("function=");
      FunctionUtils.write (pw, myCouplingFunction, fmt);
      super.writeItems (pw, fmt, ancestor);
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "coordinates")) {
         tokens.offer (new StringToken ("coordinates", rtok.lineno()));
         JointCoordinateHandle.scanHandles (rtok, tokens);
         return true;
      }
      else if (scanAttributeName (rtok, "scaleFactor")) {
         myScaleFactor = rtok.scanNumber();
         return true;
      }
      else if (scanAttributeName (rtok, "function")) {
         myCouplingFunction = FunctionUtils.scan (rtok, Diff1FunctionNx1.class);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      
      if (postscanAttributeName (tokens, "coordinates")) {
         doSetCoordinates (
            JointCoordinateHandle.postscanHandles (tokens, ancestor));
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   /* --- end I/O methods --- */
}
