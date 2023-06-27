package artisynth.core.opensim.components;

import maspack.matrix.VectorNd;
import maspack.function.ConstantFunction1x1;
import maspack.function.Diff1FunctionNx1;

public class Constant extends FunctionBase {
   
   double value;
   ConstantFunction1x1 myFxn;
   
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
   public double eval (VectorNd x) {
      return value;
   }
   
   @Override
   public void evalDeriv (VectorNd df, VectorNd cvals) {
      df.setZero ();
   }

   public ConstantFunction1x1 getFunction() {
      if (myFxn == null) {
         myFxn = new ConstantFunction1x1 (value);
      }
      return myFxn;
   }

   @Override
   public Constant clone () {
      Constant c = (Constant)super.clone ();
      c.setValue (value);
      c.myFxn = null; // clone should recreate function
      return c;
   }
}
