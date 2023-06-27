package artisynth.core.opensim.components;

import maspack.matrix.VectorNd;
import maspack.function.Diff1FunctionNx1;

public abstract class FunctionBase extends OpenSimObject {

   /**
    * Evaluates the function at the given input value
    */
   public abstract double eval(VectorNd x);
   
   /**
    * Evaluates the function derivative at the given input value
    */
   public abstract void evalDeriv (VectorNd df, VectorNd x);
   
   public abstract Diff1FunctionNx1 getFunction();
   
   @Override
   public FunctionBase clone () {
      FunctionBase f = (FunctionBase)super.clone ();
      return f;
   }

}
