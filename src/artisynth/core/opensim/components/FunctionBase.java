package artisynth.core.opensim.components;

import maspack.matrix.VectorNd;

public abstract class FunctionBase extends OpenSimObject {

   /**
    * Evaluates the function at the given input value
    */
   public abstract double evaluate(VectorNd x);
   
   /**
    * Evaluates the function derivative at the given input value
    */
   public abstract void evaluateDerivative (VectorNd x, VectorNd df);
   
   @Override
   public FunctionBase clone () {
      return (FunctionBase)super.clone ();
   }

}
