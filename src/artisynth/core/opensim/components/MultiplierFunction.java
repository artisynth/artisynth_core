package artisynth.core.opensim.components;

import maspack.function.*;
import maspack.matrix.VectorNd;

public class MultiplierFunction extends FunctionBase {
   
   double scale;
   FunctionBase function;
   Diff1FunctionNx1 myFxn;
   
   public MultiplierFunction() {
      scale = 0;
      function = null;
   }
   
   public MultiplierFunction(double scale, FunctionBase function) {
      this.scale = scale;
      setBaseFunction(function);
   }
   
   public double getScale() {
      return scale;
   }
   
   public void setScale(double scale) {
      this.scale = scale;
   }
   
   public FunctionBase getBaseFunction() {
      return function;
   }
   
   public void setBaseFunction(FunctionBase f) {
      function = f;
      function.setParent (this);
   }

   public Diff1FunctionNx1 getFunction() {
      if (myFxn == null) {
         Diff1FunctionNx1 fxn = function.getFunction();
         if (fxn instanceof Diff1Function1x1) {
            myFxn = 
               new ScaledDiff1Function1x1 (scale, (Diff1Function1x1)fxn);
         }
         else {
            myFxn = 
               new ScaledDiff1FunctionNx1 (scale, fxn);
         }
      }
      return myFxn;
   }
   
   @Override
   public double eval (VectorNd x) {
      return scale*function.eval (x);
   }
   
   @Override
   public void evalDeriv (VectorNd df, VectorNd x) {
      function.evalDeriv (df, x);
      df.scale (scale);
   }
   
   @Override
   public MultiplierFunction clone () {
      MultiplierFunction c = (MultiplierFunction)super.clone ();
      if (function != null) {
         c.setBaseFunction(function.clone());
      }
      c.myFxn = null; // clone should recreate function
      return c;
   }
}
