/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mfreemodels;

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
   MatrixNd M;
   MatrixNd Minv;
   
   protected DifferentiableFunction3x1[] myBasisFunctions;
   protected int nBasis;
   
   public MLSShapeFunction() {
      setBasisFunctions(getPolynomialBasis(LINEAR_ORDER));
      myPnt = new Point3d();
   }
   
   public void setBasisFunctions(DifferentiableFunction3x1[] functions) {
      myBasisFunctions = functions;
      nBasis = functions.length;
      M = null;
      Minv = null;
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

   protected void computeCorrelation(MatrixNd out, VectorNd p) {
      double val;
      double[] vals = p.getBuffer();
      
      for (int i=0; i<nBasis; i++) {
         out.set(i,i,vals[i]*vals[i]);
         for (int j=i+1; j<nBasis; j++) {
            val = vals[i]*vals[j];
            out.set(i,j,val);
            out.set(j,i,val);
         }
      }
   }


   protected void computePCorrelation(MatrixNd out, double x, double y, double z) {
      VectorNd _p = new VectorNd(nBasis);            
      computeP(_p, x,y,z);
      computeCorrelation(out, _p);
   }
   
   protected void computeDPCorrelation(MatrixNd out, double x, double y, double z, int dx, int dy, int dz) {
      VectorNd _p = new VectorNd(nBasis);
      computeDP(_p, x,y,z,dx,dy,dz);
      computeCorrelation(out,_p);
   }
   
   public void computeM(MatrixNd M, Point3d pnt, MFreeNode3d[] nodeList) {
    
      MatrixNd cc = new MatrixNd(nBasis,nBasis);
      M.setSize(nBasis, nBasis);
      M.setZero();
      
      for (MFreeNode3d node : nodeList) {
         Point3d pos = node.getRestPosition();
         computePCorrelation(cc, pos.x, pos.y, pos.z);
         M.scaledAdd(node.getWeight(pnt), cc);
      }
      
   }
   
   public double computeMInv(MatrixNd MInv, Point3d pnt, MFreeNode3d[] nodeList) {
      computeM(MInv,pnt,nodeList);
      SVDecomposition svd = new SVDecomposition(MInv);
      svd.pseudoInverse(MInv);
      
      if (svd.condition()>1e10) {
         System.out.println("Warning: poor condition number, "+svd.condition());
      }
      
      return svd.condition();
   }
   
   public void computePtMInv(VectorNd pTMInv, MatrixNd MInv, Point3d pnt, MFreeNode3d[] nodeList) {
      computeP(pTMInv, pnt.x,pnt.y, pnt.z);
      MInv.mulTranspose(pTMInv, pTMInv);
   }
   
   public double eval(MFreeNode3d node, MFreeNode3d[] nodes, Point3d pnt, MatrixNd MInv) {
      VectorNd _p = new VectorNd(nBasis);
      VectorNd pi = new VectorNd(nBasis);
      computePtMInv(_p, MInv, pnt, nodes);
      Point3d xi = node.getRestPosition();
      computeP(pi, xi.x, xi.y, xi.z);
      return node.getWeight(pnt)*_p.dot(pi);
   }
   
   @Override
   public boolean maybeUpdate(Vector3d coords, MFreeNode3d[] nodes) {
      if (this.myNodes == nodes) {
         if (coords.distance(this.myPnt) < nodes[0].getInfluenceRadius()) {
            return false;
         }
      }
      Point3d pnt = new Point3d(coords);
      update(pnt, nodes);
      return true;
   }
   
   @Override
   public void update(Point3d pnt, MFreeNode3d[] nodes) {
      this.myPnt.set(pnt);
   
      this.myNodes = nodes;
      if (M == null) {
         M = new MatrixNd(nBasis, nBasis);
      }
      if (Minv == null) {
         Minv = new MatrixNd(nBasis, nBasis);
      }
      computeM(M, pnt, nodes);

      SVDecomposition svd = new SVDecomposition(M);
      svd.pseudoInverse(Minv);
      if (svd.condition()>1e10) {
         System.out.println("Warning: poor condition number, "+svd.condition());
      }
   }
   
   @Override
   public double eval(int nidx) {
      return eval(myNodes[nidx], myNodes, myPnt, Minv);
   }

   public double eval(MFreeNode3d node, MFreeNode3d[] nodes, Point3d pnt) {
      MatrixNd _mInv = new MatrixNd(nBasis,nBasis);
      computeMInv(_mInv, pnt, nodes);
      return eval(node, nodes, pnt, _mInv);
   }
   

   //   public double eval(double x, double y, double z) {
   //      Point3d _pnt = new Point3d();
   //      _pnt.x = x;
   //      _pnt.y = y;
   //      _pnt.z = z;
   //      return eval(_pnt);
   //   }
   //   
   //   public double eval(double[] in) {
   //      return eval(in[0], in[1], in[2]);
   //   }
   //
   //   public int getInputSize() {
   //      return 3;
   //   }

   public void computeDDMInv(MatrixNd DDMInv, int di, int dj, MatrixNd MInv, Point3d pnt, MFreeNode3d[] nodeList) {
      
      MatrixNd mTmpi = new MatrixNd(nBasis,nBasis);
      MatrixNd mTmpj = new MatrixNd(nBasis,nBasis);
      
      
      // M^{-1}_{,kl} = M^{-1} [  (M_{,k}M^{-1})(M_{,l}M^{-1}) + (M_{,l}M^{-1})(M_{,k}M^{-1}) - M_{,kl}M^{-1}  ]
      computeDM(mTmpi,di,pnt,nodeList);
      mTmpi.mul(MInv);
      if (di != dj) {
         computeDM(mTmpj,di,pnt,nodeList);
         mTmpj.mul(MInv);
         
         DDMInv.mul(mTmpi,mTmpj);
         mTmpj.mul(mTmpi);
         DDMInv.add(mTmpj);
         
      } else {
         DDMInv.mul(mTmpi,mTmpi);
         DDMInv.scale(2);
      }
      
      computeDDM(mTmpi, di, dj, pnt, nodeList);
      mTmpi.mul(MInv);
      DDMInv.sub(mTmpi);
      
      DDMInv.mul(MInv, DDMInv);
            
   }
   
   public void computeDMInv(MatrixNd DMInv, int di, MatrixNd MInv, Point3d pnt, MFreeNode3d[] nodeList) {
      
      // M^{-1}_{,k} = -M^{-1}M_{,k}M^{-1}
      computeDM(DMInv, di, pnt, nodeList);
      DMInv.mul(MInv, DMInv);
      DMInv.mul(DMInv, MInv);
      DMInv.scale(-1);
      
   }
   
   public void computeDDM(MatrixNd DM, int di, int dj, Point3d pnt, MFreeNode3d[] nodeList) {
      
      MatrixNd cc = new MatrixNd(nBasis,nBasis);
      DM.setSize(nBasis, nBasis);
      DM.setZero();
      int derivatives[] = new int[3];
      derivatives[di] = 1;
      derivatives[dj] = derivatives[dj]+1;
      
      for (MFreeNode3d node : nodeList) {
         Point3d pos = node.getRestPosition();
         computePCorrelation(cc, pos.x, pos.y, pos.z);
         DM.scaledAdd(node.getWeightFunction().evalDerivative(pnt,derivatives), cc);
      }
      
   }
   
   public void computeDM(MatrixNd DM, int dIdx, Point3d pnt, MFreeNode3d[] nodeList) {
      
      MatrixNd cc = new MatrixNd(nBasis,nBasis);
      DM.setSize(nBasis, nBasis);
      DM.setZero();
      int[] derivatives = new int[3];
      derivatives[dIdx] = 1;
      
      for (MFreeNode3d node : nodeList) {
         Point3d pos = node.getRestPosition();
         computePCorrelation(cc, pos.x, pos.y, pos.z);
         DM.scaledAdd(node.getWeightFunction().evalDerivative(pnt, derivatives), cc);
      }
      
   }
   
   protected double evalDerivative(MFreeNode3d node, MFreeNode3d[] nodes, Point3d in, VectorNd pXi, double wx, int[] derivatives, MatrixNd MInv) {
      int dx = derivatives[0];
      int dy = derivatives[1];
      int dz = derivatives[2];
      double out =  0;

      VectorNd pTmp = new VectorNd(nBasis);
      MFreeWeightFunction fun = node.getWeightFunction();
      
      VectorNd p = new VectorNd(nBasis);
      computeP(p, in.x, in.y, in.z);     
      
      if (dx+dy+dz == 1) {
         
         MatrixNd DMInv = new MatrixNd(nBasis, nBasis);
         VectorNd pk = new VectorNd(nBasis);
         double wk;
         if (dz==1) {
            computeDMInv(DMInv, 2, MInv, in, nodes);
            computeDP(pk, in.x, in.y, in.z, 0, 0, 1);
            wk = fun.evalDerivative(in.x, in.y, in.z, 0, 0, 1);
         } else if (dy==1) {
            computeDMInv(DMInv, 1, MInv, in, nodes);
            computeDP(pk, in.x, in.y, in.z, 0, 1, 0);
            wk = fun.evalDerivative(in.x, in.y, in.z, 0, 1, 0);
         } else {
            computeDMInv(DMInv, 0, MInv, in, nodes);
            computeDP(pk, in.x, in.y, in.z, 1, 0, 0);
            wk = fun.evalDerivative(in.x, in.y, in.z, 1, 0, 0);
         }
         
         pTmp.mulTranspose(MInv, pk);
         out = wx*pTmp.dot(pXi);
         
         pTmp.mulTranspose(DMInv, p);
         out += wx*pTmp.dot(pXi);
         
         pTmp.mulTranspose(MInv, p);
         out += wk*pTmp.dot(pXi);
         return out;
         
      } else if (dx+dy+dz == 2) {
         
         MatrixNd MklInv = new MatrixNd(nBasis,nBasis);
         MatrixNd MkInv = new MatrixNd(nBasis,nBasis);
         MatrixNd MlInv;
         VectorNd pkl = new VectorNd(nBasis);
         VectorNd pk = new VectorNd(nBasis);
         VectorNd pl;

         
         double wkl;
         double wk;
         double wl;
         
         if (dx==1 && dy==1) {
            computeDDMInv(MklInv, 0, 1, MInv, in, nodes);
            computeDMInv(MkInv, 0, MInv, in, nodes);
            MlInv = new MatrixNd(nBasis,nBasis);
            computeDM(MlInv, 1, in, nodes);
            
            computeDP(pkl, in.x, in.y, in.z, 1, 1, 0);
            computeDP(pk, in.x, in.y, in.z, 1, 0, 0);
            pl = new VectorNd(nBasis);
            computeDP(pl, in.x, in.y, in.z, 0, 1, 0);
            
            wkl = fun.evalDerivative(in.x, in.y, in.z, 1, 1, 0);
            wk = fun.evalDerivative(in.x, in.y, in.z, 1, 0, 0);
            wl = fun.evalDerivative(in.x, in.y, in.z, 0, 1, 0);
            
         } else if (dx==1 && dz==1) {
            computeDDMInv(MklInv, 0, 2, MInv, in, nodes);
            computeDMInv(MkInv, 0, MInv, in, nodes);
            MlInv = new MatrixNd(nBasis,nBasis);
            computeDM(MlInv, 2, in, nodes);
            
            computeDP(pkl, in.x, in.y, in.z, 1, 0, 1);
            computeDP(pk, in.x, in.y, in.z, 1, 0, 0);
            pl = new VectorNd(nBasis);
            computeDP(pl, in.x, in.y, in.z, 0, 0, 1);
            
            wkl = fun.evalDerivative(in.x, in.y, in.z, 1, 0, 1);
            wk = fun.evalDerivative(in.x, in.y, in.z, 1, 0, 0);
            wl = fun.evalDerivative(in.x, in.y, in.z, 0, 0, 1);
            
         } else if (dy==1 && dz==1){
            computeDDMInv(MklInv, 1, 2, MInv, in, nodes);
            computeDMInv(MkInv, 1, MInv, in, nodes);
            MlInv = new MatrixNd(nBasis,nBasis);
            computeDM(MlInv, 2, in, nodes);
            
            computeDP(pkl, in.x, in.y, in.z, 0, 1, 1);
            computeDP(pk, in.x, in.y, in.z, 0, 1, 0);
            pl = new VectorNd(nBasis);
            computeDP(pl, in.x, in.y, in.z, 0, 0, 1);
            
            wkl = fun.evalDerivative(in.x, in.y, in.z, 0, 1, 1);
            wk = fun.evalDerivative(in.x, in.y, in.z, 0, 1, 0);
            wl = fun.evalDerivative(in.x, in.y, in.z, 0, 0, 1);
            
         } else if (dx==2) {
            computeDDMInv(MklInv, 0, 0, MInv, in, nodes);
            computeDMInv(MkInv, 0, MInv, in, nodes);
            MlInv = MkInv;
            
            computeDP(pkl, in.x, in.y, in.z, 2, 0, 0);
            computeDP(pk, in.x, in.y, in.z, 1, 0, 0);
            pl = pk;
            
            wkl = fun.evalDerivative(in.x, in.y, in.z, 2, 0, 0);
            wk = fun.evalDerivative(in.x, in.y, in.z, 1, 0, 0);
            wl = wk;
            
         } else if (dy==2) {
            computeDDMInv(MklInv, 1, 1, MInv, in, nodes);
            computeDMInv(MkInv, 1, MInv, in, nodes);
            MlInv = MkInv;
            
            computeDP(pkl, in.x, in.y, in.z, 0, 2, 0);
            computeDP(pk, in.x, in.y, in.z, 0, 1, 0);
            pl = pk;
            
            wkl = fun.evalDerivative(in.x, in.y, in.z, 0, 2, 0);
            wk = fun.evalDerivative(in.x, in.y, in.z, 0, 1, 0);
            wl = wk;
            
         } else {
            computeDDMInv(MklInv, 2, 2, MInv, in, nodes);
            computeDMInv(MkInv, 2, MInv, in, nodes);
            MlInv = MkInv;
            
            computeDP(pkl, in.x, in.y, in.z, 0, 0, 2);
            computeDP(pk, in.x, in.y, in.z, 0, 0, 1);
            pl = pk;
            
            wkl = fun.evalDerivative(in.x, in.y, in.z, 0, 0, 2);
            wk = fun.evalDerivative(in.x, in.y, in.z, 0, 0, 1);
            wl = wk;
         }
         
         pTmp.mulTranspose(MInv, pkl);
         out = wx*pTmp.dot(pXi);
         
         pTmp.mulTranspose(MlInv, pk);
         out += wx*pTmp.dot(pXi);
         
         pTmp.mulTranspose(MInv, pk);
         out += wl*pTmp.dot(pXi);
         
         pTmp.mulTranspose(MkInv, pl);
         out += wx*pTmp.dot(pXi);
         
         pTmp.mulTranspose(MklInv, p);
         out += wx*pTmp.dot(pXi);
         
         pTmp.mulTranspose(MkInv, p);
         out += wl*pTmp.dot(pXi);
         
         pTmp.mulTranspose(MInv, pl);
         out += wk*pTmp.dot(pXi);
         
         pTmp.mulTranspose(MlInv, p);
         out += wk*pTmp.dot(pXi);
         
         pTmp.mulTranspose(MInv, p);
         out += wkl*pTmp.dot(pXi);
         
         return out;
         
      }
      
      throw new IllegalArgumentException("Derivatives only defined up to second-order");
   }
   
   public double evalDerivative(MFreeNode3d node,  MFreeNode3d[] nodes, Point3d in, int[] derivatives, MatrixNd MInv) {
      
      VectorNd pi = new VectorNd(nBasis);
      
      MFreeWeightFunction fun = node.getWeightFunction();
      Point3d xi = node.getRestPosition();
      double w = fun.eval(in);
      computeP(pi, xi.x, xi.y, xi.z);
      
      return evalDerivative(node, nodes, in, pi, w, derivatives, MInv);
      
   }
   
   @Override
   public void evalDerivative(int nidx, Vector3d dNds) {
      int[] dd = {1,0,0};
      dNds.x = evalDerivative(myNodes[nidx], myNodes, myPnt, dd, Minv);
      dd[0] = 0; dd[1] = 1;
      dNds.y = evalDerivative(myNodes[nidx], myNodes, myPnt, dd, Minv);
      dd[1] = 0; dd[2] = 1;
      dNds.z = evalDerivative(myNodes[nidx], myNodes, myPnt, dd, Minv);
   }

   public double evalDerivative(MFreeNode3d node, MFreeNode3d[] nodes, Point3d in, int[] derivatives) {
      MatrixNd MInv = new MatrixNd(nBasis,nBasis);
      computeMInv(MInv, in, nodes);
      return evalDerivative(node, nodes, in, derivatives, MInv);
   }

   @Override
   public Point3d getCoordinate() {
      return myPnt;
   }

   @Override
   public MFreeNode3d[] getNodes() {
      return myNodes;
   }

   /**
    * Compute all shape functions at once
    * @param N shape function values
    */
   public void eval(VectorNd N) {
      N.setSize(myNodes.length);
      for (int i=0; i<myNodes.length; ++i) {
         N.set(i, eval(myNodes[i], myNodes, myPnt, Minv));
      }
   }

}
