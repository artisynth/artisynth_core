/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.io.*;

import artisynth.core.mechmodels.MechSystem.ConstraintInfo;
import artisynth.core.modelbase.*;
import artisynth.core.util.*;
import maspack.matrix.*;
import maspack.properties.PropertyList;
import maspack.render.PointLineRenderProps;
import maspack.render.Renderer;
import maspack.util.*;

/**
 * 
 * Constrain a linear combination of points to sum to a value:
 *   sum( w_i p_i, i=0..N ) = pos
 *   
 *   Useful for "attaching" an arbitrary position inside one finite element
 *   to another arbitrary position inside a different element, or to a
 *   specific point in space
 *
 */
public class LinearPointConstraint extends ConstrainerBase {

   public static double DEFAULT_COMPLIANCE = 0;
   public static double DEFAULT_DAMPING = 0;
   
   ArrayList<Point> myPoints;
   double[] myWgts;
   Point3d myTarget;
   Matrix3x3Block[] myBlks;
   double[] myLam;

   double myCompliance;
   double myDamping;
   

   static PropertyList myProps =
      new PropertyList (LinearPointConstraint.class, ConstrainerBase.class);

   static {
      myProps.add ("renderProps", "render props", new PointLineRenderProps ());
      myProps.add ("compliance", "constraint compliance", DEFAULT_COMPLIANCE);
      myProps.add ("damping", "constraint damping", DEFAULT_DAMPING);
   }
   
   @Override
   public PropertyList getAllPropertyInfo () {
      return myProps;
   }
   
   /**
    * General constructor.  Make sure to call {@link #setPoints(Point[], double[])}.
    */
   public LinearPointConstraint() {
      myDamping = DEFAULT_DAMPING;
      myCompliance = DEFAULT_COMPLIANCE;
   }

   /**
    * General constructor
    * @param pnts list of points to constrain
    * @param wgts list of weights
    */
   public LinearPointConstraint(Point[] pnts, double[] wgts) {
      this(pnts, wgts, Point3d.ZERO);
   }
   
   /**
    * General constructor
    * @param pnts list of points to constrain
    * @param wgts list of weights
    * @param target target sum
    */
   public LinearPointConstraint(Point[] pnts, double[] wgts, Point3d target) {
      this();
      setPoints(pnts, wgts);
      setTarget (target);
   }

   /**
    * General constructor
    * @param pnts list of points to constrain
    * @param wgts list of weights
    * @param target target sum
    */
   public LinearPointConstraint(Point[] pnts, VectorNd wgts, Point3d target) {
      double[] dwgts = new double[wgts.size ()];
      wgts.get (dwgts);
      setPoints(pnts, dwgts);
      setTarget (target);
   }
   
   /**
    * Initializes the constraint with a set of points and weights.  All
    * {@code Point} objects should be unique.
    * @param pnts list of points to constrain
    * @param wgts set of weights
    */
   protected void setPoints(Point[] pnts, double[] wgts) {
      myTarget = new Point3d(0, 0, 0);
      myPoints = new ArrayList<Point>();
      for (Point pnt : pnts) {
         myPoints.add (pnt);
      }
      myLam = new double[3*myPoints.size()];
      myWgts = Arrays.copyOf(wgts, wgts.length);
      myBlks = new Matrix3x3Block[myPoints.size()];
      for (int i=0; i<myPoints.size(); i++) {
         myBlks[i] = new Matrix3x3Block();
         myBlks[i].m00 = myWgts[i];
         myBlks[i].m11 = myWgts[i];
         myBlks[i].m22 = myWgts[i];
      }
   }
   
   /**
    * Sets a target sum of positions
    * @param pos target position
    */
   public void setTarget(Point3d pos) {
      myTarget.set (pos);
   }

   /**
    * @return the set of points involved in the constraint
    */
   public Point[] getPoints() {
      return myPoints.toArray (new Point[0]);
   }

   /**
    * @return the linear constraint weights
    */
   public double[] getWeights() {
      return myWgts;
   }
   
   public Point3d getTarget() {
      return myTarget;
   }
   
   public double getCompliance() {
      return myCompliance;
   }
   
   public void setCompliance(double c) {
      myCompliance = c;
   }
   
   public double getDamping() {
      return myDamping;
   }
   
   public void setDamping(double d) {
      myDamping = d;
   }

   @Override
   public void getBilateralSizes(VectorNi sizes) {
      for (Point p : myPoints) {
         if (p.getSolveIndex() != -1) {
            sizes.append(3);
         }
      }
   }

   @Override
   public int addBilateralConstraints(
      SparseBlockMatrix GT, VectorNd dg, int numb) {
      int idx = 0;
      int bj = GT.numBlockCols();
      for (Point pnt : myPoints) {
         int bi = pnt.getSolveIndex(); 
         if (bi != -1) {
            myBlks[idx].setBlockRow(bi);
            GT.addBlock(bi, bj, myBlks[idx]);
            if (dg != null) {
               dg.set(numb, 0);
            }
            numb++;
         }
         idx++;
      }
      // System.out.println(GT.toString());
      return numb;
   }

   @Override
   public int getBilateralInfo(ConstraintInfo[] ginfo, int idx) {

      Point3d sumPos = new Point3d();
      int nValid = 0;
      for (int i=0; i<myPoints.size(); i++) {
         Point pnt = myPoints.get(i);
         if (pnt.getSolveIndex() > -1) {
            nValid++;
         }
         sumPos.scaledAdd(myWgts[i], pnt.getPosition());
      }
      sumPos.sub (myTarget);

      if (nValid == 0) {
         return idx;
      }

      // x
      ConstraintInfo gi = ginfo[idx++];
      gi.dist = sumPos.x;
      gi.compliance = myCompliance;
      gi.damping = myDamping;
      gi.force = 0;

      // y
      gi = ginfo[idx++];
      gi.dist = sumPos.y;
      gi.compliance = myCompliance;
      gi.damping = myDamping;
      gi.force = 0;

      // z
      gi = ginfo[idx++];
      gi.dist = sumPos.z;
      gi.compliance = myCompliance;
      gi.damping = myDamping;
      gi.force = 0;

      return idx;
   }

   @Override
   public int setBilateralForces(VectorNd lam, double s, int idx) {
      int k = 0;
      for (Point p : myPoints) {
         if (p.getSolveIndex() != -1) {
            myLam[k++] = lam.get(idx++)*s;
            myLam[k++] = lam.get(idx++)*s;
            myLam[k++] = lam.get(idx++)*s;
            return idx;
         }
      }
      return idx;
   }

   @Override
   public int getBilateralForces(VectorNd lam, int idx) {
      int k = 0;
      for (Point p : myPoints) {
         if (p.getSolveIndex() != -1) {
            lam.set(idx++, myLam[k++]);
            lam.set(idx++, myLam[k++]);
            lam.set(idx++, myLam[k++]);
            return idx;
         }
      }
      return idx;
   }

   @Override
   public void zeroForces() {
      for (int k=0; k<myLam.length; k++) {
         myLam[k] = 0;
      }
   }

   @Override
   public double updateConstraints(double t, int flags) {
      return 0;
   }

   public void getConstrainedComponents (List<DynamicComponent> list) {
      for (int i=0; i<myPoints.size(); i++) {
         list.add (myPoints.get(i));
      }
   }
   
   @Override
   public void render(Renderer renderer, int flags) {

      Point3d diff = new Point3d();
      Point3d avgPos = new Point3d();
      for (int i=0; i<myPoints.size(); i++) {
         Point pnt = myPoints.get(i);
         diff.scaledAdd(myWgts[i], pnt.getPosition());
         if (myWgts[i] > 0) {
            avgPos.scaledAdd (myWgts[i], pnt.getPosition ());
         }
      }
      diff.sub (myTarget);
      
      float[] fpnt0 = new float[] {(float)avgPos.x, (float)avgPos.y, (float)avgPos.z};
      float[] fpnt1 = new float[] {(float)(avgPos.x-diff.x), (float)(avgPos.y-diff.y), (float)(avgPos.z-diff.z)};
      
      renderer.drawLine (getRenderProps (), fpnt0, fpnt1, isSelected());
      renderer.drawPoint (getRenderProps(), fpnt0, isSelected());
      renderer.drawPoint (getRenderProps(), fpnt1, isSelected());
      
   }

   public void scan (ReaderTokenizer rtok, Object ref) throws IOException  {
      myTarget = new Point3d();
      super.scan (rtok, ref);
   }

   public boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "points")) {
         tokens.offer (new StringToken ("points", rtok.lineno()));
         ScanWriteUtils.scanComponentsAndWeights (rtok, tokens);
         return true;
      }
      else if (scanAttributeName (rtok, "target")) {
         myTarget = new Point3d();
         myTarget.scan (rtok);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      if (postscanAttributeName (tokens, "points")) {
         Point[] points = ScanWriteUtils.postscanReferences (
            tokens, Point.class, ancestor);
         double[] weights = (double[])tokens.poll().value();
         setPoints (points, weights);
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }   

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      super.writeItems (pw, fmt, ancestor);
      pw.print ("points=");
      ScanWriteUtils.writeComponentsAndWeights (
         pw, fmt, myPoints.toArray(new Point[0]), myWgts, ancestor);
      if (!myTarget.equals (Point3d.ZERO)) {
         pw.println ("target=[" + myTarget.toString (fmt) + "]");
      }
   }

}
