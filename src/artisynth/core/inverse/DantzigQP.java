/**
 * Copyright (c) 2014, by the Authors: Ian Stavness (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.inverse;

import maspack.matrix.MatrixNd;
import maspack.matrix.VectorNd;
import maspack.solvers.DantzigLCPSolver;

public class DantzigQP implements QPSolver
{
   boolean debug = false;
   int n = 0;
   int m = 0;
   VectorNd q = new VectorNd(0);
   VectorNd tmpQ = new VectorNd(0);
   VectorNd tmpF = new VectorNd(0);
   VectorNd z = new VectorNd(0);
   boolean[] zBasic = new boolean[0];
   MatrixNd M = new MatrixNd(0, 0);
   MatrixNd AQ = new MatrixNd(0, 0);
   
   MatrixNd myQ = new MatrixNd (0, 0);
   VectorNd myF = new VectorNd (0);
   MatrixNd myA = new MatrixNd (0, 0);
   MatrixNd A = new MatrixNd (0, 0);
   VectorNd myX = new VectorNd (0);
   VectorNd myB = new VectorNd (0);
   
   DantzigLCPSolver lcp;   
   
   public DantzigQP()
   {
      lcp = new DantzigLCPSolver();
   }
   
   public void solve(double[] x, double[][] Q, double[] f, double[][] A,
      double[] b, double[] lb, double[] ub, double[] x0) throws Exception
   {
      if (x == null)
      { throw new IllegalArgumentException("x is null");
      }
      int n = x.length;
      int m = (A==null?0:A.length) + (lb==null?0:n) + (ub==null?0:n);
      
      if (n != this.n || m != this.m)
      {
         System.out.println("recreating problem: n = "+n+", m = "+m);
         setSize (n, m);
      }
      setConstraints (A, b, lb, ub);
      solve (x, Q, f, x0);
   }

   public void solve(double[] x, double[][] Q, double[] f, double[] x0)
      throws Exception
   {
      assert n == this.n;
      if (x == null)
      { throw new IllegalArgumentException("x is null");
      }     
      if (Q == null)
      { throw new IllegalArgumentException("Q is null");
      }
      

      // LCP definition
      // 
      // Mz + q = w, z >= 0, w >= 0, z' w = 0
      //
      // q = -b - A inv(Q) f
      // M = A inv(Q) A'
      // 
      // x = inv(Q) ( A' z - f )
      //
      // where QP is given by
      //
      // min 1/2 x' Q x + f' x
      // s.t.  A x >= b
      //
      // where Q = C'C and f = C'd
      // from linear least squares prob Cx ~= d


      myQ.set (Q);
      myQ.invert();
      
      if (f != null)
         myF.set (f);
      else
         myF = null;
      
      if (myA.rowSize () > 0)
      {
         AQ.mul(myA,myQ); // temporary AQ = A inv(Q)
         M.mulTransposeRight(AQ,myA); // M = A insolvev(Q) A'

         q.negate (myB);
         if (myF != null)
         {
            tmpQ.mul(AQ,myF);
            q.sub (tmpQ);
         }

         lcp.solve(z,M,q,zBasic);

         tmpF.mulTranspose(myA,z);
         if (myF != null)
            tmpF.sub(myF);
         myX.mul(myQ,tmpF);
      }
      else
      {
         q.negate (myB);
         if (myF != null)
         {
            tmpQ.mul(myQ,myF);
            q.sub (tmpQ);
         }

         lcp.solve(z,myQ,q,zBasic);

         tmpF.set (z);
         if (f != null)
            tmpF.sub(myF);
         myX.mul(myQ,tmpF);        
      }

      if (debug)
      {      
         System.out.println ("Optimal Solution: " + myX.toString ("%g"));
      }
      
      myX.get (x);
   }
   
   public void setConstraints(double[][] A, double[] b, double[] lb, double[] ub)
   {
      int numIneqCon = (A==null?0:A.length);
      if (numIneqCon != (b==null?0:b.length))
         System.err.println("inconsistent inequality constraint arguments");

      myA.setIdentity ();

      // inequality constraint
      if (A != null)
      {
         for (int i = 0; i < A.length; i++)
            for (int j = 0; j < A[i].length; j++)
               myA.set (i, j, -A[i][j]); // -Ax >= -b
      }
      
      int idx = 0;

      if (b != null)
      {
         for (int i = 0; i < b.length; i++)
            myB.set(idx++, -b[i]); // -Ax >= -b
      }
   
      if (lb != null)
      {
         for (int i = 0; i < lb.length; i++)
         {
            myA.set (idx, i, 1.0); // x >= lb
            myB.set (idx++, lb[i]);
         }
      }
      
      if (ub != null)
      {
         for (int i = 0; i < ub.length; i++)
         {
            myA.set (idx, i, -1.0); // -x >= -ub
            myB.set (idx++, -ub[i]);
         }
      }
      
      
   }

   public void setSize(int n, int m)
   {
      this.n = n;
      this.m = m;
      
      q.setSize (m);
      tmpQ.setSize (m);
      z.setSize (m);
      zBasic = new boolean[m];
      M.setSize (m,m);
      AQ.setSize (m,n);
      
      myQ.setSize (n, n);
      myF.setSize (n);
      tmpF.setSize (n);
      myX.setSize (n);

      myA.setSize (m, n);
      myB.setSize (m);
   }

}
