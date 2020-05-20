/**
 * Copyright (c) 2020, by the Authors: Fabien Pean
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import maspack.matrix.NumericalException;

public class QuadratureGaussLegendre {
   
   // https://doi.org/10.1016/j.cma.2014.04.008
   public static final QuadraturePoint[] useStroud13() {
      QuadraturePoint[] qp = new QuadraturePoint[13];
      double alpha = 0.0;
      double beta = -4.95848171425711152814212423642879E-1;
      double gamma = 2.52937117448425813473892559293236E-2;
      double lambda = 8.80304406699309780477378182098603E-1;
      double mu = 7.95621422164095415429824825675787E-1;
      double A = 1.68421052631578947368421052631579;
      double B = 5.44987351277576716846907821808944E-1;
      double C = 5.07644227669791704205723757138424E-1;

      qp[0] = new QuadraturePoint(alpha,alpha,alpha,A);
      qp[1] = new QuadraturePoint(lambda,beta,beta,B);
      qp[2] = new QuadraturePoint(-lambda,-beta,-beta,B);
      qp[3] = new QuadraturePoint(beta,lambda,beta,B);
      qp[4] = new QuadraturePoint(-beta,-lambda,-beta,B);
      qp[5] = new QuadraturePoint(beta,beta,lambda,B);
      qp[6] = new QuadraturePoint(-beta,-beta,-lambda,B);
      qp[7] = new QuadraturePoint(mu,mu,gamma,C);
      qp[8] = new QuadraturePoint(-mu,-mu,-gamma,C);
      qp[9] = new QuadraturePoint( mu,gamma,mu,C);
      qp[10] = new QuadraturePoint(-mu,-gamma,-mu,C);
      qp[11] = new QuadraturePoint(gamma,mu,mu,C);
      qp[12] = new QuadraturePoint(-gamma,-mu,-mu,C);
      return qp;
   }

   private static final void computeRule1D(int numPoints, double x[], double w[]) {
      int i, j, n;
      double z, zPrev, root, p3, p2, p1;
      double eps = 1e-09;

      // The weights and abscissa values are given for a range between -1 and 1.
      // The values are symmetric around 0, only half are computed.
      n = (numPoints + 1) / 2;

      // Compute the Legendre polynomial p1 at z using Newton-Raphson's
      // https://rosettacode.org/wiki/Numerical_integration/Gauss-Legendre_Quadrature
      for(i = 0; i < n; ++i) {
         z = Math.cos(Math.PI * ( (i+1) - 0.25) / (numPoints + 0.5));
         do {
            p1 = 1.0;
            p2 = 0.0;
            for(j = 1; j < numPoints+1; ++j) {
               p3 = p2;
               p2 = p1;
               p1 = ((2.0 * j - 1.0) * z * p2 - (j-1) * p3) / (j);
            }
            root = numPoints * (z * p1 - p2) / (z * z - 1.0);
            zPrev = z;
            z = zPrev - p1 / root;
         }
         while(Math.abs(z - zPrev) > eps);

         x[i] = z;
         x[numPoints - 1 - i] = -z;
         w[i] = 2.0 / ((1.0 - z * z) * root * root);
         w[numPoints - 1 - i] = w[i];
      }
   }
      
   public static final QuadraturePoint[] computePoints(int na, int nb, int nc) {
      return computePoints(na, nb, nc, new double[0][0]);
   }

   public static final QuadraturePoint[] computePoints(int na, int nb, int nc, double[][] nodePos) {
      double[] xa = new double[na];
      double[] wa = new double[na];
      computeRule1D(na, xa, wa);
      
      double[] xb = null;
      double[] wb = null;
      double[] xc = null;
      double[] wc = null;
      
      if(nb != 0) {
         xb = new double[nb];
         wb = new double[nb];
         computeRule1D(nb, xb, wb);
      }
      else {
         xb = new double[] { 0 };
         wb = new double[] { 1 };
         nb = 1;
      }
      
      if(nc != 0) {
         xc = new double[nc];
         wc = new double[nc];
         computeRule1D(nc, xc, wc);
      }
      else {
         xc = new double[] { 0 };
         wc = new double[] { 1 };
         nc = 1;
      }

      double[][] coords = computeTensorProduct3(xa, xb, xc, wa, wb, wc);
      
      // re-order to align with nodes
      reorderICoordsToNodes(coords, nodePos);
      
      QuadraturePoint[] qp = new QuadraturePoint[na * nb * nc];

      for(int i = 0; i < na * nb * nc; i++) {
         qp[i] = new QuadraturePoint(coords[i][0], coords[i][1], coords[i][2], coords[i][3]);
      }
      
      return qp;
   }

   private static final double[][] computeTensorProduct3(
      double[] posa, double[] posb, double[] posc, double[] wgtsa, double[] wgtsb, double[] wgtsc) {
      int na = posa.length;
      int nb = posb.length;
      int nc = posc.length;
      int n = na * nb * nc;

      double[][] coords = new double[n][4];

      int idx = 0;
      for(int i = 0; i < na; i++) {
         for(int j = 0; j < nb; j++) {
            for(int k = 0; k < nc; k++) {
               coords[idx][0] = posa[i];
               coords[idx][1] = posb[j];
               coords[idx][2] = posc[k];
               coords[idx][3] = wgtsa[i] * wgtsb[j] * wgtsc[k];
               idx++;
            }
         }
      }
      return coords;
   }

   private static final boolean reorderICoordsToNodes(double[][] coords, double[][] nodeCoords) {

      int nNodes = nodeCoords.length;
      int nICoords = coords.length;

      if(nICoords < nNodes) {
         return false;
      }
      double dx, dy, dz;
      double dist, minDist;
      int closest;
      for(int i = 0; i < nNodes; i++) {
         minDist = Double.MAX_VALUE;
         closest = i;
         for(int j = i; j < nICoords; j++) {
            dx = (coords[j][0] - nodeCoords[i][0]);
            dy = (coords[j][1] - nodeCoords[i][1]);
            dz = (coords[j][2] - nodeCoords[i][2]);
            dist = dx * dx + dy * dy + dz * dz;
            if(dist < minDist) {
               closest = j;
               minDist = dist;
            }
         }
         if(closest != i) {
            double tmp;
            for(int j = 0; j < 4; j++) {
               tmp = coords[closest][j];
               coords[closest][j] = coords[i][j];
               coords[i][j] = tmp;
               ;
            }
         }
      }
      return true;
   }
   
   static double[] GL2_root = new double[] {0.5773502691896257, -0.5773502691896257};
   static double[] GL2_wght = new double[] {1, 1};
   
   static double[] GL3_root = new double[] {0.7745966692414834, 0.0, -0.7745966692414834};
   static double[] GL3_wght = new double[] {0.5555555555555556, 0.8888888888888888, 0.5555555555555556};
   
   static double[] GL4_root = new double[] {0.8611363115940526, 0.3399810435848563, -0.3399810435848563, -0.8611363115940526};
   static double[] GL4_wght = new double[] {0.3478548451374538, 0.6521451548625461, 0.6521451548625461, 0.3478548451374538};
   
   
   public static void main(String[] args) {
      double eps = 1e-9;
      {
         int n = 2;
         double[] x = new double[n];
         double[] w = new double[n];
         computeRule1D (n, x, w);
         for(int i=0; i<n; ++i) {
            if(Math.abs (x[i]-GL2_root[i]) > eps)
               throw new NumericalException();
            if(Math.abs (w[i]-GL2_wght[i]) > eps)
               throw new NumericalException();
         }
      }
      {
         int n = 3;
         double[] x = new double[n];
         double[] w = new double[n];
         computeRule1D (n, x, w);
         for(int i=0; i<n; ++i) {
            if(Math.abs (x[i]-GL3_root[i]) > eps)
               throw new NumericalException();
            if(Math.abs (w[i]-GL3_wght[i]) > eps)
               throw new NumericalException();
         }
      }
      {
         int n = 4;
         double[] x = new double[n];
         double[] w = new double[n];
         computeRule1D (n, x, w);
         for(int i=0; i<n; ++i) {
            if(Math.abs (x[i]-GL4_root[i]) > eps)
               throw new NumericalException();
            if(Math.abs (w[i]-GL4_wght[i]) > eps)
               throw new NumericalException();
         }
      }
   }
}
