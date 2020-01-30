package artisynth.core.opensim.components;

import maspack.matrix.VectorNd;

public class LinearFunction extends FunctionBase {
   
   double coefficients[];
   
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
   public double evaluate(VectorNd x) {
      double out = 0;
      int i = 0;
      for (i=0; i<x.size() && i<coefficients.length-1; ++i) {
         out += coefficients[i]*x.get(i); 
      }
      out += coefficients[i];
      
      return out;
   }
   
   @Override
   public void evaluateDerivative (VectorNd x, VectorNd df) {
      for (int i=0; i<x.size() && i<coefficients.length-1; ++i) {
         df.set(i,coefficients[i]); 
      }
   }

   @Override
   public LinearFunction clone () {
      LinearFunction lf = (LinearFunction)super.clone ();
      if (lf.coefficients != null) {
         lf.coefficients = coefficients.clone ();
      }
      return lf;
   }
   

}
