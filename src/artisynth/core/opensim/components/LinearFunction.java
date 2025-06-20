package artisynth.core.opensim.components;

import maspack.matrix.VectorNd;
import maspack.function.*;

public class LinearFunction extends FunctionBase {

   double coefficients[];
   Diff1FunctionNx1 myFxn;
   
   public LinearFunction() {
      coefficients = null;
   }
   
   public LinearFunction(double slope, double intercept) {
      setCoefficients (new double[] {slope, intercept});
   }
   
   public LinearFunction(double[] coeffs) {
      setCoefficients (coeffs);
   }

   public void setCoefficients(double[] coeffs) {
      this.coefficients = coeffs;
   }
   
   public double[] getCoefficients () {
      return coefficients;
   }
   
   @Override
   public double eval(VectorNd x) {
      double out = 0;
      int i = 0;
      for (i=0; i<x.size() && i<coefficients.length-1; ++i) {
         out += coefficients[i]*x.get(i); 
      }
      out += coefficients[i];
      
      return out;
   }
   
   @Override
   public void evalDeriv (VectorNd df, VectorNd x) {
      for (int i=0; i<x.size() && i<coefficients.length-1; ++i) {
         df.set(i,coefficients[i]); 
      }
   }

   public Diff1FunctionNx1 getFunction() {
      if (myFxn == null) {
         switch (coefficients.length) {
            case 0: {
               myFxn = new ConstantFunction1x1 (0);
               break;
            }
            case 1: {
               myFxn = new ConstantFunction1x1 (coefficients[0]);
               break;
            }
            case 2: {
               myFxn = new LinearFunction1x1 (coefficients[0], coefficients[1]);
               break;
            }
            default: {
               myFxn = new LinearFunctionNx1 (coefficients);
               break;
            }
         }
      }
      return myFxn;
   }

   @Override
   public LinearFunction clone () {
      LinearFunction lf = (LinearFunction)super.clone ();
      if (lf.coefficients != null) {
         lf.coefficients = coefficients.clone ();
      }
      lf.myFxn = null; // clone should recreate function
      return lf;
   }
   

}
