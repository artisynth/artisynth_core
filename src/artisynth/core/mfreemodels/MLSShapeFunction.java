/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mfreemodels;

import java.util.Arrays;

import maspack.function.DifferentiableFunction3x1;
import maspack.matrix.MatrixNd;
import maspack.matrix.Point3d;
import maspack.matrix.SVDecomposition;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;

public class MLSShapeFunction implements MFreeShapeFunction {
   
   public static final int CONSTANT_ORDER = 0;
   public static final int SHEPARD = 0;
   public static final int LINEAR_ORDER = 1;
   public static final int QUADRATIC_ORDER = 2;
   
   Point3d myPnt;
   MFreeNode3d[] myNodes;
   boolean restDataValid;
   //   MatrixNd M;
   //   MatrixNd Minv;
   
   // rest data
   VectorNd pRest[];
   VectorNd dpRest[][];
   
   protected DifferentiableFunction3x1[] myBasisFunctions;
   protected int nBasis;
   
   // stored values of shape function and derivatives
   VectorNd N;
   VectorNd[] dNds;
   
   public MLSShapeFunction() {
      this(LINEAR_ORDER);
   }
   
   public MLSShapeFunction(int order) {
      setBasisFunctions(getPolynomialBasis(order));
      myPnt = new Point3d();
   }
   
   public MLSShapeFunction(MFreeNode3d[] nodes, int order) {
      setBasisFunctions(getPolynomialBasis(order));
      setNodes (nodes);
      myPnt = new Point3d();
   }
   
   public void setBasisFunctions(DifferentiableFunction3x1[] functions) {
      myBasisFunctions = functions;
      nBasis = functions.length;
      //      M = null;
      //      Minv = null;
      N = null;
      dNds = null;
      restDataValid = false;
   }
   
   @Override
   public void invalidateRestData () {
      restDataValid = false;
   }
   
   public void updateRestData() {
      
      pRest = new VectorNd[myNodes.length];
      dpRest = new VectorNd[myNodes.length][3];
      
      for (int i=0; i<pRest.length; ++i) {
         MFreeNode3d node = myNodes[i];
         Point3d pos = node.getLocalRestPosition ();
         
         // zeroeth derivative
         pRest[i] = new VectorNd(nBasis);
         computeP(pRest[i], pos.x, pos.y, pos.z);
         
         // first derivatives
         dpRest[i][0] = new VectorNd(nBasis);
         computeDP (dpRest[i][0], pos.x, pos.y, pos.z, 1, 0, 0);
         dpRest[i][1] = new VectorNd(nBasis);
         computeDP (dpRest[i][1], pos.x, pos.y, pos.z, 0, 1, 0);
         dpRest[i][2] = new VectorNd(nBasis);
         computeDP (dpRest[i][2], pos.x, pos.y, pos.z, 0, 0, 1);
      }

      restDataValid = true;
   }
   
   public static DifferentiableFunction3x1[] getPolynomialBasis(int order) {
      
      int order3 = (order+1)*(order+2)*(order+3)/6;
      PolynomialBasisFunction[] basis = new PolynomialBasisFunction[order3];
      
      int idx = 0;
      for (int i=0; i<=order; i++) {
         for (int j=0; j<=order-i; j++) {
            for (int k=0; k<=order-i-j; k++) {
               basis[idx++] = new PolynomialBasisFunction(i, j, k);
            }
         }
      }

      return basis;
   }
   
   protected void computeP(VectorNd p, double x, double y, double z) {
      if (p.size() != nBasis) {
         p.setSize(nBasis);
      }
      for (int i=0; i<nBasis; i++) {
         p.set(i, myBasisFunctions[i].eval(x,y,z));
      }
   }
   
   protected void computeDP(VectorNd dp, double x, double y, double z, int dx, int dy, int dz) {
      if (dp.size() != nBasis) {
         dp.setSize(nBasis);
      }      
      for (int i=0; i<nBasis; i++) {
         dp.set(i, myBasisFunctions[i].evalDerivative(x, y, z, dx, dy, dz));
      }
   }

   //   protected void computeCorrelation(MatrixNd out, VectorNd p) {
   //      double val;
   //      double[] vals = p.getBuffer();
   //      
   //      for (int i=0; i<nBasis; i++) {
   //         out.set(i,i,vals[i]*vals[i]);
   //         for (int j=i+1; j<nBasis; j++) {
   //            val = vals[i]*vals[j];
   //            out.set(i,j,val);
   //            out.set(j,i,val);
   //         }
   //      }
   //   }


   //   protected void computePCorrelation(MatrixNd out, double x, double y, double z) {
   //      VectorNd _p = new VectorNd(nBasis);            
   //      computeP(_p, x,y,z);
   //      computeCorrelation(out, _p);
   //   }
   //   
   //   protected void computeDPCorrelation(MatrixNd out, double x, double y, double z, int dx, int dy, int dz) {
   //      VectorNd _p = new VectorNd(nBasis);
   //      computeDP(_p, x,y,z,dx,dy,dz);
   //      computeCorrelation(out,_p);
   //   }
   
   protected void addScaledOuterProduct(MatrixNd M, double s, VectorNd N) {
      for (int i=0; i<N.size (); ++i) {
         double vi = N.get (i);
         M.add (i, i, s*vi*vi);
         for (int j=0; j<i; ++j) {
            double v = s*vi*N.get(j);
            M.add (i, j, v);
            M.add (j, i, v);
         }
      }
   }
   
   //   public void computeM(MatrixNd M, Point3d pnt, MFreeNode3d[] nodeList) {
   //    
   //      MatrixNd cc = new MatrixNd(nBasis,nBasis);
   //      M.setSize(nBasis, nBasis);
   //      M.setZero();
   //      
   //      for (MFreeNode3d node : nodeList) {
   //         Point3d   //   public void computeM(MatrixNd M, Point3d pnt, MFreeNode3d[] nodeList) {
   //    
   //      MatrixNd cc = new MatrixNd(nBasis,nBasis);
   //      M.setSize(nBasis, nBasis);
   //      M.setZero();
   //      
   //      for (MFreeNode3d node : nodeList) {
   //         Point3d pos = node.getRestPosition();
   //         computePCorrelation(cc, pos.x, pos.y, pos.z);
   //         M.scaledAdd(node.getWeight(pnt), cc);
   //      }
   //      
   //   } pos = node.getRestPosition();
   //         computePCorrelation(cc, pos.x, pos.y, pos.z);
   //         M.scaledAdd(node.getWeight(pnt), cc);
   //      }
   //      
   //   }
   
   //   public double computeMInv(MatrixNd MInv, Point3d pnt, MFreeNode3d[] nodeList) {
   //      computeM(MInv,pnt,nodeList);
   //      SVDecomposition svd = new SVDecomposition(MInv);
   //      svd.pseudoInverse(MInv);
   //      
   //      if (svd.condition()>1e10) {
   //         System.out.println("Warning: poor condition number, "+svd.condition());
   //      }
   //      
   //      return svd.condition();
   //   }
   
   //   public void computePtMInv(VectorNd pTMInv, MatrixNd MInv, Point3d pnt, MFreeNode3d[] nodeList) {
   //      computeP(pTMInv, pnt.x,pnt.y, pnt.z);
   //      MInv.mulTranspose(pTMInv, pTMInv);
   //   }
   //   
   //   private double eval(MFreeNode3d node, MFreeNode3d[] nodes, Point3d pnt, MatrixNd MInv) {
   //      VectorNd _p = new VectorNd(nBasis);
   //      VectorNd pi = new VectorNd(nBasis);
   //      
   //      computeP(_p, pnt.x, pnt.y, pnt.z);
   //      MInv.mulTranspose (_p, _p);
   //      
   //      Point3d xi = node.getRestPosition();
   //      computeP(pi, xi.x, xi.y, xi.z);
   //      return node.getWeight(pnt)*_p.dot(pi);
   //   }
   
   protected void update(Point3d pnt) {
      this.myPnt.set(pnt);
      
      if (!restDataValid) {
         updateRestData ();
      }
      
      MatrixNd M = new MatrixNd(nBasis, nBasis);
      MatrixNd Minv = new MatrixNd(nBasis, nBasis);
      VectorNd W = new VectorNd(myNodes.length);  // weight vector
      
      for (int i=0; i<pRest.length; ++i) {
         MFreeNode3d node = myNodes[i];
         double w = node.getWeight (pnt);
         W.set (i, w);
         addScaledOuterProduct (M, w, pRest[i]);
      }
      
      if (M.rowSize () == 1) {
         Minv.set (0,0,1.0/M.get (0, 0));
      } else {
         SVDecomposition svd = new SVDecomposition(M);
         svd.pseudoInverse(Minv);
         if (svd.condition()>1e10) {
            System.out.println("Warning: poor condition number, "+svd.condition());
         }
      }
      
      // evaluation point p
      VectorNd ploc = new VectorNd(nBasis);
      computeP(ploc, pnt.x, pnt.y, pnt.z);
      VectorNd pMinv = new VectorNd(ploc.size ());
      Minv.mulTranspose (pMinv, ploc);
      
      // shape function
      N = new VectorNd(myNodes.length);
      for (int i=0; i<pRest.length; ++i) {
         N.set (i, W.get (i)*pMinv.dot(pRest[i]));
      }
      
      // derivatives
      dNds = new VectorNd[3];
      VectorNd dW = new VectorNd(myNodes.length);
      VectorNd dploc = new VectorNd(nBasis);
      VectorNd dpMinv = new VectorNd(nBasis);
      VectorNd pDMinv = new VectorNd(nBasis);
      
      int[] dd =  {0, 0, 0};
      for (int k=0; k<3; ++k) {
         
         // dp/dt(x)*Minv*wi*pi + p(x)*DMkinv*wi*pi + p(x)*Minv*dwi/dx*pi
         // DMkinv = -Minv*DMk*Minv
         // DMk = sum_i dw_i/dx pi*pi
         dd[k] = 1;
         MatrixNd DM = new MatrixNd(nBasis, nBasis);
         for (int i=0; i<pRest.length; ++i) {
            MFreeNode3d node = myNodes[i];
            double dw = node.getWeightFunction ().evalDerivative (pnt.x, pnt.y, pnt.z, 
              dd[0], dd[1], dd[2]);
            dW.set (i, dw);
            addScaledOuterProduct (DM, dw, pRest[i]);
         }
         
         // DMkinv
         DM.mul (Minv, DM);
         DM.mul (Minv);
         
         // local p derivative
         computeDP (dploc, pnt.x, pnt.y, pnt.z, dd[0], dd[1], dd[2]);
         Minv.mulTranspose (dpMinv, dploc);
         DM.mulTranspose (pDMinv, ploc);
         
         // derivative
         dNds[k] = new VectorNd(myNodes.length);
         for (int i=0; i<pRest.length; ++i) {
            double v = dpMinv.dot (pRest[i])*W.get (i) - pDMinv.dot (pRest[i])*W.get (i) + pMinv.dot (pRest[i])*dW.get (i);
            dNds[k].set (i, v);
         }
         
         dd[k] = 0;
      }
      
   }
   
   @Override
   public double eval(int nidx) {
      return N.get (nidx);
   }

   
   //   public void computeDDMInv(MatrixNd DDMInv, int di, int dj, MatrixNd MInv, Point3d pnt, MFreeNode3d[] nodeList) {
   //      
   //      MatrixNd mTmpi = new MatrixNd(nBasis,nBasis);
   //      MatrixNd mTmpj = new MatrixNd(nBasis,nBasis);
   //      
   //      
   //      // M^{-1}_{,kl} = M^{-1} [  (M_{,k}M^{-1})(M_{,l}M^{-1}) + (M_{,l}M^{-1})(M_{,k}M^{-1}) - M_{,kl}M^{-1}  ]
   //      computeDM(mTmpi,di,pnt,nodeList);
   //      mTmpi.mul(MInv);
   //      if (di != dj) {
   //         computeDM(mTmpj,di,pnt,nodeList);
   //         mTmpj.mul(MInv);
   //         
   //         DDMInv.mul(mTmpi,mTmpj);
   //         mTmpj.mul(mTmpi);
   //         DDMInv.add(mTmpj);
   //         
   //      } else {
   //         DDMInv.mul(mTmpi,mTmpi);
   //         DDMInv.scale(2);
   //      }
   //      
   //      computeDDM(mTmpi, di, dj, pnt, nodeList);
   //      mTmpi.mul(MInv);
   //      DDMInv.sub(mTmpi);
   //      
   //      DDMInv.mul(MInv, DDMInv);
   //            
   //   }
   //   
   //   public void computeDMInv(MatrixNd DMInv, int di, MatrixNd MInv, Point3d pnt, MFreeNode3d[] nodeList) {
   //      
   //      // M^{-1}_{,k} = -M^{-1}M_{,k}M^{-1}
   //      computeDM(DMInv, di, pnt, nodeList);
   //      DMInv.mul(MInv, DMInv);
   //      DMInv.mul(DMInv, MInv);
   //      DMInv.scale(-1);
   //      
   //   }
   //   
   //   public void computeDDM(MatrixNd DM, int di, int dj, Point3d pnt, MFreeNode3d[] nodeList) {
   //      
   //      MatrixNd cc = new MatrixNd(nBasis,nBasis);
   //      DM.setSize(nBasis, nBasis);
   //      DM.setZero();
   //      int derivatives[] = new int[3];
   //      derivatives[di] = 1;
   //      derivatives[dj] = derivatives[dj]+1;
   //      
   //      for (MFreeNode3d node : nodeList) {
   //         Point3d pos = node.getRestPosition();
   //         computePCorrelation(cc, pos.x, pos.y, pos.z);
   //         DM.scaledAdd(node.getWeightFunction().evalDerivative(pnt,derivatives), cc);
   //      }
   //      
   //   }
   //   
   //   public void computeDM(MatrixNd DM, int dIdx, Point3d pnt, MFreeNode3d[] nodeList) {
   //      
   //      MatrixNd cc = new MatrixNd(nBasis,nBasis);
   //      DM.setSize(nBasis, nBasis);
   //      DM.setZero();
   //      int[] derivatives = new int[3];
   //      derivatives[dIdx] = 1;
   //      
   //      for (MFreeNode3d node : nodeList) {
   //         Point3d pos = node.getRestPosition();
   //         computePCorrelation(cc, pos.x, pos.y, pos.z);
   //         DM.scaledAdd(node.getWeightFunction().evalDerivative(pnt, derivatives), cc);
   //      }
   //      
   //   }
   //   
   //   protected double evalDerivative(MFreeNode3d node, MFreeNode3d[] nodes, Point3d in, VectorNd pXi, double wx, int[] derivatives, MatrixNd MInv) {
   //      int dx = derivatives[0];
   //      int dy = derivatives[1];
   //      int dz = derivatives[2];
   //      double out =  0;
   //
   //      VectorNd pTmp = new VectorNd(nBasis);
   //      MFreeWeightFunction fun = node.getWeightFunction();
   //      
   //      VectorNd p = new VectorNd(nBasis);
   //      computeP(p, in.x, in.y, in.z);     
   //      
   //      if (dx+dy+dz == 1) {
   //         
   //         MatrixNd DMInv = new MatrixNd(nBasis, nBasis);
   //         VectorNd pk = new VectorNd(nBasis);
   //         double wk;
   //         if (dz==1) {
   //            computeDMInv(DMInv, 2, MInv, in, nodes);
   //            computeDP(pk, in.x, in.y, in.z, 0, 0, 1);
   //            wk = fun.evalDerivative(in.x, in.y, in.z, 0, 0, 1);
   //         } else if (dy==1) {
   //            computeDMInv(DMInv, 1, MInv, in, nodes);
   //            computeDP(pk, in.x, in.y, in.z, 0, 1, 0);
   //            wk = fun.evalDerivative(in.x, in.y, in.z, 0, 1, 0);
   //         } else {
   //            computeDMInv(DMInv, 0, MInv, in, nodes);
   //            computeDP(pk, in.x, in.y, in.z, 1, 0, 0);
   //            wk = fun.evalDerivative(in.x, in.y, in.z, 1, 0, 0);
   //         }
   //         
   //         pTmp.mulTranspose(MInv, pk);
   //         out = wx*pTmp.dot(pXi);
   //         
   //         pTmp.mulTranspose(DMInv, p);
   //         out += wx*pTmp.dot(pXi);
   //         
   //         pTmp.mulTranspose(MInv, p);
   //         out += wk*pTmp.dot(pXi);
   //         return out;
   //         
   //      } else if (dx+dy+dz == 2) {
   //         
   //         MatrixNd MklInv = new MatrixNd(nBasis,nBasis);
   //         MatrixNd MkInv = new MatrixNd(nBasis,nBasis);
   //         MatrixNd MlInv;
   //         VectorNd pkl = new VectorNd(nBasis);
   //         VectorNd pk = new VectorNd(nBasis);
   //         VectorNd pl;
   //
   //         
   //         double wkl;
   //         double wk;
   //         double wl;
   //         
   //         if (dx==1 && dy==1) {
   //            computeDDMInv(MklInv, 0, 1, MInv, in, nodes);
   //            computeDMInv(MkInv, 0, MInv, in, nodes);
   //            MlInv = new MatrixNd(nBasis,nBasis);
   //            computeDM(MlInv, 1, in, nodes);
   //            
   //            computeDP(pkl, in.x, in.y, in.z, 1, 1, 0);
   //            computeDP(pk, in.x, in.y, in.z, 1, 0, 0);
   //            pl = new VectorNd(nBasis);
   //            computeDP(pl, in.x, in.y, in.z, 0, 1, 0);
   //            
   //            wkl = fun.evalDerivative(in.x, in.y, in.z, 1, 1, 0);
   //            wk = fun.evalDerivative(in.x, in.y, in.z, 1, 0, 0);
   //            wl = fun.evalDerivative(in.x, in.y, in.z, 0, 1, 0);
   //            
   //         } else if (dx==1 && dz==1) {
   //            computeDDMInv(MklInv, 0, 2, MInv, in, nodes);
   //            computeDMInv(MkInv, 0, MInv, in, nodes);
   //            MlInv = new MatrixNd(nBasis,nBasis);
   //            computeDM(MlInv, 2, in, nodes);
   //            
   //            computeDP(pkl, in.x, in.y, in.z, 1, 0, 1);
   //            computeDP(pk, in.x, in.y, in.z, 1, 0, 0);
   //            pl = new VectorNd(nBasis);
   //            computeDP(pl, in.x, in.y, in.z, 0, 0, 1);
   //            
   //            wkl = fun.evalDerivative(in.x, in.y, in.z, 1, 0, 1);
   //            wk = fun.evalDerivative(in.x, in.y, in.z, 1, 0, 0);
   //            wl = fun.evalDerivative(in.x, in.y, in.z, 0, 0, 1);
   //            
   //         } else if (dy==1 && dz==1){
   //            computeDDMInv(MklInv, 1, 2, MInv, in, nodes);
   //            computeDMInv(MkInv, 1, MInv, in, nodes);
   //            MlInv = new MatrixNd(nBasis,nBasis);
   //            computeDM(MlInv, 2, in, nodes);
   //            
   //            computeDP(pkl, in.x, in.y, in.z, 0, 1, 1);
   //            computeDP(pk, in.x, in.y, in.z, 0, 1, 0);
   //            pl = new VectorNd(nBasis);
   //            computeDP(pl, in.x, in.y, in.z, 0, 0, 1);
   //            
   //            wkl = fun.evalDerivative(in.x, in.y, in.z, 0, 1, 1);
   //            wk = fun.evalDerivative(in.x, in.y, in.z, 0, 1, 0);
   //            wl = fun.evalDerivative(in.x, in.y, in.z, 0, 0, 1);
   //            
   //         } else if (dx==2) {
   //            computeDDMInv(MklInv, 0, 0, MInv, in, nodes);
   //            computeDMInv(MkInv, 0, MInv, in, nodes);
   //            MlInv = MkInv;
   //            
   //            computeDP(pkl, in.x, in.y, in.z, 2, 0, 0);
   //            computeDP(pk, in.x, in.y, in.z, 1, 0, 0);
   //            pl = pk;
   //            
   //            wkl = fun.evalDerivative(in.x, in.y, in.z, 2, 0, 0);
   //            wk = fun.evalDerivative(in.x, in.y, in.z, 1, 0, 0);
   //            wl = wk;
   //            
   //         } else if (dy==2) {
   //            computeDDMInv(MklInv, 1, 1, MInv, in, nodes);
   //            computeDMInv(MkInv, 1, MInv, in, nodes);
   //            MlInv = MkInv;
   //            
   //            computeDP(pkl, in.x, in.y, in.z, 0, 2, 0);
   //            computeDP(pk, in.x, in.y, in.z, 0, 1, 0);
   //            pl = pk;
   //            
   //            wkl = fun.evalDerivative(in.x, in.y, in.z, 0, 2, 0);
   //            wk = fun.evalDerivative(in.x, in.y, in.z, 0, 1, 0);
   //            wl = wk;
   //            
   //         } else {
   //            computeDDMInv(MklInv, 2, 2, MInv, in, nodes);
   //            computeDMInv(MkInv, 2, MInv, in, nodes);
   //            MlInv = MkInv;
   //            
   //            computeDP(pkl, in.x, in.y, in.z, 0, 0, 2);
   //            computeDP(pk, in.x, in.y, in.z, 0, 0, 1);
   //            pl = pk;
   //            
   //            wkl = fun.evalDerivative(in.x, in.y, in.z, 0, 0, 2);
   //            wk = fun.evalDerivative(in.x, in.y, in.z, 0, 0, 1);
   //            wl = wk;
   //         }
   //         
   //         pTmp.mulTranspose(MInv, pkl);
   //         out = wx*pTmp.dot(pXi);
   //         
   //         pTmp.mulTranspose(MlInv, pk);
   //         out += wx*pTmp.dot(pXi);
   //         
   //         pTmp.mulTranspose(MInv, pk);
   //         out += wl*pTmp.dot(pXi);
   //         
   //         pTmp.mulTranspose(MkInv, pl);
   //         out += wx*pTmp.dot(pXi);
   //         
   //         pTmp.mulTranspose(MklInv, p);
   //         out += wx*pTmp.dot(pXi);
   //         
   //         pTmp.mulTranspose(MkInv, p);
   //         out += wl*pTmp.dot(pXi);
   //         
   //         pTmp.mulTranspose(MInv, pl);
   //         out += wk*pTmp.dot(pXi);
   //         
   //         pTmp.mulTranspose(MlInv, p);
   //         out += wk*pTmp.dot(pXi);
   //         
   //         pTmp.mulTranspose(MInv, p);
   //         out += wkl*pTmp.dot(pXi);
   //         
   //         return out;
   //         
   //      }
   //      
   //      throw new IllegalArgumentException("Derivatives only defined up to second-order");
   //   }
   //   
   //   public double evalDerivative(MFreeNode3d node,  MFreeNode3d[] nodes, Point3d in, int[] derivatives, MatrixNd MInv) {
   //      
   //      VectorNd pi = new VectorNd(nBasis);
   //      
   //      MFreeWeightFunction fun = node.getWeightFunction();
   //      Point3d xi = node.getRestPosition();
   //      double w = fun.eval(in);
   //      computeP(pi, xi.x, xi.y, xi.z);
   //      
   //      return evalDerivative(node, nodes, in, pi, w, derivatives, MInv);
   //      
   //   }
   
   @Override
   public void evalDerivative(int nidx, Vector3d dNds) {
      dNds.x = this.dNds[0].get (nidx);
      dNds.y = this.dNds[1].get (nidx);
      dNds.z = this.dNds[2].get (nidx);
   }

   //   public double evalDerivative(MFreeNode3d node, MFreeNode3d[] nodes, Point3d in, int[] derivatives) {
   //      MatrixNd MInv = new MatrixNd(nBasis,nBasis);
   //      computeMInv(MInv, in, nodes);
   //      return evalDerivative(node, nodes, in, derivatives, MInv);
   //   }
   
   @Override
   public void setCoordinate(Point3d pnt) {
      update (pnt);
   }

   @Override
   public Point3d getCoordinate() {
      return myPnt;
   }
   
   @Override
   public void setNodes (MFreeNode3d[] nodes) {
      myNodes = Arrays.copyOf (nodes, nodes.length);
      invalidateRestData ();
   }

   @Override
   public MFreeNode3d[] getNodes() {
      return myNodes;
   }

   @Override
   public void eval(VectorNd N) {
      N.set (this.N);
   }
   
   @Override
   public void evalDerivative (Vector3d[] dNds) {
      for (int i=0; i<dNds.length; ++i) {
        evalDerivative (i, dNds[i]);
      }
   }
   
   @Override
   public void eval (VectorNd N, Vector3d[] dNds) {
      eval(N);
      evalDerivative (dNds);
   }

}
