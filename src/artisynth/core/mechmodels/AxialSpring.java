/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import maspack.geometry.*;
import maspack.properties.*;
import maspack.render.*;
import maspack.util.*;
import maspack.util.ClassAliases;
import maspack.matrix.*;
import artisynth.core.util.*;
import artisynth.core.modelbase.*;
import artisynth.core.materials.*;

import java.io.*;

import maspack.render.*;

import java.util.*;

public class AxialSpring extends PointSpringBase
   implements ScalableUnits, CopyableComponent, ForceTargetComponent {
   protected Point myPnt0;
   protected Point myPnt1;
   protected SegmentData mySeg = new SegmentData (null, null);

   public static PropertyList myProps =
      new PropertyList (AxialSpring.class, PointSpringBase.class);

   static {
      //myProps.addReadOnly ("length *", "current spring length");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public AxialSpring() {
      this (null);
   }

   public AxialSpring (String name) {
      this (name, 1, 0, 0);
   }

   public AxialSpring (String name, double l0) {
      this (name, 1, 0, l0);
   }

   public AxialSpring (String name, double k, double d, double l0) {
      super (name);
//      myStiffness = k;
//      myDamping = d;
      setRestLength (l0);
      setMaterial (new LinearAxialMaterial (k, d));
   }

   public AxialSpring (double k, double d, double l0) {
      this (null);
//      myStiffness = k;
//      myDamping = d;
      setRestLength (l0);
      setMaterial (new LinearAxialMaterial (k, d));
   }


   public Point getFirstPoint() {
      return myPnt0;
   }

   public Point getSecondPoint() {
      return myPnt1;
   }
   
   public void setPoints(Point p1, Point p2) {
      setFirstPoint(p1);
      setSecondPoint(p2);
   }
   
   public void setPoints(Point p1, Point p2, double restLength) {
      setPoints(p1,p2);
      setRestLength(restLength);
   }

   public void setFirstPoint (Point pnt) {
      myPnt0 = pnt;
      mySeg.pnt0 = pnt;
   }

   public void setSecondPoint (Point pnt) {
      myPnt1 = pnt;
      mySeg.pnt1 = pnt;
   }
   
   @Override
   public double setRestLengthFromPoints() {
      double l = 0;
      if (myPnt0 != null && myPnt1 != null) {
         l = myPnt0.distance(myPnt1);
      }
      setRestLength(l);
      return l;
   }

   /**
    * Computes the force acting on the first point. The force acting on the
    * second point is the negative of this force.
    * 
    * @param f
    * returns the computed force acting on the first point
    */
   public void computeForce (Vector3d f) {
      mySeg.updateU();
      if (mySeg.len == 0) {
         f.setZero();
         return;
      }
      double F = computeF (mySeg.len, mySeg.getLengthDot());
      f.scale (F, mySeg.uvec);
   }

   public void applyForces (double t) {
      Vector3d tmp = new Vector3d();
      computeForce (tmp);
      myPnt0.addForce (tmp);
      myPnt1.subForce (tmp);
   }

  public void printPointReferences (PrintWriter pw, CompositeComponent ancestor)
      throws IOException {
      pw.print ("points=[ ");
      pw.print (ComponentUtils.getWritePathName (ancestor, myPnt0));
      pw.print (" ");
      pw.print (ComponentUtils.getWritePathName (ancestor, myPnt1));
      pw.println (" ]");
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAndStoreReferences (rtok, "points", tokens) >= 0) {
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "points")) {
         Point[] points = 
            ScanWriteUtils.postscanReferences (tokens, Point.class, ancestor);
         setFirstPoint (points[0]);
         setSecondPoint (points[1]);          
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      printPointReferences (pw, ancestor);
      getAllPropertyInfo().writeNonDefaultProps (this, pw, fmt, ancestor);
   }

   public void write (PrintWriter pw, NumberFormat fmt, Object ref)
      throws IOException {
      dowrite (pw, fmt, ref);
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      myPnt0.updateBounds (pmin, pmax);
      myPnt1.updateBounds (pmin, pmax);
   }

   public void render (Renderer renderer, int flags) {
      renderer.drawLine (
         myRenderProps, myPnt0.myRenderCoords, myPnt1.myRenderCoords,
         isSelected());
   }

   protected AxialSpring newComponent (String classId)
      throws InstantiationException, IllegalAccessException {
      return (AxialSpring)ClassAliases.newInstance (classId, AxialSpring.class);
   }

   public void scaleDistance (double s) {
      super.scaleDistance (s);
      if (myMaterial != null) {
         myMaterial.scaleDistance (s);
      }
      if (myRenderProps != null) {
         RenderableUtils.cloneRenderProps (this);
         myRenderProps.scaleDistance (s);
      }
   }

   public void scaleMass (double s) {
      super.scaleMass (s);
      if (myMaterial != null) {
         myMaterial.scaleMass (s);
      }
   }

   public Vector3d getDir() {
      mySeg.updateU();
      return mySeg.uvec;
   }

   public double getLength() {
      return mySeg.updateU();
   }

   public double getLengthDot() {
      return mySeg.getLengthDot();
   }

   /**
    * Computes the force/position Jacobian of this spring. This gives the change
    * in the force acting on the first point with respect to a change in the
    * position of the first point.
    * 
    * @param M
    * matrix in which to return the result
    */
   public void computeForcePositionJacobian (Matrix3d M) {
      double l = getLength();
      double ldot = getLengthDot();
      double F = computeF (l, ldot);
      double dFdl = computeDFdl (l, ldot);
      double dFdldot = computeDFdldot (l, ldot);
      mySeg.computeForcePositionJacobian (
         M, F, dFdl, dFdldot, l, myIgnoreCoriolisInJacobian);
   }

   /**
    * Computes the force/velocity Jacobian of this spring. This gives the change
    * in the force acting on the first point with respect to a change in the
    * velocity of the first point.
    * 
    * @param M
    * matrix in which to return the result
    */
   public void computeForceVelocityJacobian (Matrix3d M) {
      double l = getLength();
      double ldot = getLengthDot();
      double dFdldot = computeDFdldot (l, ldot);
      mySeg.computeForceVelocityJacobian (M, dFdldot);     
   }

   public void addSolveBlocks (SparseNumberedBlockMatrix M) {
      mySeg.addSolveBlocks (M);
   }

   public void addPosJacobian (SparseNumberedBlockMatrix M, double s) {
      Matrix3d Tmp = new Matrix3d();
      double l = getLength();
      double ldot = getLengthDot();
      double F = computeF (l, ldot);
      double dFdl = computeDFdl (l, ldot);
      double dFdldot = computeDFdldot (l, ldot);
      mySeg.addPosJacobian (M, s, F, dFdl, dFdldot, l, Tmp);
   }

   public void addVelJacobian (SparseNumberedBlockMatrix M, double s) {
      Matrix3d Tmp = new Matrix3d();
      double l = getLength();
      double ldot = getLengthDot();
      double dFdldot = computeDFdldot (l, ldot);
      mySeg.addVelJacobian (M, s, dFdldot, Tmp);
   }

   /* --- Begin ForceTargetComponent interface (for inverse controller) --- */

   public int getForceSize() {
      return 1;
   }
   
   public void getForce (VectorNd minf, boolean staticOnly) {
      double l = mySeg.updateU();
      double ldot = staticOnly ? 0.0 : mySeg.getLengthDot();
      double F = computeF (l, ldot, 0);
      minf.setSize (1);
      minf.set (0, F);
   }

   public int addForcePosJacobian (
      SparseBlockMatrix J, double h, boolean staticOnly, int bi) {
      double l = getLength();
      double ldot = 0;
      if (!staticOnly) {
         ldot = mySeg.getLengthDot();
      }
      double dFdl = computeDFdl (l, ldot, 0);
      double dFdldot = computeDFdldot (l, ldot, 0);
      mySeg.addMinForcePosJacobian (
         J, h, dFdl, dFdldot, l, staticOnly, bi);
      return bi++;
   }
   
   public int addForceVelJacobian (
      SparseBlockMatrix J, double h, int bi) {
      double l = getLength();
      double ldot = mySeg.getLengthDot();
      double dFdldot = computeDFdldot (l, ldot, 0);
      mySeg.addMinForceVelJacobian (J, h, dFdldot, bi);
      return bi++;
   }
   
   /* --- End ForceTargetComponent interface --- */
   
   public int getJacobianType() {
      AxialMaterial mat = getEffectiveMaterial();
      if (myIgnoreCoriolisInJacobian || mat.isDFdldotZero()) {
         return Matrix.SYMMETRIC;
      }
      else {
         return 0;
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean isDuplicatable() {
      return true;
   }

   /**
    * {@inheritDoc}
    */
   public boolean getCopyReferences (
      List<ModelComponent> refs, ModelComponent ancestor) {
      if (myPnt0 != null) {
         if (!ComponentUtils.addCopyReferences (refs, myPnt0, ancestor)) {
            return false;
         }
      }
      if (myPnt1 != null) {
         if (!ComponentUtils.addCopyReferences (refs, myPnt1, ancestor)) {
            return false;
         }
      }
      return true;
   }

   public ModelComponent copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      AxialSpring comp = (AxialSpring)super.copy (flags, copyMap);
      
      Point pnt0 = (Point)ComponentUtils.maybeCopy (flags, copyMap, myPnt0);  
      comp.setFirstPoint (pnt0);

      Point pnt1 = (Point)ComponentUtils.maybeCopy (flags, copyMap, myPnt1);  
      comp.setSecondPoint (pnt1);

      comp.setRenderProps (myRenderProps);
      if (myMaterial != null) {
         comp.setMaterial (myMaterial.clone());
      }
      //comp.setStiffness (myStiffness);
      //comp.setDamping (myDamping);
      comp.mySeg = new SegmentData (pnt0, pnt1);

      return comp;
   }

   @Override
   public void getHardReferences (List<ModelComponent> refs) {
      super.getHardReferences (refs);
      if (myPnt0 != null) {
         refs.add (myPnt0);
      }
      if (myPnt1 != null) {
         refs.add (myPnt1);
      }
   }

//   @Override
//   public void connectToHierarchy () {
//      super.connectToHierarchy ();
//      if (myPnt0 != null) {
//         myPnt0.addBackReference (this);
//      }
//      if (myPnt1 != null) {
//         myPnt1.addBackReference (this);
//      }
//   }
//
//   @Override
//   public void disconnectFromHierarchy() {
//      super.disconnectFromHierarchy();
//      if (myPnt0 != null) {
//         myPnt0.removeBackReference (this);
//      }
//      if (myPnt1 != null) {
//         myPnt1.removeBackReference (this);
//      }
//   }
}
