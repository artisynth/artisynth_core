package artisynth.core.opensim.components;

import maspack.matrix.VectorNd;

public class Constant extends FunctionBase {
   
   double value;
   
   public Constant() {
      value = 0;
   }
   
   public Constant(double val) {
      value = val;
   }
   
   public double getValue() {
      return value;
   }
   
   public void setValue(double val) {
      value = val;
   }

   @Override
   public double evaluate (VectorNd x) {
      return value;
   }
   
   @Override
   public void evaluateDerivative (VectorNd cvals, VectorNd df) {
      df.setZero ();
   }
   
   @Override
   public Constant clone () {
      Constant c = (Constant)super.clone ();
      c.setValue (value);
      return c;
   }
}
