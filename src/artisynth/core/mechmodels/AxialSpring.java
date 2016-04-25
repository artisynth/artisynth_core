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
import maspack.matrix.*;
import artisynth.core.util.*;
import artisynth.core.modelbase.*;
import artisynth.core.materials.*;

import java.io.*;

import maspack.render.*;

import java.util.*;

public class AxialSpring extends PointSpringBase
   implements ScalableUnits, CopyableComponent {
   protected Point myPnt0;
   protected Point myPnt1;
   protected SegmentData mySeg = new SegmentData (null, null);

   protected Vector3d myTmp = new Vector3d();
   protected Matrix3d myMat = null;
   protected Vector3d myU = new Vector3d();
   protected double myLength = 0;

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
//      if (getParent() != null) { 
//         // then change is happening when connected to hierarchy
//         if (myPnt0 != null) {
//            myPnt0.removeBackReference (this);
//         }
//         if (pnt != null) {
//            pnt.addBackReference (this);
//         }
//      }
      myPnt0 = pnt;
      mySeg.pnt0 = pnt;
   }

   public void setSecondPoint (Point pnt) {
//      if (getParent() != null) { 
//         // then change is happening when connected to hierarchy
//         if (myPnt1 != null) {
//            myPnt1.removeBackReference (this);
//         }
//         if (pnt != null) {
//            pnt.addBackReference (this);
//         }
//      }
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
      updateU();
      if (myLength == 0) {
         f.setZero();
         return;
      }
      double v =
         (myU.dot (myPnt1.getVelocity()) - myU.dot (myPnt0.getVelocity()));
      double F = computeF (myLength, v);
      // System.out.println (getIndex() + " " + F);
      f.scale (F, myU);
   }

   public void applyForces (double t) {
      computeForce (myTmp);
      myPnt0.addForce (myTmp);
      myPnt1.subForce (myTmp);
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
      getAllPropertyInfo().writeNonDefaultProps (this, pw, fmt);
   }

   public void write (PrintWriter pw, NumberFormat fmt, Object ref)
      throws IOException {
      dowrite (pw, fmt, ref);
   }

//   public float[] getRenderCoords0() {
//      return myPnt0.myRenderCoords;
//   }
//
//   public float[] getRenderCoords1() {
//      return myPnt1.myRenderCoords;
//   }
//
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
//      myStiffness *= s;
//      myDamping *= s;
   }

   public Vector3d getDir() {
      updateU();
      return myU;
   }

   public double getLength() {
      return mySeg.updateU();
   }

   public double getLengthDot() {
      return mySeg.getLengthDot();
   }

   protected void updateU() {
      myU.sub (myPnt1.getPosition(), myPnt0.getPosition());
      myLength = myU.norm();
      if (myLength != 0) {
         myU.scale (1 / myLength);
      }
   }

   private boolean checkForcePositionJacobian (Matrix3x3Block M, double eps) {
      Matrix3d Mcheck = new Matrix3d();

      Vector3d u = new Vector3d();
      u.sub (myPnt1.getPosition(), myPnt0.getPosition());
      double len = u.norm();
      if (len == 0) {
         Mcheck.setZero();
      }
      else {
         u.scale (1 / len);
         Vector3d delVel = new Vector3d();
         delVel.sub (myPnt1.getVelocity(), myPnt0.getVelocity());
         double ldot = u.dot (delVel);

         double F = computeF (len, ldot);

         // dFdl is the derivative of force magnitude with respect to length
         double dFdl = computeDFdl (len, ldot);

         // dFdldot is the derivative of force magnitude with respect
         // to the time derivative of length
         double dFdldot = computeDFdldot (len, ldot);

         double a = (dFdldot * ldot + F) / len;
         Mcheck.outerProduct (u, u);
         Mcheck.scale (a - dFdl);
         Mcheck.m00 -= a;
         Mcheck.m11 -= a;
         Mcheck.m22 -= a;
      }
      return Mcheck.epsilonEquals (M, eps);
   }

   private boolean checkForceVelocityJacobian (Matrix3x3Block M, double eps) {
      Matrix3d Mcheck = new Matrix3d();

      Vector3d u = new Vector3d();
      u.sub (myPnt1.getPosition(), myPnt0.getPosition());
      double len = u.norm();
      if (len == 0) {
         Mcheck.setZero();
      }
      else {
         u.scale (1 / len);
         Vector3d delVel = new Vector3d();
         delVel.sub (myPnt1.getVelocity(), myPnt0.getVelocity());
         double ldot = u.dot (delVel);

         // dFdldot is the derivative of force magnitude with respect
         // to the time derivative of length
         double dFdldot = computeDFdldot (len, ldot);

         Mcheck.outerProduct (u, u);
         Mcheck.scale (-dFdldot);
      }
      return Mcheck.epsilonEquals (M, eps);
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
      updateU();
      if (myLength == 0) {
         M.setZero();
         return;
      }
      myTmp.sub (myPnt1.getVelocity(), myPnt0.getVelocity());
      double ldot = myU.dot (myTmp);

      // dFdl is the derivative of force magnitude with respect to length
      double dFdl = computeDFdl (myLength, ldot);
      // if (myIgnoreCoriolisInJacobian) {
      //    // easy way. This is the same result for springs as stiffness warping
      //    M.outerProduct (myU, myU);
      //    M.scale (-dFdl);
      //    return;
      // }

      // components of the open product u * u':
      double uxx = myU.x * myU.x;
      double uyy = myU.y * myU.y;
      double uzz = myU.z * myU.z;
      double uxy = myU.x * myU.y;
      double uxz = myU.x * myU.z;
      double uyz = myU.y * myU.z;

      // dFdldot is the derivative of force magnitude with respect
      // to the time derivative of length
      double dFdldot = computeDFdldot (myLength, ldot);

      // F is the force magnitude along u

      double F = computeF (myLength, ldot);
      // double F = (myStiffness*(myLength-myRestLength) +
      // myDamping*(myU.dot(myTmp)));

      // Now compute M = (-dFdldot * u * myTmp' - F I) / length
      if (!myIgnoreCoriolisInJacobian && dFdldot != 0) {
         myTmp.scale (-dFdldot / myLength);
         M.outerProduct (myU, myTmp);
      }
      else {
         M.setZero();
      }
      M.m00 -= F / myLength;
      M.m11 -= F / myLength;
      M.m22 -= F / myLength;

      // form the product M * (I - u u')
      double m00 = -M.m00 * (uxx - 1) - M.m01 * uxy - M.m02 * uxz;
      double m11 = -M.m10 * uxy - M.m11 * (uyy - 1) - M.m12 * uyz;
      double m22 = -M.m20 * uxz - M.m21 * uyz - M.m22 * (uzz - 1);

      double m01 = -M.m00 * uxy - M.m01 * (uyy - 1) - M.m02 * uyz;
      double m02 = -M.m00 * uxz - M.m01 * uyz - M.m02 * (uzz - 1);
      double m12 = -M.m10 * uxz - M.m11 * uyz - M.m12 * (uzz - 1);

      double m10 = -M.m10 * (uxx - 1) - M.m11 * uxy - M.m12 * uxz;
      double m20 = -M.m20 * (uxx - 1) - M.m21 * uxy - M.m22 * uxz;
      double m21 = -M.m20 * uxy - M.m21 * (uyy - 1) - M.m22 * uyz;

      // finally, add -dFdl* u * u' to final result

      if (dFdl != 0) {
         M.m00 = m00 - dFdl * uxx;
         M.m11 = m11 - dFdl * uyy;
         M.m22 = m22 - dFdl * uzz;

         M.m01 = m01 - dFdl * uxy;
         M.m02 = m02 - dFdl * uxz;
         M.m12 = m12 - dFdl * uyz;

         M.m10 = m10 - dFdl * uxy;
         M.m20 = m20 - dFdl * uxz;
         M.m21 = m21 - dFdl * uyz;
      }
      else {
         M.m00 = m00;
         M.m11 = m11;
         M.m22 = m22;

         M.m01 = m01;
         M.m02 = m02;
         M.m12 = m12;

         M.m10 = m10;
         M.m20 = m20;
         M.m21 = m21;
      }
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
      updateU();
      myTmp.sub (myPnt1.getVelocity(), myPnt0.getVelocity());
      double ldot = myU.dot (myTmp);
      double dFdldot = computeDFdldot (myLength, ldot);
      if (myLength == 0 || dFdldot == 0) {
         M.setZero();
         return;
      }
      M.outerProduct (myU, myU);
      M.scale (-dFdldot);
   }

   private MatrixBlock addBlockIfNeeded (
      SparseBlockMatrix M, int bi, int bj, Class type) {
      MatrixBlock blk = M.getBlock (bi, bj);
      if (blk == null) {
         try {
            blk = (MatrixBlock)type.newInstance();
         }
         catch (Exception e) {
            throw new InternalErrorException ("Cannot create instance of "
            + type);
         }
         M.addBlock (bi, bj, blk);
      }
      else if (!type.isAssignableFrom (blk.getClass())) {
         throw new InternalErrorException ("bad off-diagonal block type: "
         + blk.getClass());
      }
      return blk;
   }

   public void addSolveBlocks (SparseNumberedBlockMatrix M) {
      mySeg.addSolveBlocks (M);
   }

   public void addPosJacobian (SparseNumberedBlockMatrix M, double s) {
      if (myMat == null) {
         myMat = new Matrix3d();
      }
      // computeForcePositionJacobian (myMat);
      // myMat.scale (s);
      // addToJacobianBlocks (myMat);
      double l = getLength();
      double ldot = getLengthDot();
      double F = computeF (l, ldot);
      double dFdl = computeDFdl (l, ldot);
      double dFdldot = computeDFdldot (l, ldot);
      mySeg.addPosJacobian (M, s, F, dFdl, dFdldot, l, myMat);
   }

   public void addVelJacobian (SparseNumberedBlockMatrix M, double s) {
      if (myMat == null) {
         myMat = new Matrix3d();
      }
      // computeForceVelocityJacobian (myMat);
      // myMat.scale (s);
      // addToJacobianBlocks (myMat);
      double l = getLength();
      double ldot = getLengthDot();
      double dFdldot = computeDFdldot (l, ldot);
      mySeg.addVelJacobian (M, s, dFdldot, myMat);
   }

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
      comp.myTmp = new Vector3d();
      comp.myMat = null;
      comp.myU = new Vector3d();
      comp.myLength = 0;

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
