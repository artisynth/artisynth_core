package artisynth.core.probes;

import maspack.matrix.VectorNd;
import maspack.util.Clonable;

/**
 * Defines a base implementation for DataFunction that also implements
 * Clonable.
 */
public abstract class DataFunctionBase implements DataFunction, Clonable {

   /**
    * {@inheritDoc}
    */
   public abstract void eval (VectorNd vec, double t, double trel);

   /**
    * {@inheritDoc}
    */
   public Object clone() throws CloneNotSupportedException {
      return super.clone();
   }   

}
