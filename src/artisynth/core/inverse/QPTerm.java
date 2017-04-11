package artisynth.core.inverse;

import maspack.matrix.MatrixNd;
import maspack.matrix.VectorNd;
import maspack.properties.HasProperties;

public interface QPTerm extends HasProperties {
   public void setWeight(double w);
     
   public void setSize(int size);
   public int getSize();
   
   public void dispose();
   
   /**
    * Sums this term to the arguments Q and P
    * @param Q the Quadratic term
    * @param P the Proportional term
    * @param t0 time at start of step
    * @param t1 time at end of step
    */
   public void getQP(MatrixNd Q, VectorNd P, double t0, double t1);
}
