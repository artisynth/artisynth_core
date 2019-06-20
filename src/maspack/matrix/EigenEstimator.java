package maspack.matrix;

import maspack.util.*;
import java.util.Random;

public class EigenEstimator {

   // double precision
   private static final double epsmch = 1.1102230246251565E-016;
   // from ARPACK: eps23 is epsmch^(2/3)
   private static final double eps23 = Math.pow(epsmch, 2/3.0);
   private static final double safmin = 2.2250738585072014E-308;
   // safmn2 = 2^int(log(safmin/eps)/log(2)/2)
   private static final double safmn2 = 2.0020830951831009E-146;
   private static final double safmx2 = 1/safmn2;

   private static final double zero = 0.0;
   private static final double one = 1.0;

   // test for convergence using a tolerance that is computed across all the
   // ritz values
   private boolean useGlobalConvergence = false;
   private double globalTol = 0;

   NumberFormat fmt = new NumberFormat ("%12.7f");

   protected void printv (String msg, double[] vec) {
      System.out.println (msg + (new VectorNd(vec)));
   }

   public static enum Ordering {
      LM, // increasing magnitude order
      SM, // decreasing magnitude order
      LA, // increasing algebraic order
      SA, // decreasing algebraic order
      BE; // both ends, algebraic order

      public Ordering opposite() {
         switch (this) {
            case LM: {
               return Ordering.SM;
            }
            case SM: {
               return Ordering.LM;
            }
            case LA: {
               return Ordering.SA;
            }
            case SA: {
               return Ordering.LA;
            }
            default: {
               return this;
            }
         }
      }
   }

   static void printMatrix (int m, int n, MatrixNd M) {
      printMatrix (m, n, M.buf, M.width);
   }

   static void printMatrix (int m, int n, double[] buf, int w) {
      NumberFormat fmt = new NumberFormat ("%12.7f");
      for (int i=0; i<m; i++) {
         for (int j=0; j<n; j++) {
            System.out.print (fmt.format(buf[i*w+j])+' ');
         }
         System.out.println ("");
      }
   }

   /**
    * Get column j of a matrix into col
    */
   void getColumn (int m, double[] buf, int mw, int j, double[] col) {
      for (int i=0; i<m; i++) {
         col[i] = buf[i*mw+j];
      }
   }

   /**
    * Set column j of a matrix from col
    */
   void setColumn (int m, double[] buf, int mw, int j, double[] col) {
      for (int i=0; i<m; i++) {
         buf[i*mw+j] = col[i];
      }
   }

   /**
    * Scales column j of of M by s
    *
    * @param m
    * row size of M
    * @param s
    * scale amount
    * @param buf
    * matrix buffer
    * @param mw
    * matrix width such that M(i,j) = buf[i*mw+j]
    * @param j
    * column to scale
    */
   void dscal (int m, double s, double[] buf, int mw, int j) {
      for (int i=0; i<m; i++) {
         buf[i*mw+j] *= s;
      }
   }

   /**
    * Scales a vector by s.
    */
   void dscal (int n, double s, double[] v) {
      for (int i=0; i<n; i++) {
         v[i] *= s;
      }
   }

   int dsconv (int n, double[] ritz, double[] bounds, int off, double tol) {

      NumberFormat fmt = new NumberFormat ("%16.10e");

      System.out.println ("tol=" + tol);
      System.out.print ("ritz=    ");
      for (int i=off; i<off+n; i++) {
         System.out.print (fmt.format (ritz[i])+" ");
      }
      System.out.println ("");
      System.out.print ("bounds=  ");
      for (int i=off; i<off+n; i++) {
         System.out.print (fmt.format (bounds[i])+" ");
      }
      System.out.println ("");
      System.out.println ("eps23=" + eps23);

      int nconv = 0;

      if (globalTol != 0) {
         for (int i=off; i<off+n; i++) {
            if ( bounds[i] <= globalTol ) {
               nconv = nconv + 1;
            }
         }
      }
      else {
         // original code which did not use a global tolerance bound
         for (int i=off; i<off+n; i++) {
            //   %-----------------------------------------------------%
            //   | The i-th Ritz value is considered "converged"       |
            //   | when: bounds(i) .le. TOL*max(eps23, abs(ritz(i)))   |
            //   %-----------------------------------------------------%
            double temp = Math.max(eps23, Math.abs(ritz[i]) );
            if ( bounds[i] <= tol*temp ) {
               nconv = nconv + 1;
            }
         }
      }

      return nconv;
   }

   static boolean swapNeeded (Ordering order, double a, double b) {
      switch (order) {
         case SA: return a < b;
         case SM: return Math.abs(a) < Math.abs(b);
         case LA: return a > b;
         case LM: return Math.abs(a) > Math.abs(b);
         default: {
            throw new InternalErrorException (
               "Unimplemented order "+order);
         }
      }
   }            

   static void xsortr (Ordering order, int n, double[] x1, double[] x2) {

      int igap = n / 2;

      while (true) {
         if (igap == 0) {
            return;
         }
         for (int i = igap; i <= n-1; i++) {
            int j = i-igap;
            while (true) {
               if (j < 0) {
                  break;
               }
               if (swapNeeded (order, x1[j], x1[j+igap])) {
                  double temp = x1[j];
                  x1[j] = x1[j+igap];
                  x1[j+igap] = temp;
                  if (x2 != null) {
                     temp = x2[j];
                     x2[j] = x2[j+igap];
                     x2[j+igap] = temp;
                  }
               }
               else {
                  break;
               }
               j = j-igap;
            }
         }
         igap = igap / 2;
      }
   }

   /**
    * Given the eigenvalues of the symmetric tridiagonal matrix H, computes the
    * NP shifts AMU that are zeros of the polynomial of degree NP which filters
    * out components of the unwanted eigenvectors corresponding to the AMU's
    * based on some given criteria.
    *
    * @param which
    * Shift selection criteria
    * @param kev
    * kev + np gives size of H
    * @param np
    * number of shifts to be computed
    * @param ritz
    * Vector of size (kev+np) containing eigenvalues of H on input.
    * On output, unwanted eigenvalues are in the first np locations
    * and the wanted values are in the last kev locations.
    * @param bounds
    * Error bounds correspondign to the ordering in Ritz
    * @param shifts
    * Vector of length np. Contains, on output, the shifts sorted
    * into decreasing order of magnitude with respect to the ritz
    * estimates contained in bounds.
    */
   void dsgets (Ordering which, int kev, int np, double[] ritz,
                double[] bounds, double[] shifts) {

      if (which == Ordering.BE) {

//       %-----------------------------------------------------%
//       | Both ends of the spectrum are requested.            |
//       | Sort the eigenvalues into algebraically increasing  |
//       | order first then swap high end of the spectrum next |
//       | to low end in appropriate locations.                |
//       | NOTE: when np < floor(kev/2) be careful not to swap |
//       | overlapping locations.                              |
//       %-----------------------------------------------------%

         xsortr (Ordering.LA, kev+np, ritz, bounds);
         int kevd2 = kev / 2;
         if ( kev > 1 ) {
            swap (ritz, 0, Math.max(kevd2,np), Math.min(kevd2,np));
            swap (bounds, 0, Math.max(kevd2,np), Math.min(kevd2,np));
         }
      }
      else {
//       %----------------------------------------------------%
//       | LM, SM, LA, SA case.                               |
//       | Sort the eigenvalues of H into the desired order   |
//       | and apply the resulting order to BOUNDS.           |
//       | The eigenvalues are sorted so that the wanted part |
//       | are always in the last KEV locations.               |
//       %----------------------------------------------------%

         xsortr (which, kev+np, ritz, bounds);
      }

      //System.out.println ("ritz=" + (new VectorNd(ritz)));
      if (ishift == 1 && np > 0) {
//    
//       %-------------------------------------------------------%
//       | Sort the unwanted Ritz values used as shifts so that  |
//       | the ones with largest Ritz estimates are first.       |
//       | This will tend to minimize the effects of the         |
//       | forward instability of the iteration when the shifts  |
//       | are applied in subroutine dsapps.                     |
//       %-------------------------------------------------------%
//    
         xsortr (Ordering.SM, np, bounds, ritz);
         dcopy (np, ritz, shifts);
      }
      //System.out.println ("ritz=" + (new VectorNd(ritz)));
   }

   void dcopy (int n, double[] x, double[] y) {
      for (int i=0; i<n; i++) {
         y[i] = x[i];
      }
   }

   // Swap v[off1:off1+n-1] with v[off2:off2+n-1]
   // Make sure offsets do not overlap
   void swap (double[] v, int off1, int off2, int n) {
      for (int i=0; i<n; i++) {
         double tmp = v[off1+i];
         v[off1+i] = v[off2+i];
         v[off2+i] = tmp;
      }
   }

   // variant of dlascl that multiplies a general matrix.
   //
   // regular dlascl call is
   // 
   //    dlascl (type, kl, ku, cfrom, cto, m, n, A, lda, info)
   //
   // this call is
   //
   //    info =  dlascl (cfrom, cto, m, n, A, w, off)
   //
   // where (A(i,j) = A[i*w + j + off]
   //
   int glascl (double cfrom, double cto, int m, int n,
               double[] A, int w, int off) {

      int info = 0;
      if (cfrom == 0) {
         info = -4;
      }
      else if (m < 0) {
         info = -6;
      }
      else if (n < 0) {
         info = -7;
      }
      else if (w < m) {
         info = -9;
      }
      if (info != 0) {
         xerbla ("glascl", -info);
         return info;
      }

      double smlnum=1/Double.MAX_VALUE;
      double bignum=Double.MAX_VALUE;

      boolean done = false;
      double cfromc = cfrom;
      double ctoc = cto;
      do {
         double cfrom1 = cfromc*smlnum;
         double cto1 = ctoc / bignum;
         double mul;
         if ( Math.abs( cfrom1 )>Math.abs( ctoc ) && ctoc != 0 ) {
            mul = smlnum;
            done = false;
            cfromc = cfrom1;
         }
         else if ( Math.abs( cto1 )>Math.abs( cfromc ) ) {
            mul = bignum;
            done = false;
            ctoc = cto1;
         }
         else {
            mul = ctoc / cfromc;
            done = true;
         }
         for (int i=0; i<m; i++) {
            for (int j=0; j<n; j++) {
               A[i*w+j+off] *= mul;
            }
         }
      }
      while (!done);
      return 0;
   }

   double dnrm2 (int n, double[] v) {
      double magSqr = 0;
      for (int i=0; i<n; i++) {
         magSqr += v[i]*v[i];
      }
      return Math.sqrt (magSqr);
   }

   void daxpy (int n, double a, double[] x, double[] y) {

      if (n <= 0 || a == 0) {
         return;
      }
      for (int i=0; i<n; i++) {
         y[i] += a*x[i];
      }
   }

   void xerbla (String msg, int err) {
      // just a stub for now
   }

   //
   // Compute:
   //
   // y = alpha*A*x + beta*y (type == 'N')
   // y = alpha*A^T*x + beta*y (type == 'T')
   //
   
   void dgemv (
      char type, int m, int n, double alpha, double[] A, int wa,
      double[] x, double beta, double[] y) {

      if (x == y) {
         throw new IllegalArgumentException (
            "x and y must not be the same");
      }

      int leny;

      if (type == 'N' || type == 'n') {
         type = 'N';
         leny = m;
      }
      else if (type == 'T' || type == 't') {
         type = 'T';
         leny = n;
      }
      else {
         throw new IllegalArgumentException (
            "Unknown type '"+type+"'");
      }
      
      if (m == 0 || n == 0) {
         return;
      }

      if (beta == 0) {
         for (int i=0; i<leny; i++) {
            y[i] = 0;
         }
      }
      else if (beta != 1) {
         for (int i=0; i<leny; i++) {
            y[i] *= beta;
         }
      }
      if (type == 'N') {
         for (int i=0; i<m; i++) {
            double sum = 0;
            for (int j=0; j<n; j++) {
               sum += A[i*wa+j]*x[j];
            }
            y[i] += alpha*sum;
         }
      }
      else {
         for (int i=0; i<n; i++) {
            double sum = 0;
            for (int j=0; j<m; j++) {
               sum += A[j*wa+i]*x[j];
            }
            y[i] += alpha*sum;
         }
      }
   }

   double dlartg (double f, double g, double[] sincos) {

      double r, cs, sn;

      if( g == zero ) {
         cs = one;
         sn = zero;
         r = f;
      }
      else if ( f == zero) {
         cs = zero;
         sn = one;
         r = g; 
      }
      else {
         double f1 = f;
         double g1 = g;
         double scale = Math.max( Math.abs( f1 ), Math.abs( g1 ) );
         if( scale >= safmx2 ) {
            int count = 0;
            do {
               count = count + 1;
               f1 = f1*safmn2;
               g1 = g1*safmn2;
               scale = Math.max( Math.abs( f1 ), Math.abs( g1 ) );
            }
            while ( scale >= safmx2 );
            r = Math.sqrt( f1*f1+g1*g1 );
            cs = f1 / r;
            sn = g1 / r;
            for (int i=0; i<count; i++) {
               r = r*safmx2;
            }
         }
         else if ( scale <= safmn2 ) {
            int count = 0;
            do {
               count = count + 1;
               f1 = f1*safmx2;
               g1 = g1*safmx2;
               scale = Math.max( Math.abs( f1 ), Math.abs( g1 ) );
            }
            while ( scale <= safmn2 );
            r = Math.sqrt(f1*f1 + g1*g1);
            cs = f1 / r;
            sn = g1 / r;
            for (int i=0; i<count; i++) {
               r = r*safmn2;
            }
         }
         else {
            r = Math.sqrt(f1*f1 + g1*g1);
            cs = f1 / r;
            sn = g1 / r;
         }
         if ( Math.abs( f ) > Math.abs( g ) && cs < zero ) {
            cs = -cs;
            sn = -sn;
            r = -r;
         }
      }
      sincos[0] = sn;
      sincos[1] = cs;
      return r;
   }

   /**
    * h stores the triadiagonal matrix as a 2xn matrix, with the subdiagonal
    * located at h[0,1:n-1] and the main diagonal at h[1,0:n-1].
    */   
   int dseigt (double rnorm, int n, double[] d, double[] e, double[] eig,
                      double[] bounds) {


      MatrixNd Z = new MatrixNd ();
      Z.setBuffer (1, n, bounds, n);
      Z.setZero();
      Z.set (0, n-1, 1);

      double[] sub = new double[n];
      for (int i=0; i<n; i++) {
         eig[i] = d[i];
         if (i > 0) {
            sub[i] = e[i];
         }
      }
      int rcode = EigenDecomposition.tql2 (eig, sub, n, Z, 30*n);
      if (rcode != 0) {
         return rcode;
      }
//    %-----------------------------------------------%
//    | Finally determine the error bounds associated |
//    | with the n Ritz values of H.                  |
//    %-----------------------------------------------%

      for (int k=0; k<n; k++) {
         bounds[k] = rnorm*Math.abs(bounds[k]);
      }
      return 0;
   }

   /**
    * @param n
    * problem size (dimension of A)
    * @param kev
    * kev+np is size of matrix H
    * @param np
    * number of implicit shitfs to apply
    * @param shift
    * array of length np containing shifts to be applied
    * @param v
    * n X kev+np matrix, containing Arnoldi vectors on input and updated
    * Arnoldi vectors (in first kev columns) on ouput
    * @param vw
    * width of matrix v such that v(i,j) = v[i*vw+j]
    * @param d
    * (kev+np) array containing main diagonal of tridiagonal matrix
    * @param e
    * (kev+np) array containing sub diagonal of tridiagonal matrix starting at 1
    * @param resid 
    * vector of length n containing residual vector
    * @param q
    * kev+np X kev+np work matrix
    * @param qw
    * width of q such that q(i,j) = q[i*qw+j]
    */
   void xsapps (
      int n, int kev, int np, double[] shift, double[] v, int vw, double[] d,
      double[] e, double[] resid, double[] q, int qw) {

      double[] sincos = new double[2];

      int itop = 0; // was 1, reindexed
      int kplusp = kev + np;
//    %----------------------------------------------%
//    | Initialize Q to the identity matrix of order |
//    | kplusp used to accumulate the rotations.     |
//    %----------------------------------------------%
      for (int i=0; i<kplusp; i++) {
         for (int j=0; j<kplusp; j++) {
            q[i*qw+j] = (i == j ? 1 : 0);
         }
      }
//    %----------------------------------------------%
//    | Quick return if there are no shifts to apply |
//    %----------------------------------------------%
      if (np == 0) {
         return;
      }
//    %----------------------------------------------------------%
//    | Apply the np shifts implicitly. Apply each shift to the  |
//    | whole matrix and not just to the submatrix from which it |
//    | comes.                                                   |
//    %----------------------------------------------------------%
      for (int jj=0; jj<np; jj++) {
         int istart = itop;
//       %----------------------------------------------------------%
//       | Check for splitting and deflation. Currently we consider |
//       | an off-diagonal element e[i+1] negligible if           |
//       |         e[i+1] .le. epsmch*( |d[i]| + |d[i+1]| )   |
//       | for i=1:KEV+NP-1.                                        |
//       | If above condition tests true then we set e[i+1] = 0.  |
//       | Note that e[1:KEV+NP] are assumed to be non negative.  |
//       %----------------------------------------------------------%
         while (true) {
//       %------------------------------------------------%
//       | The following loop exits early if we encounter |
//       | a negligible off diagonal element.             |
//       %------------------------------------------------%
            int iend = kplusp-1;
            for (int i=istart; i<kplusp-1; i++) {
               double big = Math.abs(d[i]) + Math.abs(d[i+1]);
               if (e[i+1] <= epsmch*big) {
                  //System.out.println ("BREAK ");
                  e[i+1] = zero;
                  iend = i;

                  break;
               }
            }
            if (istart < iend) {
//          %--------------------------------------------------------%
//          | Construct the plane rotation G'(istart,istart+1,theta) |
//          | that attempts to drive d[istart+1] to zero.          |
//          %--------------------------------------------------------%
               double f = d[istart] - shift[jj];
               double g = e[istart+1];
               double r = dlartg (f, g, sincos);
               double s = sincos[0];
               double c = sincos[1];
//             %-------------------------------------------------------%
//             | Apply rotation to the left and right of H;            |
//             | H <- G' * H * G,  where G = G(istart,istart+1,theta). |
//             | This will create a "bulge".                           |
//             %-------------------------------------------------------%
               double a1 = c*d[istart]   + s*e[istart+1];
               double a2 = c*e[istart+1] + s*d[istart+1];
               double a4 = c*d[istart+1] - s*e[istart+1];
               double a3 = c*e[istart+1] - s*d[istart];
               //DDD
               d[istart]   = c*a1 + s*a2;
               d[istart+1] = c*a4 - s*a3;
               e[istart+1] = c*a3 + s*a4;
//             %----------------------------------------------------%
//             | Accumulate the rotation in the matrix Q;  Q <- Q*G |
//             %----------------------------------------------------%
               //for (int j=0; j<Math.min(istart+jj,kplusp); j++) {
               for (int j=0; j<=Math.min(istart+jj+1,kplusp-1); j++) {
                  a1               =   c*q[j*qw+istart] + s*q[j*qw+istart+1];
                  q[j*qw+istart+1] = - s*q[j*qw+istart] + c*q[j*qw+istart+1];
                  q[j*qw+istart]   = a1;
               }
//             %----------------------------------------------%
//             | The following loop chases the bulge created. |
//             | Note that the previous rotation may also be  |
//             | done within the following loop. But it is    |
//             | kept separate to make the distinction among  |
//             | the bulge chasing sweeps and the first plane |
//             | rotation designed to drive d[istart+1] to  |
//             | zero.                                        |
//             %----------------------------------------------%
               //for (int i=istart+1; i<iend-1; i++) {
               for (int i=istart+1; i<=iend-1; i++) {
//                %----------------------------------------------%
//                | Construct the plane rotation G'(i,i+1,theta) |
//                | that zeros the i-th bulge that was created   |
//                | by G(i-1,i,theta). g represents the bulge.   |
//                %----------------------------------------------%
                  f = e[i];
                  g = s*e[i+1];
//                %----------------------------------%
//                | Final update with G(i-1,i,theta) |
//                %----------------------------------%
                  e[i+1] = c*e[i+1];
                  r = dlartg (f, g, sincos);
                  s = sincos[0];
                  c = sincos[1];                  
//                %-------------------------------------------%
//                | The following ensures that e[1:iend-1], |
//                | the first iend-2 off diagonal of elements |
//                | H, remain non negative.                   |
//                %-------------------------------------------%
                  if (r < zero) {
                     r = -r;
                     c = -c;
                     s = -s;
                  }
//                %--------------------------------------------%
//                | Apply rotation to the left and right of H; |
//                | H <- G * H * G',  where G = G(i,i+1,theta) |
//                %--------------------------------------------%
                  e[i] = r;
//    
                  a1 = c*d[i]   + s*e[i+1];
                  a2 = c*e[i+1] + s*d[i+1];
                  a3 = c*e[i+1] - s*d[i];
                  a4 = c*d[i+1] - s*e[i+1];
//    
                  // DDD
                  d[i]   = c*a1 + s*a2;
                  d[i+1] = c*a4 - s*a3;
                  e[i+1] = c*a3 + s*a4;
//                %----------------------------------------------------%
//                | Accumulate the rotation in the matrix Q;  Q <- Q*G |
//                %----------------------------------------------------%
                  //for (int j=0; j<Math.min( i+jj, kplusp ); j++) {
                  for (int j=0; j<=Math.min( i+jj+1, kplusp-1 ); j++) {
                     a1       =   c*q[j*qw+i] + s*q[j*qw+i+1];
                     q[j*qw+i+1] = - s*q[j*qw+i] + c*q[j*qw+i+1];
                     q[j*qw+i]   = a1;
                  }
               }
            }
//          %--------------------------%
//          | Update the block pointer |
//          %--------------------------%
            istart = iend + 1;
//          %------------------------------------------%
//          | Make sure that e[iend] is non-negative |
//          | If not then set e[iend] <-- -e[iend] |
//          | and negate the last column of Q.         |
//          | We have effectively carried out a        |
//          | similarity on transformation H           |
//          %------------------------------------------%
            if (e[iend] < zero) {
               e[iend] = -e[iend];
               //dscal (kplusp, -one, q(1,iend), 1);
               dscal (kplusp, -one, q, qw, iend);
            }
//          %--------------------------------------------------------%
//          | Apply the same shift to the next block if there is any |
//          %--------------------------------------------------------%
            if (iend >= kplusp-1) {
               break;
            }
         }
//       %-----------------------------------------------------%
//       | Check if we can increase the the start of the block |
//       %-----------------------------------------------------%
         for (int i = itop; i<kplusp-1; i++) {
            if (e[i+1] > zero) {
               break;
            }
            itop  = itop + 1;
         }
//       %-----------------------------------%
//       | Finished applying the jj-th shift |
//       %-----------------------------------%
      }
//    %------------------------------------------%
//    | All shifts have been applied. Check for  |
//    | more possible deflation that might occur |
//    | after the last shift is applied.         |                               
//    %------------------------------------------%
      for (int i = itop; i< kplusp-1; i++) {
         double big = Math.abs(d[i]) + Math.abs(d[i+1]);
         if (e[i+1] <= epsmch*big) {
            e[i+1] = zero;
         }
      }
//    %-------------------------------------------------%
//    | Compute the (kev+1)-st column of (V*Q) and      |
//    | temporarily store the result in WORKD(N+1:2*N). |
//    | This is not necessary if e[kev+1] = 0.         |
//    %-------------------------------------------------%
      if ( e[kev] > zero ) {
         //dgemv ('N', n, kplusp, one, v, vw, q(1,kev+1), zero, work1);
         getColumn (kplusp, q, qw, kev, work2);
         dgemv ('N', n, kplusp, one, v, vw, work2, zero, work1);
      }
//
//    %-------------------------------------------------------%
//    | Compute column 1 to kev of (V*Q) in backward order    |
//    | taking advantage that Q is an upper triangular matrix |    
//    | with lower bandwidth np.                              |
//    | Place results in v(:,kplusp-kev:kplusp) temporarily.  |
//    %-------------------------------------------------------%
      for (int i = 0; i< kev; i++) {
         //dgemv ('N', n, kplusp-i+1, one, v, vw, q(1,kev-i+1), zero, work0);
         getColumn (kplusp-i, q, qw, kev-i-1, work2);
         dgemv ('N', n, kplusp-i, one, v, vw, work2, zero, work0);
         setColumn (n, v, vw, kplusp-i-1, work0);
         //dcopy (n, work0, v(1,kplusp-i+1));
      }
//    %-------------------------------------------------%
//    |  Move v(:,kplusp-kev+1:kplusp) into v(:,1:kev). |
//    %-------------------------------------------------%
      for (int j=0; j<kev; j++) {
         for (int i=0; i<n; i++) {
            v[i*vw+j] = v[i*vw+j+np];
         }
      }
      //dlacpy ('All', n, kev, v(1,np+1), vw, v, vw);
//
//    %--------------------------------------------%
//    | Copy the (kev+1)-st column of (V*Q) in the |
//    | appropriate place if e[kev+1] .ne. zero. |
//    %--------------------------------------------%
      if ( e[kev] > zero ) {
         setColumn (n, v, vw, kev, work1);
         //dcopy (n, work1, v(1,kev+1));
      }
//    %-------------------------------------%
//    | Update the residual vector:         |
//    |    r <- sigmak*r + betak*v(:,kev+1) |
//    | where                               |
//    |    sigmak = (e_{kev+p}'*Q)*e_{kev}  |
//    |    betak = e_{kev+1}'*H*e_{kev}     |
//    %-------------------------------------%
      dscal (n, q[(kplusp-1)*qw+kev-1], resid);
      if (e[kev] > zero) {
         double s = e[kev];
         for (int i=0; i<n; i++) {
            resid[i] += s*v[i*vw+kev];
         }
         //daxpy (n, e[kev+1], v(1,kev+1), resid);
      }
   }

   double rnorm0;
   double iter;

   int xgetvcnt = 0;

   /**
    * @param ido
    * set to 0 to indicate first call
    * @param itry
    * number of times xgetv0 is called
    * @param initv
    * if true, the initial vector is given resid. Otherwise, generate
    * random initial residual vector
    * @param n
    * Dimension of the problem
    * @param j
    * Index of the residual vector to be generated (1-based)
    * @param v
    * n X j array. The first J-1 columns of V contain the current
    * Arnoldi basis if this is a "restart".
    * @param vw
    * Witdh of v such that v(i,j) = v[i*vw+j]
    * @param resid
    * vector of length n containing residual
    * @param rnorm
    * returns B-norm of the residual
    */
   int xgetv0 (
      int ido, int itry, boolean initv, int n, int j, double[] v, int vw,
      double[] resid, DoubleHolder rnorm) {

      if (ido ==  0) {
//       %-------------------------------%
//       | Initialize timing statistics  |
//       | & message level for debugging |
//       %-------------------------------%
         iter   = 0;
//       %-----------------------------------------------------%
//       | Possibly generate a random starting vector in RESID |
//       | Use a LAPACK random number generator used by the    |
//       | matrix generation routines.                         |
//       |    idist = 1: uniform (0,1)  distribution;          |
//       |    idist = 2: uniform (-1,1) distribution;          |
//       |    idist = 3: normal  (0,1)  distribution;          |
//       %-----------------------------------------------------%
         if (!initv) {
            Random rand = RandomGenerator.get();
            for (int i=0; i<n; i++) {
               resid[i] = 2*rand.nextDouble()-1;
            }
            if (true) {
               VectorNd resv = new VectorNd(n);
               resv.set (resid);
               System.out.println ("v0=\n" + resv.toString ("%16.12f"));
            }
            // if (n == 6) {
            //    if (xgetvcnt == 0) {
            //       double[] vec = new double[] {
            //          0.39574246391875789,
            //          8.6496039750016962E-004,
            //          -0.92272057899825910,  
            //          -0.91656714952780050,
            //          0.11759638488413060, 
            //          -0.29962625203712179 };
            //       for (int i=0; i<n; i++) {
            //          resid[i] = vec[i];
            //       }
            //       xgetvcnt++;
            //    }
            //    else if (xgetvcnt == 1) {
            //    }
            // }
            
            
            //dlarnv (2, iseed, n, resid);
         }
//       %----------------------------------------------------------%
//       | Force the starting vector into the range of OP to handle |
//       | the generalized problem when B is possibly (singular).   |
//       %----------------------------------------------------------%
      }
//    %------------------------------------------------------%
//    | Starting vector is now in the range of OP; r = OP*r; |
//    | Compute B-norm of starting vector.                   |
//    %------------------------------------------------------%
      dcopy (n, resid, work0);

      rnorm0 = dnrm2(n, resid);
      rnorm.value  = rnorm0;
//    %---------------------------------------------%
//    | Exit if this is the very first Arnoldi step |
//    %---------------------------------------------%
      if (j == 1) {
         return 0;
      }
//    %----------------------------------------------------------------
//    | Otherwise need to B-orthogonalize the starting vector against |
//    | the current Arnoldi basis using Gram-Schmidt with iter. ref.  |
//    | This is the case where an invariant subspace is encountered   |
//    | in the middle of the Arnoldi factorization.                   |
//    |                                                               |
//    |       s = V^{T}*B*r;   r = r - V*s;                           |
//    |                                                               |
//    | Stopping criteria used for iter. ref. is discussed in         |
//    | Parlett's book, page 107 and in Gragg & Reichel TOMS paper.   |
//    %---------------------------------------------------------------%
      while (iter <= 5) {
//       %-----------------------------------%
//       | Perform iterative refinement step |
//       %-----------------------------------%

         dgemv ('T', n, j-1, one, v, vw, work0, zero, work1);
         dgemv ('N', n, j-1, -one, v, vw, work1, one, resid);
//       %----------------------------------------------------------%
//       | Compute the B-norm of the orthogonalized starting vector |
//       %----------------------------------------------------------%
         dcopy (n, resid, work0);
         rnorm.value = dnrm2(n, resid);
//       %--------------------------------------%
//       | Check for further orthogonalization. |
//       %--------------------------------------%
         if (rnorm.value > 0.717*rnorm0) {
            return 0;
         }
//
         iter = iter + 1;
         rnorm0 = rnorm.value;
      }
//    %------------------------------------%
//    | Iterative refinement step "failed" |
//    %------------------------------------%
      for (int jj = 0; jj<n; jj++) {
         resid[jj] = zero;
      }
      rnorm.value = zero;
      return -1;
   }      


   int jsave;

  /**
    * @param ido
    * set to 0 to indicate first call
    * @param n
    * dimension of the problem
    * @param k
    * current order of h and number of columns of v
    * @param np
    * Number of additional Arnoldi steps to take.
    * @param resid
    * Residual vector of length n (input/output)
    * @param rnorm
    * Norm of the residual (input/output)
    * @param v
    * n X k+np matrix (input/output)
    * @param wv
    * witdh of v, such that v(i,j) = v[i*vw+j]
    * @param d
    * k+np diagonal entries of tridiagonal matrix (input/output)
    * @param e
    * k+np-1 off-diagonal entries of tridiagonal matrix, starting at 1
    * (input/output)
    * @param A
    * computes y = A*x, where A is the problem matrix 
    */

   int xsaitr (int ido, int n, int k, int np, double[] resid, 
               DoubleHolder rnorm, double[] v, int vw, double[] d, double[] e,
               LinearTransformNd A) {

      //VectorNd workVecP = workVec0; // was workd(ipj)
      VectorNd workVecR = workVec1; // was workd(irj)
      VectorNd workVecV = workVec2; // was workd(ivj)

      double[] workp = work0;
      double[] workr = work1;
      double[] workv = work2;

      int j; // note: j is 1-based

      if (ido == 0) {

//       %------------------------------%
//       | Initial call to this routine |
//       %------------------------------%
//
//       %--------------------------------%
//       | Pointer to the current step of |
//       | the factorization to build     |
//       %--------------------------------%

         j      = k + 1;
//
//       %------------------------------------------%
//       | Pointers used for reverse communication  |
//       | when using WORKD.                        |
//       %------------------------------------------%
      }
      else {
         j = jsave;
      }
      
//
//    %-------------------------------------------------%
//    | When in reverse communication mode one of:      |
//    | STEP3, STEP4, ORTH1, ORTH2, RSTART              |
//    | will be true                                  |
//    | STEP3: return from computing OP*v_{j}.          |
//    | STEP4: return from computing B-norm of OP*v_{j} |
//    | ORTH1: return from computing B-norm of r_{j+1}  |
//    | ORTH2: return from computing B-norm of          |
//    |        correction to the residual vector.       |
//    | RSTART: return from OP computations needed by   |
//    |         xgetv0.                                 |
//    %-------------------------------------------------%

//    %------------------------------%
//    | Else this is the first step. |
//    %------------------------------%
//
//    %--------------------------------------------------------------%
//    |                                                              |
//    |        A R N O L D I     I T E R A T I O N     L O O P       |
//    |                                                              |
//    | Note:  B*r_{j-1} is already in WORKD(1:N)=WORKD(IPJ:IPJ+N-1) |
//    %--------------------------------------------------------------%

      while (true) {

//
//       %---------------------------------------------------------%
//       | Check for exact zero. Equivalent to determing whether a |
//       | j-step Arnoldi factorization is present.                |
//       %---------------------------------------------------------%

         if (rnorm.value <= zero) {

//          %---------------------------------------------------%
//          | Invariant subspace found, generate a new starting |
//          | vector which is orthogonal to the current Arnoldi |
//          | basis and continue the iteration.                 |
//          %---------------------------------------------------%

//          %---------------------------------------------%
//          | ITRY is the loop variable that controls the |
//          | maximum amount of times that a restart is   |
//          | attempted. NRSTRT is used by stat.h         |
//          %---------------------------------------------%
            int itry   = 1;
            while (true) {

//             %--------------------------------------%
//             | If in reverse communication mode and |
//             | RSTART = true flow returns here.    |
//             %--------------------------------------%

               double ierr = xgetv0 (
                  ido, itry, false, n, j, v, vw, resid, rnorm);
               if (ierr < 0) {
                  itry = itry + 1;
                  if (itry > 3) { 
//  
//                   %------------------------------------------------%
//                   | Give up after several restart attempts.        |
//                   | Set INFO to the size of the invariant subspace |
//                   | which spans OP and exit.                       |
//                   %------------------------------------------------%
                     jsave = j;
                     return j - 1;
                  }
               }
               else {
                  break;
               }
            }
         }

//       %---------------------------------------------------------%
//       | STEP 2:  v_{j} = r_{j-1}/rnorm and p_{j} = p_{j}/rnorm  |
//       | Note that p_{j} = B*r_{j-1}. In order to avoid overflow |
//       | when reciprocating a small RNORM, test against lower    |
//       | machine bound.                                          |
//       %---------------------------------------------------------%

         setColumn (n, v, vw, j-1/*ZB*/, resid);
         //dcopy (n, resid, v(1,j));
         if (rnorm.value >= safmin) {
            double temp1 = one / rnorm.value;
            dscal (n, temp1, v, vw, j-1/*ZB*/);
            dscal (n, temp1, workp);
         }
         else {
//           %-----------------------------------------%
//           | To scale both v_{j} and p_{j} carefully |
//           | use LAPACK routine SLASCL               |
//           %-----------------------------------------%
            //int infol;

            //infol = glascl (rnorm.value, one, n, 1, v, vw, /*off=*/j-1/*ZB*/);
            //dlascl ("General", i, i, rnorm, one, n, 1, 
            //        v(1,j), n, infol);
            //infol = glascl (rnorm.value, one, n, 1, workp, 1, /*off=*/0);
            //dlascl ("General", i, i, rnorm, one, n, 1, 
            //        workd(ipj), n, infol);
         }
//
//       %------------------------------------------------------%
//       | STEP 3:  r_{j} = OP*v_{j}; Note that p_{j} = B*v_{j} |
//       | Note that this is not quite yet r_{j}. See STEP 4    |
//       %------------------------------------------------------%
         getColumn (n, v, vw, j-1/*ZB*/, workv);
         //dcopy (n, v(1,j), workv);
//
//       %-----------------------------------%
//       | Exit in order to compute OP*v_{j} |
//       %-----------------------------------%
//
         A.mul (workVecR, workVecV);
//
//       %-----------------------------------%
//       | Back from reverse communication;  |
//       | WORKD(IRJ:IRJ+N-1) := OP*v_{j}.   |
//       %-----------------------------------%

//       %------------------------------------------%
//       | Put another copy of OP*v_{j} into RESID. |
//       %------------------------------------------%

         dcopy (n, workr, resid);

//
//       %-------------------------------------------%
//       | STEP 4:  Finish extending the symmetric   |
//       |          Arnoldi to length j. If MODE = 2 |
//       |          then B*OP = B*inv(B)*A = A and   |
//       |          we don't need to compute B*OP.   |
//       | NOTE: If MODE = 2 WORKD(IVJ:IVJ+N-1) is   |
//       | assumed to have A*v_{j}.                  |
//       %-------------------------------------------%

         dcopy (n, resid, workp);

//       %-------------------------------------%
//       | The following is needed for STEP 5. |
//       | Compute the B-norm of OP*v_{j}.     |
//       %-------------------------------------%

         double wnorm = dnrm2(n, resid);

//       %-----------------------------------------%
//       | Compute the j-th residual corresponding |
//       | to the j step factorization.            |
//       | Use Classical Gram Schmidt and compute: |
//       | w_{j} <-  V_{j}^T * B * OP * v_{j}      |
//       | r_{j} <-  OP*v_{j} - V_{j} * w_{j}      |
//       %-----------------------------------------%

//       %------------------------------------------%
//       | Compute the j Fourier coefficients w_{j} |
//       | WORKD(IPJ:IPJ+N-1) contains B*OP*v_{j}.  |
//       %------------------------------------------%

         dgemv('T', n, j, one, v, vw, workp, zero, workr);

//       %--------------------------------------%
//       | Orthgonalize r_{j} against V_{j}.    |
//       | RESID contains OP*v_{j}. See STEP 3. | 
//       %--------------------------------------%

         dgemv('N', n, j, -one, v, vw, workr, one, resid);

//       %--------------------------------------%
//       | Extend H to have j rows and columns. |
//       %--------------------------------------%

         d[j-1] = workr[j-1];/*ZB*/
         if (j == 1) {
            e[j-1] = zero;/*ZB*/
         }
         else {
            e[j-1] = rnorm.value;/*ZB*/
         }
         //System.out.println ("d["+(j-1)+"]=" + d[j-1]);
         //System.out.println ("e["+(j-1)+"]=" + e[j-1]);
//
         int iter  = 0;
//
         dcopy (n, resid, workp);

//       %------------------------------%
//       | Compute the B-norm of r_{j}. |
//       %------------------------------%

         rnorm.value = dnrm2(n, resid);

//       %-----------------------------------------------------------%
//       | STEP 5: Re-orthogonalization / Iterative refinement phase |
//       | Maximum NITER_ITREF tries.                                |
//       |                                                           |
//       |          s      = V_{j}^T * B * r_{j}                     |
//       |          r_{j}  = r_{j} - V_{j}*s                         |
//       |          alphaj = alphaj + s_{j}                          |
//       |                                                           |
//       | The stopping criteria used for iterative refinement is    |
//       | discussed in Parlett's book SEP, page 107 and in Gragg &  |
//       | Reichel ACM TOMS paper; Algorithm 686, Dec. 1990.         |
//       | Determine if we need to correct the residual. The goal is |
//       | to enforce ||v(:,1:j)^T * r_{j}|| <= eps * || r_{j} ||  |
//       %-----------------------------------------------------------%

         if (rnorm.value <= 0.717*wnorm) { 
//
//          %---------------------------------------------------%
//          | Enter the Iterative refinement phase. If further  |
//          | refinement is necessary, loop back here. The loop |
//          | variable is ITER. Perform a step of Classical     |
//          | Gram-Schmidt using all the Arnoldi vectors V_{j}  |
//          %---------------------------------------------------%

            boolean refining = true;
            while (refining) {

//             %----------------------------------------------------%
//             | Compute V_{j}^T * B * r_{j}.                       |
//             | WORKD(IRJ:IRJ+J-1) = v(:,1:J)'*WORKD(IPJ:IPJ+N-1). |
//             %----------------------------------------------------%

               dgemv ('T', n, j, one, v, vw, workp, zero, workr);

//             %----------------------------------------------%
//             | Compute the correction to the residual:      |
//             | r_{j} = r_{j} - V_{j} * WORKD(IRJ:IRJ+J-1).  |
//             | The correction to H is v(:,1:J)*H(1:J,1:J) + |
//             | v(:,1:J)*WORKD(IRJ:IRJ+J-1)*e'_j, but only   |
//             | H(j,j) is updated.                           |
//             %----------------------------------------------%

               dgemv ('N', n, j, -one, v, vw, workr, one, resid);

               if (j == 1) {
                  e[j-1] = zero; /*ZB*/
               }

               d[j-1] = d[j-1] + workr[j - 1];/*ZB*/
//
               dcopy (n, resid, workp);

//             %---------------------------------------------------%
//             | Back from reverse communication if ORTH2 = true |
//             %---------------------------------------------------%

//             %-----------------------------------------------------%
//             | Compute the B-norm of the corrected residual r_{j}. |
//             %-----------------------------------------------------%
               double rnorm1 = dnrm2(n, resid);
//             %-----------------------------------------%
//             | Determine if we need to perform another |
//             | step of re-orthogonalization.           |
//             %-----------------------------------------%
               if (rnorm1 > 0.717*rnorm.value) {
//                %--------------------------------%
//                | No need for further refinement |
//                %--------------------------------%
                  rnorm.value = rnorm1;
                  refining = false;
               }
               else {
//                %-------------------------------------------%
//                | Another step of iterative refinement step |
//                | is required. NITREF is used by stat.h     |
//                %-------------------------------------------%
                  rnorm.value  = rnorm1;
                  iter   = iter + 1;
                  if (iter > 1) {
                     refining = false;
//                   %-------------------------------------------------%
//                   | Otherwise RESID is numerically in the span of V |
//                   %-------------------------------------------------%
                     for (int jj = 0; jj<n; jj++) {
                        resid[jj] = zero;
                     }
                     rnorm.value = zero;
                  }
               }
            }
         }
//
//       %----------------------------------------------%
//       | Branch here directly if iterative refinement |
//       | wasn't necessary or after at most NITER_REF  |
//       | steps of iterative refinement.               |
//       %----------------------------------------------%

//
//       %----------------------------------------------------------%
//       | Make sure the last off-diagonal element is non negative  |
//       | If not perform a similarity transformation on H(1:j,1:j) |
//       | and scale v(:,j) by -1.                                  |
//       %----------------------------------------------------------%

         if (e[j-1] < zero) {/*ZB*/
            e[j-1] = -e[j-1];/*ZB*/
            if ( j < k+np) { 
               dscal(n, -one, v, vw, j);/*ZB*/
            }
            else {
               dscal(n, -one, resid);
            }
         }
//
//       %------------------------------------%
//       | STEP 6: Update  j = j+1;  Continue |
//       %------------------------------------%

         j = j + 1;
         if (j > k+np) {
            jsave = j;
            return 0;
         }

//       %--------------------------------------------------------%
//       | Loop back to extend the factorization by another step. |
//       %--------------------------------------------------------%

      }
//
//    %---------------------------------------------------------------%
//    |                                                               |
//    |  E N D     O F     M A I N     I T E R A T I O N     L O O P  |
//    |                                                               |
//    %---------------------------------------------------------------%

//    %---------------%
//    | End of xsaitr |
//    %---------------%
   }

   double getMaxAbs (int n, double[] v) {
      double max = -1;
      for (int i=0; i<n; i++) {
         double abs = Math.abs(v[i]);
         if (abs > max) {
            max = abs;
         }
      }
      return max;
   }

   /**
    * @param n
    * Dimension of the problem
    * @param which
    * which eigenvalues to compute
    * @param nev
    * number of eigenvalues to compute
    * @param np
    * number of implicit shifts to apply during each Lanczos iteration
    * On output: number of converged ritz values
    * @param tol
    * stopping criteria
    * @param resid
    * vector of length n containing residual
    * @param v
    * n X (nev+np) matrix used to compute basis vectors
    * @param vw
    * Witdh of v such that v(i,j) = v[i*vw+j]
    * @param d
    * k+np diagonal entries of tridiagonal matrix
    * @param e
    * k+np-1 off-diagonal entries of tridiagonal matrix, starting at 1
    * @param ritz
    * vector of length nev+np; contains nev ritz vectors on output
    * @param bounds
    * vector of length nev+npl contains nev error bounds on output
    * @param q
    * nev+np X nev+np work matrix
    * @param qw
    * width of q such that q(i,j) = q[i*qw+j]
    * @param info
    * on input: if 0, use a random initial vector, otherwise, use resid
    * @param A
    * computes y = A*x, where A is the problem matrix 
    */
   int xsaup2 (
      int n, Ordering which, int nev, int np, double tol, double[] resid,
      double[] v, int vw, int info, LinearTransformNd A) {

      //
      // %-------------------------------%
      // | Initialize timing statistics  |
      // | & message level for debugging |
      // %-------------------------------%

      // %-------------------------------------%
      // | nev0 and np0 are integer variables  |
      // | hold the initial values of NEV & NP |
      // %-------------------------------------%

      int nev0   = nev;
      int np0    = np;

      // %-------------------------------------%
      // | kplusp is the bound on the largest  |
      // |        Lanczos factorization built. |
      // | nconv is the current number of      |
      // |        "converged" eigenvlues.      |
      // | iter is the counter on the current  |
      // |      iteration step.                |
      // %-------------------------------------%

      int kplusp = nev0 + np0;
      int nconv  = 0;
      int iter   = 0;
      //
      // %--------------------------------------------%
      // | Set flags for computing the first NEV steps |
      // | of the Lanczos factorization.              |
      // %--------------------------------------------%

      boolean getv0    = true;

      boolean initv;

      if (info != 0) {
         // %--------------------------------------------%
         // | User provides the initial residual vector. |
         // %--------------------------------------------%
         initv = true;
         info  = 0;
      }
      else {
         initv = false;
      }
      int ido = 0;
//
//    %---------------------------------------------%
//    | Get a possibly random starting vector and   |
//    | force it into the range of the operator OP. |
//    %---------------------------------------------%

      DoubleHolder rnorm = new DoubleHolder();
      if (getv0) {
         xgetv0 (ido, 1, initv, n, 1, v, vw, resid, rnorm);
         if (rnorm.value == zero) {
//          %-----------------------------------------%
//          | The initial vector is zero. Error exit. | 
//          %-----------------------------------------%
            return -9;
         }
         getv0 = false;
      }

//    %-------------------------------------%
//    | Back from computing residual norm   |
//    | at the end of the current iteration |
//    %-------------------------------------%

//    %----------------------------------------------------------%
//    | Compute the first NEV steps of the Lanczos factorization |
//    %----------------------------------------------------------%

      xsaitr (ido, n, 0, nev0, resid, rnorm, v, vw, d, e, A);

      if (info > 0) {

//       %-----------------------------------------------------%
//       | xsaitr was unable to build an Lanczos factorization |
//       | of length NEV0. INFO is returned with the size of   |
//       | the factorization built. Exit main loop.            |
//       %-----------------------------------------------------%
         np   = info;
         myIterCnt = iter;
         return -9999;
      }
//
//    %--------------------------------------------------------------%
//    |                                                              |
//    |           M A I N  LANCZOS  I T E R A T I O N  L O O P       |
//    |           Each iteration implicitly restarts the Lanczos     |
//    |           factorization in place.                            |
//    |                                                              |
//    %--------------------------------------------------------------%
//
      while (true) {

         iter = iter + 1;

//       %------------------------------------------------------------%
//       | Compute NP additional steps of the Lanczos factorization. |
//       %------------------------------------------------------------%

         ido = 0;

         info = xsaitr (ido, n, nev, np, resid, rnorm, v, vw, d, e, A);

         if (info > 0) {

//          %-----------------------------------------------------%
//          | xsaitr was unable to build an Lanczos factorization |
//          | of length NEV0+NP0. INFO is returned with the size  |  
//          | of the factorization built. Exit main loop.         |
//          %-----------------------------------------------------%

            np = info;
            myIterCnt = iter;
            return -9999;
         }

//       %--------------------------------------------------------%
//       | Compute the eigenvalues and corresponding error bounds |
//       | of the current symmetric tridiagonal matrix.           |
//       %--------------------------------------------------------%


         int ierr = dseigt (rnorm.value, kplusp, d, e, ritz, bounds);
         System.out.println (
            "iter=" + iter + " " + (new VectorNd(ritz)).toString ("%19.16f"));

         if (ierr != 0) {
            return -8;
         }

//       %----------------------------------------------------%
//       | Make a copy of eigenvalues and corresponding error |
//       | bounds obtained from _seigt.                       |
//       %----------------------------------------------------%

         dcopy(kplusp, ritz, workl1);
         dcopy(kplusp, bounds, workl2);

//       %---------------------------------------------------%
//       | Select the wanted Ritz values and their bounds    |
//       | to be used in the convergence test.               |
//       | The selection is based on the requested number of |
//       | eigenvalues instead of the current NEV and NP to  |
//       | prevent possible misconvergence.                  |
//       | * Wanted Ritz values := RITZ(NP+1:NEV+NP)         |
//       | * Shifts := RITZ(1:NP) := WORKL(1:NP)             |
//       %---------------------------------------------------%

         nev = nev0;
         np = np0;
         //System.out.println ("RITZ=  " + (new VectorNd(ritz)));
         //System.out.println ("BOUNDS=" + (new VectorNd(bounds)));
         dsgets (which, nev, np, ritz, bounds, workl0);
//
//       %-------------------%
//       | Convergence test. |
//       %-------------------%

         for (int i=np; i<np+nev; i++) {
            workl0[i] = bounds[i];
         }

         if (useGlobalConvergence) {
            globalTol = Math.max (eps23, tol*getMaxAbs (kplusp, ritz));
         }
         else {
            globalTol = 0;
         }
         nconv = dsconv (nev, ritz, workl0, np, tol);
         System.out.println ("nconv=" + nconv + " tol=" + tol + " np=" + np);

//       %---------------------------------------------------------%
//       | Count the number of unwanted Ritz values that have zero |
//       | Ritz estimates. If any Ritz estimates are equal to zero |
//       | then a leading block of H of order equal to at least    |
//       | the number of Ritz values with zero Ritz estimates has  |
//       | split off. None of these Ritz values may be removed by  |
//       | shifting. Decrease NP the number of shifts to apply. If |
//       | no shifts may be applied, then prepare to exit          |
//       %---------------------------------------------------------%

         int nptemp = np;
         for (int j=0; j<nptemp; j++) {
            if (bounds[j] == zero) {
               np = np - 1;
               nev = nev + 1;
            }
         }
//
         if ( (nconv >= nev0) || 
              (iter > mxiter) ||
              (np == 0) ) {
//    
//          %------------------------------------------------%
//          | Prepare to exit. Put the converged Ritz values |
//          | and corresponding bounds in RITZ(1:NCONV) and  |
//          | BOUNDS(1:NCONV) respectively. Then sort. Be    |
//          | careful when NCONV > NP since we don't want to |
//          | swap overlapping locations.                    |
//          %------------------------------------------------%

            if (which == Ordering.BE) {

//             %-----------------------------------------------------%
//             | Both ends of the spectrum are requested.            |
//             | Sort the eigenvalues into algebraically decreasing  |
//             | order first then swap low end of the spectrum next  |
//             | to high end in appropriate locations.               |
//             | NOTE: when np < floor(nev/2) be careful not to swap |
//             | overlapping locations.                              |
//             %-----------------------------------------------------%

               xsortr (Ordering.SA, kplusp, ritz, bounds);
               int nevd2 = nev0 / 2;
               int nevm2 = nev0 - nevd2 ;
               if ( nev > 1 ) {
                  swap (ritz, nevm2, Math.max(kplusp-nevd2,kplusp-np),
                        Math.min(nevd2,np));
                  swap (bounds, nevm2, Math.max(kplusp-nevd2,kplusp-np),
                        Math.min(nevd2,np));
               }
            }
            else {
//             %--------------------------------------------------%
//             | LM, SM, LA, SA case.                             |
//             | Sort the eigenvalues of H into the an order that |
//             | is opposite to WHICH, and apply the resulting    |
//             | order to BOUNDS.  The eigenvalues are sorted so  |
//             | that the wanted part are always within the first |
//             | NEV locations.                                   |
//             %--------------------------------------------------%
               xsortr (which.opposite(), kplusp, ritz, bounds);

            }

//          %--------------------------------------------------%
//          | Scale the Ritz estimate of each Ritz value       |
//          | by 1 / max(eps23,magnitude of the Ritz value).   |
//          %--------------------------------------------------%

            for (int j = 0; j<nev0; j++) {
               double temp = Math.max( eps23, Math.abs(ritz[j]) );
               bounds[j] = bounds[j]/temp;
            }

//          %----------------------------------------------------%
//          | Sort the Ritz values according to the scaled Ritz  |
//          | esitmates.  This will push all the converged ones  |
//          | towards the front of ritzr, ritzi, bounds          |
//          | (in the case when NCONV < NEV.)                    |
//          %----------------------------------------------------%

            xsortr (Ordering.LA, nev0, bounds, ritz);

//          %----------------------------------------------%
//          | Scale the Ritz estimate back to its original |
//          | value.                                       |
//          %----------------------------------------------%

            for (int j=0; j<nev0; j++) {
               double temp = Math.max( eps23, Math.abs(ritz[j]) );
               bounds[j] = bounds[j]*temp;
            }

//          %--------------------------------------------------%
//          | Sort the "converged" Ritz values again so that   |
//          | the "threshold" values and their associated Ritz |
//          | estimates appear at the appropriate position in  |
//          | ritz and bound.                                  |
//          %--------------------------------------------------%

            if (which == Ordering.BE) {

//             %------------------------------------------------%
//             | Sort the "converged" Ritz values in increasing |
//             | order.  The "threshold" values are in the      |
//             | middle.                                        |
//             %------------------------------------------------%

               xsortr (Ordering.LA, nconv, ritz, bounds);
            }
            else {
//             %----------------------------------------------%
//             | In LM, SM, LA, SA case, sort the "converged" |
//             | Ritz values according to WHICH so that the   |
//             | "threshold" value appears at the front of    |
//             | ritz.                                        |
//             %----------------------------------------------%
               xsortr(which, nconv, ritz, bounds);
            }

//          %------------------------------------------%
//          |  Use h( 1,1 ) as storage to communicate  |
//          |  rnorm to _seupd if needed               |
//          %------------------------------------------%

            rnormSave = rnorm.value;

//          %------------------------------------%
//          | Max iterations have been exceeded. | 
//          %------------------------------------%

            if (iter > mxiter && nconv < nev) info = 1;

//          %---------------------%
//          | No shifts to apply. | 
//          %---------------------%

            if (np == 0 && nconv < nev0) info = 2;

            myNumConverged = nconv;
            myIterCnt = iter;
            nev = nconv;
            return 0;
         }
         else if (nconv < nev && ishift == 1) {

//          %---------------------------------------------------%
//          | Do not have all the requested eigenvalues yet.    |
//          | To prevent possible stagnation, adjust the number |
//          | of Ritz values and the shifts.                    |
//          %---------------------------------------------------%

            int nevbef = nev;
            nev = nev + Math.min (nconv, np/2);
            if (nev == 1 && kplusp >= 6) {
               nev = kplusp / 2;
            }
            else if (nev == 1 && kplusp > 2) {
               nev = 2;
            }
            np  = kplusp - nev;
//    
//          %---------------------------------------%
//          | If the size of NEV was just increased |
//          | resort the eigenvalues.               |
//          %---------------------------------------%
//    
            if (nevbef < nev) {
               dsgets (which, nev, np, ritz, bounds,
                       workl0);
            }
         }

         if (ishift == 0) {
            //
            // get user-specified shifts into forst NP location of workl
            // then copy these into ritz
            //
            dcopy (np, workl0, ritz);
         }

//       %---------------------------------------------------------%
//       | Apply the NP0 implicit shifts by QR bulge chasing.      |
//       | Each shift is applied to the entire tridiagonal matrix. |
//       | The first 2*N locations of WORKD are used as workspace. |
//       | After xsapps is done, we have a Lanczos                 |
//       | factorization of length NEV.                            |
//       %---------------------------------------------------------%

         xsapps (n, nev, np, ritz, v, vw, d, e, resid, q, qw);
//       %---------------------------------------------%
//       | Compute the B-norm of the updated residual. |
//       | Keep B*RESID in WORKD(1:N) to be used in    |
//       | the first step of the next call to xsaitr.  |
//       %---------------------------------------------%

         dcopy (n, resid, work0);
         rnorm.value = dnrm2(n, resid);
      }

//    %---------------------------------------------------------------%
//    |                                                               |
//    |  E N D     O F     M A I N     I T E R A T I O N     L O O P  |
//    |                                                               |
//    %---------------------------------------------------------------%
   }

   double rnormSave; // passes rnorm to xseupd

   int mxiter;
   // Implicit shifts. If 0, shifts are provided by user (not supported
   // yet). If 1, then exact shifts with respect to the tridiagonal matrix are
   // used.
   int ishift = 1;

   int iterLimit = -1;
   int myIterCnt;
   int myNumConverged;

   VectorNd workVec0;
   VectorNd workVec1;
   VectorNd workVec2;

   double[] work0;
   double[] work1;
   double[] work2;

   double[] workl0;
   double[] workl1;
   double[] workl2;

   double[] d;
   double[] e;
   double[] ritz;
   double[] bounds;
   double[] q;
   int qw;

   public void setIterationLimit (int max) {
      iterLimit = max;
   }

   public int getIterationLimit () {
      return iterLimit;
   }

   public int getIterationCount() {
      return myIterCnt;
   }

   public int getNumConverged() {
      return myNumConverged;
   }

   /**
    * @param n
    * Dimension of the problem
    * @param which
    * which eigenvalues to compute
    * @param nev
    * number of eigenvalues to compute
    * @param tol
    * stopping criteria
    * @param resid
    * vector of length n containing residual
    * @param ncv
    * number of column of the matrix v
    * @param v
    * n X (nev+np) matrix used to compute basis vectors
    * @param vw
    * Witdh of v such that v(i,j) = v[i*vw+j]
    * @param d
    * k+np diagonal entries of tridiagonal matrix
    * @param e
    * k+np-1 off-diagonal entries of tridiagonal matrix, starting at 1
    * @param ritz
    * vector of length nev+np; contains nev ritz vectors on output
    * @param bounds
    * vector of length nev+npl contains nev error bounds on output
    * @param q
    * nev+np X nev+np work matrix
    * @param qw
    * width of q such that q(i,j) = q[i*qw+j]
    * @param info
    * on input: if 0, use a random initial vector, otherwise, use resid
    * @param A
    * computes y = A*x, where A is the problem matrix 
    */
   int xsaupd (
      int n, Ordering which, int nev, double tol,
      double[] resid, int ncv, double[] v, int vw, 
      int info, LinearTransformNd A) {

      int ierr = 0;
      ishift = 1;
      if (iterLimit < 0) {
         mxiter = 30*n;
      }
      else {
         mxiter = iterLimit;
      }

      int nb = 1;
      if (n <= 0) {
         ierr = -1;
      }
      else if (nev <= 0) {
         ierr = -2;
      }
      else if (ncv <= nev ||  ncv > n) {
         ierr = -3;
      }

      // %----------------------------------------------%
      // | NP is the number of additional steps to      |
      // | extend the length NEV Lanczos factorization. |
      // %----------------------------------------------%
      int np     = ncv - nev;

      if (nev == 1 && which == Ordering.BE) {
         ierr = -13;
      }
      // %------------%
      // | Error Exit |
      // %------------%
      if (ierr != 0) {
         return ierr;
      }
      // %------------------------%
      // | Set default parameters |
      // %------------------------%
      if (nb <= 0) {
         nb = 1;
      }
      if (tol <= zero) {
         tol = epsmch;
      }
      // %----------------------------------------------%
      // | NP is the number of additional steps to      |
      // | extend the length NEV Lanczos factorization. |
      // | NEV0 is the local variable designating the   |
      // | size of the invariant subspace desired.      |
      // %----------------------------------------------%
      np     = ncv - nev;
      int nev0   = nev;
      // %-----------------------------%
      // | Zero out internal workspace |
      // %-----------------------------%
      d = new double[ncv];
      e = new double[ncv];
      ritz = new double[ncv];
      bounds = new double[ncv];
      q = new double[ncv*ncv];
      qw = ncv;

      workVec0 = new VectorNd(n);
      workVec1 = new VectorNd(n);
      workVec2 = new VectorNd(n);

      work0 = workVec0.getBuffer();
      work1 = workVec1.getBuffer();
      work2 = workVec2.getBuffer();

      workl0 = new double[ncv];
      workl1 = new double[ncv];
      workl2 = new double[ncv];

      // %-------------------------------------------------------%
      // | Pointer into WORKL for address of H, RITZ, BOUNDS, Q  |
      // | etc... and the remaining workspace.                   |
      // | Also update pointer to be used on output.             |
      // | Memory is laid out as follows:                        |
      // | workl(1:2*ncv) := generated tridiagonal matrix        |
      // | workl(2*ncv+1:2*ncv+ncv) := ritz values               |
      // | workl(3*ncv+1:3*ncv+ncv) := computed error bounds     |
      // | workl(4*ncv+1:4*ncv+ncv*ncv) := rotation matrix Q     |
      // | workl(4*ncv+ncv*ncv+1:7*ncv+ncv*ncv) := workspace     |
      // %-------------------------------------------------------%
      info = xsaup2 (n, which, nev0, np, tol, resid, v, vw, info, A);
      if (info < 0) {
         return info;
      }
//
//    %--------------------------------------------------%
//    | ido != 99 implies use of reverse communication |
//    | to compute operations involving OP or shifts.    |
//    %--------------------------------------------------%
//
      myIterCnt = mxiter;

//    %------------------------------------%
//    | Exit if there was an informational |
//    | error within xsaup2 .              |
//    %------------------------------------%

      if (info < 0) {
         return info;
      }
      if (info == 2) {
         return 3;
      }
      else {
         return 0;
      }
   }


   /**
    * Sorts the vector x in the order specified by which and optionally
    * applies the permutation to the columns of a if a != null.
    *
    * @param order
    * sort order
    * @param n
    * size of x and number of columns of a
    * @param x
    * vector to sort
    * @param na
    * number of rows in a
    * @param a
    * if not null, matrix whose columns should be arranged according to x
    * @param aw
    * width of a such that a(i,j) = a[i*aw+j]
    */
   static void dsesrt (
      Ordering order, int n, double[] x, int na, double[] a, int aw) {
      
      int igap = n / 2;

      while (true) {
         if (igap == 0) {
            return;
         }
         for (int i = igap; i <= n-1; i++) {
            int j = i-igap;
            while (true) {
               if (j < 0) {
                  break;
               }
               if (swapNeeded (order, x[j], x[j+igap])) {
                  double temp = x[j];
                  x[j] = x[j+igap];
                  x[j+igap] = temp;                 
                  if (a != null) {
                     swapColumns (na, a, aw, j, j+igap);
                  }
               }
               else {
                  break;
               }
               j = j-igap;
            }
         }
         igap = igap / 2;
      }
   }

   static void swapColumns (int n, double[] a, int aw, int j0, int j1) {
      for (int i=0; i<n; i++) {
         int off = i*aw;
         double tmp = a[off+j0];
         a[off+j0] = a[off+j1];
         a[off+j1] = tmp;
      }
   }

   public int eigs (
      VectorNd e, MatrixNd Z, int nev, Ordering which, LinearTransformNd A) {

      if (nev < 0) {
         throw new IllegalArgumentException (
            "nev must not be negative");
      }
      if (A.rowSize() != A.colSize()) {
         throw new IllegalArgumentException (
            "A must be square");
      }
      if (nev == 0) {
         return 0;
      }
      int n = A.rowSize();
      if (e == null) {
         e = new VectorNd (nev);
      }
      else if (e.size() < nev) {
         e.setSize (nev);
      }
      int ncv = Math.min(2*nev+1,n);
      if (Z != null) {
         Z.setSize (n, nev);
      }
      MatrixNd V = new MatrixNd (n, ncv);

      double[] resid = new double[n];
      double tol = 100*epsmch;
      tol = 1e-14;

      myNumConverged = 0;
      myIterCnt = 0;
      rnorm0 = 0;
      iter = 0;
      rnormSave = 0;
      //RandomGenerator.setSeed (0x1234);

      int ierr = xsaupd (n, which, nev, tol, 
                         resid, ncv, V.buf, V.width, /*info=*/0, A);
      if (ierr < 0) {
         return ierr;
      }
      
      int nconv = xseupd (
         e.getBuffer(), Z, n, which, nev, tol, resid,
         ncv, V.buf, V.width, A);
      return nconv;
   }

   /**
    * @param rvec
    * compute ritz vectors
    * @param d
    * vector of length nev containing the eigenvalue approximations
    * @param Z
    * n X nev matrix containing the eigenvectors
    */

   int xseupd (
      double[] eigs, MatrixNd Z, int n, Ordering which, int nev,
      double tol, double[] resid, int ncv, double[] v, int vw,
      LinearTransformNd A) {

     //  integer    bounds , ierr   , ih    , ihb   , ihd   ,
     // &           iq     , iw     , j     , k     , ldh   ,
     // &           ldq    , nconv , next  ,
     // &           ritz   , irz    , ibd   , np    , ishift,
     // &           leftptr, rghtptr, numcnv, jj
     //  Double precision 
     // &           bnorm2 , rnorm, temp, temp1, eps23
     //  logical    reord

//    %-----------------------%
//    | Executable Statements |
//    %-----------------------%
//
//    %------------------------%
//    | Set default parameters |
//    %------------------------%
      int nconv = getNumConverged();
//    %--------------%
//    | Quick return |
//    %--------------%

      if (nconv == 0) {
         return 0;
      }
      
      int ierr = 0;

      if (nconv <= 0) {
         ierr = -14;
      }
      if (n <= 0) {
         ierr = -1;
      }
      if (nev <= 0) {
         ierr = -2;
      }
      if (ncv <= nev ||  ncv > n) {
         ierr = -3;
      }
      if (nev == 1 && which == Ordering.BE) {
         ierr = -12;
      }

//    %------------%
//    | Error Exit |
//    %------------%

      if (ierr != 0) {
         return ierr;
      }
      
//    
//    %-------------------------------------------------------%
//    | Pointer into WORKL for address of H, RITZ, BOUNDS, Q  |
//    | etc... and the remaining workspace.                   |
//    | Also update pointer to be used on output.             |
//    | Memory is laid out as follows:                        |
//    | workl(1:2*ncv) := generated tridiagonal matrix H      |
//    |       The subdiagonal is stored in workl(2:ncv).      |
//    |       The dead spot is workl(1) but upon exiting      |
//    |       dsaupd  stores the B-norm of the last residual  |
//    |       vector in workl(1). We use this !!!             |
//    | workl(2*ncv+1:2*ncv+ncv) := ritz values               |
//    |       The wanted values are in the first NCONV spots. |
//    | workl(3*ncv+1:3*ncv+ncv) := computed Ritz estimates   |
//    |       The wanted values are in the first NCONV spots. |
//    | NOTE: workl(1:4*ncv) is set by dsaupd  and is not     |
//    |       modified by xseupd .                            |
//    %-------------------------------------------------------%

//    %-------------------------------------------------------%
//    | The following is used and set by xseupd .             |
//    | workl(4*ncv+1:4*ncv+ncv) := used as workspace during  |
//    |       computation of the eigenvectors of H. Stores    |
//    |       the diagonal of H. Upon EXIT contains the NCV   |
//    |       Ritz values of the original system. The first   |
//    |       NCONV spots have the wanted values. If MODE =   |
//    |       1 or 2 then will equal workl(2*ncv+1:3*ncv).    |
//    | workl(5*ncv+1:5*ncv+ncv) := used as workspace during  |
//    |       computation of the eigenvectors of H. Stores    |
//    |       the subdiagonal of H. Upon EXIT contains the    |
//    |       NCV corresponding Ritz estimates of the         |
//    |       original system. The first NCONV spots have the |
//    |       wanted values. If MODE = 1,2 then will equal    |
//    |       workl(3*ncv+1:4*ncv).                           |
//    | workl(6*ncv+1:6*ncv+ncv*ncv) := orthogonal Q that is  |
//    |       the eigenvector matrix for H as returned by     |
//    |       dsteqr . Not referenced if RVEC = .False.       |
//    |       Ordering follows that of workl(4*ncv+1:5*ncv)   |
//    | workl(6*ncv+ncv*ncv+1:6*ncv+ncv*ncv+2*ncv) :=         |
//    |       Workspace. Needed by dsteqr  and by xseupd .    |
//    | GRAND total of NCV*(NCV+8) locations.                 |
//    %-------------------------------------------------------%


      // Ritz estimates are in workl2
      // Eigenvectors are in first ncv columsn of Q
         
//    %----------------------------------------%
//    | irz points to the Ritz values computed |
//    |     by _seigt before exiting _saup2.   |
//    | ibd points to the Ritz estimates       |
//    |     computed by _seigt before exiting  |
//    |     _saup2.                            |
//    %----------------------------------------%

//    %---------------------------------------%
//    | RNORM is B-norm of the RESID(1:N).    |
//    | BNORM2 is the 2 norm of B*RESID(1:N). |
//    | Upon exit of dsaupd  WORKD(1:N) has    |
//    | B*RESID(1:N).                         |
//    %---------------------------------------%

      double rnorm = rnormSave;

      boolean[] select = new boolean[ncv];

      if (Z != null) {

         boolean reord = false;

//       %---------------------------------------------------%
//       | Use the temporary bounds array to store indices   |
//       | These will be used to mark the select array later |
//       %---------------------------------------------------%

         for (int j =0; j<ncv; j++) {
            bounds[j] = j;
            select[j] = false;
         }

//       %-------------------------------------%
//       | Select the wanted Ritz values.      |
//       | Sort the Ritz values so that the    |
//       | wanted ones appear at the tailing   |
//       | NEV positions of workl(irr) and     |
//       | workl(iri).  Move the corresponding |
//       | error estimates in workl(bound)     |
//       | accordingly.                        |
//       %-------------------------------------%

         int np     = ncv - nev;
         ishift = 0; // XXXX
         dsgets (which, nev, np, workl1, bounds, workl0);
         ishift = 1;

//       %-----------------------------------------------------%
//       | Record indices of the converged wanted Ritz values  |
//       | Mark the select array for possible reordering       |
//       %-----------------------------------------------------%

         int numcnv = 0;
         if (globalTol != 0) {
            for (int j=0; j<ncv; j++) {
               int jj = (int)bounds[ncv-j-1];
               if (numcnv < nconv && workl2[jj] <= globalTol) {
                  select[jj] = true;
                  numcnv = numcnv + 1;
                  if (jj > nev-1) {
                     reord = true;
                  }
               }
            }
         }
         else {
            for (int j=0; j<ncv; j++) {
               double temp1 = Math.max(eps23, Math.abs(workl1[ncv-j-1]) );
               int jj = (int)bounds[ncv-j-1];
               if (numcnv < nconv && workl2[jj] <= tol*temp1) {
                  select[jj] = true;
                  numcnv = numcnv + 1;
                  if (jj > nev-1) {
                     reord = true;
                  }
               }
            }
         }

//       %-----------------------------------------------------------%
//       | Check the count (numcnv) of converged Ritz values with    |
//       | the number (nconv) reported by _saupd.  If these two      |
//       | are different then there has probably been an error       |
//       | caused by incorrect passing of the _saupd data.           |
//       %-----------------------------------------------------------%

         if (numcnv != nconv) {
            System.out.println ("numcnv=" + numcnv + " nconv=" + nconv);
            return -17;
         }

//       %-----------------------------------------------------------%
//       | Call LAPACK routine _steqr to compute the eigenvalues and |
//       | eigenvectors of the final symmetric tridiagonal matrix H. |
//       | Initialize the eigenvector matrix Q to the identity.      |
//       %-----------------------------------------------------------%

         double[] hd = workl1; // H matrix main diagonal
         double[] hb = workl2; // H matrix subdiagonal, starting at hb[0]

         for (int i=0; i<ncv; i++) {
            hb[i] = e[i];
            hd[i] = d[i];
         }

         //call dcopy (ncv-1, workl(ih+1), workl(ihb));
         //call dcopy (ncv, workl(ih+ldh), workl(ihd));

         //call dsteqr ('Identity', ncv, hd, hb,
         //             q , qw, workl0, ierr);
         MatrixNd Q = new MatrixNd();
         Q.setBuffer (ncv, ncv, q, qw);
         Q.setIdentity();
         ierr = EigenDecomposition.tql2 (hd, hb, ncv, Q, 30*ncv);
         if (ierr != 0) {
            return -8;
         }

         if (reord) {

//          %---------------------------------------------%
//          | Reordered the eigenvalues and eigenvectors  |
//          | computed by _steqr so that the "converged"  |
//          | eigenvalues appear in the first NCONV       |
//          | positions of workl(ihd), and the associated |
//          | eigenvectors appear in the first NCONV      |
//          | columns.                                    |
//          %---------------------------------------------%

            int leftptr = 0;
            int rghtptr = ncv-1;

            if (ncv != 1) { 

               do {
                  if (select[leftptr]) {

//                   %-------------------------------------------%
//                   | Search, from the left, for the first Ritz |
//                   | value that has not converged.             |
//                   %-------------------------------------------%

                     leftptr = leftptr + 1;
                  }
                  else if ( !select[rghtptr]) {

//                   %----------------------------------------------%
//                   | Search, from the right, the first Ritz value |
//                   | that has converged.                          |
//                   %----------------------------------------------%

                     rghtptr = rghtptr - 1;
                  }
                  else {

//                   %----------------------------------------------%
//                   | Swap the Ritz value on the left that has not |
//                   | converged with the Ritz value on the right   |
//                   | that has converged.  Swap the associated     |
//                   | eigenvector of the tridiagonal matrix H as   |
//                   | well.                                        |
//                   %----------------------------------------------%
                     double temp = hd[leftptr];
                     hd[leftptr] = hd[rghtptr];
                     hd[rghtptr] = temp;
                     getColumn (ncv, q, qw, leftptr, workl0);
                     getColumn (ncv, q, qw, rghtptr, workl2);
                     //dcopy (ncv, workl(iq+ncv*(leftptr-1)), workl0);
                     //dcopy (ncv, workl(iq+ncv*(rghtptr-1)),
                     //            workl(iq+ncv*(leftptr-1)));
                     //dcopy (ncv, workl0, workl(iq+ncv*(rghtptr-1)));
                     setColumn (ncv, q, qw, leftptr, workl2);
                     setColumn (ncv, q, qw, rghtptr, workl0);
                     leftptr = leftptr + 1;
                     rghtptr = rghtptr - 1;
                  }
               }
               while (leftptr < rghtptr);
            }
         }
//       %----------------------------------------%
//       | Load the converged Ritz values into D. |
//       %----------------------------------------%

         dcopy (nconv, hd, eigs);
      }
      else {
//       %-----------------------------------------------------%
//       | Ritz vectors not required. Load Ritz values into D. |
//       %-----------------------------------------------------%
         dcopy (nconv, ritz, eigs);
         dcopy (ncv, ritz, workl1);
      }
//    %------------------------------------------------------------------%
//    | Transform the Ritz values and possibly vectors and corresponding |
//    | Ritz estimates of OP to those of A*x=lambda*B*x. The Ritz values |
//    | (and corresponding data) are returned in ascending order.        |
//    %------------------------------------------------------------------%

//    %---------------------------------------------------------%
//    | Ascending sort of wanted Ritz values, vectors and error |
//    | bounds. Not necessary if only Ritz values are desired.  |
//    %---------------------------------------------------------%
      if (Z != null) {
         dsesrt (Ordering.LA, nconv, eigs, ncv, q, qw);
      }
      else {
         dcopy (ncv, bounds, workl2);
      }
//
//    %------------------------------------------------%
//    | Compute the Ritz vectors. Transform the wanted |
//    | eigenvectors of the symmetric tridiagonal H by |
//    | the Lanczos basis matrix V.                    |
//    %------------------------------------------------%
      if (Z != null) {
         Z.setSize (n, nconv);
//   
//       %----------------------------------------------------------%
//       | Compute the QR factorization of the matrix representing  |
//       | the wanted invariant subspace located in the first NCONV |
//       | columns of workl(iq,ldq).                                |
//       %----------------------------------------------------------%
//    
         MatrixNd Q = new MatrixNd ();
         Q.setBuffer (ncv, nconv, q, qw);
         QRDecomposition qr = new QRDecomposition (Q);

         // call dgeqr2 (ncv, nconv        , q ,
         //          qw, workl(iw+ncv), workl2,
         //           ierr);
//       %--------------------------------------------------------%
//       | * Postmultiply V by Q.                                 |   
//       | * Copy the first NCONV columns of VQ into Z.           |
//       | The N by NCONV matrix Z is now a matrix representation |
//       | of the approximate invariant subspace associated with  |
//       | the Ritz values in workl(ihd).                         |
//       %--------------------------------------------------------%
//    
         MatrixNd V = new MatrixNd();
         V.setBuffer (n, ncv, v, vw);
         qr.postMulQ (V, V);

         //call dorm2r ('Right', 'Notranspose', n        ,
         //             ncv    , nconv        , q,
         //             qw    , workl(iw+ncv), v        ,
         //             ldv    , workd(n+1)   , ierr);
         int zw = Z.width;
         for (int i=0; i<n; i++) {
            for (int j=0; j<nconv; j++) {
               Z.buf[i*zw+j] = v[i*vw+j];
            }
         }
         //call dlacpy ('All', n, nconv, v, ldv, z, ldz);
//       %-----------------------------------------------------%
//       | In order to compute the Ritz estimates for the Ritz |
//       | values in both systems, need the last row of the    |
//       | eigenvector matrix. Remember, it`s in factored form |
//       %-----------------------------------------------------%
         MatrixNd e = new MatrixNd (ncv, 1);
         e.set (ncv-1, 0, 1);
         // for (int j=0; j<ncv-1; j++) {
         //    workl2[j] = zero;
         // }
         // workl2[ncv-1] = one;
         // call dorm2r ('Left', 'Transpose'  , ncv       ,
         //              1     , nconv        , q ,
         //              qw   , workl(iw+ncv), workl2,
         //              ncv   , temp         , ierr);
         qr.preMulQTranspose (e, e);
         for (int j=0; j<ncv; j++) {
            workl2[j] = rnorm * Math.abs(e.get(j,0));
         }

      }
      return nconv;
   }

}

