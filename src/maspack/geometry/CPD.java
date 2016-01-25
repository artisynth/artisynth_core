/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.util.ArrayList;

import maspack.matrix.AffineTransform3d;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.LUDecomposition;
import maspack.matrix.Matrix3d;
import maspack.matrix.MatrixNd;
import maspack.matrix.Point3d;
import maspack.matrix.SVDecomposition3d;
import maspack.matrix.ScaledRigidTransform3d;
import maspack.matrix.Vector3d;

/**
 * @author antonio
 * 
 * Coherent Point Drift implementation
 * 
 * Point Set Registration: Coherent Point Drift, Andriy Myronenko and Xubo Song
 * 2010
 * 
 */
public class CPD {

   public static int DEFAULT_MAX_ITERS = 1000;
   public static boolean verbose = false;
   

   /**
    * Uses the rigid CPD algorithm to align a set of points
    * @param X reference input points
    * @param Y points to register
    * @param w weight, accounting to noise (w=0 --&gt; no noise)
    * @param tol will iterative until objective function changes by less than this
    * @param maxIters maximum number of iterations
    * @param allowScaling whether or not to allow scaling
    * @param TY transformed points
    * @param trans initial guess of scaled rigid transform
    * @param sigma2Holder initial guess of variance
    * @return the scaled rigid transform for registration
    */
   public static ScaledRigidTransform3d rigid(Point3d[] X, Point3d[] Y,
      double w, double tol, int maxIters, boolean allowScaling, Point3d[] TY,
      ScaledRigidTransform3d trans, double[] sigma2Holder) {

      int M = Y.length;
      int N = X.length;
      
      if (trans == null) {
         trans = new ScaledRigidTransform3d();
         transformPoints(Y, TY);
      } else {
         transformPoints(Y, trans, TY);
      }
      
      double sigma2;
      if (sigma2Holder == null || sigma2Holder[0] < 0) {
         sigma2 = computeVariance(X, TY, null, 1.0/M);
      } else {
         sigma2 = sigma2Holder[0];
      }
      
      SVDecomposition3d svd = new SVDecomposition3d();
      
      Matrix3d R = new Matrix3d(trans.R);
      Vector3d t = new Vector3d(trans.p);
      double s = trans.s;
      
      double [][] P = new double[M][N];
      double [] P1 = new double[M];
      double [] Pt1 = new double[N];
      double Np;
      
      double[] tr = new double[2];
      
      Matrix3d A = new Matrix3d();
      Matrix3d UVt = new Matrix3d();
      Matrix3d C = new Matrix3d();  C.set(0,0,1); C.set(1,1,1);

      Point3d meanx = new Point3d();
      Point3d meany = new Point3d();
            
      double err = Double.MAX_VALUE;
      int iters = 0;
      
      double q, qprev;
      q = Double.MAX_VALUE;
      
      // iterative part of algorithm
      while ( (iters < maxIters) && (err > tol) ) {
         
         // E-step
         Np = computeP(X, TY, sigma2, w, P, P1, Pt1);
         
         // M-step
         // mean
         computeMean(X, Pt1, Np, meanx);
         computeMean(Y, P1, Np, meany);
         
         // A = (X-mean(X))'*P'*(Y-mean(Y))
         // d = trace( trace(Y'*diag(P1)*Y) );
         computeAD(X, meanx, P, P1, Pt1, Y, meany, A, tr);
         
         // R = U*C*V', C= diag([1 1 det(U*V')])
         svd.factor(A);
         UVt.set(svd.getU());
         UVt.mulTranspose(svd.getV());
         C.set(2,2,UVt.determinant());
   
         R.set(svd.getU());
         R.mul(C);
         R.mulTranspose(svd.getV());
   
         // s = trace(A'*R)/trace(Y'*diag(P1)*Y)
         A.mulTransposeLeft(A, R);
         double trAtR = A.trace();
         
         if (allowScaling) {
            s = trAtR/tr[1];
         }
         
         // t = mean(X)-s*R*mean(Y)
         t.mul(R, meany);
         t.scale(-s);
         t.add(meanx);
         
         // Adjust output
         transformPoints(Y, s,  R, t, TY);
         
         // New objective function 
         qprev = q;
         // q = computeQ(X, TY, P, Np, sigma2);
         q = (tr[0] - 2*s*trAtR + s*s*tr[1])/(2*sigma2) 
            + 1.5*Np*Math.log(sigma2); 
         
         // new variance estimate
         // sigma2 = computeVariance(X, TY, P, Np);
         sigma2 = (tr[0]-s*trAtR)/(3*Np);
         if (sigma2 <= 0) {
            sigma2 = tol;
         }
         
         err = Math.abs(q-qprev);
         iters++;
      }
      
      if (verbose) {
         System.out.println("Registration complete in " + iters + " iterations");
      }
      
      // copy info over
      trans.R.set(R);
      trans.p.set(t);
      trans.setScale(s);   // triggers update of internal matrix
      
      if (sigma2Holder != null) {
         sigma2Holder[0] = sigma2;
      }
      
      return trans;

   }
   
   /**
    * Computes the A matrix used in rigid affine, A = X'*P'*Y
    * as well as the denominator for computing scale:
    * d = trace(Y'*diag(P1)*Y)
    * 
    * @param X input points
    * @param mx mean of input
    * @param P probability matrix
    * @param P1 P*ones(N,1)
    * @param Y transforming points
    * @param my mean of transforming
    * @param A output A matrix
    * @param tr trace values, <br>
    *        tr[0] = trace( (X-mx)'*diag(P'1)(X-mx) )<br>
    *        tr[1] = trace( (Y-my)'*diag(P1)(Y-my) )
    * @return denominator for computing scale
    */
   private static double computeAD(Point3d[] X, Point3d mx, double[][] P, 
      double[] P1, double[] Pt1, Point3d[] Y, Point3d my, Matrix3d A, 
      double[] tr) {
      
      int N = X.length;
      int M = Y.length;
      
      Point3d x = new Point3d();
      Point3d y = new Point3d();
      
      A.setZero();
      
      double xPx = 0;
      double yPy = 0;
      
      y.sub(Y[0], my);
      for (int n=0; n<N; n++) {
         x.sub(X[n], mx);
         addScaledOuterProduct(A, P[0][n], x, y);
         xPx += Pt1[n]*x.normSquared();
      }
      yPy += P1[0]*y.normSquared();
      
      for (int m=1; m<M; m++) {
         y.sub(Y[m], my);
         
         for (int n=0; n<N; n++) {
            x.sub(X[n], mx);
            addScaledOuterProduct(A, P[m][n], x, y);
         }
         
         yPy += P1[m]*y.normSquared();
      }
      
      tr[0] = xPx;
      tr[1] = yPy;
      
      return yPy;
   }
   
   private static void computeAD(Point3d[] X, Point3d mx, double[][] P, 
      double[] P1, double[] Pt1, Point3d[] Y, Point3d my, Matrix3d A, 
      Matrix3d D, double tr[]) {
      
      int N = X.length;
      int M = Y.length;
      
      Point3d x = new Point3d();
      Point3d y = new Point3d();
      
      A.setZero();
      D.setZero();
      double xPx = 0;
      
      y.sub(Y[0], my);
      for (int n=0; n<N; n++) {
         x.sub(X[n], mx);
         addScaledOuterProduct(A, P[0][n], x, y);
         xPx += Pt1[n]*x.normSquared();
      }
      addScaledOuterProduct(D, P1[0], y, y);
      
      for (int m=1; m<M; m++) {
         y.sub(Y[m], my);
         for (int n=0; n<N; n++) {
            x.sub(X[n], mx);
            addScaledOuterProduct(A, P[m][n], x, y);
         }
         addScaledOuterProduct(D, P1[m], y, y);
      }
      
      tr[0] = xPx;
      tr[1] = D.trace();
      
   }
   
   private static void addScaledOuterProduct(Matrix3d M, double s, 
      Vector3d v1, Vector3d v2) {
      
      M.m00 += s*v1.x*v2.x;
      M.m10 += s*v1.y*v2.x;
      M.m20 += s*v1.z*v2.x;

      M.m01 += s*v1.x*v2.y;
      M.m11 += s*v1.y*v2.y;
      M.m21 += s*v1.z*v2.y;

      M.m02 += s*v1.x*v2.z;
      M.m12 += s*v1.y*v2.z;
      M.m22 += s*v1.z*v2.z;
      
   }

   /**
    * Computes the CPD probability function P(m|n)
    * @param X Input points
    * @param TY Transformed output points
    * @param sigma2 variance
    * @param w weight to account for noise/outliers
    * @param P MxN probability matrix
    * @param P1 Mx1 vector, P*1
    * @param Pt1 Nx1 vector, trans(P)*1
    * @return Np the sum of all entries in P
    */
   public static double computeP(Point3d[] X, Point3d[] TY, double sigma2, double w,
      double[][] P, double[] P1, double [] Pt1) {
      return computeP(X, TY, sigma2, w, P, P1, Pt1, sigma2*1e-12);
   }
   
   /**
    * Computes the CPD probability function P(m|n)
    * @param X Input points
    * @param TY Transformed output points
    * @param sigma2 variance
    * @param w weight to account for noise/outliers
    * @param P MxN probability matrix
    * @param P1 Mx1 vector, P*1
    * @param Pt1 Nx1 vector, trans(P)*1
    * @param tol2 squared point tolerance
    * @return Np the sum of all entries in P
    */
   public static double computeP(Point3d[] X, Point3d[] TY, double sigma2, double w,
      double[][] P, double[] P1, double [] Pt1, double tol2) {
      
      double N = X.length;
      double M = TY.length;
      
      double Np = 0;
      double c = 2*Math.PI*sigma2;
      c = c*c*c;
      c = Math.sqrt(c);
      if (w == 1) {
         w = 1-1e-16;  // always between [0,1], so we can hard-code a tolerance here
      }
      c = c*M*w/((1-w)*N);
      
      Point3d xn, ym;
      double dx, dy, dz, d;
      
      for (int m=0; m<M; m++) {
         P1[m] = 0;
      }
      
      for (int n = 0; n < N; n++ ) {
         
         Pt1[n] = 0;
         
         xn = X[n];
         double msum = 0;
         for (int m = 0; m < M; m++) {
            ym = TY[m];
            dx = xn.x-ym.x;
            dy = xn.y-ym.y;
            dz = xn.z-ym.z;
            
            
            if (sigma2 > 0) {
               d = Math.exp(- (dx*dx+dy*dy+dz*dz)/(2*sigma2));
            } else if (tol2 > 0){
               d = Math.exp(- (dx*dx+dy*dy+dz*dz)/(2*tol2));
            } else {
               if ( dx*dx+dy*dy+dz*dz == 0 ) {
                  d = 1;
               } else {
                  d = 0;
               }
            }
            P[m][n] = d;
            msum += d;
            
         }
         msum += c;
         
         if (msum == 0) {
            msum = 1;
         }
         
         for (int m=0; m<M; m++) {
            
            P[m][n] = P[m][n]/msum;
            Pt1[n] += P[m][n];
            P1[m] += P[m][n];
            Np += P[m][n];
         }
         
      }
      
      return Np;
   }
   
   /**
    * CPD Objective function
    * @param X reference points
    * @param TY transformed input points
    * @param P probability matrix
    * @param Np sum of all probabilities
    * @param sigma2 probability variance
    * @return the objective function value
    */
   public static double computeQ( Point3d[] X, Point3d[] TY, double[][] P, 
      double Np, double sigma2) {
      return computeQ(X, TY, P, Np, sigma2, sigma2*1e-12);
   }
   
   /**
    * CPD Objective function
    * @param X reference points
    * @param TY transformed input points
    * @param P probability matrix
    * @param Np sum of all probabilities
    * @param sigma2 probability variance
    * @param tol2 squared point tolerance
    * @return the objective function value
    */
   public static double computeQ( Point3d[] X, Point3d[] TY, double[][] P, 
      double Np, double sigma2, double tol2) {
      
      double Q = 0;
      double M = TY.length;
      double N = X.length;
         
      double dx, dy, dz;
      Point3d xn, ym;
      
      for (int m = 0; m < M; m++) {
         
         ym = TY[m];
         for (int n = 0; n < N; n++) {
            xn = X[n];
            
            dx = xn.x-ym.x;
            dy = xn.y-ym.y;
            dz = xn.z-ym.z;
            
            Q += P[m][n]*(dx*dx+dy*dy+dz*dz); 
         }
      }
      
      if (sigma2 > 0) {
         Q = Q/(2*sigma2)+Np*3/2*Math.log(sigma2);
      } else if (tol2 > 0) {
         Q = Q/(2*tol2)+Np*3/2*Math.log(tol2);
      } else {
         Q = Q/(2e-12)+Np*3/2*Math.log(1e-12);
      }
      
      return Q;
      
   }
   
   /**
    * Transforms points based on rigid transform
    * @param Y input points
    * @param s scale
    * @param R rotation
    * @param t translation
    * @param TY transformed points
    */
   public static void transformPoints(Point3d[] Y, 
      double s, Matrix3d R, Vector3d t, Point3d[] TY) {
      
      for (int i=0; i<TY.length; i++) {
         TY[i].mul(R, Y[i]);
         TY[i].scaledAdd(s, TY[i], t);
      }
   }
   
   /**
    * Transforms a set of points
    * @param Y input points
    * @param trans transform
    * @param TY transformed points
    */
   public static void transformPoints(Point3d[] Y, AffineTransform3dBase trans, 
      Point3d[] TY) {
      
      for (int i=0; i<TY.length; i++) {
         TY[i].transform(trans, Y[i]);
      }
   }
   
   /**
    * Transforms a set of points by identity (copies points)
    * @param Y input points
    * @param TY transformed points
    */
   public static void transformPoints(Point3d[] Y, Point3d[] TY) {
      for (int i=0; i<TY.length; i++) {
         TY[i].set(Y[i]);
      }
   }
   
   /**
    * Transforms points based on affine transform
    * @param Y input points
    * @param A affine transform
    * @param t translation
    * @param TY transformed points
    */
   public static void transformPoints(Point3d[] Y, 
      Matrix3d A, Vector3d t, Point3d[] TY) {
      
      for (int i=0; i<TY.length; i++) {
         TY[i].mulAdd(A, Y[i], t);
      }
   }
   
   public static void transformPoints(Point3d[] Y, MatrixNd G, 
      MatrixNd W, Point3d[] TY) {
      
      for (int i=0; i<TY.length; i++) {
         TY[i].set(Y[i]);
         
         for (int j=0; j<TY.length; j++) {
            TY[i].x += G.get(i,j)*W.get(j, 0);
            TY[i].y += G.get(i,j)*W.get(j, 1);
            TY[i].z += G.get(i,j)*W.get(j, 2);
         }
      }
      
   }
   
   /**
    * Estimates the CPD variance
    * @param X N input points
    * @param TY M transformed output points
    * @param P probability matrix (if null, P(m,n) = 1/M)
    * @param Np Total sum of entries in probability matrix
    * @return the estimated variance
    */
   public static double computeVariance(Point3d [] X, Point3d [] TY, double[][] P, double Np) {
      double var = 0;
   
      double M = TY.length;
      double N = X.length;
      Point3d xn, ym;
      double dx, dy, dz;
      
      if (P == null) {
         // use P[m][n] = 1/M assumption
         
         for (int n = 0; n<N; n++) {
            xn = X[n];
            for (int m = 0; m < M; m++) {
               ym = TY[m];
               dx = xn.x-ym.x;
               dy = xn.y-ym.y;
               dz = xn.z-ym.z;
               var += (dx*dx+dy*dy+dz*dz);
            }
         }
         var = var/(3*N*M);
         
      } else {
        
         for (int n = 0; n<N; n++) {
            xn = X[n];
            for (int m = 0; m < M; m++) {
               ym = TY[m];
               dx = xn.x-ym.x;
               dy = xn.y-ym.y;
               dz = xn.z-ym.z;
               var += P[m][n]*(dx*dx+dy*dy+dz*dz);
            }
         }
         var = var/(3*Np);
         
      }
      
      return var;
   }
   
   /**
    * Compute and return the weighted mean
    * @param pnts set of points
    * @param P vector of probabilities
    * @param Np sum of probabilities
    * @param mean mean to fill, if null, creates new
    * @return the weighted mean
    */
   public static Point3d computeMean(Point3d[] pnts, double[] P, double Np, Point3d mean) {
      if (mean == null) {
         mean = new Point3d();
      }
      
      mean.setZero();
      for (int i=0; i<P.length; i++) {
         mean.scaledAdd(P[i], pnts[i]);
      }
      mean.scale(1.0/Np);
      
      return mean;
   }
    
   /**
    * Uses the rigid CPD algorithm to align a set of points
    * @param X reference input points
    * @param Y points to register
    * @param w weight, accounting to noise (w=0 --&gt; no noise)
    * @param tol will iterative until objective function changes by less than this
    * @param maxIters maximum number of iterations
    * @param allowScaling whether or not to allow scaling
    * @param TY transformed points
    * @return the scaled rigid transform for registration
    */
   public static ScaledRigidTransform3d rigid(Point3d[] X, Point3d[] Y,
      double w, double tol, int maxIters, boolean allowScaling, Point3d[] TY) {
      return rigid(X, Y, w, tol, maxIters, allowScaling, TY, null, null);
   }
   
   /**
    * Uses the rigid CPD algorithm to align two meshes
    * @param meshRef reference mesh
    * @param meshReg mesh to register
    * @param w weight, accounting to noise (w=0 --&gt; no noise)
    * @param tol will iterative until objective function changes by less than this
    * @param maxIters maximum number of iterations
    * @param allowScaling whether or not to allow scaling
    * @return the scaled rigid transform for registration
    */
   public static ScaledRigidTransform3d rigid(PolygonalMesh meshRef, 
      PolygonalMesh meshReg, double w, double tol, 
      int maxIters, boolean allowScaling) {
      
      int N = meshRef.numVertices();
      int M = meshReg.numVertices();
      
      Point3d[] x = new Point3d[N];
      Point3d[] y = new Point3d[M];
      Point3d[] match = new Point3d[M];
      
      for (int n=0; n<N; n++) {
         x[n] = meshRef.getVertices().get(n).getWorldPoint();  
      }
      for (int m=0; m<M; m++) {
         y[m] = meshReg.getVertices().get(m).getWorldPoint();
         match[m] =  new Point3d();
      }
      
      return rigid(x, y, w, tol, maxIters, allowScaling, match,
         null, null);
      
   }
   

   /**
    * Uses the affine CPD algorithm to align a set of points
    * @param X reference input points
    * @param Y points to register
    * @param w weight, accounting to noise (w=0 --&gt; no noise)
    * @param tol will iterative until objective function changes by less than this
    * @param maxIters maximum number of iterations
    * @param TY transformed points
    * @param trans initial guess of scaled rigid transform
    * @param sigma2Holder initial guess of variance
    * @return the scaled rigid transform for registration
    */
   public static AffineTransform3d affine(Point3d[] X, Point3d[] Y,
      double w, double tol, int maxIters, Point3d[] TY,
      AffineTransform3d trans, double[] sigma2Holder) {

      int M = Y.length;
      int N = X.length;

      SVDecomposition3d svd = new SVDecomposition3d();
      
      if (trans == null) {
         trans = new AffineTransform3d();
         transformPoints(Y, TY);
      } else {
         transformPoints(Y, trans, TY);
      }
      
      double sigma2;
      if (sigma2Holder == null || sigma2Holder[0] < 0) {
         sigma2 = computeVariance(X, TY, null, 1.0/M);
      } else {
         sigma2 = sigma2Holder[0];
      }
      
      Matrix3d B = new Matrix3d(trans.A);
      Vector3d t = new Vector3d(trans.p);
      
      double [][] P = new double[M][N];
      double [] P1 = new double[M];
      double [] Pt1 = new double[N];
      double Np;
      
      Matrix3d A = new Matrix3d();
      Matrix3d D = new Matrix3d();
      Matrix3d YPY = new Matrix3d();
      double[] tr = new double[2];
      
      Point3d meanx = new Point3d();
      Point3d meany = new Point3d();
            
      double err = Double.MAX_VALUE;
      int iters = 0;
      
      double q, qprev;
      q = Double.MAX_VALUE;
      
      // iterative part of algorithm
      while ( (iters < maxIters) && (err > tol) ) {
         
         // E-step
         Np = computeP(X, TY, sigma2, w, P, P1, Pt1);
         
         // M-step
         // mean
         computeMean(X, Pt1, Np, meanx);
         computeMean(Y, P1, Np, meany);
         
         // A = (X-mean(X))'*P'*(Y-mean(Y))
         // D = (Y-mean(Y))'*diag(P1)*(Y-mean(Y))
         computeAD(X, meanx, P, P1, Pt1, Y, meany, A, YPY, tr);
         
         // B = A*inverse(D)
         svd.factor(YPY);
         svd.pseudoInverse(D);   // pseudo-invert
         B.mul(A, D);
         
         // t = mean(X)-A*mean(Y)
         t.mul(B, meany);
         t.sub(meanx, t);
         
         // Adjust output
         transformPoints(Y, B, t, TY);
         
         // speedy compute Q and sigma2
         A.mulTranspose(B);
         double trABt = A.trace();
         YPY.mulTranspose(B);
         YPY.mul(B);
         double trBYPYB = YPY.trace();
         
         // compute new objective (speedy)
         qprev = q;
         // q = computeQ(X, TY, P, Np, sigma2);
         q = (tr[0] - 2*trABt + trBYPYB)/(2*sigma2) + 1.5*Np*Math.log(sigma2);
         
         // new variance estimate (speedy)
         // sigma2 = computeVariance(X, TY, P, Np);
         sigma2 = (tr[0]-trABt)/(3*Np);
         if (sigma2 <= 0) {
            sigma2 = tol;
         }
         
         err = Math.abs(q-qprev);
         iters++;
      }
      
      if (verbose) {
         System.out.println("Registration complete in " + iters + " iterations");
      }
      
      // copy info over
      trans.A.set(B);
      trans.p.set(t);
      
      if (sigma2Holder != null) {
         sigma2Holder[0] = sigma2;
      }
      
      return trans;
   }
   
   /**
    * Uses the affine CPD algorithm to align a set of points
    * @param X reference input points
    * @param Y points to register
    * @param w weight, accounting to noise (w=0 --&gt; no noise)
    * @param tol will iterative until objective function changes by less than this
    * @param maxIters maximum number of iterations
    * @param TY transformed points
    * @return the scaled rigid transform for registration
    */
   public static AffineTransform3d affine(Point3d[] X, Point3d[] Y,
      double w, double tol, int maxIters, Point3d[] TY) {
      return affine(X, Y, w, tol, maxIters, TY, null, null);
   }
   
   /**
    * Uses the affine CPD algorithm to align two meshes
    * @param meshRef reference mesh
    * @param meshReg mesh to register
    * @param w weight, accounting to noise (w=0 --&gt; no noise)
    * @param tol will iterative until objective function changes by less than this
    * @param maxIters maximum number of iterations
    * @return the scaled rigid transform for registration
    */
   public static AffineTransform3d affine(PolygonalMesh meshRef, PolygonalMesh meshReg, 
      double w, double tol, int maxIters) {
      
      int N = meshRef.numVertices();
      int M = meshReg.numVertices();
      
      Point3d[] x = new Point3d[N];
      Point3d[] y = new Point3d[M];
      Point3d[] match = new Point3d[M];
      
      for (int n=0; n<N; n++) {
         x[n] = meshRef.getVertices().get(n).getWorldPoint();  
      }
      for (int m=0; m<M; m++) {
         y[m] = meshReg.getVertices().get(m).getWorldPoint();
         match[m] =  new Point3d();
      }
      
      return affine(x, y, w, tol, maxIters, match, 
         null, null);
      
   }
   
   /**
    * Uses the coherent CPD algorithm to align a set of points
    * @param X reference input points
    * @param Y points to register
    * @param lambda weight factor for regularization term (&gt; 0)
    * @param beta2 coherence factor, beta^2 (&gt; 0)
    * @param w weight, accounting to noise (w=0 --&gt; no noise)
    * @param tol will iterative until objective function changes by less than this
    * @param maxIters maximum number of iterations
    * @param TY transformed points
    * @param sigma2Holder initial variance estimate
    */
   public static Point3d[] coherent(Point3d[] X, Point3d[] Y,
      double lambda, double beta2, double w, double tol, 
      int maxIters, Point3d[] TY, double[] sigma2Holder) {
      
      int M = Y.length;
      int N = X.length;
   
      if (TY == null) {
         TY = new Point3d[Y.length];
         for (int i=0; i<TY.length; i++) {
            TY[i] = new Point3d();
         }
      }
      
      transformPoints(Y, TY);
      
      double sigma2;
      if (sigma2Holder == null || sigma2Holder[0] < 0) {
         sigma2 = computeVariance(X, TY, null, 1.0/M);
      } else {
         sigma2 = sigma2Holder[0];
      }
      
      MatrixNd G = new MatrixNd(M, M);
      computeG(beta2, Y, G);
      
      LUDecomposition lu = new LUDecomposition(M);
      MatrixNd A = new MatrixNd(M, M);
      MatrixNd B = new MatrixNd(M, 3);
      MatrixNd PX = new MatrixNd(M, 3);
      
      MatrixNd W = new MatrixNd(M, 3);
      
      double [][] P = new double[M][N];
      double [] P1 = new double[M];
      double [] Pt1 = new double[N];
      double Np;
     
      double err = Double.MAX_VALUE;
      int iters = 0;
      
      double sigma2prev;
      
      // iterative part of algorithm
      while ( (iters < maxIters) && (err > tol) ) {
         
         // E-step
         Np = computeP(X, TY, sigma2, w, P, P1, Pt1);
         
         // M-step
         
         // set up AW = B, solve for W
         A.set(G);
         for (int i=0; i<M; i++) {
            A.add(i, i, lambda*sigma2/P1[i]);
         }
         computeCoherentRHS(P, P1, X, Y, PX, B);
         
         // solve
         // XXX may want to hook into Pardiso, set prev W as initial guess
         lu.factor(A);
         boolean nonSingular = lu.solve(W, B);
         if (!nonSingular) {
            System.out.println("CPD.coherent(...): Warning... matrix non-singular");
         }
         
         // update transformed points
         transformPoints(Y, G, W, TY);
         
         if (verbose) {
            System.out.println(TY[0]);
         }
         sigma2prev = sigma2;
         
         // update variance estimate
         double xPx = 0;
         double trPXTY = 0;
         double trTYPTY = 0;
         for (int m = 0; m<M; m++) {
            trPXTY += PX.get(m, 0)*TY[m].x + PX.get(m, 1)*TY[m].y + PX.get(m, 2)*TY[m].z;
            trTYPTY += P1[m]*TY[m].normSquared();
         }
         for (int n = 0; n<N; n++) {
            xPx += Pt1[n]*X[n].normSquared();
         }
         sigma2 = (xPx - 2*trPXTY + trTYPTY)/(3*Np);
         
         err = Math.abs(sigma2-sigma2prev);
         iters++;
      }
      
      if (verbose) {
         System.out.println("Registration complete in " + iters + " iterations");
      }
      
      if (sigma2Holder != null) {
         sigma2Holder[0] = sigma2;
      }
      
      return TY;
      
   }
   
   private static void computeCoherentRHS(double[][] P, double[] P1, 
      Point3d[] X, Point3d[] Y, MatrixNd PX, MatrixNd RHS) {
      
      int M = Y.length;
      int N = X.length;
      
      PX.setZero();
      
      double x, y, z;
      
      for (int m = 0; m < M; m++) {
         for (int n = 0; n < N; n++) {
            x = P[m][n]*X[n].x;
            y = P[m][n]*X[n].y;
            z = P[m][n]*X[n].z;
            PX.add(m, 0, x);
            PX.add(m, 1, y);
            PX.add(m, 2, z);
         }
         
         RHS.set(m, 0, PX.get(m, 0)/P1[m] - Y[m].x);
         RHS.set(m, 1, PX.get(m, 1)/P1[m] - Y[m].y);
         RHS.set(m, 2, PX.get(m, 2)/P1[m] - Y[m].z);
      }
      
   }
   
   /**
    * Compute the Gaussian kernel matrix for the coherent CPD algorithm
    * @param beta2 beta^2 factor, controlling amount of coherence (&gt; 0)
    * @param G Kernel matrix to be filled
    */
   private static void computeG(double beta2, Point3d[] Y, MatrixNd G) {
      
      int M = Y.length;
      for (int i=0; i<M; i++) {
         for (int j=0; j<M; j++) {
            G.set(i, j, Math.exp(-(Y[i].distanceSquared(Y[j]))/(2*beta2)));
         }
      }
      
   }
   
   /**
    * Uses the coherent CPD algorithm to align a set of points
    * @param X reference input points
    * @param Y points to register
    * @param lambda weight factor for regularization term (&gt; 0)
    * @param beta2 coherence factor, beta^2 (&gt; 0)
    * @param w weight, accounting to noise (w=0 --&gt; no noise)
    * @param tol will iterative until objective function changes by less than this
    * @param maxIters maximum number of iterations
    * @return TY transformed points
    */
   public static Point3d[] coherent(Point3d[] X, Point3d[] Y,
      double lambda, double beta2, double w, double tol, 
      int maxIters) {
      
      return coherent(X, Y, lambda, beta2, w, tol, maxIters, null, null);
      
   }
   
   /**
    * Uses the coherent CPD algorithm to align a set of points
    * @param X reference input points
    * @param Y points to register
    * @param lambda weight factor for regularization term (&gt; 0)
    * @param beta2 coherence factor, beta^2 (&gt; 0)
    * @param w weight, accounting to noise (w=0 --&gt; no noise)
    * @param tol will iterative until objective function changes by less than this
    * @param maxIters maximum number of iterations
    * @param TY transformed points
    */
   public static Point3d[] coherent(Point3d[] X, Point3d[] Y,
      double lambda, double beta2, double w, double tol, 
      int maxIters, Point3d[] TY) {
      
      return coherent(X, Y, lambda, beta2, w, tol, maxIters, TY, null);
      
   }
   
   /**
    * Uses the coherent CPD algorithm to align two meshes
    * @param meshRef reference mesh
    * @param meshReg mesh to register
    * @param lambda weight factor for regularization term (&gt; 0)
    * @param beta2 coherence factor, beta^2 (&gt; 0)
    * @param w weight, accounting to noise (w=0 --&gt; no noise)
    * @param tol will iterative until objective function changes by less than this
    * @param maxIters maximum number of iterations
    * @return out transformed mesh
    */
   public static PolygonalMesh coherent(PolygonalMesh meshRef, PolygonalMesh meshReg, 
      double lambda, double beta2, double w, double tol, int maxIters) {
      
      return coherent(meshRef, meshReg, lambda, beta2, w, tol, maxIters, null);
      
   }
   
   /**
    * Uses the coherent CPD algorithm to align two meshes
    * @param meshRef reference mesh
    * @param meshReg mesh to register
    * @param lambda weight factor for regularization term (&gt; 0)
    * @param beta2 coherence factor, beta^2 (&gt; 0)
    * @param w weight, accounting to noise (w=0 --&gt; no noise)
    * @param tol will iterative until objective function changes by less than this
    * @param maxIters maximum number of iterations
    * @param out transformed mesh
    */
   public static PolygonalMesh coherent(PolygonalMesh meshRef, PolygonalMesh meshReg, 
      double lambda, double beta2, double w, double tol, int maxIters, PolygonalMesh out) {
      
      int N = meshRef.numVertices();
      int M = meshReg.numVertices();
      
      Point3d[] x = new Point3d[N];
      Point3d[] y = new Point3d[M];
      Point3d[] match = new Point3d[M];
      
      int[][] faceIndices = new int[meshReg.numFaces()][];
      ArrayList<Face> faces = meshReg.getFaces();
      for (int i=0; i<meshReg.numFaces(); i++) {
         faceIndices[i] = faces.get(i).getVertexIndices();
      }
      
      for (int n=0; n<N; n++) {
         x[n] = meshRef.getVertices().get(n).getWorldPoint();  
      }
      for (int m=0; m<M; m++) {
         y[m] = meshReg.getVertices().get(m).getWorldPoint();
         match[m] =  new Point3d();
      }
      
      coherent(x, y, lambda, beta2, w, tol, maxIters, match, null);
      
      if (out == null) {
         out = new PolygonalMesh();
      } else {
         out.clear();
      }
      out.set(match, faceIndices);
      
      return out;
      
   }
}
