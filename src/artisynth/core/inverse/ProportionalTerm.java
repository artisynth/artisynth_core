package artisynth.core.inverse;

/**
 * Adds a cost proportional to the sum of the values
 * of the vector. In combination with a lower bound
 * of zero, this term adds a cost on the L1-norm.
 * @author Teun
 */
public class ProportionalTerm extends QPTermBase {

   public static final double defaultWeight = 1e-3;
   
   public ProportionalTerm() {
      this(defaultWeight);
   }
   
   public ProportionalTerm(double weight) {
      super(weight);
   }

   @Override
   protected void compute (double t0, double t1) {
      for (int i=0; i<P.size(); i++) {
         P.set(i,myWeight);
      }
   }
}
