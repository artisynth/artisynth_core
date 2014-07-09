/**
 * Copyright (c) 2014, by the Authors: Ian Stavness (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.ipopt;


public class JipoptTest {
	
   public static void main(String []args){
      // Create the problem
      JipoptExample hs071 = new JipoptExample();
      
      //add options, the same with IPOPT.
      //hs071.setNumericOption("tol",1E-7); //hs071.setNumericOption(Ipopt.KEY_TOL,1E-7);
      //hs071.setStringOption(Ipopt.KEY_MU_STRATEGY,"adaptive");
      //hs071.setStringOption(Ipopt.KEY_OUTPUT_FILE,"hs071cpp.out");
      //hs071.setStringOption(Ipopt.KEY_PRINT_USER_OPTIONS,"yes");
      hs071.setStringOption("nlp_scaling_method","usER-ScAling");//ignor case
      //hs071.setStringOption(Ipopt.KEY_HESSIAN_APPROXIMATION,"lImIted-memory");//LBFGS
      //hs071.setStringOption(Ipopt.KEY_DERIVATIVE_TEST,"first-order");
      //hs071.setStringOption(Ipopt.KEY_PRINT_USER_OPTIONS,"yes");
      //hs071.setStringOption("print_options_documentation","yes");
      hs071.OptimizeNLP();        
      
      
      //Below see results
      double x[]=hs071.getState();
      hs071.print(x,"Optimal Solution:");
      
      double []MLB=hs071.getMultLowerBounds();
      hs071.print(x,"Multipler LowerBounds:");
      
      double[] MUB=hs071.getMultUpperBounds();
      hs071.print(MUB,"Multipler UpperBounds:");
      
      double obj=hs071.getObjVal();
      System.out.println("Obj Value="+obj);
      
      double[] constraints=hs071.getMultConstraints();
      hs071.print(constraints,"G(x):");
      double[]lam=hs071.getMultConstraints();
      hs071.print(lam,"Constraints Multipler");
      
  }    


}
