/**
 * Copyright (c) 2014, by the Authors: Ian Stavness (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.ipopt;

import maspack.fileutil.NativeLibraryManager;

/**
 * Java side of JNI to the Ipopt "C" interface
 * 
 * @author stavness
 * 
 */

public abstract class IpoptInterface
{
   boolean nativeSupportLoaded = false;

   public IpoptInterface() throws UnsupportedOperationException
   {
      if (!nativeSupportLoaded)
      { // try loading in the native code
	 try
	 {
//	    System.out.println("java.library.path = " + 
//		     System.getProperty("java.library.path"));
	    NativeLibraryManager.load("IpoptInterface");
	    initialize();
	    nativeSupportLoaded = true;
	    setObj();
	 }
	 catch (UnsatisfiedLinkError e)
	 {
	    // e.printStackTrace();
	    System.out.println(e.getMessage());
	    throw new UnsupportedOperationException(
		     "Can't load native library \"IpoptInterface\"");
	 }
      }
   }

   /*
    *  Ipopt JNI Native Methods
    */
   protected static native void initialize();
   
   protected native void createNLP(int n, double[] x_L, double[] x_U, int m,
	    double[] g_L, double[] g_U, int nele_jac, int nele_hess,
	    int index_style);
   
   protected native void solveNLP(double[] x, double[] obj_factor, 
	    double[] mult_x_L, double[] mult_x_U, int n);

   protected native void addIpoptIntOption(String name, int val);

   protected native void addIpoptNumOption(String name, double val);
   
   protected native void addIpoptStrOption(String name, String val);
   
   protected native void testCallbacks();
   
   protected native void setObj();
   
   /*
    *  Ipopt Callback Methods:
    */
   protected boolean callbackTest(int n)
   {
      System.out.println("callback() in Java - int arg n = "+n);
      return true;
   }  
   
   protected abstract boolean Eval_F(int n, double[] x, boolean new_x,
            double[] obj_value);

   protected abstract boolean Eval_Grad_F(int n, double[] x, boolean new_x,
                    double[] grad_f);
   
   protected abstract boolean Eval_G(int n, double[] x, boolean new_x,
               int m, double[] g);
   
   protected abstract boolean Eval_Jac_G(int n, double[] x, boolean new_x,
                   int m, int nele_jac,
                   int[]iRow, int[]jCol, double[]values);

   protected abstract boolean Eval_H(int n, double[]x, boolean new_x, double obj_factor,
               int m, double[]lambda, boolean new_lambda,
               int nele_hess, int[]iRow, int[]jCol,
               double[]values);

}
