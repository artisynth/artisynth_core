/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.Arrays;
import java.util.List;

import artisynth.core.mechmodels.MechSystem.ConstraintInfo;
import maspack.matrix.Matrix3x3Block;
import maspack.matrix.Point3d;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.VectorNd;
import maspack.matrix.VectorNi;
import maspack.render.Renderer;

/**
 * 
 * Constrain a linear combination of points to sum to zero:
 *   sum( w_i p_i, i=0..N ) = [0 0 0]'
 *   
 *   Useful for "attaching" an arbitrary position inside one finite element
 *   to another arbitrary position inside a different element
 *
 */
public class LinearPointConstraint extends ConstrainerBase {

   Point[] myPoints;
   double[] myWgts;
   Matrix3x3Block[] myBlks;
   Point3d sumPos = new Point3d();
   double[] myLam;

   /**
    * General constructor.  Make sure to call {@link #setPoints(Point[], double[])}.
    */
   public LinearPointConstraint() {}

   /**
    * General constructor
    * @param pnts list of points to constrain
    * @param wgts list of weights
    */
   public LinearPointConstraint(Point[] pnts, double[] wgts) {
      setPoints(pnts, wgts);
   }

   /**
    * Initializes the constraint with a set of points and weights.  All
    * {@code Point} objects should be unique.
    * @param pnts list of points to constrain
    * @param wgts set of weights
    */
   public void setPoints(Point[] pnts, double[] wgts) {
      myPoints = Arrays.copyOf(pnts, pnts.length);
      myLam = new double[3];    // 3 constraints (x, y, z)
      myWgts = Arrays.copyOf(wgts, wgts.length);
      myBlks = new Matrix3x3Block[myPoints.length];
      for (int i=0; i<myPoints.length; i++) {
         myBlks[i] = new Matrix3x3Block();
         myBlks[i].m00 = myWgts[i];
         myBlks[i].m11 = myWgts[i];
         myBlks[i].m22 = myWgts[i];
      }
   }

   /**
    * @return the set of points involved in the constraint
    */
   public Point[] getPoints() {
      return myPoints;
   }

   /**
    * @return the linear constraint weights
    */
   public double[] getWeights() {
      return myWgts;
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

      sumPos.setZero();
      int nValid = 0;
      for (int i=0; i<myPoints.length; i++) {
         Point pnt = myPoints[i];
         if (pnt.getSolveIndex() > -1) {
            nValid++;
         }
         sumPos.scaledAdd(myWgts[i], pnt.getPosition());
      }

      if (nValid == 0) {
         return idx;
      }

      // x
      ConstraintInfo gi = ginfo[idx++];
      gi.dist = sumPos.x;
      gi.compliance = 0;
      gi.damping = 0;
      gi.force = 0;

      // y
      gi = ginfo[idx++];
      gi.dist = sumPos.y;
      gi.compliance = 0;
      gi.damping = 0;
      gi.force = 0;

      // z
      gi = ginfo[idx++];
      gi.dist = sumPos.z;
      gi.compliance = 0;
      gi.damping = 0;
      gi.force = 0;

      return idx;
   }

   @Override
   public int setBilateralImpulses(VectorNd lam, double h, int idx) {
      for (Point p : myPoints) {
         if (p.getSolveIndex() != -1) {
            myLam[0] = lam.get(idx++);
            myLam[1] = lam.get(idx++);
            myLam[2] = lam.get(idx++);
            return idx;
         }
      }
      return idx;
   }

   @Override
   public int getBilateralImpulses(VectorNd lam, int idx) {
      for (Point p : myPoints) {
         if (p.getSolveIndex() != -1) {
            lam.set(idx++, myLam[0]);
            lam.set(idx++, myLam[1]);
            lam.set(idx++, myLam[2]);
            return idx;
         }
      }
      return idx;
   }

   @Override
   public void zeroImpulses() {
      myLam[0] = 0;
      myLam[1] = 0;
      myLam[2] = 0;
   }

   @Override
   public double updateConstraints(double t, int flags) {
      return 0;
   }

   public void getConstrainedComponents (List<DynamicComponent> list) {
      for (int i=0; i<myPoints.length; i++) {
         list.add (myPoints[i]);
      }
   }
   
   @Override
   public void render(Renderer renderer, int flags) {
   }

}
