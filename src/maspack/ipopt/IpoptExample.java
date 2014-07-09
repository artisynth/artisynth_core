/**
 * Copyright (c) 2014, by the Authors: Ian Stavness (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.ipopt;

import maspack.matrix.VectorNd;

/**
 * Ipopt Interface implementing example problem HS071 from documentation
 * 
 * @author stavness
 * 
 */

public class IpoptExample extends IpoptInterface
{
   boolean useQuasiNewton = true;
   boolean nativeSupportLoaded = false;
   int n = -1; /* number of variables */
   int m = -1; /* number of constraints */

   public IpoptExample() throws UnsupportedOperationException
   {
      super();
   }

   public void create()
   {

      double[] x_L = null; /* lower bounds on x */
      double[] x_U = null; /* upper bounds on x */
      double[] g_L = null; /* lower bounds on g */
      double[] g_U = null; /* upper bounds on g */
      int nele_jac = 8;
      int nele_hess = 10;
      int index_style = 0;
      
      double[] x = null;                    /* starting point and solution vector */
      double[] mult_x_L = null;             /* lower bound multipliers at the solution */
      double[] mult_x_U = null;             /* upper bound multipliers
      					  at the solution */
      double obj;                          /* objective value */
      int i;                             /* generic counter */

      n = 4;
      x_L = new double[n];
      x_U = new double[n];
      for (i=0; i<n; i++) {
	    x_L[i] = 1.0;
	    x_U[i] = 5.0;
      }
      
      m=2;
      g_L = new double[m];
      g_U = new double[m];
      /* set the values of the constraint bounds */
      g_L[0] = 25;
      g_U[0] = 2e19;
      g_L[1] = 40;
      g_U[1] = 40;
      
      try
      {	 createNLP(n, x_L, x_U, m, g_L, g_U, nele_jac, nele_hess, index_style);
      }
      catch (Exception e)
      {
	 e.printStackTrace();
      }
      
      setOptions();
   }
   
   protected void setOptions()
   {
      addIpoptNumOption("tol", 1e-7);
      addIpoptStrOption("mu_strategy", "adaptive");
      addIpoptStrOption("output_file", "ipopt.out");
      addIpoptStrOption("derivative_test", "first-order");
      addIpoptStrOption("derivative_test_print_all", "yes");
//      addIpoptNumOption("derivative_test_perturbation",1e-4);
      if (useQuasiNewton)
	 addIpoptStrOption("hessian_approximation", "limited-memory");
   }
   
   public void solve()
   {
      double[] x = new double[n];
      x[0] = 1.0;
      x[1] = 5.0;
      x[2] = 5.0;
      x[3] = 1.0;

      /* allocate space to store the bound multipliers at the solution */
      double[] mult_x_L = new double[n];
      double[] mult_x_U = new double[n];
      double[] obj_value = new double[1];
      
      solveNLP(x, obj_value, mult_x_L, mult_x_U, n);
      
      VectorNd vec = new VectorNd(n);
      vec.set(x);
      System.out.println("\n~~~~~~ Solution Found ~~~~~~~");
      System.out.println("x_opt = " + vec.toString("%4.2e"));
      System.out.println("f(x_opt) = " + obj_value[0]);
   }
   
   
   
   // Ipopt Callback Functions:
   /** A pointer for anything that is to be passed between the called
    *  and individual callback function */
//   typedef void * UserDataPtr;

   /** Type defining the callback function for evaluating the value of
    *  the objective function.  Return value should be set to false if
    *  there was a problem doing the evaluation. */
   protected boolean Eval_F(int n, double[] x, boolean new_x,
            double[] obj_value)
//   , UserDataPtr user_data)
   {
      // System.out.println("Eval_F() in Java.");
      if (obj_value == null)
	 { System.err.println("obj_value null"); return false;}
      if (x == null)
	 { System.err.println("x null"); return false;}
      if (n != 4)
	 return false;
      
      VectorNd v = new VectorNd(n);
      v.set(x);
      // System.out.println("x = " + v.toString());
      
      obj_value[0] = x[0] * x[3] * (x[0] + x[1] + x[2]) + x[2];
      
      // System.out.println("f = " + obj_value[0]);
      
      return true;
   }

   /**
    * Type defining the callback function for evaluating the gradient of the
    * objective function. Return value should be set to false if there was a
    * problem doing the evaluation.
    */
   protected boolean Eval_Grad_F(int n, double[] x, boolean new_x,
                    double[] grad_f)
//   , UserDataPtr user_data)
   {
      // System.out.println("Eval_Grad_F() in Java.");
      if(n != 4)
	 return false;
      VectorNd v = new VectorNd(n);
      v.set(x);
      // System.out.println("x = " + v.toString());
      
      grad_f[0] = x[0] * x[3] + x[3] * (x[0] + x[1] + x[2]);
      grad_f[1] = x[0] * x[3];
      grad_f[2] = x[0] * x[3] + 1;
      grad_f[3] = x[0] * (x[0] + x[1] + x[2]);

      v.set(grad_f);
      // System.out.println("grad_f = " + v.toString());
      return true;
   }
   
   /**
    * Type defining the callback function for evaluating the value of the
    * constraint functions. Return value should be set to false if there was a
    * problem doing the evaluation.
    */
   protected boolean Eval_G(int n, double[] x, boolean new_x,
               int m, double[] g)
//   , UserDataPtr user_data);
   {
      // System.out.println("Eval_G() in Java.");
      if(n != 4||m != 2)
	 return false;

      g[0] = x[0] * x[1] * x[2] * x[3];
      g[1] = x[0]*x[0] + x[1]*x[1] + x[2]*x[2] + x[3]*x[3];
      return true;
   }
   
   /**
    * Type defining the callback function for evaluating the Jacobian of the
    * constrant functions. Return value should be set to false if there was a
    * problem doing the evaluation.
    */
   protected boolean Eval_Jac_G(int n, double[] x, boolean new_x,
                   int m, int nele_jac,
                   int[]iRow, int[]jCol, double[]values)
//                   , UserDataPtr user_data);
   {
      // System.out.println("Eval_Jac_G() in Java.");

	    /* return the structure of the jacobian */
	  if (values == null) {	
	    /* this particular jacobian is dense */
	    iRow[0] = 0;
	    jCol[0] = 0;
	    iRow[1] = 0;
	    jCol[1] = 1;
	    iRow[2] = 0;
	    jCol[2] = 2;
	    iRow[3] = 0;
	    jCol[3] = 3;
	    iRow[4] = 1;
	    jCol[4] = 0;
	    iRow[5] = 1;
	    jCol[5] = 1;
	    iRow[6] = 1;
	    jCol[6] = 2;
	    iRow[7] = 1;
	    jCol[7] = 3;
	  }
	  else
	  {
	     /* return the values of the jacobian of the constraints */
	     
	    values[0] = x[1]*x[2]*x[3]; /* 0,0 */
	    values[1] = x[0]*x[2]*x[3]; /* 0,1 */
	    values[2] = x[0]*x[1]*x[3]; /* 0,2 */
	    values[3] = x[0]*x[1]*x[2]; /* 0,3 */

	    values[4] = 2*x[0];         /* 1,0 */
	    values[5] = 2*x[1];         /* 1,1 */
	    values[6] = 2*x[2];         /* 1,2 */
	    values[7] = 2*x[3];         /* 1,3 */
	    
	    VectorNd v = new VectorNd(nele_jac);
	    v.set(values);
	    // System.out.println("jac_G = "+v.toString());
	  }
	  

	  return true;

   }
   
   /**
    * Type defining the callback function for evaluating the Hessian of the
    * Lagrangian function. Return value should be set to false if there was a
    * problem doing the evaluation.
    */
   protected boolean Eval_H(int n, double[]x, boolean new_x, double obj_factor,
               int m, double[]lambda, boolean new_lambda,
               int nele_hess, int[]iRow, int[]jCol,
               double[]values)
//   , UserDataPtr user_data);
   {
      
      if (useQuasiNewton)
      {	 System.out.println("hessian UNevaluated");
	 return false;
      }
      else
      {
	 System.out.println("hessian Evaluated"); 
      }
      // System.out.println("Eval_H() in Java.");
      int idx = 0; /* nonzero element counter */
      int row = 0; /* row counter for loop */
      int col = 0; /* col counter for loop */

      if (values == null) {
        /* return the structure. This is a symmetric matrix, fill the lower left
         * triangle only. */

        /* the hessian for this problem is actually dense */
        idx=0;
        for (row = 0; row < 4; row++) {
          for (col = 0; col <= row; col++) {
            iRow[idx] = row;
            jCol[idx] = col;
            idx++;
          }
        }

        if(idx != nele_hess)
        {
           System.out.println("Eval_H java callback - nele_hess not size of indices");
        }
      } else {        
           /* return the values. This is a symmetric matrix, 
            * fill the lower left triangle only */

        /* fill the objective portion */
        values[0] = obj_factor * (2*x[3]);               /* 0,0 */

        values[1] = obj_factor * (x[3]);                 /* 1,0 */
        values[2] = 0;                                   /* 1,1 */

        values[3] = obj_factor * (x[3]);                 /* 2,0 */
        values[4] = 0;                                   /* 2,1 */
        values[5] = 0;                                   /* 2,2 */

        values[6] = obj_factor * (2*x[0] + x[1] + x[2]); /* 3,0 */
        values[7] = obj_factor * (x[0]);                 /* 3,1 */
        values[8] = obj_factor * (x[0]);                 /* 3,2 */
        values[9] = 0;                                   /* 3,3 */


        /* add the portion for the first constraint */
        values[1] += lambda[0] * (x[2] * x[3]);          /* 1,0 */

        values[3] += lambda[0] * (x[1] * x[3]);          /* 2,0 */
        values[4] += lambda[0] * (x[0] * x[3]);          /* 2,1 */

        values[6] += lambda[0] * (x[1] * x[2]);          /* 3,0 */
        values[7] += lambda[0] * (x[0] * x[2]);          /* 3,1 */
        values[8] += lambda[0] * (x[0] * x[1]);          /* 3,2 */

        /* add the portion for the second constraint */
        values[0] += lambda[1] * 2;                      /* 0,0 */

        values[2] += lambda[1] * 2;                      /* 1,1 */

        values[5] += lambda[1] * 2;                      /* 2,2 */

        values[9] += lambda[1] * 2;                      /* 3,3 */
      }

      return true;
   }

}
