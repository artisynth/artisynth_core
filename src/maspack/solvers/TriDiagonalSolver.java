package maspack.solvers;

import maspack.matrix.*;
import maspack.util.*;

/**
 * Solvers for tridiagonal matrices, using the Thomas algorithms as described
 * in the wikipedia entry for "Tridiagonal matrix algorithm".
 */
public class TriDiagonalSolver {

   /**
    * Solves a tridiagonal matrix whose lower, diagonal and upper bands are
    * given by {@code a}, {@code b} and {@code c}, respectively. The size of
    * the matrix n is given by the size of {@code b}, and the matrix structure
    * looks like
    * <pre>
    *  [ b0 c0                              ]
    *  [ a0 b1 c1                           ]
    *  [    a1 b2 c2                        ]
    *  [                                    ]
    *  [            ....                    ]
    *  [                                    ]
    *  [               a(n-3) b(n-2) c(n-2) ]
    *  [                      a(n-2) b(n-1) ]
    * </pre>
    * {@code a} and {@code b} must have sizes {@code >=} n-1; extra values are
    * ignored.  The right hand side is given by {@code r}, and the result is
    * placed in {@code x}, which is resized if necessary.
    *
    * @param x returns the solved result
    * @param a lower band; size must be {@code >=} n-1
    * @param b diagonal band; size determines the matrix size n
    * @param c upper band; size must be {@code >=} n-1
    * @param r right hand side; size must be n
    * @return {@code true} if the matrix was successfully solved or {@code
    * false} if a zero divide was encountered
    */
   public static boolean solve (
      VectorNd x, VectorNd a, VectorNd b, VectorNd c, VectorNd r) {
      int n = b.size();
      if (n == 0) {
         throw new IllegalArgumentException (
            "system size (defined by the size of 'b') is 0");
      }
      if (c.size() < n-1) {
         throw new IllegalArgumentException (
            "'c' has size "+c.size()+"; must be >= n-1 where n=" + n);
      }
      if (a.size() < n-1) {
         throw new IllegalArgumentException (
            "'a' has size "+a.size()+"; must be >= n-1 where n=" + n);
      }
      if (r.size() != n) {
         throw new IllegalArgumentException (
            "'r' has size "+r.size()+"; expected n where n=" + n);
      }
      if (x.size() != n) {
         x.setSize (n);
      }

      double[] xbuf = x.getBuffer();
      double[] rbuf = r.getBuffer();
      double[] bbuf = b.getBuffer();

      if (bbuf[0] == 0) {
         return false;
      }
      else if (n == 1) {
         x.set(0, rbuf[0]/bbuf[0]);
         return true;
      }

      double[] abuf = a.getBuffer();
      double[] cbuf = c.getBuffer();
      double[] cmod = new double[n-1]; // modified version of c

      // forward elimination step
      cmod[0] = cbuf[0]/bbuf[0];
      xbuf[0] = rbuf[0]/bbuf[0];
      for (int i=1; i<n; i++) {
         double denom = bbuf[i] - abuf[i-1]*cmod[i-1];
         if (denom == 0) {
            return false;
         }
         if (i < n-1) {
            cmod[i] = cbuf[i]/denom;
         }
         xbuf[i] = (rbuf[i] - abuf[i-1]*xbuf[i-1])/denom;
      }
      // backward elimination step
      for (int i=n-2; i>=0; i--) {
         xbuf[i] -= cmod[i]*xbuf[i+1];
      }
      return true;
   }      

   /**
    * Vectorized version of {@link
    * #solve(VectorNd,VectorNd,VectorNd,VectorNd,VectorNd) solve()} that
    * accepts a right hand side {@code rvals} consisting of an array of n
    * 3-vectors, where nis the matrix size, and creates and returns an array of
    * n 3-vectors containing the corresponding solution. In effect, the method
    * applies the scalar solve to each of the {@code x}, {@code y}, {@code z}
    * fields of {@code rvals}.
    *
    * @param a lower band; size must be {@code >=} n-1
    * @param b diagonal band; size determines the matrix size n
    * @param c upper band; size must be {@code >=} n-1
    * @param rvals right hand side; length must be n
    * @return array of 3-vectors containing the solution, or {@code null} if
    * a zero divide was encountered
    */
   public static Vector3d[] solve (
      VectorNd a, VectorNd b, VectorNd c, Vector3d[] rvals) {
      int n = b.size();
      if (n == 0) {
         throw new IllegalArgumentException (
            "system size (defined by the size of 'b') is 0");
      }
      if (c.size() < n-1) {
         throw new IllegalArgumentException (
            "'c' has size "+c.size()+"; must be >= n-1 where n=" + n);
      }
      if (a.size() < n-1) {
         throw new IllegalArgumentException (
            "'a' has size "+a.size()+"; must be >= n-1 where n=" + n);
      }
      if (rvals.length != n) {
         throw new IllegalArgumentException (
            "'rvals' has length "+rvals.length+"; expected n where n=" + n);
      }
      Vector3d[] xvals = new Vector3d[n];
      for (int i=0; i<n; i++) {
         xvals[i] = new Vector3d();
      }

      double[] bbuf = b.getBuffer();

      if (bbuf[0] == 0) {
         return null;
      }
      else if (n == 1) {
         xvals[0].scale (1/bbuf[0], rvals[0]);
         return xvals;
      }

      double[] abuf = a.getBuffer();
      double[] cbuf = c.getBuffer();
      double[] cmod = new double[n-1]; // modified version of c

      // forward elimination step
      cmod[0] = cbuf[0]/bbuf[0];
      xvals[0].scale (1/bbuf[0], rvals[0]);
      for (int i=1; i<n; i++) {
         double denom = bbuf[i] - abuf[i-1]*cmod[i-1];
         if (denom == 0) {
            return null;
         }
         if (i < n-1) {
            cmod[i] = cbuf[i]/denom;
         }
         xvals[i].scaledAdd (-abuf[i-1], xvals[i-1], rvals[i]);
         xvals[i].scale (1/denom);
      }
      // backward elimination step
      for (int i=n-2; i>=0; i--) {
         xvals[i].scaledAdd (-cmod[i], xvals[i+1]);
      }
      return xvals;
   }      

   /**
    * Solves a cyclical tridiagonal matrix whose lower, diagonal and upper
    * bands are given by {@code a}, {@code b} and {@code c}, respectively.  The
    * size of the matrix n is given by the size of {@code b}. Cyclical
    * tridiagonal matrices arise in the analysis of periodic systems, and have
    * the same structure as standard tridiagonal matrices, with addition of two
    * terms in upper right and lower left corners:
    * <pre>
    *  [ b0 c0                       a(n-1) ]
    *  [ a0 b1 c1                           ]
    *  [    a1 b2 c2                        ]
    *  [                                    ]
    *  [            ....                    ]
    *  [                                    ]
    *  [               a(n-3) b(n-2) c(n-2) ]
    *  [ c(n-1)               a(n-2) b(n-1) ]
    * </pre>
    * {@code a} and {@code b} must have sizes {@code >=} n, with {@code a(n-1)}
    * and {@code c(n-1)} supplying the upper right and lower left terms,
    * respectively. The right hand side is given by {@code r}, and the result
    * is placed in {@code x}, which is resized if necessary.
    *
    * @param x returns the solved result
    * @param a lower band; size must be {@code >=} n
    * @param b diagonal band; size determines the matrix size n
    * @param c upper band; size must be {@code >=} n
    * @param r right hand side; size must be n
    * @return {@code true} if the matrix was successfully solved or {@code
    * false} if a zero divide was encountered
    */
   public static boolean solveCyclical (
      VectorNd x, VectorNd a, VectorNd b, VectorNd c, VectorNd r) {
      int n = b.size();
      if (n < 3) {
         throw new IllegalArgumentException (
            "system size n (defined by the size of 'b') is less than 3");
      }
      if (c.size() < n) {
         throw new IllegalArgumentException (
            "'c' has size "+c.size()+"; must be >= n where n=" + n);
      }
      if (a.size() < n) {
         throw new IllegalArgumentException (
            "'a' has size "+a.size()+"; must be >= n where n=" + n);
      }
      if (a.size() != n) {
         throw new IllegalArgumentException (
            "'a' has size "+a.size()+"; expected n where n=" + n);
      }
      if (r.size() != n) {
         throw new IllegalArgumentException (
            "'r' has size "+r.size()+"; expected n where n=" + n);
      }
      if (x.size() != n) {
         x.setSize (n);
      }

      double[] xbuf = x.getBuffer();
      double[] rbuf = r.getBuffer();
      double[] bbuf = b.getBuffer();
      double[] abuf = a.getBuffer();
      double[] cbuf = c.getBuffer();
      double[] cmod = new double[n-1]; // modified version of c
      double[] qbuf = new double[n];

      // Let alpha = a(n-1), beta = c(n-1), and gamma defined so that b(0) -
      // gamma != 0. Then define the true triadiagonal matrix formed by
      //
      // B = A = u*v^T
      //
      // where u = (gamma, 0, ..., 0, beta) and v = (1, 0, ..., 0, alpha/gamma).

      double alpha = abuf[n-1];
      double beta = cbuf[n-1];
      double gamma = bbuf[0] == 0 ? -1 : -bbuf[0];

      // first step is to solve for B x = r and B q = u.
      // forward elimination for x and q:
      double denom = bbuf[0]-gamma;
      cmod[0] = cbuf[0]/denom;
      qbuf[0] = gamma/denom;
      xbuf[0] = rbuf[0]/denom;
      for (int i=1; i<n-1; i++) {
         denom = bbuf[i] - abuf[i-1]*cmod[i-1];
         if (denom == 0) {
            return false;
         }
         cmod[i] = cbuf[i]/denom;
         qbuf[i] = (-abuf[i-1]*qbuf[i-1])/denom;
         xbuf[i] = (rbuf[i] - abuf[i-1]*xbuf[i-1])/denom;
      }
      denom = bbuf[n-1] - alpha*beta/gamma - abuf[n-2]*cmod[n-2];
      if (denom == 0) {
         return false;
      }
      qbuf[n-1] = (beta - abuf[n-2]*qbuf[n-2])/denom;
      xbuf[n-1] = (rbuf[n-1] - abuf[n-2]*xbuf[n-2])/denom;
      // backward elimination for x and q:
      for (int i=n-2; i>=0; i--) {
         qbuf[i] -= cmod[i]*qbuf[i+1];
         xbuf[i] -= cmod[i]*xbuf[i+1];
      }
      // now compute the true x solution from
      //
      // x = x - (q*v^T*x)/(1+v^T*q)
      double vdotq = qbuf[0] + alpha/gamma*qbuf[n-1];
      double vdotx = xbuf[0] + alpha/gamma*xbuf[n-1];
      if (1+vdotq == 0) {
         return false;
      }
      double scale = vdotx/(1+vdotq);
      for (int i=0; i<n; i++) {
         xbuf[i] -= scale*qbuf[i];
      }
      return true;
   }      

   /**
    * Vectorized version of {@link
    * #solveCyclical(VectorNd,VectorNd,VectorNd,VectorNd,VectorNd)
    * solveCyclical()} that accepts a right hand side {@code rvals} consisting
    * of an array of n 3-vectors, where n is the matrix size, and creates and
    * returns an array of n 3-vectors containing the corresponding solution. In
    * effect, the method applies the scalar cyclical solve to each of the
    * {@code x}, {@code y}, {@code z} fields of {@code rvals}.
    *
    * @param a lower band; size must be {@code >=} n
    * @param b diagonal band; size determines the matrix size n
    * @param c upper band; size must be {@code >=} n
    * @param rvals right hand side; length must be n
    * @return array of 3-vectors containing the solution, or {@code null} if
    * a zero divide was encountered
    */
   public static Vector3d[] solveCyclical (
      VectorNd a, VectorNd b, VectorNd c, Vector3d[] rvals) {
      int n = b.size();
      if (n < 3) {
         throw new IllegalArgumentException (
            "system size n (defined by the size of 'b') is less than 3");
      }
      if (c.size() < n) {
         throw new IllegalArgumentException (
            "'c' has size "+c.size()+"; must be >= n where n=" + n);
      }
      if (a.size() < n) {
         throw new IllegalArgumentException (
            "'a' has size "+a.size()+"; must be >= n where n=" + n);
      }
      if (rvals.length != n) {
         throw new IllegalArgumentException (
            "'rvals' has length "+rvals.length+"; expected n where n=" + n);
      }
      Vector3d[] xvals = new Vector3d[n];
      for (int i=0; i<n; i++) {
         xvals[i] = new Vector3d();
      }

      double[] bbuf = b.getBuffer();
      double[] abuf = a.getBuffer();
      double[] cbuf = c.getBuffer();
      double[] cmod = new double[n-1]; // modified version of c
      double[] qbuf = new double[n];

      // Let alpha = a(n-1), beta = c(n-1), and gamma defined so that b(0) -
      // gamma != 0. Then define the true triadiagonal matrix formed by
      //
      // B = A = u*v^T
      //
      // where u = (gamma, 0, ..., 0, beta) and v = (1, 0, ..., 0, alpha/gamma).

      double alpha = abuf[n-1];
      double beta = cbuf[n-1];
      double gamma = bbuf[0] == 0 ? -1 : -bbuf[0];

      // first step is to solve for B x = r and B q = u.
      // forward elimination for x and q:
      double denom = bbuf[0]-gamma;
      cmod[0] = cbuf[0]/denom;
      qbuf[0] = gamma/denom;
      xvals[0].scale (1/denom, rvals[0]);
      for (int i=1; i<n-1; i++) {
         denom = bbuf[i] - abuf[i-1]*cmod[i-1];
         if (denom == 0) {
            return null;
         }
         cmod[i] = cbuf[i]/denom;
         qbuf[i] = (-abuf[i-1]*qbuf[i-1])/denom;
         xvals[i].scaledAdd (-abuf[i-1], xvals[i-1], rvals[i]);
         xvals[i].scale (1/denom);
      }
      denom = bbuf[n-1] - alpha*beta/gamma - abuf[n-2]*cmod[n-2];
      if (denom == 0) {
         return null;
      }
      qbuf[n-1] = (beta - abuf[n-2]*qbuf[n-2])/denom;
      xvals[n-1].scaledAdd (-abuf[n-2], xvals[n-2], rvals[n-1]);
      xvals[n-1].scale (1/denom);
      // backward elimination for x and q:
      for (int i=n-2; i>=0; i--) {
         qbuf[i] -= cmod[i]*qbuf[i+1];
         xvals[i].scaledAdd (-cmod[i], xvals[i+1]);
      }
      // now compute the true x solution from
      //
      // x = x - (q*v^T*x)/(1+v^T*q)
      double vdotq = qbuf[0] + alpha/gamma*qbuf[n-1];
      Vector3d vdotx = new Vector3d();
      vdotx.scaledAdd (alpha/gamma, xvals[n-1], xvals[0]);
      if (1+vdotq == 0) {
         return null;
      }
      for (int i=0; i<n; i++) {
         xvals[i].scaledAdd (-qbuf[i]/(1+vdotq), vdotx);
      }
      return xvals;
   }      


}
