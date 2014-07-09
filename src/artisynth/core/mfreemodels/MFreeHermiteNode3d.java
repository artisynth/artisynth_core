/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mfreemodels;

import maspack.matrix.Point3d;
import artisynth.core.mechmodels.PointState;

public class MFreeHermiteNode3d extends MFreeNode3d {
   
   int myOrder;
   int [][][] myDerivativeIdx;
   PointState myDerivativeStates[];
   
   public MFreeHermiteNode3d() {
      super();
      setOrder(1);
   }
   
   public MFreeHermiteNode3d(int order) {
      super();
      setOrder(order);
   }
   
   public MFreeHermiteNode3d(Point3d p, int order) {
      super(p);
      setOrder(order);
   }
   
   public MFreeHermiteNode3d(double x, double y, double z) {
      super(x,y,z);
      setOrder(1);
   }
   
   public MFreeHermiteNode3d(double x, double y, double z, int order) {
      super(x,y,z);
      setOrder(order);
   }
     
   public void setOrder(int order) {
      int nD = (order+1)*(order+2)*(order+3)/6;
      this.myOrder = order;
      myDerivativeStates = new PointState[nD];
      myDerivativeIdx = new int[order+1][order+1][order+1];
      
      int idx = 0;
      for (int i=0; i<=order; i++) {
         for (int j=0; j<=order-i; j++) {
            for (int k=0; k<=order-i-j; k++) {
               myDerivativeIdx[i][j][k]  = idx;
               myDerivativeStates[idx++] = new PointState();
            }
         }
      }
      myDerivativeStates[0] = this.myState;
      
   }  
   
   public PointState getDerivativeState(int dx, int dy, int dz) {
      int idx = myDerivativeIdx[dx][dy][dz];
      return myDerivativeStates[idx];
   }

   public int getOrder() {
      return myOrder;
   }

}
