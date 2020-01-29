package artisynth.core.opensim.components;

import maspack.matrix.VectorNd;

public class MultiplierFunction extends FunctionBase {
   
   double scale;
   FunctionBase function;
   
   public MultiplierFunction() {
      scale = 0;
      function = null;
   }
   
   public MultiplierFunction(double scale, FunctionBase function) {
      this.scale = scale;
      setFunction(function);
   }
   
   public double getScale() {
      return scale;
   }
   
   public void setScale(double scale) {
      this.scale = scale;
   }
   
   public FunctionBase getFunction() {
      return function;
   }
   
   public void setFunction(FunctionBase f) {
      function = f;
      function.setParent (this);
   }

   @Override
   public double evaluate (VectorNd x) {
      return scale*function.evaluate (x);
   }
   
   @Override
   public void evaluateDerivative (VectorNd x, VectorNd df) {
      function.evaluateDerivative (x, df);
      df.scale (scale);
   }
   
   @Override
   public MultiplierFunction clone () {
      MultiplierFunction c = (MultiplierFunction)super.clone ();
      if (function != null) {
         c.setFunction(function.clone());
      }
      return c;
   }
}
