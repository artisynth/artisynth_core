/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mfreemodels;

import maspack.matrix.MatrixNd;
import maspack.matrix.Point3d;
import maspack.matrix.VectorNd;

public class GMLSShapeFunction extends MLSShapeFunction {
   
   private int[] myDerivativeIdxs;
   private int myOrder;
   
   public GMLSShapeFunction (int order, int dx, int dy, int dz) {
      myOrder = order;
      myDerivativeIdxs = new int[]{dx, dy, dz};
   }
   
   // number of permutations
   private static int dcoeff(int dx, int dy, int dz) {
      int s = dx+dy+dz;
      int v = factorial(s)/factorial(dx)/factorial(dy)/factorial(dz);      
      return v;
   }
   
   private static int factorial(int n) {
      int v = 1;
      for (int i=1; i<=n; i++) {
         v = v*i;
      }
      return v;
   }
   
   @Override
   public void computeM(MatrixNd M, Point3d pnt, MFreeNode3d[] nodeList) {
      
      MatrixNd cc = new MatrixNd(nBasis,nBasis);
      M.setSize(nBasis, nBasis);
      M.setZero();
      
      for (MFreeNode3d node : nodeList) {
         Point3d pos = node.getRestPosition();
         
         double w = node.getWeight(pnt);
         
         for (int i=0; i<=myOrder; i++) {
            for (int j=0; j<=myOrder-i; j++) {
               for (int k=0; k<=myOrder-i-j; k++) {
                  double d = dcoeff(i, j, k);   // account for symmetries
                  computeDPCorrelation(cc, pos.x, pos.y, pos.z, i, j, k);
                  M.scaledAdd(d*w, cc);         
               }
            }
         }
         
      }
      
   }
   
   @Override
   public double eval(MFreeNode3d node, MFreeNode3d[] nodes, Point3d pnt, MatrixNd MInv) {
      VectorNd _p = new VectorNd(nBasis);
      VectorNd pi = new VectorNd(nBasis);
      computePtMInv(_p, MInv, pnt, nodes);
      Point3d xi = node.getRestPosition();
      computeDP(pi, xi.x, xi.y, xi.z, myDerivativeIdxs[0], myDerivativeIdxs[1], myDerivativeIdxs[2]);
      _p.scale(node.getWeight(pnt));
      return _p.dot(pi);
   }
   
   @Override
   public void computeDDM(MatrixNd DM, int di, int dj, Point3d pnt, MFreeNode3d[] nodeList) {
      
      MatrixNd cc = new MatrixNd(nBasis,nBasis);
      DM.setSize(nBasis, nBasis);
      DM.setZero();
      int derivatives[] = new int[3];
      derivatives[di] = 1;
      derivatives[dj] = derivatives[dj]+1;
      
      for (MFreeNode3d node : nodeList) {
         Point3d pos = node.getRestPosition();
         
         double w = node.getWeightFunction().evalDerivative(pnt,derivatives);
         for (int i=0; i<=myOrder; i++) {
            for (int j=0; j<=myOrder-i; j++) {
               for (int k=0; k<=myOrder-i-j; k++) {
                  double d = dcoeff(i, j, k);   // account for symmetries
                  computeDPCorrelation(cc, pos.x, pos.y, pos.z, i, j, k);
                  DM.scaledAdd(d*w, cc);         
               }
            }
         }
      }
      
   }
   
   @Override
   public void computeDM(MatrixNd DM, int dIdx, Point3d pnt, MFreeNode3d[] nodes) {
      
      MatrixNd cc = new MatrixNd(nBasis,nBasis);
      DM.setSize(nBasis, nBasis);
      DM.setZero();
      int[] derivatives = new int[3];
      derivatives[dIdx] = 1;
      
      for (MFreeNode3d node : nodes) {
         Point3d pos = node.getRestPosition();
         double w = node.getWeightFunction().evalDerivative(pnt,derivatives);
         for (int i=0; i<=myOrder; i++) {
            for (int j=0; j<=myOrder-i; j++) {
               for (int k=0; k<=myOrder-i-j; k++) {
                  double d = dcoeff(i, j, k);   // account for symmetries
                  computeDPCorrelation(cc, pos.x, pos.y, pos.z, i, j, k);
                  DM.scaledAdd(d*w, cc);         
               }
            }
         }
      }
      
   }
   
   @Override
   public double evalDerivative(MFreeNode3d node, MFreeNode3d[] nodes, Point3d in, int[] derivatives, MatrixNd MInv) {
      
      VectorNd pi = new VectorNd(nBasis);
      
      MFreeWeightFunction fun = node.getWeightFunction();
      Point3d xi = node.getRestPosition();
      
      double w = fun.eval(in);
      computeDP(pi, xi.x, xi.y, xi.z, myDerivativeIdxs[0], myDerivativeIdxs[1], myDerivativeIdxs[2]);
      
      return evalDerivative(node, nodes, in, pi, w, derivatives, MInv);
      
   }

}
